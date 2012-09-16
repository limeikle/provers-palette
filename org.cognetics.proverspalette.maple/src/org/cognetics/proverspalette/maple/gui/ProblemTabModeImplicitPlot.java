/**
 * 
 */
package org.cognetics.proverspalette.maple.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cognetics.proverspalette.maple.MapleTranslator;
import org.cognetics.proverspalette.maple.MapleParser.MapleOperatorExpression;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.heneveld.javautils.swtui.EasyTableColumn;
import org.heneveld.javautils.swtui.EasyTableManager;
import org.heneveld.maths.structs.MathsExpression;

public class ProblemTabModeImplicitPlot extends ProblemTabMode {
	
	private final ProblemTabComposite problemTabComposite;
	public final static String NAME = "Plot";
	public ProblemTabModeImplicitPlot(ProblemTabComposite problemTabComposite) {
		super(problemTabComposite, NAME, "Select variables to plot.");
		this.problemTabComposite = problemTabComposite;
	}
	
	Set<String> vars;
	List<MathsExpression> equationsOriginalUndecomposed = new ArrayList<MathsExpression>(), 
		equationsCurrentAll = new ArrayList<MathsExpression>(); 
	List<MathsExpression> equationsCurrentSelected = new ArrayList<MathsExpression>();
	
	org.eclipse.swt.widgets.List varsX, varsY, varsZ, varsT, constants;
	Text intervalLo, intervalHi;
	
	EasyTableManager<MathsExpression> equationsTable;
	
	@Override
	public String getDescription() {
		//don't show a description
		return null;
	}
	
	@Override
	public void addControlWidgets() {
		super.addControlWidgets();
		if (this.problemTabComposite.selectedEverythingCommon==null) return;
		
		SashForm mainSash = new SashForm(this.problemTabComposite.modeGroup, SWT.HORIZONTAL);
		mainSash.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		
		Group equationsSashPart = new Group(mainSash, SWT.NULL);
		equationsSashPart.setText("Equations");
		equationsSashPart.setLayout(newFlushOneColumnGridLayout());
		
		equationsTable = new EasyTableManager<MathsExpression>();
		equationsTable.configureHeadings(false);
		
		EasyTableColumn<MathsExpression> colEqn = new EasyTableColumn<MathsExpression>("Equation", 50) {
			@Override
			public String getColumnDisplayTextFromRowData(MathsExpression rowData) {
				MathsExpression exp = rowData;
				if (exp instanceof MapleOperatorExpression)
					//remove brackets for display
					exp = new MapleOperatorExpression( ((MapleOperatorExpression)exp).getList(),
							MapleOperatorExpression.BRACKETS_NONE,
							((MapleOperatorExpression)exp).getIndexOfOperator() );
				return exp.toString();
			}
			@Override
			public boolean setValue(MathsExpression rowData, Object newValue) {
				//TODO could allow modification (here)
				//would then need to update selection
				//should also allow "add" and "remove", in that case
				return true;
			}
			@Override
			public boolean isModifiable() {
				return false;
			}
		};
		equationsTable.addColumn(colEqn);
		final Table t = equationsTable.createTable(equationsSashPart, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | 
		    	SWT.FULL_SELECTION | SWT.HIDE_SELECTION |
		    	SWT.BORDER | SWT.CHECK);
		t.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		t.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateOnEquationSelectionChange();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		t.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				//needed because grayed out equations turn black if they are selected and focus leaves
				t.setSelection(new int[0]);
				updateEquationsTableOnSelectionChange(null);
			}
			public void focusGained(FocusEvent e) {
			}
		});
		//TODO eventually table could allow specifying name, colour, group, etc
		final Menu tableMenu = new Menu(t);
		t.setMenu(tableMenu);

		final MenuItem decompose = new MenuItem(tableMenu, SWT.NONE);
		decompose.setText("&Decompose Selected");
		decompose.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				int[] sel = t.getSelectionIndices();
				for (int i : sel) {
					MathsExpression eqn = equationsTable.getData().get(i);
					if (MapleTranslator.isDecomposable(eqn)) {
						Set<MathsExpression> decomposedItems = new LinkedHashSet<MathsExpression>();
						decomposedItems.add(eqn);
						MapleTranslator.decompose(decomposedItems);
						
						int eqnIndex = equationsCurrentAll.indexOf(eqn);
						if (eqnIndex==-1) {
							System.err.println("Equation mismatch: could not find "+eqn);
							eqnIndex=equationsCurrentAll.size();
						} else {
							while (equationsCurrentAll.remove(eqn));
						}
						equationsCurrentAll.addAll(eqnIndex, decomposedItems);
					}
				}
				updateEquationsTableOnEquationsChange();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		final MenuItem revert = new MenuItem(tableMenu, SWT.NONE);
		revert.setText("&Revert All");
		revert.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				equationsCurrentAll.clear();
				equationsCurrentAll.addAll(equationsOriginalUndecomposed);
				updateEquationsTableOnEquationsChange();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		t.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				revert.setEnabled(!equationsCurrentAll.equals(equationsOriginalUndecomposed));
				
				int[] sel = t.getSelectionIndices();
				boolean hasComposedItem = false;
				for (int i : sel) {
					if (MapleTranslator.isDecomposable(equationsTable.getData().get(i))) {
						hasComposedItem = true;
						break;
					}
				}
				decompose.setEnabled(hasComposedItem);
			}
		});

		Composite varSashPart = new Composite(mainSash, SWT.NULL);
		varSashPart.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		//TODO above=4, horizSpac=2
		varSashPart.setLayout(newFlushGridLayout(4, true, 2));
		
		varsX = newVarListInGroup("x-axis", varSashPart);
		varsY = newVarListInGroup("y-axis", varSashPart);
		varsZ = newVarListInGroup("z-axis", varSashPart);
		varsT = newVarListInGroup("Time", varSashPart);
		
		
		Composite constSashPart = new Composite(mainSash, SWT.NULL);
		constSashPart.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		constSashPart.setLayout(newFlushOneColumnGridLayout());
		
		constants = newVarListInGroup("Constants", constSashPart);
		//TODO able to set constants in table
		constants.setToolTipText("Values for constants can be set in the script " +
//				"at right" +
				"on the following tab" +
				". " +
				"If not set, they default to 0.");
		Button indivRange = new Button(constSashPart, SWT.CHECK);
		indivRange.setSelection(false);
		indivRange.setText("Per-axis regions");
		//TODO checkbox for setting range on each var
		indivRange.setToolTipText("Not yet supported. To set different intervals on a per-variable (per-axis) basis, " +
				"edit the corresponding RANGE variable in the script on the following tab.");
		indivRange.setEnabled(false);

		buttonUpdateListeners.clear();
		
		Composite intervalComp = new Composite(varSashPart, SWT.NULL);
		intervalComp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false,4,1));			
		intervalComp.setLayout(newFlushGridLayout(4, false, 4));
		new Label(intervalComp, SWT.NONE).setText("Region: ");
		intervalLo = new Text(intervalComp, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL,SWT.FILL,true,false);
		gd.widthHint = 60;
		intervalLo.setLayoutData(gd);
		new Label(intervalComp, SWT.NONE).setText("to");
		intervalHi = new Text(intervalComp, SWT.BORDER);
		gd = new GridData(SWT.FILL,SWT.FILL,true,false);
		gd.widthHint = 60;
		intervalHi.setLayoutData(gd);
		intervalLo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateGuiOnSelectionChange();
			}				
		});
		intervalHi.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateGuiOnSelectionChange();
			}				
		});
		intervalHi.computeSize(-1, -1, true);
		intervalLo.setText("-10");
		intervalHi.setText("10");
		intervalComp.setToolTipText("Set the interval for all axes. \n" +
				"'-infinity' and/or 'infinity' can be used in some circumstances " +
				"(sigmoidal plots will be displayed if so).");
		for (Control c: intervalComp.getChildren())
			c.setToolTipText(intervalComp.getToolTipText());
				
		
		mainSash.setWeights(new int[] { 5, 10, 4 });

		updateVarListInGroup(varsX, "x");
		updateVarListInGroup(varsY, "y");
		updateVarListInGroup(varsZ, "z");
		updateVarListInGroup(varsT, "t");
		updateVarListInGroup(constants, "Constants");
		
		updateListBoxesOnInputDataChange();
	}

	private GridLayout newFlushGridLayout(int numCols, boolean evenlySpaced, int spaceBetween) {
		GridLayout gridLayoutMode = new GridLayout(numCols, evenlySpaced);
		gridLayoutMode.marginWidth = 0;
		gridLayoutMode.marginHeight = 0;
		gridLayoutMode.marginTop = 0;
		gridLayoutMode.horizontalSpacing = spaceBetween;
		return gridLayoutMode;
	}
	private GridLayout newFlushOneColumnGridLayout() {
		return newFlushGridLayout(1, true, 0);
	}

	//prevent drags from removing unless also dropped into a list
	private boolean dndSourceLocal = false, dndTargetLocal = false;
	
	private org.eclipse.swt.widgets.List newVarListInGroup(String title, Composite parent) {
		Group g = new Group(parent, SWT.NONE);
		g.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		g.setText(title);
		g.setLayout(newFlushOneColumnGridLayout());
		final org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(g, SWT.MULTI|SWT.BORDER);
		list.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		return list;
	}
	private void updateVarListInGroup(final org.eclipse.swt.widgets.List list, final String title) {
		Menu m = new Menu(list);
		list.setMenu(m);
		
		MenuItem moveAll = new MenuItem(m, SWT.CASCADE);
		moveAll.setText("Move &All");
		final Menu moveAllMenu = new Menu(moveAll);
		moveAll.setMenu(moveAllMenu);
		final MenuItem moveAllX = addConditionalMoveItems(moveAllMenu, title, "x", true, list, varsX);
		final MenuItem moveAllY = addConditionalMoveItems(moveAllMenu, title, "y", true, list, varsY);
		final MenuItem moveAllZ = addConditionalMoveItems(moveAllMenu, title, "z", true, list, varsZ);
		final MenuItem moveAllT = addConditionalMoveItems(moveAllMenu, title, "t", true, list, varsT);
		final MenuItem moveAllConst = addConditionalMoveItems(moveAllMenu, title, "Constants", true, list, constants);
		
		MenuItem moveSelected = new MenuItem(m, SWT.CASCADE);
		moveSelected.setText("Move &Selected");
		final Menu moveSelectedMenu = new Menu(moveSelected);
		moveSelected.setMenu(moveSelectedMenu);
		final MenuItem moveSelectedX = addConditionalMoveItems(moveSelectedMenu, title, "x", false, list, varsX);
		final MenuItem moveSelectedY = addConditionalMoveItems(moveSelectedMenu, title, "y", false, list, varsY);
		final MenuItem moveSelectedZ = addConditionalMoveItems(moveSelectedMenu, title, "z", false, list, varsZ);
		final MenuItem moveSelectedT = addConditionalMoveItems(moveSelectedMenu, title, "t", false, list, varsT);
		final MenuItem moveSelectedConst = addConditionalMoveItems(moveSelectedMenu, title, "Constants", false, list, constants);
		
		m.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				boolean nonempty = (list.getItemCount()>0);
				moveAllMenu.setEnabled(nonempty);
				if (moveAllX!=null) moveAllX.setEnabled(nonempty);
				if (moveAllY!=null) moveAllY.setEnabled(nonempty);
				if (moveAllZ!=null) moveAllZ.setEnabled(nonempty);
				if (moveAllT!=null) moveAllT.setEnabled(nonempty);
				if (moveAllConst!=null) moveAllConst.setEnabled(nonempty);
				
				boolean selected = (list.getSelectionCount()>0);
				moveSelectedMenu.setEnabled(selected);
				if (moveSelectedX!=null) moveSelectedX.setEnabled(selected);
				if (moveSelectedY!=null) moveSelectedY.setEnabled(selected);
				if (moveSelectedZ!=null) moveSelectedZ.setEnabled(selected);
				if (moveSelectedT!=null) moveSelectedT.setEnabled(selected);
				if (moveSelectedConst!=null) moveSelectedConst.setEnabled(selected);
			}
			public void menuHidden(MenuEvent e) {
			}
		});
		
		DragSource source = new DragSource(list, DND.DROP_MOVE);
		source.setTransfer(new Transfer[] {TextTransfer.getInstance()});
		source.addDragListener(new DragSourceListener() {
			public void dragFinished(DragSourceEvent event) {
				if (dndSourceLocal && dndTargetLocal && event.detail==DND.DROP_MOVE) {
					//remove if it was moved
					String[] items = list.getSelection();
					for (int j=0; j<items.length; j++) {
						for (int i=0; i<list.getItemCount(); i++)
							if (list.getItem(i).equals(items[j])) {
								list.remove(i);
								break;
							}
					}
				}
				dndSourceLocal=false; dndTargetLocal=false;
				updateGuiOnSelectionChange();
			}
			public void dragSetData(DragSourceEvent event) {
				String[] items = list.getSelection();
//				System.out.println("setting data, to type: "+event.dataType.type);
				if (items.length>0) {
					event.data = items[0];
					for (int i=1; i<items.length; i++)
						event.data = ""+event.data + ", "+items[i];
				}
			}
			public void dragStart(DragSourceEvent event) {
				dndSourceLocal=true;
//				System.out.println("drag start...");
//				dragSetData(event);
			}
		});
		
		DropTarget target = new DropTarget(list, DND.DROP_MOVE);
		target.setTransfer(new Transfer[] {TextTransfer.getInstance()});
		 	 
		target.addDropListener(new DropTargetListener() {
			public void dragEnter(DropTargetEvent event) {
				if (!dndSourceLocal) event.detail = DND.DROP_NONE;
				else event.detail = DND.DROP_MOVE;
			}
			public void dragOver(DropTargetEvent event) {
				//doesn't seem to do anything
//				event.feedback = DND.FEEDBACK_NONE; //INSERT_BEFORE | DND.FEEDBACK_SCROLL;
				if (!dndSourceLocal) event.detail = DND.DROP_NONE;
				else event.detail = DND.DROP_MOVE;
			}
			public void dragOperationChanged(DropTargetEvent event) {
				if (!dndSourceLocal) event.detail = DND.DROP_NONE;
				else event.detail = DND.DROP_MOVE;
			}
			public void dragLeave(DropTargetEvent event) {
				event.detail = DND.DROP_NONE;
			}
			public void dropAccept(DropTargetEvent event) {
				if (!dndSourceLocal) event.detail = DND.DROP_NONE;
			}
			public void drop(DropTargetEvent event) {
				//remove from source, put in target
				if (dndSourceLocal) dndTargetLocal=true;
				
				if (event.data instanceof String) {
					for (String s : ((String)event.data).split(","))
						list.add(s.trim());
				}
			}
		});
	}

	private MenuItem addConditionalMoveItems(Menu parent, String context, String target, final boolean all, 
			final org.eclipse.swt.widgets.List sourceControl, final org.eclipse.swt.widgets.List targetControl) {
		if (context.equalsIgnoreCase(target)) return null;
		MenuItem moveItem = new MenuItem(parent, SWT.NONE);
		moveItem.setText("To &"+
				(target.length()==1 ? target+"-Axis" : target+" List") );
		moveItem.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String[] items = all ? sourceControl.getItems() : sourceControl.getSelection();
				for (String item : items) {
					sourceControl.remove(item);
					targetControl.add(item);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		return moveItem;
	}

	public void updateControlWidgets() {
		updateListBoxesOnInputDataChange();
	}

	private void updateEquationsTableOnEquationsChange() {
		//underlying equationsCurrentAll has changed.
		//upate the logically selected items,
		//then update the table
		Set<MathsExpression> equationsPreviouslyShown = new LinkedHashSet<MathsExpression>(equationsTable.getData());
		
		equationsTable.clearData();
		int maxWidth = 50;
		for (MathsExpression p : equationsCurrentAll) {
			equationsTable.addDataRow(p);
			maxWidth = Math.max(maxWidth, stringWidthInEqTable(""+p)+10);
		}
		equationsTable.getColumnDefinitions().get(0).getAssociatedWidget().setWidth(maxWidth);
		updateEquationsTableOnSelectionChange(equationsPreviouslyShown);
	}			
	int validEquationsAvailable = 0;
	/** if vars change, we have to re-evaluate which are valid, which are selectable */
	private void updateEquationsTableOnSelectionChange(Set<MathsExpression> optionalEquationsPreviouslyShown) {
		Set<MathsExpression> equationsPreviouslySelected = new LinkedHashSet<MathsExpression>(equationsCurrentSelected);
		equationsCurrentSelected.clear();
		
		int i=0;
		validEquationsAvailable = 0;
		for (MathsExpression p : equationsTable.getData()) {
			boolean wasValid = !equationsTable.getTable().getItem(i).getGrayed();
			boolean valid = isExpressionPlottableForSelection(p);
			if (valid) validEquationsAvailable++;
			boolean selected = valid && (equationsPreviouslySelected.contains(p) ||
					!wasValid ||
					(optionalEquationsPreviouslyShown!=null && !optionalEquationsPreviouslyShown.contains(p)));
			
			equationsTable.getTable().getItem(i).setGrayed(!valid);
			equationsTable.getTable().getItem(i).setForeground(Display.getCurrent().getSystemColor(
					valid ? SWT.COLOR_BLACK : SWT.COLOR_GRAY) );
			equationsTable.getTable().getItem(i).setChecked(selected);			
			if (selected) equationsCurrentSelected.add(p);
			
			i++;
		}
	}
	
	private void updateOnEquationSelectionChange() {
		equationsCurrentSelected.clear();
		int i=0;
		for (TableItem ti : equationsTable.getTable().getItems()) {
			if (ti.getChecked()) {
				if (ti.getGrayed()) ti.setChecked(false);
				else equationsCurrentSelected.add(equationsTable.getData().get(i));
			}
			i++;
		}
		updateGuiOnSelectionChange();
	}

	public void updateListBoxesOnInputDataChange() {
		if (this.problemTabComposite.selectedEverythingCommon==null) return;
		vars = new LinkedHashSet<String>(this.problemTabComposite.external().getVariables(
				this.problemTabComposite.external().fromCommon(this.problemTabComposite.selectedEverythingCommon) ).keySet());

		equationsOriginalUndecomposed.clear();
		if (this.problemTabComposite.selectedConclusionCommon!=null)
			equationsOriginalUndecomposed.add(MapleTranslator.convertInequalitiesToEquality(this.problemTabComposite.external().fromCommon(this.problemTabComposite.selectedConclusionCommon)));
		if (this.problemTabComposite.selectedAssumptionsCommon!=null)
			for (MathsExpression x : this.problemTabComposite.selectedAssumptionsCommon)
				equationsOriginalUndecomposed.add(MapleTranslator.convertInequalitiesToEquality(this.problemTabComposite.external().fromCommon(x)));

		equationsCurrentAll.clear();
		equationsCurrentAll.addAll(equationsOriginalUndecomposed);
		MapleTranslator.decompose(equationsCurrentAll);
		
		LinkedHashSet<String> varsLeftover = new LinkedHashSet<String>(vars);
		if (varsX.getItemCount()>0 || varsY.getItemCount()>0 || varsZ.getItemCount()>0 || varsT.getItemCount()>0 || constants.getItemCount()>0) {
			//remove any items in each which don't fit
			for (String s : varsY.getItems()) {
				if (!varsLeftover.remove(s)) {
					varsY.remove(s);
				}
			}
			for (String s : varsX.getItems()) {
				if (!varsLeftover.remove(s)) {
					varsX.remove(s);
				}
			}
			for (String s : varsZ.getItems()) {
				if (!varsLeftover.remove(s)) {
					varsZ.remove(s);
				}
			}
			for (String s : varsT.getItems()) {
				if (!varsLeftover.remove(s)) {
					varsT.remove(s);
				}
			}
			for (String s : constants.getItems()) {
				if (!varsLeftover.remove(s)) {
					constants.remove(s);
				}
			}
		}

		//if some contain 'x', and some contain 'y', and none contain 'xy'
		if (canUseXYZtStrategy(varsLeftover)) {
			for (String s : varsLeftover) {
				if (s.indexOf("y")>=0 || s.indexOf("Y")>=0)
					varsY.add(s);
				if (s.indexOf("x")>=0 || s.indexOf("X")>=0)
					varsX.add(s);
				if (s.indexOf("z")>=0 || s.indexOf("Z")>=0)
					varsZ.add(s);
				else if (s.indexOf("t")>=0 || s.indexOf("T")>=0)
					varsT.add(s);
				else 
					constants.add(s);
			}
		} else {
			//make sure x and y and z have something, add rest to 'constants'
			List<String> vv = new ArrayList<String>(varsLeftover);
			if (!vv.isEmpty() && varsY.getItemCount()==0) varsY.add(vv.remove(0));
			if (!vv.isEmpty() && varsX.getItemCount()==0) varsX.add(vv.remove(0));
			if (!vv.isEmpty() && varsZ.getItemCount()==0) varsZ.add(vv.remove(0));
			while (!vv.isEmpty()) constants.add(vv.remove(0));
		}

		//pick a prefix for plotting which doesn't interfere...
		int i=0;
		outer: do {
			for (String v : vars)
				if (v.startsWith(plotPrefix)) {
					plotPrefix = BASE_PLOT_PREFIX+i+"_";
					i++;
					continue outer;
				}
			break;
		} while (true);
		
		updateEquationsTableOnEquationsChange();
		updateGuiOnSelectionChange();
	}
	
	private boolean canUseXYZtStrategy(LinkedHashSet<String> varsLeftover) {
		boolean somethingHasX = false, somethingHasY = false, somethingHasZ = false, somethingHasT = false;
		for (String s : varsLeftover) {
			boolean hasY = (s.indexOf("y")>=0 || s.indexOf("Y")>=0);
			boolean hasX = (s.indexOf("x")>=0 || s.indexOf("X")>=0);
			boolean hasZ = (s.indexOf("z")>=0 || s.indexOf("Z")>=0);
			boolean hasT = (s.indexOf("t")>=0 || s.indexOf("T")>=0);
			if (hasX && hasY && hasZ && hasT) return false;
			somethingHasX |= hasX;
			somethingHasY |= hasY;
			somethingHasZ |= hasZ;
			somethingHasT |= hasT;
		}
		return somethingHasX && somethingHasY;
	}

	public void onImportUpdated() {
		updateListBoxesOnInputDataChange();
	}

	List<SelectionListener >buttonUpdateListeners = new ArrayList<SelectionListener>();
	
	final String BASE_PLOT_PREFIX = "PLOT";
	String plotPrefix = BASE_PLOT_PREFIX+"_";

	public boolean isExpressionPlottableForSelection(MathsExpression p) {
		Set<String> pv = this.problemTabComposite.external().getVariables(p).keySet();
		if (!(p instanceof MapleOperatorExpression) || 
				!((MapleOperatorExpression)p).getOperator().toString().equals("="))
			return false;
		
		//need
		//-at least 1 var from x or y or z
		//-at most 1 var from any column
		
		int nx = ProblemTabComposite.countIntersection(pv, varsX.getItems());
		int ny = ProblemTabComposite.countIntersection(pv, varsY.getItems());
		int nz = ProblemTabComposite.countIntersection(pv, varsZ.getItems());
		int nt = ProblemTabComposite.countIntersection(pv, varsT.getItems());
		if (nx>1 || ny>1 || nz>1 || nt>1) return false;
		if (nx==0 && ny==0 && nz==0) return false;

		return true;
	}
	
	public boolean isExpressionSuitableToAnimate(MathsExpression p) {
		Set<String> pv = this.problemTabComposite.external().getVariables(p).keySet();
		if (!(p instanceof MapleOperatorExpression) || 
				!((MapleOperatorExpression)p).getOperator().toString().equals("="))
			return false;
		return ((ProblemTabComposite.countIntersection(pv, varsT.getItems())==1 )) ;			
	}

	public int stringWidthInEqTable(String str) {
		GC g = new GC(equationsTable.getTable());
		Font of = g.getFont();
		g.setFont(equationsTable.getTable().getFont());
		Point extent = g.stringExtent(str);
		g.setFont(of);
		return extent.x;
	}

	String gs = "", gq = "";

	/** updates selection but not the contents of the equations table */
	public void updateGuiOnSelectionChange() {
		if (equationsCurrentAll.isEmpty()) {
			updateDescription("No equations available.", null);
			return;
		}

		String description = "";
//		String invalidExpressions = "";
		  
		String tooltip = description;

		updateEquationsTableOnSelectionChange(null);
		
		if (!isValidInterval(intervalLo.getText(), intervalHi.getText())) {
			if (description.length()>0) description+="\n";
			description += "Invalid interval.";
		} else {
			String countText = 
				(validEquationsAvailable<0 ? "No equations are suitable for plotting." :
					equationsCurrentSelected.isEmpty() ? "No equations selected." :
						equationsCurrentAll.size()==1 ? "The equation will be plotted." :
							equationsCurrentAll.size()==equationsCurrentSelected.size() ?
									(equationsCurrentAll.size()==2?"Both":"All "+equationsCurrentAll.size())+" equations will be plotted." :
								equationsCurrentSelected.size()+" of "+equationsCurrentAll.size()+" equations will be plotted.");
			if (description.length()>0) description+="\n";
			description+=countText;
			tooltip = description + (validEquationsAvailable<equationsCurrentAll.size() ? 
					"\n\n"+"An equation can only be plotted if it contains " +
					"a variable in at least one plot axis ('x', 'y', or 'z'), " +
					"it does not contain multiple variables for the same axis, " +
					"and it does not contain any non-arithmetic function." + 
//					"\n\n"+"The following equations are not valid:"+
//					invalidExpressions + 
					"" : ""
						);
		}
		updateDescription(description, tooltip);
		equationsTable.getTable().setToolTipText(tooltip);

		StringBuffer mapleGoalSetup = new StringBuffer() ;
		
		mapleGoalSetup.append("# setup with right interfaces and all commands printed nicely"+ "\n");		
		mapleGoalSetup.append("with(plots):;\n\n");

		if (constants.getItemCount()>0) {
			mapleGoalSetup.append("# these 'variables' must be set to a constant for plotting"+"\n");
			for (String i : constants.getItems()) {
				mapleGoalSetup.append(i+" := "+findOldValueOfVariable(this.problemTabComposite.
						getProblemMode().getGoalSetup(), i, "0")+";"+"\n");
			}
			mapleGoalSetup.append("\n");
		}			
		
		if (varsX.getItemCount()>0)
			mapleGoalSetup.append("PLOT_RANGE_"+"x"+" :="+intervalLo.getText()+".."+intervalHi.getText() +";\n" );
		if (varsY.getItemCount()>0)
			mapleGoalSetup.append("PLOT_RANGE_"+"y"+" :="+intervalLo.getText()+".."+intervalHi.getText() +";\n" );
		if (varsZ.getItemCount()>0)
			mapleGoalSetup.append("PLOT_RANGE_"+"z"+" :="+intervalLo.getText()+".."+intervalHi.getText() +";\n" );
		if (varsT.getItemCount()>0)
			mapleGoalSetup.append("PLOT_RANGE_"+"t"+" :="+intervalLo.getText()+".."+intervalHi.getText() +";\n" );

		mapleGoalSetup.append("\n");
		mapleGoalSetup.append("# use 'default' (text) 'maplet' (maple app), 'x11' (if enabled), or see ?plot[device]\n");
		mapleGoalSetup.append("plotsetup("+findOldArgumentOfFunction(gs, "plotsetup", getDefaultPlotMode())+"):;\n");
		mapleGoalSetup.append("\n");
		mapleGoalSetup.append("# internal code for simultaneous plot support (do not modify unless you know what you are doing)\n");
		for (String i : varsX.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"x;\n");
		}
		for (String i : varsY.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"y;\n");
		}
		for (String i : varsZ.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"z;\n");
		}
		for (String i : varsT.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"t;\n");
		}

		gs = mapleGoalSetup.toString();

		StringBuffer mapleGoalQuery = new StringBuffer() ;					

		int eqnIndex = 0;
		for (MathsExpression p : equationsCurrentSelected) {
			eqnIndex++;

			//can combine animations with static plots but
			//cannot dispay 2D and 3D plots together so need to check if there is something in the z column
			if(!(varsZ.getItemCount()==0)){
				if(isExpressionSuitableToAnimate(p)){
					mapleGoalQuery.append(plotPrefix+"eq"+eqnIndex+":= animate(implicitplot3d, [ "
							+p
							+","
							+ "x ="+"PLOT_RANGE_"+"x"+","
							+ "y ="+"PLOT_RANGE_"+"y"+","
							+ "z ="+"PLOT_RANGE_"+"z"
							+"],"
							+ "t ="+"PLOT_RANGE_"+"t"
							+"):;\n");
				}
				else{
					mapleGoalQuery.append(plotPrefix+"eq"+eqnIndex+":=implicitplot3d( "
							+p
							+","
							+ "x ="+"PLOT_RANGE_"+"x"+","
							+ "y ="+"PLOT_RANGE_"+"y"+","
							+ "z ="+"PLOT_RANGE_"+"z"
							+"):;\n");

				}
			}
			//case nothin in z column
			else{
				if(isExpressionSuitableToAnimate(p)){
					mapleGoalQuery.append(plotPrefix+"eq"+eqnIndex+":= animate(implicitplot, [ "
							+p
							+","
							+ "x =" +"PLOT_RANGE_"+"x"+","
							+ "y =" +"PLOT_RANGE_"+"y"
							+"],"
							+ "t ="+"PLOT_RANGE_"+"t"
							+"):;\n");
				}
				else{
					mapleGoalQuery.append(plotPrefix+"eq"+eqnIndex+":=implicitplot( "
							+p
							+","
							+ "x ="+"PLOT_RANGE_"+"x"+","
							+ "y ="+"PLOT_RANGE_"+"y"
							+"):;\n");

				}
			}
		}

		if (eqnIndex==0){ 
			mapleGoalQuery.append("# ERROR!  no equations to plot");			
		}
		else {
			mapleGoalQuery.append("printf(\"\\nplot should appear shortly (unless error appears below);\\n" +
			"cancel or kill maple to close\\n\\n\");\n");
			mapleGoalQuery.append("display( {");
			for (int i=1; i<=eqnIndex; i++) {
				if (i>1) mapleGoalQuery.append(", ");
				mapleGoalQuery.append(plotPrefix+"eq"+i);
			}
			mapleGoalQuery.append(" });\n"); 
		}

		gq = mapleGoalQuery.toString();

		updateGoalWidgets();
		this.problemTabComposite.modeGroup.layout(true, true);
	}
			
	private boolean isValidInterval(String lo, String hi) {
		try {
			Double lod, hid;
			if (lo.toLowerCase().trim().equals("-infinity"))
				lod = Double.MIN_VALUE;
			else lod = Double.parseDouble(lo);
			if (hi.toLowerCase().trim().equals("infinity"))
				hid = Double.MAX_VALUE;
			else hid = Double.parseDouble(hi);

			return lod.compareTo(hid) < 0;
		} catch (Exception e) {
			return false;
		}
	}

	private String findOldValueOfVariable(String container, String var, String defaultValue) {
		if (container==null) return defaultValue;
		//if old text had a manual value for this assignment, take it instead
		Matcher oldValMatcher = Pattern.compile("\\s"+var+"\\s*:=.+;").matcher(container);
		if (oldValMatcher.find()) {
			String candidateOldVal = oldValMatcher.group();
			candidateOldVal = candidateOldVal.substring(0, candidateOldVal.length()-1).trim();
			candidateOldVal = candidateOldVal.replaceFirst(".*:=\\s*", "");
			if (!candidateOldVal.startsWith(plotPrefix)) {
				return candidateOldVal;
			}
		}
		return defaultValue;
	}
	private String findOldArgumentOfFunction(String container, String fn, String defaultValue) {
		if (container==null) return defaultValue;
		//if old text had a manual value for this assignment, take it instead
		Matcher oldValMatcher = Pattern.compile("\\s"+fn+"\\s*\\(.+\\)\\s*;").matcher(container);
		if (oldValMatcher.find()) {
			String candidateOldVal = oldValMatcher.group();
			//remove last ; and last )
			candidateOldVal = candidateOldVal.substring(0, candidateOldVal.length()-1).trim();
			candidateOldVal = candidateOldVal.substring(0, candidateOldVal.length()-1).trim();
			//remove first part
			candidateOldVal = candidateOldVal.replaceFirst(fn+"\\s*\\(", "");
			return candidateOldVal;
		}
		return defaultValue;
	}
			
	@Override
	public String getGoalSetup() {			
		return gs;
	}

	@Override
	public String getGoalQuery() {
		return gq;
	}

	public boolean shouldLeaveSessionOpen() {
		return true;
	}
	
	public String getDefaultPlotMode() {
		return "maplet";
	}

}