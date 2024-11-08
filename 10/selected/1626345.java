package sqlClass.daoClass;

import general.appClass.StatisticApp;
import general.dbClass.DBCategory;
import general.dbClass.DBKeyword;
import general.dbClass.DBPicInfo;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import oracle.sql.BLOB;

public class DaoUpdate {

    /**
	 * delete Image from database
	 * 
	 * @param conn
	 * @param bnr
	 * @throws SQLException
	 */
    public static void deletePic(Connection conn, int bnr) throws SQLException {
        String sql = "delete from DBPic where bnr=?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, bnr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * update lock for image.
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            image's id
	 * @param lock
	 *            1 for locked,0 for free for everyone to see
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void updateLockForPic(Connection conn, int nr, int lock) throws SQLException {
        String sql = "update DBThumb set thumb_lock=? where bnr=?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, lock);
        pstmt.setInt(2, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * update new rate for image
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            the id of the image
	 * @param rate
	 *            new rate
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void updateRateForPic(Connection conn, int nr, int rate) throws SQLException {
        String sql = "update DBPic set rate=? where nr=?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, rate);
        pstmt.setInt(2, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * change the information of the image. The request will be committed if
	 * everything works right.otherweise the database will be rolled back
	 * 
	 * @param conn
	 *            connection with database
	 * @param nr
	 *            id of the image
	 * @param lock
	 * @param picInfo
	 *            new information of the image
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void updatePicInfo(Connection conn, int nr, int lock, DBPicInfo picInfo) throws SQLException {
        String sql = "";
        PreparedStatement pstmt = null;
        try {
            if (!picInfo.getName().equals("")) {
                sql = "update DBPic set name=? where bnr=?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, picInfo.getName());
                pstmt.setInt(2, nr);
                pstmt.executeUpdate();
            }
            if (picInfo.getRate() != 0) {
                sql = "update DBPic set rate=? where bnr=?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, picInfo.getRate());
                pstmt.setInt(2, nr);
                pstmt.executeUpdate();
            }
            sql = "update DBThumb set thumb_lock=? where bnr=?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, lock);
            pstmt.setInt(2, nr);
            pstmt.executeUpdate();
            if (picInfo.getCategories() != null) {
                sql = "delete from Zuordnen where bnr=?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, nr);
                pstmt.executeUpdate();
                DaoUpdate.insertPicInCategories(conn, nr, picInfo.getCategories());
            }
            if (picInfo.getKeywords() != null) {
                sql = "delete from Haben where bnr=?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, nr);
                pstmt.executeUpdate();
                DaoUpdate.insertPicInKeywords(conn, nr, picInfo.getKeywords());
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            pstmt.close();
        }
    }

    /**
	 * insert new category into database
	 * 
	 * @param conn
	 *            connection with database
	 * @param name
	 *            new category's name
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void insertCategory(Connection conn, String name) throws SQLException {
        String sql = "insert into DBKategorie values (knr_seq.nextval,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * Modify existed category
	 * 
	 * @param conn
	 *            connection with database
	 * @param nr
	 *            category's id to modify
	 * @param name
	 *            new category's id
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void modifyCategory(Connection conn, int nr, String name) throws SQLException {
        String sql = "update DBKategorie set Name = ? where ktnr = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setInt(2, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * Delete existed category
	 * 
	 * @param conn
	 *            connection with database
	 * @param nr
	 *            category's id to delete
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void deleteCategory(Connection conn, int nr) throws SQLException {
        String sql = "delete from DBKategorie where ktnr = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * insert new keyword into database
	 * 
	 * @param conn
	 *            connection with database
	 * @param keyword
	 *            new keyword object
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void insertKeyword(Connection conn, String word) throws SQLException {
        String sql = "insert into DBKeyword values (key_seq.nextval,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, word);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * Modify existed keyword
	 * 
	 * @param conn
	 *            connection with database
	 * @param nr
	 *            keyword's id to modify
	 * @param name
	 *            new keyword's id
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void modifyKeyword(Connection conn, int nr, String word) throws SQLException {
        String sql = "update DBKeyword set Word = ? where kwnr = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, word);
        pstmt.setInt(2, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * Delete existed keyword
	 * 
	 * @param conn
	 *            connection with database
	 * @param nr
	 *            keyword's id to delete
	 * @throws SQLException
	 *             SQL communication failed
	 */
    public static void deleteKeyword(Connection conn, int nr) throws SQLException {
        String sql = "delete from DBKeyword where kwnr = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        pstmt.executeUpdate();
        conn.commit();
        pstmt.close();
    }

    /**
	 * insert the image into datenbank. The request will be commit if all things
	 * work right.otherweise the database will be rolled back.
	 * 
	 * @param conn
	 *            connection to database
	 * @param picInfo
	 *            image's information such as name,rate,categories and so on
	 * @param lock
	 *            thumb's lock 1 is locked,0 is free for every one to see
	 * @param fin
	 *            image's date as inputstream
	 * @throws SQLException
	 *             database communication failed
	 * @throws IOException
	 *             read or write image's date failed
	 * @throws JpegProcessingException
	 *             read exif information from image failed
	 */
    @SuppressWarnings("deprecation")
    public static void insertPic(Connection conn, DBPicInfo picInfo, int lock, InputStream fin) throws SQLException, IOException, JpegProcessingException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        OutputStream out = null;
        BLOB blob = null;
        byte[] buf = null;
        long uploadTime = 0;
        try {
            long beginTime = new Date().getTime();
            String sql = "insert into DBPic " + "values (bnr_seq.nextval,?,?,EMPTY_BLOB())";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, picInfo.getName());
            pstmt.setInt(2, picInfo.getRate());
            pstmt.executeUpdate();
            pstmt.close();
            sql = "select bnr_seq.currval from dual";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery(sql);
            rs.next();
            int bnr = rs.getInt(1);
            pstmt.close();
            rs.close();
            sql = "select bild from DBPic where bnr=? for update";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, bnr);
            rs = pstmt.executeQuery();
            rs.next();
            blob = (BLOB) rs.getBlob("bild");
            out = blob.getBinaryOutputStream();
            buf = new byte[fin.available()];
            fin.read(buf);
            out.write(buf);
            out.flush();
            long endTime = new Date().getTime();
            uploadTime = endTime - beginTime;
            StatisticApp.query("upload", "sql", buf.length, uploadTime, 1);
            if (picInfo.getCategories() != null) insertPicInCategories(conn, bnr, picInfo.getCategories());
            if (picInfo.getKeywords() != null) insertPicInKeywords(conn, bnr, picInfo.getKeywords());
            insertExif(conn, bnr, buf);
            insertThumb(conn, bnr, lock, buf);
            fin.close();
            out.close();
            rs.close();
            pstmt.close();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } catch (FileNotFoundException e) {
            conn.rollback();
            throw e;
        } catch (IOException e) {
            conn.rollback();
            throw e;
        } catch (MetadataException e) {
            conn.rollback();
        } finally {
            fin.close();
            out.close();
            rs.close();
            pstmt.close();
        }
    }

    /**
	 * insert the relationship between new image and categories into database
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            current image's id
	 * @param categories
	 *            the categories,which include this image
	 * @throws SQLException
	 *             SQL communication failed
	 */
    private static void insertPicInCategories(Connection conn, int nr, ArrayList<DBCategory> categories) throws SQLException {
        String sql = "insert into Zuordnen(ktnr,bnr) values(?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        for (int i = 0; i < categories.size(); i++) {
            DBCategory tmp = categories.get(i);
            pstmt.setInt(1, tmp.getNr());
            pstmt.setInt(2, nr);
            pstmt.executeUpdate();
        }
        pstmt.close();
    }

    /**
	 * insert the relationship between new image and keywords into database
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            current image's id
	 * @param keywords
	 *            the keywords,which include this image
	 * @throws SQLException
	 *             SQL communication failed
	 */
    private static void insertPicInKeywords(Connection conn, int nr, ArrayList<DBKeyword> keywords) throws SQLException {
        String sql = "insert into Haben(kwnr,bnr) values(?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        for (int i = 0; i < keywords.size(); i++) {
            DBKeyword tmp = keywords.get(i);
            pstmt.setInt(1, tmp.getNr());
            pstmt.setInt(2, nr);
            pstmt.executeUpdate();
        }
        pstmt.close();
    }

    /**
	 * read exif information from image and insert them into database
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            the image's id
	 * @param fin
	 *            image's date as inputstream
	 * @throws JpegProcessingException
	 *             read exif from image's date failed
	 * @throws SQLException
	 *             SQL communication failed
	 * @throws IOException
	 *             I/O failed
	 * @throws MetadataException
	 *             read meta daten failed
	 */
    private static void insertExif(Connection conn, int nr, byte[] buf) throws JpegProcessingException, SQLException, IOException, MetadataException {
        InputStream is = new ByteArrayInputStream(buf);
        Metadata metadata = JpegMetadataReader.readMetadata(is);
        String imgHeight = "";
        String imgWidth = "";
        String exposureTime = "";
        String fnumber = "";
        String exposureProgram = "";
        String timeOriginal = "";
        String timeDigitized = "";
        String colorSpace = "";
        String exifImgWidth = "";
        String exifImgHeight = "";
        String exposureMode = "";
        String make = "";
        String model = "";
        String orientation = "";
        String xResolution = "";
        String yResolution = "";
        Directory dic = metadata.getDirectory(JpegDirectory.class);
        if (dic != null) {
            imgHeight = dic.getDescription(JpegDirectory.TAG_JPEG_IMAGE_HEIGHT);
            imgWidth = dic.getDescription(JpegDirectory.TAG_JPEG_IMAGE_WIDTH);
        }
        dic = metadata.getDirectory(ExifSubIFDDirectory.class);
        if (dic != null) {
            exposureTime = dic.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
            fnumber = dic.getDescription(ExifSubIFDDirectory.TAG_FNUMBER);
            exposureProgram = dic.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_PROGRAM);
            timeOriginal = dic.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            timeDigitized = dic.getDescription(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
            colorSpace = dic.getDescription(ExifSubIFDDirectory.TAG_COLOR_SPACE);
            exifImgWidth = dic.getDescription(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH);
            exifImgHeight = dic.getDescription(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT);
            exposureMode = dic.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_MODE);
        }
        dic = metadata.getDirectory(ExifIFD0Directory.class);
        if (dic != null) {
            make = dic.getDescription(ExifIFD0Directory.TAG_MAKE);
            model = dic.getDescription(ExifIFD0Directory.TAG_MODEL);
            orientation = dic.getDescription(ExifIFD0Directory.TAG_ORIENTATION);
            xResolution = dic.getDescription(ExifIFD0Directory.TAG_X_RESOLUTION);
            yResolution = dic.getDescription(ExifIFD0Directory.TAG_Y_RESOLUTION);
        }
        String sql = "insert into DBExif values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        pstmt.setString(2, imgHeight);
        pstmt.setString(3, imgWidth);
        pstmt.setString(4, exposureTime);
        pstmt.setString(5, fnumber);
        pstmt.setString(6, exposureProgram);
        pstmt.setString(7, timeOriginal);
        pstmt.setString(8, timeDigitized);
        pstmt.setString(9, colorSpace);
        pstmt.setString(10, exifImgWidth);
        pstmt.setString(11, exifImgHeight);
        pstmt.setString(12, exposureMode);
        pstmt.setString(13, make);
        pstmt.setString(14, model);
        pstmt.setString(15, orientation);
        pstmt.setString(16, xResolution);
        pstmt.setString(17, yResolution);
        pstmt.executeUpdate();
        pstmt.close();
        is.close();
        sql = "insert into DBExifValue values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        try {
            pstmt.setInt(2, Integer.parseInt(imgHeight.replace(" pixels", "")));
        } catch (Exception e) {
            pstmt.setNull(2, java.sql.Types.INTEGER);
        }
        try {
            pstmt.setInt(3, Integer.parseInt(imgWidth.replace(" pixels", "")));
        } catch (Exception e) {
            pstmt.setNull(3, java.sql.Types.INTEGER);
        }
        try {
            exposureTime = exposureTime.replace(" sec", "");
            if (exposureTime.indexOf('/') >= 0) {
                double exposureTimeValue = 1.0 * Integer.parseInt(exposureTime.split("/")[0]) / Integer.parseInt(exposureTime.split("/")[1]);
                pstmt.setFloat(4, (float) exposureTimeValue);
            } else {
                double exposureTimeValue = 1.0 * Float.parseFloat(exposureTime);
                pstmt.setFloat(4, (float) exposureTimeValue);
            }
        } catch (Exception e) {
            pstmt.setNull(4, java.sql.Types.FLOAT);
        }
        try {
            pstmt.setFloat(5, Float.parseFloat(fnumber.replace("F", "").replace(",", ".")));
        } catch (Exception e) {
            pstmt.setNull(5, java.sql.Types.FLOAT);
        }
        pstmt.setString(6, exposureProgram);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        try {
            Date timeOriginalValue = sdf.parse(timeOriginal);
            pstmt.setTimestamp(7, new java.sql.Timestamp(timeOriginalValue.getTime()));
        } catch (Exception e) {
            pstmt.setNull(7, java.sql.Types.TIMESTAMP);
        }
        try {
            Date timeDigitizedValue = sdf.parse(timeOriginal);
            pstmt.setTimestamp(8, new java.sql.Timestamp(timeDigitizedValue.getTime()));
        } catch (Exception e) {
            pstmt.setNull(8, java.sql.Types.TIMESTAMP);
        }
        pstmt.setString(9, colorSpace);
        try {
            pstmt.setInt(10, Integer.parseInt(exifImgWidth.replace(" pixels", "")));
        } catch (Exception e) {
            pstmt.setNull(10, java.sql.Types.INTEGER);
        }
        try {
            pstmt.setInt(11, Integer.parseInt(exifImgHeight.replace(" pixels", "")));
        } catch (Exception e) {
            pstmt.setNull(11, java.sql.Types.INTEGER);
        }
        pstmt.setString(12, exposureMode);
        pstmt.setString(13, make);
        pstmt.setString(14, model);
        pstmt.setString(15, orientation);
        try {
            pstmt.setInt(16, Integer.parseInt(xResolution.replace(" dots per inch", "")));
        } catch (Exception e) {
            pstmt.setNull(16, java.sql.Types.INTEGER);
        }
        try {
            pstmt.setInt(17, Integer.parseInt(yResolution.replace(" dots per inch", "")));
        } catch (Exception e) {
            pstmt.setNull(17, java.sql.Types.INTEGER);
        }
        pstmt.executeUpdate();
        pstmt.close();
    }

    /**
	 * create thumbnail from image and insert it into database
	 * 
	 * @param conn
	 *            connection to database
	 * @param nr
	 *            image's id
	 * @param lock
	 *            thumb's lock,1 for locked,0 for free for every one to see
	 * @param buf
	 *            image's date as array of bytes
	 * @throws IOException
	 *             read image's date failed
	 * @throws SQLException
	 *             SQL communication failed
	 */
    @SuppressWarnings("deprecation")
    private static void insertThumb(Connection conn, int nr, int lock, byte[] buf) throws IOException, SQLException {
        InputStream is = new ByteArrayInputStream(buf);
        String sql = "insert into DBThumb(bnr,thumb_lock,thumb) values(?,?,empty_blob())";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        pstmt.setInt(2, lock);
        pstmt.executeUpdate();
        sql = "select thumb from DBThumb where bnr=? for update";
        pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, nr);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            BLOB blob = (BLOB) rs.getBlob("thumb");
            OutputStream out = blob.getBinaryOutputStream();
            BufferedImage inBuf = ImageIO.read(is);
            Image tmp = inBuf.getScaledInstance(160, 120, Image.SCALE_SMOOTH);
            BufferedImage retBuf = new BufferedImage(160, 120, BufferedImage.TYPE_INT_RGB);
            Graphics g = retBuf.getGraphics();
            g.drawImage(tmp, 0, 0, null);
            ImageIO.write(retBuf, "jpg", out);
            out.flush();
            out.close();
        }
        is.close();
        rs.close();
        pstmt.close();
    }
}
