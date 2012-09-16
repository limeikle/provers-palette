package org.cognetics.proverspalette.qepcad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cognetics.proverspalette.qepcad.QepcadParser.QepcadAbstractGroup;
import org.cognetics.proverspalette.qepcad.QepcadParser.QepcadBinding;
import org.cognetics.proverspalette.qepcad.QepcadParser.QepcadOperatorExpression;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.cognetics.proverspalette.translation.isabelle.IsabelleTranslator;
import org.heneveld.isabelle.IsabelleAssumptionsGroup;
import org.heneveld.isabelle.IsabelleImplicationGroup;
import org.heneveld.isabelle.IsabelleOperatorGroup;
import org.heneveld.isabelle.IsabelleParser;
import org.heneveld.isabelle.QuantificationOperatorGroup;
import org.heneveld.javautils.TwoWayMap;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressionVisitor;
import org.heneveld.maths.structs.MathsExpressions;
import org.heneveld.maths.structs.MathsGroup;
import org.heneveld.maths.structs.MathsOperatorGroup;
import org.heneveld.maths.structs.MathsToken;

public class QepcadTranslator implements MathsSystemTranslator {

	public BindingType getBindingTypeFromQuantificationWord(String quantificationWord) {
		//qepcad's quantification word are the same as that used by BindingType
		return BindingType.getBindingForQuantifierSimpleLabel(quantificationWord);
	}

	public String[] getLogicalGroupingChars() {
		return MathsGroup.BRACKETS_SQUARE;
	}

	//only used for importing, no longer on system interface
//	public List<MathsExpression> getConjuncts(MathsExpression systemExp) {
//		//untested...
//		if (!(systemExp instanceof QepcadOperatorExpression))
//			return Collections.singletonList(systemExp);
//		QepcadOperatorExpression opg = (QepcadOperatorExpression)systemExp;
//		if (!opg.getOperator().isToken() || opg.size(false)!=3 || opg.getIndexOfOperator()!=1)
//			return Collections.singletonList(systemExp);
//		if ( !"/\\".equals(((MathsToken)opg.getOperator()).getToken()) )
//			return Collections.singletonList(systemExp);
//		List<MathsExpression> result = new ArrayList<MathsExpression>();
//		result.addAll(getConjuncts(opg.get(0)));
//		result.addAll(getConjuncts(opg.get(2)));
//		return result;
//	}
	
	public String getQuantificationText(VariableBinding vb) {
		//qepcad's quantifier text is the same as that used by the var binding
		if (vb==null) return null;
		if (vb.bindingType==null || vb.bindingType.getSimpleLabel()==null)
			return null;
		if (vb.bindingType.getSimpleLabel().length()==0)
			return "";
		return vb.bindingType.getSimpleLabel()+" "+vb.varName;
	}

	public String getSystemName() {
		return "QEPCAD";
	}

	public Set<MathsToken> getUnknownPredicates(MathsExpression systemExpression) {
		Set<MathsToken> unknowns = new LinkedHashSet<MathsToken>();
		collectUnknownPredicates(systemExpression, unknowns);
		return unknowns;
	}

	static TwoWayMap<String,String> ISABELLE_QEPCAD_TRANSLATIONS = new TwoWayMap<String,String>();
	static {
		ISABELLE_QEPCAD_TRANSLATIONS.put("+", "+");
		ISABELLE_QEPCAD_TRANSLATIONS.put("-", "-");
		ISABELLE_QEPCAD_TRANSLATIONS.put("*", " ");
		ISABELLE_QEPCAD_TRANSLATIONS.put("^", "^");

		ISABELLE_QEPCAD_TRANSLATIONS.put("=", "=");
		
		//PG usd to think means \<simeq>, but should be default for /=
		ISABELLE_QEPCAD_TRANSLATIONS.put("~=", "/=");
		ISABELLE_QEPCAD_TRANSLATIONS.put("=/", "/=");
		//this would be natural.. but isn't in Isabelle
//		ISABELLE_QEPCAD_TRANSLATIONS.put("!=", "/=");
		
		ISABELLE_QEPCAD_TRANSLATIONS.put(">", ">");
		ISABELLE_QEPCAD_TRANSLATIONS.put("<", "<");
		ISABELLE_QEPCAD_TRANSLATIONS.put("<=", "<=");
		ISABELLE_QEPCAD_TRANSLATIONS.put(">=", ">=");
		ISABELLE_QEPCAD_TRANSLATIONS.put("~", "~");
		
		ISABELLE_QEPCAD_TRANSLATIONS.put("&", "/\\");
		ISABELLE_QEPCAD_TRANSLATIONS.put("|", "\\/");
		ISABELLE_QEPCAD_TRANSLATIONS.put("/\\", "/\\");
		ISABELLE_QEPCAD_TRANSLATIONS.put("\\/", "\\/");
		
		ISABELLE_QEPCAD_TRANSLATIONS.put("-->", "==>");
		ISABELLE_QEPCAD_TRANSLATIONS.put("<--", "<==");
		ISABELLE_QEPCAD_TRANSLATIONS.put("<->", "<==>");
		
		ISABELLE_QEPCAD_TRANSLATIONS.put("==>", "==>");
		ISABELLE_QEPCAD_TRANSLATIONS.put("<==", "<==");
		
		ISABELLE_QEPCAD_TRANSLATIONS.put("True", "TRUE");
		ISABELLE_QEPCAD_TRANSLATIONS.put("False", "FALSE");
	}

	private void collectUnknownPredicates(MathsExpression exp, Set<MathsToken> unknowns) {
		if (exp instanceof MathsToken) {
			if (ISABELLE_QEPCAD_TRANSLATIONS.containsValue( ((MathsToken)exp).getToken() )) return;
			if (((MathsToken)exp).getToken().trim().length()==0)
				//multiplication
				return;
			if (exp.isOperator()) unknowns.add((MathsToken) exp);
			//not an operator, assume is a variable
			return;
		}
		if (exp instanceof QepcadBinding) {
			collectUnknownPredicates(((QepcadBinding)exp).getUnderlyingExpression(), unknowns);
			return;
		}
		if (exp instanceof MathsOperatorGroup) {
			MathsExpression op = ((MathsOperatorGroup)exp).getOperator();
			if ((op instanceof MathsToken) && !((MathsToken)op).isOperator()) {
				collectUnknownPredicates( MathsExpressions.newToken( ((MathsToken)op).getToken(), true), unknowns );				
			}
			for (MathsExpression ei : ((MathsOperatorGroup)exp).getList()) {
				collectUnknownPredicates(ei, unknowns);
			}
		}
	}

	public Map<String, VariableBinding> getVariables(MathsExpression systemExpression) {
		Map<String, VariableBinding> result = new LinkedHashMap<String, VariableBinding>();
		MathsExpression tree = systemExpression;
		while (tree instanceof QepcadBinding) {
			QepcadBinding qb = (QepcadBinding)tree;
			String qw = qb.getQuantificationWord();
			VariableBinding.BindingType bindingType = getBindingTypeFromQuantificationWord(qw);
			if (bindingType==null) {
				//FIXME complain to listner
				System.err.println("skipping unknown binding "+qw+" for "+qb.getQuantificationWord());
			} else {			
				result.put(qb.getQuantifiedVar(), new VariableBinding(qb.getQuantifiedVar(), bindingType, false));
			}
			tree = qb.getLast();
		}
		return result;
	}

	public MathsExpression parse(String systemText) {
		return new QepcadParser().parse(systemText);
	}

	public MathsExpression stripQuantifications(MathsExpression systemExpression, boolean deep) {
		return new QepcadParser().stripQuantifications(systemExpression, deep);
	}

	/** precedingTerm is used to provide some context for 'term' (FIXME this isn't the nicest way, but it works for equality which is the case needed here) */
	public MathsExpression toCommon(MathsExpression systemTerm, MathsExpression optionalTranslatedPrecedingTerm) {
//		IsabelleParser parser = new IsabelleParser();
		if (systemTerm instanceof MathsGroup) {
			if (systemTerm instanceof QepcadBinding) {
				return new QuantificationOperatorGroup( ((QepcadBinding)systemTerm).getQuantificationWord(), 
						Collections.singletonList( MathsExpressions.newToken( ((QepcadBinding)systemTerm).getQuantifiedVar(), false ) ),
						toCommon(((QepcadBinding)systemTerm).getUnderlyingExpression(), null), MathsGroup.BRACKETS_ROUND);
			}
			
			List<MathsExpression> pTerms = new ArrayList<MathsExpression>();
			int i=0;
			int indexOfOperator = -1;
			MathsExpression previous = null;
			MathsExpression previousTranslated = null;
			for (MathsExpression pt : ((MathsGroup)systemTerm).getList()) {
				if (pt.isOperator()) {
					if (indexOfOperator<0) indexOfOperator=i;
					else {
						//FIXME listen
						System.err.println(this+" no operator in term "+systemTerm);						
					}
					if (previous!=null && previous.isOperator()) {
						//two operators in a row... unusual.
						boolean handled = false;
						if ("-".equals(pt.toString())) {
							if ("+".equals(previous.toString())) {
								pTerms.remove(pTerms.size()-1);
								handled = true;
							}
						}
						if (!handled) {
							//FIXME listen
							System.err.println(this+" skipping invalid operator combination "+previous+" "+pt);							
						}
					}
				}
				previousTranslated = toCommon(pt, previousTranslated);
				pTerms.add(previousTranslated);
				previous = pt;
				i++;
			}
			if (indexOfOperator<0) {
				//FIXME listen
				System.err.println(this+" no operator in term "+systemTerm);						
				indexOfOperator = 0;
			}
			IsabelleOperatorGroup result;
			if ((systemTerm instanceof MathsOperatorGroup) && ((MathsOperatorGroup)systemTerm).getOperator().toString().equals("-->"))
				result = new IsabelleImplicationGroup(pTerms, MathsGroup.BRACKETS_ROUND, indexOfOperator);
			else result = new IsabelleOperatorGroup(pTerms, MathsGroup.BRACKETS_ROUND, indexOfOperator);
			return result;
		}
		
		if (systemTerm instanceof MathsToken) {			
			if (systemTerm.equals(QepcadParser.MULTIPLICATION_TERM)) {
				return MathsExpressions.newToken("*", true);
			}
			String word = systemTerm.toString();
			Set<String> pWords = ISABELLE_QEPCAD_TRANSLATIONS.getKeysForValue(word);
			if (pWords==null || pWords.isEmpty()) {
				if (systemTerm.isOperator()) {
					//FIXME listen
					System.err.println(this+" unknown operator '"+systemTerm+"'");
				}
				return MathsExpressions.newToken(word, systemTerm.isOperator());
			}
			//here, the first item in the list will always be understood by isabelle
			return MathsExpressions.newToken(pWords.iterator().next(), true);
		}		
		throw new IllegalStateException("unknown term type "+systemTerm.getClass()+": "+systemTerm);
	}

	public MathsExpression toSystem(MathsExpression term, boolean isQuantificationAllowed, MathsExpression optionalTranslatedPrecedingTerm) {
		if (term instanceof MathsGroup) {
			if (term instanceof QuantificationOperatorGroup) {
				MathsExpression result = toSystem( ((QuantificationOperatorGroup)term).getUnderlyingExpression(), isQuantificationAllowed, null);
				if (!isQuantificationAllowed) 
					return result;
				BindingType bt = IsabelleTranslator.ISABELLE_VARIABLE_BINDING_TRANSLATIONS.get( ((QuantificationOperatorGroup)term).getQuantificationWord() );
				if (bt==null) {
					//TODO warn to listener
					System.err.println("skipping unknown binding '"+((QuantificationOperatorGroup)term).getQuantificationWord()+"'");
					return result;
				}
				if (!(result instanceof QepcadAbstractGroup)) {
					//TODO warn to listener
					System.err.println("underlying expression for quantification is not qepcad proposition: "+result);
					return result;					
				}
				for (MathsToken t : ((QuantificationOperatorGroup)term).getQuantifiedVars()) {
					result = new QepcadBinding(bt.getSimpleLabel(), t.toString(), (QepcadAbstractGroup) result);
				}
				return result;
			}
			if (term instanceof IsabelleOperatorGroup && ((IsabelleOperatorGroup)term).getOperator().toString().equals(IsabelleParser.COLON_TYPE_OPERATOR)) {
				return toSystem( ((IsabelleOperatorGroup)term).get(0), isQuantificationAllowed, null);				
			}
			if (term instanceof IsabelleAssumptionsGroup) {
				MathsExpression terms[] = new MathsExpression[ ((MathsGroup)term).size(false) ];
				for (int i=0; i<((MathsGroup)term).size(false); i++) {
					terms[i] = toSystem( ((MathsGroup)term).get(i), isQuantificationAllowed, null);
				}
				return new QepcadParser().newConjunction(MathsGroup.BRACKETS_SQUARE, terms);
			}
			List<MathsExpression> qTerms = new ArrayList<MathsExpression>();
			int i=0;
			int indexOfOperator = -1;
			MathsExpression precedingTermTranslated = null;
			for (MathsExpression pt : ((MathsGroup)term).getList()) {
				precedingTermTranslated = toSystem(pt, isQuantificationAllowed, precedingTermTranslated);
				qTerms.add(precedingTermTranslated);
				if (pt.isOperator()) {
					if (indexOfOperator<0) indexOfOperator=i;
					else {
						//FIXME listen
						System.err.println(this+" no operator in term "+term);						
					}					
				}
				i++;
			}
			if (indexOfOperator<0) {
				//FIXME listen
				System.err.println(this+" no operator in term "+term);						
				indexOfOperator = 0;
			}
			QepcadOperatorExpression result = new QepcadOperatorExpression(qTerms, indexOfOperator);
			return result;
		}
		if (term instanceof MathsToken) {			
			String word = term.toString();
			String qWord = ISABELLE_QEPCAD_TRANSLATIONS.get(word);
			if (qWord==null) {
				if (term.isOperator()) {
					//FIXME listen
					System.err.println(this+" unknown operator '"+term+"'");
				}				
				return MathsExpressions.newToken(word, term.isOperator());
			}
			//special cases:
			if (word.equals("*")) {
				return QepcadParser.MULTIPLICATION_TERM;
			}
			if (word.equals("=")) {
				//tokens and numeric types stay =, but propositions need <==>
				if (	(optionalTranslatedPrecedingTerm instanceof QepcadBinding)
						||
						((optionalTranslatedPrecedingTerm instanceof QepcadOperatorExpression) &&
						!((QepcadOperatorExpression)optionalTranslatedPrecedingTerm).isNumericType())
					) {
					return MathsExpressions.newToken("<==>", true);
				}
			}

			return MathsExpressions.newToken(qWord, term.isOperator());
		}		
		throw new IllegalStateException("unknown term type "+term.getClass()+": "+term);
	}

	public MathsExpression toCommon(MathsExpression systemExpression) {
		return toCommon(systemExpression, null);
	}

	public MathsExpression fromCommon(MathsExpression commonLanguageExpression) {
		return toSystem(commonLanguageExpression, true, null);
	}

	public String toText(MathsExpression systemSubgoal) {
		return systemSubgoal.toString();
	}

	public boolean isProverExpressionCompatible(
			MathsExpression proverExpression,
			MathsProverTranslator proverTranslator) {
		if (proverTranslator==null || proverExpression==null) return false;
		Set<MathsToken> unknowns = getUnknownPredicates( fromCommon( proverTranslator.toCommon( proverExpression ) ) );
		if (!unknowns.isEmpty()) return false;
		
		if (proverTranslator.shouldSuggestConvertToPnf(proverExpression)) return false;
		
		Map<String, VariableBinding> vars = proverTranslator.getVariables(proverExpression);
		for (VariableBinding vb: vars.values())
			if (vb.bindingType != BindingType.ALL && vb.bindingType != BindingType.EXISTS)
				//contains unknown var types
				return false;
		
		MathsExpression sysExp = toSystem(proverTranslator.toCommon(proverExpression), true, null);
		if (!isValidForQepcad(sysExp)) return false;
		
		return true;
	}
	
	public boolean isValidForQepcad(MathsExpression qepcadExpression) {
		final boolean[] invalid = new boolean[] { false };
		qepcadExpression.apply(new MathsExpressionVisitor() {
			public boolean visitGroup(MathsGroup expression) {
				if (!expression.isOperatorGroup()) return true;
				MathsExpression op = ((MathsOperatorGroup)expression).getOperator();
				if (!op.isToken()) {
					invalid[0] = true;
					return true;
				}
				if ( ((MathsToken)op).getToken().equals("^") ) {
					//is power: is RHS a non-neg integer token?
					if (expression.getList().size()!=3) {
						//invalid if not exactly 3 args
						invalid[0] = true;
						return true;
					}
					if (!expression.getLast().isToken()) {
						//invalid if last isn't a token
						invalid[0] = true;
						return true;
					}
					String lastWord = ((MathsToken)expression.getLast()).getToken();
					if (!lastWord.matches("[0-9]+")) {
						//invalid if last isn't a non-neg integer
						invalid[0] = true;
						return true;						
					}					
				}
				return true;
			}
			public void visitToken(MathsToken operator) {
			}			
		});
		return !invalid[0];
	}

	public MathsExpression tidyBrackets(MathsExpression systemExpression,
			Boolean putWhereNice, Boolean dontPutWhereAssociative,
			Boolean dontPutOutermost) {		
		return new QepcadParser().tidyBrackets(systemExpression, 
				putWhereNice, dontPutWhereAssociative, dontPutOutermost);
	}

	public void ensureVariableNamesAreSafe(Map<String, VariableBinding> varsOrig) {
		Map<String,VariableBinding> varsResult = new LinkedHashMap<String,VariableBinding>();
		for (String var : varsOrig.keySet()){
			VariableBinding vb = varsOrig.get(var);
			if (var.startsWith("?")) {
				var = var.substring(1);
				vb = new VariableBinding(var, VariableBinding.BindingType.EXISTS, true);
			}
			if (varsResult.get(var)!=null) {
				//conflict
				vb = new VariableBinding(var, VariableBinding.BindingType.UNKNOWN, false);
			} //otherwise just use orig
			//add this to the result list
			varsResult.put(var, vb);
		}
		varsOrig.clear();
		varsOrig.putAll(varsResult);
	}

	public Object ensureVariableNamesAreSafe(MathsExpression problemSystem) {
		return problemSystem.toString().replaceAll("\\?", "");
	}
}
