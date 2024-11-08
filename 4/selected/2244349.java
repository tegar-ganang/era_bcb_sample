package icm.unicore.plugins.laj;

import org.unicore.*;
import org.unicore.ajo.*;
import org.unicore.outcome.*;
import org.unicore.upl.*;
import org.unicore.utility.*;
import org.unicore.sets.*;
import com.pallas.unicore.security.AjoSigner;
import com.pallas.unicore.resourcemanager.*;
import com.pallas.unicore.connection.*;
import java.io.*;
import java.util.zip.*;
import java.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

/**
 *@author     Krzysztof Benedyczak
 *@created    2003
 */
public class UPLWorker {

    private static final int WAIT_TIME = 2000;

    private Vsite vsite;

    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;

    private Socket socket;

    UPLWorker(Vsite vsite) throws javax.net.ssl.SSLPeerUnverifiedException, java.io.IOException, java.io.StreamCorruptedException, java.net.ConnectException {
        this.vsite = vsite;
        Vsite.Reference vRef = vsite.getReference();
        UnicoreSSLSocketFactory factory = new UnicoreSSLSocketFactory(ResourceManager.getUser());
        socket = factory.createSocket(vRef.getAddress());
        socket.setSoTimeout(60000);
        X509Certificate[] serverCert = ((SSLSocket) socket).getSession().getPeerCertificateChain();
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public void close() {
        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
        }
    }

    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    public Reply sendAJ(AbstractJob aj, boolean polling) throws java.io.InvalidClassException, java.io.NotSerializableException, java.io.IOException, java.security.SignatureException, java.lang.ClassNotFoundException {
        AbstractJob aj_signed = null;
        AJOIdentifier id = aj.getAJOId();
        aj_signed = ConsignForm.convertTo(aj, AjoSigner.getSigner(ResourceManager.getUser()));
        ConsignJob cj = new ConsignJob(id, vsite, aj.getEndorser(), aj_signed.getConsignForm(), aj_signed.getSignature(), null, false, polling);
        outputStream.writeObject(cj);
        outputStream.flush();
        return (Reply) inputStream.readObject();
    }

    private RetrieveOutcomeReply getAJReply(AJOIdentifier id) throws java.io.InvalidClassException, java.io.NotSerializableException, java.io.IOException, java.security.SignatureException, java.lang.ClassNotFoundException {
        RetrieveOutcome ro = new RetrieveOutcome(id, vsite);
        outputStream.writeObject(ro);
        outputStream.flush();
        RetrieveOutcomeReply reply = (RetrieveOutcomeReply) inputStream.readObject();
        UnicoreResponse response = reply.getLastEntry();
        if (response.getReturnCode() != 0) {
            System.out.println("receiving not ok " + response.getComment());
            throw new IOException(response.getComment());
        }
        return reply;
    }

    public static boolean waitForState(AbstractActionStatus status, AAIdentifier id, Vsite vsite, long timeout) throws java.io.InvalidClassException, java.io.NotSerializableException, java.io.IOException, java.security.SignatureException, java.lang.ClassNotFoundException {
        AbstractActionStatus current;
        AbstractJob getStatus = new AbstractJob("JobGetStatus" + id.toString(), null, null, vsite, ResourceManager.getUser());
        GetActionStatus gas = new GetActionStatus("GetStatus" + id.toString(), id);
        getStatus.add(gas);
        UPLWorker uplW = new UPLWorker(vsite);
        long elapsed = 0;
        do {
            RetrieveOutcomeReply rep = (RetrieveOutcomeReply) uplW.sendAJ(getStatus, false);
            AbstractJob_Outcome aj_o = deserialize(rep);
            OutcomeEnumeration oe = aj_o.getOutcomes();
            GetActionStatus_Outcome gas_o = (GetActionStatus_Outcome) oe.nextElement();
            current = gas_o.getActionStatus();
            if (current.isEquivalent(AbstractActionStatus.DONE) && !status.isEquivalent(current)) return false;
            if (status.isEquivalent(current)) return true;
            try {
                Thread.sleep(WAIT_TIME);
            } catch (Exception e) {
            }
            elapsed += WAIT_TIME;
            if (timeout != -1 && elapsed > timeout) return false;
        } while (true);
    }

    public AbstractJob_Outcome performAJ(AbstractJob aj, boolean polling, boolean waitForAnswer) throws java.io.InvalidClassException, java.io.NotSerializableException, java.io.IOException, java.security.SignatureException, java.lang.ClassNotFoundException, java.lang.Exception {
        AbstractJob aj_signed = null;
        Reply tmp_rep;
        RetrieveOutcomeReply rep;
        tmp_rep = sendAJ(aj, polling);
        if (tmp_rep == null) return null;
        if (!polling && tmp_rep instanceof RetrieveOutcomeReply) rep = (RetrieveOutcomeReply) tmp_rep; else {
            if (waitForAnswer) {
                if (!waitForState(AbstractActionStatus.SUCCESSFUL, aj.getAJOId(), vsite, -1)) {
                    throw new Exception("Task failed so can't wait for it");
                }
            } else return null;
            rep = getAJReply(aj.getAJOId());
        }
        return deserialize(rep);
    }

    public byte[] performStreamedAJ(AbstractJob aj, boolean polling, boolean waitForAnswer, String endOfName) throws java.io.InvalidClassException, java.io.NotSerializableException, java.io.IOException, java.security.SignatureException, java.lang.ClassNotFoundException, java.lang.Exception {
        AbstractJob aj_signed = null;
        Reply tmp_rep;
        RetrieveOutcomeReply rep;
        AJOIdentifier id = aj.getAJOId();
        tmp_rep = sendAJ(aj, polling);
        if (tmp_rep == null) return null;
        if (!polling && tmp_rep instanceof RetrieveOutcomeReply) rep = (RetrieveOutcomeReply) tmp_rep; else {
            if (waitForAnswer) {
                if (!waitForState(AbstractActionStatus.SUCCESSFUL, aj.getAJOId(), vsite, -1)) {
                    throw new Exception("Task failed so can't wait for it");
                }
            } else return null;
        }
        rep = getAJReply(id);
        return getStreamed(rep, endOfName);
    }

    private static AbstractJob_Outcome deserialize(RetrieveOutcomeReply rep) {
        try {
            AbstractJob_Outcome tmp_out = (AbstractJob_Outcome) (new ObjectInputStream(new ByteArrayInputStream(rep.getOutcome()))).readObject();
            return ConsignForm.convertFrom(tmp_out);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getStreamed(RetrieveOutcomeReply ror, String endOfName) {
        if (ror.hasStreamed()) {
            try {
                PacketisedInputStream pis = new PacketisedInputStream(socket.getInputStream());
                ZipInputStream zis = new ZipInputStream(pis);
                ZipEntry entry = zis.getNextEntry();
                while (entry != null) {
                    if (entry.getName().endsWith(endOfName)) {
                        ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
                        while (zis.available() > 0) {
                            int read = zis.read();
                            if (zis.available() > 0) bOutputStream.write(read);
                        }
                        bOutputStream.flush();
                        zis.closeEntry();
                        zis.close();
                        pis.close();
                        return bOutputStream.toByteArray();
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
                zis.close();
                pis.close();
                return null;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
        } else return null;
    }

    public void tidy(AJOIdentifier id) {
        RetrieveOutcomeAck roa = new RetrieveOutcomeAck(id, vsite);
        try {
            outputStream.writeObject(roa);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
