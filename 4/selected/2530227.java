package net.charabia.jsmoothgen.pe;

import net.charabia.jsmoothgen.pe.res.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 *
 * @author  Rodrigo
 */
public class PEFile {

    private File m_file;

    private FileInputStream m_in = null;

    private FileChannel m_channel = null;

    private PEOldMSHeader m_oldmsheader;

    private PEHeader m_header;

    private Vector m_sections = new Vector();

    private PEResourceDirectory m_resourceDir;

    /** Creates a new instance of PEFile */
    public PEFile(File f) {
        m_file = f;
    }

    public void close() throws IOException {
        m_in.close();
    }

    public void open() throws FileNotFoundException, IOException {
        m_in = new FileInputStream(m_file);
        m_channel = m_in.getChannel();
        m_oldmsheader = new PEOldMSHeader(this);
        m_oldmsheader.read();
        long headoffset = m_oldmsheader.e_lfanew;
        m_header = new PEHeader(this, headoffset);
        m_header.read();
        int seccount = m_header.NumberOfSections;
        long offset = headoffset + (m_header.NumberOfRvaAndSizes * 8) + 24 + 96;
        for (int i = 0; i < seccount; i++) {
            PESection sect = new PESection(this, offset);
            sect.read();
            m_sections.add(sect);
            offset += 40;
        }
        ByteBuffer resbuf = null;
        long resourceoffset = m_header.ResourceDirectory_VA;
        for (int i = 0; i < seccount; i++) {
            PESection sect = (PESection) m_sections.get(i);
            if (sect.VirtualAddress == resourceoffset) {
                PEResourceDirectory prd = new PEResourceDirectory(this, sect);
                resbuf = prd.buildResource(sect.VirtualAddress);
                break;
            }
        }
    }

    public FileChannel getChannel() {
        return m_channel;
    }

    public static void main(String args[]) throws IOException, CloneNotSupportedException, Exception {
        PEFile pe = new PEFile(new File("F:/Documents and Settings/Rodrigo/Mes documents/projects/jsmooth/skeletons/simplewrap/JWrap.exe"));
        pe.open();
        File fout = new File("F:/Documents and Settings/Rodrigo/Mes documents/projects/jsmooth/skeletons/simplewrap/gen-application.jar");
        FileInputStream fis = new FileInputStream(fout);
        ByteBuffer data = ByteBuffer.allocate((int) fout.length());
        data.order(ByteOrder.LITTLE_ENDIAN);
        FileChannel fischan = fis.getChannel();
        fischan.read(data);
        data.position(0);
        fis.close();
        PEResourceDirectory resdir = pe.getResourceDirectory();
        java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().getImage("c:\\gnome-color-browser2.png");
        java.awt.MediaTracker mt = new java.awt.MediaTracker(new javax.swing.JLabel("toto"));
        mt.addImage(img, 1);
        try {
            mt.waitForAll();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        ResIcon newicon = new ResIcon(img);
        pe.replaceDefaultIcon(newicon);
        File out = new File("F:/Documents and Settings/Rodrigo/Mes documents/projects/jsmooth/skeletons/simplewrap/COPIE.exe");
        pe.dumpTo(out);
    }

    public PEResourceDirectory getResourceDirectory() throws IOException {
        if (m_resourceDir != null) return m_resourceDir;
        long resourceoffset = m_header.ResourceDirectory_VA;
        for (int i = 0; i < m_sections.size(); i++) {
            PESection sect = (PESection) m_sections.get(i);
            if (sect.VirtualAddress == resourceoffset) {
                m_resourceDir = new PEResourceDirectory(this, sect);
                return m_resourceDir;
            }
        }
        return null;
    }

    public void dumpTo(File destination) throws IOException, CloneNotSupportedException {
        int outputcount = 0;
        FileOutputStream fos = new FileOutputStream(destination);
        FileChannel out = fos.getChannel();
        PEOldMSHeader oldmsheader = (PEOldMSHeader) this.m_oldmsheader.clone();
        PEHeader peheader = (PEHeader) m_header.clone();
        Vector sections = new Vector();
        for (int i = 0; i < m_sections.size(); i++) {
            PESection sect = (PESection) m_sections.get(i);
            PESection cs = (PESection) sect.clone();
            sections.add(cs);
        }
        long newexeoffset = oldmsheader.e_lfanew;
        ByteBuffer msheadbuffer = oldmsheader.get();
        outputcount = out.write(msheadbuffer);
        this.m_channel.position(64);
        out.transferFrom(this.m_channel, 64, newexeoffset - 64);
        ByteBuffer headbuffer = peheader.get();
        out.position(newexeoffset);
        outputcount = out.write(headbuffer);
        long offset = oldmsheader.e_lfanew + (m_header.NumberOfRvaAndSizes * 8) + 24 + 96;
        out.position(offset);
        for (int i = 0; i < sections.size(); i++) {
            PESection sect = (PESection) sections.get(i);
            ByteBuffer buf = sect.get();
            outputcount = out.write(buf);
        }
        offset = 1024;
        long virtualAddress = offset;
        if ((virtualAddress % peheader.SectionAlignment) > 0) virtualAddress += peheader.SectionAlignment - (virtualAddress % peheader.SectionAlignment);
        long resourceoffset = m_header.ResourceDirectory_VA;
        for (int i = 0; i < sections.size(); i++) {
            PESection sect = (PESection) sections.get(i);
            if (resourceoffset == sect.VirtualAddress) {
                out.position(offset);
                long sectoffset = offset;
                PEResourceDirectory prd = this.getResourceDirectory();
                ByteBuffer resbuf = prd.buildResource(sect.VirtualAddress);
                resbuf.position(0);
                out.write(resbuf);
                offset += resbuf.capacity();
                long rem = offset % this.m_header.FileAlignment;
                if (rem != 0) offset += this.m_header.FileAlignment - rem;
                if (out.size() + 1 < offset) {
                    ByteBuffer padder = ByteBuffer.allocate(1);
                    out.write(padder, offset - 1);
                }
                long virtualSize = resbuf.capacity();
                if ((virtualSize % peheader.FileAlignment) > 0) virtualSize += peheader.SectionAlignment - (virtualSize % peheader.SectionAlignment);
                sect.PointerToRawData = sectoffset;
                sect.SizeOfRawData = resbuf.capacity();
                if ((sect.SizeOfRawData % this.m_header.FileAlignment) > 0) sect.SizeOfRawData += (this.m_header.FileAlignment - (sect.SizeOfRawData % this.m_header.FileAlignment));
                sect.VirtualAddress = virtualAddress;
                sect.VirtualSize = virtualSize;
                virtualAddress += virtualSize;
            } else if (sect.PointerToRawData > 0) {
                out.position(offset);
                this.m_channel.position(sect.PointerToRawData);
                long sectoffset = offset;
                out.position(offset + sect.SizeOfRawData);
                ByteBuffer padder = ByteBuffer.allocate(1);
                out.write(padder, offset + sect.SizeOfRawData - 1);
                long outted = out.transferFrom(this.m_channel, offset, sect.SizeOfRawData);
                offset += sect.SizeOfRawData;
                long rem = offset % this.m_header.FileAlignment;
                if (rem != 0) {
                    offset += this.m_header.FileAlignment - rem;
                }
                sect.PointerToRawData = sectoffset;
                sect.VirtualAddress = virtualAddress;
                virtualAddress += sect.VirtualSize;
                if ((virtualAddress % peheader.SectionAlignment) > 0) virtualAddress += peheader.SectionAlignment - (virtualAddress % peheader.SectionAlignment);
            } else {
                long virtualSize = sect.VirtualSize;
                if ((virtualSize % peheader.SectionAlignment) > 0) virtualSize += peheader.SectionAlignment - (virtualSize % peheader.SectionAlignment);
                sect.VirtualAddress = virtualAddress;
                virtualAddress += virtualSize;
            }
        }
        peheader.updateVAAndSize(m_sections, sections);
        headbuffer = peheader.get();
        out.position(newexeoffset);
        outputcount = out.write(headbuffer);
        offset = oldmsheader.e_lfanew + (m_header.NumberOfRvaAndSizes * 8) + 24 + 96;
        out.position(offset);
        for (int i = 0; i < sections.size(); i++) {
            PESection sect = (PESection) sections.get(i);
            ByteBuffer buf = sect.get();
            outputcount = out.write(buf);
        }
        fos.flush();
        fos.close();
    }

    public void replaceDefaultIcon(ResIcon icon) throws Exception {
        PEResourceDirectory resdir = getResourceDirectory();
        PEResourceDirectory.DataEntry entry = resdir.getData("#14", null, null);
        if (entry == null) {
            throw new Exception("Can't find any icon group in the file!");
        }
        entry.Data.position(0);
        entry.Data.position(0);
        ResIconDir rid = new ResIconDir(entry.Data);
        int iconid = rid.getEntries()[0].dwImageOffset;
        PEResourceDirectory.DataEntry iconentry = resdir.getData("#3", "#" + iconid, null);
        iconentry.Data.position(0);
        rid.getEntries()[0].bWidth = (short) icon.Width;
        rid.getEntries()[0].bHeight = (short) (icon.Height / 2);
        rid.getEntries()[0].bColorCount = (short) (1 << icon.BitsPerPixel);
        rid.getEntries()[0].wBitCount = icon.BitsPerPixel;
        rid.getEntries()[0].dwBytesInRes = icon.getData().remaining();
        iconentry.Data = icon.getData();
        iconentry.Size = iconentry.Data.remaining();
        entry.setData(rid.getData());
    }
}
