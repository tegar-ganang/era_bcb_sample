package synthdrivers.YamahaTX7;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyPerformanceBankDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaTX7PerformanceBankDriver extends DX7FamilyPerformanceBankDriver {

    public YamahaTX7PerformanceBankDriver() {
        super(YamahaTX7PerformanceConstants.INIT_PERFORMANCE, YamahaTX7PerformanceConstants.BANK_PERFORMANCE_PATCH_NUMBERS, YamahaTX7PerformanceConstants.BANK_PERFORMANCE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            send(YamahaTX7SysexHelper.swOffMemProt.toSysexMessage(getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }

    public JSLFrame editPatch(Patch p) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.EDIT_BANK_PERFORMANCE_STRING);
        }
        return super.editPatch(p);
    }
}