package tinybase.pf;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import tinybase.basic.BytePointer;
import tinybase.basic.RC;

public class PF_FileHandle {

    private PF_BufferMgr bufferMgr = null;

    private PF_FileHdr hdr = null;

    private boolean fileOpen = false;

    private boolean hdrChanged = false;

    private FileDescriptor descriptor = null;

    public void setDescriptor(FileDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PF_BufferMgr getBufferMgr() {
        return bufferMgr;
    }

    public void setBufferMgr(PF_BufferMgr bufferMgr) {
        this.bufferMgr = bufferMgr;
    }

    public boolean isHdrChanged() {
        return hdrChanged;
    }

    public void setHdrChanged(boolean hdrChanged) {
        this.hdrChanged = hdrChanged;
    }

    public PF_FileHdr getFileHeader() {
        return this.hdr;
    }

    public PF_FileHandle(PF_BufferMgr bufferMgr, PF_FileHdr hdr, boolean fileOpen, boolean hdrChanged, FileDescriptor fd) {
        this.bufferMgr = bufferMgr;
        this.hdr = hdr;
        this.setFileOpen(fileOpen);
        this.hdrChanged = hdrChanged;
        this.descriptor = fd;
    }

    public PF_FileHandle() {
        this.bufferMgr = null;
        this.descriptor = null;
        this.fileOpen = false;
        this.hdr = new PF_FileHdr(PageNum.PF_PAGE_LIST_END.getPageNum(), 0);
        this.hdrChanged = false;
    }

    public RC getFirstPage(PF_PageHandle phd) {
        return (this.getNextPage(-1, phd));
    }

    public RC getNextPage(int current, PF_PageHandle phd) {
        RC rc;
        if (!this.isFileOpen()) return RC.PF_INVALIDPAGE;
        if (current != -1 && !this.isValidPageNum(current)) {
            return RC.PF_INVALIDPAGE;
        }
        int iCurrent = current;
        int iNumPages = this.hdr.getNumPages();
        PageNum pNum = new PageNum(iCurrent);
        for (iCurrent++; iCurrent < iNumPages; iCurrent++) {
            rc = this.getThisPage(iCurrent, phd);
            if (rc == RC.PF_SUCCESS) {
                return rc;
            }
            if (rc != RC.PF_INVALIDPAGE) {
                return rc;
            }
        }
        return RC.PF_EOF;
    }

    public RC getThisPage(int pageNum, PF_PageHandle phd) {
        RC rc;
        if (!this.isFileOpen()) return RC.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) {
            return RC.PF_INVALIDPAGE;
        }
        BytePointer bufWrapper = new BytePointer();
        rc = this.bufferMgr.getPage(this.descriptor, new PageNum(pageNum), bufWrapper, true);
        int val = this.getNextFree(bufWrapper.getArray());
        if (val == PageNum.PF_PAGE_USED.getPageNum()) {
            phd.setPageNum(pageNum);
            phd.setData(ByteBuffer.wrap(bufWrapper.getArray(), 4, bufWrapper.getArray().length - 4).array());
            return RC.PF_SUCCESS;
        }
        rc = this.unpinPage(pageNum);
        if (rc != RC.PF_SUCCESS) {
            return rc;
        }
        return RC.PF_INVALIDPAGE;
    }

    public RC getLastPage(PF_PageHandle phd) {
        return this.getPrevPage(hdr.getNumPages(), phd);
    }

    public RC getPrevPage(int current, PF_PageHandle phd) {
        RC rc;
        if (!this.isFileOpen()) return RC.PF_CLOSEDFILE;
        if (current != hdr.getNumPages() && !this.isValidPageNum(current)) {
            return RC.PF_INVALIDPAGE;
        }
        int iCurrent = current;
        for (iCurrent--; iCurrent >= 0; iCurrent--) {
            rc = this.getThisPage(iCurrent, phd);
            if (rc != RC.PF_SUCCESS) {
                return rc;
            }
            if (rc != RC.PF_INVALIDPAGE) {
                return rc;
            }
        }
        return RC.PF_EOF;
    }

    public int getNextFree(byte[] pageBuf) {
        ByteBuffer buf = ByteBuffer.wrap(pageBuf);
        int value = buf.getInt(0);
        return value;
    }

    public RC allocatePage(PF_PageHandle phd) {
        if (!this.isFileOpen()) {
            return RC.PF_CLOSEDFILE;
        }
        PageNum pageNum = new PageNum(0);
        RC rc;
        BytePointer ptr = new BytePointer();
        if (this.hdr.getFirstFree() != PageNum.PF_PAGE_LIST_END.getPageNum()) {
            pageNum.setPageNum(this.hdr.getFirstFree());
            rc = this.bufferMgr.getPage(this.descriptor, pageNum, ptr, true);
            if (rc != RC.PF_SUCCESS) return rc;
            this.hdr.setFirstFree(getNextFree(ptr.getArray()));
        } else {
            int numPages = hdr.getNumPages();
            pageNum.setPageNum(numPages);
            rc = this.bufferMgr.allocatePage(this.descriptor, pageNum, ptr);
            if (rc != RC.PF_SUCCESS) return rc;
            this.hdr.setNumPages(numPages + 1);
        }
        this.hdrChanged = true;
        byte[] arr = ptr.getArray();
        java.util.Arrays.fill(arr, (byte) 0);
        ByteBuffer buf = ByteBuffer.wrap(arr);
        buf.putInt(0, PageNum.PF_PAGE_USED.getPageNum());
        rc = this.markDirty(pageNum.getPageNum());
        if (rc != RC.PF_SUCCESS) {
            return rc;
        }
        phd.setPageNum(pageNum.getPageNum());
        phd.setData(ByteBuffer.wrap(ptr.getArray(), 4, ptr.getArray().length - 4).array());
        return RC.PF_SUCCESS;
    }

    public RC disposePage(int pageNum) {
        RC rc;
        BytePointer ptr = new BytePointer();
        if (!this.isFileOpen()) {
            return RC.PF_CLOSEDFILE;
        }
        if (!this.isValidPageNum(pageNum)) {
            return RC.PF_INVALIDPAGE;
        }
        rc = this.bufferMgr.getPage(this.descriptor, new PageNum(pageNum), ptr, false);
        if (rc == RC.PF_SUCCESS) {
            return rc;
        }
        if (this.getNextFree(ptr.getArray()) != PageNum.PF_PAGE_USED.getPageNum()) {
            if ((rc = this.unpinPage(pageNum)) != RC.PF_SUCCESS) {
                return rc;
            }
            return RC.PF_PAGEFREE;
        }
        ByteBuffer buf = ByteBuffer.wrap(ptr.getArray());
        buf.putInt(0, hdr.getFirstFree());
        hdr.setFirstFree(pageNum);
        this.hdrChanged = true;
        if ((rc = this.markDirty(pageNum)) != RC.PF_SUCCESS) {
            return rc;
        }
        if ((rc = this.unpinPage(pageNum)) != RC.PF_SUCCESS) {
            return rc;
        }
        return RC.PF_SUCCESS;
    }

    public RC markDirty(int pageNum) {
        if (!this.isFileOpen()) return RC.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) return RC.PF_INVALIDPAGE;
        return this.bufferMgr.markDirty(this.descriptor, new PageNum(pageNum));
    }

    public RC unpinPage(int pageNum) {
        if (!this.isFileOpen()) return RC.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) return RC.PF_INVALIDPAGE;
        return this.bufferMgr.unpinPage(this.descriptor, new PageNum(pageNum));
    }

    public RC writeFileHdr() {
        FileOutputStream out = new FileOutputStream(this.descriptor);
        FileChannel channel = out.getChannel();
        ByteBuffer bbuf = ByteBuffer.allocate(8);
        bbuf.putInt(this.hdr.getFirstFree());
        bbuf.putInt(this.hdr.getNumPages());
        try {
            channel.write(bbuf, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RC.PF_SUCCESS;
    }

    public RC flushPages() {
        RC rc;
        if (!this.isFileOpen()) {
            return RC.PF_CLOSEDFILE;
        }
        if (this.hdrChanged) {
            rc = this.writeFileHdr();
            if (rc != RC.PF_SUCCESS) {
                return rc;
            }
            this.hdrChanged = false;
        }
        return this.bufferMgr.flushPages(this.descriptor);
    }

    public RC forcePages(int pageNum) {
        RC rc;
        if (!this.isFileOpen()) return RC.PF_CLOSEDFILE;
        if (this.hdrChanged) {
            rc = this.writeFileHdr();
            if (rc != RC.PF_SUCCESS) {
                return rc;
            }
            this.hdrChanged = false;
        }
        return this.bufferMgr.forcePages(this.descriptor, new PageNum(pageNum));
    }

    private boolean isValidPageNum(int pageNum) {
        return (this.isFileOpen() && pageNum >= 0 && pageNum < this.hdr.getNumPages());
    }

    public FileDescriptor getDescriptor() {
        return descriptor;
    }

    /**
	 * @param fileOpen the fileOpen to set
	 */
    public void setFileOpen(boolean fileOpen) {
        this.fileOpen = fileOpen;
    }

    /**
	 * @return the fileOpen
	 */
    public boolean isFileOpen() {
        return fileOpen;
    }
}
