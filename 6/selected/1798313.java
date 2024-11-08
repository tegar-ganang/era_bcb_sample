package org.mpn.contacts.importer;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.Packet;
import org.mpn.contacts.framework.db.Row;
import org.mpn.contacts.ui.Data;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * todo [!] Create javadocs for org.mpn.contacts.importer.ExportJabber here
 *
 * @author <a href="mailto:pmoukhataev@jnetx.ru">Pavel Moukhataev</a>
 * @version $Revision: 23 $
 */
public class ExportJabber {

    static final Logger log = Logger.getLogger("slee.ExportJabber");

    private static final String JABBER_SERVER = "talk.google.com";

    private static final int JABBER_PORT = 5222;

    private static final String JABBER_SERVICE = "gmail.com";

    private static final boolean JABBER_SASL_AUTHENTICATION_ENABLED = true;

    private static final String JABBER_CLIENT = "javatest";

    private static String ICQ_JABBER_GATEWAY = "icq2.mo.pp.ru";

    private static final Map<Long, String> GROUPS = new HashMap<Long, String>();

    static {
        for (Row row : Data.personGroupTable) {
            Long groupId = row.getData(Data.personGroupTable.id);
            String groupName = row.getData(Data.personGroupName);
            GROUPS.put(groupId, groupName.replace('/', '\\'));
        }
    }

    private XMPPConnection conn;

    private Roster roster;

    private String login;

    private String password;

    private PacketListener packetListener = new PacketListener() {

        public void processPacket(Packet packet) {
            if (packet instanceof Presence) {
                Presence presence = (Presence) packet;
                if (presence.getType() == Presence.Type.subscribe) {
                    Presence response = new Presence(Presence.Type.subscribed);
                    response.setTo(presence.getFrom());
                    conn.sendPacket(response);
                    log.info("            Subscribed for " + presence.getFrom());
                } else if (presence.getType() == Presence.Type.unsubscribe) {
                    Presence response = new Presence(Presence.Type.unsubscribed);
                    response.setFrom(conn.getUser());
                    response.setTo(presence.getFrom());
                    conn.sendPacket(response);
                    log.info("            UnSubscribed for " + presence.getFrom());
                } else {
                    log.trace("            Presence [" + presence.getFrom() + "] : " + presence.toXML());
                }
            }
        }
    };

    public void doExport(String login, String password, String jabberIcqGateway) throws Exception {
        ICQ_JABBER_GATEWAY = jabberIcqGateway;
        this.login = login;
        this.password = password;
        ConnectionConfiguration config = new ConnectionConfiguration(JABBER_SERVER, JABBER_PORT, JABBER_SERVICE);
        config.setSASLAuthenticationEnabled(JABBER_SASL_AUTHENTICATION_ENABLED);
        conn = new XMPPConnection(config);
        checkConnection();
        for (RosterGroup rosterGroup : roster.getGroups()) {
            if (rosterGroup.getName().indexOf('/') > 0) {
                rosterGroup.setName(rosterGroup.getName().replace('/', '\\'));
            }
        }
        for (Row personRow : Data.personTable) {
            exportPerson(personRow);
        }
        conn.disconnect();
    }

    private void exportPerson(Row personRow) throws XMPPException {
        Long personId = personRow.getData(Data.personTable.id);
        StringBuilder name = new StringBuilder();
        appendString(name, " ", personRow.getData(Data.personFirstName));
        appendString(name, " ", personRow.getData(Data.personMiddleName));
        appendString(name, " ", personRow.getData(Data.personLastName));
        if (name.length() == 0) return;
        Set<String> userGroups = new HashSet<String>();
        for (Row personGroupsTableRow : Data.personGroupsTable) {
            Long groupPersonId = personGroupsTableRow.getData(Data.personTable.id);
            if (personId.equals(groupPersonId)) {
                Long groupId = personGroupsTableRow.getData(Data.personGroupTable.id);
                userGroups.add(GROUPS.get(groupId));
            }
        }
        if (userGroups.isEmpty()) {
            log.error("No groups for user : " + name);
        }
        log.info(name);
        for (Row personMessagingRow : Data.personMessagingTable) {
            if (!personMessagingRow.getData(Data.personTable.id).equals(personId)) continue;
            String type = personMessagingRow.getData(Data.personMessagingType);
            if (type.equals(Data.IM_TYPE_ICQ)) {
                String id = personMessagingRow.getData(Data.personMessagingId);
                String userJid = id + '@' + ICQ_JABBER_GATEWAY;
                checkConnection();
                RosterEntry icqUser = roster.getEntry(userJid);
                if (icqUser == null) {
                    try {
                        roster.createEntry(userJid, name.toString(), userGroups.toArray(new String[userGroups.size()]));
                    } catch (XMPPException e) {
                        log.error("Exception occured : " + e);
                        checkConnection();
                    }
                    icqUser = roster.getEntry(userJid);
                    if (icqUser == null) return;
                } else {
                    if (icqUser.getName() == null || !icqUser.getName().startsWith(name.toString())) {
                        icqUser.setName(name.toString());
                    }
                    for (String userGroup : userGroups) {
                        RosterGroup group = roster.getGroup(userGroup);
                        if (group == null) {
                            group = roster.createGroup(userGroup);
                        }
                        if (!group.contains(icqUser)) {
                            try {
                                group.addEntry(icqUser);
                            } catch (Exception e) {
                                log.error("Exception occured : " + e);
                                checkConnection();
                            }
                        }
                    }
                }
                if (icqUser.getType() != RosterPacket.ItemType.both) {
                    RosterPacket.ItemStatus status = icqUser.getStatus();
                    log.trace("     User status : " + status);
                    {
                        Presence presencePacket = new Presence(Presence.Type.subscribe);
                        presencePacket.setTo(userJid);
                        presencePacket.setFrom(conn.getUser());
                        conn.sendPacket(presencePacket);
                    }
                    {
                        Presence presencePacket = new Presence(Presence.Type.subscribed);
                        presencePacket.setTo(userJid);
                        presencePacket.setFrom(conn.getUser());
                        conn.sendPacket(presencePacket);
                    }
                    {
                        Presence presencePacket = new Presence(Presence.Type.available);
                        presencePacket.setTo(userJid);
                        presencePacket.setFrom(conn.getUser());
                        conn.sendPacket(presencePacket);
                    }
                }
            }
        }
    }

    private void checkConnection() throws XMPPException {
        if (!conn.isConnected()) {
            conn.connect();
            conn.addPacketListener(packetListener, null);
        }
        if (!conn.isAuthenticated()) {
            conn.login(login + "@gmail.com", password, JABBER_CLIENT);
            roster = conn.getRoster();
            roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
            {
                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus("Hello! I am here! Exporting ICQ Users...");
                conn.sendPacket(presence);
            }
        }
    }

    static void appendString(StringBuilder value, String delimiter, String... values) {
        appendString(value, delimiter, Arrays.asList(values));
    }

    static void appendString(StringBuilder value, String delimiter, Collection<String> values) {
        for (String s : values) {
            if (s != null) {
                s = s.trim();
                if (s.length() > 0) {
                    if (value.length() > 0) {
                        value.append(delimiter);
                    }
                    value.append(s);
                }
            }
        }
    }
}
