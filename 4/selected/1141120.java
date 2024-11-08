package org.xaware.server.engine.channel.file;

import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * A File BizDriver provides a means to interact with file resources. It can be used to create, open and close readers
 * that can be used to read or write to a file. An example of a File BizDriver is: 
 * <xa:BizDriver xmlns:xa="http://xaware.org/xas/ns1" xa:bizdrivertype="FILE">
 *  <xa:input>
 *   <xa:param xa:name="mode" xa:value="xa:input::./mode" xa:datatype="string" xa:default="read" />
 *  </xa:input>
 *  <xa:file>
 *   <xa:name>data/items.txt</xa:name>
 *   <xa:request_type>%mode%</xa:request_type>
 *  </xa:file> 
 * </xa:BizDriver>
 * 
 * xa:name specifies the filename. xa:request_type must be read, write or append.
 * <br/>
 * Elements supported:
 * <li>xa:name - The file path and name (required)</li> 
 * <li>xa:request_type - read, write or append (required)</li>
 * <li>xa:input (optional)</li> 
 * <li>xa:param (optional)</li> 
 * <br/>
 *  Attributes supported:
 * <li>xa:bizdrivertype - An Attribute on the root Element which must have a value of FILE (required)</li> 
 * <li>xa:name (required if Element xa:param is present)</li> 
 * <li>xa:value (required if Element xa:param is present)</li> 
 * <li>xa:datatype (optional and only used if Element xa:param is present)</li> 
 * <li>xa:default (optional and only used if Element xa:param is present)</li> 
 * <br/>
 * @author jtarnowski
 */
public class FileBizDriver extends AbstractBizDriver {

    /** Object containing fileName and hopefully read, write or append */
    FileDriverData data = null;

    /**
     * Default constructor
     */
    public FileBizDriver() {
        super();
    }

    /**
     * BizDriver implementations must implement this method. It is responsible for Going through the BizDriver structure
     * and creating a channel object.  This is probably overkill for this BizDriver, and we could
     * probably handle parsing fileName and mode by ourselves
     * 
     * @see org.xaware.server.engine.IBizDriver#createChannelObject()
     */
    public Object createChannelObject() throws XAwareException {
        data = new FileDriverData();
        data = (FileDriverData) this.m_channelSpecification.getChannelObject();
        data.setFileName(this.substitute(data.getFileName(), this.getRootElement(), this.m_context));
        data.setMode(this.substitute(data.getMode(), this.getRootElement(), this.m_context));
        return data;
    }

    /**
     * @return the data
     */
    public FileDriverData getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(FileDriverData data) {
        this.data = data;
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}
