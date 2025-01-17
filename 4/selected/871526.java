package com.limegroup.gnutella.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Admin
 */
@SuppressWarnings("unchecked")
public class CountPercent {

    private static final int ACTION_STATISTICS = 0;

    private static final int ACTION_HTML = 1;

    private static final int ACTION_CHECK = 2;

    private static final int ACTION_UPDATE = 3;

    private static final int ACTION_RELEASE = 4;

    private static final int ACTION_NSIS = 5;

    private static final double RELEASE_PERCENTAGE = .65;

    /**
     * Launched from the console with command-line parameters.<br />
     * Usage: java CountPercent [html|check|update [<code>]|release]
     * @param args a possibly empty array of command-line parameters.
     * @throws IOException
     */
    public static void main(String[] args) throws java.io.IOException {
        final int action;
        String code = null;
        if (args != null && args.length > 0) {
            if (args[0].equals("html")) {
                action = ACTION_HTML;
            } else if (args[0].equals("check")) {
                action = ACTION_CHECK;
            } else if (args[0].equals("update")) {
                action = ACTION_UPDATE;
                if (args.length > 1) code = args[1];
            } else if (args[0].equals("release")) {
                action = ACTION_RELEASE;
            } else if (args[0].equals("nsis")) {
                action = ACTION_NSIS;
            } else {
                System.err.println("Usage: java CountPercent [html|check|update [<code>]|release|nsis]");
                return;
            }
        } else action = ACTION_STATISTICS;
        new CountPercent(action, code);
    }

    private final DateFormat df;

    private final NumberFormat rc;

    private final NumberFormat pc;

    private final Map langs;

    private final Set basicKeys, advancedKeys;

    private final int basicTotal;

    /**
     * @param action
     * @param code
     * @throws IOException
     */
    CountPercent(int action, String code) throws java.io.IOException {
        this.df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
        this.rc = NumberFormat.getNumberInstance(Locale.US);
        this.rc.setMinimumIntegerDigits(4);
        this.rc.setMaximumIntegerDigits(4);
        this.rc.setGroupingUsed(false);
        this.pc = NumberFormat.getPercentInstance(Locale.US);
        this.pc.setMinimumFractionDigits(2);
        this.pc.setMaximumFractionDigits(2);
        this.pc.setMaximumIntegerDigits(3);
        final File root = new File(".");
        final LanguageLoader loader = new LanguageLoader(root);
        final Properties defaultProps = loader.getDefaultProperties();
        this.advancedKeys = loader.getAdvancedKeys();
        defaultProps.keySet().removeAll(this.advancedKeys);
        this.basicKeys = defaultProps.keySet();
        this.basicTotal = this.basicKeys.size();
        this.langs = loader.loadLanguages();
        switch(action) {
            case ACTION_CHECK:
                checkBadKeys();
                break;
            case ACTION_STATISTICS:
                loader.extendVariantLanguages();
                loader.retainKeys(this.basicKeys);
                this.pc.setMinimumIntegerDigits(3);
                printStatistics();
                break;
            case ACTION_HTML:
                loader.extendVariantLanguages();
                loader.retainKeys(this.basicKeys);
                HTMLOutput html = new HTMLOutput(this.df, this.pc, this.langs, this.basicTotal);
                html.printHTML(System.out);
                break;
            case ACTION_RELEASE:
            case ACTION_NSIS:
            case ACTION_UPDATE:
                loader.extendVariantLanguages();
                final Set validKeys = new HashSet();
                validKeys.addAll(this.basicKeys);
                validKeys.addAll(this.advancedKeys);
                loader.retainKeys(validKeys);
                final List lines = loader.getEnglishLines();
                final LanguageUpdater updater = new LanguageUpdater(root, this.langs, lines);
                if (action == ACTION_RELEASE || action == ACTION_NSIS) updater.setSilent(true);
                if (code == null) updater.updateAllLanguages(); else {
                    LanguageInfo info = (LanguageInfo) this.langs.get(code);
                    updater.updateLanguage(info);
                }
                if (action == ACTION_RELEASE) {
                    loader.retainKeys(this.basicKeys);
                    release(root);
                } else if (action == ACTION_NSIS) {
                    loader.retainKeys(this.basicKeys);
                    nsis();
                }
                break;
        }
    }

    /**
     * Check and list extra or badly names names found in resources. Use the
     * default (English) basic and extended resource keys.
     */
    private void checkBadKeys() {
        System.out.println("List of extra or badly named resource keys:");
        System.out.println("-------------------------------------------");
        for (final Iterator i = this.langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) i.next();
            final String code = (String) entry.getKey();
            final LanguageInfo li = (LanguageInfo) entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().removeAll(this.basicKeys);
            props.keySet().removeAll(this.advancedKeys);
            if (props.size() != 0) {
                System.out.println("(" + code + ") " + li.getName() + ": " + li.getFileName());
                props.list(System.out);
                System.out.println("-------------------------------------------");
            }
        }
    }

    /**
     * Prints statistics about the number of translated resources.
     */
    private void printStatistics() {
        System.out.println("Total Number of Resources: " + this.basicTotal);
        System.out.println("---------------------------------");
        System.out.println();
        for (final Iterator i = this.langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) i.next();
            final String code = (String) entry.getKey();
            final LanguageInfo li = (LanguageInfo) entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double) count / (double) this.basicTotal;
            System.out.print("(" + code + ") " + this.pc.format(percentage) + ", count: " + this.rc.format(count) + " [" + li.getName() + ": ");
            try {
                final byte[] langName = li.toString().getBytes("UTF-8");
                System.out.write(langName, 0, langName.length);
            } catch (java.io.UnsupportedEncodingException uee) {
            }
            System.out.println("]");
        }
    }

    /**
     * Releases the properties.
     * 
     * @param root
     *            the parent directory of the "release" directory into which the
     *            released properties files will be created.
     */
    private void release(final File root) {
        List validLangs = new LinkedList();
        for (final Iterator i = this.langs.values().iterator(); i.hasNext(); ) {
            LanguageInfo li = (LanguageInfo) i.next();
            int count = li.getProperties().size();
            double percentage = (double) count / (double) this.basicTotal;
            if (percentage >= RELEASE_PERCENTAGE) validLangs.add(li);
        }
        File release = new File(root, "release");
        deleteAll(release);
        copy(root, release, new ReleaseFilter(validLangs));
    }

    /**
     * Lists all the languages that are of release-quality & have an NSIS name.
     */
    private void nsis() {
        System.out.println("English");
        for (final Iterator i = this.langs.values().iterator(); i.hasNext(); ) {
            final LanguageInfo li = (LanguageInfo) i.next();
            final int count = li.getProperties().size();
            final double percentage = (double) count / (double) this.basicTotal;
            if (percentage >= RELEASE_PERCENTAGE) {
                final String name = li.getNSISName();
                if (!name.equals("")) System.out.println(name);
            }
        }
    }

    /**
     * Recursively copies all files in root to dir that match Filter.
     * 
     * @param root
     * @param dir
     * @param filter
     */
    private void copy(final File root, final File dir, final FileFilter filter) {
        final File[] files = root.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
            final File f = files[i];
            if (f.isDirectory()) copy(f, new File(dir, f.getName()), filter); else copy(f, dir);
        }
    }

    /**
     * Recursively deletes all files in a directory.
     * 
     * @param f
     */
    private void deleteAll(File f) {
        if (f.isDirectory()) {
            final File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) deleteAll(files[i]);
        }
        f.delete();
    }

    /**
     * Copies file to dir, ignoring any lines that are comments.
     * 
     * @param src
     * @param dstDir
     */
    private void copy(final File src, final File dstDir) {
        dstDir.mkdirs();
        final File dst = new File(dstDir, src.getName());
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(src), "ISO-8859-1"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "ISO-8859-1"));
            String read;
            while ((read = reader.readLine()) != null) {
                Line line = new Line(read);
                if (line.isComment()) continue;
                writer.write(read);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                }
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Filter for releasing files.
     */
    private static class ReleaseFilter implements FileFilter {

        /**
         * Comment for <code>validLangs</code>
         */
        final List validLangs;

        /**
         * @param valid
         */
        ReleaseFilter(final List valid) {
            this.validLangs = valid;
        }

        /**
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            final String name = f.getName();
            if (!name.endsWith(".properties")) return false;
            final int idxU;
            if ((idxU = name.indexOf('_')) == -1) return true;
            final String code = name.substring(idxU + 1, name.indexOf("."));
            if (code.equals("en")) return true;
            for (final Iterator i = this.validLangs.iterator(); i.hasNext(); ) {
                LanguageInfo li = (LanguageInfo) i.next();
                if (code.equals(li.getBaseCode()) || code.equals(li.getCode())) return true;
            }
            return false;
        }
    }
}
