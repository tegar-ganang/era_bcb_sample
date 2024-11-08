package es.usc.citius.servando.android.medim.model;

import java.util.GregorianCalendar;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import es.usc.citius.servando.android.models.patients.Patient;

/**
 * Esta clase representa una solicitud de ECG. Podrá ser enviada a los dispositivos,
 * que deberán obtener el fragmento de ECG correspondiente, y responder en consecuencia.
 * @author Tomás Teijeiro Campo
 */
@Root(name = "ECGRequest")
@Default(DefaultType.FIELD)
public class ECGRequest {

    /**
     * Fecha de inicio del fragmento de ECG solicitado.
     */
    @Element(name = "startDate")
    private GregorianCalendar startDate;

    /**
     * Número de canales de la señal solictada.
     */
    @Element(name = "channels")
    private int channels;

    /**
     * Duración de la señal, en número de muestras.
     */
    @Element(name = "duration")
    private long duration;

    /**
     * Paciente del cual se solicitan los datos.
     */
    @Element(name = "patient")
    private Patient patient;

    /**
     * Fecha de inicio del fragmento de ECG solicitado.
     * @return the startDate
     */
    public GregorianCalendar getStartDate() {
        return startDate;
    }

    /**
     * Fecha de inicio del fragmento de ECG solicitado.
     * @param startDate the startDate to set
     */
    public void setStartDate(GregorianCalendar startDate) {
        this.startDate = startDate;
    }

    /**
     * Número de canales de la señal solictada.
     * @return the channels
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Número de canales de la señal solictada.
     * @param channels the channels to set
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * Duración de la señal, en número de muestras.
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Duración de la señal, en número de muestras.
     * @param duration the duration to set
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * Paciente del cual se solicitan los datos.
     * @return the patient
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * Paciente del cual se solicitan los datos.
     * @param patient the patient to set
     */
    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}
