package br.org.acessobrasil.portal.action;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import br.org.acessobrasil.portal.controle.SecurityManager;
import br.org.acessobrasil.portal.modelo.Arquivo;
import br.org.acessobrasil.portal.modelo.Conteudo;
import br.org.acessobrasil.portal.modelo.InformacaoTipoConteudo;
import br.org.acessobrasil.portal.modelo.ItemConteudo;
import br.org.acessobrasil.portal.modelo.Setor;
import br.org.acessobrasil.portal.modelo.Sitio;
import br.org.acessobrasil.portal.modelo.Usuario;
import br.org.acessobrasil.portal.persistencia.ArquivoDao;
import br.org.acessobrasil.portal.persistencia.LogDao;
import br.org.acessobrasil.portal.persistencia.SetorDao;
import br.org.acessobrasil.portal.util.StringUtils;

/**
 * TODO Limpar os arquivos temporarios ou colocar no temp do sistema
 * Gerencia os arquivos, como imagens, pdfs, etc
 * @author Fabio Issamu Oshiro
 *
 */
public class ArquivoAction extends Super {

    private static Logger logger = Logger.getLogger(ArquivoAction.class);

    private static final long serialVersionUID = 1293801945334863313L;

    private static ArquivoDao arquivoDao;

    private List<Arquivo> listArquivo;

    /**
	 * colocar o path da pasta arquivo para ser configurado pelo servidor
	 */
    private static String arquivosPath = null;

    private Arquivo arquivo;

    private String buscar;

    /**
	 * Flag para indicar se esta escolhendo um arquivo 
	 */
    private String escolhendo;

    private Integer nu_item_conteudo, nu_conteudo, nu_informacao_tipo_conteudo;

    private Conteudo conteudo;

    private ItemConteudo itemConteudo;

    private InformacaoTipoConteudo informacaoTipoConteudo;

    private String atalho;

    private File fileUpload;

    private String fileUploadContentType;

    private String fileUploadFileName;

    private String descricao;

    private String longdesc;

    private String noArquivo;

    private LogDao logDao = LogDao.getInstance();

    private static final String SUBSTITUIR = "Deseja substituir o arquivo '{fileUploadFileName}' existente?";

    private List<Setor> listSetor;

    private static SetorDao setorDao;

    private static String extensoesPermitidas[] = { "jpg", "gif", "doc", "bmp", "pdf", "xls", "swf", "zip", "png" };

    /**
	 * Path fixo,
	 * colocar o path da pasta arquivo para ser configurado pelo servidor
	 */
    public String getArquivosPath() {
        return arquivosPath;
    }

    /**
	 * Path fixo, colocar o path da pasta "arquivos" para ser configurado pelo servidor
	 * @param arquivosPath caminho
	 */
    public void setArquivosPath(String arquivosPath) {
        if (!arquivosPath.endsWith("/")) arquivosPath += "/";
        ArquivoAction.arquivosPath = arquivosPath;
    }

    /**
	 * Retorna o arquivo fisico dentro da pasta ArquivoAction.arquivosPath.<br>
	 * Eg. /home/acbr/cmsarquivos/nomeDaPastaDoSitio/caminhoRelativo
	 * 
	 * @param sitio Site
	 * @param caminhoRelativo caminho
	 * @return File 
	 */
    public static File getFile(Sitio sitio, String caminhoRelativo) {
        if (sitio.getNoPastaArquivos() == null) throw new NullPointerException("o nome da pasta esta nula");
        return new File(ArquivoAction.arquivosPath + sitio.getNoPastaArquivos() + File.separatorChar + caminhoRelativo);
    }

    public void setConteudo(Conteudo conteudo) {
        this.conteudo = conteudo;
    }

    public Conteudo getConteudo() {
        return conteudo;
    }

    public ItemConteudo getItemConteudo() {
        return itemConteudo;
    }

    public void setItemConteudo(ItemConteudo itemConteudo) {
        this.itemConteudo = itemConteudo;
    }

    public InformacaoTipoConteudo getInformacaoTipoConteudo() {
        return informacaoTipoConteudo;
    }

    public void setInformacaoTipoConteudo(InformacaoTipoConteudo informacaoTipoConteudo) {
        this.informacaoTipoConteudo = informacaoTipoConteudo;
    }

    /**
	 * Todo mundo pode alterar
	 */
    public static final int TODOS = 0;

    /**
	 * Somente pessoas do mesmo setor
	 */
    public static final int SETOR = 1;

    /**
	 * Somente o dono e o administrador
	 */
    public static final int DONO = 2;

    private int nuPermissao = SETOR;

    public ArquivoAction() {
    }

    /**
	 * Spring acessa este construtor sem estar mapeado no applicationContext
	 * @param arquivoDao
	 * @param setorDao
	 */
    public ArquivoAction(ArquivoDao arquivoDao, SetorDao setorDao) {
        ArquivoAction.arquivoDao = arquivoDao;
        ArquivoAction.setorDao = setorDao;
    }

    /**
	 * 
	 * @param nomeArquivo nome do arquivo
	 * @return a extensao do arquivo
	 */
    private String retornarExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf(".");
        if (ponto != -1) return nomeArquivo.substring(ponto + 1).toLowerCase();
        return null;
    }

    /**
	 * Cadastra todos os arquivos que nao estiverem no banco de dados
	 * @return sucesso
	 */
    public String cadastrarArquivosNaoCadastrados() {
        int totalCadastrado = 0;
        Sitio sitio = getSitioAtual();
        Usuario usuario = getUsuarioLogado();
        Setor setor = usuario.getSetor();
        File dirArqSitio = new File(getArquivosPath() + sitio.getNoPastaArquivos() + File.separatorChar + "arquivos");
        for (String subDir : extensoesPermitidas) {
            File dir = new File(dirArqSitio, subDir);
            if (!dir.exists()) {
                logger.debug("Diretorio nao existe " + dir.getAbsolutePath());
                continue;
            }
            File[] files = dir.listFiles();
            for (File arq : files) {
                boolean cadastrar = true;
                String noArquivo = "arquivos/" + subDir + "/" + arq.getName();
                try {
                    List<Arquivo> list = arquivoDao.procurar('/' + arq.getName(), sitio);
                    if (list.size() != 0) {
                        for (Arquivo arquivo : list) {
                            if (arquivo.getNoArquivo().endsWith(arq.getName())) {
                                cadastrar = false;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                }
                if (arq.isDirectory()) {
                    cadastrar = false;
                } else if (!arq.getName().toLowerCase().endsWith("." + subDir)) {
                    cadastrar = false;
                }
                if (cadastrar) {
                    Arquivo arquivo = new Arquivo();
                    arquivo.setDeArquivo("Arquivo cadastrado pelo sistema.");
                    arquivo.setNoArquivo(noArquivo);
                    arquivo.setNuUsuario(usuario.getNuUsuario());
                    arquivo.setNuSetor(setor.getNuSetor());
                    arquivo.setSitio(sitio);
                    arquivo.setNuPermissao(TODOS);
                    try {
                        arquivoDao.create(arquivo);
                        totalCadastrado++;
                    } catch (Exception e) {
                        logger.error("Erro ao cadastrar arquivo " + noArquivo, e);
                    }
                }
            }
        }
        usuario.addActionMessage("O sistema cadastrou " + totalCadastrado + " arquivo(s).");
        return listar();
    }

    /**
	 * 
	 * @param extensao
	 * @return true se a extensao for permitida
	 */
    private boolean extensaoPermitida(String extensao) {
        for (int i = 0; i < extensoesPermitidas.length; i++) {
            if (extensao.equals(extensoesPermitidas[i])) return true;
        }
        return false;
    }

    /**
	 * Sobre escreve um arquivo, acontece quando um usuario sobe um arquivo com o mesmo nome
	 * @return SUCCESS
	 */
    public String substituirArquivo() {
        Usuario usuario = getUsuarioLogado();
        try {
            if (btnCancelar == null) {
                String extensao = retornarExtensao(noArquivo);
                Sitio sitio = getSitioAtual();
                String nomeRelativo = "arquivos/" + extensao + "/" + noArquivo;
                String nomeRelativoFisico = sitio.getNoPastaArquivos() + "/arquivos/" + extensao + "/" + noArquivo;
                Arquivo arquivoDB = arquivoDao.select(nomeRelativo, getSitioAtual());
                if (arquivoDB != null && descricao != null && !descricao.equals("") && !descricao.equals("Digite a descri��o")) {
                    arquivoDB.setDeArquivo(descricao);
                    arquivoDao.update(arquivoDB);
                }
                if (getArquivosPath() != null) {
                    File tempFile = new File(getArquivosPath() + "arquivos/temp/" + noArquivo);
                    File destFile = new File(getArquivosPath() + nomeRelativoFisico);
                    FileUtils.copyFile(tempFile, destFile);
                } else {
                    File tempFile = new File(getServletContext().getRealPath("arquivos/temp/" + noArquivo));
                    File destFile = new File(getServletContext().getRealPath(nomeRelativoFisico));
                    FileUtils.copyFile(tempFile, destFile);
                }
                usuario.addActionMessage("Arquivo substituido.");
                logDao.addLog(getUsuarioLogado(), "Substituiu o arquivo '" + nomeRelativo + "'.");
            }
        } catch (Exception e) {
            usuario.addActionError(e.getMessage());
        }
        return SUCCESS;
    }

    /**
	 * Cadastra o arquivo
	 * @return SUCCESS ou INPUT
	 */
    public String adicionarArquivo() {
        Usuario usuarioLogado = getUsuarioLogado();
        if (descricao != null && (fileUpload == null || fileUploadFileName == null || fileUploadFileName.equals(""))) {
            usuarioLogado.addActionError("Por favor, preencha o campo arquivo.");
        }
        if (fileUpload != null && !fileUploadFileName.equals("")) {
            try {
                fileUploadFileName = StringUtils.removeAcentos(fileUploadFileName).replace(" ", "-");
                String extensao = retornarExtensao(fileUploadFileName);
                if (!extensaoPermitida(extensao)) {
                    throw new Exception("Tipo de arquivo n&atilde;o permitido.");
                }
                if (descricao == null || descricao.equals("") || descricao.equals(getText("predefinido.descricao"))) {
                    throw new Exception("Por favor, preencha o campo descri&ccedil;&atilde;o.");
                }
                if (longdesc == null || longdesc.equals("") || longdesc.equals(getText("predefinido.descricao")) || longdesc.equals("<p>" + getText("predefinido.descricao") + "</p>")) {
                    longdesc = null;
                }
                Sitio sitio = getSitioAtual();
                String nomeRelativo = "arquivos/" + extensao + "/" + fileUploadFileName;
                String nomeRelativoFisico = sitio.getNoPastaArquivos() + "/arquivos/" + extensao + "/" + fileUploadFileName;
                Arquivo arquivoDB = arquivoDao.select(nomeRelativo, getSitioAtual());
                if (arquivoDB != null) {
                    if (SecurityManager.podeAlterarArquivo(usuarioLogado, arquivoDB)) {
                        File theFile = null;
                        if (getArquivosPath() != null) theFile = new File(getArquivosPath() + "arquivos/temp/" + fileUploadFileName); else theFile = new File(getServletContext().getRealPath("arquivos/temp/" + fileUploadFileName));
                        FileUtils.copyFile(fileUpload, theFile);
                        usuarioLogado.addActionMessage(SUBSTITUIR.replace("{fileUploadFileName}", fileUploadFileName));
                        noArquivo = fileUploadFileName;
                        return "substituir";
                    } else {
                        if (arquivoDB.getNuPermissao() == SETOR) {
                            usuarioLogado.addActionError("J&aacute; existe um arquivo com este nome. Somente usu&aacute;rios do mesmo setor podem alterar este arquivo.");
                        } else {
                            usuarioLogado.addActionError("J&aacute; existe um arquivo com este nome. N&atilde;o &eacute; poss&iacute;vel substituir este arquivo.");
                        }
                        return INPUT;
                    }
                }
                String fullFileName = null;
                if (getArquivosPath() != null) {
                    fullFileName = getArquivosPath() + nomeRelativoFisico;
                } else {
                    fullFileName = getServletContext().getRealPath(nomeRelativoFisico);
                }
                File theFile = new File(fullFileName);
                FileUtils.copyFile(fileUpload, theFile);
                logger.debug("fullFileName=" + fullFileName);
                arquivo = new Arquivo();
                arquivo.setNoArquivo(nomeRelativo);
                arquivo.setDeArquivo(descricao);
                arquivo.setLongdesc(longdesc);
                arquivo.setNuUsuario(usuarioLogado.getNuUsuario());
                arquivo.setNuSetor(usuarioLogado.getSetor().getNuSetor());
                arquivo.setNuPermissao(nuPermissao);
                arquivo.setSitio(getSitioAtual());
                if (arquivoDB == null) {
                    arquivoDao.create(arquivo);
                    usuarioLogado.addActionMessage("Arquivo cadastrado corretamente.");
                    logDao.addLog(getUsuarioLogado(), "Cadastrou o arquivo '" + nomeRelativo + "'.");
                } else {
                    arquivoDB.setDeArquivo(descricao);
                    arquivoDao.update(arquivoDB);
                    usuarioLogado.addActionMessage("Arquivo atualizado corretamente.");
                    logDao.addLog(getUsuarioLogado(), "Substituiu o arquivo '" + nomeRelativo + "'.");
                }
            } catch (Exception e) {
                usuarioLogado.addActionError(e.getMessage());
                return INPUT;
            }
        }
        return SUCCESS;
    }

    /**
	 * popula a lista de arquivos
	 * @return SUCESSO
	 */
    public String listar() {
        try {
            if (buscar != null) {
                listArquivo = arquivoDao.procurar(buscar, getSitioAtual());
            } else {
                listArquivo = arquivoDao.listar(getSitioAtual());
            }
        } catch (Exception e) {
            logger.error("Erro ao listar arquivos de " + getSitioAtual(), e);
        }
        return SUCCESS;
    }

    /**
	 * Retorna true se a extensao for de uma imagem conhecida<br>
	 *  {"jpg","gif","bmp","png"}
	 * @param extensao ex.: "gif"
	 * @return true se a extensao for de uma imagem conhecida
	 */
    public boolean tipoImagem(String extensao) {
        String permitidos[] = { "jpg", "gif", "bmp", "png" };
        for (int i = 0; i < permitidos.length; i++) {
            if (extensao.equals(permitidos[i])) return true;
        }
        return false;
    }

    /**
	 * Carrega o atributo atalho
	 */
    private void carregarAtalho() {
        if (tipoImagem(retornarExtensao(arquivo.getNoArquivo()))) {
            atalho = "<img src=\"../" + arquivo.getNoArquivo() + "\" alt=\"Descri&ccedil;&atilde;o\" />";
        } else {
            atalho = "<a href=\"../" + arquivo.getNoArquivo() + "\">Baixar arquivo</a>";
        }
    }

    /**
	 * Carrega o arquivo especificado do DB
	 * @return SUCCESS
	 */
    public String carregarArquivo() {
        try {
            arquivo = arquivoDao.select(arquivo.getNuArquivo());
            carregarAtalho();
        } catch (Exception e) {
            logger.error("Erro ao carregar o arquivo " + arquivo, e);
        }
        return SUCCESS;
    }

    /**
	 * Apaga um arquivo se o usuario tiver permissao para isso
	 * @return SUCCESS
	 */
    public String apagarArquivo() {
        Usuario usuario = getUsuarioLogado();
        try {
            if (btnCancelar == null) {
                Sitio sitio = getSitioAtual();
                arquivo = arquivoDao.select(arquivo.getNuArquivo());
                if (SecurityManager.podeAlterarArquivo(usuario, arquivo)) {
                    String logMsg = "Apagou o arquivo '" + arquivo.getNoArquivo() + "'.";
                    String arqFullPath = null;
                    File file = new File(arquivo.getNoArquivo());
                    String extensao = retornarExtensao(file.getName());
                    if (getArquivosPath() != null) {
                        arqFullPath = getArquivosPath() + sitio.getNoPastaArquivos() + "/arquivos/" + extensao + "/" + file.getName();
                    } else {
                        arqFullPath = getServletContext().getRealPath(sitio.getNoPastaArquivos() + "/arquivos/" + extensao + "/" + file.getName());
                    }
                    File arq = new File(arqFullPath);
                    if (!arq.exists()) {
                        arquivoDao.apagar(arquivo);
                        logDao.addLog(usuario, logMsg);
                        usuario.addActionError("Este arquivo n&atilde;o existe fisicamente e foi apagado somente do banco de dados.");
                        return INPUT;
                    } else {
                        arquivoDao.apagar(arquivo);
                        arq.delete();
                        logDao.addLog(usuario, logMsg);
                    }
                } else {
                    usuario.addActionError("Voc&ecirc; n&atilde;o pode apagar este arquivo, ele pertence a outro setor.");
                    return INPUT;
                }
            }
        } catch (Exception e) {
            usuario.addActionError(e.getMessage());
            logger.error("Erro ao apagar o arquivo " + arquivo, e);
        }
        return SUCCESS;
    }

    /**
	 * Atualiza um arquivo
	 * @return SUCCESS
	 */
    public String atualizarArquivo() {
        Usuario usuario = getUsuarioLogado();
        try {
            Arquivo arquivoDB = arquivoDao.select(arquivo.getNuArquivo());
            if (SecurityManager.podeAlterarArquivo(usuario, arquivoDB)) {
                if (arquivo.getNuSetor() != null && !arquivo.getNuSetor().equals(0L)) {
                    arquivoDB.setNuSetor(arquivo.getNuSetor());
                }
                if (arquivo.getLongdesc() == null || arquivo.getLongdesc().equals("") || arquivo.getLongdesc().equals(getText("predefinido.descricao"))) {
                    arquivo.setLongdesc(null);
                }
                arquivoDB.setDeArquivo(arquivo.getDeArquivo());
                arquivoDB.setLongdesc(arquivo.getLongdesc());
                arquivoDao.update(arquivoDB);
                arquivo = arquivoDB;
                carregarAtalho();
                usuario.addActionMessage("Descri&ccedil;&atilde;o atualizada.");
                logDao.addLog(usuario, "Atualizou a descri&ccedil;&atilde;o do arquivo '" + arquivoDB.getNoArquivo() + "'");
            } else {
                usuario.addActionError("Voc&ecirc; n&atilde;o pode alterar a descri&ccedil;&atilde;o deste arquivo.");
            }
        } catch (Exception e) {
            usuario.addActionError(e.getMessage());
        }
        return SUCCESS;
    }

    public List<Arquivo> getListArquivo() {
        return listArquivo;
    }

    public void setListArquivo(List<Arquivo> listArquivo) {
        this.listArquivo = listArquivo;
    }

    public Arquivo getArquivo() {
        return arquivo;
    }

    public void setArquivo(Arquivo arquivo) {
        this.arquivo = arquivo;
    }

    public String getBuscar() {
        if (buscar == null) buscar = "Palavra chave";
        return buscar;
    }

    public void setBuscar(String buscar) {
        this.buscar = buscar;
    }

    public String getEscolhendo() {
        return escolhendo;
    }

    public void setEscolhendo(String escolhendo) {
        this.escolhendo = escolhendo;
    }

    public Integer getNu_item_conteudo() {
        return nu_item_conteudo;
    }

    public void setNu_item_conteudo(Integer nu_item_conteudo) {
        this.nu_item_conteudo = nu_item_conteudo;
    }

    public Integer getNu_conteudo() {
        return nu_conteudo;
    }

    public void setNu_conteudo(Integer nu_conteudo) {
        this.nu_conteudo = nu_conteudo;
    }

    public Integer getNu_informacao_tipo_conteudo() {
        return nu_informacao_tipo_conteudo;
    }

    public void setNu_informacao_tipo_conteudo(Integer nu_informacao_tipo_conteudo) {
        this.nu_informacao_tipo_conteudo = nu_informacao_tipo_conteudo;
    }

    public File getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(File fileUpload) {
        this.fileUpload = fileUpload;
    }

    public String getFileUploadContentType() {
        return fileUploadContentType;
    }

    public void setFileUploadContentType(String fileUploadContentType) {
        this.fileUploadContentType = fileUploadContentType;
    }

    public String getFileUploadFileName() {
        return fileUploadFileName;
    }

    public void setFileUploadFileName(String fileUploadFileName) {
        this.fileUploadFileName = fileUploadFileName;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getAtalho() {
        return atalho;
    }

    public void setAtalho(String atalho) {
        this.atalho = atalho;
    }

    public int getNuPermissao() {
        return nuPermissao;
    }

    public void setNuPermissao(int nuPermissao) {
        this.nuPermissao = nuPermissao;
    }

    public String getNoArquivo() {
        return noArquivo;
    }

    public void setNoArquivo(String noArquivo) {
        this.noArquivo = noArquivo;
    }

    public List<Setor> getListSetor() {
        if (listSetor == null) {
            listSetor = setorDao.getListSetor();
        }
        return listSetor;
    }

    /**
	 * Calcula o tamanho do arquivo
	 * @param valor nome relativo do arquivo
	 * @param sitio dono do arquivo
	 * @return String 2k 1M 1G
	 */
    public String getHumanFileSize(String valor, Sitio sitio) {
        String extensao = retornarExtensao(valor);
        File file = new File(valor);
        String arqFullPath = getArquivosPath() + sitio.getNoPastaArquivos() + "/arquivos/" + extensao + "/" + file.getName();
        file = new File(arqFullPath);
        if (file.exists()) {
            long len = file.length();
            double resposta = len;
            NumberFormat formatter = new DecimalFormat("#0.0");
            if (len > 1024) {
                resposta = resposta / 1024.0;
                if (resposta > 1024) {
                    resposta = resposta / 1024.0;
                    if (resposta > 1024) {
                        return formatter.format(resposta) + " GB";
                    } else {
                        return formatter.format(resposta) + " MB";
                    }
                } else {
                    return formatter.format(resposta) + " KB";
                }
            } else {
                return formatter.format(resposta) + " B";
            }
        }
        return " 0KB";
    }

    public String getLongdesc() {
        return longdesc;
    }

    public void setLongdesc(String longdesc) {
        this.longdesc = longdesc;
    }
}
