package net.sf.webwarp.modules.partner.address.impl;

import java.util.List;
import net.sf.webwarp.modules.partner.address.Address;
import net.sf.webwarp.modules.partner.address.AddressDao;
import net.sf.webwarp.modules.partner.address.AddressType;
import net.sf.webwarp.modules.partner.address.ContactChannel;
import net.sf.webwarp.modules.partner.partner.Partner;
import org.hibernate.criterion.Example;
import org.springframework.transaction.annotation.Transactional;
import net.sf.webwarp.util.hibernate.dao.impl.BaseMutationEntityDAOImpl;

/**
 * Implementation class of the address DAO interface.
 * 
 * @author mos
 */
@Transactional
public class AddressDaoImpl extends BaseMutationEntityDAOImpl implements AddressDao {

    public void addContactChannel(Long addressID, ContactChannel contactChannel, String userName) {
        super.save(contactChannel, userName);
        Address address = (Address) super.load(Address.class, addressID);
        address.getChannels().put(contactChannel.getChannelType(), contactChannel);
    }

    public void addContactChannel(Address address, ContactChannel contactChannel, String userName) {
        super.save(contactChannel, userName);
        address = (Address) super.load(Address.class, address.getId());
        address.getChannels().put(contactChannel.getChannelType(), contactChannel);
    }

    public void deleteAddress(Address address, String userName) {
        super.delete(address, userName);
    }

    public void deleteContactChannel(ContactChannel contactChannel, String userName) {
        super.delete(contactChannel, userName);
    }

    public void deleteContactChannel(Long contactChannelID, String userName) {
        super.delete(ContactChannel.class, contactChannelID, userName);
    }

    public void eraseAddress(Address address) {
        super.purge(address, "purge-master");
    }

    public void eraseContactChannel(ContactChannel contactChannel) {
        super.purge(contactChannel, "purge-master");
    }

    public void eraseContactChannel(Long contactChannelID) {
        super.purge(load(ContactChannel.class, contactChannelID), "purge-master");
    }

    @SuppressWarnings("unchecked")
    public List getAddresses(Long partnerID) {
        List<Address> list = getSession().createQuery("select distinct p.addresses from " + Partner.class.getName() + " p  where p.id=:id and p.addresses.deleted=false").setParameter("id", partnerID).list();
        for (Address address : list) {
            address.getChannels().isEmpty();
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public List getAddresses(Long partnerID, String addressTypeName) {
        AddressType type = new AddressType();
        type.setId(1);
        List<Address> list = getSession().createQuery("select p.addresses from " + Partner.class.getName() + " p where p.id=:id and p.addresses.addressType=:addressType and p.addresses.deleted=false").setParameter("id", partnerID).setParameter("addressType", type).list();
        for (Address address : list) {
            address.getChannels().isEmpty();
        }
        return list;
    }

    public Long insertAddress(Address address, String userName) {
        return (Long) super.save(address, userName);
    }

    public Address loadAddress(Long id, boolean loadCollections) {
        Address address = (Address) super.load(Address.class, id);
        if (loadCollections) {
            address.getChannels().entrySet();
        }
        return address;
    }

    public List searchAddresses(Address example) {
        return getSession().createCriteria(example.getClass()).add(Example.create(example).ignoreCase().enableLike()).list();
    }

    public void updateAddress(Address address, String userName) {
        super.update(address, userName);
    }

    public void updateContactChannel(ContactChannel contactChannel, String userName) {
        super.update(contactChannel, userName);
    }

    public void undeleteAddress(Address address, String userName) {
        super.undelete(address, userName);
    }

    public void undeleteContactChannel(ContactChannel contactChannel, String userName) {
        super.undelete(contactChannel, userName);
    }

    public void undeleteContactChannel(Long contactChannelID, String userName) {
        super.undelete(ContactChannel.class, contactChannelID, userName);
    }
}
