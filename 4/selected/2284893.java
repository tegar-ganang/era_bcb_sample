package org.zamia.instgraph.sim.ref;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.nio.channels.OverlappingFileLockException;
import org.zamia.ErrorReport;
import org.zamia.SourceLocation;
import org.zamia.ZamiaException;
import org.zamia.instgraph.IGContainer;
import org.zamia.instgraph.IGObject;
import org.zamia.instgraph.IGOperationLiteral;
import org.zamia.instgraph.IGRange;
import org.zamia.instgraph.IGStaticValue;
import org.zamia.instgraph.IGStaticValueBuilder;
import org.zamia.instgraph.IGSubProgram;
import org.zamia.instgraph.IGType;
import org.zamia.instgraph.IGTypeStatic;
import org.zamia.instgraph.interpreter.IGInterpreterRuntimeEnv;
import org.zamia.instgraph.interpreter.IGObjectDriver;
import org.zamia.vhdl.ast.VHDLNode;
import org.zamia.zdb.ZDB;

/**
 * @author Anton Chepurov
 */
public class IGFileDriver extends IGObjectDriver {

    private IGStaticValue fLineNr;

    public IGFileDriver(String aId, IGObject.OIDir aDir, IGObject.IGObjectCat aCat, IGObjectDriver aParent, IGTypeStatic aType, SourceLocation aLocation) throws ZamiaException {
        super(aId, aDir, aCat, aParent, aType, aLocation);
    }

    public IGStaticValue readLine(IGSubProgram aSub, IGInterpreterRuntimeEnv aRuntime, SourceLocation aLocation, VHDLNode.ASTErrorMode aErrorMode, ErrorReport aReport) throws ZamiaException {
        switch(getDir()) {
            case NONE:
                throw new ZamiaException("Attempt to access a closed file.");
            case OUT:
            case APPEND:
                String fileName = getFileName(aLocation);
                throw new ZamiaException("Attempt to read from file \"" + fileName + "\" which is opened only for writing or appending.");
        }
        File file = getFile(aLocation);
        String line = readNextLine(file, aSub, aLocation);
        if (line == null) {
            throw new ZamiaException("TEXTIO : Read past end of file \"" + file.getName() + "\".", aLocation);
        }
        IGContainer container = aSub.getContainer();
        IGTypeStatic intType = container.findIntType().computeStaticType(aRuntime, aErrorMode, aReport);
        BigInteger lineNr = fLineNr == null ? BigInteger.ONE : fLineNr.getNum().add(BigInteger.ONE);
        fLineNr = new IGStaticValueBuilder(intType, null, aLocation).setNum(lineNr).buildConstant();
        IGType stringType = container.findStringType();
        return line2IG(line, stringType, aRuntime, aLocation, aErrorMode, aReport, aSub.getZDB());
    }

    public static IGStaticValue line2IG(String aLine, IGType aStringType, IGInterpreterRuntimeEnv aRuntime, SourceLocation aLocation, VHDLNode.ASTErrorMode aErrorMode, ErrorReport aReport, ZDB aZDB) throws ZamiaException {
        IGTypeStatic idxType = aStringType.getIndexType().computeStaticType(aRuntime, aErrorMode, aReport);
        IGStaticValue left = idxType.getStaticLeft(aLocation);
        IGStaticValue right = new IGStaticValueBuilder(left, aLocation).setNum(new BigInteger("" + aLine.length())).buildConstant();
        IGStaticValue ascending = idxType.getStaticAscending();
        IGRange range = new IGRange(left, right, ascending, aLocation, aZDB);
        aStringType = aStringType.createSubtype(range, aRuntime, aLocation);
        IGOperationLiteral lineOL = new IGOperationLiteral(aLine.toUpperCase(), aStringType, aLocation);
        return lineOL.computeStaticValue(aRuntime, aErrorMode, aReport);
    }

    public boolean isEOF(IGSubProgram aSub, SourceLocation aLocation) throws ZamiaException {
        switch(getDir()) {
            case NONE:
                throw new ZamiaException("Attempt to access a closed file.");
            case OUT:
            case APPEND:
                return true;
        }
        File file = getFile(aLocation);
        String line = readNextLine(file, aSub, aLocation);
        return line == null;
    }

    public void writeLine(IGStaticValue aValue, SourceLocation aLocation) throws ZamiaException {
        switch(getDir()) {
            case NONE:
                throw new ZamiaException("Attempt to access a closed file.");
            case IN:
                String fileName = getFileName(aLocation);
                throw new ZamiaException("Attempt to write to or flush file \"" + fileName + "\" which is opened only for reading.");
        }
        File file = getFile(aLocation);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file, true));
            writer.append(aValue.toString());
            writer.newLine();
        } catch (FileNotFoundException e) {
            throw new ZamiaException("File to write to is not found: " + file.getAbsolutePath(), aLocation);
        } catch (IOException e) {
            throw new ZamiaException("Error while writing to file " + file.getAbsolutePath() + ":\n" + e.getMessage(), aLocation);
        } finally {
            close(writer);
        }
    }

    public void close() {
        setDir(IGObject.OIDir.NONE);
        fLineNr = null;
    }

    public IGStaticValue openFile(IGStaticValue aFileName, IGObject.OIDir aDir, IGType aFileOpenStatusType, SourceLocation aLocation) throws ZamiaException {
        if (getDir() != IGObject.OIDir.NONE) {
            return aFileOpenStatusType.findEnumLiteral("STATUS_ERROR");
        }
        File file = createFile(aFileName);
        FileReader reader = null;
        FileWriter writer = null;
        FileOutputStream fos = null;
        switch(aDir) {
            case IN:
                if (!file.exists()) {
                    return aFileOpenStatusType.findEnumLiteral("NAME_ERROR");
                }
                try {
                    reader = new FileReader(file);
                } catch (FileNotFoundException e) {
                    return aFileOpenStatusType.findEnumLiteral("MODE_ERROR");
                } finally {
                    close(reader);
                }
                break;
            case OUT:
            case APPEND:
                try {
                    writer = new FileWriter(file);
                    fos = new FileOutputStream(file);
                    fos.getChannel().lock().release();
                } catch (OverlappingFileLockException e) {
                    return aFileOpenStatusType.findEnumLiteral("NAME_ERROR");
                } catch (IOException e) {
                    return aFileOpenStatusType.findEnumLiteral("NAME_ERROR");
                } finally {
                    close(writer);
                    close(fos);
                }
                if (aDir == IGObject.OIDir.APPEND) {
                    try {
                        writer = new FileWriter(file, true);
                    } catch (IOException e) {
                        return aFileOpenStatusType.findEnumLiteral("MODE_ERROR");
                    } finally {
                        close(writer);
                    }
                }
                break;
        }
        setDir(aDir);
        setValue(aFileName, aLocation);
        return aFileOpenStatusType.findEnumLiteral("OPEN_OK");
    }

    private String readNextLine(File aFile, IGSubProgram aSub, SourceLocation aLocation) throws ZamiaException {
        String line;
        LineNumberReader reader = null;
        try {
            reader = createReader(aFile);
            line = reader.readLine();
        } catch (FileNotFoundException e) {
            throw new ZamiaException("File " + aFile.getAbsolutePath() + " not found while executing " + aSub, aLocation);
        } catch (IOException e) {
            throw new ZamiaException("Error while reading file " + aFile.getAbsolutePath() + ":\n" + e.getMessage(), aLocation);
        } finally {
            close(reader);
        }
        return line;
    }

    private File getFile(SourceLocation aLocation) throws ZamiaException {
        IGStaticValue fileName = getValue(aLocation);
        return createFile(fileName);
    }

    private String getFileName(SourceLocation aLocation) throws ZamiaException {
        IGStaticValue v = getValue(aLocation);
        return v.getId();
    }

    private File createFile(IGStaticValue aFileName) {
        File parent = aFileName.computeSourceLocation().getDir();
        String name = aFileName.getId();
        if (name == null) {
            name = aFileName.toString();
            aFileName.setId(name);
        }
        File res = new File(aFileName.getId());
        if (res.isAbsolute()) return res;
        return new File(parent, name);
    }

    private LineNumberReader createReader(File aFile) throws IOException {
        LineNumberReader reader = new LineNumberReader(new FileReader(aFile));
        int lineToRead = fLineNr == null ? 0 : fLineNr.getInt();
        for (int i = 0; i < lineToRead; i++) {
            reader.readLine();
        }
        return reader;
    }

    private static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
        }
    }
}
