package edu.mit.osidutil.contrivance;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.channels.FileLock;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.PrincipalName;

/**
 *  <p>
 *  Implements a credentials cache for Kerberos Tickets.
 *  </p><p>
 *  CVS $Id: KrbCredCache.java,v 1.1 2005/08/25 15:45:54 tom Exp $
 *  </p>
 *  
 *  @author  Tom Coppeto
 *  @version $Revision: 1.1 $
 *  @see     java.lang.Exception
 */
public class KrbCredCache extends Object {

    private final String file;

    private final short VERSION = 0x0503;

    private final short ADDRTYPE_INET = 0x0002;

    private final short ADDRTYPE_INET6 = 0x0018;

    /**
     * Constructs a new <code>KrbCredCache</code>.
     */
    public KrbCredCache(final String file) {
        this.file = file;
    }

    /**
     *  Initializes a new credential cache file.
     */
    public void initialize(final KerberosPrincipal principal) throws KrbCredCacheException {
        RandomAccessFile raf;
        FileLock lock;
        if (principal == null) {
            throw new KrbCredCacheException("null argument");
        }
        destroy();
        try {
            raf = new RandomAccessFile(this.file, "rw");
            lock = raf.getChannel().lock();
        } catch (Exception e) {
            throw new KrbCredCacheException("cannot open credentials file: " + this.file, e);
        }
        try {
            raf.writeShort(VERSION);
        } catch (IOException ie) {
            throw new KrbCredCacheException("cannot write version to credentials file: " + this.file, ie);
        }
        try {
            storePrincipal(raf, principal);
        } catch (KrbCredCacheException ke) {
            throw new KrbCredCacheException(ke);
        }
        try {
            lock.release();
            raf.getChannel().close();
        } catch (Exception e) {
            throw new KrbCredCacheException("cannot close credentials file: " + this.file, e);
        }
        return;
    }

    /**
     *  Destroys a credentials cache file ans removes it.
     */
    public void destroy() throws KrbCredCacheException {
        File file;
        try {
            file = new File(this.file);
        } catch (NullPointerException ne) {
            throw new KrbCredCacheException(ne);
        }
        try {
            if (!file.isFile() || !file.exists()) {
                return;
            }
        } catch (SecurityException se) {
            throw new KrbCredCacheException("permission denied", se);
        }
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            FileLock lock = raf.getChannel().lock();
            int length = (int) raf.length();
            byte[] b = new byte[length];
            raf.write(b, 0, length);
            lock.release();
            raf.close();
        } catch (Exception e) {
            System.err.println("unable to zero cred file");
            e.printStackTrace();
        }
        try {
            file.delete();
        } catch (SecurityException se) {
            throw new KrbCredCacheException("permission denied", se);
        }
        return;
    }

    public void store(final KerberosTicket ticket, final EncryptionKey key) throws KrbCredCacheException {
        File file;
        RandomAccessFile raf;
        FileLock lock;
        try {
            file = new File(this.file);
            if (!file.isFile() || !file.exists()) {
                throw new KrbCredCacheException("file does not exist");
            }
            raf = new RandomAccessFile(this.file, "rwd");
            lock = raf.getChannel().lock();
            raf.seek(raf.length());
        } catch (Exception e) {
            throw new KrbCredCacheException("cannot open credentials file: " + this.file, e);
        }
        try {
            storePrincipal(raf, ticket.getClient());
            storePrincipal(raf, ticket.getServer());
            storeShort(raf, (short) key.getEType());
            storeShort(raf, (short) key.getEType());
            storeData(raf, key.getBytes());
            storeInt(raf, (int) (ticket.getAuthTime().getTime() / 1000));
            storeInt(raf, (int) (ticket.getStartTime().getTime() / 1000));
            storeInt(raf, (int) (ticket.getEndTime().getTime() / 1000));
            if (ticket.getRenewTill() != null) {
                storeInt(raf, (int) (ticket.getRenewTill().getTime() / 1000));
            } else {
                storeInt(raf, 0);
            }
            storeByte(raf, 0);
            storeFlags(raf, ticket.getFlags());
            storeAddresses(raf, ticket.getClientAddresses());
            storeInt(raf, 1);
            storeShort(raf, (short) ticket.getSessionKeyType());
            storeData(raf, ticket.getSessionKey().getEncoded());
            storeData(raf, ticket.getEncoded());
            storeInt(raf, 0);
        } catch (KrbCredCacheException ke) {
            throw new KrbCredCacheException("unable to write ticket", ke);
        }
        try {
            lock.release();
            raf.getChannel().close();
        } catch (Exception e) {
            throw new KrbCredCacheException("cannot close credentials file: " + this.file, e);
        }
        return;
    }

    private void storePrincipal(RandomAccessFile raf, final KerberosPrincipal principal) throws KrbCredCacheException {
        int length = componentLength(principal.getName());
        storeInt(raf, principal.getNameType());
        storeInt(raf, length);
        storeData(raf, principal.getRealm());
        for (int i = 1; i <= length; i++) {
            storeData(raf, getPrincipalComponent(principal.getName(), i));
        }
        return;
    }

    private void storeAddresses(final RandomAccessFile raf, final InetAddress[] addresses) throws KrbCredCacheException {
        if (addresses == null) {
            storeInt(raf, 0);
            return;
        }
        storeInt(raf, addresses.length);
        for (int i = 0; i < addresses.length; i++) {
            storeAddress(raf, addresses[i]);
        }
        return;
    }

    private void storeAddress(final RandomAccessFile raf, final InetAddress address) throws KrbCredCacheException {
        byte[] b = address.getAddress();
        if (b.length == 4) {
            storeShort(raf, ADDRTYPE_INET);
        } else {
            storeShort(raf, ADDRTYPE_INET6);
        }
        storeData(raf, b);
        return;
    }

    private void storeFlags(final RandomAccessFile raf, final boolean[] flags) throws KrbCredCacheException {
        int f = 0;
        int shift = 0;
        for (int i = flags.length - 1; i >= 0; i--) {
            if (flags[i] == true) {
                f |= 1 >>> shift++;
            }
        }
        storeInt(raf, f);
        return;
    }

    private void storeData(RandomAccessFile raf, final String s) throws KrbCredCacheException {
        storeData(raf, s.getBytes());
        return;
    }

    private void storeData(RandomAccessFile raf, final byte[] buf) throws KrbCredCacheException {
        storeInt(raf, buf.length);
        try {
            raf.write(buf);
        } catch (IOException ie) {
            throw new KrbCredCacheException("unable to write data to: " + this.file, ie);
        }
        return;
    }

    private void storeInt(RandomAccessFile raf, final int number) throws KrbCredCacheException {
        byte[] buf = new byte[4];
        for (int i = 0; i < 4; i++) {
            buf[3 - i] = (byte) (number >>> (i * 8));
        }
        try {
            raf.write(buf);
        } catch (IOException ie) {
            throw new KrbCredCacheException("unable to write data to: " + this.file, ie);
        }
        return;
    }

    private void storeShort(RandomAccessFile raf, final short number) throws KrbCredCacheException {
        byte[] buf = new byte[2];
        for (int i = 0; i < 2; i++) {
            buf[1 - i] = (byte) (number >>> (i * 8));
        }
        try {
            raf.write(buf);
        } catch (IOException ie) {
            throw new KrbCredCacheException("unable to write data to: " + this.file, ie);
        }
        return;
    }

    private void storeByte(RandomAccessFile raf, final int value) throws KrbCredCacheException {
        try {
            raf.writeByte(value);
        } catch (IOException ie) {
            throw new KrbCredCacheException("unable to write data to: " + this.file, ie);
        }
        return;
    }

    private int componentLength(final String name) {
        if (name == null) {
            return (0);
        }
        int from = 0;
        int n = 0;
        while (from != -1) {
            ++n;
            from = name.indexOf('/', from + 1);
        }
        return (n);
    }

    private String getPrincipalComponent(final String name, final int pos) {
        if ((name == null) || (pos < 1)) {
            return (null);
        }
        String p;
        int r = name.indexOf('@');
        if (r > 0) {
            p = name.substring(0, r);
        } else {
            p = name;
        }
        int cur = 0;
        int begin = 0;
        int from = 0;
        while ((from != -1) && (cur < pos)) {
            ++cur;
            begin = from > 0 ? from + 1 : 0;
            from = p.indexOf('/', from + 1);
        }
        if (cur != pos) {
            return (null);
        }
        if (from == -1) {
            return (p.substring(begin));
        }
        if (begin > 0) {
            return (p.substring(begin + 1, from));
        }
        return (p.substring(0, from));
    }
}
