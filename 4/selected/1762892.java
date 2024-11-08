package net.sf.webwarp.modules.partner.ui.trinidad;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.joda.time.DateTime;
import org.springframework.context.MessageSource;
import net.sf.webwarp.modules.partner.address.Address;
import net.sf.webwarp.modules.partner.address.AddressType;
import net.sf.webwarp.modules.partner.address.ContactChannel;
import net.sf.webwarp.modules.partner.address.ContactChannelType;

public class AddressDisplayBean implements Serializable {

    private static final long serialVersionUID = -5771958740499968761L;

    @SuppressWarnings("unchecked")
    public static Class[] constructorSignature = new Class[] { Address.class, AddressType.class, MessageSource.class, Locale.class, List.class };

    private Address address;

    protected MessageSource messageSource;

    protected Locale locale;

    private List<ContactChannelType> contactChannelTypes;

    private AddressType addressType;

    public AddressDisplayBean(Address address, AddressType addressType, MessageSource messageSource, Locale locale, List<ContactChannelType> contactChannelTypes) {
        this.address = address;
        this.addressType = addressType;
        this.messageSource = messageSource;
        this.locale = locale;
        this.contactChannelTypes = contactChannelTypes;
    }

    public String getType() {
        return addressType.getName();
    }

    public String getTypeName() {
        return messageSource.getMessage(addressType.getName(), null, addressType.getName(), locale);
    }

    public List<ContactChannelDisplayBean> getChannels() {
        List<ContactChannelDisplayBean> displayBeans = new ArrayList<ContactChannelDisplayBean>();
        for (ContactChannelType type : contactChannelTypes) {
            Class displayBeanClass = getContactChannelDisplayBeanClass();
            ContactChannel contactChannel = address.getChannels().get(type);
            if (contactChannel == null) {
                contactChannel = getContactChannelInstance(type);
                address.getChannels().put(type, contactChannel);
            }
            try {
                Constructor constructor = displayBeanClass.getConstructor(getContactChannelDisplayBeanConstructorSignature());
                ContactChannelDisplayBean displayBean = (ContactChannelDisplayBean) constructor.newInstance(contactChannel, type, messageSource, locale);
                displayBeans.add(displayBean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return displayBeans;
    }

    protected ContactChannel getContactChannelInstance(ContactChannelType type) {
        return new ContactChannel(type);
    }

    protected Class[] getContactChannelDisplayBeanConstructorSignature() {
        return ContactChannelDisplayBean.constructorSignature;
    }

    protected Class getContactChannelDisplayBeanClass() {
        return ContactChannelDisplayBean.class;
    }

    public DateTime getCreatedAt() {
        return address.getCreatedAt();
    }

    public String getCreatedFrom() {
        return address.getCreatedBy();
    }

    public Long getId() {
        return address.getId();
    }

    public String getLocation() {
        return address.getLocation();
    }

    public String getPlz() {
        return address.getPlz();
    }

    public String getPostBox() {
        return address.getPostBox();
    }

    public String getRegion() {
        return address.getRegion();
    }

    public String getStreet() {
        return address.getStreet();
    }

    public DateTime getUpdatedAt() {
        return address.getUpdatedAt();
    }

    public String getUpdatedFrom() {
        return address.getUpdatedBy();
    }

    public void setCreatedAt(DateTime createdAt) {
        address.setCreatedAt(createdAt);
    }

    public void setCreatedFrom(String createdFrom) {
        address.setCreatedBy(createdFrom);
    }

    public void setLocation(String location) {
        address.setLocation(location);
    }

    public void setPlz(String plz) {
        address.setPlz(plz);
    }

    public void setPostBox(String postBox) {
        address.setPostBox(postBox);
    }

    public void setRegion(String region) {
        address.setRegion(region);
    }

    public void setStreet(String street) {
        address.setStreet(street);
    }

    public String getCountry() {
        return address.getCountry();
    }

    public void setCountry(String country) {
        address.setCountry(country);
    }

    public boolean isDeleted() {
        return address.isDeleted();
    }
}
