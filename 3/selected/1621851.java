package test.be.fedict.eid.applet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.SignatureService;

/**
 * Test for classname based service implementation.
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureServiceImpl implements SignatureService {

    private static final Log LOG = LogFactory.getLog(SignatureServiceImpl.class);

    static {
        if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void postSign(byte[] signatureValue, List<X509Certificate> signingCertificateChain) {
        LOG.debug("postSign");
        String signatureValueStr = new String(Hex.encodeHex(signatureValue));
        HttpSession session = getHttpSession();
        session.setAttribute("SignatureValue", signatureValueStr);
        session.setAttribute("SigningCertificateChain", signingCertificateChain);
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain) throws NoSuchAlgorithmException {
        LOG.debug("preSign");
        HttpSession session = getHttpSession();
        String toBeSigned = (String) session.getAttribute("toBeSigned");
        String digestAlgo = (String) session.getAttribute("digestAlgo");
        LOG.debug("digest algo: " + digestAlgo);
        String javaDigestAlgo = digestAlgo;
        if (digestAlgo.endsWith("-PSS")) {
            LOG.debug("RSA/PSS detected");
            javaDigestAlgo = digestAlgo.substring(0, digestAlgo.indexOf("-PSS"));
            LOG.debug("java digest algo: " + javaDigestAlgo);
        }
        MessageDigest messageDigest = MessageDigest.getInstance(javaDigestAlgo, new BouncyCastleProvider());
        byte[] digestValue = messageDigest.digest(toBeSigned.getBytes());
        String description = "Test Text Document";
        return new DigestInfo(digestValue, digestAlgo, description);
    }

    private HttpSession getHttpSession() {
        HttpServletRequest httpServletRequest;
        try {
            httpServletRequest = (HttpServletRequest) PolicyContext.getContext("javax.servlet.http.HttpServletRequest");
        } catch (PolicyContextException e) {
            throw new RuntimeException("JACC error: " + e.getMessage());
        }
        HttpSession session = httpServletRequest.getSession();
        return session;
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }
}
