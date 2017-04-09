package edu.saic.mackay;

import java.util.Calendar;
import java.util.HashSet;
import java.util.StringTokenizer;

public class StringUtil {
	public static final int YEAR_TOKEN = 0;
	public static final int MONTH_TOKEN = 1;
	public static final int WEEKDAY_TOKEN = 2;
	public static final int LATLON_TOKEN = 3;
	public static final int UNKNOWN_TOKEN = 4;
	
	public static int findTokenType(String str) {
		if (getYear(str) != -1) return YEAR_TOKEN;
		else if (getMonth(str) != -1) return MONTH_TOKEN;
		else if (getWeekday(str) != -1) return WEEKDAY_TOKEN;
		//else if (isLatLon(str)) return LATLON_TOKEN;
		return UNKNOWN_TOKEN; 
	}
	
	public static int startsWithYear(String line) {
		String[] patts = {"18", "19"};
		for (String patt: patts) {
			String ystr="";
			if (line.startsWith(patt)) {
				ystr = line.substring(0, 4);
			}
			try {
				int yy = Integer.parseInt(ystr);
				return yy;
			} catch (Exception e) {}
		}
		return -1;
	}

	public static int getYear(String line) {
		String patt1 = "18";
		String patt2 = "19";
		int index1 = line.indexOf(patt1);
		int index2 = line.indexOf(patt2);
		String ystr="";
		try {
			if (index1 >= 0) ystr = line.substring(index1, index1+4);
			else if (index2 >= 0) ystr = line.substring(index2, index2+4);
			int yy = Integer.parseInt(ystr);
			return yy;
		} catch (Exception e) {}
		return -1;
	}

	public static int startsWithMonth(String line) {
		String line2 = line.toLowerCase();
		String[] patt = {"january", "february", "march", "april", "may", "june",
				"july", "august", "september", "october", "november", "december"};
		for (int i=0; i<patt.length; i++) {
			if (line2.startsWith(patt[i])) return i;
		}
		String[] patt2 = {"jan.", "feb.", "mar.", "apr.", "may", "jun.",
				"jul.", "aug.", "sep.", "oct.", "nov.", "dec."};
		for (int i=0; i<patt2.length; i++) {
			if (line2.startsWith(patt2[i])) return i;
		}
		return -1;
	}
	
	public static int getMonth(String line) {
		String line2 = line.toLowerCase();
		String[] patt = {"january", "february", "march", "april", "may", "june",
				"july", "august", "september", "october", "november", "december"};
		for (int i=0; i<patt.length; i++) {
			if (line2.indexOf(patt[i]) >= 0) return i;
		}
		String[] patt2 = {"jan.", "feb.", "mar.", "apr.", "may", "jun.",
				"jul.", "aug.", "sep.", "oct.", "nov.", "dec."};
		for (int i=0; i<patt2.length; i++) {
			if (line2.indexOf(patt2[i]) >= 0) return i;
		}
		return -1;
	}

	public static boolean hasYearMonth(String line) { // December 1871
		return ((getYear(line)!=-1) && (startsWithMonth(line)!=-1));
	}

	public static int getMonthPos(String line) {
		String line2 = line.toLowerCase();
		String[] patt = {"january", "february", "march", "april", "may", "june",
				"july", "august", "september", "october", "november", "december"};
		for (int i=0; i<patt.length; i++) {
			int pos=-1;
			if ((pos=line2.indexOf(patt[i])) >= 0) return pos;
		}
		String[] patt2 = {"jan.", "feb.", "mar.", "apr.", "may", "jun.",
				"jul.", "aug.", "sep.", "oct.", "nov.", "dec."};
		for (int i=0; i<patt2.length; i++) {
			int pos=-1;
			if ((pos=line2.indexOf(patt2[i])) >= 0) return pos;
		}
		return -1;
	}
	
	public static int getDateFromMonth(String line, int pos) {
		String line2 = line.substring(pos);
		return getDate(line2);
	}

	public static boolean hasMonthDate(String line) { // Thurs. Nov. 2.  205 Miles
		if (getMonth(line) != -1) { // has month!
			int pos = getMonthPos(line);
			if (getDateFromMonth(line, pos) != -1) return true;
		}
		return false;
	}
	
	public static int startsWithWeekday(String line) {
		String line2 = line.toLowerCase();
		String[] patt = {"sun.", "mon.", "tues.", "wed.", "thurs.", "fri.", "sat."};
		for (int i=0; i<patt.length; i++) {
			if (line2.startsWith(patt[i])) return i;
		}
		return -1;
	}

	public static int getWeekday(String line) {
		String line2 = line.toLowerCase();
		String[] patt = {"sun.", "mon.", "tues.", "wed.", "thurs.", "fri.", "sat."};
		for (int i=0; i<patt.length; i++) {
			if (line2.indexOf(patt[i]) >= 0) return i;
		}
		return -1;
	}
	
	public static boolean isLatLon(String line) {
		String line2 = line.toLowerCase();
		String patt = "lat";
		String[] slist = line2.split(" ");
		return slist[0].startsWith(patt);
	}

	public static double[] getLatLon(String line) { // "Lat 36˚ 21”  Long. 126˚ 07”" or "Lat. 51˚. 18N"
		String line2 = line.toLowerCase();
		String patt = "lat";
		String patt2 = "˚";
		String patt3 = "”";
		String patt4 = "long";
		String[] slist = line2.split(" ");
		double lat=-1.0, lon=-1.0;
		if (slist[0].startsWith(patt)) {
			if (slist[1].endsWith(patt2) && (slist[2].endsWith(patt3))) {
				String tmp = slist[1].substring(0, slist[1].length()-1)+"."+
							slist[2].substring(0, slist[2].length()-1); // 36.21
				lat = Double.parseDouble(tmp);
			}
			if (slist[3].startsWith(patt4)) {
				if (slist[4].endsWith(patt2) && (slist[5].endsWith(patt3))) {
					String tmp = slist[4].substring(0, slist[4].length()-1)+"."+
								slist[5].substring(0, slist[5].length()-1); // 36.21
					lon = Double.parseDouble(tmp);
				}
			}
			if ((lat==-1.0) && (lon==-1.0)) return null;
			return new double[]{lat, lon};
		}
		return null;
	}
	
	public static int getDate(String line) {
		StringTokenizer st = new StringTokenizer(line);
		if (st.countTokens() == 1) st = new StringTokenizer(line, ".");
		st.nextToken();
		String dstr = st.nextToken();
		if (dstr.endsWith(".")) dstr = dstr.substring(0, dstr.length()-1);
		String[] patts = {"st", "nd", "rd", "th"};
		for (String patt: patts) {
			if (dstr.endsWith(patt)) {
				dstr = dstr.substring(0, dstr.length()-patt.length());
				break;
			}
		}
		try {
			int yy = Integer.parseInt(dstr);
			return yy;
		} catch (Exception e) {}
		return -1;
	}
	
	public static String cleanUp(String str) { // delete: "—"
		String newstr = str.replace('—', ' ');
		return newstr;
	}
	
	public static HashSet<String> suspiciousWords(String line) {
		String[] patts = {"-", "â", "á", "ā", "é", "e̍", "í", "î", "ō͘", "ò", "ô", "o͘", "ó͘", "Ô͘", "û", "ú", "u̍", "ū"};
		HashSet<String> result = new HashSet<String>();
		String[] slist = line.split(" ");
		for (String s: slist) {
			for (String patt: patts) {
				if (s.indexOf(patt) >= 0) result.add(s);
			}
		}
		return result;
	}
	
	public static String getDateStr(Calendar cal) {
		int yy = cal.get(Calendar.YEAR);
		int mm = cal.get(Calendar.MONTH)+1;
		int dd = cal.get(Calendar.DATE);
		return ""+yy+"-"+(mm<10?"0"+mm:mm)+"-"+(dd<10?"0"+dd:dd);
	}
}
