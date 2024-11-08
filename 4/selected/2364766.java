package com.loribel.commons.util.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Blank;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import com.loribel.commons.exception.GB_SaveException;
import com.loribel.commons.util.CTools;
import com.loribel.commons.util.FTools;
import com.loribel.commons.util.GB_StringSplitTools;
import com.loribel.commons.util.GB_StringTools;
import com.loribel.commons.util.STools;
import com.loribel.commons.util.csv.GB_CsvTools;

/**
 * Tools for Excel.
 *
 * Utilise la librairie : Jxl
 *
 * @author Gregory Borelli
 */
public final class GB_ExcelTools {

    /**
     * Retourne une feuille Excel, si elle n'existe pas la cr�e avant de la retourner. 
     */
    public static WritableSheet getSheetNotNull(WritableWorkbook a_workBook, String a_name) {
        WritableSheet retour = a_workBook.getSheet(a_name);
        if (retour == null) {
            retour = a_workBook.createSheet(a_name, Integer.MAX_VALUE);
        }
        return retour;
    }

    public static Workbook getWorkbook(File a_file) throws Exception {
        return Workbook.getWorkbook(a_file);
    }

    public static Workbook getWorkbook(InputStream a_is) throws Exception {
        return Workbook.getWorkbook(a_is);
    }

    public static WritableWorkbook newWorkbook(File a_file) throws IOException {
        return Workbook.createWorkbook(a_file);
    }

    public static WritableWorkbook newWorkbook(File a_file, Workbook a_workbook) throws IOException {
        return Workbook.createWorkbook(a_file, a_workbook);
    }

    public static WritableWorkbook newWorkbook(OutputStream a_out) throws IOException {
        return Workbook.createWorkbook(a_out);
    }

    public static WritableWorkbook newWorkbook(OutputStream a_out, Workbook a_workbook) throws IOException {
        return Workbook.createWorkbook(a_out, a_workbook);
    }

    /**
     * Lit une feuille Excel et retourne le contenu en format CSV.
     */
    public static String readCsvContent(Sheet a_sheet, String a_separator, int a_startCol, int a_startRow) throws IOException {
        StringWriter l_writer = new StringWriter();
        readCsvContent(l_writer, a_sheet, a_separator, a_startCol, a_startRow);
        return l_writer.toString();
    }

    /**
     * Lit une feuille Excel et retourne le contenu en format CSV.
     */
    public static String readCsvContent(Workbook a_workBook, String a_name, String a_separator) throws IOException {
        Sheet l_sheet = a_workBook.getSheet(a_name);
        if (l_sheet == null) {
            return "";
        }
        String retour = readCsvContent(l_sheet, a_separator, 0, 0);
        return retour;
    }

    /**
     * Lit une feuille Excel et l'�crit dans le writer (format CSV).
     */
    public static void readCsvContent(Writer a_writer, Sheet a_sheet, String a_separator, int a_startCol, int a_startRow) throws IOException {
        int len = a_sheet.getRows();
        for (int i = a_startRow; i < len; i++) {
            readCsvLine(a_writer, a_sheet, a_separator, a_startCol, i);
        }
    }

    /**
     * Lit une ligne d'une feuille Excel et l'�crit dans le writer (format CSV).
     */
    public static void readCsvLine(Writer a_writer, Sheet a_sheet, String a_separator, int a_startCol, int a_row) throws IOException {
        Cell[] l_cols = a_sheet.getRow(a_row);
        int len = l_cols.length;
        for (int i = a_startCol; i < len; i++) {
            Cell l_col = l_cols[i];
            String l_value = l_col.getContents();
            l_value = GB_CsvTools.encodeSL(l_value);
            if (i != (len - 1)) {
                l_value += a_separator;
            }
            a_writer.write(l_value);
        }
        a_writer.write(AA.SL);
    }

    /**
     * Ajoute � une feuille Excel une cellule.
     * Si la valeur est num�rique, l'ajoute comme en tant que nombre. 
     */
    public static void writeCell(WritableSheet a_sheet, int a_col, int a_row, String a_value) throws GB_SaveException {
        WritableCell l_cell = null;
        if (STools.isNull(a_value)) {
            l_cell = new Blank(a_col, a_row);
        } else {
            try {
                double l_number = Double.parseDouble(a_value);
                l_cell = new Number(a_col, a_row, l_number);
            } catch (Throwable ex) {
                l_cell = new Label(a_col, a_row, a_value);
            }
        }
        try {
            a_sheet.addCell(l_cell);
        } catch (Throwable ex) {
            throw new GB_SaveException("Error add Cell [" + a_col + "," + a_row + "] - " + a_value + " : " + ex.getMessage(), ex);
        }
    }

    /**
     * Ajoute � une feuille Excel, le contenu d'une ligne CSV
     */
    public static void writeCsvLine(WritableSheet a_sheet, String a_line, String a_separator, int a_col, int a_row) throws GB_SaveException {
        String[] l_values = GB_StringSplitTools.split(a_line, a_separator);
        int len = CTools.getSize(l_values);
        for (int i = 0; i < len; i++) {
            String l_value = l_values[i];
            writeCell(a_sheet, a_col + i, a_row, l_value);
        }
    }

    /**
     * Ajoute � une feuille Excel, un contenu de type CSV.
     */
    public static void writeCvsContent(WritableSheet a_sheet, String a_csv, String a_separator, int a_col, int a_row) throws GB_SaveException {
        String[] l_lines = GB_StringTools.toLinesArray(a_csv);
        int len = CTools.getSize(l_lines);
        int l_row = a_row;
        for (int i = 0; i < len; i++) {
            String l_line = l_lines[i];
            writeCsvLine(a_sheet, l_line, a_separator, a_col, l_row);
            l_row++;
        }
    }

    /**
     * Ajoute ou remplace une feuille Excel par le contenu d'un fichier CSV.
     */
    public static void writeSheetFromCsv(WritableWorkbook a_workBook, File a_file, String a_encoding, String a_separator) throws GB_SaveException, IOException {
        writeSheetFromCsv(a_workBook, null, a_file, a_encoding, a_separator);
    }

    /**
     * Ajoute ou remplace une feuille Excel par le contenu d'un fichier CSV.
     */
    public static void writeSheetFromCsv(WritableWorkbook a_workBook, String a_name, File a_file, String a_encoding, String a_separator) throws GB_SaveException, IOException {
        if (a_name == null) {
            a_name = a_file.getName();
            a_name = FTools.removeExtension(a_name);
        }
        WritableSheet l_sheet = getSheetNotNull(a_workBook, a_name);
        String l_csv = FTools.readFile(a_file, a_encoding);
        writeCvsContent(l_sheet, l_csv, a_separator, 0, 0);
    }

    private GB_ExcelTools() {
    }
}
