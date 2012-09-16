package org.cognetics.proverspalette.base.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ProversPaletteAbstractExternalProcess implements ProversPaletteExternalInvocation {

	Process process = null;

	public final StringBuffer outputCollected = new StringBuffer();

	public static String getTimeString() {
		return DateFormat.getDateTimeInstance().format(new Date());
	}

	protected void addNewOutputLine(String line) {
		synchronized (outputCollected) {
			outputCollected.append(line);
			outputCollected.append("\n");
			for (OutputListener l : outputListeners) {
				l.newOutput(line, outputCollected.toString());
			}
		}
	}

	public String getResult() {
		return result;
	} 

	public List<String> getErrors() {
		return errors;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}
	
	protected List<String> errors = new ArrayList<String>();
	protected List<String> warnings = new ArrayList<String>();
	/** the equivalent form returned by the external sytem, or null if error or not available */
	protected String result = null;

	protected byte[] input;

	public String getInput() {
		return new String(input);
	}

	protected byte[] specialInput = null;
	/** true iff the input script was set, i.e. it didn't come from the problem */
	public boolean isInputManual() {
		return specialInput!=null;
	}
	public void setManualInput(byte[] specialInput) {
		this.specialInput = specialInput;
	}

	public synchronized void run() {
		try {
			final Runtime rt = Runtime.getRuntime();			
			final String cmd = getShellCommand();
			final AtomicBoolean processStartupCompleted = new AtomicBoolean(false);
			synchronized (processStartupCompleted) {
				final AtomicReference<IOException> thrownError = new AtomicReference<IOException>(null);
				
				new Thread() {
					public void run() {
						try {
							process = rt.exec(cmd);
						} catch (IOException e) {
							thrownError.set(e);
						}
						synchronized (processStartupCompleted) {
							processStartupCompleted.set(true);
							processStartupCompleted.notify();
						}
					}
				}.start();
				processStartupCompleted.wait(10*1000);
				if (!processStartupCompleted.get()) {
					System.out.println("process apparently failed to start; interrupting");
					process.destroy();
					throw new RuntimeException("The process blocked while starting. This may be due to an Eclipse OS bug if your Eclipse Studio is running as a backgrounded process (esp on Linux).");
				}
				if (thrownError.get()!=null) {
					throw (IOException) thrownError.get();
				}
			}
			
			System.out.println(getTimeString()+"  started process "+cmd);
			
			//output to the process (i.e. the process's _input_)
			OutputStream out = process.getOutputStream();			
			try {
				input = getInputForExternalProcess();
				out.write(input);
				out.flush();  //make sure this gets written immediately (otherwise it may buffer / wait indefinitely)
			} catch (IOException e) {
				addNewOutputLine("Error: unable to write data ("+e+")\n");
				//continue to run, in case it has exitted already we want to collect the exit code
			}
			//System.out.println("sent problem"+prob.getSystemInput());						
			
			final Process thisProcess = process;
			
			Thread processOutputGobbler = new Thread(new Runnable() {
				public void run() {

					try {
					//readers (and writers) are better for strings
					//(streams are just bytes)
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(thisProcess.getInputStream()));
					//here we have several lines to read...
					//but we expect the system will terminate
					//so we can just read until readline returns null
					//(because we have read everything and process has ended)
					String line = reader.readLine();
					while (line!=null) {
						System.out.println(getTimeString()+"  process output: "+line);
						//cheat, insert a new line before the word 'Enter' to make the output look better
						if (line.startsWith("Enter ")) 
							line = "\n"+line;
						else line = line.replaceAll("Enter ", "\n\nEnter ");
						addNewOutputLine(line);
						line = reader.readLine();
					}
					} catch (IOException e) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException ei) {
							ei.printStackTrace();
						}
						if (process!=null) {
							//sleep and check the process to suppress this error when interrupted
							System.err.println(ProversPaletteAbstractExternalProcess.this+" reader got exception");
							e.printStackTrace();
						}
					}

				}
			});
			
			processOutputGobbler.start();
			
			Thread processErrorGobbler = new Thread(new Runnable() {
				public void run() {

					try {
					//readers (and writers) are better for strings
					//(streams are just bytes)
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(thisProcess.getErrorStream()));
					//here we have several lines to read...
					//but we expect the system will terminate
					//so we can just read until readline returns null
					//(because we have read everything and process has ended)
					String line = reader.readLine();
					while (line!=null) {
						System.out.println("process error: "+line);
						for (OutputListener l : outputListeners) {
							l.newErrorOutput(line);
						}
						line = reader.readLine();
					}
					} catch (IOException e) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException ei) {
							ei.printStackTrace();
						}
						if (process!=null) {
							//sleep and check the process to suppress this error when interrupted
							System.err.println(ProversPaletteAbstractExternalProcess.this+" reader got exception");
							e.printStackTrace();
						}
					}

				}
			});
			
			processErrorGobbler.start();
			
			int exitCode = process.waitFor();
			//p.destroy();
			
			analyzeOutput(exitCode);
		}
		catch (InterruptedException e) {
//			System.out.println("process interrupted");
			errors.add(e.toString()+" (probably cancelled; see console output for more information)");
		}
		catch(Exception e){	
			e.printStackTrace();
//			System.out.println("process failed");
			errors.add(e.toString()+" (see console output for more information)");
		} finally {
			process = null;
		}
	}

	/** searches for the search phrase _and_ at least one following nonblank char,
	 * up to the next blank line */
	protected List<String> findPhrasesInBuffer(StringBuffer outputCollected, String searchPhrase, boolean includeSearchPhrase, String endMarker) {
		List<String> l = new ArrayList<String>();
		int index = outputCollected.toString().indexOf(searchPhrase);
		while (index>=0) {
			int endIndex = index + searchPhrase.length();
			while (endIndex < outputCollected.length() && 
					Character.isWhitespace(outputCollected.charAt(endIndex)))
				endIndex++;
			endIndex = outputCollected.indexOf(endMarker, endIndex);
			if (endIndex==-1) endIndex = outputCollected.length();
			String msg;
			if (includeSearchPhrase) msg = outputCollected.substring(index, endIndex);
			else msg = outputCollected.substring(index + searchPhrase.length(), endIndex);
			l.add(msg);
			
			index = outputCollected.toString().indexOf(searchPhrase, endIndex);
		}
		
		return l;
	}
	
	/** finds all phrases which are wrapped by leftBookend and rightBookend */ 
	public static List<String> findPhrasesBookendedBy(String target,
			String leftBookend, String rightBookend, boolean trim) {
		List<String> result = new ArrayList<String>();
		do {			
			int outStart = target.indexOf(leftBookend);
			if (outStart==-1)
				return result;
			target = target.substring(outStart + leftBookend.length());
			
			int outEnd = target.indexOf(rightBookend);
			if (outEnd==-1)
				return result;
			
			String part = target.substring(0, outEnd);
			if (trim) part = part.trim();
			result.add(part);
			
			target = target.substring(outEnd + rightBookend.length());
		} while (true);
	}
	
	List<OutputListener> outputListeners = new ArrayList<OutputListener>();
	
	public void addOutputListener(OutputListener l) {
		outputListeners.add(l);
	}	
	
	public void interrupt() {
		if (process!=null) process.destroy();
	}

	public String getAllOutput() {
		return outputCollected.toString();
	}
	
	public boolean isResultInteresting() {
		if (getResult()==null) return false;
		String r = getResult().replaceAll("\\s+", "");
		String in = new String(input).replaceAll("\\s+", "");
		if (in.matches(".*"+r+"\\]?.go"))
			//output is exactly what was input
			return false;
		return true;
	}

	
	protected String annotatedResult = null;
	public String getAnnotatedResult() {
		return annotatedResult==null ? getResult() : annotatedResult;
	}
		
	public void setAnnotatedResult(String annotatedResult) {
		this.annotatedResult = annotatedResult;
	}

	/** returns the shell command which should be invoked for this process */
	protected abstract String getShellCommand();

	/** returns the byte stream which should be sent to the process as input */
	protected abstract byte[] getInputForExternalProcess();

	/** by inspecting getAllOutput, this should set result and/or errors/warnings, 
	 * optionally also annotated result (though the preview tab may also do that) 
	 * @throws InterruptedException */
	protected abstract void analyzeOutput(int exitCode) throws InterruptedException;

	public abstract String toString();

}
