package org.jsynthlib.synthdrivers.YamahaTX802;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyFractionalScalingSingleDriver;

public class YamahaTX802FractionalScalingSingleDriver extends DX7FamilyFractionalScalingSingleDriver {

    public YamahaTX802FractionalScalingSingleDriver(final Device device) {
        super(device, YamahaTX802FractionalScalingConstants.INIT_FRACTIONAL_SCALING, YamahaTX802FractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_PATCH_NUMBERS, YamahaTX802FractionalScalingConstants.SINGLE_FRACTIONAL_SCALING_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.STORE_SINGLE_FRACTIONAL_SCALING_STRING);
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
            YamahaTX802Strings.dxShowInformation(toString(), YamahaTX802Strings.FRACTIONAL_SCALING_CARTRIDGE_STRING);
        }
        YamahaTX802SysexHelpers.chVoiceMode(this, (byte) (getChannel() + 0x10));
        setPatchNum(patchNum + 32 * bankNum);
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
