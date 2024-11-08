package org.xaware.server.engine.channel.ldap;

import javax.naming.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractJndiChannel;
import org.xaware.server.engine.channel.JndiChannelKey;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Channel Specification for LDAP
 * 
 * @author Vasu Thadaka
 */
public class LDAPChannelSpecification extends AbstractJndiChannel {

    /** Logger for LDAPChannelSpecification class */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(LDAPChannelSpecification.class.getName());

    /** ContextSource Instance */
    private ContextSource contextSource = null;

    /** Class name for logging */
    private static final String className = LDAPChannelSpecification.class.getName();

    /** BizView Context instance */
    private IBizViewContext bizViewContext;

    @Override
    public void transformSpecInfo(IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        this.bizViewContext = bizViewContext;
        try {
            setupInitialContext(getBizDriverIdentifier() + ":LDAPChannelSpecification", bizViewContext, logger);
            contextSource = lookupContextSource(bizViewContext);
        } catch (NamingException e) {
            logger.severe(JndiChannelKey.dumpJndiTree(jndiAccessor));
            logger.debug(e);
            throw new XAwareConfigMissingException(ExceptionMessageHelper.getExceptionMessage(e), e);
        }
    }

    /** Constructs Context Source from the Lookup LDAPInitialContext */
    private ContextSource lookupContextSource(IBizViewContext bizViewContext) throws XAwareException, NamingException {
        final String methodName = "lookupContextSource";
        LdapContextSource ldapContextSource = new LdapContextSource();
        try {
            ldapContextSource.setBaseEnvironmentProperties(jndiAccessor.getJndiEnvironment());
            ldapContextSource.setUrl(url);
            ldapContextSource.setContextFactory(Class.forName(jndiFactory));
            if (user != null && user.trim().length() > 0) {
                ldapContextSource.setUserDn(user);
                ldapContextSource.setPassword(pwd);
            }
            ldapContextSource.afterPropertiesSet();
        } catch (Exception exception) {
            String errorMessage = "Exception occured while creating LDAP Context Source: " + ExceptionMessageHelper.getExceptionMessage(exception);
            logger.severe(errorMessage, className, methodName);
            throw new XAwareException(errorMessage, exception);
        }
        return ldapContextSource;
    }

    public Object getChannelObject() throws XAwareException {
        if (contextSource == null) {
            try {
                contextSource = this.lookupContextSource(bizViewContext);
            } catch (NamingException namingException) {
                logger.debug(namingException);
                throw new XAwareException(namingException);
            }
        }
        return this.contextSource;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        return null;
    }
}
