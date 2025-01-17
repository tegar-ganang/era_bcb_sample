package synthdrivers.YamahaTX802;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingBankDriver;
import core.Patch;

public class YamahaTX802FractionalScalingBankDriver extends DX7FamilyFractionalScalingBankDriver {

    public YamahaTX802FractionalScalingBankDriver() {
        super(YamahaTX802FractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaTX802FractionalScalingConstants.BANK_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaTX802FractionalScalingConstants.BANK_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaTX802SysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MEMORY_PROTECTION_STRING);
        }
        YamahaTX802SysexHelpers.chBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        YamahaTX802SysexHelpers.chBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
