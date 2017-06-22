package import_catalogue;

import java.util.Collection;

import catalogue.Catalogue;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import catalogue_object.HierarchyBuilder;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;

/**
 * Import the hierarchy sheet into the catalogue database.
 * @author avonva
 *
 */
public class HierarchySheetImporter extends SheetImporter<Hierarchy> {

	private Catalogue catalogue;
	private String masterCode;
	
	/**
	 * Initialize the import of the hierarchy sheet
	 * @param catalogue the catalogue in which we want to import the hierarchies
	 * @param masterCode the master hierarchy code. Note that this is different
	 * from the {@code catalogue} code. In fact, if we import
	 * overriding an existing catalogue, the catalogue code could be potentially 
	 * different from the one reported into the
	 * excel catalogue sheet, since we can import a different catalogue compared to the
	 * one we are overriding. Therefore, we cannot simply use {@link Catalogue#getCode()}
	 * to access the master hierarchy code, but we need the information retrieved
	 * directly from the excel sheet (which can be retrieved using
	 * {@link CatalogueSheetImporter#getExcelCode()} after having imported
	 * the catalogue sheet). We need this information since the excel workbook
	 * uses its own master hierarchy code.
	 */
	public HierarchySheetImporter( Catalogue catalogue, String masterCode ) {
		this.catalogue = catalogue;
		this.masterCode = masterCode;
	}
	
	@Override
	public Hierarchy getByResultSet(ResultDataSet rs) {
		
		// get the hierarchy code
		String code = rs.getString ( Headers.CODE );

		// if empty ignore
		if ( code.isEmpty() ) {
			System.err.println( "Empty hierarchy code found, skipping..." );
			return null;
		}

		boolean isMaster = code.equals( masterCode );

		HierarchyBuilder builder = new HierarchyBuilder();

		builder.setCatalogue( catalogue );
		
		// for local catalogues the master
		// should have the same name of the
		// local catalogue
		if ( catalogue.isLocal() && isMaster ) {
			builder.setCode( catalogue.getCode() );
			builder.setName( catalogue.getName() );
			builder.setLabel( catalogue.getLabel() );
		}
		else {
			builder.setCode( code );
			builder.setName( rs.getString ( Headers.NAME ) );
			builder.setLabel( rs.getString ( Headers.LABEL ) );
		}

		builder.setScopenotes( rs.getString ( Headers.SCOPENOTE ) );
		builder.setApplicability( rs.getString ( Headers.HIER_APPL ) );

		builder.setOrder( rs.getInt ( Headers.HIER_ORDER, 1 ) );
		builder.setStatus( rs.getString ( Headers.STATUS ) );

		// set the is_master field as true if the hierarchy code
		// is the same as the catalogue code (convention)
		// otherwise false
		builder.setMaster( isMaster );

		builder.setLastUpdate( rs.getTimestamp( Headers.LAST_UPDATE ) );
		builder.setValidFrom( rs.getTimestamp( Headers.VALID_FROM ) );
		builder.setValidTo( rs.getTimestamp( Headers.VALID_TO ) );
		builder.setVersion( rs.getString ( Headers.VERSION ) );

		builder.setDeprecated( rs.getBoolean( Headers.DEPRECATED, false ) );
		builder.setGroups( rs.getString ( Headers.HIER_GROUPS ) );

		return builder.build();
	}

	@Override
	public void insert( Collection<Hierarchy> hierarchies ) {
		
		HierarchyDAO hierDao = new HierarchyDAO( catalogue );

		// insert all the hierarchies into the database
		hierDao.insertHierarchies( hierarchies );
	}

	@Override
	public Collection<Hierarchy> getAllByResultSet(ResultDataSet rs) {
		return null;
	}

	@Override
	public void end() {}
}