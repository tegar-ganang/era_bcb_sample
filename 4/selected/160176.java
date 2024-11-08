package jpcsp.HLE.kernel.types;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.HLE.Modules;
import jpcsp.crypto.CryptoEngine;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.format.PSF;
import jpcsp.util.Utilities;

public class SceUtilitySavedataParam extends pspAbstractMemoryMappedStructure {

    public static final String savedataPath = "ms0:/PSP/SAVEDATA/";

    public static final String savedataFilePath = "ms0/PSP/SAVEDATA/";

    public static final String icon0FileName = "ICON0.PNG";

    public static final String icon1FileName = "ICON1.PNG";

    public static final String pic1FileName = "PIC1.PNG";

    public static final String snd0FileName = "SND0.AT3";

    public static final String paramSfoFileName = "PARAM.SFO";

    public static final String anyFileName = "<>";

    public pspUtilityDialogCommon base;

    public int mode;

    public static final int MODE_AUTOLOAD = 0;

    public static final int MODE_AUTOSAVE = 1;

    public static final int MODE_LOAD = 2;

    public static final int MODE_SAVE = 3;

    public static final int MODE_LISTLOAD = 4;

    public static final int MODE_LISTSAVE = 5;

    public static final int MODE_LISTDELETE = 6;

    public static final int MODE_DELETE = 7;

    public static final int MODE_SIZES = 8;

    public static final int MODE_AUTODELETE = 9;

    public static final int MODE_SINGLEDELETE = 10;

    public static final int MODE_LIST = 11;

    public static final int MODE_FILES = 12;

    public static final int MODE_MAKEDATASECURE = 13;

    public static final int MODE_MAKEDATA = 14;

    public static final int MODE_READSECURE = 15;

    public static final int MODE_READ = 16;

    public static final int MODE_WRITESECURE = 17;

    public static final int MODE_WRITE = 18;

    public static final int MODE_ERASESECURE = 19;

    public static final int MODE_ERASE = 20;

    public static final int MODE_DELETEDATA = 21;

    public static final int MODE_GETSIZE = 22;

    public static final String[] modeNames = new String[] { "AUTOLOAD", "AUTOSAVE", "LOAD", "SAVE", "LISTLOAD", "LISTSAVE", "LISTDELETE", "DELETE", "SIZES", "AUTODELETE", "SINGLEDELETE", "LIST", "FILES", "MAKEDATASECURE", "MAKEDATA", "READSECURE", "READ", "WRITESECURE", "WRITE", "ERASESECURE", "ERASE", "DELETEDATA", "GETSIZE" };

    public int bind;

    public static final int BIND_NOT_USED = 0;

    public static final int BIND_IS_OK = 1;

    public static final int BIND_IS_REJECTED = 2;

    public static final int BIND_IS_NOT_SUPPORTED = 3;

    public boolean overwrite;

    public String gameName;

    public String saveName;

    public String fileName;

    public String[] saveNameList;

    public int saveNameListAddr;

    int dataBuf;

    int dataBufSize;

    public int dataSize;

    public PspUtilitySavedataSFOParam sfoParam;

    public PspUtilitySavedataFileData icon0FileData;

    public PspUtilitySavedataFileData icon1FileData;

    public PspUtilitySavedataFileData pic1FileData;

    public PspUtilitySavedataFileData snd0FileData;

    int newDataAddr;

    public PspUtilitySavedataListSaveNewData newData;

    public int focus;

    public static final int FOCUS_UNKNOWN = 0;

    public static final int FOCUS_FIRSTLIST = 1;

    public static final int FOCUS_LASTLIST = 2;

    public static final int FOCUS_LATEST = 3;

    public static final int FOCUS_OLDEST = 4;

    public static final int FOCUS_FIRSTEMPTY = 7;

    public static final int FOCUS_LASTEMPTY = 8;

    public int abortStatus;

    public int msFreeAddr;

    public int msDataAddr;

    public int utilityDataAddr;

    public byte[] key = new byte[0x10];

    public int secureVersion;

    public int multiStatus;

    public static final int MULTI_STATUS_SINGLE = 0;

    public static final int MULTI_STATUS_INIT = 1;

    public static final int MULTI_STATUS_RELAY = 2;

    public static final int MULTI_STATUS_FINISH = 3;

    public int idListAddr;

    public int fileListAddr;

    public int sizeAddr;

    public static class PspUtilitySavedataSFOParam extends pspAbstractMemoryMappedStructure {

        public String title;

        public String savedataTitle;

        public String detail;

        public int parentalLevel;

        @Override
        protected void read() {
            title = readStringNZ(0x80);
            savedataTitle = readStringNZ(0x80);
            detail = readStringNZ(0x400);
            parentalLevel = read32();
        }

        @Override
        protected void write() {
            writeStringNZ(0x80, title);
            writeStringNZ(0x80, savedataTitle);
            writeStringNZ(0x400, detail);
            write32(parentalLevel);
        }

        @Override
        public int sizeof() {
            return 0x80 + 0x80 + 0x400 + 4;
        }
    }

    ;

    public static class PspUtilitySavedataFileData extends pspAbstractMemoryMappedStructure {

        public int buf;

        public int bufSize;

        public int size;

        @Override
        protected void read() {
            buf = read32();
            bufSize = read32();
            size = read32();
            readUnknown(4);
        }

        @Override
        protected void write() {
            write32(buf);
            write32(bufSize);
            write32(size);
            writeUnknown(4);
        }

        @Override
        public int sizeof() {
            return 4 * 4;
        }
    }

    public static class PspUtilitySavedataListSaveNewData extends pspAbstractMemoryMappedStructure {

        public PspUtilitySavedataFileData icon0;

        public int titleAddr;

        public String title;

        @Override
        protected void read() {
            icon0 = new PspUtilitySavedataFileData();
            read(icon0);
            titleAddr = read32();
            if (titleAddr != 0) {
                title = Utilities.readStringZ(mem, titleAddr);
            } else {
                title = null;
            }
        }

        @Override
        protected void write() {
            write(icon0);
            write32(titleAddr);
            if (titleAddr != 0) {
                Utilities.writeStringZ(mem, titleAddr, title);
            }
        }

        @Override
        public int sizeof() {
            return icon0.sizeof() + 4;
        }
    }

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());
        mode = read32();
        bind = read32();
        overwrite = read32() == 0 ? false : true;
        gameName = readStringNZ(13);
        readUnknown(3);
        saveName = readStringNZ(20);
        saveNameListAddr = read32();
        if (Memory.isAddressGood(saveNameListAddr)) {
            List<String> newSaveNameList = new ArrayList<String>();
            boolean endOfList = false;
            for (int i = 0; !endOfList; i += 20) {
                String saveNameItem = Utilities.readStringNZ(mem, saveNameListAddr + i, 20);
                if (saveNameItem == null || saveNameItem.length() == 0) {
                    endOfList = true;
                } else {
                    newSaveNameList.add(saveNameItem);
                }
            }
            saveNameList = newSaveNameList.toArray(new String[newSaveNameList.size()]);
        }
        fileName = readStringNZ(13);
        readUnknown(3);
        dataBuf = read32();
        dataBufSize = read32();
        dataSize = read32();
        sfoParam = new PspUtilitySavedataSFOParam();
        read(sfoParam);
        icon0FileData = new PspUtilitySavedataFileData();
        read(icon0FileData);
        icon1FileData = new PspUtilitySavedataFileData();
        read(icon1FileData);
        pic1FileData = new PspUtilitySavedataFileData();
        read(pic1FileData);
        snd0FileData = new PspUtilitySavedataFileData();
        read(snd0FileData);
        newDataAddr = read32();
        if (newDataAddr != 0) {
            newData = new PspUtilitySavedataListSaveNewData();
            newData.read(mem, newDataAddr);
        } else {
            newData = null;
        }
        focus = read32();
        abortStatus = read32();
        msFreeAddr = read32();
        msDataAddr = read32();
        utilityDataAddr = read32();
        read8Array(key);
        secureVersion = read32();
        multiStatus = read32();
        idListAddr = read32();
        fileListAddr = read32();
        sizeAddr = read32();
    }

    @Override
    protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());
        write32(mode);
        write32(bind);
        write32(overwrite ? 1 : 0);
        writeStringNZ(13, gameName);
        writeUnknown(3);
        writeStringNZ(20, saveName);
        write32(saveNameListAddr);
        writeStringNZ(13, fileName);
        writeUnknown(3);
        write32(dataBuf);
        write32(dataBufSize);
        write32(dataSize);
        write(sfoParam);
        write(icon0FileData);
        write(icon1FileData);
        write(pic1FileData);
        write(snd0FileData);
        write32(newDataAddr);
        if (newDataAddr != 0) {
            newData.write(mem, newDataAddr);
        }
        write32(focus);
        write32(abortStatus);
        write32(msFreeAddr);
        write32(msDataAddr);
        write32(utilityDataAddr);
        write8Array(key);
        write32(secureVersion);
        write32(multiStatus);
        write32(idListAddr);
        write32(fileListAddr);
        write32(sizeAddr);
    }

    public String getBasePath() {
        return getBasePath(saveName);
    }

    public String getBasePath(String saveName) {
        String path = savedataPath + gameName;
        if (saveName != null && !anyFileName.equals(saveName)) {
            path += saveName;
        }
        path += "/";
        return path;
    }

    public String getFileName(String saveName, String fileName) {
        return getBasePath(saveName) + fileName;
    }

    private int getFileSize(String fileName) {
        int size = 0;
        if (fileName != null && fileName.length() > 0) {
            SceIoStat fileStat = Modules.IoFileMgrForUserModule.statFile(getFileName(saveName, fileName));
            if (fileStat != null) {
                size = (int) fileStat.size;
            }
        }
        return size;
    }

    public int getSizeKb(String gameName, String saveName) {
        int sizeKb;
        sizeKb = Utilities.getSizeKb(getFileSize(fileName));
        sizeKb += Utilities.getSizeKb(getFileSize(icon0FileName));
        sizeKb += Utilities.getSizeKb(getFileSize(icon1FileName));
        sizeKb += Utilities.getSizeKb(getFileSize(pic1FileName));
        sizeKb += Utilities.getSizeKb(getFileSize(snd0FileName));
        sizeKb += Utilities.getSizeKb(getFileSize(paramSfoFileName));
        return sizeKb;
    }

    private SeekableDataInput getDataInput(String path, String name) {
        SeekableDataInput fileInput = Modules.IoFileMgrForUserModule.getFile(path + name, jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDONLY);
        return fileInput;
    }

    private SeekableRandomFile getDataOutput(String path, String name) {
        SeekableDataInput fileInput = Modules.IoFileMgrForUserModule.getFile(path + name, jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDWR | jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT);
        if (fileInput instanceof SeekableRandomFile) {
            return (SeekableRandomFile) fileInput;
        }
        return null;
    }

    public void singleRead(Memory mem) throws IOException {
        String path = getBasePath();
        if (CryptoEngine.getSavedataCryptoStatus()) {
            dataSize = loadEncryptedFile(mem, path, fileName, dataBuf, dataBufSize, key, secureVersion);
        } else {
            dataSize = loadFile(mem, path, fileName, dataBuf, dataBufSize);
        }
    }

    public void singleWrite(Memory mem) throws IOException {
        String path = getBasePath();
        Modules.IoFileMgrForUserModule.mkdirs(path);
        if (CryptoEngine.getSavedataCryptoStatus()) {
            writeEncryptedFile(mem, path, fileName, dataBuf, dataSize, key, secureVersion);
        } else {
            writeFile(mem, path, fileName, dataBuf, dataSize);
        }
    }

    public void load(Memory mem) throws IOException {
        String path = getBasePath();
        if (CryptoEngine.getSavedataCryptoStatus()) {
            dataSize = loadEncryptedFile(mem, path, fileName, dataBuf, dataBufSize, key, secureVersion);
        } else {
            dataSize = loadFile(mem, path, fileName, dataBuf, dataBufSize);
        }
        safeLoad(mem, icon0FileName, icon0FileData);
        safeLoad(mem, icon1FileName, icon1FileData);
        safeLoad(mem, pic1FileName, pic1FileData);
        safeLoad(mem, snd0FileName, snd0FileData);
        loadPsf(mem, path, paramSfoFileName, sfoParam);
        bind = BIND_IS_OK;
        abortStatus = 0;
    }

    private void safeLoad(Memory mem, String filename, PspUtilitySavedataFileData fileData) throws IOException {
        String path = getBasePath();
        try {
            fileData.size = loadFile(mem, path, filename, fileData.buf, fileData.bufSize);
        } catch (FileNotFoundException e) {
        }
    }

    public void save(Memory mem) throws IOException {
        String path = getBasePath();
        Modules.IoFileMgrForUserModule.mkdirs(path);
        if (CryptoEngine.getSavedataCryptoStatus()) {
            writeEncryptedFile(mem, path, fileName, dataBuf, dataSize, key, secureVersion);
        } else {
            writeFile(mem, path, fileName, dataBuf, dataSize);
        }
        writeFile(mem, path, icon0FileName, icon0FileData.buf, icon0FileData.size);
        writeFile(mem, path, icon1FileName, icon1FileData.buf, icon1FileData.size);
        writeFile(mem, path, pic1FileName, pic1FileData.buf, pic1FileData.size);
        writeFile(mem, path, snd0FileName, snd0FileData.buf, snd0FileData.size);
        if (CryptoEngine.getSavedataCryptoStatus()) {
            writeEncryptedPsf(mem, path, paramSfoFileName, sfoParam, fileName, dataSize, key, secureVersion);
        } else {
            writePsf(mem, path, paramSfoFileName, sfoParam);
        }
    }

    private int loadFile(Memory mem, String path, String name, int address, int maxLength) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return 0;
        }
        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }
        int fileSize = (int) fileInput.length();
        if ((fileSize > maxLength) && (maxLength > 0)) {
            fileSize = maxLength;
        }
        Utilities.readFully(fileInput, address, fileSize);
        fileInput.close();
        return fileSize;
    }

    private void writeEncryptedFile(Memory mem, String path, String name, int address, int length, byte[] key, int mode) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return;
        }
        SeekableRandomFile fileOutput = getDataOutput(path, name);
        CryptoEngine crypto = new CryptoEngine();
        if (fileOutput == null) {
            return;
        }
        byte[] inBuf = new byte[length];
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, 1);
        for (int i = 0; i < length; i++) {
            inBuf[i] = (byte) memoryReader.readNext();
        }
        byte[] outBuf = crypto.EncryptSavedata(inBuf, length, key, mode);
        fileOutput.write(outBuf);
        fileOutput.close();
    }

    private int loadEncryptedFile(Memory mem, String path, String name, int address, int maxLength, byte[] key, int mode) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return 0;
        }
        SeekableDataInput fileInput = getDataInput(path, name);
        CryptoEngine crypto = new CryptoEngine();
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }
        int fileSize = (int) fileInput.length();
        byte[] inBuf = new byte[fileSize];
        fileInput.readFully(inBuf);
        byte[] outBuf = crypto.DecryptSavedata(inBuf, fileSize, key, mode);
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 1);
        for (int i = 0; i < outBuf.length; i++) {
            memoryWriter.writeNext(outBuf[i]);
        }
        return outBuf.length;
    }

    private void writeFile(Memory mem, String path, String name, int address, int length) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return;
        }
        SeekableRandomFile fileOutput = getDataOutput(path, name);
        if (fileOutput == null) {
            return;
        }
        Utilities.write(fileOutput, address, length);
        fileOutput.close();
    }

    private void loadPsf(Memory mem, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput != null) {
            byte[] buffer = new byte[(int) fileInput.length()];
            fileInput.readFully(buffer);
            fileInput.close();
            PSF psf = new PSF();
            psf.read(ByteBuffer.wrap(buffer));
            sfoParam.parentalLevel = psf.getNumeric("PARENTAL_LEVEL");
            sfoParam.title = psf.getString("TITLE");
            sfoParam.detail = psf.getString("SAVEDATA_DETAIL");
            sfoParam.savedataTitle = psf.getString("SAVEDATA_TITLE");
        }
    }

    private void writePsf(Memory mem, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableRandomFile fileOutput = getDataOutput(path, name);
        if (fileOutput == null) {
            return;
        }
        PSF psf = new PSF();
        psf.put("PARENTAL_LEVEL", sfoParam.parentalLevel);
        psf.put("TITLE", sfoParam.title, 128);
        psf.put("SAVEDATA_DETAIL", sfoParam.detail, 1024);
        psf.put("SAVEDATA_TITLE", sfoParam.savedataTitle, 128);
        psf.write(fileOutput.getChannel().map(MapMode.READ_WRITE, 0, psf.size()));
    }

    private void writeEncryptedPsf(Memory mem, String path, String psfName, PspUtilitySavedataSFOParam sfoParam, String dataName, int dataLength, byte[] key, int mode) throws IOException {
        SeekableRandomFile psfOutput = getDataOutput(path, psfName);
        SeekableRandomFile dataOutput = getDataOutput(path, dataName);
        if ((psfOutput == null) || (dataOutput == null)) {
            return;
        }
        CryptoEngine crypto = new CryptoEngine();
        byte[] dataBuffer = new byte[dataLength];
        dataOutput.readFully(dataBuffer);
        PSF psf = new PSF();
        psf.put("CATEGORY", "MS", 4);
        psf.put("PARENTAL_LEVEL", sfoParam.parentalLevel);
        psf.put("SAVEDATA_DETAIL", sfoParam.detail, 1024);
        psf.put("SAVEDATA_DIRECTORY", gameName + saveName, 64);
        crypto.UpdateSavedataHashes(psf, dataBuffer, dataLength, key, dataName, mode);
        psf.put("SAVEDATA_TITLE", sfoParam.savedataTitle, 128);
        psf.put("TITLE", sfoParam.title, 128);
        psf.write(psfOutput.getChannel().map(MapMode.READ_WRITE, 0, psf.size()));
    }

    public boolean test(Memory mem) throws IOException {
        String path = getBasePath();
        boolean result = testFile(mem, path, fileName);
        return result;
    }

    private boolean testFile(Memory mem, String path, String name) throws IOException {
        if (name == null || name.length() <= 0) {
            return false;
        }
        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }
        fileInput.close();
        return true;
    }

    public boolean isPresent(String gameName, String saveName) {
        if (saveName == null || saveName.length() <= 0 || anyFileName.equals(saveName)) {
            File f = new File(savedataFilePath);
            String[] entries = f.list();
            if (entries == null) {
                return false;
            }
            for (int i = 0; i < f.list().length; i++) {
                if (entries[i].startsWith(gameName)) {
                    saveName = entries[i].replace(gameName, "");
                    break;
                }
            }
        }
        if (fileName == null || fileName.length() <= 0) {
            File f = new File(savedataFilePath + gameName + saveName);
            if (f.list() == null) {
                return false;
            }
            return true;
        }
        String path = getBasePath(saveName);
        try {
            SeekableDataInput fileInput = getDataInput(path, fileName);
            if (fileInput != null) {
                fileInput.close();
                return true;
            }
        } catch (IOException e) {
        }
        return false;
    }

    public boolean isPresent() {
        return isPresent(gameName, saveName);
    }

    public long getTimestamp(String gameName, String saveName) {
        String sfoFileName = getFileName(saveName, paramSfoFileName);
        SceIoStat sfoStat = Modules.IoFileMgrForUserModule.statFile(sfoFileName);
        if (sfoStat != null) {
            Calendar cal = Calendar.getInstance();
            ScePspDateTime pspTime = sfoStat.mtime;
            cal.set(pspTime.year, pspTime.month, pspTime.day, pspTime.hour, pspTime.minute, pspTime.second);
            return cal.getTimeInMillis();
        }
        return 0;
    }

    @Override
    public int sizeof() {
        return base.totalSizeof();
    }

    public String getModeName() {
        if (mode < 0 || mode >= modeNames.length) {
            return String.format("UNKNOWN_MODE%d", mode);
        }
        return modeNames[mode];
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("Address 0x%08X, mode=%d(%s), gameName=%s, saveName=%s, fileName=%s", getBaseAddress(), mode, getModeName(), gameName, saveName, fileName));
        for (int i = 0; saveNameList != null && i < saveNameList.length; i++) {
            if (i == 0) {
                s.append(", saveNameList=[");
            } else {
                s.append(", ");
            }
            s.append(saveNameList[i]);
            if (i == saveNameList.length - 1) {
                s.append("]");
            }
        }
        return s.toString();
    }
}
