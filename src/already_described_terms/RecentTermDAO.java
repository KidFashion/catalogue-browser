package already_described_terms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_object.Catalogue;
import user_preferences.CataloguePreference;
import user_preferences.CataloguePreferenceDAO;

/**
 * This class is used to manage all the recently described terms
 * @author avonva
 *
 */
public class RecentTermDAO implements CatalogueEntityDAO<DescribedTerm> {

	private Catalogue catalogue;
	
	/**
	 * Initialize the recent term dao with the
	 * catalogue we want to communicate with.
	 * @param catalogue
	 */
	public RecentTermDAO( Catalogue catalogue ) {
		this.catalogue = catalogue;
	}
	
	/**
	 * Retrieve all the recently described terms from the catalogue db
	 * We get them in descending order. In this way we can have the more recent in the first positions.
	 * @return
	 */
	public ArrayList<DescribedTerm> getAll () {
		
		ArrayList<DescribedTerm> recentTerms = new ArrayList<>();
		
		Connection con;
		
		String query = "select * from APP.RECENT_TERM order by RECENT_TERM_ID DESC";
		
		try {
			
			con = catalogue.getConnection();
			
			PreparedStatement stmt = con.prepareStatement( query );
			
			ResultSet rs = stmt.executeQuery();
			
			while ( rs.next() ) {
				
				// add a new recent term
				recentTerms.add( getByResultSet( rs ) );
			}
			
			rs.close();
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return recentTerms;
	}
	
	
	/**
	 * Insert a recently described term into the db
	 * @param recentTerm the new recent term we want to add
	 * @return
	 */
	public int insert ( DescribedTerm recentTerm ) {
		
		int id = -1;
		
		Connection con;
		
		String query = "insert into APP.RECENT_TERM (RECENT_TERM_CODE, RECENT_TERM_LABEL) values (?, ?)";
		
		try {
			
			// connect to the db
			con = catalogue.getConnection();
			
			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query, Statement.RETURN_GENERATED_KEYS );
			
			stmt.clearParameters();
			
			// set parameters
			stmt.setString( 1, recentTerm.getCode() );
			stmt.setString( 2, recentTerm.getLabel() );
			
			// execute query
			stmt.executeUpdate();
			
			// update the terms ids with the ones given by the database
			ResultSet rs = stmt.getGeneratedKeys();
			
			if ( rs.next() )
				id = rs.getInt(1);
			
			rs.close();
			stmt.close();
			con.close();

			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	/**
	 * Remove the old recent terms, in order to free space. The number of recent terms which are maintained in
	 * the database is a user preference
	 */
	public void removeOldTerms() {
		
		Connection con;

		// complex query, explanation step by step:
		//
		// 1� - select RECENT_TERM_ID from RECENT_TERM order by RECENT_TERM_ID desc fetch first ? rows only
		// the above query retrieves the most recent terms of the database (the number will be specified by the user preference)
		// let's call the results of this query RECENT_RES to make clearer the explanation
		//
		// 2� - select RECENT_TERM_ID from RECENT_TERM except all (RECENT_RES)
		// the above query select all the recent terms except all the most recent ones which were retrieved 
		// with the 1� query. Here we have selected all the recent terms which we want to remove from the db.
		// let's call the result of this query OLD_RES
		//
		// 3� - delete from RECENT_TERM where RECENT_TERM_ID in (OLD_RES)
		// the above query remove from the database all the recent terms which are considered old!
		// we have finished
		String query = 
				  "delete from APP.RECENT_TERM "
				+ "where RECENT_TERM_ID in ("
				+ "select RECENT_TERM_ID "
				+ "from APP.RECENT_TERM "
				+ "except all ( "
				+ "select RECENT_TERM_ID "
				+ "from APP.RECENT_TERM "
				+ "order by RECENT_TERM_ID desc "
				+ "fetch first ? rows only) )";

		try {
			
			// connect to the db
			con = catalogue.getConnection();
			
			// prepare the query
			PreparedStatement stmt = con.prepareStatement( query );
			
			stmt.clearParameters();
			
			CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO( catalogue );
			
			// get the user preference related to the max recent terms
			int maxRecentTerms = prefDao.getPreferenceIntValue( CataloguePreference.maxRecentTerms, 15 );
			stmt.setInt(1, maxRecentTerms );
			
			// execute query
			stmt.executeUpdate();
			
			stmt.close();
			con.close();
			
		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		
	}

	@Override
	public DescribedTerm getByResultSet(ResultSet rs) throws SQLException {

		String code = rs.getString( "RECENT_TERM_CODE" );
		
		String label = rs.getString( "RECENT_TERM_LABEL" );
		
		// add a new recent term
		return new DescribedTerm(code, label);
	}

	@Override
	public boolean remove(DescribedTerm object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(DescribedTerm object) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DescribedTerm getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}
}