package synthdrivers.YamahaTX802;

import synthdrivers.YamahaDX7.common.DX7FamilyAdditionalVoiceSingleDriver;
import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import core.JSLFrame;
import core.Patch;

public class YamahaTX802AdditionalVoiceSingleDriver extends DX7FamilyAdditionalVoiceSingleDriver {

    public YamahaTX802AdditionalVoiceSingleDriver() {
        super(YamahaTX802AdditionalVoiceConstants.INIT_ADDITIONAL_VOICE, YamahaTX802AdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_PATCH_NUMBERS, YamahaTX802AdditionalVoiceConstants.SINGLE_ADDITIONAL_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.STORE_SINGLE_ADDITIONAL_VOICE_STRING);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaTX802SysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
