package ch.zarzu.champions.builder;

import java.io.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import ch.zarzu.util.*;

public class SystemLink {

    private NodeList xml_pool_list, xml_subpool_list, xml_power_list, xml_stat_list, xml_talent_list, xml_build, xml_frameworks;

    private String app_path;

    private HashMap<String, HashMap<String, String>> system_strings = null;

    private HashMap<String, HashSet<String>> power_contributes_to;

    private HashMap<String, String> power_builds_on;

    private HashMap<String, NodeList> effect_map;

    private LinkedList<LinkedList<LinkedList<HashMap<String, String>>>> characteristics_list;

    private HashMap<String, String> power_archtype;

    private static SystemLink instance = null;

    private SystemLink() {
        app_path = AppPath.get();
        getLanguage();
    }

    public void getLanguage() {
        try {
            FileInputStream fis = new FileInputStream(app_path + "data/languages.xml");
            Document languages_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            NodeList xml_languages = languages_doc.getElementsByTagName("string");
            createStringMap(xml_languages);
        } catch (Exception e) {
            writeError("loading language file", e);
            e.printStackTrace();
        }
    }

    /**
	 * parses the xml files with all the data in it: powers, talents, progression, system strings
	 */
    public void parseXml() {
        String language = PrefLink.getInstance().getPreference("language");
        String language_folder = getString("language_folders").get(language);
        try {
            File file = new File(app_path + "data/" + language_folder + "/powers.xml");
            File file2 = new File(app_path + "data/" + language_folder + "/stats.xml");
            File file3 = new File(app_path + "data/build.xml");
            File file5 = new File(app_path + "data/frameworks.xml");
            File file6 = new File(app_path + "data/pools.xml");
            File file7 = new File(app_path + "data/characteristics.xml");
            File file8 = new File(app_path + "data/effects.xml");
            new File(app_path + "builds").mkdirs();
            FileInputStream fis;
            for (String s : getString("language_folders").values()) {
                fis = new FileInputStream(new File(app_path + "data/" + s + "/system.xml"));
                Document system_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
                NodeList xml_system = system_doc.getElementsByTagName("string");
                createStringMap(xml_system);
            }
            fis = new FileInputStream(file);
            Document main_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            xml_power_list = main_doc.getElementsByTagName("power");
            fis = new FileInputStream(file6);
            Document pool_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            xml_subpool_list = pool_doc.getElementsByTagName("subpool");
            xml_pool_list = pool_doc.getElementsByTagName("pool");
            fis = new FileInputStream(file2);
            Document stat_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            xml_stat_list = stat_doc.getElementsByTagName("stat");
            xml_talent_list = stat_doc.getElementsByTagName("talent");
            fis = new FileInputStream(file8);
            Document effect_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            NodeList xml_effect_list = effect_doc.getElementsByTagName("effects");
            fis = new FileInputStream(file3);
            Document build_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            xml_build = build_doc.getElementsByTagName("build");
            fis = new FileInputStream(file5);
            Document framework_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            xml_frameworks = framework_doc.getElementsByTagName("framework");
            fis = new FileInputStream(file7);
            Document characteristics_doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(fis);
            NodeList xml_characteristics = characteristics_doc.getElementsByTagName("tab");
            createEffectMap(xml_effect_list);
            createTierMaps(xml_pool_list, xml_frameworks);
            createCharacteristicsList(xml_characteristics);
        } catch (Exception e) {
            writeError("loading initial xml files", e);
            e.printStackTrace();
        }
    }

    private void createEffectMap(NodeList xml) {
        effect_map = new HashMap<String, NodeList>();
        String id;
        for (int i = 0; i < xml.getLength(); i++) {
            id = xml.item(i).getAttributes().getNamedItem("id").getNodeValue();
            effect_map.put(id, xml.item(i).getChildNodes());
        }
    }

    /**
	 * parses the system string xml and populates a HashMap with it
	 */
    private void createStringMap(NodeList xml) {
        if (system_strings == null) system_strings = new HashMap<String, HashMap<String, String>>();
        NamedNodeMap attributes;
        String name = "";
        HashMap<String, String> items;
        for (int i = 0; i < xml.getLength(); i++) {
            items = new HashMap<String, String>();
            attributes = xml.item(i).getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                if (attributes.item(j).getNodeName().equals("name")) {
                    name = attributes.item(j).getNodeValue();
                } else {
                    items.put(attributes.item(j).getNodeName(), cleanUmlaute(attributes.item(j).getNodeValue()));
                }
            }
            if (!system_strings.containsKey(name)) system_strings.put(name, items); else {
                for (String key : items.keySet()) {
                    if (!system_strings.get(name).containsKey(key)) system_strings.get(name).put(key, items.get(key));
                }
            }
        }
    }

    private void createTierMaps(NodeList pools, NodeList frameworks) {
        power_contributes_to = new HashMap<String, HashSet<String>>();
        power_builds_on = new HashMap<String, String>();
        power_archtype = new HashMap<String, String>();
        HashSet<String> set;
        Node pool_item, subpool_item, powertype_item, power_item, framework_item;
        NodeList pool_children, subpool_children, powertype_children;
        String pool_id, subpool_id, power_id, archtype;
        HashMap<String, String> pool_archtype = new HashMap<String, String>();
        for (int i = 0; i < frameworks.getLength(); i++) {
            framework_item = frameworks.item(i);
            if (framework_item.getAttributes().getNamedItem("pool") != null && framework_item.getAttributes().getNamedItem("archtype") != null) {
                pool_id = framework_item.getAttributes().getNamedItem("pool").getNodeValue();
                archtype = framework_item.getAttributes().getNamedItem("archtype").getNodeValue();
                pool_archtype.put(pool_id, archtype);
            }
        }
        for (int i = 0; i < pools.getLength(); i++) {
            pool_item = pools.item(i);
            pool_children = pool_item.getChildNodes();
            pool_id = pool_item.getAttributes().getNamedItem("id").getNodeValue();
            for (int j = 0; j < pool_children.getLength(); j++) {
                subpool_item = pool_children.item(j);
                if (subpool_item.getNodeName().equals("subpool")) {
                    subpool_children = subpool_item.getChildNodes();
                    subpool_id = subpool_item.getAttributes().getNamedItem("id").getNodeValue();
                    archtype = pool_archtype.get(subpool_id);
                    for (int k = 0; k < subpool_children.getLength(); k++) {
                        powertype_item = subpool_children.item(k);
                        powertype_children = powertype_item.getChildNodes();
                        for (int l = 0; l < powertype_children.getLength(); l++) {
                            power_item = powertype_children.item(l);
                            if (power_item.getNodeName().equals("power")) {
                                power_id = power_item.getAttributes().getNamedItem("id").getNodeValue();
                                set = new HashSet<String>();
                                set.add(pool_id);
                                set.add(subpool_id);
                                try {
                                    if (power_item.getAttributes().getNamedItem("pool").getNodeValue().equals("true")) {
                                        power_builds_on.put(power_id, pool_id);
                                    }
                                } catch (NullPointerException e) {
                                    power_builds_on.put(power_id, subpool_id);
                                }
                                if (power_contributes_to.containsKey(power_id)) {
                                    set.addAll(power_contributes_to.get(power_id));
                                }
                                power_contributes_to.put(power_id, set);
                                if (archtype != null) {
                                    power_archtype.put(power_id, archtype);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void createCharacteristicsList(NodeList xml) {
        NodeList children, tab_children;
        LinkedList<LinkedList<LinkedList<HashMap<String, String>>>> tab_list = new LinkedList<LinkedList<LinkedList<HashMap<String, String>>>>();
        LinkedList<LinkedList<HashMap<String, String>>> list;
        LinkedList<HashMap<String, String>> sublist;
        HashMap<String, String> map;
        for (int l = 0; l < xml.getLength(); l++) {
            tab_children = xml.item(l).getChildNodes();
            list = new LinkedList<LinkedList<HashMap<String, String>>>();
            sublist = new LinkedList<HashMap<String, String>>();
            map = new HashMap<String, String>();
            for (int i = 0; i < xml.item(l).getAttributes().getLength(); i++) {
                map.put(xml.item(l).getAttributes().item(i).getNodeName(), xml.item(l).getAttributes().item(i).getNodeValue());
            }
            sublist.add(map);
            list.add(sublist);
            for (int i = 1; i < tab_children.getLength(); i += 2) {
                children = tab_children.item(i).getChildNodes();
                sublist = new LinkedList<HashMap<String, String>>();
                map = new HashMap<String, String>();
                for (int j = 0; j < tab_children.item(i).getAttributes().getLength(); j++) {
                    map.put(tab_children.item(i).getAttributes().item(j).getNodeName(), tab_children.item(i).getAttributes().item(j).getNodeValue());
                }
                sublist.add(map);
                for (int j = 1; j < children.getLength(); j += 2) {
                    map = new HashMap<String, String>();
                    for (int k = 0; k < children.item(j).getAttributes().getLength(); k++) {
                        map.put(children.item(j).getAttributes().item(k).getNodeName(), children.item(j).getAttributes().item(k).getNodeValue());
                    }
                    sublist.add(map);
                }
                list.add(sublist);
            }
            tab_list.add(list);
        }
        characteristics_list = tab_list;
    }

    /**
	 * logs error messages
	 */
    public void writeError(String preamble, Exception e) {
        DateFormat dateFormat = new SimpleDateFormat("MM.dd.yyyy HH:mm:ss");
        Date date = new Date();
        String default_path = javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath().replace("\\", "/") + "/";
        app_path = AppPath.get();
        String path;
        new File(app_path + "temp").mkdir();
        if (new File(app_path + "temp").exists()) path = app_path + "data/"; else path = default_path + "championBuilder/";
        new File(app_path + "temp").delete();
        path = app_path + "data/";
        try {
            FileWriter file_writer = new FileWriter(path + "error.txt", true);
            PrintWriter print_writer = new PrintWriter(file_writer);
            print_writer.println(dateFormat.format(date) + " -- " + preamble);
            e.printStackTrace(print_writer);
            print_writer.println();
            print_writer.println();
            file_writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
	 * write to a file
	 */
    public void writeFile(File f, String s) {
        try {
            FileWriter file_writer = new FileWriter(f);
            file_writer.write(s);
            file_writer.close();
        } catch (IOException e) {
            writeError("writing to " + f.getAbsolutePath(), e);
        }
    }

    /**
	 * read a file and return its content
	 */
    public String readFile(File f) {
        String input = "";
        try {
            FileInputStream fstream = new FileInputStream(f);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (br.ready()) {
                input += br.readLine() + "\n";
            }
            if (input.length() > 1) input = input.substring(0, input.length() - 1);
            br.close();
            in.close();
            fstream.close();
        } catch (FileNotFoundException e) {
            writeError("reading from " + f.getAbsolutePath(), e);
        } catch (IOException e) {
            writeError("reading from " + f.getAbsolutePath(), e);
        }
        return input;
    }

    public boolean deleteFile(File f) {
        return f.delete();
    }

    /**
	 * returns the singleton
	 */
    public static SystemLink getInstance() {
        if (instance == null) instance = new SystemLink();
        return instance;
    }

    public LinkedList<LinkedList<LinkedList<HashMap<String, String>>>> getCharacteristics() {
        return characteristics_list;
    }

    public String translate(String s) {
        HashMap<String, String> translation_map = getString(s);
        String translation = "nooooooo, don't hack the language preference...", language = PrefLink.getInstance().getLanguage();
        if (translation_map.containsKey(language)) translation = translation_map.get(language);
        if (!translation_map.containsKey(language) || translation.equals("")) translation = translation_map.get("english");
        return translation;
    }

    public String translateCapitalized(String s) {
        return capitalizeString(translate(s));
    }

    private String cleanUmlaute(String s) {
        String[][] umlaute = { { "(ae)", "�" }, { "(Ae)", "�" }, { "(oe)", "�" }, { "(Oe)", "�" }, { "(ue)", "�" }, { "(Ue)", "�" }, { "(e aigu)", "�" }, { "(E aigu)", "�" }, { "(e grave)", "�" }, { "(E grave)", "�" }, { "(a grave)", "�" }, { "(A grave)", "�" }, { "(cedille)", "�" }, { "(Cedille)", "C" }, { "(e circonflex)", "�" }, { "(E circonflex)", "�" }, { "(a circonflex)", "�" }, { "(A circonflex)", "�" }, { "(i circonflex)", "�" }, { "(I circonflex)", "�" }, { "(i grave)", "�" }, { "(I grave)", "�" }, { "(o grave)", "�" }, { "(O grave)", "�" }, { "(o aigu)", "�" }, { "(O aigu)", "�" }, { "(u grave)", "�" }, { "(U grave)", "�" } };
        for (String[] u : umlaute) {
            s = s.replace(u[0], u[1]);
        }
        return s;
    }

    public HashMap<String, String> getString(String s) {
        if (system_strings.containsKey(s)) return system_strings.get(s); else {
            System.out.println("translation error: " + s);
            return system_strings.get("translation_error");
        }
    }

    public HashSet<String> getPoolsById(String id) {
        if (power_contributes_to.containsKey(id)) return power_contributes_to.get(id); else return new HashSet<String>();
    }

    public String getBuiltOnPoolById(String id) {
        if (power_builds_on.containsKey(id)) return power_builds_on.get(id); else return "";
    }

    public String getArchtypeById(String id) {
        if (power_archtype.containsKey(id)) return power_archtype.get(id); else return "";
    }

    public NodeList getEffectById(String id) {
        return effect_map.get(id);
    }

    public NodeList getPools() {
        return xml_subpool_list;
    }

    public NodeList getPowers() {
        return xml_power_list;
    }

    public NodeList getStats() {
        return xml_stat_list;
    }

    public NodeList getTalents() {
        return xml_talent_list;
    }

    public NodeList getProgression() {
        return xml_build;
    }

    public NodeList getFrameworks() {
        return xml_frameworks;
    }

    /**
	 * capitalizes the first letter of each word
	 */
    public String capitalizeString(String s) {
        String result = "";
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 || s.substring(i - 1, i).equals(" ")) result += s.substring(i, i + 1).toUpperCase(); else result += s.substring(i, i + 1);
        }
        return result;
    }
}
