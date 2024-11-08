package br.nic.connector.downloadlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hibernate.ScrollableResults;
import br.nic.connector.dao.PagesDAO;
import br.nic.connector.database.HibernateFactory;
import br.nic.connector.general.Constants;
import br.nic.connector.general.SimpleLog;
import br.nic.connector.generics.GenericTest;
import br.nic.connector.generics.pages.nottested.NotTestedPageTester;

/**
 * AutomatedTester que implementa os testes relativos à consolidação dos dados obtidos a partir
 * do CSV gerado pelo Wire para os hosts.
 * @author Pedro Hadek
 */
public class AutomatedCSVDownloader extends NotTestedPageTester {

    private int depth;

    private static AutomatedCSVDownloader singleton;

    private Map<String, Date> downloadedHosts;

    private long id = 0;

    private String doneFilesLocation;

    private Map<String, Integer> statusMap;

    /**
	 * Type is set in order to correctly print messages for this class.
	 */
    private AutomatedCSVDownloader() {
        type = "Download de paginas restantes";
    }

    /**
	 * Returns the static instance of this class.
	 */
    public static AutomatedCSVDownloader getInstance() {
        if (singleton == null) singleton = new AutomatedCSVDownloader();
        return singleton;
    }

    /**
	 * Returns the class responsible for implementing the test required for this AutomatedTester.
	 * In this case it's CSVDowloader, initialized with adequate values.
	 */
    @Override
    protected GenericTest getNewTestType() {
        return new CSVDownloader(this.depth, defaultFile);
    }

    /**
	 * Escreve os dados retornados em returnValue no Banco de Dados adequado, neste caso 
	 * não se aplica.
	 */
    @Override
    protected void writeTestData(Object currTestIdentifier, Object returnValue) {
    }

    /**
	 * Retorna o número de linhas analisadas pelo programa até agora.
	 */
    public Long getNumLines() {
        return soFar;
    }

    @Override
    public boolean setParameters(String testName, List<String> arguments) {
        super.setParameters(testName, arguments);
        try {
            int testNamePos = arguments.indexOf(testName);
            depth = Integer.parseInt(arguments.get(testNamePos + 2));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Rotina que inicia os testes. Apenas esta recebe todos os parametros necessários.
	 * @param maxThreads
	 * Número de threads a serem utilizadas.
	 * @param retest
	 * Indica se as páginas serão baixadas de novo ou não.
	 * @param defaultFile
	 * Pasta raiz a partir de onde os arquivos .xml serão salvos.
	 * @param depth
	 * Profundidade considerada para as pastas.
	 * @return
	 */
    public synchronized boolean startTesting() {
        System.out.println("");
        if (defaultFile.endsWith("/")) {
            this.doneFilesLocation = defaultFile + "doneNumber.done";
        } else {
            this.doneFilesLocation = defaultFile + "/doneNumber.done";
        }
        downloadedHosts = new TreeMap<String, Date>();
        Constants.STATIC_ROOT_FOLDER = defaultFile;
        pagesInfo = new PagesDAO((new HibernateFactory()).getSession(), this.encrypt);
        return super.startTesting();
    }

    public synchronized boolean shouldDownload(String hostName) {
        boolean result = false;
        Date accessDate = downloadedHosts.get(hostName);
        if (accessDate == null) result = true; else {
            if (accessDate.getTime() + Constants.XML_DOWNLOADWAITTIME > System.currentTimeMillis()) {
                result = false;
            } else {
                result = true;
            }
        }
        if (result) {
            downloadedHosts.put(hostName, new Date(System.currentTimeMillis()));
        }
        return result;
    }

    /**
	 * Obtem o maior valor de identificador utilizado até este momento, aumentando consequentemente o
	 * valor.
	 * @return
	 */
    public synchronized long getId() {
        id++;
        return id;
    }

    /**
	 * Método que permite a escrita periódica do número de arquivos já baixados, antes de utilizar o
	 * getNextUntested da classe "pai".
	 */
    @Override
    protected final Object getNextUntested() {
        if (soFar > 0 && currThreads > 0 && soFar % currThreads == 0) {
            try {
                BufferedWriter done = new BufferedWriter(new FileWriter(doneFilesLocation));
                done.write("" + (soFar - currThreads));
                done.close();
            } catch (IOException e) {
                SimpleLog.getInstance().writeLog(3, "Não foi possivel contabilizar quantidade" + " de páginas baixadas até o momento.");
            }
        }
        Object[] next = null;
        if (!untested.isLast()) {
            next = untested.get();
            untested.next();
        }
        return next;
    }

    /**
	 * Scroll para as páginas que não foram baixadas pelo Wire e que devem ser baixadas.
	 * Importante: Enquanto o Scroll estiver aberto, não é possível se escrever no Banco de Dados!
	 */
    @Override
    protected ScrollableResults getUntestedPageScroll() {
        return pagesInfo.getUnanalizedPagesW3C();
    }

    /**
	 * Na finalização, escreve o número total de páginas baixadas no arquivo que indica o
	 * progresso até o momento.
	 * Após isto, escreve no banco de dados 
	 */
    @Override
    public void finalTestConditions() {
        try {
            BufferedWriter done = new BufferedWriter(new FileWriter(doneFilesLocation));
            done.write("" + soFar);
            done.close();
        } catch (IOException e) {
            SimpleLog.getInstance().writeLog(3, "Não foi possivel contabilizar a quantidade" + " de páginas baixadas até o momento.");
        }
        untested.close();
        long time1 = System.currentTimeMillis();
        int i = 0;
        for (Iterator<String> itStatus = statusMap.keySet().iterator(); itStatus.hasNext(); ) {
            i++;
            String pagina = itStatus.next();
            int http_status = statusMap.get(pagina);
            pagesInfo.writeHttpStatusRedownload(pagina, http_status);
            if (i % 10000 == 0) {
                long time2 = System.currentTimeMillis();
                System.out.println("Tempo para escrever " + i + " status: " + (time2 - time1) / 1000 + "s");
            }
        }
    }

    @Override
    public boolean initialTestConditions() {
        if (!retest) {
            BufferedReader done = null;
            try {
                done = new BufferedReader(new FileReader(doneFilesLocation));
                int start = 0;
                if (done.ready()) {
                    start = Integer.parseInt(done.readLine());
                    done.close();
                }
                System.out.println("começando!");
                for (int i = 0; (i < start && hasNextUntested()); i++) {
                    getNextUntested();
                    id++;
                }
            } catch (FileNotFoundException e) {
            } catch (NumberFormatException e) {
                SimpleLog.getInstance().writeLog(3, "Arquivo de regitro incorreto.");
            } catch (IOException e) {
                SimpleLog.getInstance().writeLog(3, "Não conseguiu ler o arquivo de registro.");
            } finally {
                if (done != null) try {
                    done.close();
                } catch (IOException e) {
                }
            }
        }
        statusMap = new HashMap<String, Integer>();
        return true;
    }

    public boolean isRetest() {
        return retest;
    }

    public PagesDAO getPagesInfo() {
        return pagesInfo;
    }

    /**
	 * Monta um mapa com os nomes e status de retorno das páginas baixadas, para que se possa
	 * salvar esta informação posteriormente. Isto é necessário pois, tendo um ScrollableResults
	 * aberto, alterações na base de dados não são permitidas pelo Hibernate.
	 * O salvamento destas deve ser feito após o término de todos os downloads,
	 * dentro do finalTestCondition.
	 * @param address
	 * Endereço do site
	 * @param downloaded
	 * Indica o status de retorno do download da página.
	 */
    public void addAddressStatus(String address, int statusRedownload) {
        statusMap.put(address, statusRedownload);
    }
}
