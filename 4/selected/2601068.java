package dsp.auraw;

import javax.sound.sampled.*;
import java.io.*;
import dsp.*;
import dsp.exception.RecoverableInputProblem;

public class AuInput implements Input {

    private int Buffer;

    private SingleDataChunk a;

    private int NumarCanalCitit;

    private int sampleSizeInBits;

    private DataInputStream data;

    private int channels;

    private int frecventa;

    public AuInput(AudioInputStream audioInputStream, int Numar) {
        NumarCanalCitit = Numar;
        sampleSizeInBits = audioInputStream.getFormat().getSampleSizeInBits();
        channels = audioInputStream.getFormat().getChannels();
        frecventa = (int) audioInputStream.getFormat().getSampleRate();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] audioBytes = new byte[audioInputStream.getFormat().getFrameSize()];
        try {
            while ((audioInputStream.read(audioBytes)) != -1) {
                output.write(audioBytes);
            }
        } catch (Exception e) {
            System.out.println("Eroare " + e);
        }
        try {
            audioInputStream.close();
        } catch (IOException e) {
            System.out.println("Eroare la inchiderea fisierului");
        }
        data = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
    }

    public void start() {
    }

    public DataChunk getNext() throws RecoverableInputProblem {
        return new SingleDataChunk(Buffer);
    }

    public void stop() {
    }

    public boolean endOfInput() {
        try {
            switch(NumarCanalCitit) {
                case 1:
                    switch(sampleSizeInBits) {
                        case 8:
                            if (channels == 2) {
                                Buffer = data.readUnsignedByte();
                                data.readUnsignedByte();
                            } else {
                                Buffer = data.readUnsignedByte();
                            }
                            break;
                        case 16:
                            if (channels == 2) {
                                Buffer = data.readUnsignedShort();
                                data.readUnsignedShort();
                            } else {
                                Buffer = data.readUnsignedShort();
                            }
                            break;
                    }
                    break;
                case 2:
                    switch(sampleSizeInBits) {
                        case 8:
                            if (channels == 2) {
                                data.readUnsignedByte();
                                Buffer = data.readUnsignedByte();
                            } else {
                                Buffer = data.readUnsignedByte();
                            }
                            break;
                        case 16:
                            if (channels == 2) {
                                data.readUnsignedShort();
                                Buffer = data.readUnsignedShort();
                            } else {
                                Buffer = data.readUnsignedShort();
                            }
                            break;
                    }
                    break;
                case 12:
                    switch(sampleSizeInBits) {
                        case 8:
                            Buffer = data.readUnsignedByte();
                            break;
                        case 16:
                            Buffer = data.readUnsignedShort();
                            break;
                    }
                    break;
            }
        } catch (EOFException e) {
            try {
                data.close();
            } catch (IOException ee) {
                System.out.println("Eroare la inchiderea fisierului" + ee);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Eroare IOException" + e);
        }
        return false;
    }

    public int getFrequency() {
        return frecventa;
    }
}
