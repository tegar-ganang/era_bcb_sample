package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7sFractionalScalingSingleDriver extends DX7FamilyFractionalScalingSingleDriver {

    public YamahaDX7sFractionalScalingSingleDriver() {
        super(YamahaDX7sFractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaDX7sFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaDX7sFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.STORE_SINGLE_FRACTIONAL_SCALING_STRING);
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        YamahaDX7sSysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
