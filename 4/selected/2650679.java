package org.ttalbott.mytelly;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import org.apache.oro.text.perl.*;

/**
 *
 * @author  Tom Talbott
 * @version 
 */
public class XMLProgramData implements ProgramData {

    private Perl5Util m_regexp = new Perl5Util();

    private Document m_programsDoc = null;

    private MyNodeList m_progNodeList = null;

    /** Creates new XMLProgramData */
    public XMLProgramData() {
    }

    public XMLProgramData(Document programsDoc) {
        setProgramsDoc(programsDoc);
    }

    public void setProgramsDoc(Document programsDoc) {
        m_programsDoc = programsDoc;
        m_progNodeList = null;
    }

    public static XMLProgramData readPrograms(String filename) throws IOException {
        XMLProgramData programData = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document programsDoc = parser.parse(filename);
            programData = new XMLProgramData(programsDoc);
        } catch (SAXParseException spe) {
            spe.printStackTrace(System.err);
            throw new IOException(spe.getMessage());
        } catch (SAXException se) {
            if (se.getException() != null) se.getException().printStackTrace(System.err); else se.printStackTrace(System.err);
            throw new IOException(se.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new IOException(e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            throw new IOException(e.getMessage());
        }
        return programData;
    }

    public static XMLProgramData readPrograms() throws IOException {
        return readPrograms("programs.xml");
    }

    public ProgramList getPrograms() {
        if (m_programsDoc != null) return new MyNodeList(m_programsDoc.getElementsByTagName(ProgramData.PROGRAMME)); else return null;
    }

    public ProgramList getProgramsSortedByTime() {
        if (m_progNodeList == null) {
            m_progNodeList = (MyNodeList) getPrograms();
            m_progNodeList.sortAndRemoveDups();
        }
        return m_progNodeList;
    }

    public int getProgramCount() {
        ProgramList progs = getProgramsSortedByTime();
        return progs.getLength();
    }

    public int getCachedDays() {
        return 0;
    }

    private String m_curField = null;

    private int m_matchRec = 0;

    public ProgramList search(ProgramList progs, String regexp, String field, Progress progress) throws MalformedPerl5PatternException {
        MyNodeList result = new MyNodeList();
        NodeProgItem prog;
        m_curField = null;
        int numprogs = progs.getLength();
        for (int i = 0; i < numprogs; i++) {
            prog = (NodeProgItem) progs.item(i);
            if (match(prog.getItem(), regexp, field)) {
                result.add(prog);
            }
        }
        return result;
    }

    public boolean hasAdvancedSearch() {
        return false;
    }

    public ProgramList advancedSearch(ProgramList progs, String m_searchText, String m_channelText, String m_category, boolean m_distinct, boolean m_titlesOnly, String hiddenIndex, Progress progress) throws MalformedPerl5PatternException {
        return null;
    }

    public boolean match(Node node, String regexp, String field) throws MalformedPerl5PatternException {
        boolean result = false;
        if (node != null) {
            switch(node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    {
                        Element el = (Element) node;
                        String tag = el.getTagName();
                        m_curField = tag;
                        if (field == null || m_curField.equalsIgnoreCase(field)) result = m_regexp.match(regexp, tag);
                        break;
                    }
                case Node.TEXT_NODE:
                    {
                        Text txt = (Text) node;
                        String data = txt.getData();
                        if (field == null || m_curField.equalsIgnoreCase(field)) result = m_regexp.match(regexp, data);
                        break;
                    }
                case Node.ATTRIBUTE_NODE:
                    {
                        Attr attr = (Attr) node;
                        String value = attr.getValue();
                        if (field == null || m_curField.equalsIgnoreCase(field)) result = m_regexp.match(regexp, value);
                        break;
                    }
            }
            if (!result) {
                NodeList children = node.getChildNodes();
                int numChildren = children.getLength();
                for (int i = 0; i < numChildren; i++) {
                    Node child = children.item(i);
                    result = match(child, regexp, field);
                    if (result) break;
                }
            }
        } else System.out.println("Null Node found");
        m_matchRec--;
        return result;
    }

    public String getData(Node prog, String tag) {
        String result = null;
        if (prog != null && prog.getNodeType() == Node.ELEMENT_NODE) {
            Element elProg = (Element) prog;
            NodeList nodes = elProg.getElementsByTagName(tag);
            if (nodes.getLength() > 0) {
                Node dataNode = nodes.item(0);
                if (dataNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element elData = (Element) dataNode;
                    NodeList children = elData.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        if (children.item(i).getNodeType() == Node.TEXT_NODE) {
                            result = children.item(i).getNodeValue();
                        }
                    }
                    if (result == null) System.err.println("Tag: " + elData.getTagName() + " has no text?");
                } else {
                    System.err.println(dataNode.getLocalName() + " node not an Element");
                }
            } else {
            }
        } else {
            System.err.println("prog node not an Element");
        }
        return result;
    }

    public String getData(ProgItem prog, String tag) {
        if (tag.equalsIgnoreCase(ProgramData.START) || tag.equalsIgnoreCase(ProgramData.STOP) || tag.equalsIgnoreCase(ProgramData.CHANNEL)) return getNodeAttribute(prog, tag); else {
            Node node = ((NodeProgItem) prog).getItem();
            return getData(node, tag);
        }
    }

    public String getNodeAttribute(ProgItem prog, String tag) {
        String result = null;
        Node node = ((NodeProgItem) prog).getItem();
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            Element elProg = (Element) node;
            result = elProg.getAttribute(tag);
        }
        return result;
    }

    public Vector getCategories(ProgItem prog) {
        Vector ret = new Vector();
        NodeList subnodes = ((Element) ((NodeProgItem) prog).getItem()).getElementsByTagName(ProgramData.CATEGORY);
        int numSubnodes = subnodes.getLength();
        for (int i = 0; i < numSubnodes; i++) {
            NodeList children = ((Element) subnodes.item(i)).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if (children.item(j).getNodeType() == Node.TEXT_NODE) {
                    ret.add(children.item(j).getNodeValue());
                }
            }
        }
        return ret;
    }

    public boolean getPreviouslyShown(ProgItem prog) {
        NodeList subnodes = ((Element) ((NodeProgItem) prog).getItem()).getElementsByTagName(ProgramData.PREVIOUSLYSHOWN);
        if (subnodes.getLength() > 0) {
            return true;
        }
        return false;
    }

    public boolean getStereo(ProgItem prog) {
        NodeList subnodes = ((Element) ((NodeProgItem) prog).getItem()).getElementsByTagName(ProgramData.AUDIO);
        int numSubnodes = subnodes.getLength();
        for (int i = 0; i < numSubnodes; i++) {
            NodeList subsubnodes = ((Element) subnodes.item(i)).getElementsByTagName(ProgramData.STEREO);
            if (subsubnodes.getLength() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean getClosedCaption(ProgItem prog) {
        NodeList subnodes = ((Element) ((NodeProgItem) prog).getItem()).getElementsByTagName(ProgramData.SUBTITLES);
        int numSubnodes = subnodes.getLength();
        for (int i = 0; i < numSubnodes; i++) {
            String type = ((Element) subnodes.item(i)).getAttribute(ProgramData.TYPE);
            if (type != null && type.length() > 0) {
                if (type.equalsIgnoreCase(ProgramData.TELETEXT)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ProgramList getEmptyProgramList() {
        return new MyNodeList();
    }

    public ProgramList getProgramsSortedByChannel() {
        return null;
    }
}

class NodeComparitor extends java.lang.Object implements Comparator, Serializable {

    public int compare(java.lang.Object obj, java.lang.Object obj1) {
        int ret = 0;
        Element lNode = (Element) ((NodeProgItem) obj).getItem();
        Element rNode = (Element) ((NodeProgItem) obj1).getItem();
        if (lNode != null && rNode != null) {
            ret = ((String) lNode.getAttribute(ProgramData.START)).compareTo((String) rNode.getAttribute(ProgramData.START));
            if (ret == 0) {
                String lChannel = lNode.getAttribute(ProgramData.CHANNEL);
                String lChannelDesc = Programs.getChannelDesc(lChannel);
                int lEndNum = lChannelDesc.indexOf(' ');
                String lChannelNum = lChannelDesc.substring(0, lEndNum);
                String rChannel = rNode.getAttribute(ProgramData.CHANNEL);
                String rChannelDesc = Programs.getChannelDesc(rChannel);
                int rEndNum = rChannelDesc.indexOf(' ');
                String rChannelNum = rChannelDesc.substring(0, rChannelDesc.indexOf(' '));
                ret = Integer.valueOf(lChannelNum).compareTo(Integer.valueOf(rChannelNum));
                if (ret == 0) {
                    ret = lChannelDesc.substring(lEndNum).compareTo(rChannelDesc.substring(rEndNum));
                    if (ret == 0) {
                        ret = lNode.toString().compareTo(rNode.toString());
                    }
                }
            }
        }
        return ret;
    }
}

class NodeProgItem extends java.lang.Object implements ProgItem {

    Node m_item = null;

    public NodeProgItem() {
    }

    public NodeProgItem(Node item) {
        setItem(item);
    }

    public void setItem(Node item) {
        m_item = item;
    }

    public Node getItem() {
        return m_item;
    }
}

class MyNodeList extends ProgramListImp {

    public MyNodeList() {
    }

    public MyNodeList(NodeList nl) {
        set(nl);
    }

    public void add(Node node) {
        add(new NodeProgItem(node));
    }

    public void sort() {
        Collections.sort(m_items, new NodeComparitor());
    }

    public void sortAndRemoveDups() {
        TreeSet tempSet = new TreeSet(new NodeComparitor());
        tempSet.addAll(m_items);
        m_items.clear();
        m_items.addAll(tempSet);
    }

    public void set(NodeList nl) {
        m_items.clear();
        addAll(nl);
    }

    public void addAll(NodeList nl) {
        if (nl != null) {
            int count = nl.getLength();
            for (int i = 0; i < count; i++) {
                add(new NodeProgItem(nl.item(i)));
            }
        }
    }

    public void sortByChannel() {
    }
}
