package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungAuswahlkriteriumQuantitativLagermittelDAO;

/**
 * <p>
 * Title: ZuordnungAuswahlkriteriumQuantitativLagermittelDAOTest.java<br>
 * Description: Testklasse f�r Persistence Layer Wissensbasis Zuordnung quantitativer Auswahlkriterium - LM <br>
 * Copyright: Copyright (c) 2009<br>
 * Company: LAGERPLAN Organisation
 * </p>
 * 
 * @author %author: Michael Felber%
 * @version %version: 1%
 */
public class ZuordnungAuswahlkriteriumQuantitativLagermittelDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_AUSWAHLKRITERIUMQUANTITATIV_LM = new ZuordnungXYTO(2, 20);

    public ZuordnungAuswahlkriteriumQuantitativLagermittelDAOTest(String name) {
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
	 * testGetZuordnungenAuswahlkriteriumQuantitativLagermittel. Test von Funktionen.
	 */
    public final void testGetZuordnungenAuswahlkriteriumQuantitativLagermittel() {
        try {
            ZuordnungAuswahlkriteriumQuantitativLagermittelDAO dao = new ZuordnungAuswahlkriteriumQuantitativLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumQuantitativLagermittel();
            assertTrue("[testGetZuordnungenAuswahlkriteriumQuantitativLagermittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenAuswahlkriteriumQuantitativLagermittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_AUSWAHLKRITERIUMQUANTITATIV_LM);
            assertTrue("[testGetZuordnungenAuswahlkriteriumQuantitativLagermittel]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungenAuswahlkriteriumLagermittel. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO() {
        try {
            ZuordnungAuswahlkriteriumQuantitativLagermittelDAO dao = new ZuordnungAuswahlkriteriumQuantitativLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumQuantitativLagermittel();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenAuswahlkriteriumQuantitativLagermittel(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenAuswahlkriteriumQuantitativLagermittel();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQuantitativLagermittelDAO]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
