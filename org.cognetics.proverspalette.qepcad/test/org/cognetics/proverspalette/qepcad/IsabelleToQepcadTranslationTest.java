package org.cognetics.proverspalette.qepcad;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.cognetics.proverspalette.translation.isabelle.IsabelleTranslator;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsOperatorGroup;

public class IsabelleToQepcadTranslationTest extends TestCase {

	IsabelleTranslator t = new IsabelleTranslator();
	QepcadTranslator q = new QepcadTranslator();
	
	public void testSimpleParse() {
		MathsExpression exp = t.parse("x + 0 = 0");
		
		assertTrue(exp instanceof MathsOperatorGroup);
		assertEquals("=", ((MathsOperatorGroup)exp).getOperator().toString());
		MathsExpression sum = ((MathsOperatorGroup)exp).getList().get(0);
		assertTrue(sum instanceof MathsOperatorGroup);
		assertEquals("+", ((MathsOperatorGroup)sum).getOperator().toString());
	}

	public void testExpandable() {
		MathsExpression exp = t.parse("foo x");
		
		assertTrue(exp instanceof MathsOperatorGroup);
		assertEquals("foo", ((MathsOperatorGroup)exp).getOperator().toString());
		
		Set<MathsExpression> unknowns = q.getUnknownPredicates(exp);

		Iterator<MathsExpression> ui = unknowns.iterator();
		assertTrue("got: "+unknowns, ui.hasNext());
		assertEquals("foo x", ui.next().toString());
		assertFalse("got: "+unknowns, ui.hasNext());
		
		assertTrue(t.shouldSuggestExpandUnknownPredicates(exp, unknowns));
		assertEquals("apply (simp only: foo_def)", t.getCommandForExpandingPredicates(unknowns).trim());
	}

	public void testRepExpandable() {
		MathsExpression exp = t.parse("(fst (Rep_point c))");
		
		Set<MathsExpression> unknowns = q.getUnknownPredicates(exp);

		Iterator<MathsExpression> ui = unknowns.iterator();
		assertTrue("got: "+unknowns, ui.hasNext());
		assertEquals("fst", ((MathsOperatorGroup)ui.next()).getOperator().toString());
		assertTrue("got: "+unknowns, ui.hasNext());
		assertEquals("(Rep_point c)", ui.next().toString());
		assertFalse("got: "+unknowns, ui.hasNext());
		
		assertTrue(t.shouldSuggestExpandUnknownPredicates(exp, unknowns));
		assertEquals("apply (cases c)\n" +
				"apply simp\n" +
				"apply (simp only: fst_def)\n" +
				"", t.getCommandForExpandingPredicates(unknowns));
	}

	public void testAndMetaUnivQuant() {
		String problem = "\\<And>x y xa ya xb yb xc yc xd yd. "+
				"[| 0 < (x - xa) * (yb - ya) - (y - ya) * (xb - xa); "+
				"0 < (x - xa) * (yd - ya) - (y - ya) * (xd - xa); "+
				"0 < (x - xa) * (yc - ya) - (y - ya) * (xc - xa); "+
				"0 < (xb - xa) * (yd - ya) - (yb - ya) * (xd - xa); "+
				"0 < (xd - xa) * (yc - ya) - (yd - ya) * (xc - xa)|]  "+
				"==>  0 < (xb - xa) * (yc - ya) - (yb - ya) * (xc - xa)";
		MathsExpression exp = t.parse(problem);
		System.out.println("PARSED: "+exp);
	}
}
