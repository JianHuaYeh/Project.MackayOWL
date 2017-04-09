package edu.saic.mackay.ref;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class LocationTopicRelations {
	private String fname1;
	private String fname2;
	private HashSet<String> locations;
	private ArrayList<String> topics;
	private HashMap<String, TreeSet<String>> occurrence;

	public static void main(String[] args) throws Exception {
		String fname1 = "possible.location.txt";
		String fname2 = "topics.log";
		LocationTopicRelations rel = new LocationTopicRelations(fname1, fname2);
		rel.go();
	}
	
	public LocationTopicRelations(String s1, String s2) {
		this.fname1 = s1;
		this.fname2 = s2;
	}
	
	public void go() throws Exception {
		this.locations = loadLocations(this.fname1);
		this.topics = loadTopics(this.fname2);
		this.occurrence = new HashMap<String, TreeSet<String>>();
		for (String loc: locations) {
			for (int i=0; i<topics.size(); i++) {
				String toc = topics.get(i);
				if (contains(toc, loc)) {
					createRelation("Topic"+i, loc);
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
	
	public void outputRelation() {
		//System.err.println("HasRelation: ["+toc+"]-["+loc+"]");
		for (String loc: this.occurrence.keySet()) {
			TreeSet<String> occList = this.occurrence.get(loc);
			System.out.println("<mackay:Location rdf:ID=\""+loc+"\">");
			for (String toc: occList) {
				System.out.println("    <Occurrence rdf:resource=\""+toc+"\" />");
			}
			System.out.println("</mackay:Location>");
			System.out.println();
		}
	}
	
	public boolean contains(String toc, String loc) {
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
		//System.err.println("Total "+result.size()+" locations found.");
		return result;
	}
	
	public ArrayList<String> loadTopics(String s) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
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
		//System.err.println("Total "+result.size()+" topics found.");
		return result;
	}
}
