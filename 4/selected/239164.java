package issrg.SAWS;

import issrg.SAWS.callback.SAWSGUICallbackHandler;
import issrg.SAWS.callback.SAWSTextOutputCallback;
import issrg.SAWS.util.SAWSLogWriter;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.io.File;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

/**
 *
 * @author W.Xu
 */
public class TCBContentRW {

    private String trustedLocationLocal;

    private SecretKey secretKey;

    private PBEParameterSpec paramSpec = null;

    private String lastFilename;

    private int lastSN;

    private byte[] lastAccHash;

    private int debugLevel = 0;

    private CallbackHandler callbackHandler = new SAWSGUICallbackHandler();

    /**
     * @aggregation composite
     */
    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(TCBContentRW.class.getName());

    public TCBContentRW(String trustedLocationL, SecretKey secretKeyL, PBEParameterSpec param, int debugLevel, CallbackHandler ch) {
        trustedLocationLocal = trustedLocationL;
        secretKey = secretKeyL;
        paramSpec = param;
        this.debugLevel = debugLevel;
        this.setCallbackHandler(ch);
    }

    /**
     * Method that sets the callback handler for the class. If the handler
     * is null, the class will keep using the default callback handler.
     * 
     * @param ch The callback handler.
     */
    public void setCallbackHandler(CallbackHandler ch) {
        if (ch != null) {
            this.callbackHandler = ch;
        }
    }

    public TCBContentRW(String lastFilename, int SN, byte[] hash, int debugLevel, CallbackHandler ch) {
        lastFilename = lastFilename;
        lastSN = SN;
        lastAccHash = hash;
        this.debugLevel = debugLevel;
        this.setCallbackHandler(ch);
    }

    public int write() {
        File fileTemp = new File(trustedLocationLocal);
        byte[] fileBytes = null;
        int result = 0;
        byte[] asn1Block = generateASN1Block();
        if (asn1Block == null) {
            return -1;
        }
        try {
            if (!fileTemp.exists()) {
                fileTemp.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(fileTemp, "rw");
            raf.setLength(0);
            try {
                Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
                fileBytes = cipher.doFinal(asn1Block);
                raf.write(fileBytes);
            } catch (Exception e) {
                this.showMessage("Using secretKey to write TCB failed", SAWSTextOutputCallback.ERROR);
                if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e + "\nUsing secretKey to write TCB failed");
                result = -1;
            }
            raf.close();
        } catch (Exception e2) {
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e2);
            result = -1;
        }
        return result;
    }

    /**
   * This method is for reading the TCBLocation
   */
    public int read() {
        File fileTemp = new File(trustedLocationLocal);
        int fileLength = (int) fileTemp.length();
        byte[] fileBytes = new byte[fileLength];
        byte[] decryptedBytes = null;
        byte[] block = null;
        int result = 0;
        if (fileTemp.exists()) {
            try {
                RandomAccessFile raf = new RandomAccessFile(fileTemp, "rw");
                int length = raf.read(fileBytes);
                try {
                    Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
                    block = cipher.doFinal(fileBytes);
                } catch (Exception e) {
                    this.showMessage("Using secretKey to read TCB failed", SAWSTextOutputCallback.ERROR);
                    if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e + "\nUsing secretKey to read TCB failed");
                    result = -1;
                }
                raf.close();
            } catch (Exception e) {
                if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e);
                result = -1;
            }
            if (result != -1) {
                result = extractASN1Block(block);
            }
        }
        return result;
    }

    public void setLastFilename(String Filename) {
        lastFilename = Filename;
    }

    public void setLastSN(int SN) {
        lastSN = SN;
    }

    public void setLastAccHash(byte[] hash) {
        lastAccHash = hash;
    }

    public void setTCBContent(String Filename, int SN, byte[] hash) {
        lastFilename = Filename;
        lastSN = SN;
        lastAccHash = hash;
    }

    public String getLastFilename() {
        return lastFilename;
    }

    public int getLastSN() {
        return lastSN;
    }

    public byte[] getLastAccHash() {
        return lastAccHash;
    }

    public byte[] generateASN1Block() {
        byte[] arrayASN = null;
        ASN1EncodableVector vector = new ASN1EncodableVector();
        DERSequence ASN1Seq = null;
        try {
            vector.add(new DERIA5String(lastFilename));
            vector.add(new DERInteger(lastSN));
            vector.add(new DEROctetString(lastAccHash));
            ASN1Seq = new DERSequence(vector);
        } catch (Exception e) {
            e.printStackTrace();
            this.showMessage("ASN1 doesn't work! Generating ASN1 block failed!", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e + "\nASN1 doesn't work! Generating ASN1 block failed!");
            return null;
        }
        arrayASN = ASN1Seq.getDEREncoded();
        return arrayASN;
    }

    public int extractASN1Block(byte[] asn1Block) {
        try {
            DERSequence seq = (DERSequence) DERSequence.fromByteArray(asn1Block);
            DERIA5String s0 = (DERIA5String) seq.getObjectAt(0);
            lastFilename = s0.getString();
            DERInteger i1 = (DERInteger) seq.getObjectAt(1);
            lastSN = i1.getValue().intValue();
            DEROctetString b = (DEROctetString) seq.getObjectAt(2);
            lastAccHash = b.getOctets();
        } catch (Exception e) {
            this.showMessage("ASN1 doesn't work! Reading ASN1 block failed!", SAWSTextOutputCallback.ERROR);
            if (debugLevel >= SAWSConstant.ErrorInfo) sawsDebugLog.write(e + "\nASN1 doesn't work! Reading ASN1 block failed!");
            return -1;
        }
        return 0;
    }

    /**
     * Method to create the callback (SAWSTextOutputCallback) with the message to be 
     * presented to the user and send it to the callback handler.
     * 
     * @param message The message to be presented.
     * @param type The type of the message (SAWSTextOutputCallback.WARNING, 
     * SAWSTextOutputCallback.ERROR, SAWSTextOutputCallback.INFORMATION)
     */
    private void showMessage(String message, int type) {
        Callback[] cbs;
        cbs = new Callback[1];
        cbs[0] = new SAWSTextOutputCallback(type, message);
        try {
            this.callbackHandler.handle(cbs);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            sawsDebugLog.write(e);
        }
    }
}
