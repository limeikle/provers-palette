package org.cognetics.proverspalette.translation;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsToken;

public interface MathsProverTranslator extends MathsSystemTranslator {

	/** converts the prover text to an equivalent prover text but which
	 * is nicer for display */
	String preprocess(String proverText);

	/** if the argument is a conjunction then it returns a list of all conjuncts
	 * (recursively in order);
	 * otherwise returns a singleton list containing just the expression
	 */
	public List<MathsExpression> getConjuncts(MathsExpression systemExp);
		
	boolean shouldSuggestConvertToPnf(MathsExpression proverSubgoalText);	
	boolean shouldSuggestExpandUnknownPredicates(
			MathsExpression proverSubgoalText, Set<MathsToken> unknownPredicates);

	String getCommandForConvertingToPnf(String text);
	String getCommandForExpandingPredicates(Collection<String> unknownPredicates);
	
	String annotateWithTypeInformation(String result, VariableBinding[] variablesAndBindings);

}
