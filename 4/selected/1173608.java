package org.jsynthlib.synthdrivers.YamahaDX7s;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingSingleDriver;

public class YamahaDX7sFractionalScalingSingleDriver extends DX7FamilyFractionalScalingSingleDriver {

    public YamahaDX7sFractionalScalingSingleDriver(final Device device) {
        super(device, YamahaDX7sFractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaDX7sFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaDX7sFractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.STORE_SINGLE_FRACTIONAL_SCALING_STRING);
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaDX7sStrings.dxShowInformation(toString(), YamahaDX7sStrings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        }
        YamahaDX7sSysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
