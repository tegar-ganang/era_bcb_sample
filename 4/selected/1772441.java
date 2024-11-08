package uk.co.nimp.scard.dump;

import com.atolsystems.atolutilities.ATimeUtilities;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import javax.smartcardio.CardNotPresentException;
import uk.co.nimp.scard.GenericTerminal;
import uk.co.nimp.scard.PcScTerminalManager;
import uk.co.nimp.scard.ScardException;
import uk.co.nimp.smartcard.Apdu;

/**
 *
 * @author seb
 */
public class Main {

    public static void main(String[] args) throws ScardException, IOException, CardNotPresentException, Exception {
        PcScTerminalManager manager = new PcScTerminalManager();
        List<GenericTerminal> terminals = manager.list();
        if (0 == terminals.size()) {
            System.out.println("PcSc terminal not detected.");
            return;
        }
        GenericTerminal terminal = terminals.get(0);
        System.out.println("Try to connected to " + terminal);
        terminal.coldConnect();
        System.out.println("connected to " + terminal + " using protocol " + GenericTerminal.getProtocolName(terminal.getProtocol()));
        long start = System.currentTimeMillis();
        Apdu readApdu = new Apdu();
        int maxLe = 0x100;
        int base = 0x500000;
        int size = 0x024000;
        readApdu.setIns(0x0A);
        readApdu.setExpectedLe(maxLe);
        readApdu.setExpectedSw(0x9000);
        int nLoops = size / maxLe;
        int nLast = size % maxLe;
        File fout = new File(ATimeUtilities.getTimeTagMillisecond() + ".bin");
        System.out.println("Output file:");
        System.out.println(fout.getCanonicalPath());
        FileOutputStream fos = new FileOutputStream(fout);
        int address = base;
        for (int i = 0; i < nLoops; i++) {
            setHeader(readApdu, address);
            terminal.sendApdu(readApdu);
            fos.write(readApdu.getLeData());
            address += maxLe;
        }
        if (nLast > 0) {
            readApdu.setExpectedLe(nLast);
            setHeader(readApdu, address);
        }
        fos.close();
        long end = System.currentTimeMillis();
        long delta = end - start;
        System.out.println("delta= " + delta + " ms");
    }

    static void setHeader(Apdu apdu, int address) {
        int cla = address >> 16;
        int p1 = 0xFF & (address >> 8);
        int p2 = 0xFF & address;
        apdu.setCla(cla);
        apdu.setP1(p1);
        apdu.setP2(p2);
    }
}
