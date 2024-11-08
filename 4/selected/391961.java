package org.jsynthlib.synthdrivers.YamahaDX7s;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceSingleDriver;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;

public class YamahaDX7sAdditionalVoiceSingleDriver extends DX7FamilyAdditionalVoiceSingleDriver {

    public YamahaDX7sAdditionalVoiceSingleDriver(final Device device) {
        super(device, YamahaDX7sAdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaDX7sAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaDX7sAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.STORE_SINGLE_ADDITIONAL_VOICE_STRING);
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7sSysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
