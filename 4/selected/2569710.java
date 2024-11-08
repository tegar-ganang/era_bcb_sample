package org.jsynthlib.synthdrivers.YamahaTX7;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyPerformanceBankDriver;

public class YamahaTX7PerformanceBankDriver extends DX7FamilyPerformanceBankDriver {

    public YamahaTX7PerformanceBankDriver(final Device device) {
        super(device, YamahaTX7PerformanceConstants.INIT_PERFORMANCE, YamahaTX7PerformanceConstants.BANK_PERFORMANCE_PATCH_NUMBERS, YamahaTX7PerformanceConstants.BANK_PERFORMANCE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            send(YamahaTX7SysexHelper.swOffMemProt.toSysexMessage(getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.MEMORY_PROTECTION_STRING);
            }
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
