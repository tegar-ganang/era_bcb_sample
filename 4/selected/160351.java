package com.scholardesk.excel;

import com.scholardesk.utilities.OrderedProperties;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import jxl.Workbook;
import jxl.format.Colour;
import jxl.format.UnderlineStyle;
import jxl.write.Label;
import jxl.write.WritableFont;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Creates an output file as an excel workbook spreadsheet of a group 
 * of objects that implement the {@link ExcelData} interface.
 * 
 * @author Christopher M. Dunavant
 *
 */
public class ExcelWrite {

    private WritableWorkbook workbook;

    private int sheet_counter = 0;

    /**
	 * Constructor for writing to a file.
	 * 
	 * @param output the output file.
	 */
    public ExcelWrite(File output) {
        try {
            workbook = Workbook.createWorkbook(output);
        } catch (IOException e) {
            new RuntimeException("Unable to open Excel output file (" + output.getName() + ")");
        }
    }

    /**
	 * Constructor for writing to an output stream.
	 * 
	 * @param os output stream to write output to.
	 */
    public ExcelWrite(OutputStream os) {
        try {
            workbook = Workbook.createWorkbook(os);
        } catch (IOException e) {
            new RuntimeException("Unable to open Excel output stream");
        }
    }

    /**
	 * Initializes/creates a new workbook sheet.
	 * 
	 * @param sheet_name name of the sheet to create.
	 * @param config name of the excel column config file to read.
	 * @param objs list of objects to load as rows in the sheet.
	 * 
	 * @throws RuntimeException if a WriteException is encountered.
	 */
    public void createSheet(String sheet_name, String config, List<ExcelData> objs) {
        WritableSheet sheet = workbook.createSheet(sheet_name, sheet_counter);
        OrderedProperties fields;
        List<String> field_keys = new ArrayList<String>();
        File config_file = new File(config);
        fields = OrderedProperties.load(config_file);
        WritableFont font = new WritableFont(WritableFont.TIMES, 16, WritableFont.BOLD, true, UnderlineStyle.NO_UNDERLINE, Colour.WHITE);
        WritableCellFormat format = new WritableCellFormat(font);
        try {
            format.setBackground(Colour.OCEAN_BLUE);
        } catch (WriteException e) {
        }
        int col_counter = 0;
        for (Enumeration e = fields.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            field_keys.add(key);
            Label label = new Label(col_counter, 0, fields.getProperty(key), format);
            try {
                sheet.addCell(label);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to add labels to Excel spreadsheet.");
            }
            col_counter++;
        }
        addRows(sheet, field_keys, objs);
        sheet_counter++;
    }

    /**
	 * Adds rows to the workbook sheet.
	 * 
	 * @param sheet the workbook sheet.
	 * @param keys the property keys to make columns in the row.
	 * @param objs objects to inspect for the data to map into columns.
	 */
    private void addRows(WritableSheet sheet, List<String> keys, List<ExcelData> objs) {
        int row_counter = 1;
        for (ExcelData obj : objs) {
            obj.addRow(sheet, row_counter, keys);
            row_counter++;
        }
    }

    /**
	 * Writes the final workbook to the output file or stream and
	 * closes the workbook.
	 * 
	 * @throws RuntimeException if an IOException or WriteException is encountered.
	 *
	 */
    public void write() {
        try {
            workbook.write();
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close/write Excel spreadsheet.");
        } catch (WriteException e) {
            throw new RuntimeException("Unable to write Excel spreadsheet.");
        }
    }
}
