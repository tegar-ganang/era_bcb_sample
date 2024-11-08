package org.fpse.store.impl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fpse.config.AppConfiguration;
import org.fpse.forum.Forum;
import org.fpse.forum.Question;
import org.fpse.forum.TopicArea;
import org.fpse.store.Store;
import org.fpse.topic.Post;
import org.fpse.topic.PostType;
import org.fpse.utils.IOUtils;

/**
 * Created on Dec 13, 2006 5:08:13 PM by Ajay
 */
public class SQLBasedStore implements Store {

    private static final Log LOG = LogFactory.getLog(SQLBasedStore.class);

    private Connection m_connection;

    private AppConfiguration m_configuration;

    public SQLBasedStore() throws ClassNotFoundException {
        m_configuration = AppConfiguration.getInstance();
        String className = m_configuration.getString("database.class-name");
        if (null == className || className.trim().length() == 0) throw new ClassNotFoundException("No database driver class name found on the configuration file.");
        Class.forName(className);
    }

    private void ensureConnection() throws SQLException {
        if (null == m_connection || m_connection.isClosed()) {
            if (null != m_connection) {
                try {
                    m_connection.close();
                } catch (SQLException _) {
                }
            }
            m_connection = DriverManager.getConnection(m_configuration.getString("database.url"), m_configuration.getString("database.user"), m_configuration.getString("database.password"));
            m_connection.setAutoCommit(false);
        }
    }

    public void close() {
        if (null == m_connection) return;
        try {
            m_connection.close();
        } catch (SQLException _) {
        }
        m_connection = null;
    }

    public List<Question> load(TopicArea ta, Date date) throws SQLException {
        boolean loadBasedOnLimit = null == date;
        String query = "select * from fs.question where topic = ? and deleted = 0 and closed = 0 ";
        if (loadBasedOnLimit) query += " order by id desc limit ?"; else query += " and fs.question.qid in (select qid from fs.post where fs.post.question = fs.question.id and fs.post.tid = ? and date(fs.post.posted_date) > ?)";
        ensureConnection();
        PreparedStatement statement = m_connection.prepareStatement(query);
        ResultSet result = null;
        List<Question> list = new ArrayList<Question>();
        try {
            statement.setLong(1, ta.getDatabaseID());
            if (loadBasedOnLimit) statement.setInt(2, 20); else {
                statement.setString(2, ta.getName());
                statement.setDate(3, new java.sql.Date(date.getTime()));
            }
            result = statement.executeQuery();
            while (result.next()) {
                String id = result.getString("qid");
                Question question = ta.getConfig().createQuestion(id, ta, result.getString("title"), result.getString("asker"));
                question.setDatabaseID(result.getLong("id"));
                question.setPoints(result.getLong("points"));
                list.add(question);
            }
            return list;
        } finally {
            if (null != result) {
                try {
                    result.close();
                } catch (SQLException _) {
                }
            }
            try {
                statement.close();
            } catch (SQLException _) {
            }
        }
    }

    public Set<Post> loadPost(Question question) throws SQLException {
        String query = "select * from fs.post where question = ? and deleted = ?";
        ensureConnection();
        PreparedStatement statement = null;
        Savepoint savepoint = null;
        ResultSet result = null;
        Set<Post> set = new TreeSet<Post>();
        try {
            savepoint = m_connection.setSavepoint();
            statement = m_connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            statement.setLong(1, question.getDatabaseID());
            statement.setBoolean(2, false);
            result = statement.executeQuery();
            while (result.next()) {
                Post post = question.getConfig().createPost(question, result.getString("author"), result.getTimestamp("posted_date"), result.getString("pid"), PostType.valueOf(result.getString("type")));
                post.setDatabaseID(result.getLong("id"));
                if (!result.getBoolean("path")) {
                    String text = result.getString("text");
                    if (text.length() > m_configuration.getThresholdByteSize()) {
                        result.updateBoolean("path", true);
                        String path = getPath(post);
                        IOUtils.writeIntoFile(new File(m_configuration.getRepository(), path), text);
                        result.updateString("text", path);
                        result.updateRow();
                        post.setText(new DelayedLoadString(new File(m_configuration.getRepository(), path)));
                    } else post.setText(text);
                } else {
                    File file = new File(m_configuration.getRepository(), result.getString("text"));
                    if (file.isDirectory()) throw new SQLException("The date file '" + file.getAbsolutePath() + "' is a directory."); else if (!file.exists()) throw new SQLException("The data file '" + file.getAbsolutePath() + "' is not found."); else if (!file.canRead()) throw new SQLException("The data file '" + file.getAbsolutePath() + "' can't be read.");
                    post.setText(new DelayedLoadString(file));
                }
                set.add(post);
            }
            m_connection.commit();
            return set;
        } catch (SQLException e) {
            m_connection.rollback(savepoint);
            throw e;
        } catch (IOException e) {
            m_connection.rollback(savepoint);
            SQLException ex = new SQLException();
            ex.initCause(e);
            throw ex;
        } finally {
            if (null != result) {
                try {
                    result.close();
                } catch (SQLException _) {
                }
            }
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    public Question loadQuestion(TopicArea topicArea, String id) throws SQLException {
        String query = "select * from fs.question where topic = ? and qid = ?";
        ensureConnection();
        PreparedStatement statement = m_connection.prepareStatement(query);
        ResultSet result = null;
        try {
            statement.setLong(1, topicArea.getDatabaseID());
            statement.setString(2, id);
            result = statement.executeQuery();
            if (!result.next()) return null;
            Question question = topicArea.getConfig().createQuestion(id, topicArea, result.getString("title"), result.getString("asker"));
            question.setDatabaseID(result.getLong("id"));
            question.setPoints(result.getLong("points"));
            return question;
        } finally {
            if (null != result) {
                try {
                    result.close();
                } catch (SQLException _) {
                }
            }
            try {
                statement.close();
            } catch (SQLException _) {
            }
        }
    }

    public void store(Forum forum) throws SQLException {
        String query = "replace into fs.forum (name, location) values (?, ?)";
        ensureConnection();
        PreparedStatement statement = null;
        Savepoint savepoint = null;
        try {
            savepoint = m_connection.setSavepoint();
            statement = m_connection.prepareStatement(query);
            statement.setString(1, forum.getName());
            statement.setString(2, forum.getDownloadLocation());
            int count = statement.executeUpdate();
            if (0 == count) throw new SQLException("Nothing updated.");
            m_connection.commit();
        } catch (SQLException e) {
            m_connection.rollback(savepoint);
            throw e;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    private boolean hasInDatabase(TopicArea topic) throws SQLException {
        ensureConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            String query = "select id from fs.topic_area where (forum = ? and name= ?";
            if (topic.getIdentifier() != null) {
                query += " and uid = ?";
            }
            query += ") or id = ?";
            statement = m_connection.prepareStatement(query);
            if (topic.getDatabaseID() == null) statement.setNull(4, Types.NUMERIC); else statement.setLong(4, topic.getDatabaseID());
            statement.setString(1, topic.getForum().getName());
            statement.setString(2, topic.getName());
            if (topic.getIdentifier() != null) {
                statement.setString(3, topic.getIdentifier());
            }
            rs = statement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("id");
                if (topic.getDatabaseID() == null) {
                    topic.setDatabaseID(id);
                } else if (id != topic.getDatabaseID()) {
                    topic.setDatabaseID(id);
                }
                return true;
            }
            return false;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException _) {
                }
            }
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    public void store(TopicArea topicArea) throws SQLException {
        String insertQuery = "insert into fs.topic_area (id, forum, name, uid, charset) values (?, ?, ?, ?, ?)";
        String updateQuery = "update fs.topic_area set forum = ?, name = ?, uid = ?, charset = ? where id = ?";
        ensureConnection();
        PreparedStatement statement = null;
        ResultSet keys = null;
        Savepoint savepoint = null;
        try {
            savepoint = m_connection.setSavepoint();
            if (!hasInDatabase(topicArea)) {
                statement = m_connection.prepareStatement(insertQuery);
                if (topicArea.getDatabaseID() == null) statement.setNull(1, Types.NUMERIC); else statement.setLong(1, topicArea.getDatabaseID());
                statement.setString(2, topicArea.getForum().getName());
                statement.setString(3, topicArea.getName());
                statement.setString(4, topicArea.getIdentifier());
                statement.setString(5, topicArea.getCharset());
                int count = statement.executeUpdate();
                if (0 == count) throw new SQLException("Nothing updated.");
                if (topicArea.getDatabaseID() == null) {
                    keys = statement.getGeneratedKeys();
                    if (keys.next()) topicArea.setDatabaseID(keys.getLong(1)); else throw new SQLException("No key found.");
                }
            } else {
                statement = m_connection.prepareStatement(updateQuery);
                statement.setLong(5, topicArea.getDatabaseID());
                statement.setString(1, topicArea.getForum().getName());
                statement.setString(2, topicArea.getName());
                statement.setString(3, topicArea.getIdentifier());
                statement.setString(4, topicArea.getCharset());
                int count = statement.executeUpdate();
                if (0 == count) throw new SQLException("Nothing updated.");
            }
            m_connection.commit();
        } catch (SQLException e) {
            m_connection.rollback(savepoint);
            throw e;
        } finally {
            if (null != keys) {
                try {
                    keys.close();
                } catch (SQLException _) {
                }
            }
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    private boolean hasInDatabase(Question question) throws SQLException {
        ensureConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = m_connection.prepareStatement("select id, asker from question where (topic = ? and qid = ?) or id = ?");
            if (question.getDatabaseID() == null) statement.setNull(3, Types.NUMERIC); else statement.setLong(3, question.getDatabaseID());
            statement.setLong(1, question.getTopicArea().getDatabaseID());
            statement.setString(2, question.getIdentifier());
            rs = statement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("id");
                if (question.getDatabaseID() == null) {
                    question.setDatabaseID(id);
                    if (question.getAsker() == null) question.setAsker(rs.getString("asker"));
                } else if (id != question.getDatabaseID()) {
                    question.setDatabaseID(id);
                    if (question.getAsker() == null) question.setAsker(rs.getString("asker"));
                }
                return true;
            }
            return false;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException _) {
                }
            }
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    public void store(Question question, boolean saveQuestion) throws SQLException {
        String insertQuery = "insert into fs.question (id, topic, qid, title, asker, points, deleted, closed) values (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateQuery = "update fs.question set topic = ?, qid = ?, title = ?, asker = ?, points = ?, deleted = ?, closed = ? where id = ?";
        ensureConnection();
        PreparedStatement statement = null;
        ResultSet keys = null;
        Savepoint savepoint = null;
        try {
            savepoint = m_connection.setSavepoint();
            boolean insert = !hasInDatabase(question);
            if (!saveQuestion) return;
            if (insert) {
                statement = m_connection.prepareStatement(insertQuery);
                if (null == question.getDatabaseID()) statement.setNull(1, Types.NUMERIC); else statement.setLong(1, question.getDatabaseID());
                statement.setLong(2, question.getTopicArea().getDatabaseID());
                statement.setString(3, question.getIdentifier());
                statement.setString(4, question.getTitle());
                statement.setString(5, question.getAsker());
                statement.setLong(6, question.getPoints());
                statement.setInt(7, question.isDeleted() ? 1 : 0);
                statement.setInt(8, question.isClosed() ? 1 : 0);
                int count = statement.executeUpdate();
                if (0 == count) throw new SQLException("Nothing updated.");
                if (question.getDatabaseID() == null) {
                    keys = statement.getGeneratedKeys();
                    if (keys.next()) question.setDatabaseID(keys.getLong(1)); else throw new SQLException("No key found.");
                }
            } else {
                statement = m_connection.prepareStatement(updateQuery);
                statement.setLong(8, question.getDatabaseID());
                statement.setLong(1, question.getTopicArea().getDatabaseID());
                statement.setString(2, question.getIdentifier());
                statement.setString(3, question.getTitle());
                statement.setString(4, question.getAsker());
                statement.setLong(5, question.getPoints());
                statement.setInt(6, question.isDeleted() ? 1 : 0);
                statement.setInt(7, question.isClosed() ? 1 : 0);
                int count = statement.executeUpdate();
                if (0 == count) throw new SQLException("Nothing updated.");
            }
            m_connection.commit();
        } catch (SQLException e) {
            m_connection.rollback(savepoint);
            throw e;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    private boolean hasInDatabase(Post post) throws SQLException {
        ensureConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = m_connection.prepareStatement("select id from fs.post where (forum = ? and tid = ? and qid = ? and pid = ?) or id = ?");
            if (post.getDatabaseID() == null) statement.setNull(5, Types.NUMERIC); else statement.setLong(5, post.getDatabaseID());
            statement.setString(1, post.getQuestion().getTopicArea().getForum().getName());
            statement.setString(2, post.getQuestion().getTopicArea().getIdentifier());
            statement.setString(3, post.getQuestion().getIdentifier());
            statement.setString(4, post.getIdentifier());
            rs = statement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("id");
                if (post.getDatabaseID() == null) {
                    post.setDatabaseID(id);
                } else if (post.getDatabaseID() != id) {
                    post.setDatabaseID(id);
                }
                return true;
            }
            return false;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException _) {
                }
            }
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    public void store(Set<Post> posts) throws SQLException {
        store(posts, false);
    }

    private void store(Set<Post> posts, boolean delete) throws SQLException {
        if (null == posts || posts.isEmpty()) return;
        String insertQuery = "insert into fs.post (id, forum, tid, qid, pid, question, posted_date, type, text, author, deleted, path) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateQuery = "update fs.post set forum = ?, tid = ?, qid = ?, pid = ?, question = ?, posted_date = ?, type = ?, text = ?, author = ?, deleted = ?, path = ? where id = ?";
        ensureConnection();
        PreparedStatement statement = null;
        Savepoint savepoint = null;
        String path = null;
        try {
            savepoint = m_connection.setSavepoint();
            synchronized (posts) {
                for (Post post : posts) {
                    path = null;
                    boolean insert = !hasInDatabase(post);
                    if (insert) {
                        statement = m_connection.prepareStatement(insertQuery);
                        statement.setNull(1, Types.NUMERIC);
                        statement.setString(2, post.getQuestion().getTopicArea().getForum().getIdentifier());
                        statement.setString(3, post.getQuestion().getTopicArea().getIdentifier());
                        statement.setString(4, post.getQuestion().getIdentifier());
                        statement.setString(5, post.getIdentifier());
                        statement.setLong(6, post.getQuestion().getDatabaseID());
                        statement.setTimestamp(7, new Timestamp(post.getPostedDate().getTime()));
                        statement.setString(8, post.getType().toString());
                        int contentLength = 0;
                        if (null != post.getText()) contentLength = post.getText().toString().length();
                        if (contentLength < m_configuration.getThresholdByteSize()) {
                            statement.setString(9, post.getText().toString());
                            statement.setBoolean(12, false);
                        } else {
                            path = getPath(post);
                            statement.setString(9, path);
                            try {
                                IOUtils.writeIntoFile(new File(m_configuration.getRepository(), path), post.getText().toString());
                            } catch (IOException e) {
                                SQLException sqle = new SQLException();
                                sqle.initCause(e);
                                throw sqle;
                            }
                            statement.setBoolean(12, true);
                        }
                        statement.setString(10, post.getAuthor());
                        statement.setBoolean(11, delete);
                        int count = statement.executeUpdate();
                        if (0 == count) throw new SQLException("Nothing updated.");
                        ResultSet keys = statement.getGeneratedKeys();
                        try {
                            if (keys.next()) post.setDatabaseID(keys.getLong(1)); else throw new SQLException("No key found.");
                        } finally {
                            try {
                                keys.close();
                            } catch (SQLException _) {
                            }
                        }
                    } else {
                        statement = m_connection.prepareStatement(updateQuery);
                        statement.setLong(12, post.getDatabaseID());
                        statement.setString(1, post.getQuestion().getTopicArea().getForum().getIdentifier());
                        statement.setString(2, post.getQuestion().getTopicArea().getIdentifier());
                        statement.setString(3, post.getQuestion().getIdentifier());
                        statement.setString(4, post.getIdentifier());
                        statement.setLong(5, post.getQuestion().getDatabaseID());
                        statement.setTimestamp(6, new Timestamp(post.getPostedDate().getTime()));
                        statement.setString(7, post.getType().toString());
                        int contentLength = 0;
                        if (null != post.getText()) contentLength = post.getText().toString().length();
                        if (contentLength < m_configuration.getThresholdByteSize()) {
                            String pathIfThere = getPath(post);
                            File file = new File(m_configuration.getRepository(), pathIfThere);
                            if (file.exists()) delete(file);
                            statement.setString(8, post.getText().toString());
                            statement.setBoolean(11, false);
                        } else {
                            path = getPath(post);
                            statement.setString(8, path);
                            File file = new File(m_configuration.getRepository(), path);
                            try {
                                IOUtils.writeIntoFile(file, post.getText().toString());
                            } catch (IOException e) {
                                SQLException sqle = new SQLException();
                                sqle.initCause(e);
                                throw sqle;
                            }
                            statement.setBoolean(11, true);
                            post.setText(new DelayedLoadString(file));
                        }
                        statement.setString(9, post.getAuthor());
                        statement.setBoolean(10, delete);
                        int count = statement.executeUpdate();
                        if (0 == count) throw new SQLException("Nothing updated.");
                    }
                    PreparedStatement statement1 = m_connection.prepareStatement("show warnings");
                    try {
                        ResultSet rs1 = statement1.executeQuery();
                        if (rs1.next()) {
                            try {
                                System.err.println(rs1.getString("Message"));
                            } finally {
                                rs1.close();
                            }
                        }
                    } finally {
                        statement1.close();
                    }
                    statement1 = m_connection.prepareStatement("select * from fs.post where id = ?");
                    try {
                        statement1.setLong(1, post.getDatabaseID());
                        ResultSet rs1 = statement1.executeQuery();
                        try {
                            if (!rs1.next()) throw new SQLException("Nothing updated.");
                        } finally {
                            rs1.close();
                        }
                    } finally {
                        statement1.close();
                    }
                    LOG.info("Post [ID=" + post.getIdentifier() + "] [Question=" + post.getQuestion().getIdentifier() + "] [Database ID=" + post.getDatabaseID() + "] [Title=" + post.getQuestion().getTitle() + "] has been updated.");
                }
            }
            m_connection.commit();
        } catch (SQLException e) {
            m_connection.rollback(savepoint);
            delete(path);
            throw e;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        }
    }

    private void delete(String path) {
        if (null == path) return;
        File file = new File(m_configuration.getRepository(), path);
        delete(file);
    }

    private void delete(File file) {
        if (!file.exists()) return;
        if (file.isFile()) {
            if (!file.delete()) {
                LOG.error("Unable to delete the file: " + file);
                return;
            }
        }
        File[] all = file.listFiles();
        if (null == all || all.length == 0) {
            if (!file.delete()) {
                LOG.error("Unable to delete the directory: " + file);
                return;
            }
        }
        boolean onlyFolders = true;
        for (int i = 0; i < all.length && onlyFolders; i++) {
            onlyFolders = all[i].isDirectory();
        }
        if (onlyFolders) {
            for (int i = 0; i < all.length; i++) delete(all[i]);
        }
    }

    private String getPath(Post post) {
        StringBuilder builder = new StringBuilder(512);
        add(builder, post.getQuestion().getTopicArea().getForum().getIdentifier());
        builder.append("/");
        add(builder, post.getQuestion().getTopicArea().getName());
        builder.append("/");
        add(builder, post.getQuestion().getIdentifier());
        builder.append("/");
        add(builder, post.getIdentifier());
        return builder.toString();
    }

    private void add(StringBuilder builder, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c) || c == '.' || c == '_' || c == '-') builder.append(c); else if (Character.isSpaceChar(c)) builder.append(' ');
        }
    }

    public Set<PostMeta> loadUnread(String user, String forum, int max) throws SQLException {
        long start = System.currentTimeMillis();
        try {
            String query = "select * from fs.post where forum = ? and deleted = ? and id not in (select post from fs.read_post where user = ? and forum = ?) order by posted_date desc, qid limit ?";
            ensureConnection();
            PreparedStatement statement = m_connection.prepareStatement(query);
            ResultSet result = null;
            Set<PostMeta> set = new HashSet<PostMeta>();
            try {
                statement.setString(1, forum);
                statement.setBoolean(2, false);
                statement.setString(3, user);
                statement.setString(4, forum);
                statement.setInt(5, max);
                result = statement.executeQuery();
                while (result.next()) {
                    String topicArea = result.getString("tid");
                    String question = result.getString("qid");
                    String post = result.getString("pid");
                    long databaseID = result.getLong("id");
                    PostMeta meta = new PostMeta(topicArea, question, post, databaseID, false);
                    set.add(meta);
                }
                return set;
            } finally {
                if (null != result) {
                    try {
                        result.close();
                    } catch (SQLException _) {
                    }
                }
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
        } finally {
            LOG.debug("Unread load time (" + forum + ") = " + (System.currentTimeMillis() - start));
        }
    }

    public void makeRead(String user, PostMeta meta) throws SQLException {
        makeRead(user, meta.getDatabaseID(), System.currentTimeMillis());
    }

    public void makeRead(String user, Post post) throws SQLException {
        makeRead(user, post.getDatabaseID(), System.currentTimeMillis());
    }

    public void makeRead(String user, long databaseID, long time) throws SQLException {
        String query = "replace into fs.read_post (post, user, read_date) values (?, ?, ?)";
        ensureConnection();
        PreparedStatement statement = m_connection.prepareStatement(query);
        try {
            statement.setLong(1, databaseID);
            statement.setString(2, user);
            statement.setTimestamp(3, new Timestamp(time));
            int count = statement.executeUpdate();
            if (0 == count) throw new SQLException("Nothing updated.");
            m_connection.commit();
        } catch (SQLException e) {
            m_connection.rollback();
            throw e;
        } finally {
            statement.close();
        }
    }

    public Set<TopicArea> load(Forum forum) throws SQLException {
        String query = "select * from fs.topic_area where forum = ?";
        ensureConnection();
        PreparedStatement statement = m_connection.prepareStatement(query);
        ResultSet result = null;
        Set<TopicArea> set = new TreeSet<TopicArea>();
        try {
            statement.setString(1, forum.getName());
            result = statement.executeQuery();
            while (result.next()) {
                String name = result.getString("name");
                String uid = result.getString("uid");
                TopicArea ta = forum.getConfig().createTopicArea(forum, name, uid);
                ta.setDatabaseID(result.getLong("id"));
                set.add(ta);
            }
            return set;
        } finally {
            if (null != result) {
                try {
                    result.close();
                } catch (SQLException _) {
                }
            }
            try {
                statement.close();
            } catch (SQLException _) {
            }
        }
    }

    public void store(Post... posts) throws SQLException {
        if (null == posts || 0 == posts.length) return;
        store(new HashSet<Post>(Arrays.asList(posts)));
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        SQLBasedStore store = new SQLBasedStore();
        PreparedStatement statement = null;
        try {
            store.ensureConnection();
            statement = store.m_connection.prepareStatement("insert into fs.post (id, forum, tid, qid, pid, question, posted_date, type, text, author) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setNull(1, Types.NUMERIC);
            statement.setString(2, "www.experts-exchange.com");
            statement.setString(3, "Java Programming");
            statement.setString(4, "22096757");
            statement.setString(5, "0");
            statement.setLong(6, 41);
            statement.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            statement.setString(8, "Question");
            statement.setString(9, "text");
            statement.setString(10, "huzefaq");
            int count = statement.executeUpdate();
            if (0 == count) throw new SQLException("Nothing updated.");
            ResultSet keys = statement.getGeneratedKeys();
            try {
                if (keys.next()) {
                    System.out.println(keys.getLong(1));
                } else throw new SQLException("No key found.");
            } finally {
                try {
                    keys.close();
                } catch (SQLException _) {
                }
            }
            store.m_connection.commit();
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException _) {
                }
            }
            store.close();
        }
    }

    public void delete(Post... posts) throws SQLException {
        if (null == posts || 0 == posts.length) return;
        store(new HashSet<Post>(Arrays.asList(posts)), true);
    }

    public String getMaxQuestionId(String name) throws SQLException {
        String query = "select qid from fs.post where forum = ? and deleted = ? order by qid desc limit 1";
        ensureConnection();
        PreparedStatement statement = m_connection.prepareStatement(query);
        ResultSet result = null;
        try {
            statement.setString(1, name);
            statement.setBoolean(2, false);
            result = statement.executeQuery();
            while (result.next()) {
                return result.getString("qid");
            }
            return null;
        } finally {
            if (null != result) {
                try {
                    result.close();
                } catch (SQLException _) {
                }
            }
            try {
                statement.close();
            } catch (SQLException _) {
            }
        }
    }
}
