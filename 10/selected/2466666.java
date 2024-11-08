package blomo.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import blomo.synchronization.SynchronizedObject;
import blomo.util.BLoMoUtil;
import blomo.util.HibernateUtil;
import blomo.util.LRUCache;
import blomo.util.LanguageDefinition;

/**
 * @author Malte Schulze
 * 
 * Implementation for a language cache and access
 * to translations via DB calls.<br>
 * This implementation uses the currently 
 * bound Hibernate session with SQLQueries and a manual
 * flush mode to allow a rollback.
 */
public class LanguageCacheImpl extends SynchronizedObject implements LanguageCache {

    protected Map<CacheKey, CacheEntry> cache;

    protected String[] allLanguages;

    protected int maxSize;

    protected String translationTable;

    protected String translationTableSeq;

    protected String select;

    protected String defaultLanguageId;

    /**
	 * 
	 */
    public LanguageCacheImpl() {
        List<LanguageDefinition> languages = BLoMoUtil.getAllLanguages();
        allLanguages = new String[languages.size()];
        int i = 0;
        for (LanguageDefinition lang : languages) {
            allLanguages[i++] = lang.getId();
        }
        maxSize = BLoMoUtil.getLanguageCacheSize();
        translationTable = BLoMoUtil.getTranslationTable();
        if (translationTable == null) translationTable = "translationTable";
        translationTableSeq = BLoMoUtil.getTranslationTableSeq();
        if (translationTable == null) translationTableSeq = "translation_text_id_seq";
        select = "select language_id, text from " + translationTable + " where text_id = ? AND (language_id = ? OR language_id = ?)";
        defaultLanguageId = BLoMoUtil.getDefaultLanguage();
        cache = Collections.synchronizedMap(new LRUCache<CacheKey, CacheEntry>(maxSize));
    }

    @Override
    public void drop(Long textId) {
        for (String languageId : allLanguages) {
            CacheKey key = new CacheKey(textId, languageId);
            cache.remove(key);
        }
    }

    @Override
    public CacheEntry get(Long textId, String languageId) {
        return cache.get(new CacheKey(textId, languageId));
    }

    @Override
    public void store(Long textId, String languageId, String text) {
        CacheKey key = new CacheKey(textId, languageId);
        cache.put(key, new CacheEntry(text));
    }

    @Override
    public void update(Long textId, String languageId, String text) {
        updateLocal(textId, languageId, text);
        sendUpdate(textId, languageId, text);
    }

    private boolean updateLocal(Long textId, String languageId, String text) {
        CacheKey key = new CacheKey(textId, languageId);
        if (cache.containsKey(key)) {
            cache.put(key, new CacheEntry(text));
            return true;
        } else store(textId, languageId, text);
        return false;
    }

    private void sendUpdate(Long textId, String languageId, String text) {
        if (text == null) text = "@" + text + "@";
        super.send(textId + "," + languageId + "," + text);
    }

    @Override
    public String getNamespace() {
        return "languageCache";
    }

    @Override
    public void receive(String cmd) {
        if (cmd == null) return;
        String[] parts = cmd.split(",");
        if (parts.length < 3) return;
        String text;
        if (parts.length == 3 && "@null@".equals(parts[2])) text = null; else {
            text = "";
            for (int i = 2; i < parts.length; i++) text += parts[i];
        }
        updateLocal(Long.parseLong(parts[0]), parts[1], text);
    }

    @Override
    public boolean deleteTextId(Long textId) {
        if (textId != null) {
            Session session = HibernateUtil.getDefaultSessionFactory().getCurrentSession();
            boolean commit = false;
            Transaction t = session.getTransaction();
            if (!t.isActive()) {
                commit = true;
                t.begin();
            }
            SQLQuery q = session.createSQLQuery("delete from " + translationTable + " where text_id = " + textId);
            q.setFlushMode(FlushMode.MANUAL);
            int result;
            try {
                result = q.executeUpdate();
                if (commit) t.commit();
            } catch (HibernateException e) {
                e.printStackTrace();
                if (commit) t.rollback();
                return false;
            }
            if (result > 0) {
                drop(textId);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String resolveLanguageText(Long textId, String languageId) {
        if (textId == null) return null;
        if (languageId == null) languageId = defaultLanguageId;
        CacheEntry entry = get(textId, languageId);
        if (entry != null) return entry.text;
        Session session = HibernateUtil.getDefaultSessionFactory().getCurrentSession();
        boolean commit = false;
        Transaction t = session.getTransaction();
        if (!t.isActive()) {
            commit = true;
            t.begin();
        }
        SQLQuery q = session.createSQLQuery(select);
        q.addScalar("language_id", Hibernate.STRING);
        q.addScalar("text", Hibernate.STRING);
        q.setFlushMode(FlushMode.MANUAL);
        q.setLong(0, textId);
        q.setString(1, languageId);
        q.setString(2, defaultLanguageId);
        String text;
        try {
            List<Object[]> result = (List<Object[]>) q.list();
            if (result.size() == 0) text = null; else if (result.size() == 1) text = (String) result.get(0)[1]; else {
                if (defaultLanguageId.equals(result.get(0)[0])) text = (String) result.get(1)[1]; else text = (String) result.get(0)[1];
            }
            if (commit) t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (commit) t.rollback();
            return null;
        }
        store(textId, languageId, text);
        return text;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Long resolveTextId(Long textId, String text, String languageId) {
        if (languageId == null) languageId = defaultLanguageId;
        if (text != null) text = "'" + text + "'"; else text = "null";
        Session session = HibernateUtil.getDefaultSessionFactory().getCurrentSession();
        boolean commit = false;
        Transaction t = session.getTransaction();
        if (!t.isActive()) {
            commit = true;
            t.begin();
        }
        try {
            if (textId != null) {
                SQLQuery q = session.createSQLQuery("UPDATE " + translationTable + " SET \"text\"=" + text + " WHERE text_id = " + textId + " AND language_id='" + languageId + "';");
                q.setFlushMode(FlushMode.MANUAL);
                int i = q.executeUpdate();
                if (i == 0) {
                    q = session.createSQLQuery("INSERT INTO translation(text_id, language_id, \"text\")VALUES (" + textId + ", '" + languageId + "', " + text + ")");
                    q.setFlushMode(FlushMode.MANUAL);
                    if (q.executeUpdate() > 0) store(textId, languageId, text);
                } else {
                    update(textId, languageId, text);
                }
                if (commit) t.commit();
                return textId;
            } else {
                SQLQuery q = session.createSQLQuery("SELECT nextval('" + translationTableSeq + "') as r");
                q.setFlushMode(FlushMode.MANUAL);
                q.addScalar("r", Hibernate.LONG);
                List<Object> l = q.list();
                textId = (Long) l.get(0);
                q = session.createSQLQuery("INSERT INTO translation(text_id, language_id, \"text\")VALUES (" + textId + ", '" + languageId + "', " + text + ")");
                q.setFlushMode(FlushMode.MANUAL);
                int i = q.executeUpdate();
                if (commit) t.commit();
                if (i > 0) {
                    store(textId, languageId, text);
                    return textId;
                } else return null;
            }
        } catch (HibernateException e) {
            e.printStackTrace();
            if (commit) t.rollback();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> resolveLanguageTextInBulk(List<Long> textId, String languageId) {
        if (textId == null) return null;
        List<String> results = new ArrayList<String>(textId.size());
        if (languageId == null) languageId = defaultLanguageId;
        Session session = HibernateUtil.getDefaultSessionFactory().getCurrentSession();
        boolean commit = false;
        Transaction t = session.getTransaction();
        if (!t.isActive()) {
            commit = true;
            t.begin();
        }
        try {
            String inList = null;
            for (Long tid : textId) {
                if (inList == null) inList = tid.toString(); else inList += "," + tid.toString();
            }
            SQLQuery q = session.createSQLQuery("SELECT text_id, language_id, text FROM " + translationTable + " WHERE text_id IN (" + inList + ") AND (language_id = '" + languageId + "' OR language_id = '" + defaultLanguageId + "');");
            Map<Long, String> mapping = new HashMap<Long, String>();
            q.setFlushMode(FlushMode.MANUAL);
            q.addScalar("text_id", Hibernate.LONG);
            q.addScalar("language_id", Hibernate.STRING);
            q.addScalar("text", Hibernate.STRING);
            List<Object[]> rs = q.list();
            for (Object[] row : rs) {
                if (languageId.equals(row[1])) mapping.put((Long) row[0], (String) row[2]); else if (!mapping.containsKey(row[0])) mapping.put((Long) row[0], (String) row[2]);
            }
            for (Long tid : textId) {
                results.add(mapping.get(tid));
            }
            if (commit) t.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (commit) t.rollback();
            return null;
        }
        return results;
    }
}
