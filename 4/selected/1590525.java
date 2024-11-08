package shellkk.qiq.gui.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import shellkk.qiq.gui.Form;
import shellkk.qiq.jdm.engine.uriaccess.ArffDataAccessObject;
import shellkk.qiq.jdm.engine.uriaccess.IURIDataAccessObject;
import shellkk.qiq.jdm.engine.uriaccess.URIDataAccessEngine;

public class ArffManagement implements Form {

    protected URIDataAccessEngine engine;

    protected String uriPrefix;

    protected String rootPath;

    protected ArffDataAccessObject selectedDataSource;

    protected File selectedFile;

    protected List<File> files = new ArrayList();

    protected InputStream uploadStream;

    protected String uploadFileName;

    public String getName() {
        return "arffMgmt";
    }

    public void onClose() {
    }

    public void onOpen() throws Exception {
    }

    public List<ArffDataAccessObject> getDataSources() {
        ArrayList<ArffDataAccessObject> all = new ArrayList();
        for (IURIDataAccessObject ds : engine.getDaos()) {
            if (ds instanceof ArffDataAccessObject) {
                all.add((ArffDataAccessObject) ds);
            }
        }
        return all;
    }

    public void loadFiles() {
        files.clear();
        selectedFile = null;
        File root = new File(selectedDataSource.getRootPath());
        File[] fileArray = root.listFiles();
        for (File file : fileArray) {
            if (file.isFile()) {
                files.add(file);
            }
        }
    }

    public void addDataSource() {
        if (uriPrefix != null && rootPath != null) {
            ArffDataAccessObject ds = new ArffDataAccessObject();
            ds.setPrefix(uriPrefix);
            ds.setRootPath(rootPath);
            File root = new File(rootPath);
            root.mkdirs();
            engine.getDaos().add(ds);
        }
    }

    public boolean isDataSourceSelected() {
        return selectedDataSource != null;
    }

    public void removeDataSource() {
        engine.getDaos().remove(selectedDataSource);
        selectedDataSource = null;
        files.clear();
        selectedFile = null;
    }

    public boolean isDataLoadable() {
        return selectedFile != null && !isDataLoaded(selectedFile);
    }

    public boolean isDataUnloadable() {
        return selectedFile != null && isDataLoaded(selectedFile);
    }

    public boolean isFileSelected() {
        return selectedFile != null;
    }

    public boolean isDataLoaded(File file) {
        String uri = selectedDataSource.getPrefix();
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        uri += file.getName();
        return engine.getTable().containsKey(uri);
    }

    public void loadData() throws Exception {
        if (selectedDataSource != null && selectedFile != null) {
            String uri = selectedDataSource.getPrefix();
            if (!uri.endsWith("/")) {
                uri += "/";
            }
            uri += selectedFile.getName();
            engine.requestDataLoad(uri);
        }
    }

    public void unloadData() throws Exception {
        if (selectedDataSource != null && selectedFile != null) {
            String uri = selectedDataSource.getPrefix();
            if (!uri.endsWith("/")) {
                uri += "/";
            }
            uri += selectedFile.getName();
            engine.requestDataUnload(uri);
        }
    }

    public void uploadFile() throws IOException {
        if (selectedDataSource != null) {
            int index = uploadFileName.lastIndexOf("\\");
            if (index < 0) {
                index = uploadFileName.lastIndexOf("/");
            }
            String name = uploadFileName.substring(index + 1);
            String path = selectedDataSource.getRootPath() + "/" + name;
            File file = new File(path);
            FileOutputStream out = null;
            try {
                byte[] buffer = new byte[1024];
                int read;
                out = new FileOutputStream(file);
                while ((read = uploadStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            } catch (IOException e) {
                throw e;
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            loadFiles();
        }
    }

    public void deleteFile() {
        selectedFile.delete();
        files.remove(selectedFile);
        selectedFile = null;
    }

    public URIDataAccessEngine getEngine() {
        return engine;
    }

    public void setEngine(URIDataAccessEngine engine) {
        this.engine = engine;
    }

    public String getUriPrefix() {
        return uriPrefix;
    }

    public void setUriPrefix(String uriPrefix) {
        this.uriPrefix = uriPrefix;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public ArffDataAccessObject getSelectedDataSource() {
        return selectedDataSource;
    }

    public void setSelectedDataSource(ArffDataAccessObject selectedDataSource) {
        this.selectedDataSource = selectedDataSource;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    public InputStream getUploadStream() {
        return uploadStream;
    }

    public void setUploadStream(InputStream uploadStream) {
        this.uploadStream = uploadStream;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public List<File> getFiles() {
        return files;
    }
}
