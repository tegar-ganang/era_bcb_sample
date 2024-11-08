package verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import sexpression.*;
import sexpression.stream.*;
import auditorium.*;

/**
 * Generate some test data.
 * 
 * @author kyle
 * 
 */
public class TestDataGen {

    public static final int VOTES = 10000;

    public static final int INCREMENT = 50;

    private final int[] _sequence = new int[10];

    private long _nonce;

    private final Cert[] _cert = new Cert[10];

    private final Key[] _key = new Key[10];

    private final HostPointer[] _hp = new HostPointer[10];

    private Message _recent;

    private ASEWriter _out;

    private FileOutputStream _stream;

    public void run() throws Exception {
        initHosts();
        initKeys();
        initLog(Integer.toString(INCREMENT));
        initSequence();
        int current = INCREMENT;
        _out.writeASE(makePollsOpen().toASE());
        for (int lcv = 0; lcv < VOTES; lcv++) {
            String str = Integer.toString((lcv % 9) + 1);
            _out.writeASE(makeAuth(str).toASE());
            _out.writeASE(makeCast(str).toASE());
            _out.writeASE(makeReceived(str).toASE());
            if (lcv == current) {
                current += INCREMENT;
                changeLog(Integer.toString(lcv), Integer.toString(current));
            }
        }
        _out.writeASE(makePollsClosed().toASE());
        _stream.close();
    }

    public void changeLog(String from, String to) throws Exception {
        _stream.flush();
        copy("testdata/" + from, "testdata/" + to);
        _out.writeASE(makePollsClosed().toASE());
        _stream.close();
        _stream = new FileOutputStream(new File("testdata/" + to), true);
        _out = new ASEWriter(_stream);
    }

    public Message makePollsOpen() throws Exception {
        return makeMessage(0, ASExpression.make("(polls-open 0 openkey)"), true);
    }

    public Message makePollsClosed() throws Exception {
        return makeMessage(0, ASExpression.make("(polls-closed 1)"), false);
    }

    public Message makeAuth(String to) throws Exception {
        return makeMessage(0, ASExpression.make("(authorized-to-cast " + to + " " + Long.toString(_nonce) + " ballot)"), true);
    }

    public Message makeCast(String from) throws Exception {
        return makeMessage(Integer.parseInt(from), ASExpression.make("(cast-ballot " + Long.toString(_nonce) + " encryptedballot)"), true);
    }

    public Message makeReceived(String to) throws Exception {
        Message msg = makeMessage(0, ASExpression.make("(ballot-received " + to + " " + _nonce + ")"), true);
        _nonce++;
        return msg;
    }

    public Message makeMessage(int from, ASExpression message, boolean track) throws Exception {
        _sequence[from]++;
        Cert cert = _cert[from];
        Key key = _key[from];
        Signature sig;
        if (_recent != null) {
            sig = RSACrypto.SINGLETON.sign(new ListExpression(StringExpression.make("succeeds"), new ListExpression(new MessagePointer(_recent).toASE()), message), key);
        } else {
            sig = RSACrypto.SINGLETON.sign(new ListExpression(StringExpression.make("succeeds"), ListExpression.EMPTY, message), key);
        }
        Message msg = new Message("announce", _hp[from], Integer.toString(_sequence[from]), new ListExpression(StringExpression.make("signed-message"), cert.toASE(), sig.toASE()));
        if (track) _recent = msg;
        return msg;
    }

    public void initHosts() {
        for (int lcv = 0; lcv < 10; lcv++) {
            String lcvstr = Integer.toString(lcv);
            _hp[lcv] = new HostPointer(lcvstr, "192.168.1." + lcvstr, 9000);
        }
    }

    public void initLog(String runNum) {
        new File("testdata/").mkdir();
        _recent = null;
        try {
            _stream = new FileOutputStream(new File("testdata/" + runNum));
            _out = new ASEWriter(_stream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void initSequence() {
        for (int lcv = 0; lcv < 10; lcv++) _sequence[lcv] = -1;
        _nonce = -1;
    }

    public void initKeys() throws Exception {
        IKeyStore ks = new SimpleKeyStore("/keys");
        for (int lcv = 0; lcv < 10; lcv++) {
            _key[lcv] = ks.loadKey(Integer.toString(lcv));
            _cert[lcv] = ks.loadCert(Integer.toString(lcv));
        }
    }

    public static void copy(String from, String to) throws Exception {
        File inputFile = new File(from);
        File outputFile = new File(to);
        FileInputStream in = new FileInputStream(inputFile);
        FileOutputStream out = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static void main(String[] args) throws Exception {
        new TestDataGen().run();
    }
}
