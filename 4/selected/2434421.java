package synthdrivers.YamahaDX7II;

import synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceBankDriver;
import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import core.Patch;

public class YamahaDX7IIAdditionalVoiceBankDriver extends DX7FamilyAdditionalVoiceBankDriver {

    public YamahaDX7IIAdditionalVoiceBankDriver() {
        super(YamahaDX7IIAdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaDX7IIAdditionalVoiceConstants.BANK_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaDX7IIAdditionalVoiceConstants.BANK_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7IISysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MEMORY_PROTECTION_STRING);
        }
        YamahaDX7IISysexHelpers.chRcvBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7IISysexHelpers.chXmitBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
