package org.jsynthlib.synthdrivers.YamahaDX5;

import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;

public class YamahaDX5VoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaDX5VoiceSingleDriver(final Device device) {
        super(device, YamahaDX5VoiceConstants.INIT_VOICE, YamahaDX5VoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaDX5VoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public void sendPatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX5Strings.dxShowInformation(toString(), YamahaDX5Strings.SELECT_PATCH_STRING);
        }
        sendPatchWorker(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatch(p, bankNum, patchNum);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX5Strings.dxShowInformation(toString(), YamahaDX5Strings.SELECT_PATCH_STRING);
        }
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
