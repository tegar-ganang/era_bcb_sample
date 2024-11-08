package org.jsynthlib.synthdrivers.YamahaDX7s;

import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceBankDriver;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;

public class YamahaDX7sAdditionalVoiceBankDriver extends DX7FamilyAdditionalVoiceBankDriver {

    public YamahaDX7sAdditionalVoiceBankDriver(final Device device) {
        super(device, YamahaDX7sAdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaDX7sAdditionalVoiceConstants.BANK_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaDX7sAdditionalVoiceConstants.BANK_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7sSysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((YamahaDX7sDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MEMORY_PROTECTION_STRING);
            }
        }
        YamahaDX7sSysexHelpers.chRcvBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7sSysexHelpers.chXmitBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
