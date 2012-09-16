package org.cognetics.proverspalette.qepcad;

import java.util.ArrayList;
import java.util.List;

import org.cognetics.proverspalette.translation.VariableBinding;
import org.cognetics.proverspalette.translation.VariableBinding.BindingType;

public class QepcadProblem { 
	
	// this class can create our problem object,
	// i.e. all the data we need to pass to the QEPCAD process
	
	/** e.g. 'qepcad', or null to use system default (which is qepcad) */
	public String executable = null; 
	
	/** "cells" -- minimum 2 000 000; less than 0 means use default */
	public long memory = -1;
	
	/** problem name, defaults to 'unnamed' */
	public String problemName = "unnamed";
	
	/** multi array of variables with their bindings;
	 * starts empty 
	 * For example have {{"x","y","z"} {"A", "G",null}}
	 * x is element [0][0]
	 * G is element [1][1]
	 */
	
	public VariableBinding[] variablesAndBindings = {};
	
	public static final int ALL_VARS_ARE_FREE = -1;
	public static final int ALL_VARS_ARE_BOUND = 0;
	
	/** how many of the variables in "variables" are free;
	 * note these are expected to be at the beginning of the list
	 * <p>
	 * can use special constants ALL_VARS_ARE_FREE and ALL_VARS_ARE_BOUND
	 * default value is set to ALL_VARS_ARE_BOUND
	 * TODO- how do I want to use these special constants?
	 */
	public int freeVars = ALL_VARS_ARE_BOUND;
	
	public String goal;
	
	public List<String> normalizationAssumptions = new ArrayList<String>();
	
	public static final String DEFAULT_PROJ = null;
	public String projection = DEFAULT_PROJ;
	
	//TODO- introduce field for cadType
	//public static final String DEFAULT_CAD = null;
	//public String cadType = DEFAULT_CAD;
	
	//TODO- introduce field for solConstruction
	//public static final String DEFAULT_SOLConstruction = null;
	//public String solConstrcuction = DEFAULT_SOLConstruction;
	
	// do we need a class constructor? no - java will use the default class
	// constructor which makes the object public and take no params

	// public QepcadProblem() {}

	/** creates a string to be used to send the entire problem / execution spec
	 * to Qepcad in batch mode.  (note that memory must be accessed when 
	 * launching qepcad.)
	 **/	
	public String getQepcadInput() {
		StringBuffer sb = new StringBuffer();
		StringBuffer vars = new StringBuffer();
		//problem name
		sb.append("[" + this.problemName+ "]\n");

		//Makes sure variables are in right order if some vars are bound (free first); 
		//Must preserve order of quantified vars from original problem
		//TODO - make use of special constant ALL_VARS_ARE_FREE
		//TODO - what do we return if there are no variables in our problem?
		
		
		int numVars = variablesAndBindings.length;
		sb.append("(");
		for (int i=0; i<numVars; i++) {
			if (this.variablesAndBindings[i].bindingType.equals(VariableBinding.BindingType.FREE)) {
				vars.insert(0, this.variablesAndBindings[i].varName + ","); 
			}
			else {
				vars.append(this.variablesAndBindings[i].varName + ",");
			}
		}
		sb.append(vars);
		if (sb.charAt(sb.length()-1)!='(')
			sb.deleteCharAt(sb.length() -1);
		else {
			//the invalid input is now picked up before sending to qepcad
//			sb.append(")\n\nERROR: no variables found; there is something wrong with the goal");
//			return sb.toString();
		}
		sb.append(")\n");

		//number which are free
		//TODO - when is the special constant ALL_VARS_ARE_FREE introduced in the instantiation?
		//TODO - automatically instantiate freeVars by counting "free" in variablesAndBindings
		switch (this.freeVars) {
//		case ALL_VARS_ARE_BOUND: sb.append("0"); break;
		case ALL_VARS_ARE_FREE: sb.append(numVars); break;
		default:
			sb.append(this.freeVars);
		}
		sb.append("\n");
		
		// Goal: QEPCAD doesn't understand boolean operator precedence -
		// must use square brackets to group expressions!
		// TODO- Warn user of the bracketing or do this automatically?
		//       Can turn flag on in Isablle to get groupings?
		// TODO- Are we going to use qepcad syntax or xsymbol?
		
        // We add the bound vars with their bindings to the beginning
		// of the problem statement
		//TODO - Ordering of quantified vars must be preserved! If goal was in PNF
		//then all quantifiers are at front. However some vars in Isabelle are free
		//but we may bind them before sending to QEPCAD in order to eliminate them,
		//i.e. EX y. y > x 
		//really means
		//   ALL x. EX y. y > x 
		// 
		String binds = getVariableBindingString();
		sb.append(binds);
		if (binds.length()>0)
			sb.append("\n");
		// TODO - goal must contain a relational operator - check
		sb.append("[" + this.goal+ "].\n");  
		
		// Normalization phase: can add assumptions here
		// if user wants to add equational constraints
		// need to issue the command "prop_eqn_const"
		// Can enable full-cad or partial-cad in this phase
		
		int numAssumptions = normalizationAssumptions.size();
		for (int i=0; i<numAssumptions; i++) {
			sb.append("assume [");
			sb.append(normalizationAssumptions.get(i));
			sb.append("]\n");
		}
		
		// Need to enter go to end this normalisation phase.
		sb.append("go\n");
	
		
		// Projection phase: Default is McCallum 
		// The default produces a smaller projection factor set and Qepcad sometimes
		// discovers during the Stack Construction Phase that it cannot be sure of the
		// validity of this projection
		// The alternative projection is Hong's which is always valid, but produces a
		// larger projection factor set
		// If McCallum's projection produces an error message for a problem involving k
		// variables, you should issue the command "proj_op(m,m,h,h,h,..,h)"
		// where the list has k-1 elements, all but the first 2 of which are h
		// TODO- implement command if Hong projection selcted
		// TODO- in future development give people option to interact and tell
		// 			QEPCAD which projection factors are safe to remove
		// TODO- type go if less than 2 variables
	
		if (projection==null || projection.equals("McCallum")){
			sb.append("go\n");
		} else {
			if (numVars < 2){ 	
				sb.append("go.\n");
			}else{
				sb.append("proj-op (");
				for( int i=0; i < variablesAndBindings.length - 1; i++) {
					if (i>0) sb.append(",");
					if (i<2){
						sb.append("m");
					} else {
						sb.append("h");
					}
				}
				sb.append(")\n");
			}
			sb.append("go\n");
		}
		
		
		// TODO- give options for cad type
		// sb.append(this.cadType + "\n");
		sb.append("go\n");
		
		// Solution Construction Phase: to get more options use the "solutin_extension"
		// command. Options:
		// 		T (default option) - If CAD not definable, polys added to projection
		//							factor set
		//		E - Produces formulas in language of Extended Tarski formulats. 
		//			Projection factors are never added to the projection factor set
		//		G - Tries to give insight into the geometry of the solution set
		// 		H- Calls original slution formula construction routine written by Hong.
		//			Can produce nec. and suff. conditions rather than true defining
		// 			formula.
		//		I - Enters user into interactive mode, which gives im more control over
		//			how solution formulas are constructed
		//
		// TODO- Implement options for solution construction phase 
		// TODO- Want user to scroll over solution construction options (i.e. T) and 
		//			get dialog box popping up explaining what this option does
		
		//sb.append(this.solConstrcuction + "\n");
		
		//can ask for witness if we have a problem that is fully existentially quantified
		 if(existentiallyQuantified(this.variablesAndBindings)){sb.append("d-witness\n");}
		
		
		sb.append("go\n");
		
		// TODO - last four phases can be by-passed with "finish"
		
		return sb.toString();
	}

	public String getVariableBindingString() {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<variablesAndBindings.length; i++) {
			if (this.variablesAndBindings[i].bindingType.equals(BindingType.FREE)){}
			else{
				sb.append("(");
				sb.append(new QepcadTranslator().getQuantificationText(
						this.variablesAndBindings[i]));
				sb.append(")");
			}
		}
		return sb.toString();
	}

	private boolean existentiallyQuantified(VariableBinding[] variablesAndBindings) {
		//check each variables is quantified by EX
		for (VariableBinding a : variablesAndBindings){
			
			if(!a.bindingType.equals(BindingType.EXISTS)){
				return false;
			}
		}
		return true;
	}

// a good test with Isabelle is:
// lemma "EX (px::int) py qx qy rx ry sx sy. 
//    ((sx * py - sy * px > 0) ∧  
//            (sx * qy - sy * qx > 0) ∧ 
//            (sx * ry - sy * rx > 0) ∧ 
//            (qx * ry - qy * rx > 0))"
//   apply (rule_tac x="-1" in exI)
//   apply (rule_tac x="-1" in exI)   
//   apply (rule_tac x="-1" in exI)   
//   apply (rule_tac x="-3" in exI)   
//   apply (rule_tac x="-1" in exI)   
//   apply (rule_tac x="-5" in exI)     
//   apply (rule_tac x="-1" in exI)
//   apply (rule_tac x="0" in exI)
//   apply simp
//   done 

}



	