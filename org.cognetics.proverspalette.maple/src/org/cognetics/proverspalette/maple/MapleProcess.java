package org.cognetics.proverspalette.maple;

import java.util.List;

import org.cognetics.proverspalette.base.cli.ProversPaletteAbstractExternalProcess;

public class MapleProcess extends ProversPaletteAbstractExternalProcess {
	
	private final MapleProblem problem;

	public MapleProcess(MapleProblem problem) {
		this.problem = problem;
	}

	@Override
	protected String getShellCommand() {
		//TODO come from preference or config tab ?
		String cmd = "maple";
	    
		return cmd;		
	}

	@Override
	protected void analyzeOutput(int exitCode) throws InterruptedException {
		System.out.println(getTimeString()+"  exit code is "+exitCode);
		long outputCollectionOverrunMaxTime = System.currentTimeMillis() + 10*1000;
		
		if (exitCode!=0) {
			String msg = "Error: Maple exitted with unexpected exit code "+exitCode;
			if (exitCode==139) {
				msg += " (this is known to occur on some Linuxes if eclipse is running as a backgrounded process)";
			}
			msg += "\n";				
			addNewOutputLine(msg);
			//don't wait quite so long for output, as there probably isn't any
			outputCollectionOverrunMaxTime -= 8*1000;
		}

		
		while (outputCollected.indexOf( (problem.MARKER_END_TOKEN_1+problem.MARKER_END_TOKEN_2).trim() )<0) {
			Thread.sleep(100);
			if (System.currentTimeMillis() > outputCollectionOverrunMaxTime)
				break;
		}

		errors.addAll(findPhrasesInBuffer(outputCollected, "\nError,", true, "\n"));
		for (String s : errors)
			System.out.println(s.trim());
		warnings.addAll(findPhrasesInBuffer(outputCollected, "\nWarning,", true, "\n"));
		
//		warnings = findPhrasesInBuffer(outputCollected, "Warning", true);
//		for (String s : warnings)
//			System.out.println(s.trim());
								
		
		List<String> results = findPhrasesBookendedBy(outputCollected.toString(), 
				(problem.MARKER_START_TOKEN_1+problem.MARKER_START_TOKEN_2).trim(), 
				(problem.MARKER_END_TOKEN_1+problem.MARKER_END_TOKEN_2).trim(), true);
		for (String s : results)
			System.out.println("RESULT: "+s.trim());
		if (results.size()>0) {
			if (results.size()>1)
				System.err.println("maple returned "+results.size()+" results; ignoring all but the first");
			result = results.get(0).trim();
		} else {
			addNewOutputLine("Error: Results not found in Maple output.");
			System.err.println("maple returned no recognisable results");
		}
	}

	@Override
	//TODO - give problem a name from Import Tab and set it as part of the Maple problem so that it can be passed here
	public String toString() {
		return "Maple problem '"+problem.problemName+"'";
	}

	public MapleProblem getProblem() {
		   return problem;
	}

	protected byte[] getInputForExternalProcess() {
		if (isInputManual()) return specialInput;
		return problem.getMapleInput().getBytes();
	}

}
