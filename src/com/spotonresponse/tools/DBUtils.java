package com.spotonresponse.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import com.spotonresponse.beans.Incident;

public class DBUtils {

	private static final Logger logger = Logger.getLogger(DBUtils.class);
	private static Connection conn = null;
	private static String _userName = "";
	private static String _password = "";
	private static String _url = "";

	private static int lastInsertId = 0;


	public void setUserName(String userName) {
		_userName = userName;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public void setURL(String url) {
		_url = url;
	}

	public void getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			DBUtils.conn = DriverManager.getConnection(_url, _userName, _password);
			logger.debug("Database connection established");
		} catch (Exception e) {
			logger.fatal("Cannot connect to database server at: " + _url
					+ " Error: " + e);
			System.exit(1);
		}
	}

	public boolean close() {
		try {
			conn.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	
	private void getLastinsertVal() {
		// System.out.println("Getting last insert value");
		try {
			Statement s = conn.createStatement();
			s.executeQuery("SELECT LAST_INSERT_ID();");
			ResultSet rs = s.getResultSet();
			rs.next();
			lastInsertId = rs.getInt(1);
			rs.close();
			s.close();
			// System.out.println("Last insert was: " + lastInsertId);
		} catch (Exception ex) {
			logger.error("Exception occurred: " + ex);
			lastInsertId = 0;
		}
	}


	public boolean insertGeofence(Incident d) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("INSERT into geofence(active,circle,radius,lat,lon,points) VALUES(?,?,?,?,?,?);");
			s.setBoolean(1, true);
			s.setBoolean(2, true);
			s.setInt(3, 2);
			s.setDouble(4, d.getLatitude());
			s.setDouble(5, d.getLongitude());
			s.setString(6, "");

			int count = s.executeUpdate();
			
			s.close();
			logger.debug(count + " rows were inserted");
			getLastinsertVal();

		} catch (Exception ex) {
			logger.error("Error: " + ex);
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	

   public boolean insertIncident(Incident data) {
	   PreparedStatement s = null;
		try {
			String now = new Date().toString();
			s = conn.prepareStatement("INSERT into incidents(Name, Snippet, Style, Code, Created, CreatedBy, Descriptor,"
					+ "Event, LastUpdated, LastUpdatedBy, State, Type, Ver, WpID, IGid, geoFenceID, projectID) "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
			s.setString(1, data.getId());  //Name
			s.setString(2, data.getDesc()); //Snippet
			s.setString(3, ""); // Style
			s.setString(4, data.getCode()); //Code
			s.setString(5, data.getDate()); //Created
			s.setString(6, "LVMPD Website"); //Created By
			s.setString(7, data.getDesc() + "<br/><b>Address:</b> " + data.getAddr() ); // Descriptor
			s.setString(8, ""); //Event
			s.setString(9, now); // Last Updated
			s.setString(10, "LVMPD Website"); //Last Updated By
			s.setString(11, "Active"); // State
			s.setString(12, "Incident"); //Type
			s.setInt(13, 1); // Version
			s.setString(14, null); //WpID
			s.setString(15, "A"); //IgID
			s.setInt(16, lastInsertId); //getFenceID
			s.setInt(17,  Global.ProjectID); //ProjectID

			int count = s.executeUpdate();
			
			getLastinsertVal();
			s.close();
			System.out.println(count + " rows were inserted - Timestamp is: " + data.getDate());
		} catch (Exception ex) {
			System.out.println("An error occurred in insertData: " + ex);
			System.out.println("Query: " + s.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
   }
   
   
   public boolean insertXCore(Incident data) {
	   
	   PreparedStatement s = null;
		try {
			s = conn.prepareStatement("INSERT into uicdsIncidentMgmt(projectID, action, uuid, orgID, type, title"
					+ ", description, status, reason, address, lat, lon, radius, deviceLat, deviceLon"
					+ ", updateTimestamp, incidentID) "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
			s.setInt(1, Global.ProjectID);  //projectID
			s.setString(2, "ADD"); //action
			s.setString(3, "d7abce2b-4304-4530-9559-8a380c482b4b"); // uuid
			s.setInt(4, 23); //orgID
			s.setString(5, "Incident"); //type
			s.setString(6, data.getId()); //title
			s.setString(7, data.getAddr() + " " + data.getDesc()); // Description
			s.setString(8, "Active"); //Status
			s.setString(9, ""); // Reason
			s.setString(10, data.getAddr()); //address
			s.setDouble(11, data.getLatitude()); // Latitude
			s.setDouble(12, data.getLongitude()); //Longitude
			s.setInt(13, 1); // radius
			s.setDouble(14, data.getLatitude()); //deviceLat
			s.setDouble(15, data.getLongitude()); //deviceLon
			s.setString(16, data.getDate()); // updateTimestamp
			s.setInt(17, lastInsertId);

			s.executeUpdate();
			
			s.close();
		} catch (Exception ex) {
			System.out.println("An error occurred in insertData: " + ex);
			System.out.println("Query: " + s.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
   }
   
	public String getEntry(Incident data) {
		PreparedStatement s;
		String Name = "";
		
		try {
			s = conn.prepareStatement("SELECT Name FROM incidents WHERE Name = ? and projectID = ?");
			s.setString(1,data.getId());
			s.setInt(2,  Global.ProjectID);
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				if (rs.next()) {
					Name = rs.getString(1);
				}
				rs.close();
				s.close();
			}
			return Name;

		} catch (Exception ex) {
			System.out.println("Query error in getEntry: " + ex);
			return Name;
		}
	}
	


	public boolean entryExists(String id) {
		logger.debug("Checking if entry exists: " + id);
		PreparedStatement s;
		try {
			s = conn.prepareStatement("SELECT * FROM incidents WHERE Name = ? and projectID = ? and State = ?");
			//s = conn.prepareStatement("SELECT * FROM incidents WHERE Name = ? and projectID = ?");
			s.setString(1, id);
			s.setInt(2, Global.ProjectID);
			s.setString(3, "Active");
			boolean result = false;
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				if (rs.next()) {
					result = true;
				}
				rs.close();
				s.close();
			}
			if (result) {
				return true;
			}
			return false;

		} catch (Exception ex) {
			logger.error("Query error in entryExists(String id): " + ex);
			ex.printStackTrace();
			return false;
		}
	}

	public boolean isXCoreProject(int projectID) {
		PreparedStatement s;
		int active = 0;
		try {
			s = conn.prepareStatement("SELECT active from uicdsAdapters where project_id=?");
			s.setInt(1,  projectID);
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				rs.next();
				active = rs.getInt(1);
				rs.close();
			}
			s.close();
			if (active == 1) {
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			logger.error("Error isXCoreProject: ");
			ex.printStackTrace();
			return false;
		}
	}
	
	public void expireIncident(String id) {
		PreparedStatement s;
		try {
			// Expire the geofence first
			// Get the geofenceID first
			s = conn.prepareStatement("SELECT geofenceID, incidentID FROM incidents WHERE Name=? and projectID=?");
			s.setString(1, id);
			s.setInt(2, Global.ProjectID);
			int geofenceID=0;
			int incidentID=0;
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				rs.next();
				geofenceID = rs.getInt(1);
				incidentID = rs.getInt(2);
				rs.close();
			}
			s = conn.prepareStatement("UPDATE geofence set active=0 where geofenceID=?");
			s.setInt(1, geofenceID);
			s.executeUpdate();
						
			s = conn.prepareStatement("UPDATE incidents set State='Inactive', LastUpdatedBy='LVPD DBUtils' WHERE incidentID=?");
			s.setInt(1, incidentID);
			s.executeUpdate();
			
			
			// Add entry to the Uicds/XCore table if needed
			if (isXCoreProject(Global.ProjectID)) {
				if (incidentID > 0) {				
					java.text.SimpleDateFormat sdf2 = 
						     new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String now = sdf2.format(new Date());
					  
					s = conn.prepareStatement("INSERT into uicdsIncidentMgmt(Action, incidentID, projectID, uuid, orgID, type, title,"
							+ "description, status, reason, address, lat, lon, radius, deviceLat, deviceLon, updateTimestamp) "
							+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
					s.setString(1,  "DELETE"); //Action
					s.setInt(2, incidentID); //incidentID
					s.setInt(3, Global.ProjectID); //projectID
					s.setString(4, " "); //uuid
					s.setInt(5,  0); //orgID
					s.setString(6,  " "); //type
					s.setString(7,  id); //title
					s.setString(8,  " "); //description
					s.setString(9,  " "); //status
					s.setString(10,  " "); //reason
					s.setString(11,  " "); //address
					s.setDouble(12,  0.000); //lat
					s.setDouble(13,  0.000); //lon
					s.setInt(14,  0); //radius
					s.setDouble(15,  0.000); //deviceLat
					s.setDouble(16,  0.000); //deviceLon
					s.setString(17,  now); //updateTimestamp
					
					s.executeUpdate();
				    
				}
			}
		} catch (Exception ex) {
			System.err.println("expireIncident exception: ");
			ex.printStackTrace();
		}
	}


	public boolean checkExpiration(ArrayList<Incident> lvpdData) {
		ArrayList<String> al = new ArrayList<String>();
		for (Incident incident : lvpdData) {
			try {
				al.add(incident.getId());
				//logger.debug("Incident: " + incident.getId());
			} catch (Exception ex) {
                 System.err.println("Error:" + ex);
                 ex.printStackTrace();
			}
		}
		
		PreparedStatement s;
		try {
			s = conn.prepareStatement("SELECT Name "
					+ "FROM incidents "
					+ " WHERE projectID=?"
					+ " AND State='Active'"
					+ " AND Name LIKE \"LLV%\"");
			s.setInt(1,  Global.ProjectID);
			if (s.execute()) {
				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					//logger.debug("Checking: " + rs.getString(1));
					if (!al.contains(rs.getString(1))) {
						logger.debug("Expiring incident: "
								+ rs.getString(1));
						expireIncident(rs.getString(1));
					}
				}
				rs.close();
			}

			s.close();
			
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean checkConnection() {
		PreparedStatement s;
		try {
			s = conn.prepareStatement(" select name from projects where id=?");
			s.setInt(1,  Global.ProjectID);
			s.execute();
			s.close();	
			return true;
		} catch (Exception ex) {
			return false;
		}
			
	}
	
}
