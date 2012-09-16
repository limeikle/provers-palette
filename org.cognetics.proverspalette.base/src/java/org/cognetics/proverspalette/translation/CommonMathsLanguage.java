package org.cognetics.proverspalette.translation;

import java.util.List;
import java.util.Set;

import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.cognetics.proverspalette.translation.isabelle.IsabelleTranslator;
import org.heneveld.isabelle.IsabelleAssumptionsGroup;
import org.heneveld.isabelle.IsabelleImplicationGroup;
import org.heneveld.isabelle.IsabelleOperatorGroup;
import org.heneveld.isabelle.IsabelleParser;
import org.heneveld.isabelle.QuantificationOperatorGroup;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;
import org.heneveld.maths.structs.MathsGroup;

/**
 * This class offers conveniences for manipulating expressions in
 * the so-called 'common' maths language used as an intermediate format.
 * 
 * (It looks a lot like Isabelle currently, but as they say,
 * pay no attention to the man behind the curtain!) 
 */
public class CommonMathsLanguage {

	public static MathsExpression getEqualsUnderAssumptions(
			List<MathsExpression> assumptions,
			MathsExpression lhs, MathsExpression rhs) {
		MathsExpression result = new IsabelleOperatorGroup(
				MathsExpressions.makeList(
						ensureHasBrackets(lhs),
						MathsExpressions.newToken("=", true),
						ensureHasBrackets(rhs)),
						MathsGroup.BRACKETS_ROUND, 1);

		if (!assumptions.isEmpty()) {
			//insert assumptions
			result = getProverImplication(result, assumptions, true);
		} else {
			result = new IsabelleParser().changeBrackets(MathsGroup.BRACKETS_NONE, (IsabelleOperatorGroup) result);
		}
		return result;
	}


	private static MathsExpression ensureHasBrackets(MathsExpression proverParsed) {
		if (proverParsed.isToken()) return proverParsed;
		if (((MathsGroup)proverParsed).getOuterBrackets()[0].length()>0) return proverParsed;
		proverParsed = new IsabelleParser().changeBrackets(MathsGroup.BRACKETS_ROUND, (IsabelleOperatorGroup) proverParsed);
		return proverParsed;
	}

	public static MathsExpression getProverConjunction(MathsExpression ...conjuncts) {
		return new IsabelleParser().newConjunction(MathsGroup.BRACKETS_ROUND, conjuncts);		
	}

	public static MathsExpression getProverParsedWithQuantifications(
			VariableBinding[] variablesAndBindings, MathsExpression base, boolean stripInitialUniversal, boolean skipImplicit) {
		int firstNonUniversal = 0;
		if (stripInitialUniversal)
			while (firstNonUniversal < variablesAndBindings.length && variablesAndBindings[firstNonUniversal].bindingType.equals(BindingType.ALL))
				firstNonUniversal++;
		for (int i=variablesAndBindings.length-1; i>=firstNonUniversal; i--) {			
			Set<String> quantWords = IsabelleTranslator.ISABELLE_VARIABLE_BINDING_TRANSLATIONS.getKeysForValue(variablesAndBindings[i].bindingType);
			if (quantWords.isEmpty()) {
				if (!BindingType.FREE.equals(variablesAndBindings[i].bindingType))
					System.err.println("skipping unknown binding "+variablesAndBindings[i]);
			} else if (skipImplicit && variablesAndBindings[i].isImplicit) {
				//don't add quant group if implicit
			} else {
				base = new QuantificationOperatorGroup(quantWords.iterator().next(),
						MathsExpressions.makeList( MathsExpressions.newToken(variablesAndBindings[i].varName, false) ),
						base, MathsGroup.BRACKETS_NONE);
			}
		}
		return base;
	}

	public static MathsExpression getProverImplication(MathsExpression concl,
			List<MathsExpression> assms, boolean preferMeta) {
		if (assms.isEmpty()) return concl;
		concl = new IsabelleImplicationGroup(
				MathsExpressions.makeList(
						(assms.size()>1 ? 
								(preferMeta ? new IsabelleAssumptionsGroup(assms) : 
									getProverConjunction(assms.toArray(new MathsExpression[0]))) : 
										ensureHasBrackets(
												clone(assms.get(0), false)) ),
												MathsExpressions.newToken(preferMeta ? "==>" : "-->", true),
												concl),
												MathsGroup.BRACKETS_NONE, 1);
		return concl;
	}

	private static MathsExpression clone(MathsExpression mathsExpression, boolean deep) {
		return new IsabelleParser().clone(mathsExpression, deep);
	}

	public static MathsExpression stripQuantifications(MathsExpression goal, boolean deep) {
		return new IsabelleParser().stripQuantifications(goal, deep);
	}



}
