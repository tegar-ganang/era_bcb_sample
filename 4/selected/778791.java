package com.objectwave.classFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A specific attribute info.
 * @see		ClassFile
 */
public class CodeAttributeInfo extends AttributeInfo {

    AttributeInfo[] otherAttributes;

    byte[] exceptionTable;

    static final int lookupSwitchOpCode = 171;

    static final int tableSwitchOpCode = 170;

    static final int invokeInterfaceOpCode = 185;

    ConstantPoolInfo[] caughtTypes;

    /**
	 */
    protected void fixUpConstants(ClassFile target, ConstantPoolInfo[] originalPool) throws Exception {
        super.fixUpConstants(target, originalPool);
        if (caughtTypes != null) {
            for (int i = 0; i < caughtTypes.length; ++i) {
                caughtTypes[i] = target.recursiveAdd(caughtTypes[i]);
            }
        }
        if (otherAttributes != null) {
            for (int i = 0; i < otherAttributes.length; ++i) {
                otherAttributes[i].fixUpConstants(target, originalPool);
            }
        }
        fixUpCode(target, originalPool);
    }

    /**
     * If this method is copied into another class, it may become necessary to fix up all references
     * to the constants in the original pool.
     */
    protected void fixUpCode(ClassFile target, ConstantPoolInfo[] originalPool) throws Exception {
        int len = getInt(data, 4);
        byte[] twoBytes = new byte[2];
        for (int i = 8; i - 8 < len; ++i) {
            int idx = ((int) 0xff & data[i]);
            if (idx < opCodeTable.length && idx > -1) {
                if (shouldGetConstantFromOne(idx)) {
                    int barIdx = opCodeTable[idx].lastIndexOf('_');
                    int varIdx = -1;
                    if (barIdx < 0) {
                        twoBytes[0] = 0;
                        twoBytes[1] = data[++i];
                        varIdx = indexFromBytes(twoBytes);
                        ConstantPoolInfo info = originalPool[varIdx];
                        ConstantPoolInfo cp = target.recursiveAdd(info);
                        varIdx = cp.indexOf(cp, target.constantPool);
                        twoBytes = bytesFromIndex((short) varIdx);
                        data[i] = twoBytes[1];
                    }
                }
                if (shouldGetConstant(idx)) {
                    int varIdx = -1;
                    twoBytes[0] = data[++i];
                    twoBytes[1] = data[++i];
                    varIdx = indexFromBytes(twoBytes);
                    ConstantPoolInfo info = originalPool[varIdx];
                    ConstantPoolInfo cp = target.recursiveAdd(info);
                    varIdx = cp.indexOf(cp, target.constantPool);
                    twoBytes = bytesFromIndex((short) varIdx);
                    data[i - 2] = twoBytes[0];
                    data[i - 1] = twoBytes[1];
                }
            }
        }
    }

    /**
	 */
    public CodeAttributeInfo() {
    }

    public void adjustExceptionTable(short increase) {
        ConstantPoolInfo[] result;
        if (exceptionTable != null && exceptionTable.length > 0) {
            System.out.println("Adjusting exception table!");
            result = new ConstantPoolInfo[exceptionTable.length / 8];
            for (int i = 0; i < result.length; ++i) {
                int il = (i * 8) + 6;
                int start_pc = il - 6;
                int end_pc = il - 4;
                int handler_pc = il - 2;
                byte[] twoBytes = new byte[2];
                twoBytes[0] = exceptionTable[start_pc];
                twoBytes[1] = exceptionTable[start_pc + 1];
                short idx = indexFromBytes(twoBytes);
                System.out.println("Changing " + idx + " to " + (idx + increase));
                twoBytes = bytesFromIndex((short) (idx + increase));
                exceptionTable[start_pc] = twoBytes[0];
                exceptionTable[start_pc + 1] = twoBytes[1];
                twoBytes[0] = exceptionTable[end_pc];
                twoBytes[1] = exceptionTable[end_pc + 1];
                idx = indexFromBytes(twoBytes);
                twoBytes = bytesFromIndex((short) (idx + increase));
                exceptionTable[end_pc] = twoBytes[0];
                exceptionTable[end_pc + 1] = twoBytes[1];
                twoBytes[0] = exceptionTable[handler_pc];
                twoBytes[1] = exceptionTable[handler_pc + 1];
                idx = indexFromBytes(twoBytes);
                twoBytes = bytesFromIndex((short) (idx + increase));
                exceptionTable[handler_pc] = twoBytes[0];
                exceptionTable[handler_pc + 1] = twoBytes[1];
            }
        }
    }

    /**
	 * Null values in the resulting array are legal. They represent 'finally' catch blocks.
	 * @return All of the caught types declared in the exception table.
	 */
    public ConstantPoolInfo[] getCatchTypes(ConstantPoolInfo[] pool) {
        ConstantPoolInfo[] result;
        if (exceptionTable.length > 0) {
            result = new ConstantPoolInfo[exceptionTable.length / 8];
            for (int i = 0; i < result.length; ++i) {
                int idx = (i * 8) + 6;
                int start_pc = idx - 6;
                int end_pc = idx - 4;
                int handler_pc = idx - 2;
                byte[] twoBytes = new byte[2];
                twoBytes[0] = exceptionTable[start_pc];
                twoBytes[1] = exceptionTable[start_pc + 1];
                twoBytes[0] = exceptionTable[end_pc];
                twoBytes[1] = exceptionTable[end_pc + 1];
                twoBytes[0] = exceptionTable[handler_pc];
                twoBytes[1] = exceptionTable[handler_pc + 1];
                twoBytes[0] = exceptionTable[idx];
                twoBytes[1] = exceptionTable[idx + 1];
                int constPoolIdx = indexFromBytes(twoBytes);
                if (constPoolIdx != 0) {
                    result[i] = pool[constPoolIdx];
                }
            }
        } else {
            result = new ConstantPoolInfo[0];
        }
        return result;
    }

    /**
	 */
    public CodeAttributeInfo(ConstantPoolInfo newName, byte newData[]) {
        super(newName, newData);
    }

    /**
	Code_attribute {
            u2 attribute_name_index;
            u4 attribute_length;
            u2 max_stack;
            u2 max_locals;
            u4 code_length;
            u1 code[code_length];
            u2 exception_table_length;
            {
                u2 start_pc;
                u2 end_pc;
                u2  handler_pc;
                u2  catch_type;
            }
            exception_table[exception_table_length];

            u2 attributes_count;
            attribute_info attributes[attributes_count];
	 */
    public boolean read(DataInputStream di, ConstantPoolInfo pool[]) throws IOException {
        int len;
        if (name == null) {
            name = pool[di.readShort()];
        }
        len = di.readInt();
        data = new byte[len];
        di.readFully(data);
        readCodeAttributes(pool);
        return true;
    }

    /**
	 * Optional code attributes. These include a LocalVariableTable and the  LineNumberTable.
	 */
    protected void readCodeAttributes(ConstantPoolInfo pool[]) throws IOException {
        int len = data.length;
        int actualCodeLen = getInt(data, 4);
        final int headerAndCode = actualCodeLen + 8;
        int dif = len - headerAndCode;
        if (dif > 0) {
            byte[] twoBytes = new byte[2];
            twoBytes[0] = data[headerAndCode + 0];
            twoBytes[1] = data[headerAndCode + 1];
            int addlCount = indexFromBytes(twoBytes);
            int exceptionTableData = (addlCount * 8) + 2;
            if (exceptionTableData > 2) {
                exceptionTable = new byte[exceptionTableData - 2];
                System.arraycopy(data, headerAndCode + 2, exceptionTable, 0, exceptionTableData - 2);
                caughtTypes = getCatchTypes(pool);
            }
            dif = dif - exceptionTableData;
            if (dif > 0) {
                byte[] bytes = new byte[dif - 2];
                twoBytes[0] = data[headerAndCode + exceptionTableData + 0];
                twoBytes[1] = data[headerAndCode + exceptionTableData + 1];
                addlCount = indexFromBytes(twoBytes);
                System.arraycopy(data, headerAndCode + exceptionTableData + 2, bytes, 0, dif - 2);
                java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(bytes);
                DataInputStream din = new DataInputStream(bin);
                otherAttributes = new AttributeInfo[addlCount];
                for (int i = 0; i < addlCount; ++i) {
                    AttributeInfo addl = AttributeInfo.readAttributeInfo(din, pool);
                    otherAttributes[i] = addl;
                    if (addl != null) {
                    } else {
                        System.out.println("Failed optional method attribute!");
                    }
                }
            }
        }
    }

    /**
	 * Write the bytes to the output stream.
	 * @param dos The DataOutputStream upon which this is writing
	 * @param pool The constant pool in which to index.
	 */
    public void write(DataOutputStream dos, ConstantPoolInfo pool[]) throws IOException, Exception {
        dos.writeShort(ConstantPoolInfo.indexOf(name, pool));
        dos.writeInt(data.length);
        int len = data.length;
        int actualCodeLen = getInt(data, 4);
        final int headerAndCode = actualCodeLen + 8;
        dos.write(data, 0, headerAndCode + 2);
        byte[] twoBytes = new byte[2];
        twoBytes[0] = data[headerAndCode + 0];
        twoBytes[1] = data[headerAndCode + 1];
        int addlCount = indexFromBytes(twoBytes);
        if (caughtTypes == null || caughtTypes.length == 0 || addlCount != caughtTypes.length) {
            dos.write(data, headerAndCode + 2, (addlCount * 8));
        } else {
            for (int i = 0; i < addlCount; ++i) {
                dos.write(exceptionTable, (i * 8), 6);
                short idx = caughtTypes[i].indexOf(caughtTypes[i], pool);
                twoBytes = bytesFromIndex(idx);
                dos.write(twoBytes);
            }
        }
        if (otherAttributes != null) {
            twoBytes = bytesFromIndex((short) otherAttributes.length);
            dos.write(twoBytes);
            for (int i = 0; i < otherAttributes.length; ++i) {
                otherAttributes[i].write(dos, pool);
            }
        } else {
            twoBytes = bytesFromIndex((short) 0);
            dos.write(twoBytes);
        }
    }

    /**
	 * If there is a local var table, find the local var at the index.
	 */
    public String getLocalVar(int idx) {
        try {
            if (otherAttributes == null) return "";
            LocalVariableAttributeInfo locals = null;
            for (int i = 0; i < otherAttributes.length; ++i) {
                if (otherAttributes[i] instanceof LocalVariableAttributeInfo) {
                    locals = (LocalVariableAttributeInfo) otherAttributes[i];
                    break;
                }
            }
            if (locals == null) return "";
            return locals.getLocalVarName(idx);
        } catch (Throwable t) {
            return "";
        }
    }

    /**
	 */
    public String toString(ConstantPoolInfo pool[]) {
        StringBuffer x = new StringBuffer();
        String type = name.toString();
        if (caughtTypes != null) {
            x.append(String.valueOf(caughtTypes.length) + " type(s) caught by blocks\n");
            for (int i = 0; i < caughtTypes.length; ++i) {
                if (caughtTypes[i] == null) {
                    x.append("finally\n");
                } else {
                    x.append(caughtTypes[i].toString() + '\n');
                }
            }
        }
        try {
            displayCode(x, pool);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println(x);
            throw ex;
        }
        return x.toString();
    }

    /**
     */
    public void addCalledMethods(ArrayList result, ConstantPoolInfo pool[]) {
        byte[] twoBytes = new byte[2];
        int len = getInt(data, 4);
        for (int i = 8; i - 8 < len; ++i) {
            int idx = ((int) 0xff & data[i]);
            if (idx < opCodeTable.length && idx > -1) {
                if (idx == lookupSwitchOpCode || idx == tableSwitchOpCode) {
                    i = handleLookupSwitch(idx, data, i, new StringBuffer());
                }
                if (idx == invokeInterfaceOpCode) {
                    twoBytes[0] = data[++i];
                    twoBytes[1] = data[++i];
                    result.add(pool[indexFromBytes(twoBytes)]);
                    ++i;
                    i++;
                }
                if (shouldGetConstantFromOne(idx)) {
                    int barIdx = opCodeTable[idx].lastIndexOf('_');
                    int varIdx = -1;
                    if (barIdx < 0) {
                        twoBytes[0] = 0;
                        twoBytes[1] = data[++i];
                        varIdx = indexFromBytes(twoBytes);
                    } else {
                        try {
                            varIdx = new Integer(opCodeTable[idx].substring(barIdx + 1).trim()).intValue();
                        } catch (Exception t) {
                        }
                    }
                    if (varIdx > -1) {
                        if (pool[varIdx].type == ConstantPoolInfo.METHODREF) {
                            result.add(pool[varIdx]);
                        }
                    }
                }
                if (shouldGetConstant(idx)) {
                    twoBytes[0] = data[++i];
                    twoBytes[1] = data[++i];
                    int aIdx = indexFromBytes(twoBytes);
                    if (pool[aIdx].type == ConstantPoolInfo.METHODREF) {
                        result.add(pool[aIdx]);
                    }
                }
                if (localVarAccess(idx)) {
                    int barIdx = opCodeTable[idx].lastIndexOf('_');
                    if (barIdx < 0) {
                        ++i;
                    }
                }
                if (hasFourByteDatum(idx)) {
                    i++;
                    i++;
                    i++;
                    i++;
                }
                if (hasTwoByteDatum(idx)) {
                    twoBytes[0] = data[++i];
                    twoBytes[1] = data[++i];
                }
                if (hasOneByteDatum(idx)) {
                    twoBytes[1] = data[++i];
                }
            } else {
                throw new RuntimeException("Failed to correctly process bytes!!! idx in not in allowed range.");
            }
        }
    }

    /**
	 * Only called by the MethodInfo class if the System parameter of ow.showAttributes is set to non-null.
	 * @param buff The buffer upon which information is being written.
	 * @param pool ConstantPoolInfo [] The constant pool containing strings, classes, etc...
	 */
    protected void displayCode(final StringBuffer buff, ConstantPoolInfo pool[]) {
        buff.append("\tCode <" + data.length + " bytes>");
        if (exceptionTable != null) {
            buff.append("\tExceptionTable <" + exceptionTable.length + " bytes>");
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        try {
            if (System.getProperty("ow.hexDump") != null) {
                hexDump(data, data.length, sw);
                buff.append(sw.toString());
            }
            buff.append("\n");
            byte[] twoBytes = new byte[2];
            twoBytes[0] = data[0];
            twoBytes[1] = data[1];
            buff.append("\tmax_stack " + indexFromBytes(twoBytes) + "\n");
            twoBytes[0] = data[2];
            twoBytes[1] = data[3];
            buff.append("\tmax_locals " + indexFromBytes(twoBytes) + "\n");
            int len = getInt(data, 4);
            for (int i = 8; i - 8 < len; ++i) {
                buff.append("\t" + ((int) 0xff & data[i]) + " ");
                int idx = ((int) 0xff & data[i]);
                if (idx < opCodeTable.length && idx > -1) {
                    buff.append(opCodeTable[idx]);
                    if (idx == lookupSwitchOpCode || idx == tableSwitchOpCode) {
                        i = handleLookupSwitch(idx, data, i, buff);
                    }
                    if (idx == invokeInterfaceOpCode) {
                        buff.append(" ");
                        twoBytes[0] = data[++i];
                        twoBytes[1] = data[++i];
                        buff.append(pool[indexFromBytes(twoBytes)]);
                        twoBytes[0] = 0;
                        twoBytes[1] = data[++i];
                        int nArgs = indexFromBytes(twoBytes);
                        i++;
                    }
                    if (shouldGetConstantFromOne(idx)) {
                        int barIdx = opCodeTable[idx].lastIndexOf('_');
                        int varIdx = -1;
                        if (barIdx < 0) {
                            twoBytes[0] = 0;
                            twoBytes[1] = data[++i];
                            varIdx = indexFromBytes(twoBytes);
                        } else {
                            try {
                                varIdx = new Integer(opCodeTable[idx].substring(barIdx + 1).trim()).intValue();
                            } catch (Throwable t) {
                                buff.append(t.toString());
                            }
                        }
                        if (varIdx > -1) {
                            buff.append(" ");
                            buff.append(pool[varIdx]);
                        }
                    }
                    if (shouldGetConstant(idx)) {
                        buff.append(" ");
                        twoBytes[0] = data[++i];
                        twoBytes[1] = data[++i];
                        buff.append(pool[indexFromBytes(twoBytes)]);
                    }
                    if (localVarAccess(idx)) {
                        int barIdx = opCodeTable[idx].lastIndexOf('_');
                        int varIdx = -1;
                        if (barIdx < 0) {
                            buff.append(" localVar ");
                            varIdx = data[++i];
                        } else {
                            try {
                                varIdx = new Integer(opCodeTable[idx].substring(barIdx + 1).trim()).intValue();
                            } catch (Throwable t) {
                                buff.append(t.toString());
                            }
                        }
                        if (varIdx > -1) {
                            buff.append(' ');
                            buff.append(getLocalVar(varIdx));
                        }
                    }
                    if (hasFourByteDatum(idx)) {
                        buff.append(' ');
                        int ival = getInt(data, ++i);
                        i = i + 3;
                        buff.append(ival);
                    }
                    if (hasTwoByteDatum(idx)) {
                        buff.append(' ');
                        twoBytes[0] = data[++i];
                        twoBytes[1] = data[++i];
                        buff.append(indexFromBytes(twoBytes));
                    }
                    if (hasOneByteDatum(idx)) {
                        buff.append(" ");
                        twoBytes[0] = 0;
                        twoBytes[1] = data[++i];
                        buff.append(indexFromBytes(twoBytes));
                    }
                    buff.append("\n");
                } else {
                    buff.append("###FAILURE##\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * The lookupswith opcode has a variable length of data. This method
	 * will deal with that accordingly.
	 *
	 * @return The index of the last data element read. It is up to the user of this method
	 * to increment accordingly.
	 */
    protected int handleLookupSwitch(int opCode, byte[] data, int idx, StringBuffer buff) {
        while ((++idx % 4) != 0) {
        }
        buff.append('\n');
        int defaultByte = getInt(data, idx);
        idx += 4;
        int lowByte = getInt(data, idx);
        idx += 4;
        int count = lowByte;
        if (opCode == tableSwitchOpCode) {
            int highByte = getInt(data, idx);
            idx += 4;
            count = highByte - lowByte + 1;
        }
        int value;
        for (int i = 0; i < count; ++i) {
            value = getInt(data, idx);
            buff.append("key " + value);
            idx += 4;
            value = getInt(data, idx);
            buff.append(" value " + value);
            buff.append('\n');
            idx += 4;
        }
        return idx - 1;
    }

    /**
    *  This method will print a hex dump of the given byte array to the given
    *  output stream.  Each line of the output will be 2-digit hex numbers,
    *  separated by single spaces, followed by the characters corresponding to
    *  those hex numbers, or a '.' if the given character is unprintable.  Each of
    *  these numbers will correspond to a byte of the byte array.
    *
    *  @author Steve Sinclair
    *  @param bytes the byte array to write
    *  @param writer the destination for the output.
    *  @exception java.io.IOException thrown if there's an error writing strings to the writer.
    */
    public static void hexDump(final byte[] bytes, int read, final java.io.Writer writer) throws java.io.IOException {
        final int width = 16;
        for (int i = 0; i < read; i += width) {
            int limit = (i + width > read) ? read - i : width;
            int j;
            StringBuffer literals = new StringBuffer(width);
            StringBuffer hex = new StringBuffer(width * 3);
            for (j = 0; j < limit; ++j) {
                int aByte = bytes[i + j];
                if (aByte < 0) aByte = 0xff + aByte + 1;
                if (aByte < 0x10) hex.append('0');
                hex.append(Integer.toHexString(aByte));
                hex.append(' ');
                if (aByte >= 32 && aByte < 128) literals.append((char) aByte); else literals.append('.');
            }
            for (; j < width; ++j) {
                literals.append(" ");
                hex.append("-- ");
            }
            hex.append(' ');
            hex.append(literals);
            hex.append('\n');
            writer.write(hex.toString());
        }
    }

    boolean localVarAccess(int idx) {
        for (int i = 0; i < localLookup.length; ++i) {
            if (localLookup[i] == idx) return true;
        }
        return false;
    }

    boolean shouldGetConstantFromOne(int idx) {
        for (int i = 0; i < constLookupOne.length; ++i) {
            if (constLookupOne[i] == idx) return true;
        }
        return false;
    }

    boolean shouldGetConstant(int idx) {
        for (int i = 0; i < constLookup.length; ++i) {
            if (constLookup[i] == idx) return true;
        }
        return false;
    }

    boolean hasFourByteDatum(final int idx) {
        for (int i = 0; i < fourByteDatum.length; ++i) {
            if (fourByteDatum[i] == idx) return true;
        }
        return false;
    }

    boolean hasTwoByteDatum(final int idx) {
        for (int i = 0; i < twoByteDatum.length; ++i) {
            if (twoByteDatum[i] == idx) return true;
        }
        return false;
    }

    boolean hasOneByteDatum(final int idx) {
        for (int i = 0; i < oneByteDatum.length; ++i) {
            if (oneByteDatum[i] == idx) return true;
        }
        return false;
    }

    int[] localLookup = { 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78 };

    int[] constLookupOne = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 18 };

    int[] constLookup = { 19, 20, 178, 179, 180, 181, 182, 183, 184, 187, 189, 192, 193 };

    int[] oneByteDatum = { 16, 188 };

    int[] twoByteDatum = { 17, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 198, 199 };

    int[] fourByteDatum = { 200 };

    String[] opCodeTable = { "nop ", "aconst_null ", "iconst_m1 ", "iconst_0 ", "iconst_1 ", "iconst_2 ", "iconst_3 ", "iconst_4 ", "iconst_5 ", "lconst_0 ", "lconst_1 ", "fconst_0 ", "fconst_1 ", "fconst_2 ", "dconst_0 ", "dconst_1 ", "bipush ", "sipush ", "ldc ", "ldc_w ", "ldc2_w ", "iload ", "lload ", "fload ", "dload ", "aload ", "iload_0 ", "iload_1 ", "iload_2 ", "iload_3 ", "lload_0 ", "lload_1 ", "lload_2 ", "lload_3 ", "fload_0 ", "fload_1 ", "fload_2 ", "fload_3 ", "dload_0 ", "dload_1 ", "dload_2 ", "dload_3 ", "aload_0 ", "aload_1 ", "aload_2 ", "aload_3 ", "iaload ", "laload ", "faload ", "daload ", "aaload ", "baload ", "caload ", "saload ", "istore ", "lstore ", "fstore ", "dstore ", "astore ", "istore_0 ", "istore_1 ", "istore_2 ", "istore_3 ", "lstore_0 ", "lstore_1 ", "lstore_2 ", "lstore_3 ", "fstore_0 ", "fstore_1 ", "fstore_2 ", "fstore_3 ", "dstore_0 ", "dstore_1 ", "dstore_2 ", "dstore_3 ", "astore_0 ", "astore_1 ", "astore_2 ", "astore_3 ", "iastore ", "lastore ", "fastore ", "dastore ", "aastore ", "bastore ", "castore ", "sastore ", "pop ", "pop2 ", "dup ", "dup_x1 ", "dup_x2 ", "dup2 ", "dup2_x1 ", "dup2_x2 ", "swap ", "iadd ", "ladd ", "fadd ", "dadd ", "isub ", "lsub ", "fsub ", "dsub ", "imul ", "lmul ", "fmul ", "dmul ", "idiv ", "ldiv ", "fdiv ", "ddiv ", "irem ", "lrem ", "frem ", "drem ", "ineg ", "lneg ", "fneg ", "dneg ", "ishl ", "lshl ", "ishr ", "lshr ", "iushr ", "lushr ", "iand ", "land ", "ior ", "lor ", "ixor ", "lxor ", "iinc ", "i2l ", "i2f ", "i2d ", "l2i ", "l2f ", "l2d ", "f2i ", "f2l ", "f2d ", "d2i ", "d2l ", "d2f ", "i2b ", "i2c ", "i2s ", "lcmp ", "fcmpl ", "fcmpg ", "dcmpl ", "dcmpg ", "ifeq ", "ifne ", "iflt ", "ifge ", "ifgt ", "ifle ", "if_icmpeq ", "if_icmpne ", "if_icmplt ", "if_icmpge ", "if_icmpgt ", "if_icmple ", "if_acmpeq ", "if_acmpne ", "goto ", "jsr ", "ret ", "tableswitch ", "lookupswitch ", "ireturn ", "lreturn ", "freturn ", "dreturn ", "areturn ", "return ", "getstatic ", "putstatic ", "getfield ", "putfield ", "invokevirtual ", "invokespecial ", "invokestatic ", "invokeinterface ", "xxxunusedxxx ", "new ", "newarray ", "anewarray ", "arraylength ", "athrow ", "checkcast ", "instanceof ", "monitorenter ", "monitorexit ", "wide ", "multianewarray ", "ifnull ", "ifnonnull ", "goto_w ", "jsr_w ", "breakpoint ", "ldc_quick ", "ldc_w_quick ", "ldc2_w_quick ", "getfield_quick ", "putfield_quick ", "getfield2_quick ", "putfield2_quick ", "getstatic_quick ", "putstatic_quick ", "getstatic2_quick ", "putstatic2_quick ", "invokevirtual_quick ", "invokenonvirtual_quick ", "invokesuper_quick ", "invokestatic_quick ", "invokeinterface_quick ", "invokevirtualobject_quick ", "new_quick ", "anewarray_quick ", "multianewarray_quick ", "checkcast_quick ", "instanceof_quick ", "invokevirtual_quick_w ", "getfield_quick_w ", "putfield_quick_w ", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "impdep1 ", "impdep2 " };
}
