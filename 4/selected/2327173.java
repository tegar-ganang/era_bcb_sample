package synthdrivers.YamahaDX7II;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilySystemSetupIIDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaDX7IISystemSetupDriver extends DX7FamilySystemSetupIIDriver {

    public YamahaDX7IISystemSetupDriver() {
        super(YamahaDX7IISystemSetupConstants.INIT_SYSTEM_SETUP, YamahaDX7IISystemSetupConstants.SINGLE_SYSTEM_SETUP_PATCH_NUMBERS, YamahaDX7IISystemSetupConstants.SINGLE_SYSTEM_SETUP_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7IISysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaDX7IIStrings.dxShowInformation(toString(), YamahaDX7IIStrings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }
}
