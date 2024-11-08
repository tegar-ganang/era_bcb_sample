package de.lema.client.export;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import de.lema.transfer.to.LogEventRow;

public class ExcelExport {

    private final List<LogEventRow> rows;

    public ExcelExport(List<LogEventRow> rows) {
        this.rows = rows;
    }

    public void export(String filename, String date) throws IOException {
        Workbook wb = new HSSFWorkbook();
        CreationHelper createHelper = wb.getCreationHelper();
        Sheet sheet = wb.createSheet(date);
        writeHeader(wb, sheet, "ID", "Level", "Datum", "Thread", "User", "Modul", "Klasse", "Meldung", "Trace");
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 2000);
        sheet.setColumnWidth(2, 3700);
        sheet.setColumnWidth(3, 2000);
        sheet.setColumnWidth(4, 2000);
        sheet.setColumnWidth(5, 2000);
        sheet.setColumnWidth(6, 12000);
        sheet.setColumnWidth(7, 15000);
        sheet.setColumnWidth(8, 10000);
        for (int i = 1; i <= rows.size(); i++) {
            Row row = sheet.createRow(i);
            LogEventRow logEvent = rows.get(i - 1);
            row.createCell(0).setCellValue(logEvent.getId());
            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yy hh:mm:ss"));
            Cell cell = row.createCell(1);
            cell.setCellValue(logEvent.getLevelEnum().toString());
            cell = row.createCell(2);
            cell.setCellValue(new Date(logEvent.getDatum()));
            cell.setCellStyle(cellStyle);
            cell = row.createCell(3);
            cell.setCellValue(trim32000(logEvent.getThreadName()));
            cell = row.createCell(4);
            cell.setCellValue(trim32000(logEvent.getUserid()));
            cell = row.createCell(5);
            cell.setCellValue(trim32000(logEvent.getModul()));
            cell = row.createCell(6);
            cell.setCellValue(trim32000(logEvent.getKlasse()));
            cell = row.createCell(7);
            cell.setCellValue(trim32000(logEvent.getMessage()));
            cell = row.createCell(8);
            cell.setCellValue(trim32000(logEvent.getTraceShort()));
        }
        FileOutputStream fileOut = new FileOutputStream(filename);
        wb.write(fileOut);
        fileOut.close();
    }

    public String trim32000(String text) {
        if (text != null && text.length() > 32000) {
            return text.substring(0, 32000);
        } else {
            return text;
        }
    }

    private void writeHeader(Workbook wb, Sheet sheet, String... text) {
        Row row = sheet.createRow((short) (0));
        CellStyle cellStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        cellStyle.setFont(font);
        for (int i = 0; i < text.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(text[i]);
            cell.setCellStyle(cellStyle);
        }
    }
}
