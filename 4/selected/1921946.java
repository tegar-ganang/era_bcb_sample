package com.drx;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import org.xml.sax.SAXException;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfAttr;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class DrxUtils {

    private static Stack<String> H1 = new Stack<String>();

    private static Stack<String> H2 = new Stack<String>();

    private static Stack<String> H3 = new Stack<String>();

    private static ArrayList<String> tableCols = new ArrayList<String>();

    private static boolean stopXML = false;

    private static final String XML_EXCLUDE = "[ ,/=:]";

    private static boolean first = true;

    private static HashMap<String, String> xperm = new HashMap<String, String>();

    private static void DrxUtilsInit() {
        xperm.put("0", "Execute_Procedure|Change_Location");
        xperm.put("1", "Execute_Procedure");
        xperm.put("2", "Change_Location");
        xperm.put("3", "No_extended_permissions");
        xperm.put("65536", "Execute_Procedure|Change_Location|Change_State");
        xperm.put("65537", "Change_Location|Change_State");
        xperm.put("65538", "Execute_Procedure|Change_State");
        xperm.put("65539", "Change_State");
        xperm.put("131072", "Execute_Procedure|Change_Location|Change_Permission");
        xperm.put("131073", "Change_Location|Change_Permission");
        xperm.put("131074", "Execute_Procedure|Change_Permission");
        xperm.put("131075", "Change_Permission");
        xperm.put("196608", "Execute_Procedure|Change_Location|Change_State|Change_Permission");
        xperm.put("196609", "Change_Location|Change_State|Change_Permission");
        xperm.put("196610", "Execute_Procedure|Change_State|Change_Permission");
        xperm.put("196611", "Change_State|Change_Permission");
        xperm.put("262144", "Execute_Procedure|Change_Location|Change_Ownership");
        xperm.put("262145", "Change_Location|Change_Ownership");
        xperm.put("262146", "Execute_Procedure|Change_Ownership");
        xperm.put("262147", "Change_Ownership");
        xperm.put("327680", "Execute_Procedure|Change_Location|Change_State|Change_Ownership");
        xperm.put("327681", "Change_Location|Change_State|Change_Ownership");
        xperm.put("327682", "Execute_Procedure|Change_State|Change_Ownership");
        xperm.put("327683", "Change_State|Change_Ownership");
        xperm.put("393216", "Execute_Procedure|Change_Location|Change_Permission|Change_Ownership");
        xperm.put("393217", "Change_Location|Change_Permission|Change_Ownership");
        xperm.put("393218", "Execute_Procedure|Change_Permission|Change_Ownership");
        xperm.put("393219", "Change_Permission|Change_Ownership");
        xperm.put("458752", "Execute_Procedure|Change_Location|Change_State|Change_Permission|Change_Ownership");
        xperm.put("458753", "Change_Location|Change_State|Change_Permission|Change_Ownership");
        xperm.put("458754", "Execute_Procedure|Change_State|Change_Permission|Change_Ownership");
        xperm.put("458755", "Change_State|Change_Permission|Change_Ownership");
        xperm.put("524288", "Execute_Procedure|Change_Location|Extended_Delete");
        xperm.put("524289", "Change_Location|Extended_Delete");
        xperm.put("524290", "Execute_Procedure|Extended_Delete");
        xperm.put("524291", "Extended_Delete");
        xperm.put("589824", "Execute_Procedure|Change_Location|Change_State|Extended_Delete");
        xperm.put("589825", "Change_Location|Change_State|Extended_Delete");
        xperm.put("589826", "Execute_Procedure|Change_State|Extended_Delete");
        xperm.put("589827", "Change_State|Extended_Delete");
        xperm.put("655360", "Execute_Procedure|Change_Location|Change_Permission|Extended_Delete");
        xperm.put("655361", "Change_Location|Change_Permission|Extended_Delete");
        xperm.put("655362", "Execute_Procedure|Change_Permission|Extended_Delete");
        xperm.put("655363", "Change_Permission|Extended_Delete");
        xperm.put("720896", "Execute_Procedure|Change_Location|Change_State|Change_Permission|Extended_Delete");
        xperm.put("720897", "Change_Location|Change_State|Change_Permission|Extended_Delete");
        xperm.put("720898", "Execute_Procedure|Change_State|Change_Permission|Extended_Delete");
        xperm.put("720899", "Change_State|Change_Permission|Extended_Delete");
        xperm.put("786432", "Execute_Procedure|Change_Location|Change_Ownership|Extended_Delete");
        xperm.put("786433", "Change_Location|Change_Ownership|Extended_Delete");
        xperm.put("786434", "Execute_Procedure|Change_Ownership|Extended_Delete");
        xperm.put("786435", "Change_Ownership|Extended_Delete");
        xperm.put("851968", "Execute_Procedure|Change_Location|Change_State|Change_Ownership|Extended_Delete");
        xperm.put("851969", "Change_Location|Change_State|Change_Ownership|Extended_Delete");
        xperm.put("851970", "Execute_Procedure|Change_State|Change_Ownership|Extended_Delete");
        xperm.put("851971", "Change_State|Change_Ownership|Extended_Delete");
        xperm.put("917504", "Execute_Procedure|Change_Location|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("917505", "Change_Location|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("917506", "Execute_Procedure|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("917507", "Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("983040", "Execute_Procedure|Change_Location|Change_State|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("983041", "Change_Location|Change_State|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("983042", "Execute_Procedure|Change_State|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("983043", "Change_State|Change_Permission|Change_Ownership|Extended_Delete");
        xperm.put("2031616", "Extended_Delete|Execute_Procedure|Change_Permission|Change_Folder_Links|Change_Location|Change_State|Change_Ownership");
    }

    public static String Banner() {
        StringBuilder sb = new StringBuilder();
        sb.append(Drx.APP_NAME + "\n");
        sb.append(Drx.COPYRIGHT + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public static String htmlBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n<head>\n<title>" + Drx.m_reportFileName + "</title>\n</head>\n<body>\n");
        sb.append("<a name=\"top\">\n");
        sb.append("<center>");
        sb.append(Head1(Drx.APP_NAME));
        sb.append(Head3(Drx.COPYRIGHT) + "\n");
        sb.append(Head3("<a href=\"http://drxproj.sourceforge.net/\" target=\"_blank\">Visit DRX Project</a>") + "\n");
        sb.append(Head3("<a href=\"http://www.flatironssolutions.com/\" target=\"_blank\">Visit Flatirons</a>") + "\n");
        sb.append("</center>");
        return sb.toString();
    }

    public static String htmlFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("<hr/>");
        sb.append(Head2(Drx.APP_NAME) + "\n");
        sb.append(Head3("<a href=\"http://drxproj.sourceforge.net/\" target=\"_blank\">Visit DRX Project</a>&nbsp;&nbsp;|&nbsp;&nbsp;" + "<a href=\"http://www.flatironssolutions.com/technical-leadership/content-management\" target=\"_blank\">Visit Flatirons Solutions</a>"));
        sb.append(Head3(Drx.COPYRIGHT) + "\n");
        return sb.toString();
    }

    public static void DrxWrite(ArrayList<String> a) {
        for (String s : a) {
            DrxWrite(s);
        }
    }

    public static void DrxWrite(String s) {
        if (Drx.reportFile() != null && Boolean.parseBoolean(Drx.getProperty(Drx.FILE_OUTPUT)) == true) Drx.reportFile().println(s);
        if (Boolean.parseBoolean(Drx.getProperty(Drx.CONSOLE_OUTPUT)) == true) writeToConsole(s);
    }

    public static void DrxWrite(StringBuilder sb) {
        String s = sb.toString();
        DrxWrite(s);
    }

    public static void writeToConsole(String s) {
        System.out.println(s);
        System.out.flush();
    }

    public static void DrxWriteError(String c, String s) {
        DrxWrite("***** ERROR in " + c + " MODULE: " + s + " *****");
    }

    public static ArrayList<String> processCollection(IDfCollection col) throws Exception {
        return processCollection(col, false, "", null);
    }

    public static ArrayList<String> doQuery(String dql, IDfSession session) throws Exception {
        ArrayList<String> results = new ArrayList<String>();
        String from = "";
        IDfQuery q = new DfQuery();
        q.setDQL(dql);
        IDfCollection col = q.execute(session, DfQuery.DF_READ_QUERY);
        Matcher m = Pattern.compile(" from(\\s+)(\\w+)(\\s+)").matcher(dql);
        if (m.find()) {
            from = m.group(2);
        }
        if (from.equals("dmc_rps_phase_rel")) results = processCollection(col, false, from, session); else results = processCollection(col, true, from, session);
        col.close();
        return results;
    }

    public static ArrayList<String> processCollection(IDfCollection col, boolean omitObjId, String from, IDfSession session) throws Exception {
        ArrayList<String> colNames = new ArrayList<String>();
        ArrayList<String> result = new ArrayList<String>();
        String user_name = null;
        String attrName = null;
        if (xperm.isEmpty()) {
            DrxUtilsInit();
        }
        result.add(startTable());
        for (int i = 0; i < col.getAttrCount(); i++) {
            IDfAttr attr = col.getAttr(i);
            if ((omitObjId == false) || (!attr.getName().equalsIgnoreCase("r_object_id"))) {
                colNames.add((String) attr.getName());
            }
        }
        result.add(tableHeaders(colNames));
        while (col.next() == true) {
            ArrayList<String> row = new ArrayList<String>();
            for (int i = 0; i < col.getAttrCount(); i++) {
                String colValue = null;
                IDfAttr attr = null;
                attr = col.getAttr(i);
                attrName = attr.getName();
                if ((omitObjId == false) || (!attrName.equalsIgnoreCase("r_object_id"))) {
                    if (attr.isRepeating() == true) {
                        colValue = col.getAllRepeatingStrings(attrName, ",");
                        String[] vals = colValue.split(",");
                        StringBuilder sb = new StringBuilder();
                        boolean comma = false;
                        for (int j = 0; j < vals.length; j++) {
                            if (vals[j] != null && vals[j].length() > 0) {
                                if (from.equals("dm_acl") && attrName.equals("r_accessor_xpermit")) {
                                    String xpermit = xperm.get(vals[j]);
                                    if (xpermit != null) vals[j] = xpermit;
                                }
                                if (comma) sb.append(",<br>" + vals[j]); else sb.append(vals[j]);
                                comma = true;
                            } else {
                                if (comma) sb.append(",<br>&nbsp;"); else sb.append("&nbsp;");
                                comma = true;
                            }
                        }
                        colValue = sb.toString();
                        if (from.equals("dm_acl") && attrName.equals("r_accessor_permit")) {
                            colValue = colValue.replaceAll("0", "NULL");
                            colValue = colValue.replaceAll("1", "None");
                            colValue = colValue.replaceAll("2", "Browse");
                            colValue = colValue.replaceAll("3", "Read");
                            colValue = colValue.replaceAll("4", "Relate");
                            colValue = colValue.replaceAll("5", "Version");
                            colValue = colValue.replaceAll("6", "Write");
                            colValue = colValue.replaceAll("7", "Delete");
                        }
                    } else if (attr.getDataType() == IDfAttr.DM_BOOLEAN) {
                        colValue = Boolean.toString(col.getBoolean(attr.getName()));
                    } else if (attr.getDataType() == IDfAttr.DM_DOUBLE) {
                        colValue = Double.toString(col.getDouble(attr.getName()));
                    } else if (attr.getDataType() == IDfAttr.DM_ID) {
                        colValue = col.getId(attr.getName()).toString();
                    } else if (attr.getDataType() == IDfAttr.DM_INTEGER) {
                        colValue = Integer.toString(col.getInt(attr.getName()));
                    } else if (attr.getDataType() == IDfAttr.DM_STRING) {
                        colValue = col.getString(attr.getName());
                    } else if (attr.getDataType() == IDfAttr.DM_TIME) {
                        colValue = col.getTime(attr.getName()).toString();
                    } else {
                        colValue = col.getString(attrName);
                    }
                    if ((colValue == null) || (colValue.length() == 0) || (colValue.equals(" ")) || (colValue.equals(","))) {
                        colValue = "&nbsp;";
                    }
                    if (from.equals("dm_user") && attrName.equalsIgnoreCase("user_name")) {
                        user_name = colValue;
                    }
                    if (from.equals("dm_user") && attrName.equalsIgnoreCase("user_group_name")) {
                        IDfQuery q = new DfQuery();
                        String quoted_user_name = user_name.replaceAll("'", "''");
                        String dql = "select group_name from dm_group where group_class='group' and any i_all_users_names = '" + quoted_user_name + "' order by group_name";
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        StringBuilder groups = new StringBuilder();
                        while (col2.next()) {
                            groups.append(col2.getString("group_name")).append(",<br>");
                        }
                        col2.close();
                        if (groups.length() > 0) {
                            groups.delete(groups.lastIndexOf(",<br>"), groups.length());
                            colValue = groups.toString();
                        }
                    }
                    if (from.equals("dmc_rps_contact") && attrName.equalsIgnoreCase("user_name")) {
                        IDfQuery q = new DfQuery();
                        String dql = "select user_name from dm_user where r_object_id='" + colValue + "'";
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        if (col2.next()) {
                            colValue = col2.getString("user_name");
                        } else if (colValue.equals("0000000000000000")) {
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    if (from.equals("dmc_rps_authority") && attrName.equalsIgnoreCase("user_name")) {
                        String[] user_ids = colValue.split(",<br>");
                        StringBuilder users = new StringBuilder();
                        for (int j = 0; j < user_ids.length; j++) {
                            IDfQuery q = new DfQuery();
                            String dql = "select user_name from dm_user where r_object_id in " + "(select user_id from dmc_rps_contact where r_object_id='" + user_ids[j] + "')";
                            q.setDQL(dql);
                            IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                            if (col2.next()) {
                                users.append(col2.getString("user_name") + ",<br>");
                            } else {
                                users.append("&nbsp;,<br>");
                            }
                            col2.close();
                        }
                        colValue = users.toString().substring(0, users.length() - 5);
                    }
                    if (from.equals("dmc_rps_retention_policy") && attrName.endsWith("_id")) {
                        IDfQuery q = new DfQuery();
                        String dql = "select object_name from dm_sysobject where r_object_id='" + colValue + "'";
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        if (col2.next()) {
                            colValue = col2.getString("object_name");
                        } else if (colValue.equals("0000000000000000")) {
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    if (from.equals("dmc_rps_disp_strategy") && attrName.endsWith("_ids")) {
                        String[] ids = colValue.split(",<br>");
                        StringBuilder names = new StringBuilder();
                        for (int j = 0; j < ids.length; j++) {
                            IDfQuery q = new DfQuery();
                            String dql = "select object_name from dm_sysobject where r_object_id='" + ids[j] + "'";
                            q.setDQL(dql);
                            IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                            if (col2.next()) {
                                names.append(col2.getString("object_name") + ",<br>");
                            } else {
                                names.append("&nbsp;,<br>");
                            }
                            col2.close();
                        }
                        colValue = names.toString().substring(0, names.length() - 5);
                    }
                    if ((from.equals("dmc_rps_action_rel") || from.equals("dmc_rps_phase_rel")) && attrName.endsWith("_id") && !attrName.equals("r_object_id")) {
                        IDfQuery q = new DfQuery();
                        String dql = null;
                        String name = null;
                        if (colValue.startsWith("37")) {
                            dql = "select relation_name from dm_relation where r_object_id='" + colValue + "'";
                            name = "relation_name";
                        } else {
                            dql = "select object_name from dm_sysobject where r_object_id='" + colValue + "'";
                            name = "object_name";
                        }
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        if (col2.next()) {
                            if (name.equals("relation_name")) colValue = col2.getString(name) + "<br>(" + colValue + ")"; else colValue = col2.getString(name);
                        } else if (colValue.equals("0000000000000000")) {
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    if (from.equals("dmc_prm_docbase_config") && attrName.endsWith("_id")) {
                        IDfQuery q = new DfQuery();
                        String dql = null;
                        if (colValue.startsWith("4b")) {
                            dql = "select object_name from dm_process where r_object_id='" + colValue + "'";
                        } else {
                            dql = "select object_name from dm_policy where r_object_id='" + colValue + "'";
                        }
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        if (col2.next()) {
                            colValue = col2.getString("object_name");
                        } else if (colValue.equals("0000000000000000")) {
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    if (from.equals("dm_xfm_form") && attrName.equals("definition_state")) {
                        if (colValue.equals("0")) {
                            colValue = "DRAFT";
                        } else if (colValue.equals("1")) {
                            colValue = "VALIDATED";
                        } else if (colValue.equals("2")) {
                            colValue = "INSTALLED";
                        } else if (colValue.equals("3")) {
                            colValue = "OBSOLETE";
                        }
                    }
                    if (from.equals("dm_webc_config") && attrName.equals("source_folder")) {
                        if (colValue.equals("0000000000000000")) {
                            colValue = "&nbsp;";
                        } else {
                            IDfSysObject sObject = (IDfSysObject) session.getObject(new DfId(colValue));
                            StringBuilder sb = new StringBuilder();
                            for (int j = 0; j < sObject.getValueCount("r_folder_path"); j++) {
                                String path = sObject.getRepeatingString("r_folder_path", j);
                                if (path != null) {
                                    sb.append(path).append("<br>");
                                }
                            }
                            if (sb.length() <= 0) colValue = "&nbsp;"; else {
                                colValue = sb.toString();
                                int lbr = colValue.lastIndexOf("<br>");
                                colValue = colValue.substring(0, lbr);
                            }
                        }
                    }
                    if (from.equals("dm_webc_config") && attrName.equals("target_id")) {
                        IDfQuery q = new DfQuery();
                        String dql = "select object_name from dm_webc_target where r_object_id='" + colValue + "'";
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        while (col2.next()) {
                            colValue = col2.getString("object_name");
                            if (colValue != null) break;
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    if (from.equals("dmc_module") && attrName.equals("target_id")) {
                        IDfQuery q = new DfQuery();
                        String dql = "select object_name from dm_webc_target where r_object_id='" + colValue + "'";
                        q.setDQL(dql);
                        IDfCollection col2 = q.execute(session, DfQuery.DF_READ_QUERY);
                        while (col2.next()) {
                            colValue = col2.getString("object_name");
                            if (colValue != null) break;
                            colValue = "&nbsp;";
                        }
                        col2.close();
                    }
                    row.add(colValue);
                }
            }
            result.add(tableRow(row));
        }
        col.close();
        result.add(endTable());
        if (result.size() <= 3) {
            result.clear();
        }
        return result;
    }

    public static String Head1(String header, String anchor) {
        String s = "";
        if (null != anchor) {
            s = "<a name=" + anchor + "><h1>" + header + " <a href=\"#top\" style=\"font-size:16px\"> top</a></h1></a>";
        } else {
            s = "<h1>" + header + " <a href=\"#top\" style=\"font-size:16px\">top</a></h1>";
        }
        if (stopXML == false) {
            try {
                if (H3.empty() == false) {
                    String old_H3 = H3.pop();
                    Drx.m_XMLData.endElement(old_H3);
                    if (H3.empty() == false) {
                        System.out.println("H1: H3 not empty: " + H3.toString());
                    }
                }
                if (H2.empty() == false) {
                    String old_H2 = H2.pop();
                    Drx.m_XMLData.endElement(old_H2);
                    if (H2.empty() == false) {
                        System.out.println("H1: H2 not empty: " + H2.toString());
                    }
                }
                if (H1.empty() == false) {
                    String old_H1 = H1.pop();
                    if (old_H1.equals(header) == false) {
                        H1.pop();
                        Drx.m_XMLData.endElement(old_H1);
                    } else {
                        System.out.println("H1: new same as old: " + old_H1 + " size = " + H1.size());
                    }
                }
                int lparam = header.indexOf(" (");
                if (lparam > 0) {
                    header = header.substring(0, lparam);
                }
                String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                H1.push(xml_tag);
                Drx.m_XMLData.startElement(xml_tag);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
        if (first) {
            first = false;
            return s;
        } else return "<hr/>\n" + s;
    }

    public static void endHead1(String header, String anchor) {
        if (stopXML == false) {
            try {
                if (H3.empty() == false) {
                    String old_H3 = H3.pop();
                    Drx.m_XMLData.endElement(old_H3);
                    if (H3.empty() == false) {
                        System.out.println("endH1: H3 not empty: " + H3.toString());
                    }
                }
                if (H2.empty() == false) {
                    String old_H2 = H2.pop();
                    Drx.m_XMLData.endElement(old_H2);
                    if (H2.empty() == false) {
                        System.out.println("endH1: H2 not empty: " + H2.toString());
                    }
                }
                assert (H1.size() == 1);
                if (H1.empty() == false) {
                    String old_H1 = H1.pop();
                    int lparam = header.indexOf(" (");
                    if (lparam > 0) {
                        header = header.substring(0, lparam);
                    }
                    if (old_H1.equals(header)) {
                        Drx.m_XMLData.endElement(old_H1);
                    } else {
                        String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                        Drx.m_XMLData.endElement(xml_tag);
                    }
                }
                assert (H1.empty() == true);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
    }

    public static String Head1(String header) {
        return Head1(header, null);
    }

    public static void endHead1(String header) {
        endHead1(header, null);
    }

    public static String Head2(String header, String anchor) {
        String s = "";
        if (null != anchor) {
            s = "<a name=" + anchor + "><h2>" + header + "<a href=\"#top\" style=\"font-size:16px\"> top</a></h2></a>";
        } else {
            s = "<h2>" + header + "  <a href=\"#top\" style=\"font-size:16px\">top</a></h2>";
        }
        if (stopXML == false) {
            try {
                String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                if (H3.empty() == false) {
                    String old_H3 = H3.pop();
                    Drx.m_XMLData.endElement(old_H3);
                    if (H3.empty() == false) {
                        System.out.println("H2: H3 not empty: " + H3.toString());
                    }
                }
                if (H2.empty() == false) {
                    String old_H2 = H2.peek();
                    if (old_H2.equals(xml_tag) == false) {
                        H2.pop();
                        Drx.m_XMLData.endElement(old_H2);
                    } else {
                        System.out.println("H2: new same as old: " + old_H2 + " size = " + H2.size());
                    }
                }
                H2.push(xml_tag);
                Drx.m_XMLData.startElement(xml_tag);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
        return s;
    }

    public static void endHead2(String header, String anchor) {
        if (stopXML == false) {
            try {
                assert (H2.size() == 1);
                if (H2.empty() == false) {
                    String old_H2 = H2.pop();
                    String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                    if (old_H2.equals(xml_tag)) {
                        Drx.m_XMLData.endElement(old_H2);
                    } else {
                        Drx.m_XMLData.endElement(xml_tag);
                    }
                }
                assert (H2.empty() == true);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
    }

    public static String Head2(String header) {
        return Head2(header, null);
    }

    public static void endHead2(String header) {
        endHead2(header, null);
    }

    public static String Head3(String header, String anchor) {
        String s = "";
        if (null != anchor) {
            s = "<br><a name=" + anchor + "><h3>" + header + "</h3></a>";
        } else {
            s = "<br><h3>" + header + "</h3>";
        }
        if (stopXML == false) {
            try {
                int colon = header.indexOf(':');
                if (colon > 0) {
                    String tag = header.substring(0, colon).trim().replaceAll(XML_EXCLUDE, "_");
                    ;
                    String value = header.substring(colon + 1).trim();
                    Drx.m_XMLData.dataElement(tag, value);
                } else {
                    String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                    if (H3.empty() == false) {
                        String old_H3 = H3.peek();
                        if (old_H3.equals(xml_tag) == false) {
                            H3.pop();
                            Drx.m_XMLData.endElement(old_H3);
                        } else {
                            System.out.println("H3: new same as old: " + old_H3 + " size = " + H3.size());
                        }
                    }
                    H3.push(xml_tag);
                    Drx.m_XMLData.startElement(xml_tag);
                }
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
        return s;
    }

    public static void endHead3(String header, String anchor) {
        if (stopXML == false) {
            try {
                String xml_tag = header.trim().replaceAll(XML_EXCLUDE, "_");
                assert (H3.size() == 1);
                if (H3.empty() == false) {
                    String old_H3 = H3.pop();
                    if (old_H3.equals(xml_tag)) {
                        Drx.m_XMLData.endElement(old_H3);
                    } else {
                        Drx.m_XMLData.endElement(xml_tag);
                    }
                }
                assert (H3.empty() == true);
            } catch (SAXException saxe) {
                saxe.printStackTrace();
            }
        }
    }

    public static String Head3(String header) {
        return Head3(header, null);
    }

    public static void endHead3(String header) {
        endHead3(header, null);
    }

    static void endHeadAll() {
        try {
            stopXML = true;
            if (H3.empty() == false) {
                String old_H3 = H3.pop();
                Drx.m_XMLData.endElement(old_H3);
                if (H3.empty() == false) {
                    System.out.println("endHeadAll: H3 not empty: " + H3.toString());
                }
            }
            if (H2.empty() == false) {
                String old_H2 = H2.pop();
                Drx.m_XMLData.endElement(old_H2);
                if (H2.empty() == false) {
                    System.out.println("endHeadAll: H2 not empty: " + H2.toString());
                }
            }
            if (H1.empty() == false) {
                String old_H1 = H1.pop();
                Drx.m_XMLData.endElement(old_H1);
                if (H1.empty() == false) {
                    System.out.println("endHeadAll: size = " + H1.size());
                }
            }
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        }
    }

    public static String Warning(String warning) {
        return ("<p><font color=\"red\"><h3>" + warning + "</h3></font></p>");
    }

    public static String textP(String str) {
        return "<p>" + str + "</p>";
    }

    public static String textBr(String str) {
        return str + "<br/>";
    }

    public static String pre(String str) {
        return "<pre>" + str + "</pre>";
    }

    public static String bold(String str) {
        return "<b>" + str + "</b>";
    }

    public static String pre(ArrayList<String> text) {
        StringBuilder sb = new StringBuilder();
        for (String line : text) {
            sb.append(line);
        }
        return pre(sb.toString());
    }

    public static String startTable() {
        tableCols.clear();
        return "<table border=1 cellpading=2>";
    }

    public static String endTable() {
        tableCols.clear();
        return "</table><p/>";
    }

    public static String tableHeaders(ArrayList<String> cols) {
        return tableHeaders(cols.toArray(new String[0]));
    }

    public static String tableHeaders(String[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        for (String c : cols) {
            sb.append("<th bgcolor=\"0x3333FF\">" + c + "</th>");
            String xml_tag = c.trim().replaceAll(XML_EXCLUDE, "_");
            tableCols.add(xml_tag);
        }
        sb.append("</tr>");
        return sb.toString();
    }

    public static String tableRow(ArrayList<String> row) {
        return tableRow((String[]) row.toArray(new String[0]));
    }

    public static String tableRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        int col = 0;
        String xml_tag = null;
        try {
            sb.append("<tr>");
            if (tableCols.isEmpty() == false) {
                Drx.m_XMLData.startElement("row");
            }
            for (String c : row) {
                if ((c == null) || (c == "") || (c.length() == 0)) {
                    c = "&nbsp";
                }
                sb.append("<td>" + c + "</td>");
                if (tableCols.isEmpty() == true) {
                    if (col == 0) {
                        xml_tag = c.trim().replaceAll(XML_EXCLUDE, "_");
                        col++;
                    } else {
                        Drx.m_XMLData.dataElement(xml_tag, c);
                    }
                } else {
                    if (col < tableCols.size()) {
                        String val = c.replaceAll(",<br>", ",");
                        Drx.m_XMLData.dataElement(tableCols.get(col++), val);
                    }
                }
            }
            sb.append("</tr>");
            if (tableCols.isEmpty() == false) {
                Drx.m_XMLData.endElement("row");
            }
        } catch (SAXException saxe) {
            saxe.printStackTrace();
        }
        return sb.toString();
    }

    public static void createTOC() {
        boolean inList = false;
        try {
            writeToConsole("Writing table of contents...");
            File fOut = new File(Drx.getDocbroker() + "/" + Drx.m_reportFileName + "_logo.html");
            fOut.createNewFile();
            FileOutputStream out = new FileOutputStream(fOut.getAbsolutePath());
            PrintStream p = new PrintStream(out);
            p.println("<img src=\"compass.jpg\" width=\"100%\" height=\"100%\" />");
            p.close();
            CopyFile("logo/compass.jpg", Drx.getDocbroker() + "/compass.jpg");
            fOut = new File(Drx.getDocbroker() + "/" + Drx.m_reportFileName + "_title.html");
            fOut.createNewFile();
            out = new FileOutputStream(fOut.getAbsolutePath());
            p = new PrintStream(out);
            p.println("<center>");
            p.println("<h2>" + Drx.APP_NAME + "</h2>");
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy HH:mm");
            String now = formatter.format(new Date());
            String docbroker = Drx.getDocbroker();
            docbroker = docbroker.substring(0, docbroker.indexOf('_'));
            p.println("<h3> Report for Docbroker: " + docbroker + ", Docbase: " + Drx.getProperty(Drx.DOCBASE) + "<br/>" + now + "</h3>");
            p.println("<h4>See a missing module?&nbsp;&nbsp;&nbsp;Want enhancements to an existing one?<br>" + "DRX is now an Open Source Project!&nbsp;&nbsp;&nbsp;" + "<a href=\"http://drxproj.sourceforge.net/\" target=\"_blank\">Join the DRX Project Team and Help</a></h4>");
            p.println("<h5>" + Drx.COPYRIGHT + ".&nbsp;&nbsp;&nbsp;DRX provided under The Ecplise Public License v1.0</h5>");
            p.println("</center>");
            p.close();
            fOut = new File(Drx.getDocbroker() + "/" + Drx.m_reportFileName + ".html");
            fOut.createNewFile();
            out = new FileOutputStream(fOut.getAbsolutePath());
            p = new PrintStream(out);
            p.println("<HTML><HEAD><title>" + Drx.m_reportFileName + ".html</TITLE></HEAD>");
            p.println("<FRAMESET ROWS=\"35%,*\">");
            p.println("<FRAMESET COLS=\"25%,*\" FRAMEBORDER=NO>");
            p.println("<FRAME SRC=\"" + Drx.m_reportFileName + "_logo.html\" NAME=LOGO SCROLLING=NO>");
            p.println("<FRAME SRC=\"" + Drx.m_reportFileName + "_title.html\" NAME=TITLE>");
            p.println("</FRAMESET>");
            p.println("<FRAMESET COLS=\"20%,*\">");
            p.println("<FRAME SRC=\"" + Drx.m_reportFileName + "_toc.html\" NAME=TOC>");
            p.println("<FRAME SRC=\"" + Drx.m_reportFileName + "_main.html\" NAME=MAIN>");
            p.println("</FRAMESET></FRAMESET></HTML>");
            p.close();
            fOut = new File(Drx.getDocbroker() + "/" + Drx.m_reportFileName + "_toc.html");
            fOut.createNewFile();
            out = new FileOutputStream(fOut.getAbsolutePath());
            p = new PrintStream(out);
            File fIn = new File(Drx.getDocbroker() + "/" + Drx.m_reportFileName + "_main.html");
            BufferedReader br = new BufferedReader(new FileReader(fIn));
            Pattern p1 = Pattern.compile("<a name=(.+)><h1>\\W*(.+)<a href=\"#top\" style=\"font-size:16px\"> top</a></h1></a>.*");
            Pattern p2 = Pattern.compile("<a name=(.+)><h2>\\W*(.+)<a href=\"#top\" style=\"font-size:16px\"> top</a></h2></a>.*");
            String line = null;
            while ((line = br.readLine()) != null) {
                Matcher mH1 = p1.matcher(line);
                Matcher mH2 = p2.matcher(line);
                if (mH1.find()) {
                    if (inList == true) {
                        inList = false;
                        p.println("</ul>");
                    }
                    String anchor = mH1.group(1);
                    String toc = mH1.group(2);
                    p.println("<a href=\"" + Drx.m_reportFileName + "_main.html#" + anchor + "\" target=MAIN><h3>" + toc + "</h3></a>");
                }
                if (mH2.find()) {
                    if (!inList == true) {
                        inList = true;
                        p.println("<ul>");
                    }
                    String anchor = mH2.group(1);
                    String toc = mH2.group(2);
                    p.println("<a href=\"" + Drx.m_reportFileName + "_main.html#" + anchor + "\" target=MAIN><h4>" + toc + "</h4></a>");
                }
            }
            p.println("</ul>");
            p.close();
            br.close();
            fOut = null;
            fIn = null;
        } catch (Exception e) {
            DrxWriteError("createTOC", e.getMessage());
        }
    }

    public static String runSingleQuery(String dql, IDfSession session) {
        String result = "";
        try {
            IDfQuery q = new DfQuery();
            q.setDQL(dql);
            IDfCollection col = q.execute(session, DfQuery.DF_READ_QUERY);
            col.next();
            result = col.getString(col.getAttr(0).getName());
            col.close();
        } catch (Exception e) {
            DrxWriteError("runSingleQuery", e.getMessage());
        }
        return result;
    }

    /**
     * Copy sourcefile to destfile creating destination folders if required on
     * the local filesystem.
     *
     * @param sourcefile Folderpath and filename to be copied
     * @param destfile Folderpath and filename to be created
     * @return  String with full pathname to copied file
     * @throws FileNotFoundException SERCH folder or actual BIN file not found.
     * @throws IOException  Error accessing the file.
     */
    public static String CopyFile(String sourcefile, String destfile) throws FileNotFoundException, IOException {
        int last = destfile.lastIndexOf('/');
        if (last < 0) {
            DrxWriteError("CopyFile", "Destination filepath " + destfile + " doesn't contain /");
            throw new java.io.FileNotFoundException(destfile);
        }
        String parent = destfile.substring(0, last);
        if (parent.length() > 0) {
            File f = new File(parent);
            if (!f.isDirectory()) {
                if (!f.mkdirs()) {
                    DrxWriteError("CopyFile", "Folder " + parent + " doesn't exist, cannot create");
                }
            }
        }
        FileChannel srcChannel = new FileInputStream(sourcefile).getChannel();
        FileChannel dstChannel = new FileOutputStream(destfile).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
        return destfile;
    }

    public static void dumpTypedObject(IDfTypedObject obj) {
        try {
            for (int i = 0; i < obj.getAttrCount(); i++) {
                IDfAttr attr = obj.getAttr(i);
                String attrName = attr.getName();
                String xml_tag = attrName.replaceAll("[ /]", "_");
                String value = "";
                int attrCnt = 0;
                if (attr.isRepeating() == true && (attrCnt = obj.getValueCount(attrName)) > 0) {
                    for (int j = 0; j < attrCnt; j++) {
                        value = obj.getRepeatingString(attrName, j);
                        String tag = xml_tag + "_" + j;
                        DrxUtils.DrxWrite(DrxUtils.tableRow(new String[] { tag, value }));
                    }
                } else {
                    value = obj.getString(attrName);
                    DrxUtils.DrxWrite(DrxUtils.tableRow(new String[] { xml_tag, value }));
                }
            }
        } catch (DfException dfe) {
        }
    }
}
