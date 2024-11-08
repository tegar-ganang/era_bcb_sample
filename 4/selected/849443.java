package org.mc.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.mc.NonRelevantDroppable;
import org.mc.app.Inittable;
import org.mc.db.DB;
import org.mc.img.HasImages;
import org.mc.img.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author evgeniivs
 */
public abstract class Content extends Inittable implements NonRelevantDroppable, HasImages {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(Content.class);
    }

    protected abstract File getDescrFileInner(int id);

    protected int id;

    protected String descr;

    protected String title;

    public String getDescr() throws SQLException, IOException {
        if (descr == null) {
            getDescrFromHD();
        }
        return descr;
    }

    protected abstract void getDataFromDB() throws SQLException;

    public static String getDescrFromHD(File descrFile) throws IOException {
        try {
            String descr = "";
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(descrFile);
                FileChannel fcin = fin.getChannel();
                CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
                ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 1024);
                while (true) {
                    buffer.clear();
                    int r = fcin.read(buffer);
                    if (r == -1) {
                        break;
                    }
                    buffer.flip();
                    descr += decoder.decode(buffer).toString();
                }
                return descr;
            } finally {
                try {
                    fin.close();
                } catch (Exception e) {
                }
            }
        } catch (FileNotFoundException e) {
            log.warn("Не удается найти файл описания: " + descrFile);
            return "";
        }
    }

    private void getDescrFromHD() throws IOException {
        File descrFile = getDescrFileInner(id);
        descr = getDescrFromHD(descrFile);
    }

    public static List<Image> getImages(int id, int entity) throws SQLException {
        return getImages(id, entity, null);
    }

    public static List<Image> getImages(int id, int entity, Integer limit) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String query = null;
        try {
            conn = dataSource.getConnection();
            query = "select " + col.id + ", " + col.name + ", " + col.thumbName + " from " + DB.Tbl.img + " where " + col.entityId + " = ? and " + col.entity + " = ? order by " + col.order + " asc";
            if (limit != null) query += " limit 0,?";
            ps = conn.prepareStatement(query);
            ps.setInt(1, id);
            ps.setInt(2, entity);
            if (limit != null) ps.setInt(3, limit);
            rs = ps.executeQuery();
            List<Image> imgList = new LinkedList<Image>();
            while (rs.next()) {
                String name = rs.getString(col.name);
                String thName = rs.getString(col.thumbName);
                int imgId = rs.getInt(col.id);
                Image img = new Image(imgId, id, name, thName, entity);
                imgList.add(img);
            }
            return imgList;
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                ps.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }

    protected List<Image> images;

    protected Image mainImage;

    protected int entityType;

    protected Image getMainImageInternal() throws SQLException {
        List<Image> list = getImages(id, entityType, 1);
        if (list.size() == 0) return null;
        return list.get(0);
    }

    public Image getMainImage() throws SQLException {
        if (mainImage == null) {
            mainImage = getMainImageInternal();
        }
        return mainImage;
    }

    public List<Image> getImages() throws SQLException {
        if (images == null) {
            images = Content.getImages(id, entityType);
        }
        return images;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() throws SQLException {
        if (title == null) {
            getDataFromDB();
        }
        return title;
    }

    public void dropNonRelevant() {
        descr = null;
    }
}
