package synthdrivers.YamahaTX7;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyVoiceSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaTX7VoiceSingleDriver extends DX7FamilyVoiceSingleDriver {

    public YamahaTX7VoiceSingleDriver() {
        super(YamahaTX7VoiceConstants.INIT_VOICE, YamahaTX7VoiceConstants.SINGLE_VOICE_PATCH_NUMBERS, YamahaTX7VoiceConstants.SINGLE_VOICE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.MEMORY_PROTECTION_STRING);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        setPatchNum(patchNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
