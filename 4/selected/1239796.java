package com.tysanclan.site.projectewok.entities;

import java.util.LinkedList;
import java.util.List;
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
public class Realm implements DomainObject {

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Realm")
    @SequenceGenerator(name = "Realm", sequenceName = "SEQ_ID_Realm")
    private Long id;

    @Column
    private String name;

    @Column
    private String channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @Index(name = "IDX_REALM_OVERSEER")
    private User overseer;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "REALM_GAME", joinColumns = @JoinColumn(name = "realm_id"), inverseJoinColumns = @JoinColumn(name = "game_id"))
    @OrderBy("name")
    private List<Game> games;

    @OneToMany(mappedBy = "realm")
    @OrderBy("name")
    private List<GamingGroup> groups;

    @OneToMany(mappedBy = "realm")
    @OrderBy("channelName")
    private List<BattleNetChannel> channels;

    /**
	 * Creates a new Realm object
	 */
    public Realm() {
        this.games = new LinkedList<Game>();
        this.groups = new LinkedList<GamingGroup>();
    }

    /**
	 * Returns the ID of this Realm
	 */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
	 * Sets the ID of this Realm
	 */
    public void setId(Long id) {
        this.id = id;
    }

    /**
	 * @return The Name of this Realm
	 */
    public String getName() {
        return this.name;
    }

    /**
	 * Sets the Name of this Realm
	 * 
	 * @param name
	 *            The Name of this Realm
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * @return The Channel of this Realm
	 */
    public String getChannel() {
        return this.channel;
    }

    /**
	 * Sets the Channel of this Realm
	 * 
	 * @param channel
	 *            The Channel of this Realm
	 */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
	 * @return The Overseer of this Realm
	 */
    public User getOverseer() {
        return this.overseer;
    }

    /**
	 * Sets the Overseer of this Realm
	 * 
	 * @param overseer
	 *            The Overseer of this Realm
	 */
    public void setOverseer(User overseer) {
        this.overseer = overseer;
    }

    /**
	 * @return The Games of this Realm
	 */
    public List<Game> getGames() {
        return this.games;
    }

    /**
	 * Sets the Games of this Realm
	 * 
	 * @param games
	 *            The Games of this Realm
	 */
    public void setGames(List<Game> games) {
        this.games = games;
    }

    /**
	 * @return the groups
	 */
    public List<GamingGroup> getGroups() {
        return groups;
    }

    /**
	 * @param groups
	 *            the groups to set
	 */
    public void setGroups(List<GamingGroup> groups) {
        this.groups = groups;
    }

    /**
	 * @return the channels
	 */
    public List<BattleNetChannel> getChannels() {
        return channels;
    }

    /**
	 * @param channels
	 *            the channels to set
	 */
    public void setChannels(List<BattleNetChannel> channels) {
        this.channels = channels;
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    @Override
    public String toString() {
        return getName();
    }
}
