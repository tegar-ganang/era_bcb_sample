package ch.unibe.inkml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ch.unibe.inkml.InkChannel.ChannelName;

public class InkTraceFormat extends InkUniqueElement implements Iterable<InkChannel> {

    public static final String INKML_NAME = "traceFormat";

    public static final String INKML_INTERMITTENT_NAME = "intermittentChannels";

    public static final String ID_PREFIX = "tf";

    public static InkTraceFormat getDefaultTraceFormat(InkInk ink) {
        return new DefaultInkTraceFormat(ink);
    }

    private List<InkChannel> channels;

    private Map<InkChannel.ChannelName, Integer> index;

    /**
	 * Decides if a trace format is final.
	 * Only final trace formats can be used for SampleSets since changing a format 
	 * while its used will invalidate samplesets. 
	 */
    private boolean isFinal = false;

    public InkTraceFormat(InkInk ink, String id) throws InkMLComplianceException {
        super(ink, id);
        this.initialize();
    }

    public InkTraceFormat(InkInk ink) {
        super(ink);
        this.initialize();
    }

    private void initialize() {
        channels = new ArrayList<InkChannel>();
        index = new HashMap<InkChannel.ChannelName, Integer>();
    }

    @Override
    public void buildFromXMLNode(Element parentNode) throws InkMLComplianceException {
        super.buildFromXMLNode(parentNode);
        for (Node child = parentNode.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) child;
            if (el.getNodeName().equals(InkChannel.INKML_NAME)) {
                InkChannel c = InkChannel.channelFactory(this.getInk(), el);
                this.addChannel(c);
            } else if (el.getNodeName().equals(INKML_INTERMITTENT_NAME)) {
                for (Node ichild = el.getFirstChild(); ichild != null; ichild = ichild.getNextSibling()) {
                    if (ichild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element iel = (Element) ichild;
                    if (iel.getNodeName().equals(InkChannel.INKML_NAME)) {
                        InkChannel c = InkChannel.channelFactory(this.getInk(), iel);
                        c.setIntermittent(true);
                        c.setFinal();
                        this.addChannel(c);
                    }
                }
            }
        }
        setFinal();
    }

    @Override
    public void exportToInkML(Element parentNode) throws InkMLComplianceException {
        assert parentNode != null;
        assert isFinal();
        if (!isFinal()) {
            throw new InkMLComplianceException("The TraceViewFormat with id '" + getId() + "' is not final now. You can not export unfinalized TraceViewsFormat. They may be invalid.");
        }
        Element traceFormatNode = parentNode.getOwnerDocument().createElement(INKML_NAME);
        traceFormatNode.setAttribute(InkUniqueElement.INKML_ATTR_ID, this.getId());
        int i = 0;
        for (; i < channels.size(); i++) {
            if (channels.get(i).isIntermittent()) {
                break;
            }
            channels.get(i).exportToInkML(traceFormatNode);
        }
        if (i < channels.size()) {
            Element intermittendNode = parentNode.getOwnerDocument().createElement(INKML_INTERMITTENT_NAME);
            traceFormatNode.appendChild(intermittendNode);
            for (; i < channels.size(); i++) {
                channels.get(i).exportToInkML(intermittendNode);
            }
        }
        parentNode.appendChild(traceFormatNode);
    }

    /**
	 * Adds a channel to the traceFormat. This can only be done if the traceFormat is not final by then.
	 * Channels can not be changed if they are added to the traceFormat.
	 * @param channel new channel
	 * @throws InkMLComplianceException
	 */
    public void addChannel(InkChannel channel) throws InkMLComplianceException {
        assert channel != null;
        assert !isFinal();
        if (isFinal()) {
            throw new InkMLComplianceException("The TraceView is final now. You can not change it anymore. It would invalidate SampleSets.");
        }
        if (channels.size() > 0 && !channel.isIntermittent() && channels.get(channels.size() - 1).isIntermittent()) {
            throw new InkMLComplianceException("No non-intermittent channels can be added after intermittent channels");
        }
        if (index.containsKey(channel.getName())) {
            throw new InkMLComplianceException("Its not possibile to add more than one channel with same name to a TraceFormat");
        }
        channels.add(channel);
        index.put(channel.getName(), channels.indexOf(channel));
    }

    public Iterator<InkChannel> iterator() {
        return getChannels().iterator();
    }

    public InkChannel getChannel(ChannelName x) {
        for (InkChannel c : getChannels()) {
            if (c.getName() == x) {
                return c;
            }
        }
        return null;
    }

    public List<InkChannel> getChannels() {
        return new ArrayList<InkChannel>(this.channels);
    }

    public List<InkChannel> getMandatoryChannels() {
        return new ArrayList<InkChannel>(this.channels);
    }

    public boolean containsChannel(ChannelName s) {
        for (InkChannel c : this.getChannels()) {
            if (c.getName() == s) {
                return true;
            }
        }
        return false;
    }

    public int getChannelCount() {
        return this.channels.size();
    }

    /**
     * @param name
     * @param o
     * @return
     */
    public double doubleize(ChannelName name, Object o) {
        return channels.get(index.get(name)).doublize(o);
    }

    public Object objectify(ChannelName name, double d) {
        return channels.get(index.get(name)).objectify(d);
    }

    /**
     * @param name
     * @return
     */
    public int indexOf(ChannelName name) {
        return index.get(name);
    }

    public Map<ChannelName, Integer> getIndex() {
        return index;
    }

    /**
     * Sets this trace TraceFormat final. This will prevent further changes of this
     * TraceFormat. During finalization the TraceFormat is tested for sanity. If its
     * does not comply to InkML standard an corresponding Exeption is thrown. 
     * Only final TraceFormats can be used for SampleSets since changing a TraceFormat 
     * while it's used will invalidate the samplesets.
     * 
     * @throws InkMLComplianceException If the TraceFormat does not comply with InkML
     */
    public void setFinal() throws InkMLComplianceException {
        testInkAnnoCompliance();
        isFinal = true;
    }

    private void testInkAnnoCompliance() throws InkMLComplianceException {
        int i = 0;
        for (InkChannel c : channels) {
            if (c.getName() == InkChannel.ChannelName.X && !c.isIntermittent()) {
                i += 1;
            } else if (c.getName() == InkChannel.ChannelName.Y && !c.isIntermittent()) {
                i += 2;
            }
            c.setFinal();
        }
        if (i != 3) {
            throw new InkMLComplianceException("A TraceFormat must contain an X and a Y channel which are not intermittent. " + "This requirement is not explicetly stated in the definition however it is statet that: " + "'The simplest form of encoding specifies the X and Y coordinates of each sample point.'" + " Digital ink is not digital ink without X and Y coordinates.");
        }
    }

    /**
     * Test if this TraceFormat is final. Only final TraceFormats can be used for SampleSets since changing a TraceFormat 
     * while it's used will invalidate the samplesets.
     * @return true if this TraceFormat is final
     */
    public boolean isFinal() {
        return isFinal;
    }

    public void acceptAsCompatible(InkTraceFormat format, boolean strict) throws InkMLComplianceException {
        for (InkChannel channel : channels) {
            if (!channel.isIntermittent() && !format.containsChannel(channel.getName())) {
                throw new InkMLComplianceException(String.format("Channel '%s' is not present", channel.getName().toString()));
            }
            if (format.containsChannel(channel.getName())) {
                channel.acceptAsCompatible(format.getChannel(channel.getName()), strict);
            }
        }
    }

    public InkTraceFormat clone(InkInk ink) {
        InkTraceFormat f = new InkTraceFormat(ink);
        for (InkChannel channel : channels) {
            try {
                f.addChannel(channel.clone(ink));
            } catch (InkMLComplianceException e) {
                e.printStackTrace();
            }
        }
        return f;
    }
}
