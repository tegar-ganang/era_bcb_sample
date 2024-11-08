package es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import es.usc.citius.servando.android.medim.Drivers.IDriverEvents;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.CRC_CCITT;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.Command;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.CommandConfigAnalogCfm;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.CommandEcgDataTransmission;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.CommandFirmwareVersion;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.CommandIdentification;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.ECGChannels;
import es.usc.citius.servando.android.medim.model.devices.DeviceType;

/**
 *
 * @author tarasco
 */
public class Protocol {

    protected static final byte START_FLAG = (byte) 0xFC;

    protected static final byte END_FLAG = (byte) 0xFD;

    protected static final byte ESCAPE_FLAG = (byte) 0xFE;

    protected static final byte EXOR_VALUE = 0x20;

    protected byte m_NumeroPaquete;

    private InputStream m_StreamLectura;

    private OutputStream m_StreamEscritura;

    protected boolean m_LeerDatos;

    private CommandEcgDataTransmission m_ComandoEcgDataTransmission = new CommandEcgDataTransmission(null);

    protected ArrayList<IDriverEvents> m_Eventos;

    public Protocol(InputStream reader, OutputStream writer, byte packetnumber) {
        m_StreamLectura = reader;
        m_StreamEscritura = writer;
        m_NumeroPaquete = packetnumber;
        m_LeerDatos = true;
    }

    public byte getPacketNumber() {
        return m_NumeroPaquete;
    }

    protected void sendCommand(byte[] command) throws IOException {
        byte[] tmp = generateCommand(command);
        if (m_StreamEscritura != null) {
            m_StreamEscritura.write(tmp);
        }
    }

    private byte[] generateCommand(byte[] datos) {
        byte[] paquete = new byte[datos.length + 5];
        paquete[0] = START_FLAG;
        paquete[1] = m_NumeroPaquete;
        if (m_NumeroPaquete > 0xFF) m_NumeroPaquete = 1;
        System.arraycopy(datos, 0, paquete, 2, datos.length);
        CRC_CCITT.CRCHiLow tmp = CRC_CCITT.CRCITT(paquete, 1, 2 + datos.length);
        paquete[paquete.length - 3] = tmp.bCRC[1];
        paquete[paquete.length - 2] = tmp.bCRC[0];
        paquete[paquete.length - 1] = END_FLAG;
        return paquete;
    }

    private boolean searchPacketStart() {
        byte[] dato = new byte[1];
        do {
            try {
                if ((m_StreamLectura != null)) {
                    m_StreamLectura.read(dato, 0, 1);
                } else return false;
            } catch (Exception ex) {
                for (IDriverEvents event : m_Eventos) {
                    event.onBluetoothDisconnection(DeviceType.ELECTROCARDIOGRAPH);
                }
                return false;
            }
        } while ((dato[0] != START_FLAG) && (m_LeerDatos));
        return true;
    }

    public Command readResponsePacket() {
        ByteArrayOutputStream leido = new ByteArrayOutputStream();
        byte[] dato = new byte[1];
        boolean bEscapado = false;
        Command tmp = null;
        do {
            leido.reset();
            if ((m_LeerDatos) && (searchPacketStart())) {
                leido.write(START_FLAG);
                do {
                    try {
                        if ((m_StreamLectura != null)) {
                            m_StreamLectura.read(dato, 0, 1);
                            if (bEscapado) {
                                leido.write((byte) (dato[0] ^ EXOR_VALUE));
                                bEscapado = false;
                            } else if (dato[0] == ESCAPE_FLAG) bEscapado = true; else leido.write(dato[0]);
                        } else Thread.sleep(10);
                    } catch (Exception ex) {
                        for (IDriverEvents event : m_Eventos) {
                            event.onBluetoothDisconnection(DeviceType.ELECTROCARDIOGRAPH);
                        }
                        return null;
                    }
                } while ((dato[0] != END_FLAG) && (m_LeerDatos));
                tmp = parseResponsePacket(leido.toByteArray());
            } else return null;
        } while ((tmp == null) && (m_LeerDatos));
        if (m_LeerDatos) return tmp;
        return null;
    }

    private void clearBuffer() {
        ByteArrayOutputStream leido = new ByteArrayOutputStream();
        byte[] dato = new byte[1];
        boolean bEscapado = false;
        Command tmp = null;
        do {
            leido.reset();
            if ((m_LeerDatos) && (searchPacketStart())) {
                leido.write(START_FLAG);
                do {
                    try {
                        if ((m_StreamLectura != null)) {
                            m_StreamLectura.read(dato, 0, 1);
                            if (bEscapado) {
                                leido.write((byte) (dato[0] ^ EXOR_VALUE));
                                bEscapado = false;
                            } else if (dato[0] == ESCAPE_FLAG) bEscapado = true; else leido.write(dato[0]);
                        } else Thread.sleep(10);
                    } catch (Exception ex) {
                        for (IDriverEvents event : m_Eventos) {
                            event.onBluetoothDisconnection(DeviceType.ELECTROCARDIOGRAPH);
                        }
                        return;
                    }
                } while ((dato[0] != END_FLAG) && (m_LeerDatos));
                tmp = parseResponsePacket(leido.toByteArray());
            } else return;
        } while ((tmp == null) && (m_LeerDatos));
    }

    public boolean isReadData() {
        return m_LeerDatos;
    }

    public void setReadData(boolean value) {
        m_LeerDatos = value;
    }

    public void inquiryProtocolVersion() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x00, 0x01 });
    }

    public void inquiryIdentification() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x00, 0x05 });
    }

    public void inquiryMaintenanceData() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x00, 0x06 });
    }

    public void inquiryBlueToothClock() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x05, 0x07 });
    }

    public void inquiryECGDeviceConfiguration() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x10, 0x07 });
    }

    public void inquiryMedicalParameterConfiguration() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x16, 0x07 });
    }

    protected Command parseResponsePacket(byte[] paquete) {
        if (paquete.length > 5) {
            int comando = (short) ((paquete[3] << 8) | paquete[2]);
            switch(comando) {
                case 0x0500:
                    return new CommandIdentification(paquete);
                case 0x0150:
                    return new CommandFirmwareVersion(paquete);
                case 0x0701:
                    return new CommandConfigAnalogCfm(paquete);
                case 0x0724:
                    m_ComandoEcgDataTransmission.parsePacket(paquete);
                    return m_ComandoEcgDataTransmission;
            }
        }
        return null;
    }

    public void configAnalogReq(ECGChannels canal, byte sampleo) throws IOException {
        byte[] tmp = new byte[] { 0x01, 0x09, (byte) canal.ordinal(), (byte) sampleo };
        sendCommand(tmp);
    }

    public void switchBtRoleReq() throws IOException {
        sendCommand(new byte[] { 0x07, 0x09 });
    }

    public void ConfigEcgDeviceReq(int display, int volumen, int datasave, short alarmasacusticas, int energia) throws IOException {
        sendCommand(new byte[] { 0x10, 0x09, (byte) display, (byte) volumen, (byte) datasave, (byte) alarmasacusticas, (byte) ((short) alarmasacusticas >> 8), 0x00, (byte) energia });
    }

    public void flashDataReq() throws IOException {
        sendCommand(new byte[] { 0x15, 0x09 });
    }

    public void setMedicalParameterReq(short heartrtethreshold, byte pacemakerparameter) throws IOException {
        sendCommand(new byte[] { 0x16, 0x09, (byte) (heartrtethreshold >> 8), (byte) heartrtethreshold, pacemakerparameter });
    }
}
