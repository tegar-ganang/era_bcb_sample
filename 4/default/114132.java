import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 *
 * @author thibautc
 */
public class Install implements ActionListener {

    private JFrame frame = null;

    private JTextPane logPane = null;

    private JTextField homeField = null;

    private JTextField webappField = null;

    private JTextField userField = null;

    private boolean isWindows = false;

    private boolean cli = false;

    private boolean error = false;

    private static final String[] POLICY_PATH = { "/etc/tomcat6/policy.d/04webapps.policy", "/etc/tomcat5.5/policy.d/04webapps.policy", "/etc/tomcat5/policy.d/04webapps.policy", "/etc/tomcat4/policy.d/04webapps.policy" };

    public Install(String[] cmdArgs) {
        isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        cli = false;
        boolean help = false;
        for (int i = 0; i != cmdArgs.length; i++) {
            if (cmdArgs[i].equalsIgnoreCase("-cli")) {
                cli = true;
            }
            if (cmdArgs[i].equalsIgnoreCase("-h") || cmdArgs[i].equalsIgnoreCase("-help")) {
                help = true;
            }
        }
        if (!cli) {
            cli = GraphicsEnvironment.isHeadless();
        }
        if (help) {
            showHelp();
            System.exit(0);
        }
        if (cli) {
            info("System is headless(or -cli switch was passed), we will run the command line installer.");
        } else {
            info("We will run the gui install(no -cli switch passed).");
            initFrame();
        }
        checkJava();
        String home = getJotwikiHome();
        File dir = new File(home);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.exists() || !dir.canWrite()) {
            error("We cannot create / write to: " + home);
        }
        if (error) {
            return;
        }
        if (homeField != null) {
            homeField.setText(home);
        }
        boolean newInstall = !new File(dir, "VERSION.txt").exists();
        if (cli) {
            boolean ok = confirm("We are going to install jotwiki in: " + home + ". Continue?");
            if (ok) {
                String webappFolder = null;
                while (webappFolder == null || !new File(webappFolder).exists()) {
                    webappFolder = query("Please provide the path to the web application folder (Ex: /var/lib/tomcat5.5/webapps/):");
                }
                String user = null;
                if (!isWindows) {
                    user = query("Who is the web application server user(Ex: tomcat,tomcat55, root etc...)?");
                }
                runInstall(dir, webappFolder, user);
            }
        }
    }

    private void appendToPolicyFile(String policy, String policyText) throws Exception {
        FileOutputStream f = new FileOutputStream(policy, true);
        f.write(policyText.getBytes());
        f.close();
    }

    private String getPolicyText(String webappFolder, String installDir) {
        String text = "// ========== JOTWIKI Webapp Permissions ========================\n";
        text += "grant codeBase \"file:${catalina.home}/bin/tomcat-juli.jar\" {\n";
        text += "// Tomcat-Juli as  'bug', always trying to read that logging.properties, and if no permission -> crash webapp\n";
        text += "permission java.io.FilePermission \"" + webappFolder + "jotwiki/WEB-INF/classes/logging.properties\", \"read\";\n";
        text += "};\n";
        text += "// Actual jotwiki perms.\n";
        text += "grant codeBase \"file:" + webappFolder + "jotwiki/WEB-INF/-\"{\n";
        text += "//Perm to read JOTWIKI_HOME property\n";
        text += "permission java.util.PropertyPermission \"JOTWIKI_HOME\", \"read\";\n";
        text += "//Perm to read jotwiki webapp conf/property files.\n";
        text += "permission java.io.FilePermission \"" + webappFolder + "jotwiki/jotconf/*\", \"read\";\n";
        text += "//Full permissions on jotwiki data folder\n";
        text += "permission java.io.FilePermission \"" + installDir + "-\", \"read,write,delete,execute\";\n";
        text += "};\n";
        text += "// ========= End JOTWIKI ===============================\n";
        return text;
    }

    private void runInstall(File dir, String webappFolder, String user) {
        try {
            info("OK, starting the installation now.");
            info("- Creating the folder: " + dir.getAbsolutePath());
            dir.mkdirs();
            info("- Copying VERSION.txt to: " + dir.getAbsolutePath());
            copyFile(new File(dir, "VERSION.txt"), new File("VERSION.txt"));
            info("- Copying LICENCE.txt to: " + dir.getAbsolutePath());
            copyFile(new File(dir, "VERSION.txt"), new File("LICENCE.txt"));
            info("- Copying INSTALL.txt to: " + dir.getAbsolutePath());
            copyFile(new File(dir, "VERSION.txt"), new File("INSTALL.txt"));
            info("- Copying the standard templates to: " + dir.getAbsolutePath());
            File srcDir = new File("templates");
            File destDir = new File(dir, "templates");
            destDir.mkdirs();
            copyFolderContent(destDir, srcDir, true);
            info("- Creating runsetup.txt in: " + dir.getAbsolutePath() + " since it's a new install.");
            new File(dir, "runsetup.txt").createNewFile();
            if (user != null) {
                if (user.equals("")) {
                    user = "UNSET";
                }
                info("- Updating ownership/permissions of: " + dir);
                String[] args1 = { "chmod", "-R", "750", dir.getAbsolutePath() };
                String[] args2 = { "chown", "-R", user, dir.getAbsolutePath() };
                if (!exec(args1, true, true)) {
                    error("Failed to execute: " + "chmod -R 750 " + dir.getAbsolutePath());
                }
                if (error) {
                    return;
                }
                if (!exec(args2, true, true)) {
                    error("Failed to execute: " + "chown -R " + user + " " + dir.getAbsolutePath());
                }
                if (error) {
                    return;
                }
            }
            info("- Copying the web application(jotwiki.war) to the webapp folder: " + webappFolder);
            File deployedWar = new File(webappFolder, "jotwiki.war");
            copyFile(deployedWar, new File("jotwiki.war"));
            if (user != null) {
                String[] args3 = { "chown", user, deployedWar.getAbsolutePath() };
                if (user.equals("")) {
                    user = "UNSET";
                }
                if (!exec(args3, true, true)) {
                    error("Failed to execute: " + "chown " + user + deployedWar.getAbsolutePath());
                }
                if (error) {
                    return;
                }
            }
            String policy = null;
            for (int i = 0; policy == null && i != POLICY_PATH.length; i++) {
                if (new File(POLICY_PATH[i]).exists() && new File(POLICY_PATH[i]).isFile()) {
                    policy = POLICY_PATH[i];
                }
            }
            String policyText = getPolicyText(endWithForwardSlash(webappFolder), endWithForwardSlash(dir.getAbsolutePath()));
            if (policy != null) {
                info("We found a policy file at: '" + policy + "' we will add jotwiki rules to it, since it might need them(ie: Debian Linux)");
                appendToPolicyFile(policy, policyText);
                info("----- Added to policy file: ------\n" + policyText + "\n-----------------");
            } else {
                if (!isWindows) {
                    warn("We did not find a policy file, it's probably OK, unless your OS as very strict policies in place(Linux Debian),\n in which case you will need to find the policy file yourself and add the policy text (see console).");
                    info("//-------policy text-------\n" + policyText + "\n//-----------------");
                }
            }
            warn("Installation has completed, you should now go to\nhttp://yourserver:8080/jotwiki/ (or:8180)to do the initial configuration.\nNote: you might need to restart your app server first.");
            info("Installation completed successfuly.");
        } catch (Exception e) {
            e.printStackTrace();
            error("Error: " + e.getMessage());
        }
    }

    public static String endWithForwardSlash(String str) {
        if (str != null && !str.endsWith("/")) {
            str += "/";
        }
        return str;
    }

    private void checkJava() {
        String vendor = System.getProperty("java.vendor");
        String version = System.getProperty("java.version");
        info("Found java version: " + version);
        info("Java vendor: " + vendor);
        if (vendor.toLowerCase().indexOf("sun") == -1) {
            warn("Java is not from Sun, might not work right !");
        }
    }

    private boolean confirm(String question) {
        boolean ok = false;
        System.out.println(question + " (yes/no)");
        BufferedReader reader = null;
        char c = readComandLine().charAt(0);
        if (c == 'y' || c == 'Y') {
            ok = true;
        }
        return ok;
    }

    private boolean exec(String[] command, boolean displayOut, boolean displayErr) throws Exception {
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String err = "";
        String s = null;
        while ((s = reader.readLine()) != null) {
            if (displayOut) {
                info("\t\t" + s);
            }
        }
        while ((s = reader2.readLine()) != null) {
            err += s;
            if (displayErr) {
                info("\t\t ERROR: " + s);
            }
        }
        reader.close();
        reader2.close();
        return err.length() == 0;
    }

    private void initFrame() {
        frame = new JFrame("JOTWiki Installation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        Container pane = frame.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));
        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        line1.add(new JLabel("JOTWiki Installation Folder: "));
        homeField = new JTextField(30);
        homeField.setEditable(false);
        line1.add(homeField);
        line2.add(new JLabel("WebApp server webapps(war) folder: "));
        webappField = new JTextField(20);
        webappField.setEditable(false);
        line2.add(webappField);
        JButton button = new JButton("Browse...");
        button.setActionCommand("browse");
        button.addActionListener(this);
        line2.add(button);
        topPane.add(line1);
        topPane.add(line2);
        if (!isWindows) {
            JPanel line3 = new JPanel(new FlowLayout(FlowLayout.CENTER));
            line3.add(new JLabel("WebApp server user name(Ex: tomcat): "));
            userField = new JTextField(10);
            line3.add(userField);
            topPane.add(line3);
        }
        JButton installButton = new JButton("Start Installation");
        installButton.setActionCommand("install");
        installButton.addActionListener(this);
        topPane.add(installButton);
        pane.add(topPane);
        logPane = new JTextPane();
        logPane.setSize(600, 200);
        logPane.setEditable(false);
        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setPreferredSize(new Dimension(600, 200));
        pane.add(scroll);
        frame.setVisible(true);
    }

    private String readComandLine() {
        String line = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            line = reader.readLine();
        } catch (Exception e) {
        }
        return line;
    }

    private String query(String question) {
        String s = null;
        System.out.println(question);
        return readComandLine();
    }

    private String getJotwikiHome() {
        String home = System.getProperty("JOTWIKI_HOME");
        if (home == null) {
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                home = "c:\\jotwiki\\";
            } else {
                home = "/opt/jotwiki/";
            }
            info("The 'JOTWIKI_HOME' property is not set so we will use the default folder: " + home);
        } else {
            info("The 'JOTWIKI_HOME' property variable was found: " + home);
        }
        return home;
    }

    private void info(String string) {
        if (logPane == null) {
            System.out.println(string);
        } else {
            logPane.setText(logPane.getText() + string + "\n");
        }
    }

    private void warn(String string) {
        if (logPane == null) {
            System.err.println("WARNING: " + string);
        } else {
            logPane.setText(logPane.getText() + "WARNING: " + string + "\n");
            JOptionPane.showMessageDialog(frame, string);
        }
    }

    private void error(String string) {
        error = true;
        if (logPane == null) {
            System.err.println("ERROR: " + string);
            System.exit(-1);
        } else {
            logPane.setText(logPane.getText() + "ERROR: " + string + "\n");
            JOptionPane.showMessageDialog(frame, "There was an error !\n" + string);
        }
    }

    private void showHelp() {
        System.out.println("Available switches:");
        System.out.println("\t -cli \t Forces the installer to run in command line mode (no graphic installer).");
        System.out.println("\t -h -help \t Displays this help.");
        System.out.println();
    }

    public static void copyFile(java.io.File dest, java.io.File src) throws FileNotFoundException, IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            if (dest.getAbsolutePath().equals(src.getAbsolutePath())) {
                return;
            }
            input = new FileInputStream(src);
            output = new FileOutputStream(dest);
            int size = (int) src.length();
            byte buffer[] = new byte[size];
            input.read(buffer, 0, size);
            output.write(buffer, 0, size);
        } finally {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.flush();
                output.close();
            }
        }
    }

    public static void copyFolderContent(File newFolder, File oldFolder, boolean recurse) throws Exception {
        File[] content = oldFolder.listFiles();
        if (content != null) {
            for (int i = 0; i != content.length; i++) {
                String targetPath;
                File newFile;
                targetPath = newFolder.getAbsolutePath() + File.separator + content[i].getName();
                newFile = new File(targetPath);
                if (content[i].isDirectory()) {
                    if (recurse) {
                        newFile.mkdirs();
                        copyFolderContent(newFile, content[i], recurse);
                    }
                } else {
                    copyFile(newFile, content[i]);
                }
            }
        }
    }

    public static void main(String[] args) {
        new Install(args);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("browse")) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int val = chooser.showOpenDialog(frame.getContentPane());
            if (val == JFileChooser.APPROVE_OPTION) {
                webappField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getActionCommand().equals("install")) {
            String user = null;
            if (!isWindows) user = userField.getText();
            String wapp = webappField.getText();
            String dir = homeField.getText();
            runInstall(new File(dir), wapp, user);
        }
    }
}
