package org.activision.model.player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.activision.model.Animation;
import org.activision.model.CombatDefinitions;
import org.activision.model.Entity;
import org.activision.model.Graphics;
import org.activision.model.Heal;
import org.activision.model.Hits;
import org.activision.model.World;
import org.activision.model.Hits.Hit;
import org.activision.model.Hits.HitType;
import org.activision.net.Frames;
import org.activision.net.codec.ConnectionHandler;
import org.activision.content.skills.construction.Construction;
import org.activision.content.combat.Prayer;
import org.activision.util.Misc;
import org.activision.util.RSTile;

public class Player extends Entity implements Serializable {

    private static final long serialVersionUID = -393308022192269041L;

    private String Username;

    private String DisplayName;

    private String Password;

    private Calendar BirthDate;

    private Calendar RegistDate;

    private short Country;

    private String Email;

    private byte Settings;

    private boolean isMuted;

    private boolean isBanned;

    private Date Membership;

    private List<String> friends;

    private transient List<String> ignores;

    private List<String> Messages;

    private int LastIp;

    private byte rights;

    private Appearence appearence;

    private Inventory inventory;

    private Equipment equipment;

    private Skills skills;

    private Banking bank;

    private CombatDefinitions combatdefinitions;

    private Construction construction;

    private Prayer prayer;

    private MusicManager musicmanager;

    private transient ConnectionHandler connection;

    private transient Frames frames;

    private transient Mask mask;

    private transient Gpi gpi;

    private transient Gni gni;

    private transient Queue<Hit> queuedHits;

    private transient Hits hits;

    private transient InterfaceManager intermanager;

    private transient HintIconManager hinticonmanager;

    private transient MinigameManager Minigamemanager;

    private transient Dialogue dialogue;

    private transient boolean isOnline;

    private transient boolean inClient;

    public int autocast = 0;

    public Player(String Username, String Password, Calendar Birth, Calendar ThisDate, short Country, String Email, byte Settings) {
        this.setUsername(Username);
        this.setDisplayName(Username);
        this.setPassword(Password);
        this.setBirthDate(BirthDate);
        this.setRegistDate(ThisDate);
        this.setCountry(Country);
        this.setEmail(Email);
        this.setSettings(Settings);
        this.setMuted(false);
        this.setBanned(false);
        this.setMembership(new Date());
        this.setFriends(new ArrayList<String>(200));
        this.setMessages(new ArrayList<String>());
        this.setLocation(RSTile.createRSTile(3222, 3222, (byte) 0));
        this.setAppearence(new Appearence());
        this.setInventory(new Inventory());
        this.setEquipment(new Equipment());
        this.setSkills(new Skills());
        this.setCombatDefinitions(new CombatDefinitions());
        this.setConstruction(new Construction());
        this.setPrayer(new Prayer());
        this.setBank(new Banking());
        this.setMusicmanager(new MusicManager());
    }

    public void LoadPlayer(ConnectionHandler connection) {
        this.setConnection(connection);
        this.setFrames(new Frames(this));
        this.setMask(new Mask(this));
        this.setGpi(new Gpi(this));
        this.setGni(new Gni(this));
        this.setQueuedHits(new LinkedList<Hit>());
        this.setHits(new Hits());
        this.setIntermanager(new InterfaceManager(this));
        this.setHinticonmanager(new HintIconManager(this));
        this.setMinigamemanager(new MinigameManager(this));
        this.setDialogue(new Dialogue(this));
        if (this.appearence == null) this.appearence = new Appearence();
        if (this.inventory == null) this.inventory = new Inventory();
        this.getInventory().setPlayer(this);
        if (this.equipment == null) this.equipment = new Equipment();
        this.getEquipment().setPlayer(this);
        if (this.skills == null) this.skills = new Skills();
        this.getSkills().setPlayer(this);
        if (this.combatdefinitions == null) this.combatdefinitions = new CombatDefinitions();
        this.getCombatDefinitions().setPlayer(this);
        if (this.construction == null) this.construction = new Construction();
        this.getConstruction().setPlayer(this);
        if (this.prayer == null) this.prayer = new Prayer();
        this.getPrayer().setPlayer(this);
        if (this.musicmanager == null) this.musicmanager = new MusicManager();
        this.getMusicmanager().setPlayer(this);
        if (this.bank == null) this.bank = new Banking();
        this.getBank().setPlayer(this);
        this.setIgnores(new ArrayList<String>(100));
        this.EntityLoad();
        this.getFrames().loginResponce();
        this.getFrames().sendLoginInterfaces();
        this.getFrames().sendLoginConfigurations();
        this.getFrames().sendOtherLoginPackets();
        this.LoadFriend_Ignore_Lists();
        this.reset();
        this.setOnline(true);
        for (Player p2 : World.getPlayers()) {
            if (p2 == null || p2 == this) continue;
            p2.getGpi().addPlayer(this);
        }
        this.getCombatDefinitions().startHealing();
        this.getSkills().startBoostingSkill();
        this.getCombatDefinitions().startGettingSpecialUp();
        if (this.isDead()) this.getSkills().sendDead();
    }

    private void reset() {
        if (this.getConnection().getChannel() == null) return;
        if (this.LastIp == 0) {
            this.getFrames().sendChatMessage(0, "Theres many places, for find. You just need to search them!");
        }
        String ip = "" + this.getConnection().getChannel().getLocalAddress();
        ip = ip.replaceAll("/", "");
        ip = ip.replaceAll(" ", "");
        ip = ip.substring(0, ip.indexOf(":"));
        this.setLastIp(Misc.IPAddressToNumber(ip));
        if (World.getIps().containsKey(this.LastIp)) World.getIps().remove(this.LastIp);
        World.getIps().put(this.LastIp, System.currentTimeMillis());
    }

    private void LoadFriend_Ignore_Lists() {
        this.getFrames().sendUnlockIgnoreList();
        this.getFrames().sendUnlockFriendList();
        LoadIgnoreList();
        LoadFriendList();
    }

    private void LoadFriendList() {
        for (String Friend : getFriends()) {
            short WorldId = (short) (World.isOnline(Misc.formatPlayerNameForProtocol(Friend)) ? 1 : 0);
            boolean isOnline = WorldId != 0;
            this.getFrames().sendFriend(Friend, Friend, WorldId, isOnline, false);
        }
    }

    private void LoadIgnoreList() {
        for (String Ignore : getIgnores()) {
            this.getFrames().sendIgnore(Ignore, Ignore);
        }
    }

    public void UpdateFriendStatus(String Friend, short worldId, boolean isOnline) {
        this.getFrames().sendFriend(Friend, Friend, worldId, isOnline, true);
    }

    public void AddFriend(String Friend) {
        if ((getMembershipCredit() == 0 && getFriends().size() >= 100) || getFriends().size() >= 200 || Friend == null || Friend.equals("") || getFriends().contains(Friend) || getIgnores().contains(Friend) || Friend.equals(Misc.formatPlayerNameForDisplay(this.getUsername()))) return;
        getFriends().add(Friend);
        short WorldId = (short) (World.isOnline(Misc.formatPlayerNameForProtocol(Friend)) ? 1 : 0);
        boolean isOnline = WorldId != 0;
        this.getFrames().sendFriend(Friend, Friend, WorldId, false, false);
        if (isOnline) UpdateFriendStatus(Friend, WorldId, isOnline);
    }

    public void AddIgnore(String Ignore) {
        if (getIgnores().size() >= 100 || Ignore == null || getFriends().contains(Ignore) || getIgnores().contains(Ignore) || getIgnores().equals(Misc.formatPlayerNameForDisplay(this.getUsername()))) return;
        getIgnores().add(Ignore);
        this.getFrames().sendIgnore(Ignore, Ignore);
    }

    public void RemoveIgnore(String Ignore) {
        if (Ignore == null || !getIgnores().contains(Ignore)) return;
        getIgnores().remove(Ignore);
    }

    public void RemoveFriend(String Friend) {
        if (Friend == null || !getFriends().contains(Friend)) return;
        getFriends().remove(Friend);
    }

    @SuppressWarnings("deprecation")
    public void MakeMember(int numberofmonths) {
        if (getMembership().before(new Date())) setMembership(new Date());
        getMembership().setMonth(getMembership().getMonth() + numberofmonths);
    }

    public int getMembershipCredit() {
        Date today = new Date();
        if (getMembership().before(today)) return 0;
        long MembershipTime = getMembership().getTime();
        long TodayTime = today.getTime();
        int DayOfFinish = (int) (MembershipTime / 1000 / 60 / 60 / 24);
        int DayOfToday = (int) (TodayTime / 1000 / 60 / 60 / 24);
        return DayOfFinish - DayOfToday;
    }

    public void setFrames(Frames frames) {
        this.frames = frames;
    }

    public Frames getFrames() {
        if (frames == null) frames = new Frames(this);
        return frames;
    }

    public void setConnection(ConnectionHandler connection) {
        this.connection = connection;
    }

    public ConnectionHandler getConnection() {
        return connection;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getUsername() {
        if (Username == null) Username = "";
        return Username;
    }

    public void setDisplayName(String displayName) {
        DisplayName = displayName;
    }

    public String getDisplayName() {
        if (DisplayName == null) DisplayName = "";
        return DisplayName;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getPassword() {
        if (Password == null) Password = "";
        return Password;
    }

    public void setBirthDate(Calendar birthDate) {
        BirthDate = birthDate;
    }

    public Calendar getBirthDate() {
        if (BirthDate == null) BirthDate = new GregorianCalendar();
        return BirthDate;
    }

    public void setCountry(short country) {
        Country = country;
    }

    public short getCountry() {
        return Country;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getEmail() {
        if (Email == null) Email = "";
        return Email;
    }

    public void setSettings(byte settings) {
        Settings = settings;
    }

    public byte getSettings() {
        return Settings;
    }

    public void setRegistDate(Calendar registDate) {
        RegistDate = registDate;
    }

    public Calendar getRegistDate() {
        if (RegistDate == null) RegistDate = new GregorianCalendar();
        return RegistDate;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setMembership(Date membership) {
        Membership = membership;
    }

    public Date getMembership() {
        if (Membership == null) Membership = new Date();
        return Membership;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public List<String> getFriends() {
        if (friends == null) friends = new ArrayList<String>(200);
        return friends;
    }

    public void setIgnores(List<String> ignores) {
        this.ignores = ignores;
    }

    public List<String> getIgnores() {
        if (ignores == null) ignores = new ArrayList<String>(100);
        return ignores;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setMessages(List<String> messages) {
        Messages = messages;
    }

    public List<String> getMessages() {
        if (Messages == null) Messages = new ArrayList<String>();
        return Messages;
    }

    public void setLastIp(int lastIp) {
        LastIp = lastIp;
    }

    public int getLastIp() {
        return LastIp;
    }

    public void setRights(byte rights) {
        this.rights = rights;
    }

    public byte getRights() {
        return rights;
    }

    @Override
    public void animate(int id) {
        this.getMask().setLastAnimation(new Animation((short) id, (short) 0));
        this.getMask().setAnimationUpdate(true);
    }

    @Override
    public void animate(int id, int delay) {
        this.getMask().setLastAnimation(new Animation((short) id, (short) delay));
        this.getMask().setAnimationUpdate(true);
    }

    @Override
    public void graphics(int id) {
        this.getMask().setLastGraphics(new Graphics((short) id, (short) 0));
        this.getMask().setGraphicUpdate(true);
    }

    public void graphics2(int id) {
        this.getMask().setLastGraphics2(new Graphics((short) id, (short) 0));
        this.getMask().setGraphic2Update(true);
    }

    public void graphics2(int id, int delay) {
        this.getMask().setLastGraphics2(new Graphics((short) id, (short) delay));
        this.getMask().setGraphic2Update(true);
    }

    @Override
    public void graphics(int id, int delay) {
        this.getMask().setLastGraphics(new Graphics((short) id, (short) delay));
        this.getMask().setGraphicUpdate(true);
    }

    @Override
    public void heal(int amount) {
    }

    public void heal(int healdelay, int bardelay, int healspeed) {
        getMask().setLastHeal(new Heal((short) healdelay, (byte) bardelay, (byte) healspeed));
        getMask().setHealUpdate(true);
    }

    public void processQueuedHits() {
        if (!this.getMask().isHitUpdate()) {
            if (queuedHits.size() > 0) {
                Hit h = queuedHits.poll();
                this.hit(h.getDamage(), h.getType());
            }
        }
        if (!this.getMask().isHit2Update()) {
            if (queuedHits.size() > 0) {
                Hit h = queuedHits.poll();
                this.hit(h.getDamage(), h.getType());
            }
        }
    }

    public void hit(int damage, Hits.HitType type) {
        if (this.skills.getHitPoints() <= 0) {
            return;
        }
        if (System.currentTimeMillis() < this.getCombatDefinitions().getLastEmote() - 600) {
            queuedHits.add(new Hit(damage, type));
        } else if (!this.getMask().isHitUpdate()) {
            this.hits.setHit1(new Hit(damage, type));
            this.getMask().setHitUpdate(true);
            this.getSkills().hit(damage);
        } else if (!this.getMask().isHit2Update()) {
            this.hits.setHit2(new Hit(damage, type));
            this.getMask().setHit2Update(true);
            this.getSkills().hit(damage);
        } else {
            queuedHits.add(new Hit(damage, type));
        }
    }

    @Override
    public void hit(int damage) {
        if (damage > this.skills.getHitPoints()) damage = this.skills.getHitPoints();
        if (damage == 0) {
            hit(damage, Hits.HitType.NO_DAMAGE);
        } else if (damage >= 100) {
            hit(damage, Hits.HitType.NORMAL_BIG_DAMAGE);
        } else {
            hit(damage, Hits.HitType.NORMAL_DAMAGE);
        }
    }

    public void hitType(int damage, HitType hitType) {
        if (damage > this.skills.getHitPoints()) damage = this.skills.getHitPoints();
        hit(damage, hitType);
    }

    @Override
    public void resetTurnTo() {
        this.mask.setTurnToIndex(-1);
        this.mask.setTurnToReset(true);
        this.mask.setTurnToUpdate(true);
    }

    @Override
    public void turnTemporarilyTo(Entity entity) {
        this.mask.setTurnToIndex(entity.getClientIndex());
        this.mask.setTurnToReset(true);
        this.mask.setTurnToUpdate(true);
    }

    public void turnTemporarilyTo(RSTile location) {
        this.mask.setTurnToLocation(location);
        this.mask.setTurnToUpdate1(true);
    }

    @Override
    public void turnTo(Entity entity) {
        this.mask.setTurnToIndex(entity.getClientIndex());
        this.mask.setTurnToReset(false);
        this.mask.setTurnToUpdate(true);
    }

    public void setMask(Mask mask) {
        this.mask = mask;
    }

    public Mask getMask() {
        return mask;
    }

    public void setAppearence(Appearence appearence) {
        this.appearence = appearence;
    }

    public Appearence getAppearence() {
        return appearence;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setSkills(Skills skills) {
        this.skills = skills;
    }

    public Skills getSkills() {
        return skills;
    }

    public void setIntermanager(InterfaceManager intermanager) {
        this.intermanager = intermanager;
    }

    public InterfaceManager getIntermanager() {
        return intermanager;
    }

    public void setInClient(boolean inClient) {
        this.inClient = inClient;
    }

    public boolean isInClient() {
        return inClient;
    }

    public void setCombatDefinitions(CombatDefinitions combat) {
        this.combatdefinitions = combat;
    }

    public CombatDefinitions getCombatDefinitions() {
        return combatdefinitions;
    }

    public void setConstruction(Construction construction) {
        this.construction = construction;
    }

    public Construction getConstruction() {
        return construction;
    }

    public void setDialogue(Dialogue dialogue) {
        this.dialogue = dialogue;
    }

    public Dialogue getDialogue() {
        return dialogue;
    }

    public void setPrayer(Prayer prayer) {
        this.prayer = prayer;
    }

    public Prayer getPrayer() {
        return prayer;
    }

    public void setQueuedHits(Queue<Hit> queuedHits) {
        this.queuedHits = queuedHits;
    }

    public Queue<Hit> getQueuedHits() {
        return queuedHits;
    }

    public void setHits(Hits hits) {
        this.hits = hits;
    }

    public Hits getHits() {
        return hits;
    }

    public void setGpi(Gpi gpi) {
        this.gpi = gpi;
    }

    public Gpi getGpi() {
        return gpi;
    }

    public void setMusicmanager(MusicManager musicmanager) {
        this.musicmanager = musicmanager;
    }

    public MusicManager getMusicmanager() {
        return musicmanager;
    }

    public void setBank(Banking bank) {
        this.bank = bank;
    }

    public Banking getBank() {
        return bank;
    }

    public void setHinticonmanager(HintIconManager hinticonmanager) {
        this.hinticonmanager = hinticonmanager;
    }

    public HintIconManager getHinticonmanager() {
        return hinticonmanager;
    }

    public void setMinigamemanager(MinigameManager minigamemanager) {
        Minigamemanager = minigamemanager;
    }

    public MinigameManager getMinigamemanager() {
        return Minigamemanager;
    }

    public void setGni(Gni gni) {
        this.gni = gni;
    }

    public Gni getGni() {
        return gni;
    }
}
