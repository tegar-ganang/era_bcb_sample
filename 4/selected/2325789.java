package gov.sns.tools.apputils.PVSelection;

import java.util.*;
import javax.swing.tree.*;
import gov.sns.xal.smf.*;
import gov.sns.ca.*;

/** 
 * Generate sequence, device type, device, and PV tree structure 
 * @version   0.5  28 Nov 2002
 * @author C.M. Chu
 */
public class XALTreeNode extends HandleNode {

    AcceleratorSeq[] allSeqs;

    public XALTreeNode() {
        super("empty");
    }

    public XALTreeNode(Accelerator acc, String title) {
        super(title);
        allSeqs = acc.getSequences().toArray(new AcceleratorSeq[0]);
        defineSeqNodes();
    }

    public void setTitle(String title) {
        setUserObject(title);
        defineSeqNodes();
    }

    public void setAccelerator(Accelerator acc) {
        allSeqs = acc.getSequences().toArray(new AcceleratorSeq[0]);
        defineSeqNodes();
    }

    private void defineSeqNodes() {
        for (int i = 0; i < allSeqs.length; i++) {
            if (allSeqs[i].getType() != "Bnch") {
                Vector<String> typeV = new Vector<String>();
                java.util.List<AcceleratorNode> devTypes = allSeqs[i].getAllNodes();
                Iterator<AcceleratorNode> idevTypes = devTypes.iterator();
                while (idevTypes.hasNext()) {
                    String type = idevTypes.next().getType();
                    if (!typeV.contains(type)) typeV.addElement(type);
                }
                add(new SeqNode(allSeqs[i].getId(), typeV, allSeqs[i]));
            }
        }
    }
}

class SeqNode extends HandleNode {

    String sid;

    public SeqNode(String seq, Vector<String> types, AcceleratorSeq accSeq) {
        sid = seq;
        defineTypeNodes(types, accSeq);
    }

    private void defineTypeNodes(Vector<String> types, AcceleratorSeq accSeq) {
        for (int j = 0; j < types.size(); j++) {
            java.util.List<AcceleratorNode> nodesOfType = accSeq.getAllNodesOfType(types.elementAt(j));
            Vector<String> devIdV = new Vector<String>();
            Vector<AcceleratorNode> deviceV = new Vector<AcceleratorNode>();
            for (int jj = 0; jj < nodesOfType.size(); jj++) {
                AcceleratorNode accNode = nodesOfType.get(jj);
                if (accNode.getStatus() != false) {
                    devIdV.add(accNode.getId());
                    deviceV.add(accNode);
                }
            }
            add(new TypeNode(types.elementAt(j), devIdV, deviceV));
        }
    }

    @Override
    public String toString() {
        TreeNode parent = getParent();
        if (parent == null) return ("Device Types:"); else return sid;
    }
}

class TypeNode extends HandleNode {

    public TypeNode(String type, Vector<String> devIds, Vector<AcceleratorNode> devs) {
        super(type);
        defineHandleNodes(devIds, devs);
    }

    private void defineHandleNodes(Vector<String> devIds, Vector<AcceleratorNode> devs) {
        for (int k = 0; k < devs.size(); k++) {
            Collection<String> handlesOfNode = devs.elementAt(k).getHandles();
            Iterator<String> iHandles = handlesOfNode.iterator();
            Vector<String> handleV = new Vector<String>();
            while (iHandles.hasNext()) {
                String handle_str = iHandles.next();
                handleV.addElement(handle_str);
            }
            add(new DeviceNode(devIds.elementAt(k), handleV, devs.elementAt(k)));
        }
    }
}

class DeviceNode extends HandleNode {

    public DeviceNode(String devId, Vector<String> handles, AcceleratorNode dev) {
        super(devId);
        defineDeviceNodes(handles, dev);
    }

    private void defineDeviceNodes(Vector<String> handles, AcceleratorNode dev) {
        for (int k = 0; k < handles.size(); k++) {
            Channel channel = dev.getChannel(handles.elementAt(k));
            if (channel != null) {
                HandleNode h_node = new HandleNode(handles.elementAt(k));
                h_node.setAsSignal(true);
                h_node.setChannel(channel);
                h_node.setSignalName(dev.getChannel(handles.elementAt(k)).getId());
                add(h_node);
            }
        }
    }
}
