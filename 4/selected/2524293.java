package rabbit.installer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import rabbit.awt.*;
import rabbit.util.*;

/** This class installs the packages in its own zipfile.
 */
public class Installer extends Frame implements Runnable {

    private static final long serialVersionUID = 20050430;

    private static final String INSTALLCONFIG = "install.conf";

    private Config conf;

    private static String MAGIC = "htdocs/RabbIT2.gif";

    private static final String exs = "common,jr,restart";

    private static String[] executables = null;

    private static final String confs = "conf/rabbit.conf";

    private static String[] configs = null;

    private static final String specs = "conf/access,conf/users";

    private static String[] specials = null;

    private static final String chmds = "/bin/chmod,/usr/bin/chmod,/sbin/chmod,/usr/sbin/chmod";

    private static String[] chmods = null;

    private static String readme = "htdocs/README.txt";

    private static String logo = MAGIC;

    private static final int KEEP = 1;

    private static final int OVERWRITE = 2;

    private static final int MERGE = 3;

    private static boolean verbose = false;

    /** @serial */
    private Status status;

    /** @serial */
    private String myzipfile;

    /** @serial */
    private ZipFile myself;

    /** @serial */
    private String installdir = null;

    /** @serial */
    private Panel panel;

    /** @serial */
    private CardLayout card;

    /** @serial */
    private Meter meter;

    /** @serial */
    private TextField tf;

    /** @serial */
    private boolean installed = false;

    /** @serial */
    private Thread installer;

    /** @serial */
    private boolean sel = true;

    /** @serial */
    private int is = 0;

    /** @serial */
    private Dialog dialog = null;

    /** Start a new Installer.
     * @param args run with --verbose to get verbose output.
     */
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--verbose")) verbose = true;
        }
        new Installer();
    }

    /** Create a new Installer.
     */
    public Installer() {
        super("RabbIT installer");
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closeNicely();
            }
        });
        Properties p = System.getProperties();
        String classpath = p.getProperty("java.class.path");
        if (verbose) System.out.println("classpath: " + classpath);
        myzipfile = findMe(classpath);
        if (myzipfile == null) {
            System.err.println("Could not find myself, aborting");
            System.exit(-1);
        }
        if (verbose) System.out.println("Found myself as: " + myzipfile);
        addLogo();
        addStatus();
        addSteps();
        validate();
        pack();
        moveMiddle(this);
        setVisible(true);
        setStatus("OK");
    }

    /** Try to find the zipfile we are run from.
     * @param paths the path to search in.
     * @return the zipfile we are run from or null if not found.
     */
    protected String findMe(String paths) {
        StringTokenizer st = new StringTokenizer(paths, "" + File.pathSeparatorChar);
        while (st.hasMoreTokens()) {
            String file = st.nextToken();
            if (testFile(file)) return file;
        }
        return null;
    }

    private void loadConfig(ZipEntry ze) throws IOException {
        DataInputStream is = new DataInputStream(myself.getInputStream(ze));
        conf = new Config(is);
        String ex = conf.getProperty("", "executables", exs);
        StringTokenizer st = new StringTokenizer(ex, ",");
        int size = st.countTokens();
        executables = new String[size];
        for (int i = 0; i < size; i++) executables[i] = st.nextToken();
        ex = conf.getProperty("", "configs", confs);
        st = new StringTokenizer(ex, ",");
        size = st.countTokens();
        configs = new String[size];
        for (int i = 0; i < size; i++) configs[i] = st.nextToken();
        ex = conf.getProperty("", "specials", specs);
        st = new StringTokenizer(ex, ",");
        size = st.countTokens();
        specials = new String[size];
        for (int i = 0; i < size; i++) specials[i] = st.nextToken();
        ex = conf.getProperty("", "chmods", chmds);
        st = new StringTokenizer(ex, ",");
        size = st.countTokens();
        chmods = new String[size];
        for (int i = 0; i < size; i++) chmods[i] = st.nextToken();
        logo = conf.getProperty("", "Logo", logo);
    }

    /** Test a file and see if it is ourself.
     * @param file the filename to test.
     * @return true if this is ourself, false otherwise.
     */
    protected boolean testFile(String file) {
        try {
            if (verbose) System.out.println("checking: " + file);
            ZipFile zf = new ZipFile(file);
            ZipEntry ze = zf.getEntry(INSTALLCONFIG);
            if (ze != null) {
                myself = zf;
                loadConfig(ze);
                return true;
            }
        } catch (IOException e) {
            if (verbose) e.printStackTrace();
        }
        return false;
    }

    /** Move a frame to the middle of the screen.
     * @param f the Frame to move.
     */
    protected void moveMiddle(Window f) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        Dimension s = f.getSize();
        f.setLocation((d.width - s.width) / 2, (d.height - s.height) / 2);
    }

    /** Add the logo to the Frame.
     */
    protected void addLogo() {
        byte[] buf = null;
        try {
            ZipEntry ze = myself.getEntry(logo);
            long size = ze.getSize();
            buf = new byte[(int) size];
            DataInputStream is = new DataInputStream(myself.getInputStream(ze));
            is.readFully(buf);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageComponent c = new ImageComponent(buf);
        c.setBackground(Color.white);
        add("North", c);
    }

    /** Add the status panel to the Frame.
     */
    protected void addStatus() {
        status = new Status();
        status.setBackground(Color.black);
        status.setForeground(Color.white);
        add("South", status);
    }

    /** Add the wizard steps to the Frame.
     */
    protected void addSteps() {
        panel = new Panel();
        panel.setBackground(Color.white);
        panel.setLayout(card = new CardLayout());
        Panel pseldir = new Panel();
        tf = new TextField("/usr/local/RabbIT");
        pseldir.setLayout(new BorderLayout());
        pseldir.add("West", new Label("Select install directory: "));
        pseldir.add("Center", tf);
        Button ok = new Button("Ok");
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                startInstall();
            }
        };
        tf.addActionListener(al);
        ok.addActionListener(al);
        pseldir.add("East", ok);
        meter = new Meter();
        meter.setForeground(Color.blue);
        Panel donep = new Panel();
        donep.setLayout(new FlowLayout(FlowLayout.RIGHT));
        Button quit = new Button("Finish");
        quit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeNicely();
            }
        });
        Button readme = new Button("read README");
        readme.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                showReadme();
            }
        });
        donep.add(quit);
        donep.add(readme);
        panel.add(pseldir, "seldir");
        panel.add(meter, "meter");
        panel.add(donep, "done");
        card.first(panel);
        add("Center", panel);
    }

    /** Show the readme in a new Window.
     */
    protected void showReadme() {
        final Frame f = new Frame("README");
        readme = installdir + "/" + conf.getProperty("", "readme", readme);
        readme = readme.replace('/', File.separatorChar);
        File file = new File(readme);
        if (file.exists()) {
            String text = "";
            try {
                StringBuffer sb = new StringBuffer();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String str;
                while ((str = br.readLine()) != null) {
                    sb.append(str);
                    sb.append('\n');
                }
                br.close();
                text = sb.toString();
            } catch (IOException e) {
                text = "Could not read " + readme;
            }
            f.add(new TextArea(text));
        } else {
            f.add(new Label(readme + " was not found"));
        }
        f.validate();
        f.pack();
        f.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                f.setVisible(false);
                f.dispose();
            }
        });
        moveMiddle(f);
        f.setVisible(true);
    }

    /** Start the installation.
     */
    protected void startInstall() {
        installdir = tf.getText();
        File f = new File(installdir);
        if (!f.isAbsolute()) {
            f = new File(f.getAbsolutePath());
            installdir = f.getAbsolutePath();
        }
        if (f.exists()) {
            if (!askOk("Directory exists, use anyway?")) {
                setStatus("");
                return;
            }
        } else {
            if (!f.mkdirs()) {
                setStatus("failed to create directory: " + installdir);
                return;
            }
        }
        card.show(panel, "meter");
        setStatus("Installing...");
        installer = new Thread(this);
        installer.start();
    }

    /** Is it ok to do this?.
     * @param f the file to merge
     * @return true if it is ok, false otherwise.
     */
    public int askKeepOverWriteMerge(File f) {
        dialog = new Dialog(this, "Time to choose", true);
        dialog.setBackground(Color.white);
        dialog.add("Center", new Label("File " + f.getName() + " already exist. What to do (recomended: Merge)?"));
        Panel buttons = new Panel();
        Button k = new Button("Keep");
        Button o = new Button("OverWrite");
        Button m = new Button("Merge");
        buttons.add(k);
        buttons.add(o);
        buttons.add(m);
        dialog.add("South", buttons);
        k.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                is = KEEP;
                dialog.setVisible(false);
            }
        });
        o.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                is = OVERWRITE;
                dialog.setVisible(false);
            }
        });
        m.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                is = MERGE;
                dialog.setVisible(false);
            }
        });
        dialog.validate();
        dialog.pack();
        moveMiddle(dialog);
        dialog.setVisible(true);
        dialog.dispose();
        dialog = null;
        return is;
    }

    /** Is it ok to do this?.
     * @param text the question to ask.
     * @return true if it is ok, false otherwise.
     */
    public boolean askOk(String text) {
        dialog = new Dialog(this, text, true);
        dialog.setBackground(Color.white);
        dialog.add("Center", new Label(text));
        Panel buttons = new Panel();
        Button y = new Button("Yes");
        Button n = new Button("No");
        buttons.add(y);
        buttons.add(n);
        dialog.add("South", buttons);
        y.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sel = true;
                dialog.setVisible(false);
            }
        });
        n.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sel = false;
                dialog.setVisible(false);
            }
        });
        dialog.validate();
        dialog.pack();
        moveMiddle(dialog);
        dialog.setVisible(true);
        dialog.dispose();
        dialog = null;
        return sel;
    }

    /** Merge the given config beeing installed with the already existing one.
     * @param is the Config being installed.
     * @param f the current Config.
     */
    protected void merge(InputStream is, File f) throws IOException {
        if (verbose) System.out.println("merging config file: " + f.getName());
        Config newconfig = new Config(is);
        Config oldconfig = new Config(f);
        oldconfig.merge(newconfig);
        FileOutputStream fos = new FileOutputStream(f);
        oldconfig.save(fos, "Upgraded and merged the config at " + new Date());
    }

    /** Is this filename a config file?
     * @param name the filename to check.
     * @return true if the given name is in the list of config files, false otherwise.
     */
    protected boolean isConfig(String name) {
        for (int i = 0; i < configs.length; i++) {
            if (configs[i].equals(name)) return true;
        }
        return false;
    }

    /** Is the given filename a special file?
     * @param name the filename to check.
     * @return true if the given name is in the list of special files, false otherwise.
     */
    protected boolean isSpecial(String name) {
        for (int i = 0; i < specials.length; i++) {
            if (specials[i].equals(name)) return true;
        }
        return false;
    }

    /** Install the files.
     */
    public void run() {
        setStatus("Unpacking...");
        try {
            long size = 0;
            long currentsize = 0;
            Enumeration e = myself.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                size += ze.getSize();
            }
            e = myself.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                setStatus("unpacking: " + ze.getName());
                if (verbose) System.out.println("unpacking: " + ze.getName());
                if (ze.isDirectory()) {
                    File f = new File(installdir, ze.getName());
                    if (!f.exists()) f.mkdirs();
                    continue;
                }
                InputStream is = myself.getInputStream(ze);
                File f = new File(installdir, ze.getName());
                File p = new File(f.getParent());
                if (!p.exists()) p.mkdirs();
                if (f.exists()) {
                    if (isConfig(ze.getName())) {
                        int ret = askKeepOverWriteMerge(f);
                        switch(ret) {
                            case KEEP:
                                continue;
                            case OVERWRITE:
                                break;
                            case MERGE:
                                merge(is, f);
                                continue;
                        }
                    } else if (isSpecial(ze.getName())) {
                        boolean keep = askOk(ze.getName() + " already exists and is a special file," + "do you want to keep the current one?");
                        if (keep) continue;
                    }
                }
                FileOutputStream fos = new FileOutputStream(f);
                byte[] b = new byte[1024];
                int read = 0;
                try {
                    while ((read = is.read(b, 0, (int) (ze.getSize() > b.length ? b.length : ze.getSize()))) > 0) {
                        fos.write(b, 0, read);
                    }
                } catch (EOFException eof) {
                }
                fos.flush();
                fos.close();
                currentsize += ze.getSize();
                meter.setMeter((double) currentsize / size);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        setStatus("Making executables...");
        makeExecutable(executables);
        setStatus("Done");
        card.show(panel, "done");
        installed = true;
    }

    /** Try to make the given files executable (under unix something like 
     * &quot;chmod file&quot;
     * @param filenames an array of filenames to make executables.
     */
    public void makeExecutable(String[] filenames) {
        String chmod = null;
        for (int i = 0; i < chmods.length; i++) {
            File f = new File(chmods[i]);
            if (f.exists()) {
                chmod = chmods[i];
                if (verbose) System.out.println("chmod found as: " + chmod);
                break;
            }
        }
        if (chmod == null && verbose) {
            System.out.println("chmod not found, cant make executables");
            return;
        }
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < filenames.length; i++) {
            try {
                Process ps = rt.exec(chmod + " +x " + installdir + File.separatorChar + filenames[i]);
                try {
                    ps.waitFor();
                    if (ps.exitValue() != 0) System.err.println("chmodding failed for: " + filenames[i] + ":" + ps.exitValue());
                } catch (InterruptedException e) {
                    System.err.println("interupted in wait: " + e);
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Set the status.
     * @param status the new status text.
     */
    public void setStatus(String status) {
        this.status.setStatus(status);
        if (this.status.isVisible()) this.status.update(this.status.getGraphics());
    }

    /** Close down by cleaning things up nicely.
     */
    public void closeNicely() {
        if (installed) {
            Properties p = System.getProperties();
            String classpath = p.getProperty("java.class.path");
            int i = classpath.indexOf(myzipfile);
            if (i >= 0) {
                int length = myzipfile.length();
                if (i > 0) i--; else if (classpath.length() > length) length++;
                classpath = classpath.substring(0, i) + classpath.substring(i + length);
            }
            classpath = installdir + File.pathSeparatorChar + classpath;
            String cps = installdir + "/classpath";
            cps = cps.replace('/', File.separatorChar);
            try {
                FileOutputStream fos = new FileOutputStream(cps);
                fos.write("CLASSPATH=".getBytes());
                fos.write(classpath.getBytes());
                fos.close();
            } catch (IOException e) {
            }
            System.out.println("You should now cd to '" + installdir + "' and start the proxy with the jr script");
            System.out.println("use something like: '/usr/bin/bash jr' under unix");
        }
        System.exit(0);
    }

    /** Remove a directory (including all of its files).
     * @param dir the directory to remove.
     */
    protected void removeDir(File dir) {
        String files[] = dir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(dir, files[i]);
            if (f.isFile()) f.delete(); else if (f.isDirectory()) {
                removeDir(f);
            }
        }
        dir.delete();
    }
}
