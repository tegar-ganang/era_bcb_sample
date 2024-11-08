package de.genodeftest.k8055_old;

import java.io.IOException;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

public abstract class JWrapperK8055 {

    public enum DigitalOutput implements K8055Channel {

        CHANNEL1((short) 1), CHANNEL2((short) 2), CHANNEL3((short) 3), CHANNEL4((short) 4), CHANNEL5((short) 5), CHANNEL6((short) 6), CHANNEL7((short) 7), CHANNEL8((short) 8);

        public final short channelNo;

        /**
		 * Datentyp f�r die Adresse der digitalen Ausg�nge
		 * 
		 * @param channelNo einer der 8 Ausg�nge 1-8
		 */
        DigitalOutput(short channelNo) {
            this.channelNo = channelNo;
        }

        public static DigitalOutput getChannelForIndex(int channelNo) {
            switch(channelNo) {
                case 1:
                    return CHANNEL1;
                case 2:
                    return CHANNEL2;
                case 3:
                    return CHANNEL3;
                case 4:
                    return CHANNEL4;
                case 5:
                    return CHANNEL5;
                case 6:
                    return CHANNEL6;
                case 7:
                    return CHANNEL7;
                case 8:
                    return CHANNEL8;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public ChannelDir getChannelDir() {
            return ChannelDir.OUTPUT;
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.DIGITAL;
        }
    }

    public enum DigitalInput implements K8055Channel {

        CHANNEL1((short) 1), CHANNEL2((short) 2), CHANNEL3((short) 3), CHANNEL4((short) 4), CHANNEL5((short) 5);

        public final short channelNo;

        /**
		 * Datantyp f�r die Adresse der digitale Eing�nge
		 * 
		 * @param channelNo Einer der Kan�le 1-5
		 */
        DigitalInput(short channelNo) {
            this.channelNo = channelNo;
        }

        public static DigitalInput getChannelForIndex(int channelNo) {
            switch(channelNo) {
                case 1:
                    return CHANNEL1;
                case 2:
                    return CHANNEL2;
                case 3:
                    return CHANNEL3;
                case 4:
                    return CHANNEL4;
                case 5:
                    return CHANNEL5;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public ChannelDir getChannelDir() {
            return ChannelDir.INPUT;
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.DIGITAL;
        }
    }

    /**
	 * The analog output.
	 * 
	 * @author genodeftest (Christian Stadelmann)
	 * @since 24.11.2009
	 */
    public enum AnalogOutput implements K8055Channel {

        CHANNEL1((short) 1), CHANNEL2((short) 2);

        public static final short MIN_VALUE = 0;

        public static final short MAX_VALUE = 255;

        public final short channelNo;

        private static double maximumVoltage = 5;

        /**
		 * Datentyp zur Addressierung der analogen Ausg�nge
		 * 
		 * @param channelNo 1 oder 2
		 */
        AnalogOutput(short channelNo) {
            this.channelNo = channelNo;
        }

        public static AnalogOutput getChannelForIndex(int channelNo) {
            switch(channelNo) {
                case 1:
                    return CHANNEL1;
                case 2:
                    return CHANNEL2;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public ChannelDir getChannelDir() {
            return ChannelDir.OUTPUT;
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.ANALOG;
        }

        public static String toRepresentingString(short data) {
            return (Integer.toString(data) + "/255 " + convertValueToVoltage(data) + "V ");
        }

        public static double convertValueToVoltage(short data) {
            return (((int) (data * maximumVoltage / MAX_VALUE) * 10)) / 10;
        }

        public static int convertVoltageToValue(double voltage) {
            return ((int) (voltage * MAX_VALUE / maximumVoltage + 0.5));
        }

        public void setMaximumVoltage(double newMaximumVoltage) throws Exception {
            if (newMaximumVoltage < 0) {
                throw new Exception("Die Referenzspannung kann nicht so niedrig fallen!");
            }
            maximumVoltage = newMaximumVoltage;
        }

        public double getMaximumVoltage() {
            return maximumVoltage;
        }
    }

    /**
	 * The analog input. It is measured with 8Bit precision from 0 to 255. If the K8055 Board is
	 * built up with no changes, you can calculate the input voltage like this:
	 * <p>
	 * <b>U<sub>In</sub> = 5V/256*f<sub>Analog</sub></b>
	 * <p>
	 * <b>U<sub>In</sub></b> Input Voltage (in Volts)<br>
	 * <b>f<sub>Analog</sub></b> the factor you get by calling
	 * {@link JWrapperK8055#readAnalogInput(AnalogInput)}
	 * <p>
	 * This depends on the gain you use with the K8055 Board.
	 * 
	 * @author genodeftest (Christian Stadelmann)
	 * @since 24.11.2009
	 */
    public enum AnalogInput implements K8055Channel {

        CHANNEL1((short) 1), CHANNEL2((short) 2);

        public static final short MIN_VALUE = 0;

        public static final short MAX_VALUE = 255;

        public final short channelNo;

        private static double maximumVoltage = 5;

        AnalogInput(short channelNo) {
            this.channelNo = channelNo;
        }

        public static AnalogInput getChannelForIndex(int channelNo) {
            switch(channelNo) {
                case 1:
                    return CHANNEL1;
                case 2:
                    return CHANNEL2;
                default:
                    throw new IllegalArgumentException();
            }
        }

        public ChannelDir getChannelDir() {
            return ChannelDir.INPUT;
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.ANALOG;
        }

        public static String toRepresentingString(short data) {
            return (Integer.toString(data) + "/255 " + convertValueToVoltage(data) + "V ");
        }

        public static String toRepresentingString(double voltage) {
            return (Integer.toString(convertVoltageToValue(voltage)) + "/255 " + voltage + "V ");
        }

        public static double convertValueToVoltage(short data) {
            return ((double) ((int) (data * maximumVoltage / MAX_VALUE * 100)) / 100);
        }

        public static int convertVoltageToValue(double voltage) {
            return ((int) (voltage * MAX_VALUE / maximumVoltage + 0.5));
        }

        public void setMaximumVoltage(double newMaximumVoltage) throws Exception {
            if (newMaximumVoltage < 0) {
                throw new Exception("Die Referenzspannung kann nicht so niedrig fallen!");
            }
            maximumVoltage = newMaximumVoltage;
        }

        public double getMaximumVoltage() {
            return maximumVoltage;
        }
    }

    /**
	 * The counter input uses the first and the second digital input channel.
	 * <p>
	 * The value you read with {@link JWrapperK8055#readCounter(Counter) readCounter()} depends
	 * on the debounce time you can set with
	 * {@link JWrapperK8055#setCounterDebounceTime(Counter, short) setCounterDebounceTime()}.
	 * 
	 * @author genodeftest (Christian Stadelmann)
	 * @since 24.11.2009
	 */
    public enum Counter implements K8055Channel {

        CHANNEL1((short) 1), CHANNEL2((short) 2);

        public final short channelNo;

        /**
		 * Minimum value for counter value
		 */
        public static final long MIN_VALUE = 0;

        /**
		 * Maximum value for counter value
		 */
        public static final long MAX_VALUE = Long.MAX_VALUE;

        /**
		 * Minimum value for debounce time in milliseconds
		 */
        public static final short DEBOUNCETIME_MINVALUE = 0;

        /**
		 * Maximum value for debounce time in milliseconds
		 */
        public static final short DEBOUNCETIME_MAXVALUE = 5000;

        Counter(short channelNo) {
            this.channelNo = channelNo;
        }

        public static Counter getChannelForIndex(int channelNo) {
            switch(channelNo) {
                case 1:
                    return CHANNEL1;
                case 2:
                    return CHANNEL2;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public ChannelDir getChannelDir() {
            return ChannelDir.INPUT;
        }

        @Override
        public ChannelType getChannelType() {
            return ChannelType.COUNTER;
        }
    }

    private K8055AddressState boardAddress = K8055AddressState.NOT_CONNECTED;

    private boolean toBeReturnedByAutoconnect = false;

    /**
	 * Suche der aktiven de.genodeftest.K8055 Devices
	 * 
	 * @return Vector mit referenzen zur SKAddresse aller angeschlossenen betriebsbereiten
	 *         Ger�te, null bei Fehlern oder wenn kein Ger�t angeschlossen ist
	 */
    public ArrayList<K8055AddressState> searchDevices() {
        ArrayList<K8055AddressState> address = new ArrayList<K8055AddressState>();
        int a = 0;
        try {
            a = (int) searchDevices0();
            if (a < 1) {
                return null;
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Suchen von angeschlossenen Devices\n" + e.getStackTrace());
            return null;
        }
        if (a % 2 == 1) {
            address.add(K8055AddressState.CONNECT_WITH_SK0);
        }
        a = a / 2;
        if (a % 2 == 1) {
            address.add(K8055AddressState.CONNECT_WITH_SK1);
        }
        a = a / 2;
        if (a % 2 == 1) {
            address.add(K8055AddressState.CONNECT_WITH_SK2);
        }
        a = a / 2;
        if (a % 2 == 1) {
            address.add(K8055AddressState.CONNECT_WITH_SK3);
        }
        return address;
    }

    protected abstract long searchDevices0();

    /**
	 * Versucht, ein K8055 oder VM110 Device zu finden, das an den Computer angeschlossen ist.
	 * Es wird automatisch eine Verbindung mit dem Angeschlossenen Device aufgebaut. Falls
	 * mehrere Devices angeschlossen sind, wird der Benutzer zur Auswahl aufgefordert.
	 * 
	 * @return true, wenn eine Verbindung hergestellt wurde<br>
	 *         false bei Fehlern oder wenn keine Verbindung hergestellt werden konnte
	 */
    public boolean autoConnect() {
        final ArrayList<K8055AddressState> addressen;
        System.out.println("OK 0");
        try {
            addressen = searchDevices();
        } catch (Exception e) {
            return false;
        }
        System.out.println("OK 1");
        if (addressen == null) {
            return false;
        }
        System.out.println("OK 2");
        System.out.println(addressen);
        short vSize = (short) addressen.size();
        if (vSize == 1) {
            return openDevice(addressen.get(0));
        }
        System.out.println("OK 3");
        Box box = new Box(BoxLayout.Y_AXIS);
        JTextComponent info = new JTextArea("Es wurden " + vSize + " Ger�te gefunden.\nDieses Programm unterst�tzt jedoch nur ein Board.\nWenn sie mehrere Boards �ffnen wollen, m�ssen sie mehrere Instanzen\ndieses Programmes �ffnen und die gew�nschte Addresse einstellen.\ndieses Programmes �ffnen und die gew�nschte Addresse einstellen.\ndieses Programmes �ffnen und die gew�nschte Addresse einstellen.\n\nSie haben folgende Boards zur Auswahl, bitte eines ausw�hlen\n");
        info.setEditable(false);
        info.setBackground(box.getBackground());
        box.add(info);
        final JComboBox addressWahl = new JComboBox();
        String[] items = new String[addressen.size()];
        for (int i = 0; i < vSize; i++) {
            items[i] = new String("SK" + addressen.get(i).getAdressNumber());
        }
        addressWahl.setModel(new DefaultComboBoxModel(items));
        addressWahl.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent e) {
                int selectedItem = addressWahl.getSelectedIndex();
                if (selectedItem == -1) {
                    toBeReturnedByAutoconnect = false;
                }
                toBeReturnedByAutoconnect = connectWith(addressen.get(selectedItem));
            }
        });
        box.add(addressWahl);
        JOptionPane.showConfirmDialog(null, box);
        return toBeReturnedByAutoconnect;
    }

    /**
	 * �ffnet die Verbindung zum angegebenen Device
	 * 
	 * @param address die Adresse des Boards, mit dem der Treiber eine Verbindung aufbauen soll
	 * @return true wenn �ffnen gegl�ckt <br>
	 *         false wenn �ffnen gescheiter
	 */
    public boolean openDevice(K8055AddressState address) {
        if (address.getAdressNumber() == -1) throw new IllegalArgumentException("Device K8055AddressState.NOT_CONNECTED can not be connected");
        try {
            closeDevice0();
            long l = openDevice0(address.getAdressNumber());
            if (l == -1 || l != address.getAdressNumber()) {
                boardAddress = K8055AddressState.NOT_CONNECTED;
                return false;
            }
            boardAddress = address;
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim �ffnen des Devices: " + address.getAdressNumber() + e.getStackTrace());
            return false;
        }
    }

    protected abstract long openDevice0(long address);

    /**
	 * schlie�t die Verbindung zum aktuellen Device
	 * 
	 * @return true wenn Schlie�en gegl�ckt<br>
	 *         false wenn Schlie�en gescheiter
	 */
    public boolean closeDevice() {
        try {
            closeDevice0();
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Schlie�en des Devices:" + e.getStackTrace());
            return false;
        }
    }

    protected abstract void closeDevice0();

    /**
	 * Lesen des analogen Eingangssignals am angegebenen Channel und Umwandlung der
	 * Eingangsspannung in einem Wert 0-255
	 * 
	 * @param input analoger Eingangskanal
	 * @return -1 bei Fehler beim Auslesen des Wertes<br>
	 *         ansonsten: ein Wert 0-255 der Die Eingangsspannung von 0V-5V repr�sentiert
	 */
    public short readAnalogInput(AnalogInput input) {
        return readAnalogInput(input.channelNo);
    }

    /**
	 * Lesen des analogen Eingangssignals am angegebenen Channel und Umwandlung der
	 * Eingangsspannung in einem Wert 0-255
	 * 
	 * @param channelNo analoger Eingangskanal 1 oder 2
	 * @return -1 bei Fehler beim Auslesen des Wertes<br>
	 *         ansonsten: ein Wert 0-255 der Die Eingangsspannung von 0V-5V repr�sentiert
	 */
    short readAnalogInput(int channelNo) {
        short data;
        try {
            data = (short) readAnalogChannel0(channelNo + 1);
        } catch (Exception e) {
            System.err.println("Fehler beim Auslesen des Analogen Kanals " + channelNo + " : " + e.getStackTrace());
            return -1;
        }
        return data;
    }

    protected abstract long readAnalogChannel0(long channelNo);

    /**
	 * Lie�t alle analogen Eing�nge aus und gibt das Ergebnis als byte[] aus
	 * 
	 * @return Eingelesene Werte der analogen Eing�nge
	 */
    public short[] readAllAnalogInput() {
        long[] i = new long[2];
        short[] a = new short[2];
        try {
            i = readAllAnalog0();
            a[0] = (short) i[0];
            a[1] = (short) i[1];
        } catch (Exception e) {
            a[0] = -1;
            a[1] = -1;
        }
        return a;
    }

    protected abstract long[] readAllAnalog0();

    /**
	 * Setzt die Ausgangsspannung am analogen Ausgang
	 * 
	 * @param channelNo einer der analogen Ausgangskan�le
	 * @param data Ein Wert 0-255 der die Ausgangsspannung von 0V-5V repr�sentiert
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 * @throws Exception wenn data nicht im Bereich 0-255
	 */
    boolean setAnalogOutput(int channelNo, short data) {
        if (data < AnalogOutput.MIN_VALUE || data > AnalogOutput.MAX_VALUE || channelNo < 1 || channelNo > 2) {
            throw new IllegalArgumentException();
        }
        try {
            setAnalogChannel0(channelNo, data);
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen des analogen Ausgangs " + channelNo + " \n" + e.getStackTrace());
            return false;
        }
        return true;
    }

    protected abstract void setAnalogChannel0(long channelNo, long value);

    /**
	 * Setzt die Ausgangsspannung am analogen Ausgang
	 * 
	 * @param channel einer der analogen Ausgangskan�le
	 * @param value Ein Wert 0-255 der die Ausgangsspannung von 0V-5V repr�sentiert
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAnalogOutput(AnalogOutput channel, short value) {
        try {
            return setAnalogOutput(channel.channelNo, value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Setzt die Ausgangsspannung beider analoger Ausg�nge
	 * 
	 * @param data1 Ein Wert 0-255 der die Ausgangsspannung an Channel 1 von 0V-5V repr�sentiert
	 * @param data2 Ein Wert 0-255 der die Ausgangsspannung an Channel 2 von 0V-5V repr�sentiert
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAllAnalogOutput(short data1, short data2) {
        if (data1 < AnalogOutput.MIN_VALUE | data2 < AnalogOutput.MIN_VALUE | data1 > AnalogOutput.MAX_VALUE | data2 > AnalogOutput.MAX_VALUE) throw new IllegalArgumentException("Value of data out of range(0-255)");
        try {
            setAllAnalogOutput0(data1, data2);
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der Spannungswerte am analogen Ausgang: " + e.getStackTrace());
            return false;
        }
    }

    protected abstract void setAllAnalogOutput0(long value1, long value2);

    /**
	 * Setzen der Digitalen Channel Bit-Codiert (1=ON 0=OFF) Wert 0-255<br>
	 * Channel 1: x*1<br>
	 * Channel 2: x*2<br>
	 * Channel 3: x*4<br>
	 * Channel 4: x*8<br>
	 * Channel 5: x*16<br>
	 * Channel 6: x*32<br>
	 * Channel 7: x*64<br>
	 * Channel 8: x*128<br>
	 * Bsp data = 7 : ON-ON-ON-OFF-OFF-OFF-OFF-OFF
	 * 
	 * @param data
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAllDigitalOutput(short data) {
        try {
            setAllDigital0(data);
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der Spannungswerte am digitalen Ausgang: " + data + "\n" + e.getStackTrace());
            return false;
        }
    }

    protected abstract void setAllDigital0(long data);

    /**
	 * Setzen der Digitalen Channel true = ON, false = OFF
	 * 
	 * @param channels 8 Wahrheitswerte, die den Status des Kanals repr�sentieren (true = ON,
	 *        false = OFF)
	 * @return true wenn der Wert erfolgreich gesetzt wurde<br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAllDigitalOutput(boolean[] channels) {
        if (channels.length != 8) {
            throw new IllegalArgumentException();
        }
        short bitcode = 0;
        for (int i = 0; i < 8; i++) {
            if (channels[i]) {
                bitcode = (short) (bitcode + Math.pow(2, i));
            }
        }
        return setAllDigitalOutput(bitcode);
    }

    /**
	 * Setzen der Digitalen Channel true = ON, false = OFF
	 * 
	 * @param channels 8 Wahrheitswerte, die den Status des Kanals repr�sentieren (true = ON,
	 *        false = OFF)
	 * @return true wenn der Wert erfolgreich gesetzt wurde<br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAllDigitalOutput(Boolean[] channels) {
        if (channels.length != 8) {
            throw new IllegalArgumentException();
        }
        short bitcode = 0;
        for (int i = 0; i < 8; i++) {
            if (channels[i]) {
                bitcode += Math.pow(2, i);
            }
        }
        return setAllDigitalOutput(bitcode);
    }

    /**
	 * Setzen der Digitalen Ausg�nge true = ON, false = OFF
	 * 
	 * @param value1 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value2 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value3 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value4 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value5 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value6 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value7 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @param value8 Wahrheitswert der den Status des digitalen Ausgangs repr�sentiert (true =
	 *        ON, false = OFF)
	 * @return true wenn der Wert erfolgreich gesetzt wurde<br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setAllDigitalOutput(boolean value1, boolean value2, boolean value3, boolean value4, boolean value5, boolean value6, boolean value7, boolean value8) {
        short bitcode = 0;
        if (value1) bitcode += Math.pow(2, 0);
        if (value2) bitcode += Math.pow(2, 1);
        if (value3) bitcode += Math.pow(2, 2);
        if (value4) bitcode += Math.pow(2, 3);
        if (value5) bitcode += Math.pow(2, 4);
        if (value6) bitcode += Math.pow(2, 5);
        if (value7) bitcode += Math.pow(2, 6);
        if (value8) bitcode += Math.pow(2, 7);
        return setAllDigitalOutput(bitcode);
    }

    /**
	 * Diese Methode setzt den Wert eines Digitalen Ausgangs auf ON (true) odef OFF (false)
	 * 
	 * @param channel der digitale Ausgang
	 * @param value ON = true; OFF = false
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern
	 */
    public boolean setDigitalOutput(DigitalOutput channel, boolean value) {
        return setDigitalOutput(channel.channelNo, value);
    }

    /**
	 * Diese Methode setzt den Wert eines Digitalen Ausgangs auf ON (true) odef OFF (false)
	 * 
	 * @param channel Nummer des digitale Ausgangs
	 * @param value ON = true; OFF = false
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern
	 */
    boolean setDigitalOutput(int channelNo, boolean value) {
        if (channelNo < 1 || channelNo > 8) throw new IllegalArgumentException();
        try {
            if (value) setDigitalChannel0(channelNo); else clearDigitalChannel0(channelNo);
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der Spannungswerte am digitalen Ausgang: " + channelNo + "\n" + e.getStackTrace());
            return false;
        }
    }

    protected abstract void setDigitalChannel0(long channelNo);

    protected abstract void clearDigitalChannel0(long channelNo);

    /**
	 * Setzt alle Digitalen Ausg�nge auf den Wert <code>value</code>
	 * 
	 * @param value der neue Wert aller digitalen Ausg�nge
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern
	 */
    public boolean setAllDigitalOutput(boolean value) {
        try {
            if (value) setAllDigital0(); else clearAllDigital0();
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Setzen der Spannungswerte am digitalen Ausgang: " + e.getStackTrace());
            return false;
        }
    }

    protected abstract void setAllDigital0();

    protected abstract void clearAllDigital0();

    /**
	 * Lesen des Status des ausgew�hlten digitalen Kanals
	 * 
	 * @param channel ein digitaler Eingang
	 * @return true = ON , false = OFF
	 * @throws IOException bei fehlerhafter Verbindung
	 */
    public boolean readDigitalInput(DigitalInput channel) throws IOException {
        return readDigitalInput(channel.channelNo);
    }

    /**
	 * Lesen des Status des ausgew�hlten digitalen Kanals
	 * 
	 * @param channel Nummer des digitalen Eingangs(1 bis 8)
	 * @return true = ON , false = OFF
	 * @throws ConnectionException bei fehlerhafter Verbindung
	 */
    boolean readDigitalInput(int channelNo) throws IOException {
        try {
            return readDigitalChannel0(channelNo);
        } catch (Exception e) {
            throw new IOException("Fehler beim Lesen der Spannungswerte am digitalen Eingang: " + channelNo + "\n" + e.getStackTrace());
        }
    }

    protected abstract boolean readDigitalChannel0(long channelNo);

    /**
	 * Diese Methode gibt den Status aller 5 digitalen Eing�nge als Array aus.
	 * 
	 * @return true = ON<br>
	 *         false = OFF
	 * @throws IOException bei Problemen mit Aufruf der DLL
	 */
    public boolean[] readAllDigitalInput() throws IOException {
        long data = -1;
        try {
            data = readAllDigital0();
        } catch (Exception e) {
            throw new IOException();
        }
        if (data == -1) {
            throw new IOException();
        }
        boolean[] ret = new boolean[5];
        for (int i = 4; i >= 0; i--) {
            int k = (int) Math.pow(2, i);
            if (data - k >= 0) {
                data -= k;
                ret[i] = true;
            }
        }
        return ret;
    }

    protected abstract long readAllDigital0();

    /**
	 * Lesen des Inhaltes Impulsz�hlers des Digitalen Eing�nge
	 * 
	 * @param channel ein Counter-Kanal
	 * @return Z�hler
	 */
    public long readCounter(Counter channel) {
        return readCounter(channel.channelNo);
    }

    /**
	 * Lesen des Inhaltes Impulsz�hlers des Digitalen Eing�nge
	 * 
	 * @param channelNo Nummer des Counter-Kanals(1 0der 2)
	 * @return Z�hler
	 */
    long readCounter(int channelNo) {
        long d;
        try {
            d = readCounter0(channelNo);
            return d;
        } catch (Exception e) {
            System.err.println("Fehler beim Lesen der Spannungswerte am digitalen Eingang: " + channelNo + "\n" + e.getStackTrace());
            return -1;
        }
    }

    protected abstract long readCounter0(long channelNo);

    /**
	 * Zur�cksetzen des Impulsz�hlers des Digitalen Z�hlers
	 * 
	 * @param channel ein Counter-Kanal
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean resetCounter(Counter channel) {
        return resetCounter(channel.channelNo);
    }

    /**
	 * Zur�cksetzen des Impulsz�hlers des Digitalen Z�hlers
	 * 
	 * @param channelNo nummer des Kanals
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    boolean resetCounter(int channelNo) {
        try {
            resetCounter0(channelNo);
            System.out.println("Reset Counter " + channelNo);
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Zur�cksetzen des Z�hlers " + channelNo + "\n" + e.getStackTrace());
            return false;
        }
    }

    protected abstract void resetCounter0(long channelNo);

    /**
	 * Setzen der Entprellzeit(Toleranz) zwischen den Impulsen des Impulsz�hlers des Digitalen
	 * Z�hlers
	 * 
	 * @param channel ein Counter-Kanal
	 * @param debounceTimeMillis Entprellzeit in millisekunden
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    public boolean setCounterDebounceTime(Counter channel, short debounceTimeMillis) {
        return setCounterDebounceTime(channel.channelNo, debounceTimeMillis);
    }

    /**
	 * Setzen der Entprellzeit(Toleranz) zwischen den Impulsen des Impulsz�hlers des Digitalen
	 * Z�hlers
	 * 
	 * @param channelNo ein Counter-Kanal
	 * @param debounceTimeMillis Entprellzeit in millisekunden
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    boolean setCounterDebounceTime(int channelNo, int debounceTimeMillis) {
        if (debounceTimeMillis < 0) throw new IllegalArgumentException("no negative time!");
        try {
            setCounterDebounceTime0(channelNo, debounceTimeMillis);
            System.out.println("Set Counter Debounce Time of " + channelNo + " to " + debounceTimeMillis);
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim Einstellen der Entprellzeit des Z�hlers " + channelNo + "\n" + e.getStackTrace());
            return false;
        }
    }

    protected abstract void setCounterDebounceTime0(long channelNo, long debounceTimeMillis);

    /**
	 * Setzen bzw. �ndern der aktuellen de.genodeftest.K8055 Device
	 * 
	 * @param address eine SKAddresse wie auf dem Board eingestellt bzw.aufgedruckt
	 * @return true wenn der Wert erfolgreich gesetzt wurde <br>
	 *         false bei Fehlern im nativen Code
	 */
    @Deprecated
    public boolean connectWith(K8055AddressState address) {
        if (address.getAdressNumber() == -1) return false;
        try {
            long l = setCurrentDevice0(address.getAdressNumber());
            if (l == -1 || l != boardAddress.getAdressNumber()) {
                boardAddress = K8055AddressState.NOT_CONNECTED;
                return false;
            }
            boardAddress = address;
            return true;
        } catch (Exception e) {
            System.err.println("Fehler beim setzen des aktuellen Devices " + e.getStackTrace());
            return false;
        }
    }

    protected abstract long setCurrentDevice0(long address);

    /**
	 * Gibt die Adresse des aktuell angeschlossenen Devices aus
	 * 
	 * @return Adresse des Devices, <br>
	 *         -1 wenn kein Device angeschlossen oder bei Fehlern
	 */
    public K8055AddressState getCurrentDevice() {
        return boardAddress;
    }

    /**
	 * !Diese Methode muss nicht zuverl�ssig arbeiten!
	 * 
	 * @return Wahrheitswert, ob ein Board verbunden ist
	 */
    public boolean isConnected() {
        if (boardAddress.getAdressNumber() == -1) {
            return false;
        }
        return true;
    }
}
