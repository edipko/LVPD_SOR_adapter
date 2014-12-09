package com.spotonresponse.beans;

import java.text.ParseException;
import java.util.Date;

public class Incident {

	private String id;
	private String date;
	private String code;
	private String desc;
	private String addr;
	private Double latitude;
	private Double longitude;
	private String WpID;
	private String IgID;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getDate() {
		java.text.SimpleDateFormat sdf = 
		     new java.text.SimpleDateFormat("M/d/yyyy H:m:s a");
		Date parsedDate = null;
		try {
			parsedDate = sdf.parse(this.date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		java.text.SimpleDateFormat sdf2 = 
			     new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateTime = sdf2.format(parsedDate);
		return currentDateTime;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getAddr() {
		return addr;
	}
	public void setAddr(String addr) {
		this.addr = addr;
	}
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	public String getWpID() {
		return WpID;
	}
	public void setWpID(String wpID) {
		WpID = wpID;
	}
	public String getIgID() {
		return IgID;
	}
	public void setIgID(String igID) {
		IgID = igID;
	}
	
	
	
}
