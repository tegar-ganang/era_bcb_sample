package com.tysanclan.site.projectewok.entities;

import java.util.Arrays;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Index;
import com.jeroensteenbeeke.hyperion.data.DomainObject;

/**
 * @author Jeroen Steenbeeke
 */
@Entity
@AccessType("field")
@Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.TRANSACTIONAL, region = "main")
public class BattleNetUserPresence implements DomainObject {

    public static final long serialVersionUID = 1L;

    public static final int USER_BLIZZREP = 0x01;

    public static final int USER_CHANNELOP = 0x02;

    public static final int USER_SPEAKER = 0x04;

    public static final int USER_ADMIN = 0x08;

    public static final int USER_NOUDP = 0x10;

    public static final int USER_SQUELCHED = 0x20;

    public static final int USER_GUEST = 0x40;

    private static final String[] KNOWN_CLIENTS = { "LTRD", "VD2D", "PX2D", "PXES", "RATS", "NB2W", "PX3W", "3RAW" };

    public static enum SpecialType {

        SERVER_ADMIN("bnet-battlenet.gif", USER_ADMIN), BLIZZREP("bnet-blizzard.gif", USER_BLIZZREP), OPERATOR("bnet-channelops.gif", USER_CHANNELOP), SPEAKER("bnet-speaker.gif", USER_SPEAKER), SQUELCHED("bnet-squelch.gif", USER_SQUELCHED), NONE(null, 0x0);

        private final String image;

        private final int requiredFlag;

        private SpecialType(String image, int requiredFlag) {
            this.image = image;
            this.requiredFlag = requiredFlag;
        }

        /**
		 * @return the image
		 */
        public String getImage() {
            return image;
        }

        public static SpecialType get(int flags) {
            for (SpecialType type : values()) {
                if ((type.requiredFlag & flags) == type.requiredFlag) {
                    return type;
                }
            }
            return NONE;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "BattleNetUserPresence")
    @SequenceGenerator(name = "BattleNetUserPresence", sequenceName = "SEQ_ID_BattleNetUserPresence")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @Index(name = "IDX_BattleNetUserPresence_Channel")
    private BattleNetChannel channel;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpecialType specialType;

    @Column(nullable = false)
    private String client;

    @Column(nullable = false)
    private Date lastUpdate;

    /**
	 * Creates a new BattleNetUserPresence object
	 */
    public BattleNetUserPresence() {
    }

    /**
	 * Returns the ID of this BattleNetUserPresence
	 */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
	 * Sets the ID of this BattleNetUserPresence
	 */
    public void setId(Long id) {
        this.id = id;
    }

    /**
	 * @return the channel
	 */
    public BattleNetChannel getChannel() {
        return channel;
    }

    /**
	 * @param channel
	 *            the channel to set
	 */
    public void setChannel(BattleNetChannel channel) {
        this.channel = channel;
    }

    /**
	 * @return the username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * @param username
	 *            the username to set
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
	 * @return the specialType
	 */
    public SpecialType getSpecialType() {
        return specialType;
    }

    /**
	 * @param specialType
	 *            the specialType to set
	 */
    public void setSpecialType(SpecialType specialType) {
        this.specialType = specialType;
    }

    /**
	 * @return the client
	 */
    public String getClient() {
        return client;
    }

    /**
	 * @param client
	 *            the client to set
	 */
    public void setClient(String client) {
        this.client = client;
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

    public static boolean isKnownClient(String client2) {
        return Arrays.asList(KNOWN_CLIENTS).contains(client2);
    }
}
