package gov.sns.tools.pvlogger;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.data.*;
import gov.sns.tools.ArrayTool;
import java.util.*;

/**
 * SimpleStateStore is an implementation of StateStore for testing purposes only.
 * An instance of SimpleStateStore publishes snapshot to the console.  It does not
 * provide persistent storage.
 *
 * @author  tap
 */
public class SimpleStateStore implements StateStore {

    protected static final String SNAPSHOT_TYPE = "bpm_test";

    /**
	 * Publish the channel snapshot by displaying its description to a console.
	 * @param snapshot The snapshot to publish
	 * @param machineId The id of the channel snapshot's associated machine snapshot
	 */
    public void publish(ChannelSnapshot snapshot, long machineId) {
        System.out.println(snapshot.getPV() + "\t  timestamp = " + snapshot.getTimestamp() + ", status = " + snapshot.getStatus() + ", severity = " + snapshot.getSeverity() + ", value = " + ArrayTool.asString(snapshot.getValue()));
    }

    /**
	 * Publish the machine snapshot by displaying its description to a console.
	 * @param machineSnapshot The machine snapshot to publish 
	 */
    public void publish(MachineSnapshot machineSnapshot) {
        System.out.println(machineSnapshot);
    }

    /**
	 * Fetch an array of valid logger types
	 * @return an array of available logger types
	 */
    public String[] fetchTypes() {
        return new String[] { SNAPSHOT_TYPE };
    }

    /**
	 * Fetch a channel group for the specified logger type
	 * @param type the logger type
	 * @return a channel group for the logger type which includes the type, description and the pvs to log
	 */
    public ChannelGroup fetchGroup(final String type) {
        String[] pvs = getPVsToLog();
        return new ChannelGroup(SNAPSHOT_TYPE, pvs, 3600);
    }

    /**
	 * Fetch the machine snapshot with the specified id.
	 * @param id The id which identifies the machine snapshot we wish to fetch.
	 * @return The machine snapshot with the specified id or nul if none could be found.
	 */
    public MachineSnapshot fetchMachineSnapshot(final long id) {
        return null;
    }

    /**
	 * Fetch the channel snapshots from the data source and populate the machine snapshot
	 * @param machineSnapshot The machine snapshot for which to fetch the channel snapshots and load them
	 * @return the machineSnapshot which is the same as the parameter returned for convenience
	 */
    public MachineSnapshot loadChannelSnapshotsInto(final MachineSnapshot machineSnapshot) {
        return null;
    }

    /**
	 * Fetch the machine snapshots within the specified time range.  If the type is not null,
	 * then restrict the machine snapshots to those of the specified type.  The machine snapshots
	 * do not include the channel snapshots.  A complete snapshot can be obtained using the 
	 * fetchMachineSnapshot(id) method.
	 * @param type The type of machine snapshots to fetch or null for no restriction
	 * @param startTime The start time of the time range
	 * @param endTime The end time of the time range
	 * @return An array of machine snapshots meeting the specified criteria
	 */
    public MachineSnapshot[] fetchMachineSnapshotsInRange(String type, Date startTime, Date endTime) {
        return null;
    }

    /**
	 * Get the array of pvs to log.
	 * @return the array of pvs to log.
	 */
    protected static String[] getPVsToLog() {
        Accelerator accelerator = XMLDataManager.loadDefaultAccelerator();
        List<String> pvs = new ArrayList<String>();
        List<AcceleratorNode> bpms = accelerator.getAllNodesOfType(BPM.s_strType);
        Iterator<AcceleratorNode> bpmIter = bpms.iterator();
        while (bpmIter.hasNext()) {
            BPM bpm = (BPM) bpmIter.next();
            pvs.add(bpm.getChannel(BPM.X_AVG_HANDLE).channelName());
            pvs.add(bpm.getChannel(BPM.Y_AVG_HANDLE).channelName());
        }
        System.out.println("Found " + pvs.size() + " PVs: ");
        System.out.println(pvs);
        String[] pvArray = new String[pvs.size()];
        return pvs.toArray(pvArray);
    }
}
