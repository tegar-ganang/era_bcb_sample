package org.cmsuite2.job.backup.impl;

import it.ec.commons.time.TimeUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cmsuite2.job.backup.ITableBackup;
import org.cmsuite2.snmp.TrapEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class BeforeDateTableBackup implements ITableBackup, ApplicationContextAware {

    private static Logger logger = Logger.getLogger(BeforeDateTableBackup.class);

    private ApplicationContext applicationContext;

    private int hours;

    private String schema;

    private String condition;

    private String origin;

    private String destination;

    private String driver;

    private String url;

    private String username;

    private String password;

    private static final String stripRelationTableString = "CONSTRAINT.*|KEY.*|PRI.*|UNIQUE.*|=2|=3|=4|=5|=6|=7";

    private static final String normalizeString = ",\\s*\\)";

    private static Pattern stripRelationTablePattern = Pattern.compile(stripRelationTableString);

    private static Pattern normalizePattern = Pattern.compile(normalizeString);

    @Override
    public void backup() {
        Connection connection = null;
        PreparedStatement prestm = null;
        try {
            if (logger.isInfoEnabled()) logger.info("backup table " + getOrigin() + " start...");
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);
            String tableExistsResult = "";
            prestm = connection.prepareStatement("show tables from " + schema + " like '" + getDestination() + "';");
            ResultSet rs = prestm.executeQuery();
            if (rs.next()) tableExistsResult = rs.getString(1);
            rs.close();
            prestm.close();
            if (StringUtils.isBlank(tableExistsResult)) {
                String createTableSql = "";
                prestm = connection.prepareStatement("show create table " + getOrigin() + ";");
                rs = prestm.executeQuery();
                if (rs.next()) createTableSql = rs.getString(2);
                rs.close();
                prestm.close();
                createTableSql = createTableSql.replaceAll("`" + getOrigin() + "`", "`" + getDestination() + "`");
                createTableSql = createTableSql.replaceAll("auto_increment", "");
                createTableSql = createTableSql.replaceAll("AUTO_INCREMENT", "");
                Matcher matcher = stripRelationTablePattern.matcher(createTableSql);
                if (matcher.find()) createTableSql = matcher.replaceAll("");
                matcher = normalizePattern.matcher(createTableSql);
                if (matcher.find()) createTableSql = matcher.replaceAll("\n )");
                Statement stm = connection.createStatement();
                stm.execute(createTableSql);
                if (logger.isDebugEnabled()) logger.debug("table '" + getDestination() + "' created!");
            } else if (logger.isDebugEnabled()) logger.debug("table '" + getDestination() + "' already exists");
            Date date = new Date();
            date.setTime(TimeUtil.addHours(date, -getHours()).getTimeInMillis());
            date.setTime(TimeUtil.getTodayAtMidnight().getTimeInMillis());
            if (logger.isInfoEnabled()) logger.info("backuping records before: " + date);
            long currentRows = 0L;
            prestm = connection.prepareStatement("select count(*) from " + getOrigin() + " where " + getCondition() + "");
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            prestm.setDate(1, sqlDate);
            rs = prestm.executeQuery();
            if (rs.next()) currentRows = rs.getLong(1);
            rs.close();
            prestm.close();
            if (currentRows > 0) {
                connection.setAutoCommit(false);
                prestm = connection.prepareStatement("INSERT INTO " + getDestination() + " SELECT * FROM " + getOrigin() + " WHERE " + getCondition());
                prestm.setDate(1, sqlDate);
                int rows = prestm.executeUpdate();
                prestm.close();
                if (logger.isInfoEnabled()) logger.info(rows + " rows backupped");
                prestm = connection.prepareStatement("DELETE FROM " + getOrigin() + " WHERE " + getCondition());
                prestm.setDate(1, sqlDate);
                rows = prestm.executeUpdate();
                prestm.close();
                connection.commit();
                if (logger.isInfoEnabled()) logger.info(rows + " rows deleted");
            } else if (logger.isInfoEnabled()) logger.info("no backup need");
            if (logger.isInfoEnabled()) logger.info("backup table " + getOrigin() + " end");
        } catch (SQLException e) {
            logger.error(e, e);
            if (applicationContext != null) applicationContext.publishEvent(new TrapEvent(this, "dbcon", "Errore SQL durante il backup dei dati della tabella " + getOrigin(), e));
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } catch (Throwable e) {
            logger.error(e, e);
            if (applicationContext != null) applicationContext.publishEvent(new TrapEvent(this, "generic", "Errore generico durante il backup dei dati della tabella " + getOrigin(), e));
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            try {
                if (prestm != null) prestm.close();
            } catch (SQLException e) {
            }
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
            }
        }
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }
}
