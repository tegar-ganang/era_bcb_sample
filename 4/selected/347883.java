package za.co.me23.dbServices;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.ContentConnection;
import javax.microedition.io.HttpConnection;
import za.co.me23.alerts.Alerts;
import za.co.me23.canvas.Me23Canvas;
import za.co.me23.canvas.ProcessingCanvas;
import za.co.me23.chat.ContactItem;
import za.co.me23.chat.Group;
import za.co.me23.chat.GroupScreen;
import za.co.me23.chat.SendableData;
import za.co.me23.chat.ServiceContact;
import za.co.me23.chat.SettingStore;
import za.co.me23.chat.Time;
import za.co.me23.chat.Utility;
import za.co.me23.methods.SignOutMethode;
import za.co.me23.midlet.ME23;

/**
 *
 * @author Refais
 */
public class DatabaseServices {

    public static ReturnString sendData(SendableData iSendData) {
        InputStream is = null;
        StringBuffer sb = null;
        HttpConnection http = null;
        String URL = SERVER_URL + "sf.php?a=" + iSendData.mobileNumber + "&b=i";
        OutputStream os = null;
        try {
            URL = encodeURL(URL);
            http = (HttpConnection) (Connector.open(URL));
            http.setRequestMethod(HttpConnection.POST);
            http.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.0");
            http.setRequestProperty("Content-Language", "en-US");
            os = http.openDataOutputStream();
            os.write(iSendData.readData);
            if (http.getResponseCode() == HttpConnection.HTTP_OK) {
                sb = new StringBuffer();
                int ch;
                is = http.openInputStream();
                while ((ch = is.read()) != -1) sb.append((char) ch);
            } else {
                return new ReturnString(false, null);
            }
        } catch (IOException e) {
            return new ReturnString(false, null);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (sb != null) {
                return new ReturnString(true, sb.toString());
            }
            if (http != null) try {
                http.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return new ReturnString(false, null);
    }

    public static ReturnString connectionGetter(String stringURL) {
        ReturnString rs = new ReturnString(false, null);
        InputStream is = null;
        StringBuffer sb = null;
        HttpConnection http = null;
        String URL = SERVER_URL + stringURL;
        try {
            URL = encodeURL(URL);
            sendBytes += URL.length();
            http = (HttpConnection) (Connector.open(URL));
            http.setRequestMethod(HttpConnection.GET);
            if (http.getResponseCode() == HttpConnection.HTTP_OK) {
                sb = new StringBuffer();
                int ch;
                is = http.openInputStream();
                while ((ch = is.read()) != -1) {
                    sb.append((char) ch);
                    recievedBytes++;
                }
            } else {
                rs = new ReturnString(false, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            rs = new ReturnString(false, null);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (sb != null) {
                rs = new ReturnString(true, sb.toString());
            }
            if (http != null) try {
                http.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.gc();
        return rs;
    }

    public static DatabaseReturn connectionSetter(String stringURL) {
        DatabaseReturn dbr = new DatabaseReturn(false, DatabaseReturn.CODE_ME23_ERROR);
        InputStream is = null;
        StringBuffer sb = null;
        HttpConnection http = null;
        String URL = SERVER_URL + stringURL;
        System.out.println(URL);
        try {
            URL = encodeURL(URL);
            sendBytes += URL.length();
            http = (HttpConnection) (Connector.open(URL));
            http.setRequestMethod(HttpConnection.GET);
            if (http.getResponseCode() == HttpConnection.HTTP_OK) {
                sb = new StringBuffer();
                int ch;
                is = http.openInputStream();
                while ((ch = is.read()) != -1) {
                    sb.append((char) ch);
                    recievedBytes++;
                }
            } else {
                dbr = new DatabaseReturn(false, DatabaseReturn.CODE_CONNECT_ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
            dbr = new DatabaseReturn(false, DatabaseReturn.CODE_CONNECT_ERROR);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (sb != null) {
                dbr = new DatabaseReturn(true, Integer.parseInt(sb.toString()));
            }
            if (http != null) try {
                http.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return dbr;
    }

    private static ReturnString phpGetContacts(String iGroup) {
        return connectionGetter("gc.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + encodeURL(iGroup));
    }

    private static ReturnString phpGetServiceContacts() {
        return connectionGetter("gsc.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetSelected(int vid, int id) {
        return connectionGetter("gsi.php?a=" + vid + "&b=" + id);
    }

    private static ReturnString phpGetDeleteContact() {
        return connectionGetter("gdc.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetGroup() {
        return connectionGetter("gg.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetServiceMessage(String iChat) {
        return connectionGetter("gsm.php?a=" + encodeURL(iChat));
    }

    private static ReturnString phpGetVillageNames(int iId, int iLevel) {
        return connectionGetter("gvi.php?a=" + iId + "&b=" + iLevel);
    }

    private static ReturnString phpGetCharacterIds(int iId) {
        return connectionGetter("gid.php?a=" + iId);
    }

    private static ReturnString phpGetRejectedInvites() {
        return connectionGetter("gri.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetVillage(int vid, int cid) {
        return connectionGetter("vr.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + vid + "&c=" + cid);
    }

    private static ReturnString phpGetInvite() {
        return connectionGetter("gi.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetMessage() {
        return connectionGetter("gm.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetGameMessage() {
        return connectionGetter("ggm.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetUserSettings() {
        return connectionGetter("gs.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + SettingStore.getSettingStore().pin);
    }

    private static ReturnString phpGetUpdate() {
        return connectionGetter("gu.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetGameUpdate() {
        return connectionGetter("ggu.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    public static ReturnString phpGetCharacterMove(int vid, int cid) {
        return connectionGetter("gcm.php?a=" + vid + "&b=" + cid);
    }

    private static ReturnString phpGetStatusUpdate() {
        return connectionGetter("gsu.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetOnlineUpdate() {
        return connectionGetter("gou.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static ReturnString phpGetFontUpdate() {
        return connectionGetter("gfu.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static DatabaseReturn phpAddGroup(String iGroup) {
        return connectionSetter("ag.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + encodeURL(iGroup));
    }

    private static DatabaseReturn phpDeleteContact() {
        return connectionSetter("dc.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + GroupScreen.getGroupScreen().getCurrentContact().mobileNumber);
    }

    private static DatabaseReturn phpEditContact(String iCaption, String iFromGroup, String iToGroup) {
        return connectionSetter("uec.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + Utility.EncodeSendString(iFromGroup) + "&c=" + Utility.EncodeSendString(iToGroup) + "&d=" + Utility.EncodeSendString(GroupScreen.getGroupScreen().getCurrentContact().caption) + "&e=" + Utility.EncodeSendString(iCaption));
    }

    private static DatabaseReturn phpRemoveGroup(String iGroup) {
        return connectionSetter("udg.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + encodeURL(iGroup));
    }

    private static DatabaseReturn phpEditGroup(String fromGroup, String toCaption) {
        return connectionSetter("ueg.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + encodeURL(fromGroup) + "&c=" + encodeURL(toCaption));
    }

    private static DatabaseReturn phpSendInvite(String iInviteNumber, String iInviteName, String iInviteMessage) {
        return connectionSetter("si.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + encodeURL(SettingStore.getSettingStore().yourName) + "&c=" + iInviteNumber + "&d=" + encodeURL(iInviteName) + "&e=" + encodeURL(iInviteMessage) + "&f=" + new Time().toNumbers() + "&g=" + SettingStore.getSettingStore().hideMobileNumber);
    }

    public static DatabaseReturn phpUpdateFont() {
        return connectionSetter("sfc.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + SettingStore.getSettingStore().fcolor);
    }

    private static DatabaseReturn phpSendMessage(String iContactNumber, String iMessage, boolean iChat) {
        if (iChat) return connectionSetter("sm.php?a=" + new Time().toNumbers() + "&b=" + SettingStore.getSettingStore().mobileNumber + "&c=" + iContactNumber + "&d=" + encodeURL(iMessage)); else return connectionSetter("sgm.php?a=" + new Time().toNumbers() + "&b=" + SettingStore.getSettingStore().mobileNumber + "&c=" + iContactNumber + "&d=" + encodeURL(iMessage));
    }

    private static DatabaseReturn phpSendSettings() {
        return connectionSetter("ss.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + SettingStore.getSettingStore().pin + "&c=" + encodeURL(SettingStore.getSettingStore().yourName) + "&d=" + SettingStore.getSettingStore().gender + "&e=" + SettingStore.getSettingStore().hideMobileNumber + "&f=" + SettingStore.getSettingStore().yearOfBirth + "&g=" + encodeURL(SettingStore.getSettingStore().statusMessage) + "&h=" + SettingStore.getSettingStore().fcolor);
    }

    public static DatabaseReturn phpMoveCharacter(int iVid, int iCid, String iMove) {
        return connectionSetter("mc.php?a=" + iVid + "&b=" + iCid + "&c=" + iMove);
    }

    public static DatabaseReturn phpRegister() {
        return connectionSetter("ru.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + SettingStore.getSettingStore().pin + "&c=" + encodeURL(SettingStore.getSettingStore().yourName) + "&d=" + SettingStore.getSettingStore().gender + "&e=" + SettingStore.getSettingStore().hideMobileNumber + "&f=" + SettingStore.getSettingStore().yearOfBirth + "&g=" + (SettingStore.getSettingStore().statusMessage) + "&h=" + (new Time()).toNumbers());
    }

    private static DatabaseReturn phpRejectInvite(String iInviteNumber) {
        return connectionSetter("ri.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + iInviteNumber);
    }

    private static DatabaseReturn phpAcceptInvite(String iInviteNumber, String iInviteName, String iGroup) {
        return connectionSetter("ai.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + iInviteNumber + "&c=" + encodeURL(iInviteName) + "&d=" + encodeURL(iGroup) + "&e=" + SettingStore.getSettingStore().hideInfo + "&f=" + encodeURL(SettingStore.getSettingStore().statusMessage) + "&g=" + SettingStore.getSettingStore().status);
    }

    private static DatabaseReturn phpVerifyNumberOnDatabase(String iMobileNumber) {
        return connectionSetter("vu.php?a=" + iMobileNumber);
    }

    private static DatabaseReturn phpTestUrl() {
        return connectionSetter("t.php");
    }

    private static DatabaseReturn phpLogin() {
        return connectionSetter("l.php?a=" + SettingStore.getSettingStore().mobileNumber + "&b=" + SettingStore.getSettingStore().pin);
    }

    private static DatabaseReturn phpLogout() {
        return connectionSetter("lo.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    private static DatabaseReturn phpLogoutGame() {
        return connectionSetter("log.php?a=" + SettingStore.getSettingStore().mobileNumber);
    }

    public static boolean sendSettings() {
        DatabaseReturn c = phpSendSettings();
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean sendMessage(String iContactNumber, String iMessage, boolean iChat) {
        DatabaseReturn c = phpSendMessage(iContactNumber, iMessage, iChat);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean rejectInvite(String iInviteNumber) {
        DatabaseReturn c = phpRejectInvite(iInviteNumber);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean editContact(String iCaption, String iFromGroup, String iToGroup) {
        DatabaseReturn c = phpEditContact(iCaption, iFromGroup, iToGroup);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean editGroup(String iFromGroup, String iToGroup) {
        DatabaseReturn c = phpEditGroup(iFromGroup, iToGroup);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean addGroup(String iGroup) {
        DatabaseReturn c = phpAddGroup(iGroup);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean deleteGroup(String iGroup) {
        DatabaseReturn c = phpRemoveGroup(iGroup);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean deleteContact() {
        DatabaseReturn c = phpDeleteContact();
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean acceptInvite(String iInviteNumber, String iInviteName, String iGroup) {
        DatabaseReturn c = phpAcceptInvite(iInviteNumber, iInviteName, iGroup);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static boolean sendInvite(String iInviteNumber, String iInviteName, String iInviteMessage) {
        DatabaseReturn c = phpSendInvite(iInviteNumber, iInviteName, iInviteMessage);
        return (c.getExecuted() && (c.getCode()) == 1);
    }

    public static DatabaseReturn verifyNumberOnDatabase(String iMobileNumber) {
        return phpVerifyNumberOnDatabase(iMobileNumber);
    }

    public static boolean signOutGame() {
        DatabaseReturn c = phpLogoutGame();
        return (c.getExecuted()) && (c.getCode() == 1);
    }

    public static boolean testDb() {
        DatabaseReturn c = phpTestUrl();
        return (c.getExecuted()) && (c.getCode() == 1);
    }

    public static void signOut() {
        phpLogout();
    }

    public static DatabaseReturn login() {
        return phpLogin();
    }

    public static String getUpdate() {
        ReturnString s = phpGetUpdate();
        if (s.getExecuted()) return s.getString();
        return "";
    }

    public static String getGameUpdate() {
        ReturnString s = phpGetGameUpdate();
        if (s.getExecuted()) return s.getString();
        return "";
    }

    public static String[] getVillageNames(int iId, int iLevel) {
        ReturnString s = phpGetVillageNames(iId, iLevel);
        if (s.getExecuted() && (s.getString().length() > 0)) return split(s.getString());
        return null;
    }

    public static String[] getSelected(int vid, int id) {
        ReturnString s = phpGetSelected(vid, id);
        if (s.getExecuted() && (s.getString().length() > 0)) return split(s.getString());
        return new String[0];
    }

    public static String[] getCharacterIds(int iId) {
        ReturnString s = phpGetCharacterIds(iId);
        if (s.getExecuted() && (s.getString().length() >= 0)) return split(s.getString());
        return null;
    }

    public static String getServiceMessage(String iChat) {
        ReturnString s = phpGetServiceMessage(iChat);
        if (s.getExecuted()) return s.getString();
        return null;
    }

    public static String[] getRejectedInvites() {
        ReturnString s = phpGetRejectedInvites();
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static String[] getVillage(int vid, int cid) {
        ReturnString s = phpGetVillage(vid, cid);
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static String[] getUserSettingsFromDb() {
        ReturnString s = phpGetUserSettings();
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static String[] getGroupsFromDb() {
        ReturnString s = phpGetGroup();
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static String[] getContactsFromDb(String iGroup) {
        ReturnString s = phpGetContacts(iGroup);
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static String[] getServiceContactsFromDb() {
        ReturnString s = phpGetServiceContacts();
        if (s.getExecuted()) return split(s.getString());
        return null;
    }

    public static void addContacts() {
        String[] groups = getGroupsFromDb();
        if ((groups != null) && (groups.length % 2 == 0)) {
            for (int i = 0; i < groups.length / 2; i++) {
                GroupScreen.getGroupScreen().addGroup(groups[0 + (i * 2)], groups[1 + (i * 2)].equals("1"));
                String[] contacts = getContactsFromDb(groups[0 + (i * 2)]);
                if (contacts != null) {
                    if (contacts.length % 6 == 0) for (int j = 0; j < contacts.length / 6; j++) {
                        GroupScreen.getGroupScreen().addContactToGroup(contacts[1 + (j * 6)], contacts[2 + (j * 6)], groups[0 + (i * 2)], contacts[0 + (j * 6)], contacts[4 + (j * 6)].equals("1"), contacts[3 + (j * 6)].equals("1"), Integer.parseInt(contacts[5 + (j * 6)]));
                    } else ME23.getME23().switchDisplayable(Alerts.getDatabaseAlert(), ProcessingCanvas.startProcessWithOutSwitching(new SignOutMethode()));
                }
            }
            String[] serviceContacts = getServiceContactsFromDb();
            if ((serviceContacts != null) && (serviceContacts.length % 3 == 0)) for (int j = 0; j < serviceContacts.length / 3; j++) GroupScreen.getGroupScreen().addServiceContactToGroup(serviceContacts[0 + (j * 3)].charAt(0), serviceContacts[1 + (j * 3)], serviceContacts[2 + (j * 3)]); else ME23.getME23().switchDisplayable(Alerts.getDatabaseAlert(), ProcessingCanvas.startProcessWithOutSwitching(new SignOutMethode()));
        } else {
            ME23.getME23().switchDisplayable(Alerts.getDatabaseAlert(), ProcessingCanvas.startProcessWithOutSwitching(new SignOutMethode()));
        }
    }

    public static void getNewContacts() {
        String[] groups = getGroupsFromDb();
        if ((groups != null) && (groups.length % 2 == 0)) {
            for (int i = 0; i < groups.length / 2; i++) {
                String[] contacts = getContactsFromDb(groups[0 + (i * 2)]);
                if (contacts != null) {
                    if (contacts.length % 6 == 0) for (int j = 0; j < contacts.length / 6; j++) if (!verifyContactMobileNumber(contacts[0 + (j * 5)])) GroupScreen.getGroupScreen().addContactToGroup(contacts[1 + (j * 5)], contacts[2 + (j * 5)], groups[0 + (i * 2)], contacts[0 + (j * 5)], contacts[4 + (j * 5)].equals("1"), contacts[3 + (j * 5)].equals("1"), Integer.parseInt(contacts[2 + (j * 5)]));
                }
            }
            String[] serviceContacts = getServiceContactsFromDb();
            if ((serviceContacts != null) && (serviceContacts.length % 3 == 0)) for (int j = 0; j < serviceContacts.length / 3; j++) if (!verifyServiceContact(serviceContacts[0 + (j * 3)].charAt(0))) GroupScreen.getGroupScreen().addServiceContactToGroup(serviceContacts[0 + (j * 3)].charAt(0), serviceContacts[1 + (j * 3)], serviceContacts[2 + (j * 3)]);
        } else Me23Canvas.switchTo(Alerts.getDatabaseAlert());
    }

    public static boolean verifyContactMobileNumber(String iMobileNumber) {
        if (iMobileNumber.equals(SettingStore.getSettingStore().mobileNumber)) return false; else for (int i = 0; i < GroupScreen.getGroupScreen().groups.size(); i++) if (!GroupScreen.getGroupScreen().getGroup(i).realCaption.equals("Invites")) for (int j = 0; j < GroupScreen.getGroupScreen().getGroup(i).advanceItems.size(); j++) if (GroupScreen.getGroupScreen().getGroup(i).get(j).getProperty() != 's') if (((ContactItem) GroupScreen.getGroupScreen().getGroup(i).get(j)).mobileNumber.equals(iMobileNumber)) return true;
        return false;
    }

    public static boolean verifyServiceContact(char iChar) {
        Group group;
        if ((group = GroupScreen.getGroupScreen().getGroup("Me23 Contacts")) != null) {
            for (int i = 0; i < group.advanceItems.size(); i++) {
                if (group.get(i).getProperty() == 's') {
                    if (((ServiceContact) group.get(i)).character == iChar) return true;
                }
            }
        }
        return false;
    }

    public static void addInvites() {
        ReturnString s = phpGetInvite();
        if (s.getExecuted()) {
            String invitesString = s.getString();
            if (invitesString != null) {
                if (!GroupScreen.getGroupScreen().groupCaptionExists("Invites")) GroupScreen.getGroupScreen().addGroup("Invites", false);
                String[] invites = split(invitesString);
                if (invites.length % 5 == 0) {
                    for (int j = 0; j < invites.length / 5; j++) {
                        if (!verifyContactMobileNumber(invites[0 + (j * 5)])) GroupScreen.getGroupScreen().addInviteToGroup(invites[1 + (j * 5)], invites[2 + (j * 5)], invites[0 + (j * 5)], invites[3 + (j * 5)], invites[4 + (j * 5)].equals("1")); else phpRejectInvite(invites[0 + (j * 5)]);
                    }
                }
                if (GroupScreen.getGroupScreen().getGroup("Invites").advanceItems.size() == 0) GroupScreen.getGroupScreen().removeGroup("Invites");
            }
        } else Me23Canvas.switchTo(Alerts.getDatabaseAlert());
    }

    public static String[] messageRetrieverFromDb() {
        ReturnString s = phpGetMessage();
        if (s.getExecuted()) return split(s.getString()); else return null;
    }

    public static String[] gameMessageRetrieverFromDb() {
        ReturnString s = phpGetGameMessage();
        if (s.getExecuted()) return split(s.getString()); else return null;
    }

    public static boolean updateContactStatus() {
        ReturnString s = phpGetStatusUpdate();
        if (s.getExecuted()) {
            String[] status = split(s.getString());
            if (status.length % 2 == 0) for (int i = 0; i < status.length / 2; i++) GroupScreen.getGroupScreen().getContactByMobileNumber(status[0 + (i * 2)]).updateStatus(status[1 + (i * 2)]);
            return true;
        }
        return false;
    }

    public static boolean updateContactOnlineStatus() {
        ReturnString s = phpGetOnlineUpdate();
        if (s.getExecuted()) {
            String[] status = split(s.getString());
            if (status.length % 2 == 0) for (int i = 0; i < status.length / 2; i++) GroupScreen.getGroupScreen().getContactByMobileNumber(status[0 + (i * 2)]).onlineStatus = status[1 + (i * 2)].equals("1");
            return true;
        }
        return false;
    }

    public static boolean updateContactFont() {
        ReturnString s = phpGetFontUpdate();
        if (s.getExecuted()) {
            String[] font = split(s.getString());
            if (font.length % 2 == 0) for (int i = 0; i < font.length / 2; i++) GroupScreen.getGroupScreen().getContactByMobileNumber(font[0 + (i * 2)]).fontColor = Integer.parseInt(font[1 + (i * 2)]);
            return true;
        }
        return false;
    }

    public static byte[] getImage(String url) throws IOException {
        sendBytes += (SERVER_URL + url).length();
        ContentConnection connection = (ContentConnection) Connector.open(SERVER_URL + url);
        DataInputStream iStrm = connection.openDataInputStream();
        ByteArrayOutputStream bStrm = null;
        try {
            byte imageData[];
            int length = (int) connection.getLength();
            if (length != -1) {
                imageData = new byte[length];
                iStrm.readFully(imageData);
            } else {
                bStrm = new ByteArrayOutputStream();
                int ch;
                while ((ch = iStrm.read()) != -1) bStrm.write(ch);
                imageData = bStrm.toByteArray();
                bStrm.close();
            }
            recievedBytes += imageData.length;
            return (imageData == null ? null : imageData);
        } finally {
            if (iStrm != null) iStrm.close();
            if (connection != null) connection.close();
            if (bStrm != null) bStrm.close();
        }
    }

    public static boolean updateDeletedContacts() {
        ReturnString s = phpGetDeleteContact();
        if (s.getExecuted()) {
            String[] numbers = split(s.getString());
            String message = "The following contact(s) were removed: ";
            for (int i = 0; i < numbers.length; i++) {
                if (i != 0) message += ", ";
                message += GroupScreen.getGroupScreen().getContactByMobileNumber(numbers[i]).caption;
                GroupScreen.getGroupScreen().removeContactItem(numbers[i]);
            }
            Me23Canvas.getMe23Canvas().getNotificationScreen("Contacts Removed", message);
            return true;
        }
        return false;
    }

    public static String[] split(String splitString) {
        if (splitString != null) {
            int p1 = 0;
            int count = DatabaseServices.countSplits(splitString, '|');
            String[] splittedArray = new String[count];
            for (int i = 0; i < count; i++) {
                int p2 = splitString.indexOf("|", p1);
                splittedArray[i] = splitString.substring(p1, p2);
                p1 = p2 + 1;
            }
            return splittedArray;
        }
        return new String[0];
    }

    public static int countSplits(String countSplitString, char splitter) {
        int p1 = 0;
        int p2 = 0;
        int count = 0;
        while ((p2 = countSplitString.indexOf(splitter, p1)) != -1) {
            count++;
            p1 = p2 + 1;
        }
        return count;
    }

    public static String encodeURL(String URL) {
        URL = replace(URL, '$', "%24");
        URL = replace(URL, '#', "%23");
        URL = replace(URL, '£', "%A3");
        URL = replace(URL, '@', "%40");
        URL = replace(URL, '\'', "%27");
        URL = replace(URL, ' ', "%20");
        return URL;
    }

    public static String replace(String source, char oldChar, String dest) {
        String ret = "";
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) != oldChar) ret += source.charAt(i); else ret += dest;
        }
        return ret;
    }

    public static char convertDirection(int direction) {
        if (direction == 0) return 'N'; else if (direction == 1) return 'U'; else if (direction == 2) return 'D'; else if (direction == 3) return 'L'; else if (direction == 4) return 'R';
        return 'N';
    }

    private static final String SERVER_URL = "http://me23.frostnet.co.za/";

    public static int sendBytes = 0, recievedBytes = 0;
}
