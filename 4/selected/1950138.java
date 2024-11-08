package synthdrivers.YamahaDX5;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyPerformanceSingleDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX5PerformanceSingleDriver extends DX7FamilyPerformanceSingleDriver {

    public YamahaDX5PerformanceSingleDriver() {
        super(YamahaDX5PerformanceConstants.INIT_PERFORMANCE, YamahaDX5PerformanceConstants.SINGLE_PERFORMANCE_PATCH_NUMBERS, YamahaDX5PerformanceConstants.SINGLE_PERFORMANCE_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void sendPatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX5Strings.dxShowInformation(toString(), YamahaDX5Strings.SELECT_PATCH_STRING);
        sendPatchWorker(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatch(p, bankNum, patchNum);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX5Strings.dxShowInformation(toString(), YamahaDX5Strings.SELECT_PATCH_STRING);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
