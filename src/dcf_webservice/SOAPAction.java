package dcf_webservice;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import dcf_user.User;
import utilities.GlobalUtil;

/**
 * Abstract class used to create soap requests and to process soap responses
 * @author avonva
 *
 */

public abstract class SOAPAction {
	
	private String url;
	private String namespace;

	/**
	 * Set the url where we make the request and the namespace of the request
	 * @param url
	 * @param namespace
	 */
	public SOAPAction( String url, String namespace ) {

		this.url = url;
		this.namespace = namespace;
	}

	/**
	 * Create the request and get the response. Process the response and return the results.
	 * @param soapConnection
	 * @return
	 * @throws SOAPException
	 */
	public Object makeRequest() throws SOAPException {

		// TODO to be removed => this is used to avoid certificates
		final boolean isHttps = url.toLowerCase().startsWith( "https" );

		HttpsURLConnection httpsConnection = null;

		if (isHttps) {

			try {

				// Create SSL context and trust all certificates
				SSLContext sslContext = SSLContext.getInstance("SSL");

				TrustManager[] trustAll
				= new TrustManager[] {new TrustAllCertificates()};

				sslContext.init(null, trustAll, new java.security.SecureRandom() );

				// Set trust all certificates context to HttpsURLConnection
				HttpsURLConnection
				.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

				// Open HTTPS connection
				URL link = new URL( url );

				httpsConnection = (HttpsURLConnection) link.openConnection();

				// Trust all hosts
				httpsConnection.setHostnameVerifier(new TrustAllHosts() );

				// Connect
				httpsConnection.connect();
			}
			catch ( IOException | NoSuchAlgorithmException | KeyManagementException e ) {
				e.printStackTrace();
			}
		}

		// open the connection
		SOAPConnection soapConnection = GlobalUtil.openSOAPConnection();

		// request for a ping  to the web service
		SOAPMessage request = createRequest( soapConnection );

		// get the response
		SOAPMessage response = soapConnection.call( request , url );

		// close the soap connection
		soapConnection.close();

		// close https connection // TODO to be removed => this is used to avoid certificates
		if ( isHttps )
			httpsConnection.disconnect();
		
		// parse the response and get the result
		return processResponse ( response );
	}

	/** TODO to be removed => this is used to avoid certificates
	 * Dummy class implementing HostnameVerifier to trust all host names
	 */
	private static class TrustAllHosts implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	/** TODO to be removed => this is used to avoid certificates
	 * Dummy class implementing X509TrustManager to trust all certificates
	 */
	private static class TrustAllCertificates implements X509TrustManager {
		
		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	/**
	 * Create the standard structure of a SOAPMessage, including the
	 * authentication block
	 * @param username
	 * @param password
	 * @return
	 * @throws SOAPException
	 */

	public SOAPMessage createTemplateSOAPMessage ( String prefix ) throws SOAPException {

		// create the soap message
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage soapMsg = msgFactory.createMessage();
		SOAPPart soapPart = soapMsg.getSOAPPart();
		
		// add the content type header
		soapMsg.getMimeHeaders().addHeader( "Content-Type", "text/xml;charset=UTF-8" );
		
		// set the username and password for the https connection
		// in order to be able to authenticate me to the DCF
		Authenticator myAuth = new Authenticator() 
		{
			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{
				User user = User.getInstance();
				
				return new PasswordAuthentication( user.getUsername(), 
						user.getPassword().toCharArray() );
			}
		};

		// set the default authenticator
		Authenticator.setDefault( myAuth );

		// create the envelope and name it
		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration( prefix, namespace );

		// return the message
		return soapMsg;
	}
	
	/**
	 * Get the first attachment of the message
	 * @param message
	 * @return
	 * @throws SOAPException
	 */
	public AttachmentPart getFirstAttachment( SOAPMessage message ) throws SOAPException {

		// get the attachment
		Iterator<?> iter = message.getAttachments();
		
		if ( !iter.hasNext() )
			return null;

		// get the response attachment
		AttachmentPart attachment = (AttachmentPart) iter.next();
		
		return attachment;
	}

	/**
	 * Print a soap message in the System.out
	 * @param soapMsg
	 * @throws SOAPException
	 */
	public static void printSOAPMessage ( String messageType, SOAPMessage soapMsg ) throws SOAPException {

		System.out.println( messageType + " SOAP MESSAGE:\n");

		// write the messages into the system.out and then make a flush with
		// the system.out.println()
		try {
			soapMsg.writeTo( System.out );
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println();
	}
	
	/**
	 * Create the request message which will be sent to the web service
	 * @param con
	 * @return
	 */
	public abstract SOAPMessage createRequest( SOAPConnection con ) throws SOAPException;

	/**
	 * Process the web service response and return something if needed
	 * @param soapResponse the response returned after sending the soap request
	 * @return a processed object. It can be whatever you want, be aware that you
	 * need to cast it to specify its type.
	 */
	public abstract Object processResponse ( SOAPMessage soapResponse ) throws SOAPException;
}