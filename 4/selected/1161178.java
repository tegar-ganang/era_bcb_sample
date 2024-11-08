package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

public class PE {

    /** The offset of the PE section in the file */
    private static final int PE_START = 0x3c;

    /** The code that identifies the version */
    private static final int RT_VERSION = 16;

    /** The string that identifies rsrc (".rsrc\0\0\0") */
    private static final long rsrc = 0x000000637273722EL;

    /** This is the entry point to the whole file version function set.  It takes the filename as a parameter
	 * and returns the version.  If the version can't be returned for whatever reason, an IOException is
	 * thrown.
	 *
	 * @param filename The filename to get the version of.
	 * @return The version of the file, as an integer.
	 * @throws IOException If the version can't be retrieved for some reason.
	 */
    public static int getVersion(String filename, boolean byteorder) throws IOException {
        int peStart;
        int peSignature;
        short numberOfSections;
        int ptrOptionalHeader;
        MappedByteBuffer file = new FileInputStream(filename).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, new File(filename).length());
        file.order(ByteOrder.LITTLE_ENDIAN);
        peStart = file.getInt(PE_START);
        peSignature = file.getInt(peStart + 0);
        if (peSignature != 0x00004550) throw new IOException("Invalid PE file!");
        numberOfSections = file.getShort(peStart + 6);
        ptrOptionalHeader = peStart + 24;
        return processOptionalHeader(file, ptrOptionalHeader, numberOfSections, byteorder);
    }

    /** This reads the optional header and returns the version, or throws an IOException if the version
	 * could not be found.
	 *
	 * @param file The file we're reading from.
	 * @param ptrOptionalHeader A pointer to the optional header.
	 * @param numberOfSections The number of sections that we will need to process.
	 * @return The version, always.
	 * @throws IOException If the version couldn't be found.
	 */
    private static int processOptionalHeader(MappedByteBuffer file, int ptrOptionalHeader, int numberOfSections, boolean byteorder) throws IOException {
        boolean plus;
        int numberOfRvaAndSizes;
        int ptrSectionTable;
        int version;
        plus = file.getShort(ptrOptionalHeader) == 0x020b;
        numberOfRvaAndSizes = file.getInt(ptrOptionalHeader + (plus ? 108 : 92));
        ptrSectionTable = ptrOptionalHeader + 96 + (numberOfRvaAndSizes * 8);
        version = processSections(file, ptrSectionTable, numberOfSections, byteorder);
        if (version == 0) throw new IOException("Couldn't find .rsrc section!");
        return version;
    }

    /** Step through the table of sections, looking for .rsrc.  When the .rsrc sections is found, call
	 * another function to process it.
	 *
	 * @param file The file we're reading from
	 * @param sectionsBase A pointer to the beginning of the sections.
	 * @param numberOfSections The number of sections.
	 * @return The version number, or 0 if it couldn't be found.
	 * @throws IOException If there is a problem finding the version.
	 */
    private static int processSections(MappedByteBuffer file, int sectionsBase, int numberOfSections, boolean byteorder) throws IOException {
        int virtualStart;
        int rawStart;
        int rsrcVirtualToRaw;
        int sectionBase;
        for (int i = 0; i < numberOfSections; i++) {
            virtualStart = file.getInt(sectionsBase + (i * 40) + 12);
            rawStart = file.getInt(sectionsBase + (i * 40) + 20);
            sectionBase = sectionsBase + (i * 40);
            rsrcVirtualToRaw = rawStart - virtualStart;
            if (file.getLong(sectionsBase + (i * 40)) == rsrc) return processResourceRecord(new LinkedList<Integer>(), file, 0, file.getInt(sectionBase + 20), rsrcVirtualToRaw, byteorder);
        }
        return 0;
    }

    /** This indirectly recursive function walks the resource tree by calling processEntry which, in turn,
	 * calls it.  The function looks specifically for the version section.  As soon as it finds a leaf node
	 * for a version section, it returns the data all the way back up the resursive stack.  The recursion
	 * will never go deeper than 3 levels.
	 *
	 * @param tree The "tree" up to this point -- a maximum of 3 levels.
	 * @param file The file we're processing.
	 * @param recordOffset The offset of the record that we're going to process.  It's added to the rsrcStart
	 *  value to get a pointer.
	 * @param rsrcStart The very beginning of the rsrc section.  Used as a base value.
	 * @param rsrcVirtualToRaw The value that has to be added to the virtual section to get the raw section.
	 * @return The version, or 0 if it couldn't be found.
	 * @throws IOException If there's an error finding the version.
	 */
    private static int processResourceRecord(LinkedList<Integer> tree, MappedByteBuffer file, int recordOffset, int rsrcStart, int rsrcVirtualToRaw, boolean byteorder) throws IOException {
        int i;
        int recordAddress = recordOffset + rsrcStart;
        short numberNameEntries = file.getShort(recordAddress + 12);
        short numberIDEntries = file.getShort(recordAddress + 14);
        int ptrIDEntriesBase;
        int entry;
        int version;
        ptrIDEntriesBase = recordAddress + 16 + (numberNameEntries * 8);
        for (i = 0; i < numberIDEntries; i++) {
            entry = ptrIDEntriesBase + (i * 8);
            version = processEntry(new LinkedList<Integer>(tree), file, entry, rsrcStart, rsrcVirtualToRaw, byteorder);
            if (version != 0) return version;
        }
        return 0;
    }

    /** Process an entry recursively.  If a leaf node is found, and it's the version node, return it.
	 *
	 * @param tree The list of nodes we've been to.
	 * @param file The file we're processing.
	 * @param entry A pointer to the start of the entry.
	 * @param rsrcStart A pointer to the beginning of the rsrc section.
	 * @param rsrcVirtualToRaw The conversion between the virtual and raw address.
	 * @return The version, or 0 if it wasn't found in this entry.
	 * @throws IOException If there's an error finding the version.
	 */
    private static int processEntry(LinkedList<Integer> tree, MappedByteBuffer file, int entry, int rsrcStart, int rsrcVirtualToRaw, boolean byteorder) throws IOException {
        int nextAddress = file.getInt(entry + 4);
        int version;
        int dataSize;
        byte[] buffer;
        int rawDataAddress;
        tree.addLast(file.getInt(entry + 0));
        if ((nextAddress & 0x80000000) != 0) {
            version = processResourceRecord(tree, file, nextAddress & 0x7FFFFFFF, rsrcStart, rsrcVirtualToRaw, byteorder);
            if (version != 0) return version;
        } else {
            if (tree.get(0) == RT_VERSION) {
                rawDataAddress = file.getInt(rsrcStart + nextAddress) + rsrcVirtualToRaw;
                dataSize = file.getInt(rsrcStart + nextAddress + 4);
                buffer = new byte[dataSize];
                file.position(rawDataAddress);
                file.get(buffer);
                if (byteorder) return buffer[0x3C] << 24 | buffer[0x3E] << 16 | buffer[0x38] << 8 | buffer[0x3A] << 0; else return buffer[0x3A] << 24 | buffer[0x38] << 16 | buffer[0x3E] << 8 | buffer[0x3C] << 0;
            }
        }
        return 0;
    }

    public static void main(String[] args) throws IOException {
    }
}
