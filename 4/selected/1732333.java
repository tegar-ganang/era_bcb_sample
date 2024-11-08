package synthdrivers.YamahaTX802;

import synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import synthdrivers.YamahaDX7.common.DX7FamilySystemSetupIIIDriver;
import core.JSLFrame;
import core.Patch;

public class YamahaTX802SystemSetupDriver extends DX7FamilySystemSetupIIIDriver {

    public YamahaTX802SystemSetupDriver() {
        super(YamahaTX802SystemSetupConstants.INIT_SYSTEM_SETUP, YamahaTX802SystemSetupConstants.SINGLE_SYSTEM_SETUP_PATCH_NUMBERS, YamahaTX802SystemSetupConstants.SINGLE_SYSTEM_SETUP_BANK_NUMBERS);
    }

    public Patch createNewPatch() {
        return super.createNewPatch();
    }

    public JSLFrame editPatch(Patch p) {
        return super.editPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaTX802SysexHelpers.swOffMemProt(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.MEMORY_PROTECTION_STRING);
        }
        sendPatchWorker(p);
    }
}
