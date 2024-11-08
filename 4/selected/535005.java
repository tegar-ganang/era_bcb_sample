package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceSingleDriver;
import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7sAdditionalVoiceSingleDriver extends DX7FamilyAdditionalVoiceSingleDriver {

    public YamahaDX7sAdditionalVoiceSingleDriver() {
        super(YamahaDX7sAdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaDX7sAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaDX7sAdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.STORE_SINGLE_ADDITIONAL_VOICE_STRING);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7sSysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
