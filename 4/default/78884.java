import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.poi.hssf.usermodel.*;

public class Excel {

    public static void writeCell(String file, int row, int column, String text) throws FileNotFoundException, IOException {
        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFRow rown = sheet.getRow(row);
        HSSFCell cell = rown.getCell(column);
        cell.setCellValue(text);
        workbook.write(new FileOutputStream(file));
    }

    public static String readCell(String file, int row, int column) throws FileNotFoundException, IOException {
        String out = "";
        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFRow rown = sheet.getRow(row);
        HSSFCell cell = rown.getCell(column);
        out = cell.getStringCellValue();
        return out;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void deleteFile(File file) {
        file.delete();
    }

    public static int numOfPeople(String file) throws FileNotFoundException, IOException {
        String text = readCell(file, 0, 0);
        int num = Integer.parseInt(text.substring(0, (text.indexOf(','))));
        return num;
    }

    public static int numOfQuestions(String file) throws FileNotFoundException, IOException {
        String text = readCell(file, 0, 0);
        int num = Integer.parseInt(text.substring(text.indexOf(',') + 1));
        return num;
    }

    public static void deleteRow(String file, int row) throws FileNotFoundException, IOException {
        HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file));
        HSSFSheet sheet = workbook.getSheetAt(0);
        HSSFRow rown = sheet.getRow(row);
        sheet.removeRow(rown);
        rown = sheet.getRow(0);
        HSSFCell cell = rown.getCell(0);
        cell.setCellValue((numOfPeople(file) - 1) + "," + numOfQuestions(file));
        workbook.write(new FileOutputStream(file));
    }
}
