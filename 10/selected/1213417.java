package org.garret.ptl.startup.migrations;

import org.apache.commons.lang.StringUtils;
import org.garret.ptl.startup.Configuration;
import org.garret.ptl.util.ReflectionUtils;
import org.garret.ptl.util.SystemException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Migration {

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Migration.class);

    private String name;

    private String contactInfo;

    private String dataSource;

    private final List<IMigrationAction> actions = new ArrayList<IMigrationAction>();

    public Migration() {
    }

    /**
     * Constructs Migration object from DOM tree.
     */
    public Migration(Element p_node) {
        name = p_node.getAttribute("name");
        contactInfo = p_node.getAttribute("contactInfo");
        dataSource = p_node.getAttribute("dataSource");
        NodeList nodes = p_node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) continue;
            Element element = (Element) nodes.item(i);
            if ("runSQL".equals(element.getTagName())) {
                actions.add(new RunSQLAction(element));
            } else if ("action".equals(element.getTagName())) {
                String className = element.getAttribute("class");
                actions.add((IMigrationAction) ReflectionUtils.construct(className));
            }
        }
    }

    protected DataSource getDataSource() {
        return Configuration.getSpringBean(StringUtils.isNotEmpty(dataSource) ? dataSource : "dataSource");
    }

    public String getName() {
        return name;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public boolean run() {
        LOGGER.info("Executing migration: " + name);
        for (IMigrationAction action : actions) {
            if (!action.run()) {
                return false;
            }
        }
        return true;
    }

    public boolean wasExecuted() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getDataSource().getConnection();
            stmt = conn.prepareStatement("select 1 from migrations where name=?");
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            throw new SystemException("Cannot use migrations table", ex);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Throwable th) {
                LOGGER.error(th);
            }
            try {
                if (stmt != null) stmt.close();
            } catch (Throwable th) {
                LOGGER.error(th);
            }
            try {
                if (stmt != null) conn.close();
            } catch (Throwable th) {
                LOGGER.error(th);
            }
        }
    }

    public void markExecuted() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getDataSource().getConnection();
            stmt = conn.prepareStatement("insert into migrations (name,contactInfo,completed) values (?,?,?) ");
            stmt.setString(1, name);
            stmt.setString(2, contactInfo);
            stmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new SystemException("Cannot use migrations table", ex);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Throwable th) {
                LOGGER.error(th);
            }
            try {
                if (stmt != null) conn.close();
            } catch (Throwable th) {
                LOGGER.error(th);
            }
        }
    }

    private class RunSQLAction implements IMigrationAction {

        private final List<String> tasks = new ArrayList<String>();

        public RunSQLAction(Element element) {
            NodeList list = element.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() == Node.TEXT_NODE) {
                    StringTokenizer tok = new StringTokenizer(node.getNodeValue(), ";");
                    while (tok.hasMoreTokens()) {
                        tasks.add(tok.nextToken().replaceAll("\r\n", "").trim());
                    }
                }
            }
        }

        public boolean run() {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = getDataSource().getConnection();
                conn.setAutoCommit(false);
                conn.rollback();
                stmt = conn.createStatement();
                for (String task : tasks) {
                    if (task.length() == 0) continue;
                    LOGGER.info("Executing SQL migration: " + task);
                    stmt.executeUpdate(task);
                }
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (Throwable th) {
                }
                throw new SystemException("Cannot execute SQL migration", ex);
            } finally {
                try {
                    if (stmt != null) stmt.close();
                } catch (Throwable th) {
                    LOGGER.error(th);
                }
                try {
                    if (stmt != null) conn.close();
                } catch (Throwable th) {
                    LOGGER.error(th);
                }
            }
            return true;
        }
    }
}
