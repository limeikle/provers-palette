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

import org.cognetics.proverspalette.maple.MapleParser.MapleOperatorExpression;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public class ProblemTabModePlot2d extends ProblemTabMode {
	
	private final ProblemTabComposite problemTabComposite;
	public final static String NAME = "Plot as Function (2D)";
	public ProblemTabModePlot2d(ProblemTabComposite problemTabComposite) {
		super(problemTabComposite, NAME, "Select variables to plot.");
		this.problemTabComposite = problemTabComposite;
	}
	
	Set<String> vars;
	Set<MathsExpression> parts;
	
	org.eclipse.swt.widgets.List range, domain, constants;
	Text intervalLo, intervalHi;
	
	@Override
	public void addControlWidgets() {
		super.addControlWidgets();
		if (this.problemTabComposite.selectedEverythingCommon==null) return;
		
		Composite varComp = new Composite(this.problemTabComposite.modeGroup, SWT.NULL);
		varComp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));			
		GridLayout gridLayoutMode = new GridLayout(5, false);
		gridLayoutMode.marginWidth = 0;
		gridLayoutMode.marginHeight = 0;
		gridLayoutMode.marginTop = 4;
		gridLayoutMode.horizontalSpacing = 2;
		varComp.setLayout(gridLayoutMode);
		
		new Label(varComp, SWT.NONE).setText("Range");
		new Label(varComp, SWT.NONE).setText("");
		new Label(varComp, SWT.NONE).setText("Domain");
		new Label(varComp, SWT.NONE).setText("");
		new Label(varComp, SWT.NONE).setText("Constants");
		
		range = new org.eclipse.swt.widgets.List(varComp, SWT.MULTI|SWT.BORDER);
		range.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));			
		Composite buttonComp1 = new Composite(varComp, SWT.NONE);
		domain = new org.eclipse.swt.widgets.List(varComp, SWT.MULTI|SWT.BORDER);
		domain.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		Composite buttonComp2 = new Composite(varComp, SWT.NONE);
		constants = new org.eclipse.swt.widgets.List(varComp, SWT.MULTI|SWT.BORDER);
		constants.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		constants.setToolTipText("Values for constants can be set in the goal setup at right. " +
				"If not set, they default to 0.");

		buttonUpdateListeners.clear();
		RowLayout l = new RowLayout(SWT.VERTICAL);
		l.marginLeft = 1;
		l.marginRight = 1;
		l.spacing = 12;
		buttonComp1.setLayout(l);
		Button b;
		b = new Button(buttonComp1, SWT.PUSH);			
		b.setText("<-");
		b.addSelectionListener(new SelectionListenerForMoving(b, domain, range));
		b.setEnabled(false);
		b = new Button(buttonComp1, SWT.PUSH);			
		b.setText("->");
		b.addSelectionListener(new SelectionListenerForMoving(b, range, domain));
		b.setEnabled(false);
		
		l = new RowLayout(SWT.VERTICAL);
		l.marginLeft = 1;
		l.marginRight = 1;
		l.spacing = 12;
		buttonComp2.setLayout(l);
		b = new Button(buttonComp2, SWT.PUSH);			
		b.setText("<-");
		b.addSelectionListener(new SelectionListenerForMoving(b, constants, domain));
		b.setEnabled(false);
		b = new Button(buttonComp2, SWT.PUSH);			
		b.setText("->");
		b.addSelectionListener(new SelectionListenerForMoving(b, domain, constants));
		b.setEnabled(false);

		Composite intervalComp = new Composite(this.problemTabComposite.modeGroup, SWT.NULL);
		intervalComp.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));			
		gridLayoutMode = new GridLayout(4, false);
		gridLayoutMode.marginWidth = 0;
		gridLayoutMode.marginHeight = 0;
		gridLayoutMode.marginTop = 1;
		gridLayoutMode.horizontalSpacing = 5;
		intervalComp.setLayout(gridLayoutMode);
		new Label(intervalComp, SWT.NONE).setText("Domain");
		intervalLo = new Text(intervalComp, SWT.BORDER);
		GridData gd = new GridData(SWT.FILL,SWT.FILL,true,false);
		gd.widthHint = 100;
		intervalLo.setLayoutData(gd);
		new Label(intervalComp, SWT.NONE).setText("to");
		intervalHi = new Text(intervalComp, SWT.BORDER);
		gd = new GridData(SWT.FILL,SWT.FILL,true,false);
		gd.widthHint = 100;
		intervalHi.setLayoutData(gd);
		intervalLo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOnSelectionChange("");
			}				
		});
		intervalHi.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOnSelectionChange("");
			}				
		});
		intervalHi.computeSize(-1, -1, true);
		intervalLo.setText("-10");
		intervalHi.setText("10");
		intervalComp.setToolTipText("Set the interval for the x-axis. \n" +
				"'-infinity' and/or 'infinity' can be used " +
				"(sigmoidal plots will be displayed if so).");
		for (Control c: intervalComp.getChildren())
			c.setToolTipText(intervalComp.getToolTipText());
		
		updateListBoxesOnInputDataChange("Select variables for axes.");
	}

	public void updateControlWidgets() {
		updateListBoxesOnInputDataChange("");
	}

	public void updateListBoxesOnInputDataChange(String description) {
		if (this.problemTabComposite.selectedEverythingCommon==null) return;
		vars = new LinkedHashSet<String>(this.problemTabComposite.external().getVariables(
				this.problemTabComposite.external().fromCommon(this.problemTabComposite.selectedEverythingCommon) ).keySet());

		parts = new LinkedHashSet<MathsExpression>();
		if (this.problemTabComposite.selectedConclusionCommon!=null)
			parts.add(cleanForPlotting(this.problemTabComposite.external().fromCommon(this.problemTabComposite.selectedConclusionCommon)));
		if (this.problemTabComposite.selectedAssumptionsCommon!=null)
			for (MathsExpression x : this.problemTabComposite.selectedAssumptionsCommon)
				parts.add(cleanForPlotting(this.problemTabComposite.external().fromCommon(x)));		

		LinkedHashSet<String> varsLeftover = new LinkedHashSet<String>(vars);
		if (range.getItemCount()>0 || domain.getItemCount()>0 || constants.getItemCount()>0) {
			//remove any items in each which don't fit
			for (String s : range.getItems()) {
				if (!varsLeftover.remove(s)) {
					range.remove(s);
				}
			}
			for (String s : domain.getItems()) {
				if (!varsLeftover.remove(s)) {
					domain.remove(s);
				}
			}
			for (String s : constants.getItems()) {
				if (!varsLeftover.remove(s)) {
					constants.remove(s);
				}
			}
		}

		//if some contain 'x', and some contain 'y', and none contain 'xy'
		if (canUseXyStrategy(varsLeftover)) {
			for (String s : varsLeftover) {
				if (s.indexOf("y")>=0 || s.indexOf("Y")>=0)
					range.add(s);
				else if (s.indexOf("x")>=0 || s.indexOf("X")>=0)
					domain.add(s);
				else 
					constants.add(s);
			}
		} else {
			//make sure range and domain have something, add rest to 'constants'
			List<String> vv = new ArrayList<String>(varsLeftover);
			if (!vv.isEmpty() && range.getItemCount()==0) range.add(vv.remove(0));
			if (!vv.isEmpty() && domain.getItemCount()==0) domain.add(vv.remove(0));
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
		
		updateOnSelectionChange(description);
	}
	
	private boolean canUseXyStrategy(LinkedHashSet<String> varsLeftover) {
		boolean somethingHasX = false, somethingHasY = false;
		for (String s : varsLeftover) {
			boolean hasY = (s.indexOf("y")>=0 || s.indexOf("Y")>=0);
			boolean hasX = (s.indexOf("x")>=0 || s.indexOf("X")>=0);
			if (hasX && hasY) return false;
			somethingHasX |= hasX;
			somethingHasY |= hasY;
		}
		return somethingHasX && somethingHasY;
	}

	public void onImportUpdated() {
		updateListBoxesOnInputDataChange("Plotting updated equation set.");
	}

	List<SelectionListener >buttonUpdateListeners = new ArrayList<SelectionListener>();
	
	private class SelectionListenerForMoving implements SelectionListener {
		private org.eclipse.swt.widgets.List from;
		private org.eclipse.swt.widgets.List to;
		private Button button;
		public SelectionListenerForMoving(
				Button b, org.eclipse.swt.widgets.List domain,
				org.eclipse.swt.widgets.List range) {
			this.button = b;
			this.from = domain;
			this.to = range;
			SelectionListener listener = new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					button.setEnabled(from.getSelectionCount()>0);
				}					
			};
			from.addSelectionListener(listener);
			buttonUpdateListeners.add(listener);
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			String[] selected = from.getSelection();
			for (String s : selected) {
				to.add(s);
				from.remove(s);
			}			
			//seem to have to manually tell listeners
			for (SelectionListener l : buttonUpdateListeners) {
				((SelectionListener)l).widgetSelected(null);				
			}
			updateOnSelectionChange("");
		}			
	}
	final String BASE_PLOT_PREFIX = "PLOT";
	String plotPrefix = BASE_PLOT_PREFIX+"_";

	private MathsExpression cleanForPlotting(MathsExpression mapleExpression) {
		if (mapleExpression instanceof MapleOperatorExpression) {
			List<MathsExpression> terms = new ArrayList<MathsExpression>(((MapleOperatorExpression)mapleExpression).getList());
			int i = ((MapleOperatorExpression)mapleExpression).getIndexOfOperator();
			String mop = terms.get(i).toString();
			//replace inequalities with equalities for plotting
			if ("<".equals(mop) || ">".equals(mop) || "<>".equals(mop) || "<=".equals(mop) || "<=".equals(mop)){
				terms.set(i, MathsExpressions.newToken("=", true));
				mapleExpression = new MapleOperatorExpression(terms, i);
			}			
		}
		
		return mapleExpression;
	}

	public boolean isExpressionPlottableForSelection(MathsExpression p) {
		Set<String> pv = this.problemTabComposite.external().getVariables(p).keySet();
		if (!(p instanceof MapleOperatorExpression) || 
				!((MapleOperatorExpression)p).getOperator().toString().equals("="))
			return false;
		return (ProblemTabComposite.countIntersection(pv, range.getItems())==1 &&
				ProblemTabComposite.countIntersection(pv, domain.getItems())==1);			
	}
	String gs = "", gq = "";
	public void updateOnSelectionChange(String description) {
		if (parts==null) {
			updateDescription("No equations available.", null);
			return;
		}
		
		int validParts = 0;
		String invalidExpressions = "";
		for (MathsExpression p : parts) {
			if (isExpressionPlottableForSelection(p))
				validParts++;
			else
				invalidExpressions += "\n  "+p;
		}			
		String tooltip = description;
		
		if (!isValidInterval(intervalLo.getText(), intervalHi.getText())) {
			if (description.length()>0) description+="\n";
			description += "Invalid interval.";
		} else {
			String countText = 
				(parts.size()==0 ? "No equations selected." :
					validParts==0 ? "No equations can be plotted." :
						parts.size()==1 ? "The equation can be plotted." :
						parts.size()==2 && validParts==2 ? "Both equations can be plotted." :
						validParts==parts.size() ? "All "+parts.size()+" equations can be plotted." :
							(parts.size()-validParts)+" of "+parts.size()+" equations cannot be plotted.");
			if (description.length()>0) description+="\n";
			description+=countText;
			tooltip = description + (validParts<parts.size() ? 
					"\n\n"+"An equation can only be plotted if it contains " +
					"exactly one variable from the 'range' column and one variable from the 'domain' column. " +
					"\n\n"+"The following equations are not valid:"+
					invalidExpressions : "");
		}
		updateDescription(description, tooltip);
		
		StringBuffer mapleGoalSetup = new StringBuffer() ;
		if (constants.getItemCount()>0) {
			mapleGoalSetup.append("# these 'variables' must be set to a constant for plotting"+"\n");
			for (String i : constants.getItems()) {
				mapleGoalSetup.append(i+" := "+findOldValueOfVariable(this.problemTabComposite.
						getProblemMode().getGoalSetup(), i, "0")+";"+"\n");
			}
			mapleGoalSetup.append("\n");
		}			
		mapleGoalSetup.append("# use 'default' (text), 'x11' (if enabled), or see ?plot[device]\n");			
		mapleGoalSetup.append("plotsetup("+findOldArgumentOfFunction(gs, "plotsetup", getDefaultPlotMode())+");\n");
		mapleGoalSetup.append("\n");
		mapleGoalSetup.append("# internal code for simultaneous plot support (do not modify)\n");
		for (String i : range.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"y;\n");
		}
		for (String i : domain.getItems()) {
			mapleGoalSetup.append(i+" := "+plotPrefix+"x;\n");
		}
		
		gs = mapleGoalSetup.toString();
		
		StringBuffer mapleGoalQuery = new StringBuffer() ;					

		
		int numParts = 0;
		for (MathsExpression p : parts) {
			if (!isExpressionPlottableForSelection(p))
				continue;
			numParts++;
			mapleGoalQuery.append(plotPrefix+"eq"+numParts+" := solve( "+p+" , "+plotPrefix+"y );\n");
		}			

		if (numParts==0) 
			mapleGoalQuery.append("# ERROR!  no equations can be plotted for selection");			
		else {
			mapleGoalQuery.append("printf(\"\\nplot should appear shortly (unless error appears below);\\n" +
				"cancel or kill maple to close\\n\\n\");\n");
			mapleGoalQuery.append("plot( [ ");
			for (int i=1; i<=numParts; i++) {
				if (i>1) mapleGoalQuery.append(", ");
				mapleGoalQuery.append(plotPrefix+"eq"+i);
			}
			mapleGoalQuery.append(" ],\n" +
					"  "+plotPrefix+"x="+intervalLo.getText()+".."+intervalHi.getText() );

			mapleGoalQuery.append(",\n legend=[\n");
			int i=1;
			for (MathsExpression p : parts) {
				if (!isExpressionPlottableForSelection(p))
					continue;
				if (i>1) mapleGoalQuery.append(",\n");
				mapleGoalQuery.append("    \""+problemTabComposite.external().tidyBrackets(p, true, true, true)+"\" $ nops(["+plotPrefix+"eq"+i+"])");
				i++;
			}
			mapleGoalQuery.append(" ],\n" +
					" labels=[\"\",\"\"] );"+"\n");
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
	
	static String getDefaultPlotMode() {
		//TODO should be in a 'config' tab
		return "x11";
	}

}