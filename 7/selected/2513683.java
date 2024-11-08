package main.process;

import java.util.Vector;

/**
 * Classe astratta che rappresenta una politica di scheduling dei processi.
 * @author Michele Ciccozzi
 * @version 1.1
 */
public abstract class Policy {

    /**
	 * Rappresenta la durata di un quanto.
	 */
    static int quantum_;

    /**
     * Lista dei processi.
     */
    Process[] process_;

    /**
	 * Vettore che tiene traccia dei processi pronti ad eseguire.
	 */
    Vector<Process> readyList_;

    /**
     * Riferimento allo scheduler.
     */
    Scheduler scheduler_;

    /**
     * Selettore del quanto.
     * @return int - rappresentante il quanto
     */
    public static int getQuantum() {
        return quantum_;
    }

    /**
	 * Selettore della lista dei processi pronti ad eseguire.
	 * @return Vector<Process> - Vettore rappresentante la lista dei processi pronti
	 */
    public Vector<Process> getReadyList() {
        return readyList_;
    }

    /**
     * Selettore dello scheduler.
     * @return  Scheduler - riferimento allo scheduler dei processi.
     */
    public Scheduler getScheduler() {
        return scheduler_;
    }

    /**
     * Metodo astratto che calcola il processo a cui spetta eseguire.
     * @return Process - riferimento al processo che eseguira'
     */
    public abstract Process getNextProcess();

    /**
     * Imposta la durata dei quanti di tempo per una politica Round Robin.
     * @param quantum - rappresenta il quanto
     * @throws IllegalArgumentException
     */
    public static void setQuantum(int quantum) throws IllegalArgumentException {
        if (quantum <= 0) throw new IllegalArgumentException("Impossibile impostare un valore negativo alla durata del quanto."); else quantum_ = quantum;
    }

    /**
	 * Imposta la lista dei processi pronti ad eseguire.
	 * @param readyList - lista dei processi pronti
	 */
    public void setReadyList(Vector<Process> readyList) {
        readyList_ = readyList;
    }

    /**
	 * Inserisce un processo nella lista dei processi pronti.
	 * @param process - processo da inserire
	 */
    void add(Process process) {
        readyList_.add(process);
    }

    /**
	 * Inserisce un processo nella lista dei processi in modo ordinato
	 * secondo il tempo di arrivo.
	 * @param process - processo da inserire
	 */
    public void addProcess(Process process) {
        if (process_[0] == null) process_[0] = process; else {
            int i = 0;
            boolean trovato = false;
            while (i < process_.length && !trovato) if (process_[i] == null || process.getArrivalTime() < process_[i].getArrivalTime()) trovato = true; else i++;
            if (process_[i] == null) process_[i] = process; else {
                for (int j = process_.length - 1; j > i; j--) process_[j] = process_[j - 1];
                process_[i] = process;
            }
        }
    }

    /**
	 * Rimuove un processo dalla lista dei processi pronti.
	 * @param process - processo da rimuovere
	 */
    boolean remove(Process process) {
        readyList_.removeElement(process);
        boolean done = false;
        int i = 0;
        while (!done) if (process_[i].getProcessId() == process.getProcessId()) done = true; else i++;
        for (; i < process_.length - 1; i++) process_[i] = process_[i + 1];
        process_[i] = process;
        return readyList_.isEmpty();
    }

    /**
     * Imposta il riferimento allo scheduler che fa uso di una politica.
     * @param scheduler - rappresenta lo scheduler a cui fare riferimento
     */
    public void setScheduler(Scheduler scheduler) {
        scheduler_ = scheduler;
    }

    /**
     *  Crea una nuova istanza di Policy
     */
    public Policy() {
        process_ = new Process[Scheduler.getMaxProcesses()];
        readyList_ = new Vector<Process>(Scheduler.getMaxProcesses());
        quantum_ = 0;
    }
}
