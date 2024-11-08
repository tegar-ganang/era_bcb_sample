package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungLagermittelTransportmittelEinAuslagernDAO;

/**
 * <p>
 * Title: ZuordnungLagermittelTransportmittelEinAuslagernDAOTest.java<br>
 * Description: Testklasse f�r Persistence Layer Wissensbasis Zuordnung LM - TM zum Ein/Auslagern.<br>
 * Copyright: Copyright (c) 2009<br>
 * Company: LAGERPLAN Organisation
 * </p>
 * 
 * @author %author: Michael Felber%
 * @version %version: 1%
 */
public class ZuordnungLagermittelTransportmittelEinAuslagernDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_LM_TM = new ZuordnungXYTO(18, 1);

    public ZuordnungLagermittelTransportmittelEinAuslagernDAOTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        super.destroyDatabaseConnections(AbstractLagerplanTest.DB_TEST);
    }

    /**
	 * testGetZuordnungenLagermittelTransportmittelEinAuslagern. Test von Funktionen.
	 */
    public final void testGetZuordnungenLagermittelTransportmittelEinAuslagern() {
        try {
            ZuordnungLagermittelTransportmittelEinAuslagernDAO dao = new ZuordnungLagermittelTransportmittelEinAuslagernDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenLagermittelTransportmittelEinAuslagern();
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_LM_TM);
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungenLagermittelTransportmittelEinAuslagern. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungenLagermittelTransportmittelEinAuslagern() {
        try {
            ZuordnungLagermittelTransportmittelEinAuslagernDAO dao = new ZuordnungLagermittelTransportmittelEinAuslagernDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenLagermittelTransportmittelEinAuslagern();
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenLagermittelTransportmittelEinAuslagern(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenLagermittelTransportmittelEinAuslagern();
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
