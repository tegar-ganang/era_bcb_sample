package com.intel.gpe.client.transfers.local;

import java.io.InputStream;
import java.io.OutputStream;
import com.intel.gpe.client.security.GPESecurityManager;
import com.intel.gpe.client.transfers.FileImport;
import com.intel.gpe.clients.api.StorageClient;
import com.intel.gpe.clients.api.fts.local.LocalFileTransferClient;

/**
 * 
 * @author Alexander Lukichev
 * @version $Id: LocalFileImportImpl.java,v 1.2 2005/12/14 16:01:53 lukichev Exp $
 */
public class LocalFileImportImpl implements FileImport {

    private StorageClient storage;

    public LocalFileImportImpl(StorageClient storage) {
        this.storage = storage;
    }

    public void putFile(GPESecurityManager secMgr, InputStream is, String remoteFile) throws Exception {
        LocalFileTransferClient transfer = (LocalFileTransferClient) storage.importFile(remoteFile, "Local", false);
        OutputStream os = transfer.getOutputStream();
        int chunk = 16384;
        byte[] buf = new byte[chunk];
        int read = 0;
        while (read != -1) {
            read = is.read(buf, 0, chunk);
            if (read != -1) {
                os.write(buf, 0, read);
            }
        }
        os.close();
        transfer.destroy();
    }
}
