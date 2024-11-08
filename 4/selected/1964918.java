package com.sun.spot.sensorboard.util;

import com.sun.spot.peripheral.external.ISPI;
import com.sun.spot.sensorboard.hardware.Atmega;
import com.sun.spot.sensorboard.hardware.IAtmegaCode;
import com.sun.spot.util.Utils;

/**
 * Utility to ease programming the user space of the Atmega88.
 * Think of this class as reflecting the capabilities of the bootloader section
 * of the Atmega88 firmware. cause it does.
 * @author arshan
 */
public class AtmegaBootloader {

    private ISPI avrSpi;

    private ISPI BLSpi;

    private int ledstate = 0x0;

    private int pdiv = 1;

    private int progress = 0;

    private int bar = 1;

    /**
     * Creates a new instance of AtmegaBootloader
     */
    public AtmegaBootloader() {
    }

    /**
     * setup the progress bar variables
     */
    private void setupProgressBar(int num) {
        pdiv = num / 8;
    }

    /**
     * a slice of progress has occured
     */
    private void progress() {
        if (progress++ >= pdiv) {
            progress = 0;
            ledstate |= bar;
            bar <<= 1;
            setLEDS();
        }
    }

    private void cheap_progress() {
        ledstate <<= 1;
        if (ledstate > 0xffff) {
            ledstate = 1;
        }
        setLEDS();
    }

    /**
     * not official progress, but indication of life
     */
    private void heartbeat() {
        ledstate ^= 0xff00;
    }

    private void setLEDS() {
        byte[] snd = new byte[3];
        byte[] rcv = new byte[3];
        snd[0] = (byte) 6;
        snd[1] = (byte) (ledstate >> 8);
        snd[2] = (byte) ledstate;
        avrSpi.sendSPICommand(snd, 3, rcv, 3);
    }

    /**
     * given
     */
    public void flash(Atmega part, IAtmegaCode code) {
        System.out.println("starting program run");
        byte[] snd = new byte[4];
        byte[] rcv = new byte[4];
        int xor_val = 0;
        avrSpi = part.getSPI();
        BLSpi = part.getProgrammingSPI();
        System.out.println("starting bootloading");
        BLSpi.send16bits(9);
        Utils.sleep(100);
        byte[] hex = code.getArray();
        int pages = hex.length / 64;
        if ((hex.length % 64) > 0) pages += 1;
        snd[0] = (byte) 0;
        rcv[0] = (byte) 0xFF;
        avrSpi.sendSPICommand(snd, 1, rcv, 1);
        System.out.println("Verified bootloader version : " + rcv[0]);
        System.out.println("ready to write " + pages + " pages");
        setupProgressBar(pages);
        int leds = 1;
        int y = 0;
        int tries = 0;
        for (int p = 0; p < pages && tries <= 3; p++) {
            boolean ok = true;
            xor_val = 0;
            progress();
            snd[0] = (byte) 1;
            snd[1] = (byte) p;
            rcv[0] = (byte) 0x05;
            avrSpi.sendSPICommand(snd, 2, rcv, 2);
            for (int x = 0; x < 64; x++) {
                y = (p * 64) + x;
                if (y >= hex.length) {
                    snd[0] = (byte) 0xFF;
                } else {
                    snd[0] = hex[y];
                }
                avrSpi.sendSPICommand(snd, 1, rcv, 1);
                if (x > 0 && xor_val != rcv[0]) {
                    if (ok) {
                        ok = false;
                        System.out.println("ERROR: received bad value from the Atmega microcontroller");
                    }
                }
                xor_val ^= snd[0];
                if (tries > 0) {
                    Utils.sleep(tries);
                }
            }
            Utils.sleep(2 + tries);
            snd[0] = (byte) 0;
            avrSpi.sendSPICommand(snd, 1, rcv, 1);
            if (xor_val != rcv[0] || !ok) {
                ok = false;
                if (tries++ >= 3) {
                    System.out.println("ERROR: too many tries to reprogram page");
                } else {
                    System.out.println("ERROR: failed to write page " + p + " --- retrying");
                    p--;
                }
            }
            rcv[2] = (byte) 0xCC;
            Utils.sleep(3 + tries);
            while (rcv[2] != 0x00) {
                Utils.sleep(1);
                snd[0] = (byte) 5;
                snd[1] = (byte) 0;
                snd[2] = (byte) 0;
                avrSpi.sendSPICommand(snd, 3, rcv, 3);
            }
            if (ok) {
                System.out.println("page " + p + " write complete");
                tries = 0;
            }
        }
        if (tries <= 3) {
            System.out.println("done programming pages");
        } else {
            System.out.println("ERROR: failed to upgrade sensor board firmware!");
        }
        Utils.sleep(10);
        snd[0] = (byte) 3;
        rcv[0] = (byte) 0xFF;
        System.out.println("putting atmega back into run mode");
        avrSpi.sendSPICommand(snd, 2, rcv, 2);
        System.out.println("done flashing hex");
    }
}
