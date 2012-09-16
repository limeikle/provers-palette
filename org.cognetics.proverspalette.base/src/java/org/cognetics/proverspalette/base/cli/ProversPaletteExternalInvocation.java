package org.cognetics.proverspalette.base.cli;

import java.util.List;

public interface ProversPaletteExternalInvocation extends Runnable {
	
	List<String> getErrors();
	List<String> getWarnings();
	String getResult();
	String getAllOutput();
	
	void addOutputListener(OutputListener outputListener);
		
	public interface OutputListener {
		public void newOutput(String newText, String allText);
		public void newErrorOutput(String line);
	}

	void interrupt();
	boolean isResultInteresting();
	
	void setAnnotatedResult(String annotatedResult);
	/** can compute an annotation on the result or return the set value
	 * (invoked by PreviewTab) */
	String getAnnotatedResult();
}
