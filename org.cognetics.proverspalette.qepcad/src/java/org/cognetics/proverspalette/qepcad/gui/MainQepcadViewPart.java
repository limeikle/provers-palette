package org.cognetics.proverspalette.qepcad.gui;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.qepcad.QepcadTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.eclipse.swt.SWT;

public class MainQepcadViewPart extends ProversPaletteViewPartAbstract {

	@Override
	public String getName() {
		return "QEPCAD";
	}
	
	private ConfigTabComposite configComposite;

	private MathsSystemTranslator currentSystemTranslator = new QepcadTranslator();
	
	public MathsSystemTranslator getCurrentExternalSystemTranslator() {
		return currentSystemTranslator;
	}

	protected void createTabs() {
		startComposite = new StartTabComposite(this, "Start", SWT.NONE);
		importComposite = new ImportTabComposite(this, "Import", SWT.NONE);
		problemComposite = new ProblemTabComposite(this, "Problem", SWT.NONE);
		configComposite = new ConfigTabComposite(this, "Config", SWT.NONE);
		previewComposite = new PreviewTabComposite(this, "Preview", SWT.NONE); 
		finishComposite = new FinishTabComposite(this, "Finish", SWT.NONE);
	}

//	public TabFolder getMainTabFolder() {
//		return mainTabFolder;
//	}
	
	public ConfigTabComposite getConfigComposite() {
		return configComposite;
	}
	
//	public void getProblemDescriptionText() {
//		getProblemComposite().descriptionText.getText();
//	}
	
}
