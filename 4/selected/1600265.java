package br.nic.connector.linkscompleto;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import br.nic.connector.general.SimpleLog;
import br.nic.connector.linkscompleto.datastructures.LinkData;

/**
 * é uma classe Singleton. Seu principal atributo é o mapa Map<String,
 * LinksThread> mapSites, um mapa que associa a cada chave (keySite → host) uma
 * instância da classe LinksThread(ver descrição abaixo). Além disso esta classe
 * possui algumas constantes como runningThreadsLimit que define o máximo de
 * Threads realizando análise de links rodando e parallelThreadsLimit que define
 * o máximo de Threads realizando o teste num mesmo host. Além disso possui
 * vários contadores que ajudam a verificar se houve algum erro ao fim da
 * análise.
 * 
 * @author Marcio, Heitor
 */
public class MapSites {

    private static final int MAX_SITE_CONNECTIONS = 5;

    private static MapSites singleton;

    private Map<String, PriorityQueue<LinksThread>> mapSites;

    private Set<LinksThread> threadsSet;

    private Collection<String> excludedSites = new HashSet<String>();

    private int queueLinksLimit = 100;

    private int runningThreadsLimit = 300;

    private int activeLinksLimit = 10000;

    private int activeLinks = 0;

    private int totalLinks = 0;

    private int linksInvalidos = 0;

    private int totalREQ = 0;

    private int dbLinksCount = 0;

    private int dbDomCount = 0;

    private int dbExtCount = 0;

    private int mapDomCount = 0;

    private int mapExtCount = 0;

    private boolean excludeNewSites = true;

    private MapSites() {
        mapSites = Collections.synchronizedMap(new HashMap<String, PriorityQueue<LinksThread>>());
        threadsSet = Collections.synchronizedSet(new HashSet<LinksThread>());
        excludedSites = Collections.synchronizedSet(new HashSet<String>());
        addBlackList();
    }

    private void addBlackList() {
        excludedSites.add("host.ss1.com.br");
        excludedSites.add("ritter.mat.br");
    }

    /**
	 * Retorna instância estática da classe.
	 */
    public static synchronized MapSites getInstance() {
        if (singleton == null) {
            singleton = new MapSites();
        }
        return singleton;
    }

    /**
	 * Obtém a melhor thread para se colocar um novo link.
	 * Caso todas as threads já estejam no limite retorna null para que uma nova thread seja criada. 
	 */
    public synchronized LinksThread getThread(String keySites) {
        PriorityQueue<LinksThread> q = mapSites.get(keySites);
        if (q == null || q.isEmpty()) {
            return null;
        } else if (q.size() >= MAX_SITE_CONNECTIONS) {
            return q.poll();
        } else if (q.peek().getTamanhoFila() >= queueLinksLimit) {
            return null;
        } else return q.poll();
    }

    /**
	 * Adds a new thread to the sites map
	 * @param keySites The name of the site that the thread is accessing
	 * @param thread The thread that is going to be inserted.
	 */
    public synchronized void setMap(String keySites, LinksThread thread) {
        PriorityQueue<LinksThread> q = mapSites.get(keySites);
        if (q == null) {
            q = new PriorityQueue<LinksThread>(MAX_SITE_CONNECTIONS, new SiteSizeComparator());
            this.mapSites.put(keySites, q);
        }
        q.add(thread);
    }

    /**
	 * removes a thread from the sites map structure.
	 * @param thread The thread to be removed.
	 */
    public synchronized void removeThread(LinksThread thread) {
        PriorityQueue<LinksThread> q = mapSites.get(thread.getKeySite());
        if (q != null) {
            q.remove(thread);
            if (q.isEmpty()) {
                mapSites.remove(thread.getKeySite());
            }
        }
    }

    public synchronized int getSize() {
        return mapSites.size();
    }

    public synchronized void addThreadsSet(LinksThread thread) {
        threadsSet.add(thread);
    }

    public synchronized boolean paraLeitura() {
        if (threadsSet.size() > runningThreadsLimit || activeLinks > activeLinksLimit) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized void removeThreadsSet(LinksThread thread) {
        threadsSet.remove(thread);
        removeThread(thread);
        if (mapSites.size() < 2) {
            SimpleLog.getInstance().writeLog(7, "numero de threads: " + threadsSet.size());
            SimpleLog.getInstance().writeLog(7, "numero de numero de sites: " + mapSites.size());
        }
        if (threadsSet.size() == 1) {
            Iterator<LinksThread> threadIterator = threadsSet.iterator();
            if (threadIterator.hasNext()) {
                LinksThread threadAux = threadIterator.next();
                SimpleLog.getInstance().writeLog(7, "último sitio: " + threadAux.getKeySite());
            }
        }
    }

    public synchronized void excludeSite(String site) {
        if (excludeNewSites) {
            excludedSites.add(site);
            SimpleLog.getInstance().writeLog(7, "bad requesters list: " + excludedSites.size() + " " + site);
        }
    }

    public synchronized void removeExcludedSite(String site) {
        excludedSites.remove(site);
    }

    public synchronized boolean containsBadRequesters(String badrequester) {
        if (excludedSites.contains(badrequester)) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized void addTotalReq() {
        totalREQ++;
    }

    public synchronized void addLinksInvalidos(int add) {
        linksInvalidos += add;
    }

    public synchronized void addDBLinksCounter(int add) {
        dbLinksCount += add;
    }

    public synchronized void addDBDomCounter(int add) {
        dbDomCount += add;
    }

    public synchronized void addDBExtCounter(int add) {
        dbExtCount += add;
    }

    public synchronized void addMapDomCounter() {
        mapDomCount++;
    }

    public synchronized void addMapExtCounter() {
        mapExtCount++;
    }

    public synchronized int getDBLinksCounter() {
        return this.dbLinksCount;
    }

    public synchronized int getDBDomCounter() {
        return this.dbDomCount;
    }

    public synchronized int getDBExtCounter() {
        return this.dbExtCount;
    }

    public synchronized int getMapDomCounter() {
        return this.mapDomCount;
    }

    public synchronized int getMapExtCounter() {
        return this.mapExtCount;
    }

    public synchronized int getLinksInvalidos() {
        return linksInvalidos;
    }

    public int getTotalLinks() {
        return totalLinks;
    }

    /**
	 * @param queueLinksLimit the queueLinksLimit to set
	 */
    void setQueueLinksLimit(int queueLinksLimit) {
        this.queueLinksLimit = queueLinksLimit;
    }

    /**
	 * @param runningThreadsLimit the runningThreadsLimit to set
	 */
    void setRunningThreadsLimit(int runningThreadsLimit) {
        this.runningThreadsLimit = runningThreadsLimit;
    }

    /**
	 * @param activeLinksLimit the activeLinksLimit to set
	 */
    void setActiveLinksLimit(int activeLinksLimit) {
        this.activeLinksLimit = activeLinksLimit;
    }

    public synchronized void insereMapa(String keySites, LinkData linkData) {
        LinksThread siteThread = getThread(keySites);
        if (siteThread == null || !siteThread.addEstrutura(linkData)) {
            Queue<LinkData> fila = new LinkedList<LinkData>();
            fila.add(linkData);
            LinksThread thread = new LinksThread(fila, keySites);
            thread.start();
            this.setMap(keySites, thread);
        } else {
            if (siteThread.isRunning()) {
                if (siteThread.getTamanhoFila() > queueLinksLimit) {
                }
            }
        }
    }

    private synchronized void divideFila(LinksThread ThreadCheia) {
        Queue<LinkData> fila = new LinkedList<LinkData>();
        LinksThread thread = new LinksThread(fila);
        int tamanho = (ThreadCheia.getFila().size()) / 2;
        for (int i = 0; (i < tamanho); i++) {
            LinkData link = ThreadCheia.getFirstFila();
            if (link != null) {
                thread.addEstrutura(link);
            } else {
                break;
            }
        }
        thread.start();
    }

    public synchronized void addTotalLinks(int quantidade) {
        totalLinks += quantidade;
    }

    public synchronized boolean isEmpty() {
        return threadsSet.isEmpty();
    }

    public synchronized void addActiveLinks(int amount) {
        activeLinks += amount;
    }

    public void setExcludeNewSites(boolean excludeNewSites) {
        this.excludeNewSites = excludeNewSites;
    }

    private class SiteSizeComparator implements Comparator<LinksThread> {

        @Override
        public int compare(LinksThread o1, LinksThread o2) {
            if (o1 != null && o2 != null) {
                return o1.getTamanhoFila() - o2.getTamanhoFila();
            } else if (o1 == null && o2 != null) return 1; else if (o1 != null && o2 == null) return -1;
            return 0;
        }
    }
}
