package net.sf.jgcs.jgroups;

import javax.management.MBeanServer;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.Service;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;

public class JGroupsJmxConfigurator implements net.sf.jgcs.jmx.JmxConfigurator {

    public void register(DataSession dataSession, ControlSession controlSession, Service service, Protocol protocol, MBeanServer server, String domain) throws Exception {
        domain = domain.replace(':', '-');
        JGroupsDataSession jgroupsDataSession = (JGroupsDataSession) dataSession;
        JChannel channel = jgroupsDataSession.getChannel(service);
        org.jgroups.jmx.JmxConfigurator.registerChannel(channel, server, domain, channel.getClusterName(), true);
    }

    public void unregister(DataSession dataSession, ControlSession controlSession, Service service, Protocol protocol, MBeanServer server, String domain) throws Exception {
        domain = domain.replace(':', '-');
        JGroupsDataSession jgroupsDataSession = (JGroupsDataSession) dataSession;
        JChannel channel = jgroupsDataSession.getChannel(service);
        String clusterName = channel.getClusterName();
        String name = domain + ":type=channel,cluster=" + clusterName;
        JmxConfigurator.unregisterChannel(server, name);
        String tmp = domain + ":type=protocol,cluster=" + clusterName;
        JmxConfigurator.unregisterProtocols(server, channel, tmp);
    }
}
