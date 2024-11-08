package naru.aweb.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import naru.aweb.util.ServerParser;

/**
 * pac�����return�l�́A�ȉ��R�p�^�[�� 1)DIRECT 2)PROXY xxx.xxx.xxx.xxx:pppp 3)PROXY
 * xxx.xxx.xxx.xxx:pppp; PROXY yyy.yyy.yyy.yyy:qqqq; DIRECT
 * 
 * 3)�͐擪�݂̂�ΏۂƂ���
 */
public class ProxyFinder {

    private static Logger logger = Logger.getLogger(ProxyFinder.class);

    private static VelocityEngine velocityEngine = null;

    private static final String NEXT_FIND_PROXY_FOR_URL_FUNC_NAME = "NextFindProxyForURL";

    private static final ServerParser DIRECT = new ServerParser(null, 0);

    private static final Pattern FIND_PROXY_PATTERN = Pattern.compile("PROXY (([^:]*):(\\d*))", Pattern.CASE_INSENSITIVE);

    private boolean isUseProxy;

    private ServerParser httpProxyServer = null;

    private ServerParser secureProxyServer = null;

    private ServerParser ftpProxyServer = null;

    private ServerParser sockesServer = null;

    private Set<String> exceptDomians;

    private URL pacUrl;

    private String pac;

    private String nextPac;

    private Invocable pacScriptInvoker = null;

    private Set<String> httpPhantomDomians;

    private Set<String> securePhantomDomians;

    private Map<String, ServerParser> proxyServerCash;

    private Map<String, ServerParser> urlProxyCash;

    private Map<String, String> phantomPacCash;

    private String selfDomain;

    private int proxyPort;

    public static String getNextFindProxyForUrlFuncName() {
        return NEXT_FIND_PROXY_FOR_URL_FUNC_NAME;
    }

    private static VelocityEngine getVelocityEngine() {
        if (velocityEngine != null) {
            return velocityEngine;
        }
        velocityEngine = new VelocityEngine();
        velocityEngine.addProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
        velocityEngine.setProperty("runtime.log.logsystem.log4j.category", "velocity");
        velocityEngine.setProperty("resource.manager.logwhenfound", "false");
        try {
            velocityEngine.init();
        } catch (Exception e) {
            throw new RuntimeException("fail to velocityEngine.ini()", e);
        }
        return velocityEngine;
    }

    public static String contents(URL url) throws IOException {
        InputStream is = url.openStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (true) {
            int len = is.read(buf);
            if (len < 0) {
                break;
            }
            baos.write(buf, 0, len);
        }
        is.close();
        String contents = new String(baos.toByteArray(), "iso8859_1");
        baos.close();
        return contents;
    }

    private String merge(String template, String localHost) {
        VelocityContext veloContext = new VelocityContext();
        veloContext.put("proxyFinder", this);
        veloContext.put("localHost", localHost);
        veloContext.put("localServer", selfDomain);
        Writer out = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            out = new OutputStreamWriter(baos, "utf-8");
            VelocityEngine ve = getVelocityEngine();
            ve.mergeTemplate(template, "utf-8", veloContext, out);
            out.close();
            String result = new String(baos.toByteArray(), "iso8859_1");
            logger.debug("----marge result start:template:" + template + ":localHost:" + localHost);
            logger.debug(result);
            logger.debug("----marge result end----");
            return result;
        } catch (ResourceNotFoundException e) {
            logger.error("Velocity.mergeTemplate ResourceNotFoundException." + template, e);
        } catch (ParseErrorException e) {
            logger.error("Velocity.mergeTemplate ParseErrorException." + template, e);
        } catch (MethodInvocationException e) {
            logger.error("Velocity.mergeTemplate MethodInvocationException." + template, e);
        } catch (Exception e) {
            logger.error("Velocity.mergeTemplate Exception." + template, e);
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    public String getNextPac() {
        return nextPac;
    }

    public String getHttpProxy() {
        if (httpProxyServer != null) {
            return httpProxyServer.toString();
        }
        return null;
    }

    public String getSecureProxy() {
        if (secureProxyServer != null) {
            return secureProxyServer.toString();
        }
        return null;
    }

    public Set<String> getExceptDomians() {
        return exceptDomians;
    }

    public Set<String> getHttpPhantomDomians() {
        return httpPhantomDomians;
    }

    public Set<String> getSecurePhantomDomians() {
        return securePhantomDomians;
    }

    private boolean loadPac(String pac) {
        this.pac = pac;
        try {
            ScriptEngineManager factory = new ScriptEngineManager(null);
            ScriptEngine pacScriptEngine = factory.getEngineByName("JavaScript");
            if (pacScriptEngine == null) {
                logger.error("fail to getEngineByName(JavaScript) pacScriptEngine is null");
                logger.error("SecurityManager:" + System.getSecurityManager());
                for (ScriptEngineFactory f : factory.getEngineFactories()) {
                    logger.error(f.getEngineName() + ":" + f.getEngineVersion() + ":" + f.getLanguageName());
                }
                ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
                logger.error("ctxtLoader:" + ctxtLoader);
                Enumeration configs = ctxtLoader.getResources("META-INF/services/" + ScriptEngineFactory.class);
                logger.error("ContextClassLoader configs:" + configs);
                while (configs.hasMoreElements()) {
                    Object o = configs.nextElement();
                    logger.error("ContextClassLoader loaderScriptEngineFactory resouce:" + o);
                }
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                configs = loader.getResources("META-INF/services/" + ScriptEngineFactory.class);
                logger.error("SystemClassLoader:" + loader);
                logger.error("SystemClassLoader configs:" + configs);
                while (configs.hasMoreElements()) {
                    Object o = configs.nextElement();
                    logger.error("SystemClassLoader loaderScriptEngineFactory resouce:" + o);
                }
                return false;
            } else {
                logger.info("pacScriptEngine:" + pacScriptEngine.getClass().getName());
            }
            InputStream is = ProxyFinder.class.getResourceAsStream("pacscript.js");
            Reader reader = new InputStreamReader(is);
            pacScriptEngine.eval(reader);
            reader.close();
            pacScriptEngine.eval(pac);
            pacScriptInvoker = (Invocable) pacScriptEngine;
            return true;
        } catch (ScriptException e) {
            logger.error("loadPac ScriptException", e);
            return false;
        } catch (IOException e) {
            logger.error("loadPac IOException", e);
            return false;
        }
    }

    public static ProxyFinder create(String pac, String httpProxyServer, String secureProxyServer, String exceptDomians, String selfDomain, int proxyPort) throws IOException {
        Set<String> exceptDomiansSet = new HashSet<String>();
        if (exceptDomians != null) {
            String[] exceptDomiansArray = exceptDomians.split(";");
            for (String exceptDomain : exceptDomiansArray) {
                exceptDomiansSet.add(exceptDomain);
            }
        }
        URL pacUrl = null;
        if (pac != null) {
            pacUrl = new URL(pac);
        }
        ProxyFinder finder = new ProxyFinder();
        if (finder.init(pacUrl, ServerParser.parse(httpProxyServer), ServerParser.parse(secureProxyServer), exceptDomiansSet, selfDomain, proxyPort)) {
            return finder;
        } else {
            finder.term();
            return null;
        }
    }

    public boolean updatePac(File phantomHome, Set<String> httpPhantomDomians, Set<String> securePhantomDomians) {
        phantomPacCash.clear();
        this.httpPhantomDomians = httpPhantomDomians;
        this.securePhantomDomians = securePhantomDomians;
        String localPac = getProxyPac(selfDomain + ":" + proxyPort);
        OutputStream localPacOs = null;
        try {
            localPacOs = new FileOutputStream(new File(phantomHome, "proxy.pac"));
            localPacOs.write(localPac.getBytes("utf-8"));
            return true;
        } catch (IOException e) {
            logger.error("fail to cleate local pac", e);
            return false;
        } finally {
            if (localPacOs != null) {
                try {
                    localPacOs.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public void term() {
        if (httpProxyServer != null) {
            httpProxyServer.unref();
        }
        if (secureProxyServer != null) {
            secureProxyServer.unref();
        }
        if (ftpProxyServer != null) {
            ftpProxyServer.unref();
        }
        if (sockesServer != null) {
            sockesServer.unref();
        }
    }

    /**
	 * 
	 * @param pacUrl
	 *            pac�t�@�C���ւ�url,(httpProxyServer,secureProxyServer,exceptDomians�͖��������)
	 * @param httpProxyServer
	 *            httpProxy�T�[�o(pacUrl��null�̏ꍇ�L��)
	 * @param secureProxyServer
	 *            httpsProxy�T�[�o(pacUrl��null�̏ꍇ�L��)
	 * @param exceptDomians
	 *            proxy���g�p���Ȃ��h���C��(pacUrl��null�̏ꍇ�L��)
	 * @param httpPhantomDomians
	 *            http phatom�Ƃ���proxy����h���C��
	 * @param securePhantomDomians
	 *            https phantom�Ƃ���proxy����h���C��
	 * @return
	 * @throws IOException
	 */
    public boolean init(URL pacUrl, ServerParser httpProxyServer, ServerParser secureProxyServer, Set<String> exceptDomians, String selfDomain, int proxyPort) throws IOException {
        this.pacUrl = pacUrl;
        this.exceptDomians = exceptDomians;
        this.httpProxyServer = httpProxyServer;
        this.secureProxyServer = secureProxyServer;
        this.isUseProxy = true;
        this.pacScriptInvoker = null;
        this.proxyServerCash = new HashMap<String, ServerParser>();
        this.urlProxyCash = new HashMap<String, ServerParser>();
        this.phantomPacCash = new HashMap<String, String>();
        this.selfDomain = selfDomain;
        this.proxyPort = proxyPort;
        if (pacUrl != null) {
            if (loadPac(contents(pacUrl)) == false) {
                return false;
            }
            this.nextPac = pac.replaceAll("FindProxyForURL", NEXT_FIND_PROXY_FOR_URL_FUNC_NAME);
            if (this.httpProxyServer != null) {
                this.httpProxyServer.unref();
                this.httpProxyServer = null;
            }
            if (this.secureProxyServer != null) {
                this.secureProxyServer.unref();
                this.secureProxyServer = null;
            }
            this.exceptDomians = null;
            return true;
        }
        if (httpProxyServer != null) {
            assert (secureProxyServer != null);
            if (exceptDomians != null) {
                if (loadPac(merge("self.pac", null)) == false) {
                    return false;
                }
            }
        } else {
            assert (secureProxyServer == null);
            this.isUseProxy = false;
        }
        return true;
    }

    public String getProxyPac(String localHost) {
        String pac = phantomPacCash.get(localHost);
        if (pac == null) {
            pac = merge("proxy.pac", localHost);
            phantomPacCash.put(localHost, pac);
        }
        return pac;
    }

    public ServerParser findProxyServer(boolean isSsl, String host) {
        if (!isUseProxy) {
            return null;
        }
        if (isSsl) {
            if (secureProxyServer != null && exceptDomians == null) {
                return secureProxyServer;
            }
            return findProxyServer("https://" + host, host);
        } else {
            if (httpProxyServer != null && exceptDomians == null) {
                return httpProxyServer;
            }
            return findProxyServer("http://" + host, host);
        }
    }

    public ServerParser findProxyServer(String url, String host) {
        if (!isUseProxy) {
            return null;
        }
        if (secureProxyServer != null && exceptDomians == null && url.startsWith("https://")) {
            return secureProxyServer;
        }
        if (httpProxyServer != null && exceptDomians == null && url.startsWith("http://")) {
            return httpProxyServer;
        }
        ServerParser result = urlProxyCash.get(url);
        if (result != null) {
            if (result == DIRECT) {
                return null;
            }
            return result;
        }
        try {
            String pacResult = (String) pacScriptInvoker.invokeFunction("FindProxyForURL", url, host);
            Matcher matcher;
            synchronized (FIND_PROXY_PATTERN) {
                matcher = FIND_PROXY_PATTERN.matcher(pacResult);
            }
            if (!matcher.find()) {
                urlProxyCash.put(url, DIRECT);
                return null;
            }
            String pacHostPort = matcher.group(1);
            result = proxyServerCash.get(pacHostPort);
            if (result == null) {
                String pacHost = matcher.group(2);
                String pacPort = matcher.group(3);
                result = ServerParser.parse(new ServerParser(), pacHost, Integer.parseInt(pacPort));
                proxyServerCash.put(pacHostPort, result);
            }
            urlProxyCash.put(url, result);
            return result;
        } catch (ScriptException e) {
            logger.error("findProxyServer error.", e);
        } catch (NoSuchMethodException e) {
            logger.error("findProxyServer error.", e);
        }
        return null;
    }
}
