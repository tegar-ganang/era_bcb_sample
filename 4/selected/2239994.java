package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungTransportmittelEinAuslagernVorzoneDAO;

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
public class ZuordnungTransportmittelEinAuslagernVorzoneDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_TM_EINAUSLAGERN_TM_VORZONE = new ZuordnungXYTO(1, 18);

    public ZuordnungTransportmittelEinAuslagernVorzoneDAOTest(String name) {
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
	 * testGetZuordnungTransportmittelEinAuslagernVorzone. Test von Funktionen.
	 */
    public final void testGetZuordnungTransportmittelEinAuslagernVorzone() {
        try {
            ZuordnungTransportmittelEinAuslagernVorzoneDAO dao = new ZuordnungTransportmittelEinAuslagernVorzoneDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenTransportmittelEinAuslagernVorzone();
            assertTrue("[testGetZuordnungTransportmittelEinAuslagernVorzone]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungTransportmittelEinAuslagernVorzone]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            System.out.println("[testGetZuordnungTransportmittelEinAuslagernVorzone] Anzahl Elemente: " + datensaetze.size());
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_TM_EINAUSLAGERN_TM_VORZONE);
            assertTrue("[testGetZuordnungTransportmittelEinAuslagernVorzone]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungTransportmittelEinAuslagernVorzone. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungTransportmittelEinAuslagernVorzone() {
        try {
            ZuordnungTransportmittelEinAuslagernVorzoneDAO dao = new ZuordnungTransportmittelEinAuslagernVorzoneDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenTransportmittelEinAuslagernVorzone();
            assertTrue("[testSaveZuordnungTransportmittelEinAuslagernVorzone]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testSaveZuordnungTransportmittelEinAuslagernVorzone]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenTransportmittelEinAuslagernVorzone(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenTransportmittelEinAuslagernVorzone();
            assertTrue("[testSaveZuordnungTransportmittelEinAuslagernVorzone]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testSaveZuordnungTransportmittelEinAuslagernVorzone]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testGetZuordnungenLagermittelTransportmittelEinAuslagern]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
