package ui_pending_action_listener;

import dcf_log.DcfResponse;
import dcf_pending_action.PendingActionStatus;
import messages.Messages;

public class PendingUploadDataMessages implements PendingActionMessages {

	@Override
	public String getResponseMessage(DcfResponse response) {

		String msg = null;
		
		switch ( response ) {
		case ERROR:
			msg = Messages.getString( "UpData.ErrorMessage" );
			break;
		case AP:
			msg = Messages.getString( "UpData.ApMessage" );
			break;
		default:
			break;
		}

		return msg;
	}

	@Override
	public String getStatusMessage(PendingActionStatus status) {
		
		String msg = null;
		
		switch ( status ) {
		case STARTED:
			msg = Messages.getString( "UpData.StartedMessage" );
			break;
		case COMPLETED:
			msg = Messages.getString( "UpData.OkMessage" );
			break;
		default:
			break;
		}
		
		return msg;
	}
}
