
package org.cognetics.proverspalette.translation;

import java.util.LinkedHashSet;
import java.util.Set;

public class VariableType {
	public String varName;
	public PossibleTypes varType;
	
	public static enum PossibleTypes {
		UNKNOWN("", ""),
		COMPLEX("C", "Complex"), REAL("R", "Real"), RATIONAL("Q", "Rational"), INT("Z", "Integer"), NATURAL("N", "Natural");

		final String simpleLabel, niceLabel;
		
		private PossibleTypes(String simpleLabel, String niceLabel) {
			this.simpleLabel = simpleLabel;			
			this.niceLabel = niceLabel;
		}

		public String getSimpleLabel() {
			return simpleLabel;
		}

		public String getNiceLabel() {
			return niceLabel;
		}
		
		public static PossibleTypes getTypeForVariable(String var) {
			if (var==null || var.trim().length()==0) return null;
			var = var.trim();
			for (PossibleTypes t : values()) {
				if (var.equalsIgnoreCase(t.getSimpleLabel())) return t;
				if (var.equalsIgnoreCase(t.getNiceLabel())) return t;
			}
			return null;
		}

		static String[] allSimpleLabels = null; 

		public static String[] getAllSimpleLabels() {
			if (allSimpleLabels!=null) return allSimpleLabels;
			synchronized (PossibleTypes.class) {
				Set<String> result = new LinkedHashSet<String>();
				for (PossibleTypes p : values()) {
					if (p.simpleLabel.length()>0) result.add(p.getSimpleLabel());
				}
				allSimpleLabels = result.toArray(new String[0]);
			}
			return allSimpleLabels;
		}

		static String[] allNiceLabels = null; 

		public static String[] getAllNiceLabels() {
			if (allNiceLabels!=null) return allNiceLabels;
			synchronized (PossibleTypes.class) {
				Set<String> result = new LinkedHashSet<String>();
				for (PossibleTypes p : values()) {
					if (p.getNiceLabel().length()>0) result.add(p.getNiceLabel());
				}
				allNiceLabels = result.toArray(new String[0]);
			}
			return allNiceLabels;
		}

	}

	//not used anymore
//	/** optional flag, can be set if the word here is used as though it is a predicate
//	 * (rather than as a variable) */
//	public Boolean isTreatedLikePredicate = null;
	
	public VariableType() {
		varName = "";
		varType = PossibleTypes.UNKNOWN;
	}
	
	public VariableType(String varName, PossibleTypes varType) {
		this.varName = varName;
		this.varType = varType;
	}
	
	@Override
	public String toString() {
		return super.toString()+"["+varType+":"+varName+"]";
	}
}