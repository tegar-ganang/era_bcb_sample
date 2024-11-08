package common;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;

public class ChartSettings {

    private static boolean isMobile = true;

    private static boolean showHelpScreen = false;

    private static boolean hasNokiaAPI = true;

    private static int readerByteBuffersize = 234000;

    private static short lastFileIndex = 157;

    private static short dsoInfoFiles = 4;

    private static short limitMag;

    private static short limitDSOMag;

    private static boolean showDSONames = true;

    private static boolean showDSOs = true;

    private static short lightLevel;

    private static Vector circles;

    private static short rowSize = 30;

    private static boolean showLines = true;

    private static boolean drawCircles = true;

    private static double apRa;

    private static double apDec;

    private static float fov;

    private static String mainFolder;

    private static double fovMovmentDivisor;

    private static double fovZoomMultiplier;

    public static void initialize(boolean _isMobile) {
        isMobile = _isMobile;
        mainFolder = checkMCRoot();
        if (isMobile) {
            mainFolder += "jTelescope/";
        }
        apRa = ConvertionUtility.hmsToDeg(5, 38, 53);
        apDec = 0;
        limitMag = 500;
        limitDSOMag = 1200;
        fov = (float) 10.0;
        lightLevel = 3;
        fovMovmentDivisor = 4.0;
        fovZoomMultiplier = 1.0 / 1.4;
        circles = new Vector();
        circles.addElement(new FinderCircleDescription(5.6));
        circles.addElement(new FinderCircleDescription(1200, 40, 68));
        circles.addElement(new FinderCircleDescription(1200, 16, 82));
        circles.addElement(new FinderCircleDescription(1200, 6, 52));
        circles.addElement(new FinderCircleDescription(1200, 4, 52));
        Class devControlClass = null;
        try {
            devControlClass = Class.forName("com.nokia.mid.ui.DeviceControl");
        } catch (ClassNotFoundException e) {
        }
        if (devControlClass != null) {
            hasNokiaAPI = true;
        } else {
            hasNokiaAPI = false;
        }
    }

    private static String checkMCRoot() {
        String rootMC = System.getProperty("fileconn.dir.memorycard");
        String fpath = "file:///root1/";
        if (rootMC != null) {
            fpath = rootMC;
        } else {
            fpath = "D:/workspace/star/";
        }
        return fpath;
    }

    public static boolean isInitialized() {
        return mainFolder.length() == 0;
    }

    public static void writeData(FileStreamSupplier istrSupplier) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
            DataOutputStream os = new DataOutputStream(bos);
            os.writeInt(readerByteBuffersize);
            os.writeShort(lastFileIndex);
            os.writeShort(dsoInfoFiles);
            os.writeShort(limitMag);
            os.writeShort(limitDSOMag);
            os.writeBoolean(showDSONames);
            os.writeBoolean(showDSOs);
            os.writeShort(lightLevel);
            os.writeShort(circles.size());
            for (int i = 0; i < circles.size(); i++) {
                FinderCircleDescription desc = (FinderCircleDescription) circles.elementAt(i);
                os.writeShort(desc.getTelescopeFocalLen());
                if (desc.getTelescopeFocalLen() > 0) {
                    os.writeShort(desc.getEyepieceFocalLen());
                    os.writeShort((short) desc.getFov());
                } else {
                    os.writeFloat(desc.getFov());
                }
            }
            os.writeShort(rowSize);
            os.writeBoolean(showLines);
            os.writeBoolean(drawCircles);
            os.writeDouble(apRa);
            os.writeDouble(apDec);
            os.writeFloat(fov);
            os.writeUTF(mainFolder);
            os.writeDouble(fovMovmentDivisor);
            os.writeDouble(fovZoomMultiplier);
            byte[] bt = bos.toByteArray();
            if (isMobile) {
                RecordStore rs = RecordStore.openRecordStore("s_settings", true, RecordStore.AUTHMODE_ANY, false);
                if (rs.getNumRecords() > 0) {
                    rs.closeRecordStore();
                    RecordStore.deleteRecordStore("s_settings");
                    rs = RecordStore.openRecordStore("s_settings", true, RecordStore.AUTHMODE_ANY, false);
                }
                rs.addRecord(bt, 0, bt.length);
                rs.closeRecordStore();
                rs = null;
            } else {
                DataOutputStream out = istrSupplier.openOutputStream(mainFolder + "settings.txt");
                if (out != null) {
                    out.write(bt, 0, bt.length);
                    out.close();
                    out = null;
                }
            }
            os.close();
            bos.close();
            bos = null;
            os = null;
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

    public static void readData(FileStreamSupplier istrSupplier) {
        try {
            DataInputStream in;
            if (isMobile) {
                in = StarDataLoader.openInputStreamRMS("s_settings", (short) 1);
            } else {
                in = StarDataLoader.openInputStream(istrSupplier, "settings.txt");
            }
            if (in != null) {
                readerByteBuffersize = in.readInt();
                lastFileIndex = in.readShort();
                dsoInfoFiles = in.readShort();
                limitMag = in.readShort();
                limitDSOMag = in.readShort();
                showDSONames = in.readBoolean();
                showDSOs = in.readBoolean();
                lightLevel = in.readShort();
                short cirSize = in.readShort();
                for (int i = 0; i < cirSize; i++) {
                    short telFoc = in.readShort();
                    if (telFoc > 0) {
                        short eyeFoc = in.readShort();
                        short eyeFOV = in.readShort();
                        circles.addElement(new FinderCircleDescription(telFoc, eyeFoc, eyeFOV));
                    } else {
                        float cfov = in.readFloat();
                        circles.addElement(new FinderCircleDescription(cfov));
                    }
                }
                rowSize = in.readShort();
                showLines = in.readBoolean();
                drawCircles = in.readBoolean();
                apRa = in.readDouble();
                apDec = in.readDouble();
                fov = in.readFloat();
                mainFolder = in.readUTF();
                fovMovmentDivisor = in.readDouble();
                fovZoomMultiplier = in.readDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isMobile() {
        return isMobile;
    }

    public static void setMobile(boolean isMobile) {
        ChartSettings.isMobile = isMobile;
    }

    public static boolean isShowHelpScreen() {
        return showHelpScreen;
    }

    public static void setShowHelpScreen(boolean showHelpScreen) {
        ChartSettings.showHelpScreen = showHelpScreen;
    }

    public static boolean isHasNokiaAPI() {
        return hasNokiaAPI;
    }

    public static void setHasNokiaAPI(boolean hasNokiaAPI) {
        ChartSettings.hasNokiaAPI = hasNokiaAPI;
    }

    public static int getReaderByteBuffersize() {
        return readerByteBuffersize;
    }

    public static void setReaderByteBuffersize(int readerByteBuffersize) {
        ChartSettings.readerByteBuffersize = readerByteBuffersize;
    }

    public static short getLastFileIndex() {
        return lastFileIndex;
    }

    public static void setLastFileIndex(short lastFileIndex) {
        ChartSettings.lastFileIndex = lastFileIndex;
    }

    public static short getDsoInfoFiles() {
        return dsoInfoFiles;
    }

    public static void setDsoInfoFiles(short dsoInfoFiles) {
        ChartSettings.dsoInfoFiles = dsoInfoFiles;
    }

    public static short getLimitMag() {
        return limitMag;
    }

    public static void setLimitMag(short limitMag) {
        ChartSettings.limitMag = limitMag;
        if (ChartSettings.limitMag > 1550) {
            ChartSettings.limitMag = 1550;
        }
        if (ChartSettings.limitMag < 150) {
            ChartSettings.limitMag = 150;
        }
    }

    public static short getLimitDSOMag() {
        return limitDSOMag;
    }

    public static void setLimitDSOMag(short limitDSOMag) {
        ChartSettings.limitDSOMag = limitDSOMag;
        if (ChartSettings.limitDSOMag > 1800) {
            ChartSettings.limitDSOMag = 1800;
        }
        if (ChartSettings.limitDSOMag < 400) {
            ChartSettings.limitDSOMag = 400;
        }
    }

    public static boolean isShowDSONames() {
        return showDSONames;
    }

    public static void setShowDSONames(boolean showDSONames) {
        ChartSettings.showDSONames = showDSONames;
    }

    public static boolean isShowDSOs() {
        return showDSOs;
    }

    public static void setShowDSOs(boolean showDSOs) {
        ChartSettings.showDSOs = showDSOs;
    }

    public static short getLightLevel() {
        return lightLevel;
    }

    public static void setLightLevel(short lightLevel) {
        ChartSettings.lightLevel = lightLevel;
        if (ChartSettings.lightLevel > 100) {
            ChartSettings.lightLevel = 0;
        }
        if (ChartSettings.lightLevel < 0) {
            ChartSettings.lightLevel = 0;
        }
    }

    public static Vector getCircles() {
        return circles;
    }

    public static void setCircles(Vector _circles) {
        circles = _circles;
    }

    public static short getRowSize() {
        return rowSize;
    }

    public static void setRowSize(short rowSize) {
        ChartSettings.rowSize = rowSize;
    }

    public static boolean isShowLines() {
        return showLines;
    }

    public static void setShowLines(boolean showLines) {
        ChartSettings.showLines = showLines;
    }

    public static boolean isDrawCircles() {
        return drawCircles;
    }

    public static void setDrawCircles(boolean drawCircles) {
        ChartSettings.drawCircles = drawCircles;
    }

    public static double getApRa() {
        return apRa;
    }

    public static void setApRa(double apRa) {
        ChartSettings.apRa = apRa;
        if (ChartSettings.apRa > 360) {
            ChartSettings.apRa = ChartSettings.apRa - 360;
        }
        if (ChartSettings.apRa < 0) {
            ChartSettings.apRa = ChartSettings.apRa + 360;
        }
    }

    public static double getApDec() {
        return apDec;
    }

    public static void setApDec(double apDec) {
        ChartSettings.apDec = apDec;
        if (ChartSettings.apDec > 90) {
            ChartSettings.apDec = 90;
        }
        if (ChartSettings.apDec < -90) {
            ChartSettings.apDec = -90;
        }
    }

    public static float getFov() {
        return fov;
    }

    public static void setFov(float fov) {
        ChartSettings.fov = fov;
    }

    public static String getMainFolder() {
        return mainFolder;
    }

    public static void setMainFolder(String mainFolder) {
        ChartSettings.mainFolder = mainFolder;
    }

    public static double getFovMovmentDivisor() {
        return fovMovmentDivisor;
    }

    public static void setFovMovmentDivisor(double fovMovmentDivisor) {
        ChartSettings.fovMovmentDivisor = fovMovmentDivisor;
    }

    public static double getFovZoomMultiplier() {
        return fovZoomMultiplier;
    }

    public static void setFovZoomMultiplier(double fovZoomMultiplier) {
        ChartSettings.fovZoomMultiplier = fovZoomMultiplier;
    }
}
