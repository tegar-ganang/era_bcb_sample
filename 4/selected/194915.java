package com.dalsemi.onewire.container;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.debug.Debug;
import com.dalsemi.onewire.utils.Convert;

/**
  * Memory bank class for the DS1961S/DS2432.
  *
  *  @version 	0.00, 19 Dec 2000
  *  @author 	DS
  */
public class MemoryBankSHAEE implements PagedMemoryBank {

    /** turn on extra debugging output */
    private static final boolean DEBUG = false;

    /** Read Memory Command */
    public static final byte READ_MEMORY = (byte) 0xF0;

    /** Read Authenticate Page */
    public static final byte READ_AUTH_PAGE = (byte) 0xA5;

    /**
     * Check the status of the memory page.
     */
    protected boolean checked;

    /**
     * Size of memory bank in bytes
     */
    protected int size;

    /**
    * Memory bank descriptions
    */
    protected String bankDescription;

    /**
    * Memory bank usage flags
    */
    protected boolean generalPurposeMemory;

    /**
    * Flag if memory bank is read/write
    */
    protected boolean readWrite;

    /**
    * Flag if memory bank is write once (EPROM)
    */
    protected boolean writeOnce;

    /**
    * Flag if memory bank is read only
    */
    protected boolean readOnly;

    /**
    * Starting physical address in memory bank.  Needed for different
    * types of memory in the same logical memory bank.  This can be
    * used to seperate them into two virtual memory banks.  Example:
    * DS2406 status page has mixed EPROM and Volatile RAM.
    */
    protected int startPhysicalAddress;

    /**
    * Flag if read back verification is enabled in 'write()'.
    */
    protected boolean writeVerification;

    /**
    * Number of pages in memory bank
    */
    protected int numberPages;

    /**
    *  page length in memory bank
    */
    protected int pageLength;

    /**
    * Flag if reading a page in memory bank provides optional
    * extra information (counter, tamper protection, SHA-1...)
    */
    protected boolean extraInfo;

    /**
    * Extra information length in bytes
    */
    protected int extraInfoLength;

    /**
    * Max data length in page packet in memory bank
    */
    protected int maxPacketDataLength;

    /**
    * Flag if memory bank has page CRC.
    */
    protected boolean pageCRC;

    /**
    * Reference to the OneWireContainer this bank resides on.
    */
    protected OneWireContainer33 ib = null;

    /**
    * Reference to the adapter the OneWireContainer resides on.
    */
    protected DSPortAdapter adapter = null;

    /**
    * Flag to indicate that speed needs to be set
    */
    protected boolean doSetSpeed;

    /**
    * block of 0xFF's used for faster read pre-fill of 1-Wire blocks
    * Comes from OneWireContainer33 that this MemoryBank references.
    */
    protected static final byte[] ffBlock = OneWireContainer33.ffBlock;

    /**
    * block of 0x00's used for faster read pre-fill of 1-Wire blocks
    * Comes from OneWireContainer33 that this MemoryBank references.
    */
    protected static final byte[] zeroBlock = OneWireContainer33.zeroBlock;

    protected MemoryBankScratchSHAEE scratchpad;

    /**
    * Memory bank constructor.  Requires reference to the OneWireContainer
    * this memory bank resides on.
    */
    public MemoryBankSHAEE(OneWireContainer33 ibutton, MemoryBankScratchSHAEE scratch) {
        ib = ibutton;
        scratchpad = scratch;
        adapter = ib.getAdapter();
        doSetSpeed = true;
    }

    /**
    * Query to see get a string description of the current memory bank.
    *
    * @return  String containing the memory bank description
    */
    public String getBankDescription() {
        return bankDescription;
    }

    /**
    * Query to see if the current memory bank is general purpose
    * user memory.  If it is NOT then it is Memory-Mapped and writing
    * values to this memory will affect the behavior of the 1-Wire
    * device.
    *
    * @return  'true' if current memory bank is general purpose
    */
    public boolean isGeneralPurposeMemory() {
        return generalPurposeMemory;
    }

    /**
    * Query to see if current memory bank is read/write.
    *
    * @return  'true' if current memory bank is read/write
    */
    public boolean isReadWrite() {
        return readWrite;
    }

    /**
    * Query to see if current memory bank is write write once such
    * as with EPROM technology.
    *
    * @return  'true' if current memory bank can only be written once
    */
    public boolean isWriteOnce() {
        return writeOnce;
    }

    /**
    * Query to see if current memory bank is read only.
    *
    * @return  'true' if current memory bank can only be read
    */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
    * Query to see if current memory bank non-volatile.  Memory is
    * non-volatile if it retains its contents even when removed from
    * the 1-Wire network.
    *
    * @return  'true' if current memory bank non volatile.
    */
    public boolean isNonVolatile() {
        return true;
    }

    /**
    * Query to see if current memory bank pages need the adapter to
    * have a 'ProgramPulse' in order to write to the memory.
    *
    * @return  'true' if writing to the current memory bank pages
    *                 requires a 'ProgramPulse'.
    */
    public boolean needsProgramPulse() {
        return false;
    }

    /**
    * Query to see if current memory bank pages need the adapter to
    * have a 'PowerDelivery' feature in order to write to the memory.
    *
    * @return  'true' if writing to the current memory bank pages
    *                 requires 'PowerDelivery'.
    */
    public boolean needsPowerDelivery() {
        return true;
    }

    /**
    * Query to get the starting physical address of this bank.  Physical
    * banks are sometimes sub-divided into logical banks due to changes
    * in attributes.
    *
    * @return  physical starting address of this logical bank.
    */
    public int getStartPhysicalAddress() {
        return startPhysicalAddress;
    }

    /**
    * Query to get the memory bank size in bytes.
    *
    * @return  memory bank size in bytes.
    */
    public int getSize() {
        return size;
    }

    /**
    * Set the write verification for the 'write()' method.
    *
    * @param  doReadVerf   true (default) verify write in 'write'
    *                      false, don't verify write (used on
    *                      Write-Once bit manipulation)
    */
    public void setWriteVerification(boolean doReadVerf) {
        writeVerification = doReadVerf;
    }

    /**
    * Query to get the number of pages in current memory bank.
    *
    * @return  number of pages in current memory bank
    */
    public int getNumberPages() {
        return numberPages;
    }

    /**
    * Query to get  page length in bytes in current memory bank.
    *
    * @return   page length in bytes in current memory bank
    */
    public int getPageLength() {
        return pageLength;
    }

    /**
    * Query to see if current memory bank pages can be read with
    * the contents being verified by a device generated CRC.
    * This is used to see if the 'ReadPageCRC()' can be used.
    *
    * @return  'true' if current memory bank can be read with self
    *          generated CRC.
    */
    public boolean hasPageAutoCRC() {
        return pageCRC;
    }

    /**
    * Query to see if current memory bank pages when read deliver
    * extra information outside of the normal data space.  Examples
    * of this may be a redirection byte, counter, tamper protection
    * bytes, or SHA-1 result.  If this method returns true then the
    * methods 'ReadPagePacket()' and 'readPageCRC()' with 'extraInfo'
    * parameter can be used.
    *
    * @return  'true' if reading the current memory bank pages
    *                 provides extra information.
    *
    * @deprecated  As of 1-Wire API 0.01, replaced by {@link #hasExtraInfo()}
    */
    public boolean haveExtraInfo() {
        return extraInfo;
    }

    /**
    * Checks to see if this memory bank's pages deliver extra
    * information outside of the normal data space,  when read.  Examples
    * of this may be a redirection byte, counter, tamper protection
    * bytes, or SHA-1 result.  If this method returns true then the
    * methods with an 'extraInfo' parameter can be used:
    * {@link #readPage(int,boolean,byte[],int,byte[]) readPage},
    * {@link #readPageCRC(int,boolean,byte[],int,byte[]) readPageCRC}, and
    * {@link #readPagePacket(int,boolean,byte[],int,byte[]) readPagePacket}.
    *
    * @return  <CODE> true </CODE> if reading the this memory bank's
    *                 pages provides extra information
    *
    * @see #readPage(int,boolean,byte[],int,byte[]) readPage(extra)
    * @see #readPageCRC(int,boolean,byte[],int,byte[]) readPageCRC(extra)
    * @see #readPagePacket(int,boolean,byte[],int,byte[]) readPagePacket(extra)
    * @since 1-Wire API 0.01
    */
    public boolean hasExtraInfo() {
        return extraInfo;
    }

    /**
    * Query to get the length in bytes of extra information that
    * is read when read a page in the current memory bank.  See
    * 'hasExtraInfo()'.
    *
    * @return  number of bytes in Extra Information read when reading
    *          pages in the current memory bank.
    */
    public int getExtraInfoLength() {
        return extraInfoLength;
    }

    /**
    * Query to get a string description of what is contained in
    * the Extra Informationed return when reading pages in the current
    * memory bank.  See 'hasExtraInfo()'.
    *
    * @return string describing extra information.
    */
    public String getExtraInfoDescription() {
        return "The MAC for the SHA Engine";
    }

    /**
    * Query to get Maximum data page length in bytes for a packet
    * read or written in the current memory bank.  See the 'ReadPagePacket()'
    * and 'WritePagePacket()' methods.  This method is only usefull
    * if the current memory bank is general purpose memory.
    *
    * @return  max packet page length in bytes in current memory bank
    */
    public int getMaxPacketDataLength() {
        return maxPacketDataLength;
    }

    /**
    * Read  memory in the current bank with no CRC checking (device or
    * data). The resulting data from this API may or may not be what is on
    * the 1-Wire device.  It is recommends that the data contain some kind
    * of checking (CRC) like in the readPagePacket() method or have
    * the 1-Wire device provide the CRC as in readPageCRC().  readPageCRC()
    * however is not supported on all memory types, see 'hasPageAutoCRC()'.
    * If neither is an option then this method could be called more
    * then once to at least verify that the same thing is read consistantly.
    *
    * @param  startAddr     starting address, relative to physical address for
    *                       this memory bank.
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       read() continious where the last one led off
    *                       and it is inside a 'beginExclusive/endExclusive'
    *                       block.
    * @param  readBuf       byte array to place read data into
    * @param  offset        offset into readBuf to place data
    * @param  len           length in bytes to read
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void read(int startAddr, boolean readContinue, byte[] readBuf, int offset, int len) throws OneWireIOException, OneWireException {
        if (DEBUG) {
            Debug.debug("-----------------------------------------------------------");
            Debug.debug("MemoryBankSHAEE.read(int, boolean, byte[], int, int) called");
            Debug.debug("  startAddr=0x" + Convert.toHexString((byte) startAddr));
            Debug.debug("  readContinue=" + readContinue);
            Debug.debug("  offset=" + offset);
            Debug.debug("  len=" + len);
            Debug.debug("  this.startPhysicalAddress=0x" + Convert.toHexString((byte) startPhysicalAddress));
            Debug.debug("  this.pageLength=" + this.pageLength);
            Debug.debug("  this.numberPages=" + this.numberPages);
            Debug.stackTrace();
        }
        if (!readContinue && ib.adapterSet()) checkSpeed();
        if ((startAddr + len) > (pageLength * numberPages)) throw new OneWireException("Read exceeds memory bank end.");
        if (!readContinue) {
            if (!adapter.select(ib.getAddress())) throw new OneWireIOException("Device select failed.");
            int addr = startAddr + startPhysicalAddress;
            byte[] raw_buf = new byte[3];
            raw_buf[0] = READ_MEMORY;
            raw_buf[1] = (byte) (addr & 0xFF);
            raw_buf[2] = (byte) (((addr & 0xFFFF) >>> 8) & 0xFF);
            adapter.dataBlock(raw_buf, 0, 3);
            if (DEBUG) Debug.debug("  raw_buf", raw_buf, 0, 3);
        }
        int pgs = len / pageLength;
        int extra = len % pageLength;
        for (int i = 0; i < pgs; i++) System.arraycopy(ffBlock, 0, readBuf, offset + i * pageLength, pageLength);
        System.arraycopy(ffBlock, 0, readBuf, offset + pgs * pageLength, extra);
        adapter.dataBlock(readBuf, offset, len);
        if (DEBUG) {
            Debug.debug("  readBuf", readBuf, offset, len);
            Debug.debug("-----------------------------------------------------------");
        }
    }

    /**
    * Write  memory in the current bank.  It is recommended that
    * when writing  data that some structure in the data is created
    * to provide error free reading back with read().  Or the
    * method 'writePagePacket()' could be used which automatically
    * wraps the data in a length and CRC.
    *
    * When using on Write-Once devices care must be taken to write into
    * into empty space.  If write() is used to write over an unlocked
    * page on a Write-Once device it will fail.  If write verification
    * is turned off with the method 'setWriteVerification(false)' then
    * the result will be an 'AND' of the existing data and the new data.
    *
    * @param  startAddr     starting address, relative to the starting physical address
    *                       of this memory bank
    * @param  writeBuf      byte array containing data to write
    * @param  offset        offset into writeBuf to get data
    * @param  len           length in bytes to write
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void write(int startAddr, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
        if (DEBUG) {
            Debug.debug("-----------------------------------------------------------");
            Debug.debug("MemoryBankSHAEE.write(int,byte[],int,int) called");
            Debug.debug("  startAddr=0x" + Convert.toHexString((byte) startAddr));
            Debug.debug("  writeBuf", writeBuf, offset, len);
            Debug.debug("  startPhysicalAddress=0x" + Convert.toHexString((byte) startPhysicalAddress));
        }
        int room_left;
        if (!checked) checked = ib.checkStatus();
        if (len == 0) return;
        checkSpeed();
        if (!ib.isContainerSecretSet()) throw new OneWireException("Secret is not set.");
        if ((startAddr + len) > size) throw new OneWireException("Write exceeds memory bank end");
        if (isReadOnly()) throw new OneWireException("Trying to write read-only memory bank");
        int startx = 0, nextx = 0;
        byte[] raw_buf = new byte[8];
        byte[] memory = new byte[size];
        int abs_addr = startPhysicalAddress + startAddr;
        int pl = 8;
        read(startAddr & 0xE0, false, memory, 0, size);
        if (abs_addr >= 128) {
            ib.getContainerSecret(memory, 0);
        }
        do {
            room_left = pl - ((abs_addr + startx) % pl);
            if ((len - startx) > room_left) nextx = startx + room_left; else nextx = len;
            if ((startx + startAddr) >= pageLength) System.arraycopy(memory, (((startx + startAddr) / 8) * 8) - 32, raw_buf, 0, 8); else System.arraycopy(memory, (((startx + startAddr) / 8) * 8), raw_buf, 0, 8);
            if ((nextx - startx) == 8) System.arraycopy(writeBuf, offset + startx, raw_buf, 0, 8); else if (((startAddr + nextx) % 8) == 0) System.arraycopy(writeBuf, offset + startx, raw_buf, ((startAddr + startx) % 8), 8 - ((startAddr + startx) % 8)); else System.arraycopy(writeBuf, offset + startx, raw_buf, ((startAddr + startx) % 8), ((startAddr + nextx) % 8) - ((startAddr + startx) % 8));
            scratchpad.writeScratchpad(abs_addr + startx + room_left - 8, raw_buf, 0, 8);
            scratchpad.copyScratchpad(abs_addr + startx + room_left - 8, raw_buf, 0, memory, 0);
            if ((startx + startAddr) >= pageLength) System.arraycopy(raw_buf, 0, memory, (((startx + startAddr) / 8) * 8) - 32, 8); else System.arraycopy(raw_buf, 0, memory, (((startx + startAddr) / 8) * 8), 8);
            startx = nextx;
        } while (nextx < len);
        if (DEBUG) Debug.debug("-----------------------------------------------------------");
    }

    /**
    * Read  page in the current bank with no
    * CRC checking (device or data). The resulting data from this API
    * may or may not be what is on the 1-Wire device.  It is recommends
    * that the data contain some kind of checking (CRC) like in the
    * readPagePacket() method or have the 1-Wire device provide the
    * CRC as in readPageCRC().  readPageCRC() however is not
    * supported on all memory types, see 'hasPageAutoCRC()'.
    * If neither is an option then this method could be called more
    * then once to at least verify that the same thing is read consistantly.
    *
    * @param  page          page number to read packet from
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPage() continious where the last one
    *                       led off and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to place read data into
    * @param  offset        offset into readBuf to place data
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void readPage(int page, boolean readContinue, byte[] readBuf, int offset) throws OneWireIOException, OneWireException {
        read(page * pageLength, readContinue, readBuf, offset, pageLength);
    }

    /**
    * Read  page with extra information in the current bank with no
    * CRC checking (device or data). The resulting data from this API
    * may or may not be what is on the 1-Wire device.  It is recommends
    * that the data contain some kind of checking (CRC) like in the
    * readPagePacket() method or have the 1-Wire device provide the
    * CRC as in readPageCRC().  readPageCRC() however is not
    * supported on all memory types, see 'hasPageAutoCRC()'.
    * If neither is an option then this method could be called more
    * then once to at least verify that the same thing is read consistantly.
    * See the method 'hasExtraInfo()' for a description of the optional
    * extra information some devices have.
    *
    * @param  page          page number to read packet from
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPage() continious where the last one
    *                       led off and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to place read data into
    * @param  offset        offset into readBuf to place data
    * @param  extraInfo     byte array to put extra info read into
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void readPage(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo) throws OneWireIOException, OneWireException {
        byte[] pg = new byte[32];
        if (!checked) checked = ib.checkStatus();
        if (!hasPageAutoCRC()) throw new OneWireException("This memory bank doesn't have crc capabilities.");
        if (!readContinue) checkSpeed();
        if (!readAuthenticatedPage(page, pg, 0, extraInfo, 0)) throw new OneWireException("Read didn't work.");
        System.arraycopy(pg, 0, readBuf, offset, 32);
    }

    /**
    * Read a Universal Data Packet.
    *
    * The Universal Data Packet always starts on page boundaries but
    * can end anywhere in the page.  The structure specifies the length of
    * data bytes not including the length byte and the CRC16 bytes.
    * There is one length byte. The CRC16 is first initialized to
    * the page number.  This provides a check to verify the page that
    * was intended is being read.  The CRC16 is then calculated over
    * the length and data bytes.  The CRC16 is then inverted and stored
    * low byte first followed by the high byte.  This is structure is
    * used by this method to verify the data but is not returned, only
    * the data payload is returned.
    *
    * @param  page          page number to read packet from
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPagePacket() continious where the last one
    *                       stopped and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to put data read. Must have at least
    *                       'getMaxPacketDataLength()' elements.
    * @param  offset        offset into readBuf to place data
    *
    * @return  number of data bytes read from the device and written to
    *          readBuf at the offset.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset) throws OneWireIOException, OneWireException {
        byte[] raw_buf = new byte[pageLength];
        readPage(page, readContinue, raw_buf, 0);
        if ((raw_buf[0] & 0x00FF) > maxPacketDataLength) throw new OneWireIOException("Invalid length in packet.");
        if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {
            System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);
            return raw_buf[0];
        } else throw new OneWireIOException("Invalid CRC16 in packet read.");
    }

    /**
    * Read a Universal Data Packet and extra information.  See the
    * method 'readPagePacket()' for a description of the packet structure.
    * See the method 'hasExtraInfo()' for a description of the optional
    * extra information some devices have.
    *
    * @param  page          page number to read packet from
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPagePacket() continious where the last one
    *                       stopped and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to put data read. Must have at least
    *                       'getMaxPacketDataLength()' elements.
    * @param  offset        offset into readBuf to place data
    * @param  extraInfo     byte array to put extra info read into
    *
    * @return  number of data bytes read from the device and written to
    *          readBuf at the offset.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo) throws OneWireIOException, OneWireException {
        byte[] raw_buf = new byte[pageLength];
        if (!checked) checked = ib.checkStatus();
        if (!hasPageAutoCRC()) throw new OneWireException("This memory bank page doesn't have CRC capabilites.");
        readAuthenticatedPage(page, raw_buf, 0, extraInfo, 0);
        if ((raw_buf[0] & 0x00FF) > maxPacketDataLength) throw new OneWireIOException("Invalid length in packet.");
        if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {
            System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);
            return raw_buf[0];
        } else throw new OneWireIOException("Invalid CRC16 in packet read.");
    }

    /**
    * Write a Universal Data Packet.  See the method 'readPagePacket()'
    * for a description of the packet structure.
    *
    * @param  page          page number to write packet to
    * @param  writeBuf      data byte array to write
    * @param  offset        offset into writeBuf where data to write is
    * @param  len           number of bytes to write
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void writePagePacket(int page, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
        if (len > maxPacketDataLength) throw new OneWireIOException("Length of packet requested exceeds page size.");
        if (!generalPurposeMemory) throw new OneWireException("Current bank is not general purpose memory.");
        byte[] raw_buf = new byte[len + 3];
        raw_buf[0] = (byte) len;
        System.arraycopy(writeBuf, offset, raw_buf, 1, len);
        int crc = CRC16.compute(raw_buf, 0, len + 1, page);
        raw_buf[len + 1] = (byte) (~crc & 0xFF);
        raw_buf[len + 2] = (byte) (((~crc & 0xFFFF) >>> 8) & 0xFF);
        write(page * pageLength, raw_buf, 0, len + 3);
    }

    /**
    * Read a complete memory page with CRC verification provided by the
    * device.  Not supported by all devices.  See the method
    * 'hasPageAutoCRC()'.
    *
    * @param  page          page number to read
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPagePacket() continious where the last one
    *                       stopped and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to put data read. Must have at least
    *                       'getMaxPacketDataLength()' elements.
    * @param  offset        offset into readBuf to place data
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset) throws OneWireIOException, OneWireException {
        byte[] extra = new byte[20];
        byte[] pg = new byte[32];
        if (!checked) checked = ib.checkStatus();
        if (!hasPageAutoCRC()) throw new OneWireException("This memory bank doesn't have CRC capabilites.");
        if (!readAuthenticatedPage(page, pg, 0, extra, 0)) throw new OneWireException("Read didn't work.");
        System.arraycopy(pg, 0, readBuf, offset, pageLength);
    }

    /**
    * Read a complete memory page with CRC verification provided by the
    * device with extra information.  Not supported by all devices.
    * See the method 'hasPageAutoCRC()'.
    * See the method 'hasExtraInfo()' for a description of the optional
    * extra information.
    *
    * @param  page          page number to read
    * @param  readContinue  if 'true' then device read is continued without
    *                       re-selecting.  This can only be used if the new
    *                       readPagePacket() continious where the last one
    *                       stopped and it is inside a
    *                       'beginExclusive/endExclusive' block.
    * @param  readBuf       byte array to put data read. Must have at least
    *                       'getMaxPacketDataLength()' elements.
    * @param  offset        offset into readBuf to place data
    * @param  extraInfo     byte array to put extra info read into
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void readPageCRC(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo) throws OneWireIOException, OneWireException {
        byte[] pg = new byte[32];
        if (!checked) checked = ib.checkStatus();
        if (!hasPageAutoCRC()) throw new OneWireException("This memory bank doesn't have CRC capabilities.");
        if (!readAuthenticatedPage(page, pg, 0, extraInfo, 0)) throw new OneWireException("Read didn't work.");
        System.arraycopy(pg, 0, readBuf, offset, pageLength);
    }

    /**
    * Write protect the memory bank.
    */
    public void writeprotect() {
        readOnly = true;
        readWrite = false;
    }

    /**
    * Sets the EPROM mode for this page.
    */
    public void setEPROM() {
        writeOnce = true;
    }

    /**
    * Check the device speed if has not been done before or if
    * an error was detected.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void checkSpeed() throws OneWireIOException, OneWireException {
        synchronized (this) {
            if (doSetSpeed) {
                ib.doSpeed();
                doSetSpeed = false;
            }
        }
    }

    /**
    *  Reads authenticated page.
    *
    * @param page          the page number in this bank to read from.
    * @param data          the data read from the address
    * @param extra_info    the MAC calculated for this function
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public boolean readAuthenticatedPage(int page, byte[] data, int dataStart, byte[] extra_info, int extraStart) throws OneWireException, OneWireIOException {
        byte[] send_block = new byte[40];
        byte[] challenge = new byte[8];
        int addr = (page * pageLength) + startPhysicalAddress;
        ib.getChallenge(challenge, 4);
        scratchpad.writeScratchpad(addr, challenge, 0, 8);
        if (!adapter.select(ib.getAddress())) throw new OneWireIOException("Device select failed.");
        send_block[0] = READ_AUTH_PAGE;
        send_block[1] = (byte) (addr & 0xFF);
        send_block[2] = (byte) (((addr & 0xFFFF) >>> 8) & 0xFF);
        System.arraycopy(ffBlock, 0, send_block, 3, 35);
        adapter.dataBlock(send_block, 0, 38);
        if (DEBUG) {
            IOHelper.writeLine("-------------------------------------------------------------");
            IOHelper.writeLine("ReadAuthPage - send_block:");
            IOHelper.writeBytesHex(send_block, 0, 38);
        }
        if (CRC16.compute(send_block, 0, 38, 0) != 0x0000B001) {
            throw new OneWireException("First CRC didn't pass.");
        }
        System.arraycopy(send_block, 3, data, dataStart, 32);
        System.arraycopy(ffBlock, 0, send_block, 0, 22);
        try {
            Thread.sleep(2);
        } catch (InterruptedException ie) {
            ;
        }
        adapter.dataBlock(send_block, 0, 22);
        if (DEBUG) {
            IOHelper.writeLine("ReadAuthPage - MAC:");
            IOHelper.writeBytesHex(send_block, 0, 20);
        }
        if (CRC16.compute(send_block, 0, 22, 0) != 0x0000B001) {
            throw new OneWireException("Second CRC didn't pass.");
        }
        if (DEBUG) {
            IOHelper.writeLine("next read:");
            IOHelper.writeBytesHex(send_block, 0, 22);
            IOHelper.writeLine("-------------------------------------------------------------");
        }
        System.arraycopy(send_block, 0, extra_info, extraStart, 20);
        return true;
    }

    /**
    * Set the flag to indicate the next 'checkSpeed()' will force
    * a speed set and verify 'doSpeed()'.
    */
    public void forceVerify() {
        synchronized (this) {
            doSetSpeed = true;
        }
    }
}
