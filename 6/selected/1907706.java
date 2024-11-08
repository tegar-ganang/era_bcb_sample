package de.mud.ssh;

/**
 * This is an example for using the SshWrapper class. Note that the
 * password here is in plaintext, so do not make this .class file
 * available with your password inside it.
 *
 * <P>
 * <B>Maintainer:</B>Marcus Meissner
 *
 * @version $Id: SshWrapperExample.java 499 2005-09-29 08:24:54Z leo $
 * @author Matthias L. Jugel, Marcus Mei�ner
 */
public class SshWrapperExample {

    public static void main(String args[]) {
        SshWrapper ssh = new SshWrapper();
        try {
            byte[] buffer = new byte[256];
            ssh.connect(args[0], 22);
            ssh.login("marcus", "xxxxx");
            ssh.setPrompt("marcus");
            System.out.println("after login");
            ssh.send("ls -l");
            ssh.read(buffer);
            System.out.println(new String(buffer));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
