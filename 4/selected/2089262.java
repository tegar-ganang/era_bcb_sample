package fsmsim.parserXML;

import fsmsim.buffer.Buffer;
import fsmsim.dataStructure.Container;
import fsmsim.dataStructure.Event;
import fsmsim.dataStructure.FSM;
import fsmsim.exception.FSMException;
import java.util.Vector;

/**
 * <p>Title: FSMSim</p>
 *
 * <p>Description: Simulatore di macchine a stati finiti.</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: Bollati, Donati, Gabrielli, Peli</p>
 *
 * @author Bollati, Donati, Gabrielli, Peli
 * @version 3.0
 */
public class CoherenceCheck {

    private boolean isCoherent;

    public CoherenceCheck() {
        this.isCoherent = true;
    }

    public void setIsCoherent(boolean isCoherent) {
        this.isCoherent = isCoherent;
    }

    public boolean getIsCoherent() {
        return this.isCoherent;
    }

    /**
     * Metodo che controlla consistenza tra le FSM:
     * eventi uscita fsm1 devono esser ingresso di fsm2
     * @param fsm1
     * @param fms2
     * @return
     */
    private boolean checkOutputEvents(FSM fsm1, FSM fms2) {
        for (int i = 0; i < fsm1.getOutputEventList().size(); i++) {
            if ((fsm1.getOutputEventList().getElement(i) != null)) if (!(fms2.getInputEventList().containsElement(fsm1.getOutputEventList().getElement(i)))) return false;
        }
        return true;
    }

    /**
     * verifica che la lista degli eventi esterni è composta di soli eventi esterni
     * @param listaEventiEsterni
     * @return
     */
    private boolean checkExternalEvents(Container externalEventsList) {
        for (int i = 0; i < externalEventsList.size(); i++) {
            if (((Event) (externalEventsList.getElement(i))).isInput()) return false;
        }
        return true;
    }

    /**
     * verifica che ci siano almeno una lettrice e una scrittrice
     * @param fsms
     * @return
     */
    private boolean checkReadWrite(Vector<FSM> fsms) {
        int readerCount = 0;
        int writerCount = 0;
        for (int i = 0; i < fsms.size(); i++) if (fsms.get(i).isReader()) readerCount++; else writerCount++;
        if ((readerCount == 0) || (writerCount == 0)) return false;
        return true;
    }

    /**
     * checkCoherence
     */
    public boolean checkCoherence(Vector<FSM> fsms, Container externalEventsList, Buffer buffer) throws FSMException {
        if (!checkReadWrite(fsms)) {
            this.setIsCoherent(false);
            throw new FSMException("Il sistema non contiene FSM readers o FSM writers.");
        }
        if (!checkExternalEvents(externalEventsList)) {
            this.setIsCoherent(false);
            throw new FSMException("La lista degli eventi esterni contiene eventi definiti" + "    come interni.");
        }
        for (int i = 0; i < fsms.size(); i++) {
            if (!fsms.get(i).checkInputEvents()) {
                this.setIsCoherent(false);
                throw new FSMException("Transizione nella FSM" + (i + 1) + " non ha un evento in ingresso.");
            }
        }
        for (int i = 0; i < fsms.size(); i++) {
            if (!(fsms.get(i).isReader()) && (!fsms.get(i).checkInternalExternalEvents())) {
                this.setIsCoherent(false);
                throw new FSMException("Gli eventi non sono digiunti.");
            }
        }
        for (int i = 0; i < fsms.size(); i++) {
            if ((fsms.get(i).isReader()) && (!buffer.checkBufferEvents(fsms.get(i).getInputEventList()))) {
                this.setIsCoherent(false);
                throw new FSMException("Il buffer contiene eventi non ammissibili");
            }
        }
        for (int i = 0; i < fsms.size(); i++) {
            for (int j = 0; j < fsms.size(); j++) {
                if ((!fsms.get(i).isReader()) && ((fsms.get(j).isReader())) && (!checkOutputEvents(fsms.get(i), fsms.get(j)))) {
                    this.setIsCoherent(false);
                    throw new FSMException("Un evento prodotto dalla fsm1 non è " + "consumabile dalla fsm2.");
                }
            }
        }
        return this.getIsCoherent();
    }
}
