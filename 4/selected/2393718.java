package gov.sns.xal.smf;

import gov.sns.ca.*;
import gov.sns.xal.smf.attr.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.tools.data.*;
import java.util.*;
import java.util.logging.*;
import gov.sns.tools.transforms.OffsetTransform;

/**
 * The base class in the hierarchy of different accelerator node types.
 *
 * @author  Nikolay Malitsky, Christopher K. Allen, Nick D. Pattengale
 * @version $Id: AcceleratorNode.java 2 2006-08-17 12:20:30 +0000 (Thursday, 17 8 2006) t6p $
 */
public abstract class AcceleratorNode implements ElementType, DataListener {

    /** node identifier  */
    protected String m_strId;

    /** position of node   */
    protected double m_dblPos;

    /** length of node */
    protected double m_dblLen;

    /**   parent sequence object  */
    protected AcceleratorSeq m_seqParent;

    /**   the associated Accelerator object  */
    protected Accelerator m_objAccel;

    /**   all attribute buckets for node   */
    protected HashMap<String, AttributeBucket> m_mapAttrs;

    /**   alignment attribute bucket for node */
    protected AlignmentBucket m_bucAlign;

    /**    twiss parameter bucket for node   */
    protected TwissBucket m_bucTwiss;

    /**                  aperture parameters for node   */
    protected ApertureBucket m_bucAper;

    /** Indicator as to whether the Accelerator Node is functional */
    protected boolean m_bolStatus;

    /** Indicator as to whether accelerator node is valid */
    protected boolean m_bolValid;

    /** Indicator if this node is a "softNode" copy */
    protected boolean m_bolIsSoft = false;

    /** channel suite associated with this node */
    protected ChannelSuite channelSuite;

    /** Derived class must furnish a unique type id */
    public abstract String getType();

    /** 
	 * base constructor for an Accelector Node
     * @param strId the string ID for this node 
	 */
    public AcceleratorNode(String strId) {
        m_strId = strId;
        m_bolStatus = true;
        m_bolValid = true;
        m_mapAttrs = new HashMap<String, AttributeBucket>();
        setAlign(new AlignmentBucket());
        setAper(new ApertureBucket());
        setTwiss(new TwissBucket());
        channelSuite = new ChannelSuite();
    }

    /** implement DataListener interface */
    public String dataLabel() {
        return "node";
    }

    /** implement DataListener interface */
    public void update(DataAdaptor adaptor) throws NumberFormatException {
        if (m_strId == null) {
            m_strId = adaptor.stringValue("id");
        }
        if (adaptor.hasAttribute("status")) {
            m_bolStatus = adaptor.booleanValue("status");
        }
        if (adaptor.hasAttribute("len")) {
            double newLength;
            try {
                newLength = adaptor.doubleValue("len");
            } catch (NumberFormatException exception) {
                final String message = "Error reading node: " + m_strId;
                System.err.println(message);
                System.err.println(exception);
                Logger.getLogger("global").log(Level.SEVERE, message, exception);
                newLength = Double.NaN;
            }
            setLength(newLength);
        }
        if (adaptor.hasAttribute("pos")) {
            double newPosition = adaptor.doubleValue("pos");
            setPosition(newPosition);
        }
        DataAdaptor suiteAdaptor = adaptor.childAdaptor("channelsuite");
        if (suiteAdaptor != null) {
            channelSuite.update(suiteAdaptor);
        }
        Iterator<? extends DataAdaptor> parserIter = adaptor.childAdaptorIterator("attributes");
        while (parserIter.hasNext()) {
            DataAdaptor parserAdaptor = parserIter.next();
            Collection<AttributeBucket> buckets = getBuckets();
            BucketParser parser = new BucketParser(buckets);
            parser.update(parserAdaptor);
            Collection<AttributeBucket> bucketList = parser.getBuckets();
            Iterator<AttributeBucket> bucketIterator = bucketList.iterator();
            while (bucketIterator.hasNext()) {
                AttributeBucket bucket = bucketIterator.next();
                if (getType().equals("Q") || getType().equals("PQ") || getType().equals("QH") || getType().equals("QV") || getType().equals("PMQH") || getType().equals("PMQV")) {
                    if (bucket.getClass().equals(MagnetBucket.class)) {
                        MagnetBucket magbucket = (MagnetBucket) bucket;
                        double field = magbucket.getDfltField();
                        double polarity = magbucket.getPolarity();
                        if (field != 0) {
                            if ((field * polarity) < 0) {
                                System.err.println("WARNING ********************** AcceleratorNode.update, inconsistency between Magbucket field and polarity ************");
                                System.err.println("id, field, polarity = " + getId() + " " + field + " " + polarity);
                            }
                        }
                    }
                }
                if (!hasBucket(bucket)) {
                    addBucket(bucket);
                }
            }
        }
    }

    /** implement DataListener interface */
    public void write(DataAdaptor adaptor) {
        adaptor.setValue("id", m_strId);
        adaptor.setValue("type", getType());
        adaptor.setValue("status", m_bolStatus);
        adaptor.setValue("pos", m_dblPos);
        adaptor.setValue("len", m_dblLen);
        Collection<AttributeBucket> buckets = getBuckets();
        adaptor.writeNode(new BucketParser(buckets));
        adaptor.writeNode(channelSuite);
    }

    /** Identify the node by its unique ID */
    @Override
    public String toString() {
        return getId();
    }

    /** this method returns the Channel object of this node, associated with
     * a prescibed PV name. Note - xal interacts with EPICS via Channel objects.
     * @param chanHandle The handle to the epics channel in stored in the channel suite
     */
    public Channel getChannel(String chanHandle) throws NoSuchChannelException {
        Channel channel = channelSuite.getChannel(chanHandle);
        if (channel == null) {
            throw new NoSuchChannelException(this, chanHandle);
        }
        return channel;
    }

    public void align(String chanHandle) throws NoSuchChannelException {
        Channel channel = channelSuite.getChannel(chanHandle);
        if (channel == null) {
            throw new NoSuchChannelException(this, chanHandle);
        }
        if (chanHandle.equals("xAvg")) {
            OffsetTransform aTransform = new OffsetTransform(getAlign().getX());
            System.out.println("alignment x for " + chanHandle + " = " + getAlign().getX());
            channel.setValueTransform(aTransform);
        } else if (chanHandle.equals("yAvg")) {
            OffsetTransform aTransform = new OffsetTransform(getAlign().getY());
            System.out.println("alignment y for " + chanHandle + " = " + getAlign().getY());
            channel.setValueTransform(aTransform);
        }
    }

    public void unalign(String chanHandle) throws NoSuchChannelException {
        Channel channel = channelSuite.getChannel(chanHandle);
        if (channel == null) {
            throw new NoSuchChannelException(this, chanHandle);
        }
        if (chanHandle.equals("xAvg")) {
            OffsetTransform aTransform = new OffsetTransform(0);
            System.out.println("reset alignment x for " + chanHandle);
            channel.setValueTransform(aTransform);
        } else if (chanHandle.equals("yAvg")) {
            OffsetTransform aTransform = new OffsetTransform(0);
            System.out.println("reset alignment y for " + chanHandle);
            channel.setValueTransform(aTransform);
        }
    }

    /**
     * Get the channel corresponding to the specified handle and connect it. 
     * @param handle The handle for the channel to get.
     * @return The channel associated with this node and the specified handle or null if there is no match.
     * @throws gov.sns.xal.smf.NoSuchChannelException if no such channel as specified by the handle is associated with this node.
     * @throws gov.sns.ca.ConnectionException if the channel cannot be connected
     */
    public Channel getAndConnectChannel(final String handle) throws NoSuchChannelException, ConnectionException {
        final Channel channel = getChannel(handle);
        channel.connectAndWait();
        return channel;
    }

    /**
     * A method to make an EPICS ca  connection for a given PV name
     * The channel connection is initiated, and no extra work is
     * done, if the channel connection already exists
     */
    public Channel lazilyGetAndConnect(String chanHandle, Channel channel) throws ConnectionException, NoSuchChannelException {
        Channel tmpChan;
        if (channel == null) {
            tmpChan = getChannel(chanHandle);
            if (tmpChan == null) {
                throw new NoSuchChannelException(this, chanHandle);
            }
        } else {
            tmpChan = channel;
        }
        tmpChan.connectAndWait();
        return tmpChan;
    }

    /** return the ID of this node */
    public String getId() {
        return m_strId;
    }

    ;

    /** return the physical length of this node (m) */
    public double getLength() {
        return m_dblLen;
    }

    ;

    /** return the position of this node,  along the reference orbit
     * within its sequence (m) */
    public double getPosition() {
        return m_dblPos;
    }

    ;

    /** return the top level accelerator that this node belongs to */
    public Accelerator getAccelerator() {
        return m_objAccel;
    }

    ;

    /** return the parent sequence that this node belongs to */
    public AcceleratorSeq getParent() {
        return m_seqParent;
    }

    /** get the primary ancestor sequence that is a direct child of the accelerator */
    public AcceleratorSeq getPrimaryAncestor() {
        return getParent().getPrimaryAncestor();
    }

    /** Indicates if the node has a parent set */
    public boolean hasParent() {
        return (m_seqParent != null);
    }

    /**
     *  Runtime indication of accelerator component operation
     *  @return         true(up and running)
     *                  false(down)
     */
    public boolean getStatus() {
        return m_bolStatus;
    }

    ;

    /**
     *  Runtime indication of the validatity of component operation
     *  @return         true(valid operation)
     *                  false(questionable operation)
     */
    public boolean getValid() {
        return m_bolValid;
    }

    ;

    void setId(String value) {
        m_strId = value;
    }

    public void setPosition(double dblPos) {
        m_dblPos = dblPos;
    }

    ;

    public void setLength(double dblLen) {
        m_dblLen = dblLen;
    }

    ;

    /**
     *  Runtime indication of accelerator operation
     *  @param      bolStatus       true(up and running)
     *                              false(down)
     */
    public void setStatus(boolean bolStatus) {
        m_bolStatus = bolStatus;
    }

    ;

    /**
     *  Runtime indication of the validatity of component operation
     *  @param  bolValid    true(valid operation)
     *                      false(questionable operation)
     */
    public void setValid(boolean bolValid) {
        m_bolValid = bolValid;
    }

    ;

    /** General attribute buckets support */
    public void addBucket(AttributeBucket buc) {
        if (buc.getClass().equals(TwissBucket.class)) {
            setTwiss((TwissBucket) buc);
        }
        if (buc.getClass().equals(AlignmentBucket.class)) {
            setAlign((AlignmentBucket) buc);
        }
        if (buc.getClass().equals(ApertureBucket.class)) {
            setAper((ApertureBucket) buc);
        }
        m_mapAttrs.put(buc.getType(), buc);
    }

    ;

    public Collection<AttributeBucket> getBuckets() {
        return m_mapAttrs.values();
    }

    ;

    public AttributeBucket getBucket(String type) {
        return m_mapAttrs.get(type);
    }

    ;

    public boolean hasBucket(AttributeBucket bucket) {
        String bucketType = bucket.getType();
        return bucket == getBucket(bucketType);
    }

    /** returns the bucket containing the twiss parameters
     *   - see attr.TwissBucket  */
    public TwissBucket getTwiss() {
        return m_bucTwiss;
    }

    ;

    /** returns the bucket containing the alignment parameters
     *   - see attr.AlignBucket  */
    public AlignmentBucket getAlign() {
        return m_bucAlign;
    }

    ;

    /**
     * returns device pitch angle in degrees
     * @return pitch angle
     */
    public double getPitchAngle() {
        return m_bucAlign.getPitch();
    }

    /**
     * returns device yaw angle in degrees
     * @return yaw angle
     */
    public double getYawAngle() {
        return m_bucAlign.getYaw();
    }

    /**
     * returns device roll angle in degrees
     * @return roll angle
     */
    public double getRollAngle() {
        return m_bucAlign.getRoll();
    }

    /**
     * returns device x offset
     * @return x offset
     */
    public double getXOffset() {
        return m_bucAlign.getX();
    }

    /**
     * returns device y offset
     * @return y offset
     */
    public double getYOffset() {
        return m_bucAlign.getY();
    }

    /**
     * returns device z offset
     * @return z offset
     */
    public double getZOffset() {
        return m_bucAlign.getZ();
    }

    /** returns the bucket containing the Aperture parameters
     *   - see attr.ApertureBucket  */
    public ApertureBucket getAper() {
        return m_bucAper;
    }

    ;

    /** sets the bucket containing the twiss parameters
     *   - see attr.TwissBucket  */
    public void setAlign(AlignmentBucket buc) {
        m_bucAlign = buc;
        m_mapAttrs.put(buc.getType(), buc);
    }

    ;

    /** sets the bucket containing the alignment parameters
     *   - see attr.AlignBucket  */
    public void setTwiss(TwissBucket buc) {
        m_bucTwiss = buc;
        m_mapAttrs.put(buc.getType(), buc);
    }

    ;

    /** sets the bucket containing the Aperture parameters
     *   - see attr.ApertureBucket  */
    public void setAper(ApertureBucket buc) {
        m_bucAper = buc;
        m_mapAttrs.put(buc.getType(), buc);
    }

    ;

    /**
     * set device pitch angle
     * @param angle pitch angle in degree
     */
    public void setPitchAngle(double angle) {
        m_bucAlign.setPitch(angle);
    }

    /**
     * set device yaw angle
     * @param angle yaw angle in degree
     */
    public void setYawAngle(double angle) {
        m_bucAlign.setYaw(angle);
    }

    /**
     * set device roll angle
     * @param angle roll angle in degree
     */
    public void setRollAngle(double angle) {
        m_bucAlign.setRoll(angle);
    }

    /**
     * set device x offset
     * @param offset x offset
     */
    public void setXOffset(double offset) {
        m_bucAlign.setX(offset);
    }

    /**
     * set device y offset
     * @param offset y offset
     */
    public void setYOffset(double offset) {
        m_bucAlign.setY(offset);
    }

    /**
     * set device z offset
     * @param offset z offset
     */
    public void setZOffset(double offset) {
        m_bucAlign.setZ(offset);
    }

    /**
     * remove this node from the accelerator hieracrhcy
     */
    public void clear() {
        removeFromParent();
    }

    ;

    /**
     * remove this node from its immediate parent sequence
     */
    protected void removeFromParent() {
        if (m_seqParent == null) return;
        m_seqParent.removeNode(this);
    }

    ;

    /**
     * define the parent sequence for this node
     */
    protected void setParent(AcceleratorSeq parent) {
        removeFromParent();
        m_seqParent = parent;
    }

    ;

    /**
     * set the top level accelerator for this node
     */
    protected void setAccelerator(Accelerator accel) {
        if (m_objAccel != null) {
            m_objAccel.nodeRemoved(this);
        }
        m_objAccel = accel;
        if (accel != null) {
            accel.nodeAdded(this);
        }
    }

    /** channel suite accessor */
    public ChannelSuite channelSuite() {
        return channelSuite;
    }

    /** accessor to channel suite handles */
    public Collection<String> getHandles() {
        return channelSuite.getHandles();
    }

    /** 
     * Determine if a node is of the specified type.  The comparison is based 
     * upon the node's class and the element type manager handles checking 
     * for inherited classes to types get inherited.  Subclasses can override 
     * this method if the types comparison is more complicated (e.g. if more 
     * than one type can be associated with the same node class).
     * @param compType The type against which to compare.
     * @return true if the node is of the specified type; false otherwise.
     */
    public boolean isKindOf(String compType) {
        return ElementTypeManager.defaultManager().match(this.getClass(), compType);
    }

    /**
     * Determine if the node is a magnet.
     * @return true if the node is a magnet; false other.
     */
    public boolean isMagnet() {
        return false;
    }
}
