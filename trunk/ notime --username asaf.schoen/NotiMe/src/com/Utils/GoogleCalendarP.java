package com.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This class is in charge of synchronizing the events (with due dates) with
 * Google Calendar.
 */
public final class GoogleCalendarP {

	/** The authentication token. */
	private static String mAuthToken = null;// "DQAAAHUAAAA4FdYPFHGBAnCXT_-6UlZswMlrOWhdf9X0GeVIxWi5Cmxm-9Z2Hl7eVFNQ6coRTFbv63Rzxe45gPARaqeHpyEsrOyJA3_fWkwSUzzQ_q6Tp9rs2oiX-4YlOTf7Kkl7XLzhdrXdWcHLJEMqj9c8kPvhzzQs_QrTWAMBvecqBU4yTw";;

	/** The user & the password. */
	private static String mUsername = "", mPassword = "";

	/** The last activity. */
	private static long mLastActivity = 0;

	/** The Constant HOSTNAME_VERIFIER. */
	private final static HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(final String hostname, final SSLSession session) {
			return "www.google.com".equals(hostname);
		}
	};

	/**
	 * Authentication in the Google Calendar service through HTTPS.
	 * 
	 * @param force
	 *            - if true, it forces a re-authentication, even if the present
	 *            session isn't timeout
	 * 
	 * @return true if authentication succeeds
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws Exception
	 */
	public final static String authenticate(final boolean force)
			throws MalformedURLException, IOException {
		final long millis = System.currentTimeMillis();
		if (!(force) && (millis - mLastActivity < 1800000)) {
			mLastActivity = millis;
			return mAuthToken;
		} else {
			mLastActivity = millis;
		}

		final HttpsURLConnection uc = (HttpsURLConnection) new URL(
				"https://www.google.com/accounts/ClientLogin").openConnection();
		uc.setHostnameVerifier(HOSTNAME_VERIFIER);
		uc.setDoOutput(true);
		uc.setRequestMethod("POST");
		uc.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
		uc.setUseCaches(false);
		final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(uc
				.getOutputStream()));
		bw.write(URLEncoder.encode("Email", "UTF-8") + "="
				+ URLEncoder.encode(mUsername, "UTF-8") + "&"
				+ URLEncoder.encode("Passwd", "UTF-8") + "="
				+ URLEncoder.encode(mPassword, "UTF-8") + "&"
				+ URLEncoder.encode("source", "UTF-8") + "="
				+ URLEncoder.encode("NotiMe-NotiMe-0.1", "UTF-8") + "&"
				+ URLEncoder.encode("service", "UTF-8") + "="
				+ URLEncoder.encode("cl", "UTF-8"));
		bw.flush();
		bw.close();

		final BufferedReader in = new BufferedReader(new InputStreamReader(uc
				.getInputStream()));
		if (uc.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
			return null;
		}
		// only the 3rd parameter (Auth) is of interest
		in.readLine();
		in.readLine();
		mAuthToken = in.readLine().substring(5);
		in.close();
		return mAuthToken;
	}

	/**
	 * Gets the users' calendars.
	 * 
	 * @return list of all the calendars
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public final static LinkedList<NotiCalendar> getAllCals() throws Exception {

		final String sessionUrl = "http://www.google.com/calendar/feeds/default/owncalendars/full";

		final HttpURLConnection uc = (HttpURLConnection) new URL(sessionUrl)
				.openConnection();
		uc.setDoOutput(true);
		uc.setUseCaches(false);
		uc.setRequestMethod("GET");
		uc.setRequestProperty("Content-Type", "application/atom+xml");
		uc
				.setRequestProperty("Authorization", "GoogleLogin auth="
						+ mAuthToken);

		/* Get a SAXParser from the SAXPArserFactory. */
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		final SAXParser sp = spf.newSAXParser();

		/* Get the XMLReader of the SAXParser we created. */
		final XMLReader xr = sp.getXMLReader();
		/* Create a new ContentHandler and apply it to the XML-Reader */
		final CalendarsParseHandler myParser = new CalendarsParseHandler();
		xr.setContentHandler(myParser);

		/* Parse the xml-data from our URL Connection. */
		xr.parse(new InputSource(uc.getInputStream()));
		/* Parsing has finished. */

		return myParser.getParsedData();
	}

	/**
	 * Gets the events.
	 * 
	 * @param cals
	 *            the calendars from which to get the events.
	 * 
	 * @return the events orders by date and time (the first is the earliest).
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public final static LinkedList<NotiEvent> getEvents(
			final LinkedList<NotiCalendar> cals) throws Exception {

		final LinkedList<NotiEvent> events = new LinkedList<NotiEvent>();
		final Iterator<NotiCalendar> it = cals.listIterator();
		while (it.hasNext()) {

			final String sessionUrl = "http://www.google.com/calendar/feeds/"
					+ it.next().get_id() + "/private/full";

			final HttpURLConnection uc = (HttpURLConnection) new URL(sessionUrl)
					.openConnection();
			uc.setDoOutput(true);
			uc.setUseCaches(false);
			uc.setRequestMethod("GET");
			uc.setRequestProperty("Content-Type", "application/atom+xml");
			uc.setRequestProperty("Authorization", "GoogleLogin auth="
					+ mAuthToken);

			/* Get a SAXParser from the SAXPArserFactory. */
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			final SAXParser sp = spf.newSAXParser();

			/* Get the XMLReader of the SAXParser we created. */
			final XMLReader xr = sp.getXMLReader();
			/* Create a new ContentHandler and apply it to the XML-Reader */
			final EventsParseHandler myParser = new EventsParseHandler();
			xr.setContentHandler(myParser);

			/* Parse the xml-data from our URL Connection. */
			xr.parse(new InputSource(uc.getInputStream()));
			/* Parsing has finished. */

			events.addAll(myParser.getParsedData());
		}
		Collections.sort(events);
		return events;
	}

	/**
	 * Usage example. Just for testing.
	 * 
	 * @param args
	 *            the arguments
	 * 
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void main(final String[] args) throws MalformedURLException,
			IOException {
		GoogleCalendarP.setLogin("noti.me.android", "notimeand");
		// GoogleCalendarP.authenticate(false);
		// System.out.println(mAuthToken);
		mAuthToken = "DQAAAHUAAAA4FdYPFHGBAnCXT_-6UlZswMlrOWhdf9X0GeVIxWi5Cmxm-9Z2Hl7eVFNQ6coRTFbv63Rzxe45gPARaqeHpyEsrOyJA3_fWkwSUzzQ_q6Tp9rs2oiX-4YlOTf7Kkl7XLzhdrXdWcHLJEMqj9c8kPvhzzQs_QrTWAMBvecqBU4yTw";
		try {
			final LinkedList<NotiCalendar> parsedDataList = GoogleCalendarP
					.getAllCals();
			System.out.println(GoogleCalendarP.getEvents(parsedDataList));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * The login has to be set.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 */
	public final static void setLogin(final String username,
			final String password) {
		mUsername = username;
		mPassword = password;
	}
}
