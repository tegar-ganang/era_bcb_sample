package org.xaware.server.engine.channel.sf;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * A Http BizDriver provides a means to interact with HTTP resources. It can be used to get and/or post configuration.
 * An example of a Http BizDriver is:
 *  
 * <xa:BizDriver xmlns:xa="http://xaware.org/xas/ns1" xa:bizdrivertype="SF">
 *   <xa:description>Some description text</xa:description>
 *   <xa:input> 
 *     <xa:param xa:name="url" xa:value="xa:input::./url" xa:datatype="string" xa:default="http://www.google.com" />
 *   </xa:input>
 *   <xa:sf>
 *     <xa:url>%url%</xa:url> 
 *     <xa:user>Me</xa:user> 
 *     <xa:pwd>password</xa:pwd> 
 *     <xa:param> 
 *        <xa:param_timeout>100</param_timeout>
 *     </xa:param> 
 *    </xa:sf> 
 *  </xa:BizDriver>
 * 
 * 
 * <br/> 
 * Elements supported:
 * <li>xa:url (optional)</li>
 * <li>xa:input (optional)</li>
 * <li>xa:param_timeout (optional)</li>
 * <li>xa:uid (optional)</li>
 * <li>xa:pwd (optional)</li>
 * <br/> 
 * Attributes supported:
 * <li>xa:bizdrivertype - An Attribute on the root Element which must have a value of HTTP (required)</li>
 * <li>xa:name (required if Element xa:param is present)</li>
 * <li>xa:value (required if Element xa:param is present)</li>
 * <li>xa:datatype (optional and only used if Element xa:param is present)</li>
 * <li>xa:default (optional and only used if Element xa:param is present)</li>
 * <br/>
 * 
 * @author openweaver
 */
public class SalesForceBizDriver extends AbstractBizDriver implements IBizDriver {

    /**
     * Default constructor
     */
    public SalesForceBizDriver() {
        super();
    }

    /**
     * BizDriver implementations must implement this method. The channel specification gets injected by Spring
     * 
     * @see org.xaware.server.engine.IBizDriver#createChannelObject()
     */
    public Object createChannelObject() throws XAwareException {
        return null;
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        return this.m_channelSpecification.getChannelTemplate();
    }
}
