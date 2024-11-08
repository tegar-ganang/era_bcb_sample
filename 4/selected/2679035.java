package org.furthurnet.datastructures;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;
import org.furthurnet.client.ClientManager;
import org.furthurnet.clientgui.ClientGuiConstants;
import org.furthurnet.datastructures.supporting.Common;
import org.furthurnet.datastructures.supporting.Constants;
import org.furthurnet.md5.*;

public class FileManager {

    private ClientManager manager = null;

    private String tempFolder = null;

    private String saveFolder = null;

    private String objectName = null;

    private String[] md5 = null;

    private String[] calculatedMD5s = null;

    private FileTransferInfo info = null;

    public long totalBytesReceived = 0;

    private int packetsInTempFile = 0;

    private long packetsLeft[] = null;

    private Vector filesReadyToAssemble = new Vector();

    Vector packetSources = new Vector();

    Vector pendingDeletes = new Vector();

    MapManager mapManager = null;

    public FileManager(FileTransferInfo fti, ClientManager _manager) {
        info = fti;
        manager = _manager;
        generateRootMap();
    }

    public FileManager(String _tempFolder, String _saveFolder, String _objectName, ClientManager _manager) {
        objectName = Common.setFolderName(_objectName);
        manager = _manager;
        tempFolder = _tempFolder;
        saveFolder = _saveFolder;
    }

    public synchronized void setFileTransferInfo(FileTransferInfo fti) {
        info = fti;
    }

    public synchronized void prepareForTransfer() {
        packetsLeft = new long[info.numFiles];
        for (int i = 0; i < info.numFiles; i++) packetsLeft[i] = info.totalNumPackets(i);
        generateStandardMap();
        if (allFilesSuccessfullyAssembled()) manager.fatalError(ClientGuiConstants.ERROR_FILE_SET_ALREADY_HERE, 109);
    }

    private void generateRootMap() {
        PacketMap map = new PacketMap(info.totalNumPackets());
        synchronized (map) {
            map.addPacketSet();
            map.setCurrentStartPacket(0);
            map.setCurrentEndPacket(info.totalNumPackets() - 1);
            synchronized (packetSources) {
                for (int i = 0; i < info.numFiles; i++) {
                    PacketSource source = new PacketSource();
                    source.fileName = Common.appendDel(info.filePath[i]) + info.fileName[i];
                    source.startPacket = info.getFileStartPacketNum(i);
                    packetSources.add(source);
                }
            }
            mapManager = new MapManager(map);
        }
    }

    private void generateStandardMap() {
        PacketMap map = new PacketMap(info.totalNumPackets());
        File[] tempFiles = null;
        synchronized (map) {
            File f = new File(tempFolder);
            if ((f.exists()) && (f.isDirectory())) tempFiles = f.listFiles();
            for (int i = 0; i < info.numFiles; i++) {
                f = new File(saveFolder + info.fileName[i]);
                if ((f.exists()) && (f.length() == info.fileSize[i])) {
                    map.addPacketSet();
                    map.setCurrentStartPacket(info.getFileStartPacketNum(i));
                    map.setCurrentEndPacket(info.getFileFinishPacketNum(i));
                    info.filePath[i] = saveFolder;
                    info.fileStatus[i] = "Saved";
                    synchronized (packetSources) {
                        PacketSource source = new PacketSource();
                        source.fileName = saveFolder + info.fileName[i];
                        source.startPacket = info.getFileStartPacketNum(i);
                        packetSources.add(source);
                    }
                    packetsLeft[i] = 0;
                    totalBytesReceived += f.length();
                } else if (tempFiles != null) {
                    for (int j = 0; j < tempFiles.length; j++) {
                        String prefix = objectName + "_" + i + "_";
                        String tempFileName = tempFiles[j].getName();
                        int pos = tempFileName.indexOf(prefix);
                        if (pos == 0) try {
                            if ((tempFiles[j].length() % Constants.PACKET_SIZE == 0) && (tempFiles[j].length() > 0)) {
                                String substr = tempFileName.substring(prefix.length(), tempFileName.indexOf(".", prefix.length()));
                                long start = new Long(substr).longValue();
                                long packetCount = tempFiles[j].length() / Constants.PACKET_SIZE;
                                PacketMap tpmMap = new PacketMap(map.totalNumPackets);
                                tpmMap.addPacketSet();
                                tpmMap.setCurrentStartPacket(start);
                                tpmMap.setCurrentEndPacket(start + packetCount - 1);
                                PacketMap intersection = PacketMap.intersection(map, tpmMap);
                                if ((intersection == null) || (intersection.countPacketsRecieved() == 0)) {
                                    map.addPacketSet();
                                    map.setCurrentStartPacket(start);
                                    map.setCurrentEndPacket(start + packetCount - 1);
                                    packetsLeft[i] -= packetCount;
                                    totalBytesReceived += tempFiles[j].length();
                                    synchronized (packetSources) {
                                        PacketSource source = new PacketSource();
                                        source.fileName = tempFolder + tempFileName;
                                        source.startPacket = start;
                                        source.setNumPackets(packetCount);
                                        packetSources.add(source);
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        map = PacketMap.simplify(map);
        mapManager = new MapManager(map);
        if (calculatedMD5s == null) {
            calculatedMD5s = new String[info.numFiles];
            for (int i = 0; i < info.numFiles; i++) calculatedMD5s[i] = null;
        }
    }

    public DataInputStream prepareRead(long pNum, PacketReader pr) throws Exception {
        PacketSource source = getPacketSource(pNum);
        if ((source.fileName.equals(pr.currentFile)) && (pNum == pr.lastReadPacket + 1)) {
            pr.lastReadPacket = pNum;
            return pr.in;
        } else {
            if (pr.in != null) pr.closeIn();
            for (int i = 0; i < 10; i++) {
                try {
                    pr.in = new DataInputStream(new FileInputStream(source.fileName));
                    break;
                } catch (Exception e) {
                    Sleep(1000);
                }
            }
            pr.currentFile = source.fileName;
            long bytesToSkip = Constants.PACKET_SIZE * (pNum - source.startPacket);
            for (int i = 0; i < 30; i++) {
                try {
                    bytesToSkip -= pr.in.skip(bytesToSkip);
                } catch (Exception e) {
                }
                if (bytesToSkip == 0) break; else Sleep(500);
            }
            if (bytesToSkip != 0) throw new Exception();
            pr.lastReadPacket = pNum;
            return pr.in;
        }
    }

    public DataOutputStream prepareWrite(long pNum, PacketWriter pw) throws Exception {
        int fileIndex = info.getFileIndex(pNum);
        if ((pw.out != null) && (pw.lastWrittenPacket == pNum - 1) && (fileIndex == info.getFileIndex(pNum - 1)) && (packetsInTempFile < 20)) {
            pw.lastWrittenPacket = pNum;
            packetsInTempFile++;
            return pw.out;
        } else {
            pw.closeOut();
            String tempFileName = tempFolder + objectName + "_" + fileIndex + "_" + pNum + ".tpm";
            pw.out = new DataOutputStream(new FileOutputStream(tempFileName));
            pw.currentFile = tempFileName;
            pw.lastWrittenPacket = pNum;
            packetsInTempFile = 1;
            synchronized (packetSources) {
                PacketSource source = new PacketSource();
                source.fileName = tempFileName;
                source.startPacket = pNum;
                packetSources.add(source);
            }
            return pw.out;
        }
    }

    public void writeComplete(long pNum) {
        mapManager.addPacket(pNum);
        totalBytesReceived += info.getPacketSize(pNum);
        manager.updateByteCount(totalBytesReceived);
        packetsLeft[info.getFileIndex(pNum)]--;
        getPacketSource(pNum).packetWritten();
        checkForFilesReadyToAssemble();
    }

    private PacketSource getPacketSource(long pNum) {
        synchronized (packetSources) {
            PacketSource closestSource = null;
            long closestStart = -1;
            for (int i = 0; i < packetSources.size(); i++) {
                PacketSource next = ((PacketSource) packetSources.elementAt(i));
                if ((next.startPacket <= pNum) && (next.startPacket > closestStart)) {
                    closestSource = next;
                    closestStart = next.startPacket;
                }
            }
            return closestSource;
        }
    }

    public void assembleFile(int fileIndex) {
        if (packetsLeft[fileIndex] > 0) return;
        if (fileIndex < 0) return;
        if (info.fileStatus[fileIndex].equals("Saved")) return;
        info.fileStatus[fileIndex] = "Saving";
        createSaveFolder();
        File finalFile = new File(tempFolder + "assemble" + System.currentTimeMillis() + ".tpm");
        FileOutputStream out = null;
        FileInputStream in = null;
        boolean validateMD5 = true;
        try {
            final int transferLength = 1000;
            boolean success = false;
            long currentSegmentLength = 0;
            do {
                try {
                    finalFile.delete();
                    out = new FileOutputStream(finalFile);
                } catch (Exception e) {
                    try {
                        info.fileStatus[fileIndex] = "Error 106";
                    } catch (Exception e2) {
                    }
                    manager.fatalError(ClientGuiConstants.ERROR_COULD_NOT_CREATE_FILE, 106);
                    return;
                }
                long pos = info.getFileStartPacketNum(fileIndex);
                long numTransferred = 0;
                try {
                    long size = info.fileSize[fileIndex];
                    while (numTransferred < size) {
                        String tempFileName = tempFolder + objectName + "_" + fileIndex + "_" + pos + ".tpm";
                        File f = null;
                        try {
                            f = new File(tempFileName);
                            in = new FileInputStream(f);
                        } catch (Exception e) {
                            try {
                                Thread.sleep(3000);
                            } catch (Exception e2) {
                            }
                            f = new File(tempFileName);
                            in = new FileInputStream(f);
                        }
                        long numPackets = getPacketSource(pos).getNumPackets();
                        long numBytesToRead = numPackets * Constants.PACKET_SIZE;
                        if (numTransferred + numBytesToRead > size) numBytesToRead = size - numTransferred;
                        int numActuallyRead = 0;
                        int pass = 10000;
                        byte[] temp1 = new byte[pass];
                        byte[] temp;
                        do {
                            pass = 10000;
                            if (numBytesToRead < pass) {
                                pass = (int) numBytesToRead;
                                temp = new byte[pass];
                            } else temp = temp1;
                            numActuallyRead = in.read(temp);
                            if (numActuallyRead >= 0) {
                                out.write(temp, 0, numActuallyRead);
                                numBytesToRead -= numActuallyRead;
                                numTransferred += numActuallyRead;
                                currentSegmentLength += numActuallyRead;
                                if (currentSegmentLength >= 500000) {
                                    try {
                                        currentSegmentLength = 0;
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                    }
                                } else {
                                    Thread.yield();
                                }
                            }
                        } while (numBytesToRead > 0);
                        pos += numPackets;
                        in.close();
                    }
                    out.close();
                    try {
                        if ((md5 != null) && (md5.length == info.numFiles)) {
                            while (finalFile.length() < info.fileSize[fileIndex]) this.Sleep(1000);
                            info.fileStatus[fileIndex] = "Validating MD5 Checksum";
                            MD5Listener listener = new MD5Listener(info, fileIndex);
                            String calculated = MD5.getMD5(finalFile, listener, true);
                            validateMD5 = ((md5[fileIndex] == null) || (md5[fileIndex].length() != 32) || (calculated.equals(md5[fileIndex])));
                            if (validateMD5) {
                                calculatedMD5s[fileIndex] = calculated;
                            } else {
                                if (calculatedMD5s[fileIndex] == null) {
                                    calculatedMD5s[fileIndex] = calculated;
                                } else {
                                    if (calculatedMD5s[fileIndex].equals(calculated)) {
                                    } else {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                    success = true;
                    if (validateMD5) {
                        info.filePath[fileIndex] = saveFolder;
                        info.fileStatus[fileIndex] = "Saved";
                    } else {
                        info.fileStatus[fileIndex] = "Failed MD5, Retrying";
                    }
                } catch (Exception e) {
                    cleanUp(in, out);
                    e.printStackTrace();
                    info.fileStatus[fileIndex] = "Error 150";
                    return;
                }
                Thread.yield();
            } while (!success);
            if (!validateMD5) {
                try {
                    mapManager.removePackets(info.getFileStartPacketNum(fileIndex), info.getFileFinishPacketNum(fileIndex));
                    packetsLeft[fileIndex] = info.totalNumPackets(fileIndex);
                    totalBytesReceived -= info.fileSize[fileIndex];
                    manager.updateByteCountOnly(totalBytesReceived);
                } catch (Exception e) {
                }
            }
            try {
                synchronized (packetSources) {
                    long startPacket = info.getFileStartPacketNum(fileIndex);
                    long endPacket = info.getFileFinishPacketNum(fileIndex);
                    for (int i = packetSources.size() - 1; i >= 0; i--) if ((((PacketSource) packetSources.elementAt(i)).startPacket >= startPacket) && (((PacketSource) packetSources.elementAt(i)).startPacket <= endPacket)) packetSources.removeElementAt(i);
                    if (validateMD5) {
                        PacketSource newSource = new PacketSource();
                        newSource.fileName = saveFolder + info.fileName[fileIndex];
                        newSource.startPacket = startPacket;
                        packetSources.add(newSource);
                    }
                }
            } catch (Exception e) {
                cleanUp(in, out);
                try {
                } catch (Exception e2) {
                }
                info.fileStatus[fileIndex] = "Error 151";
                manager.fatalError(ClientGuiConstants.ERROR_SHOULDNT_HAPPEN, 151);
                return;
            }
            deleteTempFiles(fileIndex);
        } catch (Exception e) {
            cleanUp(in, out);
            e.printStackTrace();
            return;
        }
        if ((finalFile != null) && (finalFile.exists())) {
            try {
                File saveDest = new File(saveFolder + info.fileName[fileIndex]);
                boolean success = false;
                int numTries = 0;
                do {
                    Sleep(500);
                    if (validateMD5) {
                        if (Common.getExtension(info.fileName[fileIndex]).equals("txt") || Common.getExtension(info.fileName[fileIndex]).equals("md5")) finalFile.setReadOnly();
                        success = finalFile.renameTo(saveDest);
                    } else {
                        success = finalFile.delete();
                    }
                    numTries++;
                } while ((!success) && (numTries < 30));
                if ((!success) && (validateMD5)) {
                    fileCopy(finalFile.getAbsolutePath(), saveDest.getAbsolutePath());
                    setPendingDelete(finalFile.getAbsolutePath());
                }
            } catch (Exception e) {
            }
        }
    }

    private void cleanUp(FileInputStream in, FileOutputStream out) {
        try {
            in.close();
        } catch (Exception e2) {
        }
        try {
            out.close();
        } catch (Exception e2) {
        }
    }

    private void deleteTempFiles(int fileIndex) {
        try {
            String prefix = objectName + "_" + fileIndex + "_";
            File f = new File(tempFolder);
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) if (files[i].getName().indexOf(prefix) == 0) {
                try {
                    if (!files[i].delete()) setPendingDelete(tempFolder + files[i].getName());
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    public FileTransferInfo getInfo() {
        return info;
    }

    public String getSaveFolder() {
        return saveFolder;
    }

    public void checkForFilesReadyToAssemble() {
        try {
            for (int i = 0; i < info.numFiles; i++) if ((packetsLeft[i] == 0) && (info.filePath[i] == null)) {
                boolean found = false;
                synchronized (filesReadyToAssemble) {
                    for (int j = 0; j < filesReadyToAssemble.size(); j++) if (((Integer) filesReadyToAssemble.elementAt(j)).intValue() == i) found = true;
                    if (!found) {
                        filesReadyToAssemble.add(new Integer(i));
                        manager.notifyAssembler();
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public void assemblePendingFiles() {
        do {
            Vector nextGroup = new Vector();
            synchronized (filesReadyToAssemble) {
                if (filesReadyToAssemble.size() == 0) return;
                for (int i = 0; i < filesReadyToAssemble.size(); i++) nextGroup.add(filesReadyToAssemble.elementAt(i));
                filesReadyToAssemble.removeAllElements();
            }
            for (int i = 0; i < nextGroup.size(); i++) assembleFile(((Integer) nextGroup.elementAt(i)).intValue());
        } while (true);
    }

    private void createSaveFolder() {
        try {
            File f = new File(saveFolder);
            f.mkdir();
            try {
                FileOutputStream out = new FileOutputStream(saveFolder + Constants.DO_NOT_EDIT_FILENAME);
                out.write(Constants.DO_NOT_EDIT_MESSAGE.getBytes());
                out.close();
            } catch (Exception e2) {
            }
        } catch (Exception e) {
            manager.fatalError(ClientGuiConstants.ERROR_COULD_NOT_CREATE_FILE, 107);
        }
    }

    public synchronized void removeTempFolder() {
        try {
            String prefix = objectName + "_";
            File f = new File(tempFolder);
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) if (files[i].getName().indexOf(prefix) == 0) {
                try {
                    if (!files[i].delete()) setPendingDelete(tempFolder + files[i].getName());
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        try {
            File f = new File(tempFolder + "furthur_info.cfg");
            f.delete();
            f = new File(tempFolder);
            if (!f.delete()) setPendingDelete(tempFolder);
        } catch (Exception e) {
        }
        try {
            new File(saveFolder + Constants.DO_NOT_EDIT_FILENAME).delete();
        } catch (Exception e) {
        }
    }

    public synchronized boolean allFilesSuccessfullyAssembled() {
        try {
            for (int i = 0; i < info.numFiles; i++) if (info.filePath[i] == null) return false;
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public void setPendingDelete(String fileName) {
        synchronized (pendingDeletes) {
            pendingDeletes.add(fileName);
        }
    }

    public void finishedWithFile(String fileName) {
        if (fileName == null) return;
        synchronized (pendingDeletes) {
            try {
                for (int i = pendingDeletes.size() - 1; i >= 0; i--) if (fileName.equals((String) pendingDeletes.elementAt(i))) {
                    File f = new File(fileName);
                    if ((!f.exists()) || (f.delete())) pendingDeletes.removeElementAt(i);
                }
                for (int i = pendingDeletes.size() - 1; i >= 0; i--) if (fileName.indexOf((String) pendingDeletes.elementAt(i)) == 0) {
                    File f = new File((String) pendingDeletes.elementAt(i));
                    if ((!f.exists()) || (f.delete())) pendingDeletes.removeElementAt(i);
                }
            } catch (Exception e) {
            }
        }
    }

    public void deleteAllPending() {
        synchronized (pendingDeletes) {
            try {
                for (int i = pendingDeletes.size() - 1; i >= 0; i--) {
                    File f = new File(((String) pendingDeletes.elementAt(i)));
                    if ((!f.exists()) || (f.delete())) pendingDeletes.removeElementAt(i);
                }
            } catch (Exception e) {
            }
        }
    }

    private void Sleep(int del) {
        try {
            Thread.sleep(del);
        } catch (Exception e) {
        }
    }

    public int getNumFiles() {
        if (info == null) return 0; else return info.numFiles;
    }

    public String getFileName(int i) {
        return info.fileName[i];
    }

    public long getFileSize(int i) {
        return info.fileSize[i];
    }

    public double getFilePercent(int i) {
        long total = info.totalNumPackets(i);
        return 100.0 * (double) (total - packetsLeft[i]) / total;
    }

    public String getFileStatus(int i) {
        return info.fileStatus[i];
    }

    public synchronized long getPacketsRemaining() {
        if (mapManager == null) return 100; else return mapManager.getPacketsRemaining();
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public synchronized boolean validateMD5(String encodedMd5) {
        int i;
        String[] newMd5 = Common.tokenize(encodedMd5, info.numFiles);
        for (i = 0; i < info.numFiles; i++) if (newMd5[i].length() != 32) return false;
        if (md5 == null) {
            md5 = newMd5;
        } else {
            for (i = 0; i < info.numFiles; i++) if (!Common.equalStrings(newMd5[i], md5[i])) return false;
        }
        for (i = 0; i < info.numFiles; i++) if (info.filePath[i] != null) calculatedMD5s[i] = md5[i];
        return true;
    }

    private class MD5Listener implements MD5StatusListener {

        FileTransferInfo fti = null;

        long total = 0;

        int fileIndex = -1;

        public MD5Listener(FileTransferInfo _fti, int _fileIndex) {
            fti = _fti;
            fileIndex = _fileIndex;
        }

        public void updateStatus(long bytesProcessed) {
            fti.fileStatus[fileIndex] = (int) ((bytesProcessed + total) * 100.0 / info.fileSize[fileIndex]) + "% Validating MD5 Checksum";
        }

        public void completed(long bytesProcessed) {
            total += bytesProcessed;
        }
    }

    public boolean allMD5sMatch() {
        try {
            for (int i = 0; i < info.numFiles; i++) if (!md5[i].equals(calculatedMD5s[i])) return false;
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static void move(String input, String output) {
        File inputFile = new File(input);
        File outputFile = new File(output);
        inputFile.renameTo(outputFile);
    }

    public static void fileCopy(String src, String dst) {
        try {
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dst);
            int read = -1;
            byte[] buf = new byte[8192];
            while ((read = fis.read(buf)) != -1) {
                fos.write(buf, 0, read);
            }
            fos.flush();
            fos.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
