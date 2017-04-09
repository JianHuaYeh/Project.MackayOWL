package edu.saic.mackay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class LocationDiaryRelations {
	private String fname1;
	private String fname2;
	private HashSet<String> locations;
	private HashMap<String, DiaryRecord> diaries;
	private HashMap<String, TreeSet<String>> occurrence;

	public static void main(String[] args) throws Exception {
		String fname1 = "possible.location.txt";
		String fname2 = "diary.txt.obj";
		LocationDiaryRelations rel = new LocationDiaryRelations(fname1, fname2);
		rel.go();
	}
	
	public LocationDiaryRelations(String s1, String s2) {
		this.fname1 = s1;
		this.fname2 = s2;
	}
	
	public void go() throws Exception {
		this.locations = loadLocations(this.fname1);
		//HashMap<String, String> diaries
		this.diaries = loadDiary(this.fname2);
		this.occurrence = new HashMap<String, TreeSet<String>>();
		for (String loc: locations) {
			//System.err.println(loc);
			for (String key: diaries.keySet()) {
				DiaryRecord rec = diaries.get(key);
				String txt = (""+rec.getLocation()).toLowerCase();
				if (contains(txt, loc)) {
					createRelation(key, loc);
				}
			}
		}
		outputRelation();
	}
	
	public void createRelation(String toc, String loc) {
		if (this.occurrence.get(loc) == null)
			this.occurrence.put(loc, new TreeSet<String>());
		TreeSet<String> occList = this.occurrence.get(loc);
		occList.add(toc);
	}
	
	public void outputRelation() throws Exception {
		//System.err.println("HasRelation: ["+toc+"]-["+loc+"]");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.fname1+".owl"), "utf-8"));
		for (String loc: this.occurrence.keySet()) {
			TreeSet<String> occList = this.occurrence.get(loc);
			bw.write("  <mackay:Location rdf:ID=\""+loc+"\">"); bw.newLine();
			for (String toc: occList) {
				bw.write("    <Occurrence rdf:resource=\""+toc+"\" />"); bw.newLine();
			}
			bw.write("  </mackay:Location>"); bw.newLine();
			bw.newLine();
		}
        bw.close();
	}
	
	public boolean contains(String toc, String loc) {
		//System.err.println(toc+" ========== "+loc);
		return toc.indexOf(loc)>=0?true:false;
	}
	
	public HashSet<String> loadLocations(String s) throws Exception {
		//System.err.println("Opening: "+s);
		HashSet<String> result = new HashSet<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(s), "utf-8"));
		String line = "";
		while ((line=br.readLine()) != null) {
			// sample: "Possible location @ [1871-11-24]: Japan"
			int index = line.indexOf(":");
			if (index >= 0) {
				String loc = line.substring(index+1).trim();
				result.add(loc.toLowerCase());
			}
		}
		br.close();
		//System.err.println("Total "+result.size()+" locations loaded.");
		return result;
	}
	
	public HashMap<String, DiaryRecord> loadDiary(String s) throws Exception {
		HashMap<String, DiaryRecord> result = new HashMap<String, DiaryRecord>();
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(s));
		ArrayList<DiaryRecord> recs = (ArrayList<DiaryRecord>)ois.readObject();
		TreeSet<String> suspecious = (TreeSet<String>)ois.readObject();
		ois.close();
		//System.err.println("Total "+recs.size()+" diary documents loaded.");
		for (DiaryRecord rec: recs) {
			result.put("DiaryDocument-"+rec.getDateStr(), rec);
		}
		return result;
	}
}
