package com.amd.javalabs.tools.cajvmti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;
import com.amd.javalabs.tools.commons.RuntimeContext;
import com.amd.javalabs.tools.commons.memory.Block;
import com.amd.javalabs.tools.commons.memory.Location;

public class FileParser implements Iterable<Record> {

    private FileInputStream fileInputStream;

    private FileChannel fileChannel;

    private MappedByteBuffer mappedByteBuffer;

    private File file;

    public FileParser(File _file) {
        file = _file;
    }

    private enum HeaderToken {

        START, COMPILED_METHOD_LOAD, COMPILED_METHOD_UNLOAD, NATIVE_METHOD_BIND, DYNAMIC_CODE_GENERATED, END
    }

    ;

    private HeaderToken getHeaderToken() {
        HeaderToken headerToken = null;
        byte[] headerByte = new byte[1];
        mappedByteBuffer.get(headerByte);
        if (headerByte[0] >= 0 && headerByte[0] < HeaderToken.values().length) {
            headerToken = HeaderToken.values()[headerByte[0]];
        }
        return (headerToken);
    }

    private int getTimeStamp() {
        int timeStamp = mappedByteBuffer.getInt();
        return (timeStamp);
    }

    private byte[] getBytes() {
        int len = mappedByteBuffer.getInt();
        byte[] bytes = new byte[len];
        mappedByteBuffer.get(bytes);
        return (bytes);
    }

    private String getString() {
        int len = mappedByteBuffer.getShort();
        byte[] bytes = new byte[len];
        mappedByteBuffer.get(bytes);
        String string = new String(bytes);
        return (string);
    }

    private String getMethodName() {
        return (getString());
    }

    public void open() {
        try {
            fileInputStream = new FileInputStream(file);
            fileChannel = fileInputStream.getChannel();
            mappedByteBuffer = fileChannel.map(MapMode.READ_ONLY, 0, file.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            fileInputStream.close();
            fileChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getClassModifiers() {
        int classModifiers = mappedByteBuffer.getInt();
        return (classModifiers);
    }

    private int getMethodModifiers() {
        int methodModifiers = mappedByteBuffer.getInt();
        return (methodModifiers);
    }

    private long getMethodID() {
        long methodID = mappedByteBuffer.getLong();
        return (methodID);
    }

    private long getClassLoaderID() {
        long classLoaderID = mappedByteBuffer.getLong();
        return (classLoaderID);
    }

    private long getClassID() {
        long classID = mappedByteBuffer.getLong();
        return (classID);
    }

    private String getModulePath() {
        String modulePath = getString();
        return (modulePath);
    }

    private String getSourceFileName() {
        String sourceFileName = getString();
        return (sourceFileName);
    }

    private String getMethodSignature() {
        String methodSignature = getString();
        return (methodSignature);
    }

    private String getClassSignature() {
        String classSignature = getString();
        return (classSignature);
    }

    private Location getLocation() {
        Location location = new Location(mappedByteBuffer.getLong());
        return (location);
    }

    public List<ClassLineNumberTableEntry> getClassLineNumberTable() {
        List<ClassLineNumberTableEntry> classLineNumberTable = new ArrayList<ClassLineNumberTableEntry>();
        int len = mappedByteBuffer.getInt();
        for (int i = 0; i < len; i++) {
            long location = mappedByteBuffer.getLong();
            int lineNumber = mappedByteBuffer.getInt();
            classLineNumberTable.add(new ClassLineNumberTableEntry(i, new ByteCodeIndex((int) location), lineNumber));
        }
        return (classLineNumberTable);
    }

    public List<AddrLocationTableEntry> getAddrLocationTable() {
        List<AddrLocationTableEntry> addrLocationTable = new ArrayList<AddrLocationTableEntry>();
        int len = mappedByteBuffer.getInt();
        for (int i = 0; i < len; i++) {
            long bci = mappedByteBuffer.getLong();
            long address = mappedByteBuffer.getLong();
            addrLocationTable.add(new AddrLocationTableEntry(i, new ByteCodeIndex((int) bci), new Location(address)));
        }
        return (addrLocationTable);
    }

    private Record getRecord() {
        Record record = null;
        try {
            HeaderToken token = getHeaderToken();
            if (token != null) {
                switch(token) {
                    case START:
                        {
                            record = new StartRecord(getTimeStamp());
                            break;
                        }
                    case COMPILED_METHOD_LOAD:
                        {
                            int timeStamp = getTimeStamp();
                            long methodID = getMethodID();
                            String methodName = getMethodName();
                            String methodSignature = getMethodSignature();
                            int methodModifiers = getMethodModifiers();
                            long classID = getClassID();
                            String classSignature = getClassSignature();
                            int classModifiers = getClassModifiers();
                            long classLoaderID = getClassLoaderID();
                            String sourceFileName = getSourceFileName();
                            Location location = getLocation();
                            List<ClassLineNumberTableEntry> classLineNumberTable = getClassLineNumberTable();
                            List<AddrLocationTableEntry> addrLocationTable = getAddrLocationTable();
                            Block block = new Block(location, getBytes());
                            record = new CompiledMethodLoadRecord(timeStamp, methodID, methodName, methodSignature, methodModifiers, classID, classSignature, classModifiers, classLoaderID, sourceFileName, block, classLineNumberTable, addrLocationTable);
                            break;
                        }
                    case COMPILED_METHOD_UNLOAD:
                        {
                            int timeStamp = getTimeStamp();
                            long methodID = getMethodID();
                            String methodName = getMethodName();
                            String methodSignature = getMethodSignature();
                            int methodModifiers = getMethodModifiers();
                            long classID = getClassID();
                            String classSignature = getClassSignature();
                            int classModifiers = getClassModifiers();
                            long classLoaderID = getClassLoaderID();
                            String sourceFileName = getSourceFileName();
                            Location location = getLocation();
                            record = new CompiledMethodUnloadRecord(timeStamp, methodID, methodName, methodSignature, methodModifiers, classID, classSignature, classModifiers, classLoaderID, sourceFileName, location);
                            break;
                        }
                    case NATIVE_METHOD_BIND:
                        {
                            int timeStamp = getTimeStamp();
                            long methodID = getMethodID();
                            String methodName = getMethodName();
                            String methodSignature = getMethodSignature();
                            int methodModifiers = getMethodModifiers();
                            long classID = getClassID();
                            String classSignature = getClassSignature();
                            int classModifiers = getClassModifiers();
                            long classLoaderID = getClassLoaderID();
                            String sourceFileName = getSourceFileName();
                            Location location = getLocation();
                            String modulePath = getModulePath();
                            record = new NativeMethodBindRecord(timeStamp, methodID, methodName, methodSignature, methodModifiers, classID, classSignature, classModifiers, classLoaderID, sourceFileName, location, modulePath);
                            break;
                        }
                    case DYNAMIC_CODE_GENERATED:
                        {
                            int timeStamp = getTimeStamp();
                            String methodName = getMethodName();
                            Location location = getLocation();
                            Block block = new Block(location, getBytes());
                            record = new DynamicCodeGeneratedRecord(timeStamp, methodName, block);
                            break;
                        }
                    case END:
                        {
                            record = new EndRecord(getTimeStamp());
                            break;
                        }
                    default:
                        RuntimeContext.getInstance().message("error parsing file : Unexpected token");
                        record = null;
                        break;
                }
            } else {
                RuntimeContext.getInstance().message("error parsing file : token = NULL");
            }
        } catch (Throwable t) {
            RuntimeContext.getInstance().caught(t);
            RuntimeContext.getInstance().message("Premature end of CommonJitFormatFile");
            record = new EndRecord(0);
        }
        return (record);
    }

    private static class Iterator implements java.util.Iterator<Record> {

        private Record next;

        private FileParser test;

        public Iterator(FileParser _test) {
            test = _test;
            next = test.getRecord();
        }

        public boolean hasNext() {
            return (next != null);
        }

        public Record next() {
            Record returnMe = next;
            if (next instanceof EndRecord) {
                next = null;
            } else {
                next = test.getRecord();
            }
            return (returnMe);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator iterator() {
        return new Iterator(this);
    }

    public void visitCompiledMethodLoadRecords(CompiledMethodLoadRecordVisitor _compiledMethodLoadRecordVisitor) {
        visitRecords(_compiledMethodLoadRecordVisitor);
    }

    public static void visitCompiledMethodLoadRecords(File _file, CompiledMethodLoadRecordVisitor _compiledMethodLoadRecordVisitor) {
        FileParser fileParser = new FileParser(_file);
        fileParser.visitRecords(_compiledMethodLoadRecordVisitor);
    }

    public void visitDynamicCodeGeneratedRecords(DynamicCodeGeneratedRecordVisitor _dynamicCodeGeneratedRecordVisitor) {
        visitRecords(_dynamicCodeGeneratedRecordVisitor);
    }

    public static void visitDynamicCodeGeneratedRecords(File _file, DynamicCodeGeneratedRecordVisitor _dynamicCodeGeneratedRecordVisitor) {
        FileParser fileParser = new FileParser(_file);
        fileParser.visitRecords(_dynamicCodeGeneratedRecordVisitor);
    }

    public void visitNativeMethodBindRecords(NativeMethodBindRecordVisitor _nativeMethodBindRecordVisitor) {
        visitRecords(_nativeMethodBindRecordVisitor);
    }

    public static void visitNativeMethodBindRecords(File _file, NativeMethodBindRecordVisitor _nativeMethodBindRecordVisitor) {
        FileParser fileParser = new FileParser(_file);
        fileParser.visitRecords(_nativeMethodBindRecordVisitor);
    }

    public void visitRecords(RecordVisitor _recordVisitor) {
        open();
        for (Record record : this) {
            if (record instanceof DynamicCodeGeneratedRecord && _recordVisitor instanceof DynamicCodeGeneratedRecordVisitor) {
                ((DynamicCodeGeneratedRecordVisitor) _recordVisitor).visit((DynamicCodeGeneratedRecord) record);
            } else if (record instanceof CompiledMethodLoadRecord && _recordVisitor instanceof CompiledMethodLoadRecordVisitor) {
                ((CompiledMethodLoadRecordVisitor) _recordVisitor).visit((CompiledMethodLoadRecord) record);
            } else if (record instanceof NativeMethodBindRecord && _recordVisitor instanceof NativeMethodBindRecordVisitor) {
                ((NativeMethodBindRecordVisitor) _recordVisitor).visit((NativeMethodBindRecord) record);
            }
        }
        close();
    }

    public static void main(String[] _args) {
        File file = new File(_args[0]);
        FileParser test = new FileParser(file);
        test.open();
        for (Record record : test) {
            System.out.println(record);
        }
        test.close();
    }
}
