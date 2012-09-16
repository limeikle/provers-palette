package org.cognetics.proverspalette.maple;

import org.cognetics.proverspalette.maple.gui.ProblemTabMode;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsExpressions;

public class MapleProblem {
	
	// this class can create our problem object,
	// i.e. all the data we need to pass to the Maple process
	
	/** e.g. 'maple' to use system default */
	public String executable = null; 
	
	public String problemName = "unnamed";
	
	/** maple goal has two parts. goal-query is the part whose output we are interested in. */
	public String goalSetup;
	public String goalQuery;
	public ProblemTabMode mode;
	
//	private String DATE = SimpleDateFormat.getDateTimeInstance().format(new Date());
	//split up so we don't find the token in the generated output
	public String MARKER_START_TOKEN_1 = "BEGIN";
	public String MARKER_END_TOKEN_1 = "END";
	public String MARKER_START_TOKEN_2 = "[Prover's Palette Result]";
	public String MARKER_END_TOKEN_2 = "[Prover's Palette Result]";
	
	/** creates a string to be used to send the entire problem / execution spec
	 * to Maple in batch mode.  
	 **/	
	public String getMapleInput() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("restart;\n");
		
		//TODO - Currently just set domain to be Real, but for future work we will want to collect all
		// vars and their types and tell Maple "assume(var::type)"
		sb.append("interface(ansi=false,prettyprint=0,errorbreak=0):;\n");
		sb.append("with(RealDomain):\n");
		
		sb.append(goalSetup.trim()+"\n");
		
		sb.append("interface(echo=0):; # must set echo off and wrap output\n");
		sb.append("printf(\""+MARKER_START_TOKEN_1+
				//"\");printf(\""+
				MARKER_START_TOKEN_2+"\\n\\n\");\n");
		
		sb.append(goalQuery.trim()+"\n");
		
		sb.append("printf(\"\\n"+MARKER_END_TOKEN_1+
				//"\");printf(\""+
				MARKER_END_TOKEN_2+"\\n\");\n");

		if (mode!=null && !mode.shouldLeaveSessionOpen())
			sb.append("quit;\n");
		
		return sb.toString();		
	}
		
	public String getForFinishTab() {
		StringBuffer sb = new StringBuffer();
		sb.append(goalSetup.trim());
		if (sb.length()>0) sb.append("\n");
		sb.append(goalQuery.trim()+"\n");
		return sb.toString();
	}
	
	public MathsExpression getAsCommonLanguage() {
		//TODO
		return MathsExpressions.newToken("TODO_MaplePorlbem_getAsCommonLAn", false);
	}

}
