package org.cognetics.proverspalette.maple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cognetics.proverspalette.maple.MapleParser.MapleOperatorExpression;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
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
import org.heneveld.maths.structs.MathsOperatorToken;
import org.heneveld.maths.structs.MathsToken;

public class MapleTranslator implements MathsSystemTranslator {

	public BindingType getBindingTypeFromQuantificationWord(String quantificationWord) {
		return null;
	}

	public String[] getLogicalGroupingChars() {
		return MathsGroup.BRACKETS_ROUND;
	}

	public String getQuantificationText(VariableBinding vb) {
		return null;
	}

	public String getSystemName() {
		return "Maple";
	}

	private static final String IMPLIES = "implies";
	private static final String NOT = " not";
	private static final String OR = "or";
	private static final String AND = "and";

	private static final String FALSE = "false";
	private static final String TRUE = "true";

	public static boolean isWordOperator(String x) {
		String xl = x.toLowerCase().trim();
		if (xl.equals(AND)) return true;
		if (xl.equals(OR)) return true;
		if (xl.equals(NOT.trim())) return true;
		if (xl.equals(IMPLIES)) return true;
		return false;
	}
	
	public Set<MathsToken> getUnknownPredicates(MathsExpression systemExpression) {
		Set<MathsToken> unknowns = new LinkedHashSet<MathsToken>();
		collectUnknownPredicates(systemExpression, unknowns);
		return unknowns;
	}

	static TwoWayMap<String,String> ISABELLE_MAPLE_TRANSLATIONS = new TwoWayMap<String,String>();
	static {
		ISABELLE_MAPLE_TRANSLATIONS.put("+", "+");
		ISABELLE_MAPLE_TRANSLATIONS.put("-", "-");
		ISABELLE_MAPLE_TRANSLATIONS.put("*", "*");
		ISABELLE_MAPLE_TRANSLATIONS.put("/", "/");
		ISABELLE_MAPLE_TRANSLATIONS.put("^", "^");
		ISABELLE_MAPLE_TRANSLATIONS.put("sqrt", "sqrt");

		ISABELLE_MAPLE_TRANSLATIONS.put("=", "=");
		
		ISABELLE_MAPLE_TRANSLATIONS.put("=/", "<>");
		//PG used to use this to mean \<simeq> but isabelle usu is noteq
		ISABELLE_MAPLE_TRANSLATIONS.put("~=", "<>");
		
		ISABELLE_MAPLE_TRANSLATIONS.put(">", ">");
		ISABELLE_MAPLE_TRANSLATIONS.put("<", "<");
		ISABELLE_MAPLE_TRANSLATIONS.put("<=", "<=");
		ISABELLE_MAPLE_TRANSLATIONS.put(">=", ">=");

		ISABELLE_MAPLE_TRANSLATIONS.put("&", AND);
		ISABELLE_MAPLE_TRANSLATIONS.put("|", OR);
		ISABELLE_MAPLE_TRANSLATIONS.put("/\\", AND);
		ISABELLE_MAPLE_TRANSLATIONS.put("\\/", OR);
		ISABELLE_MAPLE_TRANSLATIONS.put("~", NOT);

		ISABELLE_MAPLE_TRANSLATIONS.put("-->", IMPLIES);
		//ISABELLE_MAPLE_TRANSLATIONS.put("<->", "iff");

		ISABELLE_MAPLE_TRANSLATIONS.put("==>", IMPLIES);

		ISABELLE_MAPLE_TRANSLATIONS.put("True", TRUE);
		ISABELLE_MAPLE_TRANSLATIONS.put("False", FALSE);
	}

	private void collectUnknownPredicates(MathsExpression exp, Set<MathsToken> unknowns) {
		if (exp instanceof MathsToken) {
			if (ISABELLE_MAPLE_TRANSLATIONS.containsValue( ((MathsToken)exp).getToken() )) return;
			if (exp.isOperator()) unknowns.add((MathsToken) exp);
			//not an operator, assume is a variable
			return;
		}
		if (exp instanceof MathsOperatorGroup) {
			MathsExpression op = ((MathsOperatorGroup)exp).getOperator();
			if ((op instanceof MathsToken) && !((MathsToken)op).isOperator()) {
				//op is not a known operator
				//TODO allow user to say what operator/mappings are used
				//for now we will say things like "sin" and "integral" aren't allowed
				collectUnknownPredicates( MathsExpressions.newToken( ((MathsToken)op).getToken(), true), unknowns );				
			}
			for (MathsExpression ei : ((MathsOperatorGroup)exp).getList()) {
				collectUnknownPredicates(ei, unknowns);
			}
		}
	}

	public Map<String, VariableBinding> getVariables(MathsExpression systemExpression) {
		Map<String, VariableBinding> result = new LinkedHashMap<String, VariableBinding>();
		collectVariables(systemExpression, result);
		//TODO - variables are not bound in Maple - do we need to collect variables from Maple result? 
		//We do need to know all the variables in the result so that we can attach the correct types
		//to them when putting the result into Isabelle, but can we take the variables from the Import Tab?
		return result;
	}

	private void collectVariables(MathsExpression systemExpression, Map<String, VariableBinding> result) {
		if (systemExpression instanceof MathsToken) {
			if (ISABELLE_MAPLE_TRANSLATIONS.containsValue( ((MathsToken)systemExpression).getToken() )) return;
			if (systemExpression.isOperator()) return;
			//it is a token which isn't an operator, treat as variable
			String vname = ((MathsToken)systemExpression).getToken();
			if (vname.matches(".*[A-Za-z].*")) {
				result.put(vname, new VariableBinding(vname, BindingType.FREE, true));
				return;
			} else {
				//no letter, probably not a variable, ignore
				return;
			}
		}
		if (systemExpression instanceof MathsOperatorGroup) {
			MathsExpression op = ((MathsOperatorGroup)systemExpression).getOperator();
			for (MathsExpression ei : ((MathsOperatorGroup)systemExpression).getList()) {
				if (ei==op) {
					if (op instanceof MathsToken) {
						//do nothing
					} else {
						//recurse through the operator term in this operator group because it is something complicated
						//(probably not used here)
						collectVariables(ei, result);
					}
				} else {
					collectVariables(ei, result);
				}
			}
		}
	}

	public MathsExpression parse(String systemText) {
		return new MapleParser().parse(systemText);
	}

	public MathsExpression stripQuantifications(MathsExpression systemExpression, boolean deep) {
		return new MapleParser().stripQuantifications(systemExpression, deep);
	}

	/** precedingTerm is used to provide some context for 'term' (FIXME this isn't the nicest way, but it works for equality which is the case needed here) */
	public MathsExpression toCommon(MathsExpression systemTerm, MathsExpression optionalTranslatedPrecedingTerm) {
		if (systemTerm instanceof MathsGroup) {

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
			String word = systemTerm.toString();
			Set<String> pWords = ISABELLE_MAPLE_TRANSLATIONS.getKeysForValue(word);
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
				//skip quantifications
				MathsExpression result = toSystem( ((QuantificationOperatorGroup)term).getUnderlyingExpression(), isQuantificationAllowed, null);
				if (!isQuantificationAllowed) 
					return result;
				//quantifications aren't relevant here, skip even if caller requested us not to
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
				return new MapleParser().newConjunction(MathsGroup.BRACKETS_ROUND, terms);
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
			MapleOperatorExpression result = new MapleOperatorExpression(qTerms, indexOfOperator);
			return result;
		}
		if (term instanceof MathsToken) {			
			String word = term.toString();
			String qWord = ISABELLE_MAPLE_TRANSLATIONS.get(word);
			if (qWord==null) {
				if (term.isOperator()) {
					//FIXME listen
					System.err.println(this+" unknown operator '"+term+"'");
				}				
				return MathsExpressions.newToken(word, term.isOperator());
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
		for (VariableBinding vb: vars.values()) {
			if (vb.bindingType == BindingType.UNKNOWN)
				//means invalid/ambiguous
				return false;
			//for now we'll allow anything else (maple is just going to treat as free)			
		}

		MathsExpression sysExp = toSystem(proverTranslator.toCommon(proverExpression), true, null);
		if (!isValidForMaple(sysExp)) return false;

		return true;
	}

	public boolean isValidForMaple(MathsExpression mapleExpression) {
		if (mapleExpression==null) return false;
		final boolean[] invalid = new boolean[] { false };
		mapleExpression.apply(new MathsExpressionVisitor() {
			public boolean visitGroup(MathsGroup expression) {
				if (!expression.isOperatorGroup()) return true;
				MathsExpression op = ((MathsOperatorGroup)expression).getOperator();
				if (!op.isToken()) {
					invalid[0] = true;
					return true;
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
		return new MapleParser().tidyBrackets(systemExpression, 
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

	/** splits all expressions which are not equations/inequalities (e.g. "x>1 ==> x>0" split into "x>1" and "x>0") */
	public static void decompose(Collection<MathsExpression> result) {
		boolean didSomething = true;
		while (didSomething) {
			Iterator<MathsExpression> ri = result.iterator();
			didSomething = false;
			Set<MathsExpression> newResults = new LinkedHashSet<MathsExpression>();
			while (ri.hasNext()) {
				MathsExpression r = ri.next();
				didSomething |= decompose(r, newResults);
				ri.remove();
			}
			result.addAll(newResults);
		}
	}
	private static boolean decompose(MathsExpression eqn, Set<MathsExpression> results) {
		if (eqn instanceof MathsOperatorGroup) {
			MathsExpression op = ((MathsOperatorGroup)eqn).getOperator();
			if (op instanceof MathsOperatorToken) {
				if (MapleTranslator.isWordOperator( ((MathsOperatorToken)op).getToken()) ) {
					for (MathsExpression e : ((MathsOperatorGroup)eqn).getList()) {
						if (!((MathsOperatorGroup)eqn).getOperator().equals(e))
							results.add(convertInequalitiesToEquality(e));
					}
					return true;
				}
			}
		}
		results.add(eqn);
		return false;
	}
	
	public static boolean isDecomposable(MathsExpression eqn) {
		if (!(eqn instanceof MathsOperatorGroup)) return false;
		MathsExpression op = ((MathsOperatorGroup)eqn).getOperator();
		if (!(op instanceof MathsOperatorToken))
			//maybe this should be true... to allow decomposing where operator is e.g. lambda expression
			return false;
		if (!(MapleTranslator.isWordOperator( ((MathsOperatorToken)op).getToken()) ))
			return false;
		return true;
	}

	/** replaces inequalities by equality */
	public static MathsExpression convertInequalitiesToEquality(MathsExpression mapleExpression) {
		if (mapleExpression instanceof MapleOperatorExpression) {
			List<MathsExpression> terms = new ArrayList<MathsExpression>(((MapleOperatorExpression)mapleExpression).getList());
			int i = ((MapleOperatorExpression)mapleExpression).getIndexOfOperator();
			String mop = terms.get(i).toString();
			//replace inequalities with equalities for plotting
			if ("<".equals(mop) || ">".equals(mop) || "<>".equals(mop) || "<=".equals(mop) || "<=".equals(mop)){
				terms.set(i, MathsExpressions.newToken("=", true));
				mapleExpression = new MapleOperatorExpression(terms, i);
			}			
		}
		
		return mapleExpression;
	}

}
