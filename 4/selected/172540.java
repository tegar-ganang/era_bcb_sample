package es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Three;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Protocol;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.Command;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.EcgTransmission;

public class Protocol3 extends Protocol {

    private static final byte VERSION = 3;

    private CommandEcgDataTransmissionAdvanced m_ComandoEcgDataTransmissionAdvanced = new CommandEcgDataTransmissionAdvanced(null);

    public Protocol3(InputStream reader, OutputStream writer, byte packetnumber) {
        super(reader, writer, packetnumber);
    }

    public void startStopEcgTransmissionAdvanced(EcgTransmission value) throws IOException {
        sendCommand(new byte[] { 0x27, 0x09, (byte) value.ordinal() });
    }

    public void inquiryFirmwareVersion() throws IOException {
        sendCommand(new byte[] { 0x00, 0x08, 0x50, 0x01 });
    }

    @Override
    protected Command parseResponsePacket(byte[] packet) {
        if (packet.length > 5) {
            int comando = (short) ((packet[3] << 8) | packet[2]);
            switch(comando) {
                case 0x0727:
                    m_ComandoEcgDataTransmissionAdvanced.parsePacket(packet);
                    return m_ComandoEcgDataTransmissionAdvanced;
            }
        }
        return super.parseResponsePacket(packet);
    }
}
