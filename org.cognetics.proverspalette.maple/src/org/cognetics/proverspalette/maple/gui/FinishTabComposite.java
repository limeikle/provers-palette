package org.cognetics.proverspalette.maple.gui;

import java.util.ArrayList;

import org.cognetics.proverspalette.base.cli.ProversPaletteAbstractExternalProcess;
import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation;
import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.maple.MapleProblem;
import org.cognetics.proverspalette.maple.MapleProcess;
import org.cognetics.proverspalette.translation.CommonMathsLanguage;
import org.eclipse.swt.widgets.Composite;
import org.heneveld.maths.structs.MathsExpression;


public class FinishTabComposite extends
		org.cognetics.proverspalette.base.gui.FinishTabComposite {
	
	public FinishTabComposite(ProversPaletteViewPartAbstract view, String tabName, int style) {
		super(view, tabName, style);
		createContents();
	}


	protected void createProofCommandModeRadioButtons(Composite row) {
		new ProofCommandButtonWrapper(row, "Oracle") {
			@Override
			public String getCurrentProofCommand() {
				return getWarningIfPreviewTabManualllyChanged()+
                 getProverCommentForCurrentResultIfEnabled()+"\n"+
				"apply (maple \"" +getProverFormOfCurrentResult() +"\" )\n" +
				 massageGoalIfNoManualChanges();
			}			
		}.getButton().setSelection(true);
				
		new ProofCommandButtonWrapper(row, "Subgoal") {
			@Override
			public String getCurrentProofCommand() {
				return getSubgoalWarningIfPreviewTabManualllyChanged()+
				getProverCommentForCurrentResultIfEnabled()+"\n"+
				"apply (subgoal_tac \"" +getProverFormOfCurrentResult()+"\" )";
			}			
		}.getButton().setSelection(false);
		
		//TODO - will Maple ever give a result we can use as a witness?
		//new ProofCommandButtonWrapper(row, "Instantiate") {
		//	@Override
		//	public String getCurrentProofCommand() {
		//		return getProverCommentForCurrentResultIfEnabled()+"\n"+
		//		computeProverInstantiateCommandText();
		//	}			
		//}.getButton().setSelection(false);
	}

	
	public String getProverFormOfCurrentResult(){
		String outputExternalForm = super.getProverFormOfCurrentResult();

		if (!(lastSuccessfulInvocation instanceof MapleProcess)) return "";
		MapleProblem inputMP = ((MapleProcess)lastSuccessfulInvocation).getProblem();
		
		MathsExpression resultCommon = CommonMathsLanguage.getEqualsUnderAssumptions(
				new ArrayList<MathsExpression>(),
				inputMP.getAsCommonLanguage(), 
				external().toCommon( external().parse( outputExternalForm ) ));

		String result = prover().toText( prover().fromCommon( resultCommon ) );
		if (result==null) return "";
		
		//TODO - what should the variablesAndBindings be set to?
//		result = prover().annotateWithTypeInformation(result, variablesAndBindings);
		
		return result;
	}
	
	public boolean isResultValid() {
		if (!super.isResultValid()) return false;
		
		if (!(lastSuccessfulInvocation instanceof MapleProcess)) return false;
		
		return true;
	}
	

	public String massageGoalIfNoManualChanges(){
		
		if(!(view.getStartComposite().hasManualChanges()) && 
				!(view.getProblemComposite().hasManualChanges()) &&
				!(view.getPreviewComposite().hasManualChanges()) && 
				!(view.getFinishComposite().hasManualChanges())){
			return massageGoal();
		}
		
		//manual changes must have been applied so no need to massage goal --- just return a space
		return "";
	}
	
	public String massageGoal(){
		StringBuffer massageCommand = new StringBuffer();
		
		//TODO - don't take from output styled text now - take from data object?
		String mapleResult = ((MapleProcess)lastSuccessfulInvocation).getResult();
		MapleProblem lpr = ((MapleProcess)lastSuccessfulInvocation).getProblem();

		if( mapleResult.equalsIgnoreCase("true") &&
		    view.getImportComposite().getConclusionIfSelected()!=null ){
			massageCommand.append("apply blast \n");

			return massageCommand.toString();
		}	

		if(mapleResult.equalsIgnoreCase("false")){
			// conclusion not selected
			if(view.getImportComposite().getConclusionIfSelected()==null){
				massageCommand.append("apply blast");
			}
			// no else as we do nothing if we sent the conclusion
			return massageCommand.toString();
		}

		return massageCommand.toString();
	}	

	public String getProverCommentForCurrentResult() {
		//FIXME isabelle only
		if (lastSuccessfulInvocation instanceof ProversPaletteAbstractExternalProcess)
			return "(* "+view.getName()+" ran with the following input: \n"+
				((ProversPaletteAbstractExternalProcess)lastSuccessfulInvocation).getInput()+
				"*)";
		return "(* comment details not available *)";
	}

	public void updateOnInvocationSuccess(ProversPaletteExternalInvocation invocation) {
		
		lastSuccessfulInvocation = invocation;
		
		if (!(invocation instanceof MapleProcess)) return;
		
		updateFinishTab();
		
	}
	
	public void updateFinishTab() {
		if (!(lastSuccessfulInvocation instanceof MapleProcess)) {
			resetFields();
			return;
		}

		MapleProcess maple = ((MapleProcess)lastSuccessfulInvocation);
		String mapleInputToDisplay = maple.getProblem().getForFinishTab();
		toolInputStyledText.setText(
				(maple.isInputManual() ? "(modified input script, originally based on problem below)\n\n" :
				 view.getPreviewComposite().hasManualChanges() ? "(input script manually changed, originally based on problem below)\n\n"
				 : "") +				
				 convertFromRealTextToDisplayText( mapleInputToDisplay, toolInputStyledText ));

		toolOutputStyledText.setText(
				convertFromRealTextToDisplayText( maple.getAnnotatedResult(), toolOutputStyledText ));


		proofCommandButtons.get("Oracle").getButton().setEnabled(true);
		proofCommandButtons.get("Subgoal").getButton().setEnabled(true);
		includeComments.setEnabled(true);

		refreshProofScriptCommandStyledText();
	}
	
}
