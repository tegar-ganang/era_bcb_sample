package com.sinas;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.IBattery;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.util.Utils;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * @author: Sinas
 */
public class SinasOnSpot extends MIDlet {

    private static final int HOST_PORT = 67;

    private static int nbPas = 0;

    private static final int SAMPLE_PERIOD = 1 * 100;

    private static final int SEUIL_PAS = 1;

    private Sensors TSensors;

    private TestConnexion TConnexion;

    protected void startApp() throws MIDletStateChangeException {
        RadiogramConnection rCon = null;
        Datagram dg = null;
        String ourAddress = System.getProperty("IEEE_ADDRESS");
        long now = 0L;
        IBattery batt = Spot.getInstance().getPowerController().getBattery();
        int reading = 0;
        ITriColorLED[] leds = EDemoBoard.getInstance().getLEDs();
        System.out.println("Starting sensor sampler application on " + ourAddress + " ...");
        try {
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            System.exit(1);
        }
        TSensors = new Sensors();
        TSensors.start();
        TConnexion = new TestConnexion();
        TConnexion.start();
        while (true) {
            try {
                now = System.currentTimeMillis();
                reading = batt.getBatteryLevel();
                leds[7].setRGB(255, 0, 0);
                leds[7].setOn();
                dg.reset();
                dg.writeLong(now);
                dg.writeInt(reading);
                dg.writeDouble(TSensors.valeur_telemetrie);
                if (TSensors.valeur_telemetrie > SEUIL_PAS) {
                    if (TConnexion.connected) {
                        nbPas++;
                        nbPas = 0;
                    } else {
                        nbPas++;
                    }
                } else {
                }
                TSensors.valeur_telemetrie = 0;
                rCon.send(dg);
                System.out.println("Battery level = " + reading);
                leds[7].setOff();
                Utils.sleep(SAMPLE_PERIOD - (System.currentTimeMillis() - now));
            } catch (Exception e) {
                System.err.println("Caught " + e + " while collecting/sending sensor sample.");
            }
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
    }
}
