package com.google.code.p.keytooliui.ktl.util.jarsigner;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import com.google.code.p.keytooliui.shared.lang.MySystem;
import com.google.code.p.keytooliui.shared.swing.optionpane.OPAbstract;

public class CmsVerif extends CmsAbs {

    private static final String _STR_KST_PROVIDER_BC = "BC";

    public CmsVerif(Frame frmOwner, String strPathAbsFileData, String strPathAbsFileSig) {
        super(frmOwner, strPathAbsFileData, strPathAbsFileSig);
    }

    public boolean doJob() {
        String strMethod = "doJob()";
        try {
            CMSSignedData cms = _getSignPkcs7();
            SignerInformationStore sis = cms.getSignerInfos();
            Collection colSignerInfo = sis.getSigners();
            Iterator itrSignerInfo = colSignerInfo.iterator();
            SignerInformation sin = (SignerInformation) itrSignerInfo.next();
            CertStore cse = cms.getCertificatesAndCRLs("Collection", CmsVerif._STR_KST_PROVIDER_BC);
            Iterator itrCert = cse.getCertificates(sin.getSID()).iterator();
            X509Certificate crt = (X509Certificate) itrCert.next();
            boolean blnCoreValidity = sin.verify(crt, CmsVerif._STR_KST_PROVIDER_BC);
            if (blnCoreValidity) {
                MySystem.s_printOutTrace(this, strMethod, "blnCoreValidity=true");
                String strBody = "CMS Detached signature is OK!";
                strBody += "\n\n" + ". CMS signature file location:";
                strBody += "\n  " + super._strPathAbsFileSig_;
                strBody += "\n\n" + ". Data file location:";
                strBody += "\n  " + super._strPathAbsFileData_;
                OPAbstract.s_showDialogInfo(super._frmOwner_, strBody);
                SignerId sid = sin.getSID();
                if (sid != null) {
                    System.out.println("sid.getSerialNumber()=" + sid.getSerialNumber());
                    System.out.println("sid.getIssuerAsString()=" + sid.getIssuerAsString());
                    System.out.println("sid.getSubjectAsString()=" + sid.getSubjectAsString());
                }
            } else {
                MySystem.s_printOutWarning(this, strMethod, "blnCoreValidity=true");
                String strBody = "CMS Detached signature is WRONG!";
                strBody += "\n\n" + ". CMS signature file location:";
                strBody += "\n  " + super._strPathAbsFileSig_;
                strBody += "\n\n" + ". Data file location:";
                strBody += "\n  " + super._strPathAbsFileData_;
                OPAbstract.s_showDialogWarning(super._frmOwner_, strBody);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            MySystem.s_printOutError(this, strMethod, "exc caught");
            String strBody = "Failed to verify CMS detached signature.";
            strBody += "\n\n" + "Possible reason: wrong data file";
            strBody += "\n\n" + "got exception.";
            strBody += "\n" + exc.getMessage();
            strBody += "\n\n" + "More: see your session.log";
            OPAbstract.s_showDialogError(super._frmOwner_, strBody);
            return false;
        }
        return true;
    }

    private CMSSignedData _getSignPkcs7() throws Exception {
        File fleDoc = new File(super._strPathAbsFileData_);
        byte[] bytsDoc = _read(fleDoc);
        File fleSigCmsPkcs7 = new File(super._strPathAbsFileSig_);
        byte[] bytsSigCmsPkcs7 = _read(fleSigCmsPkcs7);
        CMSProcessable cmdProcDoc = new CMSProcessableByteArray(bytsDoc);
        CMSSignedData cms = new CMSSignedData(cmdProcDoc, bytsSigCmsPkcs7);
        return cms;
    }

    private byte[] _read(File fle) throws Exception {
        FileInputStream fis = new FileInputStream(fle);
        java.nio.channels.FileChannel fcl = fis.getChannel();
        byte[] byts = new byte[(int) fcl.size()];
        ByteBuffer bbr = ByteBuffer.wrap(byts);
        fcl.read(bbr);
        return byts;
    }
}
