package org.cognetics.proverspalette.base.gui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.cognetics.proverspalette.base.ProofGeneralScriptingUtils;
import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.heneveld.javautils.swtui.EasyScrollingStyledText;

public abstract class FinishTabComposite extends ProversPaletteTabCompositeAbstract {

	public FinishTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
	}
	
	protected StyledText toolInputStyledText = null;
	protected StyledText toolOutputStyledText = null;
	
	protected StyledText proofCommandStyledText = null;
		
	protected Map<String,ButtonWrapper> proofCommandButtons = new LinkedHashMap<String,ButtonWrapper>();
	protected Button includeComments = null;
	protected Button buttonInsert = null;	

	protected Group toolOutputGroupAndButtons;
	private Composite toolButtonsComposite = null;

	@Override
	protected void createTabMainArea(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createExternalToolOutputComposite(sashForm);
		createProofCommandComposite(sashForm);
//		sashForm.setWeights(new int[] { 2, 3 });
	}
	
	@Override
	protected void createBottomButtonsRow(Composite composite) {
		//no bottom buttons row
	}

	protected void createExternalToolOutputComposite(Composite parent) {		
		toolOutputGroupAndButtons = new Group(parent, SWT.NONE);
		toolOutputGroupAndButtons.setLayout(new GridLayout(2, false));
		toolOutputGroupAndButtons.setText(view.getName()+" Result");

		SashForm sash = new SashForm(toolOutputGroupAndButtons, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));		
		
		Composite lsg = new Composite(sash, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.horizontalSpacing = sash.SASH_WIDTH;
		lsg.setLayout(gl);

		toolInputStyledText = new EasyScrollingStyledText(lsg, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
		toolInputStyledText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		toolInputStyledText.setEditable(false);
		toolInputStyledText.setWordWrap(true);

		Label l = new Label(lsg, SWT.NONE);
		l.setText("=");
		l.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, true));

		toolOutputStyledText = new EasyScrollingStyledText(sash, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
//		qepcadOutputStyledText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		toolOutputStyledText.setEditable(false);
		toolOutputStyledText.setWordWrap(true);
		
		//showCounterExampleButton();
		//hideQepcadButtonsComposite();
		        
//		sash.setWeights(new int[] { 4, 3 });
	}
	
	/** adds a composite to the right of the external tool (top-half) part of the widget,
	 * for buttons.  buttons can then be added.  toolOutputGroupAndButtons.layout will be needed afterwards. */
	protected Composite enableAndGetToolButtonsComposite() {
		if (toolButtonsComposite!=null) return toolButtonsComposite;
		
		toolOutputGroupAndButtons.setLayout(new GridLayout(2, false));
		toolButtonsComposite = new Composite(toolOutputGroupAndButtons, SWT.NONE);
		toolButtonsComposite.setLayout(newGridLayout(1, 0, 1));
		toolButtonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		return toolButtonsComposite;
	}
	
	protected void hideToolButtonsComposite() {
		if (toolButtonsComposite==null) return;
		toolButtonsComposite.dispose();
		toolButtonsComposite = null;
		toolOutputGroupAndButtons.setLayout(new GridLayout(1, false));
		toolOutputGroupAndButtons.layout();
	}

	
	public static abstract class ButtonWrapper {
		public ButtonWrapper(Composite parent, String text, int style) {
			button = new Button(parent, style);
			button.setText(text);
			button.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					onSelection();
				}
				
			});
		}
		Button button = null;
		public Button getButton() {
			return button;
		}
		public abstract void onSelection();
	}
	
	protected void createProofCommandComposite(Composite parent) {
		Group proofCommandGroup = new Group(parent, SWT.NONE);		
		proofCommandGroup.setLayout(new GridLayout());
		
		proofCommandGroup.setText("Proof Command");
		proofCommandStyledText = new EasyScrollingStyledText(proofCommandGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 30;
		proofCommandStyledText.setLayoutData(gd);
		
		createProofCommandButtonsRow(proofCommandGroup);
	}

	public abstract class ProofCommandButtonWrapper extends ButtonWrapper {
		public ProofCommandButtonWrapper(Composite parent, String text) {
			super(parent, text, SWT.RADIO);
			proofCommandButtons.put(text, this);
		}
		public void onSelection() {
			proofCommandStyledText.setText(
					convertFromRealTextToDisplayText( getCurrentProofCommand().trim(), proofCommandStyledText ));
		}
		public abstract String getCurrentProofCommand();
	}

	protected void createProofCommandButtonsRow(Composite parent) {
		Composite row = new Composite(parent, SWT.NONE);
		row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		row.setLayout(newGridLayout(20, 1, 1));
		addProofCommandButtons(row);
		row.setLayout(newGridLayout(row.getChildren().length, 1, 1));
	}
	
	protected void addProofCommandButtons(Composite row) {
		
		createProofCommandModeRadioButtons(row);
				
		Label l = new Label(row, SWT.NONE);
		l.setText("");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		createCommentsToggleButton(row);
		
		l = new Label(row, SWT.NONE);
		l.setText("");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		l = new Label(row, SWT.NONE);
		l.setText("");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		createInsertCommandButton(row);
		
	}
	
	protected abstract void createProofCommandModeRadioButtons(Composite row);

	public String getWarningIfPreviewTabManualllyChanged(){
		if(view.getPreviewComposite().hasManualChanges()){
			return "Warning! Preview Tab has manual changes. May not be safe to trust the result.\n\n";
		}
		return "";
	}
	
	public String getSubgoalWarningIfPreviewTabManualllyChanged(){
		if(view.getPreviewComposite().hasManualChanges()){
			return "Warning! Preview Tab has manual changes. Subgoal may not be useful.\n\n";
		}
		return "";
	}


	protected void createCommentsToggleButton(Composite row) {
		includeComments = new ButtonWrapper(row, "Comment", SWT.CHECK) {
			@Override
			public void onSelection() {
				refreshProofScriptCommandStyledText();
			}
		}.getButton();
		includeComments.setSelection(false);
	}

	protected void createInsertCommandButton(Composite row) {
		buttonInsert = new ButtonWrapper(row, "Insert in Proof Script", SWT.NONE) {
			@Override
			public void onSelection() {
				try{
					ProofGeneralScriptingUtils.insertInProofScript(
							convertFromDisplayTextToRealText( proofCommandStyledText.getText(), proofCommandStyledText ), 
							view.actionRunAfterInsert!=null && view.actionRunAfterInsert.isChecked());
				}catch(InterruptedException ex){
					ex.printStackTrace();
					//	throw ex;
				}
			}			
		}.getButton();
		buttonInsert.setEnabled(true);
	}

	protected void refreshProofScriptCommandStyledText() {
		//update the comment
		for (ButtonWrapper b : proofCommandButtons.values())
			if (b.getButton().getSelection())
				b.onSelection();
	}

	/** may return null if not valid */
	public String getProverFormOfCurrentResult(){
		if (lastSuccessfulInvocation==null) return null;
		return lastSuccessfulInvocation.getResult();
	}
	
	public boolean isResultValid() {
		if (view==null) return false;
		if (lastSuccessfulInvocation==null) return false;
		if (lastSuccessfulInvocation.getResult()==null) return false;
		return true;
	}

	public String getProverCommentForCurrentResultIfEnabled() {
		if (!includeComments.getSelection()) return "";
		return getProverCommentForCurrentResult();
	}
	
	public abstract String getProverCommentForCurrentResult();

	public void resetFields() {
		toolInputStyledText.setText("");
		toolOutputStyledText.setText("");
		proofCommandStyledText.setText("");
		
		selectProofCommandRadioButton(null);

		includeComments.setSelection(false);
		lastSuccessfulInvocation = null;
	}

	protected void selectProofCommandRadioButton(String label) {
		if (label==null) label = "Oracle";
		
		for (ButtonWrapper bi : proofCommandButtons.values())
			bi.getButton().setSelection( bi.getButton().getText().equals(label) );
	}

	@Override
	protected int computeDataHashCode() {
		return -1;
	}

	@Override
	public void update() {
		resetFields();
		noteUpdateCompleted();
	}

	protected ProversPaletteExternalInvocation lastSuccessfulInvocation = null;

	public abstract void updateOnInvocationSuccess(ProversPaletteExternalInvocation invocation);

	public abstract void updateFinishTab();
	


}
