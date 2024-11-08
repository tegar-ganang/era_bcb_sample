package com.ohua.clustering.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.Set;
import com.ohua.clustering.network.OhuaServerSocketFactory;
import com.ohua.clustering.network.OhuaSocketFactory;
import com.ohua.clustering.operators.ClusteringOperatorFactory;
import com.ohua.clustering.serialization.SerializableNetworkArc;
import com.ohua.engine.flowgraph.elements.FlowGraph;
import com.ohua.engine.flowgraph.elements.operator.OperatorCore;
import com.ohua.engine.flowgraph.elements.operator.Arc;
import com.ohua.engine.flowgraph.elements.operator.InputPort;
import com.ohua.engine.flowgraph.elements.operator.OutputPort;

/**
 * The graph piece to be executed by slaves needs to rewritten. It needs NetworkInput and
 * NetworkOutput operators for all operator with input/output ports that are connected to
 * output/input ports of operators that are located on another node or in another JVM process.
 * @author sertel
 * 
 */
public class SlaveGraphRewrite {

    public static void doInputRewrite(FlowGraph graph) throws IOException {
        for (OperatorCore operator : graph.getContainedGraphNodes()) {
            handleNetworkInputOperators(graph, operator);
        }
    }

    private static void handleNetworkInputOperators(FlowGraph graph, OperatorCore operator) throws IOException {
        for (InputPort inPort : operator.getInputPorts()) {
            if (inPort.hasIncomingArc()) {
                continue;
            }
            OperatorCore inputOperator = ClusteringOperatorFactory.createNetworkReaderOperator();
            graph.addOperator(inputOperator);
            Arc newArc = new Arc(inputOperator.getOutputPorts().get(0), inPort);
            graph.addArc(newArc);
            ServerSocket socket = OhuaServerSocketFactory.getInstance().createServerSocket(1112);
            socket.getChannel().register(LocalConnectionManager.getInstance().getGlobalSelector(), SelectionKey.OP_ACCEPT);
        }
    }

    public static void doOutputRewrite(FlowGraph graph, Set<SerializableNetworkArc> outgoingNetworkArcs) throws UnknownHostException, IOException {
        for (SerializableNetworkArc outgoingNetworkArc : outgoingNetworkArcs) {
            OperatorCore outputOperator = ClusteringOperatorFactory.createNetworkWriterOperator();
            graph.addOperator(outputOperator);
            Arc newArc = new Arc((OutputPort) graph.getPortByID(outgoingNetworkArc._sourcePort), outputOperator.getInputPorts().get(0));
            graph.addArc(newArc);
            Socket socket = OhuaSocketFactory.getInstance().createSocket(outgoingNetworkArc._ip, outgoingNetworkArc._remotePort);
            socket.getChannel().register(LocalConnectionManager.getInstance().getGlobalSelector(), SelectionKey.OP_CONNECT);
        }
    }
}
