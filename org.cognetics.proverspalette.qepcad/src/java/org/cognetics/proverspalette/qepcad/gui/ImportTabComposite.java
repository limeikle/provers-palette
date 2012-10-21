package org.cognetics.proverspalette.qepcad.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.translation.CommonMathsLanguage;
import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.heneveld.javautils.swtui.EasyScrollingComposite;
import org.heneveld.maths.structs.ImplicationInterface;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public class ImportTabComposite extends org.cognetics.proverspalette.base.gui.ImportTabComposite {

	private Composite mainComposite = null;
	public Button simpAssm = null;
	public Button simp = null;
	public Button solve = null;

	private Composite compositeAssumptions = null;
	private Composite compositeConclusion = null;
	
	private Button conclButton = null;
	private org.eclipse.swt.widgets.List varsEliminateListWidget;
	private org.eclipse.swt.widgets.List varsFreeListWidget;
	private org.eclipse.swt.widgets.List varsUnusedListWidget;
	private Button moveVarLeftButton;
	private Button moveVarRightButton;

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
		createEliminateVarsGroup(sashCompositeBottom);
		
		sashCompositeBottom.setWeights(new int[] { 1, 2 });
	}

	protected FillLayout newFillLayoutWithMargin() {
		FillLayout fl = new FillLayout();
		fl.marginHeight = 2; fl.marginWidth = 2;
		return fl;
	}
	
	
	protected void createConclusionsGroup(Composite parent){
		Group groupCG = new Group(parent, SWT.NONE);
		groupCG.setLayout(newFillLayoutWithMargin());		
		groupCG.setText("Conclusion of Isabelle Goal");
		
		compositeConclusion = new EasyScrollingComposite.EasyScrollingColumnLayoutComposite(groupCG);
		final ColumnLayout colLayout = ((EasyScrollingComposite.EasyScrollingColumnLayoutComposite)compositeConclusion).columnLayout;
		colLayout.minNumColumns = 1;
		colLayout.maxNumColumns = 1;
		colLayout.topMargin = 6;
		colLayout.verticalSpacing = 2;
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
		colLayout.maxNumColumns = 1;
		colLayout.topMargin = 6;
		colLayout.verticalSpacing = 2;
		colLayout.horizontalSpacing = 30;
		compositeAssumptions.setBackground(new Color(Display.getCurrent(), new RGB(255, 255, 255)));
	}

	protected void createEliminateVarsGroup(SashForm parent){
		Group groupEV = new Group(parent, SWT.NONE);
		GridLayout gld = new GridLayout(5, false);
		gld.horizontalSpacing = 1;
		groupEV.setLayout(gld); //newMarginnedFillLayout());
		groupEV.setText("Variables");

		Group g = new Group(groupEV, SWT.BORDER);
		g.setLayout(newFillLayoutWithMargin());
		g.setText("Eliminate (bound)");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		varsEliminateListWidget = new org.eclipse.swt.widgets.List(g, SWT.MULTI | SWT.BORDER);
		
		Composite movementButtons = new Composite(groupEV, SWT.NONE);
		movementButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		g = new Group(groupEV, SWT.BORDER);
		g.setLayout(newFillLayoutWithMargin());
		g.setText("Simplify (free)");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		varsFreeListWidget = new org.eclipse.swt.widgets.List(g, SWT.MULTI | SWT.BORDER);		
		
		//spacer
		Label spacer = new Label(groupEV, SWT.NONE);
		spacer.setText("     ");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		
		g = new Group(groupEV, SWT.BORDER);
		g.setLayout(newFillLayoutWithMargin());
		g.setText("Unused");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		varsUnusedListWidget = new org.eclipse.swt.widgets.List(g, SWT.MULTI | SWT.BORDER);

		gld = new GridLayout(1, true);
		movementButtons.setLayout(gld);
		moveVarLeftButton = new Button(movementButtons, SWT.PUSH);
		moveVarLeftButton.setText("<--");
		moveVarLeftButton.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, true));
		moveVarRightButton = new Button(movementButtons, SWT.PUSH);
		moveVarRightButton.setText("-->");
		moveVarRightButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
		
		//update button status as list selection changes
		varsEliminateListWidget.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				updateVarMovementButtons();
			}
		});
		varsFreeListWidget.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				updateVarMovementButtons();
			}			
		});
		
		moveVarLeftButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {				
				for (int i : varsFreeListWidget.getSelectionIndices()) {
					String v = varsFreeListWidget.getItem(i);
					if (v.indexOf("(")>0) v = v.substring(0, v.indexOf("(")).trim();
					VariableBinding vbOrig = varsFromSelectedIsabelleGoal.get(v);
					if (vbOrig==null) {
						System.err.println("adding unknown binding selected by user, for "+v);
						vbOrig = new VariableBinding(v, BindingType.UNKNOWN, false);
						varsFromSelectedIsabelleGoal.put(v, vbOrig);
					}
					VariableBinding vbUser = varsUserBindings.get(v);
					if (vbUser==null) {
						System.err.println("adding unknown binding selected by user, for "+v);
						vbUser = new VariableBinding(v, BindingType.UNKNOWN, vbOrig.isImplicit);
						varsUserBindings.put(v, vbUser);
						varsUserSelected.put(v, vbUser);
					}
					vbUser.bindingType = vbOrig.bindingType;
					if (!vbUser.bindingType.equals(BindingType.ALL))
						v += " ("+vbUser.bindingType.toString().toLowerCase()+")";
					
					//tricky: but basically inserts this variable in the widget list immediately after 
					//all variables which occur before this variable in the original list
					Iterator<String> origs = varsFromIsabelleGoal.keySet().iterator();
					int wi = 0;
					while (wi < varsEliminateListWidget.getItemCount() && origs.hasNext()) {
						String nextOrig = origs.next();
						if (vbUser.varName.equals(nextOrig)) break;
						if (varsEliminateListWidget.getItem(wi).split(" ")[0].equals(nextOrig)) wi++;
					}
					varsEliminateListWidget.add(v, wi);
					
				}
				varsFreeListWidget.remove(varsFreeListWidget.getSelectionIndices());
				updateVarMovementButtons();
			}			
		});
		moveVarRightButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {				
				for (int i : varsEliminateListWidget.getSelectionIndices()) {
					String v = varsEliminateListWidget.getItem(i);
					if (v.indexOf("(")>0) v = v.substring(0, v.indexOf("(")).trim();					
					VariableBinding vbOrig = varsFromSelectedIsabelleGoal.get(v);
					if (vbOrig==null) {
						System.err.println("adding unknown binding selected by user, for "+v);
						vbOrig = new VariableBinding(v, BindingType.UNKNOWN, false);
						varsFromSelectedIsabelleGoal.put(v, vbOrig);
					}
					VariableBinding vbUser = varsUserBindings.get(v);
					if (vbUser==null) {
						System.err.println("adding unknown binding selected by user, for "+v);
						vbUser = new VariableBinding(v, BindingType.UNKNOWN, false);
						varsUserBindings.put(v, vbUser);
						varsUserSelected.put(v, vbUser);
					}
					vbUser.bindingType = BindingType.FREE;

					//tricky: but basically inserts this variable in the widget list immediately after 
					//all variables which occur before this variable in the original list
					Iterator<String> origs = varsFromIsabelleGoal.keySet().iterator();
					int wi = 0;
					while (wi < varsFreeListWidget.getItemCount() && origs.hasNext()) {
						String nextOrig = origs.next();
						if (vbUser.varName.equals(nextOrig)) break;
						if (varsFreeListWidget.getItem(wi).split(" ")[0].equals(nextOrig)) wi++;
					}
					
					if (vbUser.bindingType!=BindingType.UNKNOWN && vbOrig.bindingType!=BindingType.UNKNOWN) {
						varsFreeListWidget.add(v, wi);
					} else {
						varsFreeListWidget.add(v+" ("+"invalid"+")", wi);
					}
				}
				varsEliminateListWidget.remove(varsEliminateListWidget.getSelectionIndices());
				updateVarMovementButtons();
			}			
		});
		
	}

	protected void updateVarMovementButtons() {
		moveVarRightButton.setEnabled( varsEliminateListWidget.getSelectionCount()>0 );				
		moveVarLeftButton.setEnabled( varsFreeListWidget.getSelectionCount()>0 );
	}			

	
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
		
		updateVariableSets();
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

	boolean assumptionsOnly = false;
	
	protected void updateVariableSets() {
		varsEliminateListWidget.removeAll();
		varsFreeListWidget.removeAll();
		varsUnusedListWidget.removeAll();
		String sg = getProverSubgoalOfCurrentSelection().toString();
		if (sg.length()==0) {
			for (String v : varsFromIsabelleGoal.keySet())
				varsUnusedListWidget.add(v);
			buttonFinish.setEnabled(false);
			assumptionsOnly = false;
			return;
		}
		
		varsFromSelectedIsabelleGoal = prover().getVariables( prover().parse(sg) );
		external().ensureVariableNamesAreSafe(varsFromSelectedIsabelleGoal);
		boolean valid = !varsFromSelectedIsabelleGoal.isEmpty();
		
		//if quantifications were explicit global, then they will have been removed
		//so they won't be set correctly in the selected goal.  need to check
		//the global quantifications.
		for (VariableBinding vb : varsFromSelectedIsabelleGoal.values()) {
			if (vb.isImplicit) {
				//implicit ones should be replaced by how they are defined in the original goal
				vb.bindingType = varsFromIsabelleGoal.get(vb.varName).bindingType;
			}
		}
		
		boolean assumptionsOnlyModeChanged = (conclButton.getSelection() == assumptionsOnly);
		if (assumptionsOnlyModeChanged) assumptionsOnly = !assumptionsOnly;
		
		for (String v : varsFromIsabelleGoal.keySet()) {
			//here, go through the original goal's order so we get the same order here
			if (varsFromSelectedIsabelleGoal.containsKey(v)) {
				VariableBinding vb = varsUserBindings.get(v);
				if (vb==null) {
					valid = false;
					System.err.println("adding unknown binding selected by user, for "+v);
					VariableBinding vbOrig = new VariableBinding(v, BindingType.UNKNOWN, false);
					varsFromIsabelleGoal.put(v, vbOrig);
					VariableBinding vbUser = new VariableBinding(v, BindingType.UNKNOWN, false);
					varsUserBindings.put(v, vbUser);
					varsUserSelected.put(v, vbUser);
				}
				VariableBinding vbCurrent = varsFromSelectedIsabelleGoal.get(v);
				//set this binding unknown if it is invalid for selected subgoals (e.g. x-binding in (EX x. x>0) ==> (ALL x. x>0) );
				//or clear "unknown" flag if it is now valid
				if (vbCurrent.bindingType.equals(BindingType.UNKNOWN))
					vb.bindingType = BindingType.UNKNOWN;
				else if (vb.bindingType.equals(BindingType.UNKNOWN)) {
					//if user has selected a subset which is valid,
					//restore the validity
					vb.bindingType = vbCurrent.bindingType;
				} else {
					if (assumptionsOnlyModeChanged) {
						//reset bindings so all is free when assumption-only mode is entered
						//(and restored when cleared)
						vb.bindingType = (assumptionsOnly? BindingType.FREE :
							//otherwise restore
							vbCurrent.bindingType
							);
					}
				}
				
				varsUserSelected.put(v, vb);
				if (vb.bindingType==null || vb.bindingType.equals(BindingType.UNKNOWN)) {
					valid = false;
					varsFreeListWidget.add(v+" ("+"invalid"+")");
					vb.bindingType = BindingType.FREE;
				} else if (vb.bindingType.equals(BindingType.FREE)) {
					varsFreeListWidget.add(v);
				} else {
					varsEliminateListWidget.add(v
							+(vb.bindingType.equals(BindingType.ALL) ? "" :  
								" ("+vb.bindingType.toString().toLowerCase()+")"));
				}
			} else  {
				varsUserSelected.remove(v);
			}
		}
		for (String v : varsFromIsabelleGoal.keySet())
			if (!varsUserSelected.containsKey(v))
				varsUnusedListWidget.add(v);
		updateVarMovementButtons();
		
		buttonFinish.setEnabled(valid);
	}
	
	/** variables from the prover goal and their bindings from the prover goal,
	 * as determined in TabUpdateManager.updateImportTab
	 * (null if update is pending) */
	private Map<String, VariableBinding> varsFromIsabelleGoal = new LinkedHashMap<String, VariableBinding>();
	/** copy of bindings available for current problem */
	private Map<String, VariableBinding> varsFromSelectedIsabelleGoal = new LinkedHashMap<String, VariableBinding>();
	/** copy of bindings, with binding types changed as user changes them */
	private Map<String, VariableBinding> varsUserBindings = new LinkedHashMap<String, VariableBinding>();
	/** subset of userBindings excluding those not in use */
	private Map<String, VariableBinding> varsUserSelected = new LinkedHashMap<String, VariableBinding>();
	
	public Collection<VariableBinding> getSelectedVariables() {
		List<VariableBinding> result = new ArrayList<VariableBinding>();
		//add the user binding, but in the order from the original isabelle goal
		for (VariableBinding v : varsFromIsabelleGoal.values()) {
			VariableBinding vv = varsUserSelected.get(v.varName);
			if (vv!=null) result.add(vv);
		}
		return result;
	}
	
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
				updateVariableSets();
				//translator, translator.getVariables( ((Button)b).getText() ).values() );
			}
		});
		return b;
	}			

	public void resetFields() {
		varsFromIsabelleGoal.clear();
		varsFromSelectedIsabelleGoal.clear();
		varsUserBindings.clear();
		varsUserSelected.clear();
		for (Control b : compositeAssumptions.getChildren())
			b.dispose();
		conclButton.setVisible(false);
		assumptionsOnly = false;
		
		varsEliminateListWidget.removeAll();
		varsFreeListWidget.removeAll();
		varsUnusedListWidget.removeAll();
		updateVarMovementButtons();
	}
	
	protected int computeDataHashCode() {
		int hashCode = conclButton.getSelection() ? conclButton.getText().hashCode() : 97;
		for (Control c : getCurrentAssumptionComponents()) {
			if (!(c instanceof Button)) continue;
			if (!((Button)c).getSelection()) continue;
			hashCode = hashCode * 193 + ((Button)c).getText().hashCode();
		}
		
		for (String v : varsEliminateListWidget.getItems()) {
			hashCode = hashCode * 139 + v.hashCode();
		}		
		for (String v : varsFreeListWidget.getItems()) {
			hashCode = hashCode * 147 + v.hashCode();
		}		
		for (String v : varsUnusedListWidget.getItems()) {
			hashCode = hashCode * 141 + v.hashCode();
		}		
		return hashCode;
	}
	
	public void applyChangesImpl() {
		view.getPreviewComposite().resetFields();
		view.getFinishComposite().resetFields();
			
		view.getProblemComposite().update();
	}

	public void update() {
		String problemTextProver = ((MainQepcadViewPart)view).getStartComposite().getProverProblemTextLastApplied();
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
		
//		//TODO- set mode in import tab automatically
//		if (view.getImportComposite().conclButton.getSelection()){
//			if (eliminateAllVars()){
//			view.getImportComposite().solve.setSelection(true);
//		}
//			else { 
//				view.getImportComposite().simp.setSelection(true);
//			}
//		}
//		else 
//		 {view.getImportComposite().simpAssm.setSelection(true);}
		
		//TODO- set whether to import the conclusion

		//do this whenever the import tab is updated, so that the problem tab shows current info
		//e.g. if user navigates directly from start tab to problem tab
		
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
	
}