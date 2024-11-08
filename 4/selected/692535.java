package test.junit.com.lagerplan.wissensbasis.persistence;

import java.util.Hashtable;
import test.junit.com.lagerplan.AbstractLagerplanTest;
import com.lagerplan.basisdienste.wissensbasis.data.ZuordnungXYTO;
import com.lagerplan.basisdienste.wissensbasis.persistence.ZuordnungAuswahlkriteriumQualitativLagermittelDAO;

/**
 * <p>
 * Title: ZuordnungAuswahlkriteriumQualitativLagermittelDAOTest.java<br>
 * Description: Testklasse f�r Persistence Layer Wissensbasis Zuordnung qualitativer Auswahlkriterium - LM <br>
 * Copyright: Copyright (c) 2009<br>
 * Company: LAGERPLAN Organisation
 * </p>
 * 
 * @author %author: Michael Felber%
 * @version %version: 1%
 */
public class ZuordnungAuswahlkriteriumQualitativLagermittelDAOTest extends AbstractLagerplanTest {

    private final ZuordnungXYTO ZUORD_AUSWAHLKRITERIUMQUALITATIV_LM = new ZuordnungXYTO(1, 12);

    public ZuordnungAuswahlkriteriumQualitativLagermittelDAOTest(String name) {
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
	 * testGetZuordnungenAuswahlkriteriumQualitativLagermittel. Test von Funktionen.
	 */
    public final void testGetZuordnungenAuswahlkriteriumQualitativLagermittel() {
        try {
            ZuordnungAuswahlkriteriumQualitativLagermittelDAO dao = new ZuordnungAuswahlkriteriumQualitativLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumQualitativLagermittel();
            assertTrue("[testGetZuordnungenAuswahlkriteriumQualitativLagermittel]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testGetZuordnungenAuswahlkriteriumQualitativLagermittel]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            Boolean zuordnungVorhanden = datensaetze.containsKey(ZUORD_AUSWAHLKRITERIUMQUALITATIV_LM);
            assertTrue("[testGetZuordnungenAuswahlkriteriumQualitativLagermittel]: Item darf nicht FALSE sein!", false != zuordnungVorhanden);
        } finally {
        }
    }

    /**
	 * testSaveZuordnungenAuswahlkriteriumLagermittel. Test der Speichern Funktion
	 */
    public final void testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO() {
        try {
            ZuordnungAuswahlkriteriumQualitativLagermittelDAO dao = new ZuordnungAuswahlkriteriumQualitativLagermittelDAO();
            Hashtable<ZuordnungXYTO, Boolean> datensaetze = dao.getZuordnungenAuswahlkriteriumQualitativLagermittel();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO]: Collection darf nicht NULL sein!", null != datensaetze);
            assertTrue("[testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO]: Collection muss Elemente enthalten!", datensaetze.size() > 0);
            int readCount = datensaetze.size();
            dao.saveZuordnungenAuswahlkriteriumQualitativLagermittel(datensaetze);
            Hashtable<ZuordnungXYTO, Boolean> datensaetzeNachSchreiben = dao.getZuordnungenAuswahlkriteriumQualitativLagermittel();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO]: Collection darf nicht NULL sein!", null != datensaetzeNachSchreiben);
            assertTrue("[testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO]: Collection muss Elemente enthalten!", datensaetzeNachSchreiben.size() > 0);
            int writeCount = datensaetzeNachSchreiben.size();
            assertTrue("[testSaveZuordnungAuswahlkriteriumQualitativLagermittelDAO]: Die Anzahl der Datens�tze muss �bereinstimmen!", readCount == writeCount);
        } finally {
        }
    }
}
