package org.xaware.server.engine.channel.soap;

import org.jdom.Element;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;

/**
 * A SOAP BizDriver provides a means to connect to and receive data from Web Services. 
 * An example of a SOAP BizDriver is:
 * <br/> 
 *   <br/>
 *   &lt;?xml version="1.0" encoding="UTF-8"?&gt;<br/>
 *   &lt;xa:bizDriver xmlns:xa="http://xaware.org/xas/ns1" xa:bizdrivertype="SOAP_HTTP" xa:version="5.0"&gt;<br/>
 *      &lt;xa:description&gt;description text&lt;/xa:description&gt;<br/>
 *      &lt;xa:input&gt;<br/>
 *         &lt;xa:param xa:name="user" xa:value="" xa:datatype="string" xa:default="" /&gt;<br/>
 *         &lt;xa:param xa:name="password" xa:value="" xa:datatype="string" xa:default="" /&gt;<br/>
 *         &lt;xa:param xa:name="serviceUrl" xa:value="" xa:datatype="string" xa:default="http://localhost:8090/xaware/XADocSoapServlet"/&gt;<br/>
 *      &lt;/xa:input&gt;   <br/>
 *      &lt;xa:http&gt;<br/>
 *         &lt;xa:user&gt;%userName%&lt;/xa:user&gt;<br/>
 *         &lt;xa:pwd&gt;%password%&lt;/xa:pwd&gt;<br/>
 *         &lt;xa:url&gt;%serviceUrl%&lt;/xa:url&gt;<br/>
 *      &lt;/xa:http&gt; <br/>
 *   &lt;/xa:bizDriver&gt;<br/>
 *
 * 
 * 
 * <br/> 

 * @author Basil Ibegbu
 */
public class SoapBizDriver extends AbstractBizDriver implements IBizDriver {

    /**
     * Default constructor
     */
    public SoapBizDriver() {
        super();
    }

    /**
     * BizDriver implementations must implement this method. The channel specification gets injected by Spring
     * 
     * @see org.xaware.server.engine.IBizDriver#createChannelObject()
     */
    public Object createChannelObject() throws XAwareException {
        m_channelSpecification.transferSpecificationInfo(m_bizDriverIdentifier, m_jdomStructure.getRootElement(), m_context);
        return m_channelSpecification.getChannelObject();
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}
