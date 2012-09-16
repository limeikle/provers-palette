/**
 * 
 */
package org.cognetics.proverspalette.maple.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public abstract class ProblemTabMode {

	private final ProblemTabComposite problemTabComposite;
	public ProblemTabMode(ProblemTabComposite problemTabComposite, String name, String description) {
		this.problemTabComposite = problemTabComposite;
		this.name = name;
		this.description = description;
	}
	final String name, description;
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	/** if true, 'quit' is not added to the script and maple will remain open (e.g. for plots) */
	public boolean shouldLeaveSessionOpen() {
		return false;
	}
	public void onImportUpdated() {
		updateGoalWidgets();
	}
	public abstract String getGoalSetup();
	public abstract String getGoalQuery();
	public void doUpdate(boolean reset) {
		if (reset) {
			removeOldControlWidgets();
			addControlWidgets();
		}
		updateControlWidgets();	
		updateGoalWidgets();
		this.problemTabComposite.modeGroup.getParent().layout(true, true);
	}
	public void updateControlWidgets() {			
	}
	public void removeOldControlWidgets() {
		for (Control c : this.problemTabComposite.modeGroup.getChildren())
			c.dispose();			
	}
	protected Label modeDescription = null;
	/** override/extend if extra control widgets are needed */
	public void addControlWidgets() {
		if (getDescription()==null || getDescription().length()==0) {
			//nothing
			modeDescription = null;
		} else {
			Composite c = this.problemTabComposite.modeGroup;
			//				Composite c = new Composite(modeGroup, SWT.BORDER);
			//				c.setLayout(new FillLayout());

			//TODO wrapping is off!!!

			modeDescription = new Label(c, SWT.WRAP);
			modeDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			updateDescription(getDescription(), getDescription());
		}
	}
	protected void updateDescription(String description, String toolTip) {
		if (modeDescription!=null && !modeDescription.isDisposed()) {
			modeDescription.setText(description);
			modeDescription.setToolTipText(toolTip);
		}
	}
	public void updateGoalWidgets() {
		//this.problemTabComposite.goalsSetupStyledText.setText( ProblemTabComposite.convertFromRealTextToDisplayText(getGoalSetup(), this.problemTabComposite.goalsSetupStyledText) );
		//this.problemTabComposite.goalsQueryStyledText.setText( ProblemTabComposite.convertFromRealTextToDisplayText(getGoalQuery(), this.problemTabComposite.goalsQueryStyledText) );			
		this.problemTabComposite.getFinishButton().setEnabled( this.problemTabComposite.isProblemAvailable());
		this.problemTabComposite.applyChanges();
	}
	public void register() {
		this.problemTabComposite.modeCombo.add(getName());
		this.problemTabComposite.knownModes.put(getName(), this);
	}

	public static class NullProblemMode extends ProblemTabMode {
		@SuppressWarnings("unused")
		private final ProblemTabComposite problemTabComposite;
		public NullProblemMode(ProblemTabComposite problemTabComposite, String name, String description) {
			super(problemTabComposite, name, description);
			this.problemTabComposite = problemTabComposite;
		}
		@Override
		public String getGoalQuery() {
			return "";
		}
		@Override
		public String getGoalSetup() {
			return "";
		}
		@Override
		public void onImportUpdated() {
		}		
	}
}