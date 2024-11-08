package Excel;

import Utility.ExceptionHandler;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import jxl.*;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;

/**
 * Contains two static methods that creates a new excel template for entering 
 * QUPER data, one taking path as a parameter, and one using a default path.
 * @author pontuslp
 */
public class ExcelTemplateBuilder {

    /**
	 * Generate template to file with default path.
	 * @param component Component for messages
	 */
    public static void generateTemplate(Component component) {
        generateTemplate(component, "NewQuperTemplate.xls");
    }

    /**
	 * Generate template to file at path fileName
	 * @param component Component for messages
	 * @param fileName Path to file
	 */
    public static void generateTemplate(Component component, String fileName) {
        new ExcelTemplateBuilder(component, fileName);
    }

    WritableWorkbook workbook;

    WritableSheet sheet;

    WritableFont headingFont = new WritableFont(WritableFont.ARIAL, 12);

    WritableCellFormat headingFormat = new WritableCellFormat(headingFont);

    private ExcelTemplateBuilder(Component component, String fileName) {
        try {
            File f = new File(fileName);
            if (f.length() != 0) {
                int returnVal = JOptionPane.showConfirmDialog(component, "A file named \"NewQuperTemplate.xls\" already exists, " + "overwrite?", "Overwrite?", JOptionPane.OK_CANCEL_OPTION);
                if (returnVal != JOptionPane.OK_OPTION) {
                    return;
                }
            }
            workbook = Workbook.createWorkbook(f);
            addFeature();
            addQuality();
            workbook.write();
            workbook.close();
        } catch (Exception ex) {
            ExceptionHandler.error(ex);
        }
    }

    /**
	 * Adds a feature sheet to the workbook.
	 * @throws WriteException
	 */
    private void addFeature() throws WriteException {
        sheet = workbook.createSheet("Features", 0);
        Label id = new Label(0, 0, "Id");
        sheet.addCell(id);
        Label definition = new Label(1, 0, "Definition");
        sheet.addCell(definition);
    }

    /**
	 * Adds a quality sheet to the workbook.
	 * @throws WriteException
	 */
    private void addQuality() throws WriteException {
        sheet = workbook.createSheet("Qualities", 1);
        Label feature = new Label(0, 0, "Feature Id");
        sheet.addCell(feature);
        Label id = new Label(1, 0, "Id");
        sheet.addCell(id);
        Label definition = new Label(2, 0, "Defintion");
        sheet.addCell(definition);
    }
}
