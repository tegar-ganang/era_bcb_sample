package bookshelf.makefont.pdb;

import java.io.*;
import java.util.*;

public class PDBFile {

    Vector v = new Vector();

    ArrayList resultList = new ArrayList();

    public PDBFile(File file) {
        try {
            String dst = file.getName();
            if (dst.endsWith(".pdb") || dst.endsWith(".PDB")) {
                dst = dst.substring(0, dst.length() - 4);
            }
            DataInputStream is = new DataInputStream(new FileInputStream(file));
            byte name[] = new byte[32];
            is.read(name);
            short attributes = is.readShort();
            short version = is.readShort();
            int creationDate = is.readInt();
            int modificationDate = is.readInt();
            int lastBackupDate = is.readInt();
            int modificationNumber = is.readInt();
            int appInfoID = is.readInt();
            int sortInfoID = is.readInt();
            byte type[] = new byte[4];
            is.read(type);
            byte creator[] = new byte[4];
            is.read(creator);
            int uniqueIDSeed = is.readInt();
            int nextRecordListID = is.readInt();
            short numRecords = is.readShort();
            if (appInfoID != 0) {
                Record r = new Record(dst + "-info");
                r.localChunkID = appInfoID;
                v.addElement(r);
            }
            for (int i = 0; i < numRecords; i++) {
                readRecord(is, dst + i);
            }
            is.readShort();
            int offset = 80 + numRecords * 8;
            int totNumRecords = (short) v.size();
            for (int i = 0; i < totNumRecords; i++) {
                int end;
                Record r = (Record) v.elementAt(i);
                int start = r.localChunkID;
                if (i == totNumRecords - 1) {
                    end = (int) file.length();
                } else {
                    end = ((Record) v.elementAt(i + 1)).localChunkID;
                }
                if (offset != start) {
                }
                resultList.add(writeFile(is, r.name, end - start));
                offset += end - start;
            }
        } catch (IOException e) {
            System.out.println("IOException " + e);
        }
    }

    NamedByteArrayOutputStream writeFile(DataInputStream is, String name, int bytes) throws IOException {
        byte buf[] = new byte[1024];
        NamedByteArrayOutputStream result = new NamedByteArrayOutputStream(name);
        DataOutputStream os = new DataOutputStream(result);
        while (bytes > 0) {
            int read = is.read(buf, 0, bytes > 1024 ? 1024 : bytes);
            os.write(buf, 0, read);
            bytes -= read;
        }
        os.close();
        return result;
    }

    void readRecord(DataInputStream is, String name) throws IOException {
        Record r = new Record(name);
        v.addElement(r);
        r.localChunkID = is.readInt();
        r.attributes = is.readByte();
        is.read(r.uniqueID);
    }

    public NamedByteArrayOutputStream[] getResult() {
        NamedByteArrayOutputStream result[] = new NamedByteArrayOutputStream[resultList.size()];
        return (NamedByteArrayOutputStream[]) resultList.toArray(result);
    }
}

class Record {

    int localChunkID;

    byte attributes;

    byte uniqueID[];

    String name;

    Record(String name) {
        this.name = name;
        uniqueID = new byte[3];
    }
}
