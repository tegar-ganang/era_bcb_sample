package org.jsynthlib.synthdrivers.YamahaTX7;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyVoiceBankDriver;

public class YamahaTX7VoiceBankDriver extends DX7FamilyVoiceBankDriver {

    public YamahaTX7VoiceBankDriver(final Device device) {
        super(device, YamahaTX7VoiceConstants.INIT_VOICE, YamahaTX7VoiceConstants.BANK_VOICE_PATCH_NUMBERS, YamahaTX7VoiceConstants.BANK_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            send(YamahaTX7SysexHelper.swOffMemProt.toSysexMessage(getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaTX7Strings.dxShowInformation(toString(), YamahaTX7Strings.MEMORY_PROTECTION_STRING);
            }
        }
        sendPatchWorker(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
