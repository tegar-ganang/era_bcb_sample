package eu.more.localstorage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Properties;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import eu.more.localstorage.generated.CopyAllDataToOtherFolder;
import eu.more.localstorage.generated.CopyAllDataToOtherFolderResponse;
import eu.more.localstorage.generated.CreateFolder;
import eu.more.localstorage.generated.CreateFolderResponse;
import eu.more.localstorage.generated.DataValue;
import eu.more.localstorage.generated.DeleteAllDataFromFolder;
import eu.more.localstorage.generated.DeleteAllDataFromFolderResponse;
import eu.more.localstorage.generated.DeleteFolder;
import eu.more.localstorage.generated.DeleteFolderResponse;
import eu.more.localstorage.generated.GetDataInFolderByKey;
import eu.more.localstorage.generated.GetDataInFolderByKeyResponse;
import eu.more.localstorage.generated.GetNumberOfDataKeys;
import eu.more.localstorage.generated.GetNumberOfDataKeysResponse;
import eu.more.localstorage.generated.GetNumberOfSubFolders;
import eu.more.localstorage.generated.GetNumberOfSubFoldersResponse;
import eu.more.localstorage.generated.GetRootFolderResponse;
import eu.more.localstorage.generated.ListDataKeysInFolder;
import eu.more.localstorage.generated.ListDataKeysInFolderResponse;
import eu.more.localstorage.generated.ListSubFolders;
import eu.more.localstorage.generated.ListSubFoldersResponse;
import eu.more.localstorage.generated.MoreLocalStorage;
import eu.more.localstorage.generated.RenameFolder;
import eu.more.localstorage.generated.RenameFolderResponse;
import eu.more.localstorage.generated.StoreDataInFolderByKey;
import eu.more.localstorage.generated.impl.CopyAllDataToOtherFolderResponseImpl;
import eu.more.localstorage.generated.impl.CreateFolderResponseImpl;
import eu.more.localstorage.generated.impl.DataValueImpl;
import eu.more.localstorage.generated.impl.DeleteAllDataFromFolderResponseImpl;
import eu.more.localstorage.generated.impl.DeleteFolderResponseImpl;
import eu.more.localstorage.generated.impl.GetDataInFolderByKeyResponseImpl;
import eu.more.localstorage.generated.impl.GetNumberOfDataKeysResponseImpl;
import eu.more.localstorage.generated.impl.GetNumberOfSubFoldersResponseImpl;
import eu.more.localstorage.generated.impl.GetRootFolderResponseImpl;
import eu.more.localstorage.generated.impl.ListDataKeysInFolderResponseImpl;
import eu.more.localstorage.generated.impl.ListSubFoldersResponseImpl;
import eu.more.localstorage.generated.impl.RenameFolderResponseImpl;
import eu.more.localstorage.verifier.LocalStorVerify;

public class LocalStorService implements MoreLocalStorage {

    public static Properties locstorPrt = new Properties();

    public static String rootDir;

    public static final String ROOT_DIR_KEY = "LocalStorageRootDir";

    public LocalStorService() {
        super();
        try {
            String wrkDir = System.getProperty("locstore.dir") + File.separator + "LocalStorage_prt";
            locstorPrt.load(new FileInputStream(new File(wrkDir + File.separator + "LocStorage.prt")));
            rootDir = locstorPrt.getProperty(ROOT_DIR_KEY, wrkDir);
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
    }

    public CreateFolderResponse CreateFolder(DPWSContext context, CreateFolder CreateFolderInps) throws DPWSException {
        CreateFolderResponse cfRp = new CreateFolderResponseImpl();
        String createdFolder = "";
        try {
            if ((rootDir == null) || (rootDir.length() == 0)) {
                createdFolder = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = CreateFolderInps.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String createdDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(createdDir)) {
                        File cf = new File(createdDir);
                        if (!cf.exists()) {
                            cf.mkdirs();
                            createdFolder = cf.getPath();
                        } else {
                            createdFolder = LocalStorVerify.FLD_ARDY_EX;
                        }
                    } else {
                        createdFolder = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    createdFolder = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            createdFolder = tr.getMessage();
        }
        cfRp.setCreatedFolderNam(createdFolder);
        return cfRp;
    }

    public DeleteFolderResponse DeleteFolder(DPWSContext context, DeleteFolder DeleteFolderInp) throws DPWSException {
        DeleteFolderResponse dfRp = new DeleteFolderResponseImpl();
        String deletedFolder = "";
        try {
            if ((rootDir == null) || (rootDir.length() == 0)) {
                deletedFolder = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = DeleteFolderInp.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String deletedDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(deletedDir)) {
                        File cf = new File(deletedDir);
                        if (cf.exists()) {
                            cf.delete();
                            deletedFolder = deletedDir;
                        } else {
                            deletedFolder = LocalStorVerify.FLD_NOT_EX;
                        }
                    } else {
                        deletedFolder = LocalStorVerify.FLD_TOOLNG;
                        ;
                    }
                } else {
                    deletedFolder = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            deletedFolder = tr.getMessage();
        }
        dfRp.setDeletedFolderNam(deletedFolder);
        return dfRp;
    }

    public RenameFolderResponse RenameFolder(DPWSContext context, RenameFolder RenameFolderInps) throws DPWSException {
        RenameFolderResponse rfRp = new RenameFolderResponseImpl();
        String renameFolder = "";
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                renameFolder = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String oldFolderN = RenameFolderInps.getOldName();
                String newFolderN = RenameFolderInps.getNewName();
                if (LocalStorVerify.isValid(oldFolderN) && LocalStorVerify.isValid(newFolderN)) {
                    String oldDir = rootDir + File.separator + oldFolderN;
                    String newDir = rootDir + File.separator + newFolderN;
                    if (LocalStorVerify.isLength(oldDir) && LocalStorVerify.isLength(newDir)) {
                        File of = new File(oldDir);
                        File nf = new File(newDir);
                        if (of.exists() && !nf.exists()) {
                            of.renameTo(nf);
                            renameFolder = nf.getPath();
                        } else {
                            renameFolder = (nf.exists() ? LocalStorVerify.FLD_ARDY_EX : LocalStorVerify.FLD_NOT_EX);
                        }
                    } else {
                        renameFolder = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    renameFolder = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            renameFolder = tr.getMessage();
        }
        rfRp.setNewFolderNam(renameFolder);
        return rfRp;
    }

    public CopyAllDataToOtherFolderResponse CopyAllDataToOtherFolder(DPWSContext context, CopyAllDataToOtherFolder CopyAllDataInps) throws DPWSException {
        CopyAllDataToOtherFolderResponse cpyRp = new CopyAllDataToOtherFolderResponseImpl();
        int hany = 0;
        String errorMsg = null;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String sourceN = CopyAllDataInps.getSourceName();
                String targetN = CopyAllDataInps.getTargetName();
                if (LocalStorVerify.isValid(sourceN) && LocalStorVerify.isValid(targetN)) {
                    String srcDir = rootDir + File.separator + sourceN;
                    String trgDir = rootDir + File.separator + targetN;
                    if (LocalStorVerify.isLength(srcDir) && LocalStorVerify.isLength(trgDir)) {
                        for (File fs : new File(srcDir).listFiles()) {
                            File ft = new File(trgDir + '\\' + fs.getName());
                            FileChannel in = null, out = null;
                            try {
                                in = new FileInputStream(fs).getChannel();
                                out = new FileOutputStream(ft).getChannel();
                                long size = in.size();
                                MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                                out.write(buf);
                            } finally {
                                if (in != null) in.close();
                                if (out != null) out.close();
                                hany++;
                            }
                        }
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
            hany = (-1);
        }
        if (errorMsg != null) {
        }
        cpyRp.setNum(hany);
        return cpyRp;
    }

    public DeleteAllDataFromFolderResponse DeleteAllDataFromFolder(DPWSContext context, DeleteAllDataFromFolder DeleteAllDataInp) throws DPWSException {
        DeleteAllDataFromFolderResponse daffRp = new DeleteAllDataFromFolderResponseImpl();
        int hany = 0;
        String errorMsg = null;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = DeleteAllDataInp.getDeletedName();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        for (File fd : new File(srcDir).listFiles()) {
                            fd.delete();
                            hany++;
                        }
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
            hany = (-1);
        }
        if (errorMsg != null) {
        }
        daffRp.setNum(hany);
        return daffRp;
    }

    public ListSubFoldersResponse ListSubFolders(DPWSContext context, ListSubFolders ListSubFoldersInp) throws DPWSException {
        ListSubFoldersResponse lsfRp = new ListSubFoldersResponseImpl();
        String subFolders = "";
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                subFolders = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = ListSubFoldersInp.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        for (File sf : new File(srcDir).listFiles()) {
                            if (sf.isDirectory()) {
                                subFolders = subFolders + sf.getName() + ";";
                            }
                        }
                    } else {
                        subFolders = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    subFolders = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            subFolders = tr.getMessage();
        }
        lsfRp.setListSubFoldersResp(subFolders);
        return lsfRp;
    }

    public GetRootFolderResponse GetRootFolder(DPWSContext context) throws DPWSException {
        GetRootFolderResponse grfRp = new GetRootFolderResponseImpl();
        String rootFolder = "";
        if ((rootDir == null) || (rootDir.length() == (-1))) {
            rootFolder = LocalStorVerify.ISNT_ROOTFLD;
        } else {
            rootFolder = System.getProperty("locstore.dir") + File.separator + rootDir;
        }
        grfRp.setRootFolderName(rootFolder);
        return grfRp;
    }

    @SuppressWarnings("unchecked")
    public ListDataKeysInFolderResponse ListDataKeysInFolder(DPWSContext context, ListDataKeysInFolder ListDataKeysInp) throws DPWSException {
        ListDataKeysInFolderResponse ldkRp = new ListDataKeysInFolderResponseImpl();
        List lst = ldkRp.getStringLst();
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                lst.add(LocalStorVerify.ISNT_ROOTFLD);
            } else {
                String folderN = ListDataKeysInp.getDataKeyList();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        for (File sf : new File(srcDir).listFiles()) {
                            if (!sf.isDirectory()) {
                                lst.add(sf.getName());
                            }
                        }
                    } else {
                        lst.add(LocalStorVerify.FLD_TOOLNG);
                    }
                } else {
                    lst.add(LocalStorVerify.ISNT_VALID);
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
        return ldkRp;
    }

    @SuppressWarnings("unchecked")
    public void StoreDataInFolderByKey(DPWSContext context, StoreDataInFolderByKey StoreDataInps) throws DPWSException {
        String errorMsg = null;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = StoreDataInps.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        String fileN = StoreDataInps.getDataKey();
                        List lst = StoreDataInps.getDataVal().getDataVal();
                        File destF = new File(srcDir + File.separator + fileN);
                        if (!destF.exists()) {
                            destF.createNewFile();
                        }
                        BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destF)));
                        for (Object elem : lst.toArray()) {
                            bfw.write(((Short) elem).intValue());
                        }
                        bfw.flush();
                        bfw.close();
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
        }
        if (errorMsg != null) {
        }
    }

    @SuppressWarnings("unchecked")
    public GetDataInFolderByKeyResponse GetDataInFolderByKey(DPWSContext context, GetDataInFolderByKey GetDataInps) throws DPWSException {
        GetDataInFolderByKeyResponse gdbkRp = new GetDataInFolderByKeyResponseImpl();
        String errorMsg = null;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = GetDataInps.getName(), line;
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        DataValue dv = new DataValueImpl();
                        String fileN = GetDataInps.getDataKey();
                        File sorcF = new File(srcDir + File.separator + fileN);
                        if (sorcF.exists()) {
                            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sorcF));
                            short read;
                            while ((read = (short) bis.read()) != -1) {
                                dv.getDataVal().add(read);
                            }
                            bis.close();
                            gdbkRp.setDataVal(dv);
                        }
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
        }
        if (errorMsg != null) {
        }
        return gdbkRp;
    }

    public GetNumberOfSubFoldersResponse GetNumberOfSubFolders(DPWSContext context, GetNumberOfSubFolders GetNumFolderInp) throws DPWSException {
        GetNumberOfSubFoldersResponse gnsRp = new GetNumberOfSubFoldersResponseImpl();
        String errorMsg = null;
        int hany = 0;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = GetNumFolderInp.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        for (File sf : new File(srcDir).listFiles()) {
                            if (sf.isDirectory()) {
                                hany++;
                            }
                        }
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
            hany = (-1);
        }
        if (errorMsg != null) {
        }
        gnsRp.setNum(hany);
        return gnsRp;
    }

    public GetNumberOfDataKeysResponse GetNumberOfDataKeys(DPWSContext context, GetNumberOfDataKeys GetNumKeyInp) throws DPWSException {
        GetNumberOfDataKeysResponse gndkRp = new GetNumberOfDataKeysResponseImpl();
        String errorMsg = null;
        int hany = 0;
        try {
            if ((rootDir == null) || (rootDir.length() == (-1))) {
                errorMsg = LocalStorVerify.ISNT_ROOTFLD;
            } else {
                String folderN = GetNumKeyInp.getName();
                if (LocalStorVerify.isValid(folderN)) {
                    String srcDir = rootDir + File.separator + folderN;
                    if (LocalStorVerify.isLength(srcDir)) {
                        for (File sf : new File(srcDir).listFiles()) {
                            if (!sf.isDirectory()) {
                                hany++;
                            }
                        }
                    } else {
                        errorMsg = LocalStorVerify.FLD_TOOLNG;
                    }
                } else {
                    errorMsg = LocalStorVerify.ISNT_VALID;
                }
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
            errorMsg = tr.getMessage();
            hany = (-1);
        }
        if (errorMsg != null) {
        }
        gndkRp.setNum(hany);
        return gndkRp;
    }
}
