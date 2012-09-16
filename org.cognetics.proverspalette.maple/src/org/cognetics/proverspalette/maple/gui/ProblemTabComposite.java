package org.cognetics.proverspalette.maple.gui;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.maple.MapleProblem;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.heneveld.javautils.swtui.EasyScrollingStyledText;
import org.heneveld.javautils.swtui.EasyTableManager;
import org.heneveld.maths.structs.MathsExpression;

public class ProblemTabComposite extends
		org.cognetics.proverspalette.base.gui.ProblemTabComposite {
	

	public EasyTableManager<VariableBinding> varsTable = null;
	public EasyTableManager<String[]> assumptionsTable = null;
//	public StyledText goalsSetupStyledText = null;
//	public StyledText goalsQueryStyledText = null;
	public StyledText descriptionText = null;
	
	public Group modeGroup = null;
	public Combo modeCombo = null;
		
	public ProblemTabComposite(MainMapleViewPart view, String tabLabel, int style) {
		super(view, tabLabel, style);
		createContents();
	}

	@Override
	protected void createTabMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(newGridLayout(1, 0, 0));
		
		//createTranslationDropdownComposite();
		createDescriptionGroupAtTop(composite);
		createSashCompositeAtBottom(composite);
	}

	private void createDescriptionGroupAtTop(Composite composite) {
		Group group = new Group(composite, SWT.NONE);
		group.setText("Problem Description (optional)");
		group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group.setLayout(new GridLayout(1, false));
		
		descriptionText = new EasyScrollingStyledText(group, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 35;
		descriptionText.setLayoutData(gd);
	}

	private void createSashCompositeAtBottom(Composite composite) {
		SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createModeSashLhs(sashForm);
		//createGoalsAssmsSashRhs(sashForm);
		//sashForm.setWeights(new int[] { 1, 2 });
	}
	
	private void createModeSashLhs(SashForm sashForm) {
		SashForm lhsSash = new SashForm(sashForm, SWT.NONE);
		lhsSash.setOrientation(SWT.VERTICAL);
		
		Group g = new Group(lhsSash, SWT.NONE);
		g.setText("Maple Mode");
		
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout gridLayoutMode = new GridLayout();
		gridLayoutMode.verticalSpacing = 1;
		g.setLayout(gridLayoutMode);
				
		modeCombo = new Combo(g, SWT.SINGLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		modeCombo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				getProblemMode().doUpdate(true);
			}			
		});
		
		modeGroup = new Group(g, SWT.BORDER);		
		gridLayoutMode = new GridLayout();
		modeGroup.setLayout(gridLayoutMode);
		modeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//Group groupMode = new Group(parent, SWT.NONE);
		modeGroup.setLayout(gridLayoutMode);
		modeGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//groupMode.setText("Maple Mode");

		new ProblemTabModeImplicitPlot(this).register();
		new ProblemTabModeAssumeAndCheck(this).register();
		new ProblemTabModePlot2d(this).register();
		
//		plot
//		
//		mapleAssumeAndCheck = new Button(groupMode, SWT.RADIO);
//		mapleAssumeAndCheck.setText("Assume and Check"); 
//		mapleAssumeAndCheck.setEnabled(true);
//		mapleAssumeAndCheck.setSelection(true);
//		
//		mapleSimplify = new Button(groupMode, SWT.RADIO);
//		mapleSimplify.setText("Simplify");
//		mapleSimplify.setEnabled(true);
//		
//		mapleFactor = new Button(groupMode, SWT.RADIO);
//		mapleFactor.setText("Factor");
//		mapleFactor.setEnabled(true);
//		
//		mapleEvaluate = new Button(groupMode, SWT.RADIO);
//	    mapleEvaluate.setText("Evaluate");
//	    mapleEvaluate.setEnabled(true);
//	    
//		mapleManual = new Button(groupMode, SWT.RADIO);
//		mapleManual.setText("Manual");
//		mapleManual.setEnabled(true);
				
	}


	//private void createGoalsAssmsSashRhs(SashForm sashForm) {
	//	SashForm rhsSash = new SashForm(sashForm, SWT.NONE);
	//	rhsSash.setOrientation(SWT.VERTICAL);
		
	//	Group group1 = new Group(rhsSash, SWT.NONE);
	//	group1.setText("Goal Setup");
	//	group1.setLayout(new GridLayout(1, false));
		
//		goalsSetupStyledText = 
//			new EasyScrollingStyledText(group1, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
//		goalsSetupStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		goalsSetupStyledText.setWordWrap(true);

//		Group group2 = new Group(rhsSash, SWT.NONE);
//		group2.setText("Goal Query");
//		group2.setLayout(new GridLayout(1, false));
		
//		goalsQueryStyledText = 
//			new EasyScrollingStyledText(group2, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
//		goalsQueryStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		goalsQueryStyledText.setWordWrap(true);

//		rhsSash.setWeights(new int[] { 2, 1 });
//	}

	
	
	public void resetFields(){
		selectedAssumptionsCommon = null;
		selectedConclusionCommon = null;
		selectedEverythingCommon = null;
		descriptionText.setText("");
		 //goalsSetupStyledText.setText("");
		 //goalsQueryStyledText.setText("");
	}
	
	public boolean isProblemAvailable() {
		ProblemTabMode mode = getProblemMode();
		if (mode==null) return false;
		String query = mode.getGoalQuery();
		if (query==null || query.trim().length()==0) return false;
		return true;
		//if (goalsQueryStyledText.getText().trim().length()==0) return false;		
		//return true;
	}
	
	public void contributeSettingsToMapleProblem(MapleProblem prob){
		prob.mode = getProblemMode();
		prob.goalSetup = prob.mode.getGoalSetup();
		prob.goalQuery = prob.mode.getGoalQuery();
//		prob.goalSetup = convertFromDisplayTextToRealText( goalsSetupStyledText.getText(),goalsSetupStyledText );
//		prob.goalQuery = convertFromDisplayTextToRealText( goalsQueryStyledText.getText(), goalsQueryStyledText );		
	}
	
	int dataHashCodeLastApplied = -1;

	protected Map<String,ProblemTabMode> knownModes = new LinkedHashMap<String, ProblemTabMode>();
	List<MathsExpression> selectedAssumptionsCommon;
	MathsExpression selectedConclusionCommon;
	MathsExpression selectedEverythingCommon;

	public ProblemTabMode getProblemMode() {
		if (modeCombo==null)
			return new ProblemTabMode.NullProblemMode(this, "Invalid", "Problem tab has not been configured.");
		ProblemTabMode m = knownModes.get(modeCombo.getText());
		if (m==null)
			return new ProblemTabMode.NullProblemMode(this, "Invalid", "Problem mode not selected or not known.");
		return m;
	}
	
	protected int computeDataHashCode() {
		int hashCode = descriptionText.getText().hashCode();
		hashCode = hashCode * 193; 
		ProblemTabMode mode = getProblemMode();
		if (mode!=null) {
			String query = mode.getGoalQuery();
			if (query==null || query.trim().length()==0) hashCode += 37 * query.hashCode();
			String setup = mode.getGoalSetup();
			if (setup==null || setup.trim().length()==0) hashCode += 37 * setup.hashCode();
		}
		return hashCode;
	}
	
	public void update() {
		descriptionText.setText(
				"created from "+view.getCurrentProverType()+" subgoal at "+
				DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime())
			);
		super.update();
	}
	
	@Override
	protected void updateWith(List<MathsExpression> assmsCommon,
			MathsExpression conclusionItem) {
		ProblemTabMode oldPM = getProblemMode();
		
		//set mode
		if (oldPM instanceof ProblemTabModeImplicitPlot) {
			//don't change
		} else {
			//default strategy
			modeCombo.setText(ProblemTabModeImplicitPlot.NAME);			
		}
		
		this.selectedAssumptionsCommon = assmsCommon;
		this.selectedConclusionCommon = conclusionItem;
		this.selectedEverythingCommon = createExpressionFromAssumptionsAndConclusion(assmsCommon, conclusionItem);
		
		ProblemTabMode newPM = getProblemMode();
		newPM.doUpdate(oldPM != newPM);
		
		noteUpdateCompleted();
	}

	/** returns index of item, or -1 if not present */
	public <T> int indexInSet(Set<T> set, T target) {
		if (set instanceof HashSet) {
			if (!set.contains(target))
				return -1;
		}
		int i=0;		
		for (T item : set) {
			if ((item!=null && item.equals(target)) || (item==null && target==null))
				return i;
			i++;
		}
		return -1;
	}

	public static <T> int countIntersection(Set<T> pv, T[] selection) {
		int count=0;
		for (T i : selection) {
			if (pv.contains(i)) count++;
		}
		return count;
	}

	public Button getFinishButton() {
		return buttonFinish;
	}
	
	@Override
	public void applyChanges() {
		super.applyChanges();
	}
	
	@Override
	public MathsSystemTranslator external() {
		return super.external();
	}
	
	@Override
	public MathsProverTranslator prover() {
		return super.prover();		
	}
	
	public ProversPaletteViewPartAbstract getView() {
		return view;
	}

	
	
}
