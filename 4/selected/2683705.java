package rez.utils;

import java.io.*;
import java.awt.*;

/**
 *
 * @author  chris
 * 
 */
public class FileHandler extends Frame {

    private List list;

    private TextField infoarea;

    private Panel buttons;

    private Button parent, quit, catalog;

    private FilenameFilter filter;

    private File cwd;

    private String[] entries;

    private boolean performCatalog = false;

    public FileHandler(String directory, FilenameFilter filter) throws IOException {
        super("File FileHandler");
        this.filter = filter;
        list = new List(12, false);
        infoarea = new TextField();
        infoarea.setEditable(false);
        buttons = new Panel();
        parent = new Button("Up Directory");
        quit = new Button("Quit");
        catalog = new Button("Catalog");
        buttons.add(parent);
        buttons.add(catalog);
        buttons.add(quit);
        this.add("Center", list);
        this.add("South", infoarea);
        this.add("North", buttons);
        this.setSize(550, 350);
        this.setVisible(true);
        list_directory(directory);
    }

    public void list_directory(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.isDirectory()) throw new IllegalArgumentException("FileHandler: no such directory");
        list.removeAll();
        cwd = dir;
        this.setTitle(directory);
        entries = cwd.list(filter);
        BufferedWriter fileList = new BufferedWriter(new FileWriter("fileList.txt"));
        System.out.println("entries length " + entries.length);
        for (int i = 0; i < entries.length; i++) {
            File f = new File(entries[i]);
            if (!f.isDirectory()) {
                fileList.write(entries[i] + "\n");
            }
        }
        fileList.close();
        performCatalog = false;
        for (String entry : entries) {
            list.add(entry);
        }
    }

    public void doCatalog(String directory) throws IOException {
        File dir = new File(directory);
        if (!dir.isDirectory()) throw new IllegalArgumentException("FileHandler: no such directory");
        cwd = dir;
        entries = cwd.list(filter);
        System.out.println("entries length " + entries.length);
        BufferedWriter fileList = new BufferedWriter(new FileWriter("fileList.txt"));
        for (int i = 0; i < entries.length; i++) {
            File f = new File(entries[i]);
            if (!f.isDirectory()) {
                fileList.write(entries[i] + "\n");
            }
        }
        fileList.close();
        performCatalog = false;
    }

    public void show_info(String filename) throws IOException {
        File f = new File(cwd, filename);
        String info;
        if (!f.exists()) throw new IllegalArgumentException("FileHandler.show_info(): " + "no such file or directory");
        if (f.isDirectory()) info = "Directory: "; else info = "File: ";
        info += filename + " ";
        info += (f.canRead() ? "read " : "    ") + (f.canWrite() ? "write " : "    ") + f.length() + " " + new java.util.Date(f.lastModified());
        infoarea.setText(info);
    }

    @Override
    public void processEvent(AWTEvent e) {
        if (e.getSource() == quit) System.exit(0); else if (e.getSource() == parent) {
            String localParent = cwd.getParent();
            if (localParent == null) localParent = "/";
            try {
                list_directory(localParent);
            } catch (IllegalArgumentException ex) {
                infoarea.setText("Already at top");
            } catch (IOException ex) {
                infoarea.setText("I/O Error");
            }
        } else if (e.getSource() == catalog) {
            try {
                doCatalog(".");
            } catch (IllegalArgumentException ex) {
                infoarea.setText("Already at top");
            } catch (IOException ex) {
                infoarea.setText("I/O Error");
            }
        } else if (e.getSource() == list) {
            if (e.getID() == Event.LIST_SELECT) {
                try {
                    show_info(entries[((Integer) e.getSource()).intValue()]);
                } catch (IOException ex) {
                    infoarea.setText("I/O Error");
                }
            } else if (e.getID() == Event.ACTION_EVENT) {
                try {
                    String item = new File(cwd, (String) e.getSource()).getAbsolutePath();
                    list_directory(item);
                } catch (IllegalArgumentException ex) {
                    infoarea.setText("Illegal argument");
                } catch (IOException ex) {
                    infoarea.setText("I/O Error");
                }
            }
        }
    }

    public static void usage() {
        System.out.println("Usage: java FileHandler [directory_name]" + "[-e file_extension]");
        System.exit(0);
    }

    public static void main(String args[]) throws IOException {
        FileHandler f;
        FilenameFilter filter = null;
        String directory = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-e")) {
                i++;
                if (i >= args.length) usage();
                filter = new EndsWithFilter(args[i]);
            } else {
                if (directory != null) usage(); else directory = args[i];
            }
        }
        if (directory == null) directory = System.getProperty("user.dir");
        f = new FileHandler(directory, filter);
    }
}
