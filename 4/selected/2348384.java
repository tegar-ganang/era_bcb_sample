package sipinspector;

import java.net.UnknownHostException;
import sipinspector.Utils.MD5Digest;
import sipinspector.Utils.SIPParser;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import sipinspector.ScenarioEntries.LabelEntry;
import sipinspector.ScenarioEntries.ScenarioEntry;
import sipinspector.ScenarioEntries.ScenarioException;
import sipinspector.Socket.MySocketAbstract;
import sipinspector.Utils.RemoteClient;
import sipinspector.Utils.SIValue;

/**
 *
 * @author Zarko Coklin
 */
public class SIPCall {

    public SIPCall(ScenarioProgressDialog dialog) {
        entryPos = 0;
        cseqValue = 1;
        callNumber = globalCallNumber++;
        VariablesMap = new HashMap<String, String>(10);
        callIDList = new ArrayList(2);
        callIDList.add(Integer.toString(callNumber));
        this.socket = dialog.getMainFrame().getSocket();
        key = socket.getInitialKey();
        remoteAddress = dialog.getMainFrame().getRemoteAddress();
        remotePort = dialog.getMainFrame().getRemotePort();
        callRewinded = false;
        this.dialog = dialog;
        pauseDuration = 0;
        pauseActivated = false;
        rowOfValues = dialog.getMainFrame().getValuesLoader().getRowOfValues();
        try {
            if (GlobalSettings.getSetting("RTP_Remote_Address") != null && GlobalSettings.getSetting("RTP_Remote_Port") != null) {
                remoteRTPClient = new RemoteClient(InetAddress.getByName(GlobalSettings.getSetting("RTP_Remote_Address")), Integer.parseInt(GlobalSettings.getSetting("RTP_Remote_Port")));
            } else {
                remoteRTPClient = new RemoteClient(null, 0);
            }
        } catch (UnknownHostException ex) {
            remoteRTPClient = new RemoteClient(null, 0);
        }
    }

    public SIPCall(ScenarioProgressDialog dialog, MyPacket packet) {
        entryPos = 0;
        cseqValue = 1;
        callIDList = new ArrayList(2);
        callIDList.add(SIPParser.getCallID(packet.getMessageTxt()));
        VariablesMap = new HashMap<String, String>(10);
        this.socket = dialog.getMainFrame().getSocket();
        remoteAddress = packet.getAddress();
        remotePort = packet.getPort();
        this.dialog = dialog;
        pauseDuration = 0;
        pauseActivated = false;
        rowOfValues = dialog.getMainFrame().getValuesLoader().getRowOfValues();
        remoteRTPClient = new RemoteClient(null, 0);
    }

    public void rewindClientScenario() {
        int totalNum = SIPScenario.getEntriesCount();
        ScenarioEntry entry;
        while (entryPos < totalNum - 1) {
            entry = SIPScenario.getScenarioPos(entryPos);
            if ((entry.getDirection() == ScenarioEntry.DIRECTION.OUT && entry.getOptionalFlag() == false) || entry.getType() == ScenarioEntry.TYPE.COMMAND_PLAYPCAP || entry.getType() == ScenarioEntry.TYPE.SET_REMOTE_TARGET) {
                break;
            } else {
                nextEntry();
                callRewinded = true;
            }
        }
    }

    public ScenarioEntry getScenarioPos() {
        return SIPScenario.getScenarioPos(entryPos);
    }

    public void increaseGlobalCallNumber() {
        globalCallNumber++;
    }

    public static int getGlobalCallCounter() {
        return globalCallNumber;
    }

    public ScenarioEntry getScenarioPos(int pos) {
        return SIPScenario.getScenarioPos(pos);
    }

    public void setScenarioPos(int value) {
        entryPos = value;
    }

    public int getCurrentScenarioPos() {
        return entryPos;
    }

    public void resetScenarioPos() {
        entryPos = 0;
    }

    public String getLastRcvdExpectedMsg() {
        return lastRcvdExpectedMsg;
    }

    public boolean processIncomingMsg(String rcvdMsg) throws ScenarioException {
        boolean rc = true;
        if (receivedExpectedMessage(rcvdMsg) == false) {
            rc = false;
            getScenarioPos().increaseUnexpectedCnt();
            dialog.updateStats(getScenarioPos());
        } else {
            lastRcvdExpectedMsg = rcvdMsg;
            setRemoteRTPClient(lastRcvdExpectedMsg);
            getScenarioPos().increaseExpectedCnt();
            dialog.updateStats(getScenarioPos());
            nextEntry();
        }
        return rc;
    }

    private boolean receivedExpectedMessage(String msg) {
        String received;
        int totalNum = SIPScenario.getEntriesCount();
        received = SIPParser.getMsgReqRespString(msg);
        int start = entryPos;
        while (true) {
            ScenarioEntry entry = getScenarioPos();
            switch(entry.getType()) {
                case REQUEST:
                case RESPONSE:
                    if (entry.getDirection() != ScenarioEntry.DIRECTION.IN && entry.getOptionalFlag() == false) {
                        return false;
                    }
                    if (entry.getShortCode().equals(received) == true) {
                        return true;
                    }
                    if (entry.getOptionalFlag() == false) {
                        return false;
                    }
                    break;
                case SET_REMOTE_TARGET:
                    {
                        return false;
                    }
                default:
                    break;
            }
            if (entryPos < totalNum - 1) {
                nextEntry();
            } else {
                break;
            }
        }
        entryPos = start;
        return false;
    }

    public String procesOutgoingMsg(String msg) {
        msg = replaceKeyWords(msg);
        msg = replaceAuthentication(msg);
        msg = updateContentLen(msg);
        return msg;
    }

    public LabelEntry gotoLabel(String labelName) {
        int totalNum = SIPScenario.getEntriesCount();
        int cnt = 0;
        while (cnt < totalNum) {
            ScenarioEntry entry = getScenarioPos(cnt);
            if (entry.getType() == ScenarioEntry.TYPE.LABEL) {
                LabelEntry labelEntry = (LabelEntry) entry;
                if (labelEntry.getName().equals(labelName) == true) {
                    entryPos = cnt;
                    return labelEntry;
                }
            }
            cnt++;
        }
        return null;
    }

    public String getAndStoreValue(String word) {
        int pos;
        String value;
        String oldValue = "null";
        String keyword;
        if ((pos = SIValue.findOperationSign(word)) == -1) {
            return VariablesMap.get(word);
        }
        keyword = word.substring(0, pos) + "]";
        oldValue = VariablesMap.get(keyword);
        if (oldValue != null) {
            try {
                if (word.charAt(pos) == '+') {
                    value = String.valueOf(Integer.parseInt(oldValue) + SIValue.getIntValue(word.substring(pos + 1)));
                } else {
                    value = String.valueOf(Integer.parseInt(oldValue) - SIValue.getIntValue(word.substring(pos + 1)));
                }
            } catch (NumberFormatException e) {
                return oldValue;
            }
            VariablesMap.put(keyword, value);
        }
        return oldValue;
    }

    public String replaceKeyWords(String msg) {
        int start = 0;
        int end = 0;
        int marker = 0;
        String keyword;
        String value;
        StringBuilder newMessage = new StringBuilder(2048);
        while (true) {
            start = msg.indexOf('[', end);
            if (start == -1) {
                break;
            }
            newMessage.append(msg.substring(end, start));
            end = msg.indexOf(']', start + 1);
            if (end == -1) {
                break;
            }
            marker = ++end;
            keyword = msg.substring(start, end);
            if (keyword.startsWith("[last_") == true) {
                String tmpStr = keyword.substring(6, keyword.length() - 1);
                newMessage.append(replaceLastKeywords(tmpStr));
            } else if (keyword.startsWith("[field") == true) {
                newMessage.append(dialog.getMainFrame().getValuesLoader().getFieldValue(rowOfValues, keyword));
            } else if (keyword.startsWith("[val_last_") == true) {
                String tmpStr = keyword.substring(10, keyword.length() - 1);
                newMessage.append(replaceValLastKeywords(tmpStr));
            } else if (keyword.equals("[call_number]") == true) {
                newMessage.append(getCallID());
            } else if (keyword.equals("[branch]") == true) {
                newMessage.append(replaceBranchKeyword());
            } else if (keyword.equals("[peer_tag_param]") == true) {
                newMessage.append(replacePeerTagParam());
            } else if (keyword.equals("[remote_ip]") == true) {
                newMessage.append(getRemoteAddress());
            } else if (keyword.equals("[remote_port]") == true) {
                newMessage.append(getRemotePort());
            } else if (keyword.startsWith("[cseq") == true) {
                if (keyword.charAt(5) == '+') {
                    cseqValue += SIValue.getIntValue(keyword.substring(6));
                } else if (keyword.charAt(5) == '-') {
                    cseqValue -= SIValue.getIntValue(keyword.substring(6));
                }
                newMessage.append(String.valueOf(cseqValue));
            } else if (keyword.charAt(1) == '$') {
                newMessage.append(getAndStoreValue(keyword));
            } else if (keyword.equals("[ver]") == true) {
                newMessage.append(SIPInspectorMainFrame.getSIPInspectorVersion());
            } else if (keyword.startsWith("[authentication username=")) {
                newMessage.append("[authentication username=");
                end = start + 25;
                marker = end;
            } else if ((value = GlobalSettings.KeyWordsMap.get(keyword)) != null) {
                newMessage.append(value);
            } else {
                newMessage.append(keyword);
            }
        }
        newMessage.append(msg.substring(marker));
        return newMessage.toString();
    }

    public String replaceLastKeywords(String keyword) {
        return SIPParser.grepString(lastRcvdExpectedMsg, keyword);
    }

    public String replaceValLastKeywords(String keyword) {
        return SIPParser.getHeaderValue(lastRcvdExpectedMsg, keyword);
    }

    private String replacePeerTagParam() {
        String header;
        if (lastRcvdExpectedMsg == null) {
            return "";
        }
        if (SIPParser.isMsgRequest(lastRcvdExpectedMsg) == false) {
            header = SIPParser.getHeader(lastRcvdExpectedMsg, "To", "t");
            return SIPParser.getParam(header, "tag");
        } else {
            header = SIPParser.getHeader(lastRcvdExpectedMsg, "From", "f");
            return SIPParser.getParam(header, "tag");
        }
    }

    private String replaceAuthentication(String msg) {
        int start;
        int end;
        String rc;
        String username;
        String password;
        String method;
        String authHdr;
        String ruri;
        String response;
        StringBuilder proxyAuthHdr = new StringBuilder(256);
        String old;
        start = msg.indexOf("[authentication ");
        if (start == -1) {
            return msg;
        }
        end = msg.indexOf(']', start);
        if (end == -1) {
            return msg;
        }
        end++;
        if (lastRcvdExpectedMsg != null && SIPParser.isMsgRequest(lastRcvdExpectedMsg) == false) {
            rc = SIPParser.getResponseCode(lastRcvdExpectedMsg);
            if (rc.equals("401") || rc.equals("407")) {
                old = msg.substring(start, end);
                username = SIPParser.getParamValue(old, "username");
                password = SIPParser.getParamValue(old, "password");
                method = SIPParser.getRequestMethod(msg);
                authHdr = SIPParser.grepString(lastRcvdExpectedMsg, "-Authenticate");
                ruri = SIPParser.getRURI(msg);
                MD5Digest md5 = new MD5Digest(authHdr, method, username, password, ruri);
                response = md5.calculateMD5DigestResponse();
                proxyAuthHdr.append("Digest " + "username=\"").append(username).append("\",realm=\"").append(md5.getRealm()).append("\"," + "nonce=\"").append(md5.getNonce()).append("\"," + "uri=\"").append(ruri).append("\"," + "response=\"").append(response).append("\"");
                String qop = md5.getQop();
                if (qop != null && qop.contains("auth") == true) {
                    proxyAuthHdr.append(",qop=auth");
                    proxyAuthHdr.append(",cnonce=\"").append(md5.getCnonce()).append("\",nc=00000001");
                }
                proxyAuthHdr.append(",algorithm=MD5");
                String opaque = md5.getOpaque();
                if (opaque != null && opaque.equals("") == false) {
                    proxyAuthHdr.append(",opaque=\"").append(md5.getOpaque()).append("\"");
                }
                msg = msg.replace(old, proxyAuthHdr);
            }
        }
        return msg;
    }

    private String replaceBranchKeyword() {
        return "z9hG4bK" + String.valueOf(globalCallNumber) + String.valueOf(entryPos);
    }

    public static String updateContentLen(String msg) {
        int totLen = msg.length();
        int contLen;
        int posStart = msg.indexOf("[len]");
        if (posStart == -1) {
            return msg;
        }
        posStart = msg.indexOf("\r\n\r\n", posStart);
        if (posStart == -1) {
            contLen = 0;
        } else {
            contLen = totLen - (posStart + 4);
        }
        msg = msg.replace("[len]", String.valueOf(contLen));
        return msg;
    }

    public void nextEntry() {
        entryPos++;
    }

    public boolean isEndReached() {
        int cnt = entryPos;
        boolean optionalFlag = true;
        if (entryPos == 0) {
            return false;
        } else if (entryPos >= SIPScenario.getEntriesCount()) {
            dialog.getRTPThread().delRemoteClient(remoteRTPClient);
            return true;
        } else if (callRewinded == false && getScenarioPos(cnt).getType() == ScenarioEntry.TYPE.REQUEST) {
            while (cnt > 0 && optionalFlag == true) {
                optionalFlag &= getScenarioPos(--cnt).getOptionalFlag();
            }
            return optionalFlag;
        }
        return false;
    }

    public void storeVariableValue(String variable, String value) {
        VariablesMap.put("[$" + variable + "]", value);
    }

    public void sendMsg(String msg) throws IOException {
        String outgoingMsg = procesOutgoingMsg(msg);
        String callId = SIPParser.getCallID(outgoingMsg);
        if (dialog.getCallDB().containsKey(callId) == false) {
            dialog.getCallDB().put(callId, this);
            setCallId(callId);
        }
        justSendMsg(outgoingMsg);
    }

    public void justSendMsg(String msg) throws IOException {
        lastMsgOut = new MyPacket(msg);
        lastMsgOut.setAddress(remoteAddress);
        lastMsgOut.setPort(remotePort);
        socket.send(lastMsgOut);
    }

    public void recvMsg() throws IOException {
        key.channel().register(socket.getSelector(), SelectionKey.OP_READ);
    }

    public void setPauseDuration(int newValue) {
        if (pauseActivated == false) {
            return;
        }
        pauseDuration = newValue;
        if (pauseDuration <= 0) {
            pauseActivated = false;
        }
        return;
    }

    public boolean getPauseActivated() {
        return pauseActivated;
    }

    public void setPauseActivated() {
        pauseActivated = true;
    }

    public int getPauseDuration() {
        return pauseDuration;
    }

    public String getCallID() {
        if (callIDList != null && callIDList.size() >= 1) {
            return callIDList.get(0);
        }
        return "null";
    }

    public ArrayList<String> getCallIDList() {
        return callIDList;
    }

    public SelectionKey getChannelKey() {
        return key;
    }

    private void setRemoteRTPClient(String msg) {
        InetAddress remAddress = getRTPAddress(msg);
        int remPort = getRTPPort(msg);
        if (remAddress != null && remPort != 0) {
            if (remAddress != remoteRTPClient.getRemAddress()) {
                remoteRTPClient.setRemAddress(remAddress);
            }
            if (remPort != remoteRTPClient.getRemPort()) {
                remoteRTPClient.setRemPort(remPort);
            }
        }
    }

    public RemoteClient getRemoteClient() {
        return remoteRTPClient;
    }

    private InetAddress getRTPAddress(String msg) {
        String rtpAddr = SIPParser.getStarExpression(msg, "c=IN IP4 *" + "\r\n");
        InetAddress rtpAddress = null;
        int pos;
        if (rtpAddr == null || rtpAddr.equals("") == true) {
            return rtpAddress;
        }
        pos = rtpAddr.lastIndexOf('/');
        try {
            if (pos > 0) {
                rtpAddress = InetAddress.getByName(rtpAddr.substring(0, pos));
            } else {
                rtpAddress = InetAddress.getByName(rtpAddr);
            }
        } catch (UnknownHostException ex) {
            return rtpAddress;
        }
        return rtpAddress;
    }

    private int getRTPPort(String msg) {
        String rtpPrt = SIPParser.getStarExpression(msg, "m=audio * ");
        int rtpPort = 0;
        if (rtpPrt == null || rtpPrt.equals("") == true) {
            return rtpPort;
        }
        try {
            rtpPort = Integer.parseInt(rtpPrt);
        } catch (NumberFormatException exc) {
            return rtpPort;
        }
        return rtpPort;
    }

    public void setCallId(String callId) {
        this.callIDList.add(callId);
    }

    public MyPacket getLastMsgOut() {
        return lastMsgOut;
    }

    public void setRemoteAddress(InetAddress address) {
        remoteAddress = address;
    }

    public String getRemoteAddress() {
        return remoteAddress.getHostAddress();
    }

    public void setRemotePort(int port) {
        remotePort = port;
    }

    public String getRemotePort() {
        return Integer.toString(remotePort);
    }

    public void storeCallID(String callId) {
    }

    private int entryPos;

    private static int globalCallNumber = 1;

    private int cseqValue;

    private String lastRcvdExpectedMsg;

    private HashMap<String, String> VariablesMap;

    private ArrayList<String> callIDList;

    private MySocketAbstract socket;

    private SelectionKey key;

    private InetAddress remoteAddress;

    private int remotePort;

    private ScenarioProgressDialog dialog;

    private boolean callRewinded;

    private int pauseDuration;

    private boolean pauseActivated;

    private int callNumber;

    private SIValue[] rowOfValues;

    private RemoteClient remoteRTPClient;

    private MyPacket lastMsgOut;
}
