package org.openedc.demo;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author peter
 */
public class SchemaAndDataPopulator implements InitializingBean {

    private Logger logger = LoggerFactory.getLogger(SchemaAndDataPopulator.class);

    public static final String SQL_STATEMENT_DELIMITER = ";";

    private JdbcTemplate template;

    private String resourceName;

    private String systemSettingsProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        loadInitialDbState();
        loadSystemSettings();
    }

    public void setSystemSettingsProperties(String systemSettingsProperties) {
        this.systemSettingsProperties = systemSettingsProperties;
    }

    public void setDataSource(final DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    private void loadInitialDbState() throws IOException {
        InputStream in = SchemaAndDataPopulator.class.getClassLoader().getResourceAsStream(resourceName);
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer);
        for (String statement : writer.toString().split(SQL_STATEMENT_DELIMITER)) {
            logger.info("Executing SQL Statement {}", statement);
            template.execute(statement);
        }
    }

    private void loadSystemSettings() throws IOException {
        InputStream in = SchemaAndDataPopulator.class.getClassLoader().getResourceAsStream(systemSettingsProperties);
        Properties systemSettings = new Properties();
        systemSettings.load(in);
    }
}
