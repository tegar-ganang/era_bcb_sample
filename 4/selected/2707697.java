package nacaLib.varEx;

import java.math.BigDecimal;
import jlib.misc.AsciiEbcdicConverter;
import jlib.misc.LineRead;
import nacaLib.base.CJMapObject;
import nacaLib.base.JmxGeneralStat;
import nacaLib.basePrgEnv.BaseProgram;
import nacaLib.basePrgEnv.BaseProgramManager;
import nacaLib.fpacPrgEnv.FPacVarManager;
import nacaLib.fpacPrgEnv.VarFPacLengthUndef;
import nacaLib.programPool.SharedProgramInstanceData;
import nacaLib.sqlSupport.CSQLItemType;
import nacaLib.tempCache.TempCache;
import nacaLib.tempCache.TempCacheLocator;

/**
 * @author U930DI
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class VarBase extends CJMapObject {

    VarBase(DeclareTypeBase declareTypeBase) {
        if (declareTypeBase != null) {
            BaseProgramManager programManager = declareTypeBase.getProgramManager();
            SharedProgramInstanceData sharedProgramInstanceData = programManager.getSharedProgramInstanceData();
            m_varDef = declareTypeBase.getOrCreateVarDef(sharedProgramInstanceData);
            m_varTypeId = m_varDef.getTypeId();
            if (m_varDef.m_varDefRedefinOrigin != null) m_varDef.m_varDefRedefinOrigin.addRedefinition(m_varDef);
            if (declareTypeBase.isVariableLengthDeclaration()) programManager.defineVarDynLengthMarker((Var) this);
            programManager.registerVar(this);
        }
    }

    protected VarBase() {
    }

    public VarDefBuffer getVarDef() {
        return m_varDef;
    }

    public VarDefBuffer DEBUGgetVarDef() {
        return m_varDef;
    }

    public VarBufferPos getBuffer() {
        return m_bufferPos;
    }

    boolean isBufferComputed() {
        if (m_bufferPos != null) return true;
        return false;
    }

    public String getSTCheckValue() {
        assertIfFalse(false);
        return "";
    }

    public boolean is(CobolConstantBase constant) {
        char cPattern = constant.getValue();
        String sValue = getString();
        return BaseProgram.isAll(sValue, cPattern);
    }

    public SharedProgramInstanceData getSharedProgramInstanceData() {
        SharedProgramInstanceData sharedProgramInstanceData = null;
        if (getProgramManager() != null) sharedProgramInstanceData = getProgramManager().getSharedProgramInstanceData();
        return sharedProgramInstanceData;
    }

    public String getLoggableValue() {
        if (m_varDef != null) {
            SharedProgramInstanceData sharedProgramInstanceData = getSharedProgramInstanceData();
            if (sharedProgramInstanceData != null) {
                String cs = m_varDef.toDump(sharedProgramInstanceData);
                int nDefaultAbsolutePosition = m_varDef.DEBUGgetDefaultAbsolutePosition();
                if (m_bufferPos != null) {
                    int nAbsolutePosition = m_bufferPos.m_nAbsolutePosition;
                    if (nDefaultAbsolutePosition != nAbsolutePosition) {
                        cs += " (@" + nAbsolutePosition + ")";
                    }
                    cs += ":" + m_varDef.getDottedSignedString(m_bufferPos);
                } else cs += ":(null)";
                return cs;
            }
            return "SharedProgramInstanceData is null";
        }
        return "VarDef is null";
    }

    public void internalAssignBufferShiftPosition(char oldBuffer[], int nStartPos, int nLength, VarBuffer bufferSource, int nShift) {
        if (getBuffer().m_acBuffer == oldBuffer && getBodyAbsolutePosition() >= nStartPos && getBodyAbsolutePosition() < nStartPos + nLength) {
            m_bufferPos = new VarBufferPos(bufferSource, m_varDef.m_nDefaultAbsolutePosition - nShift);
            getEditAttributManager();
        }
    }

    public void setAtAdress(VarAndEdit varSource) {
        int nStartPos = getBodyAbsolutePosition();
        char oldBuffer[] = m_bufferPos.m_acBuffer;
        int nLength = getTotalSize();
        int nShift = varSource.getBodyAbsolutePosition() - getBodyAbsolutePosition();
        BaseProgramManager pm = TempCacheLocator.getTLSTempCache().getProgramManager();
        pm.changeBufferAndShiftPosition(oldBuffer, nStartPos, nLength, varSource.getBuffer(), -nShift);
    }

    public BaseProgramManager getProgramManager() {
        BaseProgramManager pm = TempCacheLocator.getTLSTempCache().getProgramManager();
        return pm;
    }

    public void setCustomBuffer(char[] cBuffer) {
        if (m_varDef.getLevel() != 1) return;
        int nStartPos = getBodyAbsolutePosition();
        char oldBuffer[] = m_bufferPos.m_acBuffer;
        int nLength = getTotalSize();
        VarBuffer newVarBuffer = new VarBuffer(cBuffer);
        int nShift = m_bufferPos.m_nAbsolutePosition;
        BaseProgramManager pm = TempCacheLocator.getTLSTempCache().getProgramManager();
        pm.changeBufferAndShiftPosition(oldBuffer, nStartPos, nLength, newVarBuffer, nShift);
    }

    public void moveCorresponding(MoveCorrespondingEntryManager moveCorrespondingEntryManager, VarBase varDestGroup) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        VarDefBuffer varDefDestGroup = varDestGroup.getVarDef();
        int nDestOffset = varDestGroup.getInitializeReplacingOffset(tempCache);
        int nSourceOffset = getInitializeReplacingOffset(tempCache);
        if (moveCorrespondingEntryManager != null && moveCorrespondingEntryManager.isFilled()) {
            moveCorrespondingEntryManager.doMoves(tempCache.getProgramManager(), nSourceOffset, nDestOffset);
        } else {
            SharedProgramInstanceData sharedProgramInstanceData = tempCache.getSharedProgramInstanceData();
            m_varDef.moveCorrespondingItemAndChildren(moveCorrespondingEntryManager, sharedProgramInstanceData, tempCache.getProgramManager(), varDefDestGroup, nSourceOffset, nDestOffset);
            if (moveCorrespondingEntryManager != null) moveCorrespondingEntryManager.setFilledAndCompress();
        }
    }

    public void initialize(InitializeCache initializeCache) {
        m_varDef.initializeAtOffset(m_bufferPos, 0, initializeCache);
    }

    public void initializeReplacingNum(int n) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerInt(n);
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    public void initializeReplacingNum(String cs) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerDouble(cs);
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    int getInitializeReplacingOffset(TempCache tempCache) {
        int nCatalogPos = m_varDef.m_nDefaultAbsolutePosition;
        VarDefBase varDefMaster = m_varDef.getVarDefMaster(tempCache.getSharedProgramInstanceData());
        if (varDefMaster != null) {
            nCatalogPos = varDefMaster.m_nDefaultAbsolutePosition;
            m_varDef.m_arrChildren = varDefMaster.m_arrChildren;
        }
        int nItemPos = m_varDef.m_nDefaultAbsolutePosition;
        int nOffset = nItemPos - nCatalogPos;
        return nOffset;
    }

    public void initializeReplacingAlphaNum(String cs) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerString(cs);
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    public void initializeReplacingNumEdited(int n) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerIntEdited(n);
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    public void initializeReplacingNumEdited(double d) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerDoubleEdited(d);
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    public void initializeReplacingAlphaNumEdited(String cs) {
        TempCache tempCache = TempCacheLocator.getTLSTempCache();
        InitializeManager initializeManagerManager = tempCache.getInitializeManagerStringEdited();
        int nOffset = getInitializeReplacingOffset(tempCache);
        m_varDef.initializeItemAndChildren(m_bufferPos, initializeManagerManager, nOffset, null);
    }

    public String toString() {
        return getLoggableValue();
    }

    public int getTotalSize() {
        return m_varDef.getTotalSize();
    }

    public void set(char c) {
        m_varDef.write(m_bufferPos, c);
    }

    public void set(int n) {
        m_varDef.write(m_bufferPos, n);
    }

    public void set(double d) {
        m_varDef.write(m_bufferPos, d);
    }

    public void set(long l) {
        m_varDef.write(m_bufferPos, l);
    }

    public void set(String cs) {
        m_varDef.write(m_bufferPos, cs);
    }

    public void set(Dec dec) {
        m_varDef.write(m_bufferPos, dec);
    }

    public void set(BigDecimal bigDecimal) {
        m_varDef.write(m_bufferPos, bigDecimal);
    }

    public void set(VarBase varSource) {
        if (varSource.isEdit()) set(varSource); else set(varSource);
    }

    public void declareAsFiller() {
        m_varDef.setFiller(true);
    }

    int getBodyAbsolutePosition() {
        return m_varDef.getBodyAbsolutePosition(m_bufferPos);
    }

    public int DEBUGgetBodyAbsolutePosition() {
        return m_varDef.getBodyAbsolutePosition(m_bufferPos);
    }

    int getBodyLength() {
        return m_varDef.getBodyLength();
    }

    public InternalCharBuffer exportToCharBuffer() {
        int nLength = getLength();
        return exportToCharBuffer(nLength);
    }

    public InternalCharBuffer exportToCharBuffer(int nLength) {
        if (nLength == -1) {
            nLength = getLength();
        }
        InternalCharBuffer charBufferDest = new InternalCharBuffer(nLength);
        charBufferDest.copyBytes(0, nLength, m_bufferPos.m_nAbsolutePosition, m_bufferPos);
        return charBufferDest;
    }

    public char[] exportToCharArray() {
        int nLength = getLength();
        char[] arr = new char[nLength];
        int nPositionDest = 0;
        int nPositionSource = m_bufferPos.m_nAbsolutePosition;
        for (int n = 0; n < nLength; n++, nPositionDest++, nPositionSource++) {
            char cSource = m_bufferPos.m_acBuffer[nPositionSource];
            arr[nPositionDest] = cSource;
        }
        return arr;
    }

    public void exportToByteArray(byte arr[], int nLength) {
        int nPositionDest = 0;
        int nPositionSource = m_bufferPos.m_nAbsolutePosition;
        for (int n = 0; n < nLength; n++) {
            arr[nPositionDest++] = (byte) m_bufferPos.m_acBuffer[nPositionSource++];
        }
    }

    public void exportToByteArray(byte arr[], int nOffsetDest, int nLength) {
        int nPositionDest = nOffsetDest;
        int nPositionSource = m_bufferPos.m_nAbsolutePosition;
        for (int n = 0; n < nLength; n++, nPositionDest++, nPositionSource++) {
            arr[nPositionDest] = (byte) m_bufferPos.m_acBuffer[nPositionSource];
        }
    }

    public void fill(CobolConstantBase constant) {
        char c = constant.getValue();
        m_varDef.writeRepeatingchar(m_bufferPos, c);
    }

    public void fillEndOfRecord(int nNbRecordByteAlreadyFilled, int nRecordTotalLength, char cFillerConstant) {
        int nNbBytesToFill = nRecordTotalLength - nNbRecordByteAlreadyFilled;
        m_varDef.writeRepeatingcharAtOffsetWithLength(m_bufferPos, nNbRecordByteAlreadyFilled, cFillerConstant, nNbBytesToFill);
    }

    private void fillEndOfRecordForLength(int nNbRecordByteAlreadyFilled, int nNbBytesToFill, char cFillerConstant) {
        m_varDef.writeRepeatingcharAtOffsetWithLength(m_bufferPos, nNbRecordByteAlreadyFilled, cFillerConstant, nNbBytesToFill);
    }

    public void copyBytesFromSourceIntoBody(InternalCharBuffer charBuffer) {
        m_varDef.copyBytesFromSource(m_bufferPos, getBodyAbsolutePosition(), charBuffer);
    }

    public int getLength() {
        return m_varDef.getLength();
    }

    public String getHexaValueInEbcdic() {
        String csOut = new String();
        int nLg = getLength();
        int nPos = getBodyAbsolutePosition();
        for (int n = 0; n < nLg; n++, nPos++) {
            char c = m_bufferPos.m_acBuffer[nPos];
            int nCode = c;
            String cs = AsciiEbcdicConverter.getHexaValue(nCode);
            csOut += cs;
        }
        return csOut;
    }

    public boolean DEBUGisStorageAscii() {
        return true;
    }

    public CSQLItemType getSQLType() {
        if (m_varDef != null) return m_varDef.getSQLType();
        return null;
    }

    public abstract void assignBufferExt(VarBuffer bufferSource);

    protected abstract String getAsLoggableString();

    abstract boolean isEdit();

    public abstract boolean hasType(VarTypeEnum e);

    abstract EditAttributManager getEditAttributManager();

    public void setSemanticContextValue(String csValue) {
    }

    public String getSemanticContextValue() {
        return "";
    }

    public String getSemanticContextValue(int nAbsolutePosition) {
        return "";
    }

    public void restoreDefaultAbsolutePosition() {
        if (m_varDef != null && m_bufferPos != null) m_bufferPos.m_nAbsolutePosition = m_varDef.m_nDefaultAbsolutePosition;
    }

    public double getDouble() {
        return m_varDef.getDouble(m_bufferPos);
    }

    public int getInt() {
        return m_varDef.getAsDecodedInt(m_bufferPos);
    }

    public long getLong() {
        return m_varDef.getAsDecodedLong(m_bufferPos);
    }

    public boolean isWSVar() {
        return m_varDef.getWSVar();
    }

    public int setFromLineRead(LineRead lineRead) {
        int nSourceLength = lineRead.getTotalLength();
        int nDestLength = getBodyLength();
        if (nSourceLength > nDestLength) nSourceLength = nDestLength;
        m_bufferPos.setByteArray(lineRead.getBuffer(), lineRead.getOffset(), nSourceLength);
        return nSourceLength;
    }

    public void setFromLineRead2DestWithFilling(LineRead lineRead, VarBase varDest2, char cFillerConstant) {
        int nSourceLength = lineRead.getTotalLength();
        int nDest1Length = getBodyLength();
        int nFillLength1 = 0;
        if (nSourceLength < nDest1Length) nFillLength1 = nDest1Length - nSourceLength;
        if (nSourceLength > nDest1Length) nSourceLength = nDest1Length;
        int nFillLength2 = 0;
        int nDest2Length = varDest2.getBodyLength();
        if (nDest1Length < nDest2Length) nFillLength2 = nDest1Length - nDest2Length;
        if (nDest1Length > nDest2Length) nDest2Length = nDest1Length;
        m_bufferPos.setByteArray(lineRead.getBuffer(), lineRead.getOffset(), nSourceLength, varDest2.m_bufferPos, nDest2Length);
        if (nFillLength1 != 0) fillEndOfRecordForLength(nSourceLength, nFillLength1, cFillerConstant);
        if (nFillLength2 != 0) varDest2.fillEndOfRecordForLength(nSourceLength, nFillLength2, cFillerConstant);
    }

    public void setFromByteArray(byte[] tBytes, int nOffsetSource, int nLength) {
        m_bufferPos.setByteArray(tBytes, nOffsetSource, nLength);
    }

    public byte[] getAsByteArray() {
        int nLength = m_varDef.getRecordDependingLength(m_bufferPos);
        char[] tChars = m_bufferPos.getByteArray(this, nLength);
        byte[] tBytes = AsciiEbcdicConverter.noConvertUnicodeToEbcdic(tChars);
        return tBytes;
    }

    public byte[] getAsEbcdicByteArray() {
        int nLength = getLength();
        char[] tChars = m_bufferPos.getByteArray(this, nLength);
        byte[] tBytes = convertUnicodeToEbcdic(tChars);
        return tBytes;
    }

    protected abstract byte[] convertUnicodeToEbcdic(char[] tBytes);

    protected abstract char[] convertEbcdicToUnicode(byte[] tBytes);

    public byte[] doConvertUnicodeToEbcdic(char[] tChars) {
        return AsciiEbcdicConverter.convertUnicodeToEbcdic(tChars);
    }

    public char[] doConvertEbcdicToUnicode(byte[] tBytes) {
        return AsciiEbcdicConverter.convertEbcdicToUnicode(tBytes);
    }

    public abstract String getString();

    public abstract String getDottedSignedString();

    public abstract String getDottedSignedStringAsSQLCol();

    public VarFPacLengthUndef createVarFPacUndef(FPacVarManager fpacVarManager, VarBuffer varBuffer, int nAbsolutePosition) {
        return null;
    }

    public String DEBUGgetBufferDumpHexaInEbcdic() {
        return getHexaValueInEbcdic();
    }

    public void importFromByteArray(byte tBytesSource[], int nSizeSource) {
        m_bufferPos.importFromByteArray(tBytesSource, m_varDef.getTotalSize(), nSizeSource);
    }

    public void exportIntoByteArray(byte tbyDest[], int nLengthDest) {
        m_bufferPos.exportIntoByteArray(tbyDest, nLengthDest, m_varDef.getTotalSize());
    }

    public int getId() {
        return m_varDef.getId();
    }

    public VarDefBuffer m_varDef = null;

    public VarBufferPos m_bufferPos = null;

    public abstract VarType getVarType();

    public int m_varTypeId = VarTypeId.VarDefUnknownTypeId;
}
