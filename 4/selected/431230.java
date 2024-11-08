package com.intel.gpe.client2.common.transfers.gridftp;

import java.io.InputStream;
import org.globus.io.streams.GridFTPOutputStream;
import com.intel.gpe.client2.security.GPESecurityManager;
import com.intel.gpe.client2.transfers.FileImport;
import com.intel.gpe.client2.transfers.TransferFailedException;
import com.intel.gpe.clients.api.FileTransferClient;
import com.intel.gpe.clients.api.StorageClient;
import com.intel.gpe.clients.api.fts.gridftp.GridFTPFileTransferClient;
import com.intel.gpe.clients.api.security.gss.GSSSecurityManager;

/**
 * 
 * @author Alexander Lukichev
 * @version $Id: GridFTPFileImportImpl.java,v 1.2 2006/06/01 12:58:49 lukichev Exp $
 */
public class GridFTPFileImportImpl implements FileImport {

    private StorageClient storage;

    public GridFTPFileImportImpl(StorageClient storage) {
        this.storage = storage;
    }

    public void putFile(GPESecurityManager secMgr, InputStream is, String remoteFile) throws Exception {
        if (secMgr instanceof GSSSecurityManager) {
            GridFTPFileTransferClient client = (GridFTPFileTransferClient) storage.importFile(remoteFile, FileTransferClient.GRIDFTP, false);
            GridFTPOutputStream os = client.getOutputStream((GSSSecurityManager) secMgr);
            int chunk = 16384;
            byte[] buf = new byte[chunk];
            int read = 0;
            while (read != -1) {
                read = is.read(buf, 0, chunk);
                if (read != -1) {
                    os.write(buf, 0, read);
                }
            }
            client.destroy();
            os.close();
        } else {
            throw new TransferFailedException("File export failed");
        }
    }

    public String getName() {
        return "GridFTP";
    }
}
