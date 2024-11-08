package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaSpell;
import edu.univalle.lingweb.persistence.MaSpellDAO;
import edu.univalle.lingweb.persistence.MaSyntatic;
import edu.univalle.lingweb.persistence.MaSyntaticDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'ma_syntatic'( Postag spell Checker)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerMaSyntactic extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaSyntactic.class);

    /**
	 * Constructor de la clase
	 */
    public DataManagerMaSyntactic() {
        super();
        DOMConfigurator.configure(DataManagerMaSyntactic.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva unidad sintactica en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntatic
	 *            Unidad sintactica a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, MaSyntatic maSyntatic) {
        MaSyntaticDAO maSyntaticDAO = new MaSyntaticDAO();
        try {
            maSyntatic.setSytanticId(getSequence("sq_ma_syntatic"));
            EntityManagerHelper.beginTransaction();
            maSyntaticDAO.save(maSyntatic);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSyntatic);
            log.info("Palabra creada con �xito: " + maSyntatic.getSecuence());
            Object[] arrayParam = { maSyntatic.getSecuence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unidad sintactica: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una Unidad Sintactica en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntatic
	 *            Unidad sintactica a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaSyntatic maSyntatic) {
        MaSyntaticDAO maSyntaticDAO = new MaSyntaticDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maSyntaticDAO.update(maSyntatic);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSyntatic);
            Object[] args = { maSyntatic.getSecuence() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la unidad sintactica: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una unidad sintactica
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSyntatic
	 *            Unidad sintactica a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaSyntatic maSyntatic) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_MA_SYNTACTIC);
            query.setParameter(1, maSyntatic.getSytanticId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { maSyntatic.getSecuence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la unidad sintactica: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maSyntatic.getSecuence() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una unidad sintactica
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sSecuence
	 *            Secuencia de la unidad sintactica
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sSecuence) {
        List<MaSyntatic> list = new MaSyntaticDAO().findBySecuence(sSecuence);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntactic.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una unidad sintactica por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nSyntacticId
	 *            C�digo de la unidad sintactica
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nSyntacticId) {
        MaSyntatic maSyntatic = new MaSyntaticDAO().findById(nSyntacticId);
        if (maSyntatic == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntactic.search.notFound"));
        } else {
            List<MaSyntatic> list = new ArrayList<MaSyntatic>();
            EntityManagerHelper.refresh(maSyntatic);
            list.add(maSyntatic);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de unidades sintacticas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de unidades sintacticas
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        MaSyntaticDAO maSyntaticDAO = new MaSyntaticDAO();
        List<MaSyntatic> list = maSyntaticDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("syntactic.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(maSyntaticDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public RestServiceResult check(RestServiceResult serviceResult, Long nLanguageId, String sSentence) {
        MaSpell maSpell = null;
        String vectorPalabras[] = sSentence.split(" ");
        String secuenceToCheck = "";
        for (int p = 0; p < vectorPalabras.length; p++) {
            Query queryWord = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_SPELL_WORD, MaSpell.class);
            queryWord.setParameter(1, vectorPalabras[p].toLowerCase());
            queryWord.setHint(QueryHints.REFRESH, HintValues.TRUE);
            if (queryWord.getResultList().size() == 0 || queryWord.getResultList() == null) {
                secuenceToCheck = secuenceToCheck + "-";
            } else {
                maSpell = (MaSpell) queryWord.getSingleResult();
                secuenceToCheck = secuenceToCheck + maSpell.getMaPostag().getTag();
            }
            log.info("partial secuence " + secuenceToCheck);
        }
        String sSecuence = secuenceToCheck;
        List<MaSyntatic> listMaSyntax = null;
        log.info("nLanguageId: " + nLanguageId);
        log.info("Secuencia: " + sSecuence);
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_SYNTAX, MaSyntatic.class);
        query.setParameter(1, nLanguageId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listMaSyntax = query.getResultList();
        int error = 0;
        boolean revisar = true;
        for (int i = 0; i < listMaSyntax.size(); i++) {
            error = computeLevenshteinDistance(sSecuence, listMaSyntax.get(i).getSecuence());
            if (error == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("syntactic.check.notErrorFound"));
                revisar = false;
                break;
            }
        }
        if (revisar) {
            List<MaSyntatic> listSyntaxChoises = new ArrayList<MaSyntatic>();
            for (int i = 0; i < listMaSyntax.size(); i++) {
                error = computeLevenshteinDistance(sSecuence, listMaSyntax.get(i).getSecuence());
                if (error < 8) {
                    listSyntaxChoises.add(listMaSyntax.get(i));
                }
            }
            Object[] arrayParam = { listSyntaxChoises.size() };
            if (listSyntaxChoises.size() == 0) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.check.noChoises"), arrayParam));
            } else {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("syntactic.check.success"), arrayParam));
            }
            serviceResult.setObjResult(listSyntaxChoises);
            serviceResult.setNumResult(listSyntaxChoises.size());
        }
        return serviceResult;
    }

    public static void main(String[] args) {
    }

    private static int minimum(int a, int b, int c) {
        if (a <= b && a <= c) {
            return a;
        }
        if (b <= a && b <= c) {
            return b;
        }
        return c;
    }

    public static int computeLevenshteinDistance(String str1, String str2) {
        return computeLevenshteinDistance(str1.toCharArray(), str2.toCharArray());
    }

    private static int computeLevenshteinDistance(char[] str1, char[] str2) {
        int[][] distance = new int[str1.length + 1][str2.length + 1];
        for (int i = 0; i <= str1.length; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= str2.length; j++) {
            distance[0][j] = j;
        }
        for (int i = 1; i <= str1.length; i++) {
            for (int j = 1; j <= str2.length; j++) {
                distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1, distance[i - 1][j - 1] + ((str1[i - 1] == str2[j - 1]) ? 0 : 1));
            }
        }
        return distance[str1.length][str2.length];
    }
}
