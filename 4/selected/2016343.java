package es.usc.citius.servando.android.medim.Algorithms.ECG;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import android.util.Log;
import es.usc.citius.servando.android.communications.model.IntegerList;
import es.usc.citius.servando.android.communications.model.ShortList;
import es.usc.citius.servando.android.communications.model.ShortMatrix;
import es.usc.citius.servando.android.medim.MedimService;
import es.usc.citius.servando.android.medim.MonitoringMessageMgr;
import es.usc.citius.servando.android.medim.MonitoringSession;
import es.usc.citius.servando.android.medim.Algorithms.Algorithm;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Classification;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Episodes;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Hrv;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Laguna;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Parameters;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Tompkins;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.Util;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.defines.Constants;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.defines.Global;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.structures.Family;
import es.usc.citius.servando.android.medim.Algorithms.ECG.MonitoringThread.structures.HeartBeat;
import es.usc.citius.servando.android.medim.Drivers.IDriver;
import es.usc.citius.servando.android.medim.Drivers.ECG.Corscience.Protocols.Commands.BatteryStatus;
import es.usc.citius.servando.android.medim.Drivers.events.DriverEventProvider;
import es.usc.citius.servando.android.medim.Drivers.events.IDriverEventListener;
import es.usc.citius.servando.android.medim.MIT.MITFileMgr;
import es.usc.citius.servando.android.medim.MIT.Annotations.Annotation;
import es.usc.citius.servando.android.medim.MIT.Annotations.AnnotationCodes;
import es.usc.citius.servando.android.medim.MIT.Annotations.MitAnnotation;
import es.usc.citius.servando.android.medim.Storage.Channel;
import es.usc.citius.servando.android.medim.model.DiagramaTendencias;
import es.usc.citius.servando.android.medim.model.HrvHora;
import es.usc.citius.servando.android.medim.model.MorphologicalFamilies;
import es.usc.citius.servando.android.medim.model.MorphologicalFamily;
import es.usc.citius.servando.android.medim.model.devices.DeviceType;
import es.usc.citius.servando.android.medim.model.devices.Sensor;
import es.usc.citius.servando.android.medim.util.TimeUtils;
import es.usc.citius.servando.android.models.MIT.MITSignalSpecification;
import es.usc.citius.servando.android.services.ServiceConfig;

/**
 * 
 * @author Ángel Piñeiro
 * 
 */
public class ECGMonitoringThread extends Algorithm implements IDriverEventListener {

    /**
	 * Tag para debug
	 */
    public static final String DEBUG_TAG = ECGMonitoringThread.class.getSimpleName();

    /**
	 * Logger
	 */
    private static final Logger log = Logger.getLogger(ECGMonitoringThread.class);

    /**
	 * Sufixo para anexar ós nomes dos ficheiros de diagramas de tendencias
	 */
    private static final String TREND_SUFIX = "trend";

    /**
	 * Sufixo para anexar ós nomes dos ficheiros de ecg
	 */
    private static final String ECG_SUFIX = "ecg";

    /**
	 * Tamaño do buffer de almacenamento de ecg en segundos. Unha vez cheo gárdase a disco. Hai que ter en conta que
	 * nunca debe ser menor ca o buffer do driver do que se está lendo.
	 */
    private static final int ECG_BUFFER_LEN = 30;

    /**
	 * Tamaño do buffer de almacenamento de diagramas de tendencias en segundos. Unha vez cheo gárdase a disco.
	 */
    private static final int DIAGRAM_BUFFER_LEN = 10;

    /**
	 * Intervalo de envío de diagramas de tendencia en minutos
	 */
    private static final int TIEMPO_ENVIO_DIAGRAMAS_TENDENCIA = 15;

    /**
	 * Indica o timestamp da primeira mostra do diagrama de tendencias que se vai enviar
	 */
    private long trendDiagramSendTime;

    /**
	 * Buffer temporal no que se almacenan os datos de diagramas de tendencias
	 */
    private Channel[] trendData;

    /**
	 * Buffer temporal no que se almacenan os datos de ecg
	 */
    private Channel[] ecgData;

    /**
	 * Xestiona o almacenamento en ficheiros dos datos de diagramas de tendencias
	 */
    private MITFileMgr trendMitMgr;

    /**
	 * Xestiona o almacenamento en ficheiros dos datos de ecg
	 */
    private MITFileMgr ecgMitMgr;

    /**
	 * Diagramas de tendencias para enviar. Alternase entre dous para non escribir no que se está enviando.
	 */
    private DiagramaTendencias[] shippingTrendDiagrams = new DiagramaTendencias[2];

    /**
	 * Familias morfolóxicas para enviar.
	 */
    private MorphologicalFamilies shippingFamilies;

    private Global g;

    private Util util;

    /**
	 * Clasificador fran
	 */
    private Perceptron2 clasificador;

    private int counter = 0;

    /**
	 * Indice do diagrama no que se está escribindo. Pode ser 0 ou 1 (m_QueDiagrama)
	 */
    private int indiceDiagramaActual;

    /**
	 * Mostra actual (m_CualDiagrama)
	 */
    private int indiceMuestraActual;

    private static final int UMB_RESET_TIME = 900000;

    private int lead = 0;

    private boolean flagplaninicial = false;

    private boolean latidovalido = false;

    private long tiempointerp = 0;

    private short[] param;

    private Tompkins tompkins;

    private Laguna laguna;

    private Parameters parameters;

    private Classification classification;

    private Episodes episodes;

    private Hrv hrv;

    public ECGMonitoringThread() {
        deviceType = DeviceType.ELECTROCARDIOGRAPH;
        trendMitMgr = new MITFileMgr();
        ecgMitMgr = new MITFileMgr();
    }

    /**
	 * Carga en {@link Global#v} a mostra actual para cada canal
	 * 
	 * @param sampleIndex
	 */
    public void loadCurrentSamples(int sampleIndex, short[][] channels) {
        for (int channelIndex = 0; channelIndex < g.n; channelIndex++) {
            g.v[channelIndex] = (short) channels[channelIndex][sampleIndex];
        }
    }

    /**
	 * Prepara o algoritmo para o procesado en función das especificacións do driver. Inicializa os ficheiros de
	 * cabeceira, datos e anotacións de ecg e diagramas de tendencias, crea os buffers de almacenamento temporal, e
	 * inicializa as variables globáis usadas por medim na clase Global.
	 */
    @Override
    public void configurate(IDriver drv) {
        if (drv instanceof DriverEventProvider) {
            ((DriverEventProvider) drv).addEventListener(this);
        }
        this.driver = drv;
        List<MITSignalSpecification> trendSpecs = new ArrayList<MITSignalSpecification>();
        ecgData = new Channel[driver.getDeviceInfo().getSignals().size()];
        for (int i = 0; i < ecgData.length; i++) {
            ecgData[i] = new Channel(driver.getDeviceInfo().getSignals().get(i).getADCResolution().shortValue(), (short) driver.getDeviceInfo().getSignals().get(i).getSampleFrequency(), driver.getDeviceInfo().getSignals().get(i).getDescription(), ECG_BUFFER_LEN);
        }
        int cuantos = 3 + (10 * driver.getDeviceInfo().getNumberOfUsedChannels());
        trendData = new Channel[cuantos];
        for (int i = 0; i < cuantos; i++) {
            trendSpecs.add(new MITSignalSpecification(16, (short) 1, 1, "", 16, ""));
            trendData[i] = new Channel((short) 16, (short) 1, "", DIAGRAM_BUFFER_LEN);
        }
        trendData[0].setName("R-R");
        trendSpecs.get(0).setDescription("R-R");
        trendSpecs.get(0).setUnits("ms");
        trendData[1].setName("AnchoP");
        trendSpecs.get(1).setDescription("AnchoP");
        trendSpecs.get(1).setUnits("ms");
        trendData[2].setName("AnchoQRS");
        trendSpecs.get(2).setDescription("AnchoQRS");
        trendSpecs.get(2).setUnits("ms");
        for (int i = 0; i < driver.getDeviceInfo().getNumberOfUsedChannels(); i++) {
            trendData[10 * i + 3].setName("AltoP[" + i + "]");
            trendSpecs.get(10 * i + 3).setDescription("AltoP[" + i + "]");
            trendSpecs.get(10 * i + 3).setUnits("mV");
            trendData[10 * i + 4].setName("AltoQ[" + i + "]");
            trendSpecs.get(10 * i + 4).setDescription("AltoQ[" + i + "]");
            trendSpecs.get(10 * i + 4).setUnits("uV");
            trendData[10 * i + 5].setName("AltoR[" + i + "]");
            trendSpecs.get(10 * i + 5).setDescription("AltoR[" + i + "]");
            trendSpecs.get(10 * i + 5).setUnits("uV");
            trendData[10 * i + 6].setName("AltoS[" + i + "]");
            trendSpecs.get(10 * i + 6).setDescription("AltoS[" + i + "]");
            trendSpecs.get(10 * i + 6).setUnits("uV");
            trendData[10 * i + 7].setName("desvST[" + i + "]");
            trendSpecs.get(10 * i + 7).setDescription("desvST[" + i + "]");
            trendSpecs.get(10 * i + 7).setUnits("uV");
            trendData[10 * i + 8].setName("pendST[" + i + "]");
            trendSpecs.get(10 * i + 8).setDescription("pendST[" + i + "]");
            trendSpecs.get(10 * i + 8).setUnits("uV/seg");
            trendData[10 * i + 9].setName("desvT[" + i + "]");
            trendSpecs.get(10 * i + 9).setDescription("desvT[" + i + "]");
            trendSpecs.get(10 * i + 9).setUnits("uV");
            trendData[10 * i + 10].setName("AnchoQ[" + i + "]");
            trendSpecs.get(10 * i + 10).setDescription("AnchoQ[" + i + "]");
            trendSpecs.get(10 * i + 10).setUnits("ms");
            trendData[10 * i + 11].setName("AnchoR[" + i + "]");
            trendSpecs.get(10 * i + 11).setDescription("AnchoR[" + i + "]");
            trendSpecs.get(10 * i + 11).setUnits("ms");
            trendData[10 * i + 12].setName("AnchoS[" + i + "]");
            trendSpecs.get(10 * i + 12).setDescription("AnchoS[" + i + "]");
            trendSpecs.get(10 * i + 12).setUnits("ms");
        }
        try {
            trendMitMgr.writeHeader(MonitoringSession.getInstance().newFileName(TREND_SUFIX), trendSpecs, driver.getAcquisitionStart());
            trendMitMgr.openDataFile(MonitoringSession.getInstance().newFileName(TREND_SUFIX));
            ecgMitMgr.writeHeader(MonitoringSession.getInstance().newFileName(ECG_SUFIX), driver.getDeviceInfo(), driver.getAcquisitionStart());
            ecgMitMgr.openDataFile(MonitoringSession.getInstance().newFileName(ECG_SUFIX));
            ecgMitMgr.openAnnotationsFile(MonitoringSession.getInstance().newFileName(ECG_SUFIX), driver.getDeviceInfo().getSignals().get(0).getSampleFrequency(), false);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "An error ocurred while generating annotations header.", e);
        }
        Global.getInstance().initialize(driver.getDeviceInfo().getNumberOfUsedChannels(), driver.getDeviceInfo());
        configureProcessing(driver.getDeviceInfo().getNumberOfUsedChannels());
    }

    /**
	 * Instancia as clases necesarias para o funcionamento do algoritmo de procesado, como o detector de latidos,
	 * delineación, clasificador... que dependen da inicialización previa da clase Global.
	 */
    public void configureProcessing(int usedChannels) {
        g = Global.getInstance();
        util = Util.getInstance();
        param = new short[g.nParam];
        tompkins = new Tompkins();
        laguna = new Laguna();
        parameters = new Parameters();
        classification = new Classification();
        clasificador = new Perceptron2();
        hrv = new Hrv();
        episodes = new Episodes(driver, ecgMitMgr.getAnnotationsMgr());
        shippingFamilies = new MorphologicalFamilies();
        for (int i = 0; i < shippingTrendDiagrams.length; i++) {
            shippingTrendDiagrams[i] = new DiagramaTendencias(TIEMPO_ENVIO_DIAGRAMAS_TENDENCIA * 60 * 4, usedChannels);
        }
        indiceDiagramaActual = 0;
        indiceMuestraActual = 0;
        trendDiagramSendTime = 0;
    }

    /**
	 * Thread de procesado de datos
	 */
    @Override
    protected void processingThread() {
        Thread.currentThread().setName("MEDIM_" + deviceType + "_PROC_THREAD");
        Log.i(DEBUG_TAG, "Processing thread started");
        int samplesLength = 0;
        short[][] channels;
        try {
            if (processing) {
                Log.i(DEBUG_TAG, "Initializing heartbeat detector...");
                tompkins.inicializadetector(driver, ecgMitMgr);
            }
            Log.i(DEBUG_TAG, "Starting processing main loop ...");
            g.stats.initialize(new String[] { "latidos", "familias" }, new Double[] { 0d, 0d });
            while (processing) {
                Log.i(DEBUG_TAG, "Reading from buffer ...");
                channels = driver.read();
                if (channels != null && processing) {
                    samplesLength = channels[0].length;
                    int freeSpaceOnBuffer = ecgData[0].remaining();
                    if (freeSpaceOnBuffer < samplesLength) {
                        Log.d("TEST", "No enougth space. Writting " + ecgData[0].getStored() + " to file.");
                        MonitoringSession.getInstance().getStatistics().increment("stored_samples", (double) ecgData[0].getStored());
                        ecgMitMgr.getDataFile().writeChannels(ecgData);
                    }
                    for (int i = 0; i < channels.length; i++) {
                        ecgData[i].add(channels[i]);
                    }
                    for (int sampleIndex = 0; sampleIndex < samplesLength; sampleIndex++) {
                        loadCurrentSamples(sampleIndex, channels);
                        sampleProcessing();
                    }
                } else {
                    processing = false;
                    log.debug("Processing complete!");
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "An error ocurred in monitoring loop", e);
        } finally {
            if (ecgData[0].getStored() > 0) {
                MonitoringSession.getInstance().getStatistics().increment("stored_samples", (double) ecgData[0].getStored());
                ecgMitMgr.getDataFile().writeChannels(ecgData);
            }
            Log.d(DEBUG_TAG, "Saving pending trend diagrams");
            savePendingTrendDiagrams();
            sendPendingTrendDiagram();
            sendMorphologicalFamilies();
            Log.d(DEBUG_TAG, "Writiing annotations...");
            ecgMitMgr.getAnnotationsMgr().writeAll();
            Log.d(DEBUG_TAG, "Closing files ...");
            trendMitMgr.closeAnnotationsFile();
            trendMitMgr.closeDataFile();
            ecgMitMgr.closeAnnotationsFile();
            ecgMitMgr.closeDataFile();
            Log.d(DEBUG_TAG, "Files closed.");
            g.stats.put("samples", (double) (g.tiempo));
            try {
                g.stats.store(MedimService.getServiceConfig().getFileOutputStream(MonitoringSession.getInstance().getBaseFileName() + "_stats.txt", ServiceConfig.LOGS));
            } catch (IOException e1) {
                Log.e(DEBUG_TAG, "Could not store statistics.", e1);
            }
            g.release();
            util.release();
            Log.d(DEBUG_TAG, "ECG monitoring thread terminating.");
            sendProcessingCompleteEvent();
        }
    }

    /**
	 * Método main() Modificado para unha soa mostra, o bucle principal está na función procesingThread, que invoca esta
	 * función para cada nova mostra
	 */
    public void sampleProcessing() {
        MonitoringSession.getInstance().getStatistics().increment("processed_samples");
        try {
            if (g.tiempo % UMB_RESET_TIME == 0) {
                Log.i(DEBUG_TAG, "SP: Reseting QRS");
                tompkins.resetumbralesQRS();
            }
            util.leesenal();
            tompkins.detectaQRS();
            if (g.latido.poslatido != 0) {
                g.latidoanterior.rrsig = (short) (g.latido.poslatido - g.latidoanterior.poslatido);
                g.latido.rr = g.latidoanterior.rrsig;
                g.latido.clasificacion = AnnotationCodes.NORMAL;
                if (!flagplaninicial) {
                    flagplaninicial = classification.gplaninicial();
                    g.latido.clasificacion = AnnotationCodes.LEARN;
                    g.latido.subtyp = 0;
                }
                if (flagplaninicial) {
                    classification.agrupalatido();
                }
                if (g.latido.subtyp == -1) {
                    g.latido.reset();
                    g.actualizapunteros();
                    return;
                }
                laguna.delineaQRS();
                if (g.latido.onQRS == 0) {
                    g.latido.reset();
                    g.actualizapunteros();
                    return;
                }
                if (g.latido.onQRS != 0) {
                    latidovalido = false;
                    for (lead = 0; lead < g.n; lead++) {
                        if (g.latido.dermax[lead] < g.latido.maxder) {
                            continue;
                        }
                        parameters.isoelectrica((short) lead);
                        if (parameters.morphoQRS((short) lead) < 0) {
                            continue;
                        }
                        latidovalido = true;
                        parameters.areaQRS((short) lead);
                        parameters.desvST((short) lead);
                    }
                    if (!latidovalido) {
                        g.latido.reset();
                        g.actualizapunteros();
                        return;
                    }
                    hrv.insertaLatido();
                    laguna.delineaP();
                    laguna.delineaT();
                    for (lead = 0; lead < g.n; lead++) {
                        if (g.latido.dermax[lead] < g.latido.maxder) {
                            continue;
                        }
                        parameters.desvT((short) lead);
                    }
                }
                if (g.latidoanterior.poslatido != 0) {
                    MitAnnotation ma = ecgMitMgr.getAnnotationsMgr();
                    ma.generateAnnotation(g.latidoanterior.poslatido, g.latidoanterior.clasificacion, (byte) g.latidoanterior.subtyp, (byte) 0, (byte) 0, g.generateAnnotationAuxField(), true);
                    g.stats.increment("latidos");
                }
                interpola();
                g.latidoanterior = (HeartBeat) g.latido.clone();
                g.latido = new HeartBeat();
            }
            g.actualizapunteros();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "An error ocurred while generating annotation.", e);
        } catch (CloneNotSupportedException e) {
            Log.e(DEBUG_TAG, "Could not clone heartbeat. Clone not supported.", e);
        }
    }

    /**
	 * 
	 */
    private void interpola() {
        while ((2 * g.latidoanterior.poslatido) > (tiempointerp + 125)) {
            if (g.latidoanterior.rr == 32767) break;
            param[0] = (short) (4 * g.latidoanterior.rr);
            param[1] = g.latidoanterior.anchoP;
            param[2] = g.latidoanterior.anchoQRS;
            shippingTrendDiagrams[indiceDiagramaActual].getRR().getValues()[indiceMuestraActual] = (short) (4 * g.latidoanterior.rr);
            shippingTrendDiagrams[indiceDiagramaActual].getAnchoP().getValues()[indiceMuestraActual] = g.latidoanterior.anchoP;
            shippingTrendDiagrams[indiceDiagramaActual].getAnchoQRS().getValues()[indiceMuestraActual] = g.latidoanterior.anchoQRS;
            for (int derivacion = 0; derivacion < g.n; derivacion++) {
                param[10 * derivacion + 3] = g.latidoanterior.valP[derivacion];
                param[10 * derivacion + 4] = g.latidoanterior.valQ[derivacion];
                param[10 * derivacion + 5] = g.latidoanterior.valR[derivacion];
                param[10 * derivacion + 6] = g.latidoanterior.valS[derivacion];
                param[10 * derivacion + 7] = g.latidoanterior.desvST[derivacion];
                param[10 * derivacion + 8] = g.latidoanterior.pendST[derivacion];
                param[10 * derivacion + 9] = g.latidoanterior.desvT[derivacion];
                param[10 * derivacion + 10] = g.latidoanterior.anchoQ[derivacion];
                param[10 * derivacion + 11] = g.latidoanterior.anchoR[derivacion];
                param[10 * derivacion + 12] = g.latidoanterior.anchoS[derivacion];
                shippingTrendDiagrams[indiceDiagramaActual].getAltoP().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.valP[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAltoQ().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.valQ[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAltoR().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.valR[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAltoS().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.valS[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getDesvST().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.desvST[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getPendST().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.pendST[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getDesvT().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.desvT[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAnchoQ().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.anchoQ[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAnchoR().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.anchoR[derivacion]);
                shippingTrendDiagrams[indiceDiagramaActual].getAnchoS().setElementAt(derivacion, indiceMuestraActual, g.latidoanterior.anchoS[derivacion]);
            }
            indiceMuestraActual++;
            if (indiceMuestraActual == shippingTrendDiagrams[indiceDiagramaActual].getLength()) {
                trendDiagramSendTime = g.tiempo;
                long muestraInicial = g.tiempo - (TIEMPO_ENVIO_DIAGRAMAS_TENDENCIA * 60 * 250);
                shippingTrendDiagrams[indiceDiagramaActual].setTimeStamp(TimeUtils.samplesFromProtocolStart(driver.getAcquisitionStart(), muestraInicial));
                Log.d("TD", "Sending trend diagram: ");
                Log.d("TD", "g.tiempo: " + g.tiempo + " samples, " + (g.tiempo / 250));
                Log.d("TD", "muestra inicial: " + muestraInicial);
                Log.d("TD", "timestamp: " + shippingTrendDiagrams[indiceDiagramaActual].getTimeStamp());
                try {
                    MonitoringMessageMgr.getInstance().sendMessage(shippingTrendDiagrams[indiceDiagramaActual]);
                } catch (Exception e) {
                    log.error("Error sending trend diagrams", e);
                }
                sendMorphologicalFamilies();
                indiceMuestraActual = 0;
                indiceDiagramaActual = (indiceDiagramaActual == 0) ? 1 : 0;
            }
            if (trendData[0].remaining() < 1) {
                trendMitMgr.getDataFile().writeChannels(trendData);
            }
            for (int i = 0; i < g.nParam; i++) {
                trendData[i].add(param[i]);
            }
            if ((tiempointerp % 500) == 0) {
                for (lead = 0; lead < g.n; lead++) {
                    episodes.detectaEpisodio(g.diaST[lead], g.latidoanterior.desvST[lead], (short) 15, (short) 50, (short) 100, (short) 50, (short) 30, (short) 25, (short) 45, (short) 30, tiempointerp / 2, (short) lead, (short) 93, (short) 0);
                }
            }
            tiempointerp += 125;
        }
    }

    /**
	 * 
	 */
    private void sendMorphologicalFamilies() {
        shippingFamilies.getFamilies().clear();
        shippingFamilies.setTimestamp(TimeUtils.calendarFromSample(driver.getAcquisitionStart(), g.tiempo));
        for (int j = 0; j < Constants.NFAMILIAS; j++) {
            if (g.familias[classification.ordenf[j]] != null && g.familias[classification.ordenf[j]].isInitialized()) {
                MorphologicalFamily tmp = new MorphologicalFamily();
                ArrayList<Integer> canales = new ArrayList<Integer>();
                tmp.setPrimero(g.familias[classification.ordenf[j]].primero);
                tmp.setUltimo(g.familias[classification.ordenf[j]].ultimo);
                tmp.setLatidos(new ShortMatrix());
                for (int i = 0; i < g.n; i++) {
                    if (g.familias[classification.ordenf[j]].novalido[i] == false) {
                        canales.add(i + 1);
                        ShortList sl = new ShortList();
                        sl.setValues(g.familias[classification.ordenf[j]].lat[i].clone());
                        tmp.getLatidos().getRows().add(sl);
                    }
                }
                int s = canales.size();
                IntegerList validChannels = new IntegerList(s);
                for (int i = 0; i < s; i++) {
                    validChannels.getValues()[i] = canales.get(i);
                }
                tmp.setCanalesValidos(validChannels);
                shippingFamilies.getFamilies().add(tmp);
            }
        }
        if (shippingFamilies.getFamilies().size() > 0) {
            try {
                MonitoringMessageMgr.getInstance().sendMessage(shippingFamilies);
            } catch (Exception e) {
                log.error("Error sending morphological families", e);
            }
        }
    }

    /**
	 * 
	 */
    private void sendPendingTrendDiagram() {
        if (true) {
            for (; indiceMuestraActual < shippingTrendDiagrams[indiceDiagramaActual].getLength(); indiceMuestraActual++) {
                shippingTrendDiagrams[indiceDiagramaActual].getRR().getValues()[indiceMuestraActual] = 0;
                shippingTrendDiagrams[indiceDiagramaActual].getAnchoP().getValues()[indiceMuestraActual] = 0;
                shippingTrendDiagrams[indiceDiagramaActual].getAnchoQRS().getValues()[indiceMuestraActual] = 0;
                for (int i = 0; i < g.n; i++) {
                    shippingTrendDiagrams[indiceDiagramaActual].getAltoP().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAltoQ().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAltoR().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAltoS().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getDesvST().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getPendST().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getDesvT().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAnchoQ().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAnchoR().setElementAt(i, indiceMuestraActual, (short) 0);
                    shippingTrendDiagrams[indiceDiagramaActual].getAnchoS().setElementAt(i, indiceMuestraActual, (short) 0);
                }
            }
            shippingTrendDiagrams[indiceDiagramaActual].setTimeStamp(TimeUtils.samplesFromProtocolStart(driver.getAcquisitionStart(), trendDiagramSendTime));
            try {
                MonitoringMessageMgr.getInstance().sendMessage(shippingTrendDiagrams[indiceDiagramaActual]);
            } catch (Exception e) {
                log.error("Error sending pending trend diagrams.", e);
            }
        }
    }

    /**
	 * Guarda los diagramas de tendencia pendientes
	 */
    private void savePendingTrendDiagrams() {
        try {
            while (!trendData[0].isEmpty()) {
                for (int j = 0; j < trendData.length; j++) {
                    trendMitMgr.getDataFile().getOutputStream().write(trendData[j].readSingle());
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error writting pending trend diagrams", e);
        }
    }

    /**
	 * 
	 */
    private void generaDatosFamilias() {
        try {
            int nchan;
            if (g.familias.length > 0) {
                FileOutputStream fos = MedimService.getServiceConfig().getFileOutputStream(MonitoringSession.getInstance().getBaseFileName() + "_familys.data", ServiceConfig.LOGS);
                nchan = g.familias[0].lat.length;
                for (Family fam : g.familias) {
                    if (fam != null) {
                        for (int chan = 0; chan < nchan; chan++) {
                            short[] latido = fam.lat[chan];
                            for (int sample = 0; sample < latido.length; sample++) {
                                fos.write((latido[sample] + " ").getBytes());
                            }
                            fos.write("\n".getBytes());
                        }
                    }
                }
                fos.close();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Error generating families data.", e);
        }
    }

    private void generaDatosHrv() {
        try {
            ArrayList<HrvHora> hrvs = hrv.hrvs;
            FileOutputStream fos = MedimService.getServiceConfig().getFileOutputStream(MonitoringSession.getInstance().getBaseFileName() + "_hrv.data", ServiceConfig.LOGS);
            fos.write(("Count: " + hrvs.size() + "\n").getBytes());
            for (int i = 0; i < hrvs.size(); i++) {
                ArrayList<Double> cur = hrvs.get(i).getRr();
                fos.write(((rrToString(cur) + "\n").getBytes()));
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTestAnn(String comment) {
        Annotation n = new Annotation();
        n.time = g.tiempo;
        n.type = AnnotationCodes.NOTE;
        n.aux = comment.getBytes();
        if (processing) {
            try {
                ecgMitMgr.getAnnotationsMgr().generateAnnotation(n);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error writting battery status annotation.", e);
            }
        }
    }

    /**
	 * 
	 * @param rr
	 * @return
	 */
    private String rrToString(ArrayList<Double> rr) {
        StringBuilder sb = new StringBuilder();
        for (Double d : rr) {
            sb.append(d);
            sb.append(" ");
        }
        return sb.toString();
    }

    @Override
    public void onBluetoothDisconnection(DeviceType device) {
    }

    @Override
    public void onBluetoothCoverage(DeviceType device, int estado) {
    }

    @Override
    public void onBatteryStatus(DeviceType device, BatteryStatus status) {
    }

    @Override
    public void onSensorConnectivityChange(DeviceType device, boolean desconexion, Map<String, Sensor> sensors) {
        Annotation n = new Annotation();
        n.time = g != null ? g.tiempo : 0;
        n.type = AnnotationCodes.NOTE;
        n.aux = (desconexion ? "Sensor disconnection" : "Sensor reconnection").getBytes();
        if (processing) {
            try {
                ecgMitMgr.getAnnotationsMgr().generateAnnotation(n);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error writting sensor status annotation.", e);
            }
        }
    }

    @Override
    public void onPacketLost(DeviceType device, int length, Calendar when, int sampleIndexInDriver) {
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
        Annotation n = new Annotation();
        n.time = g.tiempo;
        n.type = AnnotationCodes.NOTE;
        n.aux = (length + " packets lost at " + df.format(when.getTime())).getBytes();
        if (processing) {
            try {
                ecgMitMgr.getAnnotationsMgr().generateAnnotation(n);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error writting packet lost annotation.", e);
            }
        }
    }

    @Override
    public void onMessage(String message) {
    }

    @Override
    public void onMessage(DeviceType device, String message) {
    }

    @Override
    public void onAcquisitionStarted(DeviceType device) {
    }

    @Override
    public void onAcquisitionError(DeviceType device) {
    }

    @Override
    public void onAcquisitionError(DeviceType device, String message) {
    }

    @Override
    public void onAcquisitionComplete(DeviceType device) {
    }
}
