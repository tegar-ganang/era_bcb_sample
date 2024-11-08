package hu.sztaki.lpds.pgportal.services.remoteStorage.ftp;

import hu.sztaki.lpds.pgportal.services.remoteStorage.storageClient;
import hu.sztaki.lpds.pgportal.services.utils.PropertyLoader;
import cz.dhl.ftp.*;
import cz.dhl.io.*;
import java.io.*;
import java.util.Vector;

public class ftpClient implements storageClient {

    private String path = "pgportal";

    private FtpConnect cn = null;

    private Ftp cl = null;

    /** Creates a new instance of ftpClient */
    public ftpClient() {
    }

    /**
	 * Connect to FTP server. Read connection information using PropertyLoader
	 * @throws IOException
	 * @throws Exception
	 */
    public void connect() throws IOException, Exception {
        String host = PropertyLoader.getInstance().getProperty("remote.storage.host");
        String dir = PropertyLoader.getInstance().getProperty("remote.storage.dir");
        String ruser = PropertyLoader.getInstance().getProperty("remote.storage.user");
        String pass = PropertyLoader.getInstance().getProperty("remote.storage.password");
        int port = Integer.parseInt(PropertyLoader.getInstance().getProperty("remote.storage.port"));
        cn = FtpConnect.newConnect(host + dir);
        cn.setUserName(ruser);
        cn.setPassWord(pass);
        cn.setPortNum(port);
        cl = new Ftp();
        cl.connect(cn);
    }

    public Vector<String> getDirList() throws IOException {
        Vector<String> res = new Vector<String>();
        CoFile dir = new FtpFile(cl.pwd(), cl);
        CoFile fls[] = dir.listCoFiles();
        if (fls != null) for (int n = 0; n < fls.length; n++) if (fls[n].isFile()) res.add(new String(fls[n].getName()));
        return res;
    }

    public void saveFile(String input, String output) throws IOException {
        FtpInputStream is = null;
        byte[] line = new byte[1];
        FtpFile file = new FtpFile("/" + path + "/" + input, cl);
        is = new FtpInputStream(file);
        BufferedInputStream br = new BufferedInputStream(is);
        File f = new File(output);
        f.createNewFile();
        FileOutputStream of = new FileOutputStream(f);
        while (br.read(line) != (-1)) of.write(line);
        of.close();
        br.close();
    }

    public void disConnect() throws IOException {
        cl.disconnect();
    }
}
