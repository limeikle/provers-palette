package org.cognetics.proverspalette.base.gui;

import java.util.Collection;
import java.util.Set;

import org.cognetics.proverspalette.translation.VariableBinding;
import org.heneveld.maths.structs.MathsExpression;

public abstract class ImportTabComposite extends ProversPaletteTabCompositeAbstract {

	public ImportTabComposite(ProversPaletteViewPartAbstract view, String tabLabel, int style) {
		super(view, tabLabel, style);
	}

	public abstract Set<MathsExpression> getCurrentSelectedAssumptions();

	/** null if conclusion not selected */
	public abstract MathsExpression getConclusionIfSelected();

	public abstract Collection<? extends VariableBinding> getSelectedVariables();

	public abstract boolean isEverythingSelected();

}
