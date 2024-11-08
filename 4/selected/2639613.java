package org.vardb.util.xls;

import jxl.write.WritableWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Ignore;
import org.junit.Test;
import org.vardb.util.AbstractUtilTest;
import org.vardb.util.CFileHelper;
import org.vardb.util.dao.CDataFrame;
import org.vardb.util.dao.CTable;

public class TestExcelHelper extends AbstractUtilTest {

    @Test
    @Ignore
    public void testConvertSpreadsheet() {
        String filename = "c:/projects/seqtagutils/data/filemaker/fmhcv.xlsx";
        CExcelHelper excelhelper = new CExcelHelper();
        CTable table = excelhelper.extractTable(filename);
        for (CTable.Row row : table.getRows()) {
            System.out.println(row.getValue(0));
        }
        CFileHelper.writeFile("c:/temp/spreadsheet.txt", table.toString());
    }

    @Test
    @Ignore
    public void testWriteSpreadsheetJExcel() throws Exception {
        String infile = "c:/projects/seqtagutils/src/tests/org/seqtagutils/util/dataframe1.txt";
        String outfile = "c:/temp/testjxl.xls";
        CDataFrame dataframe = CDataFrame.parseTabFile(infile);
        CTable table = dataframe.getTable();
        WritableWorkbook workbook = CJExcelHelper.createWorkbook(outfile);
        CJExcelHelper.createWorksheet(workbook, table, "table");
        CJExcelHelper.writeWorkbook(workbook);
    }

    @Test
    public void testWriteSpreadsheet() throws Exception {
        String infile = "c:/projects/seqtagutils/src/tests/org/seqtagutils/util/dataframe1.txt";
        String outfile = "c:/temp/testpoi.xls";
        CDataFrame dataframe = CDataFrame.parseTabFile(infile);
        CTable table = dataframe.getTable();
        CExcelHelper helper = new CExcelHelper();
        Workbook workbook = helper.createWorkbook();
        Sheet sheet = helper.createWorksheet(workbook, table, "table");
        sheet.createFreezePane(0, 1);
        helper.writeWorkbook(workbook, outfile);
    }

    @Test
    @Ignore
    public void testColumns() {
        CExcelHelper helper = new CExcelHelper();
        Workbook workbook = helper.createWorkbook();
        Sheet sheet = workbook.createSheet("test");
        helper.setHeaderCell(sheet, 0, 0, "Nelson");
        helper.setCell(sheet, 1, 1, "Hayes");
        helper.writeWorkbook(workbook, "c:/temp/testpoi.xls");
    }
}
