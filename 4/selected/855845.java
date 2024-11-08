package ch.oxinia.webdav.davcommander;

import java.io.*;
import javax.swing.JOptionPane;

public class LocalFileManager extends AbstractFileManager {

    public LocalFileManager(String userAgent) {
        super(userAgent);
    }

    public void setRemoteFileManager(RemoteFileManager remoteFileManager) {
        this.remoteFileManager = remoteFileManager;
    }

    public RemoteFileManager getRemoteFileManager() {
        return remoteFileManager;
    }

    @Override
    protected AbstractMenuListener createMenuListener() {
        return new LocalMenuListener();
    }

    @Override
    protected AbstractTreeView createTreeView() {
        return new LocalTreeView();
    }

    @Override
    protected AbstractFileView createFileView() {
        return new LocalFileView(this);
    }

    @Override
    protected AbstractToolBar createToolbar() {
        return new LocalToolBar();
    }

    @Override
    protected void initConnection(String userAgent) {
    }

    @Override
    protected AbstractPathBox createPathBox() {
        return new LocalPathBox();
    }

    @Override
    public void setLogging(boolean logging, String logFilename) {
    }

    @Override
    protected String getDefaultName(String fileName) {
        if (fileName.length() > 1 && fileName.endsWith(File.separator)) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        return fileName;
    }

    @Override
    protected String getDisplayFileName(String fileName) {
        return getDefaultName(fileName);
    }

    protected void deleteDocument(String fileName) {
        AbstractTreeNode n = fileView.getParentNode();
        File f = new File(fileName);
        if (!deleteDocument(f)) {
            GlobalData.getGlobalData().errorMsg("Delete error on local filesystem.");
        }
        treeView.refreshChildren(n);
    }

    private boolean deleteDocument(File f) {
        try {
            if (f.isDirectory()) {
                String[] flist = f.list();
                for (int i = 0; i < flist.length; i++) {
                    String documentInDirectory = f.getPath() + File.separator + flist[i];
                    if (!deleteDocument(new File(documentInDirectory))) {
                        return false;
                    }
                }
                return f.delete();
            } else {
                return f.delete();
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    protected void copyDocument(String fileName, String newName) {
        boolean success = false;
        File fOld = new File(fileName);
        if (fOld.isDirectory()) {
            success = copyDirectory(fOld, newName);
        } else {
            success = copyFile(fOld, newName);
        }
        if (success) {
            AbstractTreeNode parentNode = fileView.getParentNode();
            if (newName.contains("..")) {
                AbstractTreeNode upperNode = (AbstractTreeNode) parentNode.getParent();
                upperNode.setHasLoadedChildren(false);
            }
            treeView.refreshChildren(parentNode);
        } else {
            GlobalData.getGlobalData().errorMsg("Copy error on local filesystem.");
        }
    }

    private boolean copyDirectory(File directory, String newName) {
        File newDirectory = new File(newName);
        newDirectory.mkdir();
        String[] dirList = directory.list();
        for (int i = 0; i < dirList.length; i++) {
            String documentInDirectory = directory.getPath() + File.separator + dirList[i];
            File f = new File(documentInDirectory);
            if (f.isDirectory()) {
                copyDirectory(f, newName + File.separator + f.getName());
            } else {
                copyFile(f, newName + File.separator + f.getName());
            }
        }
        return true;
    }

    private boolean copyFile(File fOld, String newName) {
        try {
            File fNew = new File(newName);
            InputStream in = new FileInputStream(fOld);
            OutputStream out = new FileOutputStream(fNew);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    protected void moveDocument(String fileName, String newName) {
        File fOld = new File(fileName);
        File fNew = new File(newName);
        if (fOld.renameTo(fNew)) {
            AbstractTreeNode parentNode = fileView.getParentNode();
            if (newName.contains("..")) {
                AbstractTreeNode upperNode = (AbstractTreeNode) parentNode.getParent();
                upperNode.setHasLoadedChildren(false);
            }
            treeView.refreshChildren(parentNode);
        } else {
            GlobalData.getGlobalData().errorMsg("Move error on local filesystem.");
        }
    }

    @Override
    protected void refreshView() {
        treeView.refreshChildren(fileView.getParentNode());
    }

    @Override
    protected void viewProperties(String fileName) {
    }

    @Override
    protected void createCollection(String dirName) {
        boolean isAbsolute = false;
        if (GlobalData.getGlobalData().isWindows()) {
            isAbsolute = dirName.length() > 1 && dirName.charAt(1) == ':';
        } else {
            isAbsolute = dirName.length() > 0 && dirName.charAt(0) == File.separatorChar;
        }
        String path = "";
        if (!isAbsolute) {
            path += treeView.getCurrentPath();
            if (!path.endsWith(String.valueOf(File.separatorChar))) {
                path += File.separatorChar;
            }
        }
        path += dirName;
        File f = new File(path);
        f.mkdir();
        treeView.refreshChildren(fileView.getParentNode());
    }

    @Override
    protected void openDirectory(String pathName) {
        if (fileView.openDirectory(pathName)) {
            return;
        }
        treeView.addRowToRoot(pathName, false);
    }

    @Override
    protected void openRoot() {
        if (GlobalData.getGlobalData().isWindows()) {
            String parentPath = fileView.getParentPath();
            openDirectory(parentPath.substring(0, 3));
        } else {
            openDirectory(File.separator);
        }
    }

    @Override
    protected void pathUp() {
        String parentPath = fileView.getParentPath();
        if (parentPath.endsWith(File.separator)) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }
        int separatorIndex = parentPath.lastIndexOf(File.separator);
        if (separatorIndex != -1) {
            parentPath = parentPath.substring(0, separatorIndex);
        }
        int rootLength = GlobalData.getGlobalData().isWindows() ? 3 : 1;
        if (parentPath.length() < rootLength) {
            parentPath += File.separator;
        }
        openDirectory(parentPath);
    }

    @Override
    protected int getSiteLocation() {
        return LOCATION_LOCAL;
    }

    @Override
    protected void viewDocument() {
        JOptionPane.showMessageDialog(this, "Not implemented!");
    }

    @Override
    protected boolean isOverwriteOk(String fileName, String displayFileName, String title) {
        boolean overwriteOk = true;
        File existingFile = new File(fileName);
        if (existingFile != null && existingFile.exists()) {
            int option = JOptionPane.showConfirmDialog(this, "A file with the name " + displayFileName + " already exists.\n" + "Do you want to overwrite it?", title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            overwriteOk = (option == JOptionPane.YES_OPTION);
        }
        return overwriteOk;
    }

    class LocalMenuListener extends AbstractMenuListener {
    }

    protected RemoteFileManager remoteFileManager;
}
