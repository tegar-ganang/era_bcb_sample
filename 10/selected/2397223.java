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
import edu.univalle.lingweb.persistence.CoLanguage;
import edu.univalle.lingweb.persistence.CoLanguageDAO;
import edu.univalle.lingweb.persistence.CoSingleText;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaPostag;
import edu.univalle.lingweb.persistence.MaPostagDAO;
import edu.univalle.lingweb.persistence.MaSpell;
import edu.univalle.lingweb.persistence.MaSpellDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'ma_Spell'( Postag spell Checker)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerMaSpell extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaSpell.class);

    /**
	 * Constructor de la clase
	 */
    public DataManagerMaSpell() {
        super();
        DOMConfigurator.configure(DataManagerMaSpell.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva palabra (word) en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpell
	 *            palabra a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, MaSpell maSpell) {
        MaSpellDAO maSpellDAO = new MaSpellDAO();
        try {
            maSpell.setWordId(getSequence("sq_ma_spell"));
            EntityManagerHelper.beginTransaction();
            maSpellDAO.save(maSpell);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSpell);
            log.info("Palabra creada con �xito: " + maSpell.getWord());
            Object[] arrayParam = { maSpell.getWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la palabra: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una Palabra (Word) en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpell
	 *            Palabra a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, MaSpell maSpell) {
        MaSpellDAO maSpellDAO = new MaSpellDAO();
        try {
            EntityManagerHelper.beginTransaction();
            maSpellDAO.update(maSpell);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSpell);
            Object[] args = { maSpell.getWord() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la palabra: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una palabra
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maSpell
	 *            Palabra a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, MaSpell maSpell) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_MA_SPELL);
            query.setParameter(1, maSpell.getWordId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { maSpell.getWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la palbra: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maSpell.getWord() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una Palabra
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param sWord
	 *            Word del Spell
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sWord) {
        List<MaSpell> list = new MaSpellDAO().findByWord(sWord);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spell.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Lista una Palabra por el idioma
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nLanguageId
	 *            Id del Idoma de la palabra
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult check(RestServiceResult serviceResult, Long nLanguageId, String sWord) {
        List<MaSpell> listMaSpell = null;
        log.info("nLanguageId: " + nLanguageId);
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_SPELL, MaSpell.class);
        query.setParameter(1, nLanguageId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listMaSpell = query.getResultList();
        int error = 0;
        boolean revisar = true;
        String cleanWord = delDot(sWord.toLowerCase());
        for (int i = 0; i < listMaSpell.size(); i++) {
            error = computeLevenshteinDistance(cleanWord, listMaSpell.get(i).getWord().toLowerCase());
            if (error == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("spell.check.notErrorFound"));
                revisar = false;
                break;
            }
        }
        if (revisar) {
            List<MaSpell> listSpellChoises = new ArrayList<MaSpell>();
            for (int i = 0; i < listMaSpell.size(); i++) {
                error = computeLevenshteinDistance(cleanWord, listMaSpell.get(i).getWord().toLowerCase());
                if (error < 3) {
                    listSpellChoises.add(listMaSpell.get(i));
                }
            }
            Object[] arrayParam = { listSpellChoises.size() };
            if (listSpellChoises.size() == 0) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.check.noChoises"), arrayParam));
            } else {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.check.success"), arrayParam));
            }
            serviceResult.setObjResult(listSpellChoises);
            serviceResult.setNumResult(listSpellChoises.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un Word por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nWordId
	 *            C�digo del Word
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nWordId) {
        MaSpell maSpell = new MaSpellDAO().findById(nWordId);
        if (maSpell == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spell.search.notFound"));
        } else {
            List<MaSpell> list = new ArrayList<MaSpell>();
            EntityManagerHelper.refresh(maSpell);
            list.add(maSpell);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Palabras
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Palabras
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        MaSpellDAO maSpellDAO = new MaSpellDAO();
        List<MaSpell> list = maSpellDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("spell.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("spell.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(maSpellDAO.findAll().size()); else serviceResult.setNumResult(list.size());
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

    public static String delDot(String str) {
        str = str.replace(".", "");
        str = str.replace(",", "");
        str = str.replace(";", "");
        str = str.replace(":", "");
        str = str.replace("(", "");
        str = str.replace(")", "");
        str = str.replace("?", "");
        str = str.replace("�", "");
        str = str.replace("!", "");
        str = str.replace("�", "");
        str = str.replace("=", "");
        str = str.replace("[", "");
        str = str.replace("]", "");
        str = str.replace("{", "");
        str = str.replace("}", "");
        String cleanWord = str;
        log.info("palabra sin punto" + cleanWord);
        return cleanWord;
    }
}
