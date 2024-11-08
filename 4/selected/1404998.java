package net.sf.RecordEditor.copy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import net.sf.JRecord.Common.Constants;
import net.sf.JRecord.Common.RecordException;
import net.sf.JRecord.Common.XmlConstants;
import net.sf.JRecord.Details.AbstractLayoutDetails;
import net.sf.JRecord.Details.AbstractLine;
import net.sf.JRecord.Details.AbstractRecordDetail;
import net.sf.JRecord.Details.LayoutDetail;
import net.sf.JRecord.Details.Line;
import net.sf.JRecord.Details.RecordSelection;
import net.sf.JRecord.Details.XmlLine;
import net.sf.JRecord.ExternalRecordSelection.ExternalFieldSelection;
import net.sf.JRecord.IO.AbstractLineReader;
import net.sf.JRecord.IO.AbstractLineIOProvider;
import net.sf.JRecord.IO.AbstractLineWriter;
import net.sf.JRecord.IO.LineIOProvider;
import net.sf.JRecord.Types.Type;
import net.sf.RecordEditor.jibx.compare.CopyDefinition;
import net.sf.RecordEditor.jibx.compare.Record;
import net.sf.RecordEditor.re.openFile.AbstractLayoutSelection;
import net.sf.RecordEditor.re.openFile.LayoutSelectionFile;
import net.sf.RecordEditor.utils.CsvWriter;
import net.sf.RecordEditor.utils.common.Common;

public final class DoCopy {

    private LayoutDetail dtl1;

    private LayoutDetail dtl2;

    private CopyDefinition cpy;

    private int[][] fromTbl;

    private int[][] toTbl;

    private String[][] defaultValues;

    private int[] toIdx;

    private int[] fromIdx;

    private AbstractLineWriter writer;

    private boolean ok, first;

    private PrintStream fieldErrorStream = null;

    private int errorCount = 0;

    /**
	 * Do Copy
	 * 
	 * @param layoutReader1 layout reader 
	 * @param layoutReader2 layout reader 
	 * @param cpy Copy definition
	 * 
	 * @throws Exception any error
	 */
    public static final boolean copy(AbstractLayoutSelection layoutReader1, AbstractLayoutSelection layoutReader2, CopyDefinition copy) throws Exception {
        boolean ret = true;
        if (CopyDefinition.STANDARD_COPY.equals(copy.type)) {
            ret = new DoCopy(copy, getLayout(layoutReader1, copy.oldFile.layoutDetails.name, copy.oldFile.name, true), getLayout(layoutReader2, copy.newFile.layoutDetails.name, copy.newFile.name, false)).copy2Layouts();
        } else if (CopyDefinition.COBOL_COPY.equals(copy.type)) {
            ret = new DoCopy(copy, getLayout(new LayoutSelectionFile(true), copy.oldFile.layoutDetails.name, copy.oldFile.name, true), getLayout(new LayoutSelectionFile(true), copy.newFile.layoutDetails.name, copy.newFile.name, false)).cobolCopy();
        } else if (CopyDefinition.DELIM_COPY.equals(copy.type)) {
            new DoCopy(copy, getLayout(layoutReader1, copy.oldFile.layoutDetails.name, copy.oldFile.name, true), null).copy2BinDelim();
        } else if (CopyDefinition.VELOCITY_COPY.equals(copy.type)) {
            net.sf.RecordEditor.utils.RunVelocity.getInstance().processFile(layoutReader1.getRecordLayout(""), copy.oldFile.name, copy.velocityTemplate, copy.newFile.name);
        } else if (CopyDefinition.XML_COPY.equals(copy.type)) {
            net.sf.RecordEditor.copy.DoCopy2Xml.newCopy().copyFile(layoutReader1, copy);
        } else {
            new RuntimeException("Invalid type of Copy --> " + copy.type);
        }
        return ret;
    }

    /**
	 * Get a record layout
	 *  
	 * @param layoutReader Layout Reader
	 * @param name Layout name
	 * @param fileName Sample file name
	 * 
	 * @return requested layout
	 * @throws Exception any error
	 */
    private static AbstractLayoutDetails getLayout(AbstractLayoutSelection layoutReader, String name, String fileName, boolean input) throws Exception {
        try {
            AbstractLayoutDetails ret = layoutReader.getRecordLayout(name, fileName);
            if (ret == null) {
                System.err.println("Retrieve of layout: " + name + " failed");
            } else if (input && ret.isBuildLayout() || ret.getFileStructure() == Constants.IO_NAME_1ST_LINE) {
                AbstractLineIOProvider ioProvider = LineIOProvider.getInstance();
                AbstractLineReader reader;
                AbstractLine in;
                reader = ioProvider.getLineReader(ret.getFileStructure());
                reader.open(fileName, ret);
                reader.read();
                if (ret.getFileStructure() == Constants.IO_XML_BUILD_LAYOUT) {
                    for (int i = 0; i < 1000 && reader.read() != null; i++) {
                    }
                }
                ret = reader.getLayout();
            }
            return ret;
        } catch (Exception e) {
            String s = "Error Loading Layout: " + name;
            e.printStackTrace();
            Common.logMsg(s, e);
            throw e;
        }
    }

    public DoCopy(CopyDefinition copy, AbstractLayoutDetails detail1, AbstractLayoutDetails detail2) {
        File file = null;
        cpy = copy;
        dtl1 = (LayoutDetail) detail1;
        dtl2 = (LayoutDetail) detail2;
        if (copy.fieldErrorFile != null) {
            try {
                fieldErrorStream = new PrintStream(copy.fieldErrorFile);
            } catch (Exception e) {
                System.out.println();
                System.out.println("Error Allocating Field Error File: " + e.getMessage());
                System.out.println();
                fieldErrorStream = null;
            }
        }
    }

    /**
	 * Copy files using 2 layouts
	 * @throws IOException any IO Error
	 * @throws RecordException any record-editor exception
	 */
    private final boolean copy2Layouts() throws IOException, RecordException {
        int idx, lineNo;
        AbstractLineIOProvider ioProvider = LineIOProvider.getInstance();
        AbstractLineReader reader;
        AbstractLine in, next;
        ok = true;
        buildTranslations();
        reader = ioProvider.getLineReader(dtl1.getFileStructure());
        writer = ioProvider.getLineWriter(dtl2.getFileStructure());
        reader.open(cpy.oldFile.name, dtl1);
        writer.open(cpy.newFile.name);
        first = true;
        in = reader.read();
        lineNo = 0;
        if (dtl1.getRecordCount() < 2) {
            while (in != null) {
                writeRecord(in, (next = reader.read()), 0, lineNo++);
                in = next;
            }
        } else {
            while (in != null) {
                idx = in.getPreferredLayoutIdx();
                if (idx >= 0) {
                    while ((next = reader.read()) != null && next.getPreferredLayoutIdx() < 0) {
                    }
                    writeRecord(in, next, idx, lineNo++);
                    in = next;
                }
            }
        }
        reader.close();
        writer.close();
        closeFieldError();
        return ok;
    }

    /**
	 * Write one Record
	 * 
	 * @param in record that was readin
	 * @param idx record index
	 * @param lineNo line number
	 * @throws IOException any IO Error that occurs
	 */
    private void writeRecord(AbstractLine in, AbstractLine next, int idx, int lineNo) throws IOException {
        if (idx < fromIdx.length && idx < toIdx.length) {
            int i1 = fromIdx[idx];
            int i2 = toIdx[idx];
            if (i1 >= 0 && i2 >= 0) {
                int parentIdx;
                AbstractLine out;
                Object o = null;
                int[] fromFields = fromTbl[i1];
                int[] toFields = toTbl[i1];
                if (dtl2.isXml()) {
                    XmlLine o1 = new XmlLine(dtl2, i2);
                    out = o1;
                    setDefaultValues(i1, i2, out);
                    if (fromTbl.length == 1 || next == null || (idx == next.getPreferredLayoutIdx())) {
                        try {
                            o1.setRawField(i2, XmlConstants.END_INDEX, true);
                        } catch (Exception e) {
                        }
                    }
                    if (first && (parentIdx = dtl2.getRecord(i2).getParentRecordIndex()) >= 0) {
                        printParent(parentIdx);
                        first = false;
                    }
                } else {
                    out = LineIOProvider.getInstance().getLineProvider(dtl2.getFileStructure()).getLine(dtl2);
                    try {
                        RecordSelection sel = dtl2.getRecord(i2).getRecordSelection();
                        for (ExternalFieldSelection fs : sel.getAllFields()) {
                            if (fs != null) {
                                out.setField(fs.getFieldName(), fs.getFieldValue());
                            }
                        }
                    } catch (Exception e) {
                    }
                    setDefaultValues(i1, i2, out);
                }
                for (int i = 0; i < fromFields.length; i++) {
                    if (fromFields[i] >= 0 && toFields[i] >= 0) {
                        try {
                            o = in.getField(idx, fromFields[i]);
                            if (o == null) {
                                o = "";
                                if (dtl2.getRecord(i2).getFieldsNumericType(toFields[i]) == Type.NT_NUMBER) {
                                    o = "0";
                                }
                            }
                            out.setField(i2, toFields[i], o);
                        } catch (Exception e) {
                            String em = "Error Line " + lineNo + " Field Number " + i + " - " + e.getMessage() + " : " + o;
                            if (fieldErrorStream == null) {
                                Common.logMsg(em, null);
                            } else {
                                fieldErrorStream.println(em);
                            }
                            errorCount += 1;
                            if (cpy.maxErrors >= 0 && errorCount > cpy.maxErrors) {
                                writer.write(out);
                                writer.close();
                                throw new IOException("Max Conversion Errors Reached: " + cpy.maxErrors, e);
                            }
                        }
                    }
                }
                writer.write(out);
                first = false;
            }
        }
    }

    private void setDefaultValues(int i1, int i2, AbstractLine out) {
        if (defaultValues[i1] != null) {
            for (int i = 0; i < defaultValues[i1].length; i++) {
                if (defaultValues[i1][i] != null) {
                    try {
                        out.setField(i2, i, defaultValues[i1][i]);
                    } catch (Exception e) {
                        System.out.println("Error > " + i2 + " " + i + " " + dtl2.getRecord(i2).getField(i).getName() + " " + defaultValues[i2][i] + "<");
                        e.printStackTrace();
                        ok = false;
                    }
                }
            }
        }
    }

    private void printParent(int idx) throws IOException {
        int parentIdx;
        if ((parentIdx = dtl2.getRecord(idx).getParentRecordIndex()) >= 0) {
            printParent(parentIdx);
        }
        writer.write(new XmlLine(dtl2, idx));
    }

    /**
	 * build translation table
	 */
    private void buildTranslations() {
        AbstractRecordDetail rec1, rec2;
        Record recList1, recList2;
        int i, j, size, idx1, idx2, ix;
        boolean noDefault;
        toIdx = new int[dtl1.getRecordCount()];
        fromIdx = new int[dtl1.getRecordCount()];
        defaultValues = new String[dtl2.getRecordCount()][];
        fromTbl = new int[cpy.oldFile.layoutDetails.records.size()][];
        toTbl = new int[fromTbl.length][];
        for (i = 0; i < toIdx.length; i++) {
            toIdx[i] = -1;
        }
        for (i = 0; i < fromTbl.length; i++) {
            recList1 = cpy.oldFile.layoutDetails.records.get(i);
            recList2 = cpy.newFile.layoutDetails.records.get(i);
            idx1 = dtl1.getRecordIndex(recList1.name);
            idx2 = dtl2.getRecordIndex(recList2.name);
            if (idx1 < 0) {
                if (dtl1.getRecordCount() > 0) {
                    System.out.println("Record 0: >" + dtl1.getRecord(0).getRecordName() + "<");
                }
                throw new RuntimeException("Can not locate record: >" + recList1.name + "< in input layout");
            }
            if (idx2 < 0) {
                throw new RuntimeException("Can not locate record: " + recList2.name + " in output layout");
            }
            fromIdx[idx1] = i;
            toIdx[idx1] = idx2;
            rec1 = dtl1.getRecord(idx1);
            rec2 = dtl2.getRecord(idx2);
            size = recList1.fields.length;
            fromTbl[i] = new int[size];
            toTbl[i] = new int[size];
            defaultValues[i] = new String[rec2.getFieldCount()];
            for (j = 0; j < defaultValues[i].length; j++) {
                if (rec2.getFieldsNumericType(j) == Type.NT_NUMBER) {
                    defaultValues[i][j] = "0";
                } else {
                    defaultValues[i][j] = "";
                }
            }
            for (j = 0; j < size; j++) {
                fromTbl[i][j] = rec1.getFieldIndex(recList1.fields[j]);
                ix = rec2.getFieldIndex(recList2.fields[j]);
                toTbl[i][j] = ix;
                defaultValues[i][ix] = null;
                if (fromTbl[i][j] < 0 && toTbl[i][j] < 0) {
                    System.out.println("Can not find input & output fields: " + j + " " + recList1.fields[j] + ", " + recList2.fields[j]);
                } else if (fromTbl[i][j] < 0) {
                    StringBuilder bb = new StringBuilder();
                    for (int k = 0; k < rec1.getFieldCount(); k++) {
                        bb.append("<\t>").append(rec1.getField(k).getName());
                    }
                    System.out.println("Can not find input field: " + j + " >" + recList1.fields[j] + "< } " + bb.toString() + " -- " + fromTbl[i][j]);
                } else if (toTbl[i][j] < 0) {
                    System.out.println("Can not find output field: " + j + " " + recList2.fields[j]);
                }
            }
            noDefault = true;
            for (j = 0; j < defaultValues[i].length; j++) {
                if (defaultValues[i][j] != null) {
                    noDefault = false;
                    break;
                }
            }
            if (noDefault) {
                defaultValues[i] = null;
            }
        }
    }

    /**
	 * build translation table
	 */
    private void buildTranslations1layout() {
        AbstractRecordDetail rec1;
        Record recList1;
        int i, j, size, idx1;
        toIdx = new int[dtl1.getRecordCount()];
        fromIdx = new int[dtl1.getRecordCount()];
        size = dtl1.getRecordCount();
        if (cpy.oldFile.layoutDetails.records == null) {
            fromTbl = new int[dtl1.getRecordCount()][];
            for (i = 0; i < fromTbl.length; i++) {
                fromIdx[i] = i;
                size = dtl1.getRecord(i).getFieldCount();
                fromTbl[i] = new int[size];
                for (j = 0; j < size; j++) {
                    fromTbl[i][j] = j;
                }
            }
        } else {
            fromTbl = new int[cpy.oldFile.layoutDetails.records.size()][];
            for (i = 0; i < fromTbl.length; i++) {
                recList1 = cpy.oldFile.layoutDetails.records.get(i);
                idx1 = dtl1.getRecordIndex(recList1.name);
                fromIdx[idx1] = i;
                rec1 = dtl1.getRecord(idx1);
                size = recList1.fields.length;
                fromTbl[i] = new int[size];
                for (j = 0; j < size; j++) {
                    fromTbl[i][j] = rec1.getFieldIndex(recList1.fields[j]);
                }
            }
        }
    }

    private final boolean cobolCopy() throws IOException, RecordException {
        int idx;
        AbstractLineIOProvider ioProvider = LineIOProvider.getInstance();
        AbstractLineReader reader;
        AbstractLine in;
        ok = true;
        reader = ioProvider.getLineReader(dtl1.getFileStructure());
        writer = ioProvider.getLineWriter(dtl2.getFileStructure());
        reader.open(cpy.oldFile.name, dtl1);
        writer.open(cpy.newFile.name);
        if (dtl1.getRecordCount() < 2) {
            while ((in = reader.read()) != null) {
                writeCobRecord(in, 0);
            }
        } else {
            while ((in = reader.read()) != null) {
                idx = in.getPreferredLayoutIdx();
                if (idx >= 0) {
                    writeCobRecord(in, idx);
                }
            }
        }
        reader.close();
        writer.close();
        closeFieldError();
        return ok;
    }

    /**
	 * Write one Record
	 * 
	 * @param in record that was readin
	 * @param idx record index
	 * @throws IOException any IO Error that occurs
	 */
    private void writeCobRecord(AbstractLine in, int idx) throws IOException {
        AbstractLine out = new Line(dtl2);
        if (idx >= 0) {
            Object o = null;
            int len = in.getLayout().getRecord(idx).getFieldCount();
            for (int i = 0; i < len; i++) {
                try {
                    o = in.getField(idx, i);
                    out.setField(idx, i, o);
                } catch (Exception e) {
                    Common.logMsg("Error in Field " + in.getLayout().getRecord(idx).getField(i).getName() + " " + e.getMessage() + " : " + o, null);
                    ok = false;
                }
            }
            writer.write(out);
        }
    }

    /**
	 * copy file to a delimited file
	 * @throws IOException any IO error
	 * @throws RecordException any RecordEditor conversion issues
	 */
    private void copy2BinDelim() throws IOException, RecordException {
        int idx, lineNo;
        AbstractLineIOProvider ioProvider = LineIOProvider.getInstance();
        AbstractLineReader reader;
        AbstractLine in;
        CsvWriter writer = new CsvWriter(cpy.newFile.name, cpy.delimiter, cpy.font, cpy.quote, false, null);
        reader = ioProvider.getLineReader(dtl1.getFileStructure());
        reader.open(cpy.oldFile.name, dtl1);
        buildTranslations1layout();
        if (cpy.namesOnFirstLine) {
            for (int i = 0; i < fromTbl[fromIdx[0]].length; i++) {
                writer.writeFieldHeading(dtl1.getRecord(fromIdx[0]).getField(fromTbl[0][i]).getName());
            }
            writer.newLine();
        }
        lineNo = 0;
        if (dtl1.getRecordCount() < 2) {
            while ((in = reader.read()) != null) {
                lineNo += 1;
                writeBinCsvLine(writer, in, lineNo, 0);
            }
        } else {
            while ((in = reader.read()) != null) {
                idx = in.getPreferredLayoutIdx();
                if (idx >= 0) {
                    lineNo += 1;
                    writeBinCsvLine(writer, in, lineNo, idx);
                }
            }
        }
        reader.close();
        writer.close();
        closeFieldError();
    }

    private void writeBinCsvLine(CsvWriter writer, AbstractLine in, int lineNo, int idx) throws IOException {
        int i1 = fromIdx[idx];
        if (i1 >= 0) {
            Object o = null;
            int[] fromFields = fromTbl[i1];
            for (int i = 0; i < fromTbl[i1].length; i++) {
                try {
                    o = in.getField(i1, fromFields[i]);
                    if (o == null) {
                        writer.writeField(null, true);
                    } else {
                        writer.writeField(o.toString(), dtl1.getRecord(i1).getFieldsNumericType(fromFields[i]) == Type.NT_NUMBER);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error " + e.getMessage() + " : " + o);
                }
            }
            writer.newLine();
        }
    }

    private void closeFieldError() {
        if (fieldErrorStream != null) {
            fieldErrorStream.close();
        }
    }
}
