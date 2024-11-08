package net.sf.mustang.jdbc;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.mustang.orm.Bean;
import net.sf.mustang.orm.BeanInfo;
import net.sf.mustang.util.IOUtils;
import oracle.sql.BLOB;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

public class JdbcOracleBlobber implements JdbcBlobber {

    public void update(Bean bean, InputStream inputStream, BeanInfo beanInfo, JdbcManager jdbcManager) throws Exception {
        jdbcManager.execute(new OracleBlobberTransaction(bean, inputStream, beanInfo, jdbcManager));
    }

    public class OracleBlobberTransaction implements TransactionCallback {

        private Bean bean;

        private InputStream inputStream;

        private BeanInfo beanInfo;

        private JdbcManager jdbcManager;

        public OracleBlobberTransaction(Bean bean, InputStream inputStream, BeanInfo beanInfo, JdbcManager jdbcManager) {
            this.bean = bean;
            this.inputStream = inputStream;
            this.beanInfo = beanInfo;
            this.jdbcManager = jdbcManager;
        }

        public Object doInTransaction(TransactionStatus status) {
            jdbcManager.updateScript(beanInfo.getBlobInfo(jdbcManager.getDb()).getUpdateScriptList().get(0), bean);
            jdbcManager.queryScript(beanInfo.getBlobInfo(jdbcManager.getDb()).getUpdateScriptList().get(1), bean, new OracleBlobberRowMapper(inputStream));
            return null;
        }
    }

    public class OracleBlobberRowMapper implements RowMapper {

        private InputStream inputStream;

        public OracleBlobberRowMapper(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public Object mapRow(ResultSet rs, int i) throws SQLException {
            try {
                BLOB blob = (BLOB) rs.getBlob(1);
                OutputStream outputStream = blob.setBinaryStream(0L);
                IOUtils.copy(inputStream, outputStream);
                outputStream.close();
                inputStream.close();
            } catch (Exception e) {
                throw new SQLException(e.getMessage());
            }
            return null;
        }
    }
}
