package br.org.acessobrasil.processoacessibilidade.bo;

import java.io.File;
import java.io.FileFilter;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import br.org.acessobrasil.debug.G_Cronometro;
import br.org.acessobrasil.io.G_File;
import br.org.acessobrasil.persistencia.DataSource;
import br.org.acessobrasil.portal.action.Instalador;
import br.org.acessobrasil.portal.modelo.Conteudo;
import br.org.acessobrasil.portal.modelo.ItemConteudo;
import br.org.acessobrasil.portal.modelo.PadraoApresentacao;
import br.org.acessobrasil.portal.modelo.Pagina;
import br.org.acessobrasil.portal.modelo.Sitio;
import br.org.acessobrasil.portal.persistencia.ArquivoDaoSpring;
import br.org.acessobrasil.portal.persistencia.ConteudoDaoSpring;
import br.org.acessobrasil.portal.persistencia.ConteudoFormatadoDao;
import br.org.acessobrasil.portal.persistencia.FormatoDaoSpring;
import br.org.acessobrasil.portal.persistencia.InformacaoTipoConteudoDao;
import br.org.acessobrasil.portal.persistencia.ItemConteudoDaoSpring;
import br.org.acessobrasil.portal.persistencia.ItemMenuDaoSpring;
import br.org.acessobrasil.portal.persistencia.MenuDaoSpring;
import br.org.acessobrasil.portal.persistencia.PadraoApresentacaoDaoSpring;
import br.org.acessobrasil.portal.persistencia.PaginaDaoSpring;
import br.org.acessobrasil.portal.persistencia.PerfilDaoSpring;
import br.org.acessobrasil.portal.persistencia.PrivilegioDaoSpring;
import br.org.acessobrasil.portal.persistencia.SetorDao;
import br.org.acessobrasil.portal.persistencia.SitioDaoSpring;
import br.org.acessobrasil.portal.persistencia.TipoConteudoDaoSpring;
import br.org.acessobrasil.portal.persistencia.UsuarioDaoSpring;
import br.org.acessobrasil.processoacessibilidade.dao.ArquivoDao;
import br.org.acessobrasil.processoacessibilidade.dao.ConteudoDao;
import br.org.acessobrasil.processoacessibilidade.dao.ErroCssDao;
import br.org.acessobrasil.processoacessibilidade.dao.ErroOuAvisoDao;
import br.org.acessobrasil.processoacessibilidade.dao.FormularioDao;
import br.org.acessobrasil.processoacessibilidade.dao.Inicializador;
import br.org.acessobrasil.processoacessibilidade.dao.JobDao;
import br.org.acessobrasil.processoacessibilidade.dao.LinguagemProgramacaoDao;
import br.org.acessobrasil.processoacessibilidade.dao.LinkDao;
import br.org.acessobrasil.processoacessibilidade.dao.PaginaDao;
import br.org.acessobrasil.processoacessibilidade.dao.RelatorioDao;
import br.org.acessobrasil.processoacessibilidade.dao.SitioDao;
import br.org.acessobrasil.processoacessibilidade.dao.TemplateDao;
import br.org.acessobrasil.processoacessibilidade.vo.CssPro;
import br.org.acessobrasil.processoacessibilidade.vo.JobPro;
import br.org.acessobrasil.processoacessibilidade.vo.LinkPro;
import br.org.acessobrasil.processoacessibilidade.vo.PaginaPro;
import br.org.acessobrasil.processoacessibilidade.vo.RelatorioAcessibilidadePro;
import br.org.acessobrasil.processoacessibilidade.vo.SitioPro;

/**
 * Responsavel por saber se a url pertence ou nao ao trabalho.
 * 
 * @author Fabio, Jonatas, Zupo
 *
 */
public class Coordenador {

    public final int MAXTHREADS = 3;

    private Arquivista arquivista = null;

    private Analista analista = new Analista();

    private Conteudista conteudista = new Conteudista();

    private Retificador retificador = new Retificador();

    private FiltroDeUrl filtroDeUrl = null;

    public static String PASTA_BASE_ARQUIVOS_CMS = "../CMS08/web/";

    /**
	 * Print out do coordenador
	 */
    private static Logger logger = Logger.getLogger(Coordenador.class);

    private static PaginaDao paginaDao;

    private static SitioDao sitioDao;

    private static LinkDao linkDao;

    private static ArquivoDao arquivoDao;

    private static FormularioDao formularioDao;

    static LinguagemProgramacaoDao linguagemProgramacaoDao;

    private static JobDao jobDao;

    private static ErroCssDao erroCssDao;

    private static TemplateDao templateDao;

    private static RelatorioDao relatorioDao;

    private static ConteudoDao conteudoDao;

    private static DataSource dataSource;

    private static ErroOuAvisoDao erroOuAvisoDao;

    private static int procIniciado = 0;

    private static int procParado = 0;

    private static ConteudoDaoSpring conteudoDaoCMS;

    private static ItemConteudoDaoSpring itemConteudoDaoCMS;

    private static InformacaoTipoConteudoDao informacaoTipoConteudoDaoCMS;

    private static ConteudoFormatadoDao ConteudoFormatadoDaoCMS;

    private static SitioDaoSpring sitioDaoCMS;

    private static PaginaDaoSpring paginaDaoCMS;

    private static PadraoApresentacaoDaoSpring padraoApresentacaoDaoCMS;

    private static TipoConteudoDaoSpring tipoConteudoDaoCMS;

    private static UsuarioDaoSpring usuarioDaoCMS;

    private static PerfilDaoSpring perfilDaoCMS;

    private static SetorDao setorDaoCMS;

    private static PrivilegioDaoSpring privilegioDaoCMS;

    private static MenuDaoSpring menuDaoCMS;

    private static ItemMenuDaoSpring itemMenuDaoCMS;

    private static FormatoDaoSpring formatoDaoCMS;

    private static ArquivoDaoSpring arquivoDaoCMS;

    private Sitio sitioCms;

    private static PadraoApresentacao padraoApresentacao = null;

    static {
        Avaliador avaliador = new Avaliador();
        avaliador.setDtdBasePath("http://www.acessobrasil.org.br/acfacre/dtd/");
        Inicializador.iniciar();
        paginaDao = (PaginaDao) Inicializador.getProDao("PaginaDao");
        sitioDao = (SitioDao) Inicializador.getProDao("SitioDao");
        linkDao = (LinkDao) Inicializador.getProDao("LinkDao");
        arquivoDao = (ArquivoDao) Inicializador.getProDao("ArquivoDao");
        formularioDao = (FormularioDao) Inicializador.getProDao("FormularioDao");
        linguagemProgramacaoDao = (LinguagemProgramacaoDao) Inicializador.getProDao("LinguagemProgramacaoDao");
        jobDao = (JobDao) Inicializador.getProDao("JobDao");
        erroCssDao = (ErroCssDao) Inicializador.getProDao("ErroCssDao");
        templateDao = (TemplateDao) Inicializador.getProDao("TemplateDao");
        relatorioDao = (RelatorioDao) Inicializador.getProDao("RelatorioDao");
        conteudoDao = (ConteudoDao) Inicializador.getProDao("ConteudoDao");
        dataSource = (DataSource) Inicializador.getDataSource();
        erroOuAvisoDao = (ErroOuAvisoDao) Inicializador.getProDao("ErroOuAvisoDao");
        Visitante.setLinkDao(linkDao);
        Visitante.setPaginaDao(paginaDao);
        Arquivista.setArquivoDao(arquivoDao);
        Arquivista.setPaginaDao(paginaDao);
        Analista.setFormularioDao(formularioDao);
        Analista.setPaginaDao(paginaDao);
        Relator.setDataSource(dataSource);
        Relator.setRelatorioDao(relatorioDao);
        Relator.setErroOuAvisoDao(erroOuAvisoDao);
        Relator.setSitioDao(sitioDao);
        Relator.setPaginaDao(paginaDao);
        Relator.setLinguagemProgramacaoDao(linguagemProgramacaoDao);
        CssPro.setErroCssDao(erroCssDao);
        Conteudista.setPagProDao(paginaDao);
        conteudoDaoCMS = (ConteudoDaoSpring) Inicializador.getCmsDao("ConteudoDaoSpring");
        informacaoTipoConteudoDaoCMS = (InformacaoTipoConteudoDao) Inicializador.getCmsDao("InformacaoTipoConteudoDao");
        itemConteudoDaoCMS = (ItemConteudoDaoSpring) Inicializador.getCmsDao("ItemConteudoDaoSpring");
        ConteudoFormatadoDaoCMS = new ConteudoFormatadoDao();
        sitioDaoCMS = (SitioDaoSpring) Inicializador.getCmsDao("SitioDaoSpring");
        paginaDaoCMS = (PaginaDaoSpring) Inicializador.getCmsDao("PaginaDaoSpring");
        padraoApresentacaoDaoCMS = (PadraoApresentacaoDaoSpring) Inicializador.getCmsDao("PadraoApresentacaoDaoSpring");
        tipoConteudoDaoCMS = (TipoConteudoDaoSpring) Inicializador.getCmsDao("TipoConteudoDaoSpring");
        usuarioDaoCMS = (UsuarioDaoSpring) Inicializador.getCmsDao("UsuarioDaoSpring");
        perfilDaoCMS = (PerfilDaoSpring) Inicializador.getCmsDao("PerfilDaoSpring");
        setorDaoCMS = (SetorDao) Inicializador.getCmsDao("SetorDao");
        privilegioDaoCMS = (PrivilegioDaoSpring) Inicializador.getCmsDao("PrivilegioDaoSpring");
        menuDaoCMS = (MenuDaoSpring) Inicializador.getCmsDao("MenuDaoSpring");
        itemMenuDaoCMS = (ItemMenuDaoSpring) Inicializador.getCmsDao("ItemMenuDaoSpring");
        formatoDaoCMS = (FormatoDaoSpring) Inicializador.getCmsDao("FormatoDaoSpring");
        arquivoDaoCMS = (ArquivoDaoSpring) Inicializador.getCmsDao("ArquivoDaoSpring");
    }

    public Coordenador() {
        arquivista = new Arquivista(this);
    }

    class Processo extends Thread {

        G_Cronometro cro = new G_Cronometro();

        private Thread temporizador;

        String url;

        JobPro job;

        PaginaPro pagina;

        private Date dataGerada;

        private boolean parado = false;

        Processo(String url, JobPro job, Date dataRelatorio) {
            cro.start();
            procIniciado++;
            this.dataGerada = dataRelatorio;
            this.url = url;
            this.job = job;
            this.pagina = paginaDao.find(url);
            if (this.pagina == null) {
                logger.warn("Nao deveria criar a pagina por aqui...");
                this.pagina = new PaginaPro();
                this.pagina.setUrl(url);
                this.pagina.setIdSitio(job.getSitio().getId());
                this.pagina = paginaDao.create(this.pagina);
            }
            if (this.pagina.getIdPaginaCMS() == null) {
                Pagina paginaCms = new Pagina();
                paginaDaoCMS.create(paginaCms);
                logger.debug("Id P�gina = " + paginaCms.getNuPagina());
                this.pagina.setIdPaginaCMS(paginaCms.getNuPagina());
            }
            this.pagina.setIdLinguagem(analista.verificarLinguagem(this.pagina.getUrl()));
            this.job.setProcessos(job.getProcessos() + 1);
        }

        synchronized void parar(String erro) {
            if (parado) return;
            parado = true;
            pagina.setOriginalData(new Date());
            pagina.setErro(erro);
            paginaDao.update(pagina);
            cro.stop("Coordenador:\n\tProcesso:" + pagina.getUrl() + "\n\ttempo", logger);
            logger.debug("procParado = " + procParado + "\tprocIniciado=" + procIniciado + "\t" + (procIniciado - procParado));
            logger.info("Processo.parar():" + pagina.getUrl() + "\n\tErro=" + pagina.getErro());
            job.setProcessos(job.getProcessos() - 1);
            procParado++;
            this.interrupt();
        }

        private void iniciaTemporizador() {
            temporizador = new Thread() {

                public void run() {
                    try {
                        Thread.sleep(60000 * 20);
                    } catch (Exception e) {
                    }
                    parar("Tempo limite esgotado");
                }
            };
            temporizador.start();
        }

        @Override
        public void run() {
            try {
                iniciaTemporizador();
                Visitante visitante = new Visitante();
                visitante.visitarPagina(pagina);
                for (LinkPro link : pagina.getListaLinks()) {
                    if (verificarPaginaPertenceSitio(link.getDestino().getUrl(), job) && !job.getListaLink().contains(link.getDestino().getUrl())) {
                        job.getListaLink().add(link.getDestino().getUrl());
                    } else {
                        logger.debug("Url descartada " + link.getDestino().getUrl());
                    }
                }
                try {
                    synchronized (pagina) {
                        analista.verificarFormulario(pagina, job);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                {
                    String pathPagina = arquivista.criarPagina(pagina, job);
                    arquivista.baixarArquivos(pagina, job);
                    if (pathPagina != null) {
                        logger.info("avaliando " + pagina.getUrl());
                        Avaliador avaliador = new Avaliador();
                        avaliador.avaliarHtml(pagina.getCodTrabalho());
                        RelatorioAcessibilidadePro relatorio = avaliador.getRelatorio();
                        relatorio.setDataGerada(dataGerada);
                        relatorio.setPagina(pagina);
                        relatorioDao.create(relatorio);
                    }
                    arquivista.criarPaginaTrabalho(pagina, job);
                }
                conteudista.retirarConteudo(pagina, padraoApresentacao);
                parar(null);
            } catch (ConnectException e) {
                try {
                    parar("ConnectException: " + e.getMessage());
                } catch (Exception e2) {
                }
            } catch (UnknownHostException e) {
                try {
                    parar("UnknownHostException");
                } catch (Exception e2) {
                }
            } catch (Exception e) {
                try {
                    parar(e.getClass().getName() + " " + e.getMessage());
                    logger.error("Erro inesperado ", e);
                } catch (Exception e2) {
                    parar("erro interno");
                }
                if (e != null && e.getMessage() != null && !e.getMessage().equals("Erro 404")) {
                }
            } finally {
                temporizador.interrupt();
            }
        }
    }

    /**
	 * 
	 * @param url http://www.qqcoisa.com
	 * @param job
	 * @return true caso pertence
	 */
    public boolean verificarPaginaPertenceSitio(String url, JobPro job) {
        if (filtroDeUrl != null) {
            return filtroDeUrl.pertence(url);
        } else if (url.matches(job.getSitio().getUrlRegex())) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Faz a coordenacao do mapeamento do sitio
	 * @param sitio
	 * @throws Exception
	 */
    public void mapearSitio(SitioPro sitio) throws Exception {
        SitioPro tempSitio = sitioDao.find(sitio.getUrlInicial());
        if (tempSitio == null) {
            sitioDao.create(sitio);
        } else {
            tempSitio.setUrlRegex(sitio.getUrlRegex());
            sitio = tempSitio;
        }
        final JobPro job = new JobPro(sitio);
        jobDao.reload(job);
        if (job.getIndice() > 0) {
            logger.debug("continuando de " + job.getIndice());
        }
        arquivista.criarPastaSitio(job);
        job.getListaLink().add(sitio.getUrlInicial());
        Date dataRelatorio = new Date();
        while (job.getProcessos() > 0 || job.getIndice() < job.getListaLink().size()) {
            if (verificarMaximoProcessos(job)) continue;
            if (job.getIndice() < job.getListaLink().size()) {
                String url = job.getListaLink().get(job.getIndice());
                if (verificarPaginaPertenceSitio(url, job)) {
                    Thread t = new Processo(url, job, dataRelatorio);
                    t.start();
                } else {
                    logger.debug("Url descartada " + url);
                }
                job.setIndice(job.getIndice() + 1);
            } else {
                logger.debug("job.getProcessos()=" + job.getProcessos());
                Thread.sleep(500);
            }
        }
    }

    private boolean verificarMaximoProcessos(JobPro job) {
        if (job.getProcessos() >= MAXTHREADS) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    public void retificarPaginasTrabalho(SitioPro sitio) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        Date dataAtual = new Date();
        String pastaBkp = Arquivista.pastaDeArquivosDoSitio + "/bkp/" + sdf.format(dataAtual);
        File dirBkp = new File(pastaBkp);
        if (!dirBkp.exists()) {
            dirBkp.mkdirs();
        }
        File dirHtml = new File(Arquivista.pastaDeArquivosDoSitio);
        File arquivosHtml[] = dirHtml.listFiles(new FileFilter() {

            @Override
            public boolean accept(File arq) {
                if (arq.getName().endsWith(".html")) return true;
                return false;
            }
        });
        int c = 1;
        logger.debug("Retificando " + arquivosHtml.length + " arquivos");
        DesignerCss designerCss = new DesignerCss();
        for (File file : arquivosHtml) {
            String nomeArq = G_File.getName(file.getAbsolutePath());
            FileUtils.copyFile(file, new File(pastaBkp + "/" + nomeArq));
            G_File arqHtml = new G_File(file.getAbsolutePath());
            String codHtml = retificador.consertarErroAcessibilidade(arqHtml.read());
            codHtml = retificador.consertarErroHtml(codHtml);
            codHtml = designerCss.retirarCssInline(codHtml, nomeArq);
            arqHtml.write(codHtml);
            logger.debug("Retificado " + nomeArq + " " + (c++) + " de " + arquivosHtml.length);
        }
    }

    public void retificarPaginasTrabalho(String url) throws Exception {
        SitioPro sitio = iniciarSitio(url);
        sitio = sitioDao.find(sitio.getUrlInicial());
        if (sitio == null) {
            throw new Exception("SitioPro nao encontrado " + url);
        }
        retificarPaginasTrabalho(sitio);
    }

    public void reavaliarPaginasTrabalhadas(SitioPro sitio) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        Date dataRelatorio = new Date();
        File dirHtml = new File(Arquivista.pastaDeArquivosDoSitio);
        File arquivosHtml[] = dirHtml.listFiles(new FileFilter() {

            @Override
            public boolean accept(File arq) {
                if (arq.getName().endsWith(".html")) return true;
                return false;
            }
        });
        int c = 1;
        logger.debug("Reavaliar " + arquivosHtml.length + " arquivos");
        for (File file : arquivosHtml) {
            G_File arqHtml = new G_File(file.getAbsolutePath());
            String id = G_File.getName(file.getAbsolutePath()).replace(".html", "");
            PaginaPro pagina = paginaDao.find(Long.valueOf(id));
            Avaliador avaliador = new Avaliador();
            avaliador.setDtdBasePath("http://www.acessobrasil.org.br/acfacre/dtd/");
            avaliador.avaliarHtml(arqHtml.read());
            RelatorioAcessibilidadePro relatorio = avaliador.getRelatorio();
            relatorio.setPagina(pagina);
            relatorio.setDataGerada(dataRelatorio);
            relatorioDao.create(relatorio);
            logger.debug("Reavaliada " + id + " " + (c++) + " de " + arquivosHtml.length);
        }
    }

    public void reavaliarPaginasTrabalhadas(String url) throws Exception {
        SitioPro sitio = iniciarSitio(url);
        sitio = sitioDao.find(sitio.getUrlInicial());
        if (sitio == null) {
            throw new Exception("SitioPro nao encontrado " + url);
        }
        reavaliarPaginasTrabalhadas(sitio);
    }

    public static PaginaDao getPaginaDao() {
        return paginaDao;
    }

    public static void setPaginaDao(PaginaDao paginaDao) {
        Coordenador.paginaDao = paginaDao;
    }

    public Arquivista getArquivista() {
        return arquivista;
    }

    public void setArquivista(Arquivista arquivista) {
        this.arquivista = arquivista;
    }

    public void tentarBaixarArquivosErro(String url) {
        SitioPro sitio = null;
        try {
            sitio = iniciarSitio(url);
        } catch (Exception e) {
        }
        SitioPro tempSitio = sitioDao.find(sitio.getUrlInicial());
        if (tempSitio == null) {
            sitioDao.create(sitio);
        } else {
            tempSitio.setUrlRegex(sitio.getUrlRegex());
            sitio = tempSitio;
        }
        final JobPro job = new JobPro(sitio);
        arquivista.tentarBaixarArquivosErro(job);
    }

    /**
	 * Inicia as configurcoes de umsitio e o mapeia
	 * @param urlInicial
	 * @throws Exception
	 */
    public void mapearSitio(String urlInicial) throws Exception {
        mapearSitio(iniciarSitio(urlInicial));
    }

    /**
	 * Inicia as configurcoes de um sitio, a sua regex e a url inicial
	 * e devolve a instancia deste sitio
	 * @param urlInicial
	 * @return instancia do sitio
	 */
    public SitioPro iniciarSitio(final String urlInicial) throws Exception {
        final URL urlSitio = new URL(urlInicial);
        if (sitioCms == null) {
            sitioCms = sitioDaoCMS.select(urlSitio.getHost());
            if (sitioCms == null) {
                sitioCms = new Sitio();
                sitioCms.setUrl(urlSitio.getHost());
                sitioCms.setNoSitio(urlSitio.getHost());
                sitioDaoCMS.create(sitioCms);
                logger.info("sitioCms criado na inicializacao " + sitioCms.getNoSitio() + "#" + sitioCms.getNuSitio());
            }
        }
        arquivista.setPastaDeArquivosDoSitio(PASTA_BASE_ARQUIVOS_CMS + "arquivos" + File.separatorChar + sitioCms.getNuSitio() + File.separatorChar);
        arquivista.setPastaDeArquivosTemplates(PASTA_BASE_ARQUIVOS_CMS + "templates" + File.separatorChar + sitioCms.getNuSitio() + File.separatorChar);
        arquivista.setPastaDePaginasERelatoriosDoSitio(System.getProperty("user.home") + File.separatorChar + "sitios" + File.separatorChar + sitioCms.getNuSitio() + File.separatorChar);
        String regex = urlInicial.replace("http", "(http|https)").replace("?", "\\?").replace(".", "\\.") + ".*";
        SitioPro sitioPro = new SitioPro();
        logger.info("regex=" + regex);
        sitioPro.setUrlRegex(regex);
        sitioPro.setUrlInicial(urlInicial);
        arquivista.setFiltroDeUrl(new FiltroDeUrl() {

            public boolean pertence(String url) {
                return url.contains(urlSitio.getHost());
            }
        });
        return sitioPro;
    }

    /**
	 * verifica se a linguagem existe no banco, caso nao exista cria
	 * @param url
	 * @return o id da linguagem
	 * @deprecated Use {@link br.org.acessobrasil.processoacessibilidade.bo.Analista#verificarLinguagem(String)} instead
	 */
    public Long verificarLinguagem(String url) {
        return analista.verificarLinguagem(url);
    }

    public Analista getAnalista() {
        return analista;
    }

    public void setAnalista(Analista analista) {
        this.analista = analista;
    }

    /**
	 * Importa os links de um arquivo colocando eles na tabela de paginas.<br>
	 * o arquivo deve conter apenas links separados por \n. Ex.:<br>
	 * <code>
	 * http://www.ibc.gov.br<br>
	 * http://www.bc.gov.br<br>
	 * http://www.governoeletronico.gov.br<br>
	 * </code>
	 * @param idSitio id do sitio para importar
	 * @param arquivo arquivo com o formato descrito acima
	 */
    public static void importarLinks(long idSitio, File arquivo) {
        G_File arq = new G_File(arquivo);
        String conteudo = arq.read();
        String urls[] = conteudo.split("\n");
        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            if (url.endsWith(".gov.br")) {
                url += "/";
            }
            PaginaPro pagina = paginaDao.find(url);
            if (pagina == null) {
                PaginaPro novaPagina = new PaginaPro();
                novaPagina.setIdSitio(idSitio);
                novaPagina.setUrl(url);
                paginaDao.create(novaPagina);
            }
        }
    }

    /**
	 * Recebe o template do Designer,
	 * Cadastra o template ou pega ele do banco<br>
	 * Salva como rascunho e com o nome do arquivo
	 * @param file
	 */
    public void setPadraoApresentacao(File file) {
        if (!file.exists()) {
            throw new RuntimeException("O arquivo '" + file.getAbsolutePath() + "' nao foi encontrado.");
        }
        G_File arq = new G_File(file);
        padraoApresentacao = new PadraoApresentacao();
        String strHtml = arq.read();
        padraoApresentacao.setDePadraoTemp(strHtml);
        if (sitioCms == null) {
            throw new NullPointerException("Falha ao criar o padrao de apresentacao, sitioCMS esta nulo utilize o setSitioCMS()!");
        } else {
            padraoApresentacao.setSitio(sitioCms);
        }
        List<PadraoApresentacao> padraoApresentacao2 = padraoApresentacaoDaoCMS.like(padraoApresentacao);
        if (padraoApresentacao2 == null || padraoApresentacao2.size() == 0) {
            padraoApresentacao.setNoPadrao(file.getName());
            padraoApresentacao.setStSituacao('r');
            padraoApresentacaoDaoCMS.create(padraoApresentacao);
            logger.info("Padrao de apresentacao persistido");
        } else {
            padraoApresentacao = padraoApresentacao2.get(0);
            if (padraoApresentacao.getSitio() == null) {
                padraoApresentacao.setSitio(sitioCms);
            }
            try {
                padraoApresentacaoDaoCMS.update(padraoApresentacao);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao atualiza o padrao");
            }
            logger.info("Padrao de apresentacao existente, reutilizando");
        }
    }

    /**
	 * Facilitador<br> 
	 * <code>
	 * {
	 * 	setSitioCMS(getSitioDaoCMS().select(id));
	 * }</code>
	 * @param id do sitio
	 */
    public void setSitioCMS(long id) {
        setSitioCms(getSitioDaoCMS().select(id));
    }

    /**
	 * Sitio do CMS o qual sera trabalhado
	 * @param sitioCMS
	 */
    public void setSitioCms(Sitio sitioCMS) {
        if (sitioCMS.getNuSitio() == null) {
            Sitio sitioT = getSitioDaoCMS().select(sitioCMS);
            if (sitioT == null) {
                try {
                    getSitioDaoCMS().create(sitioCMS);
                    instalarCms(sitioCMS);
                    logger.info("Criado o sitio " + sitioCMS.getNoSitio() + "#" + sitioCMS.getNuSitio());
                } catch (Exception e) {
                    logger.error("Erro ao criar o sitio ", e);
                }
            } else {
                sitioCMS = sitioT;
                logger.info("Utilizando sitio do banco " + sitioCMS.getNoSitio() + "#" + sitioCMS.getNuSitio());
            }
        }
        this.sitioCms = sitioCMS;
    }

    public static void instalarCms(Sitio sitioCMS) throws Exception {
        Instalador instalador = new Instalador(usuarioDaoCMS, perfilDaoCMS, setorDaoCMS, getSitioDaoCMS(), privilegioDaoCMS, paginaDaoCMS, padraoApresentacaoDaoCMS, menuDaoCMS, itemMenuDaoCMS, tipoConteudoDaoCMS, formatoDaoCMS, informacaoTipoConteudoDaoCMS, conteudoDaoCMS);
        instalador.execute();
        instalador.criarSitioBase(sitioCMS);
    }

    /**
	 * Remove publicacoes feitas no cms atraves do ConteudistaPro no periodo especificado.<br>
	 * Nao remove os arquivos cadastrados
	 * @param inicio
	 * @param fim
	 */
    public static void removerPublicacoesNoCms(Date inicio, Date fim) {
        try {
            List<ItemConteudo> listItemDeConteudo = itemConteudoDaoCMS.getListaItemConteudoPorUsuario(2L, inicio, fim);
            List<Conteudo> listConteudo = new ArrayList<Conteudo>();
            List<Pagina> listPagina = new ArrayList<Pagina>();
            logger.debug("Removendo itens de conteudo...");
            for (ItemConteudo itemConteudo : listItemDeConteudo) {
                listConteudo.add(itemConteudo.getConteudo());
                itemConteudoDaoCMS.apagar(itemConteudo);
            }
            logger.debug("Ok. Removendo conteudos ...");
            for (Conteudo conteudo : listConteudo) {
                listPagina.add(conteudo.getPagina());
                conteudoDaoCMS.apagar(conteudo.getNuConteudo());
            }
            logger.debug("Ok. Removendo paginas ...");
            for (Pagina pagina : listPagina) {
                paginaDaoCMS.delete(pagina);
            }
            logger.debug("Terminada a remocao de publicacoes com sucesso.");
        } catch (Exception e) {
            logger.error("Ocorreu um erro na remocao de publicacoes do Cms", e);
        }
    }

    public static PadraoApresentacao getPadraoApresentacao() {
        return padraoApresentacao;
    }

    public static SitioDaoSpring getSitioDaoCMS() {
        return sitioDaoCMS;
    }

    public FiltroDeUrl getFiltroDeUrl() {
        return filtroDeUrl;
    }

    public void setFiltroDeUrl(FiltroDeUrl filtroDeUrl) {
        this.filtroDeUrl = filtroDeUrl;
    }

    public static ConteudoDaoSpring getConteudoDaoCMS() {
        return conteudoDaoCMS;
    }

    public static ItemConteudoDaoSpring getItemConteudoDaoCMS() {
        return itemConteudoDaoCMS;
    }

    public static InformacaoTipoConteudoDao getInformacaoTipoConteudoDaoCMS() {
        return informacaoTipoConteudoDaoCMS;
    }

    public static ConteudoFormatadoDao getConteudoFormatadoDaoCMS() {
        return ConteudoFormatadoDaoCMS;
    }

    public static LinguagemProgramacaoDao getLinguagemProgramacaoDao() {
        return linguagemProgramacaoDao;
    }

    public static PaginaDaoSpring getPaginaDaoCMS() {
        return paginaDaoCMS;
    }

    public static TipoConteudoDaoSpring getTipoConteudoDaoCMS() {
        return tipoConteudoDaoCMS;
    }

    public Sitio getSitioCms() {
        return sitioCms;
    }

    public static TemplateDao getTemplateDao() {
        return templateDao;
    }

    public static ConteudoDao getConteudoDao() {
        return conteudoDao;
    }

    public static ArquivoDaoSpring getArquivoDaoCMS() {
        return arquivoDaoCMS;
    }

    public static void setArquivoDaoCMS(ArquivoDaoSpring arquivoDaoCMS) {
        Coordenador.arquivoDaoCMS = arquivoDaoCMS;
    }

    public static SetorDao getSetorDaoCMS() {
        return setorDaoCMS;
    }
}
