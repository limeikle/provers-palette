package org.cognetics.proverspalette.base.gui;

import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation;

public abstract class PreviewTabComposite extends ProversPaletteTabCompositeAbstract {

	//TODO much of the subclass implementation of this class could be moved up to this class
	
	public PreviewTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
	}

	public abstract boolean go(boolean forceInterrupt);

	public abstract void runInBackground(ProversPaletteExternalInvocation qepcadProcess);

	public abstract void setOutputText(String string);

	protected void autoActivateAfterRunning(ProversPaletteExternalInvocation invocation) {
		if (view.actionActivateWhenAutoRunSuccess!=null && view.actionActivateWhenAutoRunSuccess.isChecked()) {
			if (invocation.getResult()==null)
				//fail
				return;
			if (!invocation.isResultInteresting())
				//nothing useful
				return;
			//show if successful
			showFinishTab();
			view.activateView();				
		}
	}	

	public void showFinishTab() {
		view.getFinishComposite().activateTab();
	}

}
