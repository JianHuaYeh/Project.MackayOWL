package edu.saic.mackay;

import java.io.Serializable;
import java.util.Calendar;
import java.util.StringTokenizer;

public class DiaryRecord implements Serializable {
//	<diary>
//	<timestamp>[時間戳記，亦即日記當時的日期，不可為空值]</timestamp>
//	<location>[地點標記，亦即日記當時的地點，可為空值]<location>
//	<temperature>[氣溫標記，可為空值]</temperature>
//	<text>[日記內文，不可為空值]</text>
//	</diary>
	private Calendar timestamp;
	private double[] latlon;
	private String location;
	private double temperature;
	private String text;
	private int id;
	
	public DiaryRecord() {
		this.timestamp = null;
		this.latlon = null;
		this.location = null;
		this.temperature = -999;
		this.text = null;
		this.id = -1;
	}
	
	public int getId() { return this.id; }
	public void setId(int id) { this.id = id; }
	public Calendar getTimestamp() { return timestamp; }
	public void setTimestamp(Calendar timestamp) { this.timestamp = timestamp; }
	public double[] getLatLon() { return latlon; }
	public void setLatLon(double[] latlon) { this.latlon = latlon; }
	public String getLocation() { return location; }
	public void setLocation(String location) { this.location = location; }
	public double getTemperature() { return temperature; }
	public void setTemperature(double temperature) { this.temperature = temperature; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public String getDateStr() {
		int yy = this.timestamp.get(Calendar.YEAR);
		int mm = this.timestamp.get(Calendar.MONTH)+1;
		int dd = this.timestamp.get(Calendar.DATE);
		return ""+yy+"-"+(mm<10?"0"+mm:mm)+"-"+(dd<10?"0"+dd:dd);
	}
	
	public String toXMLString() {
		if (this.timestamp == null) return null;
		String result="<diary><id>"+this.id+"</id>";
		result += "<timestamp>"+getDateStr()+"</timestamp>";
		result += "<timestampmillis>"+this.timestamp.getTimeInMillis()+"</timestampmillis>";
		if (this.location != null) result+="<location>"+this.location+"</location>";
		if (temperature != -999) result+="<temperature>"+this.temperature+"</temperature>";
		if (this.text != null) {
			StringTokenizer st = new StringTokenizer(this.text, "&");
			String str="";
			int count = st.countTokens();
			for (int i=0; i<count-1; i++) str += st.nextToken()+" &#38; ";
			str += st.nextToken();
			result+="<text>"+str+"</text>";
		}
		result += "</diary>";
		return result;
	}
	
	public String toString() {
		return this.toXMLString();
	}
}
