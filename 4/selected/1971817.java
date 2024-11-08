package br.nic.connector.linkscompleto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import br.nic.connector.dao.LinksCompletoDAO;
import br.nic.connector.dao.LinksDAO;
import br.nic.connector.dao.LinksFileDAO;
import br.nic.connector.dao.PagesDAO;
import br.nic.connector.database.HibernateFactory;
import br.nic.connector.database.LinksFile;
import br.nic.connector.general.Constants;
import br.nic.connector.general.SimpleLog;
import br.nic.connector.generics.GenericTest;
import br.nic.connector.generics.links.AutomatedLinksTester;
import br.nic.connector.linkscompleto.datastructures.ConsolidatedLinksData;

public class AutomatedLinksCounter extends AutomatedLinksTester {

    private static AutomatedLinksCounter singleton;

    private LinksDAO linksDAO;

    private LinksCompletoDAO linksCompletoDAO;

    private Map<Long, Long> pageToHostMap;

    private int countNull = 0;

    private int countNotNull = 0;

    private int arquivosLidos = 0;

    private Set<Long> nullPageIds;

    private PrintStream writer;

    private File downloadLinks = null;

    private PagesDAO pagesDAO;

    private long linhasLidas;

    private long linhasValidas;

    private boolean lockDB;

    private LinksFileDAO linksFileDAO;

    protected Iterator<LinksFile> linksFileIterator;

    /**
	 * Type is set in order to correctly print messages for this class.
	 */
    private AutomatedLinksCounter() {
        type = "Teste de destinos de links por host";
    }

    /**
	 * Returns the static instance of this class.
	 */
    public static AutomatedLinksCounter getInstance() {
        if (singleton == null) singleton = new AutomatedLinksCounter();
        return singleton;
    }

    /**
	 * Inicia o teste utilizando os parâmetros fornecidos. Esta implementação
	 * não implementa a função de não testar elementos já testados, pois estes
	 * não tem sentido neste contexto. Logo, o parâmetro retest pode ser
	 * ignorado.
	 * 
	 * defaultFile deve ser um diretório existente e acessível.
	 */
    @Override
    public boolean startTesting() {
        this.linksFileDAO = new LinksFileDAO(new HibernateFactory());
        File root = new File(defaultFile);
        if (!root.exists()) {
            SimpleLog.getInstance().writeLog(3, "O diretório " + defaultFile + " não existe!");
            return false;
        }
        if (!root.isDirectory()) {
            SimpleLog.getInstance().writeLog(3, "O diretório fornecido " + defaultFile + " não é" + "um diretório.");
            return false;
        }
        if (!root.canRead()) {
            SimpleLog.getInstance().writeLog(3, "O diretório fornecido " + defaultFile + " não pode" + " ser lido.");
            return false;
        }
        List<File> fileListAux = Arrays.asList(root.listFiles());
        List<File> fileList = new ArrayList<File>();
        for (File file : fileListAux) {
            if ((file.getName().endsWith("links_download.txt") || file.getName().endsWith("links_log.txt"))) fileList.add(file);
        }
        SimpleLog.getInstance().writeLog(3, "arquivos a serem analizados na pasta links: " + fileList.size());
        for (File file : fileList) {
            SimpleLog.getInstance().writeLog(7, file.getName());
        }
        SimpleLog.getInstance().writeLog(7, "fim da lista");
        List<LinksFile> linksFilesList = new ArrayList<LinksFile>();
        if (retest) {
            SimpleLog.getInstance().writeLog(3, "Reteste");
            linksFileDAO.clear();
            for (File file : fileList) {
                linksFilesList.add(new LinksFile(file.getName()));
            }
            linksFileDAO.saveList(linksFilesList);
        } else {
            linksFilesList = linksFileDAO.listLinksFile();
            SimpleLog.getInstance().writeLog(3, "Numero de arquivos no banco: " + linksFilesList.size());
            for (int j = 0; j < linksFilesList.size(); j++) {
                for (int i = 0; i < fileList.size(); i++) {
                    if (fileList.get(i).getName().equals(linksFilesList.get(j).getNome())) {
                        SimpleLog.getInstance().writeLog(7, "arquivo já existente: " + fileList.get(i).getName());
                        fileList.remove(i);
                        i--;
                        break;
                    }
                }
            }
            if (!fileList.isEmpty()) {
                for (File file : fileList) {
                    linksFilesList.add(new LinksFile(file.getName()));
                }
                linksFileDAO.saveList(linksFilesList);
            }
            for (int j = 0; j < linksFilesList.size(); j++) {
                if (linksFilesList.get(j).getStatus().equals(LinksFile.COMPLETO)) {
                    SimpleLog.getInstance().writeLog(7, "arquivo completo: " + linksFilesList.get(j).getNome());
                    linksFilesList.remove(j);
                    j--;
                }
            }
            SimpleLog.getInstance().writeLog(3, "Numero de arquivos a serem testados: " + linksFilesList.size());
            System.out.println("limpando dados corrompidos");
            SimpleLog.getInstance().writeLog(3, "limpando dados corrompidos");
            List<LinksFile> corruptedFilesList = new ArrayList<LinksFile>();
            for (LinksFile linksFile : linksFilesList) {
                if (linksFile.getStatus().equals(LinksFile.EM_ANDAMENTO)) corruptedFilesList.add(linksFile);
            }
            if (!corruptedFilesList.isEmpty()) {
                SimpleLog.getInstance().writeLog(3, "Numero de arquivos corropidos:" + linksFilesList.size());
                linksFileDAO.eraseLinksFromFiles(corruptedFilesList);
            }
        }
        SimpleLog.getInstance().writeLog(3, "arquivos a serem analizados: " + linksFilesList.size());
        for (LinksFile linksFile : linksFilesList) {
            SimpleLog.getInstance().writeLog(7, linksFile.getNome());
        }
        SimpleLog.getInstance().writeLog(7, "fim da lista");
        linksFileIterator = linksFilesList.iterator();
        testInitializer(currThreads);
        return true;
    }

    /**
	 * Returns the class responsible for implementing the test required for this AutomatedTester.
	 */
    @Override
    protected GenericTest getNewTestType() {
        return new LinksCounter();
    }

    /**
	 * Próximo arquivo não testado.
	 */
    @Override
    protected Object getNextUntested() {
        arquivosLidos++;
        System.out.println("Iniciando leitura do arquivo número " + arquivosLidos + ".");
        if (linksFileIterator.hasNext()) return linksFileIterator.next(); else return null;
    }

    /**
	 * Retorna o ID do host ao qual pertence um dado pageID.
	 */
    public long getHostFromPage(long pageID) {
        Long x = pageToHostMap.get(pageID);
        if (x == null) {
            countNull++;
            if (countNull % 1000 == 0) {
                System.out.println("pageID: " + pageID + " ; hostID: null");
            }
            nullPageIds.add(pageID);
        } else {
            countNotNull++;
        }
        return (x == null ? 0 : x);
    }

    /**
	 * Indica que não existem elementos não testados caso a lista de arquivos
	 * esteja vazia.
	 */
    @Override
    protected boolean hasNextUntested() {
        return linksFileIterator.hasNext();
    }

    /**
	 * Deve inicializar o LinksDAO e verificar se existe wire_id nas páginas. Se
	 * não, não roda o teste. Por fim, limpa o banco de dados de links.
	 */
    @Override
    public boolean initialTestConditions() {
        downloadLinks = new File("download_links.txt");
        try {
            writer = new PrintStream(downloadLinks);
            writer.println("Wire Id da Página | Wire Id do Host | Link local | Endereço");
            writer.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo de links não pode ser inicializado.");
            return false;
        }
        pagesDAO = new PagesDAO(new HibernateFactory().getSession(), this.encrypt);
        pageToHostMap = pagesDAO.mapPageWireIDtoHost();
        nullPageIds = new TreeSet<Long>();
        linksDAO = new LinksDAO(new HibernateFactory().getSession(), this.encrypt);
        linksCompletoDAO = new LinksCompletoDAO(new HibernateFactory().getSession(), this.encrypt);
        if (retest) {
            linksCompletoDAO.clearLinksTables();
            linksDAO.clearLinksTables();
        }
        System.out.println("Retest " + retest);
        if (pagesDAO.hasWireIDs()) {
            return true;
        } else return false;
    }

    /**
	 * Toda a escrita é feita após exceder o limite de espaço na consolidação,
	 * ou na finalização da análise. Logo, esta classe não precisa ser
	 * implementada.
	 */
    @Override
    protected void writeTestData(Object currTestElement, Object returnValue) {
    }

    /**
	 * Sincroniza acessos de leitura e escrita ao Banco de Dados, garantindo que
	 * não apareçam conflitos quando da inicialização dos dados. Neste caso, é
	 * para as informações de Domínios.
	 * 
	 * @see LinksDAO
	 */
    public synchronized void writeTLDSynchro(Long hostID, String dominio, Boolean local, ConsolidatedLinksData struct) {
        linksDAO.writeTLD(hostID, dominio, local, struct);
    }

    /**
	 * Sincroniza acessos de leitura e escrita ao Banco de Dados, garantindo que
	 * não apareçam conflitos quando da inicialização dos dados. Neste caso, é
	 * para as informações de Extensões.
	 * 
	 * @see LinksDAO
	 */
    public synchronized void writeExtensionSynchro(Long hostID, String extension, ConsolidatedLinksData struct) {
        linksDAO.writeExtension(hostID, extension, struct);
    }

    /**
	 * Apenas no final de todos irá escrever no banco de dados.
	 */
    @Override
    public void finalTestConditions() {
        while (!MapSites.getInstance().isEmpty()) try {
            Thread.currentThread();
            Thread.sleep(1000);
            SimpleLog.getInstance().writeLog(3, "Aguardando finalização de alguma Thread");
        } catch (InterruptedException e) {
            SimpleLog.getInstance().writeException(e, 3);
        }
        System.out.println("Consolidação das quantidades de links finalizada.");
        System.out.println("Total null: " + countNull / 2);
        System.out.println("Total não null: " + countNotNull / 2);
        System.out.println("Quantidade total de domínios: " + MapSites.getInstance().getMapDomCounter());
        System.out.println("Quantidade total de extensoes: " + MapSites.getInstance().getMapExtCounter());
        System.out.println("Quantidade total de links: " + MapSites.getInstance().getTotalLinks());
        System.out.println("Quantidade total de linhas lidas: " + this.linhasLidas);
        System.out.println("Quantidade total de linhas lidas validas: " + this.linhasValidas);
        System.out.println("Quantidade total de arquivos lidos: " + this.arquivosLidos);
        System.out.println("Terminou às " + new Date(System.currentTimeMillis()).toString());
        SimpleLog.getInstance().writeLog(5, "==================================");
        SimpleLog.getInstance().writeLog(5, "Páginas com resultado null: ");
        for (Iterator<Long> it = nullPageIds.iterator(); it.hasNext(); ) {
            SimpleLog.getInstance().writeLog(5, it.next().toString());
        }
        SimpleLog.getInstance().writeLog(5, "==================================");
        System.out.println("Links Invalidos: " + MapSites.getInstance().getLinksInvalidos());
        System.out.println("Links Escritos no Banco de Dados de Links: " + MapSites.getInstance().getDBLinksCounter());
        System.out.println("Links Escritos no Banco de Dados de Dominios:" + MapSites.getInstance().getDBDomCounter());
        System.out.println("Links Escritos no Banco de Dados de Extensoes: " + MapSites.getInstance().getDBExtCounter());
    }

    public void writeLinkDownloadList(Long pageWireId, Long hostWireId, boolean local, String url) {
        writer.println(pageWireId + " | " + hostWireId + " | " + local + " | " + url);
        writer.flush();
    }

    /**
	 * Obtêm o objeto LinksCompletoDAO criado
	 */
    public LinksCompletoDAO getLinksCompletoDAO() {
        return linksCompletoDAO;
    }

    public PagesDAO getPagesDAO() {
        return pagesDAO;
    }

    public synchronized void addLinhasLidas(long linhasLidas) {
        this.linhasLidas += linhasLidas;
    }

    public void addLinhasValidas(long countValidLines) {
        this.linhasValidas += countValidLines;
    }

    public synchronized void lockDataAccess() {
        while (lockDB) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        lockDB = true;
        notifyAll();
    }

    public synchronized void unlockDataAccess() {
        while (!lockDB) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        lockDB = false;
        notifyAll();
    }

    public synchronized void flushLinksFileSession(LinksFile currentLinksFile) {
        linksFileDAO.flushSession(currentLinksFile);
    }

    public synchronized void reopenLinksDAOSession() {
        linksDAO.closeSession();
        linksDAO.setSession(new HibernateFactory().getSession());
    }

    @Override
    public boolean setParameters(String testName, List<String> arguments) {
        for (Iterator<String> argIterator = arguments.iterator(); argIterator.hasNext(); ) {
            String arg = argIterator.next();
            if (arg.equalsIgnoreCase(Constants.LINKS_DONT_EXCLUDE_SITES)) {
                MapSites.getInstance().setExcludeNewSites(false);
            } else if (arg.equalsIgnoreCase(Constants.LINKS_ACTIVE_ELEMENTS)) {
                MapSites.getInstance().setActiveLinksLimit(Integer.parseInt(argIterator.next()));
            } else if (arg.equalsIgnoreCase(Constants.LINKS_MAX_QUEUE)) {
                MapSites.getInstance().setQueueLinksLimit(Integer.parseInt(argIterator.next()));
            } else if (arg.equalsIgnoreCase(Constants.LINKS_MAX_SITE_THREADS)) {
                MapSites.getInstance().setQueueLinksLimit(Integer.parseInt(argIterator.next()));
            }
        }
        return super.setParameters(testName, arguments);
    }
}
