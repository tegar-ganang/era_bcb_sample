package gov.sns.xal.tools.virtualaccelerator;

import gov.sns.ca.Channel;
import gov.sns.xal.smf.AcceleratorNode;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * PCAST3dGenerator
 * Generator of input file for Trace3D Virtual Accelerator (SNS version)
 * @author sako
 *
 */
public class PCAST3dGenerator {

    ArrayList<NodeT3d> nodeList;

    public PCAST3dGenerator() {
        nodeList = new ArrayList<NodeT3d>();
    }

    public void addElement(AcceleratorNode node, int index) {
        if (node != null) {
            NodeT3d nodeT3d = searchNode(node);
            if (nodeT3d == null) {
                nodeT3d = new NodeT3d(node, index);
                nodeList.add(nodeT3d);
                System.out.println("added node = " + node.getId());
            } else {
                nodeT3d.add(node, index);
            }
        }
    }

    public void sort() {
        Collections.sort(nodeList);
    }

    protected NodeT3d searchNode(AcceleratorNode n) {
        for (int i = 0; i < nodeList.size(); i++) {
            NodeT3d nt = nodeList.get(i);
            if (nt.getNode().equals(n)) {
                return nt;
            }
        }
        return null;
    }

    public void write(String outFile) throws IOException {
        sort();
        FileWriter writer = new FileWriter(outFile);
        System.out.println("PCAST3dGenerator,write, nodeList.size() = " + nodeList.size());
        for (int i = 0; i < nodeList.size(); i++) {
            NodeT3d nodeT3d = nodeList.get(i);
            String line = nodeT3d.process();
            if (line != null) {
                System.out.println("line = " + line);
                writer.write(line + "\n");
            }
        }
        writer.close();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }
}

class NodeT3d implements Comparable<NodeT3d> {

    private AcceleratorNode node;

    ArrayList<Integer> indexList;

    private String tag;

    public NodeT3d() {
        indexList = null;
    }

    public NodeT3d(AcceleratorNode n, int i) {
        this();
        add(n, i);
    }

    public void add(AcceleratorNode n, int i) {
        Integer I = new Integer(i);
        if (indexList == null) {
            node = n;
            tag = getTag(node.getType());
            System.out.println("node.getType(), node.getId() = " + node.getType() + " " + node.getId());
            indexList = new ArrayList<Integer>();
        }
        indexList.add(I);
    }

    public AcceleratorNode getNode() {
        return node;
    }

    public ArrayList<Integer> getIndexList() {
        return indexList;
    }

    public int getIndex(int j) {
        if ((0 <= j) && (j < indexList.size())) {
            Integer I = indexList.get(j);
            return I.intValue();
        } else {
            return -1;
        }
    }

    public String getTag() {
        return tag;
    }

    public String process() {
        DecimalFormat nf = new DecimalFormat("#####.#########");
        String indices = String.valueOf(indexList.size());
        String firstIndex = "";
        for (int i = 0; i < indexList.size(); i++) {
            Integer I = indexList.get(i);
            if (i == 0) {
                firstIndex = I.toString();
            }
            indices += (" " + I.toString());
        }
        String line = null;
        if (tag == null) {
            return line;
        } else {
            if (tag.equals("param")) {
                System.out.println("now tag = param");
                String dim = String.valueOf(1);
                String pvFieldSet = "";
                String pvFieldRB = "";
                Collection<String> handles = node.getHandles();
                System.out.println("handles.size() = " + handles.size());
                for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
                    String handle = handleIter.next();
                    Channel channel = node.getChannel(handle);
                    if (channel != null) {
                        if (handle.equals("fieldSet")) {
                            pvFieldSet = channel.channelName();
                        } else if (handle.equals("fieldRB")) {
                            pvFieldRB = " " + "rbName=" + channel.channelName();
                        }
                        System.out.println("handle = " + handle);
                    }
                }
                if (!pvFieldSet.equals("")) {
                    line = tag + " " + pvFieldSet + " " + indices + " " + dim + pvFieldRB;
                }
            } else if (tag.equals("quad")) {
                String dim = String.valueOf(1);
                String pvFieldSet = "";
                String pvFieldRB = "";
                Collection<String> handles = node.getHandles();
                for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
                    String handle = handleIter.next();
                    Channel channel = node.getChannel(handle);
                    if (channel != null) {
                        if (handle.equals("fieldSet")) {
                            pvFieldSet = channel.channelName();
                        } else if (handle.equals("fieldRB")) {
                            pvFieldRB = " " + channel.channelName();
                        }
                        System.out.println("handle = " + handle);
                    } else {
                        System.out.println("channel == null");
                    }
                }
                if (!pvFieldSet.equals("")) {
                    line = tag + " " + pvFieldSet + " " + indices + " " + dim + pvFieldRB;
                }
            } else if (tag.equals("bpm")) {
                String pvX = "";
                String pvY = "";
                Collection<String> handles = node.getHandles();
                for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
                    String handle = handleIter.next();
                    Channel channel = node.getChannel(handle);
                    if (channel != null) {
                        if (handle.equals("xAvg")) {
                            pvX = channel.channelName();
                        } else if (handle.equals("yAvg")) {
                            pvY = channel.channelName();
                        }
                    }
                }
                if (!pvX.equals("")) {
                    line = tag + " " + pvX + " " + firstIndex + " " + "xAvg";
                }
                if (!pvY.equals("")) {
                    if (!pvX.equals("")) {
                        line += "\n" + tag + " " + pvY + " " + firstIndex + " " + "yAvg";
                    } else {
                        line = tag + " " + pvY + " " + firstIndex + " " + "yAvg";
                    }
                }
            } else if (tag.equals("rebuncher")) {
                String pvAmp = "";
                String pvPhase = "";
                Collection<String> handles = node.getHandles();
                for (Iterator<String> handleIter = handles.iterator(); handleIter.hasNext(); ) {
                    String handle = handleIter.next();
                    Channel channel = node.getChannel(handle);
                    if (channel != null) {
                        if (handle.equals("cavAmpSet")) {
                            pvAmp = channel.channelName();
                        } else if (handle.equals("cavPhaseSet")) {
                            pvPhase = channel.channelName();
                        }
                    }
                }
                if (!pvAmp.equals("")) {
                    line = tag + " " + pvAmp + " " + firstIndex + " " + "cavAmpSet";
                }
                if (!pvPhase.equals("")) {
                    if (!pvAmp.equals("")) {
                        line += "\n" + tag + " " + pvPhase + " " + firstIndex + " " + "cavPhaseSet";
                    } else {
                        line = tag + " " + pvPhase + " " + firstIndex + " " + "cavPhaseSet";
                    }
                }
            }
            return line;
        }
    }

    protected String getTag(String type) {
        String tg = null;
        if (type.equals("QV") || type.equals("QH") || type.equals("PMQH") || type.equals("PMQV")) {
            tg = "quad";
        } else if (type.equals("DCH") || type.equals("DCV")) {
            tg = "param";
        } else if (type.equals("RG") || type.equals("BNCH")) {
            tg = "rebuncher";
        } else if (type.equals("BPM")) {
            tg = "bpm";
        }
        System.out.println("tg = " + tg);
        return tg;
    }

    public int compareTo(NodeT3d o) {
        NodeT3d t1 = this;
        NodeT3d t2 = o;
        int c1 = t1.getTag().compareTo(t2.getTag());
        if (c1 != 0) {
            return c1;
        } else {
            int c2 = t1.getNode().getId().compareTo(t2.getNode().getId());
            return c2;
        }
    }
}
