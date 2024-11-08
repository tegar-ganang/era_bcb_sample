package synthdrivers.YamahaDX7II;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingBankDriver;
import core.Patch;

public class YamahaDX7IIFractionalScalingBankDriver extends DX7FamilyFractionalScalingBankDriver {

    public YamahaDX7IIFractionalScalingBankDriver() {
        super(YamahaDX7IIFractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaDX7IIFractionalScalingConstants.BANK_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaDX7IIFractionalScalingConstants.BANK_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
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
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        YamahaDX7IISysexHelpers.chXmitBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
