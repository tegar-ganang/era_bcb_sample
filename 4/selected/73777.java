package org.statcato.utils;

import org.statcato.file.ExtensionFileFilter;
import org.statcato.spreadsheet.*;
import org.statcato.file.DownloadFile;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.awt.Toolkit;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hssf.usermodel.*;

/**
 * Miscellaneous utilities functions.
 * 
 * @author Margaret Yau
 */
public class HelperFunctions {

    /**
     * Returns true iff the given vector is empty (i.e. does not contain non-null
     * elements).
     * 
     * @param vec Vector object
     * @return true iff vec is empty
     */
    public static boolean isEmptyVector(Vector vec) {
        if (vec == null) return true;
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            if (e.nextElement() != null) return false;
        }
        return true;
    }

    /**
     * Converts a vector of Double values to an array of double values.
     * 
     * @param vector vector of Double
     * @return array of double
     */
    public static double[] ConvertDoubleVectorToArray(Vector<Double> vector) {
        double[] arr = new double[vector.size()];
        for (int j = 0; j < arr.length; ++j) {
            arr[j] = vector.elementAt(j).doubleValue();
        }
        return arr;
    }

    /**
     * Converts a vector of Integer values to an array of integervalues.
     * 
     * @param vector vector of Integer
     * @return array of integer
     */
    public static int[] ConvertIntegerVectorToArray(Vector<Integer> vector) {
        if (vector == null) return new int[0];
        int[] arr = new int[vector.size()];
        for (int j = 0; j < arr.length; ++j) {
            arr[j] = vector.elementAt(j).intValue();
        }
        return arr;
    }

    /**
     * Converts a vector of Cell to a vector of Double.  Returns
     * the converted vector, or null if the conversion fails.
     * 
     * @param vector vector of Cell
     * @return vector of Double, of null if conversion fails
     */
    public static Vector<Double> ConvertInputVectorToDoubles(Vector<Cell> vector) {
        Vector<Double> outputVector = new Vector<Double>();
        for (Enumeration e = vector.elements(); e.hasMoreElements(); ) {
            Cell elm = (Cell) e.nextElement();
            if (elm.getContents().equals("")) {
                outputVector.addElement(null);
            } else {
                Double value = elm.getNumValue();
                if (value == null) return null; else outputVector.addElement(value);
            }
        }
        return outputVector;
    }

    /**
     * Computes the pairwise differences between elements in two vector 
     * of numbers and returns a vector of the pairwise differences.
     * 
     * @param vector1 vector of Double
     * @param vector2 vector of Double, assumed to have the same size as vector1
     * @return vector of pairwise differences, or null if there is any 
     * unmatch pairs
     */
    public static Vector<Double> ComputeDiffVector(Vector<Double> vector1, Vector<Double> vector2) {
        Vector<Double> diffVector = new Vector<Double>();
        double num1, num2, diff;
        for (int i = 0; i < vector1.size(); ++i) {
            if (vector1.elementAt(i) != null) {
                num1 = ((Double) vector1.elementAt(i)).doubleValue();
                if (vector2.elementAt(i) != null) {
                    num2 = ((Double) vector2.elementAt(i)).doubleValue();
                    diffVector.addElement(num1 - num2);
                } else {
                    return null;
                }
            } else {
                if (vector2.elementAt(i) != null) return null;
            }
        }
        return diffVector;
    }

    /**
     * Displays to standard output the contents of a vector.
     * 
     * @param vec a vector to be displayed
     */
    public static void printVector(Vector vec) {
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            System.out.print(e.nextElement());
            System.out.print(", ");
        }
        System.out.println("");
    }

    public static String printVectorToString(Vector vec) {
        String s = "";
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            s = s + e.nextElement();
            s = s + ", ";
        }
        if (s.length() > 3) return s.substring(0, s.length() - 2);
        return "";
    }

    public static String printDoubleVectorToString(Vector<Double> vec) {
        String s = "";
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            Double value = (Double) e.nextElement();
            if (value == null) s = s + "   "; else s = s + String.format("%.4f", value.doubleValue());
            s = s + ", ";
        }
        if (s.length() > 3) return s.substring(0, s.length() - 2);
        return "";
    }

    /**
     * Prints the contents of a vector of vectors to the standard output.
     * 
     * @param vec vector of vectors of Cell
     */
    public static void printVectors(Vector<Vector<Cell>> vec) {
        int numCols = vec.size();
        int numRows = vec.elementAt(0).size();
        boolean hasData = false;
        for (int row = 0; row < numRows; ++row) {
            hasData = false;
            for (int col = 0; col < numCols; ++col) {
                Cell cell = vec.elementAt(col).elementAt(row);
                if (cell != null) {
                    System.out.print(vec.elementAt(col).elementAt(row).toString() + " ");
                    if (!vec.elementAt(col).elementAt(row).toString().equals("")) hasData = true;
                }
            }
            if (hasData) System.out.print("\n");
        }
        System.out.println("");
    }

    /**
     * Converts the contents of a vector to a string, each item separated by 
     * a space.
     * 
     * @param vec vector
     * @return string containing items in the input vector separated by a space
     */
    public static String convertVectorToString(Vector vec) {
        String str = "";
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            str += e.nextElement().toString() + " ";
        }
        return str;
    }

    /**
     * Converts a vector of Cell to a vector of numbers.  Returns
     * an array of Object containing the converted vector of Double,
     * the number of missing numbers, and the number of nonmissing numbers.
     * 
     * @param vector vector of Cell
     * @return array of Object containing the converted vector of Double,
     * the number of missing numbers, and the number of nonmissing numbers 
     */
    public static Object[] ConvertInputVectorToNumbers(Vector<Cell> vector) {
        Vector<Double> outputVector = new Vector<Double>();
        int nonmissing = 0;
        int missing = 0;
        int numMissingSinceLastNonmissing = 0;
        for (Enumeration e = vector.elements(); e.hasMoreElements(); ) {
            Cell elm = (Cell) e.nextElement();
            if (elm.getContents().equals("")) {
                numMissingSinceLastNonmissing++;
            } else {
                if (elm.getNumValue() == null) return null; else {
                    outputVector.addElement(elm.getNumValue());
                    nonmissing++;
                    missing += numMissingSinceLastNonmissing;
                    numMissingSinceLastNonmissing = 0;
                }
            }
        }
        Object[] outputs = new Object[3];
        outputs[0] = outputVector;
        outputs[1] = new Integer(missing);
        outputs[2] = new Integer(nonmissing);
        return outputs;
    }

    public static Vector<String> ConvertCellVectorToStringVector(Vector<Cell> vector) {
        vector = removeEndingEmptyCells(vector);
        Vector<String> outputVector = new Vector<String>();
        for (Enumeration e = vector.elements(); e.hasMoreElements(); ) {
            Cell elm = (Cell) e.nextElement();
            String c = elm.getContents();
            outputVector.addElement(c);
        }
        return outputVector;
    }

    /**
     * Converts a vector of Cell to a vector of numbers.  Returns
     * an array of Object containing the converted vector of Double,
     * the number of missing numbers, and the number of nonmissing numbers.
     * Similar to {@link #ConvertInputVectorToNumbers(Vector)}, but
     * count all elements in the vector in the total count.
     * 
     * @param vector vector of Cell
     * @return array of Object containing the converted vector of Double
     * (null if conversion fails),
     * the number of missing numbers, and the number of nonmissing numbers 
     * @see #ConvertInputVectorToNumbers(Vector)
     */
    public static Object[] ConvertInputVectorToNumbers2(Vector<Cell> vector) {
        Vector<Double> outputVector = new Vector<Double>(0);
        int nonmissing = 0;
        for (Enumeration e = vector.elements(); e.hasMoreElements(); ) {
            Cell elm = (Cell) e.nextElement();
            if (!elm.hasData()) {
            } else {
                if (!elm.isNumeric()) {
                    return null;
                } else {
                    outputVector.addElement(elm.getNumValue());
                    nonmissing++;
                }
            }
        }
        Object[] outputs = new Object[3];
        outputs[0] = outputVector;
        outputs[1] = new Integer(vector.size() - nonmissing);
        outputs[2] = new Integer(nonmissing);
        return outputs;
    }

    /**
     * Convert a vector of Double to a vector of String.
     * 
     * @param vector vector of Double
     * @return vector of String
     */
    public static Vector<String> ConvertDoubleVectorToString(Vector<Double> vector) {
        Vector<String> outputVector = new Vector<String>(0);
        for (Enumeration e = vector.elements(); e.hasMoreElements(); ) {
            Double elm = (Double) e.nextElement();
            if (elm == null) outputVector.addElement(""); else {
                double num = elm.doubleValue();
                outputVector.addElement(num + "");
            }
        }
        return outputVector;
    }

    /**
     * Returns a copy of a vector of Double in which all the null elements
     * are removed.
     * 
     * @param numbers vector of Double
     * @return vector of Double in which there is no null elements
     */
    public static Vector<Double> removeNullValues(Vector<Double> numbers) {
        if (numbers == null) return null;
        Vector<Double> vec = new Vector<Double>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (numbers.elementAt(i) != null) vec.addElement(numbers.elementAt(i));
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Double in which all the null elements
     * at the end are removed.
     * 
     * @param numbers vector of Double
     * @return vector of Double in which there is no null elements
     */
    @SuppressWarnings("unchecked")
    public static Vector<Double> removeEndingNullValues(Vector<Double> numbers) {
        Vector<Double> vec = (Vector<Double>) numbers.clone();
        for (int i = numbers.size() - 1; i >= 0; --i) {
            if (numbers.elementAt(i) != null) break; else vec.removeElementAt(i);
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Double in which all the null elements
     * at the end are removed.
     * 
     * @param vector vector of Double
     * @return vector of Double in which there is no null elements
     */
    @SuppressWarnings("unchecked")
    public static Vector<Cell> removeEndingEmptyCells(Vector<Cell> vector) {
        Vector<Cell> vec = (Vector<Cell>) vector.clone();
        for (int i = vector.size() - 1; i >= 0; --i) {
            if (!vector.elementAt(i).getContents().equals("")) break; else vec.removeElementAt(i);
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Cell in which all the null elements
     * are moved.
     * 
     * @param numbers vector of Cell
     * @return vector of Cell in which there is no null elements
     */
    public static Vector<Cell> removeNullCells(Vector<Cell> numbers) {
        Vector<Cell> vec = new Vector<Cell>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (!numbers.elementAt(i).getContents().equals("")) vec.addElement(numbers.elementAt(i));
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Double in which each element 
     * is raised to a given power.
     * 
     * @param numbers vector of Double
     * @param power integer to which each element in the vector is raised
     * @return vector of Double
     */
    public static Vector<Double> powerVector(Vector<Double> numbers, int power) {
        Vector<Double> vec = new Vector<Double>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (numbers.elementAt(i) != null) vec.addElement(Math.pow(((Double) numbers.elementAt(i)).doubleValue(), power));
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Double in which each element
     * is raised to a given power.
     *
     * @param numbers vector of Double
     * @param power to which each element in the vector is raised
     * @return vector of Double
     */
    public static Vector<Double> powerVector(Vector<Double> numbers, double power) {
        Vector<Double> vec = new Vector<Double>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (numbers.elementAt(i) != null) vec.addElement(Math.pow(((Double) numbers.elementAt(i)).doubleValue(), power));
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of Double in which
     * each element is replaced by its natural logarithm.
     * 
     * @param numbers vector of Double
     * @return vector of Double containing the natural logarithms of 
     * the input vector
     */
    public static Vector<Double> logVector(Vector<Double> numbers) {
        Vector<Double> vec = new Vector<Double>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (numbers.elementAt(i) != null) vec.addElement(Math.log(((Double) numbers.elementAt(i)).doubleValue()));
        }
        return vec;
    }

    /**
     * Returns a copy of a vector of double values in which a constant
     * is added to each element.
     *
     * @param numbers vector of Double
     * @param c constant to be added
     * @return vector of Double containing a vector added a constant
     */
    public static Vector<Double> addConstantVector(Vector<Double> numbers, double c) {
        Vector<Double> vec = new Vector<Double>(0);
        for (int i = 0; i < numbers.size(); ++i) {
            if (numbers.elementAt(i) != null) vec.addElement(((Double) numbers.elementAt(i)).doubleValue() + c);
        }
        return vec;
    }

    /** 
     * Opens a file chooser and saves specified contents to a file with 
     * a specified extension.
     * 
     * @param frame     parent frame
     * @param extensionDescription  string description of the file extension
     * @param extension file extension
     * @param contents  string contents to be saved
     */
    public static void writeFile(JFrame frame, String extensionDescription, String extension, String contents) {
        JFileChooser fc = new JFileChooser();
        fc.addChoosableFileFilter(new ExtensionFileFilter(extensionDescription, extension));
        fc.setAcceptAllFileFilterUsed(false);
        extension = extension.toLowerCase();
        int returnValue = fc.showSaveDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            String path = file.getPath();
            if (!path.toLowerCase().endsWith("." + extension)) {
                path += "." + extension;
                file = new File(path);
            }
            if (file.exists()) {
                System.out.println("file exists already");
                Object[] options = { "Overwrite file", "Cancel" };
                int choice = JOptionPane.showOptionDialog(frame, "The specified file already exists.  Overwrite existing file?", "Overwrite file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (choice != 0) return;
            }
            try {
                BufferedWriter Writer = new BufferedWriter(new FileWriter(path));
                String[] lines = contents.split("\n");
                for (int i = 0; i < lines.length; ++i) {
                    Writer.write(lines[i]);
                    Writer.newLine();
                }
                Writer.close();
            } catch (IOException e) {
                showErrorDialog(frame, "Write file failed!");
            }
        }
    }

    /**
     * Returns a string of textual data stored in the given file.
     * Each line is separated by the newline character \n.
     * 
     * @param file file containing data
     * @return string of data from file separated by a newline character
     */
    public static String getFileContents(File file) {
        StringBuffer contents = new StringBuffer();
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append("\n");
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return contents.toString();
    }

    /**
     * Parses a string of comma-separated values and converts it into
     * a string of tab-delimited values.
     * 
     * @param str string of comma-separated values 
     * @return string of tab-delimited values
     */
    public static String parseCSV(String str) {
        String out = "";
        String[] lines = str.split("\\n");
        for (int i = 0; i < lines.length; ++i) {
            String[] values = lines[i].split(",");
            for (int j = 0; j < values.length; ++j) {
                out += values[j] + "\t";
            }
            out += "\n";
        }
        return out;
    }

    /**
     * Reads an Excel file and returns its contents as a vector of strings 
     * of tab-delimited values.
     * 
     * @param file Excel file
     * @return vector of strings of tab-delimiated values
     */
    public static Vector<String> readExcelFile(File file) {
        POIFSFileSystem fs;
        HSSFWorkbook _wb = null;
        String out = "";
        Vector<String> strings = new Vector<String>();
        FileInputStream stream;
        try {
            stream = new FileInputStream(file);
            fs = new POIFSFileSystem(stream);
            _wb = new HSSFWorkbook(fs);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int r, c;
        for (int i = 0; i < _wb.getNumberOfSheets(); i++) {
            HSSFSheet sheet = _wb.getSheetAt(i);
            out = "";
            for (r = 0; r <= sheet.getLastRowNum(); r++) {
                HSSFRow row = sheet.getRow(r);
                if (row != null) {
                    for (c = 0; c <= row.getLastCellNum(); c++) {
                        HSSFCell cell = row.getCell((short) c);
                        String value = getCellValue(cell);
                        out += value + "\t";
                    }
                }
                out += "\n";
            }
            strings.addElement(out);
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return strings;
    }

    /**
     * Returns the value of a HSSFCell as a string.
     * 
     * @param cell HSSFCell
     * @return string value
     */
    public static String getCellValue(HSSFCell cell) {
        String str;
        double dnum;
        if (cell == null) {
            return "";
        }
        switch(cell.getCellType()) {
            case HSSFCell.CELL_TYPE_NUMERIC:
                str = String.valueOf(cell.getNumericCellValue());
                dnum = Double.parseDouble(str);
                str = String.valueOf(dnum);
                break;
            case HSSFCell.CELL_TYPE_STRING:
                str = cell.getStringCellValue();
                break;
            case HSSFCell.CELL_TYPE_BLANK:
                str = "";
                break;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                str = String.valueOf(cell.getBooleanCellValue());
                break;
            case HSSFCell.CELL_TYPE_FORMULA:
                str = String.valueOf(cell.getNumericCellValue());
                dnum = Double.parseDouble(str);
                str = String.valueOf(dnum);
                break;
            default:
                System.out.println("Not a supported cell type");
                str = "";
                break;
        }
        return str;
    }

    /**
     * Displays an error dialog with the given message.
     * 
     * @param frame frame of the dialog
     * @param message message to be displayed in the dialog
     */
    public static void showErrorDialog(JFrame frame, String message) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns a string of formatted date and time.
     * 
     * @return string of formatted data and time
     */
    public static String getDateTime() {
        Date date = new Date();
        return DateFormat.getInstance().format(date);
    }

    /**
     * Returns a string format of a double.  Double value of 0 is
     * simply returns as "0".  Otherwise, numbers are formatted to
     * three decimal places.
     *
     * @param num double value to be formatted
     * @return string
     */
    public static String formatFloat2(double num) {
        if (num == 0) return "0"; else return String.format("%.3f", num);
    }

    /**
     * Returns a string  format of a double.  Double value of 0 is
     * simply returns as "0".  Values less than 0.001 are formatted
     * in scientific notation.  Otherwise, numbers are formatted to three
     * decimal places.
     * 
     * @param num double value to be formatted
     * @return string
     */
    public static String formatFloat(double num) {
        if (num == 0) return "0"; else if (Math.abs(num) < 0.001) {
            String str = String.format("%8.3e", num);
            return convertScientificNotation(str);
        } else {
            return formatDecimal(num, 3);
        }
    }

    /**
     * Returns a string format of a Double using {@link #formatFloat(double)}.
     * 
     * @param num Double to be formatted into a string
     * @return string
     * @see #formatFloat(double)
     */
    public static String formatFloat(Double num) {
        return formatFloat(num.doubleValue());
    }

    /**
     * Returns a string format of a double with a specified number of 
     * decimal places.
     * 
     * @param num double value to be formatted into a string
     * @param places decimal places
     * @return string
     * @see #formatDecimal(double, int)
     */
    public static String formatFloat(double num, int places) {
        return formatDecimal(num, places);
    }

    /**
     * Returns the scientific notation of a number.
     * 
     * @param num string representating a number 
     * @return scientific notation of the input number
     */
    private static String convertScientificNotation(String num) {
        String text = "" + num;
        text = text.toLowerCase();
        int eLocation = text.indexOf('e');
        if (eLocation != -1) {
            double mantissa = Double.parseDouble(text.substring(0, eLocation));
            int exponent = Integer.parseInt(text.substring(eLocation + 1, text.length()));
            text = mantissa + "&nbsp;&middot;&nbsp;10<sup>" + exponent + "</sup>";
        }
        return text;
    }

    /**
     * Formats the given double to a string with the given number of 
     * decimal places.
     * 
     * @param num double to be formatted
     * @param places number of decimal places
     * @return string of formatted number
     */
    private static String formatDecimal(double num, int places) {
        if (num == 0) return "0";
        if (Math.abs(num) < Math.pow(10, -places)) {
            String str = String.format("%." + places + "e", num);
            return convertScientificNotation(str);
        }
        String str = num + "";
        int ptLocation = str.indexOf('.');
        if (ptLocation == -1) return num + "";
        if (str.length() - ptLocation - 1 < places) return num + "";
        return String.format("%." + places + "f", num);
    }

    /**
     * Given a vector of two possible population labels and a vector of values,
     * separate the vector of values corresponding to the labels into two vectors.  
     * The two vectors are assumed to have the same length.
     * 
     * @param Labels
     * @param Values
     * @return an array of four objects: (1) population label 1; (2) vector of
     * values for label 1; (3) population label 2; (4) vector of values for label 2;    
     */
    public static Object[] splitValuesVectorByLabels(Vector<Cell> Labels, Vector<Cell> Values) throws Exception {
        String cat1 = "", cat2 = "";
        Vector<Cell> vector1 = new Vector<Cell>();
        Vector<Cell> vector2 = new Vector<Cell>();
        for (int i = 0; i < Labels.size(); ++i) {
            String s = Labels.elementAt(i).getContents().trim();
            if (!s.equals("")) {
                if (cat1.equals("")) {
                    cat1 = s;
                    vector1.addElement(Values.elementAt(i));
                } else if (s.equals(cat1)) {
                    vector1.addElement(Values.elementAt(i));
                } else if (cat2.equals("")) {
                    cat2 = s;
                    vector2.addElement(Values.elementAt(i));
                } else if (s.equals(cat2)) {
                    vector2.addElement(Values.elementAt(i));
                } else {
                    throw new Exception("The input column contains more than two populations.");
                }
            }
        }
        Object returnValues[] = new Object[4];
        returnValues[0] = cat1;
        returnValues[1] = vector1;
        returnValues[2] = cat2;
        returnValues[3] = vector2;
        return returnValues;
    }

    /**
     * Given a vector of two possible values,
     * separate the vector of values corresponding to the labels into two vectors.  
     * The two vectors are assumed to have the same length.
     * 
     * @param Values
     * @return an array of four objects: (1) category label 1; (2) vector of
     * values for label 1; (3) category label 2; (4) vector of values for label 2; 
     */
    public static Object[] splitValuesVector(Vector<Cell> Values) throws Exception {
        String cat1 = "", cat2 = "";
        Vector<Cell> vector1 = new Vector<Cell>();
        Vector<Cell> vector2 = new Vector<Cell>();
        for (int i = 0; i < Values.size(); ++i) {
            String s = Values.elementAt(i).getContents().trim();
            if (!s.equals("")) {
                if (cat1.equals("")) {
                    cat1 = s;
                    vector1.addElement(Values.elementAt(i));
                } else if (s.equals(cat1)) {
                    vector1.addElement(Values.elementAt(i));
                } else if (cat2.equals("")) {
                    cat2 = s;
                    vector2.addElement(Values.elementAt(i));
                } else if (s.equals(cat2)) {
                    vector2.addElement(Values.elementAt(i));
                } else {
                    throw new Exception("The input column contains more than two categories.");
                }
            }
        }
        Object returnValues[] = new Object[4];
        returnValues[0] = cat1;
        returnValues[1] = vector1;
        returnValues[2] = cat2;
        returnValues[3] = vector2;
        return returnValues;
    }

    /**
     * Given a vector of cells, computes the frequency of each category
     * present in the vector.  Each distinct string in the vector is 
     * considered a category.
     * 
     * @param vec a vector of Cell
     * @return an array of two vectors: (1) a vector of Cell representing
     * the categories, (2) a vector of Double representing the respective
     * frequencies.
     */
    @SuppressWarnings("unchecked")
    public static Object[] ComputeCategoryFrequency(Vector<Cell> vec) {
        TreeMap hash = new TreeMap();
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            String cat = ((Cell) e.nextElement()).getContents();
            if (hash.containsKey(cat)) {
                int count = ((Integer) hash.get(cat)).intValue() + 1;
                hash.put(cat, new Integer(count));
            } else {
                hash.put(cat, new Integer(1));
            }
        }
        Vector<Cell> categories = new Vector<Cell>();
        Vector<Double> frequencies = new Vector<Double>();
        int i = 0;
        Set set = hash.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            String cat = (String) iterator.next();
            int count = ((Integer) hash.get(cat)).intValue();
            categories.addElement(new Cell(cat, 0, 0));
            frequencies.addElement(new Double(count));
            i++;
        }
        Object returnValues[] = new Object[2];
        returnValues[0] = categories;
        returnValues[1] = frequencies;
        return returnValues;
    }

    /**
     * Given a vector of double values, computes the frequency of each value
     * present in the vector.  
     * 
     * @param vec a vector of Cell
     * @return a TreepMap where the key is a double value present in the
     * vector and the value is the frequency of the double value
     */
    @SuppressWarnings("unchecked")
    public static TreeMap ComputeFrequency(Vector<Double> vec) {
        TreeMap hash = new TreeMap();
        for (Enumeration e = vec.elements(); e.hasMoreElements(); ) {
            Double cat = (Double) e.nextElement();
            if (cat != null) {
                if (hash.containsKey(cat)) {
                    int count = ((Integer) hash.get(cat)).intValue() + 1;
                    hash.put(cat, new Integer(count));
                } else {
                    hash.put(cat, new Integer(1));
                }
            }
        }
        return hash;
    }

    /**
     * Return a vector containing the frequency of each class delimited
     * by the given vector of upper class limits.
     * 
     * @param column a vector of numbers to be separated into classes
     * @param limits a vector of numbers representing the upper class limits
     * @return vector of class frequencies
     */
    public static Vector<Double> ComputeClassFrequency(Vector<Double> column, Vector<Double> limits) {
        Vector<Double> frequencies = new Vector<Double>();
        double limit = -1, lastLimit = Double.NEGATIVE_INFINITY;
        int count = 0;
        for (int i = 0; i < limits.size(); ++i) {
            limit = limits.elementAt(i).doubleValue();
            count = 0;
            for (int j = 0; j < column.size(); ++j) {
                double num = column.elementAt(j).doubleValue();
                if (num <= limit && num > lastLimit) {
                    count++;
                }
            }
            frequencies.addElement(new Double(count));
            lastLimit = limit;
        }
        count = 0;
        for (int j = 0; j < column.size(); ++j) {
            double num = column.elementAt(j).doubleValue();
            if (num > lastLimit) count++;
        }
        frequencies.addElement(new Double(count));
        return frequencies;
    }

    /**
     * Returns a vector of substrings in the given input string. 
     * Substrings are delimited by "" or space.
     * 
     * @param str string to be parsed
     * @return a vector of substrings
     */
    public static Vector<String> parseString(String str) {
        Vector<String> result = new Vector<String>();
        int i = 0;
        String value = "";
        while (i < str.length()) {
            if (Character.isWhitespace(str.charAt(i))) {
                if (!value.equals("")) {
                    result.addElement(value);
                    value = "";
                }
                i++;
            } else if (str.charAt(i) == '"') {
                if (!value.equals("")) {
                    result.addElement(value);
                    value = "";
                }
                i++;
                while (str.charAt(i) != '"') {
                    value += str.charAt(i);
                    i++;
                }
                i++;
                if (!value.equals("")) {
                    result.addElement(value);
                    value = "";
                }
            } else {
                value += str.charAt(i);
                i++;
            }
        }
        if (!value.equals("")) {
            result.addElement(value);
        }
        return result;
    }

    public static int getNumDecimalPlaces(String s) {
        int decLoc = s.indexOf('.');
        if (decLoc != -1) return s.length() - decLoc - 1;
        return 0;
    }

    /**
     * Returns the current version number from the Statcato web site.
     *
     * @return a string representing the version number or
     * the string "error" if fails to obtain the version number
     */
    public static String getVersionNumberFromWeb() {
        String currentDirStr = "";
        String fileType;
        File currentDir = new File(".");
        try {
            currentDirStr = currentDir.getCanonicalPath();
        } catch (IOException e) {
            return "error";
        }
        DownloadFile d = new DownloadFile("http://www.statcato.org/versions/current.txt");
        String filename = d.download();
        if (filename == null) {
            return "error";
        }
        File file = new File(filename);
        String contents = HelperFunctions.getFileContents(file);
        boolean success = file.delete();
        if (!success) System.out.println("cannot delete temp file");
        return contents.trim();
    }

    /**
     *  Returns a string representing the current date and time
     *  (mmddyyyyhhss).
     *
     * @return string 
     */
    public static String getCurrentTimeString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
