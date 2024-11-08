package synthdrivers.YamahaDX7s;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilySystemSetupIIDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7sSystemSetupDriver extends DX7FamilySystemSetupIIDriver {

    public YamahaDX7sSystemSetupDriver() {
        super(YamahaDX7sSystemSetupConstants.INIT_SYSTEM_SETUP, YamahaDX7sSystemSetupConstants.SINGLE_SYSTEM_SETUP_PATCH_NUMBERS, YamahaDX7sSystemSetupConstants.SINGLE_SYSTEM_SETUP_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7sSysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }
}
