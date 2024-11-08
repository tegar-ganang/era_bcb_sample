package net.kortsoft.gameportlet.model.hibernate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Resource;
import junit.framework.Assert;
import net.kortsoft.gameportlet.model.GameType;
import net.kortsoft.gameportlet.model.GameTypeDAO;
import net.kortsoft.gameportlet.model.impl.GameTypeImpl;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

public class GameTypeHibernateIntegrationTest extends BaseHibernateIntegrationTest {

    @Resource
    private GameTypeDAO gameTypeDAO;

    @Test
    @Transactional
    public void testThumbnail() throws IOException {
        byte[] thumbnail = readImageFile();
        GameTypeImpl gameTypeImpl = new GameTypeImpl();
        gameTypeImpl.setName("aGame");
        gameTypeImpl.setThumbnail(thumbnail);
        GameType stored = gameTypeDAO.store(gameTypeImpl);
        Assert.assertEquals(thumbnail.length, stored.getThumbnail().length);
    }

    private byte[] readImageFile() throws IOException {
        byte[] buf = new byte[4096];
        InputStream is = this.getClass().getResourceAsStream("thumbnail.png");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buf)) > 0) {
            os.write(buf, 0, read);
        }
        is.close();
        return os.toByteArray();
    }
}
