
package org.cognetics.proverspalette.translation;

public class VariableBinding {
	public String varName;
	public BindingType bindingType;
	public boolean isImplicit;
	
	public static enum BindingType {
		UNKNOWN(""),
		FREE(""),
		ALL("A"), EXISTS("E"), FINITELY_MANY("F"), ALMOST_ALL("G");
		//CONNECTED and EXACTLY k not supported
		//TODO - isn't F finitely many?
		
		//TODO should have 'AMBIGUOUS' in addition to 'UNKNOWN'
		
		final String simpleLabel;
		
		private BindingType(String simpleLabel) {
			this.simpleLabel = simpleLabel;
		}
		
		/** the 'simple' label is the simple character equivalent for this binding,
		 * following the definitions of qepcad
		 * @return
		 */
		public String getSimpleLabel() {
			return simpleLabel;
		}

		/** returns the binding type for the given simple binding string (A, E, F, and G supported) */
		public static BindingType getBindingForQuantifierSimpleLabel(String qw) {
			if (qw==null && qw.length()==0) return null;
			for (BindingType t : values()) {
				if (qw.equals(t.getSimpleLabel())) return t;
			}
			if (qw.equalsIgnoreCase("free"))
				return BindingType.FREE;
			return null;
		}

		public static String[] getAllSimpleLabels() {
			return new String[] { "A", "E", "F", "G" };
		}
	}

	//not used anymore
//	/** optional flag, can be set if the word here is used as though it is a predicate
//	 * (rather than as a variable) */
//	public Boolean isTreatedLikePredicate = null;
	
	public VariableBinding() {
		varName = "";
		bindingType = BindingType.UNKNOWN;
		isImplicit = false;
	}
	public VariableBinding(String varName, BindingType bindingType, boolean isImplicit) {
		this.varName = varName;
		this.bindingType = bindingType;
		this.isImplicit = isImplicit;
	}
	
	@Override
	public String toString() {
		return super.toString()+"["+bindingType+":"+varName+"]";
	}
}