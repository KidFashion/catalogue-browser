package ui_pending_action_listener;

import dcf_log.DcfResponse;
import dcf_pending_action.PendingActionStatus;
import messages.Messages;

public class PendingPublishMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {
		
		String msg = null;

		switch ( response ) {
		case ERROR:
			msg = Messages.getString( "Publish.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "Publish.ApMessage" );
			break;
		case FORBIDDEN:
			msg = Messages.getString( "Publish.MinorErrorMessage" );
			break;
		default:
			break;
		}

		return msg;
	}

	/**
	 * No status message is shown for publish
	 */
	@Override
	public String getStatusMessage(PendingActionStatus status) {
		
		String msg = null;
		
		switch ( status ) {
		case STARTED:
			msg = Messages.getString( "Publish.StartedMessage" );
			break;
		case ERROR:
			msg = Messages.getString( "Publish.NoAttMessage" );
			break;
		case COMPLETED:
			msg = Messages.getString( "Publish.OkMessage" );
		default:
			break;
		}
		
		return msg;
	}
}
