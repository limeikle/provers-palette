package org.cognetics.proverspalette.maple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.heneveld.maths.structs.DefaultProblemListener;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsGroup;
import org.heneveld.maths.structs.MathsOperatorGroup;
import org.heneveld.maths.structs.MathsOperatorToken;
import org.heneveld.maths.structs.ProblemListener;
import org.heneveld.maths.structs.MathsExpressionParserAbstract.TwoPassParser;
import org.heneveld.maths.structs.ProblemListener.ProblemLevel;

public class MapleParser extends TwoPassParser {

	public MapleParser() {
		this(new DefaultProblemListener(ProblemLevel.ERROR, ProblemLevel.FATAL));
	}
	
	public MapleParser(ProblemListener problemListener) {
		super(problemListener);
	}
	
	public static class MapleAbstractGroup extends MathsOperatorGroup {
		protected MapleAbstractGroup(List<MathsExpression> terms, String[] outerBrackets, int indexOfOperator) {
			super(terms, outerBrackets, indexOfOperator);
		}		
	}
	
	public static class MapleOperatorExpression extends MapleAbstractGroup {
		
//		private final boolean isNumeric;
		
		protected MapleOperatorExpression(List<MathsExpression> terms, int indexOfOperator, boolean isNumeric) {
			super(terms, BRACKETS_ROUND, indexOfOperator);
//			this.isNumeric = isNumeric;
		}

		public MapleOperatorExpression(List<MathsExpression> terms, int indexOfOperator) {
			super(terms, BRACKETS_ROUND, indexOfOperator);
//			this.isNumeric = isNumericOperator(getOperator());
		}				

		public MapleOperatorExpression(List<MathsExpression> terms, String[] brackets, int indexOfOperator) {
			super(terms, brackets, indexOfOperator);
//			this.isNumeric = isNumericOperator(getOperator());
		}				

//		@Override
//		public String[] getOuterBrackets() {
//			if (isNumeric) return BRACKETS_ROUND;
//			return super.getOuterBrackets();
//		}
		
//		public boolean isNumericType() {
//			return isNumeric;
//		}
		
		@Override
		public String toString() {
			return super.toString().replaceAll("  +", " ");
		}		
	}

	@Override
	protected MathsOperatorGroup createOperatorGroup(List<MathsExpression> outputTerms, String[] brackets, int operatorIndex) {
		if (brackets[0].equals(MathsGroup.BRACKETS_UNKNOWN[0])) 
			return new MapleOperatorExpression(outputTerms, operatorIndex);
		return new MapleOperatorExpression(outputTerms, brackets, operatorIndex);
	}
	

	@Override
	protected String[] getBracketsForExpression(MathsExpression arg0) {
		return MathsGroup.BRACKETS_ROUND;
	}
	
//	public static final String[] NUMERIC_OPERATORS = new String[] {
//		"+", "-", "*", "/", "^"
//	};
	
	public static final String[] MAPLE_OPERATORS_ASSOCIATIVE = new String[] {
		"+", "*", "and", "or"
	};
	
//	private static boolean isNumericOperator(MathsExpression op) {
//		if (!(op instanceof MathsToken) || !((MathsToken)op).isOperator() || !op.isToken()) return false;
//		String opS = op.toString();
//		for (String opN : NUMERIC_OPERATORS)
//			if (opN.equals(opS)) return true;
//		return false;
//	}

	public static final Operator[][] MAPLE_OPERATORS_RANKED = new Operator[][] {
			new Operator[] { new Infix("^", -1) }, 
			new Operator[] { new Operator("-", 1, 1, 0, 0, false, false) }, 
			new Operator[] { new Infix("*", 0), new Infix("/", -1) }, 
			new Operator[] { new Infix("+", 0), new Infix("-", -1) }, 
			new Operator[] { new Infix("<", null), new Infix(">", null), new Infix("<=", null), new Infix(">=", null) },
			new Operator[] { new Operator("not", 1, 1, 0, 0, false, false) }, 
			new Operator[] { new Infix("=", 0), new Infix("<>", -1) },  
			new Operator[] { new Infix("and", 0) },  
			new Operator[] { new Infix("or", 0) },  
			new Operator[] { new Infix("implies", 1) },   		
			new Operator[] {}	
		};


	@Override
	protected Operator[][] getOperatorsRanked() {
		return MAPLE_OPERATORS_RANKED;
	}
	
	
	public MathsExpression parse(String input) {
		MathsExpression firstPass = getFirstPassGroupOnlyParser(getProblemListener()).parse(input);
		return expandWithOperators(firstPass);
	}

	@Override
	/** just inserts multiplication wherever needed */
	protected MathsExpression expandCustom(List<MathsExpression> inputTerms, String[] brackets) {
		//force use of ? brackets so creation is good
		if (!brackets[0].equals(MathsGroup.BRACKETS_UNKNOWN[0]))
			return expandWithOperators(inputTerms, MathsGroup.BRACKETS_UNKNOWN);

		return super.expandCustom(inputTerms, brackets);		
	}

	@Override
	protected String[] getAssociativeOperators() {
		return MAPLE_OPERATORS_ASSOCIATIVE;
	}
	
	public MathsExpression newConjunction(String[] brackets, MathsExpression ...terms) {
		if (terms.length==0) {
			getProblemListener().onProblem(new ProblemListener.DefaultProblem(ProblemLevel.FATAL, "cannot make conjunction from 0 terms"));
			return new MathsGroup(Collections.<MathsExpression>emptyList(), MathsGroup.BRACKETS_NONE);
		}
		MathsExpression result = terms[0];
		if (terms.length<=1) {
//			if (brackets[0].length()>0 && result instanceof MathsGroup && ((MathsGroup)result).getOuterBrackets()[0].length()==0)
			//might not get the needed brackets...
			return result;
		}
		for (int i=1; i<terms.length-1; i++) {
			List<MathsExpression> resultList = new ArrayList<MathsExpression>();
			resultList.add(result);
			resultList.add(new MathsOperatorToken("and"));
			resultList.add(terms[i]);
			result = new MapleOperatorExpression(resultList, 1);
		}
		List<MathsExpression> resultList = new ArrayList<MathsExpression>();
		resultList.add(result);
		resultList.add(new MathsOperatorToken("and"));
		resultList.add(terms[terms.length-1]);
		result = new MapleOperatorExpression(resultList, 1);
		return result;
	}

}
