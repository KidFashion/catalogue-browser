package import_catalogue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.xml.sax.SAXException;

import catalogue.Catalogue;
import progress_bar.IProgressBar;
import utilities.GlobalUtil;
import xml_to_excel.XmlCatalogueToExcel;
import zip_manager.ZipManager;

public class CatalogueImporter {
	
	private static final Logger LOGGER = LogManager.getLogger(CatalogueImporter.class);
	
	private String filename;  // path of the file
	private ImportFileFormat format;  // the format of the file
	private Catalogue openedCat;
	private IProgressBar progressBar;
	private double maxProgress;
	private double preprocProgress;
	
	// list of temporary files which need to
	// be deleted at the end of the process
	private ArrayList<String> garbage;
	
	/**
	 * Enumerator to specify the format
	 * of the file we want to import
	 * into the catalogue database
	 * @author avonva
	 *
	 */
	public enum ImportFileFormat {
		ECF,
		XML,
		XLSX;
	}
	/**
	 * Initialize the import thread
	 * @param filename path of the file we want to import
	 * @param format in which format is the file that we want to import
	 */
	public CatalogueImporter( String filename, 
			ImportFileFormat format, IProgressBar progressBar, double maxProgress ) {

		this.filename = filename;
		this.format = format;
		this.garbage = new ArrayList<>();
		this.progressBar = progressBar;
		this.maxProgress = maxProgress;
	}
	
	/**
	 * Import the file
	 * @throws TransformerException 
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws ImportException 
	 */
	public void makeImport() throws TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		// 5% of progress bar for preprocessing
		this.preprocProgress = maxProgress * 5 / 100;
		if (progressBar != null)
			progressBar.addProgress( preprocProgress );
		
		switch ( format ) {
		case ECF:
			importEcf( filename );
			break;

		case XML:
			importXml( filename );
			break;
			
		case XLSX:
			importXlsx( filename );
			break;

		default:
			break;
		}
	}
	
	/**
	 * Process an ecf file and extract the xml catalogue
	 * contained in it.
	 * @param filename
	 * @return the created xml file
	 */
	private String processEcf ( String filename ) {
		
		String outputFile = null;
		
		try {

			File inputFile = new File ( filename );
			
			String outputFolder = GlobalUtil.getTempDir() + inputFile.getName() + "_unzip";

			// unzip the ecf file into the xml
			ZipManager.extractFolder( filename, outputFolder );

			final File unzippedFolder = new File( outputFolder );

			if ( unzippedFolder.listFiles().length <= 0 ) {
				LOGGER.error( "Wrong file format, "
						+ "cannot find the xml file inside the .ecf" );
				return null;
			}

			// add the unzipped folder to the garbage to
			// delete it at the end of the process
			garbage.add( unzippedFolder.getAbsolutePath() );
			
			// get the xml file from the folder
			File xmlFile = unzippedFolder.listFiles()[0];

			outputFile = xmlFile.getAbsolutePath();
			
			return outputFile;

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Process an .xml file to create a .xlsx catalogue file
	 * @param filename xml filename
	 * @return the created xlsx file
	 * @throws TransformerException 
	 */
	private String processXml ( String filename ) throws TransformerException {
		
		String outputFilename = filename + ".xlsx";

		// convert the xml into an excel
		XmlCatalogueToExcel converter = 
				new XmlCatalogueToExcel( filename, outputFilename );
		
		// do the conversion
		converter.convertXmlToExcel();

		return outputFilename;
	}
	
	
	/**
	 * Import an .ecf catalogue
	 * @param filename the absolute path of the .ecf file
	 * @throws TransformerException 
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws ImportException 
	 */
	private void importEcf( String filename ) throws TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {
		
		String xmlFile = processEcf( filename );
		
		// at the end of the process delete the
		// .xml temporary file
		garbage.add( xmlFile );
		
		// import the .xml file
		importXml( xmlFile );
	}
	
	/**
	 * Import a .xml catalogue
	 * @param filename the absolute path of the .xml catalogue
	 * @throws TransformerException 
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws ImportException 
	 */
	private void importXml( String filename ) throws TransformerException, 
		IOException, XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		String xlsxFile = processXml( filename );
		
		// at the end of the process delete the
		// temporary xlsx file
		garbage.add( xlsxFile );
		
		// import the xlsx catalogue
		importXlsx( xlsxFile );
	}
	
	/**
	 * Import a .xlsx catalogue
	 * @param filename the absolute path of the .xlsx catalogue
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws OpenXML4JException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws ImportException 
	 */
	private void importXlsx( final String filename ) throws IOException, 
		XMLStreamException, OpenXML4JException, SAXException, SQLException, ImportException {

		// instantiate the workbook importer and set
		// some settings
		CatalogueWorkbookImporter importer = new CatalogueWorkbookImporter();

		if ( openedCat != null )
			importer.setOpenedCatalogue( openedCat );

		// import the catalogue contained in the
		// xlsx file into the specified path (db path)
		importer.importWorkbook( progressBar, filename, maxProgress - preprocProgress );

		// end the import process
		endProcess();
	}
	
	/**
	 * End the import process
	 */
	private void endProcess() {

		// delete all the temporary files
		deleteGarbage();
	}
	

	/**
	 * Delete all the file in the garbage
	 * @throws IOException
	 */
	private void deleteGarbage() {
		for ( String filename : garbage ) {
			try {
				GlobalUtil.deleteFileCascade( new File( filename ) );
			} catch (IOException e) {}
		}
	}
	
	public void setOpenedCat(Catalogue openedCat) {
		this.openedCat = openedCat;
	}
}
