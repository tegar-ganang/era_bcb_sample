package issrg.SAWS.callback;

import issrg.SAWS.callback.gui.CreateCertificateDialog;
import issrg.SAWS.SAWSServer;
import issrg.SAWS.callback.gui.inputPassword;
import issrg.SAWS.util.CertificateData;
import issrg.SAWS.util.SAWSLogWriter;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Calendar;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * This class represents the callback handler that uses the command prompt 
 * to interact with the user.
 *
 * @author E. Silva
 * @version 1.0, Mar. 2007
 */
public class SAWSCmdPromptCallbackHandler implements CallbackHandler {

    private static SAWSLogWriter sawsDebugLog = new SAWSLogWriter(SAWSCmdPromptCallbackHandler.class.getName());

    /**
     * Constructor of the class.
     */
    public SAWSCmdPromptCallbackHandler() {
    }

    /**
     * Method that handles the callbacks sent as parameter.
     * @param callbacks The list of callbacks to be handled.
     * @throws UnsupportedCallbackException If one of the callbacks are not known (supported).
     */
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        if (callbacks == null) {
            throw new UnsupportedCallbackException(null, "ERROR: Callbacks can not be null.");
        }
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAWSTextOutputCallback) {
                switch(((SAWSTextOutputCallback) callbacks[i]).getMessageType()) {
                    case SAWSTextOutputCallback.ERROR:
                        System.err.println("\nERROR: " + ((SAWSTextOutputCallback) callbacks[i]).getMessage());
                        break;
                    case SAWSTextOutputCallback.INFORMATION:
                        System.err.println("\nINFO: " + ((SAWSTextOutputCallback) callbacks[i]).getMessage());
                        break;
                    case SAWSTextOutputCallback.WARNING:
                        System.err.println("\nWARNING: " + ((SAWSTextOutputCallback) callbacks[i]).getMessage());
                        break;
                    case SAWSTextOutputCallback.LONG_MESSAGE:
                        System.err.println("\n" + ((SAWSTextOutputCallback) callbacks[i]).getMessage());
                        break;
                }
            } else if (callbacks[i] instanceof SAWSPasswordCallback) {
                SAWSPasswordCallback pc = (SAWSPasswordCallback) callbacks[i];
                System.out.println("\n" + pc.getPrompt());
                System.out.flush();
                try {
                    pc.setPassword(readPassword(System.in));
                } catch (IOException e) {
                    System.err.println("\nWARNING: Fail to read the password from the command prompt.");
                    sawsDebugLog.write("\nWARNING: Fail to read the password from the command prompt.");
                }
            } else if (callbacks[i] instanceof CertificateDataCallback) {
                CertificateDataCallback cdc = (CertificateDataCallback) callbacks[i];
                CertificateData cd = this.readCertificateData(cdc);
                cdc.setCertData(cd);
            } else if (callbacks[i] instanceof SAWSChoiceCallback) {
                SAWSChoiceCallback cc = (SAWSChoiceCallback) callbacks[i];
                System.out.println("\n" + cc.getPrompt() + "\n");
                System.out.flush();
                InputStreamReader is = new InputStreamReader(System.in);
                BufferedReader systemIn = new BufferedReader(is);
                String[] options = cc.getOptions();
                int n = options.length;
                for (int j = 0; j < n; j = j + 1) {
                    System.out.println("\t[" + (j + 1) + "]  " + options[j]);
                }
                System.out.println("\nPlease type the number that corresponds to your choice:");
                int choice = -1;
                boolean finished = false;
                while (!finished) {
                    try {
                        choice = Integer.parseInt(systemIn.readLine());
                    } catch (IOException e) {
                        System.err.println("ERROR: Fail when reading the option from the command prompt.");
                        sawsDebugLog.write("ERROR: Fail when reading the option from the command prompt.");
                    } catch (Exception ex) {
                        System.err.println("\nWARNING: Invalid option. Please input a valid number for the option:");
                    }
                    if ((choice <= 0) || (choice > n)) {
                        System.err.println("\nWARNING: Invalid option. Please input a valid option:");
                    } else {
                        finished = true;
                    }
                }
                cc.setSelectedIndex(choice - 1);
            } else if (callbacks[i] instanceof SAWSTextInputCallback) {
                SAWSTextInputCallback cc = (SAWSTextInputCallback) callbacks[i];
                System.out.println("\n" + cc.getPrompt());
                System.out.flush();
                InputStreamReader is = new InputStreamReader(System.in);
                BufferedReader systemIn = new BufferedReader(is);
                String value = null;
                try {
                    value = systemIn.readLine();
                } catch (IOException ioe) {
                    System.err.println("ERROR: Fail when reading a value from the command prompt.");
                    sawsDebugLog.write("ERROR: Fail when reading a value from the command prompt.");
                }
                cc.setText(value);
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "ERROR: Unrecognized Callback.");
            }
        }
    }

    /**
     * Method responsible for asking the user the data about the certificate
     * being created.
     * 
     * @param cdc The callback generated to ask data about the certificate.
     * @return  The data about the certificate
     */
    private CertificateData readCertificateData(CertificateDataCallback cdc) {
        CertificateData cd = new CertificateData();
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader systemIn = new BufferedReader(is);
        boolean valid = false;
        System.out.println("\nPlease input the following data for the certificate:");
        try {
            Loop1: while (!valid) {
                System.out.println("\nValidity (DDMMYYYY):");
                String validity = systemIn.readLine();
                if (validity.length() != 8) {
                    System.err.println("WARNING: Please input a date in the format DDMMYYYY.");
                    continue Loop1;
                }
                Calendar c = Calendar.getInstance();
                c.setLenient(false);
                try {
                    c.set(Calendar.YEAR, Integer.parseInt(validity.substring(4, 8)));
                    c.set(Calendar.MONTH, Integer.parseInt(validity.substring(2, 4)) - 1);
                    c.set(Calendar.DATE, Integer.parseInt(validity.substring(0, 2)));
                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    Calendar temp = Calendar.getInstance();
                    temp.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), 0, 0, 0);
                    long todayMili = today.getTimeInMillis();
                    long dateMili = temp.getTimeInMillis();
                    if (todayMili > dateMili) {
                        System.err.println("WARNING: The selected date must be after to " + today.get(Calendar.DATE) + "/" + (today.get(Calendar.MONTH) + 1) + "/" + today.get(Calendar.YEAR) + ".");
                        continue Loop1;
                    }
                    int days = (int) ((dateMili - todayMili) / 1000 / 60 / 60 / 24);
                    today.add(Calendar.DATE, days);
                    if (today.compareTo(temp) > 0) {
                        days = days - 1;
                    } else if (today.compareTo(temp) < 0) {
                        days = days + 1;
                    }
                    cd.setValidity(days);
                    valid = true;
                } catch (NumberFormatException nfe) {
                    System.err.println("WARNING: The date can not contain letters, only numbers. Please type again.");
                } catch (IllegalArgumentException iae) {
                    System.err.println("WARNING: " + iae.getMessage() + " is not valid. Please type the validity date again.");
                }
            }
            valid = false;
            System.out.println("\nPlease answer the following questions." + "\n[Press Enter (Ret) to skip the question. At least one of the six questions must be answered.");
            while (!valid) {
                System.out.println("\nWhat is your complete name (CN)?");
                cd.setCommonName(systemIn.readLine());
                System.out.println("\nWhat is the name of your Organizational Unit (OU)?");
                cd.setOrganizationUnitName(systemIn.readLine());
                System.out.println("\nWhat is the name of your Organization (O)?");
                cd.setOrganizationName(systemIn.readLine());
                System.out.println("\nWhat is the name of your City or Locality (L)?");
                cd.setLocalityName(systemIn.readLine());
                System.out.println("\nWhat is the name of your Province or State (S)?");
                cd.setStateName(systemIn.readLine());
                Country: while (true) {
                    System.out.println("\nWhat is the two-letter Country code for this unit (C)?");
                    String country = systemIn.readLine();
                    if (country != null && !country.equals("") && country.length() != 2) {
                        System.err.println("WARNING: The country code, if specified, must contain two letters. Please try again.");
                        continue Country;
                    } else {
                        cd.setCountryName(country);
                        break Country;
                    }
                }
                String sn = cd.toSubjectName();
                if (sn != null && !sn.equals("")) {
                    System.out.println("\nIs " + sn + " correct? [y/n]");
                    System.out.print("[no]: ");
                    String option = systemIn.readLine();
                    if (option.equalsIgnoreCase("y") || option.equalsIgnoreCase("yes")) {
                        valid = true;
                    }
                } else {
                    System.err.println("WARNING: At least one of the questions must be answered. Please try again.");
                }
            }
            valid = false;
            String alg = null;
            if (cdc.getType() == CertificateDataCallback.ENCRYPTION) {
                cd.setAlgorithm("RSA");
            } else {
                System.out.println("\nPlease input the name of the algorithm for this certificate: [RSA or DSA]");
                while (!valid) {
                    alg = systemIn.readLine();
                    if (alg != null && (alg.equalsIgnoreCase("RSA") || alg.equalsIgnoreCase("DSA"))) {
                        cd.setAlgorithm(alg.toUpperCase());
                        valid = true;
                    } else {
                        System.out.println("\nWARNING: The supported algorithms are RSA and DSA. Please type one of these algorithms:");
                    }
                }
            }
            valid = false;
            int[] keySizes = null;
            alg = cd.getAlgorithm();
            if (alg.equalsIgnoreCase("RSA")) {
                keySizes = new int[] { 1024, 2048, 3072, 4096 };
            } else {
                keySizes = new int[] { 512, 640, 768, 896, 1024 };
            }
            System.out.println("\nPlease input one of the following options for the key size:\n");
            Loop2: while (!valid) {
                for (int j = 0; j < keySizes.length; j = j + 1) {
                    System.out.println("\t[" + (j + 1) + "] " + keySizes[j]);
                }
                int choice = -1;
                try {
                    choice = Integer.parseInt(systemIn.readLine());
                } catch (Exception ex) {
                    System.err.println("\nWARNING: Invalid option. Please input a valid number for the option:\n");
                    continue Loop2;
                }
                if ((choice <= 0) || (choice > keySizes.length)) {
                    System.err.println("\nWARNING: Invalid option. Please input a valid option:\n");
                } else {
                    cd.setKeySize(keySizes[choice - 1]);
                    valid = true;
                }
            }
        } catch (IOException ioe) {
            System.err.println("ERROR: Fail to read key size from the command prompt.");
            sawsDebugLog.write("ERROR: Fail to read key size from the command prompt.");
        }
        return cd;
    }

    /**
     * Method for reading the password from the command line.
     * 
     * @param in The input stream from where the password will be read. 
     * Usually 'System.in'.
     * 
     * @return the password in a char array.
     * @throws IOException If an error happens during the reading process.
     */
    private char[] readPassword(InputStream in) throws IOException {
        MaskingThread mt = new MaskingThread();
        mt.start();
        char[] lineBuffer;
        char[] buf;
        buf = lineBuffer = new char[128];
        int room = buf.length;
        int offset = 0;
        int c;
        loop: while (true) {
            c = in.read();
            switch(c) {
                case -1:
                case '\n':
                    break loop;
                case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream) in).unread(c2);
                    } else break loop;
                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }
        mt.stopMasking();
        if (offset == 0) {
            return null;
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return ret;
    }
}

/**
 * Class that represents a thread to mask the password while the user types it.
 */
class MaskingThread extends Thread {

    private volatile boolean stop;

    private char echochar = '*';

    /**
   * Contructor of the class MaskingThread.
   */
    public MaskingThread() {
        super("Masking");
    }

    /**
   * Begin masking until asked to stop.
   */
    public void run() {
        int priority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        try {
            stop = true;
            while (stop) {
                System.out.print("\010" + echochar);
                try {
                    Thread.currentThread().sleep(1);
                } catch (InterruptedException iex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            Thread.currentThread().setPriority(priority);
        }
    }

    /**
   * Instruct the thread to stop masking.
   */
    public void stopMasking() {
        this.stop = false;
    }
}
