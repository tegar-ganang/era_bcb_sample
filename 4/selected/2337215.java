package org.edits.models;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.edits.EditsModule;
import org.edits.FileTools;
import org.edits.EditsModuleLoader;
import org.edits.definition.EditsOption;
import org.edits.definition.ModuleDefinition;
import org.edits.definition.ObjectFactory;

/**
 * 
 * @author Milen Kouylekov
 * 
 */
public abstract class EDITSModel extends EditsModule {

    public void serialize(String filename, String tempdir, boolean overwrite) throws Exception {
        String date = UUID.randomUUID().toString() + "/";
        updateOptions();
        tempdir = tempdir + date;
        new File(tempdir).mkdir();
        replace(tempdir, definition());
        ObjectFactory f = new ObjectFactory();
        f.marshal(tempdir + "definition.xml", f.createModule(definition()), overwrite);
        createModelZip(filename, tempdir, overwrite);
        delete(new File(tempdir));
    }

    public static void copy(String inputFile, String outputFile) throws Exception {
        try {
            FileReader in = new FileReader(inputFile);
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (Exception e) {
            throw new Exception("Could not copy " + inputFile + " into " + outputFile + " because:\n" + e.getMessage());
        }
    }

    public static String copyFile(String path, String file, Map<String, String> renames) throws Exception {
        if (renames.containsKey(file)) return renames.get(file);
        String newfile = "" + renames.size() + new File(file).getName();
        copy(file, path + newfile);
        newfile = "${" + "tempModelDir" + "}" + newfile;
        renames.put(file, newfile);
        return newfile;
    }

    public static void createModelZip(String filename, String tempdir, boolean overwrite) throws Exception {
        FileTools.checkOutput(filename, overwrite);
        BufferedInputStream origin = null;
        FileOutputStream dest = new FileOutputStream(filename);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        int BUFFER = 2048;
        byte data[] = new byte[BUFFER];
        File f = new File(tempdir);
        for (File fs : f.listFiles()) {
            FileInputStream fi = new FileInputStream(fs.getAbsolutePath());
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(fs.getName());
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) out.write(data, 0, count);
            out.closeEntry();
            origin.close();
        }
        out.close();
    }

    public static void delete(File f) {
        for (File ff : f.listFiles()) {
            if (ff.isDirectory()) delete(ff); else ff.delete();
        }
        f.delete();
    }

    public static String getDateFormatted() {
        Calendar xcal = Calendar.getInstance();
        return new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(xcal.getTime());
    }

    public static EditsModule loadModel(String filename, String tempdir) throws Exception {
        tempdir = tempdir + "model/";
        File xx = new File(tempdir);
        if (xx.exists()) delete(xx);
        xx.mkdir();
        unzipModel(filename, tempdir);
        EditsModule c = loadModelFromFolder(tempdir);
        delete(new File(tempdir));
        return c;
    }

    public static void unzipModel(String filename, String tempdir) throws Exception {
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(filename);
            int BUFFER = 2048;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(tempdir + entry.getName());
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) dest.write(data, 0, count);
                dest.flush();
                dest.close();
            }
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Can not expand model in \"" + tempdir + "\" because:\n" + e.getMessage());
        }
    }

    private static void expandOptions(String path, ModuleDefinition def) {
        for (EditsOption o : def.getOption()) if (new File(path + o.getValue()).exists()) o.setValue(path + o.getValue());
        for (ModuleDefinition d : def.getModule()) expandOptions(path, d);
    }

    private static EditsModule loadModelFromFolder(String tempdir) throws Exception {
        ObjectFactory f = new ObjectFactory();
        String confFile = tempdir + "/definition.xml";
        ModuleDefinition def = (ModuleDefinition) f.load(confFile);
        expandOptions(tempdir, def);
        return EditsModuleLoader.loadModule(def);
    }

    private static void replace(String path, ModuleDefinition def) throws Exception {
        for (EditsOption o : def.getOption()) {
            if (o.getName().equals("training")) continue;
            if (new File(o.getValue()).exists()) {
                String newname = path + new File(o.getValue()).getName();
                copy(o.getValue(), newname);
                o.setValue(new File(newname).getName());
            }
        }
        for (ModuleDefinition d : def.getModule()) replace(path, d);
    }
}
