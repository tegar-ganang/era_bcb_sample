package net.sf.jukebox.datastream.logger.impl.udp.xap;

import java.util.Set;
import net.sf.jukebox.datastream.logger.impl.udp.UdpLogger;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSource;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * The <a link href="http://xapautomation.org/">xAP</a> logger. Listens to the
 * notifications and broadcasts them using xAP.
 *
 * @param <E> Data type to log.
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2005-2009
 */
public class XapLogger<E extends Number> extends UdpLogger<E> {

    /**
     * Create an instance with no listeners.
     * 
     * @param port Port to bind to.
     */
    public XapLogger(int port) {
        this(null, port);
    }

    /**
     * Create an instance listening to given data sources.
     * 
     * @param producers Data sources to listen to.
     * @param port Port to bind to.
     */
    public XapLogger(Set<DataSource<E>> producers, int port) {
        super(producers, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getDefaultPort() {
        return 3639;
    }

    /**
     * Write an xAP header.
     * 
     * @param sb String buffer to write the header to.
     */
    @Override
    protected final void writeHeader(StringBuffer sb) {
        sb.append("xap-header\n");
        sb.append("{\n");
        sb.append("v=12\n");
        sb.append("hop=1\n");
        sb.append("uid=FF123400\n");
        sb.append("class=dz.dac\n");
        sb.append("source=DZ.logger.").append(getSource()).append("\n");
        sb.append("}\n");
    }

    /**
     * Write xAP body.
     * 
     * @param sb String buffer to append the body to.
     * @param signature Signature to use.
     * @param value Data sample value.
     */
    @Override
    protected final void writeData(StringBuffer sb, String signature, DataSample<E> value) {
        sb.append("dz.data-sample\n");
        sb.append("{\n");
        sb.append("channel.name=").append(getChannelName(signature)).append("\n");
        sb.append("channel.signature=").append(signature).append("\n");
        sb.append("channel.value=").append(getValueString(value)).append("\n");
        if (value.isError()) {
            sb.append("error=").append(getErrorString(value)).append("\n");
        }
        sb.append("timestamp=").append(getTimestamp(value.timestamp)).append("\n");
        sb.append("}\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescription() {
        return "xAP Logger";
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        JmxDescriptor d = super.getJmxDescriptor();
        return new JmxDescriptor("jukebox", d.name, d.instance, "Broadcasts sensor readings via xAP protocol");
    }
}
