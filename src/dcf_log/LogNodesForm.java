package dcf_log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;

public class LogNodesForm {
	
	private Shell shell;
	private Shell dialog;
	private DcfLog log;
	
	public LogNodesForm( Shell shell, DcfLog log ) {
		this.shell = shell;
		this.log = log;
	}
	
	public void display () {

		this.dialog = new Shell( shell , SWT.TITLE | SWT.APPLICATION_MODAL | SWT.SHELL_TRIM );
		dialog.setText( Messages.getString( "LogNodesForm.Title" ) );
		
		dialog.setLayout( new GridLayout( 1, false ) );
		
		// add the generic log information
		new LogMacroOperationViewer( dialog, log );
		
		// create the table
		new LogNodesTableViewer( dialog, log );
		
		dialog.open();
		dialog.pack();
	}
}
