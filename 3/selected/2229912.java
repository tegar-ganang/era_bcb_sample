package test.be.fedict.eid.applet.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.ejb3.annotation.LocalBinding;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.SignatureService;

@Stateless
@Local(SignatureService.class)
@LocalBinding(jndiBinding = "test/eid/applet/model/SignatureServiceBean")
public class SignatureServiceBean implements SignatureService {

    private static final Log LOG = LogFactory.getLog(SignatureServiceBean.class);

    public void postSign(byte[] signatureValue, List<X509Certificate> signingCertificateChain) {
        LOG.debug("postSign");
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        String signatureValueStr = new String(Hex.encodeHex(signatureValue));
        HttpSession session = httpServletRequest.getSession();
        session.setAttribute("SignatureValue", signatureValueStr);
        session.setAttribute("SigningCertificateChain", signingCertificateChain);
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain) throws NoSuchAlgorithmException {
        LOG.debug("preSign");
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        HttpSession session = httpServletRequest.getSession();
        String toBeSigned = (String) session.getAttribute("toBeSigned");
        String digestAlgo = (String) session.getAttribute("digestAlgo");
        LOG.debug("digest algo: " + digestAlgo);
        MessageDigest messageDigest = MessageDigest.getInstance(digestAlgo, new BouncyCastleProvider());
        byte[] digestValue = messageDigest.digest(toBeSigned.getBytes());
        String description = "Test Text Document";
        return new DigestInfo(digestValue, digestAlgo, description);
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }
}
