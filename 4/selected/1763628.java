package pf;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import basic.*;

public class PF_Manager implements PF_Manager_Interface {

    private PF_BufferMgr pBufferMgr;

    private Hashtable openFiles = new Hashtable();

    public PF_Manager() {
        pBufferMgr = new PF_BufferMgr();
        openFiles = null;
    }

    public ReturnCode CreateFile(String filename) {
        File f = new File(filename);
        if (f.exists()) return ReturnCode.PF_FILEEXIST;
        try {
            f.createNewFile();
            RandomAccessFile ioStream = new RandomAccessFile(f, "rws");
            PageNum numPages = new PageNum(1);
            PageNum fisrtFree = new PageNum(0);
            PF_FileHdr hdr = new PF_FileHdr(fisrtFree, numPages);
            ioStream.setLength(Constant.PF_PAGE_SIZE);
            int fFree = Integer.parseInt(fisrtFree.toString());
            int nPages = Integer.parseInt(numPages.toString());
            ioStream.writeInt(fFree);
            ioStream.writeInt(nPages);
            ioStream.close();
        } catch (FileNotFoundException e) {
            return ReturnCode.PF_CANNOTCREATEFILE;
        } catch (IOException e) {
            return ReturnCode.PF_CANNOTCREATEFILE;
        }
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode OpenFile(String filename, PF_FileHandle fileHandle) throws IOException {
        File f = new File(filename);
        if (!f.exists()) return ReturnCode.PF_FILENOTFOUND;
        if (openFiles.get(filename).equals(fileHandle)) return ReturnCode.PF_FILEOPEN;
        fileHandle = getFileHandle(f);
        openFiles.put(fileHandle.getFileChanel(), fileHandle);
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode CloseFile(PF_FileHandle fileHandle) {
        if (!openFiles.contains(fileHandle)) return ReturnCode.PF_CLOSEDFILE;
        fileHandle.flushPages();
        FileChannel fc = fileHandle.getFileChanel();
        if (fc == null) return ReturnCode.PF_CLOSEDFILE;
        try {
            fc.close();
        } catch (IOException e) {
            return ReturnCode.PF_EOF;
        }
        openFiles.remove(fileHandle.getFileChanel());
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode DestroyFile(String filename) {
        File f = new File(filename);
        if (!f.exists()) return ReturnCode.PF_FILENOTFOUND;
        if (openFiles.containsKey(filename)) return ReturnCode.PF_FILEOPEN;
        if (!f.delete()) return ReturnCode.PF_FAILEDTODELETEFILE;
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode clearBuffer() {
        return pBufferMgr.clearBuffer();
    }

    public ReturnCode printBuffer() {
        return pBufferMgr.printBuffer();
    }

    public ReturnCode ResizeBuffer(int iNewSize) {
        return pBufferMgr.ResizeBuffer(iNewSize);
    }

    public ReturnCode GetBlockSize(int length) {
        return pBufferMgr.GetBlockSize(length);
    }

    public ReturnCode AllocateBlock(byte[] buffer) {
        return pBufferMgr.AllocateBlock(buffer);
    }

    public ReturnCode DisposeBlock(byte[] buffer) {
        return pBufferMgr.DisposeBlock(buffer);
    }

    private PF_FileHandle getFileHandle(File f) throws IOException {
        PF_FileHdr hdr = null;
        PF_BufferMgr pBufMgr = null;
        RandomAccessFile ioStream = new RandomAccessFile(f, "rws");
        FileChannel fc = ioStream.getChannel();
        ioStream.seek(0);
        hdr.setNumPages(new PageNum(ioStream.readInt()));
        hdr.setFirstFree(new PageNum(ioStream.readInt()));
        pBufMgr = new PF_BufferMgr();
        return new PF_FileHandle(pBufMgr, hdr, true, true, fc);
    }

    public ReturnCode PrintBuffer() {
        return pBufferMgr.printBuffer();
    }

    public ReturnCode ClearBuffer() {
        return pBufferMgr.clearBuffer();
    }
}
