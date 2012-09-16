package org.cognetics.proverspalette.qepcad.gui;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

import org.cognetics.proverspalette.qepcad.QepcadProblem;
import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.heneveld.javautils.swtui.EasyScrollingStyledText;
import org.heneveld.javautils.swtui.EasyTableColumn;
import org.heneveld.javautils.swtui.EasyTableManager;
import org.heneveld.javautils.swtui.MoveableRowsTableColumn;
import org.heneveld.maths.structs.MathsExpression;

public class ProblemTabComposite extends org.cognetics.proverspalette.base.gui.ProblemTabComposite {

	public EasyTableManager<VariableBinding> varsTable = null;
	public EasyTableManager<String[]> assumptionsTable = null;
	public StyledText goalsStyledText = null;
	public StyledText descriptionText = null;
	public Combo translationCombo = null;

	public ProblemTabComposite(MainQepcadViewPart view, String tabLabel, int style) {
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
		createSashCompositeOfVarsGoalAssms(composite);
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

	private void createSashCompositeOfVarsGoalAssms(Composite composite) {
		SashForm sashForm = new SashForm(composite, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createVarsSashLhs(sashForm);
		createGoalsAssmsSashRhs(sashForm);
		sashForm.setWeights(new int[] { 1, 2 });
	}

	public static final String[] BINDING_TYPES = 
		//VariableBinding.BindingType.getAllQepcadLabels();  //plus "free" 
	new String[] {
		"free", "A", "E", "F", "G"
		//, "C", "1x", "kx"
	};

	private void createVarsSashLhs(SashForm parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setText("Variables");
		g.setLayout(new GridLayout(1, false));

		varsTable = new EasyTableManager<VariableBinding>()
			.configureHeadings(true).configureLines(true)
			.addColumn(new MoveableRowsTableColumn<VariableBinding>(new VariableBinding("", VariableBinding.BindingType.UNKNOWN, false),
					new Callable<VariableBinding>() {	
						public VariableBinding call() {
							return new VariableBinding("variable", VariableBinding.BindingType.FREE, false);
						}
					}))
			.addColumn(new EasyTableColumn<VariableBinding>("Name") {
				@Override
				public String getColumnDisplayTextFromRowData(VariableBinding element) {
					return element.varName;
				}
				@Override
				public boolean setValue(VariableBinding rowData, Object newValue) {
					rowData.varName = newValue.toString();
					return true;
				}
			})
			.addColumn(new EasyTableColumn<VariableBinding>("Binding", 80) {
				@Override
				public String getColumnDisplayTextFromRowData(VariableBinding element) {
					if (BindingType.FREE.equals(element.bindingType)) return "free";
					return element.bindingType.getSimpleLabel();
				}
				@Override
				public Object getColumnModifierInput(VariableBinding element) {
					String text = super.getColumnModifierInput(element).toString();
					for (int i=0; i<BINDING_TYPES.length; i++)
						if (BINDING_TYPES[i].equalsIgnoreCase(text))
							return i;
					return 0;
				}
				@Override
				public CellEditor createCellEditor(Table table) {
					ComboBoxCellEditor cce = new ComboBoxCellEditor(table, BINDING_TYPES);
					return cce;
				}
				@Override
				public boolean setValue(VariableBinding rowData, Object newValue) {					
					rowData.bindingType = VariableBinding.BindingType.getBindingForQuantifierSimpleLabel( BINDING_TYPES[(Integer)newValue] );
					return true;
				}
			});
		varsTable.createTable(g);		
		varsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	private void createGoalsAssmsSashRhs(SashForm sashForm) {
		SashForm rhsSash = new SashForm(sashForm, SWT.NONE);
		rhsSash.setOrientation(SWT.VERTICAL);
		
		Group group1 = new Group(rhsSash, SWT.NONE);
		group1.setText("Goal");
		group1.setLayout(new GridLayout(1, false));
		
		goalsStyledText = 
			new EasyScrollingStyledText(group1, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
		goalsStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		goalsStyledText.setWordWrap(true);
		
		createAssumptionsTableComposite(rhsSash);

		rhsSash.setWeights(new int[] { 2, 1 });
	}

	private void createAssumptionsTableComposite(Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		//TODO - give warning if assumption uses anything but a free var
		g.setText("Normalisation Assumptions (optional)");
		g.setLayout(new GridLayout(1, false));
		
		assumptionsTable = new EasyTableManager<String[]>()
			.addColumn(new MoveableRowsTableColumn<String[]>(new String[] { "" }, new Callable<String[]>() {
				public String[] call() throws Exception {
					return new String[] { "(enter assumption here)" };
				}				
			}))
			.addColumn(new EasyTableColumn<String[]>("Assumption") {
				@Override
				public Object getColumnEntryFromRowData(String[] rowData) {
					return rowData[0];
				}
			})
			.configureHeadings(false);
		assumptionsTable.createTable(g);
		assumptionsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	public void resetFields(){
          descriptionText.setText("");
		  goalsStyledText.setText("");
		  varsTable.clearData();
		  assumptionsTable.clearData();
	}
	
	public boolean isProblemAvailable() {
		if (goalsStyledText.getText().trim().length()==0) return false;
		
		List<VariableBinding> vars = varsTable.getData();
		if (vars.isEmpty()) return false;
		for (VariableBinding vb :vars)
			if (vb.bindingType==null || vb.bindingType == BindingType.UNKNOWN) return false;
		
		return true;
	}
	
	public void contributeSettingsToQepcadProblem(QepcadProblem prob){
		prob.problemName = descriptionText.getText();
		prob.goal = goalsStyledText.getText();
		
		int numVars = varsTable.getData().size();
		prob.variablesAndBindings = new VariableBinding[numVars];
		int i=0;
		prob.freeVars = 0;
		for (VariableBinding varRow : varsTable.getData()) {	
			prob.variablesAndBindings[i] = varRow;
			if (varRow.bindingType.equals(BindingType.FREE))
				prob.freeVars++;
			i++;
		}
		prob.normalizationAssumptions.clear();
		for (String[] ti : assumptionsTable.getData()) {	
			prob.normalizationAssumptions.add(ti[0]);
		}
		
	}
	
	int dataHashCodeLastApplied = -1;
	
	protected int computeDataHashCode() {
		int hashCode = descriptionText.getText().hashCode();
		hashCode = hashCode * 193 + goalsStyledText.getText().hashCode();		
		for (VariableBinding varRow : varsTable.getData()) {	
			hashCode = hashCode * 139 + varRow.bindingType.hashCode() * 53 + varRow.varName.hashCode();
		}
		for (String[] ti : assumptionsTable.getData()) {	
			hashCode = hashCode * 239 + ti[0].hashCode();
		}
		return hashCode;
	}

	@Override
	public void update() {
		descriptionText.setText(
				"created from "+view.getCurrentProverType()+" subgoal at "+
				DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime())
				//					"\n"+sg.concl.toString()
		);
		super.update();
	}
	
	protected void updateWith(List<MathsExpression> assmsCommon, MathsExpression conclusionItem) {
		//TODO do code for when leaving import tabhere... 
		// update problem tab:
		//  - table of vars in problem tab has a corresondence with vars to eliminate in import tab
		//    if var checked to eliminate then make it's binding E or A depending on what
		//    the Isabelle subgoal said (include G and F later)
		//    (all binidings are at beginning of subgoal, if not make A unless var has ? at front, in this case make it E)
		//    all vars not to eliminate should be free
		//  - goal in problem tab should be made of (assumptions)* --> (conclusion)*
		//    * means if checked and could stand for 0 or more times

//		varsTable.getData().clear();
//		for (VariableBinding v : vars.values()) {			
//			view.getProblemComposite().varsTable.
//				getData().add(v);
//		}	
//		view.getProblemComposite().varsTable.refreshFromData();

		MathsExpression selectedItemsSubgoalCommon = createExpressionFromAssumptionsAndConclusion(
				assmsCommon, conclusionItem);

		String qepcadGoal = "";
		List<VariableBinding> varsDataForTable = varsTable.getData();
		varsDataForTable.clear();
		
		if (selectedItemsSubgoalCommon.toString().length()>0) {
			
			MathsExpression problemSystem =
				view.getCurrentExternalSystemTranslator().tidyBrackets(
						view.getCurrentExternalSystemTranslator().stripQuantifications(
								view.getCurrentExternalSystemTranslator().fromCommon(selectedItemsSubgoalCommon), true),
				true, true, true);
			qepcadGoal = external().ensureVariableNamesAreSafe(problemSystem).toString();
			goalsStyledText.setText( qepcadGoal );
		
			varsDataForTable.addAll(view.getImportComposite().getSelectedVariables());		
			varsTable.refreshFromData();		

			buttonFinish.setEnabled( isProblemAvailable());
		} else {
			goalsStyledText.setText( "" );
			varsTable.refreshFromData();		
			buttonFinish.setEnabled( false );
		}
	}
}

