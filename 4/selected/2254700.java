package synthdrivers.YamahaDX7II;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7IIFractionalScalingSingleDriver extends DX7FamilyFractionalScalingSingleDriver {

    public YamahaDX7IIFractionalScalingSingleDriver() {
        super(YamahaDX7IIFractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaDX7IIFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaDX7IIFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.STORE_SINGLE_FRACTIONAL_SCALING_STRING);
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        YamahaDX7IISysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
