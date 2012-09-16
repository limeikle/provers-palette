package org.cognetics.proverspalette.maple.gui;

import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation;
import org.cognetics.proverspalette.base.cli.ProversPaletteExternalInvocation.OutputListener;
import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.cognetics.proverspalette.maple.MapleDataException;
import org.cognetics.proverspalette.maple.MapleProblem;
import org.cognetics.proverspalette.maple.MapleProcess;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.heneveld.javautils.swtui.EasyScrollingStyledText;

public class PreviewTabComposite extends
		org.cognetics.proverspalette.base.gui.PreviewTabComposite {

	private StyledText outputStyledText = null;
	private StyledText inputStyledText = null;
	
	private Button goButton;
	private Button cancelButton;

	public PreviewTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
		createContents();
		setGoButtonEnablement(false);
	}

	@Override
	protected void createTabMainArea(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createInputComposite(sashForm);
		createOutputComposite(sashForm);
	}

	protected void createOutputComposite(Composite parent) {
		Group outputGroup = new Group(parent, SWT.NONE);
		outputGroup.setText(view.getName()+" Output");
		outputGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outputGroup.setLayout(new GridLayout(1, true));

		outputStyledText = new EasyScrollingStyledText(outputGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();		
		outputStyledText.setEditable(false);
		outputStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	protected void createInputComposite(Composite parent) {		
		Group inputGroup = new Group(parent, SWT.NONE);
		inputGroup.setText(view.getName()+" Input");
		inputGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		inputGroup.setLayout(new GridLayout(2, false));

		inputStyledText = new EasyScrollingStyledText(inputGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER).getStyledText();
		//editable is useful and now works
		//but some edits could cause the isabelle commands not to work
//		inputStyledText.setEditable(false);
		inputStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite executionButtons = new Composite(inputGroup, SWT.NONE);
		executionButtons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		executionButtons.setLayout(new GridLayout(1, true));

		goButton = new Button(executionButtons, SWT.PUSH);
		goButton.setText("Go");
		goButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		goButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				go(false);
			}
		});

		cancelButton = new Button(executionButtons, SWT.PUSH);
		cancelButton.setText("Cancel");
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cancelButton.setEnabled(false);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				cancel(true);
			}
		});
		cancelButton.setEnabled(false);
	}
	
	public void cancel(boolean allowMoreOutput) {
		try {
			synchronized (this) {
				if (!allowMoreOutput) noMoreOutputAllowed = true;
				if (externalCliProcessThread!=null) externalCliProcessThread.interrupt();
				if (currentlyExecutingInvocation!=null) currentlyExecutingInvocation.interrupt();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void resetFields(){
		//TODO do we need to cancel every time it is reset?  or should the caller have to cancel?
		cancel(false);
		outputStyledText.setText("");
		inputStyledText.setText("");
		currentlyExecutingInvocation = null;
		lastInvocationCompleted = null;
	}

	/** null means look it up */
	protected boolean setGoButtonEnablement(Boolean isEnabled) {
		if (isEnabled==null) isEnabled = isProblemAvailable();
		if (goButton!=null) goButton.setEnabled(isEnabled);
		if (buttonFinish!=null) buttonFinish.setEnabled(isEnabled);
		return isEnabled();
	}
	
	ProversPaletteExternalInvocation currentlyExecutingInvocation = null;
	private ProversPaletteExternalInvocation lastInvocationCompleted = null;
	
	/** last problem successfully run, or null if there was an error */
	public ProversPaletteExternalInvocation getLastInvocationCompleted() {
		return lastInvocationCompleted;
	}

	@Override
	protected void createNextButton(Composite buttonsRow) {
		//no next button
	}

	@Override
	public void finish(boolean forceInterrupt) {
		go(forceInterrupt);
	}
	

	public synchronized boolean go(boolean forceInterrupt) {		
		if (externalCliProcessThread!=null) {
			if (forceInterrupt) {
				System.out.println("qepcad task scheduled for automatic running while another task is running, interrupting and retrying");
				new Thread() {
					@Override
					public void run() {
						//cancel anything running and wait for it to finish
						Thread runningExternalCliProcessThread = null;
						synchronized (this) {							
							runningExternalCliProcessThread = externalCliProcessThread;
							cancel(true);
						}
						if (runningExternalCliProcessThread!=null) {
							try {
								runningExternalCliProcessThread.join(1000);
							} catch (InterruptedException e) {
								System.err.println("next "+view.getName()+" task interrupted before scheduled");
								return;
							}
							if (externalCliProcessThread!=null) {
								System.err.println("earlier "+view.getName()+" thread did not terminate quickly; unable to schedule next task");
								return;
							}
						}
						System.out.println("previous "+view.getName()+" task interrupted; now running new task");
						go(true);
					}
				}.start();
				return true;
			}
			//already processing
			MessageDialog.openError(getDisplay().getActiveShell(), view.getName()+" Already Running", 
					view.getName()+" is busy processing an earlier command.\n\n" +
			"Click 'Cancel' then 'Go' to run the new input.");
			return false;
		}
		
		//inputStyledText.setText("initialising Qepcad Problem");
		//inputStyledText.setText(view.getProblemComposite().descriptionText.getText());		
		try {
			runInBackground(newExternalInvocation());
		} catch (Exception e) {
			outputStyledText.setText("(The current settings are not valid. \n" +
					"Check that all fields are instantiated. \n" +
					"The error reported was: "+e+".)\n");
			e.printStackTrace();
		}
		return true;
	}

	Thread externalCliProcessThread = null;
	public void runInBackground(final ProversPaletteExternalInvocation invocation) {		
		if (externalCliProcessThread!=null) {
			outputStyledText.setText("("+view.getName()+" is already running.)");
			//TODO could allow user to interrupt
			//(shouldn't come here since we disabled the go button)
			return;
		}
		setGoButtonEnablement(false);
		externalCliProcessThread = new Thread(new Runnable() {
			public void run() {
				runInForeground(invocation);
			}
		}, "provers-palette-"+view.getName());
		externalCliProcessThread.start();
	}

	private String lastOutputTextQueued = null;
	
	protected void setOutputText(final String msg, final boolean isFinal) {
		lastOutputTextQueued = msg;
		if (Display.getCurrent()!=null) {
			//have a display thread
			if (!noMoreOutputAllowed) {
				outputStyledText.setText(msg);
//				((ScrolledComposite)(outputStyledText.getParent().getParent())).
//				setOrigin(outputStyledText.computeSize(SWT.DEFAULT, SWT.DEFAULT));

				int yForTop =
					outputStyledText.getLineHeight(
							outputStyledText.getLineAtOffset(msg.length()) ) +
							outputStyledText.getLinePixel(										
									outputStyledText.getLineAtOffset(msg.length()) ) -
									outputStyledText.getSize().y;
				if (yForTop>=0)
					outputStyledText.setTopPixel(yForTop);
				noMoreOutputAllowed = isFinal;
			}
		} else {
			//not in display thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (!noMoreOutputAllowed) {
						if (msg.equals(lastOutputTextQueued)) {
							//above check will skip updates which have been superseded
							outputStyledText.setText(msg);
							int yForTop =
								outputStyledText.getLineHeight(
										outputStyledText.getLineAtOffset(msg.length()) ) +
										outputStyledText.getLinePixel(										
												outputStyledText.getLineAtOffset(msg.length()) ) -
												outputStyledText.getSize().y;
							if (yForTop>=0)
								outputStyledText.setTopPixel(yForTop);
//							outputStyledText.setCaretOffset(msg.length());
						}
						noMoreOutputAllowed = isFinal;
					}
				}
			});
		}
	}

	boolean noMoreOutputAllowed = false;

	protected void runInForeground(final ProversPaletteExternalInvocation invocation) {
		synchronized (this) {
			if (currentlyExecutingInvocation!=null) {
				System.err.println("Already running "+currentlyExecutingInvocation+" when setting new problem "+invocation);
			}
			currentlyExecutingInvocation = invocation;
		}
		try {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					cancelButton.setEnabled(true);
				}
			});
			lastInvocationCompleted = null;
			noMoreOutputAllowed = false;
			setOutputText("("+view.getName()+" is running, please wait)", false);
			invocation.addOutputListener(new OutputListener() {
				public void newErrorOutput(String line) {
					invocation.getWarnings().add(line);
					System.err.println(view.getName()+" gave error output: "+line);
				}
				public void newOutput(String newText, String allText) {					
					setOutputText(allText, false);					
				}				
			});

			invocation.run();
			
			updateOutputAfterRunning(invocation);

		} catch (Exception e) {
			e.printStackTrace();
			noMoreOutputAllowed = false;
			setOutputText("("+view.getName()+" execution had an error: "+e+")", true);
		} finally {
			//set this back to null so user can run another problem
			synchronized (this) {
				externalCliProcessThread = null;
				currentlyExecutingInvocation = null;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {					
						//re-enable the go button
						setGoButtonEnablement(null);
						cancelButton.setEnabled(false);
					}
				});
			}
		}

	}

	protected void updateOutputAfterRunning(final ProversPaletteExternalInvocation invocation) {
		final StringBuffer finalOutput = new StringBuffer(					
				invocation.getAllOutput().trim().length() >= 0 &&
						(invocation.getResult()==null ||
						!invocation.getAllOutput().trim().equals(invocation.getResult().trim()))
					?
							invocation.getAllOutput()+
							"\n= = = = = = = = = = = = = = OUTPUT SUMMARY = = = = = = = = = = = = = =\n\n"
					: "");

		if (!invocation.getErrors().isEmpty()) {
			finalOutput.append("The following error" + (invocation.getErrors().size()==1 ? " was" : "s were") + " reported");
			finalOutput.append(":\n\n");
			for (String error : invocation.getErrors())
				finalOutput.append("\t"+error.trim()+"\n");
		} else {
			//set this only when no errors
			lastInvocationCompleted = invocation;
			if (invocation.getResult()==null) {
				finalOutput.append("(No result available from "+view.getName()+"!)\n");
			} else {
				invocation.setAnnotatedResult( getAnnotatedResult(invocation) );
			}
		}
		
		if (!invocation.getWarnings().isEmpty()) {
			if (finalOutput.length()>0) finalOutput.append("\n");
			finalOutput.append("The following warning" + (invocation.getWarnings().size()==1 ? " was" : "s were") + " reported");
			finalOutput.append(":\n\n");
			for (String warning : invocation.getWarnings())
				finalOutput.append("\t"+warning.trim()+"\n");
		}

		if (invocation.getResult() != null) {
			if (!invocation.getErrors().isEmpty() || !invocation.getWarnings().isEmpty())
				finalOutput.append("\nRESULT" +
						(invocation.getErrors().isEmpty() ? "" : " (unreliable due to ERROR)")+
						":\n\t");
			finalOutput.append(invocation.getAnnotatedResult()+"\n");
		}			

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				//synchronize on above 'set currentlyExecutingProcess' block 
				//to ensure no new processes can start while the display update is current
				//(guarantees that all lastXxxx fields are consistent)
				synchronized (PreviewTabComposite.this) {
					if (currentlyExecutingInvocation!=null && currentlyExecutingInvocation!=invocation) {
						//only happens if subsequent problem already submitted, if so, skip the display update
						System.out.println("Skipping display update for finished problem as the problem run ("+invocation+") is no longer current");							
						return;
					}
					try {
						setOutputText(finalOutput.toString(), true);							
						
						if (invocation.getErrors().isEmpty()) {
							view.getFinishComposite().updateOnInvocationSuccess(invocation);
							markChangesApplied();
							if (isTabActive()) showFinishTab();
							
							autoActivateAfterRunning(invocation);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	protected int computeDataHashCode() {
		return inputStyledText.getText().hashCode();
	}

	@Override
	public void update() {
		cancel(false);
		resetFields();		
		if (setGoButtonEnablement(null)) {
			try {
				inputStyledText.setText(
						convertFromRealTextToDisplayText(generateMapleProblem().getMapleInput(), inputStyledText));
			} catch (MapleDataException e) {
				System.err.println("unable to generate maple problem: "+e);
			}
		}
		noteUpdateCompleted();
	}

	public void setOutputText(String newMessage) {
		outputStyledText.setText(newMessage);
	}
	
	//qepcad-specific

//	QepcadProblem currentInputQepcadProblem;

	protected boolean isProblemAvailable() {
		return view.getProblemComposite().isProblemAvailable();
	}

	/** sets up and returns the invocation which will run the external tool on the data collected at this point in time */
	public synchronized ProversPaletteExternalInvocation newExternalInvocation() throws Exception {
		try {
			MapleProblem problem = generateMapleProblem();			
			
//			for (VariableBinding vb : problem.variablesAndBindings) {
//				if (vb.bindingType==null || vb.bindingType==BindingType.UNKNOWN) {
//					return new ProversPaletteExternalFailure("(The current settings are not valid QEPCAD input. \n" +
//							"Variable '"+vb.varName+"' has ambiguous or undefined binding.)");
//				}
//			}
			
			MapleProcess mp = new MapleProcess(problem);
			
			if (inputStyledText.getText().trim().length()>0 &&
					!convertFromDisplayTextToRealText(inputStyledText.getText(), inputStyledText).trim().
					equals(problem.getMapleInput().trim())) {
				System.out.println("maple script looks like it has been editted; sending it anyway");
				mp.setManualInput(
						(convertFromDisplayTextToRealText(inputStyledText.getText(), inputStyledText).
								trim()+"\n").getBytes());
			} else {
				inputStyledText.setText(
						convertFromRealTextToDisplayText( problem.getMapleInput(), inputStyledText) );
			}
				
			return mp;
		} catch (Exception e) {
			inputStyledText.setText("");
			throw e;
		}
	}

	private MapleProblem generateMapleProblem() throws MapleDataException {
		MapleProblem problem = new MapleProblem();
		((ProblemTabComposite) ((MainMapleViewPart)view).getProblemComposite()).
			contributeSettingsToMapleProblem(problem);
		//TODO - add a config tab? then we can set path to where Maple is located?
		//((MainMapleViewPart)view).getConfigComposite().contributeSettingsToQepcadProblem(problem);
		return problem;
	}

	protected String getAnnotatedResult(ProversPaletteExternalInvocation invocation) {
		if (invocation instanceof MapleProcess) {
			return invocation.getResult().trim() + "\n\n";
		}
		return null;
	}
	
}
