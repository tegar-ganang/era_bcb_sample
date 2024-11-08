package audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioNormalizado extends AudioBase {

    private static final long serialVersionUID = 1;

    private static AudioNormalizado instancia = null;

    private long tamArqBytes;

    private long tamArqBlocos;

    private long samplesPorCanal;

    private float sampleRate;

    private int sampleSizeBytes;

    private int channels;

    private int frameSize;

    private float frameRate;

    private boolean bigEndian;

    private String nomeArquivo;

    private String nomeArquivoTemp;

    private int tamBloco;

    private byte[] buffer;

    private static final int kByte = 1024;

    private SourceDataLine line;

    private long arqOffset;

    private long offsetFinal;

    private RandomAccessFile arqInputStream;

    AudioFormat formatoNovo;

    private boolean inicializar;

    private float tempoInicio;

    private float tempoFim;

    private AudioNormalizado() {
        super();
        whoIAm = "NORMALIZADO";
        tamArqBytes = 0;
        tamArqBlocos = 0;
        samplesPorCanal = 0;
        arqInputStream = null;
        tamBloco = kByte;
        nomeArquivo = "";
        nomeArquivoTemp = "";
        arqOffset = 0;
        offsetFinal = 0;
        line = null;
        buffer = new byte[1024];
        inicializar = false;
        tempoInicio = 0;
        tempoFim = 0;
    }

    public static AudioNormalizado instancia() {
        if (instancia == null) {
            instancia = new AudioNormalizado();
        }
        return instancia;
    }

    public long getTamArqBytes() {
        return tamArqBytes;
    }

    public long getTamArqBlocos() {
        return tamArqBlocos;
    }

    public long getSamplesPorCanal() {
        return samplesPorCanal;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(float sr) {
        sampleRate = sr;
    }

    public int getSampleSizeBytes() {
        return sampleSizeBytes;
    }

    public void setSampleSizeBytes(int ssb) {
        sampleSizeBytes = ssb;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int c) {
        channels = c;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(int fs) {
        frameSize = fs;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float fr) {
        frameRate = fr;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public void setBigEndian(boolean b) {
        bigEndian = b;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public AudioFormat getFormatoNovo() {
        return formatoNovo;
    }

    public void tocar(String nomeArq, float inicio, float fim) {
        inicializar = !nomeArquivo.equals(nomeArq) || (arqInputStream == null) || (line == null);
        tempoInicio = inicio;
        tempoFim = fim;
        nomeArquivo = nomeArq;
        estado = INICIAR;
        System.out.println("TOCAR Normalizado - Estado Iniciar... ");
    }

    @Override
    public void abortar() {
        synchronized (this) {
            if (line != null) line.flush();
            estado = PARADO;
        }
        System.out.println("Aborta fala");
    }

    public void fechar() throws AudioException {
        if (arqInputStream != null) {
            abortar();
            fechaArquivo();
        }
        estado = PARADO;
        System.out.println("Fechar");
    }

    public void encerrar() throws AudioException {
        fechar();
        estado = ENCERRAR;
    }

    public float getDuracao() {
        return (samplesPorCanal / frameRate) * 1000;
    }

    public long getSample(float tempo) {
        return (long) ((frameRate * tempo) / 1000);
    }

    public float getTempo(long sample) {
        return (1000 * (sample + 1)) / frameRate;
    }

    public float getTempoSeg(long sample) {
        return (sample + 1) / frameRate;
    }

    private void criar() throws AudioException {
        byte[] buffer;
        int numBytesLidos;
        File arqSomOriginal;
        FileOutputStream audioSaida;
        AudioFormat formatoOriginal;
        AudioInputStream audioOriginal;
        AudioInputStream audioConvertido;
        arqSomOriginal = new File(nomeArquivo);
        nomeArquivoTemp = "/tmp/audio" + arqSomOriginal.getName();
        try {
            audioOriginal = AudioSystem.getAudioInputStream(arqSomOriginal);
        } catch (Exception e) {
            estado = PARADO;
            throw new AudioException(e.getMessage());
        }
        formatoOriginal = audioOriginal.getFormat();
        bigEndian = false;
        sampleSizeBytes = 2;
        channels = formatoOriginal.getChannels();
        frameSize = formatoOriginal.getChannels() * sampleSizeBytes;
        frameRate = formatoOriginal.getSampleRate();
        sampleRate = formatoOriginal.getSampleRate();
        tamBloco = kByte * channels;
        buffer = new byte[tamBloco];
        formatoNovo = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeBytes * 8, channels, frameSize, frameRate, bigEndian);
        try {
            estado = NORMALIZAR;
            audioConvertido = AudioSystem.getAudioInputStream(formatoNovo, audioOriginal);
            audioSaida = new FileOutputStream(new File(nomeArquivoTemp));
            tamArqBytes = 0;
            tamArqBlocos = 0;
            while ((numBytesLidos = audioConvertido.read(buffer, 0, buffer.length)) != -1) {
                if (estado != NORMALIZAR) {
                    audioConvertido.close();
                    audioOriginal.close();
                    audioSaida.close();
                    return;
                }
                tamArqBlocos++;
                tamArqBytes += numBytesLidos;
                audioSaida.write(buffer, 0, numBytesLidos);
            }
            samplesPorCanal = tamArqBytes / frameSize;
            audioConvertido.close();
            audioOriginal.close();
            audioSaida.close();
            estado = INICIAR;
            System.out.println("Fim da Normalizacao - Estado Iniciar... ");
        } catch (IOException e) {
            estado = PARADO;
            throw new AudioException(e.getMessage());
        }
        System.out.println("Lidos = " + tamArqBlocos + " blocos");
    }

    public void mostraFormato(AudioFormat formato) {
        System.out.println("Encoding: " + formato.getEncoding().toString());
        System.out.println("channels: " + formato.getChannels());
        System.out.println("frameSize: " + formato.getFrameSize());
        System.out.println("frameRate: " + formato.getFrameRate());
        System.out.println("bigEndian: " + formato.isBigEndian());
        System.out.println("sample Rate: " + formato.getSampleRate());
        System.out.println("sampleSizeInBits: " + formato.getSampleSizeInBits());
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.addLineListener(this);
        res.open(audioFormat);
        return res;
    }

    public void abreArquivo() throws AudioException {
        try {
            arqOffset = 0;
            if (arqInputStream == null) arqInputStream = new RandomAccessFile(new File(nomeArquivoTemp), "r");
            line = getLine(getFormatoNovo());
            line.start();
        } catch (Exception e) {
            estado = PARADO;
            throw new AudioException(e.getMessage());
        }
    }

    public int proximoBuffer(byte[] buffer) throws AudioException {
        int nbytes = -1;
        if (arqInputStream == null) return -1;
        try {
            nbytes = arqInputStream.read(buffer, 0, buffer.length);
        } catch (IOException e) {
            estado = PARADO;
            throw new AudioException(e.getMessage());
        }
        if (nbytes > 0) arqOffset++;
        return nbytes;
    }

    public void setPosArq(long p) throws AudioException {
        if (arqInputStream != null) {
            arqOffset = p;
            try {
                arqInputStream.seek(p * tamBloco);
            } catch (IOException e) {
                estado = PARADO;
                throw new AudioException(e.getMessage());
            }
        }
    }

    public void fechaArquivo() throws AudioException {
        try {
            if (arqInputStream != null) arqInputStream.close();
        } catch (IOException e) {
            estado = PARADO;
            throw new AudioException(e.getMessage());
        }
        arqOffset = 0;
        arqInputStream = null;
        line.stop();
        line.close();
        line = null;
    }

    public long getNumByteDoSample(long ns) {
        return ns * frameSize;
    }

    public long getBlocoDoSample(long ns) {
        return getNumByteDoSample(ns) / tamBloco;
    }

    public long getPosBlocoDoSample(long ns) {
        return getNumByteDoSample(ns) % tamBloco;
    }

    public List<Integer> getLisSamples(long ini, long fim, int canal, int step) throws AudioException {
        int p, oc;
        long posAtual;
        long posSample;
        long finalBloco;
        byte[] buffer;
        List<Integer> lisSamples;
        buffer = new byte[tamBloco];
        lisSamples = new ArrayList<Integer>();
        if (ini > fim) {
            long t;
            t = ini;
            ini = fim;
            fim = t;
        }
        oc = sampleSizeBytes * canal;
        posSample = ini;
        posAtual = getPosBlocoDoSample(ini);
        setPosArq(getBlocoDoSample(ini));
        finalBloco = proximoBuffer(buffer);
        if (finalBloco < 0) return lisSamples;
        while (posSample <= fim) {
            if (posAtual >= finalBloco) {
                finalBloco = proximoBuffer(buffer);
                if (finalBloco < 0) return lisSamples;
                posAtual %= tamBloco;
            }
            p = (int) posAtual + oc;
            lisSamples.add(buffer[p] | (buffer[p + 1] << 8));
            posAtual += frameSize * step;
            posSample += step;
        }
        return lisSamples;
    }

    public List<Integer> getLisSamples(float tini, float tfim, int canal, int step) throws AudioException {
        return getLisSamples(getSample(tini), getSample(tfim), canal, step);
    }

    public void gravaPausas(int canal, long totalSamples) throws AudioException {
        final long freqSilencio = 100;
        int p, oc;
        long posAtual;
        long posSample;
        int absFreq;
        long iniPausa, pausaMinima;
        int desprezados;
        byte[] buffer;
        buffer = new byte[tamBloco];
        pausaMinima = getSample(400) + 1;
        oc = sampleSizeBytes * canal;
        iniPausa = -1;
        posSample = 0;
        posAtual = tamBloco;
        if (totalSamples == 0) totalSamples = samplesPorCanal;
        abreArquivo();
        desprezados = 0;
        while (posSample < totalSamples) {
            if (posAtual == tamBloco) {
                proximoBuffer(buffer);
                posAtual = 0;
            }
            p = (int) posAtual + oc;
            absFreq = Math.abs(buffer[p] | (buffer[p + 1] << 8));
            if (absFreq <= freqSilencio) {
                if (iniPausa < 0) iniPausa = posSample;
            } else if (iniPausa > -1) {
                if ((posSample - iniPausa) > pausaMinima) System.out.println("Ini = " + getTempoSeg(iniPausa) + " Fin = " + getTempoSeg(posSample - 1)); else desprezados++;
                iniPausa = -1;
            }
            posAtual += frameSize;
            posSample++;
        }
        if (iniPausa > -1) {
            if ((posSample - iniPausa) > pausaMinima) System.out.println("Ini = " + getTempoSeg(iniPausa) + " Fin = " + getTempoSeg(posSample - 1)); else desprezados++;
        }
        System.out.println("Desprezei = " + desprezados);
        fechaArquivo();
    }

    protected void estadoIniciar() throws AudioException {
        if (estado != INICIAR) return;
        if (inicializar) {
            if (arqInputStream != null) {
                abortar();
                fechaArquivo();
            }
            criar();
            abreArquivo();
        } else {
            if (line == null) throw new AudioException("Line é nula!");
            line.start();
        }
        setPosArq(getSample(tempoInicio));
        offsetFinal = (tempoFim > 0) ? getSample(tempoFim) : getSamplesPorCanal();
        estado = TOCAR;
    }

    protected void estadoTocar() throws AudioException {
        int lidos = 0;
        if (estado != TOCAR) return;
        if (arqOffset > offsetFinal) {
            estado = TESTAR;
            line.stop();
            return;
        }
        if ((lidos = proximoBuffer(buffer)) == -1) {
            line.drain();
            line.stop();
            estado = TESTAR;
            return;
        }
        line.write(buffer, 0, lidos);
    }

    @Override
    protected void estadoTestar() throws AudioException {
    }
}
