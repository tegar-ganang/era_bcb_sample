package org.one.stone.soup.xapp.swing.components;

import java.awt.Component;
import java.io.File;
import java.util.Hashtable;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;
import org.one.stone.soup.file.FileHelper;
import org.one.stone.soup.swing.SimpleDirFilter;
import org.one.stone.soup.swing.SimpleFileFilter;
import org.one.stone.soup.xapp.XappPersister;
import org.one.stone.soup.xapp.filebrowser.XappFileBrowser;
import org.one.stone.soup.xml.XmlElement;
import org.one.stone.soup.xml.helper.XmlHelper;

/**
 * Insert the type's description here.
 * Creation date: (16/05/03 17:26:55)
 * @author:
 */
public class XappSwingFileBrowser implements XappPersister, XappFileBrowser {

    private static Hashtable lastDirs = new Hashtable();

    private String fileType;

    private String fileMask;

    private String title;

    private Component owner;

    private FileSystemView view = null;

    private String alias;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
 * Insert the method's description here.
 * Creation date: (13/02/04 11:50:23)
 * @param owner java.awt.Frame
 */
    public XappSwingFileBrowser(String title, String fileTypeDescription, String fileExtension) {
        this.owner = null;
        this.fileType = fileTypeDescription;
        this.fileMask = fileExtension;
        this.title = title;
    }

    public void setFileExtension(String ext) {
        fileMask = ext;
    }

    public void setFileType(String type) {
        fileType = type;
    }

    public void transferDirectory(String title) {
        String lastDir = (String) lastDirs.get(title);
        if (lastDir != null) {
            lastDirs.put(this.title, lastDir);
        }
    }

    /**
 * Insert the method's description here.
 * Creation date: (13/02/04 12:11:48)
 * @return java.lang.String
 */
    public String getOpenFile() {
        return selectFileOpen(title, fileType, fileMask);
    }

    public String getOpenDir() {
        String dir = (String) lastDirs.get(title);
        if (dir == null) {
            dir = System.getProperty("user.dir");
            lastDirs.put(title, dir);
        }
        return dir;
    }

    public String getOpenDirectory() {
        return selectOpenDirectory(title);
    }

    public String getSaveAsFile(String currentName) {
        return selectFileSaveAs(title, fileType, currentName);
    }

    public String getSaveDir() {
        String dir = (String) lastDirs.get(title);
        if (dir == null) {
            dir = getOpenDir();
        }
        return dir;
    }

    public String getSaveDirectory() {
        return selectSaveDirectory(title);
    }

    public java.lang.String getTitle() {
        return title;
    }

    private String selectFileOpen(String title, String fileType, String mask) {
        File lastDirFile = new File(getOpenDir());
        JFileChooser fd = null;
        if (view == null) {
            fd = new JFileChooser(lastDirFile);
        } else {
            fd = new JFileChooser(lastDirFile, view);
        }
        fd.setDialogTitle("Open " + title);
        fd.setFileFilter(new SimpleFileFilter(mask, fileType));
        File result = null;
        if (fd.showOpenDialog(owner) != JFileChooser.CANCEL_OPTION) {
            result = fd.getSelectedFile();
        }
        String fileName = null;
        if (result != null) {
            fileName = org.one.stone.soup.file.FileHelper.getPath(result.getPath());
            fileName += result.getName();
            lastDirs.put(title, FileHelper.getPath(result.getPath()));
        }
        return fileName;
    }

    private String selectFileSaveAs(String title, String fileType, String newFileName) {
        File lastDirFile = new File(getSaveDir());
        if (newFileName != null) {
            lastDirFile = new File(getSaveDir() + "/" + newFileName);
        } else {
            lastDirFile = new File(getSaveDir() + "/untitled." + fileType);
        }
        JFileChooser fd = null;
        if (view == null) {
            fd = new JFileChooser();
        } else {
            fd = new JFileChooser(view);
        }
        fd.setDialogTitle("Save As " + title);
        fd.setSelectedFile(lastDirFile);
        File result = null;
        if (fd.showSaveDialog(owner) != JFileChooser.CANCEL_OPTION) {
            result = fd.getSelectedFile();
        }
        String fileName = null;
        if (result != null) {
            if (result.exists()) {
                if (JOptionPane.showConfirmDialog(owner, "The file " + result.getName() + " alread exists. Do you wish to Overwrite it?") != JOptionPane.YES_OPTION) {
                    return null;
                }
            }
            fileName = org.one.stone.soup.file.FileHelper.getPath(result.getPath());
            fileName += result.getName();
            lastDirs.put(title, FileHelper.getPath(result.getPath()));
        }
        return fileName;
    }

    private String selectOpenDirectory(String title) {
        File lastDirFile = new File(getOpenDir());
        JFileChooser fd = null;
        if (view == null) {
            fd = new JFileChooser(lastDirFile);
        } else {
            fd = new JFileChooser(lastDirFile, view);
        }
        fd.setDialogTitle(title);
        fd.setFileFilter(new SimpleDirFilter());
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fd.showDialog(owner, "Select") == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        String fileName = fd.getSelectedFile().getPath();
        if (fileName == null) {
            return null;
        }
        lastDirs.put(title, fileName);
        return fileName;
    }

    private String selectSaveDirectory(String title) {
        File lastDirFile = new File(getSaveDir());
        JFileChooser fd = null;
        if (view == null) {
            fd = new JFileChooser(lastDirFile);
        } else {
            fd = new JFileChooser(lastDirFile, view);
        }
        fd.setDialogTitle(title);
        fd.setFileFilter(new SimpleDirFilter());
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File result = null;
        if (fd.showDialog(owner, "Select") == JFileChooser.CANCEL_OPTION) {
            result = fd.getSelectedFile();
        }
        String fileName = null;
        if (result != null) {
            fileName = result.getPath();
            lastDirs.put(title, fileName);
        }
        return fileName;
    }

    public void setOpenDir(String dir) {
        lastDirs.put(title, dir);
    }

    public void setSaveDir(String dir) {
        lastDirs.put(title, dir);
    }

    public void setTitle(java.lang.String newTitle) {
        title = newTitle;
    }

    public void setFileSystemView(FileSystemView view) {
        this.view = view;
    }

    public XmlElement getData() {
        return XmlHelper.getHashtableAsElement("fileBrowser", lastDirs);
    }

    public void setData(XmlElement data) {
        lastDirs = XmlHelper.getElementAsHashtable(data, "key");
    }
}
