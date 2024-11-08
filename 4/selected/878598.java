package org.jsynthlib.synthdrivers.YamahaDX7II;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilySystemSetupIIDriver;

public class YamahaDX7IISystemSetupDriver extends DX7FamilySystemSetupIIDriver {

    public YamahaDX7IISystemSetupDriver(final Device device) {
        super(device, YamahaDX7IISystemSetupConstants.INIT_SYSTEM_SETUP, YamahaDX7IISystemSetupConstants.SINGLE_SYSTEM_SETUP_PATCH_NUMBERS, YamahaDX7IISystemSetupConstants.SINGLE_SYSTEM_SETUP_BANK_NUMBERS);
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
}
