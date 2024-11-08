package org.kwanta.ksync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class SyncFolder {

    private File folder;

    Map<String, FileInfo> fileInfos;

    FileInfo[] files;

    private SyncFolder parallel;

    private OPTION option = OPTION.COPY_NEW;

    private OPTION parallelOption = OPTION.COPY_NEW;

    private SELECT overwriteUpToDate = SELECT.NONE;

    private SELECT overwriteNew = SELECT.NONE;

    public enum OPTION {

        COPY_NEW, UPDATE_ONLY, DELETE, NOCHANGE
    }

    public enum SELECT {

        NONE, THIS, PARALLEL
    }

    /**
     * 
     * Private constructor used to create a parallel SyncFolder
     *
     * @param folder the folder for the SyncFolder
     * @param option Syncronisation for the new Folder
     * @param parallel the parallel SyncFolder
     */
    private SyncFolder(File folder, OPTION option, SyncFolder parallel) {
        if (folder != null && folder.isDirectory()) this.folder = folder;
        this.option = option;
        this.parallel = parallel;
        switch(parallel.overwriteUpToDate) {
            case THIS:
                this.overwriteUpToDate = SELECT.PARALLEL;
                break;
            case PARALLEL:
                this.overwriteUpToDate = SELECT.THIS;
        }
        switch(parallel.overwriteNew) {
            case THIS:
                this.overwriteNew = SELECT.PARALLEL;
                break;
            case PARALLEL:
                this.overwriteNew = SELECT.THIS;
        }
    }

    /**
     * 
     * Standard Constructor
     *
     * @param folder the folder
     * @param option the option for this folder
     * @param parallel the parallel folder
     * @param paralelOption the option for the parallel folder
     * @param overwriteUpDoDate tells if up-to-date files get
     * overwritten at this OR at the parallel folder
     */
    public SyncFolder(File folder, OPTION option, File parallel, OPTION parallelOption, SELECT overwriteUpToDate, SELECT overwriteNew) throws NotAFolderException {
        if (folder != null && folder.isDirectory()) this.folder = folder; else throw new NotAFolderException();
        this.option = option;
        this.overwriteUpToDate = overwriteUpToDate;
        this.overwriteNew = overwriteNew;
        this.parallelOption = parallelOption;
        this.parallel = new SyncFolder(parallel, parallelOption, this);
    }

    /**
     * 
     * Synchronizes this SyncFolder with it's parallel SyncFolder
     *
     * @param recursive if true, Subfolders will also be
     * synchronized
     */
    public void sync(boolean recursive) {
        initFiles();
        parallel.initFiles();
        syncThis(recursive);
        parallel.syncThis(recursive);
    }

    /**
     * 
     * Does the actual Syncing work
     *
     * @param recursive if true, Subfolders will also be
     * synchronized
     */
    private void syncThis(boolean recursive) {
        for (int i = 0; i < files.length; i++) {
            FileInfo f = files[i];
            if (f.file.isDirectory() && recursive) syncSubFolder(f); else syncFile(f);
        }
    }

    /**
     * 
     * Synchronizes a subfolder
     *
     * @param f the FileInfo which holds the Folder
     */
    private void syncSubFolder(FileInfo f) {
        if (!f.synched) {
            FileInfo p;
            try {
                p = parallel.getFileInfo(f.name);
            } catch (FileNotFoundException e) {
                if (option == OPTION.DELETE) {
                    f.file.delete();
                    return;
                } else if (parallel.option == OPTION.COPY_NEW || parallel.option == OPTION.DELETE) {
                    p = new FileInfo(new File(parallel.folder, f.name));
                    parallel.createFolder(p);
                } else return;
            }
            try {
                SyncFolder sf = new SyncFolder(f.file, option, p.file, parallelOption, overwriteUpToDate, overwriteNew);
                sf.sync(true);
            } catch (NotAFolderException e) {
            }
            if (p != null) p.check();
            folder.mkdir();
            f.check();
        }
    }

    /**
	 * creates a new folder
	 * 
	 * @param p parallel folder
	 */
    private void createFolder(FileInfo f) {
        if (option == OPTION.COPY_NEW || option == OPTION.DELETE) {
            f.file.mkdir();
            fileInfos.put(f.name, f);
        }
    }

    /**
     * 
     * Synchronizes a file
     *
     * @param f the FileInfo which holds the file
     */
    private void syncFile(FileInfo f) {
        if (!f.synched) {
            FileInfo p;
            try {
                p = parallel.getFileInfo(f.name);
                if (f.lastModified < p.lastModified) if (this.overwriteNew == SELECT.PARALLEL) parallel.writeFile(p, f); else this.writeFile(f, p); else if (f.lastModified > p.lastModified) if (this.overwriteNew != SELECT.THIS) this.writeFile(f, p); else parallel.writeFile(p, f); else if (f.lastModified == p.lastModified && overwriteUpToDate != SELECT.NONE) if (overwriteUpToDate == SELECT.THIS) this.writeFile(f, p); else parallel.writeFile(p, f);
                p.check();
            } catch (FileNotFoundException e) {
                if (option == OPTION.DELETE) {
                    f.file.delete();
                } else {
                    p = parallel.writeNewFile(f);
                    if (p != null) p.check();
                }
            }
            f.check();
        }
    }

    /**
     * 
     * writes a file depending on the options
     *
     * @param local File in this SyncFolder which gets
     * overwritten
     * @param paralle File in the parallel SyncFolder which
     * will be copied
     */
    private void writeFile(FileInfo local, FileInfo parallel) {
        if (option == OPTION.COPY_NEW || option == OPTION.UPDATE_ONLY || option == OPTION.DELETE) copy(local, parallel);
    }

    /**
     * 
     * writes a new file depending on the options
     *
     * @param p File in the Parallel SyncFolder to copy
     */
    private FileInfo writeNewFile(FileInfo p) {
        if (option == OPTION.COPY_NEW || option == OPTION.DELETE) {
            FileInfo f = new FileInfo(new File(folder, p.name));
            try {
                f.file.createNewFile();
                copy(p, f);
            } catch (IOException e) {
            }
            return p;
        }
        return null;
    }

    /**
     * 
     * copies a file
     *
     * @param inputFile the File to copy
     * @param outputFile the File write into
     */
    private void copy(FileInfo inputFile, FileInfo outputFile) {
        try {
            FileReader in = new FileReader(inputFile.file);
            FileWriter out = new FileWriter(outputFile.file);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
            outputFile.file.setLastModified(inputFile.lastModified);
        } catch (IOException e) {
        }
    }

    /**
     * 
     * reads all the files of this Folder into
     * member arrays
     */
    private void initFiles() {
        if (files == null) {
            File[] temp = folder.listFiles();
            fileInfos = new HashMap<String, FileInfo>();
            files = new FileInfo[temp.length];
            for (int i = 0; i < temp.length; i++) {
                FileInfo file = new FileInfo(temp[i]);
                files[i] = file;
                fileInfos.put(file.name, file);
            }
        }
    }

    /**
     * 
     * returns the requested FileInfo
     *
     * @param inputFile the Name of the File
     */
    private FileInfo getFileInfo(String name) throws FileNotFoundException {
        FileInfo f = fileInfos.get(name);
        if (f == null) throw new FileNotFoundException();
        return f;
    }
}
