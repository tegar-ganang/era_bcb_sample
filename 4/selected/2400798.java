package org.jsynthlib.synthdrivers.YamahaDX7II;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyPerformanceIIBankDriver;

public class YamahaDX7IIPerformanceBankDriver extends DX7FamilyPerformanceIIBankDriver {

    public YamahaDX7IIPerformanceBankDriver(final Device device) {
        super(device, YamahaDX7IIPerformanceConstants.INIT_PERFORMANCE, YamahaDX7IIPerformanceConstants.BANK_PERFORMANCE_PATCH_NUMBERS, YamahaDX7IIPerformanceConstants.BANK_PERFORMANCE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7IISysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MEMORY_PROTECTION_STRING);
            }
        }
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        YamahaDX7IISysexHelpers.chXmitBlock(this, (byte) (getChannel() + 0x10), (byte) (bankNum));
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
