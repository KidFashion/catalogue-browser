package user_preferences;

/**
 * Preference which is used to store preferences related to the UI,
 * like general settings (hide deprecated, hide not reportable... )
 * @author avonva
 *
 */
public class UIPreference extends Preference {

	public static final String hideDeprMain = "hideDeprMain";
	public static final String hideNotReprMain = "hideNotReprMain";
	public static final String hideDeprDescribe = "hideDeprDescribe";
	public static final String hideNotReprDescribe = "hideNotReprDescribe";
	
	public UIPreference( String key, PreferenceType type, Object value ) {
		super(key, type, value);
	}
}