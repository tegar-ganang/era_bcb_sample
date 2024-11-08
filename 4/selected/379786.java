package org.easyrec.utils.spring.store.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easyrec.utils.spring.store.dao.DBRootDAO;
import org.easyrec.utils.spring.store.dao.annotation.DAO;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a Mysql implementation of the {@link org.easyrec.utils.spring.store.dao.DBRootDAO} interface. It
 * provides
 * methods to create and delete databases;
 * <p/>
 * <p><b>Company:&nbsp;</b>
 * SAT, Research Studios Austria</p>
 * <p/>
 * <p><b>Copyright:&nbsp;</b>
 * (c) 2007</p>
 * <p/>
 * <p><b>last modified:</b><br/>
 * $Author: sat-rsa $<br/>
 * $Date: 2011-08-12 12:40:54 -0400 (Fri, 12 Aug 2011) $<br/>
 * $Revision: 119 $</p>
 *
 * @author Roman Cerny
 */
@DAO
public class DBRootDAOMysqlImpl extends JdbcDaoSupport implements DBRootDAO {

    public static final boolean DEFAULT_OVERWRITE_EXISTING_DATABASE = false;

    public static final boolean DEFAULT_CHECK_EXISTING_DATABASE = true;

    public static final boolean DEFAULT_OMIT_ERROR_IF_EXISTING = false;

    private final Log logger = LogFactory.getLog(this.getClass());

    public DBRootDAOMysqlImpl(DataSource dataSource) {
        setDataSource(dataSource);
    }

    public void createDatabase(String databaseName) {
        createDatabase(databaseName, DEFAULT_OVERWRITE_EXISTING_DATABASE);
    }

    public void createDatabase(String databaseName, boolean overwriteIfExists) {
        createDatabase(databaseName, overwriteIfExists, DEFAULT_CHECK_EXISTING_DATABASE);
    }

    public void createDatabase(String databaseName, boolean overwriteIfExists, boolean checkExisting) {
        createDatabase(databaseName, overwriteIfExists, checkExisting, DEFAULT_OMIT_ERROR_IF_EXISTING);
    }

    public void createDatabase(String databaseName, boolean overwriteIfExists, boolean checkExisting, boolean omitErrorIfExisting) {
        if (databaseName == null || "".equals(databaseName)) {
            throw new IllegalArgumentException("passed param 'databaseName' must not be null or empty");
        }
        if (checkExisting) {
            if (existsDatabase(databaseName)) {
                if (overwriteIfExists) {
                    deleteDatabase(databaseName);
                } else {
                    if (omitErrorIfExisting) {
                        logger.warn("the database '" + databaseName + "' already exists, could not re-create database (since overwrite=" + overwriteIfExists + " was passed), using OLD existing database");
                        return;
                    } else {
                        throw new IllegalArgumentException("the database '" + databaseName + "' already exists, could not re-create database (since overwrite=" + overwriteIfExists + " was passed)");
                    }
                }
            }
        }
        StringBuilder ddl = new StringBuilder("CREATE DATABASE ");
        ddl.append(databaseName);
        getJdbcTemplate().execute(ddl.toString());
        if (logger.isDebugEnabled()) {
            logger.debug("database '" + databaseName + "' has been created successfully");
            logger.debug("used following statement: " + ddl);
        }
    }

    public void deleteDatabase(String databaseName) {
        deleteDatabase(databaseName, DEFAULT_CHECK_EXISTING_DATABASE);
    }

    public void deleteDatabase(String databaseName, boolean checkExisting) {
        if (checkExisting) {
            if (!existsDatabase(databaseName)) {
                throw new IllegalArgumentException("the database '" + databaseName + "' does not exist, it can not be deleted");
            }
        }
        StringBuilder ddl = new StringBuilder("DROP DATABASE ");
        ddl.append(databaseName);
        getJdbcTemplate().execute(ddl.toString());
        if (logger.isDebugEnabled()) {
            logger.debug("database '" + databaseName + "' has been deleted successfully");
            logger.debug("used following statement: " + ddl);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public boolean existsDatabase(String databaseName) {
        List<String> dbNames;
        DatabaseMetaDataCallback callback = new DatabaseMetaDataCallback() {

            public Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException {
                List<String> ret = new LinkedList<String>();
                ResultSet rs = dbmd.getCatalogs();
                while (rs.next()) {
                    ret.add(rs.getString(1));
                }
                return ret;
            }
        };
        try {
            dbNames = (List<String>) JdbcUtils.extractDatabaseMetaData(getDataSource(), callback);
        } catch (Exception e) {
            throw new RuntimeException("unable to read database metadata", e);
        }
        return dbNames.contains(databaseName);
    }
}
