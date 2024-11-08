package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilyMicroTuningBankDriver;
import core.Patch;

public class YamahaDX7sMicroTuningBankDriver extends DX7FamilyMicroTuningBankDriver {

    public YamahaDX7sMicroTuningBankDriver() {
        super(YamahaDX7sMicroTuningConstants.INIT_MICRO_TUNING, YamahaDX7sMicroTuningConstants.BANK_MICRO_TUNING_PATCH_NUMBERS, YamahaDX7sMicroTuningConstants.BANK_MICRO_TUNING_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MICRO_TUNING_CARTRIDGE_STRING);
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7sSysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MICRO_TUNING_CARTRIDGE_STRING);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
