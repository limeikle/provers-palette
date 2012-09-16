package org.cognetics.proverspalette.base.gui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cognetics.proverspalette.base.ProversPaletteProofGeneralListener;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.cognetics.proverspalette.translation.isabelle.IsabelleTranslator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.ViewPart;
import org.heneveld.maths.structs.GoalsRecord;

public abstract class ProversPaletteViewPartAbstract extends ViewPart {

	protected Composite parent;
	
	protected TabFolder mainTabFolder;
	
	protected StartTabComposite startComposite;
	protected ImportTabComposite importComposite;
	protected ProblemTabComposite problemComposite;
	protected PreviewTabComposite previewComposite;
	protected FinishTabComposite finishComposite;	
	
	protected ProversPaletteProofGeneralListener listener;

	public abstract String getName();

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		this.parent = parent;
		mainTabFolder = new TabFolder(parent, SWT.NONE);		
		
		createTabs(); 

		listenerForProofGeneralStart();		
		resetAllTabs();
        createDropDownMenu();

		//TODO warning on edit
		//TODO var bindings:  A E F G C k
		//TODO var order, bound must be same order, and free must come first (?)
		//TODO offer advice on optimisations, var ordering, etc...		
	}

	/** should create the tabs needed for this widget (tabs appear in the order they are created here);
	 *  tab contents should typically extend AbstractProversPaletteTabComposite */
	protected abstract void createTabs();
	
	protected Action actionRunAfterInsert;
	protected Action actionActivateWhenApplicable;
	protected Action actionActivateWhenAutoRunSuccess;
	protected Action actionAutoRun;

	/** creates the dropdown menu in the toolbar of the view,
	 * offering 'run after insert', 'show when applicable' 'run automatically', and 'show on success' */ 
	protected void createDropDownMenu() {
		IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
				
		actionRunAfterInsert = new Action("Run after &Insert", IAction.AS_CHECK_BOX) {
			public void run() {}
		};		
		actionRunAfterInsert.setChecked(true);
		mgr.add(actionRunAfterInsert);

		mgr.add(new Separator());

		actionActivateWhenApplicable = new Action("Show &when Applicable", IAction.AS_CHECK_BOX) {
			public void run() {}
		};		
		mgr.add(actionActivateWhenApplicable);

		mgr.add(new Separator());
		
		actionAutoRun = new Action("&Run Automatically", IAction.AS_CHECK_BOX) {
			public void run() {}
		};		
		mgr.add(actionAutoRun);
		
		actionActivateWhenAutoRunSuccess = new Action("Show on &Success", IAction.AS_CHECK_BOX) {
			public void run() {}
		};		
		mgr.add(actionActivateWhenAutoRunSuccess);		
	}

	
	protected void listenerForProofGeneralStart() {
		//link to prover
		listener = new ProversPaletteProofGeneralListener(this);
		listener.startListeningWithDuplicates();
	}

	protected void listenerForProofGeneralStop() {
		if (listener!=null) listener.stopListening();
	}

	@Override
	public void dispose() {
		try {
			listenerForProofGeneralStop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.dispose();
	}

	@Override
	public void setFocus() {
		//required by ViewPart
		//don't need to do anything special
	}

	// ------------------ tab maintenance --------------------------

	/** creates a new 'folder' and 'item' for the tab (the thing which gets displayed,
	 * and the little tab which is always visible, respectively);
	 * for internal use only (in AbstractProversPaletteTabComposite
	 */
	Composite newTabFolder(String labelText) {
		TabItem ti = new TabItem(mainTabFolder, SWT.NONE);
		ti.setText(labelText);
		
		final Composite newTab = new Composite(mainTabFolder, SWT.NONE);
		newTab.setLayout(new FillLayout());		
		ti.setControl(newTab);
		return newTab;
	}

	private final Map<String,ProversPaletteTabCompositeAbstract> tabs =
		new LinkedHashMap<String,ProversPaletteTabCompositeAbstract>();
	
	/** automatically invoked by AbstractProversPaletteTabComposite
	 * so that the view is aware of all tabs */
	void registerComposite(String tabLabel,
			ProversPaletteTabCompositeAbstract tabComposite) {
		tabs.put(tabLabel, tabComposite);
	}

	/** returns an unmodifiable map of the tabs in this view, by name and in order */
	public Map<String,ProversPaletteTabCompositeAbstract> tabs() {
		return Collections.unmodifiableMap(tabs);
	}
	
	/** resets all tabs, apart from the start tab */
	public void resetAllTabs() {
		for (ProversPaletteTabCompositeAbstract tab : tabs().values()) {
			tab.resetFields();
		}
	}

	// ------------------ current settings -------------------
	
	protected GoalsRecord mostRecentProverGoals = null;
	
	public void onNewProblem(GoalsRecord goalsRecord) {
		this.mostRecentProverGoals = goalsRecord;
		getStartComposite().update();
	}
	
	public GoalsRecord getMostRecentProverProblem() {
		// TODO look it up if null ... and make sure StartTabComposite.update() is invoked when view is created
		// (so that the widget doesn't start blank if started after ProofGeneral has been running)
		return mostRecentProverGoals;
	}

	// ----------------- prover and system ---------------------------
	
	private String currentProverType = null;
	private MathsProverTranslator currentProverTranslator = null;

	public abstract MathsSystemTranslator getCurrentExternalSystemTranslator();
	
	public void setProverType(String proverType) {
		if ((proverType==null && currentProverType!=null) ||
				(proverType!=null && !proverType.equals(currentProverType))) {
			currentProverType = proverType;
			currentProverTranslator = getTranslatorForProverType(proverType);
		}
	}
	
	public String getCurrentProverType() {
		return currentProverType;
	}
	
	public MathsProverTranslator getCurrentProverTranslator() {
		if (currentProverTranslator==null)
			currentProverTranslator = getTranslatorForProverType(currentProverType);
		return currentProverTranslator;
	}
	
	protected MathsProverTranslator getTranslatorForProverType(String proverType) {
		MathsProverTranslator translator;
		if (proverType==null) {
			System.err.println("no prover type set; assuming isabelle as prover");
			translator = new IsabelleTranslator();
		} else if ("isabelle".equalsIgnoreCase(proverType)) {
			translator = new IsabelleTranslator();
		} else {
			System.err.println("unknown prover type '"+proverType+"' set; will use isabelle as prover");
			translator = new IsabelleTranslator();
		}		
		return translator;
	}
	
	
	// --------------------- conveniences -----------------------------------	
	
	public StartTabComposite getStartComposite() {
		return startComposite;
	}
	
	public ImportTabComposite getImportComposite() {
		return importComposite;
	}
	
	public ProblemTabComposite getProblemComposite() {
		return problemComposite;
	}

	public PreviewTabComposite getPreviewComposite() {
		return previewComposite;
	}
	
	public FinishTabComposite getFinishComposite() {
		return finishComposite;
	}

	/** convenience to make the view appear */
	public void activateView() {
		getSite().getPage().activate(this);
	}


}
