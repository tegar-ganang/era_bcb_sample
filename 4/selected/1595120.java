package br.org.direto.util;

import jcifs.netbios.NbtAddress;
import jcifs.util.*;
import jcifs.smb.*;
import java.io.OutputStream;
import java.util.Date;

public class AuthListFiles extends NtlmAuthenticator {

    public static String readLine() throws Exception {
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = System.in.read()) != '\n') {
            if (c == -1) return "";
            sb.append((char) c);
        }
        return sb.toString().trim();
    }

    public AuthListFiles(String[] argv) throws Exception {
        NtlmAuthenticator.setDefault(this);
        SmbFileInputStream in = null;
        try {
            in = new SmbFileInputStream(argv[0] + "1.doc");
            System.out.println(in.toString());
            OutputStream out = System.out;
            int nextChar;
            while ((nextChar = in.read()) != -1) out.write(Character.toUpperCase((char) nextChar));
            out.write('\n');
            out.flush();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    protected NtlmPasswordAuthentication getNtlmPasswordAuthentication() {
        System.out.println(getRequestingException().getMessage() + " for " + getRequestingURL());
        try {
            System.out.print("username: ");
            String username = readLine();
            System.out.print("password: ");
            String password = readLine();
            if (password.length() == 0) {
                return null;
            }
            return new NtlmPasswordAuthentication(null, username, password);
        } catch (Exception e) {
        }
        return null;
    }

    public static void main(String[] argv) throws Exception {
        String[] sambaUrl = { "smb://10.133.108.133/temp/" };
        new AuthListFiles(sambaUrl);
    }
}
