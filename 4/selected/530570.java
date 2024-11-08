package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungAuswahlkriteriumTransportmittelDAO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungLagermittelTransportmittelEinAuslagernDAO;

/**
 * <p>
 * Title: ZuordnungAuswahlkriteriumTransportmittelDAOTest.java<br>
 * Description: Testklasse f�r Persistence Layer Wissensbasis Zuordnung Auswahlkriterium - TM <br>
 * Copyright: Copyright (c) 2009<br>
 * Company: LAGERPLAN Organisation
 * </p>
 * 
 * @author %author: Michael Felber%
 * @version %version: 1%
 */
public class ZuordnungAuswahlkriteriumTransportmittelDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_AUSWAHLKRITERIUM_TM = new ZuordnungXYTO(9, 8);

    public ZuordnungAuswahlkriteriumTransportmittelDAOTest(String name) {
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
	 * testGetZuordnungenAuswahlkriteriumTransportmittel. Test von Funktionen.
	 */
    public final void testGetZuordnungenAuswahlkriteriumTransportmittel() {
        try {
            ZuordnungAuswahlkriteriumTransportmittelDAO dao = new ZuordnungAuswahlkriteriumTransportmittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumTransportmittel();
            assertTrue("[testGetZuordnungenAuswahlkriteriumTransportmittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenAuswahlkriteriumTransportmittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_AUSWAHLKRITERIUM_TM);
            assertTrue("[testGetZuordnungenAuswahlkriteriumTransportmittel]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungenAuswahlkriteriumTransportmittel. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungenAuswahlkriteriumTransportmittel() {
        try {
            ZuordnungAuswahlkriteriumTransportmittelDAO dao = new ZuordnungAuswahlkriteriumTransportmittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumTransportmittel();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumTransportmittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testSaveZuordnungenAuswahlkriteriumTransportmittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenAuswahlkriteriumTransportmittel(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenAuswahlkriteriumTransportmittel();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumTransportmittel]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testSaveZuordnungenAuswahlkriteriumTransportmittel]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumTransportmittel]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
