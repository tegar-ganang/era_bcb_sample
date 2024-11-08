package uk.gov.dti.og.fox;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import oracle.sql.BLOB;
import oracle.sql.CLOB;
import oracle.sql.CharacterSet;
import oracle.sql.Datum;
import oracle.xdb.XMLType;
import uk.gov.dti.og.fox.bean.GenericPropertyBean;
import uk.gov.dti.og.fox.bean.PropertyBean;
import uk.gov.dti.og.fox.dom.DOM;
import uk.gov.dti.og.fox.dom.DOMList;
import uk.gov.dti.og.fox.ex.ExDBSyntax;
import uk.gov.dti.og.fox.ex.ExDBTimeout;
import uk.gov.dti.og.fox.ex.ExDBTooFew;
import uk.gov.dti.og.fox.ex.ExDBTooMany;
import uk.gov.dti.og.fox.ex.ExInternal;
import uk.gov.dti.og.fox.ex.ExModule;
import uk.gov.dti.og.fox.ex.ExRoot;
import uk.gov.dti.og.fox.ex.ExServiceUnavailable;
import uk.gov.dti.og.fox.filetransfer.UploadInfo;
import uk.gov.dti.og.fox.io.IOUtil;
import uk.gov.dti.og.fox.io.StreamParcelInput;
import uk.gov.dti.og.fox.io.StreamParcelInputBLOB;
import uk.gov.dti.og.fox.io.StreamParcelInputCLOB;
import uk.gov.dti.og.fox.track.Track;
import uk.gov.dti.og.fox.xhtml.NameValuePair;
import uk.gov.dti.og.net.fileuploads.FileUploadInfo;

public class WorkingFileStoreLocation extends Track {

    private FileStorageLocation mStorageLocation;

    private static final int RECORD_LOCKED_ATTEMPTS = 5;

    private static final int STATUS_CLOSED = 1;

    private static final int STATUS_OPEN = 1;

    private static final int STATUS_TIMEOUT_SECONDS = 10;

    private String mEvaluatedQueryStrVals[];

    private String mEvaluatedInsertStrVals[];

    private String mEvaluatedUpdateStrVals[];

    private String mEvaluatedAPIStrVals[];

    private int mStatus = STATUS_CLOSED;

    private BLOB mBLOB;

    private static String LOB_SIZE_QUERY = "BEGIN\n" + "  :1 := dbms_lob.getlength(:2);\n" + "END;";

    private static HashMap ORACLE_TO_JAVA_CHARSETS = new HashMap(5);

    {
        ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.WE8MSWIN1252_CHARSET), "windows-1252");
        ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.AL32UTF8_CHARSET), "UTF-8");
        ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.AL16UTF16_CHARSET), "UTF-16");
        ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.US7ASCII_CHARSET), "US-ASCII");
        ORACLE_TO_JAVA_CHARSETS.put(new Short(CharacterSet.WE8ISO8859P1_CHARSET), "ISO-8859-1");
    }

    public WorkingFileStoreLocation(FileStorageLocation fsl, ContextUElem contextUElem) throws ExModule, ExInternal {
        String lUniqueConstant = XFUtil.unique();
        mStorageLocation = fsl;
        mEvaluatedQueryStrVals = convertTuplesArrayToValuesArray(StoreLocation.evaluateQueryUsingClauses(contextUElem, fsl.getQueryUsingClauses(), lUniqueConstant));
        mEvaluatedInsertStrVals = StoreLocation.evaluateQueryUsingClauses(contextUElem, fsl.getInsertUsingClauses(), lUniqueConstant);
        mEvaluatedUpdateStrVals = StoreLocation.evaluateQueryUsingClauses(contextUElem, fsl.getUpdateUsingClauses(), lUniqueConstant);
        mEvaluatedAPIStrVals = StoreLocation.evaluateQueryUsingClauses(contextUElem, fsl.getAPICallUsingClauses(), lUniqueConstant);
    }

    public DOM serialise() {
        DOM lDOM = DOM.createDocument("working-file-store-location");
        DOM lSelect = lDOM.addElem("select");
        lSelect.addElem("statement", mStorageLocation.getQueryStatement());
        if (mEvaluatedQueryStrVals != null) {
            for (int i = 0; i < mEvaluatedQueryStrVals.length; i++) lSelect.addElem("bind-" + i, mEvaluatedQueryStrVals[i]);
        }
        DOM lInsert = lDOM.addElem("insert");
        lInsert.addElem("statement", mStorageLocation.getInsertStatement());
        if (mEvaluatedInsertStrVals != null) {
            for (int i = 0; i < mEvaluatedInsertStrVals.length; i++) lInsert.addElem("bind-" + i, mEvaluatedInsertStrVals[i]);
        }
        DOM lUpdate = lDOM.addElem("update");
        lUpdate.addElem("statement", mStorageLocation.getUpdateStatement());
        if (mEvaluatedUpdateStrVals != null) {
            for (int i = 0; i < mEvaluatedUpdateStrVals.length; i++) lUpdate.addElem("bind-" + i, mEvaluatedUpdateStrVals[i]);
        }
        DOM lAPI = lDOM.addElem("api");
        lAPI.addElem("statement", mStorageLocation.getAPICallScript());
        if (mEvaluatedAPIStrVals != null) {
            for (int i = 0; i < mEvaluatedAPIStrVals.length; i++) lAPI.addElem("bind-" + i, mEvaluatedAPIStrVals[i]);
        }
        return lDOM;
    }

    public FileStorageLocation getFileStorageLocation() {
        return mStorageLocation;
    }

    /**
   * This method seeks bind variables specified in the storage location that reference
   * the metadata supplied on a file upload, and reloads the evaluated string value arrays
   * accordingly. All other XPaths are unaffected and will remain in the state they were in
   * when this object is instantiated.
   * <br/><br/>
   * It is necessary to perform this step because when the file upload routine is writing
   * the uploaded data to a storage location, the metadata does not exist on a DOM. Additionally,
   * this WorkingFileStorageLocation will have been instantiated during HTML generation, so the
   * results of the initial XPath evaluation based on upload metadata are potentially stale.
   * <br/><br/> 
   * It is envisaged that this routine will mostly be used to populate the correct file-id
   * into the WFSL before an update is attempted. FOX Developers should ensure they only reference
   * such metadata in a relative manner when the WFSL is being used to enable file uploads.
   * <br/><br/> 
   * @param pUploadInfoDOM The metadata from the file upload, wrapped in a containing element
   */
    public void reEvaluateBindsUsingUploadInfoDOM(DOM pUploadInfoDOM) {
        Set lUploadInfoPathSet = new HashSet();
        getSimplePathsForAllNodesInDOM(pUploadInfoDOM, lUploadInfoPathSet);
        seekAndReplaceBinds(mStorageLocation.getQueryUsingClauses(), mEvaluatedQueryStrVals, lUploadInfoPathSet, pUploadInfoDOM, false);
        seekAndReplaceBinds(mStorageLocation.getInsertUsingClauses(), mEvaluatedInsertStrVals, lUploadInfoPathSet, pUploadInfoDOM, true);
        seekAndReplaceBinds(mStorageLocation.getUpdateUsingClauses(), mEvaluatedUpdateStrVals, lUploadInfoPathSet, pUploadInfoDOM, true);
        seekAndReplaceBinds(mStorageLocation.getAPICallUsingClauses(), mEvaluatedAPIStrVals, lUploadInfoPathSet, pUploadInfoDOM, true);
    }

    private void seekAndReplaceBinds(String[] pClauseTuples, String[] pEvalutatedStrVals, Set pUploadInfoPathSet, DOM pUploadInfoDOM, boolean pEvaluatedStrValsIsTuplesArray) {
        if (pClauseTuples == null) return;
        CLAUSE_LOOP: for (int i = 0; i < pClauseTuples.length; i++) {
            if (i % 2 == 0) continue CLAUSE_LOOP;
            String lClauseXpath = pClauseTuples[i].trim();
            for (Iterator it = pUploadInfoPathSet.iterator(); it.hasNext(); ) {
                String lPath = ((String) it.next()).trim();
                if (lClauseXpath.indexOf(lPath) != -1) {
                    if (lClauseXpath.indexOf(":{") != -1 || lClauseXpath.charAt(0) == '/') {
                        throw new ExInternal("Unsupported use of complex or absolute XPath detected referencing file upload metadata. This XPath is required to be relative to the upload element at this time.");
                    }
                    pEvalutatedStrVals[i / (pEvaluatedStrValsIsTuplesArray ? 1 : 2)] = pUploadInfoDOM.get1SNoEx("./" + lPath);
                }
            }
        }
    }

    /**
   * Gets a Set of all the simple XPaths yielded from nodes in the given DOM,
   * excluding the name of the containing element. I.e. /file-id, /captured-field/description, etc
   * @param pDOM
   * @param pSet
   */
    private static void getSimplePathsForAllNodesInDOM(DOM pDOM, Set pSet) {
        DOMList lDOMList = pDOM.getChildElements();
        DOM lChild;
        while ((lChild = lDOMList.popHead()) != null) {
            String lPath = lChild.absolute();
            pSet.add(lPath.substring(lPath.substring(1).indexOf('/') + 1));
            getSimplePathsForAllNodesInDOM(lChild, pSet);
        }
    }

    /**
   * Selects the target BLOB for update and returns a pointer
   * @return pointer to an open, writeable BLOB
   */
    public BLOB openBLOB(UCon pUCon, UploadInfo pUploadInfo) {
        return openInternal(pUCon, pUploadInfo);
    }

    private BLOB openInternal(UCon pUCon, UploadInfo pUploadInfo) {
        Object lResultObjs[] = null;
        Object lBindVals[] = null;
        try {
            TIMEOUT_LOOP: for (int i = 0; i < STATUS_TIMEOUT_SECONDS; i++) {
                if (mBLOB != null || mStatus != STATUS_CLOSED) {
                    Thread.sleep(1000);
                } else {
                    break TIMEOUT_LOOP;
                }
            }
        } catch (InterruptedException e) {
            throw new ExInternal("Interrupted while waiting for WorkingFileStorageLocation to become open.");
        }
        if (mBLOB != null) throw new ExInternal("Cannot open a storage location with a non-null mBLOB.");
        if (mStatus != STATUS_CLOSED) throw new ExInternal("Cannot open a WorkingFileStoreLocation if already open.");
        try {
            lResultObjs = attemptSelect(pUCon);
            if (lResultObjs == null) {
                if (performInsert(pUCon, pUploadInfo) != 1) {
                    throw new ExInternal("Unable to successfully insert a row for file storage location. An insert statement is required if the select statement might return 0 rows.");
                }
                lResultObjs = attemptSelect(pUCon);
                if (lResultObjs == null) {
                    throw new ExInternal("Unable to select a row after performing insert. Check insert/select statement definitions in file storage location.");
                }
            }
            BLOB lBLOB;
            lBLOB = (BLOB) getLOBFromResultArray(lResultObjs);
            if (lBLOB.isTemporary()) throw new ExInternal("File storage location open would have returned a temporary LOB. Non-temporary LOB required.");
            if (!lBLOB.isOpen()) lBLOB.open(BLOB.MODE_READWRITE);
            mStatus = STATUS_OPEN;
            mBLOB = lBLOB;
            return mBLOB;
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL Error ", ex);
        } finally {
            freeTemporaryLOBS(lBindVals, mBLOB);
        }
    }

    /**
   * Run any update statements and close the BLOB and storage location.
   */
    public void close(UCon pUCon, UploadInfo pUploadInfo) {
        if (mBLOB == null) throw new ExInternal("Cannot close a storage location with a null mBLOB.");
        if (mStatus != STATUS_OPEN) throw new ExInternal("Cannot close a storage location which is not open.");
        try {
            if (mStorageLocation.getUpdateStatement() != null) {
                Object[] lBindVals = convertBindTupleVals(mEvaluatedUpdateStrVals, mBLOB, pUCon, pUploadInfo);
                pUCon.executeDML(mStorageLocation.getUpdateStatement(), lBindVals);
            }
            mBLOB.close();
            mBLOB = null;
            mStatus = STATUS_CLOSED;
        } catch (ExDBSyntax ex) {
            throw ex.toUnexpected();
        } catch (ExDBTimeout ex) {
            throw ex.toUnexpected();
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL Error ", ex);
        }
    }

    /**
   * Logical close without running update or closing BLOB (use after a rollback to 
   * enable subsequent uploads to the same WFSL)
   */
    public void closeOnError() {
        mBLOB = null;
        mStatus = STATUS_CLOSED;
    }

    private Object[] attemptSelect(UCon pUCon) throws ExServiceUnavailable, ExDBTooMany, ExDBSyntax {
        Object[] lResultObjs = null;
        TRY_LOOP: for (int lTry = 0; lTry < RECORD_LOCKED_ATTEMPTS; lTry++) {
            try {
                lResultObjs = pUCon.selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
                if (lResultObjs != null) break;
            } catch (ExDBTooFew e) {
                return null;
            } catch (ExDBTimeout e) {
                if (lTry == RECORD_LOCKED_ATTEMPTS) throw new ExServiceUnavailable("File Storage Location: Row remains locked after " + RECORD_LOCKED_ATTEMPTS + " tries: " + mStorageLocation.getQueryStatement(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException x) {
                    throw new ExServiceUnavailable("Storage Location: sleep was interrupted", x);
                }
            }
        }
        return lResultObjs;
    }

    private int performInsert(UCon pUCon, UploadInfo pUploadInfo) throws ExDBSyntax, ExDBTimeout {
        if (mStorageLocation.getInsertStatement() != null) {
            if (hasFileReferenceInBindParams(mEvaluatedInsertStrVals)) {
                Object[] lBindVals = convertBindTupleVals(mEvaluatedInsertStrVals, null, pUCon, pUploadInfo);
                return pUCon.executeDML(mStorageLocation.getInsertStatement(), lBindVals);
            } else {
                String[] evaluatedInsertStrValsCpy = convertTuplesArrayToValuesArray(mEvaluatedInsertStrVals);
                return pUCon.executeDML(mStorageLocation.getInsertStatement(), evaluatedInsertStrValsCpy);
            }
        } else {
            return 0;
        }
    }

    /**
   * Updates/Inserts the specified file, from the <code>InputStream</code> specified.
   * 
   * @param file the file to upload.
   * @throws ExInternal if an unexpected or system I/O error occurs during the update.
   */
    public void updateFile(File file, ContextUCon contextUCon, FileUploadInfo fileUploadInfo) throws ExInternal {
        Object resultObjs[] = null;
        Object bindVals[] = null;
        try {
            resultObjs = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            if (mStorageLocation.getUpdateStatement() != null) {
                bindVals = convertBindTupleVals(mEvaluatedUpdateStrVals, contextUCon.getUCon(), file, fileUploadInfo);
                contextUCon.getUCon().executeDML(mStorageLocation.getUpdateStatement(), bindVals);
            } else {
                updateUsingSelectedLOB(file, contextUCon, fileUploadInfo, resultObjs);
            }
            contextUCon.getUCon().commit();
        } catch (ExDBTooFew ex) {
            try {
                if (mStorageLocation.getInsertStatement() != null) {
                    if (hasFileReferenceInBindParams(mEvaluatedInsertStrVals)) {
                        bindVals = convertBindTupleVals(mEvaluatedInsertStrVals, contextUCon.getUCon(), file, fileUploadInfo);
                        contextUCon.getUCon().executeDML(mStorageLocation.getInsertStatement(), bindVals);
                    } else {
                        String[] evaluatedInsertStrValsCpy = convertTuplesArrayToValuesArray(mEvaluatedInsertStrVals);
                        contextUCon.getUCon().executeDML(mStorageLocation.getInsertStatement(), evaluatedInsertStrValsCpy);
                        resultObjs = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
                        updateUsingSelectedLOB(file, contextUCon, fileUploadInfo, resultObjs);
                    }
                } else {
                    if (mStorageLocation.getUpdateStatement() != null) {
                        bindVals = convertBindTupleVals(mEvaluatedUpdateStrVals, contextUCon.getUCon(), file, fileUploadInfo);
                        contextUCon.getUCon().executeDML(mStorageLocation.getUpdateStatement(), bindVals);
                    } else {
                        throw new ExInternal("Unable to update file - the file storage location in the module does not have a mechanism to insert or " + "update the file, using \"insert\" or \"update\" statements respectively. Please update the file storage location " + "with at least one of these.");
                    }
                }
                contextUCon.getUCon().commit();
            } catch (ExRoot rootEx) {
                throw rootEx.toUnexpected();
            }
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        } finally {
            freeTemporaryLOBS(bindVals);
        }
    }

    public void updateLOB(Object pLOB, ContextUCon contextUCon, FileUploadInfo fileUploadInfo) throws ExInternal {
        Object resultObjs[] = null;
        Object bindVals[] = null;
        try {
            resultObjs = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            if (mStorageLocation.getUpdateStatement() != null) {
                bindVals = convertBindTupleVals(mEvaluatedUpdateStrVals, contextUCon.getUCon(), pLOB, fileUploadInfo);
                contextUCon.getUCon().executeDML(mStorageLocation.getUpdateStatement(), bindVals);
            } else {
                updateUsingSelectedLOB(pLOB, contextUCon, fileUploadInfo, resultObjs);
            }
            contextUCon.getUCon().commit();
        } catch (ExDBTooFew ex) {
            try {
                if (mStorageLocation.getInsertStatement() != null) {
                    if (hasFileReferenceInBindParams(mEvaluatedInsertStrVals)) {
                        bindVals = convertBindTupleVals(mEvaluatedInsertStrVals, contextUCon.getUCon(), pLOB, fileUploadInfo);
                        contextUCon.getUCon().executeDML(mStorageLocation.getInsertStatement(), bindVals);
                    } else {
                        String[] evaluatedInsertStrValsCpy = convertTuplesArrayToValuesArray(mEvaluatedInsertStrVals);
                        contextUCon.getUCon().executeDML(mStorageLocation.getInsertStatement(), evaluatedInsertStrValsCpy);
                        resultObjs = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
                        updateUsingSelectedLOB(pLOB, contextUCon, fileUploadInfo, resultObjs);
                    }
                } else {
                    if (mStorageLocation.getUpdateStatement() != null) {
                        bindVals = convertBindTupleVals(mEvaluatedUpdateStrVals, contextUCon.getUCon(), pLOB, fileUploadInfo);
                        contextUCon.getUCon().executeDML(mStorageLocation.getUpdateStatement(), bindVals);
                    } else {
                        throw new ExInternal("Unable to update file - the file storage location in the module does not have a mechanism to insert or " + "update the file, using \"insert\" or \"update\" statements respectively. Please update the file storage location " + "with at least one of these.");
                    }
                }
                contextUCon.getUCon().commit();
            } catch (ExRoot rootEx) {
                throw rootEx.toUnexpected();
            }
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        } finally {
            freeTemporaryLOBS(bindVals);
        }
    }

    private void updateUsingSelectedLOB(BLOB pTemporaryBlob, UCon pUCon, Object[] resultObjs) throws ExInternal {
        BLOB blob = null;
        try {
            Object lob = null;
            for (int n = 0; n < resultObjs.length && lob == null; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[n] instanceof BLOB) lob = resultObjs[n];
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            if (lob instanceof CLOB) {
                CLOB clob = (CLOB) lob;
                clob.trim(0);
            } else if (lob instanceof BLOB) {
                blob = (BLOB) lob;
                blob.trim(0);
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
            String stmt = "DECLARE " + "  lBlobLocatorIn BLOB := :1; " + "  lBlobLocatorOut BLOB := :2; " + "  lLength NUMBER; " + "BEGIN " + "  lLength := dbms_lob.getlength(lBlobLocatorIn); " + "  :3 := lLength; " + "  DBMS_LOB.COPY(lBlobLocatorOut, lBlobLocatorIn, lLength, 1, 1); " + "  :4 := dbms_lob.getlength(lBlobLocatorOut); " + "END;";
            Object params[] = { pTemporaryBlob, blob, String.class, String.class };
            params = pUCon.executeCall(stmt, params, new char[] { 'I', 'I', 'O', 'O' });
            if (!((String) params[2]).equals(((String) params[3]))) throw new ExInternal("Failed to write file to database because the temporary blob was not the same size as the destination blob.");
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB update - see nested exception for " + "further information.", ex);
        } catch (ExDBSyntax ex) {
            throw new ExInternal("Unexpected DB Syntax error during file storage location CLOB/BLOB update.", ex);
        } catch (ExDBTimeout ex) {
            throw new ExInternal("Unexpected DB timeout error during file storage location CLOB/BLOB update.", ex);
        }
    }

    private void updateUsingSelectedLOB(File file, ContextUCon contextUCon, FileUploadInfo fileUploadInfo, Object[] resultObjs) throws ExInternal {
        InputStream fileIS = null;
        try {
            fileIS = new FileInputStream(file);
            Object lob = null;
            for (int n = 0; n < resultObjs.length && lob == null; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[n] instanceof BLOB) lob = resultObjs[n];
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            OutputStream os;
            if (lob instanceof CLOB) {
                CLOB clob = (CLOB) lob;
                clob.trim(0);
                os = clob.getAsciiOutputStream();
            } else if (lob instanceof BLOB) {
                BLOB blob = (BLOB) lob;
                blob.trim(0);
                os = blob.getBinaryOutputStream();
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
            byte transferBuf[] = new byte[32000];
            int readCount;
            fileUploadInfo.currentUploadSize = 0;
            while ((readCount = fileIS.read(transferBuf)) != -1) {
                fileUploadInfo.currentUploadSize += readCount;
                os.write(transferBuf, 0, readCount);
                os.flush();
            }
            os.close();
        } catch (IOException ex) {
            throw new ExInternal("Unexpected error occurred during update of file storage location - see nested exception for " + "further information.", mStorageLocation.getXML(), ex);
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB update - see nested exception for " + "further information.", ex);
        } finally {
            IOUtil.close(fileIS);
        }
    }

    private void updateUsingSelectedLOB(Object pLOB, ContextUCon contextUCon, FileUploadInfo fileUploadInfo, Object[] resultObjs) throws ExInternal {
        InputStream lobIS = null;
        try {
            if (pLOB instanceof CLOB) {
                lobIS = ((CLOB) pLOB).getAsciiStream();
            } else if (pLOB instanceof BLOB) {
                lobIS = ((BLOB) pLOB).getBinaryStream();
            }
            Object lob = null;
            for (int n = 0; n < resultObjs.length && lob == null; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[n] instanceof BLOB) lob = resultObjs[n];
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            OutputStream os;
            if (lob instanceof CLOB) {
                CLOB clob = (CLOB) lob;
                clob.trim(0);
                os = clob.getAsciiOutputStream();
            } else if (lob instanceof BLOB) {
                BLOB blob = (BLOB) lob;
                blob.trim(0);
                os = blob.getBinaryOutputStream();
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
            byte transferBuf[] = new byte[32000];
            int readCount;
            fileUploadInfo.currentUploadSize = 0;
            while ((readCount = lobIS.read(transferBuf)) != -1) {
                fileUploadInfo.currentUploadSize += readCount;
                os.write(transferBuf, 0, readCount);
                os.flush();
            }
            os.close();
        } catch (IOException ex) {
            throw new ExInternal("Unexpected error occurred during update of file storage location - see nested exception for " + "further information.", mStorageLocation.getXML(), ex);
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB update - see nested exception for " + "further information.", ex);
        } finally {
            IOUtil.close(lobIS);
        }
    }

    private Object[] convertBindTupleVals(String[] strBindValues, BLOB pTempBlob, UCon pUCon, UploadInfo pUploadInfo) {
        try {
            Object newBindTupleValues[] = new Object[strBindValues.length / 2];
            for (int n = 0; n < strBindValues.length / 2; n++) {
                if (strBindValues[n * 2].equals(StoreLocation.CLOB_USING_TYPE)) {
                    CLOB temporaryCLOB = null;
                    if (pTempBlob == null) temporaryCLOB = CLOB.empty_lob(); else {
                        temporaryCLOB = pUCon.getTemporaryClob();
                        InputStream lClobIS = temporaryCLOB.getStream();
                        OutputStream lBlobOS = pTempBlob.getBinaryOutputStream();
                        IOUtil.transfer(lClobIS, lBlobOS, 32768);
                        IOUtil.close(lClobIS);
                        IOUtil.close(lBlobOS);
                    }
                    newBindTupleValues[n] = temporaryCLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.BLOB_USING_TYPE)) {
                    BLOB temporaryBLOB = null;
                    if (pTempBlob == null) temporaryBLOB = BLOB.empty_lob(); else {
                        temporaryBLOB = pTempBlob;
                    }
                    newBindTupleValues[n] = temporaryBLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.XMLTYPE_USING_TYPE)) {
                    CLOB temporaryCLOB = null;
                    if (pTempBlob == null) temporaryCLOB = CLOB.empty_lob(); else {
                        temporaryCLOB = pUCon.getTemporaryClob();
                        InputStream lClobIS = temporaryCLOB.getStream();
                        OutputStream lBlobOS = pTempBlob.getBinaryOutputStream();
                        IOUtil.transfer(lClobIS, lBlobOS, 32768);
                        IOUtil.close(lClobIS);
                        IOUtil.close(lBlobOS);
                    }
                    XMLType temporaryXMLType = pUCon.createXmlType(temporaryCLOB);
                    newBindTupleValues[n] = temporaryXMLType;
                } else if (strBindValues[n * 2].equals(StoreLocation.FILE_METADATA_XMLTYPE)) {
                    DOM metaDataDOM = DOM.createDocument("file-metadata");
                    XThread.writeUploadMetadataToDOM(metaDataDOM, pUploadInfo, false, false);
                    String metaDataDocStr = metaDataDOM.outputDocumentToString();
                    CLOB clob = pUCon.getTemporaryClob();
                    clob.trim(0);
                    Writer writer = clob.getCharacterOutputStream();
                    Reader reader = new StringReader(metaDataDocStr);
                    IOUtil.transfer(reader, writer, 32768);
                    IOUtil.close(writer);
                    XMLType temporaryFileInfoXMLType = pUCon.createXmlType(clob);
                    newBindTupleValues[n] = temporaryFileInfoXMLType;
                } else {
                    newBindTupleValues[n] = strBindValues[n * 2 + 1];
                }
            }
            return newBindTupleValues;
        } catch (Throwable th) {
            throw new ExInternal("Unexpected error uploading file content - see nested exception for further information.", th);
        }
    }

    private Object[] convertBindTupleVals(String[] strBindValues, UCon connection, File uploadedFile, FileUploadInfo fileUploadInfo) {
        try {
            Object newBindTupleValues[] = new Object[strBindValues.length / 2];
            for (int n = 0; n < strBindValues.length / 2; n++) {
                if (strBindValues[n * 2].equals(StoreLocation.CLOB_USING_TYPE)) {
                    CLOB temporaryCLOB = null;
                    if (uploadedFile == null) temporaryCLOB = CLOB.empty_lob(); else {
                        temporaryCLOB = connection.getTemporaryClob();
                        temporaryCLOB.trim(0);
                        Writer writer = temporaryCLOB.getCharacterOutputStream();
                        Reader reader = new FileReader(uploadedFile);
                        IOUtil.transfer(reader, writer, 32768);
                        IOUtil.close(writer);
                        IOUtil.close(reader);
                    }
                    newBindTupleValues[n] = temporaryCLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.BLOB_USING_TYPE)) {
                    BLOB temporaryBLOB = null;
                    if (uploadedFile == null) temporaryBLOB = BLOB.empty_lob(); else {
                        temporaryBLOB = connection.getTemporaryBlob();
                        temporaryBLOB.trim(0);
                        OutputStream os = temporaryBLOB.getBinaryOutputStream();
                        InputStream is = new FileInputStream(uploadedFile);
                        IOUtil.transfer(is, os, 32768);
                        IOUtil.close(os);
                        IOUtil.close(is);
                    }
                    newBindTupleValues[n] = temporaryBLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.XMLTYPE_USING_TYPE)) {
                    CLOB temporaryCLOB = null;
                    if (uploadedFile == null) temporaryCLOB = CLOB.empty_lob(); else {
                        temporaryCLOB = connection.getTemporaryClob();
                        temporaryCLOB.trim(0);
                        Writer writer = temporaryCLOB.getCharacterOutputStream();
                        Reader reader = new FileReader(uploadedFile);
                        IOUtil.transfer(reader, writer, 32768);
                        IOUtil.close(writer);
                        IOUtil.close(reader);
                    }
                    XMLType temporaryXMLType = connection.createXmlType(temporaryCLOB);
                    newBindTupleValues[n] = temporaryXMLType;
                } else if (strBindValues[n * 2].equals(StoreLocation.FILE_METADATA_XMLTYPE)) {
                    DOM metaDataDOM = DOM.createDocument("file-metadata");
                    new DOM(fileUploadInfo.serialiseFileMetadataTOXML().getDocumentElement()).copyContentsTo(metaDataDOM);
                    String metaDataDocStr = metaDataDOM.outputDocumentToString();
                    CLOB clob = connection.getTemporaryClob();
                    clob.trim(0);
                    Writer writer = clob.getCharacterOutputStream();
                    Reader reader = new StringReader(metaDataDocStr);
                    IOUtil.transfer(reader, writer, 32768);
                    IOUtil.close(writer);
                    XMLType temporaryFileInfoXMLType = connection.createXmlType(clob);
                    newBindTupleValues[n] = temporaryFileInfoXMLType;
                } else {
                    newBindTupleValues[n] = strBindValues[n * 2 + 1];
                }
            }
            return newBindTupleValues;
        } catch (Throwable th) {
            throw new ExInternal("Unexpected error uploading file content - see nested exception for " + "further information.", th);
        }
    }

    private Object[] convertBindTupleVals(String[] strBindValues, UCon connection, Object pLOB, FileUploadInfo fileUploadInfo) {
        try {
            Object newBindTupleValues[] = new Object[strBindValues.length / 2];
            for (int n = 0; n < strBindValues.length / 2; n++) {
                if (strBindValues[n * 2].equals(StoreLocation.CLOB_USING_TYPE)) {
                    CLOB temporaryCLOB = (CLOB) pLOB;
                    newBindTupleValues[n] = temporaryCLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.BLOB_USING_TYPE)) {
                    BLOB temporaryBLOB = (BLOB) pLOB;
                    newBindTupleValues[n] = temporaryBLOB;
                } else if (strBindValues[n * 2].equals(StoreLocation.XMLTYPE_USING_TYPE)) {
                    CLOB temporaryCLOB = (CLOB) pLOB;
                    XMLType temporaryXMLType = connection.createXmlType(temporaryCLOB);
                    newBindTupleValues[n] = temporaryXMLType;
                } else if (strBindValues[n * 2].equals(StoreLocation.FILE_METADATA_XMLTYPE)) {
                    DOM metaDataDOM = DOM.createDocument("file-metadata");
                    new DOM(fileUploadInfo.serialiseFileMetadataTOXML().getDocumentElement()).copyContentsTo(metaDataDOM);
                    String metaDataDocStr = metaDataDOM.outputDocumentToString();
                    CLOB clob = connection.getTemporaryClob();
                    clob.trim(0);
                    Writer writer = clob.getCharacterOutputStream();
                    Reader reader = new StringReader(metaDataDocStr);
                    IOUtil.transfer(reader, writer, 32768);
                    IOUtil.close(writer);
                    XMLType temporaryFileInfoXMLType = connection.createXmlType(clob);
                    newBindTupleValues[n] = temporaryFileInfoXMLType;
                } else {
                    newBindTupleValues[n] = strBindValues[n * 2 + 1];
                }
            }
            return newBindTupleValues;
        } catch (Throwable th) {
            throw new ExInternal("Unexpected error uploading file content - see nested exception for " + "further information.", th);
        }
    }

    private void freeTemporaryLOBS(Object[] bindVals, BLOB pTempBlob) {
        if (bindVals == null) return;
        for (int n = 0; n < bindVals.length; n++) {
            try {
                if (bindVals[n] != pTempBlob) {
                    if (bindVals[n] instanceof BLOB && !((BLOB) bindVals[n]).isEmptyLob() && ((BLOB) bindVals[n]).isTemporary()) ((BLOB) bindVals[n]).freeTemporary(); else if (bindVals[n] instanceof CLOB && !((BLOB) bindVals[n]).isEmptyLob() && ((BLOB) bindVals[n]).isTemporary()) ((BLOB) bindVals[n]).freeTemporary(); else if (bindVals[n] instanceof XMLType) {
                        XMLType xmlType = (XMLType) bindVals[n];
                        if (!xmlType.getClobVal().isEmptyLob() && xmlType.getClobVal().isTemporary()) xmlType.getClobVal().freeTemporary();
                        xmlType.close();
                    }
                }
            } catch (Throwable ignoreEx) {
                ignoreEx.printStackTrace();
            }
        }
    }

    private void freeTemporaryLOBS(Object[] bindVals) {
        if (bindVals == null) return;
        for (int n = 0; n < bindVals.length; n++) {
            try {
                if (bindVals[n] instanceof BLOB && !((BLOB) bindVals[n]).isEmptyLob() && ((BLOB) bindVals[n]).isTemporary()) ((BLOB) bindVals[n]).freeTemporary(); else if (bindVals[n] instanceof CLOB && !((BLOB) bindVals[n]).isEmptyLob() && ((BLOB) bindVals[n]).isTemporary()) ((BLOB) bindVals[n]).freeTemporary(); else if (bindVals[n] instanceof XMLType) {
                    XMLType xmlType = (XMLType) bindVals[n];
                    if (!xmlType.getClobVal().isEmptyLob() && xmlType.getClobVal().isTemporary()) xmlType.getClobVal().freeTemporary();
                    xmlType.close();
                }
            } catch (Throwable ignoreEx) {
                ignoreEx.printStackTrace();
            }
        }
    }

    private boolean hasFileReferenceInBindParams(String[] strBindValues) {
        for (int n = 0; n < strBindValues.length / 2; n++) {
            if (strBindValues[n * 2].equals(StoreLocation.CLOB_USING_TYPE) || strBindValues[n * 2].equals(StoreLocation.BLOB_USING_TYPE) || strBindValues[n * 2].equals(StoreLocation.XMLTYPE_USING_TYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
   * Runs the storage location query in order to retrieve the seleected columns
   * that describe the file.
   * 
   * @param contextUCon the connection context
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public PropertyBean getFileInfo(ContextUCon contextUCon) throws ExInternal {
        try {
            NameValuePair resultObjs[] = contextUCon.getUCon().selectOneRowWithColumnNames(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            GenericPropertyBean fileBean = new GenericPropertyBean();
            for (int n = 0; n < resultObjs.length; n++) {
                fileBean.addProperty(resultObjs[n].getName(), resultObjs[n].getValue());
            }
            return fileBean;
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
    }

    /**
   * Returns a Datum from the file lob
   * 
   * @param contextUCon the connection context
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public Datum getLOB(ContextUCon contextUCon) throws ExInternal {
        try {
            UCon lUCon = contextUCon.getUConOrNull(ContextUCon.THREAD_TRANSACION);
            if (lUCon == null) throw new ExInternal("ContextUCon passed to getLOB needs a THREAD_TRANSACTION defined.");
            Object resultObjs[] = lUCon.selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            return getLOBFromResultArray(resultObjs);
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
    }

    private Datum getLOBFromResultArray(Object[] pResultArray) {
        Object lLOB = null;
        for (int n = 0; n < pResultArray.length && lLOB == null; n++) {
            if (pResultArray[n] instanceof CLOB || pResultArray[n] instanceof BLOB) lLOB = pResultArray[n];
        }
        if (lLOB == null) {
            throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
        }
        if (lLOB instanceof CLOB) {
            return (CLOB) lLOB;
        } else if (lLOB instanceof BLOB) {
            return (BLOB) lLOB;
        } else {
            throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
        }
    }

    /**
   * Opens a stream to the file at this location and streams the file to the specified
   * <code>OutputStream</code>.
   * 
   * @param os the output stream where the file will be written.
   * @param contextUCon the connection context
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public void streamFile(OutputStream os, ContextUCon contextUCon) throws ExInternal {
        try {
            Object resultObjs[] = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            Object lob = null;
            for (int n = 0; n < resultObjs.length && lob == null; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[n] instanceof BLOB) lob = resultObjs[n];
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            InputStream fileIS;
            if (lob instanceof CLOB) {
                CLOB clob = (CLOB) lob;
                fileIS = clob.getAsciiStream();
            } else if (lob instanceof BLOB) {
                BLOB blob = (BLOB) lob;
                fileIS = blob.getBinaryStream();
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
            IOUtil.transfer(fileIS, os, 64000);
            fileIS.close();
            contextUCon.getUCon().commit();
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (IOException ex) {
            throw new ExInternal("Unexpected error occurred during download of file storage location - see nested exception for " + "further information.", mStorageLocation.getXML(), ex);
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB download - see nested exception for " + "further information.", ex);
        } catch (ExServiceUnavailable ex) {
            ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
    }

    public StreamParcelInput getStreamParcelInput(String pFileName, String pPath, String pFileType, UCon pUCon) throws ExInternal {
        StreamParcelInput lStreamParcelInput = null;
        try {
            NameValuePair resultObjs[] = pUCon.selectOneRowWithColumnNames(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            Object lob = null;
            String lContentType = pFileType;
            for (int n = 0; n < resultObjs.length; n++) {
                if (resultObjs[n].getValue() instanceof CLOB || resultObjs[n].getValue() instanceof BLOB) {
                    if (lob == null) {
                        lob = resultObjs[n].getValue();
                    }
                } else if ("CONTENT_TYPE".equals(resultObjs[n].getName())) {
                    lContentType = XFUtil.nvl((String) resultObjs[n].getValue(), lContentType);
                }
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            Object[] lSize = new Object[] { String.class, lob };
            pUCon.executeCall(LOB_SIZE_QUERY, lSize, new char[] { 'O', 'I' });
            if (lob instanceof CLOB) {
                CLOB lClob = (CLOB) lob;
                lStreamParcelInput = new StreamParcelInputCLOB(pFileName, pPath, lContentType, lClob, (String) lSize[0]);
            } else if (lob instanceof BLOB) {
                BLOB lBlob = (BLOB) lob;
                lStreamParcelInput = new StreamParcelInputBLOB(pFileName, pPath, lContentType, lBlob, (String) lSize[0]);
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
        return lStreamParcelInput;
    }

    /**
   * Returns an InputStream for this storage location
   * 
   * @param pUCon the connection to use
   * @throws ExInternal 
   */
    public InputStream getInputStream(UCon pUCon) throws ExInternal {
        InputStream fileIS;
        try {
            Object resultObjs[] = pUCon.selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            Object lob = null;
            for (int n = 0; n < resultObjs.length && lob == null; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[n] instanceof BLOB) lob = resultObjs[n];
            }
            if (lob == null) {
                throw new ExInternal("The file storage location query, \"" + mStorageLocation.getQueryStatement() + "\" must return a CLOB or BLOB column type.");
            }
            if (lob instanceof CLOB) {
                CLOB clob = (CLOB) lob;
                String lDBCharacterSet = (String) ORACLE_TO_JAVA_CHARSETS.get(new Short(clob.getConnection().getPhysicalConnection().getDbCsId()));
                if (lDBCharacterSet == null) {
                    throw new ExInternal("Database character set not supported");
                }
                StringBuffer lBuf = new StringBuffer();
                try {
                    InputStreamReader lReader2 = new InputStreamReader(clob.binaryStreamValue(), lDBCharacterSet);
                    Reader in = new BufferedReader(lReader2);
                    int ch;
                    while ((ch = in.read()) > -1) {
                        lBuf.append((char) ch);
                    }
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                fileIS = null;
                try {
                    fileIS = new ByteArrayInputStream(lBuf.toString().getBytes("UTF-16"));
                } catch (UnsupportedEncodingException e) {
                }
            } else if (lob instanceof BLOB) {
                BLOB blob = (BLOB) lob;
                fileIS = blob.getBinaryStream();
            } else {
                throw new ExInternal("Error in file storage location, \"" + mStorageLocation.getName() + "\" - the query returned a type that was not a CLOB or BLOB!");
            }
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (SQLException e) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB download - see nested exception for " + "further information.", e);
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
        return fileIS;
    }

    /**
   * Opens a stream to the first LOB type retrieved by the query and
   * send the LOB data over the specified output stream.
   * 
   * @param os the output stream where the LOB will be written.
   * @param contextUCon the connection context
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public void streamFirstLOC(OutputStream os, ContextUCon contextUCon) throws ExInternal {
        try {
            Object resultObjs[] = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            int lobCol = -1;
            for (int n = 0; n < resultObjs.length && lobCol < 0; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[0] instanceof BLOB) {
                    lobCol = n;
                }
            }
            if (lobCol < 0) {
                throw new ExInternal("No LOB column returned from storage location query, \"" + mStorageLocation.getQueryStatement() + "\" - expecting a CLOB or BLOB column in query result set!");
            }
            InputStream fileIS = null;
            if (resultObjs[lobCol] instanceof CLOB) {
                CLOB clob = (CLOB) resultObjs[lobCol];
                fileIS = clob.getAsciiStream();
            } else if (resultObjs[lobCol] instanceof BLOB) {
                BLOB blob = (BLOB) resultObjs[lobCol];
                fileIS = blob.getBinaryStream();
            }
            IOUtil.transfer(fileIS, os, 64000);
            fileIS.close();
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (IOException ex) {
            throw new ExInternal("Unexpected error occurred during download of file storage location - see nested exception for " + "further information.", mStorageLocation.getXML(), ex);
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB download - see nested exception for " + "further information.", ex);
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
    }

    /**
   * Opens a stream to the first LOB type retrieved by the query and
   * send the LOB data over the specified output stream.
   * 
   * @param response the response containing the stream where the LOB will be written.
   * @param contentType the content type of the data.
   * @param contextUCon the connection context
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public void streamFirstLOC(HttpServletResponse response, String contentType, ContextUCon contextUCon) throws ExInternal {
        OutputStream os = null;
        try {
            Object resultObjs[] = contextUCon.getUCon().selectOneRow(mStorageLocation.getQueryStatement(), mEvaluatedQueryStrVals);
            int lobCol = -1;
            for (int n = 0; n < resultObjs.length && lobCol < 0; n++) {
                if (resultObjs[n] instanceof CLOB || resultObjs[0] instanceof BLOB) {
                    lobCol = n;
                }
            }
            if (lobCol < 0) {
                throw new ExInternal("No LOB column returned from storage location query, \"" + mStorageLocation.getQueryStatement() + "\" - expecting a CLOB or BLOB column in query result set!");
            }
            InputStream fileIS = null;
            long contentLength = -1;
            if (resultObjs[lobCol] instanceof CLOB) {
                CLOB clob = (CLOB) resultObjs[lobCol];
                contentLength = clob.length();
                fileIS = clob.getAsciiStream();
            } else if (resultObjs[lobCol] instanceof BLOB) {
                BLOB blob = (BLOB) resultObjs[lobCol];
                contentLength = blob.length();
                fileIS = blob.getBinaryStream();
            }
            response.setContentType(contentType);
            if (contentLength >= 0) {
                response.setContentLength((int) contentLength);
            }
            os = response.getOutputStream();
            IOUtil.transfer(fileIS, os, 64000);
            fileIS.close();
        } catch (ExDBTooMany ex) {
            throw ex.toUnexpected();
        } catch (IOException ex) {
            throw new ExInternal("Unexpected error occurred during download of file storage location - see nested exception for " + "further information.", mStorageLocation.getXML(), ex);
        } catch (SQLException ex) {
            throw new ExInternal("Unexpected SQL exception during file storage location CLOB/BLOB download - see nested exception for " + "further information.", ex);
        } catch (ExRoot ex) {
            throw ex.toUnexpected();
        }
    }

    /**
   * Converts from an array of tuples (name/value pairs) to an
   * array containing the values elements only.
   * 
   * @param tuplesArray the array containing the name/value pairs.
   * @return an array containing only the value elements
   * @throws ExInternal if an unexpected or system I/O error occurs during the read/write.
   */
    public String[] convertTuplesArrayToValuesArray(String[] tuplesArray) throws ExInternal {
        if (tuplesArray == null) return null;
        if (tuplesArray.length % 1 == 1) {
            throw new ExInternal("Error attempting to convert tuple array of name/value pairs to an array of values only - the input array " + "contains an odd number of elements (" + tuplesArray.length + ")!");
        }
        String result[] = new String[tuplesArray.length / 2];
        for (int n = 1; n < tuplesArray.length; n += 2) {
            result[n / 2] = tuplesArray[n];
        }
        return result;
    }

    public void fireUploadEvent(BLOB pTempBlob, UCon pUCon, UploadInfo pUploadInfo) throws ExRoot {
        if (mStorageLocation.getAPICallScript() == null) return;
        Object bindVals[] = convertBindTupleVals(mEvaluatedAPIStrVals, pTempBlob, pUCon, pUploadInfo);
        pUCon.executeDML(mStorageLocation.getAPICallScript(), bindVals);
        freeTemporaryLOBS(bindVals, pTempBlob);
    }

    public void fireUploadEvent(File uploadedFile, ContextUCon contextUCon, FileUploadInfo fileUploadInfo) throws ExRoot {
        if (mStorageLocation.getAPICallScript() == null) return;
        Object bindVals[] = convertBindTupleVals(mEvaluatedAPIStrVals, contextUCon.getUCon(), uploadedFile, fileUploadInfo);
        contextUCon.getUCon().executeDML(mStorageLocation.getAPICallScript(), bindVals);
        freeTemporaryLOBS(bindVals);
    }
}
