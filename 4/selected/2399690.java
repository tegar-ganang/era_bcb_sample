package sun.security.jgss.krb5;

import com.sun.security.jgss.AuthorizationDataEntry;
import org.ietf.jgss.*;
import java.io.InputStream;
import java.io.IOException;
import sun.security.krb5.*;
import java.net.InetAddress;
import sun.security.krb5.internal.AuthorizationData;
import sun.security.krb5.internal.KerberosTime;

class InitSecContextToken extends InitialToken {

    private KrbApReq apReq = null;

    /**
     * For the context initiator to call. It constructs a new
     * InitSecContextToken to send over to the peer containing the desired
     * flags and the AP-REQ. It also updates the context with the local
     * sequence number and shared context key.
     * (When mutual auth is enabled the peer has an opportunity to
     * renegotiate the session key in the followup AcceptSecContextToken
     * that it sends.)
     */
    InitSecContextToken(Krb5Context context, Credentials tgt, Credentials serviceTicket) throws KrbException, IOException, GSSException {
        boolean mutualRequired = context.getMutualAuthState();
        boolean useSubkey = true;
        boolean useSequenceNumber = true;
        OverloadedChecksum gssChecksum = new OverloadedChecksum(context, tgt, serviceTicket);
        Checksum checksum = gssChecksum.getChecksum();
        context.setTktFlags(serviceTicket.getFlags());
        context.setAuthTime(new KerberosTime(serviceTicket.getAuthTime()).toString());
        apReq = new KrbApReq(serviceTicket, mutualRequired, useSubkey, useSequenceNumber, checksum);
        context.resetMySequenceNumber(apReq.getSeqNumber().intValue());
        EncryptionKey subKey = apReq.getSubKey();
        if (subKey != null) context.setKey(subKey); else context.setKey(serviceTicket.getSessionKey());
        if (!mutualRequired) context.resetPeerSequenceNumber(0);
    }

    /**
     * For the context acceptor to call. It reads the bytes out of an
     * InputStream and constructs an InitSecContextToken with them.
     */
    InitSecContextToken(Krb5Context context, EncryptionKey[] keys, InputStream is) throws IOException, GSSException, KrbException {
        int tokenId = ((is.read() << 8) | is.read());
        if (tokenId != Krb5Token.AP_REQ_ID) throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1, "AP_REQ token id does not match!");
        byte[] apReqBytes = new sun.security.util.DerValue(is).toByteArray();
        InetAddress addr = null;
        if (context.getChannelBinding() != null) {
            addr = context.getChannelBinding().getInitiatorAddress();
        }
        apReq = new KrbApReq(apReqBytes, keys, addr);
        EncryptionKey sessionKey = apReq.getCreds().getSessionKey();
        EncryptionKey subKey = apReq.getSubKey();
        if (subKey != null) {
            context.setKey(subKey);
        } else {
            context.setKey(sessionKey);
        }
        OverloadedChecksum gssChecksum = new OverloadedChecksum(context, apReq.getChecksum(), sessionKey);
        gssChecksum.setContextFlags(context);
        Credentials delegCred = gssChecksum.getDelegatedCreds();
        if (delegCred != null) {
            Krb5CredElement credElement = Krb5InitCredential.getInstance((Krb5NameElement) context.getSrcName(), delegCred);
            context.setDelegCred(credElement);
        }
        Integer apReqSeqNumber = apReq.getSeqNumber();
        int peerSeqNumber = (apReqSeqNumber != null ? apReqSeqNumber.intValue() : 0);
        context.resetPeerSequenceNumber(peerSeqNumber);
        if (!context.getMutualAuthState()) context.resetMySequenceNumber(peerSeqNumber);
        context.setAuthTime(new KerberosTime(apReq.getCreds().getAuthTime()).toString());
        context.setTktFlags(apReq.getCreds().getFlags());
        AuthorizationData ad = apReq.getCreds().getAuthzData();
        if (ad == null) {
            context.setAuthzData(null);
        } else {
            AuthorizationDataEntry[] authzData = new AuthorizationDataEntry[ad.count()];
            for (int i = 0; i < ad.count(); i++) {
                authzData[i] = new AuthorizationDataEntry(ad.item(i).adType, ad.item(i).adData);
            }
            context.setAuthzData(authzData);
        }
    }

    public final KrbApReq getKrbApReq() {
        return apReq;
    }

    public final byte[] encode() throws IOException {
        byte[] apReqBytes = apReq.getMessage();
        byte[] retVal = new byte[2 + apReqBytes.length];
        writeInt(Krb5Token.AP_REQ_ID, retVal, 0);
        System.arraycopy(apReqBytes, 0, retVal, 2, apReqBytes.length);
        return retVal;
    }
}
