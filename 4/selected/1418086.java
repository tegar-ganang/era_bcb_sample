package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;

public class StarDataLoader {

    private Hashtable iCoordinatesTOsFilenames;

    private static byte[] byteBuffer;

    private boolean useFiles;

    private CartesianVector[] vectorRect = new CartesianVector[4];

    private FileStreamSupplier istrSupplier;

    public StarDataLoader(boolean useFiles, FileStreamSupplier istrSupplier) {
        super();
        this.istrSupplier = istrSupplier;
        this.useFiles = useFiles;
        if (useFiles) {
            readCatalog();
        } else {
            readCatalogRMS();
        }
        for (int i = 0; i < 4; i++) {
            vectorRect[i] = new CartesianVector();
        }
    }

    private static byte[] getBuffer() {
        if (byteBuffer == null) {
            byteBuffer = new byte[ChartSettings.getReaderByteBuffersize()];
        }
        return byteBuffer;
    }

    public static DataInputStream openInputStreamRMS(String storeName, short index) {
        RecordStore rs = null;
        int validBytes = 0;
        try {
            rs = RecordStore.openRecordStore(storeName, false);
            validBytes = rs.getRecord(index, getBuffer(), 0);
        } catch (RecordStoreException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.closeRecordStore();
                }
            } catch (RecordStoreException e) {
            }
        }
        if (validBytes != 0) {
            return new DataInputStream(new ByteArrayInputStream(getBuffer(), 0, validBytes));
        }
        return null;
    }

    public static DataInputStream openInputStream(FileStreamSupplier istrSupplier, String name) {
        return istrSupplier.openInputStream(ChartSettings.getMainFolder() + name, getBuffer());
    }

    public void genRS() {
        if (iCoordinatesTOsFilenames == null || iCoordinatesTOsFilenames.isEmpty()) {
            return;
        }
        try {
            for (int crrFile = 1; crrFile <= ChartSettings.getLastFileIndex(); crrFile++) {
                DataInputStream in = openInputStream(istrSupplier, "data/dataFile_" + crrFile + ".dat");
                RecordStore rs = RecordStore.openRecordStore("dataFile_" + crrFile, true, RecordStore.AUTHMODE_ANY, true);
                try {
                    while (true) {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(bout);
                        int size = in.readShort();
                        out.writeShort(size);
                        for (int crrStar = 0; crrStar < size; crrStar++) {
                            out.writeFloat(in.readFloat());
                            out.writeFloat(in.readFloat());
                            out.writeShort(in.readShort());
                        }
                        size = in.readShort();
                        out.writeShort(size);
                        for (int crrStar = 0; crrStar < size; crrStar++) {
                            out.writeFloat(in.readFloat());
                            out.writeFloat(in.readFloat());
                            out.writeShort(in.readShort());
                            out.writeByte(in.readByte());
                            out.writeFloat(in.readFloat());
                            out.writeUTF(in.readUTF());
                        }
                        byte[] bt = bout.toByteArray();
                        rs.addRecord(bt, 0, bt.length);
                    }
                } catch (EOFException e) {
                }
                in.close();
                rs.closeRecordStore();
            }
        } catch (RecordStoreFullException e) {
            e.printStackTrace();
        } catch (RecordStoreNotFoundException e) {
            e.printStackTrace();
        } catch (RecordStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void genRMSCatalog() {
        if (iCoordinatesTOsFilenames.isEmpty()) {
            return;
        }
        Enumeration en = iCoordinatesTOsFilenames.keys();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(iCoordinatesTOsFilenames.size() * 4);
            DataOutputStream os = new DataOutputStream(bos);
            os.writeInt(iCoordinatesTOsFilenames.size());
            while (en.hasMoreElements()) {
                Integer iel = (Integer) en.nextElement();
                os.writeShort(iel.intValue());
                StoreInfo info = (StoreInfo) iCoordinatesTOsFilenames.get(iel);
                os.writeShort(info.getStorePos());
                os.writeUTF(info.getFileName());
            }
            byte[] bt = bos.toByteArray();
            RecordStore rs = RecordStore.openRecordStore("s_catalog", true, RecordStore.AUTHMODE_ANY, false);
            if (rs.getNumRecords() > 0) {
                rs.closeRecordStore();
                RecordStore.deleteRecordStore("s_catalog");
                rs = RecordStore.openRecordStore("s_catalog", true, RecordStore.AUTHMODE_ANY, false);
            }
            os.close();
            rs.addRecord(bt, 0, bt.length);
            rs.closeRecordStore();
        } catch (RecordStoreFullException e) {
            e.printStackTrace();
        } catch (RecordStoreNotFoundException e) {
            e.printStackTrace();
        } catch (RecordStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCatalog() {
        DataInputStream in = null;
        try {
            in = openInputStream(istrSupplier, "data/catalog.cat");
            if (in == null) {
                return;
            }
            int size = in.readShort();
            iCoordinatesTOsFilenames = new Hashtable();
            for (int crrStar = 0; crrStar < size; crrStar++) {
                short coord = in.readShort();
                short filePos = in.readShort();
                String name = in.readUTF();
                iCoordinatesTOsFilenames.put(new Integer(coord), new StoreInfo(name, filePos));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (in != null) in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void readCatalogRMS() {
        DataInputStream in = null;
        try {
            in = openInputStreamRMS("s_catalog", (short) 1);
            int size = in.readInt();
            iCoordinatesTOsFilenames = new Hashtable();
            for (int crrStar = 0; crrStar < size; crrStar++) {
                short coord = in.readShort();
                short index = in.readShort();
                String name = in.readUTF();
                iCoordinatesTOsFilenames.put(new Integer(coord), new StoreInfo(name, index));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (in != null) in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private int f1(double d) {
        int res = (int) Math.floor((d + 1) * 10.0);
        return res;
    }

    public Hashtable loadData(double fov, double aimPointRa, double aimPointDec, Hashtable loaded) {
        if (iCoordinatesTOsFilenames == null) {
            return null;
        }
        short minStars;
        short minDSOs;
        if (fov < 5.0) {
            minStars = 20000;
            minDSOs = 300;
        } else {
            if (fov < 10.) {
                minStars = 1100;
                minDSOs = 90;
            } else {
                if (fov < 20.) {
                    minStars = 90;
                    minDSOs = 10;
                } else {
                    minStars = 8;
                    minDSOs = 2;
                }
            }
        }
        double ra, dec;
        ra = aimPointRa * ConvertionUtility.degToRad;
        dec = aimPointDec * ConvertionUtility.degToRad;
        double apX = Math.cos(ra) * Math.cos(dec);
        double apY = Math.sin(ra) * Math.cos(dec);
        double apZ = Math.sin(dec);
        double tmpX = apX;
        double tmpY = apY;
        double tmpZ = 0;
        if (aimPointDec == 90 || aimPointDec == -90) {
            tmpX = Math.cos(ra);
            tmpY = Math.sin(ra);
        }
        if (apZ == 0) {
            tmpX = 0;
            tmpY = 0;
            tmpZ = -1;
        }
        double XYperX = apY * tmpZ - apZ * tmpY;
        double XYperY = apZ * tmpX - apX * tmpZ;
        double XYperZ = apX * tmpY - apY * tmpX;
        double sizeXYper = Math.sqrt(XYperX * XYperX + XYperY * XYperY + XYperZ * XYperZ);
        double APZperX = apY * XYperZ - apZ * XYperY;
        double APZperY = apZ * XYperX - apX * XYperZ;
        double APZperZ = apX * XYperY - apY * XYperX;
        double sizeAPZper = Math.sqrt(APZperX * APZperX + APZperY * APZperY + APZperZ * APZperZ);
        double XYmultiplier = Math.sin(fov * ConvertionUtility.degToRad) / sizeXYper;
        double APZmultiplier = Math.sin(fov * ConvertionUtility.degToRad) / sizeAPZper;
        XYperX *= XYmultiplier;
        XYperY *= XYmultiplier;
        XYperZ *= XYmultiplier;
        APZperX *= APZmultiplier;
        APZperY *= APZmultiplier;
        APZperZ *= APZmultiplier;
        double leftTopX = apX + APZperX - XYperX;
        double leftTopY = apY + APZperY - XYperY;
        double leftTopZ = apZ + APZperZ - XYperZ;
        vectorRect[0].set(leftTopX, leftTopY, leftTopZ);
        double rightTopX = apX + APZperX + XYperX;
        double rightTopY = apY + APZperY + XYperY;
        double rightTopZ = apZ + APZperZ + XYperZ;
        vectorRect[1].set(rightTopX, rightTopY, rightTopZ);
        double leftBottomX = apX - APZperX - XYperX;
        double leftBottomY = apY - APZperY - XYperY;
        double leftBottomZ = apZ - APZperZ - XYperZ;
        vectorRect[2].set(leftBottomX, leftBottomY, leftBottomZ);
        double rightBottomX = apX - APZperX + XYperX;
        double rightBottomY = apY - APZperY + XYperY;
        double rightBottomZ = apZ - APZperZ + XYperZ;
        vectorRect[3].set(rightBottomX, rightBottomY, rightBottomZ);
        System.out.println("\nAim point RA=" + ConvertionUtility.degToHMS(aimPointRa) + "\tDEC=" + (aimPointDec) + "\tFOV=" + Math.floor(fov * 200 + 0.5) / 100);
        for (int i = 0; i < vectorRect.length; i++) {
            vectorRect[i].normalize();
        }
        double minx = 1000, maxx = -1000, miny = 1000, maxy = -1000, minz = 1000, maxz = -1000;
        for (int i = 0; i < vectorRect.length; i++) {
            minx = Math.min(minx, vectorRect[i].getX());
            maxx = Math.max(maxx, vectorRect[i].getX());
            miny = Math.min(miny, vectorRect[i].getY());
            maxy = Math.max(maxy, vectorRect[i].getY());
            minz = Math.min(minz, vectorRect[i].getZ());
            maxz = Math.max(maxz, vectorRect[i].getZ());
        }
        int ltX = f1(maxx), ltY = f1(maxy), ltZ = f1(maxz);
        int rbX = f1(minx), rbY = f1(miny), rbZ = f1(minz);
        StoreInfo storeInfo;
        Hashtable starsToVisualize = new Hashtable();
        DataInputStream in = null;
        System.out.println("X:" + rbX + "-" + ltX);
        System.out.println("Y:" + rbY + "-" + ltY);
        System.out.println("X:" + rbZ + "-" + ltZ);
        for (int crrX = rbX; crrX <= ltX; crrX++) {
            for (int crrY = rbY; crrY <= ltY; crrY++) {
                for (int crrZ = rbZ; crrZ <= ltZ; crrZ++) {
                    int index = ConvertionUtility.toIndex(crrX, crrY, crrZ);
                    Integer oIndex = new Integer(index);
                    storeInfo = (StoreInfo) iCoordinatesTOsFilenames.get(oIndex);
                    if (storeInfo == null) {
                        continue;
                    }
                    Object found = loaded.get(oIndex);
                    if (found != null) {
                        LoadedStarPocket foundPock = (LoadedStarPocket) found;
                        if (foundPock.getStarLimit() == minStars) {
                            starsToVisualize.put(oIndex, found);
                            continue;
                        }
                    }
                    try {
                        LoadedStarPocket pock = new LoadedStarPocket(minStars);
                        if (useFiles) {
                            in = openInputStream(istrSupplier, "data/" + storeInfo.getFileName() + ".dat");
                            if (in != null) {
                                readStarFileData(in, pock, storeInfo.getStorePos());
                                starsToVisualize.put(oIndex, pock);
                            }
                        } else {
                            in = openInputStreamRMS(storeInfo.getFileName(), storeInfo.getStorePos());
                            if (in != null) {
                                readStarFileDataRMS(in, pock, minStars, minDSOs);
                                starsToVisualize.put(oIndex, pock);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                            in = null;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
        return starsToVisualize;
    }

    private void readStarFileDataRMS(DataInputStream in, LoadedStarPocket pock, int sizeLimit, int dsoLimit) {
        if (pock == null) {
            return;
        }
        try {
            int size = in.readShort();
            System.out.println("Limits, star: " + sizeLimit + " , dso:" + dsoLimit + " ,actual size:" + size);
            for (int crrStar = 0; crrStar < size; crrStar++) {
                float ra = in.readFloat();
                float dec = in.readFloat();
                short mag = in.readShort();
                if (crrStar < sizeLimit) {
                    pock.star().addElement(new StarData(ra, dec, mag));
                }
            }
            size = in.readShort();
            for (int crrStar = 0; crrStar < size; crrStar++) {
                float ra = in.readFloat();
                float dec = in.readFloat();
                short mag = in.readShort();
                byte type = in.readByte();
                float objSize = in.readFloat();
                String name = in.readUTF();
                if (crrStar < dsoLimit) {
                    pock.dso().addElement(new DSOData(ra, dec, mag, type, objSize, name));
                }
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (in != null) in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void readStarFileData(DataInputStream in, LoadedStarPocket pock, short pos) {
        if (pock == null) {
            return;
        }
        try {
            for (int crrPos = 1; crrPos <= pos; crrPos++) {
                int size = in.readShort();
                for (int crrStar = 0; crrStar < size; crrStar++) {
                    float ra = in.readFloat();
                    float dec = in.readFloat();
                    short mag = in.readShort();
                    if (crrPos == pos) {
                        pock.star().addElement(new StarData(ra, dec, mag));
                    }
                }
                size = in.readShort();
                for (int crrStar = 0; crrStar < size; crrStar++) {
                    float ra = in.readFloat();
                    float dec = in.readFloat();
                    short mag = in.readShort();
                    byte type = in.readByte();
                    float objSize = in.readFloat();
                    String name = in.readUTF();
                    if (crrPos == pos) {
                        pock.dso().addElement(new DSOData(ra, dec, mag, type, objSize, name));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (in != null) in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public String genDSOData() {
        if (iCoordinatesTOsFilenames.isEmpty()) {
            return null;
        }
        for (int crrFile = 1; crrFile <= ChartSettings.getDsoInfoFiles(); crrFile++) {
            DataInputStream in = openInputStream(istrSupplier, "data/dsoData" + crrFile + ".dat");
            if (in == null) {
                return null;
            }
            float ra, dec;
            String name, constell, descr;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(151000);
            DataOutputStream os = new DataOutputStream(bos);
            try {
                while (true) {
                    name = in.readUTF();
                    constell = in.readUTF();
                    ra = in.readFloat();
                    dec = in.readFloat();
                    descr = in.readUTF();
                    os.writeUTF(name);
                    os.writeUTF(constell);
                    os.writeFloat(ra);
                    os.writeFloat(dec);
                    os.writeUTF(descr);
                }
            } catch (EOFException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                byte[] bt = bos.toByteArray();
                RecordStore rs = RecordStore.openRecordStore("dsoData" + crrFile, true, RecordStore.AUTHMODE_ANY, false);
                if (rs.getNumRecords() > 0) {
                    rs.closeRecordStore();
                    RecordStore.deleteRecordStore("dsoData" + crrFile);
                    rs = RecordStore.openRecordStore("dsoData" + crrFile, true, RecordStore.AUTHMODE_ANY, false);
                }
                rs.addRecord(bt, 0, bt.length);
                rs.closeRecordStore();
                os.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RecordStoreFullException e) {
                e.printStackTrace();
            } catch (RecordStoreNotFoundException e) {
                e.printStackTrace();
            } catch (RecordStoreException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String findDSODataRMS(String nameToFind, StarData dsoPosition) {
        for (int crrFile = 1; crrFile <= ChartSettings.getDsoInfoFiles(); crrFile++) {
            DataInputStream in = openInputStreamRMS("dsoData" + crrFile, (short) 1);
            if (in == null) {
                return null;
            }
            String name, constell, descr;
            float ra, dec;
            try {
                while (true) {
                    name = in.readUTF();
                    constell = in.readUTF();
                    ra = in.readFloat();
                    dec = in.readFloat();
                    descr = in.readUTF();
                    if (name.compareTo(nameToFind) == 0) {
                        in.close();
                        if (dsoPosition != null) {
                            dsoPosition.set(ra, dec, (short) 0);
                        }
                        return constell + "~" + descr;
                    }
                }
            } catch (EOFException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String findDSOData(String nameToFind, StarData dsoPosition) {
        if (useFiles) {
            return findDSODataFile(nameToFind, dsoPosition);
        }
        return findDSODataRMS(nameToFind, dsoPosition);
    }

    public String findDSODataFile(String nameToFind, StarData dsoPosition) {
        for (int crrFile = 1; crrFile <= ChartSettings.getDsoInfoFiles(); crrFile++) {
            DataInputStream in = openInputStream(istrSupplier, "data/dsoData" + crrFile + ".dat");
            if (in == null) {
                return null;
            }
            String name, constell, descr;
            float ra, dec;
            try {
                while (true) {
                    name = in.readUTF();
                    constell = in.readUTF();
                    ra = in.readFloat();
                    dec = in.readFloat();
                    descr = in.readUTF();
                    if (name.compareTo(nameToFind) == 0) {
                        in.close();
                        if (dsoPosition != null) {
                            dsoPosition.set(ra, dec, (short) 0);
                        }
                        return constell + "~" + descr;
                    }
                }
            } catch (EOFException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
