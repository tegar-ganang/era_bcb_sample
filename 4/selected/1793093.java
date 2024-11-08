package randres.kindle.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import randres.kindle.covers.CoversManager;
import randres.kindle.gui.tree.DisplayableDataTree;
import randres.kindle.previewer.MobiDecoder;
import randres.kindle.previewer.PreviewInfo;
import randres.kindle.synopsis.SynopsisManager;
import randres.kindle.util.PropertyHandler;

public class FileItem extends Item {

    public static final String DOC_ROOT = "/mnt/us/documents/";

    String hash;

    String absPath;

    String file;

    String title;

    PreviewInfo previewInfo;

    private String folder;

    public FileItem(String absPath, String fileRelativePath) throws NoSuchAlgorithmException {
        this.absPath = absPath;
        this.file = fileRelativePath;
        String fileComplete = DOC_ROOT + file;
        this.hash = SHA1.getHash(fileComplete);
        this.folder = PropertyHandler.getInstance().getProperty(PropertyHandler.KINDLE_SYNOPSIS_FOLDER_KEY);
        File propFile = getPropertyFile();
        FileReader fileReader = null;
        if (propFile.exists()) {
            try {
                fileReader = new FileReader(propFile);
                previewInfo = PreviewInfo.load(fileReader);
            } catch (Exception e) {
                System.out.println("Unable to load the cached PRC file info " + absPath);
                e.printStackTrace();
            } finally {
                try {
                    if (fileReader != null) {
                        fileReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            FileWriter fileWriter = null;
            try {
                MobiDecoder preview = new MobiDecoder(absPath);
                previewInfo = preview.getPreviewInfo();
                fileWriter = new FileWriter(propFile);
                previewInfo.store(fileWriter);
            } catch (Exception e) {
                System.out.println("Unable to get PRC file info " + absPath);
                e.printStackTrace();
            } finally {
                try {
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String calculateSHA1(String fileRelativePath) throws NoSuchAlgorithmException {
        String fileComplete = DOC_ROOT + fileRelativePath;
        return SHA1.getHash(fileComplete);
    }

    public String getHash() {
        return hash;
    }

    public String getName() {
        return file;
    }

    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    public String getSynopsis() {
        return SynopsisManager.getInstance().getSynopsis(this);
    }

    public boolean hasSynopsis() {
        return SynopsisManager.getInstance().hasSynopsis(this);
    }

    public String getCoverPath() {
        return CoversManager.getInstance().getCoverPath(this);
    }

    public void downloadSynopsis(DisplayableDataTree mTree) {
        SynopsisManager.getInstance().downloadSynopsis(this, mTree);
    }

    public List<String> getTags() {
        return SynopsisManager.getInstance().getTags(this);
    }

    public void downloadSynopsisBackground() {
        SynopsisManager.getInstance().downloadSynopsis(this, null);
    }

    public void deleteSynopsis(DisplayableDataTree mTree) {
        SynopsisManager.getInstance().deleteSynopsis(this, mTree);
    }

    public void downloadCover(DisplayableDataTree tree) {
        CoversManager.getInstance().downloadCover(this, tree);
    }

    public void downloadCoverBackground() {
        CoversManager.getInstance().downloadCover(this, null);
    }

    public void deleteCover(DisplayableDataTree tree) {
        CoversManager.getInstance().deleteCover(this, tree);
    }

    @Override
    public boolean hasCover() {
        return CoversManager.getInstance().hasCover(this);
    }

    public String getAbsPath() {
        return absPath;
    }

    private File getPropertyFile() {
        return new File(folder + File.separator + getHash() + ".prop");
    }

    public void exportFile() {
        String expfolder = PropertyHandler.getInstance().getProperty(PropertyHandler.KINDLE_EXPORT_FOLDER_KEY);
        File out = new File(expfolder + File.separator + previewInfo.getTitle() + ".prc");
        File f = new File(absPath);
        try {
            FileOutputStream fout = new FileOutputStream(out);
            FileInputStream fin = new FileInputStream(f);
            int read = 0;
            byte[] buffer = new byte[1024 * 1024];
            while ((read = fin.read(buffer)) > 0) {
                fout.write(buffer, 0, read);
            }
            fin.close();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getSample() {
        return previewInfo.getData(PreviewInfo.SAMPLE);
    }
}
