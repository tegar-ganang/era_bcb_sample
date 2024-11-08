package synthdrivers.YamahaDX7II;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyMicroTuningBankDriver;
import core.Patch;

public class YamahaDX7IIMicroTuningBankDriver extends DX7FamilyMicroTuningBankDriver {

    public YamahaDX7IIMicroTuningBankDriver() {
        super(YamahaDX7IIMicroTuningConstants.INIT_MICRO_TUNING, YamahaDX7IIMicroTuningConstants.BANK_MICRO_TUNING_PATCH_NUMBERS, YamahaDX7IIMicroTuningConstants.BANK_MICRO_TUNING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MICRO_TUNING_CARTRIDGE_STRING);
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7IISysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MICRO_TUNING_CARTRIDGE_STRING);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
