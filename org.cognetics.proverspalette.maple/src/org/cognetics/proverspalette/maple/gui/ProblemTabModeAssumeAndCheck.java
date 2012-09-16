/**
 * 
 */
package org.cognetics.proverspalette.maple.gui;

import org.heneveld.maths.structs.MathsExpression;

public class ProblemTabModeAssumeAndCheck extends ProblemTabMode {

	private final ProblemTabComposite problemTabComposite;
	public final static String NAME = "Assume and Check";
	public ProblemTabModeAssumeAndCheck(ProblemTabComposite problemTabComposite) {
		super(problemTabComposite, NAME, 
			"This mode will tell Maple to assume the assumptions then check whether the conclusion is always true.");
		this.problemTabComposite = problemTabComposite;
	}		

	@Override
	public String getGoalSetup() {
		StringBuffer mapleGoalSetup = new StringBuffer() ;
		if(!this.problemTabComposite.getView().getImportComposite().getCurrentSelectedAssumptions().isEmpty()){ 
			boolean first = true;

			mapleGoalSetup.append("assume( ");
			boolean multiple = this.problemTabComposite.getView().getImportComposite().getCurrentSelectedAssumptions().size()>1;
			for (MathsExpression importedPart : this.problemTabComposite.getView().getImportComposite().getCurrentSelectedAssumptions()){
				if (first) first = false;
				else mapleGoalSetup.append(",");
				if (multiple) mapleGoalSetup.append("\n  ");

				mapleGoalSetup.append(this.problemTabComposite.external().tidyBrackets(
						this.problemTabComposite.getView().getCurrentExternalSystemTranslator().stripQuantifications(
								this.problemTabComposite.external().fromCommon(
										this.problemTabComposite.prover().toCommon(importedPart))
								, true),
								true,true,true));
			}
			mapleGoalSetup.append(" );\n");
		}
		return mapleGoalSetup.toString();
	}

	@Override
	public String getGoalQuery() {
		StringBuffer mapleGoalQuery = new StringBuffer() ;					
		MathsExpression conclusionItem = this.problemTabComposite.getView().getImportComposite().getConclusionIfSelected();
		if (conclusionItem!=null) {
			mapleGoalQuery.append("is( ");	
			mapleGoalQuery.append(this.problemTabComposite.external().tidyBrackets(
					this.problemTabComposite.external().stripQuantifications(
							this.problemTabComposite.external().fromCommon(this.problemTabComposite.prover().toCommon(conclusionItem))
							, true),
							true,true,true));
			mapleGoalQuery.append(" );");
		}
		return mapleGoalQuery.toString();

		//TODO -old part kept to ensure variable names are safe?
		//					MathsExpression selectedItemsSubgoalCommon = null;
		//					List<MathsExpression> assmsCommon = new ArrayList<MathsExpression>();
		//					for (MathsExpression importedPart : view.getImportComposite().getCurrentSelectedAssumptions()) {
		//						assmsCommon.add(transProver.toCommon( importedPart ));			
		//					}
		//					
		//					String mapleGoal = "";
		//					
		//					MathsExpression conclusionItem = view.getImportComposite().getConclusionIfSelected();
		//					if (conclusionItem!=null) {
		//						selectedItemsSubgoalCommon = transProver.toCommon( conclusionItem );
		//						selectedItemsSubgoalCommon = CommonMathsLanguage.getProverImplication(
		//								selectedItemsSubgoalCommon, assmsCommon, true);
		//					} else if (!assmsCommon.isEmpty()) {
		//						selectedItemsSubgoalCommon = CommonMathsLanguage.getProverConjunction(
		//								assmsCommon.toArray(new MathsExpression[0]));
		//					} else {
		//						selectedItemsSubgoalCommon = MathsExpressions.newToken("", false);
		//					}
		//					
		//					if (selectedItemsSubgoalCommon.toString().length()>0) {
		//						
		//						MathsExpression problemSystem =
		//							view.getCurrentExternalSystemTranslator().tidyBrackets(
		//									view.getCurrentExternalSystemTranslator().stripQuantifications(
		//											view.getCurrentExternalSystemTranslator().fromCommon(selectedItemsSubgoalCommon), true),
		//							true, true, true);
		//						mapleGoal = external().ensureVariableNamesAreSafe(problemSystem).toString();
		//						goalsStyledText.setText( mapleGoal );		
		//
		//						buttonFinish.setEnabled( isProblemAvailable());
		//					} else {
		//						goalsStyledText.setText( "" );	
		//						buttonFinish.setEnabled( false );
		//					}


	}

}