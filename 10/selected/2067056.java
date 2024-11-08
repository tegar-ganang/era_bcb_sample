package termfee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import tls.OraConn;

public class ImpFee {

    private Connection cnProd = null;

    private String WinDir = "P:/OWSWork/Prod/Data/Reports/Devices/";

    private String UnixDir = "/mnt/p/OWSWork/Prod/Data/Reports/Devices/";

    private String fDir = null;

    private String fFileMask = "acq_comiss_[0-9]{8}.txt";

    Logger Log;

    static final int CARD_THEM = 1, CARD_BA = 2, CARD_US = 3;

    static final int THEM_VC = 0, THEM_VE = 1, THEM_EC = 2, THEM_EE = 3, THEM_D = 4, BA_VC = 5, BA_VE = 6, BA_EC = 7, BA_EE = 8, BA_D = 9, US_VC = 10, US_VE = 11, US_EC = 12, US_EE = 13, US_D = 14;

    public ImpFee() throws Exception {
        PropertyConfigurator.configure("/WORK/etc/logger.conf");
        Log = Logger.getLogger("Total");
        Log.info("================> impfee started");
        OraConn conn = new OraConn();
        cnProd = conn.GetOraConn("PROD_CONN_REP", "PROD_USER_REP", "PROD_PASS_REP");
        cnProd.setAutoCommit(false);
        if (System.getProperty("file.separator").equals("\\")) fDir = WinDir; else fDir = UnixDir;
    }

    public boolean ReadFile() {
        boolean ret = false;
        FilenameFilter FileFilter = null;
        File dir = new File(fDir);
        String[] FeeFiles;
        int Lines = 0;
        BufferedReader FeeFile = null;
        PreparedStatement DelSt = null, InsSt = null;
        String Line = null, Term = null, CurTerm = null, TermType = null, Code = null;
        double[] Fee = new double[US_D + 1];
        double FeeAm = 0;
        String UpdateSt = "INSERT INTO reporter.term_fee (TERM, TERM_TYPE, THEM_VC,	THEM_VE, THEM_EC, THEM_EE, THEM_D," + "BA_VC, BA_VE, BA_EC, BA_EE, BA_D," + "US_VC, US_VE, US_EC, US_EE, US_D)" + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            FileFilter = new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    if ((new File(dir, name)).isDirectory()) return false; else return (name.matches(fFileMask));
                }
            };
            FeeFiles = dir.list(FileFilter);
            java.util.Arrays.sort(FeeFiles);
            System.out.println(FeeFiles[FeeFiles.length - 1] + " " + (new SimpleDateFormat("dd.MM.yy HH:mm:ss")).format(new Date()));
            Log.info(String.format("Load = %1s", fDir + FeeFiles[FeeFiles.length - 1]));
            FeeFile = new BufferedReader(new FileReader(fDir + FeeFiles[FeeFiles.length - 1]));
            FeeZero(Fee);
            DelSt = cnProd.prepareStatement("delete from reporter.term_fee");
            DelSt.executeUpdate();
            InsSt = cnProd.prepareStatement(UpdateSt);
            WriteTerm(FeeFiles[FeeFiles.length - 1] + " " + (new SimpleDateFormat("dd.MM.yy HH:mm:ss")).format(new Date()), "XXX", Fee, InsSt);
            while ((Line = FeeFile.readLine()) != null) {
                Lines++;
                if (!Line.matches("\\d{15}\\s+��������.+")) continue;
                Term = Line.substring(7, 15);
                if ((CurTerm == null) || !Term.equals(CurTerm)) {
                    if (CurTerm != null) {
                        WriteTerm(CurTerm, TermType, Fee, InsSt);
                    }
                    CurTerm = Term;
                    if (Line.indexOf("���") > 0) TermType = "���"; else TermType = "���";
                    FeeZero(Fee);
                }
                Code = Line.substring(64, 68).trim().toUpperCase();
                if (Code.equals("ST") || Code.equals("AC") || Code.equals("8110") || Code.equals("8160")) continue;
                FeeAm = new Double(Line.substring(140, 160)).doubleValue();
                if (Line.indexOf("�� ����� ������") > 0) SetFee(Fee, CARD_THEM, Code, FeeAm); else if (Line.indexOf("�� ������ �����") > 0) SetFee(Fee, CARD_BA, Code, FeeAm); else if (Line.indexOf("�� ������ ��") > 0) SetFee(Fee, CARD_US, Code, FeeAm); else throw new Exception("������ ���� ����.:" + Line);
            }
            WriteTerm(CurTerm, TermType, Fee, InsSt);
            cnProd.commit();
            ret = true;
        } catch (Exception e) {
            System.out.printf("Err = %1s\r\n", e.getMessage());
            Log.error(String.format("Err = %1s", e.getMessage()));
            Log.error(String.format("Line = %1s", Line));
            try {
                cnProd.rollback();
            } catch (Exception ee) {
            }
            ;
        } finally {
            try {
                if (FeeFile != null) FeeFile.close();
            } catch (Exception ee) {
            }
        }
        try {
            if (DelSt != null) DelSt.close();
            if (InsSt != null) InsSt.close();
            cnProd.setAutoCommit(true);
        } catch (Exception ee) {
        }
        Log.info(String.format("Lines = %1d", Lines));
        return (ret);
    }

    private void SetFee(double[] pFee, int pCardType, String pCode, double pFeeAm) throws Exception {
        if (pCode.isEmpty() || pCode.equals("0") || pCode.equals("*")) {
            if (pCardType == CARD_THEM) {
                pFee[THEM_VC] = pFeeAm;
                pFee[THEM_VE] = pFeeAm;
                pFee[THEM_EC] = pFeeAm;
                pFee[THEM_EE] = pFeeAm;
                pFee[THEM_D] = pFeeAm;
            } else if (pCardType == CARD_BA) {
                pFee[BA_VC] = pFeeAm;
                pFee[BA_VE] = pFeeAm;
                pFee[BA_EC] = pFeeAm;
                pFee[BA_EE] = pFeeAm;
                pFee[BA_D] = pFeeAm;
            } else {
                pFee[US_VC] = pFeeAm;
                pFee[US_VE] = pFeeAm;
                pFee[US_EC] = pFeeAm;
                pFee[US_EE] = pFeeAm;
                pFee[US_D] = pFeeAm;
            }
        } else if (pCode.equals("CM")) SetFee4CardType(pFee, pCardType, THEM_EE, pFeeAm); else if (pCode.equals("EC") || pCode.equals("MC")) SetFee4CardType(pFee, pCardType, THEM_EC, pFeeAm); else if (pCode.equals("VC") || pCode.equals("VG") || pCode.equals("VS")) SetFee4CardType(pFee, pCardType, THEM_VC, pFeeAm); else if (pCode.equals("VE") || pCode.equals("PC")) SetFee4CardType(pFee, pCardType, THEM_VE, pFeeAm); else if (pCode.equals("CC") || pCode.equals("DCI") || pCode.equals("DC") || pCode.equals("��")) SetFee4CardType(pFee, pCardType, THEM_D, pFeeAm); else throw new Exception("(SetFee) Error pCode = " + pCode);
    }

    private void SetFee4CardType(double[] pFee, int pCardType, int pPos, double pFeeAm) {
        if (pCardType == CARD_THEM) pFee[pPos] = pFeeAm; else if (pCardType == CARD_BA) pFee[pPos + 5] = pFeeAm; else pFee[pPos + 10] = pFeeAm;
    }

    private void WriteTerm(String pCurTerm, String pTermType, double pFee[], PreparedStatement InsSt) throws Exception {
        InsSt.setString(1, pCurTerm);
        InsSt.setString(2, pTermType);
        InsSt.setDouble(3, pFee[THEM_VC]);
        InsSt.setDouble(4, pFee[THEM_VE]);
        InsSt.setDouble(5, pFee[THEM_EC]);
        InsSt.setDouble(6, pFee[THEM_EE]);
        InsSt.setDouble(7, pFee[THEM_D]);
        InsSt.setDouble(8, pFee[BA_VC]);
        InsSt.setDouble(9, pFee[BA_VE]);
        InsSt.setDouble(10, pFee[BA_EC]);
        InsSt.setDouble(11, pFee[BA_EE]);
        InsSt.setDouble(12, pFee[BA_D]);
        InsSt.setDouble(13, pFee[US_VC]);
        InsSt.setDouble(14, pFee[US_VE]);
        InsSt.setDouble(15, pFee[US_EC]);
        InsSt.setDouble(16, pFee[US_EE]);
        InsSt.setDouble(17, pFee[US_D]);
        InsSt.executeUpdate();
    }

    private void FeeZero(double[] pFee) {
        for (int i = 0; i < pFee.length; i++) {
            pFee[i] = 0;
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        boolean ret = true;
        ImpFee o = null;
        try {
            o = new ImpFee();
            ret = o.ReadFile();
        } catch (Exception e) {
            ret = false;
            System.out.printf("err main impfee = %1$s\r\n", e.getMessage());
        }
        System.out.printf("impfee = %1$s\r\n", ret);
        o.Log.info(String.format("impfee = %1$s", ret));
        o.Log.info("================> impfee finished");
    }
}
