package br.net.woodstock.rockframework.office.spreadsheet.poi;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map.Entry;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import br.net.woodstock.rockframework.office.DocumentException;
import br.net.woodstock.rockframework.office.spreadsheet.Alignment;
import br.net.woodstock.rockframework.office.spreadsheet.Cell;
import br.net.woodstock.rockframework.office.spreadsheet.CellMerge;
import br.net.woodstock.rockframework.office.spreadsheet.Color;
import br.net.woodstock.rockframework.office.spreadsheet.Image;
import br.net.woodstock.rockframework.office.spreadsheet.Image.ImageType;
import br.net.woodstock.rockframework.office.spreadsheet.IntegerCellMerge;
import br.net.woodstock.rockframework.office.spreadsheet.Row;
import br.net.woodstock.rockframework.office.spreadsheet.Sheet;
import br.net.woodstock.rockframework.office.spreadsheet.SpreadsheetDocument;
import br.net.woodstock.rockframework.office.spreadsheet.SpreadsheetDocumentWriter;
import br.net.woodstock.rockframework.office.spreadsheet.VerticalAlignment;

public final class XSSFSpreadsheetDocumentWriter implements SpreadsheetDocumentWriter {

    private static SpreadsheetDocumentWriter instance = new XSSFSpreadsheetDocumentWriter();

    private XSSFSpreadsheetDocumentWriter() {
        super();
    }

    @Override
    public void write(final SpreadsheetDocument document, final OutputStream outputStream) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        for (Sheet sheet : document.getSheets()) {
            this.handleSheet(workbook, sheet);
        }
        workbook.write(outputStream);
    }

    private void handleSheet(final XSSFWorkbook workbook, final Sheet sheet) {
        XSSFSheet s = workbook.createSheet(sheet.getName());
        s.setDisplayGridlines(sheet.isDisplayGridLines());
        s.setPrintGridlines(sheet.isPrintGridLines());
        s.setFitToPage(sheet.isFitToPage());
        s.setHorizontallyCenter(sheet.isHorizontallyCenter());
        s.setAutobreaks(true);
        XSSFPrintSetup printSetup = s.getPrintSetup();
        printSetup.setLandscape(sheet.isLandscape());
        printSetup.setFitHeight((short) 1);
        printSetup.setFitWidth((short) 1);
        int rowNum = 0;
        for (Row row : sheet.getRows()) {
            this.handleRow(workbook, s, row, rowNum++);
        }
        for (Entry<Integer, Integer> entry : sheet.getColumnsWith().entrySet()) {
            int column = entry.getKey().intValue();
            int value = entry.getValue().intValue();
            s.setColumnWidth(column, 256 * value);
        }
        for (CellMerge<?, ?> cellMerge : sheet.getMerges()) {
            IntegerCellMerge integerCellMerge = (IntegerCellMerge) cellMerge;
            int firstRow = integerCellMerge.getFirstRow().intValue();
            int lastRow = integerCellMerge.getLastRow().intValue();
            int firstColumn = integerCellMerge.getFirstColumn().intValue();
            int lastColumn = integerCellMerge.getLastColumn().intValue();
            CellRangeAddress address = new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
            s.addMergedRegion(address);
        }
        for (Image image : sheet.getImages()) {
            byte[] bytes = image.getBytes();
            ImageType imageType = image.getType();
            int type = -1;
            switch(imageType) {
                case JPEG:
                    type = Workbook.PICTURE_TYPE_JPEG;
                    break;
                case PNG:
                    type = Workbook.PICTURE_TYPE_PNG;
                    break;
                default:
                    break;
            }
            if (type == -1) {
                throw new DocumentException("Unsupported image type " + imageType);
            }
            short row = (short) image.getRow();
            short column = (short) image.getColumn();
            int index = workbook.addPicture(bytes, type);
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, row, column, row, column);
            XSSFDrawing drawing = s.createDrawingPatriarch();
            XSSFPicture picture = drawing.createPicture(anchor, index);
            picture.resize();
        }
    }

    private void handleRow(final XSSFWorkbook workbook, final XSSFSheet sheet, final Row row, final int rowNum) {
        XSSFRow r = sheet.createRow(rowNum);
        r.setHeightInPoints(row.getHeight());
        int cellNum = 0;
        for (Cell cell : row.getCells()) {
            this.handleCell(workbook, r, cell, cellNum++);
        }
    }

    private void handleCell(final XSSFWorkbook workbook, final XSSFRow row, final Cell cell, final int cellNum) {
        XSSFCell c = row.createCell(cellNum);
        XSSFFont font = workbook.createFont();
        font.setColor(this.getColor(cell.getFontColor()));
        font.setFontName(cell.getFont());
        font.setFontHeightInPoints((short) cell.getFontSize());
        if (cell.isBold()) {
            font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        }
        if (cell.isItalic()) {
            font.setItalic(true);
        }
        if (cell.isStrikeout()) {
            font.setStrikeout(true);
        }
        if (cell.isUnderline()) {
            font.setUnderline(Font.U_SINGLE);
        }
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(this.getAlignment(cell.getAlignment()));
        style.setVerticalAlignment(this.getVerticalAlignment(cell.getVerticalAlignment()));
        style.setWrapText(cell.isWrap());
        style.setFont(font);
        if (cell.getLeftBorder() != null) {
            style.setBorderLeft((short) cell.getLeftBorder().getWidth());
            style.setLeftBorderColor(this.getColor(cell.getLeftBorder().getColor()));
        }
        if (cell.getTopBorder() != null) {
            style.setBorderTop((short) cell.getTopBorder().getWidth());
            style.setTopBorderColor(this.getColor(cell.getTopBorder().getColor()));
        }
        if (cell.getRightBorder() != null) {
            style.setBorderRight((short) cell.getRightBorder().getWidth());
            style.setRightBorderColor(this.getColor(cell.getRightBorder().getColor()));
        }
        if (cell.getBottomBorder() != null) {
            style.setBorderBottom((short) cell.getBottomBorder().getWidth());
            style.setBottomBorderColor(this.getColor(cell.getBottomBorder().getColor()));
        }
        if (cell.getBackgroundColor() != null) {
            style.setFillForegroundColor(this.getBackgroundColor(cell.getBackgroundColor()));
            style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        }
        switch(cell.getType()) {
            case BLANK:
                c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK);
                c.setCellValue("");
                break;
            case BOOLEAN:
                if (cell.getValue() != null) {
                    Object value = cell.getValue();
                    Boolean bool = null;
                    if (value instanceof Boolean) {
                        bool = (Boolean) cell.getValue();
                    } else {
                        bool = Boolean.valueOf(value.toString());
                    }
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN);
                    c.setCellValue(bool.booleanValue());
                } else {
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN);
                    c.setCellValue("");
                }
                break;
            case FORMULA:
                if (cell.getValue() != null) {
                    String formula = cell.getValue().toString();
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA);
                    c.setCellValue(formula);
                } else {
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA);
                    c.setCellValue("");
                }
                break;
            case NUMERIC:
                if (cell.getValue() != null) {
                    Object value = cell.getValue();
                    Number number = null;
                    if (value instanceof Number) {
                        number = (Number) cell.getValue();
                    } else {
                        number = new BigDecimal(value.toString());
                    }
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC);
                    c.setCellValue(number.doubleValue());
                } else {
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC);
                    c.setCellValue("");
                }
                break;
            case TEXT:
                if (cell.getValue() != null) {
                    XSSFRichTextString value = new XSSFRichTextString(cell.getValue().toString());
                    c.setCellValue(value);
                } else {
                    c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                    c.setCellValue("");
                }
                break;
            default:
                c.setCellType(org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING);
                c.setCellValue("");
                break;
        }
        c.setCellStyle(style);
    }

    private short getAlignment(final Alignment alignment) {
        switch(alignment) {
            case CENTER:
                return CellStyle.ALIGN_CENTER;
            case JUSTIFY:
                return CellStyle.ALIGN_JUSTIFY;
            case LEFT:
                return CellStyle.ALIGN_LEFT;
            case RIGHT:
                return CellStyle.ALIGN_RIGHT;
            default:
                return CellStyle.ALIGN_LEFT;
        }
    }

    private short getVerticalAlignment(final VerticalAlignment verticalAlignment) {
        switch(verticalAlignment) {
            case BOTTOM:
                return CellStyle.VERTICAL_BOTTOM;
            case MIDDLE:
                return CellStyle.VERTICAL_CENTER;
            case TOP:
                return CellStyle.VERTICAL_TOP;
            default:
                return CellStyle.VERTICAL_TOP;
        }
    }

    private short getColor(final Color color) {
        XSSFColor c = new XSSFColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()));
        return c.getIndexed();
    }

    private short getBackgroundColor(final Color color) {
        XSSFColor c = new XSSFColor(new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue()));
        return c.getIndexed();
    }

    public static SpreadsheetDocumentWriter getInstance() {
        return XSSFSpreadsheetDocumentWriter.instance;
    }
}
