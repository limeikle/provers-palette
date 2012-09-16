package org.cognetics.proverspalette.qepcad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.heneveld.maths.structs.DefaultProblemListener;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressionParserAbstract;
import org.heneveld.maths.structs.MathsExpressions;
import org.heneveld.maths.structs.MathsGroup;
import org.heneveld.maths.structs.MathsOperatorGroup;
import org.heneveld.maths.structs.MathsOperatorToken;
import org.heneveld.maths.structs.MathsToken;
import org.heneveld.maths.structs.ProblemListener;
import org.heneveld.maths.structs.ProblemListener.ProblemLevel;

public class QepcadParser extends MathsExpressionParserAbstract.TwoPassParser {

	public QepcadParser() {
		this(new DefaultProblemListener(ProblemLevel.ERROR, ProblemLevel.FATAL));
	}
	
	public QepcadParser(ProblemListener problemListener) {
		super(problemListener);
	}

	public static class QepcadAbstractGroup extends MathsOperatorGroup {
		protected QepcadAbstractGroup(List<MathsExpression> terms, String[] outerBrackets, int indexOfOperator) {
			super(terms, outerBrackets, indexOfOperator);
		}		
	}

	public static class QepcadBinding extends QepcadAbstractGroup {
		public QepcadBinding(String quantificationWord, String quantifiedVar, QepcadAbstractGroup nextExpression) {
			super(makeList(MathsExpressions.newToken(quantificationWord, true),
					MathsExpressions.newToken(quantifiedVar, false),
					nextExpression), BRACKETS_ROUND, 0);
		}
		private static List<MathsExpression> makeList(MathsExpression ...terms) {
			List<MathsExpression> result = new ArrayList<MathsExpression>();
			for (MathsExpression e : terms)
				result.add(e);
			return result;
		}
		public String getQuantificationWord() {
			return get(0).toString();
		}
		public String getQuantifiedVar() {
			return get(1).toString();
		}
		public QepcadAbstractGroup getUnderlyingExpression() {
			return (QepcadAbstractGroup) get(2);
		}
		@Override
		public String toString() {
			return "("+getQuantificationWord()+" "+getQuantifiedVar()+")"+getUnderlyingExpression();
		}
	}
	
	public static class QepcadOperatorExpression extends QepcadAbstractGroup {
		private final boolean isNumeric;
		
		protected QepcadOperatorExpression(List<MathsExpression> terms, int indexOfOperator, boolean isNumeric) {
			super(terms, isNumeric ? BRACKETS_ROUND : BRACKETS_SQUARE, indexOfOperator);
			this.isNumeric = isNumeric;
		}

		public QepcadOperatorExpression(List<MathsExpression> terms, int indexOfOperator) {
			super(terms, BRACKETS_SQUARE, indexOfOperator);
			this.isNumeric = isNumericOperator(getOperator());
		}				

		public QepcadOperatorExpression(List<MathsExpression> terms, String[] brackets, int indexOfOperator) {
			super(terms, brackets, indexOfOperator);
			this.isNumeric = isNumericOperator(getOperator());
		}				

		@Override
		public String[] getOuterBrackets() {
			if (isNumeric) return BRACKETS_ROUND;
			return super.getOuterBrackets();
		}
		
		public boolean isNumericType() {
			return isNumeric;
		}
		
		@Override
		public String toString() {
			return super.toString().replaceAll("  +", " ");
		}		
	}
	
	/** multiplication, defined so we can detect it explicitly, even though it is an empty string,
	 * e.g. to translate it appropriately */
	public static final MathsToken MULTIPLICATION_TERM = new MathsOperatorToken("");
	
	public static final String[] NUMERIC_OPERATORS = new String[] {
		"+", "-", "/", "^"
	};

	public static final String[] QEPCAD_OPERATORS_ASSOCIATIVE = new String[] {
		"+", "", "/\\", "\\/"
	};

	private static boolean isNumericOperator(MathsExpression op) {
		if (!(op instanceof MathsToken) || !((MathsToken)op).isOperator() || !op.isToken()) return false;
		if (op==MULTIPLICATION_TERM) return true;
		String opS = op.toString();
		for (String opN : NUMERIC_OPERATORS)
			if (opN.equals(opS)) return true;
		return false;
	}

	public static final Operator[][] QEPCAD_OPERATORS_RANKED = new Operator[][] {
		new Operator[] { new Infix("^", -1) }, 
		new Operator[] { new Operator("-", 1, 1, 0, 0, false, false) }, 
		new Operator[] { new Infix("", 0), new Infix("/", -1) }, 
		new Operator[] { new Infix("+", 0), new Infix("-", -1) }, 
		new Operator[] { new Infix("<", null), new Infix(">", null), new Infix("<=", null), new Infix(">=", null) },
		new Operator[] { new Operator("~", 1, 1, 0, 0, false, false) }, 
		new Operator[] { new Infix("=", 0), new Infix("/=", -1) },  
		new Operator[] { new Infix("/\\", 0) },  
		new Operator[] { new Infix("\\/", 0) },  
		new Operator[] { new Infix("-->", 1) },  
		new Operator[] { new Infix("==>", 1), new Infix("<==>", 0) },  		
		new Operator[] {}	
	};
	
	@Override
	protected MathsOperatorGroup createOperatorGroup(List<MathsExpression> outputTerms, String[] brackets, int operatorIndex) {
		if (brackets[0].equals(MathsGroup.BRACKETS_UNKNOWN[0])) 
			return new QepcadOperatorExpression(outputTerms, operatorIndex);
		return new QepcadOperatorExpression(outputTerms, brackets, operatorIndex);
	}

	@Override
	protected Operator[][] getOperatorsRanked() {
		return QEPCAD_OPERATORS_RANKED;
	}
	
	public MathsExpression parse(String input) {
		MathsExpression firstPass = getFirstPassGroupOnlyParser(getProblemListener()).parse(input);
		if (isBindingExpression(firstPass)) {
			MathsGroup g = (MathsGroup) firstPass;
			QepcadAbstractGroup result = (QepcadAbstractGroup) expandWithOperators(g.getLast());
			for (int i=g.getList().size()-2; i>=0; i--) {
				result = new QepcadBinding(g.get(0).toString(), g.get(1).toString(), result);
			}
			return result;
		} else return expandWithOperators(firstPass);
	}

	private boolean isBindingExpression(MathsExpression exp) {
		if (exp.isToken()) return false;
		MathsGroup g = (MathsGroup) exp;
		if (g.getOuterBrackets()[0].length()>0) return false;
		if (g.getLast().isToken()) return false;
		if (!((MathsGroup)g.getLast()).getOuterBrackets()[0].equals("[")) return false;		
		for (int i=g.getList().size()-2; i>=0; i--) {
			MathsExpression gi = g.get(i);
			if (gi.isToken()) return false;
			MathsGroup gg = (MathsGroup) gi;
			if (gg.getOuterBrackets()[0].equals("(")) return false;
			if (gg.size(false)!=2) return false;
			if (!gg.get(0).isToken()) return false;
			if (!gg.get(1).isToken()) return false;			
		}
		return true;
	}

	@Override
	/** just inserts multiplication wherever needed */
	protected MathsExpression expandCustom(List<MathsExpression> inputTerms, String[] brackets) {
		//force use of ? brackets so creation is good
		if (!brackets[0].equals(MathsGroup.BRACKETS_UNKNOWN[0]))
			return expandWithOperators(inputTerms, MathsGroup.BRACKETS_UNKNOWN);
		
		//insert multiplication where needed
		boolean needsChanging = false;
		MathsExpression previous = null;
		for (MathsExpression tl : inputTerms) {
			if (tl.isOperator()) {
				//nothing
			} else {
				if (previous!=null && !previous.isOperator()) {
					//two vars in a row
					needsChanging = true;
					break;
				}
			}
			previous = tl;
		}

		if (needsChanging) {

			//duplicate list and insert multiplication
			List<MathsExpression> result = new ArrayList<MathsExpression>();
			previous = null;
			for (MathsExpression tl : inputTerms) {
				boolean skipThisTerm = false;
				if (tl.isOperator()) {
					//nothing
				} else {
					if (previous!=null && !previous.isOperator()) {
						//insert multiplication
						result.add(MULTIPLICATION_TERM);
					}				
				}
				if (!skipThisTerm)
					result.add(tl);
				previous = tl;
			}
			return expandWithOperators(result, brackets);
		}
		
		//no changes for multiplication... 
//		is bracket type correct?
//		(not needed, because we pass the unknown bracket '?')
//		String[] desiredBrackets = MathsGroup.BRACKETS_SQUARE;
//		for (MathsExpression tl : inputTerms) {
//			if (isNumericOperator(tl)) desiredBrackets = MathsGroup.BRACKETS_ROUND;
//		}
//		if (!brackets[0].equals(desiredBrackets[0])) {
//			//wrong brackets -- correct and expand
//			return expandWithOperators(inputTerms, desiredBrackets);
//		}
		
		//no custom changes... probably return null
		return super.expandCustom(inputTerms, brackets);		
	}

	@Override
	protected String[] getAssociativeOperators() {
		return QEPCAD_OPERATORS_ASSOCIATIVE;
	}

	@Override
	protected String[] getBracketsForExpression(MathsExpression expression) {
		if (expression.isToken()) return MathsGroup.BRACKETS_NONE;
		if (expression instanceof QepcadOperatorExpression && ((QepcadOperatorExpression)expression).isNumericType())
			return MathsGroup.BRACKETS_ROUND;
		return MathsGroup.BRACKETS_SQUARE;
	}

	public MathsExpression stripQuantifications(MathsExpression q, boolean deep) {
		if (q instanceof QepcadBinding)
			return stripQuantifications( ((QepcadBinding)q).getUnderlyingExpression(), deep );
		return super.stripQuantifications(q, deep);
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
			resultList.add(new MathsOperatorToken("/\\"));
			resultList.add(terms[i]);
			result = new QepcadOperatorExpression(resultList, 1);
		}
		List<MathsExpression> resultList = new ArrayList<MathsExpression>();
		resultList.add(result);
		resultList.add(new MathsOperatorToken("/\\"));
		resultList.add(terms[terms.length-1]);
		result = new QepcadOperatorExpression(resultList, 1);
		return result;
	}
	

}
