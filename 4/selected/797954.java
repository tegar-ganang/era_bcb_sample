package Cosmo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import Cosmo.event.CosmoEvent;
import Cosmo.ui.graphics.model;

public class Utils {

    public static final void writeToFile(String fileName, String value) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("C:\\Sajjan\\Work\\WITA\\workspace\\Cosmo-New\\log\\" + fileName, true));
            out.write("\n" + value);
            out.close();
        } catch (Exception e) {
            Cosmo.util.Constants.iLog.LogInfoLine("Error writing to trace file:" + e);
        }
    }

    public static void copy(String src, String dest) throws IOException {
        File sourceFile = new File(src);
        File destFile = new File(dest);
        InputStream in = new FileInputStream(sourceFile);
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void copyFolderStucture(String strPath, String dstPath) throws IOException {
        Constants.iLog.LogInfoLine("copying " + strPath);
        File src = new File(strPath);
        File dest = new File(dstPath);
        if (src.isDirectory()) {
            dest.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                String dest1 = dest.getAbsolutePath() + "\\" + list[i];
                String src1 = src.getAbsolutePath() + "\\" + list[i];
                copyFolderStucture(src1, dest1);
            }
        } else {
            FileInputStream fin = new FileInputStream(src);
            FileOutputStream fout = new FileOutputStream(dest);
            int c;
            while ((c = fin.read()) >= 0) fout.write(c);
            fin.close();
            fout.close();
        }
    }

    public static void createAlert(String message, String header, int type) {
        JOptionPane.showMessageDialog(new JFrame(), message, header, type);
    }

    public static void printStringArray(String[] str) {
        int length = str.length;
        System.out.println("---------Contents of Array-------");
        for (int i = 0; i < length; i++) {
            System.out.println("[" + i + "]" + str[i]);
        }
    }

    public static Object[] appendToArray(Object[] original, Object item) {
        Object[] retVal = new Object[original.length + 1];
        for (int i = 0; i < original.length; i++) {
            retVal[i] = original[i];
        }
        retVal[retVal.length - 1] = item;
        return retVal;
    }

    public static final String replaceNullWithEmpty(Object obj) {
        if (obj == null) return ""; else return obj.toString();
    }

    public static boolean containsString(Vector<Vector<String>> existingVariableNames, String valueAt) {
        System.out.println("in utils: vector=" + existingVariableNames);
        System.out.println("in utils: valueAt=" + valueAt);
        if (existingVariableNames == null || valueAt == null) return false;
        int noOfElementsInVector = existingVariableNames.size();
        for (int i = 0; i < noOfElementsInVector; i++) {
            if (replaceNullWithEmpty(valueAt).equalsIgnoreCase(existingVariableNames.elementAt(i).elementAt(0).toString())) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(extractModelName("CA[hi"));
    }

    public static CosmoEvent getErrorResponse(String message) {
        return new CosmoEvent(CosmoEvent.ERROR, CosmoEvent.TEMPLATE_MODEL, message);
    }

    public static CosmoEvent getSuccessResponse(String message) {
        return new CosmoEvent(CosmoEvent.REFRESH, CosmoEvent.TEMPLATE_MODEL, message);
    }

    public static CosmoEvent getNoResponse(String message) {
        return new CosmoEvent(CosmoEvent.DONTSEND, CosmoEvent.TEMPLATE_MODEL, message);
    }

    public static Vector<String> toVector(String[] values) {
        Vector<String> retVal = new Vector<String>();
        for (int i = 0; i < values.length; i++) {
            retVal.add(values[i]);
        }
        return retVal;
    }

    public static Vector<String> toVector(Enumeration<String> values) {
        Vector<String> retVal = new Vector<String>();
        while (values.hasMoreElements()) {
            retVal.add(values.nextElement());
        }
        return retVal;
    }

    public static String[] toStringArray(Object[] original) {
        String[] retVal = new String[original.length];
        for (int i = 0; i < original.length; i++) {
            retVal[i] = original[i].toString();
        }
        return retVal;
    }

    public static int indexOfClassArray(Class<? extends Object>[] array, String className) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].getSimpleName().equalsIgnoreCase(className)) return i;
        }
        return -1;
    }

    public static int indexOfStringArray(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    public static String[] getFieldNames(Field[] f) {
        String[] retVal = new String[f.length];
        for (int i = 0; i < f.length; i++) {
            retVal[i] = f[i].getName();
        }
        return retVal;
    }

    public static Vector<String> getDifference(Vector<String> v1, Vector<String> v2) {
        Vector<String> retVal = new Vector<String>();
        for (int i = 0; i < v1.size(); i++) {
            if (!v2.contains(v1.elementAt(i))) retVal.add(v1.elementAt(i));
        }
        return retVal;
    }

    public static String extractModelName(String nodeString) {
        return nodeString.split("\\[")[0].trim();
    }

    public static String[] toStringArray(Vector<String> columnNamesForTable) {
        String[] retvStringArray = new String[columnNamesForTable.size()];
        for (int i = 0; i < columnNamesForTable.size(); i++) {
            retvStringArray[i] = columnNamesForTable.elementAt(i).toString();
        }
        return retvStringArray;
    }

    public static Vector<String> getColumnFromVectorOfVectors(Vector<Vector<String>> v, int index) {
        Vector<String> retVector = new Vector<String>();
        for (int i = 0; i < v.size(); i++) {
            retVector.add(v.elementAt(i).elementAt(index).toString());
        }
        return retVector;
    }

    /**
	 * First splits by _ and then by : to take out the dimension
	 * @param modelName
	 * @return
	 */
    public static int extractInstanceID(String modelName) {
        try {
            String[] nameComponents = modelName.split(Constants._);
            if (nameComponents.length >= 2) return Integer.parseInt(nameComponents[1].split(":")[0]);
            return 0;
        } catch (NumberFormatException e) {
            Utils.createAlert("Error resolving model ID:" + modelName, "Parse Error", Constants.MESSAGE_ERROR);
            e.printStackTrace();
            return 0;
        }
    }

    public static String extractInstanceModelName(String modelName) {
        return modelName.split(Constants._)[0];
    }

    public static String extractModelNameAndModelID(String modelName) {
        return modelName.split(":")[0];
    }

    public static Vector<Vector<String>> getStateVariables(model m) {
        Vector<Vector<String>> StateVariables = new Vector<Vector<String>>();
        Vector stateVariables = m.getStates(m.getTID());
        for (int j = 0; j < stateVariables.size(); j++) {
            Vector<String> currStateVariable = new Vector<String>();
            String stateVarName = stateVariables.get(j).toString();
            String stateVarVal = m.getStateVariableValue(m.getTID(), stateVarName);
            String stateVarType = m.getStateVariableType(m.getTID(), stateVarName);
            currStateVariable.add(stateVarName);
            currStateVariable.add(stateVarVal);
            currStateVariable.add(stateVarType);
            StateVariables.add(currStateVariable);
        }
        return StateVariables;
    }
}
