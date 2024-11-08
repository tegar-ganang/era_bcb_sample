package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingBankDriver;
import core.Patch;

public class YamahaDX7sFractionalScalingBankDriver extends DX7FamilyFractionalScalingBankDriver {

    public YamahaDX7sFractionalScalingBankDriver() {
        super(YamahaDX7sFractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaDX7sFractionalScalingConstants.BANK_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaDX7sFractionalScalingConstants.BANK_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7sSysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MEMORY_PROTECTION_STRING);
        }
        YamahaDX7sSysexHelpers.chRcvBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        YamahaDX7sSysexHelpers.chXmitBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
