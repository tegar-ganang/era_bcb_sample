package net.bnubot.core.bncs;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import net.bnubot.bot.gui.ProfileEditor;
import net.bnubot.core.BNFTPConnection;
import net.bnubot.core.CommandResponseCookie;
import net.bnubot.core.Connection;
import net.bnubot.core.EventHandler;
import net.bnubot.core.PluginManager;
import net.bnubot.core.Profile;
import net.bnubot.core.UnsupportedFeatureException;
import net.bnubot.core.bnls.BNLSManager;
import net.bnubot.core.bnls.VersionCheckResult;
import net.bnubot.core.botnet.BotNetConnection;
import net.bnubot.core.clan.ClanCreationInvitationCookie;
import net.bnubot.core.clan.ClanInvitationCookie;
import net.bnubot.core.clan.ClanMember;
import net.bnubot.core.clan.ClanRankIDs;
import net.bnubot.core.clan.ClanStatusIDs;
import net.bnubot.core.friend.FriendEntry;
import net.bnubot.logging.Out;
import net.bnubot.settings.ConnectionSettings;
import net.bnubot.settings.GlobalSettings;
import net.bnubot.util.BNetInputStream;
import net.bnubot.util.BNetUser;
import net.bnubot.util.ByteArray;
import net.bnubot.util.CookieUtility;
import net.bnubot.util.StatString;
import net.bnubot.util.TimeFormatter;
import net.bnubot.util.UnloggedException;
import net.bnubot.util.UserProfile;
import net.bnubot.util.crypto.HexDump;
import net.bnubot.util.task.Task;
import org.jbls.Hashing.BrokenSHA1;
import org.jbls.Hashing.DoubleHash;
import org.jbls.Hashing.HashMain;
import org.jbls.Hashing.SRP;

/**
 * Represents a connection to a Battle.Net Chat Server (BNCS)
 * @author scotta
 */
public class BNCSConnection extends Connection {

    public static final String[] clanRanks = { "Initiate", "Peon", "Grunt", "Shaman", "Chieftain" };

    private static final String BNCS_TYPE = "Battle.net";

    private BotNetConnection botnet = null;

    public BotNetConnection getBotNet() {
        return botnet;
    }

    private InputStream bncsInputStream = null;

    private DataOutputStream bncsOutputStream = null;

    private ProductIDs productID = null;

    private int verByte;

    private Integer nlsRevision = null;

    private BNCSWarden warden = null;

    private byte[] warden_seed = null;

    private int serverToken = 0;

    private final int clientToken = Math.abs(new Random().nextInt());

    private SRP srp = null;

    private byte proof_M2[] = null;

    protected Integer myClan = null;

    protected Byte myClanRank = null;

    protected long lastNormalJoin;

    public BNCSConnection(ConnectionSettings cs, Profile p) {
        super(cs, p);
        if (cs.enableBotNet) try {
            botnet = new BotNetConnection(this, cs, profile);
            botnet.start();
        } catch (Exception e) {
            Out.exception(e);
            botnet = null;
        }
    }

    @Override
    public String getServerType() {
        return BNCS_TYPE;
    }

    /**
	 * Initialize the connection, send game id
	 *
	 * @throws Exception
	 */
    private void initializeBNCS(Task connect) throws Exception {
        nlsRevision = null;
        warden = null;
        warden_seed = null;
        productID = cs.product;
        connect.updateProgress("Connecting to Battle.net");
        socket = makeSocket(getServer(), getPort());
        bncsInputStream = socket.getInputStream();
        bncsOutputStream = new DataOutputStream(socket.getOutputStream());
        bncsOutputStream.writeByte(0x01);
        connect.updateProgress("Connected");
    }

    /**
	 * Send the initial set of packets
	 *
	 * @throws Exception
	 */
    private void sendInitialPackets(Task connect) throws Exception {
        connect.updateProgress("Initializing BNCS");
        BNCSPacket p;
        Locale loc = Locale.getDefault();
        String prodLang = loc.getLanguage() + loc.getCountry();
        int tzBias = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / -60000;
        switch(productID) {
            case STAR:
            case SEXP:
            case D2DV:
            case D2XP:
            case WAR3:
            case W3XP:
                {
                    p = new BNCSPacket(this, BNCSPacketId.SID_AUTH_INFO);
                    p.writeDWord(0);
                    p.writeDWord(PlatformIDs.PLATFORM_IX86);
                    p.writeDWord(productID.getDword());
                    p.writeDWord(verByte);
                    p.writeDWord(prodLang);
                    p.writeDWord(0);
                    p.writeDWord(tzBias);
                    p.writeDWord(0x409);
                    p.writeDWord(0x409);
                    p.writeNTString(loc.getISO3Country());
                    p.writeNTString(loc.getDisplayCountry());
                    p.sendPacket(bncsOutputStream);
                    break;
                }
            case DRTL:
            case DSHR:
            case SSHR:
            case JSTR:
            case W2BN:
                {
                    if (productID == ProductIDs.SSHR) {
                        p = new BNCSPacket(this, BNCSPacketId.SID_CLIENTID);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeByte(0);
                        p.writeByte(0);
                        p.sendPacket(bncsOutputStream);
                    } else {
                        p = new BNCSPacket(this, BNCSPacketId.SID_CLIENTID2);
                        p.writeDWord(1);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeDWord(0);
                        p.writeByte(0);
                        p.writeByte(0);
                        p.sendPacket(bncsOutputStream);
                    }
                    p = new BNCSPacket(this, BNCSPacketId.SID_LOCALEINFO);
                    p.writeQWord(0);
                    p.writeQWord(0);
                    p.writeDWord(tzBias);
                    p.writeDWord(0x409);
                    p.writeDWord(0x409);
                    p.writeDWord(0x409);
                    p.writeNTString("ena");
                    p.writeNTString("1");
                    p.writeNTString(loc.getISO3Country());
                    p.writeNTString(loc.getDisplayCountry());
                    p.sendPacket(bncsOutputStream);
                    p = new BNCSPacket(this, BNCSPacketId.SID_STARTVERSIONING);
                    p.writeDWord(PlatformIDs.PLATFORM_IX86);
                    p.writeDWord(productID.getDword());
                    p.writeDWord(verByte);
                    p.writeDWord(0);
                    p.sendPacket(bncsOutputStream);
                    break;
                }
            default:
                dispatchRecieveError("Don't know how to connect with product " + productID);
                disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                break;
        }
    }

    @Override
    protected void initializeConnection(Task connect) throws Exception {
        if (botnet != null) botnet.sendStatusUpdate();
        myClan = null;
        myClanRank = null;
        verByte = HashMain.getVerByte(cs.product.getBnls());
        try {
            connect.updateProgress("Getting verbyte from BNLS");
            int vb = BNLSManager.getVerByte(cs.product);
            if (vb != verByte) {
                dispatchRecieveInfo("BNLS_REQUESTVERSIONBYTE: 0x" + Integer.toHexString(vb) + ".");
                verByte = vb;
            }
        } catch (EOFException e) {
            completeTask(connect);
            Out.error(getClass(), "BNLS login failed");
            disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
            return;
        }
        initializeBNCS(connect);
        sendInitialPackets(connect);
    }

    /**
	 * Do the login work up to SID_ENTERCHAT
	 *
	 * @throws Exception
	 */
    @Override
    protected boolean sendLoginPackets(Task connect) throws Exception {
        while (isConnected() && !socket.isClosed() && !disposed) {
            if (bncsInputStream.available() <= 0) {
                sleep(200);
            } else {
                BNCSPacketReader pr = new BNCSPacketReader(bncsInputStream);
                BNetInputStream is = pr.getData();
                switch(pr.packetId) {
                    case SID_OPTIONALWORK:
                    case SID_EXTRAWORK:
                    case SID_REQUIREDWORK:
                        break;
                    case SID_NULL:
                        {
                            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_NULL);
                            p.sendPacket(bncsOutputStream);
                            break;
                        }
                    case SID_PING:
                        {
                            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_PING);
                            p.writeDWord(is.readDWord());
                            p.sendPacket(bncsOutputStream);
                            break;
                        }
                    case SID_AUTH_INFO:
                    case SID_STARTVERSIONING:
                        {
                            if (pr.packetId == BNCSPacketId.SID_AUTH_INFO) {
                                nlsRevision = is.readDWord();
                                serverToken = is.readDWord();
                                is.skip(4);
                            }
                            long mpqFileTime = is.readQWord();
                            String mpqFileName = is.readNTString();
                            byte[] valueStr = is.readNTBytes();
                            Out.debug(getClass(), "MPQ: " + mpqFileName);
                            byte extraData[] = null;
                            if (is.available() == 0x80) {
                                extraData = new byte[0x80];
                                is.read(extraData, 0, 0x80);
                            }
                            assert (is.available() == 0);
                            byte keyHash[] = null;
                            byte keyHash2[] = null;
                            if (nlsRevision != null) {
                                keyHash = HashMain.hashKey(clientToken, serverToken, cs.cdkey).getBuffer();
                                if ((productID == ProductIDs.D2XP) || (productID == ProductIDs.W3XP)) keyHash2 = HashMain.hashKey(clientToken, serverToken, cs.cdkey2).getBuffer();
                                warden = null;
                                warden_seed = new byte[4];
                                System.arraycopy(keyHash, 16, warden_seed, 0, 4);
                            }
                            Task task = createTask("BNLS_VERSIONCHECKEX2", "...");
                            VersionCheckResult vcr = BNLSManager.sendVersionCheckEx2(task, productID, mpqFileTime, mpqFileName, valueStr);
                            completeTask(task);
                            if (vcr == null) {
                                dispatchRecieveError("CheckRevision failed.");
                                disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                break;
                            }
                            if (nlsRevision != null) {
                                connect.updateProgress("CheckRevision/CD Key challenge");
                                BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_AUTH_CHECK);
                                p.writeDWord(clientToken);
                                p.writeDWord(vcr.exeVersion);
                                p.writeDWord(vcr.exeHash);
                                if (keyHash2 == null) p.writeDWord(1); else p.writeDWord(2);
                                p.writeDWord(0);
                                if (keyHash.length != 36) throw new Exception("Invalid keyHash length");
                                p.write(keyHash);
                                if (keyHash2 != null) {
                                    if (keyHash2.length != 36) throw new Exception("Invalid keyHash2 length");
                                    p.write(keyHash2);
                                }
                                p.writeNTString(vcr.exeInfo);
                                p.writeNTString(cs.username);
                                p.sendPacket(bncsOutputStream);
                            } else {
                                connect.updateProgress("CheckRevision");
                                BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_REPORTVERSION);
                                p.writeDWord(PlatformIDs.PLATFORM_IX86);
                                p.writeDWord(productID.getDword());
                                p.writeDWord(verByte);
                                p.writeDWord(vcr.exeVersion);
                                p.writeDWord(vcr.exeHash);
                                p.writeNTString(vcr.exeInfo);
                                p.sendPacket(bncsOutputStream);
                            }
                            break;
                        }
                    case SID_REPORTVERSION:
                    case SID_AUTH_CHECK:
                        {
                            int result = is.readDWord();
                            String extraInfo = is.readNTString();
                            assert (is.available() == 0);
                            if (pr.packetId == BNCSPacketId.SID_AUTH_CHECK) {
                                if (result != 0) {
                                    switch(result) {
                                        case 0x0100:
                                            dispatchRecieveError("Update required: " + extraInfo);
                                            BNFTPConnection.downloadFile(extraInfo);
                                            break;
                                        case 0x0101:
                                            dispatchRecieveError("Invalid version.");
                                            break;
                                        case 0x102:
                                            dispatchRecieveError("Game version must be downgraded: " + extraInfo);
                                            break;
                                        case 0x200:
                                            dispatchRecieveError("Invalid CD key.");
                                            break;
                                        case 0x201:
                                            dispatchRecieveError("CD key in use by " + extraInfo);
                                            break;
                                        case 0x202:
                                            dispatchRecieveError("Banned key.");
                                            break;
                                        case 0x203:
                                            dispatchRecieveError("Wrong product for CD key.");
                                            break;
                                        case 0x210:
                                            dispatchRecieveError("Invalid second CD key.");
                                            break;
                                        case 0x211:
                                            dispatchRecieveError("Second CD key in use by " + extraInfo);
                                            break;
                                        case 0x212:
                                            dispatchRecieveError("Banned second key.");
                                            break;
                                        case 0x213:
                                            dispatchRecieveError("Wrong product for second CD key.");
                                            break;
                                        default:
                                            dispatchRecieveError("Unknown SID_AUTH_CHECK result 0x" + Integer.toHexString(result));
                                            break;
                                    }
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                }
                                dispatchRecieveInfo("Passed CD key challenge and CheckRevision.");
                            } else {
                                if (result != 2) {
                                    switch(result) {
                                        case 0:
                                            dispatchRecieveError("Failed version check.");
                                            break;
                                        case 1:
                                            dispatchRecieveError("Old game version.");
                                            break;
                                        case 3:
                                            dispatchRecieveError("Reinstall required.");
                                            break;
                                        default:
                                            dispatchRecieveError("Unknown SID_REPORTVERSION result 0x" + Integer.toHexString(result));
                                            break;
                                    }
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                }
                                dispatchRecieveInfo("Passed CheckRevision.");
                            }
                            connect.updateProgress("Logging in");
                            sendKeyOrPassword();
                            break;
                        }
                    case SID_CDKEY:
                    case SID_CDKEY2:
                        {
                            int result = is.readDWord();
                            String keyOwner = is.readNTString();
                            if (result != 1) {
                                switch(result) {
                                    case 0x02:
                                        dispatchRecieveError("Invalid CD key.");
                                        break;
                                    case 0x03:
                                        dispatchRecieveError("Bad CD key product.");
                                        break;
                                    case 0x04:
                                        dispatchRecieveError("CD key banned.");
                                        break;
                                    case 0x05:
                                        dispatchRecieveError("CD key in use by " + keyOwner);
                                        break;
                                    default:
                                        dispatchRecieveError("Unknown SID_CDKEY response 0x" + Integer.toHexString(result));
                                        break;
                                }
                                disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                break;
                            }
                            dispatchRecieveInfo("CD key accepted.");
                            connect.updateProgress("Logging in");
                            sendPassword();
                            break;
                        }
                    case SID_AUTH_ACCOUNTLOGON:
                        {
                            int status = is.readDWord();
                            switch(status) {
                                case 0x00:
                                    dispatchRecieveInfo("Login accepted; requires proof.");
                                    connect.updateProgress("Login accepted; proving");
                                    break;
                                case 0x01:
                                    dispatchRecieveError("Account doesn't exist; creating...");
                                    connect.updateProgress("Creating account");
                                    if (srp == null) {
                                        dispatchRecieveError("SRP is not initialized!");
                                        disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                        break;
                                    }
                                    byte[] salt = new byte[32];
                                    new Random().nextBytes(salt);
                                    byte[] verifier = srp.get_v(salt).toByteArray();
                                    if (salt.length != 32) throw new Exception("Salt length wasn't 32!");
                                    if (verifier.length != 32) throw new Exception("Verifier length wasn't 32!");
                                    BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_AUTH_ACCOUNTCREATE);
                                    p.write(salt);
                                    p.write(verifier);
                                    p.writeNTString(cs.username);
                                    p.sendPacket(bncsOutputStream);
                                    break;
                                case 0x05:
                                    dispatchRecieveError("Account requires upgrade");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                default:
                                    dispatchRecieveError("Unknown SID_AUTH_ACCOUNTLOGON status 0x" + Integer.toHexString(status));
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                            }
                            if (status != 0) break;
                            if (srp == null) {
                                dispatchRecieveError("SRP is not initialized!");
                                disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                break;
                            }
                            byte s[] = new byte[32];
                            byte B[] = new byte[32];
                            is.read(s, 0, 32);
                            is.read(B, 0, 32);
                            byte M1[] = srp.getM1(s, B);
                            proof_M2 = srp.getM2(s, B);
                            if (M1.length != 20) throw new Exception("Invalid M1 length");
                            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_AUTH_ACCOUNTLOGONPROOF);
                            p.write(M1);
                            p.sendPacket(bncsOutputStream);
                            break;
                        }
                    case SID_AUTH_ACCOUNTCREATE:
                        {
                            int status = is.readDWord();
                            switch(status) {
                                case 0x00:
                                    dispatchRecieveInfo("Account created; logging in.");
                                    connect.updateProgress("Logging in");
                                    sendKeyOrPassword();
                                    break;
                                default:
                                    dispatchRecieveError("Create account failed with error code 0x" + Integer.toHexString(status));
                                    break;
                            }
                            break;
                        }
                    case SID_AUTH_ACCOUNTLOGONPROOF:
                        {
                            int status = is.readDWord();
                            byte server_M2[] = new byte[20];
                            is.read(server_M2, 0, 20);
                            String additionalInfo = null;
                            if (is.available() != 0) additionalInfo = is.readNTStringUTF8();
                            switch(status) {
                                case 0x00:
                                    break;
                                case 0x02:
                                    dispatchRecieveError("Incorrect password.");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                case 0x0E:
                                    dispatchRecieveError("An email address should be registered for this account.");
                                    connect.updateProgress("Registering email address");
                                    sendSetEmail();
                                    break;
                                case 0x0F:
                                    dispatchRecieveError("Custom bnet error: " + additionalInfo);
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                default:
                                    dispatchRecieveError("Unknown SID_AUTH_ACCOUNTLOGONPROOF status: 0x" + Integer.toHexString(status));
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                            }
                            if (!isConnected()) break;
                            for (int i = 0; i < 20; i++) {
                                if (server_M2[i] != proof_M2[i]) throw new Exception("Server couldn't prove password");
                            }
                            dispatchRecieveInfo("Login successful; entering chat.");
                            connect.updateProgress("Entering chat");
                            sendEnterChat();
                            break;
                        }
                    case SID_LOGONRESPONSE2:
                        {
                            int result = is.readDWord();
                            switch(result) {
                                case 0x00:
                                    dispatchRecieveInfo("Login successful; entering chat.");
                                    connect.updateProgress("Entering chat");
                                    sendEnterChat();
                                    sendGetChannelList();
                                    sendJoinChannel(cs.channel);
                                    break;
                                case 0x01:
                                    dispatchRecieveInfo("Account doesn't exist; creating...");
                                    connect.updateProgress("Creating account");
                                    int[] passwordHash = BrokenSHA1.calcHashBuffer(cs.password.toLowerCase().getBytes());
                                    BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CREATEACCOUNT2);
                                    p.writeDWord(passwordHash[0]);
                                    p.writeDWord(passwordHash[1]);
                                    p.writeDWord(passwordHash[2]);
                                    p.writeDWord(passwordHash[3]);
                                    p.writeDWord(passwordHash[4]);
                                    p.writeNTString(cs.username);
                                    p.sendPacket(bncsOutputStream);
                                    break;
                                case 0x02:
                                    dispatchRecieveError("Incorrect password.");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                case 0x06:
                                    dispatchRecieveError("Your account is closed.");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                default:
                                    dispatchRecieveError("Unknown SID_LOGONRESPONSE2 result 0x" + Integer.toHexString(result));
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                            }
                            break;
                        }
                    case SID_CLIENTID:
                        {
                            break;
                        }
                    case SID_LOGONCHALLENGE:
                        {
                            serverToken = is.readDWord();
                            break;
                        }
                    case SID_LOGONCHALLENGEEX:
                        {
                            is.readDWord();
                            serverToken = is.readDWord();
                            break;
                        }
                    case SID_CREATEACCOUNT2:
                        {
                            int status = is.readDWord();
                            is.readNTString();
                            switch(status) {
                                case 0x00:
                                    dispatchRecieveInfo("Account created");
                                    connect.updateProgress("Logging in");
                                    sendKeyOrPassword();
                                    break;
                                case 0x02:
                                    dispatchRecieveError("Name contained invalid characters");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                case 0x03:
                                    dispatchRecieveError("Name contained a banned word");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                case 0x04:
                                    dispatchRecieveError("Account already exists");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                case 0x06:
                                    dispatchRecieveError("Name did not contain enough alphanumeric characters");
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                                default:
                                    dispatchRecieveError("Unknown SID_CREATEACCOUNT2 status 0x" + Integer.toHexString(status));
                                    disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                                    break;
                            }
                            break;
                        }
                    case SID_SETEMAIL:
                        {
                            dispatchRecieveError("An email address should be registered for this account.");
                            connect.updateProgress("Registering email address");
                            sendSetEmail();
                            break;
                        }
                    case SID_ENTERCHAT:
                        {
                            String uniqueUserName = is.readNTString();
                            StatString myStatString = new StatString(is.readNTString());
                            is.readNTString();
                            myUser = new BNetUser(this, uniqueUserName, cs.myRealm);
                            myUser.setStatString(myStatString);
                            dispatchEnterChat(myUser);
                            dispatchTitleChanged();
                            if (GlobalSettings.displayBattleNetMOTD) {
                                BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_NEWS_INFO);
                                p.writeDWord((int) (new java.util.Date().getTime() / 1000));
                                p.sendPacket(bncsOutputStream);
                            }
                            sendFriendsList();
                            if (nlsRevision != null) {
                                sendGetChannelList();
                                sendJoinChannel(cs.channel);
                            }
                            return true;
                        }
                    case SID_GETCHANNELLIST:
                        {
                            recieveGetChannelList(is);
                            break;
                        }
                    case SID_CLANINFO:
                        {
                            recvClanInfo(is);
                            break;
                        }
                    case SID_WARDEN:
                        {
                            recieveWarden(is);
                            break;
                        }
                    default:
                        Out.debugAlways(getClass(), "Unexpected packet " + pr.packetId.name() + "\n" + HexDump.hexDump(pr.data));
                        break;
                }
            }
        }
        return false;
    }

    private void recieveWarden(BNetInputStream is) {
        if (warden == null) try {
            warden = new BNCSWarden(this, warden_seed);
        } catch (Exception e) {
            warden = null;
            Out.exception(e);
        }
        if (warden != null) try {
            warden.processWardenPacket(is.readFully(), bncsOutputStream);
            return;
        } catch (Exception e) {
            Out.exception(e);
        }
        Out.error(getClass(), "Recieved SID_WARDEN; " + "you will be disconnected from battle.net in 2 minutes. Visit " + "http://forums.clanbnu.net/index.php/topic,681.0.html " + "for more information.");
    }

    /**
	 * @param is
	 * @throws IOException
	 * @throws SocketException
	 */
    private void recvClanInfo(BNetInputStream is) throws IOException, SocketException {
        is.readByte();
        myClan = is.readDWord();
        myClanRank = is.readByte();
        dispatchTitleChanged();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANMEMBERLIST);
        p.writeDWord(0);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * JSTR: Send SID_CDKEY W2BN: Send SID_CDKEY2 else: Call sendPassword()
	 *
	 * @throws Exception
	 */
    private void sendKeyOrPassword() throws Exception {
        BNCSPacket p;
        switch(productID) {
            case JSTR:
                p = new BNCSPacket(this, BNCSPacketId.SID_CDKEY);
                p.writeDWord(0);
                p.writeNTString(cs.cdkey);
                p.writeNTString(cs.username);
                p.sendPacket(bncsOutputStream);
                break;
            case W2BN:
                byte[] keyHash = HashMain.hashW2Key(clientToken, serverToken, cs.cdkey).getBuffer();
                if (keyHash.length != 40) throw new Exception("Invalid keyHash length");
                p = new BNCSPacket(this, BNCSPacketId.SID_CDKEY2);
                p.writeDWord(0);
                p.write(keyHash);
                p.writeNTString(cs.username);
                p.sendPacket(bncsOutputStream);
                break;
            default:
                sendPassword();
                break;
        }
    }

    /**
	 * Send SID_LOGONRESPONSE2 (OLS) or SID_AUTH_ACCOUNTLOGON (NLS)
	 *
	 * @throws Exception
	 */
    private void sendPassword() throws Exception {
        if (!cs.enablePlug) switch(productID) {
            case DSHR:
            case DRTL:
            case SSHR:
            case JSTR:
            case STAR:
            case SEXP:
            case W2BN:
                BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_UDPPINGRESPONSE);
                p.writeDWord("bnet");
                p.sendPacket(bncsOutputStream);
                break;
        }
        if ((nlsRevision == null) || (nlsRevision == 0)) {
            int passwordHash[] = DoubleHash.doubleHash(cs.password.toLowerCase(), clientToken, serverToken);
            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_LOGONRESPONSE2);
            p.writeDWord(clientToken);
            p.writeDWord(serverToken);
            p.writeDWord(passwordHash[0]);
            p.writeDWord(passwordHash[1]);
            p.writeDWord(passwordHash[2]);
            p.writeDWord(passwordHash[3]);
            p.writeDWord(passwordHash[4]);
            p.writeNTString(cs.username);
            p.sendPacket(bncsOutputStream);
        } else {
            srp = new SRP(cs.username, cs.password);
            srp.set_NLS(nlsRevision);
            byte A[] = srp.get_A();
            if (A.length != 32) throw new Exception("Invalid A length");
            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_AUTH_ACCOUNTLOGON);
            p.write(A);
            p.writeNTString(cs.username);
            p.sendPacket(bncsOutputStream);
        }
    }

    /**
	 * Send SID_ENTERCHAT
	 *
	 * @throws Exception
	 */
    private void sendEnterChat() throws Exception {
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_ENTERCHAT);
        p.writeNTString("");
        p.writeNTString("");
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_GETCHANNELLIST
	 *
	 * @throws Exception
	 */
    private void sendGetChannelList() throws Exception {
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_GETCHANNELLIST);
        p.writeDWord(productID.getDword());
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * This method is the main loop after recieving SID_ENTERCHAT
	 *
	 * @throws Exception
	 */
    @Override
    protected void connectedLoop() throws Exception {
        lastNullPacket = System.currentTimeMillis();
        lastNormalJoin = 0;
        profile.lastAntiIdle = lastNullPacket;
        if (botnet != null) botnet.sendStatusUpdate();
        while (isConnected() && !socket.isClosed() && !disposed) {
            long timeNow = System.currentTimeMillis();
            if (true) {
                long timeSinceNullPacket = (timeNow - lastNullPacket) / 1000;
                if (timeSinceNullPacket > 5) {
                    lastNullPacket = timeNow;
                    BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_NULL);
                    p.sendPacket(bncsOutputStream);
                }
            }
            if ((channelName != null) && cs.enableAntiIdle) {
                synchronized (profile) {
                    long timeSinceAntiIdle = timeNow - profile.lastAntiIdle;
                    timeSinceAntiIdle /= 1000;
                    timeSinceAntiIdle /= 60;
                    if (timeSinceAntiIdle >= cs.antiIdleTimer) {
                        profile.lastAntiIdle = timeNow;
                        sendChatInternal(getAntiIdle());
                    }
                }
            }
            if (bncsInputStream.available() <= 0) {
                sleep(200);
            } else {
                BNCSPacketReader pr = new BNCSPacketReader(bncsInputStream);
                BNetInputStream is = pr.getData();
                switch(pr.packetId) {
                    case SID_NULL:
                        {
                            lastNullPacket = timeNow;
                            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_NULL);
                            p.sendPacket(bncsOutputStream);
                            break;
                        }
                    case SID_PING:
                        {
                            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_PING);
                            p.writeDWord(is.readDWord());
                            p.sendPacket(bncsOutputStream);
                            break;
                        }
                    case SID_NEWS_INFO:
                        {
                            int numEntries = is.readByte();
                            is.skip(12);
                            for (int i = 0; i < numEntries; i++) {
                                int timeStamp = is.readDWord();
                                String news = is.readNTStringUTF8().trim();
                                if (timeStamp == 0) dispatchRecieveServerInfo(news);
                            }
                            break;
                        }
                    case SID_GETCHANNELLIST:
                        {
                            recieveGetChannelList(is);
                            break;
                        }
                    case SID_CHATEVENT:
                        {
                            BNCSChatEventId eid = BNCSChatEventId.values()[is.readDWord()];
                            int flags = is.readDWord();
                            int ping = is.readDWord();
                            is.skip(12);
                            String username = is.readNTString();
                            ByteArray data = null;
                            StatString statstr = null;
                            switch(eid) {
                                case EID_SHOWUSER:
                                case EID_JOIN:
                                    statstr = is.readStatString();
                                    break;
                                case EID_USERFLAGS:
                                    statstr = is.readStatString();
                                    if (statstr.toString().length() == 0) statstr = null;
                                    break;
                                default:
                                    data = new ByteArray(is.readNTBytes());
                                    break;
                            }
                            BNetUser user = null;
                            switch(eid) {
                                case EID_SHOWUSER:
                                case EID_USERFLAGS:
                                case EID_JOIN:
                                case EID_LEAVE:
                                case EID_TALK:
                                case EID_EMOTE:
                                case EID_WHISPERSENT:
                                case EID_WHISPER:
                                    switch(productID) {
                                        case D2DV:
                                        case D2XP:
                                            int asterisk = username.indexOf('*');
                                            if (asterisk >= 0) username = username.substring(asterisk + 1);
                                            break;
                                    }
                                    if (myUser.equals(username)) user = myUser; else user = getCreateBNetUser(username, myUser);
                                    user.setFlags(flags);
                                    user.setPing(ping);
                                    if (statstr != null) user.setStatString(statstr);
                                    break;
                            }
                            switch(eid) {
                                case EID_SHOWUSER:
                                case EID_USERFLAGS:
                                    dispatchChannelUser(user);
                                    break;
                                case EID_JOIN:
                                    dispatchChannelJoin(user);
                                    break;
                                case EID_LEAVE:
                                    dispatchChannelLeave(user);
                                    break;
                                case EID_TALK:
                                    dispatchRecieveChat(user, data);
                                    break;
                                case EID_BROADCAST:
                                    dispatchRecieveBroadcast(username, flags, data.toString());
                                    break;
                                case EID_EMOTE:
                                    dispatchRecieveEmote(user, data.toString());
                                    break;
                                case EID_INFO:
                                    dispatchRecieveServerInfo(data.toString());
                                    break;
                                case EID_ERROR:
                                    dispatchRecieveServerError(data.toString());
                                    break;
                                case EID_CHANNEL:
                                    String newChannel = data.toString();
                                    if ((channelName != null) && !channelName.equals(newChannel)) clearQueue();
                                    channelName = newChannel;
                                    dispatchJoinedChannel(newChannel, flags);
                                    dispatchTitleChanged();
                                    if (botnet != null) botnet.sendStatusUpdate();
                                    break;
                                case EID_WHISPERSENT:
                                    dispatchWhisperSent(user, data.toString());
                                    break;
                                case EID_WHISPER:
                                    dispatchWhisperRecieved(user, data.toString());
                                    break;
                                case EID_CHANNELDOESNOTEXIST:
                                    dispatchRecieveError("Channel does not exist; creating");
                                    sendJoinChannel2(data.toString());
                                    break;
                                case EID_CHANNELRESTRICTED:
                                    long timeSinceNormalJoin = timeNow - lastNormalJoin;
                                    if ((lastNormalJoin != 0) && (timeSinceNormalJoin < 5000)) {
                                        dispatchRecieveError("Channel is restricted; forcing entry");
                                        sendJoinChannel2(data.toString());
                                    } else {
                                        dispatchRecieveError("Channel " + data.toString() + " is restricted");
                                    }
                                    break;
                                case EID_CHANNELFULL:
                                    dispatchRecieveError("Channel " + data.toString() + " is full");
                                    break;
                                default:
                                    dispatchRecieveError("Unknown SID_CHATEVENT " + eid + ": " + data.toString());
                                    break;
                            }
                            break;
                        }
                    case SID_MESSAGEBOX:
                        {
                            is.readDWord();
                            String text = is.readNTStringUTF8();
                            String caption = is.readNTStringUTF8();
                            dispatchRecieveInfo("<" + caption + "> " + text);
                            break;
                        }
                    case SID_FLOODDETECTED:
                        {
                            dispatchRecieveError("You have been disconnected for flooding.");
                            disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
                            break;
                        }
                    case SID_QUERYREALMS2:
                        {
                            is.readDWord();
                            int numRealms = is.readDWord();
                            String realms[] = new String[numRealms];
                            for (int i = 0; i < numRealms; i++) {
                                is.readDWord();
                                realms[i] = is.readNTStringUTF8();
                                is.readNTStringUTF8();
                            }
                            dispatchQueryRealms2(realms);
                            break;
                        }
                    case SID_LOGONREALMEX:
                        {
                            if (pr.packetLength < 12) throw new Exception("pr.packetLength < 12"); else if (pr.packetLength == 12) {
                                is.readDWord();
                                int status = is.readDWord();
                                switch(status) {
                                    case 0x80000001:
                                        dispatchRecieveError("Realm is unavailable.");
                                        break;
                                    case 0x80000002:
                                        dispatchRecieveError("Realm logon failed");
                                        break;
                                    default:
                                        throw new Exception("Unknown status code 0x" + Integer.toHexString(status));
                                }
                            } else {
                                int MCPChunk1[] = new int[4];
                                MCPChunk1[0] = is.readDWord();
                                MCPChunk1[1] = is.readDWord();
                                MCPChunk1[2] = is.readDWord();
                                MCPChunk1[3] = is.readDWord();
                                int ip = is.readDWord();
                                int port = is.readDWord();
                                port = ((port & 0xFF00) >> 8) | ((port & 0x00FF) << 8);
                                int MCPChunk2[] = new int[12];
                                MCPChunk2[0] = is.readDWord();
                                MCPChunk2[1] = is.readDWord();
                                MCPChunk2[2] = is.readDWord();
                                MCPChunk2[3] = is.readDWord();
                                MCPChunk2[4] = is.readDWord();
                                MCPChunk2[5] = is.readDWord();
                                MCPChunk2[6] = is.readDWord();
                                MCPChunk2[7] = is.readDWord();
                                MCPChunk2[8] = is.readDWord();
                                MCPChunk2[9] = is.readDWord();
                                MCPChunk2[10] = is.readDWord();
                                MCPChunk2[11] = is.readDWord();
                                String uniqueName = is.readNTString();
                                dispatchLogonRealmEx(MCPChunk1, ip, port, MCPChunk2, uniqueName);
                            }
                            break;
                        }
                    case SID_READUSERDATA:
                        {
                            int numAccounts = is.readDWord();
                            int numKeys = is.readDWord();
                            @SuppressWarnings("unchecked") List<Object> keys = (List<Object>) CookieUtility.destroyCookie(is.readDWord());
                            if (numAccounts != 1) throw new IllegalStateException("SID_READUSERDATA with numAccounts != 1");
                            UserProfile up = new UserProfile((String) keys.remove(0));
                            dispatchRecieveInfo("Profile for " + up.getUser());
                            for (int i = 0; i < numKeys; i++) {
                                String key = (String) keys.get(i);
                                String value = is.readNTStringUTF8();
                                if ((key == null) || (key.length() == 0)) continue;
                                value = prettyProfileValue(key, value);
                                if (value.length() != 0) {
                                    dispatchRecieveInfo(key + " = " + value);
                                } else if (key.equals(UserProfile.PROFILE_DESCRIPTION) || key.equals(UserProfile.PROFILE_LOCATION) || key.equals(UserProfile.PROFILE_SEX)) {
                                } else {
                                    continue;
                                }
                                up.put(key, value);
                            }
                            if (PluginManager.getEnableGui()) new ProfileEditor(up, this);
                            break;
                        }
                    case SID_FRIENDSLIST:
                        {
                            byte numEntries = is.readByte();
                            FriendEntry[] entries = new FriendEntry[numEntries];
                            for (int i = 0; i < numEntries; i++) {
                                String uAccount = is.readNTString();
                                byte uStatus = is.readByte();
                                byte uLocation = is.readByte();
                                int uProduct = is.readDWord();
                                String uLocationName = is.readNTStringUTF8();
                                entries[i] = new FriendEntry(uAccount, uStatus, uLocation, uProduct, uLocationName);
                            }
                            dispatchFriendsList(entries);
                            break;
                        }
                    case SID_FRIENDSUPDATE:
                        {
                            byte fEntry = is.readByte();
                            byte fLocation = is.readByte();
                            byte fStatus = is.readByte();
                            int fProduct = is.readDWord();
                            String fLocationName = is.readNTStringUTF8();
                            dispatchFriendsUpdate(new FriendEntry(fEntry, fStatus, fLocation, fProduct, fLocationName));
                            break;
                        }
                    case SID_FRIENDSADD:
                        {
                            String fAccount = is.readNTString();
                            byte fLocation = is.readByte();
                            byte fStatus = is.readByte();
                            int fProduct = is.readDWord();
                            String fLocationName = is.readNTStringUTF8();
                            dispatchFriendsAdd(new FriendEntry(fAccount, fStatus, fLocation, fProduct, fLocationName));
                            break;
                        }
                    case SID_FRIENDSREMOVE:
                        {
                            byte entry = is.readByte();
                            dispatchFriendsRemove(entry);
                            break;
                        }
                    case SID_FRIENDSPOSITION:
                        {
                            byte oldPosition = is.readByte();
                            byte newPosition = is.readByte();
                            dispatchFriendsPosition(oldPosition, newPosition);
                            break;
                        }
                    case SID_CLANINFO:
                        {
                            recvClanInfo(is);
                            break;
                        }
                    case SID_CLANFINDCANDIDATES:
                        {
                            Object cookie = CookieUtility.destroyCookie(is.readDWord());
                            byte status = is.readByte();
                            byte numCandidates = is.readByte();
                            List<String> candidates = new ArrayList<String>(numCandidates);
                            for (int i = 0; i < numCandidates; i++) candidates.add(is.readNTString());
                            switch(status) {
                                case 0x00:
                                    if (numCandidates < 9) dispatchRecieveError("Insufficient elegible W3 players (" + numCandidates + "/9)."); else dispatchClanFindCandidates(cookie, candidates);
                                    break;
                                case 0x01:
                                    dispatchRecieveError("Clan tag already taken");
                                    break;
                                case 0x08:
                                    dispatchRecieveError("Already in a clan");
                                    break;
                                case 0x0a:
                                    dispatchRecieveError("Invalid clan tag");
                                    break;
                                default:
                                    dispatchRecieveError("Unknown response 0x" + Integer.toHexString(status));
                                    break;
                            }
                            break;
                        }
                    case SID_CLANCREATIONINVITATION:
                        {
                            int cookie = is.readDWord();
                            int clanTag = is.readDWord();
                            String clanName = is.readNTString();
                            String inviter = is.readNTString();
                            ClanCreationInvitationCookie c = new ClanCreationInvitationCookie(this, cookie, clanTag, clanName, inviter);
                            dispatchClanCreationInvitation(c);
                            break;
                        }
                    case SID_CLANINVITATION:
                        {
                            Object cookie = CookieUtility.destroyCookie(is.readDWord());
                            byte status = is.readByte();
                            String result;
                            switch(status) {
                                case 0x00:
                                    result = "Invitation accepted";
                                    break;
                                case 0x04:
                                    result = "Invitation declined";
                                    break;
                                case 0x05:
                                    result = "Failed to invite user";
                                    break;
                                case 0x09:
                                    result = "Clan is full";
                                    break;
                                default:
                                    result = "Unknown response 0x" + Integer.toHexString(status);
                                    break;
                            }
                            if (cookie instanceof CommandResponseCookie) ((CommandResponseCookie) cookie).sendChat(result); else Out.info(getClass(), result);
                            break;
                        }
                    case SID_CLANINVITATIONRESPONSE:
                        {
                            int cookie = is.readDWord();
                            int clanTag = is.readDWord();
                            String clanName = is.readNTString();
                            String inviter = is.readNTString();
                            ClanInvitationCookie c = new ClanInvitationCookie(this, cookie, clanTag, clanName, inviter);
                            dispatchClanInvitation(c);
                            break;
                        }
                    case SID_CLANRANKCHANGE:
                        {
                            int cookie = is.readDWord();
                            byte status = is.readByte();
                            Object obj = CookieUtility.destroyCookie(cookie);
                            String statusCode = null;
                            switch(status) {
                                case ClanStatusIDs.CLANSTATUS_SUCCESS:
                                    statusCode = "Successfully changed rank";
                                    break;
                                case 0x01:
                                    statusCode = "Failed to change rank";
                                    break;
                                case ClanStatusIDs.CLANSTATUS_TOO_SOON:
                                    statusCode = "Cannot change user'socket rank yet";
                                    break;
                                case ClanStatusIDs.CLANSTATUS_NOT_AUTHORIZED:
                                    statusCode = "Not authorized to change user rank*";
                                    break;
                                case 0x08:
                                    statusCode = "Not allowed to change user rank**";
                                    break;
                                default:
                                    statusCode = "Unknown ClanStatusID 0x" + Integer.toHexString(status);
                            }
                            dispatchRecieveInfo(statusCode + "\n" + obj.toString());
                            break;
                        }
                    case SID_CLANMOTD:
                        {
                            int cookieId = is.readDWord();
                            is.readDWord();
                            String text = is.readNTStringUTF8();
                            Object cookie = CookieUtility.destroyCookie(cookieId);
                            dispatchClanMOTD(cookie, text);
                            break;
                        }
                    case SID_CLANMEMBERLIST:
                        {
                            is.readDWord();
                            byte numMembers = is.readByte();
                            ClanMember[] members = new ClanMember[numMembers];
                            for (int i = 0; i < numMembers; i++) {
                                String uName = is.readNTString();
                                byte uRank = is.readByte();
                                byte uOnline = is.readByte();
                                String uLocation = is.readNTStringUTF8();
                                members[i] = new ClanMember(uName, uRank, uOnline, uLocation);
                            }
                            dispatchClanMemberList(members);
                            break;
                        }
                    case SID_CLANMEMBERREMOVED:
                        {
                            String username = is.readNTString();
                            dispatchClanMemberRemoved(username);
                            break;
                        }
                    case SID_CLANMEMBERSTATUSCHANGE:
                        {
                            String username = is.readNTString();
                            byte rank = is.readByte();
                            byte status = is.readByte();
                            String location = is.readNTStringUTF8();
                            dispatchClanMemberStatusChange(new ClanMember(username, rank, status, location));
                            break;
                        }
                    case SID_CLANMEMBERRANKCHANGE:
                        {
                            byte oldRank = is.readByte();
                            byte newRank = is.readByte();
                            String user = is.readNTString();
                            dispatchRecieveInfo("Rank changed from " + ClanRankIDs.ClanRank[oldRank] + " to " + ClanRankIDs.ClanRank[newRank] + " by " + user);
                            dispatchClanMemberRankChange(oldRank, newRank, user);
                            break;
                        }
                    case SID_WARDEN:
                        {
                            recieveWarden(is);
                            break;
                        }
                    default:
                        Out.debugAlways(getClass(), "Unexpected packet " + pr.packetId.name() + "\n" + HexDump.hexDump(pr.data));
                        break;
                }
            }
        }
    }

    /**
	 * Recieve SID_GETCHANNELLIST
	 *
	 * @param is
	 * @throws IOException
	 */
    private void recieveGetChannelList(BNetInputStream is) throws IOException {
        String channelList = null;
        do {
            String s = is.readNTString();
            if (s.length() == 0) break;
            if (channelList == null) channelList = s; else channelList += ", " + s;
        } while (true);
        if (GlobalSettings.displayBattleNetChannels) dispatchRecieveInfo("Channels: " + channelList + ".");
    }

    @Override
    public boolean isOp() {
        if (myUser == null) return false;
        return (myUser.getFlags() & 0x02) == 0x02;
    }

    /**
	 * Send SID_SETEMAIL
	 *
	 * @throws Exception
	 */
    private void sendSetEmail() throws Exception {
        String email = GlobalSettings.email;
        if (email == null) return;
        if (email.length() == 0) return;
        dispatchRecieveInfo("Register email address: " + email);
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_SETEMAIL);
        p.writeNTString(email);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_LEAVECHAT
	 */
    @Override
    public void sendLeaveChat() throws Exception {
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_LEAVECHAT);
        p.sendPacket(bncsOutputStream);
        channelName = null;
        dispatchJoinedChannel(null, 0);
    }

    /**
	 * Send SID_JOINCHANNEL
	 */
    @Override
    public void sendJoinChannel(String channel) throws Exception {
        lastNormalJoin = System.currentTimeMillis();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_JOINCHANNEL);
        p.writeDWord(0);
        p.writeNTString(channel);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_JOINCHANNEL with create channel flag
	 */
    @Override
    public void sendJoinChannel2(String channel) throws Exception {
        lastNormalJoin = 0;
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_JOINCHANNEL);
        p.writeDWord(2);
        p.writeNTString(channel);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CHATCOMMAND
	 */
    @Override
    public void sendChatCommand(ByteArray data) {
        switch(productID) {
            case D2DV:
            case D2XP:
                if ((data.length() > 1) && (data.byteAt(0) == '/')) {
                    String cmd = data.toString().substring(1);
                    int i = cmd.indexOf(' ');
                    if (i != -1) {
                        String theRest = cmd.substring(i + 1);
                        cmd = cmd.substring(0, i);
                        if (isTargetedCommand(cmd)) {
                            if (theRest.charAt(0) != '*') data = new ByteArray('/' + cmd + " *" + theRest);
                        }
                    }
                }
                break;
        }
        super.sendChatCommand(data);
        try {
            BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CHATCOMMAND);
            p.writeNTString(data);
            p.sendPacket(bncsOutputStream);
        } catch (IOException e) {
            dispatchRecieveError(e.getMessage());
            disconnect(ConnectionState.LONG_PAUSE_BEFORE_CONNECT);
            return;
        }
        if (GlobalSettings.displaySlashCommands || data.byteAt(0) != '/') dispatchRecieveChat(myUser, data);
    }

    private boolean isTargetedCommand(String command) {
        if (command.length() < 1) return false;
        switch(command.charAt(0)) {
            case 'b':
                if (command.equals("ban")) return true;
                break;
            case 'd':
                if (command.equals("designate")) return true;
                break;
            case 'i':
                if (command.equals("ignore")) return true;
                break;
            case 'k':
                if (command.equals("kick")) return true;
                break;
            case 'm':
                if (command.equals("m")) return true;
                if (command.equals("msg")) return true;
                break;
            case 's':
                if (command.equals("squelch")) return true;
                break;
            case 'u':
                if (command.equals("unignore")) return true;
                if (command.equals("unsquelch")) return true;
                break;
            case 'w':
                if (command.equals("w")) return true;
                if (command.equals("whisper")) return true;
                if (command.equals("whois")) return true;
                if (command.equals("where")) return true;
                if (command.equals("whereis")) return true;
                break;
        }
        return false;
    }

    /**
	 * Require WAR3 or W3XP
	 * @throws UnsupportedFeatureException
	 */
    private void requireW3() throws UnsupportedFeatureException {
        switch(productID) {
            case WAR3:
            case W3XP:
                return;
        }
        throw new UnsupportedFeatureException("Only WAR3/W3XP support this feature");
    }

    /**
	 * Require the user be on W3 and in or out of a clan
	 * @param inClan if true, require the use to be in a clan; false for out of clan
	 */
    private void requireInClan(boolean inClan) throws UnsupportedFeatureException, IllegalStateException {
        requireW3();
        if (inClan) {
            if (myClan == null) throw new UnloggedException("You are not in a clan");
        } else {
            if (myClan != null) throw new UnloggedException("You are already in a clan");
        }
    }

    /**
	 * Require D2DV or D2XP
	 * @throws UnsupportedFeatureException
	 */
    private void requireD2() throws UnsupportedFeatureException {
        switch(productID) {
            case D2DV:
            case D2XP:
                return;
        }
        throw new UnsupportedFeatureException("Only D2DV/D2XP support this feature");
    }

    /**
	 * Send SID_FRIENDSLIST
	 */
    @Override
    public void sendFriendsList() throws Exception {
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_FRIENDSLIST);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANFINDCANDIDATES
	 */
    @Override
    public void sendClanFindCandidates(Object cookie, int clanTag) throws Exception {
        requireInClan(false);
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANFINDCANDIDATES);
        p.writeDWord(CookieUtility.createCookie(cookie));
        p.writeDWord(clanTag);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANINVITEMULTIPLE
	 * Use this method to create a clan; invite 9 users
	 * Invitees will reiceve SID_CLANCREATIONINVITATION
	 */
    @Override
    public void sendClanInviteMultiple(Object cookie, String clanName, int clanTag, List<String> invitees) throws Exception {
        requireInClan(false);
        if (invitees.size() != 9) throw new UnloggedException("You should invite exactly 9 people");
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANINVITEMULTIPLE);
        p.writeDWord(CookieUtility.createCookie(cookie));
        p.writeNTString(clanName);
        p.writeDWord(clanTag);
        p.writeByte(invitees.size());
        for (String user : invitees) p.writeNTString(user);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANCREATIONINVITATION
	 * Accept or decline an invitation to create a clan
	 * @param response 0x04 = Decline, 0x06 = Accept
	 * TODO Verify these response codes are correct
	 */
    @Override
    public void sendClanCreationInvitation(int cookie, int clanTag, String inviter, int response) throws Exception {
        requireW3();
        switch(response) {
            case 4:
            case 6:
                break;
            default:
                throw new IllegalStateException("Unknown response code 0x" + Integer.toHexString(response));
        }
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANINVITATIONRESPONSE);
        p.writeDWord(cookie);
        p.writeDWord(clanTag);
        p.writeNTString(inviter);
        p.writeByte(response);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANINVITATION
	 */
    @Override
    public void sendClanInvitation(Object cookie, String user) throws Exception {
        requireInClan(true);
        if (myClanRank < 3) throw new UnloggedException("Must be " + clanRanks[3] + " or " + clanRanks[4] + " to invite");
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANINVITATION);
        p.writeDWord(CookieUtility.createCookie(cookie));
        p.writeNTString(user);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANRANKCHANGE
	 */
    @Override
    public void sendClanRankChange(Object cookie, String user, int newRank) throws Exception {
        requireW3();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANRANKCHANGE);
        p.writeDWord(CookieUtility.createCookie(cookie));
        p.writeNTString(user);
        p.writeByte(newRank);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANMOTD
	 */
    @Override
    public void sendClanMOTD(Object cookie) throws Exception {
        requireW3();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANMOTD);
        p.writeDWord(CookieUtility.createCookie(cookie));
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANINVITATIONRESPONSE
	 * @param cookie the cookie from the received SID_CLANINVITATIONRESPONSE
	 * @param clanTag the clan tag from the received SID_CLANINVITATIONRESPONSE
	 * @param inviter the inviter from the received SID_CLANINVITATIONRESPONSE
	 * @param response 0x04 = Decline, 0x06 = Accept
	 * @throws Exception
	 */
    public void sendClanInvitationResponse(int cookie, int clanTag, String inviter, int response) throws Exception {
        requireW3();
        switch(response) {
            case 4:
            case 6:
                break;
            default:
                throw new IllegalStateException("Unknown response code 0x" + Integer.toHexString(response));
        }
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANINVITATIONRESPONSE);
        p.writeDWord(cookie);
        p.writeDWord(clanTag);
        p.writeNTString(inviter);
        p.writeByte(response);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_CLANSETMOTD
	 */
    @Override
    public void sendClanSetMOTD(String text) throws Exception {
        requireW3();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_CLANSETMOTD);
        p.writeDWord(0);
        p.writeNTString(text);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_QUERYREALMS2
	 */
    @Override
    public void sendQueryRealms2() throws Exception {
        requireD2();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_QUERYREALMS2);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_LOGONREALMEX
	 */
    @Override
    public void sendLogonRealmEx(String realmTitle) throws Exception {
        requireD2();
        int[] hash = DoubleHash.doubleHash("password", clientToken, serverToken);
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_LOGONREALMEX);
        p.writeDWord(clientToken);
        p.writeDWord(hash[0]);
        p.writeDWord(hash[1]);
        p.writeDWord(hash[2]);
        p.writeDWord(hash[3]);
        p.writeDWord(hash[4]);
        p.writeNTString(realmTitle);
        p.sendPacket(bncsOutputStream);
    }

    private String prettyProfileValue(String key, String value) {
        if (UserProfile.SYSTEM_ACCOUNT_CREATED.equals(key) || UserProfile.SYSTEM_LAST_LOGON.equals(key) || UserProfile.SYSTEM_LAST_LOGOFF.equals(key)) {
            String parts[] = value.split(" ", 2);
            long time = Long.parseLong(parts[0]);
            time <<= 32;
            time += Long.parseLong(parts[1]);
            return TimeFormatter.fileTime(time).toString();
        } else if (UserProfile.SYSTEM_TIME_LOGGED.equals(key)) {
            long time = Long.parseLong(value);
            time *= 1000;
            return TimeFormatter.formatTime(time);
        }
        return value;
    }

    /**
	 * Send SID_READUSERDATA
	 */
    @Override
    public void sendReadUserData(String user) throws Exception {
        List<String> keys = new ArrayList<String>(7);
        keys.add(user);
        keys.add(UserProfile.PROFILE_SEX);
        keys.add(UserProfile.PROFILE_LOCATION);
        keys.add(UserProfile.PROFILE_DESCRIPTION);
        keys.add(UserProfile.PROFILE_ + "dbkey1");
        keys.add(UserProfile.PROFILE_ + "dbkey2");
        if (myUser.equals(user)) {
            keys.add(UserProfile.SYSTEM_ACCOUNT_CREATED);
            keys.add(UserProfile.SYSTEM_LAST_LOGON);
            keys.add(UserProfile.SYSTEM_LAST_LOGOFF);
            keys.add(UserProfile.SYSTEM_TIME_LOGGED);
            keys.add(UserProfile.SYSTEM_USERNAME);
        }
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_READUSERDATA);
        p.writeDWord(1);
        p.writeDWord(keys.size() - 1);
        p.writeDWord(CookieUtility.createCookie(keys));
        for (String key : keys) p.writeNTString(key);
        p.sendPacket(bncsOutputStream);
    }

    /**
	 * Send SID_WRITEUSERDATA
	 */
    @Override
    public void sendWriteUserData(UserProfile profile) throws Exception {
        if (!myUser.equals(profile.getUser())) throw new Exception("You may only write your own profile!");
        String user = myUser.getShortLogonName();
        int i = user.lastIndexOf('@');
        if (i != -1) user = user.substring(0, i);
        i = user.lastIndexOf('#');
        if (i != -1) user = user.substring(0, i);
        List<String> profileKeys = profile.keySetProfile();
        BNCSPacket p = new BNCSPacket(this, BNCSPacketId.SID_WRITEUSERDATA);
        p.writeDWord(1);
        p.writeDWord(profileKeys.size());
        p.writeNTString(user);
        for (String key : profileKeys) p.writeNTString(key.toString());
        for (String key : profileKeys) p.writeNTString(profile.get(key));
        p.sendPacket(bncsOutputStream);
    }

    @Override
    public String toString() {
        if (myUser != null) {
            String out = new String();
            if (myClan != null) {
                out += "Clan ";
                out += HexDump.DWordToPretty(myClan);
                out += " ";
            }
            if (myClanRank != null) {
                out += clanRanks[myClanRank];
                out += " ";
            }
            out += myUser.getShortLogonName();
            if (channelName != null) out += " - [ #" + channelName + " ]";
            return out;
        }
        return toShortString();
    }

    @Override
    public ProductIDs getProductID() {
        return productID;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (botnet != null) botnet.dispose();
    }

    protected void dispatchQueryRealms2(String[] realms) {
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.queryRealms2(this, realms);
        }
    }

    protected void dispatchLogonRealmEx(int[] MCPChunk1, int ip, int port, int[] MCPChunk2, String uniqueName) {
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.logonRealmEx(this, MCPChunk1, ip, port, MCPChunk2, uniqueName);
        }
    }

    protected void dispatchFriendsList(FriendEntry[] entries) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.friendsList(this, entries);
        }
    }

    protected void dispatchFriendsUpdate(FriendEntry friend) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.friendsUpdate(this, friend);
        }
    }

    protected void dispatchFriendsAdd(FriendEntry friend) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.friendsAdd(this, friend);
        }
    }

    protected void dispatchFriendsRemove(byte entry) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.friendsRemove(this, entry);
        }
    }

    protected void dispatchFriendsPosition(byte oldPosition, byte newPosition) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.friendsPosition(this, oldPosition, newPosition);
        }
    }

    protected void dispatchClanMOTD(Object cookie, String text) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanMOTD(this, cookie, text);
        }
    }

    protected void dispatchClanMemberList(ClanMember[] members) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanMemberList(this, members);
        }
    }

    protected void dispatchClanMemberRemoved(String username) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanMemberRemoved(this, username);
        }
    }

    protected void dispatchClanMemberRankChange(byte oldRank, byte newRank, String user) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanMemberRankChange(this, oldRank, newRank, user);
        }
    }

    protected void dispatchClanMemberStatusChange(ClanMember member) {
        if (!isPrimaryConnection()) return;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanMemberStatusChange(this, member);
        }
    }

    protected void dispatchClanFindCandidates(Object cookie, List<String> candidates) {
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanFindCandidates(this, cookie, candidates);
        }
    }

    protected void dispatchClanCreationInvitation(ClanCreationInvitationCookie c) {
        lastAcceptDecline = c;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanCreationInvitation(this, c);
        }
    }

    protected void dispatchClanInvitation(ClanInvitationCookie c) {
        lastAcceptDecline = c;
        synchronized (eventHandlers) {
            for (EventHandler eh : eventHandlers) eh.clanInvitation(this, c);
        }
    }
}
