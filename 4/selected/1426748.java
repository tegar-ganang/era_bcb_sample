package com.doculibre.intelligid.entrepot.commons.transaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.transaction.file.FileResourceManager;
import org.apache.commons.transaction.file.ResourceManagerSystemException;

public class CommonsTransactionTest {

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        FileResourceManager frm = CommonsTransactionContext.configure(new File("C:/tmp"));
        try {
            frm.start();
        } catch (ResourceManagerSystemException e) {
            throw new RuntimeException(e);
        }
        FileInputStream is = new FileInputStream("C:/Alfresco/WCM_Eval_Guide2.0.pdf");
        CommonsTransactionOutputStream os = new CommonsTransactionOutputStream(new Ownerr());
        IOUtils.copy(is, os);
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
        try {
            frm.stop(FileResourceManager.SHUTDOWN_MODE_NORMAL);
        } catch (ResourceManagerSystemException e) {
            throw new RuntimeException(e);
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    public static class Ownerr implements CommonsTransactionOwner {

        private String resourceId;

        public String getResourceId() {
            return resourceId;
        }

        public String getUpdatedResourceId() {
            return resourceId;
        }

        public void setUpdatedResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public Boolean isGenerateNewResourceId() {
            return false;
        }

        @Override
        public void setGenerateNewResourceId(Boolean generateNewResourceId) {
        }

        @Override
        public void setResourceId(String resourceId) {
        }
    }
}
