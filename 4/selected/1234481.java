package org.jsynthlib.synthdrivers.YamahaDX7;

import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyPerformanceSingleDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;

public class YamahaDX7PerformanceSingleDriver extends DX7FamilyPerformanceSingleDriver {

    public YamahaDX7PerformanceSingleDriver(final Device device) {
        super(device, YamahaDX7PerformanceConstants.INIT_PERFORMANCE, YamahaDX7PerformanceConstants.SINGLE_PERFORMANCE_PATCH_NUMBERS, YamahaDX7PerformanceConstants.SINGLE_PERFORMANCE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.STORE_SINGLE_PERFORMANCE_STRING);
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.PERFORMANCE_STRING);
        }
    }

    public JSLFrame editPatch(Patch p) {
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.PERFORMANCE_EDITOR_STRING);
            }
        }
        return super.editPatch(p);
    }
}
