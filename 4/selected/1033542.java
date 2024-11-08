package org.jsynthlib.synthdrivers.YamahaDX7II;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceSingleDriver;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;

public class YamahaDX7IIAdditionalVoiceSingleDriver extends DX7FamilyAdditionalVoiceSingleDriver {

    public YamahaDX7IIAdditionalVoiceSingleDriver(final Device device) {
        super(device, YamahaDX7IIAdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaDX7IIAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaDX7IIAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.STORE_SINGLE_ADDITIONAL_VOICE_STRING);
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7IISysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
