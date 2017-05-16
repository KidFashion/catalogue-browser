package term_code_generator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import global_manager.GlobalManager;
import messages.Messages;
import version_control.ReleaseVersion;

/**
 * Class used to create a new code for a term,
 * given its code mask. TODO move all this stuff to
 * the term class or make this class non static
 * @author avonva
 *
 */
public class CodeGenerator {

	public static int convertAlphaNumToInt32 ( String alphacode ) {

		char[] figures = alphacode.toCharArray();

		int val = 0;

		for ( int j = 0 ; j < figures.length ; j++ ) {
			int c = (int) figures[figures.length - j - 1];

			if ( ( c >= 48 ) && ( c <= 58 ) ) /* Allowed number are 0 to 9 */
				val = val + (int) ( Math.pow( 32, j ) * ( c - 48 ) );
			else {
				int offset = 0;
				if ( c >= 65 )
					offset = 0;
				if ( c >= 73/* I */)
					offset = -1;
				if ( c >= 79/* O */)
					offset = -2;
				if ( c >= 85/* U */)
					offset = -3;
				if ( c >= 87/* W */)
					offset = -4;
				val = val + (int) ( Math.pow( 32, j ) * ( c - 65 + 10 + offset ) );
			}
		}

		return val;
	}

	public static String convertIntToAlphaNum32 ( int intCode , int numFigures ) {

		String alphaNum = "";
		int val;

		while ( intCode > 0 ) {

			val = intCode % 32;
			intCode = intCode / 32;

			if ( val < 10 ) { /* all numbers */
				val = val + 48;
				alphaNum = (char) val + alphaNum;
			} else {
				val = val - 10 + 65;
				if ( val >= 73 ) /* i */
					val++;
				if ( val >= 79 ) /* o */
					val++;
				if ( val >= 85 ) /* u */
					val++;
				if ( val >= 87 ) /* w */
					val++;
				alphaNum = (char) val + alphaNum;
			}
		}

		return alphaNum;
	}

	/*
	 * Possible increments
	 * 
	 * # numbers -> 0,1,2,3,4,5,6,7,8,9
	 * 
	 * @ letters excluding i, o, u, w ->
	 * A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,V,X,Y,Z � numbers and letters ->
	 * 0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,V,X,Y,Z
	 */

	private static char[]	/* # */numberCode		= "0123456789".toCharArray();
	private static char[]	/* @ */alphaCode		= "ABCDEFGHJKLMNPQRSTVXYZ".toCharArray();
	private static char[]	/* � */numberAlphaCode	= "0123456789ABCDEFGHJKLMNPQRSTVXYZ".toCharArray();

	private static char[] initialiseCode ( char[] mask ) {

		char[] retVal = new char[mask.length];

		for ( int i = 0 ; i < mask.length ; i++ ) {
			if ( mask[i] == '#' )
				retVal[i] = numberCode[0];
			if ( mask[i] == '@' )
				retVal[i] = alphaCode[0];
			if ( mask[i] == '�' )
				retVal[i] = numberAlphaCode[0];
		}
		return retVal;
	}

	private static String initialiseCode ( String mask ) {
		return ( String.valueOf( initialiseCode( mask.toCharArray() ) ) );
	}

	private static int findChar ( char[] list , char elem ) {
		int retpos = -1;
		for ( int j = 0 ; j < list.length ; j++ ) {
			if ( list[j] == elem ) {
				retpos = j;
				break;
			}
		}
		return retpos;
	}

	private static char[] incrementCodeRec ( char[] alphaNumCode , char[] mask , int i ) {

		int index;

		if ( mask[i] == '#' ) {

			index = findChar( numberCode, alphaNumCode[i] );

			if ( index == ( numberCode.length - 1 ) ) {
				alphaNumCode[i] = numberCode[0];
				incrementCodeRec( alphaNumCode, mask, i - 1 );
			} else {
				alphaNumCode[i] = numberCode[index + 1];
			}
		} else if ( mask[i] == '@' ) {
			index = findChar( alphaCode, alphaNumCode[i] );
			if ( index == ( alphaCode.length - 1 ) ) {
				alphaNumCode[i] = alphaCode[0];
				incrementCodeRec( alphaNumCode, mask, i - 1 );
			} else {
				alphaNumCode[i] = alphaCode[index + 1];
			}
		} else {
			if ( mask[i] == '�' ) {
				index = findChar( numberAlphaCode, alphaNumCode[i] );
				if ( index == ( numberAlphaCode.length - 1 ) ) {
					alphaNumCode[i] = numberAlphaCode[0];
					incrementCodeRec( alphaNumCode, mask, i - 1 );
				} else {
					alphaNumCode[i] = numberAlphaCode[index + 1];
				}
			}
		}
		return alphaNumCode;
	}

	private static String incrementCode ( String alphaNumCode , String mask ) {

		return String.valueOf( incrementCodeRec( alphaNumCode.toCharArray(), mask.toCharArray(),
				mask.length() - 1 ) );

	}

	private static String restructureCode ( ArrayList< StringSegment > constantSegments ,
			ArrayList< StringSegment > variableSegments , String alphaNumCode , String codeMask ) {

		String retVal = "";
		/*
		 * I need to keep for the variable an indication where I am copying the
		 * data since the code was compacted in one string
		 */
		int variableStart = 0;
		int variableEnd = 0;
		int iterations = constantSegments.size() + variableSegments.size();
		for ( int i = 0 ; i < iterations ; i++ ) {
			/*
			 * I need to have a variable segment to use, if there are segments
			 * of the two types, and the variable that starts before
			 */
			if ( ( ( variableSegments.size() > 0 ) && ( constantSegments.size() > 0 ) && ( variableSegments
					.get( 0 ).start < constantSegments.get( 0 ).start ) )
					|| ( ( variableSegments.size() > 0 ) && ( constantSegments.size() == 0 ) ) ) {
				/*
				 * I have to include a variable segment, which will reflect a
				 * part of the generated code
				 */
				variableEnd = variableStart + variableSegments.get( 0 ).length;
				retVal = retVal + alphaNumCode.substring( variableStart, variableEnd );
				variableStart = variableEnd;
				variableSegments.remove( 0 );
			} else
				/*
				 * I need to have a constant segment to use, if the constant start
				 * before
				 */
				if ( ( ( variableSegments.size() > 0 ) && ( constantSegments.size() > 0 ) && ( constantSegments
						.get( 0 ).start < variableSegments.get( 0 ).start ) )
						|| ( ( variableSegments.size() == 0 ) && ( constantSegments.size() >= 0 ) ) ) {
					/*
					 * I have to include a variable segment, which will reflect a
					 * part of the code mask
					 */
					retVal = retVal
							+ codeMask.substring( constantSegments.get( 0 ).start, constantSegments.get( 0 )
									.getEnd() );
					constantSegments.remove( 0 );
				}
		}
		return retVal;

	}

	/**
	 * Get the code of a new term given the code mask
	 * @param codeMask
	 * @return
	 */
	public static String getTermCode ( String codeMask ) {

		// I am preparing the selection mask

		ArrayList< StringSegment > constantSegments = new ArrayList< StringSegment >();
		ArrayList< StringSegment > variableSegments = new ArrayList< StringSegment >();

		boolean readingConstant = false;
		boolean readingVariable = false;

		for ( int i = 0 ; i < codeMask.length() ; i++ ) {
			if ( ( codeMask.charAt( i ) == '#' ) || ( codeMask.charAt( i ) == '@' )
					|| ( codeMask.charAt( i ) == '�' ) ) {
				// I am reading a variable
				if ( readingConstant ) {
					// I was reading a constant
					StringSegment strSeg = new StringSegment();
					strSeg.start = i;
					strSeg.length = 1;
					variableSegments.add( strSeg );
				} else {
					// if I was not reading a variable it must be the first time
					if ( !readingVariable ) {
						StringSegment strSeg = new StringSegment();
						strSeg.start = i;
						strSeg.length = 1;
						variableSegments.add( strSeg );
					} else {
						// I was already reading a variable
						StringSegment strSeg = variableSegments.get( variableSegments.size() - 1 );
						strSeg.length++;
					}
				}
				readingVariable = true;
				readingConstant = false;
			} else {
				// I am reading a constant
				if ( readingVariable ) {
					/* I was reading a variable */
					StringSegment strSeg = new StringSegment();
					strSeg.start = i;
					strSeg.length = 1;
					constantSegments.add( strSeg );
				} else {
					/*
					 * I should have been reading a constant, otherwise it is
					 * the first time
					 */
					if ( !readingConstant ) {
						// then I have to create a segment because I was not
						// doing anything
						StringSegment strSeg = new StringSegment();
						strSeg.start = i;
						strSeg.length = 1;
						constantSegments.add( strSeg );
					} else {
						/* I was already reading a constant */
						StringSegment strSeg = constantSegments.get( constantSegments.size() - 1 );
						strSeg.length++;
					}
				}
				readingVariable = false;
				readingConstant = true;
			}

		}

		/* prepare SQL variable */

		String sqlVariable = "";

		for ( int i = 0 ; i < variableSegments.size() ; i++ ) {
			sqlVariable = sqlVariable + "SUBSTR(TERM_CODE," + ( variableSegments.get( i ).start + 1 ) + ","
					+ variableSegments.get( i ).length + ")";
			if ( i < variableSegments.size() - 1 ) {
				sqlVariable = sqlVariable + "||";
			}
		}

		String sqlConstant = "";
		/* prepare SQL constant */
		for ( int i = 0 ; i < constantSegments.size() ; i++ ) {
			sqlConstant = sqlConstant
					+ "SUBSTR(TERM_CODE,"
					+ ( constantSegments.get( i ).start + 1 )
					+ ","
					+ constantSegments.get( i ).length
					+ ")='"
					+ codeMask
					.substring( constantSegments.get( i ).start, constantSegments.get( i ).getEnd() )
					+ "'";
			if ( i < constantSegments.size() - 1 ) {
				sqlConstant = sqlConstant + " AND ";
			}
		}

		/* prepare the mask for the variable part to use in the increment */

		String variableMask = "";

		for ( int i = 0 ; i < variableSegments.size() ; i++ ) {
			variableMask = variableMask
					+ codeMask
					.substring( variableSegments.get( i ).start, variableSegments.get( i ).getEnd() );
		}

		Connection con = null;
		String alphaNumCode = "";
		String currAlphaNumCode = "";

		try {
			
			GlobalManager manager = GlobalManager.getInstance();
			con = manager.getCurrentCatalogue().getConnection();
			
			/* get the maximum code according to the specified mask */
			String sql = "select max(" + sqlVariable + ") as CURR_CODE from APP.TERM";

			if ( sqlConstant.length() > 0 ) {
				sql = sql + " where " + sqlConstant;
			}

			System.err.println( "SQL for searching the last generated code: " + sql );
			PreparedStatement codeStmt = con.prepareStatement( sql );

			ResultSet codeRs = codeStmt.executeQuery();

			while ( codeRs.next() ) {
				currAlphaNumCode = codeRs.getString( "CURR_CODE" );
			}
			if ( ( currAlphaNumCode == null ) || currAlphaNumCode == "" ) {
				// this is the first instance of this code therefore it has to
				// be initialised
				currAlphaNumCode = initialiseCode( variableMask );
			}

			alphaNumCode = restructureCode( constantSegments, variableSegments,
					incrementCode( currAlphaNumCode, variableMask ), codeMask );

		} catch ( SQLException e ) {
			e.printStackTrace();
		}
		return alphaNumCode;

	}
}