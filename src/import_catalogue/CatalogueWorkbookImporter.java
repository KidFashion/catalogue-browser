package import_catalogue;

import java.io.IOException;
import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import catalogue_browser_dao.ReleaseNotesDAO;
import messages.Messages;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;
import ui_progress_bar.FormProgressBar;
import ui_search_bar.SearchOptionDAO;
import user_preferences.CataloguePreferenceDAO;

/**
 * Import an entire catalogue workbook (xslx) into the database
 * @author avonva
 *
 */
public class CatalogueWorkbookImporter {
	
	// set this to import a local catalogue
	private Catalogue openedCat;
	
	private FormProgressBar progressBar;
	
	/**
	 * Set this to true if the catalogue you
	 * are importing an .xlsx in a catalogue
	 * (the one opened in the main panel)
	 * in order to override its data
	 * @param openedCat
	 */
	public void setOpenedCatalogue( Catalogue openedCat ) {
		this.openedCat = openedCat;
	}
	
	
	/**
	 * Set a progress bar for the import process.
	 * @param progressBar, the progress bar which is displayed in the main UI
	 */
	public void setProgressBar ( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
	}
	
	/**
	 * Update the progress bar progress and label
	 * @param progress
	 * @param label
	 */
	private void updateProgressBar ( int progress, String label ) {
		
		if ( progressBar == null )
			return;
		
		progressBar.addProgress( progress );
		progressBar.setLabel( label );
	}
	
	/**
	 * Import the workbook
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws SQLException 
	 * @throws Exception
	 */
	public void importWorkbook( String filename ) 
			throws IOException, XMLStreamException, OpenXML4JException, 
			SAXException, SQLException {

		long importTimeStart = System.currentTimeMillis();
		
		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ReadingData") );
		
		// get the excel data
		WorkbookReader workbookReader = new WorkbookReader( filename );
		
		CatalogueSheetImporter catImp = importCatalogueSheet( workbookReader );
		
		// get the imported catalogue (if it was local it is the same)
		Catalogue catalogue = catImp.getImportedCatalogue();
		String catExcelCode = catImp.getExcelCode();
		
		// import hierarchies
		importHierarchySheet ( workbookReader, catalogue, catExcelCode );
		
		// import attributes
		importAttributeSheet ( workbookReader, catalogue );

		long termTimeStart = System.currentTimeMillis();
		
		// import terms
		importTermSheet ( workbookReader, catalogue );

		long termTimeEnd = System.currentTimeMillis();
		
		// import the release note sheet
		importReleaseNotes ( workbookReader, catalogue );

		// close the connection with excel reader
		workbookReader.close();

		// after having imported the excel, we can insert the default preferences
		System.out.println ( "Creating default preferences" );
		
		// insert default preferences
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
		prefDao.insertDefaultPreferences();
		
		// insert the default search options
		SearchOptionDAO optDao = new SearchOptionDAO ( catalogue );
		optDao.insertDefaultSearchOpt();
		
		long importTimeEnd = System.currentTimeMillis();
		
		// end process
		if ( progressBar != null )
			progressBar.close();
		
		System.out.println( catalogue + " successfully imported in " + catalogue.getDbPath() );
		
		System.out.println( "Statistics: " + 
				"overall time = " + (importTimeEnd-importTimeStart)/1000.00 + " seconds" +
				"; term sheet time = " + (termTimeEnd-termTimeStart)/1000.00 + " seconds" );
	}
	
	/**
	 * Import the catalogue sheet
	 * @param workbookReader
	 * @param dbPath
	 * @return
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private CatalogueSheetImporter importCatalogueSheet ( WorkbookReader workbookReader ) 
			throws InvalidFormatException, IOException, XMLStreamException {
		
		System.out.println( "Importing catalogue sheet" );
		updateProgressBar( 1, Messages.getString("ImportExcelXLSX.ImportCatalogue") );
		
		// get the catalogue sheet and check if the catalogues are compatible
		// (the open catalogue and the one we want to import)
		workbookReader.processSheetName( Headers.CAT_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		CatalogueSheetImporter catImp = new CatalogueSheetImporter();
		
		if ( openedCat != null )
			catImp.setOpenedCatalogue ( openedCat );
		
		catImp.importData( sheetData );
		
		return catImp;
	}
	
	/**
	 * Import the attribute sheet
	 * @param workbookReader
	 * @param catalogue
	 * @throws XMLStreamException
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	private void importAttributeSheet( WorkbookReader workbookReader, Catalogue catalogue ) 
			throws XMLStreamException, InvalidFormatException, IOException {
		
		System.out.println( "Importing attribute sheet" );
		updateProgressBar( 2, Messages.getString("ImportExcelXLSX.ImportAttributeLabel") );
		
		// get the attribute sheet
		workbookReader.processSheetName( Headers.ATTR_SHEET_NAME );

		ResultDataSet sheetData = workbookReader.next();
		
		AttributeSheetImporter attrImp = new 
				AttributeSheetImporter( catalogue );
		// start the import
		attrImp.importData( sheetData );
		
		
		// import the term types related to the attributes
		TermTypeImporter ttImp = new TermTypeImporter( catalogue );
		ttImp.importSheet();
	}
	
	/**
	 * Import the hierarchy sheet. Note that you need to set
	 * the master hierarchy code, since if we are overriding
	 * an already existing catalogue which has its own master
	 * code, we cannot use it, we need the master hierarchy
	 * code contained in the excel sheet to get the master
	 * hierarchy data correctly.
	 * @param workbookReader
	 * @param catalogue
	 * @param catExcelCode the master hierarchy code
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private void importHierarchySheet ( WorkbookReader workbookReader, 
			Catalogue catalogue, String catExcelCode ) 
					throws InvalidFormatException, IOException, XMLStreamException {
		
		System.out.println( "Importing hierarchy sheet" );
		updateProgressBar( 2, Messages.getString("ImportExcelXLSX.ImportHierarchyLabel") );
		
		// get the hierarchy sheet
		workbookReader.processSheetName( Headers.HIER_SHEET_NAME );
		
		ResultDataSet sheetData = workbookReader.next();
		
		HierarchySheetImporter hierImp = new 
				HierarchySheetImporter( catalogue, catExcelCode );
		
		// start the import
		hierImp.importData( sheetData );
	}
	
	/**
	 * Import a sheet in a smarter way. In particular, two parallel
	 * threads are started. The first thread reads the data from the
	 * workbookReader object. The second thread writes the read data
	 * into the database. Note that the first thread reads the data
	 * also while the second one is writing the data of the previous
	 * batch. This results in improved performances, since delay times
	 * are reduced. Note that this method requires that you set a
	 * batch size for the workbookReader (see {@link WorkbookReader#setBatchSize(int)}, 
	 * otherwise the data would not be separable, since they are
	 * all contained in a single big batch.
	 * @param workbookReader the reader with a sheet already loaded
	 * @param importer the sheet importer
	 * @throws XMLStreamException
	 * @throws CloneNotSupportedException
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	private void importQuickly ( WorkbookReader workbookReader, String sheetName,
			int batchSize, final SheetImporter<?> importer ) throws XMLStreamException, 
			InvalidFormatException, IOException {
		
		QuickImporter quickImp = new QuickImporter( workbookReader, sheetName, batchSize ) {
			
			@Override
			public void importData(ResultDataSet rs) {
				importer.importData( rs );
			}
		};
		
		quickImp.importSheet();
	}

	/**
	 * Import the entire term sheet into the db. Note that you have
	 * to import the hierarchies and the attributes before importing
	 * this sheet. See {@link #importAttributeSheet(WorkbookReader, Catalogue)}
	 * and {@link #importHierarchySheet(WorkbookReader, Catalogue, String)}.
	 * @param workbookReader
	 * @param catalogue
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws SQLException 
	 */
	private void importTermSheet ( WorkbookReader workbookReader, Catalogue catalogue ) 
			throws InvalidFormatException, IOException, XMLStreamException, SQLException {

		final int batchSize = 100;
		
		System.out.println( "Importing term sheet" );
		updateProgressBar( 15, Messages.getString("ImportExcelXLSX.ImportTermLabel") );

		TermSheetImporter termImp = new TermSheetImporter( catalogue );

		// import terms in a quick way
		importQuickly ( workbookReader, Headers.TERM_SHEET_NAME, batchSize, termImp );

		// note that we need to have imported the terms to import
		// term attributes and parent terms!
		
		System.out.println( "Importing term attributes and parent terms" );
		updateProgressBar( 50, Messages.getString("ImportExcelXLSX.ImportTermAttrLabel") );
		
		// import term attributes and parent terms in a parallel way
		// since they are independent processes
		QuickParentAttributesImporter tapImporter = new QuickParentAttributesImporter( catalogue, 
				workbookReader, Headers.TERM_SHEET_NAME, batchSize );
		
		tapImporter.manageNewTerms( termImp.getNewCodes() );
		
		tapImporter.importSheet();
	}
	
	/**
	 * Import the release notes
	 * @param workbookReader
	 * @param catalogue
	 */
	private void importReleaseNotes ( WorkbookReader workbookReader, Catalogue catalogue ) {
		
		System.out.println( "Importing release notes sheet" );
		updateProgressBar( 5, Messages.getString("ImportExcelXLSX.ImportReleaseNotes") );
		
		// add the catalogue information
		ReleaseNotesDAO notesDao = new ReleaseNotesDAO( catalogue );
		if ( catalogue.getReleaseNotes() != null )
			notesDao.insert( catalogue.getReleaseNotes() );
		
		// import the release notes operations
		try {

			workbookReader.processSheetName( Headers.NOTES_SHEET_NAME );
			
			ResultDataSet sheetData = workbookReader.next();
			
			NotesSheetImporter notesImp = new 
					NotesSheetImporter( catalogue );
			notesImp.importData( sheetData );
			
		} catch ( Exception e ) {
			System.err.println( "Release notes not found for " + catalogue );
		}
	}
}