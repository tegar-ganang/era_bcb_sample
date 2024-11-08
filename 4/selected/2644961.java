package synthdrivers.AlesisQS;

import core.Driver;
import core.ErrorMsg;
import core.Patch;
import core.SysexHandler;

/**
 * AlesisQSMixDriver.java
 *
 * Mix program driver for Alesis QS series synths
 * Feb 2002
 * @author Zellyn Hunter (zellyn@bigfoot.com, zjh)
 * @version $Id: AlesisQSMixDriver.java 1182 2011-12-04 22:07:24Z chriswareham $
 * GPL v2
 */
public class AlesisQSMixDriver extends Driver {

    public AlesisQSMixDriver() {
        super("Mix", "Zellyn Hunter");
        sysexID = "F000000E0E**";
        sysexRequestDump = new SysexHandler("F0 00 00 0E 0E *opcode* *patchNum* F7");
        patchSize = QSConstants.PATCH_SIZE_MIX;
        deviceIDoffset = 0;
        ;
        checksumStart = 0;
        checksumEnd = 0;
        checksumOffset = 0;
        bankNumbers = QSConstants.WRITEABLE_BANK_NAMES;
        patchNumbers = QSConstants.PATCH_NUMBERS_MIX_WITH_EDIT_BUFFER;
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
        return SysexRoutines.getChars(((Patch) ip).sysex, QSConstants.HEADER, QSConstants.MIX_NAME_START, QSConstants.MIX_NAME_LENGTH);
    }

    /**
   * Set patch name in sysex buffer
   * @param p the patch to set the name in
   * @param name the string to set the name to
   */
    public void setPatchName(Patch p, String name) {
        SysexRoutines.setChars(name, ((Patch) p).sysex, QSConstants.HEADER, QSConstants.MIX_NAME_START, QSConstants.MIX_NAME_LENGTH);
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
   * Create a new mix patch
   * @return the new Patch
   */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        for (int i = 0; i < QSConstants.GENERIC_HEADER.length; i++) {
            sysex[i] = QSConstants.GENERIC_HEADER[i];
        }
        sysex[QSConstants.POSITION_OPCODE] = QSConstants.OPCODE_MIDI_USER_MIX_DUMP;
        sysex[QSConstants.POSITION_LOCATION] = 0;
        Patch p = new Patch(sysex, this);
        setPatchName(p, QSConstants.DEFAULT_NAME_MIX);
        return p;
    }

    /**
   * Copied from Driver.java by zjh.  Requests a patch dump.  Use opcode 0F - MIDI User Program Dump Request.
   * 100 corresponds to the Mix mode edit buffer
   * @param bankNum not used
   * @param patchNum the patch number, 0-100
   */
    public void requestPatchDump(int bankNum, int patchNum) {
        int location = patchNum;
        int opcode = QSConstants.OPCODE_MIDI_USER_MIX_DUMP_REQ;
        send(sysexRequestDump.toSysexMessage(getChannel(), new SysexHandler.NameValue("opcode", opcode), new SysexHandler.NameValue("patchNum", location)));
    }

    /**
   * Sends a patch to the synth's mix edit buffer.
   * @param p the patch to send to the edit buffer
   */
    public void sendPatch(Patch p) {
        storePatch(p, 0, QSConstants.MAX_LOCATION_MIX + 1);
    }

    /**
   * Sends a patch to a set location on a synth.  See comment for requestPatchDump for
   * explanation of patch numbers.  We save the old values, then set the
   * opcode and target location, then send it, then restore the old values
   * @param p the patch to send
   * @param bankNum ignored - you can only send to the User bank on Alesis QS synths
   * @param patchNum the patch number to send it to
   */
    public void storePatch(Patch p, int bankNum, int patchNum) {
        byte location = (byte) patchNum;
        byte opcode = QSConstants.OPCODE_MIDI_USER_MIX_DUMP;
        byte oldOpcode = ((Patch) p).sysex[QSConstants.POSITION_OPCODE];
        byte oldLocation = ((Patch) p).sysex[QSConstants.POSITION_LOCATION];
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = opcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = location;
        ErrorMsg.reportStatus(((Patch) p).sysex);
        sendPatchWorker(p);
        ((Patch) p).sysex[QSConstants.POSITION_OPCODE] = oldOpcode;
        ((Patch) p).sysex[QSConstants.POSITION_LOCATION] = oldLocation;
    }
}
