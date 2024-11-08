package simplephoto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PhotoServer extends Server {

    public static String PHOTODIR;

    public static void add(User user, File in, String mimetype) throws SQLException, IOException {
        String localname = createLocalName(in.getName());
        File out = new File(localname);
        copyFile(in, out);
        long length = out.length();
        Connection con = null;
        try {
            con = dbcp.getConnection();
            PreparedStatement stmt = con.prepareStatement("insert into photo (localname,mimetype,length, user_id) values (?,?,?,?)");
            stmt.setString(1, in.getName());
            stmt.setString(2, mimetype);
            stmt.setLong(3, length);
            stmt.setInt(4, user.getId());
            stmt.executeUpdate();
        } finally {
            if (con != null) con.close();
        }
    }

    public static String createLocalName(String name) {
        return PHOTODIR + name;
    }

    private static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
