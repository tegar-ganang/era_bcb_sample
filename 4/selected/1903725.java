package gov.sns.tools.pvlogger;

import java.sql.Timestamp;
import java.util.Date;

public class MachineSnapshotSP extends MachineSnapshot {

    protected ChannelSnapshot[] _channelSnapshotsSP;

    public MachineSnapshotSP(Date timestamp, String comment, ChannelSnapshot[] channelSnapshots, ChannelSnapshot[] channelSnapshotsSP) {
        super(timestamp, comment, channelSnapshots);
        _channelSnapshotsSP = channelSnapshotsSP;
    }

    public MachineSnapshotSP(long id, String type, Timestamp timestamp, String comment, ChannelSnapshot[] channelSnapshots, ChannelSnapshot[] channelSnapshotsSP) {
        super(id, type, timestamp, comment, channelSnapshots);
        _channelSnapshotsSP = channelSnapshotsSP;
    }

    public MachineSnapshotSP(int length) {
        super(length);
    }

    /**
 * Set the channel snapshot for the specified index.
 * @param index The index identifying the channel snapshot placeholder
 * @param channelSnapshot The channel snapshot to associate with this machine snapshot.
 */
    public void setChannelSnapshotSP(int index, ChannelSnapshot channelSnapshotSP) {
        _channelSnapshotsSP[index] = channelSnapshotSP;
    }

    /**
	 * Set the channel snapshots for the machine snapshot
	 * @param channelSnapshots The array of channel snapshots to associate with the machine snapshot
	 */
    void setChannelSnapshotsSP(final ChannelSnapshot[] channelSnapshotsSP) {
        _channelSnapshotsSP = channelSnapshotsSP;
    }

    /**
	 * Get the channel snapshots.
	 * @return The array of channel snapshots.
	 */
    public ChannelSnapshot[] getChannelSnapshotsSP() {
        return _channelSnapshotsSP;
    }

    /**
	 * Override toString() to get a textual description of the machine snapshot.
	 * @return a textual description of the machine snapshot.
	 */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("id: " + _id + "\n");
        buffer.append("type:  " + _type + "\n");
        buffer.append("Timestamp:  " + TIME_FORMAT.format(_timestamp) + "\n");
        buffer.append("Comment:  " + _comment + "\n");
        for (int index = 0; index < _channelSnapshots.length; index++) {
            ChannelSnapshot channelSnapshot = _channelSnapshots[index];
            ChannelSnapshot channelSnapshotSP = _channelSnapshotsSP[index];
            if (channelSnapshot != null) {
                buffer.append("ch:" + channelSnapshot + "\n");
                buffer.append("spch:" + channelSnapshotSP + "\n");
            }
        }
        return buffer.toString();
    }
}
