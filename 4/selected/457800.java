package org.xaware.server.engine.channel.sql;

import java.util.Map;
import org.apache.commons.dbcp.BasicDataSource;
import org.jdom.Document;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IChannelPoolingSpecification;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.LocalChannelKey;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.engine.instruction.bizcomps.config.BizDriverConfig;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;

/**
 * @author tferguson
 * 
 */
public class MockMySqlBizDriver implements IBizDriver {

    private final String driver = "com.mysql.jdbc.Driver";

    private final String url = "jdbc:mysql://cs-rd02.xaware.com:3306/tferguson";

    private final String user = "tferguson";

    private final String password = "xabeerme";

    private final int initialSize = 2;

    private final int maxActive = 5;

    private final int maxIdle = 2;

    public Object createChannelObject() {
        System.out.println("Adding datasource: driver class: " + driver + " url: " + url + " user: " + user + " password: " + password);
        final BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(driver);
        bds.setUrl(url);
        bds.setUsername(user);
        bds.setPassword(password);
        bds.setInitialSize(initialSize);
        bds.setMaxActive(maxActive);
        bds.setMaxIdle(maxIdle);
        return bds;
    }

    public String getBizDriverIdentifier() {
        return null;
    }

    public String getBizDriverLocation() {
        return null;
    }

    public String getBizDriverType() {
        return null;
    }

    public IChannelPoolingSpecification getChannelPoolingSpecification() throws XAwareConfigurationException, XAwareSubstitutionException, XAwareConfigMissingException, XAwareException {
        return null;
    }

    public IChannelSpecification getChannelSpecification() throws XAwareConfigurationException, XAwareSubstitutionException, XAwareConfigMissingException, XAwareException {
        return null;
    }

    public IChannelKey getChannelSpecificationKey() {
        return new LocalChannelKey(driver + url + user + password);
    }

    public void setBizDriverIdentifier(final String p_bizDriverIdentifier) {
    }

    public void setBizDriverLocation(final String p_bizDriverLocation) {
    }

    public void setBizDriverType(final String p_bizDriverType) {
    }

    public void setChannelPoolingSpecification(final IChannelPoolingSpecification p_poolingSpec) {
    }

    public void setChannelSpecification(final IChannelSpecification p_channelSpec) {
    }

    public void setJdomDocument(final Document p_jdom) {
    }

    public void setupContext(IBizViewContext p_parentContext, BizDriverConfig p_bizDriverConfig) throws XAwareException {
    }

    public void setupContext(String p_bizViewName) throws XAwareException {
    }

    public void setupContext(String p_bizViewName, Map<String, Object> p_params, String p_inputXml) throws XAwareException {
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        return null;
    }
}
