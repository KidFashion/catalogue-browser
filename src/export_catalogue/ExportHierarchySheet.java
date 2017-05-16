package export_catalogue;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Catalogue;
import catalogue_object.Mappable;
import global_manager.GlobalManager;
import sheet_converter.SheetHeader;

public class ExportHierarchySheet extends SheetWriter {

	public ExportHierarchySheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {
		
		HashMap<String, SheetHeader> headers = new HashMap<>();

		headers.put( "HIERARCHY_CODE",          new SheetHeader(0, "code" ) );
		headers.put( "HIERARCHY_NAME",          new SheetHeader(1, "name" ) );
		headers.put( "HIERARCHY_LABEL",         new SheetHeader(2, "label" ) );
		headers.put( "HIERARCHY_SCOPENOTE",     new SheetHeader(3, "scopeNote" ) );
		headers.put( "HIERARCHY_APPLICABILITY", new SheetHeader(4, "hierarchyApplicability" ) );
		headers.put( "HIERARCHY_ORDER",         new SheetHeader(5, "hierarchyOrder" ) );
		headers.put( "HIERARCHY_VERSION",       new SheetHeader(6, "version" ) );
		headers.put( "HIERARCHY_LAST_UPDATE",   new SheetHeader(7, "lastUpdate" ) );
		headers.put( "HIERARCHY_VALID_FROM",    new SheetHeader(8, "validFrom" ) );
		headers.put( "HIERARCHY_VALID_TO",      new SheetHeader(9, "validTo" ) );
		headers.put( "HIERARCHY_STATUS",        new SheetHeader(10, "status" ) );
		headers.put( "HIERARCHY_DEPRECATED",    new SheetHeader(11, "deprecated" ) );
		headers.put( "HIERARCHY_GROUPS",        new SheetHeader(12, "hierarchyGroups" ) );

		return headers;
	}

	@Override
	public ArrayList< ? extends Mappable > getData() {
		
		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();
		
		// get the current catalogue
		Catalogue currentCat = manager.getCurrentCatalogue();
		
		HierarchyDAO hierDao = new HierarchyDAO( currentCat );
		
		// get all the hierarchies as data
		return hierDao.getAll();
	}

	
}