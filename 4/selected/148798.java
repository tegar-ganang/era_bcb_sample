package trang.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import acs.jni.ACR120U;

public class Tool_read {

    static ACR120U acr120u = new ACR120U();

    static short hReader = acr120u.open(acr120u.ACR120_USB1);

    static short status = acr120u.status(hReader);

    static byte[] key = new byte[6];

    static byte[] pResultSN = new byte[4];

    public Tool_read() {
    }

    public static void loginCard(byte sector) {
        System.out.println("hReader: " + hReader);
        if (hReader > 0) {
            status = acr120u.reset(hReader);
        } else {
            System.out.println("Error: " + status);
        }
        byte[] pResultTagType = new byte[16];
        byte[] pResultTagLength = new byte[16];
        for (int i = 0; i < 1000; i++) {
            short select = acr120u.select(hReader, pResultTagType, pResultTagLength, pResultSN);
            if (select == 0) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(Arrays.toString(pResultSN));
        System.out.println("" + acr120u.getClassVersion());
        byte storedNo = 0;
        byte[] pKey = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
        System.out.println("Login: " + acr120u.login(hReader, sector, (byte) acr120u.AC_MIFARE_LOGIN_KEYTYPE_A, storedNo, pKey));
        System.out.println("Test: " + status);
        System.out.println("FirmwareVersion: " + acr120u.getFirmwareVersion());
    }

    public static void login(byte keyType, byte[] key, byte sector) {
        System.out.println("hReader: " + hReader);
        if (hReader > 0) {
            status = acr120u.reset(hReader);
        } else {
            System.out.println("Error: " + status);
        }
        byte[] pResultTagType = new byte[16];
        byte[] pResultTagLength = new byte[16];
        for (int i = 0; i < 1000; i++) {
            short select = acr120u.select(hReader, pResultTagType, pResultTagLength, pResultSN);
            if (select == 0) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(Arrays.toString(pResultSN));
        System.out.println("" + acr120u.getClassVersion());
        byte storedNo = 0;
        System.out.println("Login: " + acr120u.login(hReader, sector, keyType, storedNo, key));
        System.out.println("Test: " + status);
        System.out.println("FirmwareVersion: " + acr120u.getFirmwareVersion());
    }

    public static String[][] readAll() {
        String[][] data = new String[64][2];
        for (int i = 0; i < 16; i++) {
            loginCard((byte) i);
            System.out.println("----------------------sector    :   " + "\t" + i + "-----------------------------------------------------");
            byte[] readData = new byte[16];
            for (int j = i * 4; j < i * 4 + 4; j++) {
                acr120u.read(hReader, (byte) j, readData);
                String result = "";
                for (int k = 0; k < readData.length; k++) {
                    if (readData[k] == (byte) 0) {
                        result = result + " 0";
                    }
                    result = result + " " + Byte.toString(readData[k]);
                }
                System.out.println("block   " + j + "\t" + result);
                data[j][0] = j + "";
                data[j][1] = result;
            }
        }
        return data;
    }

    public static void format() {
        for (int i = 2; i < 16; i++) {
            loginCard((byte) i);
            System.out.println("----------------------sector    :   " + "\t" + i + "-----------------------------------------------------");
            byte[] readData = new byte[16];
            for (int j = i * 4; j < i * 4 + 4; j++) {
                acr120u.read(hReader, (byte) j, readData);
                System.out.println("block   " + j + "\t" + new String(readData));
                if (j != i * 4 + 3) {
                    for (int k = 0; k < readData.length; k++) {
                        readData[k] = (byte) 0;
                    }
                    acr120u.write(hReader, (byte) j, readData);
                }
            }
        }
    }

    public static String[][] readBlocks(String startBlock, String endBlock) {
        int start = Integer.valueOf(startBlock.trim(), 16).intValue();
        int end = Integer.valueOf(endBlock.trim(), 16).intValue();
        String[][] dataSave = new String[end - start + 1][2];
        int startSector = start / 4;
        int endSector = end / 4;
        int j = start;
        for (int i = startSector; i <= endSector; i++) {
            loginCard((byte) i);
            byte[] dataLength = new byte[16];
            for (; j < i * 4 + 4; j++) {
                if (j > end) break;
                acr120u.read(hReader, (byte) j, dataLength);
                String result = "";
                for (int k = 0; k < dataLength.length; k++) {
                    if (dataLength[k] == (byte) 0) {
                        result = result + " 0";
                    }
                    result = result + " " + Byte.toString(dataLength[k]);
                }
                System.out.println("block   " + j + "\t" + result);
                dataSave[j][0] = j + "";
                dataSave[j][1] = result;
            }
        }
        return dataSave;
    }

    public static String[][] readSectors(int fromSector, int toSector) {
        String[][] array = new String[(toSector - fromSector + 1) * 4][2];
        Map<String, String> dataSave = new HashMap<String, String>();
        int point = 0;
        for (int i = fromSector; i <= toSector; i++) {
            loginCard((byte) i);
            byte[] readData = new byte[16];
            for (int j = i * 4; j < i * 4 + 4; j++) {
                acr120u.read(hReader, (byte) j, readData);
                String result = toHexString(readData);
                array[point][0] = j + "";
                array[point][1] = result;
                point += 1;
                System.out.println("block   " + j + "\t" + result);
            }
        }
        return array;
    }

    public static String[][] readBlock(int blockIndex) {
        int sectorIndex = blockIndex / 4;
        loginCard((byte) sectorIndex);
        byte[] readData = new byte[16];
        String[][] dataSave = new String[1][2];
        acr120u.read(hReader, (byte) blockIndex, readData);
        String result = "";
        result = new String(readData);
        dataSave[0][0] = blockIndex + "";
        dataSave[0][1] = result;
        return dataSave;
    }

    public static boolean clearBlock(int blockIndex) {
        int sectorIndex = blockIndex / 4;
        System.out.println("index  :  " + sectorIndex);
        loginCard((byte) sectorIndex);
        byte[] readData = new byte[16];
        acr120u.read(hReader, (byte) blockIndex, readData);
        if (blockIndex != sectorIndex * 4 + 3) {
            for (int i = 0; i < readData.length; i++) {
                readData[i] = (byte) 0x00;
            }
            acr120u.write(hReader, (byte) blockIndex, readData);
            return true;
        } else {
            System.out.println("khong the xoa du lieu trong block nay.");
            return false;
        }
    }

    public static boolean clearSector(int sectorIndex) {
        loginCard((byte) sectorIndex);
        for (int i = sectorIndex * 4; i < sectorIndex * 4 + 4; i++) {
            byte[] readData = new byte[16];
            if (i != sectorIndex * 4 + 3) {
                acr120u.read(hReader, (byte) i, readData);
                for (int j = 0; j < readData.length; j++) {
                    readData[j] = (byte) 0x00;
                }
                acr120u.write(hReader, (byte) i, readData);
                return true;
            } else {
                System.out.println("khong the xoa du lieu trong block nay.");
            }
        }
        return false;
    }

    public static String toHexString(byte bytes[]) {
        StringBuffer retString = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i) {
            retString.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1) + " ");
        }
        return retString.toString();
    }

    public static void main(String[] args) {
        String b = "duong linh";
        byte[] a = b.getBytes();
        for (int i = 0; i < a.length; i++) {
            System.out.println("byte  : " + a[i]);
        }
        System.out.println("test : " + toHexString(a));
    }
}
