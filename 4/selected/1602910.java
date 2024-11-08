package pl.sind.blip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pl.sind.blip.BlipConnector;
import pl.sind.blip.BlipConnectorException;
import pl.sind.blip.entities.Avatar;
import pl.sind.http.Download;
import pl.sind.http.HttpRequestException;

@RunWith(JUnit4.class)
public class BlipConnectorAvatarsTest extends BaseBlipTestCase {

    @Test
    public void testGetAvatar() throws BlipConnectorException {
        BlipConnector bc = newConnector();
        Avatar res = bc.getAvatar();
        System.out.println(res);
    }

    @Test
    public void testGetUserAvatar() throws BlipConnectorException {
        BlipConnector bc = newConnector();
        Avatar res = bc.getUserAvatar("wooda");
        System.out.println(res);
    }

    @Test
    public void testCreateAvatar() throws BlipConnectorException, FileNotFoundException {
        BlipConnector bc = newConnector();
        File file = new File("bloody_kiss.jpg");
        Avatar res = bc.createAvatar(new FileInputStream(file), "kiss.jpg", "image/jpeg", file.length());
        System.out.println(res);
    }

    @Test
    public void testDeleteAvatar() throws BlipConnectorException, FileNotFoundException {
        BlipConnector bc = newConnector();
        bc.deleteAvatar();
    }

    @Test
    public void testDownloadUserAvatar() throws BlipConnectorException, IOException, HttpRequestException {
        BlipConnector bc = newConnector();
        Download userAvatar = bc.getUserAvatar("wooda", BlipConnector.AvatarSize.femto);
        byte[] buffer = new byte[1024];
        int read = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = userAvatar.getContent().read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        System.out.println(out.toByteArray().length);
    }

    @Test
    public void testDownloadAvatar() throws BlipConnectorException, IOException, HttpRequestException {
        BlipConnector bc = newConnector();
        Download userAvatar = bc.getAvatar(BlipConnector.AvatarSize.nano);
        byte[] buffer = new byte[1024];
        int read = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = userAvatar.getContent().read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        System.out.println(out.toByteArray().length);
    }
}
