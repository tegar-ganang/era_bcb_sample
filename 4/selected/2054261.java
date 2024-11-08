package pf;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import basic.*;

public class PF_FileHandle {

    private PF_BufferMgr bufferMgr = null;

    private PF_FileHdr hdr = null;

    private boolean fileOpen = false;

    private boolean hdrChanged = false;

    private FileDescriptor descriptor = null;

    private FileChannel fileChannel;

    public PF_FileHandle() {
        this.fileOpen = false;
        this.bufferMgr = null;
    }

    public PF_FileHandle(PF_BufferMgr bufferMgr, PF_FileHdr hdr, boolean fileOpen, boolean hdrChanged, FileChannel fileChannel) {
        this.bufferMgr = bufferMgr;
        this.hdr = hdr;
        this.fileOpen = fileOpen;
        this.hdrChanged = hdrChanged;
        this.fileChannel = fileChannel;
    }

    public PF_FileHandle(PF_FileHandle fhd) {
        this.bufferMgr = fhd.bufferMgr;
        this.hdr = fhd.hdr;
        this.fileOpen = fhd.fileOpen;
        this.hdrChanged = fhd.hdrChanged;
        this.descriptor = fhd.descriptor;
    }

    public ReturnCode getFirstPage(PF_PageHandle phd) {
        return (this.getNextPage(new PageNum(-1), phd));
    }

    public ReturnCode getNextPage(PageNum current, PF_PageHandle phd) {
        ReturnCode rc;
        if (!this.fileOpen) return ReturnCode.PF_INVALIDPAGE;
        if (current.getPageNum() != -1 && !this.isValidPageNum(current)) {
            return ReturnCode.PF_INVALIDPAGE;
        }
        int iCurrent = current.getPageNum();
        int iNumPages = hdr.getNumPages().getPageNum();
        PageNum pNum = new PageNum(iCurrent);
        for (iCurrent++; iCurrent < iNumPages; iCurrent++) {
            pNum.setPageNum(iCurrent);
            rc = getThisPage(pNum, phd);
            if (rc == ReturnCode.PF_SUCCESS) {
                return rc;
            }
            if (rc != ReturnCode.PF_INVALIDPAGE) {
                return rc;
            }
        }
        return ReturnCode.PF_EOF;
    }

    public ReturnCode getThisPage(PageNum pageNum, PF_PageHandle phd) {
        ReturnCode rc;
        if (!this.fileOpen) return ReturnCode.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) {
            return ReturnCode.PF_INVALIDPAGE;
        }
        BytePointer bufWrapper = new BytePointer();
        rc = bufferMgr.getPage(descriptor, pageNum, bufWrapper, true);
        PF_PageHdr phdr = getHdr(bufWrapper.addr);
        if (phdr.nextFree.equals(PageNum.PF_PAGE_USED)) {
            phd.pageNum.setPageNum(pageNum.getPageNum());
            phd.refData = ByteBuffer.wrap(bufWrapper.addr, 4, bufWrapper.addr.length - 4).array();
            return ReturnCode.PF_SUCCESS;
        }
        rc = this.unpinPage(pageNum);
        if (rc != ReturnCode.PF_SUCCESS) {
            return rc;
        }
        return ReturnCode.PF_INVALIDPAGE;
    }

    public ReturnCode getLastPage(PF_PageHandle phd) {
        return this.getPrevPage(hdr.getNumPages(), phd);
    }

    public ReturnCode getPrevPage(PageNum current, PF_PageHandle phd) {
        ReturnCode rc;
        if (!this.fileOpen) return ReturnCode.PF_CLOSEDFILE;
        if (current.getPageNum() != hdr.getNumPages().getPageNum() && !this.isValidPageNum(current)) {
            return ReturnCode.PF_INVALIDPAGE;
        }
        PageNum pageNumBuf = new PageNum(current.getPageNum());
        int iCurrent = current.getPageNum();
        for (iCurrent--; iCurrent >= 0; iCurrent--) {
            pageNumBuf.setPageNum(iCurrent);
            rc = this.getThisPage(pageNumBuf, phd);
            if (rc != ReturnCode.PF_SUCCESS) {
                return rc;
            }
            if (rc != ReturnCode.PF_INVALIDPAGE) {
                return rc;
            }
        }
        return ReturnCode.PF_EOF;
    }

    public PF_PageHdr getHdr(byte[] pageBuf) {
        ByteBuffer buf = ByteBuffer.wrap(pageBuf);
        int value = buf.getInt();
        PF_PageHdr h = new PF_PageHdr();
        h.nextFree = new PageNum(value);
        return h;
    }

    public ReturnCode allocatePage(PF_PageHandle phd) {
        if (!this.fileOpen) {
            return ReturnCode.PF_CLOSEDFILE;
        }
        PageNum pageNum = new PageNum(0);
        ReturnCode rc;
        BytePointer ptr = new BytePointer();
        if (this.hdr.getFirstFree() != PageNum.PF_PAGE_LIST_END) {
            pageNum = this.hdr.getFirstFree();
            rc = this.bufferMgr.getPage(this.descriptor, pageNum, ptr, true);
            if (rc != ReturnCode.PF_SUCCESS) return rc;
            this.hdr.setFirstFree(getHdr(phd.refData).nextFree);
        } else {
            pageNum = hdr.getNumPages();
            rc = this.bufferMgr.allocatePage(this.descriptor, pageNum, ptr.addr);
            if (rc != ReturnCode.PF_SUCCESS) return rc;
            this.hdr.getNumPages().addOne();
        }
        this.hdrChanged = true;
        getHdr(phd.refData).nextFree = PageNum.PF_PAGE_USED;
        rc = this.markDirty(pageNum);
        if (rc != ReturnCode.PF_SUCCESS) {
            return rc;
        }
        phd.pageNum = pageNum;
        phd.refData = ByteBuffer.wrap(ptr.addr, 4, ptr.addr.length - 4).array();
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode disposePage(PageNum pageNum) {
        ReturnCode rc;
        BytePointer ptr = new BytePointer();
        if (!this.fileOpen) {
            return ReturnCode.PF_CLOSEDFILE;
        }
        if (!this.isValidPageNum(pageNum)) {
            return ReturnCode.PF_INVALIDPAGE;
        }
        rc = this.bufferMgr.getPage(this.descriptor, pageNum, ptr, false);
        if (rc == ReturnCode.PF_SUCCESS) {
            return rc;
        }
        if (this.getHdr(ptr.addr).nextFree != PageNum.PF_PAGE_USED) {
            if ((rc = this.unpinPage(pageNum)) != ReturnCode.PF_SUCCESS) {
                return rc;
            }
            return ReturnCode.PF_PAGEFREE;
        }
        this.getHdr(ptr.addr).nextFree = hdr.getFirstFree();
        hdr.setFirstFree(pageNum);
        this.hdrChanged = true;
        if ((rc = this.markDirty(pageNum)) != ReturnCode.PF_SUCCESS) {
            return rc;
        }
        if ((rc = this.unpinPage(pageNum)) != ReturnCode.PF_SUCCESS) {
            return rc;
        }
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode markDirty(PageNum pageNum) {
        if (!this.fileOpen) return ReturnCode.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) return ReturnCode.PF_INVALIDPAGE;
        return this.bufferMgr.MarkDirty(this.descriptor, pageNum);
    }

    public ReturnCode unpinPage(PageNum pageNum) {
        if (!this.fileOpen) return ReturnCode.PF_CLOSEDFILE;
        if (!this.isValidPageNum(pageNum)) return ReturnCode.PF_INVALIDPAGE;
        return this.bufferMgr.unpinPage(this.descriptor, pageNum);
    }

    public ReturnCode writeFileHdr() {
        FileOutputStream out = new FileOutputStream(this.descriptor);
        FileChannel channel = out.getChannel();
        ByteBuffer bbuf = ByteBuffer.allocate(8);
        bbuf.putInt(this.hdr.getFirstFree().getPageNum());
        bbuf.putInt(this.hdr.getNumPages().getPageNum());
        try {
            channel.write(bbuf, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ReturnCode.PF_SUCCESS;
    }

    public ReturnCode flushPages() {
        ReturnCode rc;
        if (!this.fileOpen) {
            return ReturnCode.PF_CLOSEDFILE;
        }
        if (this.hdrChanged) {
            rc = this.writeFileHdr();
            if (rc != ReturnCode.PF_SUCCESS) {
                return rc;
            }
            this.hdrChanged = false;
        }
        return this.bufferMgr.flushPages(this.descriptor);
    }

    public ReturnCode forcePages(PageNum pageNum) {
        ReturnCode rc;
        if (!this.fileOpen) return ReturnCode.PF_CLOSEDFILE;
        if (this.hdrChanged) {
            rc = this.writeFileHdr();
            if (rc != ReturnCode.PF_SUCCESS) {
                return rc;
            }
            this.hdrChanged = false;
        }
        return this.bufferMgr.forcePages(this.descriptor, pageNum);
    }

    /**
	 * Private members
	 */
    private boolean isValidPageNum(PageNum pageNum) {
        return (this.fileOpen && pageNum.getPageNum() >= 0 && pageNum.getPageNum() < this.hdr.getNumPages().getPageNum());
    }

    public FileDescriptor getFileDescriptor() {
        return descriptor;
    }

    public FileChannel getFileChanel() {
        return fileChannel;
    }
}
