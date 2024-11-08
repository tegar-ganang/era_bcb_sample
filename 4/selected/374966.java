package audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class AudioWave extends AudioBase {

    private static final long serialVersionUID = 1;

    private static AudioWave instancia = null;

    private String nomeArquivo;

    private SourceDataLine line;

    private byte[] buffer;

    private long arqOffset;

    private RandomAccessFile arqInputStream;

    private boolean inicializar;

    private AudioWave() {
        super();
        whoIAm = "WAVE";
        arqInputStream = null;
        nomeArquivo = "";
        arqOffset = 0;
        line = null;
        buffer = new byte[1024];
        inicializar = false;
    }

    public static AudioWave instancia() {
        if (instancia == null) {
            instancia = new AudioWave();
        }
        return instancia;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void tocar(String nomeArq) {
        inicializar = !nomeArquivo.equals(nomeArq) || (arqInputStream == null) || (line == null);
        nomeArquivo = nomeArq;
        estado = INICIAR;
        System.out.println("Tocar Wave " + nomeArq);
    }

    @Override
    public void abortar() {
        synchronized (this) {
            if (line != null) {
                line.stop();
                line.flush();
            }
            estado = PARADO;
        }
        System.out.println("Pausar Wave");
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
        File arq;
        AudioFormat formato;
        arq = new File(nomeArquivo);
        arqOffset = 0;
        if (arqInputStream == null) {
            try {
                arqInputStream = new RandomAccessFile(arq, "r");
                arqInputStream.seek(44);
            } catch (IOException e) {
                estado = PARADO;
                throw new AudioException(e.getMessage());
            }
        }
        try {
            formato = AudioSystem.getAudioInputStream(arq).getFormat();
            if (line == null) line = getLine(formato);
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

    protected void estadoIniciar() throws AudioException {
        System.out.println("Iniciar Audio Wave " + nomeArquivo);
        if (estado != INICIAR) return;
        if (inicializar) {
            System.out.println("Inicializar...");
            if (arqInputStream != null) {
                abortar();
                fechaArquivo();
            }
            abreArquivo();
        } else {
            if (line == null) {
                estado = PARADO;
                throw new AudioException("Line é nula!");
            }
            line.start();
        }
        estado = TOCAR;
    }

    protected void estadoTocar() throws AudioException {
        int lidos = 0;
        if (estado != TOCAR) return;
        if ((lidos = proximoBuffer(buffer)) == -1) {
            if (line != null) {
                line.drain();
                line.stop();
            }
            estado = TESTAR;
            return;
        }
        line.write(buffer, 0, lidos);
    }

    @Override
    protected void estadoTestar() throws AudioException {
    }
}
