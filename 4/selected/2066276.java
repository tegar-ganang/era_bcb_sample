package com.intel.gpe.client2.common.transfers.gass;

import java.io.InputStream;
import org.globus.io.streams.HTTPOutputStream;
import com.intel.gpe.client2.security.GPESecurityManager;
import com.intel.gpe.client2.transfers.FileImport;
import com.intel.gpe.clients.api.FileTransferClient;
import com.intel.gpe.clients.api.StorageClient;
import com.intel.gpe.clients.api.fts.gass.GASSFileTransferClient;

/**
 * @author Alexander Lukichev
 * @version $Id: GASSFileImportImpl.java,v 1.3 2006/10/19 13:40:53 dizhigul Exp $
 */
public class GASSFileImportImpl implements FileImport {

    private StorageClient storage;

    public GASSFileImportImpl(StorageClient storage) {
        this.storage = storage;
    }

    public void putFile(GPESecurityManager secMgr, InputStream is, String remoteFile) throws Exception {
        GASSFileTransferClient transfer = (GASSFileTransferClient) storage.importFile(remoteFile, FileTransferClient.GASS, false);
        HTTPOutputStream os = transfer.getOutputStream();
        Thread.sleep(100);
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

    public String getName() {
        return "GASS";
    }
}
