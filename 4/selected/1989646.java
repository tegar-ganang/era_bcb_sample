package com.tegsoft.cc;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import org.apache.log4j.Level;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.asteriskjava.manager.event.StatusEvent;
import org.zkoss.util.media.Media;
import org.zkoss.zul.Window;
import com.tegsoft.pbx.ManagerConnection;
import com.tegsoft.pbx.TegsoftPBX;
import com.tegsoft.tobe.db.command.Command;
import com.tegsoft.tobe.db.connection.Connection;
import com.tegsoft.tobe.db.dataset.DataRow;
import com.tegsoft.tobe.db.dataset.Dataset;
import com.tegsoft.tobe.os.LicenseManager;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.Converter;
import com.tegsoft.tobe.util.DateUtil;
import com.tegsoft.tobe.util.JobUtil;
import com.tegsoft.tobe.util.NullStatus;
import com.tegsoft.tobe.util.UiUtil;
import com.tegsoft.tobe.util.message.MessageUtil;

public class CampaignManager extends Thread {

    private Dataset TBLCCCAMPCONT;

    private Dataset TBLCCCAMPCALL;

    private Dataset TBLCCCAMPCALLD;

    private Dataset TBLCCCAMPAIGN;

    @Override
    public void run() {
        try {
            JobUtil.prepareThread();
            Connection.initNonJ2EE();
            TBLCCCAMPCONT = new Dataset("TBLCCCAMPCONT", "TBLCCCAMPCONT");
            TBLCCCAMPCONT.addDataColumn("PHONENO");
            TBLCCCAMPAIGN = new Dataset("TBLCCCAMPAIGN", "TBLCCCAMPAIGN");
            TBLCCCAMPCALL = new Dataset("TBLCCCAMPCALL", "TBLCCCAMPCALL");
            TBLCCCAMPCALLD = new Dataset("TBLCCCAMPCALLD", "TBLCCCAMPCALLD");
        } catch (Exception ex) {
            MessageUtil.logMessage(CampaignManager.class, Level.FATAL, ex);
            return;
        }
        while (true) {
            try {
                findCampaign();
                Thread.sleep(5000);
            } catch (Exception ex) {
                MessageUtil.logMessage(CampaignManager.class, Level.FATAL, ex);
            }
        }
    }

    private void findCampaign() throws Exception {
        JobUtil.prepareThread();
        Connection.initNonJ2EE();
        String PBXID = TegsoftPBX.getLocalPBXID();
        if (NullStatus.isNull(PBXID)) {
            MessageUtil.logMessage(CampaignManager.class, Level.FATAL, "Unable to locate PBXID exiting");
            return;
        }
        if (LicenseManager.isValid("TegsoftCC_Progressive") <= 0) {
            MessageUtil.logMessage(CampaignManager.class, Level.FATAL, "Unable validate License exiting");
            return;
        }
        try {
            Command command = new Command("SELECT * FROM TBLCCCAMPAIGN WHERE 1=1 AND UNITUID={UNITUID} AND STARTDATE< {NOW} AND ENDDATE>{NOW}");
            command.append("AND STARTTIME<'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
            command.append("AND ENDTIME>'" + Converter.asNotNullFormattedString(DateUtil.now(), "HHmm") + "'");
            command.append("AND ISACTIVE='true' AND CAMPTYPE IN ('UNATTENDED','ATTENDED')");
            TBLCCCAMPAIGN.fill(command);
            for (int j = 0; j < TBLCCCAMPAIGN.getRowCount(); j++) {
                DataRow rowTBLCCCAMPAIGN = TBLCCCAMPAIGN.getRow(j);
                if (NullStatus.isNull(rowTBLCCCAMPAIGN.getDecimal("MAXCHANNEL"))) {
                    rowTBLCCCAMPAIGN.setDecimal("MAXCHANNEL", new BigDecimal("100"));
                }
                if (NullStatus.isNull(rowTBLCCCAMPAIGN.getString("CONTEXTID"))) {
                    continue;
                }
                callContact(rowTBLCCCAMPAIGN, PBXID);
            }
        } catch (Exception ex) {
            MessageUtil.logMessage(CampaignManager.class, Level.FATAL, ex);
        }
        Connection.closeActive();
    }

    private void callContact(DataRow rowTBLCCCAMPAIGN, String PBXID) throws Exception {
        int MAXCHANNEL = 0;
        Command command = new Command("SELECT COUNT(*) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND STATUS='PROGRESS' AND CAMPAIGNID=");
        command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
        int activeCallCount = command.executeScalarAsDecimal().intValue();
        if ("UNATTENDED".equals(rowTBLCCCAMPAIGN.getString("CAMPTYPE"))) {
            if (activeCallCount >= rowTBLCCCAMPAIGN.getDecimal("MAXCHANNEL").intValue()) {
                return;
            }
        } else {
            MAXCHANNEL = 0;
            final ManagerConnection managerConnection = TegsoftPBX.createManagerConnection(PBXID);
            managerConnection.prepareChannels();
            for (int i = 0; i < managerConnection.getChannels().size(); i++) {
                StatusEvent statusEvent = managerConnection.getChannels().get(i);
                if (Compare.equal("6", statusEvent.getChannelState())) {
                    if (NullStatus.isNull(statusEvent.getBridgedChannel())) {
                        String agentCAMPAIGNID = TegsoftPBX.getChannelVariable(managerConnection, statusEvent, "CAMPWAITING");
                        if (Compare.equal(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"), agentCAMPAIGNID)) {
                            MAXCHANNEL++;
                        }
                    }
                }
            }
            managerConnection.disconnect();
            if (activeCallCount >= MAXCHANNEL) {
                return;
            }
        }
        command = new Command("SELECT * FROM TBLCCCAMPCALL WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
        command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
        command.append("ORDER BY ORDERID");
        TBLCCCAMPCALL.fill(command);
        if (TBLCCCAMPCALL.getRowCount() == 0) {
            return;
        }
        command = new Command("SELECT * FROM TBLCCCAMPCALLD WHERE UNITUID={UNITUID} AND STATUS='IVRSCHEDULED' AND CAMPAIGNID=");
        command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
        command.append("AND CALLDATE<{NOW} ");
        TBLCCCAMPCALLD.fill(command);
        for (int i = 0; i < TBLCCCAMPCALLD.getRowCount(); i++) {
            DataRow rowTBLCCCAMPCALLD = TBLCCCAMPCALLD.getRow(i);
            command = new Command("SELECT * FROM TBLCCCAMPCALL WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
            command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
            command.append("AND ORDERID=");
            command.bind(rowTBLCCCAMPCALLD.getString("ORDERID"));
            command.append("ORDER BY ORDERID");
            TBLCCCAMPCALL.fill(command);
            String PHONENO = TBLCCCAMPCALL.getRow(0).getString("PHONENO");
            rowTBLCCCAMPCALLD.setTimestamp("CALLDATE", DateUtil.now());
            rowTBLCCCAMPCALLD.setString("STATUS", "PROGRESS");
            TBLCCCAMPCALLD.save();
            command = new Command("SELECT A.*,B." + PHONENO + " AS PHONENO FROM TBLCCCAMPCONT A,TBLCRMCONTACTS B WHERE ");
            command.append("A.UNITUID=B.UNITUID AND A.CONTID=B.CONTID ");
            command.append("AND A.UNITUID={UNITUID} AND A.CAMPAIGNID=");
            command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
            command.append("AND A.CONTID=");
            command.bind(rowTBLCCCAMPCALLD.getString("CONTID"));
            command.append("ORDER BY A.ORDERID");
            TBLCCCAMPCONT.fill(command);
            if (TBLCCCAMPCONT.getRowCount() > 0) {
                DataRow rowTBLCCCAMPCONT = TBLCCCAMPCONT.getRow(0);
                rowTBLCCCAMPCONT.setString("STATUS", "PROGRESS");
                TBLCCCAMPCONT.save();
                if (NullStatus.isNull(rowTBLCCCAMPCONT.getString("PHONENO"))) {
                    rowTBLCCCAMPCALLD.setString("STATUS", "CLOSED");
                    rowTBLCCCAMPCALLD.setString("DIALRESULT", "NOTCON");
                    TBLCCCAMPCALLD.save();
                    if (!DialerThread.createNextCall(rowTBLCCCAMPCALLD.getString("CAMPAIGNID"), rowTBLCCCAMPCALLD.getString("CONTID"), rowTBLCCCAMPCALLD.getDecimal("ORDERID"))) {
                        continue;
                    }
                }
                new DialerThread(rowTBLCCCAMPCALLD.getString("CAMPAIGNID"), rowTBLCCCAMPCALLD.getString("CONTID"), rowTBLCCCAMPCALLD.getDecimal("ORDERID"), PBXID).start();
                command = new Command("SELECT COUNT(*) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND STATUS='PROGRESS' AND CAMPAIGNID=");
                command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
                activeCallCount = command.executeScalarAsDecimal().intValue();
                if ("UNATTENDED".equals(rowTBLCCCAMPAIGN.getString("CAMPTYPE"))) {
                    if (activeCallCount >= rowTBLCCCAMPAIGN.getDecimal("MAXCHANNEL").intValue()) {
                        return;
                    }
                } else {
                    if (activeCallCount >= MAXCHANNEL) {
                        return;
                    }
                }
            } else {
                rowTBLCCCAMPCALLD.setString("STATUS", "CLOSED");
                rowTBLCCCAMPCALLD.setString("DIALRESULT", "NOTCON");
                TBLCCCAMPCALLD.save();
            }
        }
        String PHONENO = TBLCCCAMPCALL.getRow(0).getString("PHONENO");
        command = new Command("SELECT A.*,B." + PHONENO + " AS PHONENO FROM TBLCCCAMPCONT A,TBLCRMCONTACTS B WHERE ");
        command.append("A.UNITUID=B.UNITUID AND A.CONTID=B.CONTID ");
        command.append("AND A.UNITUID={UNITUID} AND A.STATUS IS NULL AND A.CAMPAIGNID=");
        command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
        command.append("ORDER BY A.ORDERID");
        TBLCCCAMPCONT.fill(command);
        for (int i = 0; i < TBLCCCAMPCONT.getRowCount(); i++) {
            DataRow rowTBLCCCAMPCONT = TBLCCCAMPCONT.getRow(i);
            rowTBLCCCAMPCONT.setString("STATUS", "PROGRESS");
            TBLCCCAMPCONT.save();
            DataRow rowTBLCCCAMPCALLD = TBLCCCAMPCALLD.addNewDataRow();
            rowTBLCCCAMPCALLD.setString("CAMPAIGNID", rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
            rowTBLCCCAMPCALLD.setString("CONTID", rowTBLCCCAMPCONT.getString("CONTID"));
            rowTBLCCCAMPCALLD.setDecimal("ORDERID", TBLCCCAMPCALL.getRow(0).getDecimal("ORDERID"));
            rowTBLCCCAMPCALLD.setTimestamp("CALLDATE", DateUtil.now());
            rowTBLCCCAMPCALLD.setString("STATUS", "PROGRESS");
            TBLCCCAMPCALLD.save();
            if (NullStatus.isNull(rowTBLCCCAMPCONT.getString("PHONENO"))) {
                rowTBLCCCAMPCALLD.setString("STATUS", "CLOSED");
                rowTBLCCCAMPCALLD.setString("DIALRESULT", "NOTCON");
                TBLCCCAMPCALLD.save();
                if (!DialerThread.createNextCall(rowTBLCCCAMPCALLD.getString("CAMPAIGNID"), rowTBLCCCAMPCALLD.getString("CONTID"), rowTBLCCCAMPCALLD.getDecimal("ORDERID"))) {
                    continue;
                }
            }
            new DialerThread(rowTBLCCCAMPCALLD.getString("CAMPAIGNID"), rowTBLCCCAMPCALLD.getString("CONTID"), rowTBLCCCAMPCALLD.getDecimal("ORDERID"), PBXID).start();
            command = new Command("SELECT COUNT(*) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND STATUS='PROGRESS' AND CAMPAIGNID=");
            command.bind(rowTBLCCCAMPAIGN.getString("CAMPAIGNID"));
            activeCallCount = command.executeScalarAsDecimal().intValue();
            if ("UNATTENDED".equals(rowTBLCCCAMPAIGN.getString("CAMPTYPE"))) {
                if (activeCallCount >= rowTBLCCCAMPAIGN.getDecimal("MAXCHANNEL").intValue()) {
                    return;
                }
            } else {
                if (activeCallCount >= MAXCHANNEL) {
                    return;
                }
            }
        }
    }

    public static void importExcel(Media media) throws Exception {
        HSSFWorkbook workbook;
        InputStream is;
        if (media.inMemory()) {
            if (media.isBinary()) {
                byte[] bytes = media.getByteData();
                if (bytes == null) {
                    ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                    return;
                }
                is = new ByteArrayInputStream(bytes);
                workbook = new HSSFWorkbook(is);
            } else {
                byte[] bytes = media.getStringData().getBytes();
                if (bytes == null) {
                    ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                    return;
                }
                is = new ByteArrayInputStream(bytes);
                workbook = new HSSFWorkbook(is);
            }
        } else {
            is = media.getStreamData();
            if (is == null) {
                ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                return;
            }
            workbook = new HSSFWorkbook(is);
        }
        if (is != null) {
            is.close();
        }
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(workbook);
        ArrayList<String> titles = new ArrayList<String>();
        int column = 0;
        int row = 0;
        HSSFRow excelRow = sheet.getRow(row);
        if (excelRow == null) {
            return;
        }
        String value = getCellValue(evaluator, excelRow.getCell(column));
        while (NullStatus.isNotNull(value)) {
            titles.add(value);
            value = getCellValue(evaluator, excelRow.getCell(++column));
        }
        if (titles.size() >= 2) {
            Dataset TBLCCCAMPCONT = new Dataset("TBLCCCAMPCONT", "TBLCCCAMPCONT");
            TBLCCCAMPCONT.getMetaData().getDataColumn("ORDERID").setDefaultValue(new BigDecimal("50"));
            Dataset TBLCCCAMPCONTDET = new Dataset("TBLCCCAMPCONTDET", "TBLCCCAMPCONTDET");
            String CAMPAIGNID = Converter.asNullableString(UiUtil.getDataSourceValue("TBLCCCAMPAIGN", "CAMPAIGNID", null, null));
            row++;
            boolean moveon = false;
            excelRow = sheet.getRow(row);
            if (excelRow == null) {
                UiUtil.getDataset("TBLCCCAMPCONT").reFill();
                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                return;
            }
            if (NullStatus.isNull(getCellValue(evaluator, excelRow.getCell(0)))) {
                UiUtil.getDataset("TBLCCCAMPCONT").reFill();
                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                return;
            }
            if (NullStatus.isNull(getCellValue(evaluator, excelRow.getCell(1)))) {
                UiUtil.getDataset("TBLCCCAMPCONT").reFill();
                UiUtil.getDataset("TBLCRMCONTACTS").reFill();
                ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
                return;
            }
            moveon = true;
            while (moveon) {
                moveon = false;
                String CONTID = getCONTID(titles, evaluator, sheet.getRow(row));
                if (NullStatus.isNull(CONTID)) {
                    row++;
                    continue;
                }
                Command command = new Command("SELECT COUNT(*) FROM TBLCCCAMPCONT WHERE UNITUID={UNITUID} AND CAMPAIGNID=");
                command.bind(CAMPAIGNID);
                command.append("AND CONTID=");
                command.bind(CONTID);
                if (command.executeScalarAsDecimal().intValue() > 0) {
                    row++;
                    excelRow = sheet.getRow(row);
                    if (excelRow != null) {
                        if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(0)))) {
                            if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(1)))) {
                                moveon = true;
                            }
                        }
                    }
                    continue;
                }
                DataRow newDataRow = null;
                for (int i = 0; i < TBLCCCAMPCONT.getRowCount(); i++) {
                    if (Compare.equal(CONTID, TBLCCCAMPCONT.getRow(i).get("CONTID"))) {
                        newDataRow = TBLCCCAMPCONT.getRow(i);
                        if (titles.indexOf("ORDERID") >= 0) {
                            String ORDERID = getCellValue(evaluator, excelRow.getCell(titles.indexOf("ORDERID")));
                            if (NullStatus.isNotNull(ORDERID)) {
                                newDataRow.set("ORDERID", ORDERID);
                            } else {
                                newDataRow.set("ORDERID", "50");
                            }
                            TBLCCCAMPCONT.save();
                        }
                        break;
                    }
                }
                if (newDataRow != null) {
                    row++;
                    excelRow = sheet.getRow(row);
                    if (excelRow != null) {
                        if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(0)))) {
                            if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(1)))) {
                                moveon = true;
                            }
                        }
                    }
                    continue;
                }
                if (newDataRow == null) {
                    newDataRow = TBLCCCAMPCONT.addNewDataRow();
                    newDataRow.set("CAMPAIGNID", CAMPAIGNID);
                    newDataRow.set("CONTID", CONTID);
                    if (titles.indexOf("ORDERID") >= 0) {
                        String ORDERID = getCellValue(evaluator, excelRow.getCell(titles.indexOf("ORDERID")));
                        if (NullStatus.isNotNull(ORDERID)) {
                            newDataRow.set("ORDERID", ORDERID);
                        } else {
                            newDataRow.set("ORDERID", "50");
                        }
                    }
                    TBLCCCAMPCONT.save();
                }
                Dataset TBLCRMCONTACTS = UiUtil.getDataset("TBLCRMCONTACTS_IMPORT");
                for (int i = 0; i < titles.size(); i++) {
                    if (TBLCRMCONTACTS.getMetaData().isDbColumn(titles.get(i))) {
                        continue;
                    }
                    DataRow rowTBLCCCAMPCONTDET = TBLCCCAMPCONTDET.addNewDataRow();
                    rowTBLCCCAMPCONTDET.set("CAMPAIGNID", newDataRow.get("CAMPAIGNID"));
                    rowTBLCCCAMPCONTDET.set("CONTID", newDataRow.get("CONTID"));
                    rowTBLCCCAMPCONTDET.set("KEYNAME", titles.get(i));
                    rowTBLCCCAMPCONTDET.set("KEYVALUE", getCellValue(evaluator, excelRow.getCell(i)));
                    TBLCCCAMPCONTDET.save();
                }
                row++;
                excelRow = sheet.getRow(row);
                if (excelRow != null) {
                    if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(0)))) {
                        if (NullStatus.isNotNull(getCellValue(evaluator, excelRow.getCell(1)))) {
                            moveon = true;
                        }
                    }
                }
            }
            UiUtil.getDataset("TBLCCCAMPCONT").reFill();
            UiUtil.getDataset("TBLCRMCONTACTS").reFill();
        }
        ((Window) UiUtil.findComponent("importWindow")).setVisible(false);
    }

    private static String getCellValue(HSSFFormulaEvaluator evaluator, HSSFCell cell) {
        if (cell == null) {
            return null;
        }
        switch(evaluator.evaluateInCell(cell).getCellType()) {
            case HSSFCell.CELL_TYPE_BLANK:
                return "";
            case HSSFCell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue() ? "true" : "false";
            case HSSFCell.CELL_TYPE_ERROR:
                return "";
            case HSSFCell.CELL_TYPE_FORMULA:
                System.out.println("CELL_TYPE_FORMULA " + cell);
                return null;
            case HSSFCell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString();
            case HSSFCell.CELL_TYPE_NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    return Converter.asNullableString(cell.getDateCellValue());
                } else {
                    Double doubleNumber = cell.getNumericCellValue();
                    NumberFormat format = NumberFormat.getInstance();
                    format.setGroupingUsed(false);
                    return Converter.asNullableString(format.format(doubleNumber));
                }
            default:
                return "";
        }
    }

    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("/home/eray/SampleCampaign.xls");
        HSSFWorkbook workbook = new HSSFWorkbook(fis);
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(workbook);
        System.out.println(getCellValue(evaluator, sheet.getRow(2).getCell(5)));
        fis.close();
    }

    private static String getCONTID(ArrayList<String> titles, HSSFFormulaEvaluator evaluator, HSSFRow excelRow) throws Exception {
        String CONTID = "";
        for (int i = 0; i < titles.size(); i++) {
            if ("CONTID".equals(titles.get(i))) {
                CONTID = getCellValue(evaluator, excelRow.getCell(i));
            }
        }
        if (NullStatus.isNull(CONTID)) {
            return null;
        }
        Command command = new Command("SELECT * FROM TBLCRMCONTACTS WHERE UNITUID={UNITUID} AND CONTID=");
        command.bind(CONTID);
        Dataset TBLCRMCONTACTS = UiUtil.getDataset("TBLCRMCONTACTS_IMPORT");
        TBLCRMCONTACTS.fill(command);
        DataRow newDataRow;
        if (TBLCRMCONTACTS.getRowCount() > 0) {
            newDataRow = TBLCRMCONTACTS.getRow(0);
        } else {
            newDataRow = TBLCRMCONTACTS.addNewDataRow();
        }
        for (int i = 0; i < titles.size(); i++) {
            String value = getCellValue(evaluator, excelRow.getCell(i));
            if (NullStatus.isNull(value)) {
                continue;
            }
            if (Compare.equal("PHONE1", titles.get(i))) {
                if (NullStatus.isNull(newDataRow.getString("PHONE1"))) {
                    newDataRow.set(titles.get(i), value);
                    continue;
                } else if (!Compare.equal(newDataRow.getString("PHONE1"), value)) {
                    if (NullStatus.isNull(newDataRow.getString("PHONE2"))) {
                        newDataRow.set("PHONE2", value);
                        continue;
                    } else if (!Compare.equal(newDataRow.getString("PHONE2"), value)) {
                        if (NullStatus.isNull(newDataRow.getString("PHONE3"))) {
                            newDataRow.set("PHONE3", value);
                            continue;
                        } else if (!Compare.equal(newDataRow.getString("PHONE3"), value)) {
                            if (NullStatus.isNull(newDataRow.getString("PHONE4"))) {
                                newDataRow.set("PHONE4", value);
                                continue;
                            } else if (!Compare.equal(newDataRow.getString("PHONE4"), value)) {
                                if (NullStatus.isNull(newDataRow.getString("PHONE5"))) {
                                    newDataRow.set("PHONE5", value);
                                    continue;
                                } else if (!Compare.equal(newDataRow.getString("PHONE5"), value)) {
                                    if (NullStatus.isNull(newDataRow.getString("PHONE6"))) {
                                        newDataRow.set("PHONE6", value);
                                        continue;
                                    } else if (!Compare.equal(newDataRow.getString("PHONE6"), value)) {
                                        if (NullStatus.isNull(newDataRow.getString("PHONE7"))) {
                                            newDataRow.set("PHONE7", value);
                                            continue;
                                        } else if (!Compare.equal(newDataRow.getString("PHONE7"), value)) {
                                            if (NullStatus.isNull(newDataRow.getString("PHONE8"))) {
                                                newDataRow.set("PHONE8", value);
                                                continue;
                                            } else if (!Compare.equal(newDataRow.getString("PHONE8"), value)) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                continue;
            }
            if (TBLCRMCONTACTS.getMetaData().isDbColumn(titles.get(i))) {
                newDataRow.set(titles.get(i), value);
            }
        }
        TBLCRMCONTACTS.save();
        return CONTID;
    }
}
