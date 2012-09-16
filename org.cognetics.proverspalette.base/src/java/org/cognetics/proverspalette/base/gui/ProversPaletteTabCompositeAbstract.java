package org.cognetics.proverspalette.base.gui;

import java.util.Iterator;

import org.cognetics.proverspalette.translation.MathsProverTranslator;
import org.cognetics.proverspalette.translation.MathsSystemTranslator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabItem;

/** abstract superclass for provers palette tabs;
 * subclasses should typically call createContents in constructor,
 * and implement/extend methods as described below
 */
public abstract class ProversPaletteTabCompositeAbstract extends Composite {

	protected final ProversPaletteViewPartAbstract view;
	private final int tabIndex;

	private Button buttonBack = null;
	private Button buttonNext = null;
	protected Button buttonFinish = null;

	public ProversPaletteTabCompositeAbstract(ProversPaletteViewPartAbstract view, String tabLabel, int style) {				
		super(view.newTabFolder(tabLabel), style);
		this.view = view;
		this.tabIndex = findTabWithLabel(tabLabel);
				
		view.registerComposite(tabLabel, this);
		
		getParent().addListener(SWT.Hide, new Listener() {
			public void handleEvent(Event event) {
				onTabHidden();
			}			
		});
	}
	
	private int findTabWithLabel(String tabLabel) {
		TabItem[] tabItems = view.mainTabFolder.getItems();
		int i=0;
		for (TabItem ti : tabItems) {
			if (tabLabel.equals(ti.getText())) return i;
			i++;
		}
		throw new RuntimeException("Cannot find tab for '"+tabLabel+"'");
	}

	protected void createContents() {		
		setLayout(newGridLayout(1, 0, 0));
		createTabMainArea(this);
		createBottomButtonsRow(this);
	}

	/** should create the main body of the tab (everything but the buttons, in a composite above the buttons row) */
	protected abstract void createTabMainArea(Composite parent);
	
	/** creates a row of buttons at the bottom of the tab;
	 * subclasses typically override or extend createBottomLeftButtons or createBottomRightButtons */
	protected void createBottomButtonsRow(Composite composite) {
		Composite buttonsRow = new Composite(composite, SWT.NONE);
		buttonsRow.setLayout(newGridLayout(2, 0, 0));
		buttonsRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite buttons = new Composite(buttonsRow, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		buttons.setLayout(new RowLayout());
		createBottomLeftButtons(buttons);
		
		buttons = new Composite(buttonsRow, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));		
		buttons.setLayout(new RowLayout());
		createBottomRightButtons(buttons);

	}

	/** creates any buttons desired at the bottom left; none by default */
	protected void createBottomLeftButtons(Composite buttons) {
		//none, by default
	}

	/** creates any buttons desired at the bottom right; 
	 * default invokes createBackButton if not the first tab,
	 * and createNextButton and createFinishButton always */
	protected void createBottomRightButtons(Composite buttonsRow) {
		if (tabIndex>0) createBackButton(buttonsRow);		
		createNextButton(buttonsRow);
		new Label(buttonsRow, SWT.NONE).setText(" ");		
		createFinishButton(buttonsRow);
	}

	protected void createBackButton(Composite buttonsRow) {
		buttonBack = new Button(buttonsRow, SWT.PUSH);
		buttonBack.setText("< &Back");
//		buttonNext.setToolTipText("Proceed to the next step (selecting parts of the subgoal to use)");
		buttonBack.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getPreviousTab().activateTab();
			}
		});
	}

	protected void createNextButton(Composite buttonsRow) {
		buttonNext = new Button(buttonsRow, SWT.PUSH);
		buttonNext.setText("&Next >");
//		buttonNext.setToolTipText("Proceed to the next step (selecting parts of the subgoal to use)");
		buttonNext.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					applyChangesForced();
					if (getFollowingTab()==null)
						System.err.println("no 'next' tab after "+this);
					else
						getFollowingTab().activateTab();
				} catch (Exception exc) {
					System.err.println("Error setting Qepcad from Isabelle: "+exc);
					exc.printStackTrace();
				}
			}
		});
	}

	protected void createFinishButton(Composite buttonsRow) {
		buttonFinish = new Button(buttonsRow, SWT.PUSH);
		buttonFinish.setToolTipText("Send this goal to "+view.getName()+" using the values set " +
				"and attempting to intelligently select any other values needed");
		buttonFinish.setText("&Finish");
		buttonFinish.setEnabled(false);
		buttonFinish.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				finish(false);
			}
		});
	}

	/** Eclipse SWT widgets treat &x as an "x" with an underline, for any character x,
	 * apart from & (&& results in a single &);
	 * so if a proof says "A & B" we need to tell eclipse to display "A && B"
	 * <p>
	 * there might be other things which aren't displayed correctly too!
	 */
	public static String convertFromRealTextToDisplayText(String realText, Control displayItemType) {
		String result = realText;
		if (result==null) result="";
		if (displayItemType instanceof Button) {
			result = result.replaceAll("&", "&&").trim();
		}
		return result;
	}
	public static String convertFromDisplayTextToRealText(String realText, Control displayItemType) {
		String result = realText;
		if (displayItemType instanceof Button) {
			result = result.replaceAll("&&", "&").trim();
		}
		return result;
	}	
	
	public static GridLayout newGridLayout(int numColumns, int marginX, int marginY) {
		GridLayout gl = new GridLayout();
		gl.numColumns = numColumns;
		gl.marginHeight = marginX;
		gl.marginWidth = marginY;
		return gl;
	}
	
	protected MathsProverTranslator prover() {
		return view.getCurrentProverTranslator();
	}
	
	protected MathsSystemTranslator external() {
		return view.getCurrentExternalSystemTranslator();
	}

	private ProversPaletteTabCompositeAbstract previousTab = null;
	protected ProversPaletteTabCompositeAbstract getPreviousTab() {
		if (previousTab!=null || tabIndex==0) return previousTab;
		Iterator<ProversPaletteTabCompositeAbstract> ti = view.tabs().values().iterator();
		for (int i=0; i<tabIndex-1; i++) ti.next();
		previousTab = ti.next();
		return previousTab;
	}

	private ProversPaletteTabCompositeAbstract followingTab = null;
	protected ProversPaletteTabCompositeAbstract getFollowingTab() {
		if (previousTab!=null || view.tabs().size()<=tabIndex+1) return followingTab;
		Iterator<ProversPaletteTabCompositeAbstract> ti = view.tabs().values().iterator();
		for (int i=0; i<tabIndex+1; i++) ti.next();
		followingTab = ti.next();
		return followingTab;
	}
	
	/** forces the current tab to take the focus;
	 * note that this does not actually mean it is visible,
	 * the entire ViewPart might be hidden
	 * <p>
	 * use AbstractProversPaletteView.activateView to make sure the view is visible
	 */
	public void activateTab() {
		view.mainTabFolder.setSelection(tabIndex);
	}

	public boolean isTabActive() {
		return view.mainTabFolder.getSelectionIndex() == tabIndex;
	}
	
	private int dataHashCodeLastApplied = -1;
	
	/** forces subsequent tabs to update, even if data here has not changed */
	protected final void applyChangesForced() {
		dataHashCodeLastApplied = -1;
		applyChangesIfNecessary();
	}
	
	/** causes subsequent tabs to update if there have been any changes on this tab
	 * since the last time the subsequent tabs were told to update
	 * <p>
	 * checking changes is important because if user has changed tab Y (dependent on X),
	 * then browsed tab X again for reference but not changed anything, 
	 * then goes back to tab Y, we don't want the user's changes on Y to be lost 
	 */
	protected void applyChangesIfNecessary() {
		try {
			int thisHashCode = computeDataHashCode();
			if (thisHashCode==dataHashCodeLastApplied)
				//don't update anything if nothing has changed
				return;		
			applyChanges();			
			dataHashCodeLastApplied = thisHashCode;
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
	
	protected boolean hasChangesNotYetApplied() {
		return (computeDataHashCode() != dataHashCodeLastApplied);
	}
	
	protected void markChangesApplied() {
		dataHashCodeLastApplied = computeDataHashCode();
	}
	
	/** should compute a hash code for all important data in fields,
	 * used to detect when data has changed and changes have to be applied to subsequent tabs */
	protected abstract int computeDataHashCode();

	/** should ensure that all tabs which depend on data in this tab are updated
	 * with the data changed in this tab,
	 * and that changes in those tabs are propagated as well
	 * <p>
	 * applyChangesIfNecessary checks whether data in this tab has changed before invoking this method
	 * <p>
	 * the default implementation invokes update on all subsequent tabs
	 * then calls 'markChangesApplied' on all subsequent tabs 
	 * (to prevent unnecessary/unwanted subsequent applications; see applyChangesIfNecessary)
	 * <p>
	 * customised implementations could simply call update and applyChangesIfNecessary
	 * on dependent tabs */
	protected void applyChanges() {		
		ProversPaletteTabCompositeAbstract tab = getFollowingTab();
		while (tab!=null) {
			tab.update();
			tab = tab.getFollowingTab();
		}
		
		tab = this;
		while (tab!=null) { 
			tab.markChangesApplied();
			tab = tab.getFollowingTab();
		}		
	}

	
	/** should set all fields to their initial/default/blank values */
	public abstract void resetFields();

	/** whether any tabs this tab depends on have changed since their changes were last applied;
	 * default implementation looks at all previous tabs for changes */
	public boolean needsUpdating() {
		ProversPaletteTabCompositeAbstract tab = getPreviousTab();
		while (tab!=null) {
			if (tab.hasChangesNotYetApplied()) return true;
			tab = tab.getPreviousTab();
		}
		return false;
	}
	
	/** should update all fields in this tab (taking from whatever source is appropriate, typically the previous tab);
	 * as an optimization, can check whether needsUpdating;
	 * implementations should call noteUpdateCompleted when completed,
	 * to ensure the hash code is remembered so it can detect manual changes */
	public abstract void update(); 

	protected void noteUpdateCompleted() {
		lastUpdateDataHashCode = computeDataHashCode();
	}
	
	private int lastUpdateDataHashCode = -2;
	
	/** detects whether any fields have been modified since the tab was last automatically updated */
	public boolean hasManualChanges(){		
		return (lastUpdateDataHashCode != computeDataHashCode());
	}

	/** method automatically invoked when the tab is hidden;
	 * default calls 'applyChanges'; can be overridden if needed */
	protected void onTabHidden() {
		applyChangesIfNecessary();
	}
	
	/** does whatever is needed when 'finish' is pressed;
	 * default will force apply changes on all tabs up to the preview tab 
	 * then activating and running 'go' on the preview tab */
	public void finish(boolean forceInterrupt) {
		PreviewTabComposite preview = view.getPreviewComposite();
		ProversPaletteTabCompositeAbstract tab = this;
		while (tab!=preview && tab!=null) {
			tab.applyChangesForced();
			tab = tab.getFollowingTab();
		}
		if (tab==null) {
			System.err.println("No preview tab found when applying finish for '"+getClass().getCanonicalName()+"'");
			return;
		}
		preview.activateTab();
		preview.go(forceInterrupt);
	}
	



}
