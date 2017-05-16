package ui_search_bar;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import catalogue_object.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Term;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_panel.HierarchySelector;
import user_preferences.CataloguePreference;
import user_preferences.CataloguePreferenceDAO;

/**
 * This class implements a search bar which allows to perform a search in the database
 * In particular, you can write the keywords you want to search and set some settings:
 * - search as exact match, any word or all words
 * - search in the selected hierarchy or globally
 * 
 * When the search button is pressed, the search is performed. When the results are ready to be
 * used by the program, a listener is called to update the main thread that the search is finished
 * and it can use the results.
 * @author avonva
 *
 */
public class SearchBar implements Observer {

	private Catalogue catalogue;
	
	private Text textSearch;
	private Combo comboOptSearch;
	private Button buttonSearch;
	private Button localSearch;
	private Button globalSearch;
	
	private SearchListener listener;
	
	// we can search terms only in these hierarchies (only if globalSearch is disabled)
	private ArrayList<Hierarchy> searchableHierarchies = new ArrayList<>();
	
	// Array list which contains the results of the search (terms)
	private ArrayList < Term > searchResults = new ArrayList<>();
	
	// composite which contains the search bar
	Composite parent;
	boolean addGlobalSearch;  // should global search button be added?
	boolean globalSearchEnabled = false;
	
	// TODO use these settings to show also search results
	private boolean hideDeprecated;
	private boolean hideNotInUse;
	
	public SearchBar( Composite parent, boolean addGlobalSearch ) {
		this.parent = parent;
		this.addGlobalSearch = addGlobalSearch;
	}
	
	/**
	 * Set the catalogue on which we want to search
	 * @param catalogue
	 */
	public void setCatalogue(Catalogue catalogue) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Set the entire search panel enabled or disabled
	 * Note that if a catalogue was not set, the search will
	 * give errors.
	 * @param enabled
	 */
	public void setEnabled ( boolean enabled ) {

		comboOptSearch.setEnabled( enabled );
		buttonSearch.setEnabled( enabled );
		textSearch.setEnabled( enabled );
		
		if ( catalogue != null )
			textSearch.setText("");
		
		if ( addGlobalSearch ) {
			
			// reset the default settings
			localSearch.setSelection( true );
			globalSearch.setSelection( false );
			
			localSearch.setEnabled( enabled );
			globalSearch.setEnabled( enabled );
		}
	}
	
	
	/**
	 * Restrict the search space to a single hierarchy
	 */
	public void setSearchHierarchy ( Hierarchy hierarchy ) {
		searchableHierarchies = new ArrayList<>();
		searchableHierarchies.add( hierarchy );
	}
	
	/**
	 * Restrict the search space to a multiple hierarchies
	 * @param 
	 */
	public void setSearchHierarchies ( ArrayList<Hierarchy> searchableHierarchies ) {
		this.searchableHierarchies = searchableHierarchies;
	}
	
	/**
	 * Should deprecated terms be hidden?
	 * @param hideDeprecated
	 */
	public void setHideDeprecated(boolean hideDeprecated) {
		this.hideDeprecated = hideDeprecated;
	}
	
	/**
	 * Should not in use terms be hidden?
	 * @param hideNotInUse
	 */
	public void setHideNotInUse(boolean hideNotInUse) {
		this.hideNotInUse = hideNotInUse;
	}
	
	/**
	 * Get the written keyword
	 * @return
	 */
	public String getKeyword() {
		
		if ( textSearch == null )
			return "";
		
		return textSearch.getText();
	}
	
	
	/**
	 * Get the search option which was selected before
	 * @return
	 */
	public SearchType getSearchMode() {
		
		if ( comboOptSearch == null )
			return SearchType.EXACT_MATCH;
		
		// default
		SearchType type = SearchType.EXACT_MATCH;
		
		switch( comboOptSearch.getSelectionIndex() ) {
		case 0:
			type = SearchType.EXACT_MATCH;
			break;
		case 1:
			type = SearchType.ANY_WORD;
			break;
		case 2:
			type = SearchType.ALL_WORDS;
			break;
		default:
			break;
		};
		
		return type;
	}
	
	/**
	 * Get the search results ( it is filled only when the search button is pressed )
	 * @return
	 */
	public ArrayList <Term> getSearchResults () {
		
		return searchResults;
	}
	
	/**
	 * Get the search button
	 * @return
	 */
	public Button getButton () {
		return buttonSearch;
	}
	
	/**
	 * Get the search text box
	 * @return
	 */
	public Text getText () {
		return textSearch;
	}
	/**
	 * Set the listener for the search
	 * @param listener
	 */
	public void setListener ( SearchListener listener ) {
		this.listener = listener;
	}
	
	/**
	 * Update the search globally feature (used to restore previous state)
	 * @param searchGlobally
	 */
	public void setSearchGlobally ( boolean searchGlobally ) {
		
		// if global search is not allowed return
		if ( !addGlobalSearch )
			return;

		// otherwise update the selection of the radio buttons
		globalSearch.setSelection( searchGlobally );
		localSearch.setSelection( !searchGlobally );
	}
	
	/**
	 * Get the minimum number of characters needed to perform a search
	 * @return
	 */
	private int getMinSearchChar() {
		
		// if we have a catalogue open check which is the min search char preference
		// otherwise always allows
		int minSearchChar = -1;

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );

		minSearchChar = prefDao.
				getPreferenceIntValue( CataloguePreference.minSearchChar, 3 );

		return minSearchChar;

	}
	
	/**
	 * Check if we can perform a search or not based on the number of character we inputed in the text box
	 * @return
	 */
	private boolean canSearch( int inputedCharacters ) {
		return inputedCharacters >= getMinSearchChar();
	}
	
	
	
	/**
	 * Display the search bar, instantiate the UI
	 */
	public void display( ) {
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		
		// Setting the "search" widget 
		Group searchGroup = new Group( parent , SWT.NONE );
		
		// if no global buttons place all the element on one row
		int numberOfColumns = addGlobalSearch ? 3 : 4;
		
		searchGroup.setLayout( new GridLayout( numberOfColumns , false ) );
		searchGroup.setLayoutData( gridData );

		// Search label
		Label labelSearch = new Label( searchGroup , SWT.NONE );
		labelSearch.setText( Messages.getString("SearchBar.SearchLabel") );

		// Search text box (where you write keywords)
		textSearch = addSearchTextBox ( searchGroup );
		
		// listener for text changes (enable/disable button search)
		textSearch.addModifyListener( new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				
				buttonSearch.setEnabled( canSearch( textSearch.getText().trim().length() ) && textSearch.isEnabled() );
			}
		});

		// search options, all words, any word...
		comboOptSearch = addSearchOptions ( searchGroup );
		
		// add global search button if required
		if ( addGlobalSearch ) {
			
			Composite g = new Composite( searchGroup , SWT.NONE );
			gridData = new GridData();
			gridData.verticalAlignment = SWT.FILL;
			gridData.horizontalAlignment = SWT.FILL;
			gridData.horizontalSpan = 3;

			g.setLayoutData( gridData );
			RowLayout r = new RowLayout();
			r.justify = true;
			r.center = true;
			g.setLayout( r );
			
			// add global search radio button
			addGlobalSearch( g );
			
			buttonSearch = new Button( g , SWT.PUSH );// searchGroup
		}
		else
			buttonSearch = new Button( searchGroup , SWT.PUSH );// searchGroup
		

		buttonSearch.setAlignment( SWT.CENTER );
		buttonSearch.setText( Messages.getString("SearchBar.GoButton") );
		buttonSearch.setEnabled( false );  // until a keyword is added, disabled
		buttonSearch.pack();
		
		buttonSearch.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				// change the cursor to wait
				Cursor cursor = new Cursor( parent.getDisplay() , SWT.CURSOR_WAIT );
				parent.getShell().setCursor( cursor );
				
				// call the external listener to say that the search results are ready to be used
				searchResults = search( getKeyword(), getSearchMode() );
				
				// dispose the old cursor and instantiate the new one
				if ( cursor != null )
					cursor.dispose();

				// reload the old cursor, the search is finished
				cursor = new Cursor( parent.getDisplay() , SWT.CURSOR_ARROW );
				parent.getShell().setCursor( cursor );
				
				
				if ( listener != null ) {
					
					SearchEvent event = new SearchEvent();
					event.setResults( searchResults );

					// call the search listener
					listener.searchPerformed( event );
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
	
	
	/**
	 * Add global search button if required
	 * @param parent
	 */
	private void addGlobalSearch ( Composite parent ) {
		
		localSearch = new Button( parent , SWT.RADIO );
		localSearch.setText( Messages.getString( "SearchBar.SearchCurrentButton" ) );

		globalSearch = new Button( parent , SWT.RADIO );
		globalSearch.setText( Messages.getString( "SearchBar.SearchDictionaryButton" ) );
		
		/* setting local/global search */
		localSearch.setSelection( true );
		globalSearch.setSelection( false );

		globalSearch.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				globalSearch.setSelection( true );
				localSearch.setSelection( false );
				globalSearchEnabled = true;
			}
		} );

		localSearch.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				localSearch.setSelection( true );
				globalSearch.setSelection( false );
				globalSearchEnabled = false;
			}
		} );
	}
	
	
	
	/**
	 * Add a text box for search
	 * @param parent
	 * @return
	 */
	private Text addSearchTextBox ( Composite parent ) {
		
		Text textSearch = new Text( parent , SWT.BORDER );
		textSearch.setMessage( Messages.getString( "SearchBar.SearchTipText" ) );
		
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.CENTER;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		textSearch.setLayoutData( gridData );
		textSearch.setSize( 100, 50 );
		
		return textSearch;
	}
	
	
	
	/**
	 * Add a combo box for searching with options
	 * @param parent
	 * @return
	 */
	private Combo addSearchOptions ( Composite parent ) {
		
		Combo comboOptSearch = new Combo( parent , SWT.READ_ONLY );
		String items[] = { Messages.getString("SearchBar.SearchOption1"), Messages.getString("SearchBar.SearchOption2"), 
				Messages.getString("SearchBar.SearchOption3") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		comboOptSearch.setItems( items );
		comboOptSearch.select( 0 );
		comboOptSearch.pack();
		comboOptSearch.setToolTipText( Messages.getString("SearchBar.SearchTip1") //$NON-NLS-1$
				+ Messages.getString("SearchBar.SearchTip2") //$NON-NLS-1$
				+ Messages.getString("SearchBar.SearchTip3") //$NON-NLS-1$
				+ Messages.getString("SearchBar.SearchTip4") ); //$NON-NLS-1$
		
		return comboOptSearch;
	}
	
	
	/**
	 * The search method check the query string and search the terms who match
	 * the query string. Method added for reducing and avoiding duplication code
	 * in the system.
	 * Return an array list of terms (search results), which is empty if no results are found
	 */
	private ArrayList< Term > search ( String keyword, SearchType type ) {
		
		// output array
		ArrayList< Term > searchResults = new ArrayList<>();
		
		// if the number of characters of the search are less than the minimum number of characters
		if ( !canSearch( keyword.trim().length() ) )
			return searchResults;

		SearchDAO searchDao = new SearchDAO( catalogue );
		
		// search terms
		if ( !globalSearchEnabled )
			searchResults = searchDao.startSearch( keyword, type, searchableHierarchies );
		else 
			searchResults = searchDao.startSearch( keyword, type );

		// return the results
		return searchResults;
	}

	
	/**
	 * What to do if the selected hierarchy is changed? Save the selected hierarchy
	 */
	@Override
	public void update ( Observable o, Object data ) {
		
		if ( o instanceof HierarchySelector ) {
			setSearchHierarchy ( (Hierarchy) data );
		}
		
		// update catalogue if it was changed
		// with the open catalogue menu item
		/*if ( o instanceof CatalogueHandler ) {
			catalogue = ((CatalogueHandler) o).getCatalogue();
		}*/
		
		if ( o instanceof GlobalManager && data instanceof Catalogue ) {
			
			this.catalogue = (Catalogue) data;
		}
	}
}