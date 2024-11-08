package it.yacme.csi.templates.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TemplateZipper {

    private String filename;

    private String basepath;

    private String savepath = null;

    private String extractpath = null;

    private ZipOutputStream zipf;

    private byte[] buffer = new byte[1024];

    public TemplateZipper() {
    }

    /**
	 * Costruttore della classe per creare il documento in formato ODT
	 * 
	 * @param sPath  Directory dove salvare il file
	 */
    public TemplateZipper(String sPath) {
        this.savepath = sPath;
    }

    public void setExtractPath(String ePath) {
        this.extractpath = ePath;
    }

    public void setSavePath(String sPath) {
        this.savepath = sPath;
    }

    public void setBasePath(String bPath) {
        this.basepath = bPath;
    }

    public void setFilename(String fName) {
        this.filename = fName;
    }

    /**
	 * 
	 * @param entryFile
	 * @throws Exception
	 */
    private void addFileEntry(FileInputStream entryFile) throws Exception {
        int len;
        while ((len = entryFile.read(buffer)) != -1) zipf.write(buffer, 0, len);
        entryFile.close();
    }

    /**
	 * 
	 * @param fileList
	 * @param path
	 * @param rpath
	 * @throws Exception
	 */
    private void addDirEntry(String[] fileList, String path, String rpath) throws Exception {
        int i;
        File fin;
        String[] subfilelist;
        ZipEntry entry;
        String fname, zentry;
        for (i = 0; i < fileList.length; i++) {
            if (fileList[i].equalsIgnoreCase("ZipStore")) continue;
            fname = path + "/" + fileList[i];
            if (rpath == null) {
                zentry = fileList[i];
            } else {
                zentry = rpath + fileList[i];
            }
            fin = new File(fname);
            if (fin.isDirectory()) {
                subfilelist = fin.list();
                addDirEntry(subfilelist, fname, zentry + "/");
            } else {
                if (!(zentry.endsWith("odt"))) {
                    entry = new ZipEntry(zentry);
                    zipf.putNextEntry(entry);
                    addFileEntry(new FileInputStream(fin));
                    zipf.closeEntry();
                }
            }
        }
    }

    /**
	 */
    public void doZip() throws Exception {
        File fdir;
        File fdat;
        String[] filelist;
        fdat = new File(this.savepath + File.separator + this.filename);
        if (fdat.exists()) {
            fdat.delete();
        }
        fdir = new File(basepath);
        filelist = fdir.list();
        zipf = new ZipOutputStream(new FileOutputStream(this.savepath + File.separator + this.filename));
        addDirEntry(filelist, basepath, null);
        zipf.close();
    }

    public void extractZip(byte[] file) throws Exception {
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream = null;
        ZipEntry zipentry;
        zipinputstream = new ZipInputStream(new ByteArrayInputStream(file));
        new File(extractpath).mkdirs();
        zipentry = zipinputstream.getNextEntry();
        while (zipentry != null) {
            String entryName = zipentry.getName();
            int n;
            FileOutputStream fileoutputstream;
            File newFile = new File(entryName);
            String directory = newFile.getParent();
            if (directory != null) {
                new File(extractpath + File.separator + directory).mkdirs();
            }
            fileoutputstream = new FileOutputStream(extractpath + File.separator + entryName);
            while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
            fileoutputstream.close();
            zipinputstream.closeEntry();
            zipentry = zipinputstream.getNextEntry();
        }
        zipinputstream.close();
    }

    public byte[] getZip() throws Exception {
        RandomAccessFile bis = new RandomAccessFile(new File(this.savepath + File.separator + this.filename), "r");
        byte[] ret = new byte[(int) bis.length()];
        bis.read(ret);
        bis.close();
        return ret;
    }
}
