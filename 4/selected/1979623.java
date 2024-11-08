package com.doculibre.intelligid.test.commons.transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.transaction.file.FileResourceManager;
import org.apache.commons.transaction.file.ResourceManagerException;
import org.apache.commons.transaction.file.ResourceManagerSystemException;
import org.apache.commons.transaction.file.SmbFileResourceManager;
import org.apache.commons.transaction.util.CommonsLoggingLogger;
import org.apache.commons.transaction.util.LoggerFacade;
import org.apache.tika.exception.TikaException;
import com.doculibre.intelligid.utils.TikaUtils;

public class TestCommonsTransaction {

    private static final Log logger = LogFactory.getLog(TestCommonsTransaction.class);

    public static void main(String[] args) throws Exception {
        File rootDir = new File("C:\\dev\\workspace_fgd\\gouvqc_crggid\\WebContent\\WEB-INF\\upload");
        File storeDir = new File(rootDir, "storeDir");
        File workDir = new File(rootDir, "workDir");
        LoggerFacade loggerFacade = new CommonsLoggingLogger(logger);
        final FileResourceManager frm = new SmbFileResourceManager(storeDir.getPath(), workDir.getPath(), true, loggerFacade);
        frm.start();
        final String resourceId = "811375c8-7cae-4429-9a0e-9222f47dab45";
        {
            if (!frm.resourceExists(resourceId)) {
                String txId = frm.generatedUniqueTxId();
                frm.startTransaction(txId);
                FileInputStream inputStream = new FileInputStream(resourceId);
                frm.createResource(txId, resourceId);
                OutputStream outputStream = frm.writeResource(txId, resourceId);
                IOUtils.copy(inputStream, outputStream);
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
                frm.prepareTransaction(txId);
                frm.commitTransaction(txId);
            }
        }
        for (int i = 0; i < 30; i++) {
            final int index = i;
            new Thread() {

                @Override
                public void run() {
                    try {
                        String txId = frm.generatedUniqueTxId();
                        frm.startTransaction(txId);
                        InputStream inputStream = frm.readResource(resourceId);
                        frm.prepareTransaction(txId);
                        frm.commitTransaction(txId);
                        synchronized (System.out) {
                            System.out.println(index + " ***********************" + txId + " (début)");
                        }
                        String contenu = TikaUtils.getParsedContent(inputStream, "file.pdf");
                        synchronized (System.out) {
                            System.out.println(index + " ***********************" + txId + " (fin)");
                        }
                    } catch (ResourceManagerSystemException e) {
                        throw new RuntimeException(e);
                    } catch (ResourceManagerException e) {
                        throw new RuntimeException(e);
                    } catch (TikaException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        Thread.sleep(60000);
        frm.stop(FileResourceManager.SHUTDOWN_MODE_NORMAL);
    }
}
