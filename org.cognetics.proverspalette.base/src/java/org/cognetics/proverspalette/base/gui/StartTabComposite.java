package org.cognetics.proverspalette.base.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.heneveld.javautils.swtui.EasyScrollingStyledText;
import org.heneveld.maths.structs.GoalsRecord;
import org.heneveld.maths.structs.MathsExpression;

import ed.inf.utils.datastruct.StringManipulation;

public abstract class StartTabComposite extends ProversPaletteTabCompositeAbstract {

	protected StyledText problemText = null;
	protected Combo proverChoice = null;
	

	public StartTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
	}

	// --------------- widget gui creation ----------------------------

	protected void createTabMainArea(Composite composite) {
		Group g = new Group(composite, SWT.NONE);
		g.setText("Goal to Send to "+view.getName());
		//this shows up in text field, yuck
//		g.setToolTipText("Shows the Isabelle goal being prepared for sending to QEPCAD. " +
//				"The goal can be edited prior to sending it. (Isabelle syntax expected here.)");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g.setLayout(new GridLayout(1, false));
		
		proverChoice = new Combo(g, SWT.DROP_DOWN); 
		proverChoice.setText("ProofGeneral Session");
		proverChoice.setToolTipText("Select where the goal is coming from.");
		proverChoice.add("ProofGeneral Session", 0);

		problemText = new EasyScrollingStyledText(g, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
		problemText.setText("(uninitialized");
		problemText.setToolTipText("");
		problemText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		problemText.setEditable(true);
	}
	
	protected void createBottomLeftButtons(Composite buttons) {
		makeSubgoalLifecycleButtons(buttons);
		new Label(buttons, SWT.NONE).setText(" ");				
		makeProverTidyingButtons(buttons);
	}

	protected void makeSubgoalLifecycleButtons(Composite buttons) {
		Button b;
		b = new Button(buttons, SWT.PUSH);
		b.setText("&Manual");
		b.setToolTipText("Moves to the problem tab where you can enter a new "+view.getName()+" formula");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//create a new blank problem
				resetFields();
				//propagate the cleared data
				view.getImportComposite().update();
				view.getProblemComposite().update();
				//and show the problem tab
				view.getProblemComposite().activateTab();
			}
		});

		b = new Button(buttons, SWT.PUSH);
		b.setText("&Clear");
		b.setToolTipText("Clears the current problem but not any config settings (use this to enter a new " +
				(view.getCurrentProverType()!=null ? view.getCurrentProverType() : "prover") +
				" formula)");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				problemText.setText("");
			}
		});


		b = new Button(buttons, SWT.PUSH);
		b.setText("Re&vert");
		b.setToolTipText("Clears the problem and returns config settings to their defaults");
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				view.resetAllTabs();
				update();
			}
		});
	}

	protected void makeProverTidyingButtons(Composite buttons) {
		//can be overridden
	}

	
	protected boolean updateFields(String problemTextProver,
			MathsExpression problemProver, boolean isValid) {
		problemText.setText(convertFromRealTextToDisplayText(problemTextProver, problemText));
		buttonFinish.setEnabled( isValid );
		
		return isValid;
	}
	
	
	
	protected void updateAutoRun(boolean isValid) {	
		if (!isValid)
			//do nothing when new problem not applicable to qepcad
			return;		

		boolean finishApplied = false;
		if (view.actionAutoRun!=null && view.actionAutoRun.isChecked()) {
			view.getStartComposite().finish(true);
			finishApplied = true;
		}
		if (view.actionActivateWhenApplicable!=null && view.actionActivateWhenApplicable.isChecked()) {
			if (!finishApplied)
				view.getStartComposite().activateTab();			
			view.activateView();
		}
	}

	@Override
	public void resetFields() {
		problemText.setText("");
		buttonFinish.setEnabled(false);
//		proverChoice.setxxx
	}

	
	// -------------------- navigation and updates ----------------------
	
	public String getProverProblemTextCurrentlyDisplayed() {
		//TODO ideally have several "configurations" or a history in this tab
		//easily done now that subsequent tabs get updated from this method and the prover setting in applyChanges  		
		return convertFromDisplayTextToRealText(problemText.getText(), problemText);
	}

	GoalsRecord currentProverGoal;
	
	public void update() {
		updateWithProblem(view.getMostRecentProverProblem());
		noteUpdateCompleted();
	}
	
	public void updateWithProblem(GoalsRecord proverGoal) {
		currentProverGoal = proverGoal;
		String newProblem;
		boolean isInteresting = false;
		if (currentProverGoal==null)
			newProblem = "(no goal information)";
		else if (currentProverGoal.count==0)
			newProblem = "(no subgoals)";
		else {
			newProblem = currentProverGoal.firstSubgoal;
			isInteresting = true;
		}
		
		if (isInteresting) {
			newProblem = newProblem.replaceAll("\\n\\s+", "\n").trim();
						
			//goal from provers (e.g. isabelle) sometimes start and end with parens so strip that if unnecessary
			if (newProblem.startsWith("(") && newProblem.endsWith(")")) {
				if (StringManipulation.parenthesesMatch(newProblem.substring(1, newProblem.length()-1), false, false, false, true, true))
					newProblem = newProblem.substring(1, newProblem.length()-1);
			}

			String newProbNice = newProblem;
//			String newProbShort = newProblem;
			try {
				newProbNice = view.getCurrentProverTranslator().preprocess(newProbNice);
//				newProbShort = ProofGeneralPlugin.getSomeSessionManager().getProver().getSymbols().
//					useShortcutsAscii(newProbNice);
//				newProbNice = newProbShort;
//				//if we use unicode here, we have to convert back to plain text later
////				newProbNice = ProofGeneralPlugin.getSomeSessionManager().getProver().getSymbols().
////					useUnicodeForOutput(newProblem, false);
//				
//				
			} catch (Exception e) {
				System.err.println("unable to get prover's symbols; X-Symbols will be left unconverted");
			}

			MathsExpression problemProver = view.getCurrentProverTranslator().parse(newProbNice);

			boolean isValid = view.getCurrentExternalSystemTranslator().isProverExpressionCompatible(
					problemProver, view.getCurrentProverTranslator());
			
			updateFields(newProbNice, problemProver, isValid);			
			updateAutoRun(isValid);
		} else  {			
//			view.setProverType(null);
			resetFields();
			problemText.setText(newProblem);			
		}
	}

	protected int computeDataHashCode() {
		return problemText.getText().hashCode();
	}

	protected String lastAppliedProverText;
	
	public void applyChanges() {
		if (currentProverGoal!=null) view.setProverType(currentProverGoal.proverType);
		lastAppliedProverText = getProverProblemTextCurrentlyDisplayed();
		super.applyChanges();
	}

	public String getProverProblemTextLastApplied() {
		return lastAppliedProverText;
	}

}
