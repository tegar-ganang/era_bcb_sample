package synthdrivers.YamahaTX802;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyPerformanceIIIBankDriver;
import core.Patch;

public class YamahaTX802PerformanceBankDriver extends DX7FamilyPerformanceIIIBankDriver {

    public YamahaTX802PerformanceBankDriver() {
        super(YamahaTX802PerformanceConstants.INIT_PERFORMANCE, YamahaTX802PerformanceConstants.BANK_PERFORMANCE_PATCH_NUMBERS, YamahaTX802PerformanceConstants.BANK_PERFORMANCE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaTX802SysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }

    ;
}
