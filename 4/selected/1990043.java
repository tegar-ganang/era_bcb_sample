package org.jsynthlib.synthdrivers.YamahaTX802;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyMicroTuningBankDriver;

public class YamahaTX802MicroTuningBankDriver extends DX7FamilyMicroTuningBankDriver {

    public YamahaTX802MicroTuningBankDriver(final Device device) {
        super(device, YamahaTX802MicroTuningConstants.INIT_MICRO_TUNING, YamahaTX802MicroTuningConstants.BANK_MICRO_TUNING_PATCH_NUMBERS, YamahaTX802MicroTuningConstants.BANK_MICRO_TUNING_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MICRO_TUNING_CARTRIDGE_STRING);
        }
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaTX802SysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MEMORY_PROTECTION_STRING);
            }
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MICRO_TUNING_CARTRIDGE_STRING);
        }
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
