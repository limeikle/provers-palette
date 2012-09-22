package org.cognetics.proverspalette.qepcad.gui;


import org.cognetics.proverspalette.base.gui.ProversPaletteTabCompositeAbstract;
import org.cognetics.proverspalette.qepcad.QepcadDataException;
import org.cognetics.proverspalette.qepcad.QepcadProblem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class ConfigTabComposite extends ProversPaletteTabCompositeAbstract {

	public StyledText memoryText = null;
	public StyledText executableText = null;
	public Button qepcadNormal = null;
	public Button qepcadSlfq = null;
	public Button qepcadCad2d = null;
	public Button projMcCallum = null;
	public Button projHong = null;
	public Button solnT = null;
	public Button solnE = null;
	public Button solnG = null;
	public Button solnH = null;

	public ConfigTabComposite(MainQepcadViewPart view, String tabLabel, int style) {
		super(view, tabLabel, style);
		createContents();
	}

	@Override
	protected void createTabMainArea(Composite composite) {
		ScrolledComposite scrolledComposite = new ScrolledComposite(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		scrolledComposite.setLayoutData(gd);
		Composite scrollMainComposite = createScrollCompositeBody(scrolledComposite);
		Point s = scrollMainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolledComposite.setMinSize(s);
	}

	private Composite createScrollCompositeBody(ScrolledComposite scrolledComposite) {
		Composite scrollMainComposite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(scrollMainComposite);
		scrollMainComposite.setLayout(new GridLayout());

		createProgramGroup(scrollMainComposite);
		createProjectionPhaseGroup(scrollMainComposite);
		createSolutionGroup(scrollMainComposite);
		
		return scrollMainComposite;		
	}

	private void createProgramGroup(Composite parent) {
		Composite groupProgram = new Composite(parent, SWT.NONE);
		//alternative layout, draws a box around these options (too crowded)
//		new Group(scrollMainComposite, SWT.NONE);
//		((Group)groupProgram).setText("Program");

		groupProgram.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		groupProgram.setLayout(newGridLayout(2, 0, 0));

		createSubProgramArea1(groupProgram);
		createSubProgramArea2(groupProgram);

	}

	private void createSubProgramArea1(Composite parent){
		GridLayout gridLayoutTA1 = new GridLayout();
		gridLayoutTA1.numColumns = 1;
		gridLayoutTA1.marginWidth = 0;
		gridLayoutTA1.marginHeight = 0;
		gridLayoutTA1.marginRight = 3;

		GridData gridDataPA1 = new GridData();
		gridDataPA1.grabExcessVerticalSpace = false;
		gridDataPA1.verticalAlignment = GridData.BEGINNING;
		gridDataPA1.grabExcessHorizontalSpace = true;
		gridDataPA1.horizontalAlignment = GridData.FILL;	

		GridData gridDataPA1a = new GridData();
		gridDataPA1a.widthHint = 60;
		gridDataPA1a.verticalAlignment = GridData.CENTER;
		gridDataPA1a.grabExcessHorizontalSpace = true;
		gridDataPA1a.horizontalAlignment = GridData.FILL;

		GridData gridDataPA1b = new GridData();
		gridDataPA1b.widthHint = 60;
		gridDataPA1b.verticalAlignment = GridData.CENTER;
		gridDataPA1b.grabExcessHorizontalSpace = true;
		gridDataPA1b.horizontalAlignment = GridData.FILL;

		//Should this be a composite?
		Composite groupPA1 = new Composite(parent, SWT.NONE);
		groupPA1.setLayoutData(gridDataPA1);
		groupPA1.setLayout(gridLayoutTA1);

		Group groupPA1a = new Group(groupPA1, SWT.NONE);
		groupPA1a.setLayoutData(gridDataPA1);
		groupPA1a.setLayout(new GridLayout(1, true));
		Group groupPA1b = new Group(groupPA1, SWT.NONE);
		groupPA1b.setLayoutData(gridDataPA1);
		groupPA1b.setLayout(new GridLayout(1, true));

		groupPA1a.setText("Executable");
		executableText = new StyledText(groupPA1a, SWT.BORDER);
		executableText.setLayoutData(gridDataPA1a);

		groupPA1b.setText("Memory");
		memoryText = new StyledText(groupPA1b, SWT.BORDER);
		memoryText.setLayoutData(gridDataPA1b);


	}

	private void createSubProgramArea2(Composite parent) {
		GridLayout gridLayoutPA2 = new GridLayout();
		gridLayoutPA2.verticalSpacing = 1;

		Group groupPA2 = new Group(parent, SWT.NONE);
		groupPA2.setLayout(gridLayoutPA2);
		groupPA2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		groupPA2.setText("QEPCAD Package");
		
		qepcadNormal = new Button(groupPA2, SWT.RADIO);
		qepcadNormal.setText("QEPAD");
		qepcadSlfq = new Button(groupPA2, SWT.RADIO);
		qepcadSlfq.setText("SLFQ");
		qepcadCad2d = new Button(groupPA2, SWT.RADIO);
		qepcadCad2d.setText("CAD2D");
		qepcadNormal.setSelection(true);

		//TODO enable the others
		qepcadSlfq.setEnabled(false);
		qepcadCad2d.setEnabled(false);
		qepcadNormal.setToolTipText("Only QEPCAD (normal mode) is currently available.");
	}

	private void createProjectionPhaseGroup(Composite parent) {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.marginWidth = 5;
		gridLayout1.marginHeight = 5;
		
		Group group1 = new Group(parent, SWT.NONE);
		group1.setLayout(gridLayout1);
		group1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		group1.setText("Projection Phase");
		
		//label1 = new Label(group1, SWT.NONE);
		//label1.setText("Choose the default projection to be applied to each variable:\n(This can be overridden per variable in interactive mode.)");
		projMcCallum = new Button(group1, SWT.RADIO);
		projMcCallum.setText("McCallum Projection (QEPCAD default)");
		projMcCallum.setSelection(true);
		projHong = new Button(group1, SWT.RADIO);
		projHong.setText("Hong Projection (larger equations but works on more problems)");
	}

	private void createSolutionGroup(Composite parent) {
		Group solutionsGroup = new Group(parent, SWT.NONE);
		solutionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		solutionsGroup.setLayout(new GridLayout(1, false));
		solutionsGroup.setText("Solution Construction Phase");
		createSolnSubgroupOtherOptions(solutionsGroup);
		createSolnSubgroupCadUndefined(solutionsGroup);
		
		// TODO when connected to qepcad, make visible
		solutionsGroup.setVisible(false);
	}

	private void createSolnSubgroupCadUndefined(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.verticalSpacing = 1;
		
		Group undefinableCadGroup = new Group(parent, SWT.NONE);
		undefinableCadGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		undefinableCadGroup.setLayout(gridLayout);
		undefinableCadGroup.setText("Behaviour on Undefinable CAD");
		solnT = new Button(undefinableCadGroup, SWT.RADIO);
		solnT.setText("Add polynomials to factor set (\"T\", the default)");
		solnE = new Button(undefinableCadGroup, SWT.RADIO);
		solnE.setText("Use Extended Tarski Formulas (\"E\")");
	}

	private void createSolnSubgroupOtherOptions(Composite parent) {
		Composite group = parent;
		//alternative layout:
//		group = new Group(parent, SWT.NONE);
//		group.setLayout(new GridLayout());
//		group.setText("Other Options");
//		group.setLayoutData(newGridLayout(1, 0, 0));
		solnG = new Button(group, SWT.CHECK);
		solnG.setText("Geometry insight (\"G\")");
		solnH = new Button(group, SWT.CHECK);
		solnH.setText("Old-style construction using Hong's routines (\"H\")");
		//checkBox2 = new Button(group7, SWT.CHECK);
		//checkBox2.setText("QEPCAD interactive mode (only applies to interactive runs; \"I\")");
	}

	public void resetFields(){
		buttonFinish.setEnabled(false);
		executableText.setText("qepcad");
		memoryText.setText("2000000");
		projMcCallum.setSelection(true);
		projHong.setSelection(false);
		qepcadNormal.setSelection(true);
		qepcadCad2d.setSelection(false);
		qepcadSlfq.setSelection(false);
		solnE.setSelection(false);
		solnG.setSelection(false);
		solnT.setSelection(false);
		solnH.setSelection(false);
	}

	public void contributeSettingsToQepcadProblem(QepcadProblem prob) throws QepcadDataException {
		
		prob.executable = executableText.getText();
		
		try {
			prob.memory = Long.parseLong(memoryText.getText());
		} catch (NumberFormatException e) {
			throw new QepcadDataException("The value '"+memoryText.getText()+"' " +
			"for memory is not valid.");
		}

		if (projMcCallum.getSelection()){
			prob.projection = "McCallum";
		}
		else prob.projection = "Hong";		

		//TODO- syn soln construction and QEPCAD type 
		// I have not programmed these options in my QEPCADProblem object!

		//TODO- there is no functionality in the GUI for adding assumptions in the 
		// normalisation phase

	}

	protected int computeDataHashCode() {
		int hashCode = executableText.getText().hashCode();
		hashCode = hashCode * 139 + memoryText.getText().hashCode();
		hashCode = hashCode * 139 + (projMcCallum.getSelection()?1:0);
		hashCode = hashCode * 139 + (qepcadNormal.getSelection()?1:0);
		hashCode = hashCode * 139 + (solnE.getSelection()?1:0);
		hashCode = hashCode * 139 + (solnG.getSelection()?1:0);
		hashCode = hashCode * 139 + (solnT.getSelection()?1:0);
		hashCode = hashCode * 139 + (solnH.getSelection()?1:0);
		return hashCode;
	}

	@Override
	public void update() {
		//only finish needs updating
		buttonFinish.setEnabled( view.getProblemComposite().isProblemAvailable() );
		noteUpdateCompleted();
	}

}
