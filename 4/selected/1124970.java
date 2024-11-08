package org.xaware.server.engine.channel.smtp;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * SMTP BizComponents send e-mail messages from one defined mail server to another. 
 * An example of a SMTP BizDriver is:
 *  
 * <xa:BizDriver xmlns:xa="http://xaware.org/xas/ns1" xa:bizdrivertype="SMTP">                           
 *  <xa:description>Some description text</xa:description>                                               
 *   <xa:input>                                                                                          
 *     <xa:param xa:name="server" xa:value="" xa:datatype="string" xa:default="mail.mailserver.com" />   
 *   </xa:input>                                                                                         
 *   <xa:smtp>                                                                                           
 *     <xa:server>%server%</xa:server>                                                                   
 *     <xa:user>USER_ID</xa:user>                                                                        
 *     <xa:pwd>PASSWORD</xa:pwd>                                                                         
 *     <xa:port>25</xa:port>                                                                             
 *     <xa:authentication>yes</xa:authentication>                                                        
 *   </xa:smtp>                                                                                          
 *  </xa:BizDriver>                                                                                      
 * 
 * 
 * <br/> 
 * Elements supported:
 * <li>xa:smtp - the parent of server, user, pwd, port and authentication</li>
 * <li>xa:server - the mail send mail server</li>
 * <li>xa:input (optional)</li>
 * <li>xa:param (optional)</li>
 * <li>xa:user </li>
 * <li>xa:pwd </li>
 * <li>xa:port </li>
 * <li>xa:authentication (optional)</li>
 * <br/> 
 * Attributes supported:
 * <li>xa:bizdrivertype - An Attribute on the root Element which must have a value of SMTP (required)</li>
 * <li>xa:name (required if Element xa:param is present)</li>
 * <li>xa:value (required if Element xa:param is present)</li>
 * <li>xa:datatype (optional and only used if Element xa:param is present)</li>
 * <li>xa:default (optional and only used if Element xa:param is present)</li>
 * <br/>
 * 
 * @author dwieland
 */
public class SmtpBizDriver extends AbstractBizDriver implements IBizDriver {

    public static final String SPECIFICATION_ELEMENT_NAME = "smtp";

    /**
     * Default constructor
     */
    public SmtpBizDriver() {
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
