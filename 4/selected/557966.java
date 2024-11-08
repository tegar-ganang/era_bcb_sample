package net.sourceforge.mythtvj.mythtvprotocol;

import net.sourceforge.mythtvj.mythtvprotocol.util.recordingInformation40;
import net.sourceforge.mythtvj.mythtvprotocol.sql.SQLConnection;
import net.sourceforge.mythtvj.mythtvprotocol.net.Protocol40;
import net.sourceforge.mythtvj.mythtvprotocol.net.ProtocolException;
import net.sourceforge.mythtvj.mythtvprotocol.net.CommandSocket;
import java.sql.SQLException;
import java.util.Date;
import java.util.Vector;
import java.io.IOException;

/**
 * This class implements some commands of the mythtv protocol.
 * It is basicly an additional abstraction layer to make the communication with the backend more intuitionally.
 *
 * @author jjwin2k
 */
public class Commands40 extends Commands {

    /**
     * Creates a new session with the backend using the passed socket.
     * This method of initialization should only be used if it is garantued, that no sql queries will be made by this object.
     * @param con
     */
    public Commands40(CommandSocket con) {
        super(con);
    }

    /**
     * Creates a new session with the backend using the passed socket and the passed sql connection
     * @param con
     * @param dbCon
     */
    public Commands40(CommandSocket con, SQLConnection dbCon) {
        super(con, dbCon);
    }

    public void open(CONNECTION_MODE mode) throws IOException, ProtocolException {
        if (queryProtocolVersion() == Protocol40.PROTOCOL_VERSION) {
            switch(mode) {
                case PLAYBACK:
                    Protocol40.ANN(getCon(), Protocol40.ANN.PLAYBACK, Protocol40.getHostname(), "0");
                    break;
                case MONITOR:
                    Protocol40.ANN(getCon(), Protocol40.ANN.MONITOR, Protocol40.getHostname(), "0");
                    break;
                default:
                    throw new IllegalArgumentException("establishConnection: Unsupported mode");
            }
            this.connectionEstablished = true;
            this.connectionMode = mode;
        } else {
            throw new ProtocolException("Version differs.");
        }
    }

    public void close() throws ProtocolException, IOException {
        for (LiveTV tv : this.tvSessions) {
            tv.stop();
        }
        Protocol40.DONE(getCon());
        getCon().close();
    }

    /** Querys the free space on the backend and returns the size in KiB */
    public String queryFreeSpaceTotal() throws IOException, ProtocolException {
        String freeSpaceData[] = Protocol40.QUERY_FREE_SPACE_SUMMARY(getCon());
        return "" + (Long.decode(freeSpaceData[0]) - Long.decode(freeSpaceData[1]));
    }

    public int queryProtocolVersion() throws IOException, ProtocolException {
        return Protocol40.MYTH_PROTO_VERSION(getCon());
    }

    public boolean fileExists(String file) throws IOException, ProtocolException {
        return false;
    }

    /**
     * Queries the backend for available recorders.
     * @return A list of available recorders. (Their ID's)
     * @throws mythtvprotocol.IOException
     * @throws mythtvprotocol.ProtocolException
     */
    public Recorder[] getFreeRecorders() throws IOException, ProtocolException {
        String response[] = Protocol40.GET_FREE_RECORDER_LIST(getCon());
        Vector<Recorder> v = new Vector<Recorder>();
        for (String s : response) {
            v.add(getDbCon().getRecorderByID(Integer.decode(s)));
        }
        Recorder[] returnValue = new Recorder[v.size()];
        return v.toArray(returnValue);
    }

    /**
     * Queries the backend whether the recorder is currently recording.
     * @param recorderID
     * @return true if yes, false if not.
     * @throws mythtvprotocol.IOException
     * @throws mythtvprotocol.ProtocolException
     */
    public boolean isRecording(Recorder r) throws ProtocolException, IOException {
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.IS_RECORDING, r.getId())[0];
        if (response.equals("0")) {
            return false;
        } else if (response.equals("1")) {
            return true;
        } else {
            throw new ProtocolException("isRecording: Unexpected Response.");
        }
    }

    public String startLiveTV(Recorder recorder, Channel chan) throws IOException, ProtocolException {
        String chainID = Protocol40.getChainID();
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.SPAWN_LIVETV, recorder.getId(), chainID, Integer.toString(chan.getChannum()))[0];
        return chainID;
    }

    public Recorder getRecorderForSource(Source source) throws ProtocolException, IOException {
        Recorder[] recorders = getFreeRecorders();
        for (Recorder r : recorders) {
            if (r.getSource().equals(source)) {
                return r;
            }
        }
        return null;
    }

    public Recorded getCurrentRecording(Recorder recorder) throws ProtocolException, SQLException, IOException {
        String[] response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.GET_CURRENT_RECORDING, recorder.getId());
        int i = 0;
        recordingInformation40 ri = new recordingInformation40(response[i++], response[i++], response[i++], response[i++], getChannel(Integer.decode(response[i++])), response[i++], response[i++], response[i++], response[i++], Commands.decodeLong(response[i++], response[i++]), new Date(Long.decode(response[i++])), new Date(Long.decode(response[i++])), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), response[i++], Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), new Date(Long.decode(response[i++])), new Date(Long.decode(response[i++])), Integer.decode(response[i++]), Integer.decode(response[i++]), response[i++], Integer.decode(response[i++]), response[i++], response[i++], response[i++], Long.decode(response[i++]), Float.valueOf(response[i++]), response[i++], Integer.decode(response[i++]), response[i++], Integer.decode(response[i++]), Integer.decode(response[i++]), getDbCon().getStoragegroupByName(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]), Integer.decode(response[i++]));
        ri.getStoragegroup().setHostname(con.getBackendIP());
        ri.getStoragegroup().setDirname(ri.getPathname().substring(0, ri.getPathname().lastIndexOf('/')));
        return new Recorded(ri);
    }

    public Recorded[] getRecordings(GET_RECORDINGS_ORDER mode) throws SQLException {
        switch(mode) {
            case DESCENDING:
                return getDbCon().getRecordingsDescending();
            case NOW:
                return getDbCon().getRecordingsRecording();
            default:
                return getDbCon().getRecordings();
        }
    }

    public String stopLiveTV(Recorder recorder) throws IOException, ProtocolException {
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.STOP_LIVETV, recorder.getId())[0];
        return response;
    }

    public void channelUp(Recorder recorder) throws IOException, ProtocolException {
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.CHANGE_CHANNEL_UP, recorder.getId())[0];
        if (!response.equals("ok")) {
            throw new ProtocolException("channelUp: Command failed");
        }
    }

    public void channelDown(Recorder recorder) throws IOException, ProtocolException {
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.CHANGE_CHANNEL_DOWN, recorder.getId())[0];
        if (!response.equals("ok")) {
            throw new ProtocolException("channelUp: Command failed");
        }
    }

    /** 
     * Tunes the given recorder to the given channel.
     * @param recorderID The recorder to tune
     * @param channel The channel to tune to
     * @throws mythtvprotocol.IOException
     * @throws mythtvprotocol.ProtocolException
     */
    public void changeChannel(Recorder recorder, Channel channel) throws IOException, ProtocolException {
        String response = Protocol40.QUERY_RECORDER(getCon(), Protocol40.QUERY_RECORDER.SET_CHANNEL, recorder.getId(), "" + channel.getChannum())[0];
        if (!response.equalsIgnoreCase("ok")) {
            throw new ProtocolException("changeChannel: Unexpected Response.");
        }
    }

    public Channel[] getChannels() throws SQLException {
        return getDbCon().getListOfChannels();
    }

    public Program[] getProgram(Channel channel, Date from, int limit) throws SQLException {
        return getDbCon().getProgramForChannel(channel, new java.sql.Timestamp(from.getTime()), limit);
    }

    public Program[] getProgram(Channel channel, Date from, Date to) throws SQLException {
        return getDbCon().getProgramForChannel(channel, new java.sql.Timestamp(from.getTime()), new java.sql.Timestamp(to.getTime()));
    }

    public Channel getChannel(int channelid) throws SQLException {
        return getDbCon().getChannel(channelid);
    }

    public Channel getChannelByNumber(Recorder r, int channelNumber) throws SQLException {
        return getDbCon().getChannel(getDbCon().getChannelIDBYChannum(getDbCon().getSourceByRecorder(r.getId()), channelNumber));
    }

    public Recorder getRecorderByID(int id) throws SQLException {
        return getDbCon().getRecorderByID(id);
    }

    public Source getSourceByID(int id) {
        return getDbCon().getSourceByID(id);
    }

    public Source getSourceByName(String name) {
        return getDbCon().getSourceByName(name);
    }

    public int getProtocolVersion() {
        return 40;
    }
}
