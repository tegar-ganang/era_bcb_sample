package org.enilu.ftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.enilu.domain.Host;

/**
 * ʹ��commons��net�����ftp����. ��ذ�commons-net-1.4.1.jar ;
 * commons-io-1.2.jar;jakarta-oro-2.0.8.jar����ͨ��.�����г�ftp�ϵ��ļ�
 * ͨ���ftp�������ϵ��ļ������ӵ�outSteam�����԰��ļ����ص������Ŀ¼..�������Ŀ¼Ϊ��������Ҫ����.���ʹ��Ӣ���ļ���
 * 
 * @author xzgf email:
 * 
 * 
 * @create 2007-2-11
 * 
 */
public class FtpSession {

    private FTPClient ftpClient = new FTPClient();

    public FtpSession(Host host) throws IOException {
        login(host);
    }

    /**
	 * ��¼ftp������
	 * 
	 * @throws IOException
	 * 
	 */
    private void login(Host host) throws IOException {
        ftpClient.connect(host.getIp());
        ftpClient.login(host.getUserName(), host.getPassword());
    }

    /**
	 * �ر�ftp����
	 * 
	 * @throws IOException
	 */
    public void close() throws IOException {
        if (ftpClient != null) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    /**
	 * �����ļ�
	 * 
	 * @param remoteName
	 *            Զ���ļ����
	 * @param localFile
	 *            �����ļ����
	 * @throws Exception
	 */
    public void downloadFile(String remoteName, String localFile) throws Exception {
        InputStream is = ftpClient.retrieveFileStream(remoteName);
        byte[] buf = new byte[1024];
        int size = 0;
        BufferedInputStream bis = new BufferedInputStream(is);
        FileOutputStream fos = new FileOutputStream(localFile);
        while ((size = bis.read(buf)) != -1) fos.write(buf, 0, size);
        fos.close();
        bis.close();
    }

    /**
	 * ���ط�������ָ��Ŀ¼�µ������ļ�������Ŀ¼
	 * 
	 * @param remoteDir
	 *            ��������ָ�����ص�Ŀ¼
	 * @param localDir
	 *            ���ش�ŵ�Ŀ¼
	 * @throws IOException
	 */
    public void downloadFiles(String remoteDir, String localDir) throws IOException {
        File local = new File(localDir);
        if (!local.exists()) {
            local.mkdir();
        }
        System.out.println(local.canWrite());
        ftpClient.changeWorkingDirectory(remoteDir);
        FTPFile[] remoteFiles = ftpClient.listFiles();
        if (remoteFiles == null || remoteFiles.length == 0) {
            return;
        }
        for (int i = 0; i < remoteFiles.length; i++) {
            String name = remoteFiles[i].getName();
            FileOutputStream fos = new FileOutputStream((localDir + name));
            ftpClient.retrieveFile(remoteDir + name, fos);
            fos.close();
        }
    }

    /**
	 * Description: ��FTP�������ϴ��ļ�
	 * 
	 * @param remoteDir
	 *            FTP����������Ŀ¼
	 * @param filename
	 *            �ϴ���FTP�������ϵ��ļ���
	 * @param input
	 *            ������
	 * @return �ɹ�����true�����򷵻�false
	 */
    public void uploadFile(String remoteDir, String fileName, InputStream input) {
        try {
            ftpClient.changeWorkingDirectory(remoteDir);
            ftpClient.storeFile(fileName, input);
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
