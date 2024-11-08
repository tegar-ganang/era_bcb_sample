package com.dalsemi.onewire.container;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.container.OneWireContainer;

/**
 * Memory bank class for the EEPROM section of iButtons and 1-Wire devices on the DS2408.
 *
 *  @version    0.00, 28 Aug 2000
 *  @author     DS
 */
class MemoryBankEEPROM implements OTPMemoryBank {

    /**
     * Read Memory Command
     */
    public static final byte READ_MEMORY_COMMAND = (byte) 0xF0;

    /**
     * Write Scratchpad Command
     */
    public static final byte WRITE_SCRATCHPAD_COMMAND = (byte) 0x0F;

    /**
     * Read Scratchpad Command
     */
    public static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xAA;

    /**
     * Copy Scratchpad Command
     */
    public static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x55;

    /**
    * Reference to the OneWireContainer this bank resides on.
    */
    protected OneWireContainer ib;

    /**
    * block of 0xFF's used for faster read pre-fill of 1-Wire blocks
    */
    protected byte[] ffBlock = new byte[150];

    /**
    * Flag to indicate that speed needs to be set
    */
    protected boolean doSetSpeed;

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
    * Flag if memory bank is non volatile
    * (will not erase when power removed)
    */
    protected boolean nonVolatile;

    /**
    * Flag if memory bank needs program Pulse to write
    */
    protected boolean programPulse;

    /**
    * Flag if memory bank needs power delivery to write
    */
    protected boolean powerDelivery;

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
    * Max data length in page packet in memory bank
    */
    protected int maxPacketDataLength;

    /**
    * Flag if memory bank has page auto-CRC generation
    */
    protected boolean pageAutoCRC;

    /**
    * Flag if reading a page in memory bank provides optional
    * extra information (counter, tamper protection, SHA-1...)
    */
    protected boolean extraInfo;

    /**
    * Length of extra information when reading a page in memory bank
    */
    protected int extraInfoLength;

    /**
    * Extra information descriptoin when reading page in memory bank
    */
    protected String extraInfoDescription;

    /**
    * Flag if memory bank can have pages locked
    */
    protected boolean lockPage;

    /**
    * Memory bank to lock pages in 'this' memory bank
    */
    protected MemoryBankEEPROMstatus mbLock;

    /**
    * Memory bank contstuctor.  Requires reference to the OneWireContainer
    * this memory bank resides on.  Requires reference to memory banks used
    * in OTP operations.
    */
    public MemoryBankEEPROM(OneWireContainer ibutton) {
        ib = ibutton;
        mbLock = null;
        lockPage = true;
        generalPurposeMemory = true;
        bankDescription = "Main memory for the DS2408";
        numberPages = 4;
        size = 128;
        pageLength = 32;
        maxPacketDataLength = 0;
        readWrite = false;
        writeOnce = false;
        readOnly = false;
        nonVolatile = true;
        pageAutoCRC = true;
        lockPage = true;
        programPulse = false;
        powerDelivery = true;
        extraInfo = false;
        extraInfoLength = 0;
        extraInfoDescription = null;
        writeVerification = false;
        startPhysicalAddress = 0;
        doSetSpeed = true;
        for (int i = 0; i < 150; i++) ffBlock[i] = (byte) 0xFF;
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
        return nonVolatile;
    }

    /**
    * Query to see if current memory bank pages need the adapter to
    * have a 'ProgramPulse' in order to write to the memory.
    *
    * @return  'true' if writing to the current memory bank pages
    *                 requires a 'ProgramPulse'.
    */
    public boolean needsProgramPulse() {
        return programPulse;
    }

    /**
    * Query to see if current memory bank pages need the adapter to
    * have a 'PowerDelivery' feature in order to write to the memory.
    *
    * @return  'true' if writing to the current memory bank pages
    *                 requires 'PowerDelivery'.
    */
    public boolean needsPowerDelivery() {
        return powerDelivery;
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
    * Query to see if current memory bank pages can be read with
    * the contents being verified by a device generated CRC.
    * This is used to see if the 'ReadPageCRC()' can be used.
    *
    * @return  'true' if current memory bank can be read with self
    *          generated CRC.
    */
    public boolean hasPageAutoCRC() {
        return pageAutoCRC;
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
        return extraInfoDescription;
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
    * Query to see if current memory bank pages can be redirected
    * to another pages.  This is mostly used in Write-Once memory
    * to provide a means to update.
    *
    * @return  'true' if current memory bank pages can be redirected
    *          to a new page.
    */
    public boolean canRedirectPage() {
        return false;
    }

    /**
    * Query to see if current memory bank pages can be locked.  A
    * locked page would prevent any changes to the memory.
    *
    * @return  'true' if current memory bank pages can be redirected
    *          to a new page.
    */
    public boolean canLockPage() {
        return lockPage;
    }

    /**
    * Query to see if current memory bank pages can be locked from
    * being redirected.  This would prevent a Write-Once memory from
    * being updated.
    *
    * @return  'true' if current memory bank pages can be locked from
    *          being redirected to a new page.
    */
    public boolean canLockRedirectPage() {
        return false;
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
    * @param  startAddr     starting physical address
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
        byte[] buff = new byte[150];
        System.arraycopy(ffBlock, 0, buff, 0, 150);
        if ((startAddr + len) > (pageLength * numberPages)) throw new OneWireException("Read exceeds memory bank end");
        if (!readContinue) {
            checkSpeed();
            if (ib.adapter.select(ib.address)) {
                buff[0] = READ_MEMORY_COMMAND;
                buff[1] = (byte) (startAddr & 0xFF);
                buff[2] = (byte) (((startAddr & 0xFFFF) >>> 8) & 0xFF);
                ib.adapter.dataBlock(buff, 0, len + 3);
                System.arraycopy(buff, 3, readBuf, offset, len);
            } else throw new OneWireIOException("Device select failed");
        } else {
            ib.adapter.dataBlock(buff, 0, len);
            System.arraycopy(buff, 0, readBuf, offset, len);
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
    * @param  startAddr     starting address
    * @param  writeBuf      byte array containing data to write
    * @param  offset        offset into writeBuf to get data
    * @param  len           length in bytes to write
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void write(int startAddr, byte[] writeBuf, int offset, int len) throws OneWireIOException, OneWireException {
        int i, room_left;
        if (len == 0) return;
        if ((isPageLocked(0) && (startAddr < 32)) || (isPageLocked(1))) throw new OneWireIOException("The page is locked.");
        checkSpeed();
        if ((startAddr + len) > size) throw new OneWireException("Write exceeds memory bank end");
        if (isReadOnly()) throw new OneWireException("Trying to write read-only memory bank");
        int startx = 0, nextx = 0;
        byte[] raw_buf = new byte[8];
        byte[] memory = new byte[128];
        byte[] scratchpad = new byte[8];
        byte[] es_data = new byte[3];
        int abs_addr = startAddr;
        int pl = 8;
        read(0, false, memory, 0, 128);
        do {
            room_left = pl - ((abs_addr + startx) % pl);
            if ((len - startx) > room_left) nextx = startx + room_left; else nextx = len;
            System.arraycopy(memory, (((startx + startAddr) / 8) * 8), raw_buf, 0, 8);
            if ((nextx - startx) == 8) {
                System.arraycopy(writeBuf, offset + startx, raw_buf, 0, 8);
            } else {
                if (((startAddr + nextx) % 8) == 0) {
                    System.arraycopy(writeBuf, offset + startx, raw_buf, ((startAddr + startx) % 8), 8 - ((startAddr + startx) % 8));
                } else {
                    System.arraycopy(writeBuf, offset + startx, raw_buf, ((startAddr + startx) % 8), ((startAddr + nextx) % 8) - ((startAddr + startx) % 8));
                }
            }
            if (!writeScratchpad(abs_addr + startx + room_left - 8, raw_buf, 0, 8)) throw new OneWireIOException("Invalid CRC16 in write");
            if (!readScratchpad(scratchpad, 0, 8, es_data)) throw new OneWireIOException("Read scratchpad was not successful.");
            if ((es_data[2] & 0x20) == 0x20) {
                throw new OneWireIOException("The write scratchpad command was not completed.");
            } else {
                for (i = 0; i < 8; i++) if (scratchpad[i] != raw_buf[i]) {
                    throw new OneWireIOException("The read back of the data in the scratch pad did " + "not match.");
                }
            }
            copyScratchpad(es_data);
            if (startAddr >= pageLength) System.arraycopy(raw_buf, 0, memory, (((startx + startAddr) / 8) * 8) - 32, 8); else System.arraycopy(raw_buf, 0, memory, (((startx + startAddr) / 8) * 8), 8);
            startx = nextx;
        } while (nextx < len);
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
        throw new OneWireException("Read extra information not supported on this memory bank");
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
    * @return  number of data bytes written to readBuf at the offset.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset, byte[] extraInfo) throws OneWireIOException, OneWireException {
        throw new OneWireException("Read extra information not supported on this memory bank");
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
    *
    * @return  number of data bytes written to readBuf at the offset.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public int readPagePacket(int page, boolean readContinue, byte[] readBuf, int offset) throws OneWireIOException, OneWireException {
        byte[] raw_buf = new byte[pageLength];
        read((page * pageLength), readContinue, raw_buf, 0, pageLength);
        if ((raw_buf[0] & 0x00FF) > maxPacketDataLength) {
            forceVerify();
            throw new OneWireIOException("Invalid length in packet");
        }
        if (CRC16.compute(raw_buf, 0, raw_buf[0] + 3, page) == 0x0000B001) {
            System.arraycopy(raw_buf, 1, readBuf, offset, raw_buf[0]);
            return raw_buf[0];
        } else {
            forceVerify();
            throw new OneWireIOException("Invalid CRC16 in packet read");
        }
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
        if (len > maxPacketDataLength) throw new OneWireIOException("Length of packet requested exceeds page size");
        if (!generalPurposeMemory) throw new OneWireException("Current bank is not general purpose memory");
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
        throw new OneWireException("Read page with CRC not supported in this memory bank");
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
        throw new OneWireException("Read page with CRC not supported in this memory bank");
    }

    /**
    * Lock the specifed page in the current memory bank.  Not supported
    * by all devices.  See the method 'canLockPage()'.
    *
    * @param  page   number of page to lock
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void lockPage(int page) throws OneWireIOException, OneWireException {
        byte[] wr_byte = new byte[1];
        wr_byte[0] = (byte) 0x6C;
        if (page == 0) {
            mbLock.write(0, wr_byte, 0, 1);
        } else {
            mbLock.write(1, wr_byte, 0, 1);
        }
        if (!isPageLocked(page)) {
            forceVerify();
            throw new OneWireIOException("Read back from write incorrect, could not lock page");
        }
    }

    /**
    * Query to see if the specified page is locked.
    * See the method 'canLockPage()'.
    *
    * @param  page  number of page to see if locked
    *
    * @return  'true' if page locked.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public boolean isPageLocked(int page) throws OneWireIOException, OneWireException {
        byte[] rd_byte = new byte[2];
        if (page == 0) {
            mbLock.read(0, false, rd_byte, 0, 2);
            if ((rd_byte[0] == 0x6C) || (rd_byte[1] == 0x6C)) return true; else return false;
        } else {
            mbLock.read(1, false, rd_byte, 0, 1);
            if (rd_byte[0] == 0x6C) return true; else return false;
        }
    }

    /**
    * Redirect the specifed page in the current memory bank to a new page.
    * Not supported by all devices.  See the method 'canRedirectPage()'.
    *
    * @param  page      number of page to redirect
    * @param  newPage   new page number to redirect to
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void redirectPage(int page, int newPage) throws OneWireIOException, OneWireException {
        throw new OneWireException("This memory bank does not support redirection.");
    }

    /**
    * Query to see if the specified page is redirected.
    * Not supported by all devices.  See the method 'canRedirectPage()'.
    *
    * @param  page      number of page check for redirection
    *
    * @return  return the new page number or 0 if not redirected
    *
    * @throws OneWireIOException
    * @throws OneWireException
    *
    * @deprecated  As of 1-Wire API 0.01, replaced by {@link #getRedirectedPage(int)}
    */
    public int isPageRedirected(int page) throws OneWireIOException, OneWireException {
        throw new OneWireException("This memory bank does not support redirection.");
    }

    /**
    * Gets the page redirection of the specified page.
    * Not supported by all devices.
    *
    * @param  page  page to check for redirection
    *
    * @return  the new page number or 0 if not redirected
    *
    * @throws OneWireIOException on a 1-Wire communication error such as
    *         no device present or a CRC read from the device is incorrect.  This could be
    *         caused by a physical interruption in the 1-Wire Network due to
    *         shorts or a newly arriving 1-Wire device issuing a 'presence pulse'.
    * @throws OneWireException on a communication or setup error with the 1-Wire
    *         adapter.
    *
    * @see #canRedirectPage() canRedirectPage
    * @see #redirectPage(int,int) redirectPage
    * @since 1-Wire API 0.01
    */
    public int getRedirectedPage(int page) throws OneWireIOException, OneWireException {
        throw new OneWireException("This memory bank does not support redirection.");
    }

    /**
    * Lock the redirection option for the specifed page in the current
    * memory bank. Not supported by all devices.  See the method
    * 'canLockRedirectPage()'.
    *
    * @param  page      number of page to redirect
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public void lockRedirectPage(int page) throws OneWireIOException, OneWireException {
        throw new OneWireException("This memory bank does not support redirection.");
    }

    /**
    * Query to see if the specified page has redirection locked.
    * Not supported by all devices.  See the method 'canRedirectPage()'.
    *
    * @param  page      number of page check for locked redirection
    *
    * @return  return 'true' if redirection is locked for this page
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public boolean isRedirectPageLocked(int page) throws OneWireIOException, OneWireException {
        throw new OneWireException("This memory bank does not support redirection.");
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
    * Set the flag to indicate the next 'checkSpeed()' will force
    * a speed set and verify 'doSpeed()'.
    */
    public void forceVerify() {
        synchronized (this) {
            doSetSpeed = true;
        }
    }

    /**
    * Write to the Scratch Pad, which is a max of 8 bytes...  Note that if
    * less than 8 bytes are written, the ending offset will still report
    * that a full eight bytes are on the buffer.  This means that all 8 bytes
    * of the data in the scratchpad will be copied, not just the bytes user
    * wrote into it.
    *
    * @param  addr          the address to write the data to
    * @param  out_buf       byte array to write into scratch pad
    * @param  offset        offset into out_buf to write the data
    * @param  len           length of the write data
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public boolean writeScratchpad(int addr, byte[] out_buf, int offset, int len) throws OneWireIOException, OneWireException {
        byte[] send_block = new byte[14];
        if (len > 8) len = 8;
        if (ib.adapter.select(ib.getAddress())) {
            int cnt = 0;
            send_block[cnt++] = WRITE_SCRATCHPAD_COMMAND;
            send_block[cnt++] = (byte) (addr & 0x00FF);
            send_block[cnt++] = (byte) (((addr & 0x00FFFF) >>> 8) & 0x00FF);
            System.arraycopy(out_buf, offset, send_block, 3, len);
            cnt += len;
            send_block[cnt++] = (byte) 0x00FF;
            send_block[cnt++] = (byte) 0x00FF;
            ib.adapter.dataBlock(send_block, 0, cnt);
            if (CRC16.compute(send_block, 0, cnt) != 0x0000B001) throw new OneWireIOException("Invalid CRC16 in Writing Scratch Pad");
        } else throw new OneWireIOException("Device select failed.");
        return true;
    }

    /**
    * Copy all 8 bytes of the Sratch Pad to a certain address in memory.
    *
    * @param addr the address to copy the data to
    * @param auth byte[] containing write authorization
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public synchronized void copyScratchpad(byte[] es_data) throws OneWireIOException, OneWireException {
        byte[] send_block = new byte[4];
        if (ib.adapter.select(ib.getAddress())) {
            send_block[3] = es_data[2];
            send_block[2] = es_data[1];
            send_block[1] = es_data[0];
            send_block[0] = COPY_SCRATCHPAD_COMMAND;
            ib.adapter.dataBlock(send_block, 0, 3);
            ib.adapter.setPowerDuration(DSPortAdapter.DELIVERY_INFINITE);
            ib.adapter.startPowerDelivery(DSPortAdapter.CONDITION_AFTER_BYTE);
            ib.adapter.putByte(send_block[3]);
            try {
                Thread.sleep(12);
            } catch (InterruptedException e) {
            }
            ib.adapter.setPowerNormal();
            byte test = (byte) ib.adapter.getByte();
            if (test == (byte) 0x00FF) {
                throw new OneWireIOException("The scratchpad did not get copied to memory.");
            }
        } else throw new OneWireIOException("Device select failed.");
    }

    /**
    * Read from the Scratch Pad, which is a max of 8 bytes.
    *
    * @param  readBuf       byte array to place read data into
    *                       length of array is always pageLength.
    * @param  offset        offset into readBuf to pug data
    * @param  len           length in bytes to read
    * @param  extraInfo     byte array to put extra info read into
    *                       (TA1, TA2, e/s byte)
    *                       Can be 'null' if extra info is not needed.
    *
    * @throws OneWireIOException
    * @throws OneWireException
    */
    public boolean readScratchpad(byte[] readBuf, int offset, int len, byte[] es_data) throws OneWireIOException, OneWireException {
        if (!ib.adapter.select(ib.address)) {
            forceVerify();
            throw new OneWireIOException("Device select failed");
        }
        byte[] raw_buf = new byte[14];
        raw_buf[0] = READ_SCRATCHPAD_COMMAND;
        System.arraycopy(ffBlock, 0, raw_buf, 1, 13);
        ib.adapter.dataBlock(raw_buf, 0, 14);
        if (CRC16.compute(raw_buf, 0, 14) == 0x0000B001) {
            if (es_data != null) System.arraycopy(raw_buf, 1, es_data, 0, 3);
            System.arraycopy(raw_buf, 4, readBuf, offset, len);
            return true;
        } else throw new OneWireException("Error due to CRC.");
    }
}
