package com.frinika.client.frinika;

import java.util.ArrayList;
import java.util.List;
import com.frinika.ejb.VoiceInfo;
import com.frinika.sequencer.patchname.MyPatch;

public class MidiDeviceVoiceInfo extends VoiceInfo {

    private static final long serialVersionUID = 1L;

    Long deviceID;

    int channel;

    int patchCode;

    public MidiDeviceVoiceInfo(Long deviceID, int midiChannel, int patchCode) {
        this.deviceID = deviceID;
        this.channel = midiChannel;
        this.patchCode = patchCode;
    }

    @Override
    public List<Long> getResourceList() {
        List<Long> list = new ArrayList<Long>();
        if (deviceID != null) {
            list.add(deviceID);
        }
        return list;
    }

    public int getChannel() {
        return channel;
    }

    public MyPatch getMyPatch() {
        return new MyPatch(patchCode);
    }
}
