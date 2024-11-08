package org.atricore.idbus.kernel.main.mediation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: IdentityMediationUnitContainer.java 1040 2009-03-05 00:56:52Z gbrigand $
 */
public class SpringMediationUnit implements IdentityMediationUnit, ApplicationContextAware, DisposableBean, InitializingBean {

    private static final Log logger = LogFactory.getLog(SpringMediationUnit.class);

    private String name;

    private Collection<Channel> channels = new ArrayList<Channel>();

    private ApplicationContext applicationContext;

    private IdentityMediationUnitContainer container;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IdentityMediationUnitContainer getContainer() {
        return container;
    }

    public void setContainer(IdentityMediationUnitContainer unitContainer) {
        this.container = unitContainer;
    }

    public Collection<Channel> getChannels() {
        return channels;
    }

    /**
     * @org.apache.xbean.Property alias="channels" nestedType="org.josso.federation.channel.Channel"
     *
     * @param channels
     */
    public void setChannels(Collection<Channel> channels) {
        this.channels = channels;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        logger.info("Using ApplicationContext " + (applicationContext != null ? applicationContext.getClass().getName() : "null"));
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void destroy() throws Exception {
    }

    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public String toString() {
        return super.toString() + "[name='" + name + "'" + "]";
    }
}
