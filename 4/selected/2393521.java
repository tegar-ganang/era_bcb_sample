package hu.sztaki.lpds.pgportal.services.credential.slcs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import ch.swing.gridcertlib.CredentialsPathInfo;
import ch.swing.gridcertlib.GridProxyFactory;
import ch.swing.gridcertlib.SLCSFactory;

public class SLCSManager {

    private static SLCSManager instance = null;

    public static final long CERTMINLIFETIME = 3600;

    public static final long PROXYMINLIFETIME = 3600;

    public static final String CERTFILEPREFIX = "slcsCert-";

    public static final String KEYFILEPREFIX = "slcsKey-";

    public static final String EXTENSION = ".pem";

    public static final String PROXYFILENAME = "x509up.NORDUGRID";

    /**
	 * Stores users and 
	 * 
	 */
    private HashMap<String, SLCSUserBean> userStore = null;

    private Properties gridCertLibProps = null;

    private SLCSFactory slcsFactory = null;

    private GridProxyFactory gridProxyFactory = null;

    protected SLCSManager(InputStream propertyStream) throws IOException {
        userStore = new HashMap<String, SLCSUserBean>();
        gridCertLibProps = new Properties();
        gridCertLibProps.load(propertyStream);
        slcsFactory = new SLCSFactory(gridCertLibProps, true);
        gridProxyFactory = new GridProxyFactory(gridCertLibProps);
    }

    public static SLCSManager getInstance(InputStream propertyStream) throws IOException {
        if (instance == null) {
            instance = new SLCSManager(propertyStream);
        }
        return instance;
    }

    public boolean checkProxyValidity(String userName) {
        SLCSUserBean bean = this.userStore.get(userName);
        Date now = new Date();
        return (bean != null) && bean.getDateOfProxyCreation() + bean.getProxyValidity() > now.getTime() + PROXYMINLIFETIME;
    }

    private boolean checkCertValidity(String userName) {
        SLCSUserBean bean = this.userStore.get(userName);
        Date now = new Date();
        return (bean != null) && bean.getDateOfCertCreation() + bean.getCertValidity() > now.getTime() + CERTMINLIFETIME;
    }

    public void generateCertificate(String userName, String assertion) {
        if (!checkCertValidity(userName)) {
            if (!assertion.isEmpty() && slcsFactory != null) {
                System.out.println("Creating user cert from assertion " + assertion);
                File oldcertfile = new File(slcsFactory.getStoreDirectory() + File.separator + userName + File.separator + CERTFILEPREFIX + userName + EXTENSION);
                if (oldcertfile.exists()) {
                    oldcertfile.delete();
                }
                File oldkeyfile = new File(slcsFactory.getStoreDirectory() + File.separator + userName + File.separator + KEYFILEPREFIX + userName + EXTENSION);
                if (oldkeyfile.exists()) {
                    oldkeyfile.delete();
                }
                String certfile = slcsFactory.getStoreDirectory() + File.separator + userName + File.separator + CERTFILEPREFIX + userName + EXTENSION;
                String keyfile = slcsFactory.getStoreDirectory() + File.separator + userName + File.separator + KEYFILEPREFIX + userName + EXTENSION;
                CredentialsPathInfo userCert = slcsFactory.newSLCS(assertion, certfile.replaceAll(" ", ""), keyfile.replaceAll(" ", ""));
                Date now = new Date();
                SLCSUserBean bean = new SLCSUserBean(userName, userCert.getPrivateKeyPassword(), now.getTime(), userCert);
                this.userStore.put(userName, bean);
            } else {
                throw new IllegalArgumentException("assertion empty");
            }
        }
    }

    public SLCSUserBean getProxy(String userName) throws Exception {
        if (checkCertValidity(userName)) {
            generateProxy(userName);
            return this.userStore.get(userName);
        } else {
            throw new Exception("Certificate Expired");
        }
    }

    private void generateProxy(String userName) throws IOException {
        if (!checkProxyValidity(userName)) {
            String vo = gridCertLibProps.getProperty("gridcertlib.vo");
            String proxypath = gridProxyFactory.newProxy(this.userStore.get(userName).getUserCert(), vo);
            this.storeProxy(proxypath, userName);
            this.userStore.get(userName).setDateOfProxyCreation(new Date().getTime());
            this.userStore.get(userName).setVo(vo);
        }
    }

    private String storeProxy(String proxyPath, String userName) throws IOException {
        File inputFile = new File(proxyPath);
        String outfile = slcsFactory.getStoreDirectory() + File.separator + userName + File.separator + PROXYFILENAME;
        File outputFile = new File(outfile);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
        Process p = Runtime.getRuntime().exec("/bin/chmod 600 " + outfile);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return outfile;
    }
}
