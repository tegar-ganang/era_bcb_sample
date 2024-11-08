package tk.pak0.audiomidifier.model.audio;

import javax.sound.sampled.*;
import javax.sound.sampled.Mixer.Info;

/**
 * @author Francisco Fuentes Barrera
 */
public class AudioTools {

    float frec = 16000.0F;

    byte depth = 8;

    byte channels = 1;

    AudioFormat format = new AudioFormat(frec, depth, channels, true, false);

    private Info[] mixersInfo;

    private Mixer[] mixers;

    private Line.Info[][] lineasEntradasInfo;

    private Line[][] lineasEntradas;

    public Mixer[] getMixers() {
        return mixers;
    }

    public void setMixers(Mixer[] mixers) {
        this.mixers = mixers;
    }

    public Info[] getMixersInfo() {
        return mixersInfo;
    }

    public void setMixersInfo(Info[] mixersInfo) {
        this.mixersInfo = mixersInfo;
    }

    public void setLineasEntradas(Line[][] lineasEntradas) {
        this.lineasEntradas = lineasEntradas;
    }

    public Line[][] getLineasEntradas() {
        return lineasEntradas;
    }

    public void setLineasEntradasInfo(Line.Info[][] lineasEntradasInfo) {
        this.lineasEntradasInfo = lineasEntradasInfo;
    }

    public Line.Info[][] getLineasEntradasInfo() {
        return lineasEntradasInfo;
    }

    void setFrec(float f) {
        frec = f;
    }

    float getFrec() {
        return frec;
    }

    void setDepth(byte b) {
        depth = b;
    }

    byte getDepth() {
        return depth;
    }

    void setChannels(byte c) {
        channels = c;
    }

    byte getChannels() {
        return channels;
    }

    void redoFormat() {
        format = new AudioFormat(frec, depth, channels, true, false);
    }

    public AudioTools() throws LineUnavailableException {
        mixersInfo = initInfoMixers();
        mixers = initMixers();
        lineasEntradasInfo = initLineasEntradaInfo();
        lineasEntradas = initLineasEntrada();
    }

    public Mixer.Info[] initInfoMixers() {
        Mixer.Info[] mesas = AudioSystem.getMixerInfo();
        return mesas;
    }

    public Mixer[] initMixers() {
        Mixer[] mixers = new Mixer[mixersInfo.length];
        for (int i = 0; i < mixers.length; i++) {
            mixers[i] = getMixerFromInfo(mixersInfo[i]);
        }
        return mixers;
    }

    public Mixer getMixerFromInfo(Mixer.Info minfo) {
        Mixer mesaL = null;
        try {
            mesaL = AudioSystem.getMixer(minfo);
        } catch (Exception e) {
            System.err.println("Error al obtener la mesa: " + e);
        }
        return mesaL;
    }

    Line.Info[] getInfoLineas(Mixer mesa) {
        Line.Info[] info = mesa.getTargetLineInfo();
        return info;
    }

    Line.Info[][] initLineasEntradaInfo() {
        Line.Info[][] lineasEntradasInfo = new Line.Info[mixers.length][];
        for (int i = 0; i < mixers.length; i++) {
            lineasEntradasInfo[i] = mixers[i].getSourceLineInfo();
        }
        return lineasEntradasInfo;
    }

    Line[][] initLineasEntrada() throws LineUnavailableException {
        Line[][] lineasEntradas = new Line[mixers.length][];
        for (int i = 0; i < mixers.length; i++) {
            lineasEntradas[i] = new Line[lineasEntradasInfo[i].length];
            for (int j = 0; j < lineasEntradasInfo[i].length; j++) {
                lineasEntradas[i][j] = mixers[i].getLine(lineasEntradasInfo[i][j]);
            }
        }
        return lineasEntradas;
    }

    double[] crearHannWindow(int size) {
        double[] hann = new double[size];
        for (int i = 1; i <= size; i++) {
            hann[i - 1] = 0.5 * (1 - Math.cos((2 * Math.PI * i) / (size - 1)));
        }
        return hann;
    }
}
