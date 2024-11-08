package com.intel.gpe.client2.common.transfers.baseline;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.intel.gpe.client2.security.GPESecurityManager;
import com.intel.gpe.client2.transfers.FileImport;
import com.intel.gpe.clients.api.FileTransferClient;
import com.intel.gpe.clients.api.StorageClient;
import com.intel.gpe.clients.api.fts.baseline.BaselineFileTransferClient;

/**
 * @author Thomas Kentemich 
 * @version $Id: BaselineFileImportImpl.java,v 1.2 2006/06/01 12:58:49 lukichev Exp $
 */
public class BaselineFileImportImpl implements FileImport {

    private StorageClient storage;

    public BaselineFileImportImpl(StorageClient storage) {
        this.storage = storage;
    }

    public void putFile(GPESecurityManager manager, InputStream is, String remoteFile) throws Exception {
        BaselineFileTransferClient transfer = (BaselineFileTransferClient) storage.importFile(remoteFile, FileTransferClient.BASELINE, false);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int chunk = 16384;
        byte[] buf = new byte[chunk];
        int read = 0;
        while (read != -1) {
            read = is.read(buf, 0, chunk);
            if (read != -1) {
                bout.write(buf, 0, read);
            }
        }
        bout.close();
        transfer.putFile(bout.toByteArray());
    }

    public String getName() {
        return "Baseline";
    }
}
