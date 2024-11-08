package org.tolk.io.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;
import org.tolk.io.DataSource;

/**
 * Class to connect, read and write to a Database
 * 
 * @author Johan Roets
 */
public class DataBaseDataSource extends DataSource implements InitializingBean {

    private static Logger logger = Logger.getLogger(DataBaseDataSource.class);

    private SimpleJdbcTemplate jdbcTemplate;

    private String readAllTagsSql;

    private String writeTagSql;

    /**
     * see {@link DataSource#read()}
     * 
     * This method does not make any logical sense and thus will throw a NoSuchMethodError when called. Normally, this error is
     * caught by the compiler; this error can only occur at run time if the definition of a class has incompatibly changed.
     * 
     * see {@link NoSuchMethodError}
     */
    @Override
    public String read() {
        throw new NoSuchMethodError();
    }

    /**
     * see {@link DataBaseDataSource#write(String)}
     */
    @Override
    public void write(String str) {
        if (this.jdbcTemplate != null) {
            this.jdbcTemplate.update(this.writeTagSql, str);
        }
    }

    /**
     * see {@link InitializingBean#afterPropertiesSet()}
     */
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.jdbcTemplate, "jdbcTemplate cannot be null");
        Assert.notNull(this.readAllTagsSql, "insertTagSql cannot be null");
        Assert.notNull(this.writeTagSql, "readAllTagsFromTableSql cannot be null");
    }

    /**
     * @param jdbcTemplate
     *            the jdbcTemplate to set
     */
    void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param readAllTagsSql
     *            the readAllTagsSql to set
     */
    void setReadAllTagsSql(String readAllTagsSql) {
        this.readAllTagsSql = readAllTagsSql;
    }

    /**
     * @param writeTagSql
     *            the writeTagSql to set
     */
    void setWriteTagSql(String writeTagSql) {
        this.writeTagSql = writeTagSql;
    }

    /**
     * Implementation of ParameterizedRowMapper class. see {@link ParameterizedRowMapper}
     * 
     * @author Johan Roets
     */
    class DefaultRowmapper implements ParameterizedRowMapper<String> {

        /**
         * see {@link ParameterizedRowMapper#mapRow(ResultSet, int)}
         */
        public String mapRow(ResultSet rs, int arg1) throws SQLException {
            String tag = rs.getString(1);
            logger.info("read tag from db " + tag);
            return tag;
        }
    }
}
