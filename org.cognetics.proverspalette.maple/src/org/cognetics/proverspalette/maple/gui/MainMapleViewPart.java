package org.cognetics.proverspalette.maple.gui;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.maple.MapleTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.eclipse.swt.SWT;

public class MainMapleViewPart extends ProversPaletteViewPartAbstract {

	

	@Override
	public String getName() {
		return "Maple";
	}

	private MathsSystemTranslator currentSystemTranslator = new MapleTranslator();
	
	public MathsSystemTranslator getCurrentExternalSystemTranslator() {
		return currentSystemTranslator;
	}

	protected void createTabs() {
		startComposite = new StartTabComposite(this, "Start", SWT.NONE);
		importComposite = new ImportTabComposite(this, "Import", SWT.NONE);
		problemComposite = new ProblemTabComposite(this, "Problem", SWT.NONE);
		previewComposite = new PreviewTabComposite(this, "Preview", SWT.NONE); 
		finishComposite = new FinishTabComposite(this, "Finish", SWT.NONE);
	}
	
}
