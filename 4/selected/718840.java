package windroute;

import java.io.*;
import com.siemens.icm.io.*;
import com.siemens.icm.io.file.*;
import javax.microedition.midlet.*;
import javax.microedition.io.*;

public class WindRoute extends MIDlet {

    private SyncAtCommand ata;

    private ATListenerA lta;

    private String URCResponse, URCResponse2;

    private int[] ADCValue;

    private boolean[] kvadrant;

    private boolean readEvents;

    private int nIterations;

    private int windAverageDirection;

    private double windAverageSpeed, windMinSpeed, windMaxSpeed, speed, rps1, rps2;

    private int periodeSec;

    private FileConnection fileConnection;

    private OutputStream logFileOutputStream;

    private int counter1, counter2;

    private long timer1, speedTimer, speedTimerPrev, t1, t2;

    class ATListenerA implements ATCommandListener {

        String listen_for;

        public ATListenerA() {
        }

        public void ATEvent(String Event) {
            if (readEvents) {
                if (Event.indexOf("^SCPOL: 0,0") >= 0) {
                    speedTimer = System.currentTimeMillis();
                    if (counter1 > 0) {
                        t1 = t1 + speedTimer - speedTimerPrev;
                    }
                    speedTimerPrev = speedTimer;
                    counter1++;
                } else if ((Event.indexOf("^SCPOL: 1,0") >= 0) && (counter1 > 0)) {
                    speedTimer = System.currentTimeMillis();
                    t2 = t2 + (speedTimer - speedTimerPrev);
                    counter2++;
                }
            }
            if (Event.indexOf("SSCNT") >= 0) {
                URCResponse = Event;
            }
        }

        public void RINGChanged(boolean SignalState) {
        }

        public void DCDChanged(boolean SignalState) {
        }

        public void DSRChanged(boolean SignalState) {
        }

        public void CONNChanged(boolean SignalState) {
        }
    }

    /**
   * Constructor for the application, sets up all the used classes
   * oeb: NO LONGER "Sets up the ATCommand instances to be used." should it?
   */
    public WindRoute() {
        ModuleResources mr = ModuleResources.getInstance();
        ADCValue = new int[200];
        kvadrant = new boolean[4];
        nIterations = mr.getnIterations();
        periodeSec = mr.getPeriodeSec();
        try {
            initializeLogFile();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            destroyApp(false);
            notifyDestroyed();
        }
    }

    private void initializeLogFile() throws IOException {
        fileConnection = (FileConnection) Connector.open(ModuleResources.getInstance().getLogFileName());
        if (!fileConnection.exists()) {
            fileConnection.create();
        }
        logFileOutputStream = fileConnection.openOutputStream(fileConnection.fileSize());
    }

    /** Method for logging messages to a file */
    public void log(String s) throws IOException {
        System.out.println(s);
        logFileOutputStream.write((s + ".").getBytes());
        logFileOutputStream.flush();
    }

    /** Method for logging messages to a file */
    public void trace(String s) throws IOException {
        System.out.println(s);
        if (ModuleResources.getInstance().getDebugLevel() == 1) {
            logFileOutputStream.write((s + ".").getBytes());
            logFileOutputStream.flush();
        }
    }

    public void sendgrpsdata(String url) throws IOException {
        int ch;
        StreamConnection c = null;
        InputStream s = null;
        c = (StreamConnection) Connector.open(url);
        s = c.openInputStream();
        while ((ch = s.read()) != -1) {
            System.out.print((char) ch);
        }
        if (s != null) s.close();
        if (c != null) c.close();
    }

    public void sendgprsdata(String wind, String windMin, String windMax, String retning, String temp1, String temp2, String lightValue, String stationId) throws IOException {
        String url;
        url = ModuleResources.getInstance().getServerBase() + "&Vind=" + Utilities.writeUTF(wind) + "&VindMin=" + Utilities.writeUTF(windMin) + "&VindMax=" + Utilities.writeUTF(windMax) + "&Retning=" + Utilities.writeUTF(retning) + "&Temp1=" + Utilities.writeUTF(temp1) + "&Temp2=" + Utilities.writeUTF(temp2) + "&Light=" + Utilities.writeUTF(lightValue) + "&Id=" + Utilities.writeUTF(stationId);
        trace(url);
        sendgrpsdata(url);
    }

    void calculateAverages() {
        windAverageDirection = 0;
        if (kvadrant[0] & kvadrant[3] & (!kvadrant[2])) {
            for (int iIterations = 0; iIterations < nIterations; iIterations++) {
                if (ADCValue[iIterations] > 189) {
                    ADCValue[iIterations] = ADCValue[iIterations] - 255 - 7;
                }
            }
        }
        for (int iIterations = 0; iIterations < nIterations; iIterations++) {
            windAverageDirection = windAverageDirection + ADCValue[iIterations];
        }
        windAverageDirection = windAverageDirection / nIterations;
        windAverageSpeed = windAverageSpeed / nIterations;
        if (windAverageDirection < 0) {
            windAverageDirection = windAverageDirection + 255 + 7;
        }
    }

    void readMessages() throws ATCommandFailedException, java.io.IOException, java.lang.InterruptedException {
        for (int i = 1; i < 20; i++) {
            readMessage(i);
        }
    }

    void readMessage(int nMessageNo) throws ATCommandFailedException, java.io.IOException, java.lang.InterruptedException {
        String sPeriode = "";
        String sSleepValue = "";
        String sIterations = "";
        String sId = "";
        String response;
        String atBuff;
        trace("<b.1>");
        if (nMessageNo == 1) {
            response = ata.send("at^ssmss=1\r");
            Thread.currentThread().sleep(10000);
            trace(response);
            trace("<b.11>");
            response = ata.send("at+cmgf=1\r");
            Thread.currentThread().sleep(10000);
            trace(response);
        }
        trace("<b.12>");
        response = ata.send("at+cmgr=" + nMessageNo + "\r");
        Thread.currentThread().sleep(1000);
        trace(response);
        trace("<b.2>");
        int mark = -1;
        int nextMark = -1;
        int pos;
        char c;
        mark = response.indexOf("Javelin periode=");
        if (mark > 0) {
            nextMark = response.indexOf("Javelin periode=", mark + 1);
            while (nextMark > 0) {
                mark = nextMark;
                nextMark = response.indexOf("Javelin periode=", mark + 1);
            }
            for (int n = 0; n < response.toString().length() - (mark + 16); n++) {
                pos = mark + 16 + n;
                c = response.charAt(pos);
                if (c >= 48 && c <= 57) {
                    sPeriode = sPeriode + c;
                } else break;
            }
            System.out.print("New periode:");
            System.out.print(sPeriode);
            atBuff = "at+cpbw=6,\"" + sPeriode + "\",,\"Periode\"\r";
            ata.send(atBuff);
        }
        mark = -1;
        mark = response.indexOf("Javelin iterations=");
        if (mark > 0) {
            nextMark = response.indexOf("Javelin iterations=", mark + 1);
            while (nextMark > 0) {
                mark = nextMark;
                nextMark = response.indexOf("Javelin iterations=", mark + 1);
            }
            for (int n = 0; n < response.toString().length() - (mark + 19); n++) {
                pos = mark + 19 + n;
                c = response.charAt(pos);
                if (c >= 48 && c <= 57) {
                    sIterations = sIterations + c;
                } else break;
            }
            System.out.print("New iterations:");
            System.out.print(sIterations);
            atBuff = "at+cpbw=10,\"" + sIterations + "\",,\"Iteration\"\r";
            ata.send(atBuff);
        }
        trace("<b.7>");
        mark = response.indexOf("Javelin id=");
        if (mark > 0) {
            nextMark = response.indexOf("Javelin id=", mark + 1);
            while (nextMark > 0) {
                mark = nextMark;
                nextMark = response.indexOf("Javelin id=", mark + 1);
            }
            for (int n = 0; n < response.toString().length() - (mark + 11); n++) {
                pos = mark + 11 + n;
                c = response.charAt(pos);
                if (c >= 48 && c <= 57) {
                    sId = sId + c;
                } else break;
            }
            System.out.print("New id:");
            System.out.print(sId);
            atBuff = "at+cpbw=11,\"" + sId + "\",,\"ID\"\r";
            ata.send(atBuff);
        }
        mark = -1;
        atBuff = "at+cmgd=" + nMessageNo + "\r";
        trace("<b.71>" + atBuff.toString());
        response = ata.send(atBuff.toString());
        if (nMessageNo == 1) Thread.currentThread().sleep(30000);
        trace("<b.72>" + response);
    }

    String readCPBValue(int pos, String returnValueIfNotFount) throws ATCommandFailedException {
        String response = ata.send("at+cpbr=" + pos + "\r");
        String s = "+CPBR: " + pos;
        String sChar = new String("");
        char c;
        if (response.indexOf(s.toString()) != -1) {
            int mark = response.indexOf("\"");
            for (int n = 0; n < response.toString().length() - (mark + 1); n++) {
                pos = mark + 1 + n;
                c = response.charAt(pos);
                if (c != 34) {
                    sChar = sChar + c;
                } else break;
            }
            System.out.print("Found value:");
            System.out.println(sChar);
            if ((Integer.parseInt(sChar) > 0) && (Integer.parseInt(sChar) < 30200)) {
                return sChar;
            }
        }
        System.out.println("Didn't find any value, but returned default value");
        return returnValueIfNotFount;
    }

    String readPeriodeValue() throws ATCommandFailedException {
        return readCPBValue(6, "900");
    }

    String readIterationsValue() throws ATCommandFailedException {
        return readCPBValue(10, "30");
    }

    String readModuleId() throws ATCommandFailedException {
        return readCPBValue(11, ModuleResources.getInstance().getStationId());
    }

    /**
   * Method to deal with the setting up of the pin number for
   * the module... This is read in from flash memory
   * along with some other parameters...
   * @throws ATCommandFailedException
   */
    private void sendPin() throws ATCommandFailedException, IOException, java.lang.InterruptedException, ATCommandFailedException {
        String strRcv;
        System.out.println("setup pin");
        strRcv = ata.send("AT+CPIN?\r");
        if (strRcv.indexOf("ERROR") >= 0) {
            Thread.currentThread().sleep(10000);
            strRcv = ata.send("AT+CPIN?\r");
            if (strRcv.indexOf("ERROR") >= 0) throw new ATCommandFailedException("Wrong answer from module");
        }
        if (strRcv.indexOf("+CPIN: READY") < 0) {
            if (strRcv.indexOf("+CPIN: SIM PIN") >= 0) {
                System.out.println("requires pin");
                strRcv = ata.send("AT+CPIN=" + ModuleResources.getInstance().getPin() + "\r");
            } else if (strRcv.indexOf("PUK") >= 0) {
                System.out.println("PUK Code required!");
                log("PUK Code required!");
                destroyApp(true);
            } else {
                System.out.println("SIM Card required!");
                log("SIM Card required!");
                destroyApp(true);
            }
        }
    }

    private void sendLogFileToWebServer() throws IOException, ATCommandFailedException {
        log("PreSendLog2");
        InputStream i = fileConnection.openInputStream();
        byte[] b = new byte[((int) fileConnection.fileSize())];
        String buff = new String();
        i.read(b);
        for (int idx = 0, idy = 0; idx < b.length; idx++) {
            buff = buff + ((char) b[idx]);
            if (idy == 200) {
                sendgrpsdata(ModuleResources.getInstance().getServerLogBase() + "&id=" + readModuleId() + "&log=" + ModuleResources.getInstance().getVersion() + Utilities.writeUTF(buff.toString()));
                idy = 0;
                buff = "";
            }
            idy++;
        }
        sendgrpsdata(ModuleResources.getInstance().getServerLogBase() + "&id=" + readModuleId() + "&log=" + ModuleResources.getInstance().getVersion() + Utilities.writeUTF(buff.toString()));
        fileConnection.delete();
        initializeLogFile();
    }

    /**
   * Midlet is paused. Shouldn't happen?
   */
    public void pauseApp() {
        System.out.println("pause");
        try {
            log("pauseApp");
        } catch (IOException e) {
        }
    }

    /**
   * Ensures that the midlet releases resources and dies.
   * @param cond calling with true forces an exit!
   */
    public void destroyApp(boolean cond) {
        System.out.println("destroy");
        try {
            if (ata != null) {
                ata.release();
                ata = null;
            }
            log("destroy");
            logFileOutputStream.close();
            fileConnection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyDestroyed();
    }

    /**
   * Start up for the midlet.. Kicks everything off.
   * @throws MIDletStateChangeException
   */
    public void startApp() {
        String strRcv;
        long applicationStartTime, iterationStartTime, lastLogTransferTime;
        System.out.println("startApp");
        applicationStartTime = System.currentTimeMillis();
        lastLogTransferTime = System.currentTimeMillis();
        do {
            try {
                log("startApp kalt");
                lta = new ATListenerA();
                ata = new SyncAtCommand(false);
                ata.addListener(lta);
                String response;
                trace("<a.1>");
                response = ata.send("AT^SCFG=\"MEopMode/Airplane\",\"off\"\r");
                Thread.currentThread().sleep(2500);
                trace("<a.11>");
                response = ata.send("AT+CCLK=\"07/01/01,00:00:00\"\r");
                Thread.currentThread().sleep(1000);
                response = ata.send("AT+CALA=\"07/01/01,01:05:00\",0,0,\"Good Morning\"\r");
                Thread.currentThread().sleep(1000);
                trace("<a.2>");
                sendPin();
                trace("<a.3>");
                response = ata.send("AT^SJNET=GPRS," + ModuleResources.getInstance().getGprsPoint() + "," + ModuleResources.getInstance().getGprsUser() + "," + ModuleResources.getInstance().getGprsPass() + ",,10\r");
                if (response.indexOf("OK") < 0) {
                    log("GPRS Setup : NOT OK ");
                }
                trace("<a.4>");
                do {
                    int smark, emark;
                    int ofs1, ofs2, amp1, amp2, spenning;
                    iterationStartTime = System.currentTimeMillis();
                    kvadrant[0] = kvadrant[1] = kvadrant[2] = kvadrant[3] = false;
                    windAverageSpeed = 0.0;
                    windMaxSpeed = 0.0;
                    windMinSpeed = 0.0;
                    trace("<a.5>");
                    readMessages();
                    trace("<a.6>");
                    periodeSec = Integer.parseInt(readPeriodeValue());
                    nIterations = Integer.parseInt(readIterationsValue());
                    trace("<a.7>");
                    response = ata.send("AT+CCLK=\"07/01/01,00:00:00\"\r");
                    Thread.currentThread().sleep(1000);
                    response = ata.send("AT+CALA=\"07/01/01,01:05:00\",0,0,\"Good Morning\"\r");
                    Thread.currentThread().sleep(1000);
                    trace("<a.8>");
                    response = ata.send("AT^SAADC?" + "\r");
                    Thread.currentThread().sleep(1000);
                    smark = response.indexOf(',');
                    smark = response.indexOf(',', smark + 1);
                    emark = response.indexOf(',', smark + 1);
                    ofs2 = Integer.parseInt(response.substring(smark + 1, emark).trim());
                    smark = response.indexOf(',');
                    smark = response.indexOf(',', smark + 1);
                    smark = response.indexOf(',', smark + 1);
                    emark = response.indexOf(13, smark + 1);
                    amp2 = Integer.parseInt(response.substring(smark + 1, emark).trim());
                    response = ata.send("AT^SRADC=1" + "\r");
                    smark = response.indexOf(',');
                    smark = response.indexOf(',', smark + 1);
                    emark = response.indexOf(13, smark + 1);
                    spenning = Integer.parseInt(response.substring(smark + 1, emark).trim());
                    spenning = (spenning - ofs2) * amp2 / 4096;
                    trace("<a.9>");
                    double temp = 25 + (spenning - 750) / 10.0;
                    for (int iIterations = 0; iIterations < nIterations; iIterations++) {
                        if (ModuleResources.getInstance().isDavis()) {
                            trace("<a.10>");
                            response = ata.send("AT^SAADC?" + "\r");
                            smark = response.indexOf(':');
                            emark = response.indexOf(',');
                            ofs1 = Integer.parseInt(response.substring(smark + 1, emark).trim());
                            smark = response.indexOf(',');
                            emark = response.indexOf(',', smark + 1);
                            amp1 = Integer.parseInt(response.substring(smark + 1, emark).trim());
                            response = ata.send("AT^SRADC=0" + "\r");
                            smark = response.indexOf(',');
                            smark = response.indexOf(',', smark + 1);
                            emark = response.indexOf(13, smark + 1);
                            spenning = Integer.parseInt(response.substring(smark + 1, emark).trim());
                            spenning = (spenning - ofs1) * amp1 / 4096;
                            spenning = (255 * spenning) / 2900;
                            ADCValue[iIterations] = spenning;
                            if (ADCValue[iIterations] >= 0 && ADCValue[iIterations] <= 58) {
                                kvadrant[0] = true;
                            } else if (ADCValue[iIterations] > 58 && ADCValue[iIterations] <= 189) {
                                kvadrant[1] = true;
                                kvadrant[2] = true;
                            } else kvadrant[3] = true;
                            response = ata.send("AT^SCCNT=1,0\r");
                            long time1 = System.currentTimeMillis();
                            response = ata.send("AT^SSCNT=1\r");
                            Thread.currentThread().sleep(10000);
                            long time2 = System.currentTimeMillis();
                            response = ata.send("AT^SSCNT=3\r");
                            String command = "AT^SSCNT=2\r";
                            response = ata.send(command);
                            smark = URCResponse.indexOf(':');
                            emark = URCResponse.indexOf(13, smark + 1);
                            int pulse = Integer.parseInt(URCResponse.substring(smark + 1, emark).trim());
                            trace("<a.11>");
                            trace("pulses:" + pulse);
                            speed = (double) (pulse * 1032 * 1000) / (double) ((time2 - time1) * 1000);
                        } else {
                            trace("<a.11>");
                            response = ata.send("AT^SPIO=1\r");
                            Thread.currentThread().sleep(1000);
                            response = ata.send("AT^SCPIN=1,0,0\r");
                            Thread.currentThread().sleep(1000);
                            response = ata.send("AT^SCPIN=1,1,0\r");
                            Thread.currentThread().sleep(1000);
                            response = ata.send("AT^SCPOL=1,0\r");
                            Thread.currentThread().sleep(1000);
                            response = ata.send("AT^SCPOL=1,1\r");
                            Thread.currentThread().sleep(2000);
                            trace("<a.12>");
                            counter1 = counter2 = 0;
                            t1 = t2 = 0;
                            readEvents = true;
                            timer1 = System.currentTimeMillis();
                            while (System.currentTimeMillis() - timer1 < 10000) {
                            }
                            readEvents = false;
                            trace("<a.13>");
                            response = ata.send("AT^SPIO=0\r");
                            Thread.currentThread().sleep(1000);
                            if ((counter1 > 1) && (counter2 > 1)) {
                                t1 = t1 / (counter1 - 1);
                                t2 = t2 / (counter2 - 1);
                                rps1 = 1000.0 / (double) t1;
                                if ((rps1 >= 0.001) && (rps1 <= 3.229)) {
                                    speed = (-0.1095 * rps1 * rps1) + (2.9318 * rps1) - 0.1412;
                                } else if ((rps1 > 3.229) && (rps1 <= 54.362)) {
                                    speed = (0.0052 * rps1 * rps1) + (2.1980 * rps1) + 1.1091;
                                } else if ((rps1 > 54.362) && (rps1 <= 66.332)) {
                                    speed = (0.1104 * rps1 * rps1) - (9.5685 * rps1) + 329.87;
                                } else speed = 0.0;
                                speed = speed * 0.48037;
                                ADCValue[iIterations] = (int) (255 * (((double) t2) / ((double) t1)));
                            } else {
                                speed = 0.0;
                                ADCValue[iIterations] = 0;
                            }
                            trace("<a.14>");
                            trace("t1" + t1);
                            trace("t2" + t2);
                        }
                        windAverageSpeed = windAverageSpeed + speed;
                        trace("<a.12>");
                        if (iIterations == 0) {
                            windMaxSpeed = speed;
                            windMinSpeed = speed;
                        } else {
                            if (speed < windMinSpeed) windMinSpeed = speed;
                            if (speed > windMaxSpeed) windMaxSpeed = speed;
                        }
                    }
                    trace("<a.13>");
                    calculateAverages();
                    trace("<a.14>");
                    response = ata.send("AT^COPS=0\r");
                    sendgprsdata(Utilities.FormatDouble(windAverageSpeed, 2, 1), Utilities.FormatDouble(windMinSpeed, 2, 1), Utilities.FormatDouble(windMaxSpeed, 2, 1), Utilities.FormatDouble(windAverageDirection, 3, 0), "999", Utilities.FormatDouble(temp + 50, 2, 1), "0000", readModuleId());
                    trace("<a.15>");
                    if (ModuleResources.getInstance().getLogFileTransferEnabled() && System.currentTimeMillis() > lastLogTransferTime + (ModuleResources.getInstance().getLogFileTransferInterval() * 60 * 1000)) {
                        lastLogTransferTime = System.currentTimeMillis();
                        sendLogFileToWebServer();
                    }
                    response = ata.send("AT^COPS=2\r");
                    Thread.currentThread().sleep(periodeSec * 1000 - (System.currentTimeMillis() - iterationStartTime));
                    trace("<a.16>");
                } while (true);
            } catch (Throwable e) {
                try {
                    log(e.getClass().getName());
                    log(e.getMessage());
                } catch (Throwable ate) {
                }
            } finally {
                try {
                    log("AT+CFUN=0,1\n");
                    Thread.currentThread().sleep(1000);
                } catch (Throwable e) {
                }
                try {
                    strRcv = ata.send("AT+CFUN=0,1\r");
                } catch (Throwable e) {
                }
            }
        } while (true);
    }
}
