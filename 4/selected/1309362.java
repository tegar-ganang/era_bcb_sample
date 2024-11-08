package synthdrivers.YamahaTX802;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaTX802VoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaTX802VoiceSingleDriver() {
        super(YamahaTX802VoiceConstants.INIT_VOICE, YamahaTX802VoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaTX802VoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.STORE_SINGLE_VOICE_STRING);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaTX802SysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
