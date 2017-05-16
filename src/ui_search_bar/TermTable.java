package ui_search_bar;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import catalogue_object.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import term.LabelProviderTerm;
import ui_main_panel.HierarchySelector;

/**
 * Table which is used to show a list of terms
 * @author avonva
 *
 */
public class TermTable implements Observer {

	private Composite parent;
	private TableViewer table;
	private LabelProviderTerm labelProvider;

	public TermTable( Composite parent, Catalogue catalogue ) {

		this.parent = parent;
		
		// create the table viewer for hosting the search results
		table = new TableViewer( parent , SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.FULL_SELECTION );

		// set the content and the label provider of the table
		table.setContentProvider( new ContentProviderTermList() );

		labelProvider = new LabelProviderTerm();
		table.setLabelProvider( labelProvider );
		
		if ( catalogue != null )
			labelProvider.setCurrentHierarchy( catalogue.getMasterHierarchy() );

		// layout data of the table
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		table.getControl().setLayoutData( gridData );
	}
	
	/**
	 * Add double click listener to the table
	 * @param listener
	 */
	public void addDoubleClickListener ( IDoubleClickListener listener ) {
		table.addDoubleClickListener( listener );
	}
	
	/**
	 * Add focus listener to the table
	 * @param listener
	 */
	public void addFocusListener ( FocusListener listener ) {
		table.getTable().addFocusListener( listener );
	}
	
	/**
	 * Add selection listener to the table
	 * @param listener
	 */
	public void addSelectionChangedListener ( ISelectionChangedListener listener ) {
		table.addSelectionChangedListener( listener );
	}
	
	/**
	 * Check if the selection of the table is
	 * empty
	 * @return
	 */
	public boolean isSelectionEmpty() {
		return table.getSelection().isEmpty();
	}
	
	/**
	 * Does the table contain the term?
	 * @param term
	 * @return
	 */
	public boolean contains ( Term term ) {
		
		// if the term is already present return true
		for ( int i = 0; i < getItemCount(); i++ ) {
		
			if ( ( (Term) table.getElementAt( i ) ).equals( term ) )
				return true;
		}
		
		// otherwise return false
		return false;
	}
	
	/**
	 * Get the first selected term in the table
	 * @return
	 */
	public Term getFirstSelectedTerm() {
		
		if ( isSelectionEmpty() )
			return null;
		
		IStructuredSelection selection = (IStructuredSelection) table.getSelection();
		
		// get the selected term (there is only one)
		return (Term) selection.getFirstElement();
	}
	
	/**
	 * Set the hierarchy we are working with
	 * we need this to set the font and reportable flag
	 * for terms (properties which relies on hierarchies)
	 * @param hierarchy
	 */
	public void setCurrentHierarchy ( Hierarchy hierarchy ) {
		labelProvider.setCurrentHierarchy( hierarchy );
	}
	
	/**
	 * Enable/disable table
	 * @param enabled
	 */
	public void setEnabled ( boolean enabled ) {
		table.getTable().setEnabled( enabled );
	}
	
	/**
	 * Get the number of terms
	 */
	public int getItemCount() {
		return table.getTable().getItemCount();
	}
	
	/**
	 * Set the table input
	 * @param input
	 */
	public void setInput ( ArrayList<Term> input ) {
		table.setInput ( input );
	}
	
	/**
	 * Add a term to the table
	 * @param term
	 */
	public void addTerm ( Term term ) {
		table.add( term );
	}
	
	/**
	 * Remove a term from the table
	 * @param term
	 */
	public void removeTerm ( Term term ) {
		table.remove( term );
	}
	
	/**
	 * Add contextual menu
	 * @param menu
	 */
	public void addMenu ( Menu menu ) {
		table.getTable().setMenu( menu );
	}
	
	/**
	 * Remove contextual menu
	 * @param menu
	 */
	public void removeMenu () {
		table.getTable().setMenu( null );
	}
	
	/**
	 * Remove all the terms
	 */
	public void removeAll() {
		table.getTable().removeAll();
	}
	
	/**
	 * Refresh the table
	 * @param label
	 */
	public void refresh ( boolean label ) {
		table.refresh( label );
	}
	
	/**
	 * Get the parent of the widget
	 * @return
	 */
	public Composite getParent() {
		return parent;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		// update hierarchy
		if ( arg0 instanceof HierarchySelector ) {
			labelProvider.setCurrentHierarchy( ((HierarchySelector) arg0).getSelectedHierarchy() );
		}
	}
}