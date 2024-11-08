package shu.cms.colorformat.file;

import java.io.*;
import java.io.File;
import jxl.*;
import jxl.read.biff.*;
import jxl.write.*;
import shu.util.*;
import shu.util.log.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class ExcelFile {

    public ExcelFile(String filename) throws BiffException, IOException {
        this(filename, false);
    }

    public ExcelFile(File file, Mode mode) throws BiffException, IOException {
        this(file, null, mode);
    }

    public ExcelFile(String filename, Mode mode) throws BiffException, IOException {
        this(new File(filename), null, mode);
    }

    public ExcelFile(InputStream is) throws BiffException, IOException {
        this(null, is, Mode.ReadOnly);
    }

    public ExcelFile(String filename, boolean create) throws BiffException, IOException {
        this(new File(filename), create);
    }

    public ExcelFile(File file, boolean create) throws BiffException, IOException {
        this(file, null, create ? Mode.Create : Mode.ReadOnly);
    }

    public static enum Mode {

        Create, Update, ReadOnly
    }

    private Mode mode;

    private Workbook getWorkbook(File file, InputStream is) {
        try {
            if (file == null) {
                return Workbook.getWorkbook(is);
            } else {
                return Workbook.getWorkbook(file);
            }
        } catch (BiffException ex) {
            Logger.log.error("", ex);
        } catch (IOException ex) {
            Logger.log.error("", ex);
        }
        return null;
    }

    public ExcelFile(String readFilename, String writeFilename) throws IOException {
        this(new File(readFilename), new File(writeFilename));
    }

    public ExcelFile(File readFile, File writeFile) throws IOException {
        this.mode = Mode.Update;
        Workbook workbook = getWorkbook(readFile, null);
        writableWorkbook = Workbook.createWorkbook(writeFile, workbook);
        workbook.close();
        sheet = writableWorkbook.getSheet(0);
    }

    private ExcelFile(File file, InputStream is, Mode mode) throws BiffException, IOException {
        this.mode = mode;
        switch(mode) {
            case Create:
                {
                    if (file == null) {
                        throw new IllegalArgumentException("file == null");
                    }
                    writableWorkbook = Workbook.createWorkbook(file);
                    sheet = writableWorkbook.createSheet("Sheet1", 0);
                    break;
                }
            case Update:
                {
                    File temp = File.createTempFile("xls", null);
                    Utils.copyFile(file, temp);
                    Workbook workbook = getWorkbook(temp, is);
                    writableWorkbook = Workbook.createWorkbook(file, workbook);
                    workbook.close();
                    sheet = writableWorkbook.getSheet(0);
                    break;
                }
            case ReadOnly:
                {
                    workbook = getWorkbook(file, is);
                    sheet = workbook.getSheet(0);
                    break;
                }
            default:
                throw new IllegalStateException();
        }
    }

    public String getCellAsString(int x, int y) {
        Cell c = sheet.getCell(x, y);
        return c.getContents();
    }

    public Range[] getMergedCells() {
        return sheet.getMergedCells();
    }

    public double getCell(int x, int y) {
        Cell c = sheet.getCell(x, y);
        CellType type = c.getType();
        if (type == CellType.NUMBER) {
            return ((NumberCell) c).getValue();
        } else if (type == CellType.NUMBER_FORMULA) {
            return ((NumberFormulaCell) c).getValue();
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean isEmpty(int x, int y) {
        Cell c = sheet.getCell(x, y);
        CellType type = c.getType();
        return type == CellType.EMPTY;
    }

    public Cell[] getRow(int row) {
        Cell[] cells = sheet.getRow(row);
        return cells;
    }

    public int getRows() {
        return sheet.getRows();
    }

    public void setCell(int x, int y, double value) throws WriteException {
        readOnlyChecking();
        Cell cell = ((WritableSheet) sheet).getCell(x, y);
        CellType type = (cell == null) ? null : cell.getType();
        if (type == null || type == CellType.EMPTY) {
            jxl.write.Number number = new jxl.write.Number(x, y, value);
            ((WritableSheet) sheet).addCell(number);
            return;
        }
        if (type == CellType.NUMBER) {
            ((jxl.write.Number) cell).setValue(value);
        } else {
            throw new IllegalStateException();
        }
    }

    public void setCell(int x, int y, String value) throws WriteException {
        readOnlyChecking();
        Cell cell = ((WritableSheet) sheet).getCell(x, y);
        CellType type = (cell == null) ? null : cell.getType();
        if (type == null || type == CellType.EMPTY) {
            jxl.write.Label label = new jxl.write.Label(x, y, value);
            ((WritableSheet) sheet).addCell(label);
            return;
        }
        if (type == CellType.LABEL) {
            ((jxl.write.Label) cell).setString(value);
        } else {
            throw new IllegalStateException();
        }
    }

    protected void readOnlyChecking() {
        if (mode == Mode.ReadOnly) {
            throw new UnsupportedOperationException("readOnly");
        }
    }

    public int getColumnWidth(int column) {
        CellView cv = sheet.getColumnView(column);
        return cv.getSize();
    }

    public void setColumnView(int column, int width) {
        readOnlyChecking();
        ((WritableSheet) sheet).setColumnView(column, width);
    }

    public void close() throws WriteException, IOException {
        if (writableWorkbook != null) {
            writableWorkbook.write();
            writableWorkbook.close();
        }
    }

    public void selectSheet(int index) {
        switch(mode) {
            case Create:
            case Update:
                this.sheet = writableWorkbook.getSheet(index);
                break;
            case ReadOnly:
                this.sheet = workbook.getSheet(index);
                break;
        }
    }

    public void selectSheet(String name) {
        switch(mode) {
            case Create:
            case Update:
                this.sheet = writableWorkbook.getSheet(name);
                break;
            case ReadOnly:
                this.sheet = workbook.getSheet(name);
                break;
        }
    }

    public void setSheetName(String name) {
        ((WritableSheet) sheet).setName(name);
    }

    public Cell findCell(String contents) {
        return sheet.findCell(contents);
    }

    protected Sheet sheet;

    protected WritableWorkbook writableWorkbook;

    protected Workbook workbook;

    public static void main(String[] args) throws Exception {
        ExcelFile file = new ExcelFile("a.xls", "b.xls");
        for (int x = 0; x < 4; x++) {
            file.setCell(0, x, file.getCell(0, x) * 5);
            file.setCell(1, x, file.getCell(1, x) + "b");
        }
        file.close();
    }
}
