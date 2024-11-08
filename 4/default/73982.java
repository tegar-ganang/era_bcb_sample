import java.lang.*;
import java.net.*;
import java.io.*;
import java.util.*;

class Logger {

    public static String[] report = new String[101];

    public static int repcount;

    public static int status;

    Logger(String s) {
        if (status == 0) {
            repcount = 0;
            for (int i = 0; i <= 100; i++) {
                report[i] = "";
            }
        }
    }

    public static String reports() {
        if (repcount > 0 && status == 1) {
            repcount--;
            return report[repcount] + "\n";
        }
        return "";
    }

    public static void debug(String debug) {
    }

    public static void info(String info) {
        System.out.println("I: " + info);
    }

    public static void warn(String warn) {
        System.out.println("W: " + warn);
    }

    public static void error(String error) {
        System.out.println("E: " + error);
        if (repcount < 100 && status == 1) {
            status = 2;
            report[repcount] = error;
            repcount++;
            status = 1;
        }
    }

    /***********************************************************
Additional message types can be added using this template.
 public static void error(String error)
 {
  System.out.println("E: " + error);
  if (repcount < 100 && status == 1)
  {
   status = 2;
   report[repcount] = error;
   repcount++;
   status = 1;
  }
 }
************************************************************/
    public static void fatal(String fatal) {
        System.out.println("F: " + fatal);
        if (repcount < 100 && status == 1) {
            status = 3;
            report[repcount] = fatal;
            repcount++;
            status = 1;
        }
    }
}

class TFSNData {

    public static String[] banned = new String[10];

    public static String[] addresses = new String[10];

    public static String[] port = new String[10];

    public static String[] remotebanned = new String[10];

    public TFSNData() {
    }

    public static int lr(int x, int y) {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNData ";
        if (x <= 0 || x >= 10 || y <= 0 || y >= 10) {
            logger.debug(customMsg.toString() + "lr prevented reading a undefined portion of the array".toString());
            return 0;
        }
        try {
            if (banned[x].equals(remotebanned[y])) {
                logger.debug(customMsg.toString() + "Not adding bans because it already exist".toString());
                return 0;
            }
        } catch (NullPointerException e) {
            logger.error(e.toString());
        }
        logger.debug(customMsg.toString() + "Adding ban because it does not exist yet".toString());
        return 1;
    }
}

class TFSNServerSoc implements Runnable {

    Thread TSN;

    Socket socket;

    TFSNServerSoc() {
        TFSNServerSocGo();
    }

    public void TFSNServerSocGo() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNClientSoc ";
        while (TFSNData.port[0] == "0") logger.debug(customMsg.toString() + "Reading server port".toString());
        int port = Integer.parseInt(TFSNData.port[0].trim());
        while (true) {
            try {
                Thread.sleep(5000);
                TSN = new Thread(this, "TFSN ServerSoc");
                ServerSocket srv = new ServerSocket(port);
                logger.debug(customMsg.toString() + "Listening".toString());
                socket = srv.accept();
                logger.debug(customMsg.toString() + "Connection accepted".toString());
                TSN.start();
            } catch (IOException e) {
                logger.error(customMsg.toString() + e.toString());
            } catch (InterruptedException e) {
                logger.error(customMsg.toString() + e.toString());
            }
        }
    }

    public void run() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNClientSoc thread ";
        logger.debug(customMsg.toString() + "Creating Client Socket thread".toString());
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String str = ".";
            String cmdp = "n";
            String cmds = "n";
            int stage = 0;
            while (str != null) {
                Thread.sleep(1000);
                try {
                    str = rd.readLine();
                } catch (IOException e) {
                    logger.error(e.toString());
                }
                if (stage == 0) {
                    logger.debug(customMsg.toString() + "Stage 0".toString());
                    if (str.equals("[") || str.equals("(")) {
                        cmdp = str;
                        logger.debug(customMsg.toString() + "Received commmand staring with".toString());
                    }
                    if (str.equals("]") || str.equals(")")) {
                        cmds = str;
                        logger.debug(customMsg.toString() + "Received command ending with".toString());
                    }
                    if (str.equals(cmdp + "yes" + cmds)) {
                        logger.debug(customMsg.toString() + "Validated".toString());
                        stage = 1;
                    }
                }
                if (stage == 1) {
                    logger.debug(customMsg.toString() + "Stage 1".toString());
                    wr.write(cmdp + "good" + cmds + "\n");
                    wr.flush();
                    stage = 2;
                }
                if (stage == 2) {
                    logger.debug(customMsg.toString() + "Stage 2".toString());
                    wr.write(cmdp + "down" + cmds + "\n");
                    wr.flush();
                    logger.debug(customMsg.toString() + "Telling the client to throttle down".toString());
                    stage = 3;
                }
                if (stage >= 3 && stage <= 8) {
                    wr.write(cmdp + "GA" + cmds + "\n");
                    wr.flush();
                    logger.debug(customMsg.toString() + "Waiting...".toString());
                    stage++;
                }
                if (stage == 8) {
                    logger.debug(customMsg.toString() + "Stage 8".toString());
                    wr.write(cmdp + "hold" + cmds + "\n");
                    wr.flush();
                    wr.write(cmdp + "log1" + cmds + "\n");
                    wr.flush();
                    wr.write("Welcome!\n");
                    wr.flush();
                    wr.write(cmdp + "log0" + cmds + "\n");
                    wr.flush();
                    wr.write(cmdp + "GA" + cmds + "\n");
                    wr.flush();
                    stage = 9;
                }
                if (stage == 9) {
                    if (str.equals(cmdp + "refresh" + cmds)) {
                        wr.write(cmdp + "banned" + cmds + "\n");
                        wr.flush();
                        for (int i = 0; i < TFSNData.banned.length; i++) {
                            wr.write(TFSNData.banned[i] + "\n");
                            wr.flush();
                        }
                        wr.write(cmdp + "GA" + cmds + "\n");
                        wr.flush();
                    } else {
                        wr.write(cmdp + "GA" + cmds + "\n");
                        wr.flush();
                    }
                }
            }
        } catch (IOException e) {
            logger.debug(e.toString());
        } catch (InterruptedException e) {
            logger.debug(e.toString());
        }
    }
}

class TFSNClientSoc implements Runnable {

    Thread TSN;

    Socket sock;

    int nndnum;

    TFSNClientSoc() {
        TFSNClientSocGo();
    }

    public void TFSNClientSocGo() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNClientSoc ";
        logger.debug(customMsg.toString() + "Client socket started".toString());
        nndnum = 1;
        logger.debug(customMsg.toString() + "Reading properties file".toString());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("tfsn.ini"));
        } catch (IOException e) {
            logger.info(customMsg.toString() + "Could not find tfsn.ini. Please create that file and restart this program.".toString());
            logger.error(e.toString());
            logger.fatal(customMsg.toString() + "Client Socket failed because tfsn.ini does not exist".toString());
        }
        String connections = properties.getProperty("connections");
        int x = Integer.parseInt(connections.trim());
        logger.debug(customMsg.toString() + "Reading addr1..10s from tfsn.ini file".toString());
        TFSNData.addresses[0] = properties.getProperty("address");
        TFSNData.port[0] = properties.getProperty("port");
        if (x >= 1) {
            TFSNData.addresses[1] = properties.getProperty("addr1");
            TFSNData.port[1] = properties.getProperty("port1");
            logger.debug(customMsg.toString() + "Adding addr1".toString());
        }
        if (x >= 2) {
            TFSNData.addresses[2] = properties.getProperty("addr2");
            TFSNData.port[2] = properties.getProperty("port2");
            logger.debug(customMsg.toString() + "Adding addr2".toString());
        }
        if (x >= 3) {
            TFSNData.addresses[3] = properties.getProperty("addr3");
            TFSNData.port[3] = properties.getProperty("port3");
            logger.debug(customMsg.toString() + "Adding addr3".toString());
        }
        if (x >= 4) {
            TFSNData.addresses[4] = properties.getProperty("addr4");
            TFSNData.port[4] = properties.getProperty("port4");
            logger.debug(customMsg.toString() + "Adding addr4".toString());
        }
        if (x >= 5) {
            TFSNData.addresses[5] = properties.getProperty("addr5");
            TFSNData.port[5] = properties.getProperty("port5");
            logger.debug(customMsg.toString() + "Adding addr5".toString());
        }
        if (x >= 6) {
            TFSNData.addresses[6] = properties.getProperty("addr6");
            TFSNData.port[6] = properties.getProperty("port6");
            logger.debug(customMsg.toString() + "Adding addr6".toString());
        }
        if (x >= 7) {
            TFSNData.addresses[7] = properties.getProperty("addr7");
            TFSNData.port[7] = properties.getProperty("port7");
            logger.debug(customMsg.toString() + "Adding addr7".toString());
        }
        if (x >= 8) {
            TFSNData.addresses[8] = properties.getProperty("addr8");
            TFSNData.port[8] = properties.getProperty("port8");
            logger.debug(customMsg.toString() + "Adding addr8".toString());
        }
        if (x >= 9) {
            TFSNData.addresses[9] = properties.getProperty("addr9");
            TFSNData.port[9] = properties.getProperty("port9");
            logger.debug(customMsg.toString() + "Adding addr9".toString());
        }
        if (x >= 10) {
            TFSNData.addresses[10] = properties.getProperty("addr10");
            TFSNData.port[10] = properties.getProperty("port10");
            logger.debug(customMsg.toString() + "Adding addr10".toString());
        }
        for (int i = 0; i < x; i++) {
            try {
                TSN = new Thread(this, "TFSN ClientSoc Description");
                TSN.start();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error(e.toString());
            }
        }
    }

    public void run() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNClientSoc thread ";
        logger.debug(customMsg.toString() + "Creating Client Socket thread".toString());
        try {
            InetAddress addr = InetAddress.getByName(TFSNData.addresses[nndnum]);
            int port = Integer.parseInt(TFSNData.port[nndnum].trim());
            logger.debug(customMsg.toString() + "Creating new client socket".toString());
            Socket sock = new Socket(addr, port);
            TFSNData.remotebanned[0] = "open";
            logger.debug(customMsg.toString() + "Adding command prefix and command suffix".toString());
            String cmdp = "[";
            String cmds = "]";
            String str = cmdp + "new" + cmds;
            logger.debug(customMsg.toString() + "Intilizing varables".toString());
            int i = 0;
            int throttle = 0;
            int logging = 0;
            int kill = 0;
            int setthrottle = 1000;
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            BufferedReader rd = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            logger.debug(customMsg.toString() + "Creating buffers".toString());
            while (!str.equals(cmdp + "good" + cmds)) {
                logger.debug(customMsg.toString() + "Running pre suf checker".toString());
                try {
                    sock.setSoTimeout(5000);
                    str = rd.readLine();
                } catch (IOException e) {
                    logger.error(e.toString());
                }
                if (str.equals(cmdp + "new" + cmds)) {
                    logger.debug(customMsg.toString() + "Sending command prefix".toString());
                    wr.write(cmdp + "\n");
                    wr.flush();
                    logger.debug(customMsg.toString() + "Sending command suffix".toString());
                    wr.write(cmds + "\n");
                    wr.flush();
                    logger.debug(customMsg.toString() + "Sending prefix suffix and yes to accept the prefix and suffix for future commands".toString());
                    wr.write(cmdp + "yes" + cmds + "\n");
                    wr.flush();
                }
            }
            while (str != null && kill == 0) {
                logger.debug(customMsg.toString() + "Reseting string".toString());
                logger.info(customMsg.toString() + "Client started".toString());
                str = "[CA]";
                logger.debug(customMsg.toString() + "Refresh code sent".toString());
                wr.write(cmdp + "refresh" + cmds + "\n");
                wr.flush();
                try {
                    Thread.sleep(setthrottle);
                    if (throttle >= 1 && throttle < 10000) {
                        logger.debug(customMsg.toString() + "Throttling connection".toString());
                        setthrottle = setthrottle + throttle;
                    }
                    i = -1;
                    while (!str.equals(cmdp + "GA" + cmds)) {
                        logger.debug(customMsg.toString() + "Waiting for server to go ahead - Looping".toString());
                        if (i != -1) {
                            i++;
                            logger.debug(customMsg.toString() + "Testing to see whether remotebanned is being accessed".toString());
                            if (TFSNData.remotebanned[0] == "unlock" || TFSNData.remotebanned[0] == "adding") {
                                logger.debug(customMsg.toString() + "Testing to see whether remotebanned is being accessed by this own thread".toString());
                                if (TFSNData.remotebanned[0] == "unlock") {
                                    logger.debug(customMsg.toString() + "Locking remotebanned array".toString());
                                    TFSNData.remotebanned[0] = "adding";
                                }
                                logger.debug(customMsg.toString() + "Checking remotebanned length".toString());
                                if (i < TFSNData.remotebanned.length) {
                                    logger.debug(customMsg.toString() + "Writing a ban to remotebanned array".toString());
                                    TFSNData.remotebanned[i] = str;
                                } else {
                                    logger.debug(customMsg.toString() + "Unlocking remotebanned array".toString());
                                    TFSNData.remotebanned[0] = "unlock";
                                }
                            }
                        }
                        if (str.equals(cmdp + "banned" + cmds)) {
                            logger.debug(customMsg.toString() + "Banned command received".toString());
                            i++;
                        }
                        if (str.equals(cmdp + "down" + cmds)) {
                            logger.debug(customMsg.toString() + "Throttle down command received".toString());
                            throttle = 1000;
                        }
                        if (str.equals(cmdp + "hold" + cmds)) {
                            logger.debug(customMsg.toString() + "Throttle hold command received".toString());
                            throttle = 0;
                        }
                        if (str.equals(cmdp + "log1" + cmds)) {
                            logger.debug(customMsg.toString() + "Server message log enabled".toString());
                            logging = 1;
                        }
                        if (str.equals(cmdp + "log0" + cmds)) {
                            logger.debug(customMsg.toString() + "Server message log disabled".toString());
                            logging = 0;
                        }
                        if (str.equals(cmdp + "kill" + cmds)) {
                            logger.debug(customMsg.toString() + "Kill command received".toString());
                            logger.info(customMsg.toString() + "Killed by server".toString());
                            kill = 1;
                        }
                        if (logging == 1) {
                            logger.info(customMsg.toString() + "By server: " + str.toString());
                        }
                        logger.debug(customMsg.toString() + "Waiting for server to send a command".toString());
                        str = rd.readLine();
                    }
                } catch (IOException e) {
                    logger.error(e.toString());
                } catch (InterruptedException e) {
                    logger.debug(customMsg.toString() + "This is not an error. A timeout was sent because nothing was received.".toString());
                    logger.error(e.toString());
                }
            }
            logger.debug(customMsg.toString() + "Closing connection".toString());
            logger.info(customMsg.toString() + "Client disconnected".toString());
            rd.close();
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }
}

class TFSNRWFile implements Runnable {

    Thread TSN;

    TFSNRWFile() {
        TFSNRWFileGo();
    }

    public void TFSNRWFileGo() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNRWFile ";
        for (int x = 0; x < TFSNData.banned.length; x++) {
            logger.fatal(customMsg.toString() + "Defining banned array".toString());
            TFSNData.banned[x] = "myname";
        }
        for (int x = 0; x < TFSNData.remotebanned.length; x++) {
            logger.fatal(customMsg.toString() + "Defining remote banned array".toString());
            TFSNData.remotebanned[x] = "myname";
        }
        TSN = new Thread(this, "TFSN Readwritefile Description");
        TSN.start();
    }

    public void run() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNRWFile thread ";
        logger.debug(customMsg.toString() + "Read Write thread started".toString());
        logger.debug(customMsg.toString() + "Unlocking ban array".toString());
        TFSNData.remotebanned[0] = "open";
        TFSNData.remotebanned[0] = "unlock";
        BufferedReader in;
        BufferedWriter out;
        while (true) {
            logger.debug(customMsg.toString() + "Looping".toString());
            try {
                Thread.sleep(1500);
                in = new BufferedReader(new FileReader("banned.txt"));
                logger.debug(customMsg.toString() + "Creating buffer".toString());
                logger.debug(customMsg.toString() + "Initalizing varables".toString());
                String str;
                int i = 0;
                while (TFSNData.remotebanned[0] == "adding") logger.debug(customMsg.toString() + "Locked".toString());
                logger.debug(customMsg.toString() + "Locking".toString());
                TFSNData.remotebanned[0] = "lock";
                while ((str = in.readLine()) != null) {
                    logger.debug(customMsg.toString() + "Reading file".toString());
                    if (i < TFSNData.banned.length) {
                        TFSNData.banned[i] = str;
                        i = i + 1;
                        for (int x = 0; x < TFSNData.remotebanned.length; x++) {
                            logger.debug(customMsg.toString() + "Comparing banned to remote banned".toString());
                            if (TFSNData.lr(i, x) == 0) {
                                logger.debug(customMsg.toString() + "Duplicate found".toString());
                                TFSNData.remotebanned[x] = "myname";
                            }
                        }
                    }
                }
                for (; i < TFSNData.banned.length; i++) {
                    logger.debug(customMsg.toString() + "Clearing the rest".toString());
                    TFSNData.banned[i] = "myname";
                }
                logger.debug(customMsg.toString() + "Closing file".toString());
                in.close();
                out = new BufferedWriter(new FileWriter("banned.txt", true));
                for (int x = 0; x < TFSNData.remotebanned.length; x++) {
                    logger.debug(customMsg.toString() + "Found a new ban candidate".toString());
                    if (TFSNData.remotebanned[x] != "myname") {
                        System.out.println(TFSNData.remotebanned[x]);
                        out.write(TFSNData.remotebanned[x] + "\n");
                        logger.info(customMsg.toString() + TFSNData.remotebanned[x].toString());
                        TFSNData.remotebanned[x] = "myname";
                    }
                }
                out.close();
                logger.debug(customMsg.toString() + "Unlocking array".toString());
                TFSNData.remotebanned[0] = "unlock";
            } catch (InterruptedException e) {
                logger.error(e.toString());
            } catch (IOException e) {
                logger.error(e.toString());
            }
        }
    }
}

class TFSNLogclient implements Runnable {

    Thread TSN;

    Socket client;

    BufferedWriter wr;

    TFSNLogclient() {
        TFSNLogclientGo();
    }

    public void TFSNLogclientGo() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNLogclient ";
        TSN = new Thread(this, "TFSN Log Description");
        TSN.start();
    }

    public void run() {
        Logger logger = new Logger("Main Class");
        String customMsg = "TFSNLogclient thread ";
        try {
            InetAddress addr = InetAddress.getByName("we6jbo.mine.nu");
            client = new Socket(addr, 1965);
            wr = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            while (true) {
                wr.write(logger.reports());
                wr.flush();
            }
        } catch (IOException e) {
        }
    }
}

public class TFSN {

    public static void main(String[] args) {
        Logger logger = new Logger("Main Class");
        String customMsg = "MAIN ";
        logger.status = 1;
        TFSNData.port[0] = "0";
        new TFSNData();
        new TFSNClientSoc();
        new TFSNRWFile();
        new TFSNLogclient();
        new TFSNServerSoc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }
}
