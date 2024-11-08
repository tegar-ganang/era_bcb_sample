package es.f2020.osseo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import es.f2020.osseo.core.OsseoFailure;
import es.f2020.osseo.core.util.DB;
import es.f2020.osseo.domain.Keyphrase;
import es.f2020.osseo.domain.Website;

/**
 *  Persistance class for websites.
 */
public class WebsiteDAO {

    private DB db;

    public WebsiteDAO(ServletContext servletContext) {
        this.db = new DB(servletContext);
    }

    public Website load(int id) {
        Website website = null;
        Connection conn = null;
        try {
            conn = db.getConnection();
            String sql1 = "SELECT url, name, description FROM websites WHERE id=?";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setInt(1, id);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                String url = rs1.getString("url");
                String name = rs1.getString("name");
                String description = rs1.getString("description");
                String sql2 = "SELECT id, keyphrase FROM keyphrases WHERE website_id=? ORDER BY id";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, id);
                ResultSet rs2 = ps2.executeQuery();
                List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
                while (rs2.next()) {
                    keyphrases.add(new Keyphrase(rs2.getInt("id"), id, rs2.getString("keyphrase")));
                }
                website = new Website(id, url, name, description, keyphrases);
                rs2.close();
                ps2.close();
            }
            rs1.close();
            ps1.close();
        } catch (SQLException ex) {
            throw new OsseoFailure("SQL error: cannot read website with id " + id + ".", ex);
        } finally {
            db.putConnection(conn);
        }
        return website;
    }

    public List<Website> loadAll() {
        List<Website> websites = new ArrayList<Website>();
        Connection conn = null;
        try {
            conn = db.getConnection();
            String sql1 = "SELECT id, url, name, description FROM websites";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                int id = rs1.getInt("id");
                String url = rs1.getString("url");
                String name = rs1.getString("name");
                String description = rs1.getString("description");
                String sql2 = "SELECT id, keyphrase FROM keyphrases WHERE website_id=? ORDER BY id";
                PreparedStatement ps2 = conn.prepareStatement(sql2);
                ps2.setInt(1, id);
                ResultSet rs2 = ps2.executeQuery();
                List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
                while (rs2.next()) {
                    keyphrases.add(new Keyphrase(rs2.getInt("id"), id, rs2.getString("keyphrase")));
                }
                Website website = new Website(id, url, name, description, keyphrases);
                websites.add(website);
                rs2.close();
                ps2.close();
            }
            rs1.close();
            ps1.close();
        } catch (SQLException ex) {
            throw new OsseoFailure("SQL error: cannot read website list. ", ex);
        } finally {
            db.putConnection(conn);
        }
        return websites;
    }

    public Website save(Website website) {
        Connection conn = null;
        try {
            conn = db.getConnection();
            String sql = "INSERT INTO websites (name, url, description) VALUES (?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, website.getName());
            ps.setString(2, website.getUrl());
            ps.setString(3, website.getDescription());
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int id = rs.getInt(1);
            website.setId(id);
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            throw new OsseoFailure("SQL error: cannot store website. ", ex);
        } finally {
            db.putConnection(conn);
        }
        return website;
    }

    public boolean delete(int id) {
        boolean deletionOk = false;
        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);
            String sql = "DELETE FROM keyphrases WHERE website_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            deletionOk = ps.executeUpdate() == 1;
            ps.close();
            sql = "DELETE FROM websites WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            boolean success = ps.executeUpdate() == 1;
            deletionOk = deletionOk && success;
            ps.close();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException sqle) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException sex) {
                throw new OsseoFailure("SQL error: roll back failed. ", sex);
            }
            throw new OsseoFailure("SQL error: cannot remove website with id " + id + ".", sqle);
        } finally {
            db.putConnection(conn);
        }
        return deletionOk;
    }
}
