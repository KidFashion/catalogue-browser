package catalogue_object;

/**
 * Link terms with their parents in each hierarchy
 * @author avonva
 *
 */
public class Applicability {

	private Term child;
	private Nameable parentTerm;
	private Hierarchy hierarchy;
	private int order;
	private boolean reportable;
	
	
	public Applicability( Term child, Nameable parentTerm, Hierarchy hierarchy, int order, boolean reportable ) {
		this.child = child;
		this.parentTerm = parentTerm;
		this.hierarchy = hierarchy;
		this.order = order;
		this.reportable = reportable;
	}
	
	public Term getChild() {
		return child;
	}
	public Nameable getParentTerm() {
		return parentTerm;
	}
	public Hierarchy getHierarchy() {
		return hierarchy;
	}
	public int getOrder() {
		return order;
	}
	public boolean isReportable() {
		return reportable;
	}
	public void setReportable(boolean reportable) {
		this.reportable = reportable;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	
	/**
	 * Is the hierarchy of the applicability the one passed as input?
	 * @param h
	 * @return
	 */
	public boolean hasHierarchy ( Hierarchy h ) {
		
		if ( h == null ) {
			try {
				throw new Exception( "Null argument for Applicability:hasHierarchy regarding " + this );
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		
		return hierarchy.getId() == h.getId();
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		
		Applicability appl = (Applicability) obj;
		
		// if same term and same hierarchy => we have the same applicability (only one parent is allowed)
		boolean equals = appl.getChild().getId() == child.getId() && 
				appl.getHierarchy().getId() == hierarchy.getId();
		
		return equals;
	}
	
	@Override
	public String toString() {
		return "[" + child + " child of " + parentTerm + " in " + hierarchy + "]";
	}
	
}