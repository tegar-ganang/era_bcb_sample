package sonido;

import java.util.Vector;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class ReproductorMusicaOgg extends Thread {

    private Mixer mixer;

    private Vector<MusicaOgg> musicas;

    private Object signal;

    private float volumen;

    private boolean volumenCambiado;

    private int indiceMusicaReproduciendo;

    private MusicaOgg musicaReproduciendo;

    private boolean reproducirContinuamente;

    private int indiceMusicaEncolada;

    private boolean encoladaContinuamente;

    private boolean despertar;

    private boolean pararMusica;

    private boolean terminar;

    private byte datosSonido[];

    public ReproductorMusicaOgg() {
        setName("AntaresMusicThread");
        mixer = SoundUtils.getSoftwareOutputMixer();
        if (mixer == null) {
            log.Log.log("Error: mixer not found. Music is disabled.");
        }
        musicas = new Vector<MusicaOgg>();
        indiceMusicaReproduciendo = -1;
        musicaReproduciendo = null;
        indiceMusicaEncolada = -1;
        datosSonido = new byte[4096];
        signal = new Object();
        setVolumen(1.0f);
    }

    public void marcarTerminacion() {
        terminar = true;
        pararMusica = true;
        indiceMusicaEncolada = -1;
        synchronized (signal) {
            signal.notify();
        }
    }

    public int nuevaMusica(String path) {
        for (int i = 0; i < musicas.size(); i++) {
            if (musicas.elementAt(i).path.compareTo(path) == 0) {
                return i;
            }
        }
        MusicaOgg musica = new MusicaOgg();
        musica.path = path;
        musicas.add(musica);
        return musicas.size() - 1;
    }

    public int getNumMusicas() {
        return musicas.size();
    }

    public int getIndiceMusicaReproduciendo() {
        return indiceMusicaReproduciendo;
    }

    public void setVolumen(float volumen) {
        if (volumen < 0.0f) {
            volumen = 0.0f;
        }
        if (volumen > 1.0f) {
            volumen = 1.0f;
        }
        this.volumen = volumen;
        volumenCambiado = true;
    }

    public boolean reproducirMusica(int indiceMusica, boolean continuamente) {
        synchronized (signal) {
            boolean exito = false;
            if (indiceMusicaReproduciendo != -1) {
                indiceMusicaEncolada = indiceMusica;
                encoladaContinuamente = continuamente;
                exito = true;
            } else {
                exito = inicioReproduccion(indiceMusica, continuamente);
            }
            if (exito) {
                despertar = true;
                signal.notify();
            }
            return exito;
        }
    }

    public void pararMusica() {
        if (indiceMusicaReproduciendo == -1) {
            return;
        }
        pararMusica = true;
    }

    private boolean inicioReproduccion(int indiceMusica, boolean continuamente) {
        if (mixer == null) {
            return false;
        }
        MusicaOgg musica = musicas.elementAt(indiceMusica);
        musica.fichero = new File(musica.path);
        try {
            musica.audioInputStreamCodificada = AudioSystem.getAudioInputStream(musica.fichero);
        } catch (UnsupportedAudioFileException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        if (musica.audioInputStreamCodificada == null) {
            return false;
        }
        AudioFormat formatoOriginal = musica.audioInputStreamCodificada.getFormat();
        AudioFormat formatoDecodificado = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, formatoOriginal.getSampleRate(), 16, formatoOriginal.getChannels(), formatoOriginal.getChannels() * 2, formatoOriginal.getSampleRate(), false);
        musica.audioInputStreamDecodificada = AudioSystem.getAudioInputStream(formatoDecodificado, musica.audioInputStreamCodificada);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, formatoDecodificado);
        try {
            musica.dataLine = (SourceDataLine) mixer.getLine(info);
            if (musica.dataLine == null) {
                try {
                    musica.audioInputStreamDecodificada.close();
                    musica.audioInputStreamCodificada.close();
                } catch (IOException e) {
                }
                return false;
            }
            musica.dataLine.open(formatoDecodificado);
        } catch (LineUnavailableException e) {
            try {
                musica.audioInputStreamDecodificada.close();
                musica.audioInputStreamCodificada.close();
            } catch (IOException ioe) {
            }
            return false;
        }
        musica.dataLine.start();
        musica.controlGanancia = (FloatControl) musica.dataLine.getControl(FloatControl.Type.MASTER_GAIN);
        indiceMusicaReproduciendo = indiceMusica;
        musicaReproduciendo = musica;
        reproducirContinuamente = continuamente;
        volumenCambiado = true;
        return true;
    }

    private void bucleReproduccion() {
        boolean seguir = true;
        while (seguir && !pararMusica) {
            seguir = false;
            int nBytesLeidos = 0;
            try {
                while (nBytesLeidos != -1 && !pararMusica && indiceMusicaEncolada == -1) {
                    if (volumenCambiado) {
                        if (volumen == 0.0f) {
                            musicaReproduciendo.controlGanancia.setValue(musicaReproduciendo.controlGanancia.getMinimum());
                        } else {
                            musicaReproduciendo.controlGanancia.setValue(-30.0f * (1.0f - volumen * SoundUtils.MAX_VOLUME));
                        }
                        volumenCambiado = false;
                    }
                    nBytesLeidos = musicaReproduciendo.audioInputStreamDecodificada.read(datosSonido, 0, datosSonido.length);
                    if (nBytesLeidos != -1) {
                        musicaReproduciendo.dataLine.write(datosSonido, 0, nBytesLeidos);
                    }
                }
            } catch (IOException e) {
            }
            if (indiceMusicaEncolada != -1 && !pararMusica) {
                finReproduccion();
                inicioReproduccion(indiceMusicaEncolada, encoladaContinuamente);
                indiceMusicaEncolada = -1;
                seguir = true;
                despertar = false;
            } else if (reproducirContinuamente && !pararMusica) {
                finReproduccion();
                inicioReproduccion(indiceMusicaReproduciendo, true);
                seguir = true;
            }
        }
        pararMusica = false;
        finReproduccion();
        indiceMusicaReproduciendo = -1;
        musicaReproduciendo = null;
    }

    private void finReproduccion() {
        musicaReproduciendo.dataLine.drain();
        musicaReproduciendo.dataLine.stop();
        musicaReproduciendo.dataLine.close();
        try {
            musicaReproduciendo.audioInputStreamDecodificada.close();
            musicaReproduciendo.audioInputStreamCodificada.close();
        } catch (IOException e) {
        }
        musicaReproduciendo.audioInputStreamCodificada = null;
        musicaReproduciendo.audioInputStreamDecodificada = null;
        musicaReproduciendo.dataLine = null;
    }

    public void run() {
        while (!terminar) {
            synchronized (signal) {
                if (!despertar && indiceMusicaEncolada == -1) {
                    try {
                        signal.wait();
                    } catch (InterruptedException e) {
                    }
                }
                despertar = false;
                if (indiceMusicaEncolada != -1) {
                    finReproduccion();
                    inicioReproduccion(indiceMusicaEncolada, encoladaContinuamente);
                    indiceMusicaEncolada = -1;
                }
            }
            if (indiceMusicaReproduciendo != -1) {
                bucleReproduccion();
            }
        }
    }
}
