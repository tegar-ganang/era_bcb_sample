package com.makeabyte.jhosting.test.appstoreapi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import javax.activation.DataHandler;
import org.testng.annotations.Test;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import com.makeabyte.appstore.webservice.AppstoreAPIStub;
import com.makeabyte.appstore.webservice.AppstoreAPIStub.Login;
import com.makeabyte.appstore.webservice.AppstoreAPIStub.Download;
import com.makeabyte.appstore.webservice.AppstoreAPIStub.GetAppsByPlatformId;

public class Axis2Test {

    AppstoreAPIStub stub;

    public Axis2Test() {
        try {
            stub = new AppstoreAPIStub();
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.REUSE_HTTP_CLIENT, true);
        } catch (AxisFault af) {
            System.out.println(af.getMessage());
        }
    }

    @Test
    public void testLogin() throws RemoteException {
        Login login;
        login = new AppstoreAPIStub.Login();
        login.setUsername("admin");
        login.setPassword("test");
        login.setApiKey("123qwe");
        AppstoreAPIStub.LoginE loginImpl = new AppstoreAPIStub.LoginE();
        loginImpl.setLogin(login);
        boolean result = stub.login(loginImpl).getLoginResponse().get_return();
        assert result == true;
    }

    @Test
    public void testGetAppsByPlatformId() throws RemoteException {
        testLogin();
        GetAppsByPlatformId getAppsByPlatformId = new AppstoreAPIStub.GetAppsByPlatformId();
        getAppsByPlatformId.setId(1);
        AppstoreAPIStub.GetAppsByPlatformIdE getAppsByPlatformIdImpl = new AppstoreAPIStub.GetAppsByPlatformIdE();
        getAppsByPlatformIdImpl.setGetAppsByPlatformId(getAppsByPlatformId);
        AppstoreAPIStub.AppWS[] apps = stub.getAppsByPlatformId(getAppsByPlatformIdImpl).getGetAppsByPlatformIdResponse().get_return();
        for (AppstoreAPIStub.AppWS app : apps) System.out.println(app.getName());
        assert apps.length > 0;
    }

    @Test
    public void testDownload() throws IOException {
        testLogin();
        Download download = new AppstoreAPIStub.Download();
        download.setId(4);
        AppstoreAPIStub.DownloadE downloadImpl = new AppstoreAPIStub.DownloadE();
        downloadImpl.setDownload(download);
        DataHandler data = stub.download(downloadImpl).getDownloadResponse().get_return();
        InputStream inputStream = data.getInputStream();
        File f = new File("/home/jhahn/Desktop/htmlTest.jhp");
        OutputStream out = new FileOutputStream(f);
        byte buf[] = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
        out.close();
        inputStream.close();
        assert f.length() > 0;
    }
}
