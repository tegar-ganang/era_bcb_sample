package cryptix.sasl.srp;

import cryptix.sasl.Base64;
import cryptix.sasl.ClientMechanism;
import cryptix.sasl.ConfidentialityException;
import cryptix.sasl.IllegalMechanismStateException;
import cryptix.sasl.InputBuffer;
import cryptix.sasl.IntegrityException;
import cryptix.sasl.OutputBuffer;
import cryptix.sasl.SaslParams;
import cryptix.sasl.SaslUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;
import java.util.StringTokenizer;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslClientExt;
import javax.security.sasl.SaslException;
import org.apache.log4j.Category;

/**
 * The Cryptix implementation of an SM2-capable client-side SASL-SRP mechanism.
 *
 * @version $Revision: 1.13 $
 * @since draft-burdis-cat-sasl-srp-04
 * @since draft-naffah-cat-sasl-sm2-00
 */
public class SRPClient extends ClientMechanism implements SaslClientExt, SRPParams, SaslParams {

    private static Category cat = Category.getInstance(SRPClient.class);

    private String U;

    private byte[] salt;

    private char[] password;

    private byte[] M1;

    private String L;

    private String o;

    private String chosenIntegrityAlgorithm;

    private String chosenConfidentialityAlgorithm;

    private int rawSendSize = BUFFER_LIMIT;

    private KeyPair kp;

    private BigInteger A, N, g;

    private SRP srp;

    private SecretKey K;

    private boolean replayDetection = true;

    private int inCounter = 0;

    private int outCounter = 0;

    private IALG inMac, outMac;

    private CALG inCipher, outCipher;

    public SRPClient(String mdName, String authorizationID, String protocol, String serverName, Map props, CallbackHandler cbh) {
        super(SRP_MECHANISM + "-" + (mdName.equals("") ? SRP_DEFAULT_DIGEST_NAME : mdName), authorizationID, protocol, serverName, props, cbh);
        srp = SRP.instance(mdName);
    }

    public AlgorithmParameterSpec getSaslSecurityContext() throws SaslException {
        if (!complete) throw new IllegalMechanismStateException("getSaslSecurityContext()");
        return new SRPContext(K, replayDetection, inCounter, outCounter, inMac, outMac, inCipher, outCipher);
    }

    public void setSaslSecurityContext(AlgorithmParameterSpec ctx) throws SaslException {
        SRPContext ssc = (SRPContext) ctx;
        K = ssc.getK();
        replayDetection = ssc.hasReplayDetection();
        inCounter = ssc.getIncomingCounter();
        outCounter = ssc.getOutgoingCounter();
        inMac = ssc.getIncomingMac();
        outMac = ssc.getOutgoingMac();
        inCipher = ssc.getIncomingCipher();
        outCipher = ssc.getOutgoingCipher();
        complete = true;
    }

    public byte[] evaluateEvidence(byte[] peerEvidence) throws SaslException {
        byte[] key = K.getEncoded();
        int t = key[0];
        for (int i = 0; i < key.length - 1; i++) key[i] = key[i + 1];
        key[key.length - 1] = (byte) t;
        K = new SRPSecretKey(key);
        return ((SRPContext) getSaslSecurityContext()).getSignature();
    }

    public boolean hasInitialResponse() {
        return true;
    }

    public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
        switch(state) {
            case 0:
                state++;
                return sendIdentity();
            case 1:
                state++;
                return sendRequest(challenge);
            case 2:
                state++;
                return sendEvidence(challenge);
            case 3:
                if (chosenConfidentialityAlgorithm == null) {
                    state++;
                    return verifyEvidence(challenge);
                }
            default:
                throw new IllegalMechanismStateException("evaluateChallenge()");
        }
    }

    protected byte[] engineUnwrap(byte[] incoming, int offset, int len) throws SaslException {
        cat.debug("==> engineUnwrap()");
        byte[] data = null;
        try {
            InputBuffer frameIn = InputBuffer.getInstance(incoming, offset, len);
            data = frameIn.getEOS();
            if (inMac != null) {
                byte[] received_mac = frameIn.getOS();
                cat.debug("Got C (received MAC): " + SaslUtil.dumpString(received_mac));
                inMac.update(data);
                if (replayDetection) {
                    inCounter++;
                    cat.debug("inCounter=" + String.valueOf(inCounter));
                    inMac.update(new byte[] { (byte) (inCounter >>> 24), (byte) (inCounter >>> 16), (byte) (inCounter >>> 8), (byte) inCounter });
                }
                byte[] computed_mac = inMac.doFinal();
                cat.debug("Computed MAC: " + SaslUtil.dumpString(computed_mac));
                if (!SaslUtil.areEqual(received_mac, computed_mac)) throw new IntegrityException("engineUnwrap()");
            }
            if (inCipher != null) data = inCipher.doFinal(data);
        } catch (IOException x) {
            cat.debug("engineUnwrap()", x);
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("engineUnwrap()", x);
        }
        cat.debug("<== engineUnwrap()");
        return data;
    }

    protected byte[] engineWrap(byte[] outgoing, int offset, int len) throws SaslException {
        cat.debug("==> engineWrap()");
        byte[] data = new byte[len];
        System.arraycopy(outgoing, offset, data, 0, len);
        byte[] result = null;
        try {
            OutputBuffer frameOut = new OutputBuffer();
            if (outCipher != null) {
                data = outCipher.doFinal(data);
                cat.debug("Encoding c (encrypted plaintext): " + SaslUtil.dumpString(data));
            } else cat.debug("Encoding p (plaintext): " + SaslUtil.dumpString(data));
            frameOut.setEOS(data);
            if (outMac != null) {
                outMac.update(data);
                if (replayDetection) {
                    outCounter++;
                    cat.debug("outCounter=" + String.valueOf(outCounter));
                    outMac.update(new byte[] { (byte) (outCounter >>> 24), (byte) (outCounter >>> 16), (byte) (outCounter >>> 8), (byte) outCounter });
                }
                byte[] C = outMac.doFinal();
                frameOut.setOS(C);
                cat.debug("Encoding C (integrity checksum): " + SaslUtil.dumpString(C));
            }
            result = frameOut.wrap();
        } catch (IOException x) {
            cat.debug("engineWrap()", x);
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("engineWrap()", x);
        }
        cat.debug("<== engineWrap()");
        return result;
    }

    public void dispose() throws SaslException {
        password = null;
        M1 = null;
        kp = null;
        A = N = g = null;
        K = null;
        inMac = outMac = null;
        inCipher = outCipher = null;
    }

    protected String getNegotiatedQOP() {
        if (inMac != null) if (inCipher != null) return "auth-conf"; else return "auth-int";
        return "auth";
    }

    protected String getNegotiatedStrength() {
        return "high";
    }

    protected String getNegotiatedRawSendSize() {
        return String.valueOf(rawSendSize);
    }

    private byte[] sendIdentity() throws SaslException {
        cat.debug("==> sendIdentity()");
        getUsernameAndPassword();
        cat.debug("Password: \"" + new String(password) + "\"");
        cat.debug("Encoding U (username): \"" + U + "\"");
        OutputBuffer frameOut = new OutputBuffer();
        try {
            frameOut.setText(U);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendIdentity()", x);
        }
        byte[] result = frameOut.encode();
        cat.debug("<== sendIdentity() --> " + SaslUtil.dumpString(result));
        cat.info("C: " + Base64.encode(result));
        cat.info("  U = " + U);
        return result;
    }

    private byte[] sendRequest(byte[] input) throws SaslException {
        cat.debug("==> sendRequest()");
        cat.debug("challenge: " + SaslUtil.dumpString(input));
        InputBuffer frameIn = new InputBuffer(input);
        try {
            N = frameIn.getMPI();
            cat.debug("Got N (modulus): " + SaslUtil.dump(N));
            g = frameIn.getMPI();
            cat.debug("Got g (generator): " + SaslUtil.dump(g));
            salt = frameIn.getOS();
            cat.debug("Got s (salt): " + SaslUtil.dumpString(salt));
            L = frameIn.getText();
            cat.info("Got L (available options): \"" + L + "\"");
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendRequest()", x);
        }
        cat.debug("Encoding I (authorisation identity): \"" + this.authorizationID + "\"");
        cat.debug("Generating ephemeral public keypair...");
        kp = srp.generateClientKeyPair(N, g);
        A = ((SRPPublicKey) kp.getPublic()).getExponent();
        cat.debug("Encoding A (client's ephemeral public key): " + SaslUtil.dump(A));
        o = createOptionsList(L.toLowerCase());
        cat.debug("Encoding o (client's options list): \"" + o + "\"");
        OutputBuffer frameOut = new OutputBuffer();
        try {
            frameOut.setMPI(A);
            frameOut.setText(this.authorizationID);
            frameOut.setText(o);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendRequest()", x);
        }
        byte[] result = frameOut.encode();
        cat.debug("<== sendRequest() --> " + SaslUtil.dumpString(result));
        cat.info("C: " + Base64.encode(result));
        cat.info("  A = " + A.toString());
        cat.info("  I = " + this.authorizationID);
        cat.info("  o = " + o);
        return result;
    }

    private byte[] sendEvidence(byte[] input) throws SaslException {
        cat.debug("==> sendEvidence()");
        cat.debug("challenge: " + SaslUtil.dumpString(input));
        InputBuffer frameIn = new InputBuffer(input);
        BigInteger B;
        try {
            B = frameIn.getMPI();
            cat.debug("Got B (server's ephermeral public key): " + SaslUtil.dump(B));
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendEvidence()", x);
        }
        MessageDigest ctxt = srp.newDigest();
        ctxt.update(salt);
        ctxt.update(srp.userHash(U, new String(password)));
        BigInteger _x = new BigInteger(1, ctxt.digest());
        cat.debug("x: " + SaslUtil.dump(_x));
        K = srp.generateClientSecretKey(kp, B, _x);
        byte[] k = K.getEncoded();
        M1 = srp.generateClientEvidence(N, g, U, salt, A, B, k, L);
        cat.debug("Encoding M1 (client's evidence): " + SaslUtil.dumpString(M1));
        OutputBuffer frameOut = new OutputBuffer();
        try {
            frameOut.setOS(M1);
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendEvidence()", x);
        }
        byte[] result = frameOut.encode();
        if (chosenConfidentialityAlgorithm != null) setupSecurityServices();
        cat.debug("<== sendEvidence() --> " + SaslUtil.dumpString(result));
        cat.info("C: " + Base64.encode(result));
        return result;
    }

    private byte[] verifyEvidence(byte[] input) throws SaslException {
        cat.debug("==> verifyEvidence()");
        cat.debug("challenge: " + SaslUtil.dumpString(input));
        InputBuffer frameIn = new InputBuffer(input);
        byte[] M2;
        try {
            M2 = frameIn.getOS();
            cat.debug("Got M2 (server's evidence): " + SaslUtil.dumpString(M2));
        } catch (IOException x) {
            if (x instanceof SaslException) throw (SaslException) x;
            throw new SaslException("sendEvidence()", x);
        }
        byte[] expected = srp.generateServerEvidence(A, M1, K.getEncoded(), U, authorizationID, o);
        cat.debug("Expected: " + SaslUtil.dumpString(expected));
        if (!SaslUtil.areEqual(M2, expected)) throw new SaslException("Verification of server's evidence failed");
        setupSecurityServices();
        return null;
    }

    private void getUsernameAndPassword() throws SaslException {
        try {
            if ((!properties.containsKey(USERNAME)) && (!properties.containsKey(PASSWORD))) {
                NameCallback nameCB;
                String defaultName = System.getProperty("user.name");
                if (defaultName == null) nameCB = new NameCallback("username: "); else nameCB = new NameCallback("username: ", defaultName);
                PasswordCallback pwdCB = new PasswordCallback("password: ", false);
                handler.invokeCallback(new Callback[] { nameCB, pwdCB });
                U = nameCB.getName();
                password = pwdCB.getPassword();
            } else {
                if (properties.containsKey(USERNAME)) this.U = (String) properties.get(USERNAME); else {
                    NameCallback nameCB;
                    String defaultName = System.getProperty("user.name");
                    if (defaultName == null) nameCB = new NameCallback("username: "); else nameCB = new NameCallback("username: ", defaultName);
                    this.handler.invokeCallback(new Callback[] { nameCB });
                    this.U = nameCB.getName();
                }
                if (properties.containsKey(PASSWORD)) this.password = ((String) properties.get(PASSWORD)).toCharArray(); else {
                    PasswordCallback pwdCB = new PasswordCallback("password: ", false);
                    this.handler.invokeCallback(new Callback[] { pwdCB });
                    this.password = pwdCB.getPassword();
                }
            }
            if (U == null) throw new SaslException("null username supplied");
            if (password == null) throw new SaslException("null password supplied");
        } catch (UnsupportedCallbackException x) {
            cat.error("getUsernameAndPassword()", x);
            throw new SaslException("getUsernameAndPassword()", x);
        } catch (IOException x) {
            cat.error("getUsernameAndPassword()", x);
            throw new SaslException("getUsernameAndPassword()", x);
        }
    }

    private String createOptionsList(String availableOptionsList) throws SaslException {
        cat.debug("==> createOptionsList(\"" + availableOptionsList + "\")");
        boolean replaydetectionAvailable = false;
        boolean integrityAvailable = false;
        boolean confidentialityAvailable = false;
        String option, mandatory = DEFAULT_MANDATORY;
        int i;
        StringTokenizer st = new StringTokenizer(availableOptionsList, ",");
        while (st.hasMoreTokens()) {
            option = st.nextToken();
            if (option.equals("replay detection")) replaydetectionAvailable = true; else if (option.startsWith("integrity=")) {
                option = option.substring(option.indexOf('=') + 1);
                cat.debug("algorithm: <" + option + ">");
                for (i = 0; i < INTEGRITY_ALGORITHMS.length; i++) if (INTEGRITY_ALGORITHMS[i].equals(option)) {
                    chosenIntegrityAlgorithm = option;
                    integrityAvailable = true;
                    break;
                }
            } else if (option.startsWith("confidentiality=")) {
                option = option.substring(option.indexOf('=') + 1);
                cat.debug("algorithm: <" + option + ">");
                for (i = 0; i < CONFIDENTIALITY_ALGORITHMS.length; i++) if (CONFIDENTIALITY_ALGORITHMS[i].equals(option)) {
                    chosenConfidentialityAlgorithm = option;
                    confidentialityAvailable = true;
                    break;
                }
            } else if (option.startsWith("mandatory=")) mandatory = option.substring(option.indexOf('=') + 1); else if (option.startsWith("maxbuffersize=")) {
                String maxBufferSize = option.substring(option.indexOf('=') + 1);
                try {
                    rawSendSize = Integer.parseInt(maxBufferSize);
                    if (rawSendSize > BUFFER_LIMIT || rawSendSize < 1) throw new SaslException("Illegal value for 'maxbuffersize' option");
                } catch (NumberFormatException x) {
                    throw new SaslException("maxbuffersize=" + String.valueOf(maxBufferSize), x);
                }
            }
        }
        replayDetection = replaydetectionAvailable && Boolean.valueOf((String) properties.get(SRP_REPLAY_DETECTION)).booleanValue();
        boolean integrity = integrityAvailable && Boolean.valueOf((String) properties.get(SRP_INTEGRITY_PROTECTION)).booleanValue();
        boolean confidentiality = confidentialityAvailable && Boolean.valueOf((String) properties.get(SRP_CONFIDENTIALITY)).booleanValue();
        if (MANDATORY_REPLAY_DETECTION.equals(mandatory)) {
            replayDetection = true;
            integrity = true;
        } else if (MANDATORY_INTEGRITY.equals(mandatory)) integrity = true; else if (MANDATORY_CONFIDENTIALITY.equals(mandatory)) confidentiality = true;
        if (replayDetection) if (chosenIntegrityAlgorithm == null) throw new SaslException("Replay detection is required but no " + "integrity protection algorithm was chosen"); else ;
        if (integrity) if (chosenIntegrityAlgorithm == null) throw new SaslException("Integrity protection is required but no " + "algorithm was chosen"); else ;
        if (confidentiality) if (chosenConfidentialityAlgorithm == null) throw new SaslException("Confidentiality protection is required " + "but no algorithm was chosen"); else ;
        StringBuffer sb = new StringBuffer();
        if (replayDetection) sb.append("replay detection,");
        if (integrity) sb.append("integrity=").append(chosenIntegrityAlgorithm).append(",");
        if (confidentiality) sb.append("confidentiality=").append(chosenConfidentialityAlgorithm).append(",");
        String result = sb.append("maxbuffersize=").append(BUFFER_LIMIT).toString();
        cat.debug("<== createOptionsList() --> " + result);
        return result;
    }

    private void setupSecurityServices() throws SaslException {
        complete = true;
        outCounter = inCounter = 0;
        if (chosenIntegrityAlgorithm != null) {
            cat.info("Activating integrity protection filter");
            inMac = IALG.getInstance(chosenIntegrityAlgorithm, K);
            outMac = IALG.getInstance(chosenIntegrityAlgorithm, K);
        }
        if (chosenConfidentialityAlgorithm != null) {
            cat.info("Activating confidentiality protection filter");
            inCipher = CALG.getInstance(chosenConfidentialityAlgorithm, K, CALG.DECRYPT);
            outCipher = CALG.getInstance(chosenConfidentialityAlgorithm, K, CALG.ENCRYPT);
        }
    }
}
