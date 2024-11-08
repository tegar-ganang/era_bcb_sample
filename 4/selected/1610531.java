package org.dbe.servent.deployer.bpel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dbe.servent.CustomDeployer;
import org.dbe.servent.Servent;
import org.dbe.servent.ServentContext;
import org.dbe.servent.ServiceConfiguration;

public class BpelDeployer implements CustomDeployer {

    private static final Logger LOGGER = Logger.getLogger(BpelDeployer.class);

    private static final String BPR_FOLDER = "bpr";

    private String m_bpr;

    public BpelDeployer() {
    }

    public void deploy(String applicationPath, ServiceConfiguration serviceConfig) {
        try {
            doBprStuff(applicationPath, serviceConfig);
        } catch (MalformedURLException e) {
            LOGGER.error("Could not deploy service composition " + applicationPath, e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not deploy service composition " + applicationPath, e);
        } catch (AeBprException e) {
            LOGGER.error("Could not deploy service composition " + applicationPath, e);
        } catch (IOException e) {
            LOGGER.error("Could not deploy service composition " + applicationPath, e);
        }
    }

    /**
     * @param applicationPath
     * @param serviceConfig
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws AeBprException
     */
    private void doBprStuff(String applicationPath, ServiceConfiguration serviceConfig) throws MalformedURLException, FileNotFoundException, IOException, AeBprException {
        if (serviceConfig.getBprs() != null) {
            loadBprs(applicationPath, serviceConfig.getBprs());
        }
    }

    /**
     * @param dir
     * @param bprSeq
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws AeBprException
     */
    private void loadBprs(String dir, String[] bprSeq) throws MalformedURLException, FileNotFoundException, IOException, AeBprException {
        File rootDir = new File(dir);
        File fbpr = new File(rootDir, Servent.SERVICE_CHAIN);
        File[] listBprs = fbpr.listFiles();
        if (listBprs != null) {
            File bprFile = null;
            for (int seq = 0; seq < bprSeq.length; seq++) {
                bprFile = getCorrectBprFile(bprSeq[seq], listBprs);
                FileUtils.copyFileToDirectory(bprFile, new File(m_bpr));
                LOGGER.info("deploying " + (seq + 1) + ". bpr=" + bprFile.getName() + " " + m_bpr);
            }
        }
    }

    private File getCorrectBprFile(String fileName, File[] filesList) {
        File bprFile;
        for (int i = 0; i < filesList.length; i++) {
            bprFile = filesList[i];
            if (bprFile.getName().equals(fileName)) {
                return bprFile;
            }
        }
        return null;
    }

    public void init(ServentContext context) {
        m_bpr = context.getConfig().getRootPath() + File.separator + BPR_FOLDER + File.separator;
    }
}
