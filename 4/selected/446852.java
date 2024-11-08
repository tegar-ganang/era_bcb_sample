package com.patientis.upgrade.dbreplace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import com.patientis.client.common.PromptsController;
import com.patientis.framework.controls.forms.ISFrame;
import com.patientis.framework.locale.SystemUtil;
import com.patientis.framework.utility.ZipUtil;
import com.patientis.model.common.DateTimeModel;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

/**
 * @author gcaulton
 *
 */
public class DeployUpdate {

    private static String targetServer = "patientos.org";

    private static String ftpuser = null;

    private static String ftppassword = null;

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        String baseServerDir = "/home/wfacey";
    }

    /**
	 * @param args
	 */
    public static void mainOLD(String[] args) throws Exception {
        boolean uploadDatabase = PromptsController.questionIsYesForYesNo(new ISFrame(), "Upload database", "Long upload");
        boolean latestJars = false;
        long msStart = DateTimeModel.getNowInMS();
        String version = "0.92";
        String baseServerDir = "/home/httpd/vhosts/patientos.org/httpdocs/forum_temp/";
        FileUtils.copyFile(new File("C:\\dev\\patientis\\output\\deploy\\patientis.ejb3"), new File("C:\\dev\\local\\deploy\\v092\\server\\appserver\\server\\default\\deploy\\patientis.ejb3"));
        FileUtils.copyFile(new File("C:\\dev\\patientis\\output\\deploy\\patientos.war"), new File("C:\\dev\\local\\deploy\\v092\\server\\appserver\\server\\default\\deploy\\patientos.war"));
        FileUtils.copyFile(new File("C:\\dev\\patientis\\output\\client\\patientis.jar"), new File("C:\\dev\\local\\deploy\\v092\\client\\lib\\patientis.jar"));
        FileUtils.copyFile(new File("C:\\dev\\patientis\\output\\client\\resources.jar"), new File("C:\\dev\\local\\deploy\\v092\\client\\lib\\resources.jar"));
        uploadFile(getClientLibZip(latestJars), baseServerDir + "upgrade-<version>-clientlib.zip".replace("<version>", version));
        uploadFile(getServerDeployZip(), baseServerDir + "upgrade-<version>-serverdeploy.zip".replace("<version>", version));
        if (uploadDatabase) {
            System.out.println("uploading database");
            uploadFile(getDatabaseZip(), baseServerDir + "upgrade-<version>-database.zip".replace("<version>", version));
        }
        System.out.println("upload complete in " + ((int) ((DateTimeModel.getNowInMS() - msStart) / 1000.0)));
        System.exit(1);
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    private static File getClientBinZip() throws Exception {
        String path = "C:\\dev\\local\\deploy\\v092\\client\\bin\\patientos.bat";
        return ZipUtil.zipFiles(true, path);
    }

    private static File getDatabaseZip() throws Exception {
        return new File("C:\\dev\\local\\deploy\\v092\\upgrade\\05database\\backup.zip");
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    private static File getClientLibZip(boolean latestJars) throws Exception {
        List<String> paths = new ArrayList<String>();
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\patientis.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\resources.jar");
        if (latestJars) {
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\barcode.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\PDFRenderer.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jpedal.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpclient-4.0-beta2.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpcore-4.0-beta3.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpcore-nio-4.0-beta3.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpmime-4.0-beta2.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\fop.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\xmlgraphics-commons-1.3.1.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\avalon-framework-4.2.0.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\batik-all-1.7.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jhall.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\patientos-help.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jftp.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\HTMLEditorLight.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\venaliapi.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jaxrpc-1.1.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\axis.jar");
            paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\commons-discovery.jar");
        }
        return ZipUtil.zipFiles(true, paths);
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    private static File getServerDeployZip() throws Exception {
        String path1 = "C:\\dev\\local\\deploy\\v092\\server\\appserver\\server\\default\\deploy\\patientis.ejb3";
        String path2 = "C:\\dev\\local\\deploy\\v092\\server\\appserver\\server\\default\\deploy\\patientos.war";
        return ZipUtil.zipFiles(true, path1, path2);
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    private static File getServerFilesZip() throws Exception {
        String path1 = "C:\\dev\\local\\deploy\\v092\\server\\data\\files.zip";
        return ZipUtil.zipFiles(true, path1);
    }

    /**
	 * 
	 * @return
	 * @throws Exception
	 */
    private static File getServerLibZip() throws Exception {
        List<String> paths = new ArrayList<String>();
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\barcode.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\PDFRenderer.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jpedal.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpclient-4.0-beta2.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpcore-4.0-beta3.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpcore-nio-4.0-beta3.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\httpmime-4.0-beta2.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\fop.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\xmlgraphics-commons-1.3.1.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\avalon-framework-4.2.0.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\batik-all-1.7.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jftp.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\patientos-help.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\venaliapi.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\jaxrpc-1.1.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\axis.jar");
        paths.add("C:\\dev\\local\\deploy\\v092\\client\\lib\\commons-discovery.jar");
        return ZipUtil.zipFiles(true, paths);
    }

    /**
	 * 
	 * @param localFile
	 * @param targetFile
	 * @throws Exception
	 */
    public static void uploadFile(File localFile, String targetFile) throws Exception {
        if (ftpuser == null) {
            ftpuser = PromptsController.getInput(null, "FTP User", "root");
        }
        if (ftppassword == null) {
            ftppassword = PromptsController.getInput(null, "FTP Password", "");
        }
        System.out.println("uploading " + targetFile);
        com.sshtools.j2ssh.SftpClient s;
        SshClient ssh = new SshClient();
        ssh.connect(targetServer, 22, new HostKeyVerification() {

            @Override
            public boolean verifyHost(String arg0, SshPublicKey arg1) throws TransportProtocolException {
                return true;
            }
        });
        PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
        passwordAuthenticationClient.setUsername(ftpuser);
        passwordAuthenticationClient.setPassword(ftppassword);
        int result = ssh.authenticate(passwordAuthenticationClient);
        if (result != AuthenticationProtocolState.COMPLETE) {
            throw new Exception("Login failed");
        }
        SftpClient client = ssh.openSftpClient();
        client.put(new FileInputStream(localFile), targetFile);
        ssh.disconnect();
    }
}
