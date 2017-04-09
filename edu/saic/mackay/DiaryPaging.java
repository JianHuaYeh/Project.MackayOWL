package edu.saic.mackay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.TreeSet;

public class DiaryPaging {
	private String basepath;
	private String fname;
	private String outpath;
	private HashSet<String> locations;
	public static final int LOCATION_TOKEN_COUNT=3;
	
	private int currYear=-1;
	private int currMonth=-1;
	private int currDate=-1;
	private double[] currLatLon;
	private String location;
	private double temperature;

	public static void main(String[] args) {
		String basepath="Mackay";
		basepath = "expr";
		String fname="diary.txt";
		DiaryPaging dp = new DiaryPaging(basepath, fname);
		dp.go();
	}
	
	public DiaryPaging(String s1, String s2) {
		this.basepath = s1.endsWith("/")?s1:s1+"/";
		this.fname = s2;
		this.locations = new HashSet<String>();
	}
	
	public Calendar getTimeStamp() {
		if ((this.currYear==-1) || (this.currMonth==-1) || (this.currDate==-1))
			return null;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, this.currYear);
		cal.set(Calendar.MONTH, this.currMonth);
		cal.set(Calendar.DATE, this.currDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
	
	public DiaryRecord parseRecord(ArrayList<String> lines) {
		if (lines.size() == 1) {
			//System.err.println("Single line diary: "+lines.get(0));
		}
		String text="";
		String prev_line="";
		String location="";
		int last_token=StringUtil.UNKNOWN_TOKEN;
		for (String line: lines) {
			try {
				String[] slist = line.split(" ");
				// process first token
				int type = StringUtil.findTokenType(slist[0]);
				switch (type) {
					case StringUtil.YEAR_TOKEN: // 1871
						int year = StringUtil.startsWithYear(line);
						if (year != -1) {
							/*if (year != this.currYear) {
								System.err.println("Year mark changed, from "+this.currYear+" to "+year);
							}*/
							this.currYear = year;
							if (!prev_line.trim().equals("") && last_token==StringUtil.UNKNOWN_TOKEN)
								location = prev_line.trim();
						}
						break;
					case StringUtil.MONTH_TOKEN: // "December 1871" or "November"
						year = StringUtil.getYear(line);
						if (year != -1) {
							/*if (year != this.currYear) {
								System.err.println("Year mark changed, from "+this.currYear+" to "+year);
							}*/
							this.currYear = year;
						}
						this.currMonth = StringUtil.getMonth(line);
						if (!prev_line.trim().equals("") && last_token==StringUtil.UNKNOWN_TOKEN)
							location = prev_line.trim();
						break;
					case StringUtil.WEEKDAY_TOKEN: // "Thurs. Nov. 2.  205 Miles" or "Fri. 3rd." or "Sat. 4.  Miles 218"
						int month = StringUtil.getMonth(line);
						if (month != -1) {
							this.currMonth = month;
							int pos = StringUtil.getMonthPos(line);
							this.currDate = StringUtil.getDateFromMonth(line, pos);
						}
						else { // "Fri. 3rd." or "Thurs.13"
							this.currDate = StringUtil.getDate(line);
						}
						if (!prev_line.trim().equals("") && last_token==StringUtil.UNKNOWN_TOKEN)
							location = prev_line.trim();
						break;
					case StringUtil.LATLON_TOKEN: // Lat 36�� 21��  Long. 126�� 07��
						this.currLatLon = StringUtil.getLatLon(line);
						break;
					case StringUtil.UNKNOWN_TOKEN: // "San Francisco" or "Hong Kong"
						text += line.trim()+" ";
				}
				prev_line = line;
				last_token = type;
			} catch (Exception e) {
				e.printStackTrace(System.err);
				System.err.println(line);
				System.exit(-1);
			}
		}
		if (text.trim().length() > 0) {
			Calendar timestamp = getTimeStamp();
			DiaryRecord dr = new DiaryRecord();
			dr.setTimestamp(timestamp);
			dr.setLatLon(this.currLatLon);
			String str = StringUtil.cleanUp(text);
			dr.setText(str);
			if ((location=refineLocation(location)) != null) {
//				if (timestamp != null)
//					System.err.println("Possible location @ ["+StringUtil.getDateStr(timestamp)+"]: "+location);
//				else
//					System.err.println("Possible location: "+location);
			}
			dr.setLocation(location);
			locations.add(location);
			return dr;
		}
		return null;
	}
	
	public String refineLocation(String str) {
		String[] slist = {"rain", "storm", "typhoon", "weather", "lantern", "farming", "stone", "crow", "sick",
				"p.m", "��", "hospital", "fine", "day", "snow", "atwood"};
		if (!str.equals("") && str.split(" ").length<=LOCATION_TOKEN_COUNT) {
			boolean found=false;
			for (String s: slist)
				if (str.toLowerCase().indexOf(s) >= 0) {
					found = true;
					break;
				}
			if (found) return null;
			return str;
		}
		return null;
	}
		
	public void go() {
		String infname = this.basepath+fname;
		ArrayList<DiaryRecord> recs = new ArrayList<DiaryRecord>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infname), "utf-8"));
			String line="";
			ArrayList<String> lines = new ArrayList<String>();
			TreeSet<String> suspicious = new TreeSet<String>();
			int count=0;
			while ((line=br.readLine()) != null) {
				if ("".equals(line.trim())) {
					// record ended, start parsing
					DiaryRecord rec = parseRecord(lines);
					if (rec != null) {
						HashSet<String> set = StringUtil.suspiciousWords(rec.getText());
						suspicious.addAll(set);
						String xmlstr = rec.toXMLString();
						if (xmlstr != null) {
							rec.setId(++count);
							recs.add(rec);
						}
					}
					lines = new ArrayList<String>();
				}
				else lines.add(line);
			}
			br.close();
			System.err.println("Total "+recs.size()+" records.");
			// location refresh
			String currLoc="";
			int loccount=0;
			for (DiaryRecord rec: recs) {
				String loc = rec.getLocation();
				if (loc!=null && !loc.equals(""))
					currLoc = loc;
				else if (!currLoc.equals("")) {
					rec.setLocation(currLoc);
					loccount++;
				}
			}
			System.err.println("Total "+loccount+" locations set.");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(infname+".xml"), "utf-8"));
			bw.write("<diary-collection>"); bw.newLine();
			for (DiaryRecord rec: recs) {
				String xmlstr = rec.toXMLString();
				bw.write(xmlstr);
				bw.newLine();
			}
			bw.write("</diary-collection>"); bw.newLine();
			bw.close();
			BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(infname+".check"), "utf-8"));
			for (String s: suspicious) {
				bw2.write(s);
				bw2.newLine();
			}
			bw2.close();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(infname+".obj"));
			oos.writeObject(recs);
			oos.writeObject(suspicious);
			oos.close();
			// output locations
			/*for (String loc: locations) {
				System.out.println(loc);
			}*/
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}
