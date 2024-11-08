package org.xaware.server.engine.channel.ftp;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * A FTP BizDriver provides a means to interact with FTP resources. An example
 * of a FTP BizDriver is:
 * 
 * <br/> Elements supported:
 * <li>xa:host - Host name to connect.</li>
 * <li>xa:port - Port number to connect.</li>
 * <li>xa:user(optional)</li>
 * <li>xa:pwd (optional)</li>
 * <li>xa:remote_verfication (optional)</li>
 * <li>xa:proxy_user (optional)</li>
 * <li>xa:proxy_pwd (optional)</li>
 * <br/> Attributes supported:
 * <li>xa:bizdrivertype - An Attribute on the root Element which must have a
 * value of FTP (required)</li>
 * <br/>
 * 
 * @author Vasu Thadaka
 */
public class FTPBizDriver extends AbstractBizDriver implements IBizDriver {

    /** Specification root element name */
    public static final String SPECIFICATION_ELEMENT_NAME = "ftp";

    /**
	 * Default constructor
	 */
    public FTPBizDriver() {
        super();
    }

    /**
	 * BizDriver implementations must implement this method. The channel
	 * specification gets injected by Spring
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
