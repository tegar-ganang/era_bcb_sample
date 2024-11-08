package nacaLib.varEx;

import jlib.log.Log;
import jlib.misc.AdvancedFileDescriptorMode;
import jlib.misc.BaseDataFile;
import jlib.misc.BaseDataFileBuffered;
import jlib.misc.DataFileLineReader;
import jlib.misc.EnvironmentVar;
import jlib.misc.LineRead;
import jlib.misc.LittleEndingSignBinaryBufferStorage;
import jlib.misc.LogicalFileDescriptor;
import jlib.misc.RecordLengthDefinition;
import jlib.misc.StringUtil;
import nacaLib.basePrgEnv.BaseEnvironment;
import nacaLib.basePrgEnv.BaseResourceManager;
import nacaLib.basePrgEnv.BaseSession;
import nacaLib.batchOOApi.WriteBufferExt;

public class FileDescriptor extends BaseFileDescriptor {

    private VarDefEncodingConvertibleManagerContainer m_varDefEncodingConvertibleManagerContainer = null;

    private byte[] m_tbyHeader = null;

    public FileDescriptor(String csLogicalName) {
        super(null, csLogicalName);
    }

    public FileDescriptor(String csLogicalName, BaseSession session) {
        super(null, csLogicalName);
        setSession(session);
    }

    public FileDescriptor(BaseEnvironment env, String csLogicalName) {
        super(env, csLogicalName);
    }

    public static boolean isExistingFileDescriptor(String csLogicalName, BaseSession baseSession) {
        if (baseSession != null && csLogicalName != null) {
            LogicalFileDescriptor logicalFileDescriptor = baseSession.getLogicalFileDescriptor(csLogicalName);
            if (logicalFileDescriptor != null) {
                return true;
            } else {
                String csPhysicalDesc = EnvironmentVar.getParamValue(csLogicalName);
                if (StringUtil.isEmpty(csPhysicalDesc)) csPhysicalDesc = EnvironmentVar.getParamValue("File_" + csLogicalName);
                if (csPhysicalDesc != null && !StringUtil.isEmpty(csPhysicalDesc)) return true;
            }
        }
        return false;
    }

    public void inheritSettings(FileDescriptor fileDescSource) {
        m_fileManagerEntry.inheritSettings(fileDescSource.m_fileManagerEntry);
    }

    public boolean isEbcdic() {
        return m_fileManagerEntry.isEbcdic();
    }

    public boolean isVariableLength() {
        return m_fileManagerEntry.isVariableLength();
    }

    public boolean isVariableLength4BytesHeaderWithLF() {
        return m_fileManagerEntry.isVariableLength4BytesHeaderWithLF();
    }

    public RecordLengthDefinition getRecordLengthDefinition() {
        return m_fileManagerEntry.getRecordLengthDefinition();
    }

    public FileDescriptor lengthDependingOn(Var varLengthDependingOn) {
        setVarLengthDependingOn(varLengthDependingOn);
        return this;
    }

    public void writeLinePositionning(int n) {
        byte tbCodes[] = BaseResourceManager.getPrinterCodeNewLine();
        while (n > 0) {
            m_fileManagerEntry.m_dataFile.write(tbCodes, 0, tbCodes.length);
            n--;
        }
    }

    public void writePagePositionning() {
        byte tbCodes[] = BaseResourceManager.getPrinterCodeNewPage();
        m_fileManagerEntry.m_dataFile.write(tbCodes, 0, tbCodes.length);
    }

    public void write() {
        writeFrom(m_varLevel01, false);
    }

    public void writeFrom(VarBase varWorking) {
        writeFrom(varWorking, false);
    }

    public void rewrite() {
        writeFrom(m_varLevel01, true);
    }

    public void rewriteFrom(VarBase varWorking) {
        writeFrom(varWorking, true);
    }

    private void writeFrom(VarBase varFrom, boolean bRewriteMode) {
        if (isLogFile) {
            if (bRewriteMode) Log.logDebug("writeFrom (rewrite mode) File=" + getLogicalName() + " varFrom=" + varFrom.getSTCheckValue()); else Log.logDebug("writeFrom File=" + getLogicalName() + " varFrom=" + varFrom.getSTCheckValue());
        }
        if (m_fileManagerEntry.isDummyFile()) return;
        VarBase varLevel01 = m_varLevel01;
        if (varLevel01 == null) varLevel01 = varFrom;
        int nRecordSize = varLevel01.getTotalSize();
        int nVarFromSize = nRecordSize;
        int nMinSize = nRecordSize;
        int nMaxSize = nRecordSize;
        if (varLevel01 != varFrom) {
            nVarFromSize = varFrom.getTotalSize();
            if (nRecordSize <= nVarFromSize) {
                nMinSize = nRecordSize;
                nMaxSize = nVarFromSize;
            } else {
                varLevel01.fill(CobolConstant.LowValue);
                nMinSize = nVarFromSize;
                nMaxSize = nRecordSize;
            }
        }
        byte tbyFilebuffer[] = m_fileManagerEntry.m_dataFile.getByteBuffer(nMaxSize);
        varFrom.exportToByteArray(tbyFilebuffer, nVarFromSize);
        if (varLevel01 != varFrom) varLevel01.setFromByteArray(tbyFilebuffer, 0, nMinSize);
        if (m_fileManagerEntry.isEbcdic()) {
            if (m_varDefEncodingConvertibleManagerContainer == null) m_varDefEncodingConvertibleManagerContainer = new VarDefEncodingConvertibleManagerContainer();
            m_varDefEncodingConvertibleManagerContainer.getConvertedBytesAsciiToEbcdic(varLevel01, tbyFilebuffer, nMaxSize);
        } else {
            varLevel01.exportToByteArray(tbyFilebuffer, nRecordSize);
        }
        if (m_fileManagerEntry.isStandardMode()) {
            if (m_fileManagerEntry.isVariableLength()) {
                int nRecordLength = getRecordLength(varLevel01);
                if (m_tbyHeader == null) m_tbyHeader = new byte[4];
                LittleEndingSignBinaryBufferStorage.writeInt(m_tbyHeader, nRecordLength, 0);
                if (bRewriteMode) m_fileManagerEntry.m_dataFile.rewrite(m_tbyHeader, 0, 4); else m_fileManagerEntry.m_dataFile.write(m_tbyHeader, 0, 4);
                m_fileManagerEntry.m_dataFile.writeWithEOL(tbyFilebuffer, nRecordLength);
                incNbRecordWrite();
            } else {
                if (bRewriteMode) m_fileManagerEntry.m_dataFile.rewriteWithEOL(tbyFilebuffer, nRecordSize); else m_fileManagerEntry.m_dataFile.writeWithEOL(tbyFilebuffer, nRecordSize);
                incNbRecordWrite();
            }
        } else {
            AdvancedFileDescriptorMode advancedFileDescriptorMode = m_fileManagerEntry.getAdvancedFileDescriptorMode();
            if (advancedFileDescriptorMode.isVariable()) {
                int nRecordLength = getRecordLength(varLevel01);
                if (advancedFileDescriptorMode.isUsingRecordLengthHeader()) {
                    if (m_tbyHeader == null) m_tbyHeader = new byte[4];
                    LittleEndingSignBinaryBufferStorage.writeInt(m_tbyHeader, nRecordLength, 0);
                    if (bRewriteMode) m_fileManagerEntry.m_dataFile.rewrite(m_tbyHeader, 0, 4); else m_fileManagerEntry.m_dataFile.write(m_tbyHeader, 0, 4);
                } else {
                    if (advancedFileDescriptorMode.getMFCobolLineSequential()) m_fileManagerEntry.m_dataFile.writeWithOptionalEOLMFCobol(tbyFilebuffer, nRecordLength, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF()); else m_fileManagerEntry.m_dataFile.writeWithOptionalEOL(tbyFilebuffer, nRecordLength, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                }
                incNbRecordWrite();
            } else {
                if (advancedFileDescriptorMode.getMFCobolLineSequential()) {
                    m_fileManagerEntry.m_dataFile.writeWithOptionalEOLMFCobol(tbyFilebuffer, nRecordSize, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                    incNbRecordWrite();
                } else {
                    if (advancedFileDescriptorMode.isUsingRecordLengthHeader()) {
                        int nRecordLength = getRecordLength(varLevel01);
                        if (m_tbyHeader == null) m_tbyHeader = new byte[4];
                        LittleEndingSignBinaryBufferStorage.writeInt(m_tbyHeader, nRecordLength, 0);
                        if (bRewriteMode) m_fileManagerEntry.m_dataFile.rewrite(m_tbyHeader, 0, 4); else m_fileManagerEntry.m_dataFile.write(m_tbyHeader, 0, 4);
                    }
                    if (bRewriteMode) m_fileManagerEntry.m_dataFile.rewriteWithOptionalEOL(tbyFilebuffer, nRecordSize, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF()); else m_fileManagerEntry.m_dataFile.writeWithOptionalEOL(tbyFilebuffer, nRecordSize, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                    incNbRecordWrite();
                }
            }
        }
    }

    public byte[] getWriteBuffer(int nMaxSize) {
        byte tbyFilebuffer[] = m_fileManagerEntry.m_dataFile.getByteBuffer(nMaxSize);
        return tbyFilebuffer;
    }

    public void writeFrom(LineRead lineRead) {
        m_fileManagerEntry.m_dataFile.writeWithEOL(lineRead);
    }

    public RecordDescriptorAtEnd read() {
        return readInto(m_varLevel01);
    }

    private void convertEbcdicToAsciiAndWrite(LineRead lineRead, Var varDest) {
        if (m_varDefEncodingConvertibleManagerContainer == null) m_varDefEncodingConvertibleManagerContainer = new VarDefEncodingConvertibleManagerContainer();
        if (m_varLevel01 != varDest) m_varDefEncodingConvertibleManagerContainer.getEncodingManagerConvertAndWrite(lineRead, varDest); else m_varDefEncodingConvertibleManagerContainer.getEncodingManagerConvertAndWrite(lineRead, m_varLevel01);
    }

    private void convertEbcdicToAsciiAndWrite(LineRead lineRead) {
        if (m_varDefEncodingConvertibleManagerContainer == null) m_varDefEncodingConvertibleManagerContainer = new VarDefEncodingConvertibleManagerContainer();
        m_varDefEncodingConvertibleManagerContainer.getEncodingManagerConvertAndWrite(lineRead, m_varLevel01);
    }

    public RecordDescriptorAtEnd readInto(Var varDest) {
        RecordDescriptorAtEnd e = doReadInto(varDest);
        if (isLogFile) Log.logDebug("readInto File=" + getLogicalName() + " varDest=" + varDest.getSTCheckValue());
        if (e == RecordDescriptorAtEnd.End) {
            reportStatus(FileStatusEnum.END_OF_FILE);
            if (isLogFile) Log.logDebug("readInto File=" + getLogicalName() + " End of file found");
        } else reportStatus(FileStatusEnum.OK);
        return e;
    }

    private RecordDescriptorAtEnd doReadInto(Var varDest) {
        if (m_fileManagerEntry.isDummyFile()) return RecordDescriptorAtEnd.End;
        if (m_fileManagerEntry.isStandardMode()) {
            if (hasVarVariableLengthMarker()) {
                long lLastHeaderStartPosition = m_fileManagerEntry.m_dataFile.getFileCurrentPosition();
                LineRead header = m_fileManagerEntry.m_dataFile.readBuffer(4, false);
                if (header != null) {
                    int nLengthExcludingHeader = header.getAsLittleEndingUnsignBinaryInt();
                    LineRead lineRead = m_fileManagerEntry.m_dataFile.readBuffer(nLengthExcludingHeader, true);
                    m_fileManagerEntry.m_dataFile.setLastPosition(lLastHeaderStartPosition);
                    if (lineRead != null) {
                        fillInto(lineRead, varDest);
                        int nVariableRecordLength = getVariableRecordLength(nLengthExcludingHeader);
                        fillVarLengthDependingOn(nVariableRecordLength);
                        return RecordDescriptorAtEnd.NotEnd;
                    }
                }
                return RecordDescriptorAtEnd.End;
            } else {
                int nRecordLength = getRecordLength(m_varLevel01);
                LineRead lineRead;
                if (nRecordLength > 0) {
                    lineRead = m_fileManagerEntry.m_dataFile.readBuffer(nRecordLength, true);
                } else lineRead = m_fileManagerEntry.m_dataFile.readNextUnixLine();
                if (lineRead != null) {
                    fillInto(lineRead, varDest);
                    return RecordDescriptorAtEnd.NotEnd;
                }
                return RecordDescriptorAtEnd.End;
            }
        } else {
            AdvancedFileDescriptorMode advancedFileDescriptorMode = m_fileManagerEntry.getAdvancedFileDescriptorMode();
            if (hasVarVariableLengthMarker()) {
                if (advancedFileDescriptorMode.isUsingRecordLengthHeader()) {
                    long lLastHeaderStartPosition = m_fileManagerEntry.m_dataFile.getFileCurrentPosition();
                    LineRead header = m_fileManagerEntry.m_dataFile.readBuffer(4, false);
                    if (header != null) {
                        int nLengthExcludingHeader = header.getAsLittleEndingUnsignBinaryInt();
                        LineRead lineRead = m_fileManagerEntry.m_dataFile.readBufferOptionalEOL(nLengthExcludingHeader, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                        m_fileManagerEntry.m_dataFile.setLastPosition(lLastHeaderStartPosition);
                        if (lineRead != null) {
                            fillInto(lineRead, varDest);
                            int nVariableRecordLength = getVariableRecordLength(nLengthExcludingHeader);
                            fillVarLengthDependingOn(nVariableRecordLength);
                            return RecordDescriptorAtEnd.NotEnd;
                        }
                    }
                } else {
                    LineRead lineRead = null;
                    if (advancedFileDescriptorMode.isEndingCRLF()) lineRead = m_fileManagerEntry.m_dataFile.readNextLineCRLFTerminated(); else if (advancedFileDescriptorMode.isEndingLF()) lineRead = m_fileManagerEntry.m_dataFile.readNextUnixLine();
                    if (lineRead != null) {
                        fillInto(lineRead, varDest);
                        int nVariableRecordLength = lineRead.getTotalLength();
                        fillVarLengthDependingOn(nVariableRecordLength);
                        return RecordDescriptorAtEnd.NotEnd;
                    }
                }
                return RecordDescriptorAtEnd.End;
            } else {
                if (advancedFileDescriptorMode.isUsingRecordLengthHeader()) {
                    long lLastHeaderStartPosition = m_fileManagerEntry.m_dataFile.getFileCurrentPosition();
                    LineRead header = m_fileManagerEntry.m_dataFile.readBuffer(4, false);
                    if (header != null) {
                        int nLengthExcludingHeader = header.getAsLittleEndingUnsignBinaryInt();
                        LineRead lineRead = m_fileManagerEntry.m_dataFile.readBufferOptionalEOL(nLengthExcludingHeader, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                        m_fileManagerEntry.m_dataFile.setLastPosition(lLastHeaderStartPosition);
                        if (lineRead != null) {
                            fillInto(lineRead, varDest);
                            return RecordDescriptorAtEnd.NotEnd;
                        }
                    }
                } else if (advancedFileDescriptorMode.getMFCobolLineSequential()) {
                    LineRead lineRead = m_fileManagerEntry.m_dataFile.readNextUnixLineMFCobol();
                    if (lineRead != null) {
                        fillIntoMFCobol(lineRead, varDest);
                        return RecordDescriptorAtEnd.NotEnd;
                    }
                } else {
                    int nRecordLength = getRecordLength(m_varLevel01);
                    LineRead lineRead;
                    if (nRecordLength > 0) {
                        lineRead = m_fileManagerEntry.m_dataFile.readBufferOptionalEOL(nRecordLength, advancedFileDescriptorMode.isEndingCRLF(), advancedFileDescriptorMode.isEndingLF());
                    } else lineRead = m_fileManagerEntry.m_dataFile.readNextUnixLine();
                    if (lineRead != null) {
                        fillInto(lineRead, varDest);
                        return RecordDescriptorAtEnd.NotEnd;
                    }
                }
                return RecordDescriptorAtEnd.End;
            }
        }
    }

    private void fillIntoMFCobol(LineRead lineRead, Var varDest) {
        LineRead lineReadUnformatted = lineRead.getAsDeserializedFromMFCobol();
        incNbRecordRead();
        if (m_varLevel01 != varDest) {
            if (m_fileManagerEntry.isEbcdic()) fillInto2DestEbcdic(lineReadUnformatted, varDest); else varDest.setFromLineRead2DestWithFilling(lineReadUnformatted, m_varLevel01, CobolConstant.Space.getValue());
        } else {
            if (m_fileManagerEntry.isEbcdic()) {
                varDest.fill(CobolConstant.LowValue);
                convertEbcdicToAsciiAndWrite(lineReadUnformatted);
            } else {
                int nRecordSize = m_varLevel01.getTotalSize();
                int nNbByteWritten = m_varLevel01.setFromLineRead(lineReadUnformatted);
                if (nRecordSize > nNbByteWritten) varDest.fillEndOfRecord(nNbByteWritten, nRecordSize, CobolConstant.Space.getValue());
            }
        }
    }

    private void fillInto(LineRead lineRead, Var varDest) {
        incNbRecordRead();
        if (m_varLevel01 != varDest) {
            if (m_fileManagerEntry.isEbcdic()) fillInto2DestEbcdic(lineRead, varDest); else varDest.setFromLineRead2DestWithFilling(lineRead, m_varLevel01, CobolConstant.LowValue.getValue());
        } else {
            if (m_fileManagerEntry.isEbcdic()) {
                varDest.fill(CobolConstant.LowValue);
                convertEbcdicToAsciiAndWrite(lineRead);
            } else {
                int nRecordSize = m_varLevel01.getTotalSize();
                int nNbByteWritten = m_varLevel01.setFromLineRead(lineRead);
                if (nRecordSize > nNbByteWritten) varDest.fillEndOfRecord(nNbByteWritten, nRecordSize, CobolConstant.LowValue.getValue());
            }
        }
    }

    private void fillInto2DestEbcdic(LineRead lineRead, Var varDest) {
        varDest.fill(CobolConstant.LowValue);
        m_varLevel01.fill(CobolConstant.LowValue);
        int nRecordSize = m_varLevel01.getTotalSize();
        int nDestSize = varDest.getTotalSize();
        if (nRecordSize > nDestSize) nRecordSize = nDestSize;
        convertEbcdicToAsciiAndWrite(lineRead, varDest);
        byte tbyFilebuffer[] = m_fileManagerEntry.m_dataFile.getByteBuffer(nRecordSize);
        varDest.exportToByteArray(tbyFilebuffer, nRecordSize);
        m_varLevel01.setFromByteArray(tbyFilebuffer, 0, nRecordSize);
    }

    public String toString() {
        if (m_fileManagerEntry != null) {
            String cs = m_fileManagerEntry.toString();
            return cs + " mapped on " + m_varLevel01.toString();
        }
        return "Unknown FileManagerEntry";
    }

    public LineRead readALine(BaseDataFileBuffered dataFileIn, LineRead lastLineRead) {
        if (m_fileManagerEntry.isDummyFile()) return null;
        if (isVariableLength()) {
            boolean bReadLF = isVariableLength4BytesHeaderWithLF();
            boolean bHeader4Bytes = isVariableLength4BytesHeaderWithLF();
            lastLineRead = dataFileIn.readVariableLengthLine(bReadLF, bHeader4Bytes, lastLineRead);
        } else {
            RecordLengthDefinition recordLengthDefinition = getRecordLengthDefinition();
            if (recordLengthDefinition == null) lastLineRead = dataFileIn.readNextUnixLine(); else {
                int nRecordLength = recordLengthDefinition.getRecordLength();
                lastLineRead = dataFileIn.readBuffer(nRecordLength, true);
            }
        }
        return lastLineRead;
    }

    public LogicalFileDescriptor getLogicalFileDescriptor() {
        if (m_fileManagerEntry != null) return m_fileManagerEntry.getLogicalFileDescriptor();
        return null;
    }

    public void tryAutoDetermineRecordLengthIfRequired(BaseDataFile dataFileIn) {
        if (isVariableLength()) return;
        if (getRecordLengthDefinition() != null) return;
        LogicalFileDescriptor logicalFileDescriptor = getLogicalFileDescriptor();
        if (logicalFileDescriptor != null) logicalFileDescriptor.tryAutoDetermineRecordLength(dataFileIn);
    }

    public void write(WriteBufferExt writeBufferExt, boolean bForcedVariableLenght) {
        if (m_fileManagerEntry.isVariableLength() || bForcedVariableLenght) {
            int nRecordLength = writeBufferExt.getRecordCurrentPosition();
            if (m_tbyHeader == null) m_tbyHeader = new byte[4];
            LittleEndingSignBinaryBufferStorage.writeInt(m_tbyHeader, nRecordLength, 0);
            m_fileManagerEntry.m_dataFile.write(m_tbyHeader, 0, 4);
            byte tbyFilebuffer[] = writeBufferExt.getAsByteArrayWithTrailingLF();
            m_fileManagerEntry.m_dataFile.writeWithEOL(tbyFilebuffer, tbyFilebuffer.length);
            incNbRecordWrite();
        } else {
            byte tbyFilebuffer[] = writeBufferExt.getAsByteArrayWithTrailingLF();
            m_fileManagerEntry.m_dataFile.writeWithEOL(tbyFilebuffer, tbyFilebuffer.length);
            incNbRecordWrite();
        }
    }

    public void rewrite(WriteBufferExt writeBufferExt) {
        byte tbyFilebuffer[] = writeBufferExt.getAsByteArrayWithTrailingLF();
        m_fileManagerEntry.m_dataFile.rewriteWithEOL(tbyFilebuffer, tbyFilebuffer.length);
        incNbRecordWrite();
    }

    public boolean read(WriteBufferExt writeExt) {
        if (m_fileManagerEntry.isDummyFile()) return false;
        if (isVariableLength()) {
            long lLastHeaderStartPosition = m_fileManagerEntry.m_dataFile.getFileCurrentPosition();
            LineRead header = m_fileManagerEntry.m_dataFile.readBuffer(4, false);
            if (header != null) {
                int nLengthExcludingHeader = header.getAsLittleEndingUnsignBinaryInt();
                LineRead lineRead = m_fileManagerEntry.m_dataFile.readBuffer(nLengthExcludingHeader, true);
                m_fileManagerEntry.m_dataFile.setLastPosition(lLastHeaderStartPosition);
                if (lineRead != null) {
                    writeExt.setFromLineRead(lineRead, 0);
                    int n = getVariableRecordLength(nLengthExcludingHeader);
                    writeExt.setVariableRecordWholeLength(n);
                    return true;
                }
            }
            return false;
        } else {
            if (m_fileManagerEntry.m_dataFile.isEOF()) return false;
            int nRecordLength = getRecordLength(null);
            LineRead lineRead = null;
            if (nRecordLength > 0) lineRead = m_fileManagerEntry.m_dataFile.readBuffer(nRecordLength, true); else lineRead = m_fileManagerEntry.m_dataFile.readNextUnixLine();
            if (lineRead != null) {
                writeExt.setFromLineRead(lineRead, 0);
                incNbRecordRead();
                return true;
            }
            return false;
        }
    }

    public void registerStatus(Var varStatus) {
        m_varStatus = varStatus;
    }

    public void reportStatus(FileStatusEnum fs) {
        if (m_varStatus != null && fs != null) {
            String csCode = fs.getCode();
            m_varStatus.set(csCode);
        }
    }

    private Var m_varStatus = null;
}
