package org.cognetics.proverspalette.translation.isabelle;

import org.heneveld.isabelle.IsabelleImplicationGroup;
import org.heneveld.isabelle.IsabelleParser;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public class IsabellePnfChecker {
	
	public static boolean inMeta(String input){
		return MathsExpressions.containsToken(new IsabelleParser().parse(input), MathsExpressions.newToken("==>", true));
	}
	
	public static boolean inObject(String input){
		return !inMeta(input);
	}
	
//	/** @deprecated use IsabelleQepcadTranslator */
//	public static boolean checkIfPNF(String input){
//		MathsExpression parsedInput = new IsabelleParser().parse(input);
//		//strip quantifications from the front
//		parsedInput = new IsabelleParser().stripQuantifications(parsedInput, false);
//		return !containsQuantificationAnywhere(parsedInput);
//	}
//	
//	/** true if the term, or any term contained therein, is a quantification
//	 * (ie. has ALL or EX at its head)
//	 */
//	public static boolean hasQuantifier(String text) {
//		return containsQuantificationAnywhere(new IsabelleParser().parse(text));
//	}
//
//	/** true if the term, or any term contained therein, is a quantification
//	 * (ie. has ALL or EX at its head)
//	 * @deprecated use IsabelleQepcadTranslater
//	 */
//	public static boolean containsQuantificationAnywhere(MathsExpression expression) {
//		return new IsabelleQepcadTranslator().containsQuantificationAnywhere(expression);		
//	}

	public static String objectConvertIsabelleToPNF(String input) {		
		StringBuffer command = new StringBuffer();		
	    //anything else?
	    command.append("apply (simp only: prenex_normal_form)\n");
	    return command.toString();
	    
	}	
	
	public static String convertToObject(String input){
		IsabelleParser parser = new IsabelleParser();
		return parser.toObjectLevel(parser.parse(input)).toString();
	}
	
	public static String metaConvertIsabelleToPNF(String input) {
		IsabelleParser parser = new IsabelleParser();
		MathsExpression exp = parser.parse(input);
		
		StringBuffer command = new StringBuffer();

		MathsExpression unquantified = new IsabelleParser().stripQuantifications(exp, false);
		if (unquantified instanceof IsabelleImplicationGroup && "==>".equals( ((IsabelleImplicationGroup)unquantified).getOperator().toString() )) {
			
//			String metaLevel = new String();		
//			SubgoalRecord sg = IsabelleSubgoalParser.parseIsabelleString(input);		
//			metaLevel = convertToObject(input);

//			command.append("apply (subgoal_tac \"" + convertToObject(input) +"\")\n");
//			command.append("apply (rotate_tac -1, erule impE)\n");
//			command.append("apply blast\n");
//			command.append("apply assumption\n");
//			for (MathsExpression a : ((IsabelleImplicationGroup)unquantified).getAssumptions()) {
//				command.append("apply(thin_tac \"" +a +"\")\n");
//			}
			command.append("apply (atomize (full))\n");
		}
		
		command.append("apply (simp only: prenex_normal_form)\n");


		return command.toString();

	}

	
	

}
