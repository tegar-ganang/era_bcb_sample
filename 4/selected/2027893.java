package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7sVoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaDX7sVoiceSingleDriver() {
        super(YamahaDX7sVoiceConstants.INIT_VOICE, YamahaDX7sVoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaDX7sVoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.STORE_SINGLE_VOICE_STRING);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7sSysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
