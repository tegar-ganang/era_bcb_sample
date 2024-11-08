package com.tysanclan.site.projectewok.entities;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Index;
import com.jeroensteenbeeke.hyperion.data.BaseDomainObject;

/**
 * @author Jeroen Steenbeeke
 */
@Entity
@AccessType("field")
@Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL, region = "main")
public class BattleNetChannel extends BaseDomainObject {

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BattleNetChannel")
    @SequenceGenerator(name = "BattleNetChannel", sequenceName = "SEQ_ID_BattleNetChannel")
    private Long id;

    @Column(nullable = false)
    private String channelName;

    @ManyToOne(fetch = FetchType.LAZY)
    @Index(name = "IDX_BattleNetChannel_Realm")
    private Realm realm;

    @Column(nullable = false, unique = true)
    private String webServiceUserId;

    @Column(nullable = false)
    private String webServicePassword;

    @Column(nullable = false)
    private Date lastUpdate;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    @OrderBy("specialType desc, username asc")
    private List<BattleNetUserPresence> users;

    /**
	 * Creates a new BattleNetChannel object
	 */
    public BattleNetChannel() {
    }

    /**
	 * Returns the ID of this BattleNetChannel
	 */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
	 * Sets the ID of this BattleNetChannel
	 */
    public void setId(Long id) {
        this.id = id;
    }

    /**
	 * @return the channelName
	 */
    public String getChannelName() {
        return channelName;
    }

    /**
	 * @param channelName
	 *            the channelName to set
	 */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
	 * @return the realm
	 */
    public Realm getRealm() {
        return realm;
    }

    /**
	 * @param realm
	 *            the realm to set
	 */
    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    /**
	 * @return the webServiceUserId
	 */
    public String getWebServiceUserId() {
        return webServiceUserId;
    }

    /**
	 * @param webServiceUserId
	 *            the webServiceUserId to set
	 */
    public void setWebServiceUserId(String webServiceUserId) {
        this.webServiceUserId = webServiceUserId;
    }

    /**
	 * @return the webServicePassword
	 */
    public String getWebServicePassword() {
        return webServicePassword;
    }

    /**
	 * @param webServicePassword
	 *            the webServicePassword to set
	 */
    public void setWebServicePassword(String webServicePassword) {
        this.webServicePassword = webServicePassword;
    }

    /**
	 * @return the lastUpdate
	 */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
	 * @param lastUpdate
	 *            the lastUpdate to set
	 */
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
	 * @return the users
	 */
    public List<BattleNetUserPresence> getUsers() {
        return users;
    }

    /**
	 * @param users
	 *            the users to set
	 */
    public void setUsers(List<BattleNetUserPresence> users) {
        this.users = users;
    }
}
