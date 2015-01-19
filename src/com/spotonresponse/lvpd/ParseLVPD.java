package com.spotonresponse.lvpd;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import com.spotonresponse.tools.DBUtils;
import com.spotonresponse.tools.Global;
import com.spotonresponse.beans.GeoLocation;
import com.spotonresponse.beans.Incident;

public class ParseLVPD {

	private static final Logger logger = Logger.getLogger(ParseLVPD.class);

	private static DBUtils db = null;
	private static boolean useXcore = false;

	private static int MAX_DB_RUNS = 300;
	
	public static void sleep(int time) {
		try {
			Thread.sleep(time); // 1000 milliseconds is one second.
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public static void main(String[] args) {

		/*
		 * Read the command line options to get the location of the properties
		 * file we are to use
		 */
		String propertiesFile = System.getProperty("propfile");
		try {
			propertiesFile.length();
		} catch (Exception ex) {
			System.out.println("No properties file specified: " + ex);
			System.exit(1);
		}

		/*
		 * Read the properties file for connection and user information
		 */
		Properties prop = new Properties();
		db = new DBUtils();
		int timer = 60000;

		try {
			// load a properties file
			prop.load(new FileInputStream(propertiesFile));

			logger.debug("Done reading properties: DBurl: "
					+ prop.getProperty("DBurl"));
			// get the property value and print it out
			Global.uicdsURL = prop.getProperty("uicdsURL");
			Global.uicdsUser = prop.getProperty("uicdsUser");
			Global.uicdsPass = prop.getProperty("uicdsPass");

			Global.UICDStimeout = Integer.valueOf(prop
					.getProperty("UICDStimeout"));
			Global.ProjectID = Integer.valueOf(prop.getProperty("ProjectID"));

			String dbUser = prop.getProperty("DBuser");
			String dbPass = prop.getProperty("DBpassword");
			String dbURL = prop.getProperty("DBurl");

			timer = Integer.valueOf(prop.getProperty("SecondsBetweenRuns")) * 1000;

			db.setUserName(dbUser);
			db.setPassword(dbPass);
			db.setURL(dbURL);
			//db.getConnection();

		} catch (IOException ex) {
			logger.fatal("Error setting properties");
			ex.printStackTrace();
			System.exit(2);
		}

		try {
			// String url = args[0];
			String url = "http://www.lvmpd.com/News/CurrentTraffic/tabid/450/Default.aspx";
			int db_run = 0;
			while (true) {
				
				/*
				 * The open/close database in this loop was added to try and prevent a java heap error
				 * After so long (a few days of running, the program would throw an exception
				 *    Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
				 * The thought is to allow the connection to remain open for some amount of runs
				 * Then close and re-open as needed to eliminate this exception.  I am not sure this is
				 *   the correct solution, so it should be looked at further
				 */
				
				if (db_run > MAX_DB_RUNS) {
					// Close the database connection
					db.close();
					
					// Get a database connection
					db.getConnection();
					
					// Reset counter
					db_run = 0;
				}
				
				
				// Determine if we should update the XCore table
				useXcore = db.isXCoreProject(Global.ProjectID);

				// Process the data
				getData(url);
	
				// Increment DB run counter
				db_run++;
				
				// Sleep until next run
				sleep(timer);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// Clean up and close the database connections
			db.close();
		}
	}

	public static GeoLocation getGeoLocation(String address) {
		/*
		 * 
		 * Go get a lat/long to coorespond to the address we pulled
		 */
		// http://maps.googleapis.com/maps/api/geocode/xml
		// String key = "AIzaSyDvu0lsvg3VCRWkne7dYzQN9XXyyg15ASk";

		GeoLocation gl = new GeoLocation();
		URI uri = null;
		try {
			uri = new URI("http", "maps.google.com", "/maps/api/geocode/xml",
					"address=" + address + ", Las Vegas, NV" + "&sensor=false",
					null);
			String geurl = uri.toASCIIString();
			long start = System.currentTimeMillis();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			org.w3c.dom.Document gedoc = db.parse(new URL(geurl).openStream());

			long end = System.currentTimeMillis();
			long totalTime = end - start;
			System.out
					.println("Got lat/lon from Google in " + totalTime + "ms");

			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			XPathExpression xPathExpression = xpath
					.compile("//GeocodeResponse/result/geometry/location/lat");
			gl.setLatitude(Double.valueOf(xPathExpression.evaluate(gedoc,
					XPathConstants.STRING).toString()));
			xPathExpression = xpath
					.compile("//GeocodeResponse/result/geometry/location/lng");
			gl.setLongitude(Double.valueOf(xPathExpression.evaluate(gedoc,
					XPathConstants.STRING).toString()));

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.lang.NumberFormatException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return gl;
	}

	public static void getData(String url) {
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();

			// Make sure we have a good Database connection
			if (!db.checkConnection()) {
				db.getConnection();
			}

			Element table = doc.getElementById("dnn_ctr1018_View_gvTraffic");
			Elements trs = table.getElementsByTag("tr");
			ArrayList<Incident> incidentList = new ArrayList<Incident>();

			for (Element tr : trs) {
				Elements tds = tr.getElementsByTag("td");
				if (tds.size() > 1) {

					Incident _incident = new Incident();

					_incident.setId(tds.get(0).text());
					_incident.setDate(tds.get(1).text());
					_incident.setCode(tds.get(2).text());
					_incident.setDesc(tds.get(3).text());
					_incident.setAddr(tds.get(4).text());

					// See if we already have this incident, we will not
					// re-geocode
					// if is exists
					if (!db.entryExists(_incident.getId())) {
						GeoLocation gl = getGeoLocation(tds.get(4).text());
						if (gl.getLatitude() != null) {
							_incident.setLatitude(gl.getLatitude());
							_incident.setLongitude(gl.getLongitude());

							// Create the new Geofence and Incident
							db.insertGeofence(_incident);
							db.insertIncident(_incident);

							// Need to add to XCore Adapter
							if (useXcore) {
								logger.debug("Adding to XCore table");
								db.insertXCore(_incident);
							}
						} else {
							// We did not get a location, so skip this entry
							logger.debug("No location returned for address: "
									+ tds.get(4).text());
						}

					}

					// Add the data to a list we can loop through to delete old
					incidentList.add(_incident);

				}

			}

			// Use the created list to expire incidents that have left the list
			db.checkExpiration(incidentList);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
