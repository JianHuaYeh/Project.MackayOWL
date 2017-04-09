package edu.saic.mackay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class Diary2SORP {
	private TreeSet<String> suspicious;
	private ArrayList<DiaryRecord> recs;
	private TreeSet<String> stoplist;
	private int range;
	private String path;
	private String destpath;
	public static final int BY_WEEK=0;
	public static final int BY_MONTH=1;
	public static final int BY_DATE=2;
	
	public static void main(String[] args) throws Exception {
		String path="/Desktop/expr/";
		String fname="diary.txt.obj";
		String stopfile="stop.list";
		String dest="corpus";
		int range=Diary2SORP.BY_DATE;
		Diary2SORP d2s = new Diary2SORP(path, fname, dest, range, stopfile);
		d2s.go();
	}
	
	public Diary2SORP(String path, String fname, String destfolder, int range, String stopf) throws Exception {
		this.range = range;
		this.path = path.endsWith("/")?path:path+"/";
		this.destpath = destfolder.endsWith("/")?destfolder:destfolder+"/";
		this.destpath = this.path+this.destpath;
		File dir = new File(this.destpath);
		if (dir.exists()) {
			System.err.println("Destination path exists: "+this.destpath+", deleting content...");
			Process proc = Runtime.getRuntime().exec("rm "+this.destpath+"*");
			proc.waitFor();
		}
		dir.mkdirs();
		String infname = path+fname;
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(infname));
		TreeSet<String> suspicious = new TreeSet<String>();
		this.recs =(ArrayList<DiaryRecord>)ois.readObject();
		this.suspicious = (TreeSet<String>)ois.readObject();
		ois.close();
		// read stop list
		infname = path+stopf;
		this.stoplist = new TreeSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(infname));
		String line="";
		while ((line=br.readLine()) != null)
			this.stoplist.add(line.trim());
		br.close();
	}
	
	public Calendar getBoundaryDate(Calendar base, int range) {
		Calendar limit=(Calendar)base.clone();
		switch (range) {
			case BY_WEEK:
				limit.roll(Calendar.DATE, 7);
				break;
			case BY_MONTH:
				limit.roll(Calendar.MONTH, 1);
				limit.set(Calendar.DATE, 1);
				break;
		}
		limit.set(Calendar.HOUR_OF_DAY, 0);
		limit.set(Calendar.MINUTE, 0);
		limit.set(Calendar.SECOND, 0);
		limit.set(Calendar.MILLISECOND, 0);
		return limit;
	}
	
	public boolean beforeBoundaryDate(Calendar cal, Calendar limit) {
		return cal.before(limit);
	}
	
	public boolean isNumeric(String s) {
		try {
			double d = Double.parseDouble(s);
			return true;
		} catch (Exception e) {}
		return false;
	}
	
	public String postProcess(String s) {
		// left: "��", should convert to normal term manually
		String str = s.toLowerCase();
		//if (str.indexOf("a.m.")>=0) System.err.println(str);
		StringTokenizer st = new StringTokenizer(str, " �����");
		String result="";
		String tok="";
		String prevTok="";
		while (st.hasMoreTokens()) {
			prevTok = tok;
			tok = st.nextToken().trim();
			
			// now check our stop list
			if (this.stoplist.contains(tok)) continue;
			
			// case: single char
			if (tok.length()==1 && !Character.isDigit(tok.charAt(0))) tok=" ";
			// case: [blank]
			else if (tok.equals("[blank]")) continue;
			// case: 6 A.M., find "A.M." or "P.M." and concat with last token
			else if (tok.equals("a.m.") || tok.equals("p.m.")) {
				if (prevTok.length()>0 && Character.isDigit(prevTok.charAt(prevTok.length()-1))) {
					result = result.trim();
					//System.err.println("["+prevTok+"]");
				}
			}
			// check number with "��"(temp)
			else if (tok.indexOf("��") >= 0) {
				int index=tok.indexOf("��");
				tok = tok.substring(0, index-1);
				if (isNumeric(tok)) {
					tok="";
				}
			}
			// check number with "-"(baro)
			else if (tok.indexOf("-")>0) {
				StringTokenizer st2 = new StringTokenizer(tok, "-");
				boolean found=true;
				while (st2.hasMoreTokens()) {
					String tok2 = st2.nextToken();
					if (!isNumeric(tok2)) { found=false; break; }
				}
				if (found) {
					//System.err.println("found baro pattern: "+tok);
					tok = "";
				}
			}
			// check number with ":"(bible section)
			else if (tok.indexOf(":")>0) {
				StringTokenizer st2 = new StringTokenizer(tok, ":");
				boolean found=true;
				while (st2.hasMoreTokens()) {
					String tok2 = st2.nextToken();
					if (!isNumeric(tok2)) { found=false; break; }
				}
				if (found) {
					//System.err.println("found bible section pattern: "+tok);
					tok = "";
				}
			}
			// case: possessive form
			else if (tok.endsWith("'s")) tok=tok.substring(0, tok.length()-2);
			// case: possessive form & other marks
			else if (tok.endsWith("'") || tok.endsWith(",") || tok.endsWith(".") || tok.endsWith("!")) tok=tok.substring(0, tok.length()-1);
			
			result += tok+" ";
		}
		return result.trim();
	}
	
	public String dump2Corpus(ArrayList<DiaryRecord> set, Calendar limit) {
		String line="";
		for (DiaryRecord rec: set) {
			String str=rec.getText();
			str = postProcess(str);
			line += str+"\n";
		}
		String fname="";
		if (limit == null) {
			fname = this.destpath+System.currentTimeMillis()+".txt";
		}
		else {
			fname = limit.get(Calendar.YEAR)+"-"+(limit.get(Calendar.MONTH)+1)+"-"+limit.get(Calendar.DATE)+".txt";
			fname = this.destpath+fname;
		}
		//System.err.println("Generating corpus file: "+fname);
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname), "utf-8"));
			bw.write(line);
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
		return fname;
	}
	
	public void doSingleDayDump() {
		int count=0;
		//Calendar limit=this.recs.get(0).getTimestamp();
		ArrayList<DiaryRecord> set = new ArrayList<DiaryRecord>();
		for (int i=0; i<this.recs.size(); i++) {
			DiaryRecord rec=this.recs.get(i);
			set.add(rec);
			dump2Corpus(set, null);
			count++;
		}
		System.err.println("Total "+count+" corpus files dumped.");
	}
	
	public void go() {
		if (this.range == BY_DATE) {
			doSingleDayDump();
			return;
		}
		Calendar base=this.recs.get(0).getTimestamp();
		Calendar limit=getBoundaryDate(base, this.range);
		ArrayList<DiaryRecord> set = new ArrayList<DiaryRecord>();
		int count=0;
		for (int i=0; i<this.recs.size(); i++) {
			DiaryRecord rec=this.recs.get(i);
			Calendar cal=rec.getTimestamp();
			if (beforeBoundaryDate(cal, limit)) set.add(rec);
			else { // boundary reached
				// process previous set
				if (set.size() > 0) { 
					String fname = dump2Corpus(set, limit);
					count++;
					System.err.println("Generating corpus file("+count+"): "+fname);
				}
				base = cal;
				limit = getBoundaryDate(base, this.range);
				set = new ArrayList<DiaryRecord>(); 
			}
		}
		if (set.size() > 0) { // process final set
			String fname = dump2Corpus(set, limit);
			count++;
			System.err.println("Generating corpus file("+count+"): "+fname);
		}
		System.err.println("Total "+count+" corpus files dumped.");
	}

}
