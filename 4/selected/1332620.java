package synthdrivers.YamahaTX7;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyPerformanceSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaTX7PerformanceSingleDriver extends DX7FamilyPerformanceSingleDriver {

    public YamahaTX7PerformanceSingleDriver() {
        super(YamahaTX7PerformanceConstants.INIT_PERFORMANCE, YamahaTX7PerformanceConstants.SINGLE_PERFORMANCE_PATCH_NUMBERS, YamahaTX7PerformanceConstants.SINGLE_PERFORMANCE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.STORE_SINGLE_PERFORMANCE_STRING);
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        setPatchNum(patchNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
