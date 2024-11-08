package vbullmin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import vbullmin.gui.GUI;

/**
 * Local database exporter to phpbb2 table struct
 * <p>The export doing 3 work<br /><br />
 * 1- Forums<br />
 * 2- Topics<br />
 * 3- Posts<br />
 * </p>
 * @author Onur Aslan
 */
public class Exporter {

    /**
   * Database reference
   */
    public DB db;

    /**
   * Writer
   */
    private OutputStreamWriter writer;

    /**
   * Constructor
   */
    public Exporter(DB db) {
        this.db = db;
        Initialize();
    }

    /**
   * Initialize
   */
    private void Initialize() {
        try {
            File file = new File(DB.dbname + ".sql");
            writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
            File phpbb_schema = new File("patterns/phpbb2_mysql_schema.sql");
            BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(phpbb_schema)));
            String line;
            while ((line = buff.readLine()) != null) {
                write(line);
            }
            buff.close();
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(87);
        } catch (IOException e2) {
            System.err.println(e2);
            System.exit(87);
        }
    }

    /**
   * Close writer
   */
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println(e);
            System.exit(87);
        }
    }

    /**
   * Forums export
   */
    public void forums() {
        try {
            write("# -- vbullmin - Forums");
            Statement sta = DB.conn.createStatement();
            ResultSet result = sta.executeQuery("SELECT id, title FROM forums ORDER BY id ASC");
            int count = 0;
            while (result.next()) {
                write("INSERT INTO phpbb_forums (forum_id, " + "forum_name, forum_desc, cat_id, forum_order, " + "forum_posts, forum_topics, forum_last_post_id, " + "auth_view, auth_read, auth_post, auth_reply, " + "auth_edit, auth_delete, auth_announce, auth_sticky, " + "auth_pollcreate, auth_vote, auth_attachments) " + "VALUES (" + result.getInt(1) + ", '" + DB.escape(result.getString(2)) + "', NULL, 1, " + count + ", " + 0 + ", " + 0 + ", 1, 0, 0, 1, 1, 1, 1, 3, 3, 1, 1, 3);");
                count++;
                GUI.progress.setValue(count);
            }
        } catch (SQLException e) {
            DB.exception(e);
        }
    }

    /**
   * Posts exporter
   */
    public void posts() {
        try {
            write("# -- vbullmin - Posts");
            Statement sta = DB.conn.createStatement();
            ResultSet result = sta.executeQuery("SELECT id, tid, uid, text FROM posts ORDER BY id ASC");
            int count = 0;
            while (result.next()) {
                write("INSERT INTO phpbb_posts (post_id, topic_id, forum_id," + " poster_id, post_time, post_username, poster_ip) " + "VALUES (" + result.getInt(1) + ", " + result.getInt(2) + ", " + db.forumId(result.getInt(2)) + ", -1, UNIX_TIMESTAMP(), '" + DB.escape(db.username(result.getInt(3))) + "', '7F000001');");
                write("INSERT INTO phpbb_posts_text (post_id, post_subject, post_text) " + "VALUES (" + result.getInt(1) + ", NULL, '" + DB.escape(result.getString(4)) + "');");
                count++;
                GUI.progress.setValue(count);
            }
        } catch (SQLException e) {
            DB.exception(e);
        }
    }

    /**
   * Topics export
   */
    public void topics() {
        try {
            write("# -- vbullmin - Topics");
            Statement sta = DB.conn.createStatement();
            ResultSet result = sta.executeQuery("SELECT id, fid, title FROM topics ORDER BY id ASC");
            int count = 0;
            while (result.next()) {
                int tid = result.getInt(1);
                write("INSERT INTO phpbb_topics (topic_id, " + "topic_title, " + "topic_poster, " + "topic_time, " + "topic_views, " + "topic_replies, " + "forum_id, " + "topic_status, " + "topic_type, " + "topic_vote, " + "topic_first_post_id, " + "topic_last_post_id) " + "VALUES " + "(" + tid + ", '" + DB.escape(result.getString(3)) + "', -1, UNIX_TIMESTAMP(), 0, " + db.postCount(tid) + ", " + result.getInt(2) + ", 0, 0, 0, " + db.topicFirstId(tid) + ", " + db.topicLastId(tid) + ");");
                count++;
                GUI.progress.setValue(count);
            }
        } catch (SQLException e) {
            DB.exception(e);
        }
    }

    /**
   * Write to writer
   * @param in
   */
    private void write(String in) {
        try {
            writer.write(in + "\n");
            writer.flush();
        } catch (IOException e) {
            System.err.println(e);
            System.exit(87);
        }
    }

    /**
   * Export all
   */
    public void export() {
        forums();
        topics();
        posts();
        close();
    }
}
