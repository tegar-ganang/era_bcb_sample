package it.trento.comune.j4sign.installer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JOptionPane;

/**
 * @author resolir
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class Installer {

    private static final short OS_WINDOWS = 0;

    private static final short OS_LINUX = 1;

    private static final short OS_MAC = 2;

    private static final short OS_UNSUPPORTED = -1;

    private static final short ARCH_X86 = 0;

    private static final short ARCH_AMD64 = 1;

    protected short os;

    protected short arch;

    private String targetDir = null;

    private String osName = null;

    String osArch = null;

    public Installer() {
        this.osName = System.getProperty("os.name");
        this.osArch = System.getProperty("os.arch");
        if (osArch.contains("x86")) this.arch = ARCH_X86; else if (osArch.contains("amd64")) this.arch = ARCH_AMD64;
        if (osName.toLowerCase().indexOf("win") > -1) {
            this.os = OS_WINDOWS;
        } else if (osName.toLowerCase().indexOf("linux") > -1) {
            this.os = OS_LINUX;
        } else if (osName.toLowerCase().indexOf("mac") > -1) {
            this.os = OS_MAC;
        } else this.os = OS_UNSUPPORTED;
        String extDirs = System.getProperty("java.ext.dirs");
        String extDir = extDirs;
        if (extDirs != null) {
            int separatorIndex = -1;
            if ((os == OS_LINUX) && extDirs.contains(":")) separatorIndex = extDirs.indexOf(":");
            if ((os == OS_WINDOWS) && extDirs.contains(";")) separatorIndex = extDirs.indexOf(";");
            if (separatorIndex != -1) {
                extDir = extDirs.substring(0, separatorIndex);
                JOptionPane.showMessageDialog(null, "L'installazione delle librerie verrà effettuata nella prima directory:\n'" + extDir + "'", "Rilevata più di una directory per le estensioni", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "L'installazione delle librerie verrà effettuata nella directory:\n'" + extDir + "'", "Rilevata una directory per le estensioni", JOptionPane.INFORMATION_MESSAGE);
            }
            this.targetDir = extDir;
        }
    }

    public boolean install() throws IOException {
        installFile("SmartCardAccess-signed.jar");
        switch(os) {
            case OS_WINDOWS:
                switch(arch) {
                    case ARCH_X86:
                        installFile("lib32/OCFPCSC1.dll");
                        installFile("lib32/pkcs11wrapper.dll");
                        break;
                    case ARCH_AMD64:
                        this.os = OS_UNSUPPORTED;
                        break;
                }
                break;
            case OS_LINUX:
                switch(arch) {
                    case ARCH_X86:
                        installFile("lib32/libOCFPCSC1.so");
                        installFile("lib32/libpkcs11wrapper.so");
                        break;
                    case ARCH_AMD64:
                        installFile("lib64/libOCFPCSC1.so");
                        installFile("lib64/libpkcs11wrapper.so");
                        break;
                }
                break;
            case OS_MAC:
                this.os = OS_UNSUPPORTED;
                break;
            default:
                this.os = OS_UNSUPPORTED;
                break;
        }
        if (this.os == OS_UNSUPPORTED) {
            System.out.println("==== Smart Card Access Extension NOT installed! ====");
            JOptionPane.showMessageDialog(null, "Sistema '" + this.osName + " " + this.osArch + "' non supportato!", "Installation complete.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public static void main(String[] args) {
        System.out.println("===== Smart Card Access Extension installation ====");
        Installer installer = new Installer();
        try {
            installer.install();
            System.out.println("==== Smart Card Access Extension installed. ====");
            JOptionPane.showMessageDialog(null, "L'installazione e' stata completata\ncon successo!", "Installation complete.", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            System.out.println("Error: " + e);
            System.out.println("==== Smart Card Access Extension NOT installed! ====");
            JOptionPane.showMessageDialog(null, "L'installazione non si e' conclusa correttamente!", "Installation complete.", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void installFile(String name) throws IOException {
        String s = System.getProperty("file.separator");
        String destName = name.substring(name.indexOf("/") + 1);
        File f = new File(this.targetDir + s + destName);
        System.out.println("Installing '" + f.getAbsolutePath() + "'");
        boolean exists = f.isFile();
        InputStream in = getClass().getResourceAsStream(name);
        if (in != null) {
            BufferedInputStream bufIn = new BufferedInputStream(in);
            try {
                OutputStream fout = new BufferedOutputStream(new FileOutputStream(f));
                byte[] bytes = new byte[1024 * 10];
                for (int n = 0; n != -1; n = bufIn.read(bytes)) fout.write(bytes, 0, n);
                fout.close();
            } catch (IOException ioe) {
                if (!exists) throw ioe;
            }
        } else throw new IOException("Found no resource named: " + name);
    }
}
