package org.mc.content;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.SQLException;
import org.mc.db.DB;
import org.mc.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author evgeniivs
 */
public abstract class ContentCreator extends Content {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(ContentCreator.class);
    }

    protected User user;

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String getDescr() {
        return descr;
    }

    @Override
    public String getTitle() {
        return title;
    }

    protected void saveDescr(int id) throws IOException {
        File descrFile = getDescrFileInner(id);
        saveDescr(descr, descrFile);
    }

    public static void saveDescr(String descr, File descrFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(descrFile);
            FileChannel fc = fos.getChannel();
            fc.write(Charset.forName("UTF-8").encode(descr));
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    @Deprecated
    protected void getDataFromDB() throws SQLException {
        throw new UnsupportedOperationException("Не должен поддерживаться.");
    }

    protected abstract String sameTitleExistsQuery();

    protected boolean sameTitleExists() throws SQLException {
        return DB.Action.sameExists(sameTitleExistsQuery());
    }
}
