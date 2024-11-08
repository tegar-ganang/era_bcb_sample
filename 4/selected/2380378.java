package com.webmotix.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import javax.jcr.Node;
import javax.jcr.Session;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.webmotix.core.MotixNodeTypes;
import com.webmotix.core.PathCache;
import com.webmotix.core.SystemParameter;
import com.webmotix.core.SystemProperty;
import com.webmotix.dao.LanguageDAO;
import com.webmotix.dao.MetaDataDAO;
import com.webmotix.dao.StaticPageDAO;
import com.webmotix.exception.DAONotFoundException;
import com.webmotix.repository.RepositoryHelper;
import com.webmotix.util.DateUtil;
import com.webmotix.util.MotixURICache;

/**
 * Processa  a fila de gera��o de p�ginas est�ticas
 * 
 * @author wsouza
 * 
 */
public class CacheEventQueue extends Thread {

    /**
	 * Logger.
	 */
    private static final Logger log = LoggerFactory.getLogger(CacheEventQueue.class);

    /**
	 * Tempo m�ximo que a thread ir� dormir: 10 segundos
	 */
    private static int MAX_SLEEP = 10000;

    /**
	 * Tempo m�ximo de execu��o do request
	 */
    private static final int DEFAULT_TIMEOUT = 60000;

    /**
	 * N�mero m�ximo de conex�es (session) abertas com o servidor web.
	 */
    private static final int MAX_TOTAL_CONNECTIONS = 5;

    /**
	 * Quantidade m�ximo de p�ginas que ser�o processadas por vez
	 */
    private static int MAX_PAGES = 7;

    /**
	 * Fila de execu��o do cache
	 */
    private static final HashMap<String, String> QUEUE = new HashMap<String, String>();

    private static final Stack<String[]> QUEUE_STATIC_PAGES = new Stack<String[]>();

    private static CacheEventQueue instance = null;

    private static final CacheQueueInfo queueInfo = new CacheQueueInfo();

    private static int countThread = 0;

    /**
	 * Gerenciador de conex�es.
	 */
    private static MultiThreadedHttpConnectionManager connectionManager = null;

    /**
   	 * Cliente que executa os request.
   	 */
    private static HttpClient client = null;

    /**
   	 * Workspace padr�o usado pelo JCR
   	 */
    private String workspace = SystemProperty.getProperty(SystemProperty.WEBMOTIX_CONNECTION_JCR_WORKSPACE);

    /**
	 * Vari�veis auxiliares 
	 */
    private static final String PROPERTY_LANGUAGE = MotixNodeTypes.NS_PROPERTY + ":language";

    private static final String PROPERTY_STATUS = MotixNodeTypes.NS_PROPERTY + ":status";

    private static Map<String, Locale> LOCALE_CACHE = new HashMap<String, Locale>();

    private final String ROOT_DIRECTORY = PathCache.getRootDirectory().getPath();

    static final SimpleDateFormat df = new SimpleDateFormat(DateUtil.FORMAT_SHORTTIMEPATTERN);

    /**
	 * Retorna uma insta�ncia da fila para o workspace
	 * @param workspace
	 * @return
	 */
    public static synchronized CacheEventQueue getInstance() {
        if (instance == null) {
            instance = new CacheEventQueue();
        }
        return instance;
    }

    /**
	 * Cria instancia
	 * @param workspace
	 */
    private CacheEventQueue() {
        super();
        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.getParams().setConnectionTimeout(DEFAULT_TIMEOUT);
        connectionManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        client = new HttpClient(connectionManager);
    }

    /**
	 * Adiciona o controle de p�ginas estaticas que ser�o processadas posteriormente.
	 * @param uuid
	 * @param path
	 */
    public synchronized void addContent(final String uuid, final String path) {
        QUEUE_STATIC_PAGES.add(new String[] { uuid, path });
        this.notifyAll();
    }

    /**
	 * Adiciona uma url na fila de processamento
	 * @param uri request a ser processado
	 * @param file arquivo que ser� gravado
	 */
    public synchronized void add(final String uri, final String file) {
        QUEUE.put(file, uri);
        this.notifyAll();
    }

    /**
	 * Adiciona uma p�gina est�tica para ser processada em todoas os idiomas.
	 * @param uuid
	 */
    public synchronized void add(final String uuid) {
        try {
            final StaticPageDAO staticPageDAO = StaticPageDAO.findByUUIDCached(uuid, this.workspace);
            final Locale[] locales = LanguageDAO.findAllLocale(this.workspace);
            for (int i = 0; i < locales.length; i++) {
                final Locale locale = locales[i];
                final String file = processFile(staticPageDAO, locale);
                if (!QUEUE.containsKey(file)) {
                    final String[] value = { processURI(staticPageDAO, locale), file };
                    QUEUE.put(file, processURI(staticPageDAO, locale));
                }
            }
            this.notifyAll();
        } catch (final DAONotFoundException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
	 * Adiciona uma lista de p�ginas est�ticas para serem processadas em todoas os idiomas.
	 * @param uuid
	 */
    public synchronized void add(final String[] uuid) {
        for (int j = 0; j < uuid.length; j++) {
            final String key = uuid[j];
            try {
                final StaticPageDAO staticPageDAO = StaticPageDAO.findByUUIDCached(key, this.workspace);
                final Locale[] locales = LanguageDAO.findAllLocale(this.workspace);
                for (int i = 0; i < locales.length; i++) {
                    final Locale locale = locales[i];
                    final String file = processFile(staticPageDAO, locale);
                    if (!QUEUE.containsKey(file)) {
                        final String[] value = { processURI(staticPageDAO, locale), file };
                        QUEUE.put(file, processURI(staticPageDAO, locale));
                    }
                }
            } catch (final DAONotFoundException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        this.notifyAll();
    }

    @Override
    public synchronized void run() {
        try {
            this.wait();
            while (true) {
                MAX_SLEEP = Integer.parseInt(StringUtils.defaultIfEmpty(SystemParameter.getProperty(SystemParameter.WEBMOTIX_WEB_CACHE_MAX_SLEEP), "10")) * 1000;
                MAX_PAGES = Integer.parseInt(StringUtils.defaultIfEmpty(SystemParameter.getProperty(SystemParameter.WEBMOTIX_WEB_CACHE_MAX_PAGES), String.valueOf(MAX_PAGES)));
                Thread.sleep((int) (Math.random() * MAX_SLEEP));
                if (countThread < MAX_PAGES) {
                    prepareQueueStaticPage();
                    for (int i = 0; i < MAX_PAGES && !QUEUE.isEmpty(); i++) {
                        final String file = QUEUE.keySet().iterator().next();
                        final String uri = QUEUE.get(file);
                        final GetThread cache = new GetThread(uri, file);
                        cache.start();
                        QUEUE.remove(file);
                    }
                }
                if (QUEUE.isEmpty()) {
                    this.wait();
                }
            }
        } catch (final Exception e) {
            log.error("Falha ao processar fila de p�ginas em cache.", e);
        }
    }

    /**
	 * cria o arquivo a partir da pagina template.
	 * @param staticPageDAO
	 * @param locale
	 * @return
	 */
    private String processFile(final StaticPageDAO staticPageDAO, final Locale locale) {
        return ROOT_DIRECTORY + File.separator + locale.toString().toLowerCase() + File.separator + StringUtils.replace(staticPageDAO.getUrl(), "/", File.separator);
    }

    /**
	 * Cria uri a partir da pagina template
	 * @param staticPageDAO
	 * @param locale
	 * @return
	 */
    private String processURI(final StaticPageDAO staticPageDAO, final Locale locale) {
        final String context = staticPageDAO.getModuleContext();
        String urlGet = MotixURICache.WEBMOTIX_WEB_CONTEXT;
        if (!staticPageDAO.getTemplate().startsWith("/")) {
            urlGet += "/";
        }
        urlGet += staticPageDAO.getTemplate();
        if (urlGet.indexOf("?") == -1) {
            return urlGet + "?context=" + context + "&locale=" + locale.toString().toLowerCase();
        } else {
            return urlGet + "&context=" + context + "&locale=" + locale.toString().toLowerCase();
        }
    }

    /**
	 * Retorna o locale a partir do idioma. Faz o controle de cache para evitar acesso ao repositorio.
	 * @param session
	 * @param language
	 * @return
	 */
    private Locale getLocale(final Session session, final String language) {
        if (!LOCALE_CACHE.containsKey(language)) {
            try {
                final Locale locale = LanguageDAO.findByUUID(language, session).getLocale();
                LOCALE_CACHE.put(language, locale);
                return locale;
            } catch (final DAONotFoundException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return LOCALE_CACHE.get(language);
    }

    /**
	 * Prepara a fila de processamento de paginas a partir do registro de "p�ginas estaticas"
	 */
    private void prepareQueueStaticPage() {
        if (!QUEUE_STATIC_PAGES.isEmpty()) {
            Session session = null;
            try {
                session = RepositoryHelper.getSession(workspace);
                while (!QUEUE_STATIC_PAGES.isEmpty()) {
                    final String[] item = QUEUE_STATIC_PAGES.pop();
                    final String uuid = item[0];
                    final String path = item[1];
                    final Node content = (Node) session.getItem(path);
                    long status = -1;
                    if (content.hasProperty(PROPERTY_STATUS)) {
                        status = content.getProperty(PROPERTY_STATUS).getLong();
                    }
                    if (status == MetaDataDAO.STATUS_PUBLISHED || status == -1) {
                        final StaticPageDAO staticPageDAO = StaticPageDAO.findByUUIDCached(uuid, this.workspace);
                        final Locale locale = getLocale(session, content.getProperty(PROPERTY_LANGUAGE).getString());
                        final String file = processFile(staticPageDAO, locale);
                        if (!QUEUE.containsKey(file)) {
                            QUEUE.put(file, processURI(staticPageDAO, locale));
                        }
                    }
                }
                QUEUE_STATIC_PAGES.clear();
            } catch (final Exception e) {
                log.error("Falha ao preparar lista de p�ginas est�ticas ( " + QUEUE_STATIC_PAGES + " ): " + e.getMessage(), e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }

    public static void flush(final String uri, final String filePath) {
        long timestamp = System.currentTimeMillis();
        GetMethod method = null;
        try {
            final File tmpFile = new File(filePath + ".mtx");
            if (!tmpFile.exists()) {
                int status = -1;
                final File file = new File(filePath);
                final File folder = file.getParentFile();
                if (!folder.exists()) {
                    if (!folder.mkdirs()) {
                        log.error("N�o conseguiu criar diret�rio: " + folder);
                    }
                }
                method = new GetMethod(uri);
                FileOutputStream out = null;
                InputStream is = null;
                status = client.executeMethod(method);
                if (status == HttpStatus.SC_OK) {
                    try {
                        is = method.getResponseBodyAsStream();
                        out = new FileOutputStream(tmpFile);
                        final byte[] buffer = new byte[4096];
                        for (int n; (n = is.read(buffer)) != -1; ) {
                            out.write(buffer, 0, n);
                        }
                    } finally {
                        IOUtils.closeQuietly(is);
                        IOUtils.closeQuietly(out);
                        if (tmpFile.exists()) {
                            copy(tmpFile, file);
                        }
                    }
                    final Date end = new Date();
                    timestamp = System.currentTimeMillis() - timestamp;
                    queueInfo.add(new String[] { uri, filePath, String.valueOf(timestamp), df.format(end) });
                } else {
                    log.error("CACHE ERROR (" + uri + " -> " + filePath + "): Status " + status + " - " + HttpStatus.getStatusText(status));
                    queueInfo.addError(new String[] { uri, filePath, status + " - " + HttpStatus.getStatusText(status), df.format(new Date()) });
                }
            }
        } catch (final Exception ex) {
            log.error("CACHE ERROR (" + uri + " -> " + filePath + "): " + ex.getMessage());
            queueInfo.addError(new String[] { uri, filePath, ex.getMessage(), df.format(new Date()) });
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
	 * A thread que executa os request e salva em disco.
	 */
    static class GetThread extends Thread {

        private String uri = null;

        private String filePath = null;

        public GetThread(final String uri, final String filePath) {
            this.uri = uri;
            this.filePath = filePath;
            synchronized (this) {
                countThread++;
            }
        }

        /**
		 * Executes the GetMethod and prints some satus information.
		 */
        @Override
        public void run() {
            try {
                flush(this.uri, this.filePath);
            } catch (final Exception ex) {
                log.error(ex.getMessage(), ex);
            } finally {
                synchronized (this) {
                    --countThread;
                }
            }
        }
    }

    /**
	 * C�pia rapida usando NIO
	 * @param src
	 * @param dst
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
    public static void copy(final File src, final File dst) throws IOException, IllegalArgumentException {
        long fileSize = src.length();
        final FileInputStream fis = new FileInputStream(src);
        final FileOutputStream fos = new FileOutputStream(dst);
        final FileChannel in = fis.getChannel(), out = fos.getChannel();
        try {
            long offs = 0, doneCnt = 0;
            final long copyCnt = Math.min(65536, fileSize);
            do {
                doneCnt = in.transferTo(offs, copyCnt, out);
                offs += doneCnt;
                fileSize -= doneCnt;
            } while (fileSize > 0);
        } finally {
            try {
                in.close();
            } catch (final IOException e) {
            }
            try {
                out.close();
            } catch (final IOException e) {
            }
            try {
                fis.close();
            } catch (final IOException e) {
            }
            try {
                fos.close();
            } catch (final IOException e) {
            }
            src.delete();
        }
    }

    /**
	 * A thread que executa os request e salva em disco.
	 */
    public static class CacheQueueInfo {

        private static int total = 0;

        private static int count = 0;

        private static int countError = 0;

        private static Date startTime = new Date();

        /**
		 * limite de registro das ultimas gera��es.
		 */
        private final int MAX_HISTORY = 100;

        private final String[][] STATISTIC = new String[MAX_HISTORY][4];

        private final String[][] ERRORS = new String[MAX_HISTORY][4];

        public void add(final String[] item) {
            if (count >= MAX_HISTORY) {
                count = 0;
            }
            STATISTIC[count++] = item;
            total++;
        }

        public void addError(final String[] item) {
            if (countError >= MAX_HISTORY) {
                countError = 0;
            }
            ERRORS[countError++] = item;
        }

        public int getTotalPages() {
            return total;
        }

        public HashMap<String, String> getQueue() {
            return QUEUE;
        }

        public boolean isRunning() {
            return !QUEUE.isEmpty();
        }

        public String getStartTime() {
            return df.format(startTime);
        }

        public int getCountThreads() {
            return countThread;
        }

        public int getCountPages() {
            return count;
        }

        public int getConnectionsInUse() {
            return connectionManager.getConnectionsInPool();
        }

        public int getConnectionTimeout() {
            return DEFAULT_TIMEOUT;
        }

        public int getMaxTotalConnections() {
            return MAX_TOTAL_CONNECTIONS;
        }

        public int getTimeWait() {
            return MAX_SLEEP;
        }

        public int getMaxPages() {
            return MAX_PAGES;
        }

        public int getPagesPerMinute() {
            try {
                return (int) (total / ((new Date().getTime() - startTime.getTime()) / (1000 * 60)));
            } catch (ArithmeticException ex) {
                return 0;
            }
        }

        public String[][] getStatistics() {
            return STATISTIC;
        }

        public String[][] getErrors() {
            return ERRORS;
        }
    }

    public static CacheQueueInfo getQueueInfo() {
        return queueInfo;
    }
}
