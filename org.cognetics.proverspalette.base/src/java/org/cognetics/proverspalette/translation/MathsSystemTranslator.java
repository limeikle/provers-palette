package org.cognetics.proverspalette.translation;

import java.util.Map;
import java.util.Set;

import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsToken;

public interface MathsSystemTranslator {

	String getSystemName();
	
	/** returns the system-specific parse-tree corresponding to 
	 * the given parse-tree in the common language */
	MathsExpression fromCommon(MathsExpression commonLanguageExpression);

	/** returns the common language parse-tree corresponding to 
	 * the given system-specific parse-tree */
	MathsExpression toCommon(MathsExpression systemExpression);

	/** returns the system-specific text representation for 
	 * the given system-specific parse-tree */
	String toText(MathsExpression systemExpression);
	
	/** returns the system-specific parse-tree for 
	 * the given text representation of an expression in the system's language */
	MathsExpression parse(String systemText);
	
	/** attempts to clean up bracketing in an expression;
	 * this is an optional method, implementations need not respect 'true' and 'false' here
	 * (but _must not_ change bracketing unless true or false is set)
	 * <p>
	 * for each parameters null means don't change, 
	 * true/false means change those brackets referred to by the parameter   
	 * <p>
	 * dontPutOutermost means whether the entire expression should not have a bracket around it;
	 * dontPutWhereAssociative means whenever an associatiave operation is used multiple times,
	 * don't bracket the individual usages (e.g. rewrite (2+(3+4)) as 2+3+4);
	 * putWhereNice refers to bracketing where order of operations means bracketing isn't necessary
	 * but where a user might like them, e.g. (2*3)+(3*4), or ((P --> Q) ==> R),
	 * (this may vary by system and by translator implementation) 
	 * <p>
	 * true, true, true is the most common for tidying
	 **/
	public MathsExpression tidyBrackets(MathsExpression systemExpression, 
			Boolean putWhereNice, 
			Boolean dontPutWhereAssociative, 
			Boolean dontPutOutermost);
	
//	/** preferMeta argument is whether a 'meta' style format is preferred (for provers where it is supported);
//	 * in Isabelle, for example, this is [| assm1 ; assm2 |] ==> conclusion (as opposed to (assm1 & assm2) --> conclusion)
//	 */
//	String toCommonText(String systemText, boolean preferMeta);	
//
//	/** preferMeta argument is whether a 'meta' style format is preferred (for provers where it is supported);
//	 * in Isabelle, for example, this is [| assm1 ; assm2 |] ==> conclusion (as opposed to (assm1 & assm2) --> conclusion)
//	 */
//	MathsExpression toCommonParsed(MathsExpression subgoal, boolean preferMeta);	
//
//	/** returns variables and their bindings, in the appropriate quantification order:
//	 * bindings for vars in the problem may be UNKNOWN but should not be null;
//	 * note that re ordering, any global vars (implicit quantifications) should come before
//	 * vars which are explicitly quantified, with forall before exists in most provers
//	 */  
	
	/** returns all variables and bindings for the given system-specific expression */
	Map<String,VariableBinding> getVariables(MathsExpression systemExpression);

	/** returns the binding type for the given system-specific quantification word
	 * (e.g. "EX" or "E"); return null if the quantiication is not known here */
	BindingType getBindingTypeFromQuantificationWord(String quantificationWord);
	
	/** grouping characters used for logic expressions in this system;
	 * usually one of MathsGroup.BRACKETS_SQUARE or BRACKETS_ROUND */
	String[] getLogicalGroupingChars();
    
	/** system-specific text for the given variable binding, 
	 * e.g. "E x" or "EX x"; or null if not known here */
	String getQuantificationText(VariableBinding vb);

//	/** true if the expression can be sent to, and understood by, the system
//	 * (e.g. if it is in PNF, predicate free, and 
//	 * contains no forbidden operations (like division in QEPCAD)) */
//	boolean isValid(MathsExpression systemExpression);
	
	/** true if the given prover expression, with the given translator, can be understood by this system;
	 * (e.g. for QEPCAD, true iff it is in PNF, predicate free, and 
//	 * contains no forbidden operations like division) */
	boolean isProverExpressionCompatible(MathsExpression proverExpression, MathsProverTranslator proverTranslator);
	
//	String combineConjuncts(String assmLine, String text);
//
//	/** returns a prover statement saying that the input equals the output under the normalization assumptions
//	 * with the given bindings; or null if no statement can be formed
//	 */
//	String getEqualsUnderAssumptionsProverText(VariableBinding[] variablesAndBindings,
//			List<String> normalizationAssumptionsQepcad, String inputQepcad, String outputQepcad);
//	
//	public MathsExpression getProverConjunction(MathsExpression ...conjuncts);
//
//	public MathsExpression getProverImplication(MathsExpression concl,
//			List<MathsExpression> assms, boolean preferMeta);
//
//	public MathsExpression getProverParsedWithQuantifications(
//			VariableBinding[] variablesAndBindings, MathsExpression base, boolean stripInitialUniversal);

	/** strips quantifications, either from the outside only, or everywhere if deep is set */
	MathsExpression stripQuantifications(MathsExpression systemExpression, boolean deep);
	
	Set<MathsToken> getUnknownPredicates(MathsExpression systemExpression);

	/** marks any invalid variable names as 'unknown' or, in certain permitted circumstances,
	 * renames them for use in the target system, if we know that the renaming will be recognised by the prover
	 * (e.g. stripping the question mark in isabelle variable names).
	 * TODO in future rename maps might be permitted */
	void ensureVariableNamesAreSafe(Map<String, VariableBinding> vars);
	
	/** returns a version of the problem with the same renaming as the other 'ensureVariableNamesAreSafe' method;
	 * may return either a string or a MathsExpression (callers should use 'toString');
	 * TODO this method should be replaced by something more sensible at some point in the future 
	 */
	Object ensureVariableNamesAreSafe(MathsExpression problemSystem);

}
