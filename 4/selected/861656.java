package net.sourceforge.jcoupling2.dao;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.jcoupling2.dao.internal.Utilities;
import net.sourceforge.jcoupling2.dao.obsolete.Attribute;
import net.sourceforge.jcoupling2.dao.obsolete.AttributeException;
import net.sourceforge.jcoupling2.dao.obsolete.DataAccessObject;
import net.sourceforge.jcoupling2.dao.obsolete.Operation;
import net.sourceforge.jcoupling2.dao.obsolete.Range;
import net.sourceforge.jcoupling2.dao.obsolete.ToBeRegisteredProperties;
import net.sourceforge.jcoupling2.dao.obsolete.ToBeRegisteredProperty;
import net.sourceforge.jcoupling2.dao.obsolete.TransferObject;
import net.sourceforge.jcoupling2.dao.obsolete.TransferObjectHandler;
import org.apache.log4j.Logger;

/**
 * A <code>channel</code> object is a technology-agnostic abstraction of a "conduit". It serves to pass messages between
 * communication endpoints. For a <code>channel</code> to become persistent it must first be added to the
 * {@link ChannelRepository ChannelRepository}.
 */
public class Channel extends DataAccessObject implements Comparable<Channel> {

    private static Logger log = Logger.getLogger(Channel.class.getSimpleName());

    /**
	 * A static set of elements of type {@link net.sourceforge.jcoupling2.dao.obsolete.Operation Operation}, specifying the
	 * operations on a channel that are supported by the persistence layer. This <code>TreeSet</code> is loaded upon
	 * start-up of JCoupling and will remain unchanged until shutdown.
	 */
    public static TreeSet<Operation> supportedOperations = null;

    /**
	 * A static set of elements of type {@link net.sourceforge.jcoupling2.dao.obsolete.Attribute Attribute}, specifying the
	 * channel attributes that the persistence layer is able to support (e.g. "channel ID" and "channel name"). The
	 * attributes are loaded into the <code>TreeSet</code> upon start-up of JCoupling and will remain unchanged until
	 * shutdown. Typically, they don't possess a value as they are not related to any object instance. Be careful to
	 * specify only attributes that are contained in this <code>TreeSet</code> object.
	 */
    public static TreeSet<Attribute> supportedAttributes = null;

    /**
	 * A dynamic set of elements of type {@link net.sourceforge.jcoupling2.dao.obsolete.Attribute Attribute}, i.e. the
	 * attributes of the channel <b>instance</b>. Unlike the static attributes (where is makes no sense to assign to them
	 * values), the instance attribute usually carry values. Be careful to specify only attributes that are elements of
	 * {@linkplain net.sourceforge.jcoupling2.dao.Channel#supportedAttributes supportedAttributes}.
	 */
    private TreeSet<Attribute> channelAttributes = new TreeSet<Attribute>();

    /**
	 * The transfer object relates to the data access pattern. It takes the Dao's attributes as the payload.
	 */
    private TransferObject transferObject = null;

    private TransferObjectHandler handler = null;

    private String storedProcedure = null;

    /**
	 * The name of the attribute that specifies the channel's ID.
	 */
    public static final String CHANNEL_ID = "channelId";

    /**
	 * The name of the attribute that specifies the channel name.
	 */
    public static final String CHANNEL_NAME = "channelName";

    public Channel() {
    }

    /**
	 * 
	 * @param channelName
	 */
    public Channel(String channelName) {
        this.setChannelName(channelName);
    }

    /**
	 * Returns an instance of <code>Channel</code> where it's attributes are set to the values that are specified by the
	 * argument.
	 * 
	 * @param attributes
	 *          - one or more attributes where each of the must be an element of the set of supported attributes
	 * @throws AttributeException
	 */
    public Channel(TreeSet<Attribute> attributes) throws AttributeException {
        this.channelAttributes = attributes;
    }

    /**
	 * Creates or alters the fingerprint table that is associated with the current channel. By default it will have a
	 * timestamp property.
	 * 
	 * @param destination
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
    public void registerProperties(Set<net.sourceforge.jcoupling.peer.mql.Property> properties) throws IllegalArgumentException, IllegalAccessException {
        ToBeRegisteredProperties toBeRegisteredProperties = new ToBeRegisteredProperties();
        for (Attribute attribute : channelAttributes) {
            if (attribute.name.equals("channelname")) {
                toBeRegisteredProperties.tablename = attribute.value;
            }
        }
        boolean storedProcedurefound = false;
        for (net.sourceforge.jcoupling.peer.mql.Property property : properties) {
            if (storedProcedurefound == false) {
                Class classDefinition = property.getClass();
                Operation operation = new Operation();
                Field fieldlist[] = classDefinition.getDeclaredFields();
                String fieldname;
                for (int i = 0; i < fieldlist.length; i++) {
                    Field fld = fieldlist[i];
                    fieldname = fld.getName();
                    if (fieldname.equals("storedProcedureRegister")) {
                        storedProcedure = fld.get(property).toString();
                    }
                }
                storedProcedurefound = true;
            }
            ToBeRegisteredProperty propertyresult = new ToBeRegisteredProperty();
            propertyresult.propertyname = property.getName();
            propertyresult.columntype = property.getDBColumnType();
            toBeRegisteredProperties.properties.add(propertyresult);
        }
        try {
            Operation operation = new Operation();
            operation.operationType = Range.OperationType.REGISTER;
            transferObject = new TransferObject(operation);
            transferObject.setStoredProcedure(storedProcedure);
            transferObject.setToBeRegisteredProperties(toBeRegisteredProperties);
            handler = new TransferObjectHandler();
            transferObject = handler.processDdlStmnt(transferObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TreeSet<Attribute> getChannelAttributes() {
        if (this.channelAttributes == null) {
            this.channelAttributes = new TreeSet<Attribute>();
        }
        return this.channelAttributes;
    }

    public String getChannelAttributeValue(String attributename) {
        Iterator<Attribute> iterator = channelAttributes.iterator();
        Attribute attribute;
        String result = null;
        while (iterator.hasNext()) {
            attribute = iterator.next();
            if (attribute.name.equals(attributename)) {
                result = attribute.value;
            }
        }
        return result;
    }

    /**
	 * Set the passed attributes to the channel after having performed a type compliance check.
	 * 
	 * @throws AttributeException
	 *           if one or more {@link net.sourceforge.jcoupling2.dao.obsolete.Attribute Attribute} objects in the
	 *           <code>TreeSet</code>, that is passed to the method, are invalid, e.g. if they are not type compliant.
	 */
    public void setChannelAttributesChecked(TreeSet<Attribute> attributes) throws AttributeException {
        if (Utilities.isTypeCompliant(attributes)) {
            this.channelAttributes = attributes;
        } else {
            throw new AttributeException(AttributeException.NOT_TYPE_COMLIANT);
        }
    }

    /**
	 * 
	 * @param attributes
	 * @throws AttributeException
	 *           if one or more {@link net.sourceforge.jcoupling2.dao.obsolete.Attribute Attribute} objects in the
	 *           <code>TreeSet</code>, that is passed to the method, are invalid, e.g. if they are not supported by the
	 *           persistence layer.
	 */
    public void setChannelAttributes(TreeSet<Attribute> attributes) throws AttributeException {
        this.channelAttributes = attributes;
    }

    /**
	 * 
	 * @param channelId
	 */
    public void setId(String channelId) {
        Attribute attribute = null;
        attribute = new Attribute(Channel.CHANNEL_ID, channelId);
        if (channelAttributes.contains(attribute)) {
            channelAttributes.remove(attribute);
        }
        channelAttributes.add(attribute);
    }

    /**
	 * 
	 * @param channelName
	 */
    private void setChannelName(String channelName) {
        String name = Channel.CHANNEL_NAME;
        String value = channelName;
        Attribute attribute = createAttribute(name, value);
        if (attribute != null) {
            this.channelAttributes.add(attribute);
        }
    }

    /**
	 * 
	 */
    private Attribute createAttribute(String name, String value) {
        Iterator<Attribute> attributeList = Channel.supportedAttributes.iterator();
        Attribute supportedAttribute = null;
        Attribute attribute = null;
        Range.Datatype type = null;
        Integer id = null;
        while (attributeList.hasNext()) {
            supportedAttribute = attributeList.next();
            if (supportedAttribute.name.equals(name)) {
                id = supportedAttribute.id;
                type = supportedAttribute.datatype;
                attribute = new Attribute(id, name, value, type);
                break;
            }
        }
        return attribute;
    }

    public String toString() {
        Iterator<Attribute> attributeList = this.channelAttributes.iterator();
        Attribute attribute = null;
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.getClass().getSimpleName());
        buffer.append("[");
        while (attributeList.hasNext()) {
            attribute = attributeList.next();
            buffer.append(attribute.name);
            buffer.append("=");
            buffer.append(attribute.value);
            buffer.append(",");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("]");
        return buffer.toString();
    }

    /**
	 * We need to implement the method <code>compareTo()</code> because we have implemented the interface
	 * <code>Comparable</code>. Comparing two objects is based on the assumption that each object has been assigned a set
	 * of supported attributes (<code>supportedAtributes</code>) where only one of them is the primary attribute which is
	 * exactly the one that we use for comparison. Note that in case we have a "bare object", i.e. with no supported
	 * attributes specified at all, a comparison does not make much sense and we will return a "1" instead.
	 */
    public int compareTo(Channel channel) {
        Iterator<Attribute> attributeList = null;
        Attribute attribute = null;
        String thisId = null;
        String id = null;
        if (this.channelAttributes != null && channel.channelAttributes != null) {
            attributeList = this.channelAttributes.iterator();
            while (attributeList.hasNext()) {
                attribute = attributeList.next();
                if (attribute.isPrimary) {
                    thisId = attribute.value;
                }
            }
            attributeList = channel.channelAttributes.iterator();
            while (attributeList.hasNext()) {
                attribute = attributeList.next();
                if (attribute.isPrimary) {
                    id = attribute.value;
                }
            }
            if (thisId != null && id != null) {
                return thisId.compareTo(id);
            } else {
                return 1;
            }
        } else {
            return 1;
        }
    }

    public void updateFingerprintTable(Message message) {
    }

    public boolean supportsInbound() {
        return "true".equals(getChannelAttributeValue("supportsinbound"));
    }

    public String getName() {
        return getChannelAttributeValue("channelname");
    }
}
