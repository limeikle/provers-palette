package org.cognetics.proverspalette.maple.gui;

import java.util.LinkedHashSet;
import java.util.Set;

import org.cognetics.proverspalette.base.ProofGeneralScriptingUtils;
import org.cognetics.proverspalette.base.gui.ProversPaletteViewPartAbstract;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.heneveld.maths.structs.MathsExpression;
import org.heneveld.maths.structs.MathsToken;

public class StartTabComposite extends
		org.cognetics.proverspalette.base.gui.StartTabComposite {
	
	public Button buttonConvertToPnf = null;
	public Button buttonExpandPredicates = null;

	public StartTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
		createContents();
	}

	// --------------- widget gui creation ----------------------------

	protected void makeProverTidyingButtons(Composite buttons) {
		// Need to check if subgoal is in PNF
		// If not, give user warning and enable button to insert Isabelle command 
		
		buttonConvertToPnf = new Button(buttons, SWT.PUSH);
		buttonConvertToPnf.setText("&PNF");
		buttonConvertToPnf.setToolTipText("The goal is not in PNF but PNF is required by QEPCAD. " +
				"This button will insert a command to attempt to automatically convert the current prover goal to PNF form.");
		buttonConvertToPnf.setEnabled(false);
	
		buttonConvertToPnf.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try{
					ProofGeneralScriptingUtils.insertInProofScript(prover().getCommandForConvertingToPnf(getProverProblemTextCurrentlyDisplayed()),true );
				}catch(InterruptedException ex){
					ex.printStackTrace();
					//	throw ex;
				}
			}
		});
		
		buttonExpandPredicates = new Button(buttons, SWT.PUSH);
		buttonExpandPredicates.setText("&Expand");
		buttonExpandPredicates.setToolTipText("The goal contains predicates which will not be understood by Maple. " +
				"This button will insert a command to attempt to automatically expand those predicates. " +
				"(It is not guaranteed to succeed, but even if if fails it might still be useful for manual conversion.)");
		buttonExpandPredicates.setEnabled(false);
	
		buttonExpandPredicates.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try{
					MathsExpression problemProver = prover().parse( getProverProblemTextCurrentlyDisplayed() );
					MathsExpression problemCommonLang = view.getCurrentProverTranslator().toCommon(problemProver);
					Set<MathsExpression> unknownPredicatesSystem = view.getCurrentExternalSystemTranslator().
						getUnknownPredicates( view.getCurrentExternalSystemTranslator().fromCommon(problemCommonLang) );
					ProofGeneralScriptingUtils.insertInProofScript(							
							prover().getCommandForExpandingPredicates(unknownPredicatesSystem),true );
				}catch(InterruptedException ex){
					ex.printStackTrace();
				}
			}
		});
	}

	

	protected boolean updateFields(String problemTextProver,
			MathsExpression problemProver, boolean isValid) {
		isValid &= super.updateFields(problemTextProver, problemProver, isValid);
		
		buttonConvertToPnf.setEnabled( 
				view.getCurrentProverTranslator().shouldSuggestConvertToPnf(problemProver) );
		
		MathsExpression problemCommonLang = view.getCurrentProverTranslator().toCommon(problemProver);
		Set<MathsExpression> unknownPredicatesSystem = view.getCurrentExternalSystemTranslator().
			getUnknownPredicates( view.getCurrentExternalSystemTranslator().fromCommon(problemCommonLang) );					
		buttonExpandPredicates.setEnabled(
				view.getCurrentProverTranslator().shouldSuggestExpandUnknownPredicates(
						problemProver, unknownPredicatesSystem));
		
		return isValid;
	}
	
	@Override
	public void resetFields() {
		super.resetFields();		
		buttonConvertToPnf.setEnabled(false);
		buttonExpandPredicates.setEnabled(false);		
	}


}
