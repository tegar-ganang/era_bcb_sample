package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungAuswahlkriteriumLagermittelDAO;

/**
 * <p>
 * Title: ZuordnungAuswahlkriteriumLagermittelDAOTest.java<br>
 * Description: Testklasse f�r Persistence Layer Wissensbasis Zuordnung Auswahlkriterium - LM <br>
 * Copyright: Copyright (c) 2009<br>
 * Company: LAGERPLAN Organisation
 * </p>
 * 
 * @author %author: Michael Felber%
 * @version %version: 1%
 */
public class ZuordnungAuswahlkriteriumLagermittelDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_AUSWAHLKRITERIUM_LM = new ZuordnungXYTO(1, 12);

    public ZuordnungAuswahlkriteriumLagermittelDAOTest(String name) {
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
	 * testGetZuordnungenAuswahlkriteriumLagermittel. Test von Funktionen.
	 */
    public final void testGetZuordnungenAuswahlkriteriumLagermittel() {
        try {
            ZuordnungAuswahlkriteriumLagermittelDAO dao = new ZuordnungAuswahlkriteriumLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumLagermittel();
            assertTrue("[testGetZuordnungenAuswahlkriteriumLagermittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenAuswahlkriteriumLagermittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_AUSWAHLKRITERIUM_LM);
            assertTrue("[testGetZuordnungenAuswahlkriteriumLagermittel]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungenAuswahlkriteriumLagermittel. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungenAuswahlkriteriumLagermittel() {
        try {
            ZuordnungAuswahlkriteriumLagermittelDAO dao = new ZuordnungAuswahlkriteriumLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumLagermittel();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumLagermittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testSaveZuordnungenAuswahlkriteriumLagermittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenAuswahlkriteriumLagermittel(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenAuswahlkriteriumLagermittel();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumLagermittel]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testSaveZuordnungenAuswahlkriteriumLagermittel]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testSaveZuordnungenAuswahlkriteriumLagermittel]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
