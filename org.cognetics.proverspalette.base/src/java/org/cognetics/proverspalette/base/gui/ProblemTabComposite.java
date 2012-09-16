package org.cognetics.proverspalette.base.gui;

import java.util.ArrayList;
import java.util.List;

import org.cognetics.proverspalette.translation.CommonMathsLanguage;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public abstract class ProblemTabComposite extends ProversPaletteTabCompositeAbstract {

	public ProblemTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
	}

	public abstract boolean isProblemAvailable();

	public void update() {
		MathsProverTranslator transProver = prover();
		
		List<MathsExpression> assmsCommon = new ArrayList<MathsExpression>();
		for (MathsExpression importedPart : view.getImportComposite().getCurrentSelectedAssumptions()) {
			assmsCommon.add(transProver.toCommon( importedPart ));			
		}
		
		MathsExpression conclusionItem = transProver.toCommon( 
				view.getImportComposite().getConclusionIfSelected() );
		
		updateWith(assmsCommon, conclusionItem);
				
		applyChanges();		
		noteUpdateCompleted();
	}

	protected MathsExpression createExpressionFromAssumptionsAndConclusion(
			List<MathsExpression> assmsCommon, MathsExpression conclusionItem) {
		MathsExpression selectedItemsSubgoalCommon;
		if (conclusionItem!=null) {
			selectedItemsSubgoalCommon = prover().toCommon( conclusionItem );
			selectedItemsSubgoalCommon = CommonMathsLanguage.getProverImplication(
					selectedItemsSubgoalCommon, assmsCommon, true);
		} else if (!assmsCommon.isEmpty()) {
			selectedItemsSubgoalCommon = CommonMathsLanguage.getProverConjunction(
					assmsCommon.toArray(new MathsExpression[0]));
		} else {
			selectedItemsSubgoalCommon = MathsExpressions.newToken("", false);
		}
		return selectedItemsSubgoalCommon;
	}

	/** update this form based on items selected from previous form;
	 * may use createExpressionFromAssumptionsAndConclusion;
	 * parameters are in the common language */ 
	protected abstract void updateWith(List<MathsExpression> assmsCommon, MathsExpression conclusionItem);		
		
}
