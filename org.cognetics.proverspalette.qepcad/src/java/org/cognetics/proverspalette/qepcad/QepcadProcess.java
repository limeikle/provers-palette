package org.cognetics.proverspalette.qepcad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cognetics.proverspalette.base.cli.ProversPaletteAbstractExternalProcess;

public class QepcadProcess extends ProversPaletteAbstractExternalProcess {
	
	private final QepcadProblem problem;

	private ExecutionMode mode;

	public QepcadProcess(QepcadProblem problem) {
		this(problem, ExecutionMode.NORMAL);
	}

	public QepcadProcess(QepcadProblem problem, ExecutionMode mode) {
		this.problem = problem;
		this.mode = mode;
	}

	public static enum ExecutionMode {
		NORMAL, COUNTER_EXAMPLE
	}

	public QepcadProblem getProblem() {
		return problem;
	}
	
	public ExecutionMode getExecutionMode() {
		return mode;
	}

	protected String witnessMessage = null;
	protected List<String> witnessList = null;
	
	public List<String> getWitnessList() {
		return witnessList;
	}
	public String getWitnessMessage() {
		return witnessMessage;
	}
	
	@Override
	public String toString() {
		return "QEPCAD problem '"+problem.problemName+"'";
	}

	protected String getShellCommand() {
		String cmd;
		if (problem.executable!=null) cmd = problem.executable;
		else cmd = "qepcad";
		if (problem.memory>=0)
			cmd = cmd + " +N"+problem.memory;
		return cmd;
	}
	
	protected byte[] getInputForExternalProcess() {
		if (isInputManual()) return specialInput;
		return problem.getQepcadInput().getBytes();
	}
	
	public static final String SUCCESSFUL_OUTPUT_MARKER = 
		"An equivalent quantifier-free formula:";
	
	public static final String SUCCESSFUL_WITNESSES_FOUND =
		"----------   Sample point  ----------";
	
	public static final String END_MARKER =
		"=====================  The End  =======================";

	protected void analyzeOutput(int exitCode) throws InterruptedException {
		System.out.println(getTimeString()+"  exit code is "+exitCode);
		long outputCollectionOverrunMaxTime = System.currentTimeMillis() + 10*1000;
		
		if (exitCode!=0) {
			String msg = "Error: QEPCAD exited with unexpected exit code "+exitCode;
			if (exitCode==139) {
				msg += " (this is known to occur when the 'qe' environment variable is not being passed through; to fix, " +
						"rename the qepcad binary to something, e.g. qepcad-binary, and create a script qepcad which sets qe " +
						"then invokes qepcad-binary)";
			}
			msg += "\n";				
			addNewOutputLine(msg);
			//don't wait quite so long for output, as there probably isn't any
			outputCollectionOverrunMaxTime -= 8*1000;
		}

		while (findPhrasesInBuffer(outputCollected, END_MARKER, false, "\n\n").isEmpty()) {
			Thread.sleep(100);
			if (System.currentTimeMillis() > outputCollectionOverrunMaxTime)
				break;
		}

		errors = findPhrasesInBuffer(outputCollected, "Error", true, "\n\n");
		for (String s : errors)
			System.out.println(s.trim());
		
		warnings = findPhrasesInBuffer(outputCollected, "Warning", true, "\n\n");
		for (String s : warnings)
			System.out.println(s.trim());
								
		List<String> results = findPhrasesInBuffer(outputCollected, SUCCESSFUL_OUTPUT_MARKER, false, "\n\n");
		for (String s : results)
			System.out.println("RESULT: "+s.trim());
		if (results.size()>0) {
			if (results.size()>1)
				System.err.println("qepcad returned "+results.size()+" results; ignoring all but the first");
			result = results.get(0).trim();
		}

		collectWitnesses();
	}
		
	public static class AlgebraicRootWitnessException extends Exception {
		private static final long serialVersionUID = 1L;		
	}
	public static class WitnessOutputParseException extends Exception {
		private static final long serialVersionUID = 1L;		
	}

	public void collectWitnesses() {		
		int witnessSectionStart = outputCollected.indexOf(SUCCESSFUL_WITNESSES_FOUND);
		if (witnessSectionStart<0) return;
		String witnessText = outputCollected.substring(witnessSectionStart + SUCCESSFUL_WITNESSES_FOUND.length());
		int witnessSectionEnd = witnessText.indexOf("----------------");
		if (witnessSectionEnd<0) return;
		witnessText = witnessText.substring(0, witnessSectionEnd);
		
		int coordStart = witnessText.indexOf("Coordinate 1 =");
		if (coordStart<0) return;
		witnessText = witnessText.substring(coordStart);
		
		try {
			witnessList = collectWitnesses( witnessText );
			if (!witnessList.isEmpty()) {
				Iterator<String> wi = witnessList.iterator();
				int i=0;
				witnessMessage = "A witness was found: ("+
					(problem.variablesAndBindings.length>=1 ? problem.variablesAndBindings[0].varName+"=" : "")+
					wi.next();				
				while (wi.hasNext()) 
					witnessMessage += "," +
						(problem!=null && problem.variablesAndBindings.length>(++i) ? problem.variablesAndBindings[i].varName+"=" : "")+
						wi.next();
				witnessMessage += ")";
			};			
		} catch (AlgebraicRootWitnessException e) {
			witnessMessage = "A witness was found but it included algebraic roots. " +
			"Consult the output in the preview page for more information.";
		} catch (WitnessOutputParseException e) {
			System.out.println(witnessText);			
			e.printStackTrace();
			witnessMessage = "The text describing the witness was in an unexpected format. " +
			"Consult the output in the preview page for more information.";
		}
	}
	
	public static List<String> collectWitnesses(String allWitnessText) throws AlgebraicRootWitnessException, WitnessOutputParseException {
		
		List<String> witnessList = new ArrayList<String>();
		
		if (allWitnessText == null)
			return witnessList;
		
		allWitnessText = allWitnessText.trim();
		
		int coordNum = 1;
		while (allWitnessText.length() > 0) {
			int nextCoordIndex = allWitnessText.indexOf("Coordinate");
			if (nextCoordIndex<0) throw new WitnessOutputParseException();
			allWitnessText = allWitnessText.substring(nextCoordIndex + "Coordinate".length()).trim();
			if (!allWitnessText.startsWith(""+coordNum+" = ")) throw new WitnessOutputParseException();
			allWitnessText = allWitnessText.substring( (""+coordNum+" = ").length() ).trim();
						
			String candidate = allWitnessText;
			int endOfThisLine = candidate.indexOf('\n');
			if (endOfThisLine>=0) {
				candidate = candidate.substring(0, endOfThisLine).trim();
				allWitnessText = allWitnessText.substring(endOfThisLine).trim();
			} else {
				allWitnessText = "";
			}
			if (allWitnessText.startsWith("=")) {
				endOfThisLine = allWitnessText.indexOf('\n');
				if (endOfThisLine<0) allWitnessText = "";
				else allWitnessText = allWitnessText.substring(endOfThisLine).trim(); 
			}
			
			//check value is a number (maybe negative, maybe fraction)
			if (!candidate.matches("(- *)?[0-9]+( */ *[0-9])?"))
				throw new AlgebraicRootWitnessException();
			witnessList.add(candidate); 

			coordNum++;
		}
		
		return witnessList;
	}
	
}
