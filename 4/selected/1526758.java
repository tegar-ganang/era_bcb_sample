package org.xaware.server.engine.channel.http;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * A Http BizDriver provides a means to interact with HTTP resources. It can be used to get and/or post configuration.
 * An example of a Http BizDriver is:
 *  
 * <xa:BizDriver xmlns:xa="http://xaware.org/xas/ns1" xa:bizdrivertype="HTTP">
 *   <xa:description>Some description text</xa:description>
 *   <xa:input> 
 *     <xa:param xa:name="url" xa:value="xa:input::./url" xa:datatype="string" xa:default="http://www.google.com" />
 *   </xa:input>
 *   <xa:http>
 *     <xa:url>%url%</xa:url> 
 *     <xa:user>Me</xa:user> 
 *     <xa:pwd>password</xa:pwd> 
 *     <xa:proxy> 
 *        <xa:proxy_host>\\cs-ad2</xa:proxy_host>
 *        <xa:proxy_port>8090</xa:proxy_port>
 *        <xa:proxy_user>ZZZ</xa:proxy_user>
 *        <xa:proxy_pwd>ZZZZZZ</xa:proxy_pwd>
 *     </xa:proxy> 
 *    </xa:http> 
 *  </xa:BizDriver>
 * 
 * xa:url or xa:base_url (but not both) is required. 
 * 
 * <br/> 
 * Elements supported:
 * <li>xa:url - The full url to visit</li>
 * <li>xa:base_url - first part of url when the BizComponent will do multiple gets and/or posts</li>
 * <li>xa:input (optional)</li>
 * <li>xa:param (optional)</li>
 * <li>xa:uid (optional)</li>
 * <li>xa:pwd (optional)</li>
 * <li>xa:host (optional)</li>
 * <li>xa:port (optional)</li>
 * <li>xa:proxy_uid (optional)</li>
 * <li>xa:proxy_pwd (optional)</li>
 * <li>xa:proxy_host (optional)</li>
 * <li>xa:proxy_port (optional)</li>
 * <br/> 
 * Attributes supported:
 * <li>xa:bizdrivertype - An Attribute on the root Element which must have a value of HTTP (required)</li>
 * <li>xa:name (required if Element xa:param is present)</li>
 * <li>xa:value (required if Element xa:param is present)</li>
 * <li>xa:datatype (optional and only used if Element xa:param is present)</li>
 * <li>xa:default (optional and only used if Element xa:param is present)</li>
 * <br/>
 * 
 * @author jtarnowski
 */
public class HttpBizDriver extends AbstractBizDriver implements IBizDriver {

    public static final String SPECIFICATION_ELEMENT_NAME = "http";

    /**
     * Default constructor
     */
    public HttpBizDriver() {
        super();
    }

    /**
     * BizDriver implementations must implement this method. The channel specification gets injected by Spring
     * 
     * @see org.xaware.server.engine.IBizDriver#createChannelObject()
     */
    public Object createChannelObject() throws XAwareException {
        return m_channelSpecification.getChannelObject();
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}
