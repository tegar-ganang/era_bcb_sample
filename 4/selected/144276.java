package br.net.woodstock.rockframework.office.spreadsheet.poi;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map.Entry;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
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

public final class HSSFSpreadsheetDocumentWriter implements SpreadsheetDocumentWriter {

    private static SpreadsheetDocumentWriter instance = new HSSFSpreadsheetDocumentWriter();

    private HSSFSpreadsheetDocumentWriter() {
        super();
    }

    @Override
    public void write(final SpreadsheetDocument document, final OutputStream outputStream) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        for (Sheet sheet : document.getSheets()) {
            this.handleSheet(workbook, sheet);
        }
        workbook.write(outputStream);
    }

    private void handleSheet(final HSSFWorkbook workbook, final Sheet sheet) {
        HSSFSheet s = workbook.createSheet(sheet.getName());
        s.setDisplayGridlines(sheet.isDisplayGridLines());
        s.setPrintGridlines(sheet.isPrintGridLines());
        s.setFitToPage(sheet.isFitToPage());
        s.setHorizontallyCenter(sheet.isHorizontallyCenter());
        s.setAutobreaks(true);
        HSSFPrintSetup printSetup = s.getPrintSetup();
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
                    type = HSSFWorkbook.PICTURE_TYPE_JPEG;
                    break;
                case PNG:
                    type = HSSFWorkbook.PICTURE_TYPE_PNG;
                    break;
                default:
                    break;
            }
            if (type == -1) {
                throw new DocumentException("Unsupported image type " + imageType);
            }
            short row = (short) image.getRow();
            short column = (short) image.getColumn();
            int width = Math.abs(image.getWidth());
            int height = Math.abs(image.getHeight());
            int index = workbook.addPicture(bytes, type);
            HSSFClientAnchor anchor = new HSSFClientAnchor(0, 0, 0, 0, row, column, row, column);
            HSSFPatriarch patriarch = s.createDrawingPatriarch();
            HSSFPicture picture = patriarch.createPicture(anchor, index);
            picture.resize();
            if ((width != 0) && (height != 0)) {
                picture.getImageDimension().setSize(width, height);
            }
        }
    }

    private void handleRow(final HSSFWorkbook workbook, final HSSFSheet sheet, final Row row, final int rowNum) {
        HSSFRow r = sheet.createRow(rowNum);
        r.setHeightInPoints(row.getHeight());
        int cellNum = 0;
        for (Cell cell : row.getCells()) {
            this.handleCell(workbook, r, cell, cellNum++);
        }
    }

    private void handleCell(final HSSFWorkbook workbook, final HSSFRow row, final Cell cell, final int cellNum) {
        HSSFCell c = row.createCell(cellNum);
        HSSFFont font = workbook.createFont();
        font.setColor(this.getColor(workbook, cell.getFontColor()));
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
        HSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(this.getAlignment(cell.getAlignment()));
        style.setVerticalAlignment(this.getVerticalAlignment(cell.getVerticalAlignment()));
        style.setWrapText(cell.isWrap());
        style.setFont(font);
        if (cell.getLeftBorder() != null) {
            style.setBorderLeft((short) cell.getLeftBorder().getWidth());
            style.setLeftBorderColor(this.getColor(workbook, cell.getLeftBorder().getColor()));
        }
        if (cell.getTopBorder() != null) {
            style.setBorderTop((short) cell.getTopBorder().getWidth());
            style.setTopBorderColor(this.getColor(workbook, cell.getTopBorder().getColor()));
        }
        if (cell.getRightBorder() != null) {
            style.setBorderRight((short) cell.getRightBorder().getWidth());
            style.setRightBorderColor(this.getColor(workbook, cell.getRightBorder().getColor()));
        }
        if (cell.getBottomBorder() != null) {
            style.setBorderBottom((short) cell.getBottomBorder().getWidth());
            style.setBottomBorderColor(this.getColor(workbook, cell.getBottomBorder().getColor()));
        }
        if (cell.getBackgroundColor() != null) {
            style.setFillForegroundColor(this.getBackgroundColor(workbook, cell.getBackgroundColor()));
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
                    HSSFRichTextString value = new HSSFRichTextString(cell.getValue().toString());
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

    private short getColor(final HSSFWorkbook workbook, final Color color) {
        HSSFColor c = this.getHSSFColor(workbook, color);
        if (c == null) {
            return HSSFColor.BLACK.index;
        }
        return c.getIndex();
    }

    private short getBackgroundColor(final HSSFWorkbook workbook, final Color color) {
        HSSFColor c = this.getHSSFColor(workbook, color);
        if (c == null) {
            return HSSFColor.WHITE.index;
        }
        return c.getIndex();
    }

    private HSSFColor getHSSFColor(final HSSFWorkbook workbook, final Color color) {
        byte red = (byte) color.getRed();
        byte green = (byte) color.getGreen();
        byte blue = (byte) color.getBlue();
        HSSFPalette palette = workbook.getCustomPalette();
        HSSFColor c = palette.findSimilarColor(red, green, blue);
        if (c == null) {
            try {
                c = palette.addColor(red, green, blue);
            } catch (RuntimeException e) {
                c = palette.findSimilarColor(red, green, blue);
            }
        }
        return c;
    }

    public static SpreadsheetDocumentWriter getInstance() {
        return HSSFSpreadsheetDocumentWriter.instance;
    }
}
