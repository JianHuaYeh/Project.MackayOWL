package edu.saic.mackay;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.*;

public class TopicClusteringLocationContain {
    private HashMap<String, double[]> data;
    private HashMap<String, String> content;
    private String infname;

    public static void main(String[] args) throws Exception {
        //HClustering hc = new HClustering(args[0]);
    	String fname = "topics.log";
    	fname = "topics.log";
		String fname1 = "possible.location.txt";

    	TopicClusteringLocationContain hc = new TopicClusteringLocationContain(fname, fname1);
        hc.doClustering(0);
    }

    public TopicClusteringLocationContain(String s, String s1) {
    	this.infname = s;
        this.data = loadData(s);
        if (this.data == null) System.exit(0);
        this.fname1 = s1;
    }

    public HashMap<String, double[]> loadData(String fname) {
        HashMap<String, double[]> result = new HashMap<String, double[]>();
        this.content = new HashMap<String, String>();
        try {
        	// enumerate all used words first
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fname), "utf-8"));
            String line="";
            int count=0;
            TreeSet<String> wordset = new TreeSet<String>();
            while ((line=br.readLine()) != null) {
            	// data format: "Topic 0: students board college spoke wet fukien hills making savages chapels"
            	StringTokenizer st = new StringTokenizer(line);
            	st.nextToken(); // Topic
            	st.nextToken(); // 0:
            	while (st.hasMoreTokens()) wordset.add(st.nextToken());
            }
            br.close();
            ArrayList<String> wordlist = new ArrayList<String>(wordset);
            int size=wordlist.size();
            //System.err.println("Topic vector length: "+size);
            // now make topic vector
            br = new BufferedReader(new FileReader(fname));
            line="";
            count=0;
            while ((line=br.readLine()) != null) {
            	StringTokenizer st = new StringTokenizer(line);
            	String s1 = st.nextToken(); // Topic
            	String s2 = st.nextToken(); // 0:
            	s2 = s2.substring(0, s2.length()-1);
            	String key=s1+s2;
            	String val=line.substring(line.indexOf(":")+1).trim();
            	double[] vec = new double[size];
            	while (st.hasMoreTokens()) {
            		String tok = st.nextToken();
            		int index = wordlist.indexOf(tok);
            		if (index >= 0) vec[index]++;
            	}
            	result.put(key, vec);
            	//this.content.put(key, line);
            	this.content.put(key, val);
            	count++;
            }
            br.close();
            //System.out.println("Total "+count+" topic records.");
            return result;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private ArrayList<BiCluster> makeInitialClusters() {
        ArrayList<BiCluster> result = new ArrayList<BiCluster>();
        for (Iterator<String> it=this.data.keySet().iterator(); it.hasNext(); ) {
            String blogname = it.next();
            double[] freqs = (double[])this.data.get(blogname);
            //String str = this.content.get(blogname);
            //BiCluster cl = new BiCluster(str, freqs);
            BiCluster cl = new BiCluster(blogname, freqs);
            result.add(cl);
        }
        return result;
    }

    private double distance(BiCluster c0, BiCluster c1, int method) {
        double[] vec0 = c0.getVec();
        double[] vec1 = c1.getVec();
        switch (method) {
            case 0: return 1.0/eucledianSimilarity(vec0, vec1);
            case 1: return 1.0/pearsonSimilarity(vec0, vec1);
            case 2:
            default: return 1.0/cosineSimilarity(vec0, vec1);
        }
    }

    private double[] mergeVec(double[] vec0, double[] vec1) {
        double[] result = new double[vec0.length];
        for (int i=0; i<vec0.length; i++) {
            result[i] = (vec0[i]+vec1[i])/2.0;
        }
        return result;
    }
    
    public void doClustering(int method) throws Exception {
        ArrayList<BiCluster> clust = makeInitialClusters();
        int topicCount = clust.size();

        BiCluster c0 = clust.get(0);
        BiCluster c1 = clust.get(1);
        double closest = distance(c0, c1, method);
        while (clust.size() > 1) {
            int[] lowestpair = new int[2];
            lowestpair[0] = 0; lowestpair[1] = 1;
            //System.out.println("Cluster size = "+clust.size());
            for (int i=0; i<clust.size(); i++) {
                for (int j=i+1; j<clust.size(); j++) {
                    c0 = (BiCluster)clust.get(i);
                    c1 = (BiCluster)clust.get(j);
                    double dist = distance(c0, c1, method);
                    if (dist < closest) {
                        closest = dist;
                        lowestpair[0] = i;
                        lowestpair[1] = j;
                    }
                } // for j
            } // for i

            //System.out.println("Lowest pair: "+lowestpair[0]+", "+lowestpair[1]);
            c0 = clust.get(lowestpair[0]);
            c1 = clust.get(lowestpair[1]);
            //String newid = "{"+c0.getId()+","+c1.getId()+"}";
            String newid = "Topic"+topicCount;
            topicCount++;
            double[] mergeVec = mergeVec(c0.getVec(), c1.getVec());
            BiCluster newcluster = new BiCluster(newid, mergeVec, c0, c1);
            clust.remove(c1);
            clust.remove(c0);
            clust.add(newcluster);
        } // while

        // only one cluster left
        BiCluster result = clust.get(0);
        //System.out.println(result.getId());

        // make JTree
        DefaultMutableTreeNode root = makeTree(result);
        JTree tree = new JTree(root);
        JScrollPane pane = new JScrollPane(tree);
        JFrame frame = new JFrame();
        frame.add(pane);
        frame.setSize(800, 600);
        frame.setVisible(true);
        
        //printTree(root, 0);
        
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.infname+".owl"), "utf-8"));
        printOwnershipXML(root, bw);
        printBaseTopicXML(this.infname, bw);
        bw.close();
    }
    
	private String fname1;
	private HashSet<String> locations;
	private ArrayList<String> topics;
	private HashMap<String, TreeSet<String>> occurrence;
    
    private void printBaseTopicXML(String fname, BufferedWriter bw) {
    	try {
    		// link topic with locations
    		this.locations = loadLocations(this.fname1);
    		this.topics = loadTopics(this.infname);
    		this.occurrence = new HashMap<String, TreeSet<String>>();
    		for (String loc: locations) {
    			for (int i=0; i<topics.size(); i++) {
    				String toc = topics.get(i);
    				if (contains(toc, loc)) {
    					createRelation("Topic"+i, loc);
    				}
    			}
    		}
        	// enumerate all used words first
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fname), "utf-8"));
            String line="";
            int count=0;
            ArrayList<String> wordlist = new ArrayList<String>();
            while ((line=br.readLine()) != null) {
            	// data format: "Topic 0: students board college spoke wet fukien hills making savages chapels"
            	StringTokenizer st = new StringTokenizer(line);
            	String s1=st.nextToken(); // Topic
            	String s2=st.nextToken(); // 0:
            	int index=line.indexOf(":");
            	String s3="N/A";
            	if (index>0) s3=line.substring(index+1).trim();
            	
                // to render this
//              <mackay:Topic rdf:ID="Topic1">
//              <rdfs:label>years when When modern opening peace world power military ma</rdfs:label>
//              </mackay:Topic>
            	String topicstr = s1+s2.substring(0, s2.length()-1);
            	bw.write("  <mackay:Topic rdf:ID=\""+topicstr+"\">"); bw.newLine();
            	bw.write("    <rdfs:label>"+s3+"</rdfs:label>"); bw.newLine();
            	TreeSet<String> occList = this.occurrence.get(topicstr);
            	if (occList != null) {
            		for (String loc: occList) {
            			bw.write("    <Contains rdf:resource=\""+loc+"\" />"); bw.newLine();
        			}
            	}
            	bw.write("  </mackay:Topic>"); bw.newLine();
            	bw.newLine();
            }
            br.close();
            
    	} catch (Exception e) {
    		e.printStackTrace(System.err);
    	}
    }
    
    public void createRelation(String toc, String loc) {
		if (this.occurrence.get(toc) == null)
			this.occurrence.put(toc, new TreeSet<String>());
		TreeSet<String> occList = this.occurrence.get(toc);
		occList.add(loc);
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
    
    private void printOwnership(DefaultMutableTreeNode node) {
    	String result = node.toString()+" = ";
    	for (int i=0; i<node.getChildCount(); i++)
    		result += node.getChildAt(i).toString()+" $ ";
    	System.out.println(result);
    	
    	for (int i=0; i<node.getChildCount(); i++)
    		printOwnership((DefaultMutableTreeNode)node.getChildAt(i));
    }
    
    private void printOwnershipXML(DefaultMutableTreeNode node, BufferedWriter bw) throws Exception {
    	
    	if (node.getChildCount() == 0) return;
    	
    	String xmlstr = "  <mackay:Topic rdf:ID=\""+node.toString()+"\">\n";
    	for (int i=0; i<node.getChildCount(); i++)
    		xmlstr += "    <Contains rdf:resource=\""+node.getChildAt(i).toString()+"\" />\n";
    	xmlstr += "  </mackay:Topic>\n";
    	bw.write(xmlstr); bw.newLine();
    	
    	for (int i=0; i<node.getChildCount(); i++)
    		printOwnershipXML((DefaultMutableTreeNode)node.getChildAt(i), bw);
    }
    
    private void printTree(DefaultMutableTreeNode node, int level) {
    	for (int i=0; i<level; i++) System.out.print("\t");
    	System.out.println(node);
    	for (int i=0; i<node.getChildCount(); i++)
    		printTree((DefaultMutableTreeNode)node.getChildAt(i), level+1);
    }

    private DefaultMutableTreeNode makeTree(BiCluster bc) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(bc);
        BiCluster left = bc.getLeftNode();
        BiCluster right = bc.getRightNode();
        if (left==null && right==null) {
        	String str = this.content.get(bc.getId());
        	bc.setId(str);
        	return node;
        }
        DefaultMutableTreeNode lnode = makeTree(left);
        DefaultMutableTreeNode rnode = makeTree(right);
        node.add(lnode);
        node.add(rnode);
        return node;
    }

    private double eucledianSimilarity(double[] vec0, double[] vec1) {
        double sum = 0.0;
        for (int i=0; i<vec0.length; i++) {
            double score = vec0[i];
            double score2 = vec1[i];
            sum += (score-score2)*(score-score2);
        }
        double d = Math.sqrt(sum);
        return 1.0/(d+1);
    }

    private double pearsonSimilarity(double[] vec0, double[] vec1) {
        double sum1=0.0, sum2=0.0;
        double sum1sq=0.0, sum2sq=0.0;
        double psum=0.0;
        int n = vec0.length;
        for (int i=0; i<n; i++) {
            double r1 = vec0[i];
            double r2 = vec1[i];
            sum1 += r1;
            sum2 += r2;
            sum1sq += r1*r1;
            sum2sq += r2*r2;
            psum += r1*r2;
        }
        double num = psum-(sum1*sum2)/n;
        double den=Math.sqrt((sum1sq-sum1*sum1/n)*(sum2sq-sum2*sum2/n));
        if (den == 0.0) return 0.0;
        return num/den;
    }

    private double cosineSimilarity(double[] vec0, double[] vec1) {
        double sum1sq=0.0, sum2sq=0.0;
        double psum=0.0;
        int n = vec0.length;
        for (int i=0; i<n; i++) {
            double r1 = vec0[i];
            double r2 = vec1[i];
            sum1sq += r1*r1;
            sum2sq += r2*r2;
            psum += r1*r2;
        }
        double den=Math.sqrt(sum1sq)*Math.sqrt(sum2sq);
        if (den == 0.0) return 0.0;
        return psum/den;
    }

}
