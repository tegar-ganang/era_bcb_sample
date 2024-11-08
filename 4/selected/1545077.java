package ade;

import ade.ADEServerInfo;
import ade.ADEGlobals;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;
import java.util.regex.*;
import java.rmi.registry.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.security.AccessControlException;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;

/** The graphical front end to the {@link ADEServerInfo ADEServerInfo} class. */
public class ADEServerInfoGui extends JFrame implements ActionListener {

    private static String prg = "ADEServerInfoGui";

    private static String title = "ADE Server Information";

    private static boolean debug = false;

    ADEGuiServerImpl gui;

    RegexFormatter maskSmallInt, maskIP, maskIPs, maskDir, maskRootDir, maskFile, maskNoSpaces;

    String ip;

    HashSet<String> onlyonhosts;

    int port;

    String type;

    String name;

    HashSet<String> userAccess;

    HashSet<String> requiredDevices;

    int conns;

    int hb;

    String com;

    String dir;

    String[] args;

    int restarts;

    String conf;

    int numfields = 14;

    public ADEServerInfoGui(ADEGuiServerImpl ags, ADEServerInfo si) {
        gui = ags;
        maskSmallInt = new RegexFormatter("(\\d)+");
        maskIP = new RegexFormatter("([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})");
        maskIPs = new RegexFormatter("((\\d)\\.(\\d)\\.(\\d)\\.(\\d) )*");
        maskDir = new RegexFormatter("((\\w)+" + File.separator + ")+");
        maskRootDir = new RegexFormatter(File.separator + "((\\w)+" + File.separator + ")*");
        maskFile = new RegexFormatter("((\\w)*" + File.separator + ")*(\\w)+");
        maskNoSpaces = new RegexFormatter("\\S+");
        if (si == null) setDefaults(); else dupServerInfo(si);
    }

    private void setDefaults() {
        ip = new String(gui.getHostMe());
        onlyonhosts = new HashSet<String>();
        port = Registry.REGISTRY_PORT;
        type = new String();
        name = new String();
        userAccess = new HashSet<String>();
        userAccess.add(ADEGlobals.ALL_ACCESS);
        requiredDevices = new HashSet<String>();
        conns = ADEGlobals.DEF_MAXCONN;
        hb = ADEGlobals.DEF_HBPULSE;
        com = new String();
        dir = new String();
        args = new String[] { new String() };
        restarts = ADEGlobals.DEF_RESTARTS;
        conf = new String();
    }

    private void dupServerInfo(ADEServerInfo si) {
        ip = new String(si.host);
        if (si.onlyonhosts == null) onlyonhosts = new HashSet<String>(); else onlyonhosts = new HashSet<String>(si.onlyonhosts);
        port = si.port;
        type = new String("FestivalServerImpl");
        name = new String("FestivalServerImpl1");
        userAccess = new HashSet<String>(si.userAccess);
        if (si.requiredDevices == null) requiredDevices = new HashSet<String>(); else requiredDevices = new HashSet<String>(si.requiredDevices);
        conns = si.connectsAllowed;
        hb = si.heartBeatPeriod;
        dir = new String(si.startdirectory);
        if (si.additionalargs != null) {
            args = si.additionalargs.split(" ");
        } else {
            args = new String[] { new String(" ") };
        }
        restarts = si.numrestarts;
        if (si.configfile != null) conf = new String(si.configfile); else conf = new String();
    }

    private String strArrayToStr(String[] sa) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sa.length; i++) {
            sb.append(sa[i]);
            sb.append(" ");
        }
        return sb.toString();
    }

    public ADEServerInfo makeServerInfo() {
        HashSet<String> acc = new HashSet<String>();
        acc.add(ADEGlobals.ALL_ACCESS);
        HashSet<String> devs = new HashSet<String>();
        ADEServerInfo si = new ADEServerInfo(ip, onlyonhosts, port, type, name, conns, hb, acc, devs, dir, null, null, restarts, conf, null, false);
        for (String arg : args) {
            si.addAdditionalarg(arg);
        }
        si.addAdditionalarg("--uilogging");
        return si;
    }

    public boolean showGuiInfo(boolean enabled) {
        LinkedHashMap<String, Object> itemMap = makeItems(enabled);
        Object[] items = itemMap.values().toArray();
        final JDialog jf = new JDialog(gui.mainframe, true);
        int sel = JOptionPane.CANCEL_OPTION;
        JButton button;
        if (debug) System.out.println(prg + ": in showGuiInfo(" + enabled + ")");
        jf.setTitle(title);
        jf.setResizable(false);
        JPanel jp = new JPanel(new GridLayout(numfields + 1, 2));
        jf.setContentPane(jp);
        for (int i = 0; i < items.length; i++) {
            jp.add((JComponent) items[i]);
        }
        class ButtonPress implements ActionListener {

            public String pressed = "None";

            public void actionPerformed(ActionEvent e) {
                pressed = ((JButton) e.getSource()).getText();
                jf.setVisible(false);
                jf.dispose();
            }
        }
        ButtonPress bp = new ButtonPress();
        jp.add(button = new JButton("OK"));
        button.addActionListener(bp);
        jp.add(button = new JButton("Cancel"));
        button.addActionListener(bp);
        jf.pack();
        jf.setVisible(true);
        if (bp.pressed.equals("OK")) {
            storeInfo(itemMap);
            if (conf.replaceAll("(\\s)+", "").equals("")) conf = null;
            return true;
        }
        return false;
    }

    private void setDefaultNameField(String classname) {
        String friendlyName = classname.substring(classname.lastIndexOf('.') + 1);
        ((JFormattedTextField) (JTextField) items.get("name")).setValue(friendlyName);
    }

    public void actionPerformed(ActionEvent e) {
        setDefaultNameField((String) ((JComboBox) e.getSource()).getSelectedItem());
    }

    LinkedHashMap<String, Object> items = null;

    private LinkedHashMap<String, Object> makeItems(boolean enabled) {
        items = new LinkedHashMap((numfields * 2) + 7);
        JLabel jl;
        jl = new JLabel("Host IP:");
        items.put("ipl", jl);
        JFormattedTextField tfip = mkfield(maskIP, ip);
        items.put("ip", tfip);
        jl = new JLabel("Only run on hosts:");
        items.put("onlyonhostsl", jl);
        JFormattedTextField tfonlyonhosts = mkfield(maskIPs, onlyonhosts);
        items.put("onlyonhosts", tfonlyonhosts);
        jl = new JLabel("Host Port:");
        items.put("portl", jl);
        JFormattedTextField tfport = mkfield(maskSmallInt, new Integer(port));
        items.put("port", tfport);
        jl = new JLabel("ADE Server type:");
        items.put("typel", jl);
        JFormattedTextField tftype = mkfield(maskNoSpaces, type);
        items.put("type", tftype);
        ArrayList<String> servers = new ServerLister().getServers("com");
        Collections.sort(servers);
        JComboBox serverList = new JComboBox(servers.toArray());
        if (servers.size() > 0) {
            name = servers.get(0);
            serverList.addActionListener(this);
        }
        items.put("type", serverList);
        jl = new JLabel("ADE Server name:");
        items.put("namel", jl);
        JFormattedTextField tfname = mkfield(maskNoSpaces, name);
        items.put("name", tfname);
        jl = new JLabel("ADE Server access list:");
        items.put("userAccessl", jl);
        JTextField tfuserAccess = new JTextField(strArrayToStr(userAccess.toArray(args)));
        items.put("acc", tfuserAccess);
        jl = new JLabel("Required device interfaces:");
        items.put("requiredDevicesl", jl);
        JTextField tfrequiredDevices = new JTextField(strArrayToStr(requiredDevices.toArray(args)));
        items.put("requiredDevices", tfrequiredDevices);
        jl = new JLabel("Maximum Connections:");
        items.put("connsl", jl);
        JFormattedTextField tfconns = mkfield(maskSmallInt, new Integer(conns));
        items.put("conns", tfconns);
        jl = new JLabel("Heartbeat Period:");
        items.put("hbl", jl);
        JFormattedTextField tfhb = mkfield(maskSmallInt, new Integer(hb));
        items.put("hb", tfhb);
        jl = new JLabel("Startup Command:");
        items.put("coml", jl);
        JTextField tfcom = new JTextField(com);
        items.put("com", tfcom);
        jl = new JLabel("Startup Directory:");
        items.put("dirl", jl);
        JFormattedTextField tfdir = mkfield(maskRootDir, dir);
        items.put("dir", tfdir);
        jl = new JLabel("Additional Startup Arguments:");
        items.put("argsl", jl);
        JTextField tfargs = new JTextField(strArrayToStr(args));
        items.put("args", tfargs);
        jl = new JLabel("Restarts:");
        items.put("restartsl", jl);
        JFormattedTextField tfrestarts = mkfield(maskSmallInt, new Integer(restarts));
        items.put("restarts", tfrestarts);
        jl = new JLabel("Configuration File:");
        items.put("confl", jl);
        JFormattedTextField tfconf = mkfield(maskFile, conf);
        items.put("conf", tfconf);
        return items;
    }

    private JFormattedTextField mkfield(RegexFormatter format, Object val) {
        return (new JFormattedTextField(format.factory(), val));
    }

    private void storeInfo(LinkedHashMap<String, Object> info) {
        if (debug) System.out.println(prg + ": in storeInfo");
        Field[] fds = this.getClass().getDeclaredFields();
        Field fd = null;
        JTextField obj = null;
        for (int i = 0; i < fds.length; i++) {
            fd = fds[i];
            try {
                if (fd.getName() == "type") {
                    String s = (String) ((JComboBox) info.get(fd.getName())).getSelectedItem();
                    System.out.println(s);
                    fd.set(this, s);
                } else if ((obj = (JTextField) info.get(fd.getName())) != null) {
                    if (obj instanceof JFormattedTextField) {
                        if (debug) System.out.println("\tSetting " + fd.getName() + " to " + ((JFormattedTextField) obj).getValue());
                        fd.set(this, ((JFormattedTextField) obj).getValue());
                    } else {
                        if (debug) System.out.println("\tSetting " + fd.getName() + " to " + obj.getText());
                        if (fd.getName().equals("args")) {
                            fd.set(this, obj.getText().split(" "));
                        } else if (fd.getName().equals("userAccess") || fd.getName().equals("onlyonhosts") || fd.getName().equals("requiredDevices")) {
                            HashSet<String> tmp = new HashSet<String>();
                            String[] tmpsarr = obj.getText().split(" ");
                            for (String tmps : tmpsarr) tmp.add(tmps);
                            fd.set(this, tmp);
                        } else {
                            fd.set(this, obj.getText());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(prg + ": Error setting value of " + fd.getName());
            }
        }
    }

    class RegexFormatter extends DefaultFormatter {

        Matcher m;

        public RegexFormatter(String pat) {
            if (debug) System.out.println("Constructing RegexFormatter: pattern is " + pat);
            Pattern p = Pattern.compile(pat);
            m = p.matcher("");
        }

        public Object stringToValue(String str) throws java.text.ParseException {
            if (str == null) return null;
            m.reset(str);
            if (!m.matches()) throw new java.text.ParseException("Does not match", 0);
            return super.stringToValue(str);
        }

        public DefaultFormatterFactory factory() {
            return (new DefaultFormatterFactory(this));
        }
    }

    /**
@author Jack Harris
*/
    private class ServerLister {

        public ArrayList<String> getServers(String directoryName) {
            String filesep = System.getProperty("file.separator");
            ArrayList<String> servers = new ArrayList<String>();
            File dir = new File(directoryName);
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (new File(directoryName + filesep + children[i]).list() != null) {
                        for (String s : new ServerLister().getServers(directoryName + filesep + children[i])) {
                            servers.add(s);
                        }
                    }
                    if (children[i].length() > 9) {
                        String n = children[i].substring(0, children[i].length() - 5);
                        String ext = children[i].substring(children[i].length() - 9);
                        if (ext.equals("Impl.java") && new File(directoryName + filesep + n + ".class").exists()) {
                            Pattern p = Pattern.compile("abstract[\n\t ]+public[\n\t ]+class[\n\t ]+" + n, Pattern.MULTILINE);
                            String fileName = children[i];
                            Pattern patMainMethod = Pattern.compile("public[\n\t ]+static[\n\t ]+void[\n\t ]main", Pattern.MULTILINE);
                            File f = new File(directoryName + filesep + fileName);
                            try {
                                FileInputStream fis = new FileInputStream(f);
                                FileChannel fc = fis.getChannel();
                                ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
                                Charset cs = Charset.forName("8859_1");
                                CharsetDecoder cd = cs.newDecoder();
                                CharBuffer cb = cd.decode(bb);
                                Matcher m = p.matcher(cb);
                                Matcher m2 = patMainMethod.matcher(cb);
                                boolean keepLooking = false;
                                if (m2.find()) {
                                    keepLooking = true;
                                }
                                if (keepLooking && m.find()) {
                                    keepLooking = false;
                                }
                                if (keepLooking) {
                                    servers.add(directoryName.replace(filesep, ".") + "." + n);
                                }
                            } catch (Exception e) {
                                System.out.println("ServerLister Error: " + e.toString());
                            }
                        }
                    }
                }
            }
            return servers;
        }

        public ServerLister() {
        }
    }
}
