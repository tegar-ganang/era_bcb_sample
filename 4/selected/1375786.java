package net.sf.jcablib.test;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.sf.jcablib.*;

/**
* This class provides a high level interface to the CabFile class.
* @author David Himelright <a href="mailto:dhimelright@gmail.com">dhimelright@gmail.com</a>
*/
public class CabDump extends CabFile {

    /**
	* @param cab Input file
	* @exception CabException the Cabinet header may be malformed
	* @exception IOException Problem reading the input file
	*/
    public CabDump(File inFile) throws IOException {
        super(inFile);
    }

    /**
	* High level extraction method.
	* @param cabfolder a CabFolder to fully extract
	* @param outputFolder a File reference to the output folder to extract into
	*/
    public void extract(CabFolder cabfolder, File outputFolder) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        File file;
        byte[] buffer = new byte[2048];
        int read, remain, maxread = buffer.length;
        in = new CabFileInputStream(cabfolder);
        CabEntry[] cabentries = this.getEntries();
        for (int i = 0; i < cabentries.length; i++) {
            if (cabentries[i].getCabFolder() == cabfolder) {
                file = new File(outputFolder, cabentries[i].getName().replace('\\', '/'));
                FileUtils.createParents(file);
                out = new FileOutputStream(file);
                remain = cabentries[i].getInflatedSize();
                while (remain != 0 && (read = in.read(buffer, 0, maxread)) != -1) {
                    out.write(buffer, 0, read);
                    remain -= read;
                    if (buffer.length > remain) maxread = remain; else maxread = buffer.length;
                }
                in.close();
                out.close();
            }
        }
    }

    public void reportHeader(PrintStream out) {
        out.println(getFile().getPath());
        if (cab_file_size != file_size) out.println("shorter than expected (maybe corrupt, but happens a lot).");
        if ((kFlagReserve & cab_flags) == kFlagReserve) out.println("Reserved data skipped");
        out.println("Header checksum  " + cab_header_checksum);
        out.println("File size        " + cab_file_size);
        out.println("Folder checksum  " + cab_folder_checksum);
        out.println("Entry offset     " + cab_entry_offset);
        out.println("File checksum    " + cab_file_checksum);
        out.println("Version          0x" + Integer.toHexString(cab_version));
        out.println("Folders          " + cab_folders);
        out.println("Files            " + cab_files);
        out.println("Flags            0x" + Integer.toHexString(cab_flags));
        out.println("Cabinet series   " + cab_setid);
        out.println("Cabinet number   " + cab_icab);
    }

    public void reportEntries(PrintStream out) {
        StringBuffer buffer;
        int total_file_size = 0;
        out.println("name--------------------------size------flags--format--folder--");
        for (int i = 0; i < entries.length; i++) {
            total_file_size += entries[i].getInflatedSize();
            buffer = new StringBuffer();
            int len = 30;
            buffer.append(entries[i].getName());
            len -= entries[i].getName().length();
            for (int j = 0; j < len; j++) buffer.append(' ');
            len = 10;
            len -= new String(entries[i].getInflatedSize() + "").length();
            buffer.append(entries[i].getInflatedSize());
            for (int j = 0; j < len; j++) buffer.append(' ');
            len = 5;
            len -= new String(entries[i].getAttributes() + "").length();
            buffer.append("0x" + Integer.toHexString(entries[i].getAttributes()));
            for (int j = 0; j < len; j++) buffer.append(' ');
            switch(entries[i].getMethod()) {
                case kNoCompression:
                    buffer.append("none    ");
                    break;
                case kMszipCompression:
                    buffer.append("zip     ");
                    break;
                case kQuantumCompression:
                    buffer.append("quantum ");
                    break;
                case kLzxCompression:
                    buffer.append("lzx     ");
                    break;
                default:
                    buffer.append("unknown ");
            }
            buffer.append(entries[i].getFolderIndex());
            out.println(buffer.toString());
        }
        out.println("total-------------------------" + total_file_size);
        out.println("");
    }
}
