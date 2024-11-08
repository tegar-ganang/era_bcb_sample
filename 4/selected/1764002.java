package net.sf.pepper.driver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Improved TableProvider for excel spreadsheets. <br>Improvements include: 
 * <ul>
 * <li>1. case sensitive column names</li>
 * <li>2. using open source driver for excel</li>
 * <li>3. correct handling of cells that include newlines</li>
 * </ul>
 */
public class ExcelPoiTableProvider implements TableProvider {

    protected String spreadSheetFileName;

    protected String sheetName;

    protected HSSFWorkbook workBook;

    private HSSFSheet dataSheet;

    private Map<String, Short> columnMap;

    private List<DynaBean> rows;

    public List<DynaBean> getRows() {
        if (rows == null) {
            throw new RuntimeException("Attempt to fetch rows prior to loading a workbook");
        }
        return rows;
    }

    public void updateCell(String value, String columnName, String rowId) {
        int rowNum = Integer.parseInt(rowId);
        short colNum = columnMap.get(columnName);
        dataSheet.createRow(rowNum).createCell(colNum);
        HSSFCell cell = dataSheet.getRow(rowNum).getCell(colNum);
        cell.setCellValue(new HSSFRichTextString(value));
        cell.getCellStyle().setWrapText(true);
    }

    public void open() {
        try {
            InputStream fin = new FileInputStream(getSpreadSheetFileName());
            HSSFWorkbook wb = new HSSFWorkbook(fin);
            loadWorkbook(wb);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read input spreadsheet", e);
        }
    }

    protected void loadWorkbook(HSSFWorkbook workbook) {
        setWorkBook(workbook);
        dataSheet = workBook.getSheet(this.getSheetName());
        loadHeader();
        loadRows();
    }

    private void loadRows() {
        int rowCount = dataSheet.getLastRowNum() + 1;
        rows = new ArrayList<DynaBean>(rowCount);
        for (int i = 1; i < rowCount; i++) {
            DynaBean row = loadRow(i);
            rows.add(row);
        }
    }

    private void loadHeader() {
        HSSFRow headerRow = dataSheet.getRow(0);
        columnMap = new HashMap<String, Short>();
        int colCount = headerRow.getLastCellNum();
        for (short col = 0; col <= colCount; col++) {
            HSSFCell cell = headerRow.getCell(col);
            String columnName = getCellValue(cell);
            if (columnName == null) {
                System.out.println("Warning: column " + col + " has no header.");
                continue;
            }
            columnName = columnNameToProperty(columnName);
            columnMap.put(columnName, col);
        }
    }

    public DynaBean loadRow(int currentRowIndex) {
        DynaBean bean = new LazyDynaBean();
        HSSFRow currentRow = dataSheet.getRow(currentRowIndex);
        for (String columnName : columnMap.keySet()) {
            String cellValue = null;
            if (currentRow != null) {
                HSSFCell cell = currentRow.getCell(columnMap.get(columnName));
                cellValue = getCellValue(cell);
            }
            bean.set(columnName, cellValue);
        }
        bean.set("_rowid_", new Integer(currentRowIndex));
        currentRowIndex += 1;
        return bean;
    }

    public void saveResults() {
        try {
            String resultSpreadSheet = new StringBuffer(spreadSheetFileName).insert(this.spreadSheetFileName.lastIndexOf('.'), ".results").toString();
            OutputStream fout = new FileOutputStream(resultSpreadSheet);
            getWorkBook().write(fout);
            fout.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to results spreadsheet.", e);
        }
    }

    private String columnNameToProperty(String columnName) {
        columnName = columnName.substring(0, 1).toLowerCase() + columnName.substring(1);
        return columnName;
    }

    private String getCellValue(HSSFCell cell) {
        String cellValue = null;
        if (cell != null) {
            if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
                cellValue = "" + cell.getNumericCellValue();
            } else {
                cellValue = cell.getRichStringCellValue().getString();
                if (cellValue.length() == 0) {
                    cellValue = null;
                }
            }
        }
        return cellValue;
    }

    public String getSpreadSheetFileName() {
        return spreadSheetFileName;
    }

    public void setSpreadSheetFileName(String spreadSheetFileName) {
        this.spreadSheetFileName = spreadSheetFileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    private HSSFWorkbook getWorkBook() {
        return workBook;
    }

    private void setWorkBook(HSSFWorkbook workBook) {
        this.workBook = workBook;
    }
}
