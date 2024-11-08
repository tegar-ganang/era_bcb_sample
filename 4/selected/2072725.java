package org.jsynthlib.synthdrivers.YamahaDX7;

import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyDevice;
import org.jsynthlib.synthdrivers.YamahaDX7.common.DX7FamilyVoiceBankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Patch;

public class YamahaDX7VoiceBankDriver extends DX7FamilyVoiceBankDriver {

    public YamahaDX7VoiceBankDriver(final Device device) {
        super(device, YamahaDX7VoiceConstants.INIT_VOICE, YamahaDX7VoiceConstants.BANK_VOICE_PATCH_NUMBERS, YamahaDX7VoiceConstants.BANK_VOICE_BANK_NUMBERS);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSwOffMemProtFlag() & 0x01) == 1) {
            YamahaDX7SysexHelper.swOffMemProt(this, (byte) (getChannel() + 0x10), (byte) 0x21, (byte) 0x25);
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.MEMORY_PROTECTION_STRING);
            }
        }
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
            YamahaDX7SysexHelper.chBank(this, (byte) (getChannel() + 0x10), (byte) (0x25));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.RECEIVE_STRING);
            }
        }
        sendPatchWorker(p);
    }

    ;

    public void requestPatchDump(int bankNum, int patchNum) {
        if ((((DX7FamilyDevice) (getDevice())).getSPBPflag() & 0x01) == 1) {
            YamahaDX7SysexHelper.mkSysInfoAvail(this, (byte) (getChannel() + 0x10));
            YamahaDX7SysexHelper.xmitBankDump(this, (byte) (getChannel() + 0x10));
        } else {
            if ((((DX7FamilyDevice) (getDevice())).getTipsMsgFlag() & 0x01) == 1) {
                YamahaDX7Strings.dxShowInformation(toString(), YamahaDX7Strings.REQUEST_VOICE_STRING);
            }
        }
    }
}
