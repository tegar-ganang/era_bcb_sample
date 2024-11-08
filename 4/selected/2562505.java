package hci.gnomex.utility;

import hci.gnomex.model.SequenceLane;
import hci.gnomex.model.SequencingControl;
import hci.gnomex.model.WorkItem;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.hibernate.Session;
import org.jdom.Document;
import org.jdom.Element;

public class WorkItemSolexaAssembleParser implements Serializable {

    private Document docFlowCellChannels;

    private Document docDirtyWorkItem;

    private TreeMap<String, List<ChannelContent>> channelContentMap = new TreeMap<String, List<ChannelContent>>();

    private TreeMap<String, String> channelConcentrationMap = new TreeMap<String, String>();

    private TreeMap<String, String> controlMap = new TreeMap<String, String>();

    private ArrayList<WorkItem> dirtyWorkItemList = new ArrayList<WorkItem>();

    public WorkItemSolexaAssembleParser(Document docFlowCellChannels, Document docDirtyWorkItem) {
        this.docFlowCellChannels = docFlowCellChannels;
        this.docDirtyWorkItem = docDirtyWorkItem;
    }

    public void parse(Session sess) throws Exception {
        if (docFlowCellChannels != null) {
            Element workItemListNode = this.docFlowCellChannels.getRootElement();
            for (Iterator<?> i = workItemListNode.getChildren().iterator(); i.hasNext(); ) {
                Element node = (Element) i.next();
                String channelNumber = node.getAttributeValue("channelNumber");
                ChannelContent cc = new ChannelContent();
                String sampleConcentration = node.getAttributeValue("sampleConcentrationpM");
                String isControl = node.getAttributeValue("isControl");
                String isEditable = node.getAttributeValue("editable");
                if (node.getName().equals("WorkItem")) {
                    String idSequenceLaneString = node.getAttributeValue("idSequenceLane");
                    String idWorkItemString = node.getAttributeValue("idWorkItem");
                    SequenceLane lane = (SequenceLane) sess.load(SequenceLane.class, new Integer(idSequenceLaneString));
                    WorkItem workItem = (WorkItem) sess.load(WorkItem.class, new Integer(idWorkItemString));
                    cc.setSequenceLane(lane);
                    cc.setWorkItem(workItem);
                } else {
                    String idSequencingControlString = node.getAttributeValue("idSequencingControl");
                    SequencingControl control = (SequencingControl) sess.load(SequencingControl.class, new Integer(idSequencingControlString));
                    cc.setSequenceControl(control);
                }
                List<ChannelContent> channelContents = (List<ChannelContent>) channelContentMap.get(channelNumber);
                if (channelContents == null) {
                    channelContents = new ArrayList<ChannelContent>();
                }
                channelContents.add(cc);
                channelContentMap.put(channelNumber, channelContents);
                if (isEditable != null && isEditable.equals("true")) {
                    if (sampleConcentration != null && !sampleConcentration.equals("")) {
                        channelConcentrationMap.put(channelNumber, sampleConcentration);
                    }
                    controlMap.put(channelNumber, isControl != null && isControl.equals("true") ? "Y" : "N");
                }
            }
        }
        if (docDirtyWorkItem != null) {
            Element dirtyWorkItemListNode = this.docDirtyWorkItem.getRootElement();
            for (Iterator<?> i = dirtyWorkItemListNode.getChildren().iterator(); i.hasNext(); ) {
                Element node = (Element) i.next();
                String idWorkItemString = node.getAttributeValue("idWorkItem");
                WorkItem workItem = (WorkItem) sess.load(WorkItem.class, new Integer(idWorkItemString));
                if (node.getAttributeValue("assembleStatus") != null && !node.getAttributeValue("assembleStatus").equals("")) {
                    workItem.setStatus(node.getAttributeValue("assembleStatus"));
                } else {
                    workItem.setStatus(null);
                }
                dirtyWorkItemList.add(workItem);
            }
        }
    }

    public List<WorkItem> getDirtyWorkItemList() {
        return dirtyWorkItemList;
    }

    public void resetIsDirty() {
        if (docFlowCellChannels != null) {
            Element workItemListNode = this.docFlowCellChannels.getRootElement();
            for (Iterator<?> i = workItemListNode.getChildren("WorkItem").iterator(); i.hasNext(); ) {
                Element workItemNode = (Element) i.next();
                workItemNode.setAttribute("isDirty", "N");
            }
        }
        if (this.docDirtyWorkItem != null) {
            Element workItemListNode = this.docDirtyWorkItem.getRootElement();
            for (Iterator<?> i = workItemListNode.getChildren("WorkItem").iterator(); i.hasNext(); ) {
                Element workItemNode = (Element) i.next();
                workItemNode.setAttribute("isDirty", "N");
            }
        }
    }

    public Set<String> getChannelNumbers() {
        return channelContentMap.keySet();
    }

    public List<ChannelContent> getChannelContents(String channelNumber) {
        return channelContentMap.get(channelNumber);
    }

    public List<WorkItem> getWorkItems(String channelNumber) {
        List<ChannelContent> channelContents = channelContentMap.get(channelNumber);
        List<WorkItem> workItems = new ArrayList<WorkItem>();
        for (Iterator<?> i = channelContents.iterator(); i.hasNext(); ) {
            ChannelContent cc = (ChannelContent) i.next();
            if (cc.getWorkItem() != null) {
                workItems.add(cc.getWorkItem());
            }
        }
        return workItems;
    }

    public BigDecimal getSampleConcentrationpm(String channelNumber) {
        String sc = (String) channelConcentrationMap.get(channelNumber);
        if (sc != null && !sc.equals("")) {
            return new BigDecimal(sc);
        } else {
            return null;
        }
    }

    public String getIsControl(String channelNumber) {
        String isControl = this.controlMap.get(channelNumber);
        return isControl;
    }

    public static class ChannelContent implements Serializable {

        private SequenceLane sequenceLane;

        private SequencingControl sequenceControl;

        private WorkItem workItem;

        private String isControl;

        public SequenceLane getSequenceLane() {
            return sequenceLane;
        }

        public void setSequenceLane(SequenceLane sequenceLane) {
            this.sequenceLane = sequenceLane;
        }

        public SequencingControl getSequenceControl() {
            return sequenceControl;
        }

        public void setSequenceControl(SequencingControl sequenceControl) {
            this.sequenceControl = sequenceControl;
        }

        public WorkItem getWorkItem() {
            return workItem;
        }

        public void setWorkItem(WorkItem workItem) {
            this.workItem = workItem;
        }

        public String getIsControl() {
            return isControl;
        }

        public void setIsControl(String isControl) {
            this.isControl = isControl;
        }
    }
}
