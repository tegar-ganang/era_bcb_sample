import java.io.*;
import java.util.*;
import com.jun.tools.*;
import jxl.*;
import jxl.write.*;

public class Catalyst2spd {

    String strLsr = "";

    int iTotal = 0;

    public void toSpdHead(String strFile) {
        try {
            String strLine = "";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(strFile));
            RandomAccessFile spdRaf = new RandomAccessFile(strFile + ".csv", "rw");
            spdRaf.writeBytes("Spreadsheet Format\nTest Program:\nLot ID:\nOperator:\nComputer:\nDate:\n");
            String strTestNo = ",,";
            String strName = ",,";
            String strNullLine = ",,,";
            String strMax = ",,";
            String strMin = ",,";
            String strUnit = "Serial#,Bin#,";
            while ((strLine = bufferedReader.readLine()) != null) {
                strLine = strLine.trim();
                String[] strLineArr = strLine.split(" +");
                if (strLineArr.length == 11) {
                    strTestNo += strLineArr[0] + ",";
                    strName += strLineArr[1] + ",";
                    strMax += strLineArr[9] + ",";
                    strMin += strLineArr[3] + ",";
                    strUnit += strLineArr[4] + ",";
                }
                if (strLineArr.length == 12) {
                    strTestNo += strLineArr[0] + ",";
                    strName += strLineArr[1] + ",";
                    strMax += strLineArr[10] + ",";
                    strMin += strLineArr[3] + ",";
                    strUnit += strLineArr[4] + ",";
                }
                if (strLine.length() > 6 && strLineArr.length == 2) {
                    if (strLine.substring(0, 3).equals("Bin")) {
                        spdRaf.writeBytes(strTestNo + "\n");
                        spdRaf.writeBytes(strName + "\n");
                        spdRaf.writeBytes(strNullLine + "\n");
                        spdRaf.writeBytes(strMax + "\n");
                        spdRaf.writeBytes(strMin + "\n");
                        spdRaf.writeBytes(strUnit + "\n");
                        return;
                    }
                }
            }
            bufferedReader.close();
            spdRaf.close();
        } catch (Exception e) {
            System.out.println(e + " toSpdHead()");
        }
    }

    public void toSpd(String strFile) {
        toSpdHead(strFile);
        try {
            String strLine = "";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(strFile));
            RandomAccessFile spdRaf = new RandomAccessFile(strFile + ".csv", "rw");
            spdRaf.seek(spdRaf.length());
            String strBin = "";
            String strSerial = "";
            String strData = "";
            int count = 0;
            while ((strLine = bufferedReader.readLine()) != null) {
                strLine = strLine.trim();
                String[] strLineArr = strLine.split(" +");
                if (strLineArr.length == 11 || strLineArr.length == 12) {
                    strData += strLineArr[6] + ",";
                }
                if (strLine.length() > 8 && strLineArr.length == 6) {
                    if (strLine.substring(0, 6).equals("Device")) {
                        strSerial = strLineArr[1] + ",";
                    }
                }
                if (strLine.length() > 6 && strLineArr.length == 2) {
                    if (strLine.substring(0, 3).equals("Bin")) {
                        strBin = strLineArr[1] + ",";
                        spdRaf.writeBytes(strSerial + strBin + strData + "\n");
                        strBin = "";
                        strSerial = "";
                        strData = "";
                        count++;
                    }
                }
            }
            iTotal = count;
            System.out.println(strFile + " count:" + count + "\n");
            bufferedReader.close();
            spdRaf.close();
            writeLsr(strFile);
        } catch (Exception e) {
            System.out.println(e + " toSpd()");
        } finally {
        }
    }

    public void run() {
        try {
            HashSet<String> fileSet = new HashSet<String>(200);
            JunFiles Jun = new JunFiles();
            Iterator it = Jun.listAllFiles(new File("catalyst"), ".txt", true);
            while (it.hasNext()) {
                File f = (File) it.next();
                toSpd(f.getCanonicalPath());
            }
        } catch (Exception e) {
            System.out.println(e + " run() \n");
        }
    }

    public void writeLsr(String strFile) {
        strLsr = "";
        strLsr += "                           Lot Summary Report\n";
        strLsr += "___________________________________________________________________\n";
        strLsr += "\n";
        strLsr += "Test Program    : XXXXXXXXX (FT) Total        :" + iTotal + "\n";
        strLsr += "Version         : No Version#                Total Pass   : 0\n";
        strLsr += "Lot ID          : XXXXXXXXXXXXXXXXX          Total Fail   : 0\n";
        strLsr += "Operator        : oper                       Most Fail Bin: 0\n";
        strLsr += "Computer        : XXXXXXX                    Bin #        : 0\n";
        strLsr += "Handler         : XXXXXXX                    Yield %      : 0 \n";
        strLsr += "Autocorrelation : Disabled                   Next Serial #: 0\n";
        strLsr += "___________________________________________________________________\n";
        strLsr += "Tuesday, December 02, 2008 18:45:44/Thursday, December 04, 2008 00:06:48\n";
        strLsr += "\n";
        strLsr += "                    SW Bins                             HW Bins \n";
        strLsr += "[1]    All Pass             0             0 %   1   0    0 % \n";
        strLsr += "[2]                         0          0.00 %   2   1          0.00 %\n";
        strLsr += "[3]                         0          0.00 %   3   7          0.01 %\n";
        strLsr += "[4]                         0          0.00 %   4   304        0.23 %\n";
        strLsr += "[5]    Open                 1          0.00 % \n";
        strLsr += "[6]    Short                7          0.01 %\n";
        strLsr += "\n";
        try {
            RandomAccessFile spdRaf = new RandomAccessFile(strFile + ".lsr", "rw");
            spdRaf.writeBytes(strLsr);
            spdRaf.close();
        } catch (Exception e) {
            System.out.println(e + "  writeLsr()");
        }
    }

    public static void main(String s[]) {
        Catalyst2spd catalyst2spd = new Catalyst2spd();
        catalyst2spd.run();
    }
}
