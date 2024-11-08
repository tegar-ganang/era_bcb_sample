package org.cmsuite2.job.aggregate.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cmsuite2.job.aggregate.ITableAgregate;
import org.cmsuite2.snmp.TrapEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CompleteTableAggregate implements ITableAgregate, ApplicationContextAware {

    private static Logger logger = Logger.getLogger(CompleteTableAggregate.class);

    private ApplicationContext applicationContext;

    private String schema;

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
    public void aggregate() {
        Connection connection = null;
        PreparedStatement prestm = null;
        try {
            if (logger.isInfoEnabled()) logger.info("aggregate table <" + origin + "> start...");
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
            long currentRows = 0L;
            prestm = connection.prepareStatement("select count(*) from " + origin);
            rs = prestm.executeQuery();
            if (rs.next()) currentRows = rs.getLong(1);
            rs.close();
            prestm.close();
            if (logger.isInfoEnabled()) logger.info("found " + currentRows + " record");
            prestm = connection.prepareStatement("select max(d_insDate) from " + destination);
            rs = prestm.executeQuery();
            Date from = null;
            if (rs.next()) from = rs.getTimestamp(1);
            rs.close();
            prestm.close();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fromStr = null;
            if (from != null) fromStr = sdf.format(from);
            if (logger.isInfoEnabled()) logger.info("last record date:" + fromStr);
            if (currentRows > 0) {
                connection.setAutoCommit(false);
                if (from != null && fromStr != null) {
                    prestm = connection.prepareStatement("INSERT INTO " + destination + " SELECT * FROM " + origin + " WHERE d_insDate > '" + fromStr + "'");
                    if (logger.isDebugEnabled()) logger.debug("Query: INSERT INTO " + destination + " SELECT * FROM " + origin + " WHERE d_insDate > '" + fromStr + "'");
                } else {
                    prestm = connection.prepareStatement("INSERT INTO " + destination + " SELECT * FROM " + origin);
                    if (logger.isDebugEnabled()) logger.debug("Query: INSERT INTO " + destination + " SELECT * FROM " + origin);
                }
                int rows = prestm.executeUpdate();
                prestm.close();
                if (logger.isInfoEnabled()) logger.info(" > " + rows + " rows aggregated");
                connection.commit();
            } else if (logger.isInfoEnabled()) logger.info("no aggregation need");
            if (logger.isInfoEnabled()) logger.info("aggregate table " + origin + " end");
        } catch (SQLException e) {
            logger.error(e, e);
            if (applicationContext != null) applicationContext.publishEvent(new TrapEvent(this, "dbcon", "Errore SQL durante l'aggregazione dei dati della tabella " + origin, e));
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } catch (Throwable e) {
            logger.error(e, e);
            if (applicationContext != null) applicationContext.publishEvent(new TrapEvent(this, "generic", "Errore generico durante l'aggregazione dei dati della tabella " + origin, e));
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

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
