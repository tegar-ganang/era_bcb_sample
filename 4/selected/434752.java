package org.tolk.io.extension.ipico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.tolk.util.extension.ipico.ReaderVo;

/**
 * Class to connect, read and write to a Database and also lookup a tag and return a readerVo
 * 
 * @author Werner van Rensburg
 */
public class ReaderDao extends IpicoDao {

    private static final String readReaderSql = "readReaderSql";

    private static final String readReaderNameSql = "readReaderNameSql";

    private final Logger logger = Logger.getLogger(ReaderDao.class);

    /**
     * @param readerId
     *            the readerId to lookup
     * 
     * @return the readerVo if found.
     */
    public ReaderVo getReaderVo(String readerId) {
        try {
            List<ReaderVo> results = this.jdbcTemplate.query(this.sqlStatements.get(readReaderSql), new ReaderRowmapper(), new Object[] { readerId });
            if (results.size() > 0) {
                return results.get(0);
            }
        } catch (Exception e) {
            logger.info("tbl$reader does not exist. Creating now.");
            initTable();
        }
        return null;
    }

    /**
     * @param readerId
     *            the readerId to lookup
     * 
     * @return the readerVo if found.
     */
    public ReaderVo getReaderVoName(String readerName) {
        try {
            List<ReaderVo> results = this.jdbcTemplate.query(this.sqlStatements.get(readReaderNameSql), new ReaderRowmapper(), new Object[] { readerName });
            if (results.size() > 0) {
                return results.get(0);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            this.initTable();
        }
        return null;
    }

    /**
     * Reads all the ReaderVo's from the database
     * 
     * @return all ReaderVo's in the database.
     */
    public Collection<ReaderVo> readAll() {
        try {
            return this.jdbcTemplate.query(this.sqlStatements.get(readAllSql), new ReaderRowmapper());
        } catch (Exception e) {
            logger.info("ReaderTable does not exist. Creating reader table...");
            this.initTable();
        }
        return null;
    }

    /**
     * 
     */
    public void write(ReaderVo readerVo) {
        String readerId = readerVo.getReaderId();
        String readerName = readerVo.getReaderName();
        int count = 0;
        try {
            count = this.jdbcTemplate.queryForInt(this.sqlStatements.get(existSql), readerId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            this.initTable();
        }
        if (count > 0) {
            this.jdbcTemplate.update(this.sqlStatements.get(updateSql), readerId, readerName, readerId);
        } else {
            this.jdbcTemplate.update(this.sqlStatements.get(insertSql), readerId, readerName);
        }
    }

    /**
     * Checks if the related table exists and creates it if it does not
     */
    public void initTable() {
        try {
            this.jdbcTemplate.query(this.sqlStatements.get(readAllSql), new ReaderRowmapper());
        } catch (Exception e) {
            logger.debug("Trying to create new Table:\n" + this.sqlStatements.get(createTableSql));
            if (e.getMessage().contains("Table not found in statement")) {
                this.jdbcTemplate.update(this.sqlStatements.get(createTableSql), new Object[] {});
                logger.debug("Event Table successfully created");
            }
        }
    }

    /**
     * Implementation of ParameterizedRowMapper class. see {@link ParameterizedRowMapper}
     * 
     * @author Werner van Rensburg
     */
    class ReaderRowmapper implements ParameterizedRowMapper<ReaderVo> {

        /**
         * see {@link ParameterizedRowMapper#mapRow(ResultSet, int)}
         */
        public ReaderVo mapRow(ResultSet rs, int arg1) throws SQLException {
            ReaderVo readerVo = new ReaderVo();
            String readerId = rs.getString(1);
            readerVo.setReaderId(readerId);
            String readerName = rs.getString(2);
            readerVo.setReaderName(readerName);
            return readerVo;
        }
    }
}
