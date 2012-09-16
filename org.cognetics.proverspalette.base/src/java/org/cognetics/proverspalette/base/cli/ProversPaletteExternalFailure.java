package org.cognetics.proverspalette.base.cli;

import java.util.Collections;
import java.util.List;

public class ProversPaletteExternalFailure implements ProversPaletteExternalInvocation {

	private String message;

	public ProversPaletteExternalFailure(String message) {
		this.message = message;
	}
	
	public List<String> getErrors() {
		return Collections.singletonList(message);
	}

	public String getAllOutput() {
		return "";
	}
	
	public String toString() {
		return getClass().getSimpleName();
	}

	public String getResult() {
		return null;
	}

	public void run() {
		//nothing needs doing
	}

	public void addOutputListener(OutputListener outputListener) {
	}

	public void interrupt() {
	}

	public boolean isResultInteresting() {
		return false;
	}
	
	public String getAnnotatedResult() {
		return getResult();
	}
	
	public void setAnnotatedResult(String annotatedResult) {
		//ignored
	}

	public List<String> getWarnings() {
		return Collections.emptyList();
	}
}
