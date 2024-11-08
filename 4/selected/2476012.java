package sijapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Vector;

public class Sijapp {

    public static String JAVASRC_EXT = ".java";

    public static String LANG_EXT = ".lang";

    private File srcDir;

    private File destDir;

    private String[] filenames;

    public Sijapp(File srcDir, File destDir) {
        this.srcDir = new File(srcDir.getPath());
        this.destDir = new File(destDir.getPath());
        this.filenames = this.scanDir(this.srcDir, "");
    }

    private String[] scanDir(File srcDir, String srcDirExt) {
        Vector filenames = new Vector();
        File[] files = (new File(srcDir, srcDirExt)).listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile() && (files[i].getName().endsWith(Sijapp.JAVASRC_EXT) || files[i].getName().endsWith(Sijapp.LANG_EXT))) {
                filenames.add(srcDirExt + File.separator + files[i].getName());
            } else if (files[i].isDirectory()) {
                filenames.addAll(Arrays.asList(this.scanDir(srcDir, srcDirExt + File.separator + files[i].getName())));
            }
        }
        String[] ret = new String[filenames.size()];
        filenames.copyInto(ret);
        return (ret);
    }

    public void run(Preprocessor pp) throws SijappException {
        for (int i = 0; i < this.filenames.length; i++) {
            File srcFile = new File(this.srcDir, this.filenames[i]);
            BufferedReader reader;
            try {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(srcFile), "CP1251");
                reader = new BufferedReader(isr);
            } catch (Exception e) {
                throw (new SijappException("File " + srcFile.getPath() + " could not be read"));
            }
            File destFile = new File(this.destDir, this.filenames[i]);
            BufferedWriter writer;
            try {
                (new File(destFile.getParent())).mkdirs();
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(destFile), "CP1251");
                writer = new BufferedWriter(osw);
            } catch (Exception e) {
                throw (new SijappException("File " + destFile.getPath() + " could not be written"));
            }
            try {
                pp.run(reader, writer);
            } catch (SijappException e) {
                try {
                    reader.close();
                } catch (IOException f) {
                }
                try {
                    writer.close();
                } catch (IOException f) {
                }
                try {
                    destFile.delete();
                } catch (SecurityException f) {
                }
                throw (new SijappException(srcFile.getPath() + ":" + e.getMessage()));
            }
            try {
                reader.close();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
    }
}
