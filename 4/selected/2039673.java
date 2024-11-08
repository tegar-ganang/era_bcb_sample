package net.sf.webwarp.modules.partner.partner.impl;

import java.util.List;
import net.sf.webwarp.modules.partner.UndeleatableException;
import net.sf.webwarp.modules.partner.address.Address;
import net.sf.webwarp.modules.partner.partner.MediaContent;
import net.sf.webwarp.modules.partner.partner.Partner;
import net.sf.webwarp.modules.partner.partner.PartnerDao;
import net.sf.webwarp.modules.partner.partner.Relation;
import net.sf.webwarp.modules.partner.partner.RelationDao;
import net.sf.webwarp.modules.partner.partner.RelationType;
import org.hibernate.criterion.Example;
import org.springframework.transaction.annotation.Transactional;
import net.sf.webwarp.util.hibernate.dao.MutationType;
import net.sf.webwarp.util.hibernate.dao.impl.BaseMutationEntityDAOImpl;

/**
 * DAO implementation of the Partner DAO interface.
 * 
 * @author atr
 * @param <T>
 */
@Transactional
public class PartnerDaoImpl<T extends Partner> extends BaseMutationEntityDAOImpl implements PartnerDao<T> {

    /** DAO for handling partner relations */
    protected RelationDao relationDao;

    public void addAddress(Long partnerID, Address address, String userName) {
        super.save(address, userName);
        Partner partner = (Partner) load(Partner.class, partnerID);
        partner.getAddresses().put(address.getAddressType(), address);
    }

    public void addAddress(Partner partner, Address address, String userName) {
        super.save(address, userName);
        partner = (Partner) load(Partner.class, partner.getId());
        partner.getAddresses().put(address.getAddressType(), address);
    }

    public void addMediaContent(Long partnerID, MediaContent mediaContent, String userName) {
        if (!(mediaContent instanceof MutationType)) {
            throw new IllegalArgumentException("Media content must inherit from MutationType: " + mediaContent);
        }
        super.save((MutationType) mediaContent, userName);
        Partner partner = (Partner) load(Partner.class, partnerID);
        partner.getMedia().add(mediaContent);
    }

    public void addMediaContent(Partner partner, MediaContent mediaContent, String userName) {
        if (!(mediaContent instanceof MutationType)) {
            throw new IllegalArgumentException("Media content must inherit from MutationType: " + mediaContent);
        }
        super.save((MutationType) mediaContent, userName);
        partner = (Partner) load(Partner.class, partner.getId());
        partner.getMedia().add(mediaContent);
    }

    public void deleteMediaContent(MediaContent mediaContent, String userName) {
        if (!(mediaContent instanceof MutationType)) {
            throw new IllegalArgumentException("Media content must inherit from MutationType: " + mediaContent);
        }
        super.delete((MutationType) mediaContent, userName);
    }

    public void deleteMediaContent(Long mediaContentID, String userName) {
        super.delete(LazyMediaContent.class, mediaContentID, userName);
    }

    public void deletePartner(Partner partner, String userName, boolean force) throws UndeleatableException {
        deletePartner(partner.getId(), partner.getClass(), userName, force);
    }

    @SuppressWarnings("unchecked")
    public void deletePartner(Long partnerID, Class partnerClass, String userName, boolean force) throws UndeleatableException {
        boolean throwing = false;
        List<Relation> relationsByPartner1 = relationDao.getRelationsByPartner1(partnerID);
        List<Relation> relationsByPartner2 = null;
        if (!force) {
            if (relationsByPartner1.isEmpty()) {
                List<RelationType> relationTypesByPartner2 = relationDao.getRelationTypesByPartner2(partnerClass, true);
                relationsByPartner2 = relationDao.getRelationsByPartner2(partnerID);
                for (Relation relationByPartner2 : relationsByPartner2) {
                    if (relationTypesByPartner2.contains(relationByPartner2.getRelationType())) {
                        throwing = true;
                    }
                }
            } else {
                throwing = true;
            }
            if (throwing) {
                throw new UndeleatableException("This Partner is still referenced by other objects");
            }
            super.delete(Partner.class, partnerID, userName);
            if (relationsByPartner2 != null) {
                for (Relation subRelation : relationsByPartner2) {
                    super.delete(subRelation, userName);
                }
            }
        } else {
            relationsByPartner2 = relationDao.getRelationsByPartner2(partnerID);
            super.delete(Partner.class, partnerID, userName);
            if (relationsByPartner1 != null) {
                for (Relation subRelation : relationsByPartner1) {
                    super.delete(subRelation, userName);
                }
            }
            if (relationsByPartner2 != null) {
                for (Relation subRelation : relationsByPartner2) {
                    super.delete(subRelation, userName);
                }
            }
        }
    }

    public void eraseMediaContent(MediaContent mediaContent) {
        super.purge(mediaContent, "purge-master");
    }

    public void eraseMediaContent(Long mediaContentID) {
        super.purge(load(LazyMediaContent.class, mediaContentID), "purge-master");
    }

    public void erasePartner(Partner partner) {
        super.purge(partner, "purge-master");
    }

    public void erasePartner(Long partnerID) {
        super.purge(load(Partner.class, partnerID), "purge-master");
    }

    @SuppressWarnings("unchecked")
    public List<T> getPartners(Long mandantID) {
        return (List<T>) _executeHQLQuery("from " + Partner.class.getName() + " p where p.mandantID=:mandantID and p.deleted=false", "mandantID", mandantID);
    }

    public Long insertPartner(Partner partner, String userName) {
        return (Long) super.save(partner, userName);
    }

    @SuppressWarnings("unchecked")
    public T loadPartner(Long id, boolean loadMedia, boolean loadAddresses, boolean loadContactChannels) {
        T partner = (T) super.load(Partner.class, id);
        if (loadMedia) {
            partner.getMedia().isEmpty();
        }
        if (loadAddresses) {
            if (loadContactChannels) {
                for (Address address : partner.getAddresses().values()) {
                    address.getChannels().isEmpty();
                }
            } else {
                partner.getAddresses().isEmpty();
            }
        }
        return partner;
    }

    @SuppressWarnings("unchecked")
    public List<T> searchPartners(Partner example) {
        return getSession().createCriteria(example.getClass()).add(Example.create(example).ignoreCase().enableLike()).list();
    }

    public void undeletePartner(Partner partner, String userName) {
        super.undelete(partner, userName);
    }

    public void undeletePartner(Long partnerID, String userName) {
        super.undelete(Partner.class, partnerID, userName);
    }

    public void updatePartner(Partner partner, String userName) {
        super.update(partner, userName);
    }

    public void undeleteMediaContent(MediaContent mediaContent, String userName) {
        if (!(mediaContent instanceof MutationType)) {
            throw new IllegalArgumentException("Media content must inherit from MutationType: " + mediaContent);
        }
        super.undelete((MutationType) mediaContent, userName);
    }

    public void undeleteMediaContent(Long mediaContentID, String userName) {
        super.undelete(LazyMediaContent.class, mediaContentID, userName);
    }

    /**
	 * Set the relation DAO to be used by this component
	 * 
	 * @param relationDao
	 *            The relation DAO to be used.
	 */
    public void setRelationDao(RelationDao<Relation> relationDao) {
        this.relationDao = relationDao;
    }
}
