package org.jsynthlib.synthdrivers.YamahaTX7;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;

public class YamahaTX7VoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaTX7VoiceSingleDriver(final Device device) {
        super(device, YamahaTX7VoiceConstants.INIT_VOICE, YamahaTX7VoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaTX7VoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.MEMORY_PROTECTION_STRING);
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        setPatchNum(patchNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
