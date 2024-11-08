package com.googlecode.acpj.channels;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * Simple output of the graph of Actors, Channels and Ports reachable using the
 * {@link ChannelMonitor}. These tools allow output for debugging of the graph
 * in common and useful representations.
 * </p>
 * <p>
 * The methods included here are tolerant of a value of <code>null</code> for the
 * monitor instance, this allows a client to pass the monitor in without having 
 * to know whether the current {@link com.googlecode.acpj.channels.ChannelFactory} 
 * is providing a valid monitor or returning <code>null</code>. For example:
 * </p>
 * <pre>
 *     public void dumpChannelStatus() {
 *         ChannelMonitorOutput.writeDOT(ChannelFactory.getInstance().getChannelMonitor(), System.err);
 *     }
 * </pre>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class ChannelMonitorOutput {

    private static final String ENCODING = "UTF-8";

    /**
	 * The RDF format for outputting in N-TRIPLES.
	 */
    public static final String RDF_FORMAT_NTRIPLES = "N-TRIPLES";

    /**
	 * <p>
	 * Write out the current Channel graph as RDF data, specifically construct a 
	 * graph of RDF statements that describe all the channels, their ports and the
	 * actors owning the ports. This method defaults to outputting the RDF as
	 * N-Triples to <code>System.out</code>.
	 * </p>
	 * <p>
	 * The resulting RDF uses the vocabulary URI 
	 * <code>http://acpj.googlecode.com/vocab#</code> extensively for the 
	 * predicates used to describe the structure of the system.
	 * </p>
	 * 
	 * @param monitor a channel monitor retrieved from the channel factory.
	 * 
	 * @throws IOException
	 */
    public static void writeRDF(ChannelMonitor monitor) throws IOException {
        writeRDF(monitor, System.out, RDF_FORMAT_NTRIPLES);
    }

    /**
	 * <p>
	 * Write out the current Channel graph as RDF data, specifically construct a 
	 * graph of RDF statements that describe all the channels, their ports and the
	 * actors owning the ports. This method defaults to outputting the RDF as
	 * N-Triples.
	 * </p>
	 * <p>
	 * The resulting RDF uses the vocabulary URI 
	 * <code>http://acpj.googlecode.com/vocab#</code> extensively for the 
	 * predicates used to describe the structure of the system.
	 * </p>
	 * 
	 * @param monitor a channel monitor retrieved from the channel factory.
	 * @param os an output stream to write to.
	 * 
	 * @throws IOException
	 */
    public static void writeRDF(ChannelMonitor monitor, OutputStream os) throws IOException {
        writeRDF(monitor, os, RDF_FORMAT_NTRIPLES);
    }

    /**
	 * <p>
	 * Write out the current Channel graph as RDF data, specifically construct a 
	 * graph of RDF statements that describe all the channels, their ports and the
	 * actors owning the ports. 
	 * </p>
	 * <p>
	 * The resulting RDF uses the vocabulary URI 
	 * <code>http://acpj.googlecode.com/vocab#</code> extensively for the 
	 * predicates used to describe the structure of the system.
	 * </p>
	 * 
	 * @param monitor a channel monitor retrieved from the channel factory.
	 * @param os an output stream to write to.
	 * @param format the RDF format to be used.
	 * 
	 * @throws IOException
	 */
    public static void writeRDF(ChannelMonitor monitor, OutputStream os, String format) throws IOException {
        if (monitor == null || os == null || format == null) {
            return;
        }
        for (Iterator<MonitoredChannel> iterator = monitor.getChannels(); iterator.hasNext(); ) {
            MonitoredChannel channel = iterator.next();
            final String name = channel.getName();
            int portId = 1;
            os.write(String.format("<%s> a <http://acpj.googlecode.com/vocab#Channel> .\n", name).getBytes(ENCODING));
            os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#isBuffered> \"%s\" .\n", name, channel.isBuffered()).getBytes(ENCODING));
            if (channel.isBuffered()) {
                os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#capacity> \"%d\" .\n", name, channel.getBufferCapacity()).getBytes(ENCODING));
                os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#size> \"%d\" .\n", name, channel.size()).getBytes(ENCODING));
            }
            os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#readPortArity> \"%s\" .\n", name, channel.getReadPortArity()).getBytes(ENCODING));
            for (Iterator<MonitoredPort> iterator2 = channel.getReadPorts(); iterator2.hasNext(); ) {
                MonitoredPort port = iterator2.next();
                os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#hasPort> _:P%02d .\n", name, portId).getBytes(ENCODING));
                os.write(String.format("_:P%02d a <http://acpj.googlecode.com/vocab#ReadPort> .\n", portId).getBytes(ENCODING));
                os.write(String.format("_:P%02d <http://acpj.googlecode.com/vocab#owningActor> <%s> .\n", portId, port.getOwningActor().getName()).getBytes(ENCODING));
                os.write(String.format("_:P%02d <http://acpj.googlecode.com/vocab#isClosed> \"%s\" .\n", portId, port.isClosed()).getBytes(ENCODING));
                portId++;
            }
            os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#writePortArity> \"%s\" .\n", name, channel.getWritePortArity()).getBytes(ENCODING));
            for (Iterator<MonitoredPort> iterator2 = channel.getWritePorts(); iterator2.hasNext(); ) {
                MonitoredPort port = iterator2.next();
                os.write(String.format("<%s> <http://acpj.googlecode.com/vocab#hasPort> _:P%02d .\n", name, portId).getBytes(ENCODING));
                os.write(String.format("_:P%02d a <http://acpj.googlecode.com/vocab#WritePort> .\n", portId).getBytes(ENCODING));
                os.write(String.format("_:P%02d <http://acpj.googlecode.com/vocab#owningActor> <%s> .\n", portId, port.getOwningActor().getName()).getBytes(ENCODING));
                os.write(String.format("_:P%02d <http://acpj.googlecode.com/vocab#isClosed> \"%s\" .\n", portId, port.isClosed()).getBytes(ENCODING));
                portId++;
            }
        }
    }

    /**
	 * <p>
	 * Write out the current Channel graph in the DOT format defined by
	 * the <a href="http://www.graphviz.org">Graphviz</a> tools. This method 
	 * defaults to outputting to <code>System.out</code>.
	 * </p>
	 * <p>
	 * If the output of the method below is written to the file named
	 * "graph.dot" the command shown below will convert the output into
	 * a  PNG image file.
	 * </p>
	 * <pre>
	 * $ dot -Tpng -o graph.png graph.dot
	 * </pre>
	 *  
	 * @param monitor a channel monitor retrieved from the channel factory.
	 * 
	 * @throws IOException
	 */
    public static void writeDOT(ChannelMonitor monitor) throws IOException {
        writeDOT(monitor, System.out);
    }

    /**
	 * <p>
	 * Write out the current Channel graph in the DOT format defined by
	 * the <a href="http://www.graphviz.org">Graphviz</a> tools.
	 * </p>
	 * <p>
	 * If the output of the method below is written to the file named
	 * "graph.dot" the command shown below will convert the output into
	 * a  PNG image file.
	 * </p>
	 * <pre>
	 * $ dot -Tpng -o graph.png graph.dot
	 * </pre>
	 *  
	 * @param monitor a channel monitor retrieved from the channel factory.
	 * @param os an output stream to write to.
	 * 
	 * @throws IOException
	 */
    public static void writeDOT(ChannelMonitor monitor, OutputStream os) throws IOException {
        if (monitor == null || os == null) {
            return;
        }
        Map<String, String> actorMap = new HashMap<String, String>();
        os.write("digraph channels {\n".getBytes(ENCODING));
        os.write("  graph [center rankdir=LR];\n".getBytes(ENCODING));
        os.write("  node [fontsize=10];\n".getBytes(ENCODING));
        int channelId = 1;
        for (Iterator<MonitoredChannel> iterator = monitor.getChannels(); iterator.hasNext(); ) {
            final MonitoredChannel channel = iterator.next();
            String fill = channel.isPoisoned() ? "fillcolor=\"lightblue2\", style=\"filled\"" : "fillcolor=\"lightgray\", style=\"filled\"";
            String depth = channel.isBuffered() ? String.format(" {%d/%d}", channel.size(), channel.getBufferCapacity()) : "";
            os.write(String.format("  node [shape=\"box\" %s label=\"%s%s\"] channel_%d;\n", fill, channel.getName(), depth, channelId).getBytes(ENCODING));
            for (Iterator<MonitoredPort> iterator2 = channel.getWritePorts(); iterator2.hasNext(); ) {
                final MonitoredPort port = iterator2.next();
                final String actorsName = port.getOwningActor().getName();
                final String style = port.isClosed() ? "[style=\"dashed\"]" : "[style=\"solid\"]";
                if (actorMap.containsKey(actorsName)) {
                    final String actorName = actorMap.get(actorsName);
                    os.write(String.format("  %s -> channel_%d %s;\n", actorName, channelId, style).getBytes(ENCODING));
                } else {
                    os.write(String.format("  node [shape=\"ellipse\" fillcolor=\"lightgray\", style=\"filled\" label=\"%s\"] actor_%d;\n", actorsName, port.getOwningActor().getLocalId()).getBytes(ENCODING));
                    final String actorName = String.format("actor_%d", port.getOwningActor().getLocalId());
                    actorMap.put(actorsName, actorName);
                    os.write(String.format("  %s -> channel_%d %s;\n", actorName, channelId, style).getBytes(ENCODING));
                }
            }
            for (Iterator<MonitoredPort> iterator2 = channel.getReadPorts(); iterator2.hasNext(); ) {
                final MonitoredPort port = iterator2.next();
                if (port.getOwningActor() != null) {
                    final String actorsName = port.getOwningActor().getName();
                    final String style = port.isClosed() ? "[style=\"dashed\"]" : "[style=\"solid\"]";
                    if (actorMap.containsKey(actorsName)) {
                        final String actorName = actorMap.get(actorsName);
                        os.write(String.format("  channel_%d -> %s %s;\n", channelId, actorName, style).getBytes(ENCODING));
                    } else {
                        os.write(String.format("  node [shape=\"ellipse\" fillcolor=\"lightgray\", style=\"filled\" label=\"%s\"] actor_%d;\n", actorsName, port.getOwningActor().getLocalId()).getBytes(ENCODING));
                        final String actorName = String.format("actor_%d", port.getOwningActor().getLocalId());
                        actorMap.put(actorsName, actorName);
                        os.write(String.format("  channel_%d -> %s %s;\n", channelId, actorName, style).getBytes(ENCODING));
                    }
                }
            }
            channelId++;
        }
        os.write("}\n".getBytes(ENCODING));
    }
}
