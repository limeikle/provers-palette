package org.cognetics.proverspalette.qepcad.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cognetics.proverspalette.base.cli.ProversPaletteAbstractExternalProcess;
import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation;
import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.qepcad.QepcadProblem;
import org.cognetics.proverspalette.qepcad.QepcadProcess;
import org.cognetics.proverspalette.qepcad.QepcadProcess.ExecutionMode;
import org.cognetics.proverspalette.translation.CommonMathsLanguage;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.heneveld.isabelle.IsabelleImplicationGroup;
import org.heneveld.isabelle.QuantificationOperatorGroup;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;
import org.heneveld.maths.structs.MathsToken;

public class FinishTabComposite extends org.cognetics.proverspalette.base.gui.FinishTabComposite {

	public FinishTabComposite(ProversPaletteViewPartAbstract view, String tabName, int style) {
		super(view, tabName, style);
		createContents();
	}

	
	public void showCounterExampleButton() {
		ButtonWrapper cex = new ButtonWrapper(enableAndGetToolButtonsComposite(), "Find\nCounter-\nexample", SWT.PUSH) {
			@Override
			public void onSelection() {
				try {
					if (lastSuccessfulInvocation instanceof QepcadProcess) {
					//TODO - negate problem (giving EX form) and find witness which will be counterexample to original problem
						findCounterExample( ((QepcadProcess)lastSuccessfulInvocation).getProblem() );
					} else {
						System.err.println("counter example button should not have been enabled, no invocation available");;						
						getButton().setEnabled(false);
					}
				} catch(Exception ex){
					ex.printStackTrace();
				}
			}			
		};
		cex.getButton().setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));		
		cex.getButton().setEnabled(true);
		toolOutputGroupAndButtons.layout();
	}


	protected void createProofCommandModeRadioButtons(Composite row) {
		new ProofCommandButtonWrapper(row, "Oracle") {
			@Override
			public String getCurrentProofCommand() {
				return getWarningIfPreviewTabManualllyChanged()+
                 getProverCommentForCurrentResultIfEnabled()+"\n"+
				"apply (qepcad \"" +getProverFormOfCurrentResult() +"\" )\n" +
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
				
		new ProofCommandButtonWrapper(row, "Instantiate") {
			@Override
			public String getCurrentProofCommand() {
				return getProverCommentForCurrentResultIfEnabled()+"\n"+
				computeProverInstantiateCommandText();
			}			
		}.getButton().setSelection(false);
	}

	
	public String getProverFormOfCurrentResult(){
		String outputExternalForm = super.getProverFormOfCurrentResult();

		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) return "";
		QepcadProblem inputQP = ((QepcadProcess)lastSuccessfulInvocation).getProblem();
		
		// NORM ASSMS ....  LHS  ...to qepcad ... qepcad says   RHS
		// now send   translate( ASSMS ==> ( LHS = RHS ) )   to prover
		// (or send   translate( ASSMS ) ==> ( translate (LHS) = translate (RHS) )   to prover...
		
		List<MathsExpression> assumptionsInCommon = new ArrayList<MathsExpression>();
		for (String assm : inputQP.normalizationAssumptions)
			assumptionsInCommon.add(
					external().toCommon( external().parse( assm ) ));
					
		VariableBinding[] binds = inputQP.variablesAndBindings;
		
		//set 'orderNonStandard' if the order is not:  implicit existentials, implicit universals, then explicit vars
//		boolean implicitsDone = false;
//		boolean implicitExistentialsDone = false;
//		boolean orderNonStandard = false;
//		for (VariableBinding vb : binds) {
//			if (!vb.isImplicit) implicitsDone = true;
//			else {
//				if (implicitsDone) orderNonStandard = true;
//				if (vb.bindingType != BindingType.EXISTS) {
//					implicitExistentialsDone = true;
//				} else {
//					if (implicitExistentialsDone) orderNonStandard = true;
//				}
//			}
//		}
//		if (orderNonStandard) {
//			//if order is non-standard then mark all bindings as explicit
//			//(because the implicit bindings will not be accurate)
//			VariableBinding[] bindsOrig = binds;
//			binds = new VariableBinding[bindsOrig.length];
//			for (int i=0; i<bindsOrig.length; i++) {
//				binds[i] = new VariableBinding(
//						bindsOrig[i].varName, bindsOrig[i].bindingType, false);
//			}
//		}
		
		//mark all vars are explicitly quantified 
		VariableBinding[] bindsOrig = binds;
		binds = new VariableBinding[bindsOrig.length];
		for (int i=0; i<bindsOrig.length; i++) {
			binds[i] = new VariableBinding(
					bindsOrig[i].varName, bindsOrig[i].bindingType, false);
		}
		
		//insert quantifications for all bindings 
		MathsExpression quantifiedInputQP = CommonMathsLanguage.getProverParsedWithQuantifications(binds, 
				external().toCommon( external().parse( inputQP.goal )), 
				false, false);
		
		MathsExpression resultCommon = CommonMathsLanguage.getEqualsUnderAssumptions(
				assumptionsInCommon, 
				quantifiedInputQP, 
				external().toCommon( external().parse( outputExternalForm ) ));

		String result = prover().toText( prover().fromCommon( resultCommon ) );
		if (result==null) return "";
				
		result = prover().annotateWithTypeInformation(result, binds);
		
		return result;
	}
	
	protected void findCounterExample(QepcadProblem problem){
		translateToExistentialForm(problem);
		runTranslatedProblem(problem, ExecutionMode.COUNTER_EXAMPLE);
		view.getPreviewComposite().activateTab();
	}
	
	public boolean runTranslatedProblem(QepcadProblem problem, ExecutionMode mode) {	
		try {			
			view.getPreviewComposite().runInBackground( new QepcadProcess(problem, mode) );
		} catch (Exception e) {
			view.getPreviewComposite().
				setOutputText("(The current settings are not valid QEPCAD input. \n" +
					"Check that all fields are instantiated. \n" +
					"The error reported was: "+e+".)\n");
			e.printStackTrace();
		}
		return true;
	}
	
	public void translateToExistentialForm(QepcadProblem problem){
		int numVars = problem.variablesAndBindings.length;
		for (int i=0; i<numVars; i++) {
			if (!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.ALL) &&
					!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.FREE))
				//shouldn't happen but guard against it
				throw new IllegalStateException("This problem cannot be inverted to a purely existential form.");
				
			problem.variablesAndBindings[i].bindingType = VariableBinding.BindingType.EXISTS;			
		}
		
		//TODO might be introducing too many brackets
		problem.goal = "[ ~ [ "+problem.goal+" ] ]";
	}

	public boolean isResultValid() {
		if (!super.isResultValid()) return false;
		
		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) return false;
		
		return true;
	}
	
	
	public String computeProverInstantiateCommandText(){
		if (!isResultValid()) return "";
		QepcadProcess qep = (QepcadProcess) lastSuccessfulInvocation;
		if (qep.getWitnessList()==null || qep.getWitnessList().isEmpty() ) 
			return "(no instantiations suggested)";
		
		StringBuffer instantiationCommands = new StringBuffer();
		
		if (!view.getImportComposite().isEverythingSelected() || 
				view.getProblemComposite().hasManualChanges() ||
				view.getPreviewComposite().hasManualChanges() ||
				view.getImportComposite().hasManualChanges()  ||
				view.getStartComposite().hasManualChanges())
			instantiationCommands.append("(* instantiations may not be valid as problem is modified. *)\n\n");

		Map<String,String> varValues = new LinkedHashMap<String,String>();			
		VariableBinding[] vars = qep.getProblem().variablesAndBindings;			
		List<String> witnesses = qep.getWitnessList();
		int i=0;
		while (i<vars.length && i<witnesses.size()) {
			varValues.put(vars[i].varName, witnesses.get(i));
			i++;
		}

		MathsExpression proverGoal = prover().parse(view.getStartComposite().getProverProblemTextLastApplied());

		//first the implicit existentials

		//TODO better would be to restore every implicit existential to have its question mark here
		//(instead we do it from the string)
		boolean foundImplicits = false;
		//we only care about the conclusion
		//(and using TrueE we can't replace the assumptions anyway)			
		if ((proverGoal instanceof IsabelleImplicationGroup) && 
				(((IsabelleImplicationGroup)proverGoal).getOperator().equals(MathsExpressions.newToken("==>", true))) )
			proverGoal = ((IsabelleImplicationGroup)proverGoal).getConclusion();
		String proverGoalString = " " + proverGoal.toString() + " ";
		for (VariableBinding b : qep.getProblem().variablesAndBindings) {				
			if (b.isImplicit && proverGoalString.matches(".*\\?"+b.varName+"\\b.*")) {
				String value = varValues.remove(b.varName);
				if (value==null)
					//qepcad says any value will do
					value="0";
				foundImplicits = true;
				proverGoalString = proverGoalString.replaceAll("\\?"+b.varName+"\\b", value);
			}
		}
		if (foundImplicits) {
			//TODO much better would be something like exI... apply (?x=0)

			//the TrueI is to get rid of 'True' introduced here (there might be a better way)
			instantiationCommands.append("apply (rule_tac P=\"" + proverGoalString.trim() +"\" in TrueE, rule TrueI)\n" );
		}

		//now the explicit existentials				
		do {
			if (proverGoal instanceof QuantificationOperatorGroup) {			
				QuantificationOperatorGroup qt = (QuantificationOperatorGroup) proverGoal;
				List<MathsToken> varsHere = qt.getQuantifiedVars();
				BindingType quantType = prover().getBindingTypeFromQuantificationWord(qt.getQuantificationWord());
				if (BindingType.EXISTS.equals( quantType )) {
					for (MathsToken v : varsHere) {							
						String vw = v.getToken();
						if (vw.startsWith("?")) vw = vw.substring(1);
						vw = varValues.remove( vw );
						if (vw!=null) {
							//add command to instantiate vw next
							instantiationCommands.append("apply (rule_tac x=\"" + vw +"\" in exI)\n" );
						} else {
							//no witness returned... presumably it can be anything so set to 0
							//TODO add comment that value doesn't matter
							instantiationCommands.append("apply (rule_tac x=\"0\" in exI)\n" );
						}
					}
				} else if (BindingType.ALL.equals( quantType )) {
					for (@SuppressWarnings("unused")
							MathsToken v : varsHere) {
						//apply rule allI
						//(if qepcad gave us witnesses for the problem then the variable isn't used)
						instantiationCommands.append("apply (rule allI)\n" );
					}
				} else {
					//can't handle it, bail out
					break;
				}
				proverGoal = qt.getUnderlyingExpression();
			} else if (proverGoal instanceof IsabelleImplicationGroup) {
				//ignore assumptions, focus on conclusion
				proverGoal = ((IsabelleImplicationGroup)proverGoal).getConclusion();
			} else {
				//can't handle it
				break;
			}
		} while (true);


		if (!varValues.isEmpty()) {
			instantiationCommands.setLength(0);
			instantiationCommands.append("(* ERROR: the Isabelle goal is not in a form where all variables can be instantiated. \n" +
					" * (Did you convert to PNF?)\n" +
					" * \n" +
			" * QEPCAD reports that:\n");
			for (String v : varValues.keySet()) {
				instantiationCommands.append(" *    "+v+" = "+varValues.get(v)+"\n");
			}
			instantiationCommands.append(" *)" );
		}
		else {
			instantiationCommands.append("apply simp\n" );
		}

		return instantiationCommands.toString();
	}

	public static boolean atLeastOneAssumptionSelected(QepcadProblem prob){
		int s =	prob.normalizationAssumptions.size();
		if (s > 0) {return true; }
		else return false;
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
		String qepcadResult = ((QepcadProcess)lastSuccessfulInvocation).getResult();
		QepcadProblem lpr = ((QepcadProcess)lastSuccessfulInvocation).getProblem();

		if( qepcadResult.equalsIgnoreCase("true") &&
		    view.getImportComposite().getConclusionIfSelected()!=null ){
			massageCommand.append("apply blast \n");

			return massageCommand.toString();
		}	

		if(qepcadResult.equalsIgnoreCase("false")){
			// conclusion not selected
			if(view.getImportComposite().getConclusionIfSelected()==null){
				//if vars were all free then the assumptions contradict and we can prove anything
				if(varsAllFree(lpr))
				massageCommand.append("apply blast");
			}
			// no else as we do nothing if we sent the conclusion
			return massageCommand.toString();
		}

		//else {
			// We must have returned a simplified formula
			// conclusion only
			//if(view.getImportComposite().getConclButton().getSelection() && 
			//	view.getImportComposite()	){
				
			//}
			//return massageCommand.toString();
		//}

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
		//TODO super part
		lastSuccessfulInvocation = invocation;
		
		if (!(invocation instanceof QepcadProcess)) return;
		
		QepcadProcess qepcadInvocation = (QepcadProcess) invocation;
		if (qepcadInvocation.getExecutionMode()==ExecutionMode.NORMAL)
			updateFinishTab();
		else if (qepcadInvocation.getExecutionMode()==ExecutionMode.COUNTER_EXAMPLE)
			updateFinishTabForCounterExample();
		else {
			System.err.println("Unknown execution mode: "+qepcadInvocation.getExecutionMode()+"; finish tab will not be updated");								
		}
	}
	
	public void updateFinishTab() {
		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) {
			resetFields();
			return;
		}

		QepcadProcess qep = ((QepcadProcess)lastSuccessfulInvocation);
		String bindings = qep.getProblem().getVariableBindingString();
		String qepcadInputToDisplay =
			bindings+"\n"+"[ "+qep.getProblem().goal+" ]";
		//bindings.indexOf("(E")>=0 ? bindings+"\n"+"[ "+qep.getProblem().goal+" ]" :
		//	 qep.getProblem().goal;
		//TODO include normalization assupmtions above rather than message below
		toolInputStyledText.setText(
				(qep.isInputManual() ? "(modified input script, originally based on problem below)\n\n" :
					view.getPreviewComposite().hasManualChanges() ? "(input script manually changed, originally based on problem below)\n\n" :
						qep.getProblem().normalizationAssumptions.isEmpty() ? "" : "(script included normalization assumptions not shown below)\n\n")+				
						convertFromRealTextToDisplayText( qepcadInputToDisplay, toolInputStyledText )
		);

		toolOutputStyledText.setText(
				convertFromRealTextToDisplayText( qep.getAnnotatedResult(), toolOutputStyledText ));

		//set counterexample button when applicable
		if (qep.getAnnotatedResult().trim().equalsIgnoreCase("false") && 
				varsAllUniversallyBound(qep.getProblem())){
			showCounterExampleButton();
		}
		else hideToolButtonsComposite();

		proofCommandButtons.get("Oracle").getButton().setEnabled(true);
		proofCommandButtons.get("Subgoal").getButton().setEnabled(true);

		if(!view.getPreviewComposite().hasManualChanges()){
			buttonInsert.setEnabled(true);
		}
		else buttonInsert.setEnabled(false);

		includeComments.setEnabled(true);

		if ((lastSuccessfulInvocation instanceof QepcadProcess) && 
				((QepcadProcess)lastSuccessfulInvocation).getWitnessList()!=null && 
				!((QepcadProcess)lastSuccessfulInvocation).getWitnessList().isEmpty()) {
			proofCommandButtons.get("Instantiate").getButton().setEnabled(true);
			if (view.getImportComposite().isEverythingSelected() &&
					!(view.getProblemComposite().hasManualChanges()) &&
					!(view.getPreviewComposite().hasManualChanges()) &&
					!(view.getImportComposite().hasManualChanges()) &&
					!(view.getStartComposite().hasManualChanges()) &&
					proofCommandButtons.get("Instantiate").getButton().isEnabled()) {
				selectProofCommandRadioButton("Instantiate");
			} 
		} else {
			proofCommandButtons.get("Instantiate").getButton().setEnabled(false);
			if (proofCommandButtons.get("Instantiate").getButton().getSelection()) {
				selectProofCommandRadioButton(null);
			}
		}

		refreshProofScriptCommandStyledText();
	}
	
 	public boolean varsAllUniversallyBound(QepcadProblem problem){ 	
 		int numVars = problem.variablesAndBindings.length;
		for (int i=0; i<numVars; i++) {
			if (!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.ALL))
			    return false;
		}
		 
		return true;
 	}
 	
 	public boolean varsAllFree(QepcadProblem problem){	
 		int numVars = problem.variablesAndBindings.length;
		for (int i=0; i<numVars; i++) {
			if (!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.FREE))
			    return false;
		}
		 
		return true;
 	}
	
	
	public void updateFinishTabForCounterExample() {
		if (lastSuccessfulInvocation instanceof QepcadProcess) {
			if ( ((QepcadProcess)lastSuccessfulInvocation).getWitnessMessage()!=null ) {
//			if (lastProcessCompleted.witnessList!=null && !lastProcessCompleted.witnessList.isEmpty()){		
				((FinishTabComposite)view.getFinishComposite()).showCounterExampleButton();
				
				for (ButtonWrapper bw : proofCommandButtons.values())
					bw.getButton().setEnabled(false);

				buttonInsert.setEnabled(false);
				includeComments.setEnabled(false);
				
				proofCommandStyledText.setText("");
				
				toolOutputStyledText.setText(
						"FALSE" + "\n\n" + 
						((QepcadProcess)lastSuccessfulInvocation).getWitnessMessage().replaceAll("witness", "counterexample"));
			}
		}

	}
//	
//	
//	
//	
//	
//	@Override
//	protected void createTabMainArea(Composite parent) {
//		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
//		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		
//		createQepcadOutputComposite(sashForm);
//		createProofCommandComposite(sashForm);
////		sashForm.setWeights(new int[] { 2, 3 });
//	}
//	
//	@Override
//	protected void createBottomButtonsRow(Composite composite) {
//		//no bottom buttons row
//	}
//
//	private void createQepcadOutputComposite(Composite parent) {		
//		toolOutputGroupAndButtons = new Group(parent, SWT.NONE);
//		toolOutputGroupAndButtons.setLayout(new GridLayout(2, false));
//		toolOutputGroupAndButtons.setText("QEPCAD Result");
//
//		SashForm sash = new SashForm(toolOutputGroupAndButtons, SWT.HORIZONTAL);
//		sash.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));		
//		
//		Composite lsg = new Composite(sash, SWT.NONE);
//		GridLayout gl = new GridLayout(2, false);
//		gl.marginHeight = 0;
//		gl.marginWidth = 0;
//		gl.horizontalSpacing = sash.SASH_WIDTH;
//		lsg.setLayout(gl);
//
//		toolInputStyledText = new EasyScrollingStyledText(lsg, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
//		toolInputStyledText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
//		toolInputStyledText.setEditable(false);
//		toolInputStyledText.setWordWrap(true);
//
//		Label l = new Label(lsg, SWT.NONE);
//		l.setText("=");
//		l.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, true));
//
//		toolOutputStyledText = new EasyScrollingStyledText(sash, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
////		qepcadOutputStyledText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
//		toolOutputStyledText.setEditable(false);
//		toolOutputStyledText.setWordWrap(true);
//		
//		//showCounterExampleButton();
//		//hideQepcadButtonsComposite();
//		        
////		sash.setWeights(new int[] { 4, 3 });
//	}
//	
//	/** adds a composite to the right of the external tool (top-half) part of the widget,
//	 * for buttons.  buttons can then be added.  toolOutputGroupAndButtons.layout will be needed afterwards. */
//	protected Composite enableAndGetToolButtonsComposite() {
//		if (toolButtonsComposite!=null) return toolButtonsComposite;
//		
//		toolOutputGroupAndButtons.setLayout(new GridLayout(2, false));
//		toolButtonsComposite = new Composite(toolOutputGroupAndButtons, SWT.NONE);
//		toolButtonsComposite.setLayout(newGridLayout(1, 0, 1));
//		toolButtonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
//		
//		return toolButtonsComposite;
//	}
//	
//	protected void hideToolButtonsComposite() {
//		if (toolButtonsComposite==null) return;
//		toolButtonsComposite.dispose();
//		toolButtonsComposite = null;
//		toolOutputGroupAndButtons.setLayout(new GridLayout(1, false));
//		toolOutputGroupAndButtons.layout();
//	}
//
//	
//	public static abstract class ButtonWrapper {
//		public ButtonWrapper(Composite parent, String text, int style) {
//			button = new Button(parent, style);
//			button.setText(text);
//			button.addSelectionListener(new SelectionListener() {
//				public void widgetDefaultSelected(SelectionEvent e) {
//				}
//				public void widgetSelected(SelectionEvent e) {
//					onSelection();
//				}
//				
//			});
//		}
//		Button button = null;
//		public Button getButton() {
//			return button;
//		}
//		public abstract void onSelection();
//	}
//	
//	public void showCounterExampleButton() {
//		ButtonWrapper cex = new ButtonWrapper(enableAndGetToolButtonsComposite(), "Find\nCounter-\nexample", SWT.PUSH) {
//			@Override
//			public void onSelection() {
//				try {
//					if (lastSuccessfulInvocation instanceof QepcadProcess) {
//					//TODO - negate problem (giving EX form) and find witness which will be counterexample to original problem
//						findCounterExample( ((QepcadProcess)lastSuccessfulInvocation).getProblem() );
//					} else {
//						System.err.println("counter example button should not have been enabled, no invocation available");;						
//						getButton().setEnabled(false);
//					}
//				} catch(Exception ex){
//					ex.printStackTrace();
//				}
//			}			
//		};
//		cex.getButton().setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));		
//		cex.getButton().setEnabled(true);
//		toolOutputGroupAndButtons.layout();
//	}
//
//
//	protected void createProofCommandComposite(Composite parent) {
//		Group proofCommandGroup = new Group(parent, SWT.NONE);		
//		proofCommandGroup.setLayout(new GridLayout());
//		
//		proofCommandGroup.setText("Proof Command");
//		proofCommandStyledText = new EasyScrollingStyledText(proofCommandGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();		
//		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.heightHint = 30;
//		proofCommandStyledText.setLayoutData(gd);
//		
//		createProofCommandButtonsRow(proofCommandGroup);
//	}
//
//	public abstract class ProofCommandButtonWrapper extends ButtonWrapper {
//		public ProofCommandButtonWrapper(Composite parent, String text) {
//			super(parent, text, SWT.RADIO);
//			proofCommandButtons.put(text, this);
//		}
//		public void onSelection() {
//			proofCommandStyledText.setText(
//					convertFromRealTextToDisplayText( getCurrentProofCommand().trim(), proofCommandStyledText ));
//		}
//		public abstract String getCurrentProofCommand();
//	}
//
//	protected void createProofCommandButtonsRow(Composite parent) {
//		Composite row = new Composite(parent, SWT.NONE);
//		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		row.setLayout(newGridLayout(20, 1, 1));
//		addProofCommandButtons(row);
//		row.setLayout(newGridLayout(row.getChildren().length, 1, 1));
//	}
//	
//	protected void addProofCommandButtons(Composite row) {
//		
//		createProofCommandModeRadioButtons(row);
//				
//		Label l = new Label(row, SWT.NONE);
//		l.setText("");
//		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		
//		createCommentsToggleButton(row);
//		
//		l = new Label(row, SWT.NONE);
//		l.setText("");
//		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		l = new Label(row, SWT.NONE);
//		l.setText("");
//		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		
//		createInsertCommandButton(row);
//		
//	}
//	
//	public String getWarningIfPreviewTabManualllyChanged(){
//		if(view.getPreviewComposite().hasManualChanges()){
//			return "Warning! Preview Tab has manual changes. May not be safe to trust the result in Isabelle.\n\n";
//		}
//		return "";
//	}
//	
//	public String getSubgoalWarningIfPreviewTabManualllyChanged(){
//		if(view.getPreviewComposite().hasManualChanges()){
//			return "Warning! Preview Tab has manual changes. Subgoal may never be proved in Isabelle.\n\n";
//		}
//		return "";
//	}
//
//	protected void createProofCommandModeRadioButtons(Composite row) {
//		new ProofCommandButtonWrapper(row, "Oracle") {
//			@Override
//			public String getCurrentProofCommand() {
//				return getWarningIfPreviewTabManualllyChanged()+
//                 getProverCommentForCurrentResultIfEnabled()+"\n"+
//				"apply (qepcad \"" +getProverFormOfCurrentResult() +"\" )\n" +
//				 massageGoalIfNoManualChanges();
//			}			
//		}.getButton().setSelection(true);
//				
//		new ProofCommandButtonWrapper(row, "Subgoal") {
//			@Override
//			public String getCurrentProofCommand() {
//				return getSubgoalWarningIfPreviewTabManualllyChanged()+
//				getProverCommentForCurrentResultIfEnabled()+"\n"+
//				"apply (subgoal_tac \"" +getProverFormOfCurrentResult()+"\" )";
//			}			
//		}.getButton().setSelection(false);
//				
//		new ProofCommandButtonWrapper(row, "Instantiate") {
//			@Override
//			public String getCurrentProofCommand() {
//				return getProverCommentForCurrentResultIfEnabled()+"\n"+
//				computeProverInstantiateCommandText();
//			}			
//		}.getButton().setSelection(false);
//	}
//
//	protected void createCommentsToggleButton(Composite row) {
//		includeComments = new ButtonWrapper(row, "Comment", SWT.CHECK) {
//			@Override
//			public void onSelection() {
//				refreshProofScriptCommandStyledText();
//			}
//		}.getButton();
//		includeComments.setSelection(false);
//	}
//
//	protected void createInsertCommandButton(Composite row) {
//		buttonInsert = new ButtonWrapper(row, "Insert in Proof Script", SWT.NONE) {
//			@Override
//			public void onSelection() {
//				try{
//					ProofGeneralScriptingUtils.insertInProofScript(
//							convertFromDisplayTextToRealText( proofCommandStyledText.getText(), proofCommandStyledText ), 
//							view.actionRunAfterInsert!=null && view.actionRunAfterInsert.isChecked());
//				}catch(InterruptedException ex){
//					ex.printStackTrace();
//					//	throw ex;
//				}
//			}			
//		}.getButton();
//		buttonInsert.setEnabled(true);
//	}
//
//	protected void refreshProofScriptCommandStyledText() {
//		//update the comment
//		for (ButtonWrapper b : proofCommandButtons.values())
//			if (b.getButton().getSelection())
//				b.onSelection();
//	}
//
//	public String getProverFormOfCurrentResult(){
//		String outputExternalForm = lastSuccessfulInvocation.getResult();
//
//		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) return "";
//		QepcadProblem inputQP = ((QepcadProcess)lastSuccessfulInvocation).getProblem();
//		
//		// NORM ASSMS ....  LHS  ...to qepcad ... qepcad says   RHS
//		// now send   translate( ASSMS ==> ( LHS = RHS ) )   to prover
//		// (or send   translate( ASSMS ) ==> ( translate (LHS) = translate (RHS) )   to prover...
//		
//		List<MathsExpression> assumptionsInCommon = new ArrayList<MathsExpression>();
//		for (String assm : inputQP.normalizationAssumptions)
//			assumptionsInCommon.add(
//					external().toCommon( external().parse( assm ) ));
//					
//		VariableBinding[] binds = inputQP.variablesAndBindings;
//		
//		//set 'orderNonStandard' if the order is not:  implicit existentials, implicit universals, then explicit vars
////		boolean implicitsDone = false;
////		boolean implicitExistentialsDone = false;
////		boolean orderNonStandard = false;
////		for (VariableBinding vb : binds) {
////			if (!vb.isImplicit) implicitsDone = true;
////			else {
////				if (implicitsDone) orderNonStandard = true;
////				if (vb.bindingType != BindingType.EXISTS) {
////					implicitExistentialsDone = true;
////				} else {
////					if (implicitExistentialsDone) orderNonStandard = true;
////				}
////			}
////		}
////		if (orderNonStandard) {
////			//if order is non-standard then mark all bindings as explicit
////			//(because the implicit bindings will not be accurate)
////			VariableBinding[] bindsOrig = binds;
////			binds = new VariableBinding[bindsOrig.length];
////			for (int i=0; i<bindsOrig.length; i++) {
////				binds[i] = new VariableBinding(
////						bindsOrig[i].varName, bindsOrig[i].bindingType, false);
////			}
////		}
//		
//		//mark all vars are explicitly quantified 
//		VariableBinding[] bindsOrig = binds;
//		binds = new VariableBinding[bindsOrig.length];
//		for (int i=0; i<bindsOrig.length; i++) {
//			binds[i] = new VariableBinding(
//					bindsOrig[i].varName, bindsOrig[i].bindingType, false);
//		}
//		
//		//insert quantifications for all bindings 
//		MathsExpression quantifiedInputQP = CommonMathsLanguage.getProverParsedWithQuantifications(binds, 
//				external().toCommon( external().parse( inputQP.goal )), 
//				false, false);
//		
//		MathsExpression resultCommon = CommonMathsLanguage.getEqualsUnderAssumptions(
//				assumptionsInCommon, 
//				quantifiedInputQP, 
//				external().toCommon( external().parse( outputExternalForm ) ));
//
//		String result = prover().toText( prover().fromCommon( resultCommon ) );
//		if (result==null) return "";
//				
//		result = prover().annotateWithTypeInformation(result, binds);
//		
//		return result;
//	}
//	
//	protected void findCounterExample(QepcadProblem problem){
//		translateToExistentialForm(problem);
//		runTranslatedProblem(problem, ExecutionMode.COUNTER_EXAMPLE);
//		view.getPreviewComposite().activateTab();
//	}
//	
//	public boolean runTranslatedProblem(QepcadProblem problem, ExecutionMode mode) {	
//		try {			
//			view.getPreviewComposite().runInBackground( new QepcadProcess(problem, mode) );
//		} catch (Exception e) {
//			view.getPreviewComposite().
//				setOutputText("(The current settings are not valid QEPCAD input. \n" +
//					"Check that all fields are instantiated. \n" +
//					"The error reported was: "+e+".)\n");
//			e.printStackTrace();
//		}
//		return true;
//	}
//	
//	public void translateToExistentialForm(QepcadProblem problem){
//		int numVars = problem.variablesAndBindings.length;
//		for (int i=0; i<numVars; i++) {
//			if (!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.ALL) &&
//					!problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.FREE))
//				//shouldn't happen but guard against it
//				throw new IllegalStateException("This problem cannot be inverted to a purely existential form.");
//				
//			problem.variablesAndBindings[i].bindingType = VariableBinding.BindingType.EXISTS;			
//		}
//		
//		//TODO might be introducing too many brackets
//		problem.goal = "[ ~ [ "+problem.goal+" ] ]";
//	}
//
//	public boolean isResultValid() {
//		if (view==null) return false;
//		if (lastSuccessfulInvocation==null) return false;
//		if (lastSuccessfulInvocation.getResult()==null) return false;
//		
//		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) return false;
//		
//		return true;
//	}
//
//	public String computeProverInstantiateCommandText(){
//		if (!isResultValid()) return "";
//		QepcadProcess qep = (QepcadProcess) lastSuccessfulInvocation;
//		if (qep.getWitnessList()==null || qep.getWitnessList().isEmpty() ) 
//			return "(no instantiations suggested)";
//		
//		StringBuffer instantiationCommands = new StringBuffer();
//		
//		if (!view.getImportComposite().isEverythingSelected() || 
//				view.getProblemComposite().hasManualChanges() ||
//				view.getPreviewComposite().hasManualChanges() ||
//				view.getImportComposite().hasManualChanges()  ||
//				view.getStartComposite().hasManualChanges())
//			instantiationCommands.append("(* instantiations may not be valid as problem is modified. *)\n\n");
//
//		Map<String,String> varValues = new LinkedHashMap<String,String>();			
//		VariableBinding[] vars = qep.getProblem().variablesAndBindings;			
//		List<String> witnesses = qep.getWitnessList();
//		int i=0;
//		while (i<vars.length && i<witnesses.size()) {
//			varValues.put(vars[i].varName, witnesses.get(i));
//			i++;
//		}
//
//		MathsExpression proverGoal = prover().parse(view.getStartComposite().getProverProblemTextLastApplied());
//
//		//first the implicit existentials
//
//		//TODO better would be to restore every implicit existential to have its question mark here
//		//(instead we do it from the string)
//		boolean foundImplicits = false;
//		//we only care about the conclusion
//		//(and using TrueE we can't replace the assumptions anyway)			
//		if ((proverGoal instanceof IsabelleImplicationGroup) && 
//				(((IsabelleImplicationGroup)proverGoal).getOperator().equals(MathsExpressions.newToken("==>", true))) )
//			proverGoal = ((IsabelleImplicationGroup)proverGoal).getConclusion();
//		String proverGoalString = " " + proverGoal.toString() + " ";
//		for (VariableBinding b : qep.getProblem().variablesAndBindings) {				
//			if (b.isImplicit && proverGoalString.matches(".*\\?"+b.varName+"\\b.*")) {
//				String value = varValues.remove(b.varName);
//				if (value==null)
//					//qepcad says any value will do
//					value="0";
//				foundImplicits = true;
//				proverGoalString = proverGoalString.replaceAll("\\?"+b.varName+"\\b", value);
//			}
//		}
//		if (foundImplicits) {
//			//TODO much better would be something like exI... apply (?x=0)
//
//			//the TrueI is to get rid of 'True' introduced here (there might be a better way)
//			instantiationCommands.append("apply (rule_tac P=\"" + proverGoalString.trim() +"\" in TrueE, rule TrueI)\n" );
//		}
//
//		//now the explicit existentials				
//		do {
//			if (proverGoal instanceof QuantificationOperatorGroup) {			
//				QuantificationOperatorGroup qt = (QuantificationOperatorGroup) proverGoal;
//				List<MathsToken> varsHere = qt.getQuantifiedVars();
//				BindingType quantType = prover().getBindingTypeFromQuantificationWord(qt.getQuantificationWord());
//				if (BindingType.EXISTS.equals( quantType )) {
//					for (MathsToken v : varsHere) {							
//						String vw = v.getToken();
//						if (vw.startsWith("?")) vw = vw.substring(1);
//						vw = varValues.remove( vw );
//						if (vw!=null) {
//							//add command to instantiate vw next
//							instantiationCommands.append("apply (rule_tac x=\"" + vw +"\" in exI)\n" );
//						} else {
//							//no witness returned... presumably it can be anything so set to 0
//							//TODO add comment that value doesn't matter
//							instantiationCommands.append("apply (rule_tac x=\"0\" in exI)\n" );
//						}
//					}
//				} else if (BindingType.ALL.equals( quantType )) {
//					for (@SuppressWarnings("unused")
//							MathsToken v : varsHere) {
//						//apply rule allI
//						//(if qepcad gave us witnesses for the problem then the variable isn't used)
//						instantiationCommands.append("apply (rule allI)\n" );
//					}
//				} else {
//					//can't handle it, bail out
//					break;
//				}
//				proverGoal = qt.getUnderlyingExpression();
//			} else if (proverGoal instanceof IsabelleImplicationGroup) {
//				//ignore assumptions, focus on conclusion
//				proverGoal = ((IsabelleImplicationGroup)proverGoal).getConclusion();
//			} else {
//				//can't handle it
//				break;
//			}
//		} while (true);
//
//
//		if (!varValues.isEmpty()) {
//			instantiationCommands.setLength(0);
//			instantiationCommands.append("(* ERROR: the Isabelle goal is not in a form where all variables can be instantiated. \n" +
//					" * (Did you convert to PNF?)\n" +
//					" * \n" +
//			" * QEPCAD reports that:\n");
//			for (String v : varValues.keySet()) {
//				instantiationCommands.append(" *    "+v+" = "+varValues.get(v)+"\n");
//			}
//			instantiationCommands.append(" *)" );
//		}
//		else {
//			instantiationCommands.append("apply arith\n" );
//		}
//
//		return instantiationCommands.toString();
//	}
//
//	public static boolean atLeastOneAssumptionSelected(QepcadProblem prob){
//		int s =	prob.normalizationAssumptions.size();
//		if (s > 0) {return true; }
//		else return false;
//	}
//
//	
//	public String massageGoalIfNoManualChanges(){
//		
//		if(!(view.getStartComposite().hasManualChanges()) && 
//				!(view.getProblemComposite().hasManualChanges()) &&
//				!(view.getPreviewComposite().hasManualChanges()) && 
//				!(view.getFinishComposite().hasManualChanges())){
//			return massageGoal();
//		}
//		
//		//manual changes must have been applied so no need to massage goal --- just return a space
//		return "";
//	}
//	
//	public String massageGoal(){
//		StringBuffer massageCommand = new StringBuffer();
//		
//		//TODO - don't take from output styled text now - take from data object?
//		String qepcadResult = ((QepcadProcess)lastSuccessfulInvocation).getResult();
//		QepcadProblem lpr = ((QepcadProcess)lastSuccessfulInvocation).getProblem();
//
//		if( qepcadResult.equalsIgnoreCase("true") &&
//		    view.getImportComposite().getConclButton().getSelection() ){
//			massageCommand.append("apply blast \n");
//
//			return massageCommand.toString();
//		}	
//
//		if(qepcadResult.equalsIgnoreCase("false")){
//			// conclusion not selected
//			if(!view.getImportComposite().getConclButton().getSelection()){
//				//if vars were all free then the assumptions contradict and we can prove anything
//				if(varsAllFree(lpr))
//				massageCommand.append("apply blast");
//			}
//			// no else as we do nothing if we sent the conclusion
//			return massageCommand.toString();
//		}
//
//		//else {
//			// We must have returned a simplified formula
//			// conclusion only
//			//if(view.getImportComposite().getConclButton().getSelection() && 
//			//	view.getImportComposite()	){
//				
//			//}
//			//return massageCommand.toString();
//		//}
//
//		return massageCommand.toString();
//	}	
//
//	public String getProverCommentForCurrentResultIfEnabled() {
//		if (!includeComments.getSelection()) return "";
//		return getProverCommentForCurrentResult();
//	}
//	
//	public String getProverCommentForCurrentResult() {
//		//FIXME isabelle only
//		if (lastSuccessfulInvocation instanceof ProversPaletteAbstractExternalProcess)
//			return "(* "+view.getName()+" ran with the following input: \n"+
//				((ProversPaletteAbstractExternalProcess)lastSuccessfulInvocation).getInput()+
//				"*)";
//		return "(* comment details not available *)";
//	}
//
//	public void resetFields() {
//		toolInputStyledText.setText("");
//		toolOutputStyledText.setText("");
//		proofCommandStyledText.setText("");
//		
//		selectProofCommandRadioButton(null);
//
//		includeComments.setSelection(false);
//		lastSuccessfulInvocation = null;
//	}
//
//	protected void selectProofCommandRadioButton(String label) {
//		if (label==null) label = "Oracle";
//		
//		for (ButtonWrapper bi : proofCommandButtons.values())
//			bi.getButton().setSelection( bi.getButton().getText().equals(label) );
//	}
//
//	@Override
//	protected int computeDataHashCode() {
//		return -1;
//	}
//
//	@Override
//	public void update() {
//		resetFields();
//		noteUpdateCompleted();
//	}
//
//	ProversPaletteExternalInvocation lastSuccessfulInvocation = null;
//	
//	public void updateOnInvocationSuccess(ProversPaletteExternalInvocation invocation) {
//		//TODO super part
//		lastSuccessfulInvocation = invocation;
//		
//		if (!(invocation instanceof QepcadProcess)) return;
//		
//		QepcadProcess qepcadInvocation = (QepcadProcess) invocation;
//		if (qepcadInvocation.getExecutionMode()==ExecutionMode.NORMAL)
//			updateFinishTab();
//		else if (qepcadInvocation.getExecutionMode()==ExecutionMode.COUNTER_EXAMPLE)
//			updateFinishTabForCounterExample();
//		else {
//			System.err.println("Unknown execution mode: "+qepcadInvocation.getExecutionMode()+"; finish tab will not be updated");								
//		}
//	}
//
//	public void updateFinishTab() {		
//		if (!(lastSuccessfulInvocation instanceof QepcadProcess)) {
//			resetFields();
//			return;
//		}
//
//		QepcadProcess qep = ((QepcadProcess)lastSuccessfulInvocation);
//		String bindings = qep.getProblem().getVariableBindingString();
//		String qepcadInputToDisplay =
//			bindings+"\n"+"[ "+qep.getProblem().goal+" ]";
//		//bindings.indexOf("(E")>=0 ? bindings+"\n"+"[ "+qep.getProblem().goal+" ]" :
//		//	 qep.getProblem().goal;
//		//TODO include normalization assupmtions above rather than message below
//		toolInputStyledText.setText(
//				(qep.isInputManual() ? "(modified input script, originally based on problem below)\n\n" :
//					view.getPreviewComposite().hasManualChanges() ? "(input script manually changed, originally based on problem below)\n\n" :
//						qep.getProblem().normalizationAssumptions.isEmpty() ? "" : "(script included normalization assumptions not shown below)\n\n")+				
//						convertFromRealTextToDisplayText( qepcadInputToDisplay, toolInputStyledText )
//		);
//
//		toolOutputStyledText.setText(
//				convertFromRealTextToDisplayText( qep.getAnnotatedResult(), toolOutputStyledText ));
//
//		//set counterexample button when applicable
//		if (qep.getAnnotatedResult().trim().equalsIgnoreCase("false") && 
//				varsAllUniversallyBound(qep.getProblem())){
//			showCounterExampleButton();
//		}
//		else hideToolButtonsComposite();
//
//		proofCommandButtons.get("Oracle").getButton().setEnabled(true);
//		proofCommandButtons.get("Subgoal").getButton().setEnabled(true);
//
//		if(!view.getPreviewComposite().hasManualChanges()){
//			buttonInsert.setEnabled(true);
//		}
//		else buttonInsert.setEnabled(false);
//
//		includeComments.setEnabled(true);
//
//		if ((lastSuccessfulInvocation instanceof QepcadProcess) && 
//				((QepcadProcess)lastSuccessfulInvocation).getWitnessList()!=null && 
//				!((QepcadProcess)lastSuccessfulInvocation).getWitnessList().isEmpty()) {
//			proofCommandButtons.get("Instantiate").getButton().setEnabled(true);
//			if (view.getImportComposite().isEverythingSelected() &&
//					!(view.getProblemComposite().hasManualChanges()) &&
//					!(view.getPreviewComposite().hasManualChanges()) &&
//					!(view.getImportComposite().hasManualChanges()) &&
//					!(view.getStartComposite().hasManualChanges()) &&
//					proofCommandButtons.get("Instantiate").getButton().isEnabled()) {
//				selectProofCommandRadioButton("Instantiate");
//			} 
//		} else {
//			proofCommandButtons.get("Instantiate").getButton().setEnabled(false);
//			if (proofCommandButtons.get("Instantiate").getButton().getSelection()) {
//				selectProofCommandRadioButton(null);
//			}
//		}
//
//		refreshProofScriptCommandStyledText();
//	}
//	
// 	public boolean varsAllUniversallyBound(QepcadProblem problem){ 	
// 		int numVars = problem.variablesAndBindings.length;
//		for (int i=0; i<numVars; i++) {
//			if (problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.ALL))
//			    return true;
//		}
//		 
//		return false;
// 	}
// 	
// 	public boolean varsAllFree(QepcadProblem problem){	
// 		int numVars = problem.variablesAndBindings.length;
//		for (int i=0; i<numVars; i++) {
//			if (problem.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.FREE))
//			    return true;
//		}
//		 
//		return false;
// 	}
//	
//	
//	public void updateFinishTabForCounterExample() {
//		if (lastSuccessfulInvocation instanceof QepcadProcess) {
//			if ( ((QepcadProcess)lastSuccessfulInvocation).getWitnessMessage()!=null ) {
////			if (lastProcessCompleted.witnessList!=null && !lastProcessCompleted.witnessList.isEmpty()){		
//				view.getFinishComposite().showCounterExampleButton();
//				
//				for (ButtonWrapper bw : proofCommandButtons.values())
//					bw.getButton().setEnabled(false);
//
//				buttonInsert.setEnabled(false);
//				includeComments.setEnabled(false);
//				
//				proofCommandStyledText.setText("");
//				
//				toolOutputStyledText.setText(
//						"FALSE" + "\n\n" + 
//						((QepcadProcess)lastSuccessfulInvocation).getWitnessMessage().replaceAll("witness", "counterexample"));
//			}
//		}
//
//	}

}
