package org.jsynthlib.synthdrivers.AlesisQS;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * AlesisQSEffectsDriver.java
 *
 * Effects program driver for Alesis QS series synths
 * Feb 2002
 * @author Zellyn Hunter (zellyn@bigfoot.com, zjh)
 * @version $Id: AlesisQSEffectsDriver.java 1186 2011-12-19 14:11:46Z chriswareham $
 * GPL v2
 */
public class AlesisQSEffectsDriver extends Driver {

    public AlesisQSEffectsDriver(final Device device) {
        super(device, "Effects", "Zellyn Hunter");
        sysexID = "F000000E0E**";
        sysexRequestDump = new SysexHandler("F0 00 00 0E 0E *opcode* *patchNum* F7");
        patchSize = QSConstants.PATCH_SIZE_EFFECTS;
        deviceIDoffset = 0;
        checksumStart = 0;
        checksumEnd = 0;
        checksumOffset = 0;
        bankNumbers = QSConstants.WRITEABLE_BANK_NAMES;
        patchNumbers = QSConstants.PATCH_NUMBERS_EFFECTS_WITH_EDIT_BUFFERS;
    }

    /**
   * Print a byte in binary, for debugging packing/unpacking code
   **/
    public String toBinaryStr(byte b) {
        String output = new String();
        for (int i = 7; i >= 0; i--) {
            output += ((b >> i) & 1);
        }
        return output;
    }

    /**
   * Override the checksum and do nothing - the Alesis does not use checksums
   * @param p the ignored
   * @param start ignored
   * @param end ignored
   * @param ofs ignored
   */
    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
    }

    /**
   * Create a new effects patch
   * @return the new Patch
   */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        for (int i = 0; i < QSConstants.GENERIC_HEADER.length; i++) {
            sysex[i] = QSConstants.GENERIC_HEADER[i];
        }
        sysex[QSConstants.POSITION_OPCODE] = QSConstants.OPCODE_MIDI_USER_EFFECTS_DUMP;
        sysex[QSConstants.POSITION_LOCATION] = 0;
        return new Patch(sysex, this);
    }

    /**
   * Copied from Driver.java by zjh.  Requests a patch dump.  If the
   * patch number is 0..127, then use opcode 07 - MIDI User Effects
   * Dump Request.  If the patch number is 128..129, use opcode 09 -
   * MIDI Edit Effects Dump Request.  128 corresponds to the Program
   * mode effects edit buffer, and 129 to the Mix mode effects edit
   * buffer
   * @param bankNum not used
   * @param patchNum the patch number
   */
    public void requestPatchDump(int bankNum, int patchNum) {
        int location = patchNum;
        int opcode = QSConstants.OPCODE_MIDI_USER_EFFECTS_DUMP_REQ;
        if (location > QSConstants.MAX_LOCATION_PROG) {
            location -= (QSConstants.MAX_LOCATION_PROG + 1);
            opcode = QSConstants.OPCODE_MIDI_EDIT_EFFECTS_DUMP_REQ;
        }
        send(sysexRequestDump.toSysexMessage(getChannel(), new SysexHandler.NameValue("opcode", opcode), new SysexHandler.NameValue("patchNum", location)));
    }

    /**
   * Sends a patch to the synth's program effects edit buffer.
   * @param p the patch to send to the edit buffer
   */
    public void sendPatch(Patch p) {
        storePatch(p, 0, QSConstants.MAX_LOCATION_PROG + 1);
    }

    /**
   * Sends a patch to a set location on a synth.  See comment for requestPatchDump for
   * explanation of patch numbers > 127.  We save the old values, then set the
   * opcode and target location, then send it, then restore the old values
   * @param p the patch to send
   * @param bankNum ignored - you can only send to the User bank on Alesis QS synths
   * @param patchNum the patch number to send it to
   */
    public void storePatch(Patch p, int bankNum, int patchNum) {
        int location = patchNum;
        byte opcode = QSConstants.OPCODE_MIDI_USER_EFFECTS_DUMP;
        byte oldOpcode = ((Patch) p).sysex[QSConstants.POSITION_OPCODE];
        byte oldLocation = ((Patch) p).sysex[QSConstants.POSITION_LOCATION];
        if (location > QSConstants.MAX_LOCATION_PROG) {
            location -= (QSConstants.MAX_LOCATION_PROG + 1);
            opcode = QSConstants.OPCODE_MIDI_EDIT_EFFECTS_DUMP;
        }
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = opcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = (byte) location;
        Logger.reportStatus(((Patch) p).sysex);
        sendPatchWorker(p);
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = oldOpcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = oldLocation;
    }
}
