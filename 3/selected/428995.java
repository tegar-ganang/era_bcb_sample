package net.sourceforge.filebot.web;

import static java.lang.Math.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.filebot.web.OpenSubtitlesHasher.*;
import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import redstone.xmlrpc.XmlRpcException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.OpenSubtitlesXmlRpc.Query;
import net.sourceforge.tuned.Timer;

/**
 * SubtitleClient for OpenSubtitles.
 */
public class OpenSubtitlesClient implements SubtitleProvider, VideoHashSubtitleService, MovieIdentificationService {

    private final OpenSubtitlesXmlRpc xmlrpc;

    public OpenSubtitlesClient(String useragent) {
        this.xmlrpc = new OpenSubtitlesXmlRpc(useragent);
    }

    @Override
    public String getName() {
        return "OpenSubtitles";
    }

    @Override
    public URI getLink() {
        return URI.create("http://www.opensubtitles.org");
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.opensubtitles");
    }

    @Override
    public List<SearchResult> search(String query) throws Exception {
        login();
        try {
            List<Movie> resultSet = xmlrpc.searchMoviesOnIMDB(query);
            return asList(resultSet.toArray(new SearchResult[0]));
        } catch (ClassCastException e) {
            throw new XmlRpcException("Illegal XMLRPC response on searchMoviesOnIMDB");
        }
    }

    @Override
    public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws Exception {
        int imdbid = ((Movie) searchResult).getImdbId();
        String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
        login();
        SubtitleDescriptor[] subtitles = xmlrpc.searchSubtitles(imdbid, languageFilter).toArray(new SubtitleDescriptor[0]);
        return asList(subtitles);
    }

    public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] files, String languageName) throws Exception {
        String[] languageFilter = languageName != null ? new String[] { getSubLanguageID(languageName) } : new String[0];
        Map<String, File> hashMap = new HashMap<String, File>(files.length);
        Map<File, List<SubtitleDescriptor>> resultMap = new HashMap<File, List<SubtitleDescriptor>>(files.length);
        List<Query> queryList = new ArrayList<Query>(files.length);
        for (File file : files) {
            if (file.length() > HASH_CHUNK_SIZE) {
                String movieHash = computeHash(file);
                queryList.add(Query.forHash(movieHash, file.length(), languageFilter));
                hashMap.put(movieHash, file);
            }
            resultMap.put(file, new LinkedList<SubtitleDescriptor>());
        }
        if (queryList.size() > 0) {
            login();
            int batchSize = 50;
            for (int bn = 0; bn < ceil((float) queryList.size() / batchSize); bn++) {
                List<Query> batch = queryList.subList(bn * batchSize, min((bn * batchSize) + batchSize, queryList.size()));
                for (OpenSubtitlesSubtitleDescriptor subtitle : xmlrpc.searchSubtitles(batch)) {
                    File file = hashMap.get(subtitle.getMovieHash());
                    resultMap.get(file).add(subtitle);
                }
            }
        }
        return resultMap;
    }

    @Override
    public boolean publishSubtitle(int imdbid, String languageName, File videoFile, File subtitleFile) throws Exception {
        return false;
    }

    /**
	 * Calculate MD5 hash.
	 */
    private String md5(byte[] data) {
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");
            hash.update(data);
            return String.format("%032x", new BigInteger(1, hash.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Movie> searchMovie(String query, Locale locale) throws Exception {
        login();
        return xmlrpc.searchMoviesOnIMDB(query);
    }

    @Override
    public Movie getMovieDescriptor(int imdbid, Locale locale) throws Exception {
        login();
        return xmlrpc.getIMDBMovieDetails(imdbid);
    }

    public Movie getMovieDescriptor(File movieFile, Locale locale) throws Exception {
        return getMovieDescriptors(singleton(movieFile), locale).get(movieFile);
    }

    @Override
    public Map<File, Movie> getMovieDescriptors(Collection<File> movieFiles, Locale locale) throws Exception {
        Map<File, Movie> result = new HashMap<File, Movie>();
        Map<String, File> hashMap = new HashMap<String, File>(movieFiles.size());
        for (File file : movieFiles) {
            if (file.length() > HASH_CHUNK_SIZE) {
                hashMap.put(computeHash(file), file);
            }
        }
        if (hashMap.size() > 0) {
            login();
            List<String> hashes = new ArrayList<String>(hashMap.keySet());
            int batchSize = 50;
            for (int bn = 0; bn < ceil((float) hashes.size() / batchSize); bn++) {
                List<String> batch = hashes.subList(bn * batchSize, min((bn * batchSize) + batchSize, hashes.size()));
                for (Entry<String, Movie> entry : xmlrpc.checkMovieHash(batch).entrySet()) {
                    result.put(hashMap.get(entry.getKey()), entry.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
        Movie movie = (Movie) searchResult;
        String sublanguageid = "all";
        if (languageName != null) {
            try {
                sublanguageid = getSubLanguageID(languageName);
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
            }
        }
        return URI.create(String.format("http://www.opensubtitles.org/en/search/imdbid-%d/sublanguageid-%s", movie.getImdbId(), sublanguageid));
    }

    public Locale detectLanguage(byte[] data) throws Exception {
        login();
        List<String> languages = xmlrpc.detectLanguage(data);
        return languages.size() > 0 ? new Locale(languages.get(0)) : null;
    }

    protected synchronized void login() throws Exception {
        if (!xmlrpc.isLoggedOn()) {
            xmlrpc.loginAnonymous();
        }
        logoutTimer.set(10, TimeUnit.MINUTES, true);
    }

    protected synchronized void logout() {
        if (xmlrpc.isLoggedOn()) {
            try {
                xmlrpc.logout();
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Logout failed", e);
            }
        }
        logoutTimer.cancel();
    }

    protected final Timer logoutTimer = new Timer() {

        @Override
        public void run() {
            logout();
        }
    };

    /**
	 * SubLanguageID by English language name
	 */
    @SuppressWarnings("unchecked")
    protected synchronized Map<String, String> getSubLanguageMap() throws Exception {
        Cache cache = CacheManager.getInstance().getCache("web-persistent-datasource");
        String cacheKey = getClass().getName() + ".subLanguageMap";
        Element element = cache.get(cacheKey);
        Map<String, String> subLanguageMap;
        if (element == null) {
            subLanguageMap = new HashMap<String, String>();
            for (Entry<String, String> entry : xmlrpc.getSubLanguages().entrySet()) {
                subLanguageMap.put(entry.getValue().toLowerCase(), entry.getKey().toLowerCase());
            }
            subLanguageMap.put("brazilian", "pob");
            cache.put(new Element(cacheKey, subLanguageMap));
        } else {
            subLanguageMap = (Map<String, String>) element.getValue();
        }
        return subLanguageMap;
    }

    protected String getSubLanguageID(String languageName) throws Exception {
        Map<String, String> subLanguageMap = getSubLanguageMap();
        String key = languageName.toLowerCase();
        if (!subLanguageMap.containsKey(key)) {
            throw new IllegalArgumentException(String.format("SubLanguageID for '%s' not found", key));
        }
        return subLanguageMap.get(key);
    }

    protected String getLanguageName(String subLanguageID) throws Exception {
        for (Entry<String, String> it : getSubLanguageMap().entrySet()) {
            if (it.getValue().equals(subLanguageID.toLowerCase())) return it.getKey();
        }
        return null;
    }
}
