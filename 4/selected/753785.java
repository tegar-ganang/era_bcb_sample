package sonido.mezcladorsoftware;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import sonido.SoundUtils;
import log.Log;

/**
 * 
 * Este sistema de sonido mezclador por software funciona, pero al sumar dos o mas ondas se oye mal.
 * 
 * 
 * Ciclo de vida:
 *   iniciar()
 *   nuevoSonido()*
 *   	Todos los samples que se carguen de fichero, en streaming o que se generen sinteticamente deben tener la misma frecuencia (samples/segundo)
 *   	El primer sonido que se crea en el sistema de sonido establece esa frecuencia, y no puede ser de streaming.
 *   reproducirSonido()*, pararSonido()*, etc...
 *   detener()
 *   y vuelta a empezar...
 *   (los asteriscos significan 'varias veces') 
 * @author yombo
 *
 */
public class SistemaSonido {

    public static final int TIPO_SAMPLE_SINTETIZADO = 0;

    public static final int TIPO_SAMPLE_EN_MEMORIA = 1;

    public static final int TIPO_SAMPLE_STREAMING = 2;

    public static final int MAX_TIPOS_SAMPLE = TIPO_SAMPLE_STREAMING;

    public static final int SONIDO_REP_VACIO = 0;

    public static final int SONIDO_REP_PLAY = 1;

    public static final int TIPO_PETICION_NUEVO_SONIDO = 0;

    public static final int TIPO_PETICION_PLAY_SONIDO = 1;

    public static final int TIPO_PETICION_PARAR_SONIDO = 2;

    public static final int TIPO_PETICION_CAMBIO_PARAMS_SONIDO = 3;

    private Vector<Sonido> sonidos;

    private Vector<SonidoReproduciendo> sonidosReproduciendo;

    private Vector<PeticionSonido> peticionesSonido;

    private boolean sistemaIniciado;

    private SistemaSonidoThread hilo;

    private Object sinc;

    private int samplesPorSegundo;

    public static final int TAM_BUFER_LECTURA = 4096;

    private byte buferLectura[];

    private byte buferCarga[];

    public static final int TAM_BUFER_MEZCLA = 4096 / 2;

    private int buferMezclaSample[];

    private int buferMezcla[];

    private byte buferSalida[];

    private Mixer mixer;

    public SourceDataLine dataLine;

    private SonidoReproduciendo sonidoRepCarga;

    /**
	 * 
	 * @param maxSonidosSimultaneos Incluye todo tipo de sonidos (musica, samples en memoria y sonidos sinteticos)
	 */
    public SistemaSonido(int maxSonidosSimultaneos) {
        sinc = new Object();
        sonidos = new Vector<Sonido>();
        sonidosReproduciendo = new Vector<SonidoReproduciendo>();
        for (int i = 0; i < maxSonidosSimultaneos; i++) {
            SonidoReproduciendo sonidoRep = new SonidoReproduciendo();
            sonidoRep.estado = SONIDO_REP_VACIO;
            sonidosReproduciendo.add(sonidoRep);
            peticionesSonido = new Vector<PeticionSonido>();
        }
        peticionesSonido = new Vector<PeticionSonido>();
        peticionesSonido.ensureCapacity(100);
        buferLectura = new byte[TAM_BUFER_LECTURA];
        buferCarga = new byte[TAM_BUFER_LECTURA];
        samplesPorSegundo = -1;
        buferMezclaSample = new int[TAM_BUFER_MEZCLA];
        buferMezcla = new int[TAM_BUFER_MEZCLA];
        buferSalida = new byte[TAM_BUFER_LECTURA];
        mixer = SoundUtils.getSoftwareOutputMixer();
        if (mixer == null) {
            log.Log.log("Error: mixer not found. Sound system is disabled.");
        }
        sonidoRepCarga = new SonidoReproduciendo();
    }

    public boolean iniciarSistema() {
        synchronized (this) {
            if (isSistemaIniciado()) {
                return false;
            }
            sistemaIniciado = true;
            hilo = new SistemaSonidoThread(this);
            hilo.start();
            return true;
        }
    }

    public boolean isSistemaIniciado() {
        return sistemaIniciado;
    }

    public void detenerSistema() {
        synchronized (this) {
            if (!isSistemaIniciado()) {
                return;
            }
            sistemaIniciado = false;
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        hilo = null;
    }

    public void nuevoSonido(PeticionSonido peticion, String path, int tipo, boolean estereo, int datosSample[], int samplesPorSegundo) {
        peticion.tipoPeticion = TIPO_PETICION_NUEVO_SONIDO;
        peticion.path = path;
        peticion.tipo = tipo;
        peticion.datosSample = datosSample;
        peticion.samplesPorSegundo = samplesPorSegundo;
        peticion.estereo = estereo;
        peticion.empezada = true;
        peticion.acabada = false;
        peticion.exito = false;
        synchronized (sinc) {
            peticionesSonido.add(peticion);
        }
    }

    public void reproducirSonido(PeticionSonido peticion, int indiceSonido, boolean loop, float volumen, float pan) {
        peticion.tipoPeticion = TIPO_PETICION_PLAY_SONIDO;
        peticion.indiceSonido = indiceSonido;
        peticion.loop = loop;
        peticion.volumen = volumen;
        peticion.pan = pan;
        peticion.empezada = true;
        peticion.acabada = false;
        peticion.exito = false;
        synchronized (sinc) {
            peticionesSonido.add(peticion);
        }
    }

    public void pararSonido(PeticionSonido peticion, int indiceSonidoReproduciendo) {
        peticion.tipoPeticion = TIPO_PETICION_PARAR_SONIDO;
        peticion.indiceSonidoReproduciendo = indiceSonidoReproduciendo;
        peticion.empezada = true;
        peticion.acabada = false;
        peticion.exito = false;
        synchronized (sinc) {
            peticionesSonido.add(peticion);
        }
    }

    private int nuevoSonidoInterno(String path, int tipo, boolean estereo, int datosSample[], int samplesPorSegundo) {
        if (path == null || path.equals("")) {
            return -1;
        }
        if (tipo < 0 || tipo > MAX_TIPOS_SAMPLE) {
            return -1;
        }
        if (tipo != TIPO_SAMPLE_SINTETIZADO) {
            File fichero = new File(path);
            if (!fichero.exists() || !fichero.isFile()) {
                return -1;
            }
        }
        int indiceSonido = buscarSonido(path);
        if (indiceSonido != -1) {
            return indiceSonido;
        }
        if (sonidos.size() == 0) {
            if ((tipo == TIPO_SAMPLE_SINTETIZADO || (tipo == TIPO_SAMPLE_EN_MEMORIA && datosSample != null)) && samplesPorSegundo != 44100 && samplesPorSegundo != 22050 && samplesPorSegundo != 11025) {
                return -1;
            }
        }
        Sonido sonido = new Sonido();
        sonido.tipo = tipo;
        sonido.path = path;
        AudioFormat audioFormat = null;
        switch(tipo) {
            case TIPO_SAMPLE_SINTETIZADO:
                sonido.estereo = estereo;
                if (sonidos.size() == 0) {
                    audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplesPorSegundo, 16, 2, 2 * 2, samplesPorSegundo, false);
                }
                break;
            case TIPO_SAMPLE_EN_MEMORIA:
                if (datosSample != null) {
                    sonido.buferSample = datosSample;
                    sonido.estereo = estereo;
                    sonido.numSamples = sonido.buferSample.length / (sonido.estereo ? 2 : 1);
                    if (sonidos.size() == 0) {
                        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplesPorSegundo, 16, 2, 2 * 2, samplesPorSegundo, false);
                    }
                } else {
                    sonidoRepCarga.sonido = sonido;
                    audioFormat = abrirAudioStream(sonidoRepCarga);
                    if (audioFormat == null) {
                        return -1;
                    }
                    if (sonidos.size() > 0 && audioFormat.getSampleRate() != this.samplesPorSegundo) {
                        Log.log("Cannot load sound file, sample rate is not the same of the system or number of channels is not 2. File name: [" + sonido.path + "]");
                        return -1;
                    }
                    sonido.estereo = audioFormat.getChannels() == 2;
                    sonido.numSamples = (int) sonidoRepCarga.audioInputStreamDecodificada.getFrameLength();
                    if (sonido.numSamples == -1) {
                        return -1;
                    }
                    if (sonido.estereo) {
                        sonido.buferSample = new int[sonido.numSamples * 2];
                    } else {
                        sonido.buferSample = new int[sonido.numSamples];
                    }
                    try {
                        try {
                            int nBytesLeidos = 0;
                            int numSampleCh = 0;
                            while (nBytesLeidos != -1) {
                                nBytesLeidos = sonidoRepCarga.audioInputStreamDecodificada.read(buferCarga, 0, buferCarga.length);
                                if (nBytesLeidos > 0) {
                                    int posCarga = 0;
                                    for (posCarga = 0; posCarga < nBytesLeidos; posCarga += 2) {
                                        int byteBajo = buferCarga[posCarga];
                                        int byteAlto = buferCarga[posCarga + 1];
                                        int valor = (byteBajo & 0xFF) | ((byteAlto << 8) & 0xFF00);
                                        sonido.buferSample[numSampleCh++] = valor;
                                    }
                                    if (posCarga != nBytesLeidos) {
                                        int kk = 34;
                                        int gg = kk;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            return -1;
                        }
                    } finally {
                        cerrarAudioStream(sonidoRepCarga);
                        sonidoRepCarga.sonido = null;
                    }
                }
                break;
            case TIPO_SAMPLE_STREAMING:
                break;
            default:
                return -1;
        }
        if (sonidos.size() == 0) {
            AudioFormat audioFormat2 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 2 * 2, 44100, false);
            if (audioFormat == null || !abrirDispositivoSonido(audioFormat2)) {
                return -1;
            }
        }
        sonidos.add(sonido);
        indiceSonido = sonidos.size() - 1;
        sonido.indiceSonido = indiceSonido;
        return indiceSonido;
    }

    /**
	 * 
	 * @param indiceSonido
	 * @param loop
	 * @param volumen
	 * @param pan
	 * @return Devuelve el indice del array sonidosReproduciendo
	 */
    private int reproducirSonidoInterno(int indiceSonido, boolean loop, float volumen, float pan) {
        if (!checkIndiceSonido(indiceSonido)) {
            return -1;
        }
        Sonido sonido = sonidos.get(indiceSonido);
        int indiceSonidoRep = buscarSonidoRepSlotLibre();
        if (indiceSonidoRep == -1) {
            return -1;
        }
        SonidoReproduciendo sonidoRep = sonidosReproduciendo.get(indiceSonidoRep);
        sonidoRep.loop = loop;
        sonidoRep.pan = pan;
        sonidoRep.posicionActual = 0;
        sonidoRep.sonido = sonido;
        sonidoRep.volumen = volumen;
        sonidoRep.estado = SONIDO_REP_PLAY;
        return indiceSonidoRep;
    }

    private boolean pararSonidoInterno(int indiceSonidoReproduciendo) {
        if (!checkIndiceSonidoReproduciendo(indiceSonidoReproduciendo)) {
            return false;
        }
        SonidoReproduciendo sonidoRep = sonidosReproduciendo.get(indiceSonidoReproduciendo);
        if (sonidoRep.estado != SONIDO_REP_PLAY) {
            return false;
        }
        sonidoRep.estado = SONIDO_REP_VACIO;
        sonidoRep.sonido = null;
        return true;
    }

    private int buscarSonido(String path) {
        int ns = sonidos.size();
        for (int i = 0; i < ns; i++) {
            if (sonidos.get(i).path.equals(path)) {
                return i;
            }
        }
        return -1;
    }

    private boolean checkIndiceSonido(int indiceSonido) {
        return indiceSonido >= 0 && indiceSonido <= sonidos.size();
    }

    private boolean checkIndiceSonidoReproduciendo(int indiceSonidoReproduciendo) {
        return indiceSonidoReproduciendo >= 0 && indiceSonidoReproduciendo <= sonidosReproduciendo.size();
    }

    private int buscarSonidoRepSlotLibre() {
        int ns = sonidosReproduciendo.size();
        for (int i = 0; i < ns; i++) {
            SonidoReproduciendo sonidoRep = sonidosReproduciendo.get(i);
            if (sonidoRep.estado == SONIDO_REP_VACIO) {
                return i;
            }
        }
        return -1;
    }

    public AudioFormat abrirAudioStream(SonidoReproduciendo sonidoRep) {
        if (mixer == null) {
            return null;
        }
        File fichero = new File(sonidoRep.sonido.path);
        try {
            sonidoRep.audioInputStreamCodificada = AudioSystem.getAudioInputStream(fichero);
        } catch (UnsupportedAudioFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        if (sonidoRep.audioInputStreamCodificada == null) {
            return null;
        }
        AudioFormat formatoOriginal = sonidoRep.audioInputStreamCodificada.getFormat();
        AudioFormat formatoDecodificado = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, formatoOriginal.getSampleRate(), 16, formatoOriginal.getChannels(), formatoOriginal.getChannels() * 2, formatoOriginal.getSampleRate(), false);
        sonidoRep.audioInputStreamDecodificada = AudioSystem.getAudioInputStream(formatoDecodificado, sonidoRep.audioInputStreamCodificada);
        return formatoDecodificado;
    }

    private void cerrarAudioStream(SonidoReproduciendo sonidoRep) {
        try {
            sonidoRep.audioInputStreamDecodificada.close();
            sonidoRep.audioInputStreamCodificada.close();
        } catch (IOException e) {
        }
    }

    private boolean abrirDispositivoSonido(AudioFormat formato) {
        if (dataLine != null) {
            return true;
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, formato);
        try {
            dataLine = (SourceDataLine) mixer.getLine(info);
            if (dataLine == null) {
                return false;
            }
            dataLine.open(formato);
        } catch (LineUnavailableException e) {
            dataLine = null;
            return false;
        }
        samplesPorSegundo = (int) formato.getSampleRate();
        dataLine.start();
        return true;
    }

    private void cerrarDispositivoSonido() {
        if (dataLine == null) {
            return;
        }
        dataLine.drain();
        dataLine.stop();
        dataLine.close();
        int ns = sonidosReproduciendo.size();
        for (int i = 0; i < ns; i++) {
            SonidoReproduciendo sonidoRep = sonidosReproduciendo.get(i);
            if (sonidoRep.estado == SONIDO_REP_PLAY && sonidoRep.sonido.tipo == TIPO_SAMPLE_STREAMING) {
                cerrarAudioStream(sonidoRep);
            }
        }
    }

    public void ciclo() {
        while (isSistemaIniciado()) {
            procesaPeticiones();
            procesaChunkAudio();
        }
        cerrarDispositivoSonido();
    }

    private void procesaPeticiones() {
        synchronized (sinc) {
            int indicePeticion = 0;
            while (indicePeticion < peticionesSonido.size()) {
                PeticionSonido peticion = peticionesSonido.get(indicePeticion);
                if (!peticion.empezada || peticion.acabada) {
                    peticionesSonido.remove(indicePeticion);
                    continue;
                }
                switch(peticion.tipoPeticion) {
                    case TIPO_PETICION_NUEVO_SONIDO:
                        peticion.indiceSonido = nuevoSonidoInterno(peticion.path, peticion.tipo, peticion.estereo, peticion.datosSample, peticion.samplesPorSegundo);
                        peticion.exito = peticion.indiceSonido != -1;
                        break;
                    case TIPO_PETICION_PLAY_SONIDO:
                        peticion.indiceSonidoReproduciendo = reproducirSonidoInterno(peticion.indiceSonido, peticion.loop, peticion.volumen, peticion.pan);
                        peticion.exito = peticion.indiceSonidoReproduciendo != -1;
                        break;
                    case TIPO_PETICION_PARAR_SONIDO:
                        peticion.exito = pararSonidoInterno(peticion.indiceSonidoReproduciendo);
                        break;
                    case TIPO_PETICION_CAMBIO_PARAMS_SONIDO:
                        break;
                    default:
                        peticion.exito = false;
                }
                peticionesSonido.remove(indicePeticion);
                peticion.acabada = true;
            }
        }
    }

    private void procesaChunkAudio() {
        if (dataLine == null) {
            return;
        }
        Arrays.fill(buferMezcla, 0);
        int ns = sonidosReproduciendo.size();
        for (int indSonidoRep = 0; indSonidoRep < ns; indSonidoRep++) {
            SonidoReproduciendo sonidoRep = sonidosReproduciendo.get(indSonidoRep);
            if (sonidoRep.estado == SONIDO_REP_VACIO) {
                continue;
            }
            Sonido sonido = sonidoRep.sonido;
            switch(sonido.tipo) {
                case TIPO_SAMPLE_SINTETIZADO:
                    for (int indSampleCh = 0; indSampleCh < TAM_BUFER_MEZCLA; ) {
                        float fase = sonidoRep.posicionActual * 0.05f;
                        int valorIzq = (int) (32767 * 0.6f * Math.sin(fase));
                        int valorDer = (int) (32767 * 0.6f * Math.sin(fase));
                        buferMezclaSample[indSampleCh++] = valorIzq;
                        buferMezclaSample[indSampleCh++] = valorDer;
                        sonidoRep.posicionActual++;
                    }
                    break;
                case TIPO_SAMPLE_EN_MEMORIA:
                    int numSamplesAProcesar = Math.min(TAM_BUFER_MEZCLA / 2, sonido.numSamples - sonidoRep.posicionActual);
                    int numSamplesChAProcesar = numSamplesAProcesar * 2;
                    int posBuferSampleCh = sonidoRep.posicionActual * (sonido.estereo ? 2 : 1);
                    if (sonido.estereo) {
                        for (int indSampleCh = 0; indSampleCh < numSamplesChAProcesar; ) {
                            int valor = sonido.buferSample[posBuferSampleCh++];
                            buferMezclaSample[indSampleCh++] = valor;
                        }
                    } else {
                        for (int indSampleCh = 0; indSampleCh < numSamplesChAProcesar; ) {
                            int valor = sonido.buferSample[posBuferSampleCh++];
                            buferMezclaSample[indSampleCh++] = valor;
                            buferMezclaSample[indSampleCh++] = valor;
                        }
                    }
                    sonidoRep.posicionActual += numSamplesAProcesar;
                    if (sonidoRep.posicionActual >= sonido.numSamples) {
                        sonidoRep.estado = SONIDO_REP_VACIO;
                    }
                    break;
                default:
                    Arrays.fill(buferMezclaSample, 0);
            }
            for (int indSample = 0; indSample < TAM_BUFER_MEZCLA; indSample++) {
                int val1 = buferMezcla[indSample];
                int val2 = buferMezclaSample[indSample];
                float val1f = val1 / 32768.0f;
                float val2f = val2 / 32768.0f;
                float val = val1f + val2f;
                buferMezcla[indSample] = (int) (val * 32767);
            }
        }
        int indSampleCh = 0;
        for (int indByte = 0; indByte < TAM_BUFER_LECTURA; ) {
            int valor = buferMezcla[indSampleCh++];
            byte valorHigh = (byte) ((valor & 0xFF00) >> 8);
            byte valorLow = (byte) (valor & 0xFF);
            buferSalida[indByte++] = valorLow;
            buferSalida[indByte++] = valorHigh;
        }
        dataLine.write(buferSalida, 0, TAM_BUFER_LECTURA);
    }

    public static void main(String[] args) {
        SistemaSonido sistema = new SistemaSonido(2);
        sistema.iniciarSistema();
        espera(1000);
        PeticionSonido peticion = new PeticionSonido();
        sistema.nuevoSonido(peticion, "/media/datos1/Devel/java/Antares/data/sounds/man.wav", SistemaSonido.TIPO_SAMPLE_EN_MEMORIA, false, null, 44100);
        while (!peticion.acabada) {
            espera(100);
        }
        if (!peticion.exito) {
            System.out.println("No se ha cargado el sonido");
            sistema.detenerSistema();
            return;
        }
        espera(1000);
        int indiceSonido = peticion.indiceSonido;
        sistema.nuevoSonido(peticion, "/media/datos1/Devel/java/Antares/data/sounds/pitido3.wav", SistemaSonido.TIPO_SAMPLE_EN_MEMORIA, false, null, 44100);
        while (!peticion.acabada) {
            espera(100);
        }
        if (!peticion.exito) {
            System.out.println("No se ha cargado el sonido");
            sistema.detenerSistema();
            return;
        }
        espera(1000);
        int indiceSonido2 = peticion.indiceSonido;
        sistema.reproducirSonido(peticion, indiceSonido, true, 1.0f, 0.0f);
        int esp = 0;
        while (!peticion.acabada) {
            espera(100);
            esp += 100;
        }
        System.out.println("esperado a reproducir: " + esp);
        if (!peticion.exito) {
            System.out.println("No se ha reproducido el sonido");
            sistema.detenerSistema();
            return;
        }
        int indiceSonidoReproduciendo = peticion.indiceSonidoReproduciendo;
        espera(3000);
        sistema.reproducirSonido(peticion, indiceSonido2, true, 1.0f, 0.0f);
        esp = 0;
        while (!peticion.acabada) {
            espera(100);
            esp += 100;
        }
        System.out.println("esperado a reproducir: " + esp);
        espera(7000);
        System.out.println("Fin del programa");
        sistema.detenerSistema();
        System.out.println("Saliendo");
    }

    public static void espera(int ms) {
        try {
            System.out.println("Espera " + ms + " ms...");
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
}
