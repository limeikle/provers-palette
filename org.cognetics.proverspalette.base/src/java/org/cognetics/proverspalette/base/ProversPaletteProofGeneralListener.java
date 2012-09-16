package org.cognetics.proverspalette.base;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.heneveld.featwizpg.prover.ProverNewProofStateListener;

import ed.inf.utils.eclipse.DisplayCallable;

public class ProversPaletteProofGeneralListener extends ProverNewProofStateListener {

	ProversPaletteViewPartAbstract view = null;
	
	public ProversPaletteProofGeneralListener(ProversPaletteViewPartAbstract view) {
		this.view = view;
	}

	@Override
	public void onNewState() {
		
		//have to do this in display thread
		new DisplayCallable() {
			@Override
			public Object run() throws Exception {
				if (getGoalsRecord()==null || getGoalsRecord().parseTree==null)
					return null;
				
				System.out.println("sending new problem to prover's palette " +
						view.getName() + "widget: "+getGoalsRecord().parseTree.asXML());	
				
				view.onNewProblem(getGoalsRecord());

				return null;
			}
		}.runDisplay();
		
	}

}
