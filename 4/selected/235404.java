package synthdrivers.AlesisQS;

import core.Driver;
import core.ErrorMsg;
import core.JSLFrame;
import core.Patch;
import core.SysexHandler;

public class AlesisQSProgramDriver extends Driver {

    /** The offset at which program data starts.  In other words,
   *   when the Alesis specification says data starts at offset n,
   *   it will be found at sysex buffer[dataoffset + n].
   */
    final int dataoffset = 7;

    public AlesisQSProgramDriver() {
        super("Program", "Zellyn Hunter/Chris Halls");
        sysexID = "F000000E0E**";
        sysexRequestDump = new SysexHandler("F0 00 00 0E 0E *opcode* *patchNum* F7");
        patchSize = QSConstants.PATCH_SIZE_PROGRAM;
        deviceIDoffset = 0;
        ;
        checksumStart = 0;
        checksumEnd = 0;
        checksumOffset = 0;
        bankNumbers = QSConstants.WRITEABLE_BANK_NAMES;
        patchNumbers = QSConstants.PATCH_NUMBERS_PROGRAM_WITH_EDIT_BUFFERS;
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
   * Get patch name from sysex buffer
   * @param ip the patch to get the name from
   * @return the name of the patch
   */
    public String getPatchName(Patch ip) {
        return SysexRoutines.getChars(((Patch) ip).sysex, QSConstants.HEADER, QSConstants.PROG_NAME_START, QSConstants.PROG_NAME_LENGTH);
    }

    /**
   * Set patch name in sysex buffer
   * @param p the patch to set the name in
   * @param name the string to set the name to
   */
    public void setPatchName(Patch p, String name) {
        SysexRoutines.setChars(name, ((Patch) p).sysex, QSConstants.HEADER, QSConstants.PROG_NAME_START, QSConstants.PROG_NAME_LENGTH);
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
   * Create a new program patch
   * @return the new Patch
   */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        for (int i = 0; i < QSConstants.GENERIC_HEADER.length; i++) {
            sysex[i] = QSConstants.GENERIC_HEADER[i];
        }
        sysex[QSConstants.POSITION_OPCODE] = QSConstants.OPCODE_MIDI_USER_PROG_DUMP;
        sysex[QSConstants.POSITION_LOCATION] = 0;
        Patch p = new Patch(sysex, this);
        setPatchName(p, QSConstants.DEFAULT_NAME_PROG);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return new AlesisQSProgramEditor((Patch) p);
    }

    /**
   * Copied from Driver.java by zjh.  Requests a patch dump.  If the patch number is 0..127, then
   * use opcode 01 - MIDI User Program Dump Request.  If the patch number is 128..144, use
   * opcode 03 - MIDI Edit Program Dump Request.  128 corresponds to the Program mode edit buffer,
   * and 129..144 to Mix mode edit buffers 0..15
   * @param bankNum not used
   * @param patchNum the patch number
   */
    public void requestPatchDump(int bankNum, int patchNum) {
        int location = patchNum;
        int opcode = QSConstants.OPCODE_MIDI_USER_PROG_DUMP_REQ;
        if (location > QSConstants.MAX_LOCATION_PROG) {
            location -= (QSConstants.MAX_LOCATION_PROG + 1);
            opcode = QSConstants.OPCODE_MIDI_EDIT_PROG_DUMP_REQ;
        }
        send(sysexRequestDump.toSysexMessage(getChannel(), new SysexHandler.NameValue("opcode", opcode), new SysexHandler.NameValue("patchNum", location)));
    }

    /**
   * Sends a patch to the synth's edit buffer.
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
        byte opcode = QSConstants.OPCODE_MIDI_USER_PROG_DUMP;
        byte oldOpcode = ((Patch) p).sysex[QSConstants.POSITION_OPCODE];
        byte oldLocation = ((Patch) p).sysex[QSConstants.POSITION_LOCATION];
        if (location > QSConstants.MAX_LOCATION_PROG) {
            location -= (QSConstants.MAX_LOCATION_PROG + 1);
            opcode = QSConstants.OPCODE_MIDI_EDIT_PROG_DUMP;
        }
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = opcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = (byte) location;
        ErrorMsg.reportStatus(((Patch) p).sysex);
        sendPatchWorker(p);
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = oldOpcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = oldLocation;
    }
}
