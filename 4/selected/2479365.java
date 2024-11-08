package com.google.code.p.keytooliui.ktl.util.jarsigner;

import java.nio.ByteBuffer;
import java.security.cert.CertificateExpiredException;
import javax.security.cert.CertificateNotYetValidException;
import com.google.code.p.keytooliui.ktl.swing.dialog.*;
import com.google.code.p.keytooliui.shared.lang.*;
import com.google.code.p.keytooliui.shared.swing.optionpane.*;
import com.google.code.p.keytooliui.shared.util.jarsigner.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.awt.*;
import java.io.*;
import java.util.*;

public abstract class KTLKprOpenSigDetOutAbs extends KTLKprOpenSigDetAbs {

    private static final String _STR_PROVIDERSIGINSTANCE = KTLAbs.f_s_strProviderKstBC;

    protected abstract KeyStore _getKeystoreOpen_(File fleOpen);

    protected abstract boolean _doJobSelectKpr_(File fleSaveSig, File fleSaveCrt, File fleOpenData, KeyStore kstOpen, String[] strsAliasPKTC, Boolean[] boosIsTCEntryPKTC, Boolean[] boosValidDatePKTC, Boolean[] boosSelfSignedCertPKTC, Boolean[] boosTrustedCertPKTC, String[] strsSizeKeyPublPKTC, String[] strsTypeCertPKTC, String[] strsAlgoSigCertPKTC, Date[] dtesLastModifiedPKTC, String[] strsAliasSK, Date[] dtesLastModifiedSK);

    /**
    if any error in code, exiting
        in case of trbrl: open up a warning dialog, and return false;
        
        algo:
        . get fileOpen keystore
        . get fileSave crt (MEMO: check for files prior to open up keypair selection dialog)
        . open keystore
        . !! if no entry in keystore, show warning dialog, then return false
        . WRONG !!!!!!!!!! if no entry of type "RSA-self-signed" in keystore, show warning dialog, then return false
        . fill in table keypair
        . WRONG !!!!!!!!!  show dialog keypair select RSA self, to get:
          . alias keypair
          . password keypair
        . get private key
        . get first X509 cert in cert chain
        . generate CRT string from cert and private key
        
        . write crt string to fileSave
    **/
    public boolean doJob() {
        String strMethod = "doJob()";
        super._setEnabledCursorWait_(true);
        if (super._strProviderKst_ == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutExit(this, strMethod, "nil super._strProviderKst_");
        }
        if (this._strFormatFileSig_ == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutExit(this, strMethod, "nil this._strFormatFileSig_");
        }
        File fleOpenKst = UtilJsrFile.s_getFileOpen(super._frmOwner_, super._strPathAbsKst_);
        if (fleOpenKst == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutError(this, strMethod, "nil fleOpenKst");
            return false;
        }
        if (super._strPathAbsFileData_ == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutExit(this, strMethod, "nil super._strPathAbsFileData_");
        }
        File fleOpenData = UtilJsrFile.s_getFileOpen(super._frmOwner_, super._strPathAbsFileData_);
        if (fleOpenData == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutWarning(this, strMethod, "nil fleOpenData");
            return false;
        }
        if (super._strPathAbsFileSig_ == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutExit(this, strMethod, "nil super._strPathAbsFileSig_");
        }
        File fleSaveSig = UtilJsrFile.s_getFileSave(super._frmOwner_, super._strPathAbsFileSig_, true);
        if (fleSaveSig == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutWarning(this, strMethod, "nil fleSaveSig");
            return false;
        }
        File fleSaveCrt = null;
        if (super._strPathAbsFileSaveCrt_ != null) {
            fleSaveCrt = UtilJsrFile.s_getFileSave(super._frmOwner_, super._strPathAbsFileSaveCrt_, true);
            if (fleSaveCrt == null) {
                super._setEnabledCursorWait_(false);
                MySystem.s_printOutWarning(this, strMethod, "nil fleSaveCrt");
                return false;
            }
        }
        if (super._chrsPasswdKst_ == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutExit(this, strMethod, "nil super._chrsPasswdKst_");
        }
        KeyStore kstOpen = _getKeystoreOpen_(fleOpenKst);
        if (kstOpen == null) {
            super._setEnabledCursorWait_(false);
            MySystem.s_printOutError(this, strMethod, "nil kstOpen");
            return false;
        }
        String[] strsAliasPKTC = UtilKstAbs.s_getStrsAliasPKTC(super._frmOwner_, kstOpen);
        if (strsAliasPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil strsAliasPKTC");
        }
        String[] strsAliasSK = UtilKstAbs.s_getStrsAliasSK(super._frmOwner_, kstOpen);
        if (strsAliasSK == null) {
            MySystem.s_printOutExit(strMethod, "nil strsAliasPKTC");
        }
        Boolean[] boosIsTCEntryPKTC = UtilKstAbs.s_getBoosEntryTcr(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (boosIsTCEntryPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil boosIsTCEntryPKTC");
        }
        Boolean[] boosValidDatePKTC = UtilKstAbs.s_getBoosValidDatePKTC(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (boosValidDatePKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil boosValidDatePKTC");
        }
        Boolean[] boosSelfSignedCertPKTC = UtilKstAbs.s_getBoosSelfSigned(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (boosSelfSignedCertPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil boosSelfSignedCertPKTC");
        }
        Boolean[] boosTrustedCertPKTC = UtilKstAbs.s_getBoosTrusted(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (boosTrustedCertPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil boosTrustedCertPKTC");
        }
        String[] strsSizeKeyPublPKTC = UtilKstAbs.s_getStrsSizeKeyPubl(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (strsSizeKeyPublPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil strsSizeKeyPublPKTC");
        }
        String[] strsTypeCertPKTC = UtilKstAbs.s_getStrsTypeCertificatePKTC(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (strsTypeCertPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil strsTypeCertPKTC");
        }
        String[] strsAlgoSigCertPKTC = UtilKstAbs.s_getStrsAlgoSigCertPKTC(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (strsAlgoSigCertPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil strsAlgoSigCertPKTC");
        }
        Date[] dtesLastModifiedPKTC = UtilKstAbs.s_getDtesLastModified(super._frmOwner_, kstOpen, strsAliasPKTC);
        if (dtesLastModifiedPKTC == null) {
            MySystem.s_printOutExit(strMethod, "nil dtesLastModifiedPKTC");
        }
        Date[] dtesLastModifiedSK = UtilKstAbs.s_getDtesLastModified(super._frmOwner_, kstOpen, strsAliasSK);
        if (dtesLastModifiedSK == null) {
            MySystem.s_printOutExit(strMethod, "nil dtesLastModifiedPKTC");
        }
        super._setEnabledCursorWait_(false);
        if (!_doJobSelectKpr_(fleSaveSig, fleSaveCrt, fleOpenData, kstOpen, strsAliasPKTC, boosIsTCEntryPKTC, boosValidDatePKTC, boosSelfSignedCertPKTC, boosTrustedCertPKTC, strsSizeKeyPublPKTC, strsTypeCertPKTC, strsAlgoSigCertPKTC, dtesLastModifiedPKTC, strsAliasSK, dtesLastModifiedSK)) {
            MySystem.s_printOutTrace(this, strMethod, "either aborted or failed");
            return false;
        }
        return true;
    }

    /**
    if any error in code, exiting
        in case of trbrl: open up a warning dialog, and return false;
        
        algo:
        . get fileOpen keystore
        . get fileSave crt (MEMO: check for files prior to open up keypair selection dialog)
        . open keystore
        . !! if no entry in keystore, show warning dialog, then return false
        . WRONG LINE !! if no entry of type "RSA-self-signed" in keystore, show warning dialog, then return false
        . fill in table keypair
        . WRONG LINE show dialog keypair select RSA self, to get:
          . alias keypair
          . password keypair (if not from pkcs12 kst)
        . get private key
        . get first X509 cert in cert chain
        . generate CRT string from cert and private key
        
        . write crt string to fileSave
    **/
    protected boolean _doJob_(KeyStore kstOpen, String strAliasKpr, char[] chrsPasswdKpr, File fleSaveSig, File fleSaveCrt, File fleOpenData) {
        String strMethod = "_doJob_(kstOpen, strAliasKpr, chrsPasswdKpr, fleSaveSig, fleSaveCrt, fleOpenData)";
        if (kstOpen == null || strAliasKpr == null || chrsPasswdKpr == null || fleSaveSig == null || fleOpenData == null) MySystem.s_printOutExit(this, strMethod, "nil arg");
        Key key = UtilKstAbs.s_getKey(super._frmOwner_, kstOpen, strAliasKpr, chrsPasswdKpr);
        if (key == null) {
            MySystem.s_printOutError(this, strMethod, "nil key");
            return false;
        }
        PrivateKey pkyPrivate = (PrivateKey) key;
        X509Certificate crtX509FirstInChain = KTLKprOpenCrtAbs.s_getCertX509FirstInChain(kstOpen, strAliasKpr);
        if (crtX509FirstInChain == null) {
            MySystem.s_printOutError(this, strMethod, "nil crtX509FirstInChain");
            MySystem.s_printOutError(this, strMethod, "failed");
            String strBody = "Failed to get first in chain X509 certificate.";
            strBody += "\n\n" + "More: see your session.log";
            OPAbstract.s_showDialogError(super._frmOwner_, strBody);
            return false;
        }
        try {
            crtX509FirstInChain.checkValidity();
        } catch (CertificateExpiredException excCertificateExpired) {
            MySystem.s_printOutWarning(this, strMethod, "excCertificateExpired caught");
            String strBody = "Certificate has expired!";
            strBody += "\n\n" + "Continue anyway?";
            if (!OPAbstract.s_showWarningConfirmDialog(super._frmOwner_, strBody)) {
                MySystem.s_printOutTrace(this, strMethod, "aborted by user");
                return false;
            }
        } catch (java.security.cert.CertificateNotYetValidException excCertificateNotYetValid) {
            MySystem.s_printOutWarning(this, strMethod, "excCertificateNotYetValid caught");
            String strBody = "Certificate not yet valid!";
            strBody += "\n\n" + "Continue anyway?";
            if (!OPAbstract.s_showWarningConfirmDialog(super._frmOwner_, strBody)) {
                MySystem.s_printOutTrace(this, strMethod, "aborted by user");
                return false;
            }
        }
        String strCrtSigAlgo = crtX509FirstInChain.getSigAlgName();
        if (!_doSignFile(fleSaveSig, fleOpenData, pkyPrivate, strAliasKpr, strCrtSigAlgo)) {
            MySystem.s_printOutError(this, strMethod, "failed");
            return false;
        }
        boolean blnDoneWriteCrt = false;
        if (fleSaveCrt != null) {
            blnDoneWriteCrt = _doWriteCrt(pkyPrivate, fleSaveCrt, crtX509FirstInChain);
        }
        String strBody = "File successfully signed with private key aliased:";
        strBody += "\n  " + strAliasKpr;
        strBody += "\n\nSignature saved in:";
        strBody += "\n  " + fleSaveSig.getAbsolutePath();
        if (fleSaveCrt != null) {
            strBody += "\n\n";
            if (blnDoneWriteCrt) strBody += "Certificate (containing public key) saved"; else strBody += "Failed to save certificate";
            strBody += " in:";
            strBody += "\n  ";
            strBody += super._strPathAbsFileSaveCrt_;
        }
        OPAbstract.s_showDialogInfo(super._frmOwner_, strBody);
        return true;
    }

    private Boolean[] _getBoosElligible(Boolean[] boosEntryKpr, String[] strsAlgoKeyPubl, Boolean[] boosTypeCertX509, String[] strsAlgoSigCert) {
        String strMethod = "_getBoosElligible(...)";
        if (boosEntryKpr == null || strsAlgoKeyPubl == null || boosTypeCertX509 == null) MySystem.s_printOutExit(this, strMethod, "nil arg");
        Boolean[] boosElligible = new Boolean[boosEntryKpr.length];
        for (int i = 0; i < boosEntryKpr.length; i++) {
            boolean blnOk = true;
            if (boosEntryKpr[i].booleanValue() == false) {
                blnOk = false;
            } else if (strsAlgoKeyPubl[i].toLowerCase().compareTo(KTLAbs.f_s_strTypeKeypairRsa.toLowerCase()) != 0 && strsAlgoKeyPubl[i].toLowerCase().compareTo(KTLAbs.f_s_strTypeKeypairDsa.toLowerCase()) != 0) {
                blnOk = false;
            } else if (boosTypeCertX509[i].booleanValue() == false) {
                blnOk = false;
            }
            boosElligible[i] = new Boolean(blnOk);
        }
        return boosElligible;
    }

    protected KTLKprOpenSigDetOutAbs(Frame frmOwner, String strPathAbsOpenKst, char[] chrsPasswdOpenKst, String strPathAbsFileOpenData, String strPathAbsFileSaveSig, String strPathAbsFileSaveCrt, String strFormatFileSig, String strFormatFileSaveCrt, String strProviderKst) {
        super(frmOwner, strPathAbsOpenKst, chrsPasswdOpenKst, strPathAbsFileOpenData, strPathAbsFileSaveSig, strPathAbsFileSaveCrt, strProviderKst);
        this._strFormatFileSig_ = strFormatFileSig;
        this._strFormatFileSaveCrt_ = strFormatFileSaveCrt;
        if (this._strFormatFileSaveCrt_ != null) {
            if (this._strFormatFileSaveCrt_.toLowerCase().compareTo(KTLAbs.f_s_strFormatFileCrtPem.toLowerCase()) == 0) this._blnFormatCrtPrintable = true;
        }
    }

    private String _strFormatFileSig_ = null;

    private String _strFormatFileSaveCrt_ = null;

    private boolean _blnFormatCrtPrintable = false;

    private byte[] _read(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        java.nio.channels.FileChannel fcl = fis.getChannel();
        byte[] byrsData = new byte[(int) fcl.size()];
        ByteBuffer bbr = ByteBuffer.wrap(byrsData);
        fcl.read(bbr);
        return byrsData;
    }

    /**
     * MEMO: from 2SE API 1.6
     * There are three phases to the use of a Signature object for either signing data or verifying a signature:

   1. Initialization, with either
          * a public key, which initializes the signature for verification (see initVerify), or
          * a private key (and optionally a Secure Random Number Generator), which initializes the signature for signing (see initSign(PrivateKey) and initSign(PrivateKey, SecureRandom)). 

   2. Updating

      Depending on the type of initialization, this will update the bytes to be signed or verified. See the update methods.

   3. Signing or Verifying a signature on all updated bytes. See the sign methods and the verify method. 
     **/
    private boolean _doSignFile(File fleSaveSig, File fleOpenData, PrivateKey pkyPrivate, String strAlias, String strCrtSigAlgo) {
        byte[] bytsSignature = null;
        try {
            byte[] bytsData = _read(fleOpenData);
            bytsSignature = _signFile(strCrtSigAlgo, bytsData, pkyPrivate);
            if (bytsSignature == null) {
                String strBody = "Failed to sign file!";
                OPAbstract.s_showDialogWarning(super._frmOwner_, strBody);
                return false;
            }
            KTLKprOpenSigDetOutAbs._s_byteToFile(bytsSignature, fleSaveSig);
        } catch (Exception exc) {
            exc.printStackTrace();
            String strBody = "Failed to sign file!";
            strBody += "\n\n" + "Exception caught:";
            strBody += "\n  " + exc.getMessage();
            OPAbstract.s_showDialogWarning(super._frmOwner_, strBody);
            return false;
        }
        return true;
    }

    private static void _s_byteToFile(byte[] bytes, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStream out = new BufferedOutputStream(fos);
        out.write(bytes);
        out.close();
    }

    private byte[] _signFile(String strCrtSigAlgo, byte[] bytsData, PrivateKey pkyPrivate) throws Exception {
        java.security.Signature signature = java.security.Signature.getInstance(strCrtSigAlgo, KTLKprOpenSigDetOutAbs._STR_PROVIDERSIGINSTANCE);
        signature.initSign(pkyPrivate);
        signature.update(bytsData);
        return signature.sign();
    }

    private boolean _writeCrtAscii(X509Certificate crt, File fleSaveCrt) {
        String strMethod = "_writeCrtAscii(crt, fleSaveCrt)";
        String str = null;
        if (this._strFormatFileSaveCrt_.toLowerCase().compareTo(KTLAbs.f_s_strFormatFileCrtPem.toLowerCase()) == 0) {
            str = UtilCrtX509Pem.s_generateCrt(super._frmOwner_, crt);
            if (str == null) {
                MySystem.s_printOutError(this, strMethod, "nil str");
                return false;
            }
        } else {
            MySystem.s_printOutExit(this, strMethod, "DEV CODE ERROR, uncaught ascii certificate format, super._strFormatFileSveCrt_=" + this._strFormatFileSaveCrt_);
        }
        FileWriter fwrSaveCrt = null;
        try {
            fwrSaveCrt = new FileWriter(fleSaveCrt);
            fwrSaveCrt.write(str);
            fwrSaveCrt.close();
            fwrSaveCrt = null;
        } catch (IOException excIO) {
            excIO.printStackTrace();
            MySystem.s_printOutWarning(this, strMethod, "excIO caught");
            String strBody = "Got an IO exception while attempting to write certificate file:";
            strBody += "\n";
            strBody += "  ";
            strBody += fleSaveCrt.getAbsolutePath();
            OPAbstract.s_showDialogWarning(super._frmOwner_, strBody);
            return false;
        }
        return true;
    }

    private boolean _writeCrtBinary(X509Certificate crtX509FirstInChain, PrivateKey pkyPrivate, File fleSaveCrt) {
        String strMethod = "_writeCrtBinary(...)";
        byte[] bytsCrt = null;
        if (this._strFormatFileSaveCrt_.toLowerCase().compareTo(KTLAbs.f_s_strFormatFileCrtPkcs7.toLowerCase()) == 0) {
            bytsCrt = UtilCrtX509Pkcs7.s_generateCrt(super._frmOwner_, crtX509FirstInChain, KTLKprOpenSigDetOutAbs._STR_PROVIDERSIGINSTANCE);
        } else if (this._strFormatFileSaveCrt_.toLowerCase().compareTo(KTLAbs.f_s_strFormatFileCrtDer.toLowerCase()) == 0) {
            bytsCrt = UtilCrtX509Der.s_generateCrt(super._frmOwner_, crtX509FirstInChain, KTLKprOpenSigDetOutAbs._STR_PROVIDERSIGINSTANCE);
        } else {
            MySystem.s_printOutExit(this, strMethod, "DEV CODE ERROR: uncaught format, this._strFormatFileSaveCrt_=" + this._strFormatFileSaveCrt_);
        }
        if (bytsCrt == null) {
            MySystem.s_printOutError(this, strMethod, "nil bytsCrt");
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(fleSaveCrt);
            fos.write(bytsCrt);
            fos.close();
            fos = null;
        } catch (Exception exc) {
            exc.printStackTrace();
            MySystem.s_printOutError(this, strMethod, "exc caught");
            String strBody = "Got an IO exception while attempting to write certificate file:";
            strBody += "\n";
            strBody += "  ";
            strBody += fleSaveCrt.getAbsolutePath();
            OPAbstract.s_showDialogWarning(super._frmOwner_, strBody);
            return false;
        }
        return true;
    }

    private boolean _doWriteCrt(PrivateKey pkyPrivate, File fleSaveCrt, X509Certificate crtX509FirstInChain) {
        String strMethod = "_doWriteCrt(pkyPrivate, fleSaveCrt, crtX509FirstInChain)";
        if (pkyPrivate == null || fleSaveCrt == null || crtX509FirstInChain == null) MySystem.s_printOutExit(this, strMethod, "nil arg");
        if (this._blnFormatCrtPrintable) {
            if (!_writeCrtAscii(crtX509FirstInChain, fleSaveCrt)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                return false;
            }
        } else {
            if (!_writeCrtBinary(crtX509FirstInChain, pkyPrivate, fleSaveCrt)) {
                MySystem.s_printOutError(this, strMethod, "failed");
                return false;
            }
        }
        return true;
    }
}
