package org.cognetics.proverspalette.maple.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.translation.CommonMathsLanguage;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableType;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.cognetics.proverspalette.translation.VariableType.PossibleTypes;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.heneveld.javautils.swtui.EasyScrollingComposite;
import org.heneveld.javautils.swtui.EasyTableColumn;
import org.heneveld.javautils.swtui.EasyTableManager;
import org.heneveld.javautils.swtui.MoveableRowsTableColumn;
import org.heneveld.maths.structs.ImplicationInterface;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public class ImportTabComposite extends
org.cognetics.proverspalette.base.gui.ImportTabComposite {

	private Composite mainComposite = null;
	//public Button simpAssm = null;
	//public Button simp = null;
	//public Button solve = null;

	private Composite compositeAssumptions = null;
	private Composite compositeConclusion = null;
	// public EasyTableManager<VariableType> varsTypesTable = null;

	private Button conclButton = null;

	public ImportTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
		createContents();
	}

	@Override
	protected void createTabMainArea(Composite composite) {
		mainComposite = new Composite(composite, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainComposite.setLayout(newGridLayout(1, 0, 0));		

		SashForm mainSash = new SashForm(mainComposite, SWT.VERTICAL);
		mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createConclusionsGroup(mainSash);		
		createAssmsVarsSashForm(mainSash);

		mainSash.setWeights(new int[] { 2, 3 });
	}

	protected void createAssmsVarsSashForm(Composite parent) {
		SashForm sashCompositeBottom = new SashForm(parent, SWT.HORIZONTAL);
//		sashCompositeBottom.setLayout(new GridLayout(2, false));

		createAssumptionsGroup(sashCompositeBottom);
//		createVarsTypesGroup(sashCompositeBottom);

//       sashCompositeBottom.setWeights(new int[] { 1, 2 });
	}

	protected FillLayout newFillLayoutWithMargin() {
		FillLayout fl = new FillLayout();
		fl.marginHeight = 2; fl.marginWidth = 2;
		return fl;
	}


	protected void createConclusionsGroup(Composite parent){
		Group groupCG = new Group(parent, SWT.NONE);
		groupCG.setLayout(newFillLayoutWithMargin());		
		groupCG.setText("Conclusion of Prover Goal");

		compositeConclusion = new EasyScrollingComposite.EasyScrollingColumnLayoutComposite(groupCG);
		final ColumnLayout colLayout = ((EasyScrollingComposite.EasyScrollingColumnLayoutComposite)compositeConclusion).columnLayout;
		colLayout.minNumColumns = 1;
		colLayout.maxNumColumns = 1;
		colLayout.topMargin = 10;
		colLayout.verticalSpacing = 10;
		colLayout.horizontalSpacing = 30;
		compositeConclusion.setBackground(new Color(Display.getCurrent(), new RGB(255, 255, 255)));

		conclButton = makePropositionButton(compositeConclusion);
		conclButton.setVisible(false);
	}

	protected void createAssumptionsGroup(SashForm parent){
		Group groupAG = new Group(parent, SWT.V_SCROLL);
		groupAG.setLayout(newFillLayoutWithMargin());
		groupAG.setText("Assumptions From Isabelle Goal");

		compositeAssumptions = new EasyScrollingComposite.EasyScrollingColumnLayoutComposite(groupAG);
		final ColumnLayout colLayout = ((EasyScrollingComposite.EasyScrollingColumnLayoutComposite)compositeAssumptions).columnLayout;
		colLayout.minNumColumns = 1;
		colLayout.maxNumColumns = 2;
		colLayout.topMargin = 10;
		colLayout.verticalSpacing = 10;
		colLayout.horizontalSpacing = 30;
		compositeAssumptions.setBackground(new Color(Display.getCurrent(), new RGB(255, 255, 255)));
	}

//	protected void createVarsTypesGroup(SashForm parent){
//
//		Group groupVT = new Group(parent, SWT.NONE);
//		GridLayout gld = new GridLayout(1, false);
//		gld.horizontalSpacing = 1;
//		groupVT.setLayout(gld); //newMarginnedFillLayout());
//		groupVT.setText("Variable Types");
//
//		varsTypesTable = new EasyTableManager<VariableType>()
//		.configureHeadings(true).configureLines(true)
//		.addColumn(new MoveableRowsTableColumn<VariableType>(new VariableType("", VariableType.PossibleTypes.UNKNOWN),
//				new Callable<VariableType>() {	
//			public VariableType call() {
//				return new VariableType("variable", VariableType.PossibleTypes.UNKNOWN);
//			}
//		}))
//		.addColumn(new EasyTableColumn<VariableType>("Name") {
//			@Override
//			public String getColumnDisplayTextFromRowData(VariableType element) {
//				return element.varName;
//			}
//			@Override
//			public boolean setValue(VariableType rowData, Object newValue) {
//				rowData.varName = newValue.toString();
//				return true;
//			}
//		})
//		.addColumn(new EasyTableColumn<VariableType>("Type", 80) {
//			@Override
//			public String getColumnDisplayTextFromRowData(VariableType element) {
//				return element.varType.getSimpleLabel();
//			}
//			@Override
//			public Object getColumnModifierInput(VariableType element) {
//				String text = super.getColumnModifierInput(element).toString();
//				for (int i=0; i<PossibleTypes.getAllSimpleLabels().length; i++)
//					if (PossibleTypes.getAllSimpleLabels()[i].equalsIgnoreCase(text))
//						return i;
//				for (int i=0; i<PossibleTypes.getAllNiceLabels().length; i++)
//					if (PossibleTypes.getAllNiceLabels()[i].equalsIgnoreCase(text))
//						return i;
//				return 0;
//			}
//			@Override
//			public CellEditor createCellEditor(Table table) {
//				ComboBoxCellEditor cce = new ComboBoxCellEditor(table, PossibleTypes.getAllNiceLabels());
//				return cce;
//			}
//			@Override
//			public boolean setValue(VariableType rowData, Object newValue) {	
//				rowData.varType = VariableType.PossibleTypes.getTypeForVariable( PossibleTypes.getAllNiceLabels()[(Integer)newValue] );
//				return true;
//			}
//		});
//		varsTypesTable.createTable(groupVT);		
//		varsTypesTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//	}

	public Control[] getCurrentAssumptionComponents() {		
		return compositeAssumptions.getChildren();
	}

	MathsProverTranslator newCurrentTranslator = null;

	public void updateWithSubgoal(MathsExpression proverGoal, Map<String, VariableBinding> vars){
		MathsProverTranslator translator = view.getCurrentProverTranslator();
		if (translator!=null) newCurrentTranslator = translator;

		resetFields();

		MathsExpression cleanedProverGoal = proverGoal;
		//remove quantifications, but not if any vars are invalid because then they are useful to see
		boolean allVarsAreValid = true;
		for (VariableBinding v : vars.values()) {
			if (v.bindingType == BindingType.UNKNOWN) {
				allVarsAreValid = false;
				break;
			}
		}
		if (allVarsAreValid)
			cleanedProverGoal = translator.stripQuantifications(cleanedProverGoal, false);

		//TODO could strip :: type specs here... 
		MathsExpression conclusion = cleanedProverGoal;

		List<MathsExpression> assumptions = new ArrayList<MathsExpression>();
		if (cleanedProverGoal instanceof ImplicationInterface) {
			conclusion = ((ImplicationInterface)cleanedProverGoal).getConclusion();
			List<MathsExpression> assumptionGroupElements = ((ImplicationInterface)cleanedProverGoal).getAssumptions();
			for (MathsExpression assm : assumptionGroupElements)
				assumptions.addAll(prover().getConjuncts(assm));			
		}

		conclButton.setText( convertFromRealTextToDisplayText(conclusion.toString(), conclButton) );
		conclButton.setSelection( external().isProverExpressionCompatible(conclusion, translator) &&
				areAllVarsValid(conclusion, translator, vars) );
		conclButton.setVisible(true);		

		for (MathsExpression a : assumptions){		
			Button assum = makePropositionButton(compositeAssumptions);
			assum.setText( convertFromRealTextToDisplayText( a.toString(), assum ));
			assum.setSelection( external().isProverExpressionCompatible(a, translator) &&
					areAllVarsValid(a, translator, vars) );
		}

		layout(true, true);

		this.varsFromIsabelleGoal = new LinkedHashMap<String, VariableBinding>(vars);
		varsUserBindings = new LinkedHashMap<String, VariableBinding>();
		for (String v : varsFromIsabelleGoal.keySet()) {
			VariableBinding vb = varsFromIsabelleGoal.get(v);
			varsUserBindings.put(v, new VariableBinding(vb.varName, vb.bindingType, vb.isImplicit));
		}

	}

	public static boolean areAllVarsValid(MathsExpression proverTerm,
			MathsProverTranslator translator, Map<String, VariableBinding> vars) {
		Map<String, VariableBinding> foundVars = translator.getVariables(proverTerm);
		for (String v : foundVars.keySet()) {
			if (v.startsWith("?")) v = v.substring(1);
			VariableBinding vb = vars.get(v);
			if (vb==null) return false;
			if (vb.bindingType==BindingType.UNKNOWN) return false;
		}
		return true;
	}


	//TODO not needed for maple

	/** variables from the prover goal and their bindings from the prover goal,
	 * as determined in TabUpdateManager.updateImportTab
	 * (null if update is pending) */
	private Map<String, VariableBinding> varsFromIsabelleGoal = new LinkedHashMap<String, VariableBinding>();
	/** copy of bindings available for current problem */
//	private Map<String, VariableBinding> varsFromSelectedIsabelleGoal = new LinkedHashMap<String, VariableBinding>();
	/** copy of bindings, with binding types changed as user changes them */
	private Map<String, VariableBinding> varsUserBindings = new LinkedHashMap<String, VariableBinding>();
	/** subset of userBindings excluding those not in use */
//	private Map<String, VariableBinding> varsUserSelected = new LinkedHashMap<String, VariableBinding>();

	public MathsExpression getProverSubgoalOfCurrentSelection() {		
		List<MathsExpression> assms = new ArrayList<MathsExpression>();
		for (Control b : getCurrentAssumptionComponents()) {
			if ((b instanceof Button) && ((Button)b).getSelection())
				assms.add( prover().toCommon( prover().parse( 
						convertFromDisplayTextToRealText( ((Button)b).getText(), b) )));
		}
		if (conclButton.getSelection()) {
			MathsExpression concl = prover().toCommon( prover().parse( 
					convertFromDisplayTextToRealText( conclButton.getText(), conclButton ) ) );
			if (assms.isEmpty()) return concl;
			return prover().fromCommon( CommonMathsLanguage.getProverImplication(concl, assms, false) );
		} else {
			if (assms.size()>0)
				return prover().fromCommon( CommonMathsLanguage.getProverConjunction(assms.toArray(new MathsExpression[0]) ));
			return MathsExpressions.newToken("", false);
		}				
	}		



	private Button makePropositionButton(Composite parent) {
		final Button b = new Button(parent, SWT.CHECK);
		b.setBackground( Display.getCurrent().getSystemColor(SWT.COLOR_WHITE) );
		b.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			public void widgetSelected(SelectionEvent e) {
				//TODO what needs updating when conclusion or assumption is checked/unchecked
//				updateVariableSets();
				//translator, translator.getVariables( ((Button)b).getText() ).values() );
			}
		});
		return b;
	}			

	public void resetFields() {
		for (Control b : compositeAssumptions.getChildren())
			b.dispose();
		conclButton.setVisible(false);		
		//varsTypesTable.clearData();		
	}

	protected int computeDataHashCode() {
		int hashCode = conclButton.getSelection() ? conclButton.getText().hashCode() : 97;
		for (Control c : getCurrentAssumptionComponents()) {
			if (!(c instanceof Button)) continue;
			if (!((Button)c).getSelection()) continue;
			hashCode = hashCode * 193 + ((Button)c).getText().hashCode();
		}
		return hashCode;
	}

	public void applyChangesImpl() {
		view.getPreviewComposite().resetFields();
		view.getFinishComposite().resetFields();
		view.getProblemComposite().update();
	}

	public void update() {
		String problemTextProver = ((MainMapleViewPart)view).getStartComposite().getProverProblemTextLastApplied();
		if (problemTextProver==null || problemTextProver.trim().length()==0) {
			resetFields();
			noteUpdateCompleted();
			return;
		}

		MathsExpression problemProver = view.getCurrentProverTranslator().parse(problemTextProver);
//		String qepcadText = translator.toQepcadText(sg);		
		Map<String, VariableBinding> vars = 
			view.getCurrentProverTranslator().getVariables(problemProver);

		external().ensureVariableNamesAreSafe(vars);

		updateWithSubgoal(problemProver, vars);

		applyChangesIfNecessary();
		noteUpdateCompleted();
	}		

	public Button getConclButton() {
		return conclButton;
	}

	public boolean isEverythingSelected() {		

		for (Control b : getCurrentAssumptionComponents()) {
			if ((b instanceof Button) && !((Button)b).getSelection())
				return false;
		}

		if (!conclButton.getSelection()){
			return false;
		}

		return true;
	}

	@Override
	public Set<MathsExpression> getCurrentSelectedAssumptions() {
		Set<MathsExpression> result = new LinkedHashSet<MathsExpression>();
		for (Control assmButton : getCurrentAssumptionComponents()) {
			if (!(assmButton instanceof Button)) continue;
			if (((Button)assmButton).getSelection())
				result.add(prover().parse( 
						convertFromDisplayTextToRealText( ((Button)assmButton).getText(), assmButton) ));				
		}
		return result;
	}

	@Override
	public MathsExpression getConclusionIfSelected() {
		if (!conclButton.getSelection()) return null;
		return prover().parse( convertFromDisplayTextToRealText( conclButton.getText(), conclButton ) );
	}

	public Collection<VariableBinding> getSelectedVariables() {

		return null;

//		List<VariableBinding> result = new ArrayList<VariableBinding>();
//		add the user binding, but in the order from the original isabelle goal
//		for (VariableBinding v : varsFromIsabelleGoal.values()) {
//			VariableBinding vv = varsUserSelected.get(v.varName);
//			if (vv!=null) result.add(vv);
//		}
//		return result;
	}

	



}
