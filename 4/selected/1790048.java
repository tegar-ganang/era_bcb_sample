package net.sf.webwarp.modules.partner.address;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import net.sf.webwarp.modules.partner.address.impl.AddressDaoImpl;
import net.sf.webwarp.util.hibernate.dao.RecursiveMutationType;
import net.sf.webwarp.util.hibernate.dao.impl.AIDMutationTypeImpl;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;
import org.joda.time.DateTime;

/**
 * Entity that models an address of a partner. For additional properties inherit from this class and initialize the
 * corresponding DAO component as needed.
 * 
 * @see AddressDaoImpl
 * @author mos
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "Partner_ID", "AddressType" }))
@Proxy(lazy = false)
public class Address extends AIDMutationTypeImpl<Long> implements RecursiveMutationType {

    private static final long serialVersionUID = 6901661309367423872L;

    private AddressType addressType;

    private Map<ContactChannelType, ContactChannel> channels = new HashMap<ContactChannelType, ContactChannel>();

    private String street;

    private String zipCode;

    private String postBox;

    private String location;

    private String region;

    private String country;

    /**
	 * Constructor
	 */
    public Address() {
    }

    /**
	 * Constructor
	 * 
	 * @param type
	 *            The address type
	 */
    public Address(AddressType type) {
        this.addressType = type;
    }

    public void setCreatedFromRecursive(String userID) {
        setCreatedBy(userID);
        for (ContactChannel contactChannel : channels.values()) {
            if (contactChannel instanceof RecursiveMutationType) {
                ((RecursiveMutationType) contactChannel).setCreatedFromRecursive(userID);
            }
        }
    }

    public void setUpdatedAtRecursive(DateTime updatedAt) {
        setUpdatedAt(updatedAt);
        for (ContactChannel contactChannel : channels.values()) {
            if (contactChannel instanceof RecursiveMutationType) {
                ((RecursiveMutationType) contactChannel).setUpdatedAtRecursive(updatedAt);
            }
        }
    }

    public void setUpdatedFromRecursive(String updatedFrom) {
        setUpdatedBy(updatedFrom);
        for (ContactChannel contactChannel : channels.values()) {
            if (contactChannel instanceof RecursiveMutationType) {
                ((RecursiveMutationType) contactChannel).setUpdatedFromRecursive(updatedFrom);
            }
        }
    }

    public void setDeletedRecursive(boolean deleted) {
        setDeleted(deleted);
        for (ContactChannel contactChannel : channels.values()) {
            if (contactChannel instanceof RecursiveMutationType) {
                ((RecursiveMutationType) contactChannel).setDeletedRecursive(deleted);
            }
        }
    }

    @Override
    @Id
    @GeneratedValue(generator = "autoGen")
    @GenericGenerator(name = "autoGen", strategy = "native", parameters = { @Parameter(name = "sequence", value = "Address_seq") })
    public Long getId() {
        return super.id;
    }

    /**
	 * Get the map with the contact channels. Key's are the channel types (email, skype etc.).
	 * 
	 * @return
	 */
    @OneToMany(cascade = CascadeType.ALL)
    @ForeignKey(name = "Address_ContactChannel_FK")
    @JoinColumn(name = "Address_ID")
    @MapKey(name = "channelType")
    public Map<ContactChannelType, ContactChannel> getChannels() {
        return channels;
    }

    /**
	 * Set the channels of this address
	 * 
	 * @param channels
	 */
    public void setChannels(Map<ContactChannelType, ContactChannel> channels) {
        this.channels = channels;
    }

    /**
	 * Get the address's location
	 * 
	 * @return
	 */
    public String getLocation() {
        return location;
    }

    /**
	 * Sets the location
	 * 
	 * @param location
	 */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
	 * Get the zip code
	 * 
	 * @return
	 * @deprecated Use the method getZipCode instead
	 */
    @Transient
    public String getPlz() {
        return getZipCode();
    }

    /**
	 * Get the zip code
	 * 
	 * @return
	 */
    public String getZipCode() {
        return zipCode;
    }

    /**
	 * Set the zip code
	 * 
	 * @deprecated Use the method setZipCode instead
	 * @param plz
	 */
    public void setPlz(String plz) {
        setZipCode(plz);
    }

    /**
	 * Set the zip code
	 * 
	 * @param plz
	 */
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    /**
	 * Get the PO Box
	 * 
	 * @return
	 */
    public String getPostBox() {
        return postBox;
    }

    /**
	 * Set the PO box
	 * 
	 * @param postBox
	 */
    public void setPostBox(String postBox) {
        this.postBox = postBox;
    }

    /**
	 * Get the region
	 * 
	 * @return
	 */
    public String getRegion() {
        return region;
    }

    /**
	 * Set the region
	 * 
	 * @param region
	 */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
	 * Get the street
	 * 
	 * @return
	 */
    public String getStreet() {
        return street;
    }

    /**
	 * Set the street
	 * 
	 * @param street
	 */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
	 * Gets the address type. This is used since a partner can have multiple addresses.
	 * 
	 * @return
	 */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @ForeignKey(name = "Address_AddressType_FK")
    @JoinColumn(name = "AddressType")
    public AddressType getAddressType() {
        return addressType;
    }

    /**
	 * Sets the address type. This is used since a partner can have multiple addresses.
	 * 
	 * @param type
	 */
    public void setAddressType(AddressType type) {
        this.addressType = type;
    }

    /**
	 * Get the country name or code. The country code can (but must not!) reference an item of type Country.
	 * 
	 * @see Country
	 * @return The country name or code
	 */
    public String getCountry() {
        return country;
    }

    /**
	 * Set the country name or code. The country code can (but must not!) reference an item of type Country.
	 * 
	 * @see Country
	 * @param country
	 *            Country name or code
	 */
    public void setCountry(String country) {
        this.country = country;
    }
}
