package org.cognetics.proverspalette.translation.isabelle;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.heneveld.isabelle.IsabelleImplicationGroup;
import org.heneveld.isabelle.IsabelleOperatorGroup;
import org.heneveld.isabelle.IsabelleParser;
import org.heneveld.isabelle.QuantificationOperatorGroup;
import org.heneveld.javautils.TwoWayMap;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsGroup;
import org.heneveld.maths.structs.MathsToken;

import ed.inf.proofgeneral.ProofGeneralPlugin;

public class IsabelleTranslator implements MathsProverTranslator {

	//FIXME confirm 
	public final static String ISABELLE_VARIABLE_REGEX = "[A-Za-z][A-Za-z_0-9]*";

	public String getCommandForConvertingToPnf(String text) {
		StringBuffer command = new StringBuffer();
		
		if(IsabellePnfChecker.inMeta(text)){
		     command.append(IsabellePnfChecker.metaConvertIsabelleToPNF(text));
		}
		else command.append(IsabellePnfChecker.objectConvertIsabelleToPNF(text));
		
		return command.toString();
	}

	public String getCommandForExpandingPredicates( Collection<String> unknownPredicates ) {
		StringBuffer command = new StringBuffer();		
		command.append("apply (simp only:");

		for (String u1 : unknownPredicates){
			if (isTokenExpandable(u1))
				command.append(" "+u1+"_def");
		}	    

		command.append(")\n");	 
		return command.toString();
	}

	public boolean shouldSuggestConvertToPnf(MathsExpression proverSubgoalText) {
		return !isPNF(proverSubgoalText);
	}
	
	public boolean isPNF(MathsExpression parsedInput) {
		parsedInput = stripQuantifications(parsedInput, false);
		return !containsQuantificationAnywhere(parsedInput);		
	}

	public boolean containsQuantificationAnywhere(MathsExpression expression) {
		if (expression instanceof MathsToken) return false;
		if (expression instanceof QuantificationOperatorGroup) return true;
		//other type of list... recurse through its contents
		for (MathsExpression arg : ((MathsGroup)expression).getList()) {
			if (containsQuantificationAnywhere(arg)) return true;
		}
		return false;
	}

	/** operation symbols which cannot be expanded, even if the system complains about them */
	// TODO we could have special rule for expanding ^ when RHS is contstant
	// (or / when RHS is 1... but that should be up to simp or other technique)
	public static final Set<String> KNOWN_NOT_EXPANDABLE_SYMBOLS =
		Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
				"/", "^"
		})));

	private boolean isTokenExpandable(String token) {
		//(only called for unknown items)
		if (KNOWN_NOT_EXPANDABLE_SYMBOLS.contains(token))
			//division and power, can't be expanded, look at the next token 
			return false;
		//it's a predicate or implicit existential var
		if (token.matches("\\?"+ISABELLE_VARIABLE_REGEX))
			//it looks like an existential var, so can't be expanded
			//TODO better would be to have the information from parsing the PGIP goal here
			//(in case there is a predicate which starts with "?"
			return false;
		return true;
	}
	public boolean shouldSuggestExpandUnknownPredicates(
			MathsExpression proverSubgoal, Set<MathsToken> unknowns) {
		if (unknowns.isEmpty()) return false;
//		Map<String, VariableBinding> vars = getVariables(proverSubgoalText);
		for (MathsToken u : unknowns) {
			if (!isTokenExpandable(u.getToken()))
				continue;
			//it looks like a predicate, so suggest expanding
			return true;
		}
		return false;
	}

	public static TwoWayMap<String,VariableBinding.BindingType> ISABELLE_VARIABLE_BINDING_TRANSLATIONS = new TwoWayMap<String,VariableBinding.BindingType>();
	static {
		ISABELLE_VARIABLE_BINDING_TRANSLATIONS.put("ALL", VariableBinding.BindingType.ALL);
		ISABELLE_VARIABLE_BINDING_TRANSLATIONS.put("\\<forall>", VariableBinding.BindingType.ALL);
		ISABELLE_VARIABLE_BINDING_TRANSLATIONS.put("EX", VariableBinding.BindingType.EXISTS);
		ISABELLE_VARIABLE_BINDING_TRANSLATIONS.put("\\<exists>", VariableBinding.BindingType.EXISTS);
	}

	static Set<String> ISABELLE_RESERVED_WORDS = new LinkedHashSet<String>();
	static {
		ISABELLE_RESERVED_WORDS.add("True");
		ISABELLE_RESERVED_WORDS.add("False");
	}

	public BindingType getBindingTypeFromQuantificationWord(String quantificationWord) {
		return ISABELLE_VARIABLE_BINDING_TRANSLATIONS.get(quantificationWord);
	}

	public String[] getLogicalGroupingChars() {
		return MathsGroup.BRACKETS_ROUND;
	}

	public String getQuantificationText(VariableBinding vb) {
		Set<String> isabelleWords = ISABELLE_VARIABLE_BINDING_TRANSLATIONS.getKeysForValue(vb.bindingType);
		if (isabelleWords==null || isabelleWords.isEmpty()) {
			System.err.println("unable to convert binding "+vb+" to isabelle; skipping");
		}
		return isabelleWords.iterator().next()+" "+vb.varName;
	}

	public String getSystemName() {
		return "Isabelle";
	}

	public Set<MathsToken> getUnknownPredicates(MathsExpression proverText) {
		return Collections.emptySet();
	}

	public String preprocess(String proverSubgoalText) {
		//remove x-symbol, prefer ASCII shortcuts
		try {			
			proverSubgoalText = ProofGeneralPlugin.getSomeSessionManager().getProver().getSymbols().useShortcutsAscii(proverSubgoalText);

			//shortcuts aren't always valid input, e.g. /\ for and; but our parser accepts it, so it's okay
			
//			proverSubgoalText = ProofGeneralPlugin.getSomeSessionManager().getProver().getSymbols().
//				useUnicodeForOutput(proverSubgoalText, false);
		} catch (Exception e) {
			System.err.println("unable to get prover's symbols; X-Symbols will be left unconverted");
		}		

		//FIXME parser should do this
		proverSubgoalText = proverSubgoalText.replaceAll("\\bEX\\s*!", "EX! ");
		proverSubgoalText = proverSubgoalText.replaceAll("\\\\<twosuperior>", " ^ 2 ");
		
		//FIXME really the parser should do this but currently it doesn't understand "\<And>"
		if (proverSubgoalText.trim().startsWith("\\<And>")) {
			System.out.println("Stripping use of 'And' at beginning of Isabelle problem to indicate meta-level universal quantification: "+proverSubgoalText);
			while (proverSubgoalText.matches("\\s*\\\\<And>\\s*"+
					ISABELLE_VARIABLE_REGEX+"" +
					"(\\s+"+ISABELLE_VARIABLE_REGEX+")*\\s*\\..+")) {
				proverSubgoalText = proverSubgoalText.substring(proverSubgoalText.indexOf('.')+1).trim();
			}
			if (proverSubgoalText.trim().startsWith("\\<And>")) {
				System.out.println("Unable to strip use of 'And' at beginning of Isabelle problem to indicate meta-level universal quantification: "+proverSubgoalText);
			}
		}
		return proverSubgoalText;
	}

	public Map<String, VariableBinding> getVariables(MathsExpression systemExpression) {
		Map<String,VariableBinding> varsExplicit = new LinkedHashMap<String,VariableBinding>();

		Map<String, VariableBinding> varsExplicitOnWholeSubgoal = new LinkedHashMap<String,VariableBinding>(varsExplicit);
		Map<String, VariableBinding> varsImplicitExistential = new LinkedHashMap<String,VariableBinding>();
		Map<String, VariableBinding> varsImplicitUniversal = new LinkedHashMap<String,VariableBinding>();

		collectVariablesFromTermList(systemExpression,
				false,
				true,
				varsExplicitOnWholeSubgoal,
				varsExplicit, varsImplicitExistential, varsImplicitUniversal);

		Map<String,VariableBinding> varsResult = new LinkedHashMap<String,VariableBinding>();
		varsResult.putAll(varsImplicitExistential);
		varsResult.putAll(varsImplicitUniversal);
		varsResult.putAll(varsExplicit);

		//remove numbers (they shouldn't have been put in, by our parser, but maybe they snuck in somehow...)
		Iterator<Entry<String, VariableBinding>> vi = varsResult.entrySet().iterator();
		while (vi.hasNext()) {
			if (isNumber(vi.next().getKey())) vi.remove();
		}
		
		return varsResult;
	}
	
	public static boolean isNumber(String n) {
		for (int i=0; i<n.length(); i++) {
			if (!Character.isDigit(n.charAt(i))) return false;
		}
		return true;
	}

	private static boolean addBindingMaybeChange(VariableBinding b, Map<String,VariableBinding> map) {
		VariableBinding oldB = map.put(b.varName, b);
		if (oldB!=null) {
			//if both are ALL we can combine them! (since qepcad only handles numbers)
			// (ALL x. p x) /\ (ALL x. q x) = (ALL x. p x /\ q x)			
			if (oldB.bindingType.equals(BindingType.ALL) && b.bindingType.equals(BindingType.ALL))
				;
			else {
				//if either is not ALL then we don't know how to handle it
				//set it to be unknown
//				System.out.println("replace binding of "+b.varName+" to unknown");
				b.bindingType = VariableBinding.BindingType.UNKNOWN;
				return false;
			}
		} else {
//			System.out.println("put binding of "+b.varName+" as "+b.bindingType);
		}
		return true;
	}

	private void collectVariablesFromTermList(MathsExpression parsedTerm,
			boolean isInAssumption /** true means explicit existentials introduced here should be bound as universal and vice versa */,
			boolean isInConclusion /** true means universals of the same name can be combined */,
			Map<String, VariableBinding> varsExplicitInScope /** explicitly bound things which apply here */,
			Map<String, VariableBinding> varsExplicitGlobalList /** explicitly bound things in expression but which may not be bound here */, 
			Map<String, VariableBinding> varsImplicitExistential,
			Map<String, VariableBinding> varsImplicitUniversal) {
		
//		System.out.println("collecting from '"+parsedTerm+"', list lengths are "+
//				varsExplicitInScope.size()+", "+
//				varsExplicitGlobalList.size()+", "+
//				varsImplicitExistential.size()+", "+
//				varsImplicitUniversal.size());
		
		if (parsedTerm.isToken()) {			
			if (parsedTerm.isOperator()) return;
//			if (parsedTerm.toString().matches("[\\s0-9\\.]*"))
//				//something funny looking
//				return;
			String varName = parsedTerm.toString();			
			if (!parsedTerm.toString().trim().matches("\\??"+ISABELLE_VARIABLE_REGEX)) {
				if (parsedTerm.toString().trim().matches("[0-9]+"))
					//number
					return;				
				System.out.println("Skipping unrecognised non-operator and non-variable token: "+parsedTerm);
				//must have at least one letter in it
				return;
			}
			//if it is a reserved word, it's not a variable, just return
			if (ISABELLE_RESERVED_WORDS.contains(varName)) return;
			//it is a variable -- if it's already quantified, ignore it
			if (varsExplicitInScope.containsKey(varName)) return;
			if (varsImplicitExistential.containsKey(varName)) return;
			if (varsImplicitUniversal.containsKey(varName)) return;
			//not known yet -- it must be implicitly bound
			VariableBinding b;
			if (varName.startsWith("?")) {
				//existential
				b = new VariableBinding(varName, BindingType.EXISTS, true);
				addBindingMaybeChange(b, varsImplicitExistential);
			} else {
				//universal
				b = new VariableBinding(varName, BindingType.ALL, true);
				addBindingMaybeChange(b, varsImplicitUniversal);
			}
			return;
		}
		//it's a term list
		//is it a quantification?
		if (parsedTerm instanceof QuantificationOperatorGroup) {
			BindingType bt = getBindingTypeFromQuantificationWord( 
					((QuantificationOperatorGroup)parsedTerm).getQuantificationWord() );
			if (bt==null) bt = BindingType.UNKNOWN;
			
			for (int i=1; i<((MathsGroup)parsedTerm).getList().size()-1; i++) {
				VariableBinding existingBinding = null;
				String varName = ((MathsGroup)parsedTerm).getList().get(i).toString();
				existingBinding = varsExplicitGlobalList.get(varName);
				if (existingBinding==null)
					existingBinding = varsImplicitExistential.get(varName);
				if (existingBinding==null)
					existingBinding = varsImplicitUniversal.get(varName);
				if (existingBinding!=null) {
					//already existed
					if (isInAssumption) {
						//set binding unknown
						existingBinding.bindingType = BindingType.UNKNOWN;
					} else if (!bt.equals(BindingType.ALL) || !existingBinding.bindingType.equals(BindingType.ALL)) {
						//one is existential -- set unknown
						existingBinding.bindingType = BindingType.UNKNOWN;
					} else {
						//existing one is already "all"; so we can treat this occurrence under the _same_ quantification
						//(do nothing)
					}
				} else {					
					//new, just add it
					BindingType bt2 = bt;
					if (isInAssumption) {
						//for assumptions, reverse semantics
						if (bt.equals(BindingType.EXISTS)) bt2 = BindingType.ALL;
						else if (bt.equals(BindingType.ALL)) bt2 = BindingType.EXISTS;
						else bt2 = BindingType.UNKNOWN;
					}
					
					addBindingMaybeChange(new VariableBinding(varName, bt2, false), varsExplicitGlobalList);
					varsExplicitInScope = new LinkedHashMap<String, VariableBinding>(varsExplicitInScope);
					addBindingMaybeChange(new VariableBinding(varName, bt2, false), varsExplicitInScope);
				}				
			}
			
			//recurse on quantification target
			collectVariablesFromTermList( ((MathsGroup)parsedTerm).getList().get( ((MathsGroup)parsedTerm).getList().size()-1 ),					
					isInAssumption, isInConclusion,
					varsExplicitInScope, varsExplicitGlobalList, varsImplicitExistential, varsImplicitUniversal);
		} else {
			if (isImplication(parsedTerm)) {
				//parse conclusion first (assume last thing)
				
				collectVariablesFromTermList( ((MathsGroup)parsedTerm).getList().get( ((MathsGroup)parsedTerm).getList().size() - 1 ),				
					isInAssumption, isInConclusion,
							varsExplicitInScope, varsExplicitGlobalList, varsImplicitExistential, varsImplicitUniversal);
				
				//then do assumptions with assumption flag inverted
				
				for (int i=0; i<((MathsGroup)parsedTerm).getList().size()-1; i++) {
					collectVariablesFromTermList( ((MathsGroup)parsedTerm).getList().get(i),
							!isInAssumption, false,
							varsExplicitInScope, varsExplicitGlobalList, varsImplicitExistential, varsImplicitUniversal);
				}
				
			} else if (parsedTerm instanceof IsabelleOperatorGroup && ((IsabelleOperatorGroup)parsedTerm).
					getOperator().toString().equals(IsabelleParser.COLON_TYPE_OPERATOR)) {
				//only take the LHS of type assignments
				collectVariablesFromTermList( ((MathsGroup)parsedTerm).getList().get(0),
						isInAssumption, isInConclusion,
						varsExplicitInScope, varsExplicitGlobalList, varsImplicitExistential, varsImplicitUniversal);
			} else {		
				//otherwise iterate over contents
				for (MathsExpression child : ((MathsGroup)parsedTerm).getList())
					collectVariablesFromTermList(child, isInAssumption, isInConclusion,
							varsExplicitInScope, varsExplicitGlobalList, varsImplicitExistential, varsImplicitUniversal);
			}
		}
	}

	private boolean isImplication(MathsExpression operator) {
		if (operator instanceof IsabelleImplicationGroup) return true;
		if (operator.isToken() && operator.isOperator() && IsabelleParser.isImplicationOperator(operator.toString())) return true;
		return false;
	}	

//	public boolean isValid(MathsExpression systemExpression) {
//		return true;
//	}

	public MathsExpression parse(String systemText) {
		systemText = preprocess(systemText);
		MathsExpression exp = new IsabelleParser().parse(systemText);
		return exp;
	}

	public MathsExpression stripQuantifications(MathsExpression goal, boolean deep) {
		return new IsabelleParser().stripQuantifications(goal, deep);
	}

	public MathsExpression toCommon(MathsExpression systemLanguageSubgoal) {
		//TODO for now isabelle is the common language but eventually it might be something else (math ml?)
		return systemLanguageSubgoal;
	}

	public MathsExpression fromCommon(MathsExpression commonLanguageSubgoal) {
		//TODO for now isabelle is the common language but eventually it might be something else (math ml?)
		return commonLanguageSubgoal;
	}

	public String toText(MathsExpression systemSubgoal) {
		return toText(systemSubgoal, null);
	}
	
	/** preferMeta argument is whether a 'meta' style format is preferred (for provers where it is supported);
	 * in Isabelle, for example, this is [| assm1 ; assm2 |] ==> conclusion (as opposed to (assm1 & assm2) --> conclusion)
	 */
	//this may be needed at API level (?)
	public String toText(MathsExpression proverSubgoal, Boolean preferMeta) {
		if (preferMeta!=null) {
			if (preferMeta) proverSubgoal = new IsabelleParser().toMetaLevel(proverSubgoal);
			else proverSubgoal = new IsabelleParser().toObjectLevel(proverSubgoal);
		}
		return proverSubgoal.toString();
	}

	public boolean isProverExpressionCompatible(MathsExpression proverExpression,
			MathsProverTranslator proverTranslator) {
		//TODO if we had other provers we might have expressions in them which aren't understood here...
		return true;
	}

	public MathsExpression tidyBrackets(MathsExpression systemExpression,
			Boolean putWhereNice, Boolean dontPutWhereAssociative,
			Boolean dontPutOutermost) {
		return new IsabelleParser().tidyBrackets(systemExpression, putWhereNice, dontPutWhereAssociative, dontPutOutermost);
	}

	public String annotateWithTypeInformation(String result,
			VariableBinding[] variablesAndBindings) {
		
		//need to find 1st occurrence of each variable? (only need to find one variable?) 
		//in the result and explicitly state that it is of type real

		result = " " + result + " ";
		
		//indicate that each var (first usage only) is of type real
		for (VariableBinding vb : variablesAndBindings) {
			result = result.replaceFirst("\\b"+vb.varName+"\\b", "("+vb.varName+"::"+"real"+")");
		}
		
		//also restore every implicit existential to have its question mark
		for (VariableBinding vb : variablesAndBindings) {
			if (vb.isImplicit && vb.bindingType==BindingType.EXISTS) {
				result = result.replaceAll("\\b"+vb.varName+"\\b", "\\?"+vb.varName);
			}
		}
		
		return result.trim();
	}

	public void ensureVariableNamesAreSafe(Map<String, VariableBinding> vars) {
		//does nothing
	}

	public Object ensureVariableNamesAreSafe(MathsExpression problemSystem) {
		return problemSystem;
	}
	
	public List<MathsExpression> getConjuncts(MathsExpression systemExp) {
		return IsabelleParser.getConjuncts(systemExp, true);
	}
}
