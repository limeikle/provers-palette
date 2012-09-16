package org.cognetics.proverspalette.base;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.actions.retargeted.NextCommandAction;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.DisplayCallable;
import ed.inf.utils.process.ThreadCaller;

public class ProofGeneralScriptingUtils {

	//old code, for comparison
//	/** will also throw runtime exceptions wrapping any other errors */
//	public boolean insertCommand(String c) throws InterruptedException {
//		try {
//			SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
//			if (sm==null) return false;
//			final ProofScriptDocument doc = sm.getActiveScript();
//			if (doc==null) return false;
//			if (doc.getLockOffset()>doc.getLockOffset()) return false;  //doc locked
//
//			final String command = "\n"+c;
////			command = Converter.getPlaintext("\n"+Converter.stringToXml(c));
//			System.out.println("inserting "+command);
//
//			final int offset = doc.getProcessedOffset();
//			new DisplayCallable("update command") {
//				public Object run() throws Exception {
//					doc.replace(offset+1, 0, command);			
//					return null;
//				}
//			}.runDisplayWaiting();
//
//			if (view.actionRunAfterInsert==null || !view.actionRunAfterInsert.isChecked())
//				//don't run if not selected
//				return true;
//
//			int targetOffset = offset + command.length();
//			
//			while (doc.getLockOffset() < targetOffset-1) {
//				NextCommandAction mySendCommand = new NextCommandAction(doc, sm) {
//					@Override
//					public void done(IJobChangeEvent event) {
//						super.done(event);
//						synchronized (this) { this.notifyAll(); }
//					}
//				};
//				while (mySendCommand.isBusy()) Thread.sleep(50);
//				
//				int lockStart = doc.getLockOffset();
//				synchronized (mySendCommand) {
//					mySendCommand.run();
//					if (mySendCommand.isBusy())
//						mySendCommand.wait();
//				}
//
//				if (doc.getLockOffset() <= lockStart+1) {
//					//failure
//					return false;
//				}
//			}
//			return true;
//
//		} catch (InterruptedException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new ThreadCaller.RuntimeExceptionWrapper(e);
//		} finally {
//			runNextCommandThread = null;
//			inserting = false;
//		}
//	}

	/** convenience to run a command */
	public static boolean insertInProofScript(String proofScriptText, final boolean proverRunNextInProofScript) throws InterruptedException {
		try {
			final SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
			if (sm==null) return false;
			final ProofScriptDocument doc = sm.getActiveScript();
			if (doc==null) return false;
			if (doc.getLockOffset()>doc.getLockOffset()) return false;  //doc locked

			final String command = "\n"+
				translateProverShortcutTextToProverDocumentText(proofScriptText);

//			command = Converter.getPlaintext("\n"+Converter.stringToXml(c));
			System.out.println("inserting "+command);

			final int offset = doc.getProcessedOffset();
			new DisplayCallable("update command") {
				public Object run() throws Exception {
					doc.replace(offset+1, 0, command);
					return null;
				}
			}.runDisplayWaiting();

			if (proverRunNextInProofScript) {
				
				int targetOffset = offset + command.length();
				
				while (doc.getLockOffset() < targetOffset-1) {
					NextCommandAction mySendCommand = new NextCommandAction(doc, sm) {
						@Override
						public void done(IJobChangeEvent event) {
							super.done(event);
							synchronized (this) { this.notifyAll(); }
						}
					};
					while (mySendCommand.isBusy()) Thread.sleep(50);
					
					int lockStart = doc.getLockOffset();
					synchronized (mySendCommand) {
						mySendCommand.run();
						if (mySendCommand.isBusy())
							mySendCommand.wait();
					}

					if (doc.getLockOffset() <= lockStart+1) {
						//failure
						return false;
					}
				}				
				
			}
			
			return true;
			
		} catch (InterruptedException e) {
			throw e;
		} catch (Exception e) {
			throw new ThreadCaller.RuntimeExceptionWrapper(e);
		}
	}

	private static String translateProverShortcutTextToProverDocumentText(
			String proofScriptText) {
		StringBuffer result = new StringBuffer();
		
		StringTokenizer st = new StringTokenizer(proofScriptText, " \t\n\r", true);
		while (st.hasMoreTokens()) {
			String word = st.nextToken();
			if (word.matches("\\s+")) 
				result.append(word);
			else
				result.append(
					ProofGeneralPlugin.getSomeSessionManager().getProver().getSymbols().
						useUnicodeForTyping(word) );
		}
		return result.toString();
	}

}
