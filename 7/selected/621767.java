package jdos.dos;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.ints.Bios_disk;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.*;

public class DosMSCDEX {

    private static final int MSCDEX_VERSION_HIGH = 2;

    private static final int MSCDEX_VERSION_LOW = 23;

    private static final int MSCDEX_MAX_DRIVES = 8;

    private static final int MSCDEX_ERROR_INVALID_FUNCTION = 1;

    private static final int MSCDEX_ERROR_BAD_FORMAT = 11;

    private static final int MSCDEX_ERROR_UNKNOWN_DRIVE = 15;

    private static final int MSCDEX_ERROR_DRIVE_NOT_READY = 21;

    private static final int REQUEST_STATUS_DONE = 0x0100;

    private static final int REQUEST_STATUS_ERROR = 0x8000;

    private static int forceCD = -1;

    private static class DOS_DeviceHeader extends MemStruct {

        public static final int size = 22;

        public DOS_DeviceHeader(int ptr) {
            pt = ptr;
        }

        ;

        public void SetNextDeviceHeader(int ptr) {
            SaveIt(4, 0, ptr);
        }

        public int GetNextDeviceHeader() {
            return GetIt(4, 0);
        }

        public void SetAttribute(int atr) {
            SaveIt(2, 4, atr);
        }

        public void SetDriveLetter(int letter) {
            SaveIt(1, 20, letter);
        }

        public void SetNumSubUnits(short num) {
            SaveIt(1, 21, num);
        }

        public short GetNumSubUnits() {
            return (short) GetIt(1, 21);
        }

        public void SetName(String _name) {
            Memory.MEM_BlockWrite(pt + 10, _name, 8);
        }

        public void SetInterrupt(int ofs) {
            SaveIt(2, 8, ofs);
        }

        public void SetStrategy(int ofs) {
            SaveIt(2, 6, ofs);
        }
    }

    private static class CMscdex {

        int GetVersion() {
            return (MSCDEX_VERSION_HIGH << 8) + MSCDEX_VERSION_LOW;
        }

        int GetNumDrives() {
            return numDrives;
        }

        int GetFirstDrive() {
            return dinfo[0].drive;
        }

        int numDrives;

        public static class TDriveInfo {

            short drive;

            short physDrive;

            boolean audioPlay;

            boolean audioPaused;

            long audioStart;

            long audioEnd;

            boolean locked;

            boolean lastResult;

            long volumeSize;

            Dos_cdrom.TCtrl audioCtrl = new Dos_cdrom.TCtrl();
        }

        int defaultBufSeg;

        TDriveInfo[] dinfo = new TDriveInfo[MSCDEX_MAX_DRIVES];

        Dos_cdrom.CDROM_Interface[] cdrom = new Dos_cdrom.CDROM_Interface[MSCDEX_MAX_DRIVES];

        int rootDriverHeaderSeg;

        CMscdex() {
            numDrives = 0;
            rootDriverHeaderSeg = 0;
            defaultBufSeg = 0;
            for (int i = 0; i < MSCDEX_MAX_DRIVES; i++) dinfo[i] = new TDriveInfo();
        }

        void GetDrives(int data) {
            for (int i = 0; i < GetNumDrives(); i++) Memory.mem_writeb(data + i, dinfo[i].drive);
        }

        boolean IsValidDrive(int _drive) {
            _drive &= 0xff;
            for (int i = 0; i < GetNumDrives(); i++) if (dinfo[i].drive == _drive) return true;
            return false;
        }

        short GetSubUnit(int _drive) {
            _drive &= 0xff;
            for (int i = 0; i < GetNumDrives(); i++) if (dinfo[i].drive == _drive) return (short) i;
            return 0xff;
        }

        int RemoveDrive(int _drive) {
            int idx = MSCDEX_MAX_DRIVES;
            for (int i = 0; i < GetNumDrives(); i++) {
                if (dinfo[i].drive == _drive) {
                    idx = i;
                    break;
                }
            }
            if (idx == MSCDEX_MAX_DRIVES || (idx != 0 && idx != GetNumDrives() - 1)) return 0;
            cdrom[idx].close();
            if (idx == 0) {
                for (int i = 0; i < GetNumDrives(); i++) {
                    if (i == MSCDEX_MAX_DRIVES - 1) {
                        cdrom[i] = null;
                        dinfo[i] = new TDriveInfo();
                    } else {
                        dinfo[i] = dinfo[i + 1];
                        cdrom[i] = cdrom[i + 1];
                    }
                }
            } else {
                cdrom[idx] = null;
                dinfo[idx] = new TDriveInfo();
            }
            numDrives--;
            if (GetNumDrives() == 0) {
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg, 0));
                int off = DOS_DeviceHeader.size;
                devHeader.SetStrategy(off + 4);
                devHeader.SetInterrupt(off + 4);
                devHeader.SetDriveLetter(0);
            } else if (idx == 0) {
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader((Memory.PhysMake(rootDriverHeaderSeg, 0)));
                devHeader.SetDriveLetter(GetFirstDrive() + 1);
            }
            return 1;
        }

        int AddDrive(int _drive, String physicalPath, ShortRef subUnit) {
            subUnit.value = 0;
            if (GetNumDrives() + 1 >= MSCDEX_MAX_DRIVES) return 4;
            if (GetNumDrives() != 0) {
                if (dinfo[0].drive - 1 != _drive && dinfo[numDrives - 1].drive + 1 != _drive) return 1;
            }
            int result = 0;
            switch(Dos_cdrom.CDROM_GetMountType(physicalPath, forceCD)) {
                case 0x00:
                    {
                        if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Mounting physical cdrom not supported: " + physicalPath);
                    }
                    break;
                case 0x01:
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_NORMAL, "MSCDEX: Mounting iso file as cdrom: " + physicalPath);
                    cdrom[numDrives] = new CDROM_Interface_Image((short) numDrives);
                    break;
                case 0x02:
                    cdrom[numDrives] = new CDROM_Interface_Fake();
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_NORMAL, "MSCDEX: Mounting directory as cdrom: " + physicalPath);
                    if (Log.level <= LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_NORMAL, "MSCDEX: You wont have full MSCDEX support !");
                    result = 5;
                    break;
                default:
                    return 6;
            }
            if (!cdrom[numDrives].SetDevice(physicalPath, forceCD)) {
                return 3;
            }
            if (rootDriverHeaderSeg == 0) {
                int driverSize = DOS_DeviceHeader.size + 10;
                int seg = Dos_tables.DOS_GetMemory((driverSize + 15) / 16);
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(seg, 0));
                devHeader.SetNextDeviceHeader(0xFFFFFFFF);
                devHeader.SetAttribute(0xc800);
                devHeader.SetDriveLetter(_drive + 1);
                devHeader.SetNumSubUnits((short) 1);
                devHeader.SetName("MSCD001 ");
                long start = Dos.dos_infoblock.GetDeviceChain() & 0xFFFFFFFFl;
                int segm = (int) (start >> 16);
                int offm = (int) (start & 0xFFFF);
                while (start != 0xFFFFFFFFl) {
                    segm = (int) (start >>> 16);
                    offm = (int) (start & 0xFFFF);
                    start = Memory.real_readd(segm, offm) & 0xFFFFFFFFl;
                }
                Memory.real_writed(segm, offm, seg << 16);
                int off = DOS_DeviceHeader.size;
                int call_strategy = Callback.CALLBACK_Allocate();
                Callback.CallBack_Handlers[call_strategy] = MSCDEX_Strategy_Handler;
                Memory.real_writeb(seg, off + 0, (short) 0xFE);
                Memory.real_writeb(seg, off + 1, (short) 0x38);
                Memory.real_writew(seg, off + 2, call_strategy);
                Memory.real_writeb(seg, off + 4, (short) 0xCB);
                devHeader.SetStrategy(off);
                off += 5;
                int call_interrupt = Callback.CALLBACK_Allocate();
                Callback.CallBack_Handlers[call_interrupt] = MSCDEX_Interrupt_Handler;
                Memory.real_writeb(seg, off + 0, (short) 0xFE);
                Memory.real_writeb(seg, off + 1, (short) 0x38);
                Memory.real_writew(seg, off + 2, call_interrupt);
                Memory.real_writeb(seg, off + 4, (short) 0xCB);
                devHeader.SetInterrupt(off);
                rootDriverHeaderSeg = seg;
            } else if (GetNumDrives() == 0) {
                DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg, 0));
                int off = DOS_DeviceHeader.size;
                devHeader.SetDriveLetter(_drive + 1);
                devHeader.SetStrategy(off);
                devHeader.SetInterrupt(off + 5);
            }
            DOS_DeviceHeader devHeader = new DOS_DeviceHeader(Memory.PhysMake(rootDriverHeaderSeg, 0));
            devHeader.SetNumSubUnits((short) (devHeader.GetNumSubUnits() + 1));
            if (dinfo[0].drive - 1 == _drive) {
                Dos_cdrom.CDROM_Interface _cdrom = cdrom[numDrives];
                CDROM_Interface_Image _cdimg = CDROM_Interface_Image.images[numDrives];
                for (int i = GetNumDrives(); i > 0; i--) {
                    dinfo[i] = dinfo[i - 1];
                    cdrom[i] = cdrom[i - 1];
                    CDROM_Interface_Image.images[i] = CDROM_Interface_Image.images[i - 1];
                }
                cdrom[0] = _cdrom;
                CDROM_Interface_Image.images[0] = _cdimg;
                dinfo[0].drive = (short) _drive;
                dinfo[0].physDrive = (short) physicalPath.toUpperCase().charAt(0);
                subUnit.value = 0;
            } else {
                dinfo[numDrives].drive = (short) _drive;
                dinfo[numDrives].physDrive = (short) physicalPath.toUpperCase().charAt(0);
                subUnit.value = (short) numDrives;
            }
            numDrives++;
            for (int chan = 0; chan < 4; chan++) {
                dinfo[subUnit.value].audioCtrl.out[chan] = chan;
                dinfo[subUnit.value].audioCtrl.vol[chan] = 0xff;
            }
            StopAudio(subUnit.value);
            return result;
        }

        boolean HasDrive(int drive) {
            return (GetSubUnit(drive) != 0xff);
        }

        void ReplaceDrive(Dos_cdrom.CDROM_Interface newCdrom, short subUnit) {
            cdrom[subUnit].close();
            cdrom[subUnit] = newCdrom;
            StopAudio(subUnit);
        }

        int GetDefaultBuffer() {
            if (defaultBufSeg == 0) {
                int size = (2352 * 2 + 15) / 16;
                defaultBufSeg = Dos_tables.DOS_GetMemory(size);
            }
            return Memory.PhysMake(defaultBufSeg, 2352);
        }

        int GetTempBuffer() {
            if (defaultBufSeg == 0) {
                int size = (2352 * 2 + 15) / 16;
                defaultBufSeg = Dos_tables.DOS_GetMemory(size);
            }
            return Memory.PhysMake(defaultBufSeg, 0);
        }

        void GetDriverInfo(int data) {
            for (int i = 0; i < GetNumDrives(); i++) {
                Memory.mem_writeb(data, (short) i);
                Memory.mem_writed(data + 1, Memory.RealMake(rootDriverHeaderSeg, 0));
                data += 5;
            }
        }

        boolean GetCDInfo(short subUnit, ShortRef tr1, ShortRef tr2, Dos_cdrom.TMSF leadOut) {
            if (subUnit >= numDrives) return false;
            IntRef tr1i = new IntRef(0), tr2i = new IntRef(0);
            cdrom[subUnit].InitNewMedia();
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioTracks(tr1i, tr2i, leadOut);
            if (!dinfo[subUnit].lastResult) {
                tr1.value = tr2.value = 0;
                leadOut.clear();
            } else {
                tr1.value = (short) tr1i.value;
                tr2.value = (short) tr2i.value;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean GetTrackInfo(short subUnit, short track, ShortRef attr, Dos_cdrom.TMSF start) {
            if (subUnit >= numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioTrackInfo(track, start, attr);
            if (!dinfo[subUnit].lastResult) {
                attr.value = 0;
                start.clear();
            }
            return dinfo[subUnit].lastResult;
        }

        boolean PlayAudioSector(short subUnit, long sector, long length) {
            if (subUnit >= numDrives) return false;
            if (dinfo[subUnit].audioPaused && (sector == dinfo[subUnit].audioStart) && (dinfo[subUnit].audioEnd != 0)) {
                dinfo[subUnit].lastResult = cdrom[subUnit].PauseAudio(true);
            } else dinfo[subUnit].lastResult = cdrom[subUnit].PlayAudioSector(sector, length);
            if (dinfo[subUnit].lastResult) {
                dinfo[subUnit].audioPlay = true;
                dinfo[subUnit].audioPaused = false;
                dinfo[subUnit].audioStart = sector;
                dinfo[subUnit].audioEnd = length;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean PlayAudioMSF(short subUnit, long start, long length) {
            if (subUnit >= numDrives) return false;
            short min = (short) ((start >> 16) & 0xFF);
            short sec = (short) ((start >> 8) & 0xFF);
            short fr = (short) ((start >> 0) & 0xFF);
            long sector = min * 60 * 75 + sec * 75 + fr - 150;
            return dinfo[subUnit].lastResult = PlayAudioSector(subUnit, sector, length);
        }

        boolean GetSubChannelData(short subUnit, ShortRef attr, ShortRef track, ShortRef index, Dos_cdrom.TMSF rel, Dos_cdrom.TMSF abs) {
            if (subUnit >= numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioSub(attr, track, index, rel, abs);
            if (!dinfo[subUnit].lastResult) {
                attr.value = track.value = index.value = 0;
                rel.clear();
                abs.clear();
            }
            return dinfo[subUnit].lastResult;
        }

        boolean GetAudioStatus(short subUnit, BooleanRef playing, BooleanRef pause, Dos_cdrom.TMSF start, Dos_cdrom.TMSF end) {
            if (subUnit >= numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetAudioStatus(playing, pause);
            if (dinfo[subUnit].lastResult) {
                long addr = dinfo[subUnit].audioStart + 150;
                start.fr = (short) (addr % 75);
                addr /= 75;
                start.sec = (short) (addr % 60);
                start.min = (short) (addr / 60);
                addr = dinfo[subUnit].audioEnd + 150;
                end.fr = (short) (addr % 75);
                addr /= 75;
                end.sec = (short) (addr % 60);
                end.min = (short) (addr / 60);
            } else {
                playing.value = false;
                pause.value = false;
                start.clear();
                end.clear();
            }
            return dinfo[subUnit].lastResult;
        }

        boolean StopAudio(short subUnit) {
            if (subUnit >= numDrives) return false;
            if (dinfo[subUnit].audioPlay) dinfo[subUnit].lastResult = cdrom[subUnit].PauseAudio(false); else dinfo[subUnit].lastResult = cdrom[subUnit].StopAudio();
            if (dinfo[subUnit].lastResult) {
                if (dinfo[subUnit].audioPlay) {
                    Dos_cdrom.TMSF pos = new Dos_cdrom.TMSF();
                    GetCurrentPos(subUnit, pos);
                    dinfo[subUnit].audioStart = pos.min * 60 * 75 + pos.sec * 75 + pos.fr - 150;
                    dinfo[subUnit].audioPaused = true;
                } else {
                    dinfo[subUnit].audioPaused = false;
                    dinfo[subUnit].audioStart = 0;
                    dinfo[subUnit].audioEnd = 0;
                }
                dinfo[subUnit].audioPlay = false;
            }
            return dinfo[subUnit].lastResult;
        }

        boolean ResumeAudio(short subUnit) {
            if (subUnit >= numDrives) return false;
            return dinfo[subUnit].lastResult = PlayAudioSector(subUnit, dinfo[subUnit].audioStart, dinfo[subUnit].audioEnd);
        }

        long GetVolumeSize(short subUnit) {
            if (subUnit >= numDrives) return 0;
            ShortRef tr1 = new ShortRef(0), tr2 = new ShortRef(0);
            Dos_cdrom.TMSF leadOut = new Dos_cdrom.TMSF();
            dinfo[subUnit].lastResult = GetCDInfo(subUnit, tr1, tr2, leadOut);
            if (dinfo[subUnit].lastResult) return (leadOut.min * 60 * 75) + (leadOut.sec * 75) + leadOut.fr;
            return 0;
        }

        boolean ReadVTOC(int drive, int volume, int data, IntRef error) {
            short subunit = GetSubUnit(drive);
            if (!ReadSectors(subunit, false, 16 + volume, 1, data)) {
                error.value = MSCDEX_ERROR_DRIVE_NOT_READY;
                return false;
            }
            byte[] id = new byte[5];
            Memory.MEM_BlockRead(data + 1, id, 5);
            if (!"CD001".equals(new String(id))) {
                error.value = MSCDEX_ERROR_BAD_FORMAT;
                return false;
            }
            short type = Memory.mem_readb(data);
            error.value = (type == 1) ? 1 : (type == 0xFF) ? 0xFF : 0;
            return true;
        }

        boolean GetVolumeName(short subUnit, StringRef data) {
            if (subUnit >= numDrives) return false;
            int drive = dinfo[subUnit].drive;
            IntRef error = new IntRef(0);
            boolean success = false;
            int ptoc = GetTempBuffer();
            success = ReadVTOC(drive, 0x00, ptoc, error);
            if (success) {
                data.value = Memory.MEM_StrCopy(ptoc + 40, 31);
                data.value = data.value.trim();
            }
            return success;
        }

        boolean GetCopyrightName(int drive, int data) {
            IntRef error = new IntRef(0);
            boolean success = false;
            int ptoc = GetTempBuffer();
            success = ReadVTOC(drive, 0x00, ptoc, error);
            if (success) {
                Memory.MEM_BlockCopy(data, ptoc + 702, 37);
                Memory.mem_writeb(data + 37, 0);
            }
            return success;
        }

        boolean GetAbstractName(int drive, int data) {
            IntRef error = new IntRef(0);
            boolean success = false;
            int ptoc = GetTempBuffer();
            success = ReadVTOC(drive, 0x00, ptoc, error);
            if (success) {
                Memory.MEM_BlockCopy(data, ptoc + 739, 37);
                Memory.mem_writeb(data + 37, 0);
            }
            return success;
        }

        boolean GetDocumentationName(int drive, int data) {
            IntRef error = new IntRef(0);
            boolean success = false;
            int ptoc = GetTempBuffer();
            success = ReadVTOC(drive, 0x00, ptoc, error);
            if (success) {
                Memory.MEM_BlockCopy(data, ptoc + 776, 37);
                Memory.mem_writeb(data + 37, 0);
            }
            return success;
        }

        boolean GetUPC(short subUnit, ShortRef attr, StringRef upc) {
            if (subUnit >= numDrives) return false;
            return dinfo[subUnit].lastResult = cdrom[subUnit].GetUPC(attr, upc);
        }

        boolean ReadSectors(short subUnit, boolean raw, long sector, int num, int data) {
            if (subUnit >= numDrives) return false;
            if ((4 * num * 2048 + 5) < CPU.CPU_Cycles) CPU.CPU_Cycles -= 4 * num * 2048; else CPU.CPU_Cycles = 5;
            dinfo[subUnit].lastResult = cdrom[subUnit].ReadSectors(data, raw, sector, num);
            return dinfo[subUnit].lastResult;
        }

        boolean ReadSectorsMSF(short subUnit, boolean raw, long start, int num, int data) {
            if (subUnit >= numDrives) return false;
            short min = (short) ((start >> 16) & 0xFF);
            short sec = (short) ((start >> 8) & 0xFF);
            short fr = (short) ((start >> 0) & 0xFF);
            long sector = min * 60 * 75 + sec * 75 + fr - 150;
            return ReadSectors(subUnit, raw, sector, num, data);
        }

        boolean ReadSectors(int drive, long sector, int num, int data) {
            return ReadSectors(GetSubUnit(drive), false, sector, num, data);
        }

        boolean GetDirectoryEntry(int drive, boolean copyFlag, int pathname, int buffer, IntRef error) {
            String volumeID;
            String searchName;
            String entryName;
            boolean foundComplete = false;
            boolean foundName;
            boolean nextPart = true;
            String useName = "";
            int entryLength, nameLength;
            error.value = 0;
            searchName = Memory.MEM_StrCopy(pathname + 1, Memory.mem_readb(pathname)).toUpperCase();
            String searchPos = searchName;
            int searchlen = searchName.length();
            if (searchlen > 1 && searchName.indexOf("..") >= 0) if (searchName.charAt(searchlen - 1) == '.') searchName = searchName.substring(0, searchlen - 1);
            int defBuffer = GetDefaultBuffer();
            if (!ReadSectors(GetSubUnit(drive), false, 16, 1, defBuffer)) return false;
            volumeID = Memory.MEM_StrCopy(defBuffer + 1, 5);
            boolean iso = ("CD001".equals(volumeID));
            if (!iso) Log.exit("MSCDEX: GetDirEntry: Not an ISO 9960 CD.");
            int dirEntrySector = Memory.mem_readd(defBuffer + 156 + 2);
            int dirSize = Memory.mem_readd(defBuffer + 156 + 10);
            int index;
            while (dirSize > 0) {
                index = 0;
                if (!ReadSectors(GetSubUnit(drive), false, dirEntrySector, 1, defBuffer)) return false;
                foundName = false;
                if (nextPart) {
                    if (searchPos.length() > 0) {
                        useName = searchPos;
                        int pos = searchPos.indexOf("\\");
                        if (pos >= 0) searchPos = searchPos.substring(pos + 1); else searchPos = "";
                    }
                    if (searchPos.length() == 0) foundComplete = true;
                }
                do {
                    entryLength = Memory.mem_readb(defBuffer + index);
                    if (entryLength == 0) break;
                    nameLength = Memory.mem_readb(defBuffer + index + 32);
                    entryName = Memory.MEM_StrCopy(defBuffer + index + 33, nameLength);
                    if (entryName.equals(useName)) {
                        foundName = true;
                        break;
                    }
                    int longername = entryName.indexOf(';');
                    if (longername >= 0) {
                        if (entryName.substring(0, longername).equals(useName)) {
                            foundName = true;
                            break;
                        }
                    }
                    index += entryLength;
                } while (index + 33 <= 2048);
                if (foundName) {
                    if (foundComplete) {
                        if (copyFlag) {
                            Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_WARN, "MSCDEX: GetDirEntry: Copyflag structure not entirely accurate maybe");
                            byte[] readBuf = new byte[256];
                            byte[] writeBuf = new byte[256];
                            if (entryLength > 256) return false;
                            Memory.MEM_BlockRead(defBuffer + index, readBuf, entryLength);
                            writeBuf[0] = readBuf[1];
                            System.arraycopy(readBuf, 0x2, writeBuf, 1, 4);
                            writeBuf[5] = 0;
                            writeBuf[6] = 8;
                            System.arraycopy(readBuf, 0xa, writeBuf, 7, 4);
                            System.arraycopy(readBuf, 0x12, writeBuf, 0xb, 7);
                            writeBuf[0x12] = readBuf[0x19];
                            writeBuf[0x13] = readBuf[0x1a];
                            writeBuf[0x14] = readBuf[0x1b];
                            System.arraycopy(readBuf, 0x1c, writeBuf, 0x15, 2);
                            writeBuf[0x17] = readBuf[0x20];
                            System.arraycopy(readBuf, 0x21, writeBuf, 0x18, readBuf[0x20] <= 38 ? readBuf[0x20] : 38);
                            Memory.MEM_BlockWrite(buffer, writeBuf, 0x18 + 40);
                        } else {
                            Memory.MEM_BlockCopy(buffer, defBuffer + index, entryLength);
                        }
                        error.value = iso ? 1 : 0;
                        return true;
                    }
                    dirEntrySector = Memory.mem_readd(defBuffer + index + 2);
                    dirSize = Memory.mem_readd(defBuffer + index + 10);
                    nextPart = true;
                } else {
                    dirSize -= 2048;
                    dirEntrySector++;
                    nextPart = false;
                }
            }
            error.value = 2;
            return false;
        }

        boolean GetCurrentPos(short subUnit, Dos_cdrom.TMSF pos) {
            if (subUnit >= numDrives) return false;
            Dos_cdrom.TMSF rel = new Dos_cdrom.TMSF();
            ShortRef attr = new ShortRef(0), track = new ShortRef(0), index = new ShortRef(0);
            dinfo[subUnit].lastResult = GetSubChannelData(subUnit, attr, track, index, rel, pos);
            if (!dinfo[subUnit].lastResult) pos.clear();
            return dinfo[subUnit].lastResult;
        }

        boolean GetMediaStatus(short subUnit, BooleanRef media, BooleanRef changed, BooleanRef trayOpen) {
            if (subUnit >= numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].GetMediaTrayStatus(media, changed, trayOpen);
            return dinfo[subUnit].lastResult;
        }

        long GetDeviceStatus(short subUnit) {
            if (subUnit >= numDrives) return 0;
            BooleanRef media = new BooleanRef(), changed = new BooleanRef(), trayOpen = new BooleanRef();
            dinfo[subUnit].lastResult = GetMediaStatus(subUnit, media, changed, trayOpen);
            long status = ((trayOpen.value ? 1 : 0) << 0) | ((dinfo[subUnit].locked ? 1 : 0) << 1) | (1 << 2) | (1 << 4) | (1 << 8) | (1 << 9) | (((!media.value) ? 1 : 0) << 11);
            return status;
        }

        boolean GetMediaStatus(short subUnit, ShortRef status) {
            if (subUnit >= numDrives) return false;
            status.value = (short) (Bios_disk.getSwapRequest() ? 0xFF : 0x01);
            return true;
        }

        boolean LoadUnloadMedia(short subUnit, boolean unload) {
            if (subUnit >= numDrives) return false;
            dinfo[subUnit].lastResult = cdrom[subUnit].LoadUnloadMedia(unload);
            return dinfo[subUnit].lastResult;
        }

        boolean SendDriverRequest(int drive, int data) {
            short subUnit = GetSubUnit(drive);
            if (subUnit >= numDrives) return false;
            Memory.mem_writeb(data + 1, subUnit);
            MSCDEX_Strategy_Handler.call();
            MSCDEX_Interrupt_Handler.call();
            return true;
        }

        int GetStatusWord(short subUnit, int status) {
            if (subUnit >= numDrives) return REQUEST_STATUS_ERROR | 0x02;
            if (dinfo[subUnit].lastResult) status |= REQUEST_STATUS_DONE; else status |= REQUEST_STATUS_ERROR;
            if (dinfo[subUnit].audioPlay) {
                Dos_cdrom.TMSF start = new Dos_cdrom.TMSF(), end = new Dos_cdrom.TMSF();
                BooleanRef playing = new BooleanRef(), pause = new BooleanRef();
                if (GetAudioStatus(subUnit, playing, pause, start, end)) {
                    dinfo[subUnit].audioPlay = playing.value;
                } else dinfo[subUnit].audioPlay = false;
                status |= ((dinfo[subUnit].audioPlay ? 1 : 0) << 9);
            }
            dinfo[subUnit].lastResult = true;
            return status;
        }

        void InitNewMedia(short subUnit) {
            if (subUnit < numDrives) {
                cdrom[subUnit].InitNewMedia();
            }
        }

        boolean ChannelControl(int subUnit, Dos_cdrom.TCtrl ctrl) {
            if (subUnit >= numDrives) return false;
            if (ctrl.out[0] > 1) ctrl.out[0] = 0;
            if (ctrl.out[1] > 1) ctrl.out[1] = 1;
            dinfo[subUnit].audioCtrl = ctrl;
            cdrom[subUnit].ChannelControl(ctrl);
            return true;
        }

        boolean GetChannelControl(int subUnit, Dos_cdrom.TCtrl ctrl) {
            if (subUnit >= numDrives) return false;
            ctrl.copy(dinfo[subUnit].audioCtrl);
            return true;
        }
    }

    private static CMscdex mscdex = null;

    private static int curReqheaderPtr = 0;

    private static int MSCDEX_IOCTL_Input(int buffer, short drive_unit) {
        int ioctl_fct = Memory.mem_readb(buffer);
        if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: IOCTL INPUT Subfunction " + Integer.toString(ioctl_fct, 16));
        switch(ioctl_fct) {
            case 0x00:
                Memory.mem_writed(buffer + 1, Memory.RealMake(mscdex.rootDriverHeaderSeg, 0));
                break;
            case 0x01:
                {
                    Dos_cdrom.TMSF pos = new Dos_cdrom.TMSF();
                    mscdex.GetCurrentPos(drive_unit, pos);
                    short addr_mode = Memory.mem_readb(buffer + 1);
                    if (addr_mode == 0) {
                        long frames = pos.min * 60 * Dos_cdrom.CD_FPS + pos.sec * Dos_cdrom.CD_FPS + pos.fr;
                        if (frames < 150) if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Get position: invalid position " + pos.min + ":" + pos.sec + ":" + pos.fr); else frames -= 150;
                        Memory.mem_writed(buffer + 2, (int) frames);
                    } else if (addr_mode == 1) {
                        Memory.mem_writeb(buffer + 2, pos.fr);
                        Memory.mem_writeb(buffer + 3, pos.sec);
                        Memory.mem_writeb(buffer + 4, pos.min);
                        Memory.mem_writeb(buffer + 5, 0x00);
                    } else {
                        if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Get position: invalid address mode " + Integer.toString(addr_mode, 16));
                        return 0x03;
                    }
                }
                break;
            case 0x04:
                Dos_cdrom.TCtrl ctrl = new Dos_cdrom.TCtrl();
                if (!mscdex.GetChannelControl(drive_unit, ctrl)) return 0x01;
                for (int chan = 0; chan < 4; chan++) {
                    Memory.mem_writeb(buffer + chan * 2 + 1, ctrl.out[chan]);
                    Memory.mem_writeb(buffer + chan * 2 + 2, ctrl.vol[chan]);
                }
                break;
            case 0x06:
                Memory.mem_writed(buffer + 1, (int) mscdex.GetDeviceStatus(drive_unit));
                break;
            case 0x07:
                if (Memory.mem_readb(buffer + 1) == 0) Memory.mem_writed(buffer + 2, 2048); else if (Memory.mem_readb(buffer + 1) == 1) Memory.mem_writed(buffer + 2, 2352); else return 0x03;
                break;
            case 0x08:
                Memory.mem_writed(buffer + 1, (int) mscdex.GetVolumeSize(drive_unit));
                break;
            case 0x09:
                ShortRef status = new ShortRef();
                if (!mscdex.GetMediaStatus(drive_unit, status)) {
                    status.value = 0;
                }
                Memory.mem_writeb(buffer + 1, status.value);
                break;
            case 0x0A:
                ShortRef tr1 = new ShortRef(), tr2 = new ShortRef();
                Dos_cdrom.TMSF leadOut = new Dos_cdrom.TMSF();
                if (!mscdex.GetCDInfo(drive_unit, tr1, tr2, leadOut)) return 0x05;
                Memory.mem_writeb(buffer + 1, tr1.value);
                Memory.mem_writeb(buffer + 2, tr2.value);
                Memory.mem_writeb(buffer + 3, leadOut.fr);
                Memory.mem_writeb(buffer + 4, leadOut.sec);
                Memory.mem_writeb(buffer + 5, leadOut.min);
                Memory.mem_writeb(buffer + 6, 0x00);
                break;
            case 0x0B:
                {
                    ShortRef attr = new ShortRef();
                    Dos_cdrom.TMSF start = new Dos_cdrom.TMSF();
                    short track = Memory.mem_readb(buffer + 1);
                    mscdex.GetTrackInfo(drive_unit, track, attr, start);
                    Memory.mem_writeb(buffer + 2, start.fr);
                    Memory.mem_writeb(buffer + 3, start.sec);
                    Memory.mem_writeb(buffer + 4, start.min);
                    Memory.mem_writeb(buffer + 5, 0x00);
                    Memory.mem_writeb(buffer + 6, attr.value);
                    break;
                }
            case 0x0C:
                {
                    ShortRef attr = new ShortRef(), track = new ShortRef(), index = new ShortRef();
                    Dos_cdrom.TMSF abs = new Dos_cdrom.TMSF(), rel = new Dos_cdrom.TMSF();
                    mscdex.GetSubChannelData(drive_unit, attr, track, index, rel, abs);
                    Memory.mem_writeb(buffer + 1, attr.value);
                    Memory.mem_writeb(buffer + 2, track.value);
                    Memory.mem_writeb(buffer + 3, index.value);
                    Memory.mem_writeb(buffer + 4, rel.min);
                    Memory.mem_writeb(buffer + 5, rel.sec);
                    Memory.mem_writeb(buffer + 6, rel.fr);
                    Memory.mem_writeb(buffer + 7, 0x00);
                    Memory.mem_writeb(buffer + 8, abs.min);
                    Memory.mem_writeb(buffer + 9, abs.sec);
                    Memory.mem_writeb(buffer + 10, abs.fr);
                    break;
                }
            case 0x0E:
                {
                    ShortRef attr = new ShortRef();
                    StringRef upc = new StringRef();
                    mscdex.GetUPC(drive_unit, attr, upc);
                    Memory.mem_writeb(buffer + 1, attr.value);
                    for (int i = 0; i < 7; i++) Memory.mem_writeb(buffer + 2 + i, upc.value.charAt(i));
                    Memory.mem_writeb(buffer + 9, 0x00);
                    break;
                }
            case 0x0F:
                {
                    BooleanRef playing = new BooleanRef(), pause = new BooleanRef();
                    Dos_cdrom.TMSF resStart = new Dos_cdrom.TMSF(), resEnd = new Dos_cdrom.TMSF();
                    mscdex.GetAudioStatus(drive_unit, playing, pause, resStart, resEnd);
                    Memory.mem_writeb(buffer + 1, pause.value ? 1 : 0);
                    Memory.mem_writeb(buffer + 3, resStart.min);
                    Memory.mem_writeb(buffer + 4, resStart.sec);
                    Memory.mem_writeb(buffer + 5, resStart.fr);
                    Memory.mem_writeb(buffer + 6, 0x00);
                    Memory.mem_writeb(buffer + 7, resEnd.min);
                    Memory.mem_writeb(buffer + 8, resEnd.sec);
                    Memory.mem_writeb(buffer + 9, resEnd.fr);
                    Memory.mem_writeb(buffer + 10, 0x00);
                    break;
                }
            default:
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Unsupported IOCTL INPUT Subfunction " + Integer.toString(ioctl_fct, 16));
                return 0x03;
        }
        return 0x00;
    }

    private static int MSCDEX_IOCTL_Optput(int buffer, short drive_unit) {
        int ioctl_fct = Memory.mem_readb(buffer);
        switch(ioctl_fct) {
            case 0x00:
                if (!mscdex.LoadUnloadMedia(drive_unit, true)) return 0x02;
                break;
            case 0x03:
                Dos_cdrom.TCtrl ctrl = new Dos_cdrom.TCtrl();
                for (int chan = 0; chan < 4; chan++) {
                    ctrl.out[chan] = Memory.mem_readb(buffer + chan * 2 + 1);
                    ctrl.vol[chan] = Memory.mem_readb(buffer + chan * 2 + 2);
                }
                if (!mscdex.ChannelControl(drive_unit, ctrl)) return 0x01;
                break;
            case 0x01:
                break;
            case 0x02:
                Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_WARN, "cdromDrive reset");
                if (!mscdex.StopAudio(drive_unit)) return 0x02;
                break;
            case 0x05:
                if (!mscdex.LoadUnloadMedia(drive_unit, false)) return 0x02;
            default:
                if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Unsupported IOCTL OUTPUT Subfunction " + Integer.toString(ioctl_fct, 16));
                return 0x03;
        }
        return 0x00;
    }

    private static Callback.Handler MSCDEX_Strategy_Handler = new Callback.Handler() {

        public String getName() {
            return "MSCDEX_Strategy_Handler";
        }

        public int call() {
            curReqheaderPtr = Memory.PhysMake((int) CPU.Segs_ESval, CPU_Regs.reg_ebx.word());
            return Callback.CBRET_NONE;
        }
    };

    private static Callback.Handler MSCDEX_Interrupt_Handler = new Callback.Handler() {

        public String getName() {
            return "MSCDEX_Interrupt_Handler";
        }

        public int call() {
            if (curReqheaderPtr == 0) {
                Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: invalid call to interrupt handler");
                return Callback.CBRET_NONE;
            }
            short subUnit = Memory.mem_readb(curReqheaderPtr + 1);
            short funcNr = Memory.mem_readb(curReqheaderPtr + 2);
            int errcode = 0;
            int buffer = 0;
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Driver Function " + Integer.toString(funcNr, 16));
            if ((funcNr == 0x03) || (funcNr == 0x0c) || (funcNr == 0x80) || (funcNr == 0x82)) {
                buffer = Memory.PhysMake(Memory.mem_readw(curReqheaderPtr + 0x10), Memory.mem_readw(curReqheaderPtr + 0x0E));
            }
            switch(funcNr) {
                case 0x03:
                    {
                        int error = MSCDEX_IOCTL_Input(buffer, subUnit);
                        if (error != 0) errcode = error;
                        break;
                    }
                case 0x0C:
                    {
                        int error = MSCDEX_IOCTL_Optput(buffer, subUnit);
                        if (error != 0) errcode = error;
                        break;
                    }
                case 0x0D:
                case 0x0E:
                    break;
                case 0x80:
                case 0x82:
                    {
                        long start = Memory.mem_readd(curReqheaderPtr + 0x14) & 0xFFFFFFFFl;
                        int len = Memory.mem_readw(curReqheaderPtr + 0x12);
                        boolean raw = (Memory.mem_readb(curReqheaderPtr + 0x18) == 1);
                        if (Memory.mem_readb(curReqheaderPtr + 0x0D) == 0x00) mscdex.ReadSectors(subUnit, raw, start, len, buffer); else mscdex.ReadSectorsMSF(subUnit, raw, start, len, buffer);
                        break;
                    }
                case 0x83:
                    break;
                case 0x84:
                    {
                        long start = Memory.mem_readd(curReqheaderPtr + 0x0E) & 0xFFFFFFFFl;
                        long len = Memory.mem_readd(curReqheaderPtr + 0x12) & 0xFFFFFFFFl;
                        if (Memory.mem_readb(curReqheaderPtr + 0x0D) == 0x00) mscdex.PlayAudioSector(subUnit, start, len); else mscdex.PlayAudioMSF(subUnit, start, len);
                        break;
                    }
                case 0x85:
                    mscdex.StopAudio(subUnit);
                    break;
                case 0x88:
                    mscdex.ResumeAudio(subUnit);
                    break;
                default:
                    if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Unsupported Driver Request " + Integer.toString(funcNr, 16));
                    break;
            }
            Memory.mem_writew(curReqheaderPtr + 3, mscdex.GetStatusWord(subUnit, errcode));
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Status : " + Integer.toString(Memory.mem_readw(curReqheaderPtr + 3), 16));
            return Callback.CBRET_NONE;
        }
    };

    private static Dos_system.MultiplexHandler MSCDEX_Handler = new Dos_system.MultiplexHandler() {

        public boolean call() {
            if (CPU_Regs.reg_eax.high() == 0x11) {
                if (CPU_Regs.reg_eax.low() == 0x00) {
                    int check = Memory.PhysMake((int) CPU.Segs_SSval, CPU_Regs.reg_esp.word());
                    if (Memory.mem_readw(check + 6) == 0xDADA) {
                        Memory.mem_writew(check + 6, 0xADAD);
                    }
                    CPU_Regs.reg_eax.low(0xff);
                    return true;
                } else {
                    Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "NETWORK REDIRECTOR USED!!!");
                    CPU_Regs.reg_eax.word(0x49);
                    Callback.CALLBACK_SCF(true);
                    return true;
                }
            }
            if (CPU_Regs.reg_eax.high() != 0x15) return false;
            int data = Memory.PhysMake((int) CPU.Segs_ESval, CPU_Regs.reg_ebx.word());
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: INT 2F " + Integer.toString(CPU_Regs.reg_eax.word(), 16) + " BX= " + Integer.toString(CPU_Regs.reg_ebx.word(), 16) + " CX=" + Integer.toString(CPU_Regs.reg_ecx.word(), 16));
            switch(CPU_Regs.reg_eax.word()) {
                case 0x1500:
                    CPU_Regs.reg_ebx.word(mscdex.GetNumDrives());
                    if (CPU_Regs.reg_ebx.word() > 0) CPU_Regs.reg_ecx.word(mscdex.GetFirstDrive());
                    CPU_Regs.reg_eax.low(0xff);
                    return true;
                case 0x1501:
                    mscdex.GetDriverInfo(data);
                    return true;
                case 0x1502:
                    if (mscdex.GetCopyrightName(CPU_Regs.reg_ecx.word(), data)) {
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                        Callback.CALLBACK_SCF(true);
                    }
                    return true;
                case 0x1503:
                    if (mscdex.GetAbstractName(CPU_Regs.reg_ecx.word(), data)) {
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                        Callback.CALLBACK_SCF(true);
                    }
                    return true;
                case 0x1504:
                    if (mscdex.GetDocumentationName(CPU_Regs.reg_ecx.word(), data)) {
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                        Callback.CALLBACK_SCF(true);
                    }
                    return true;
                case 0x1505:
                    {
                        IntRef error = new IntRef(0);
                        if (mscdex.ReadVTOC(CPU_Regs.reg_ecx.word(), CPU_Regs.reg_edx.word(), data, error)) {
                            Callback.CALLBACK_SCF(false);
                        } else {
                            CPU_Regs.reg_eax.word(error.value);
                            Callback.CALLBACK_SCF(true);
                        }
                    }
                    return true;
                case 0x1508:
                    {
                        long sector = (CPU_Regs.reg_esi.word() << 16) + CPU_Regs.reg_edi.word();
                        if (mscdex.ReadSectors(CPU_Regs.reg_ecx.word(), sector, CPU_Regs.reg_edx.word(), data)) {
                            CPU_Regs.reg_eax.word(0);
                            Callback.CALLBACK_SCF(false);
                        } else {
                            CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                            Callback.CALLBACK_SCF(true);
                        }
                        return true;
                    }
                case 0x1509:
                    CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                    Callback.CALLBACK_SCF(true);
                    return true;
                case 0x150B:
                    CPU_Regs.reg_eax.word((mscdex.IsValidDrive(CPU_Regs.reg_ecx.word()) ? 0x5ad8 : 0x0000));
                    CPU_Regs.reg_ebx.word(0xADAD);
                    return true;
                case 0x150C:
                    CPU_Regs.reg_ebx.word(mscdex.GetVersion());
                    return true;
                case 0x150D:
                    mscdex.GetDrives(data);
                    return true;
                case 0x150E:
                    if (mscdex.IsValidDrive(CPU_Regs.reg_ecx.word())) {
                        if (CPU_Regs.reg_ebx.word() == 0) {
                            CPU_Regs.reg_edx.word(0x100);
                            Callback.CALLBACK_SCF(false);
                        } else if (CPU_Regs.reg_ebx.word() == 1) {
                            if (CPU_Regs.reg_edx.high() == 1) {
                                Callback.CALLBACK_SCF(false);
                            } else {
                                CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                                Callback.CALLBACK_SCF(true);
                            }
                        } else {
                            CPU_Regs.reg_eax.word(MSCDEX_ERROR_INVALID_FUNCTION);
                            Callback.CALLBACK_SCF(true);
                        }
                    } else {
                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                        Callback.CALLBACK_SCF(true);
                    }
                    return true;
                case 0x150F:
                    {
                        IntRef error = new IntRef(0);
                        boolean success = mscdex.GetDirectoryEntry(CPU_Regs.reg_ecx.low(), (CPU_Regs.reg_ecx.high() & 1) != 0, data, Memory.PhysMake(CPU_Regs.reg_esi.word(), CPU_Regs.reg_edi.word()), error);
                        CPU_Regs.reg_eax.word(error.value);
                        Callback.CALLBACK_SCF(!success);
                    }
                    return true;
                case 0x1510:
                    if (mscdex.SendDriverRequest(CPU_Regs.reg_ecx.word(), data)) {
                        Callback.CALLBACK_SCF(false);
                    } else {
                        CPU_Regs.reg_eax.word(MSCDEX_ERROR_UNKNOWN_DRIVE);
                        Callback.CALLBACK_SCF(true);
                    }
                    return true;
            }
            if (Log.level <= LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MISC, LogSeverities.LOG_ERROR, "MSCDEX: Unknwon call : " + Integer.toString(CPU_Regs.reg_eax.word(), 16));
            return true;
        }
    };

    private static class device_MSCDEX extends DOS_Device {

        device_MSCDEX() {
            SetName("MSCD001");
        }

        public boolean Read(byte[] data, IntRef size) {
            return false;
        }

        public boolean Write(byte[] data, IntRef size) {
            Log.log(LogTypes.LOG_ALL, LogSeverities.LOG_NORMAL, "Write to mscdex device");
            return false;
        }

        public boolean Seek(LongRef pos, int type) {
            return false;
        }

        public boolean Close() {
            return false;
        }

        public int GetInformation() {
            return 0xc880;
        }

        public boolean ReadFromControlChannel(int bufptr, int size, IntRef retcode) {
            if (MSCDEX_IOCTL_Input(bufptr, (short) 0) == 0) {
                retcode.value = size;
                return true;
            }
            return false;
        }

        public boolean WriteToControlChannel(int bufptr, int size, IntRef retcode) {
            if (MSCDEX_IOCTL_Optput(bufptr, (short) 0) == 0) {
                retcode.value = size;
                return true;
            }
            return false;
        }
    }

    public static int MSCDEX_AddDrive(char driveLetter, String physicalPath, ShortRef subUnit) {
        return mscdex.AddDrive(driveLetter - 'A', physicalPath, subUnit);
    }

    public static int MSCDEX_RemoveDrive(char driveLetter) {
        if (mscdex == null) return 0;
        return mscdex.RemoveDrive(driveLetter - 'A');
    }

    public static boolean MSCDEX_HasDrive(char driveLetter) {
        return mscdex.HasDrive(driveLetter - 'A');
    }

    public static void MSCDEX_ReplaceDrive(Dos_cdrom.CDROM_Interface cdrom, short subUnit) {
        mscdex.ReplaceDrive(cdrom, subUnit);
    }

    public static boolean MSCDEX_GetVolumeName(short subUnit, StringRef name) {
        return mscdex.GetVolumeName(subUnit, name);
    }

    private static Dos_cdrom.TMSF[] leadOut = new Dos_cdrom.TMSF[MSCDEX_MAX_DRIVES];

    static {
        for (int i = 0; i < leadOut.length; i++) leadOut[i] = new Dos_cdrom.TMSF();
    }

    public static boolean MSCDEX_HasMediaChanged(short subUnit) {
        Dos_cdrom.TMSF leadnew = new Dos_cdrom.TMSF();
        ShortRef tr1 = new ShortRef(), tr2 = new ShortRef();
        if (mscdex.GetCDInfo(subUnit, tr1, tr2, leadnew)) {
            boolean changed = (leadOut[subUnit].min != leadnew.min) || (leadOut[subUnit].sec != leadnew.sec) || (leadOut[subUnit].fr != leadnew.fr);
            if (changed) {
                leadOut[subUnit].min = leadnew.min;
                leadOut[subUnit].sec = leadnew.sec;
                leadOut[subUnit].fr = leadnew.fr;
                mscdex.InitNewMedia(subUnit);
            }
            return changed;
        }
        if (subUnit < MSCDEX_MAX_DRIVES) {
            leadOut[subUnit].min = 0;
            leadOut[subUnit].sec = 0;
            leadOut[subUnit].fr = 0;
        }
        return true;
    }

    public static void MSCDEX_SetCDInterface(int intNr, int numCD) {
        forceCD = numCD;
    }

    public static Section.SectionFunction MSCDEX_ShutDown = new Section.SectionFunction() {

        public void call(Section section) {
            mscdex = null;
            curReqheaderPtr = 0;
        }
    };

    public static Section.SectionFunction MSCDEX_Init = new Section.SectionFunction() {

        public void call(Section section) {
            section.AddDestroyFunction(MSCDEX_ShutDown);
            DOS_Device newdev = new device_MSCDEX();
            Dos_devices.DOS_AddDevice(newdev);
            curReqheaderPtr = 0;
            Dos_misc.DOS_AddMultiplexHandler(MSCDEX_Handler);
            mscdex = new CMscdex();
        }
    };
}
