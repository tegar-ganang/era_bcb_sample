package net.sf.webwarp.modules.partner.address;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import net.sf.webwarp.util.hibernate.dao.impl.AIDMutationTypeImpl;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;

/**
 * Entity that models a contact channel. A contact channel is a method how you can get in contact with a partner. For
 * additional properties and features just subclass this class.
 * 
 * @author mos
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "Address_ID", "ContactChannelType" }))
@Proxy(lazy = false)
public class ContactChannel extends AIDMutationTypeImpl<Long> {

    private static final long serialVersionUID = -4811335349282511764L;

    private ContactChannelType channelType;

    private String channel;

    private boolean preferred = false;

    /**
	 * Constructor
	 */
    public ContactChannel() {
    }

    /**
	 * Constructor
	 * 
	 * @param type
	 */
    public ContactChannel(ContactChannelType type) {
        this.channelType = type;
    }

    @Override
    @Id
    @GeneratedValue(generator = "autoGen")
    @GenericGenerator(name = "autoGen", strategy = "native", parameters = { @Parameter(name = "sequence", value = "ContactChannel_seq") })
    public Long getId() {
        return super.id;
    }

    /**
	 * Get the channel descriptor, e.g. the phone number or the email address
	 * 
	 * @return The channel descriptor
	 */
    public String getChannel() {
        return channel;
    }

    /**
	 * Set the channel descriptor, e.g. the phone number or the email address
	 * 
	 * @param channel
	 */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
	 * Get the preferred channel flag. If set to TRUE this channel is the preferred channel for the owning address.
	 * 
	 * @return the preferred channel flag
	 */
    public boolean isPreferred() {
        return preferred;
    }

    /**
	 * Set the preferred channel flag. If set to TRUE this channel is the preferred channel for the owning address.
	 * 
	 * @param preferred
	 */
    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    /**
	 * Get the type of this channel. The type identifies what kind of channel this instance is, e.g. the business email,
	 * the private email, the personal homepage
	 * 
	 * @return
	 */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @ForeignKey(name = "ContactCh_ContactChType_FK")
    @JoinColumn(name = "ContactChannelType")
    public ContactChannelType getChannelType() {
        return channelType;
    }

    /**
	 * Set the type of this channel. The type identifies what kind of channel this instance is, e.g. the business email,
	 * the private email, the personal homepage
	 * 
	 * @param type
	 */
    public void setChannelType(ContactChannelType type) {
        this.channelType = type;
    }
}
