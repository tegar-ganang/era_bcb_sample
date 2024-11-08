package org.pwsafe.core.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import org.pwsafe.lib.I18nHelper;
import org.pwsafe.lib.Log;
import org.pwsafe.lib.Util;
import org.pwsafe.lib.crypto.HmacPws;
import org.pwsafe.lib.crypto.SHA256Pws;
import org.pwsafe.lib.crypto.TwofishPws;
import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;

/**
 * Encapsulates version 3 PasswordSafe files.
 * 
 * @author Glen Smith (based on Kevin Preece's v2 implementation).
 */
public class PwsFileV3 extends PwsFile {

    private static final Log LOG = Log.getInstance(PwsFileV3.class.getPackage().getName());

    /**
	 * The PasswordSafe database version number that this class supports.
	 */
    public static final int VERSION = 3;

    /**
	 * The string that identifies a database as V2 rather than V1
	 */
    public static final byte[] ID_STRING = "PWS3".getBytes();

    /**
	 * The file's standard header.
	 */
    protected PwsFileHeaderV3 headerV3;

    /**
	 * End of File marker. HMAC follows this tag.
	 */
    static byte[] EOF_BYTES_RAW = "PWS3-EOFPWS3-EOF".getBytes();

    protected byte[] stretchedPassword;

    protected byte[] decryptedRecordKey;

    protected byte[] decryptedHmacKey;

    TwofishPws twofishCbc;

    HmacPws hasher;

    PwsRecordV3 headerRecord;

    /**
	 * Constructs and initialises a new, empty version 3 PasswordSafe database in memory.
	 */
    public PwsFileV3() {
        super();
        headerV3 = new PwsFileHeaderV3();
        headerRecord = new PwsRecordV3();
        headerRecord.setField(new PwsVersionField(0, new byte[] { 1, 3 }));
    }

    /**
	 * Use of this constructor to load a PasswordSafe database is STRONGLY discouraged
	 * since it's use ties the caller to a particular file version.  Use {@link
	 * PwsFileFactory#loadFile(String, String)} instead.
	 * </p><p>
	 * <b>N.B. </b>this constructor's visibility may be reduced in future releases.
	 * </p>
	 * @param filename   the name of the database to open.
	 * @param passphrase the passphrase for the database.
	 * 
	 * @throws EndOfFileException
	 * @throws IOException
	 * @throws UnsupportedFileVersionException
	 * @throws NoSuchAlgorithmException 
	 */
    public PwsFileV3(PwsStorage storage, String passphrase) throws EndOfFileException, IOException, UnsupportedFileVersionException, NoSuchAlgorithmException {
        super(storage, passphrase);
    }

    protected void open(String passphrase) throws EndOfFileException, IOException, UnsupportedFileVersionException {
        LOG.enterMethod("PwsFileV3.init");
        Passphrase = passphrase;
        if (storage != null) {
            InStream = new ByteArrayInputStream(storage.load());
        }
        headerV3 = new PwsFileHeaderV3(this);
        int iter = Util.getIntFromByteArray(headerV3.getIter(), 0);
        LOG.debug1("Using iterations: [" + iter + "]");
        stretchedPassword = stretchPassphrase(passphrase.getBytes(), headerV3.getSalt(), iter);
        if (!Util.bytesAreEqual(headerV3.getPassword(), SHA256Pws.digest(stretchedPassword))) {
            throw new IOException("Invalid password");
        }
        try {
            byte[] rka = TwofishPws.processECB(stretchedPassword, false, headerV3.getB1());
            byte[] rkb = TwofishPws.processECB(stretchedPassword, false, headerV3.getB2());
            decryptedRecordKey = Util.mergeBytes(rka, rkb);
            byte[] hka = TwofishPws.processECB(stretchedPassword, false, headerV3.getB3());
            byte[] hkb = TwofishPws.processECB(stretchedPassword, false, headerV3.getB4());
            decryptedHmacKey = Util.mergeBytes(hka, hkb);
            hasher = new HmacPws(decryptedHmacKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error reading encrypted fields");
        }
        twofishCbc = new TwofishPws(decryptedRecordKey, false, headerV3.getIV());
        readExtraHeader(this);
        LOG.leaveMethod("PwsFileV3.init");
    }

    /**
	 * Writes this file back to the filesystem.  If successful the modified flag is also 
	 * reset on the file and all records.
	 * 
	 * @throws IOException if the attempt fails.
	 */
    public void save() throws IOException {
        PwsRecordV3 rec;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutStream = baos;
        try {
            headerV3.save(this);
            twofishCbc = new TwofishPws(decryptedRecordKey, true, headerV3.getIV());
            writeExtraHeader(this);
            for (Iterator iter = getRecords(); iter.hasNext(); ) {
                rec = (PwsRecordV3) iter.next();
                if (!rec.isHeaderRecord()) rec.saveRecord(this);
            }
            OutStream.write(PwsRecordV3.EOF_BYTES_RAW);
            OutStream.write(hasher.doFinal());
            OutStream.close();
            if (storage.save(baos.toByteArray())) {
                Modified = false;
            } else {
                LOG.error(I18nHelper.getInstance().formatMessage("E00010", new Object[] { "Storage" }));
                return;
            }
        } catch (IOException e) {
            try {
                OutStream.close();
            } catch (Exception e2) {
            }
            throw e;
        } finally {
            OutStream = null;
        }
    }

    /**
	 * Calculate stretched key.
	 * 
	 * http://www.schneier.com/paper-low-entropy.pdf (Section 4.1), 
	 * with SHA-256 as the hash function, and ITER iterations 
	 * (at least 2048, i.e., t = 11).
	 * @param passphrase the user entered passphrase
	 * @param salt the salt from the file
	 * @param iter the number of iters from the file
	 * @return the stretched user key for comparison
	 */
    static byte[] stretchPassphrase(byte[] passphrase, byte[] salt, int iter) {
        byte[] p = Util.mergeBytes(passphrase, salt);
        byte[] hash = SHA256Pws.digest(p);
        for (int i = 0; i < iter; i++) {
            hash = SHA256Pws.digest(hash);
        }
        return hash;
    }

    /**
	 * Returns the major version number for the file.
	 * 
	 * @return The major version number for the file.
	 */
    public int getFileVersionMajor() {
        return VERSION;
    }

    /**
	 * Allocates a new, empty record unowned by any file.  The record type is
	 * {@link PwsRecordV2}.
	 * 
	 * @return A new empty record
	 * 
	 * @see org.pwsafe.lib.file.PwsFile#newRecord()
	 */
    public PwsRecord newRecord() {
        return new PwsRecordV3();
    }

    /**
	 * Reads the extra header present in version 2 files.
	 * 
	 * @param file the file to read the header from.
	 * 
	 * @throws EndOfFileException If end of file is reached.
	 * @throws IOException If an error occurs whilst reading. 
	 * @throws UnsupportedFileVersionException If the header is not a valid V2 header.
	 */
    protected void readExtraHeader(PwsFile file) throws EndOfFileException, IOException, UnsupportedFileVersionException {
        headerRecord = new PwsRecordV3(this, true);
    }

    /**
	 * Writes the extra version 2 header.
	 * 
	 * @param file the file to write the header to.
	 * 
	 * @throws IOException if an error occurs whilst writing the header. 
	 */
    protected void writeExtraHeader(PwsFile file) throws IOException {
        headerRecord.saveRecord(this);
    }

    /**
	 * Reads bytes from the file and decrypts them.  <code>buff</code> may be any length provided
	 * that is a multiple of <code>BLOCK_LENGTH</code> bytes in length.
	 * 
	 * @param buff the buffer to read the bytes into.
	 * 
	 * @throws EndOfFileException If end of file has been reached.
	 * @throws IOException If a read error occurs.
	 * @throws IllegalArgumentException If <code>buff.length</code> is not an integral multiple of <code>BLOCK_LENGTH</code>.
	 */
    public void readDecryptedBytes(byte[] buff) throws EndOfFileException, IOException {
        if ((buff.length == 0) || ((buff.length % getBlockSize()) != 0)) {
            throw new IllegalArgumentException(I18nHelper.getInstance().formatMessage("E00001"));
        }
        readBytes(buff);
        if (Util.bytesAreEqual(buff, EOF_BYTES_RAW)) {
            throw new EndOfFileException();
        }
        byte[] decrypted;
        try {
            decrypted = twofishCbc.processCBC(buff);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error decrypting field");
        }
        Util.copyBytes(decrypted, buff);
    }

    /**
	 * Encrypts then writes the contents of <code>buff</code> to the file.
	 * 
	 * @param buff the data to be written.
	 * 
	 * @throws IOException
	 */
    public void writeEncryptedBytes(byte[] buff) throws IOException {
        if ((buff.length == 0) || ((buff.length % getBlockSize()) != 0)) {
            throw new IllegalArgumentException(I18nHelper.getInstance().formatMessage("E00001"));
        }
        byte[] temp;
        try {
            temp = twofishCbc.processCBC(buff);
        } catch (Exception e) {
            throw new IOException("Error writing encrypted field");
        }
        writeBytes(temp);
    }

    /**
	 * @see org.pwsafe.lib.file.PwsFile#getBlockSize()
	 */
    protected int getBlockSize() {
        return 16;
    }
}
