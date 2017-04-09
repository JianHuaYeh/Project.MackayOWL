/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.saic.mackay.ref;

import java.io.*;
import java.util.*;

/**
 *
 * @author jhyeh
 */
public class GenerateOWL {
    private String fname;
    private String type; // 0:person, 1:mission, 2:subject
    private Hashtable hash;
    
    public GenerateOWL(String s, String t) {
        this.fname = s;
        this.type = t;
        hash = new Hashtable();
    }
    
    public static void main(String[] args) {
        GenerateOWL gowl = new GenerateOWL(args[0], args[1]);
        gowl.doGenerate();
    }
    
    private String[] _getPD(String str, String default_vol) {
        // example: "FIN:63:550", "65:153", "66"
        String[] result = new String[2];
        String[] tokens = str.split(":");
        if (tokens.length > 0) {
            String val = tokens[0];
            if (val.length() < 1) return null;
            if (val.charAt(0) >= 'A' && val.charAt(0) <= 'Z') { // "FIN:63:550"
                // skip
                if (tokens.length == 3) {
                    val = tokens[1];
                    if (val.length() < 1) return null;
                    if (!(val.charAt(0) >= 'A' && val.charAt(0) <= 'Z')) {
                        result[0] = tokens[1];
                        result[1] = tokens[2];
                        return result;
                    }
                    else return null; // "FIN:FIN:550"?? should not happen
                }
                else return null;
            }
            else {
                switch (tokens.length) {
                    case 1: result[0] = default_vol; result[1] = tokens[0]; break;
                    case 2: result[0] = tokens[0]; result[1] = tokens[1]; break;
                    default: return null;
                }
                return result;
            }
        }
        else return null;
        
    }
    
    private ArrayList getPD(String str) {
        // example: "See AAMS", "FIN:63:550", "63:550", "Anhwei,Wuhu,65:153,66:127"
        //          "FIN:49:111,113,57:375,58:258,63:550"
        //          "LOC:65-154,155,158"
        // extract vol 31~40 only
        // rule: tokenize by "," and drop all text part
        //       if start by number, try to tokenize by ":"
        //         if success, we get vol # and page #
        //         if fail on only one number(page #), use previous vol #
        ArrayList result = new ArrayList();
        String[] tokens = str.split(",");
        String vol = "";
        for (int i=0; i<tokens.length; i++) {
            String[] parts = _getPD(tokens[i], vol);
            if (parts != null) {
                //if (parts[0].equals("") && !vol.equals("")) parts[0] = vol;
                if (parts[0].equals("")) parts[0] = vol;
                else vol = parts[0];
                result.add(parts);
            }
        }
        return result;
    }
    
    private String[] getPD2(String str) {
        // example: "See AAMS", "FIN:63:550", "63:550", "Anhwei,Wuhu,65:153,66:127"
        // "LOC:65-154,155,158"
        // extract vol 31~40 only
        String result[] = new String[2];
        result[1] = "PD-";
        StringTokenizer st = new StringTokenizer(str, ":");
        if (st.hasMoreTokens()) {
            String val = st.nextToken();
            try {
                if (val.startsWith("See")) {
                    result[0] = "See";
                    result[1] = val.substring("See".length()+1).trim();
                    return result;
                }
                else if (val.charAt(0) >= 'A' && val.charAt(0) <= 'Z') { // not CR vol #
                    result[0] = val;
                    val = st.nextToken();
                    if (val == null) {
                        result[1] = ""; return result;
                    }
                    if (val.charAt(0) >= 'A' && val.charAt(0) <= 'Z') { // not normal case
                        return null;
                    }
                    result[1] += val+"-"+st.nextToken();
                }
                else { // normal case
                    //val = val+"-"+st.nextToken();
                    result[0] = "";
                    result[1] += val+"-"+st.nextToken();
                }
            } catch (Exception e) {}
        }
        return result;
    }

    private void createRecord(String line) {
        // parse line here
        // example: "American Free Methodiat Mission (AFMM), FIN:63:550; PER:42:310."
        String name="";
        int idx = -1;
        idx = line.indexOf(",");
        String rest="";
        if (idx > 0) {
            name = line.substring(0, idx);
            if (name.indexOf(":") >= 0)
                return; // not valid record
            if (name.charAt(0) >= '0' && name.charAt(0) <= '9')
                return; // not valid record
            rest = line.substring(idx+1).trim();
        }
        StringTokenizer st = new StringTokenizer(rest, ";.");
        //String[] occur;
        ArrayList occur = new ArrayList();
        ArrayList al = new ArrayList();
        while (st.hasMoreTokens()) {
            occur = getPD(st.nextToken().trim());
            if ((occur != null) && (occur.size() > 0)) {
                al.addAll(occur);
                //if (!"See".equals(occur[0])) {
                //    al.add(occur[1]);
                    //System.out.println(occur[1]);
                //}
            }
        }
        hash.put(name, al);
    }
    
    private boolean _inFilter(String volstr) {
        try {
            int vol = Integer.parseInt(volstr);
            if ((vol >= 31) && (vol <=40)) return true;
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void outputRecordHash() {
        for (Enumeration en=hash.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();
            ArrayList al = (ArrayList)hash.get(key);
            int count = 0;
            for (Iterator it=al.iterator(); it.hasNext(); ) {
                //String val = (String)it.next();
                //val.replace(":", "-");
                String[] tmp = (String[])it.next();
                if (_inFilter(tmp[0])) {
                    count++;
                }
            }
            if (count == 0) continue;
            String result = "\n  <"+type+" rdf:ID=\""+key+"\">";
            for (Iterator it=al.iterator(); it.hasNext(); ) {
                //String val = (String)it.next();
                //val.replace(":", "-");
                String[] tmp = (String[])it.next();
                if (_inFilter(tmp[0])) {
                    result += "\n    <Occurrence rdf:resource=\"PD-"+tmp[0]+"-"+tmp[1]+"\" />";
                }
            }
            result += "\n  </"+type+">";
            System.out.println(result);
        }
    }
    
    public void doGenerate() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fname));
            String line = "";
            while ((line=br.readLine()) != null) {
                //System.out.println(line);
                createRecord(line);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("OWL generation error: "+e);
        }
        outputRecordHash();
    }
}
