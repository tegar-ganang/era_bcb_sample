package org.aacc.asterisk;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.asteriskjava.manager.event.NewChannelEvent;
import org.aacc.main.MainClass;
import org.aacc.campaigns.Contact;
import org.aacc.campaigns.Disposition;
import org.aacc.campaigns.Product;
import org.aacc.campaigns.AbstractCampaign;

public class Call {

    public static final int TYPE_UNKNOWN = -1;

    public static final int TYPE_INBOUND = 1;

    public static final int TYPE_OUTBOUND = 2;

    public static final int TYPE_INTERNAL = 3;

    public static final int TYPE_CONSULTATION = 4;

    /**
     * Call results
     */
    public static final byte CALL_ANSWERED = 1;

    public static final byte CALL_BUSY = 2;

    public static final byte CALL_NOANSWER = 3;

    public static final byte CALL_UNKNOWN = 4;

    /**
     * Collection of currentCalls currently being attended
     */
    static Map<String, Call> currentCalls = Collections.synchronizedMap(new HashMap());

    /**
     *  Agent connected to the call
     */
    private Agent agent;

    /**
     * ID of the call generated in calllog table
     */
    private long id = 0;

    /**
     *  id of the call (same used by Asterisk)
     *  <br>
     */
    private String uniqueId;

    /**
     *  Time when the call started
     */
    private Date timeStart;

    /**
     * Time when the call finished
     */
    private Date timeEnd;

    /**
     * Time when agent started talking
     */
    private Date talkStart;

    /**
     * Time when agent stopped talking
     */
    private Date talkEnd;

    /**
     * DNIS
     */
    private String DNIS;

    /**
     * ANI
     */
    private String ANI;

    private String callingChannel;

    private String calledChannel;

    private int callType = TYPE_UNKNOWN;

    /**
     *  Contact associated to this call, from contacts database.
     */
    private Contact contact;

    /**
     *  Contains information pertaining to this call, that can be defined by the
     *      user. It will be logged in Call Details
     */
    private Map<String, String> callData = Collections.synchronizedMap(new HashMap());

    private Map<Long, Product> products = Collections.synchronizedMap(new HashMap());

    private Map<Long, Disposition> dispositions = Collections.synchronizedMap(new HashMap());

    private String recordFile;

    private AbstractCampaign campaign;

    /**
     * Original position in queue
     */
    private int qPosition;

    /**
     * Time the caller was in queue
     */
    private int qTime;

    /**
     * Comments for this call
     */
    private String comments = "";

    /**
     * Channel originating the call
     */
    private String channel1 = "";

    /**
     * Channel answering the call
     */
    private String channel2 = "";

    private String recording = "";

    private static PreparedStatement stmtInsert;

    private static PreparedStatement stmtUpdateFinish;

    private static boolean staticsInitialized = false;

    public Call() {
        initializeInstance();
    }

    public Call(NewChannelEvent newChannelEvent) {
        ANI = newChannelEvent.getCallerIdNum();
        callingChannel = newChannelEvent.getChannel();
        uniqueId = newChannelEvent.getUniqueId();
        initializeInstance();
    }

    private void initializeInstance() {
        if (!staticsInitialized) {
            staticsInitialized = true;
            try {
                stmtInsert = MainClass.getDbConnection().prepareStatement("INSERT INTO calllog " + "(uniqueId, number, channel1, start) VALUES (?,?,?,?)");
                stmtUpdateFinish = MainClass.getDbConnection().prepareStatement("UPDATE calllog " + "SET callresult = ?, " + "totalTime = ?, " + "finish = ?, " + "campaign = ?, " + "contact = ?, " + "comments = ?, " + "agentId = ?, " + "recording = ? " + "WHERE id = ?");
            } catch (SQLException ex) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
            }
        }
        timeStart = Calendar.getInstance().getTime();
        try {
            stmtInsert.clearParameters();
            stmtInsert.setString(1, uniqueId);
            stmtInsert.setString(2, ANI != null ? ANI : "");
            stmtInsert.setString(3, channel1);
            stmtInsert.setTimestamp(4, new Timestamp(timeStart.getTime()));
            stmtInsert.executeUpdate();
            ResultSet rs = stmtInsert.getGeneratedKeys();
            rs.next();
            id = rs.getLong(1);
        } catch (SQLException ex) {
            Logger.getLogger(Call.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setId(String val) {
        this.uniqueId = val;
    }

    /**
     *  Handles stuff related to the end of the call. For instance, generating
     *      call disposition information.
     */
    private void disposition() {
    }

    /**
     * Call is being recorded. Write info into database.
     */
    public void notifyStartRecording() {
    }

    /**
     * Call has stopped recording
     */
    public void notifyStopRecording() {
    }

    /**
     *  Adds a Key Value Pair (KVP) to callData. If key exists, it will overwrite
     *      it.<br>@param key: The name of the Key
     *  <br>@param value: The value to be assigned to Key
     */
    public void attach(String key, String value) {
        callData.put(key, value);
    }

    /**
     *  Removes a KVP from callData.
     */
    public void detach(String key) {
        callData.remove(key);
    }

    /**
     *  Synonym of notifyFinish()
     * @see notifyFinish()
     */
    public void notifyHangup() {
        notifyFinish();
    }

    /**
     *  Transfers the call.
     *  <br>@param dest: number to notifyTransfer the call to
     */
    public void notifyTransfer(String dest) {
    }

    public void notifyHold(String dest) {
    }

    /**
     * Handle the end of call. Typically, it would mean to log the call
     */
    public void notifyFinish() {
        timeEnd = Calendar.getInstance().getTime();
        long duration = (long) ((timeEnd.getTime() - timeStart.getTime()) / 1000);
        try {
            stmtUpdateFinish.clearParameters();
            stmtUpdateFinish.setString(1, "unknown");
            stmtUpdateFinish.setLong(2, duration);
            stmtUpdateFinish.setTimestamp(3, new Timestamp(timeEnd.getTime()));
            stmtUpdateFinish.setLong(4, (campaign != null) ? campaign.getId() : 0);
            stmtUpdateFinish.setLong(5, (contact != null) ? contact.getId() : 0);
            stmtUpdateFinish.setString(6, comments);
            stmtUpdateFinish.setLong(7, (agent != null) ? agent.getId() : 0);
            stmtUpdateFinish.setString(8, recording);
            stmtUpdateFinish.setLong(9, id);
            stmtUpdateFinish.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
        currentCalls.remove(uniqueId);
    }

    public AbstractCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(AbstractCampaign campaign) {
        this.campaign = campaign;
    }

    public String getRecordFile() {
        return recordFile;
    }

    public void setRecordFile(String val) {
        this.recordFile = val;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent val) {
        this.agent = val;
    }

    /**
     * Get a call by asterisk unique id
     * @param uniqueId Asterisk unique id
     * @return a Call instance
     */
    public static Call get(String uniqueId) {
        return currentCalls.get(uniqueId);
    }

    /**
     * Add a call to the list of current calls in the system.
     * @param uniqueId
     * @param call
     */
    public static void put(String uniqueId, Call call) {
        currentCalls.put(uniqueId, call);
    }

    public int getQPosition() {
        return qPosition;
    }

    public void setQPosition(int qPosition) {
        this.qPosition = qPosition;
    }

    public int getQTime() {
        return qTime;
    }

    public void setQTime(int qTime) {
        this.qTime = qTime;
    }

    public String getANI() {
        return ANI;
    }

    public void setANI(String ANI) {
        this.ANI = ANI;
    }

    public String getDNIS() {
        return DNIS;
    }

    public void setDNIS(String DNIS) {
        this.DNIS = DNIS;
    }

    public Date getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Date timeStart) {
        this.timeStart = timeStart;
    }

    public void setProducts(Map<Long, Product> products) {
        this.products = products;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Map<Long, Disposition> getDispositions() {
        return dispositions;
    }

    public void setDispositions(Map<Long, Disposition> dispositions) {
        this.dispositions = dispositions;
    }

    public String getCallingChannel() {
        return callingChannel;
    }

    public void setCallingChannel(String channel) {
        this.callingChannel = channel;
    }

    public String getCalledChannel() {
        return calledChannel;
    }

    public void setCalledChannel(String channel) {
        this.calledChannel = channel;
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

    public Date getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Date timeEnd) {
        this.timeEnd = timeEnd;
    }

    public Date getTalkEnd() {
        return talkEnd;
    }

    public void setTalkEnd(Date talkEnd) {
        this.talkEnd = talkEnd;
    }

    /**
     * Sets the moment when the agent stopped talking to current time
     */
    public void setTalkEnd() {
        this.talkEnd = Calendar.getInstance().getTime();
    }

    public Date getTalkStart() {
        return talkStart;
    }

    public void setTalkStart(Date talkStart) {
        this.talkStart = talkStart;
    }

    /**
     * Sets the moment when agent started talking to current time
     */
    public void setTalkStart() {
        this.talkStart = Calendar.getInstance().getTime();
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getChannel1() {
        return channel1;
    }

    public void setChannel1(String channel1) {
        this.channel1 = channel1;
    }

    public String getChannel2() {
        return channel2;
    }

    public void setChannel2(String channel2) {
        this.channel2 = channel2;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecording() {
        return recording;
    }

    public void setRecording(String recording) {
        this.recording = recording;
    }
}
