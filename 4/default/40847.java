import java.io.*;
import java.sql.*;
import java.util.*;

public class KB_MESH {

    private excellantTools tools;

    private excellantSettings settings;

    private String MESHdbname;

    private String MESHtreetablename;

    private String MESHtreefilename;

    private String MESHdescriptorfilename;

    private String MESHgraphtableprefix;

    private String MESHgraphtableUID_NAME;

    private String MESHgraphtableTREENR_UID;

    private String MESHgraphtableUID_UID;

    private String MESHgraphdir;

    private boolean stopTreeImport;

    private int countTreeImport;

    private String listPrefix;

    private class DBrow {

        private int index;

        private String item_name;

        private String item_annotation;

        DBrow() {
            index = 0;
            item_name = "";
            item_annotation = "";
        }

        DBrow(int i, String name, String annotation) {
            index = i;
            item_name = name;
            item_annotation = annotation;
        }

        public String toString() {
            return item_name;
        }

        public int getItemIndex() {
            return index;
        }

        public String getItemName() {
            return item_name;
        }

        public String getItemAnnotation() {
            return item_annotation;
        }
    }

    /** Creates a new instance of KB_MeSH */
    public KB_MESH(excellantSettings sets) {
        settings = sets;
        tools = new excellantTools(settings);
        stopTreeImport = false;
        listPrefix = "mesh_tree_";
        MESHdbname = settings.getKBDBname();
        MESHtreetablename = "mesh_tree";
        MESHtreefilename = settings.getDataDir() + "MeSH\\mtrees2003.bin";
        MESHdescriptorfilename = settings.getDataDir() + "MeSH\\desc2003.xml";
        MESHgraphtableprefix = "mesh_graph_";
        MESHgraphtableUID_NAME = MESHgraphtableprefix + "uid_name";
        MESHgraphtableTREENR_UID = MESHgraphtableprefix + "treenr_uid";
        MESHgraphtableUID_UID = MESHgraphtableprefix + "uid_uid";
        MESHgraphdir = settings.getGraphDir();
    }

    public String getMESHdbname() {
        return MESHdbname;
    }

    public String getMESHtreetablename() {
        return MESHtreetablename;
    }

    public void setMESHtreefilename(String s) {
        MESHtreefilename = s;
    }

    public String getMESHtreefilename() {
        return MESHtreefilename;
    }

    public String getMESHdescriptorfilename() {
        return MESHdescriptorfilename;
    }

    public String getMESHgraphtableUID_NAME() {
        return MESHgraphtableUID_NAME;
    }

    public String getMESHgraphtableTREENR_UID() {
        return MESHgraphtableTREENR_UID;
    }

    public String getMESHgraphtableUID_UID() {
        return MESHgraphtableUID_UID;
    }

    public void setMESHdescriptorfilename(String s) {
        MESHdescriptorfilename = s;
    }

    public String getListPrefix() {
        return listPrefix;
    }

    public String getMESHgraphdir() {
        return MESHgraphdir;
    }

    public void setMESHgraphdir(String s) {
        MESHgraphdir = s;
    }

    public void setStopTreeImport(boolean b) {
        stopTreeImport = b;
    }

    public boolean getStopTreeImport() {
        return stopTreeImport;
    }

    public int getCountTreeImport() {
        return countTreeImport;
    }

    /*******************/
    private void createMESHtreetable(Connection db) {
        String command;
        Set tables = tools.getTableNames2Set(db, "%");
        if (tables.contains(getMESHtreetablename())) tools.deleteTable(db, getMESHtreetablename());
        command = "CREATE TABLE " + getMESHtreetablename() + " ( ";
        command = command + "index INT4, term TEXT, ";
        command = command + "hierarchy_key CHAR(3), hierarchy_prev CHAR(3), hierarchy_complete TEXT, ";
        command = command + "PRIMARY KEY (index) )";
        tools.execute(db, command);
    }

    /*******************/
    public void readMESHTreeFileIntoDB() {
        String inputLine;
        String filename = getMESHtreefilename();
        try {
            Connection db = tools.openDB(getMESHdbname());
            createMESHtreetable(db);
            BufferedReader in = new BufferedReader(new FileReader(filename));
            countTreeImport = 0;
            while (((inputLine = in.readLine()) != null) && (!stopTreeImport)) {
                String term = inputLine.substring(0, inputLine.indexOf(";"));
                if (term.indexOf("'") > -1) {
                    term = term.replace('\'', '$');
                }
                String hier_complete = inputLine.substring(inputLine.indexOf(";") + 1);
                String hier_prev, hier_key;
                if (hier_complete.length() == 3) {
                    hier_prev = "-";
                    hier_key = hier_complete;
                } else {
                    int l = hier_complete.length();
                    hier_prev = hier_complete.substring(l - 7, l - 4);
                    hier_key = hier_complete.substring(l - 3, l);
                }
                String ins, conv;
                conv = "";
                ins = conv.valueOf(countTreeImport) + ",'" + term + "','" + hier_key;
                ins = ins + "','" + hier_prev + "','" + hier_complete + "'";
                tools.insertIntoDB(db, getMESHtreetablename(), ins);
                countTreeImport++;
                if ((countTreeImport % 100) == 0) System.out.println(countTreeImport);
            }
            in.close();
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog("Error while reading MESH tree file: " + e.getMessage());
        }
    }

    /*******************/
    private void createMESHtreegraphtables(Connection db) {
        String command;
        Set tables = tools.getTableNames2Set(db, "%");
        if (tables.contains(getMESHgraphtableUID_NAME())) tools.deleteTable(db, getMESHgraphtableUID_NAME());
        if (tables.contains(getMESHgraphtableUID_UID())) tools.deleteTable(db, getMESHgraphtableUID_UID());
        if (tables.contains(getMESHgraphtableTREENR_UID())) tools.deleteTable(db, getMESHgraphtableTREENR_UID());
        command = "CREATE TABLE " + getMESHgraphtableUID_NAME() + " ( ";
        command = command + "index INT4, uid TEXT, name TEXT, ";
        command = command + "PRIMARY KEY (index) )";
        tools.execute(db, command);
        command = "CREATE TABLE " + getMESHgraphtableUID_UID() + " ( ";
        command = command + "index INT4, uid1 TEXT, uid2 TEXT, ";
        command = command + "PRIMARY KEY (index) )";
        tools.execute(db, command);
        command = "CREATE TABLE " + getMESHgraphtableTREENR_UID() + " ( ";
        command = command + "index INT4, treenr TEXT, uid TEXT, ";
        command = command + "PRIMARY KEY (index) )";
        tools.execute(db, command);
    }

    /*******************/
    public void readMESHDescriptorFileIntoDB() {
        String inputLine, ins;
        String filename = getMESHdescriptorfilename();
        String uid, name;
        Vector treenr = new Vector();
        Vector related = new Vector();
        int start, end;
        int countUID, countTreenr, countRelated;
        try {
            Connection db = tools.openDB(getMESHdbname());
            createMESHtreegraphtables(db);
            BufferedReader in = new BufferedReader(new FileReader(filename));
            countUID = 0;
            countTreenr = 0;
            countRelated = 0;
            tools.printDate();
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.indexOf("<DescriptorRecord DescriptorClass") != -1) {
                    treenr.clear();
                    related.clear();
                    inputLine = in.readLine();
                    start = inputLine.indexOf("<DescriptorUI>") + 14;
                    end = inputLine.indexOf("</DescriptorUI>");
                    uid = inputLine.substring(start, end);
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    start = inputLine.indexOf("<String>") + 8;
                    end = inputLine.indexOf("</String>");
                    name = inputLine.substring(start, end);
                    if (name.indexOf("'") != -1) name = name.replace('\'', '$');
                    inputLine = in.readLine();
                    while ((inputLine != null) && (inputLine.indexOf("<SeeRelatedList>") == -1) && (inputLine.indexOf("<TreeNumberList>") == -1)) inputLine = in.readLine();
                    while ((inputLine != null) && (inputLine.indexOf("</SeeRelatedList>") == -1) && (inputLine.indexOf("<TreeNumberList>") == -1)) {
                        if (inputLine.indexOf("<DescriptorUI>") != -1) {
                            start = inputLine.indexOf("<DescriptorUI>") + 14;
                            end = inputLine.indexOf("</DescriptorUI>");
                            String nr = inputLine.substring(start, end);
                            related.add(nr);
                        }
                        inputLine = in.readLine();
                    }
                    while ((inputLine != null) && (inputLine.indexOf("<TreeNumberList>") == -1)) inputLine = in.readLine();
                    inputLine = in.readLine();
                    while ((inputLine != null) && (inputLine.indexOf("</TreeNumberList>") == -1)) {
                        start = inputLine.indexOf("<TreeNumber>") + 12;
                        end = inputLine.indexOf("</TreeNumber>");
                        String nr = inputLine.substring(start, end);
                        treenr.add(nr);
                        inputLine = in.readLine();
                    }
                    ins = countUID + ", '" + uid + "', '" + name + "'";
                    tools.insertIntoDB(db, getMESHgraphtableUID_NAME(), ins);
                    for (int i = 0; i < related.size(); i++) {
                        ins = countRelated + ", '" + uid + "', '" + (String) related.get(i) + "'";
                        tools.insertIntoDB(db, getMESHgraphtableUID_UID(), ins);
                        countRelated++;
                    }
                    for (int i = 0; i < treenr.size(); i++) {
                        ins = countTreenr + ", '" + (String) treenr.get(i) + "', '" + uid + "'";
                        tools.insertIntoDB(db, getMESHgraphtableTREENR_UID(), ins);
                        countTreenr++;
                    }
                    countUID++;
                    if ((countUID % 500) == 0) System.out.println(countUID);
                }
            }
            System.out.println("End import descriptors: " + countUID);
            tools.printDate();
            in.close();
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog("Error while reading MESH descriptor file: " + e.getMessage());
        }
    }

    private String getNextLine(BufferedReader in) {
        try {
            String inputline = in.readLine();
            inputline = inputline.substring(inputline.indexOf("<"), inputline.length());
            return inputline;
        } catch (Exception e) {
        }
        return null;
    }

    private String getChemName(String name) {
        String ret = name;
        if (name.indexOf(", ") != -1) {
            StringTokenizer st = new StringTokenizer(name);
            if (st.countTokens() > 0) {
                String token = st.nextToken();
                String namenew = "";
                boolean stop = false;
                while (st.hasMoreTokens() && !stop) {
                    if (token.endsWith(",")) {
                        stop = true;
                        token = token.substring(0, token.length() - 1);
                        namenew = namenew + token;
                    } else namenew = namenew + token + " ";
                    token = st.nextToken();
                }
                ret = namenew;
            }
        }
        return ret;
    }

    public void readMESHDescriptorFileIntoFiles(String outfiledir) {
        String inputLine, ins;
        String filename = getMESHdescriptorfilename();
        String uid = "";
        String name = "";
        String description = "";
        String element_of = "";
        Vector treenr = new Vector();
        Vector related = new Vector();
        Vector synonyms = new Vector();
        Vector actions = new Vector();
        Vector chemicals = new Vector();
        Vector allCASchemicals = new Vector();
        Set CAS = new TreeSet();
        Map treenr2uid = new TreeMap();
        Map uid2name = new TreeMap();
        String cut1, cut2;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String outfile = outfiledir + "\\mesh";
            BufferedWriter out_concept = new BufferedWriter(new FileWriter(outfile + "_concept.txt"));
            BufferedWriter out_concept_name = new BufferedWriter(new FileWriter(outfile + "_concept_name.txt"));
            BufferedWriter out_relation = new BufferedWriter(new FileWriter(outfile + "_relation.txt"));
            BufferedWriter cas_mapping = new BufferedWriter(new FileWriter(outfile + "to_cas_mapping.txt"));
            BufferedWriter ec_mapping = new BufferedWriter(new FileWriter(outfile + "to_ec_mapping.txt"));
            Connection db = tools.openDB("kb");
            String query = "SELECT hierarchy_complete,uid FROM mesh_tree, mesh_graph_uid_name WHERE term=name";
            ResultSet rs = tools.executeQuery(db, query);
            while (rs.next()) {
                String db_treenr = rs.getString("hierarchy_complete");
                String db_uid = rs.getString("uid");
                treenr2uid.put(db_treenr, db_uid);
            }
            db.close();
            System.out.println("Reading in the DUIDs ...");
            BufferedReader in_for_mapping = new BufferedReader(new FileReader(filename));
            inputLine = getNextLine(in_for_mapping);
            boolean leave = false;
            while ((in_for_mapping != null) && (inputLine != null)) {
                if (inputLine.startsWith("<DescriptorRecord DescriptorClass")) {
                    inputLine = getNextLine(in_for_mapping);
                    cut1 = "<DescriptorUI>";
                    cut2 = "</DescriptorUI>";
                    String mesh_uid = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                    if (mesh_uid.compareTo("D041441") == 0) leave = true;
                    inputLine = getNextLine(in_for_mapping);
                    inputLine = getNextLine(in_for_mapping);
                    cut1 = "<String>";
                    cut2 = "</String>";
                    String mesh_name = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                    uid2name.put(mesh_uid, mesh_name);
                }
                inputLine = getNextLine(in_for_mapping);
            }
            in_for_mapping.close();
            BufferedReader in_ec_numbers = new BufferedReader(new FileReader("e:\\projects\\ondex\\ec_concept_acc.txt"));
            Set ec_numbers = new TreeSet();
            String ec_line = in_ec_numbers.readLine();
            while (in_ec_numbers.ready()) {
                StringTokenizer st = new StringTokenizer(ec_line);
                st.nextToken();
                ec_numbers.add(st.nextToken());
                ec_line = in_ec_numbers.readLine();
            }
            in_ec_numbers.close();
            tools.printDate();
            inputLine = getNextLine(in);
            while (inputLine != null) {
                if (inputLine.startsWith("<DescriptorRecord DescriptorClass")) {
                    treenr.clear();
                    related.clear();
                    synonyms.clear();
                    actions.clear();
                    chemicals.clear();
                    boolean id_ready = false;
                    boolean line_read = false;
                    while ((inputLine != null) && (!inputLine.startsWith("</DescriptorRecord>"))) {
                        line_read = false;
                        if ((inputLine.startsWith("<DescriptorUI>")) && (!id_ready)) {
                            cut1 = "<DescriptorUI>";
                            cut2 = "</DescriptorUI>";
                            uid = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                            inputLine = getNextLine(in);
                            inputLine = getNextLine(in);
                            cut1 = "<String>";
                            cut2 = "</String>";
                            name = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                            id_ready = true;
                        }
                        if (inputLine.compareTo("<SeeRelatedList>") == 0) {
                            while ((inputLine != null) && (inputLine.indexOf("</SeeRelatedList>") == -1)) {
                                if (inputLine.startsWith("<DescriptorUI>")) {
                                    cut1 = "<DescriptorUI>";
                                    cut2 = "</DescriptorUI>";
                                    String id = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    related.add(id);
                                }
                                inputLine = getNextLine(in);
                                line_read = true;
                            }
                        }
                        if (inputLine.compareTo("<TreeNumberList>") == 0) {
                            while ((inputLine != null) && (inputLine.indexOf("</TreeNumberList>") == -1)) {
                                if (inputLine.startsWith("<TreeNumber>")) {
                                    cut1 = "<TreeNumber>";
                                    cut2 = "</TreeNumber>";
                                    String id = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    treenr.add(id);
                                }
                                inputLine = getNextLine(in);
                                line_read = true;
                            }
                        }
                        if (inputLine.startsWith("<Concept PreferredConceptYN")) {
                            boolean prefConcept = false;
                            if (inputLine.compareTo("<Concept PreferredConceptYN=\"Y\">") == 0) prefConcept = true;
                            while ((inputLine != null) && (inputLine.indexOf("</Concept>") == -1)) {
                                if (inputLine.startsWith("<CASN1Name>") && prefConcept) {
                                    cut1 = "<CASN1Name>";
                                    cut2 = "</CASN1Name>";
                                    String casn1 = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    String chem_name = casn1;
                                    String chem_description = "";
                                    if (casn1.length() > chem_name.length() + 2) chem_description = casn1.substring(chem_name.length() + 2, casn1.length());
                                    String reg_number = "";
                                    inputLine = getNextLine(in);
                                    if (inputLine.startsWith("<RegistryNumber>")) {
                                        cut1 = "<RegistryNumber>";
                                        cut2 = "</RegistryNumber>";
                                        reg_number = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    }
                                    Vector chemical = new Vector();
                                    String type = "";
                                    if (reg_number.startsWith("EC")) {
                                        type = "EC";
                                        reg_number = reg_number.substring(3, reg_number.length());
                                    } else {
                                        type = "CAS";
                                    }
                                    chemical.add(type);
                                    chemical.add(reg_number);
                                    chemical.add(chem_name);
                                    chemical.add(chem_description);
                                    chemicals.add(chemical);
                                    if (type.compareTo("CAS") == 0) {
                                        if (!CAS.contains(reg_number)) {
                                            CAS.add(reg_number);
                                            allCASchemicals.add(chemical);
                                        }
                                    }
                                }
                                if (inputLine.startsWith("<ScopeNote>") && prefConcept) {
                                    cut1 = "<ScopeNote>";
                                    description = inputLine.substring(cut1.length(), inputLine.length());
                                }
                                if (inputLine.startsWith("<TermUI>")) {
                                    inputLine = getNextLine(in);
                                    cut1 = "<String>";
                                    cut2 = "</String>";
                                    String syn = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    if (syn.indexOf("&amp;") != -1) {
                                        String syn1 = syn.substring(0, syn.indexOf("&amp;"));
                                        String syn2 = syn.substring(syn.indexOf("amp;") + 4, syn.length());
                                        syn = syn1 + " & " + syn2;
                                    }
                                    if (name.compareTo(syn) != 0) synonyms.add(syn);
                                }
                                if (inputLine.startsWith("<PharmacologicalAction>")) {
                                    inputLine = getNextLine(in);
                                    inputLine = getNextLine(in);
                                    cut1 = "<DescriptorUI>";
                                    cut2 = "</DescriptorUI>";
                                    String act_ui = inputLine.substring(cut1.length(), inputLine.indexOf(cut2));
                                    actions.add(act_ui);
                                }
                                inputLine = getNextLine(in);
                                line_read = true;
                            }
                        }
                        if (!line_read) inputLine = getNextLine(in);
                    }
                    String pos_tag = "";
                    element_of = "MESHD";
                    String is_primary = "0";
                    out_concept.write(uid + "\t" + pos_tag + "\t" + description + "\t" + element_of + "\t");
                    out_concept.write(is_primary + "\n");
                    String name_stemmed = "";
                    String name_tagged = "";
                    element_of = "MESHD";
                    String is_unique = "0";
                    int is_preferred = 1;
                    String original_name = name;
                    String is_not_substring = "0";
                    out_concept_name.write(uid + "\t" + name + "\t" + name_stemmed + "\t");
                    out_concept_name.write(name_tagged + "\t" + element_of + "\t");
                    out_concept_name.write(is_unique + "\t" + is_preferred + "\t");
                    out_concept_name.write(original_name + "\t" + is_not_substring + "\n");
                    is_preferred = 0;
                    for (int i = 0; i < synonyms.size(); i++) {
                        name = (String) synonyms.get(i);
                        original_name = name;
                        out_concept_name.write(uid + "\t" + name + "\t" + name_stemmed + "\t");
                        out_concept_name.write(name_tagged + "\t" + element_of + "\t");
                        out_concept_name.write(is_unique + "\t" + is_preferred + "\t");
                        out_concept_name.write(original_name + "\t" + is_not_substring + "\n");
                    }
                    String rel_type = "is_r";
                    element_of = "MESHD";
                    String from_name = name;
                    for (int i = 0; i < related.size(); i++) {
                        String to_uid = (String) related.get(i);
                        String to_name = (String) uid2name.get(to_uid);
                        out_relation.write(uid + "\t" + to_uid + "\t");
                        out_relation.write(rel_type + "\t" + element_of + "\t");
                        out_relation.write(from_name + "\t" + to_name + "\n");
                    }
                    rel_type = "is_a";
                    element_of = "MESHD";
                    related.clear();
                    for (int i = 0; i < treenr.size(); i++) {
                        String tnr = (String) treenr.get(i);
                        if (tnr.length() > 3) tnr = tnr.substring(0, tnr.lastIndexOf("."));
                        String rel_uid = (String) treenr2uid.get(tnr);
                        if (rel_uid != null) related.add(rel_uid); else System.out.println(uid + ": No DUI found for " + tnr);
                    }
                    for (int i = 0; i < related.size(); i++) {
                        String to_uid = (String) related.get(i);
                        String to_name = (String) uid2name.get(to_uid);
                        out_relation.write(uid + "\t" + to_uid + "\t");
                        out_relation.write(rel_type + "\t" + element_of + "\t");
                        out_relation.write(from_name + "\t" + to_name + "\n");
                    }
                    if (related.size() == 0) System.out.println(uid + ": No is_a relations");
                    rel_type = "act";
                    element_of = "MESHD";
                    for (int i = 0; i < actions.size(); i++) {
                        String to_uid = (String) actions.get(i);
                        String to_name = (String) uid2name.get(to_uid);
                        out_relation.write(uid + "\t" + to_uid + "\t");
                        out_relation.write(rel_type + "\t" + element_of + "\t");
                        out_relation.write(from_name + "\t" + to_name + "\n");
                    }
                    String method = "IMPM";
                    String score = "1.0";
                    for (int i = 0; i < chemicals.size(); i++) {
                        Vector chemical = (Vector) chemicals.get(i);
                        String type = (String) chemical.get(0);
                        String chem = (String) chemical.get(1);
                        if (!ec_numbers.contains(chem) && (type.compareTo("EC") == 0)) {
                            if (chem.compareTo("1.14.-") == 0) chem = "1.14.-.-"; else System.out.println("MISSING EC: " + chem);
                        }
                        String id = type + ":" + chem;
                        String entry = uid + "\t" + id + "\t" + method + "\t" + score + "\n";
                        if (type.compareTo("CAS") == 0) cas_mapping.write(entry); else ec_mapping.write(entry);
                    }
                } else inputLine = getNextLine(in);
            }
            System.out.println("End import descriptors");
            tools.printDate();
            in.close();
            out_concept.close();
            out_concept_name.close();
            out_relation.close();
            cas_mapping.close();
            ec_mapping.close();
            outfile = outfiledir + "\\cas";
            out_concept = new BufferedWriter(new FileWriter(outfile + "_concept.txt"));
            out_concept_name = new BufferedWriter(new FileWriter(outfile + "_concept_name.txt"));
            BufferedWriter out_concept_acc = new BufferedWriter(new FileWriter(outfile + "_concept_acc.txt"));
            for (int i = 0; i < allCASchemicals.size(); i++) {
                Vector chemical = (Vector) allCASchemicals.get(i);
                String cas_id = "CAS:" + (String) chemical.get(1);
                String cas_name = (String) chemical.get(2);
                String cas_pos_tag = "";
                String cas_description = (String) chemical.get(3);
                String cas_element_of = "CAS";
                String cas_is_primary = "0";
                out_concept.write(cas_id + "\t" + cas_pos_tag + "\t" + cas_description + "\t");
                out_concept.write(cas_element_of + "\t" + cas_is_primary + "\n");
                String cas_name_stemmed = "";
                String cas_name_tagged = "";
                String cas_is_unique = "0";
                String cas_is_preferred = "0";
                String cas_original_name = cas_name;
                String cas_is_not_substring = "0";
                out_concept_name.write(cas_id + "\t" + cas_name + "\t" + cas_name_stemmed + "\t");
                out_concept_name.write(cas_name_tagged + "\t" + cas_element_of + "\t");
                out_concept_name.write(cas_is_unique + "\t" + cas_is_preferred + "\t");
                out_concept_name.write(cas_original_name + "\t" + cas_is_not_substring + "\n");
                out_concept_acc.write(cas_id + "\t" + (String) chemical.get(1) + "\t");
                out_concept_acc.write(cas_element_of + "\n");
            }
            out_concept.close();
            out_concept_name.close();
            out_concept_acc.close();
        } catch (Exception e) {
            settings.writeLog("Error while reading MESH descriptor file: " + e.getMessage());
        }
    }

    public Vector getTables() {
        String search = getListPrefix() + "%";
        Vector tables = new Vector();
        try {
            Connection db = tools.openDB(getMESHdbname());
            tables = tools.getTableNames2Vector(db, search);
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog("KB_MESH.getTables(): " + e.getMessage());
        }
        Vector reducedTables = new Vector();
        for (int i = 0; i < tables.size(); i++) {
            String tablename = (String) tables.get(i);
            if ((tablename.indexOf("pkey") == -1) && (tablename.indexOf("index") == -1)) reducedTables.add(tablename);
        }
        return reducedTables;
    }

    public void createMeshList(String listname) {
        String command = "CREATE TABLE " + listname + " ( ";
        command = command + "index  INT4, name TEXT, annotation TEXT, PRIMARY KEY (index) )";
        try {
            Connection db = tools.openDB(getMESHdbname());
            tools.execute(db, command);
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog(e.getMessage());
        }
    }

    public void deleteMeshList(String listname) {
        String command = "DROP TABLE " + listname;
        try {
            Connection db = tools.openDB(getMESHdbname());
            tools.execute(db, command);
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog(e.getMessage());
        }
    }

    public Vector db2vector(String tablename) {
        Vector result = new Vector();
        try {
            String query = "SELECT * FROM " + tablename + " ORDER BY name";
            Connection db = tools.openDB(getMESHdbname());
            ResultSet rs = tools.executeQuery(db, query);
            tools.closeDB(db);
            while (rs.next()) {
                int index = rs.getInt("index");
                String name = rs.getString("name");
                String annot = rs.getString("annotation");
                DBrow row = new DBrow(index, name, annot);
                result.add(row);
            }
        } catch (Exception e) {
            settings.writeLog("KB_MESH (db2vector) ERROR: " + e.getMessage());
        }
        return result;
    }

    public void deleteSelectedItems(Object[] selection, String tablename) {
        String querybase = "DELETE FROM " + tablename + " WHERE index = ";
        try {
            Connection db = tools.openDB(getMESHdbname());
            for (int i = 0; i < selection.length; i++) {
                DBrow row = new DBrow();
                row = (DBrow) selection[i];
                String conv = "";
                String index = conv.valueOf(row.getItemIndex());
                String query = querybase + index;
                tools.execute(db, query);
            }
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog(e.getMessage());
        }
    }

    public String getItemAnnotation(Object selection) {
        DBrow row = (DBrow) selection;
        String ret = row.getItemAnnotation();
        return ret;
    }

    public void updateItemAnnotation(String tablename, Object selection, String newAnnot) {
        DBrow row = (DBrow) selection;
        int index = row.getItemIndex();
        String convert = "";
        String setClause = "annotation = '" + newAnnot + "'";
        String whereClause = "index = " + convert.valueOf(index);
        try {
            Connection db = tools.openDB(getMESHdbname());
            tools.updateRow(db, tablename, setClause, whereClause);
            tools.closeDB(db);
        } catch (Exception e) {
            settings.writeLog(e.getMessage());
        }
    }
}
