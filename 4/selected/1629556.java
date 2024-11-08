package br.ufrn.cerescaico.sepe.actions;

import br.ufrn.cerescaico.sepe.beans.Area;
import br.ufrn.cerescaico.sepe.beans.Atividade;
import br.ufrn.cerescaico.sepe.beans.Categoria;
import br.ufrn.cerescaico.sepe.beans.Trabalho;
import br.ufrn.cerescaico.sepe.bo.SepeException;
import br.ufrn.cerescaico.sepe.email.MailManager;
import br.ufrn.cerescaico.sepe.email.MailMessage;
import br.ufrn.cerescaico.sepe.util.RandomAlphaNumeric;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Taciano
 */
public class SubmissaoAction extends SepeAction {

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(SubmissaoAction.class);

    private Categoria categoria;

    private List<Categoria> categoriasTrabalho;

    private List<Categoria> categoriasAtividade;

    private Area area;

    private List<Area> areas;

    private Trabalho trabalho;

    private Atividade atividade;

    private final String SUBMETER_TRABALHO = "submeterTrabalho";

    private final String SUBMETER_ATIVIDADE = "submeterAtividade";

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private String fileCaption;

    public SubmissaoAction() {
        try {
            setCategoriasTrabalho(getSepe().listarCategoriasTrabalhos());
            setCategoriasAtividade(getSepe().listarCategoriasAtividades());
            setAreas(getSepe().listarAreas());
        } catch (SepeException ex) {
            setCategoriasTrabalho(new ArrayList<Categoria>());
            setCategoriasAtividade(new ArrayList<Categoria>());
            setAreas(new ArrayList<Area>());
            logger.error(getText(ex.getMessage()), ex);
            addActionError(getText(ex.getMessage()));
        }
    }

    /**
     * Verifica a entrada do formulário de registro e encaminha para a página
     * principal se não houver erro.
     * @see com.opensymphony.xwork2.ActionSupport#execute()
     */
    @Override
    public String execute() throws Exception {
        logger.info("execute()");
        return SUCCESS;
    }

    private byte[] copiarArquivo(String nomeArquivo) throws IOException {
        byte[] arquivo = getBytesFromFile(upload);
        copyFile(upload, new File(nomeArquivo));
        return arquivo;
    }

    private String definirNomeArquivoTrabalho() {
        String nomeArquivo = "/home/sepe/uploads/trabalhos/" + trabalho.getPrimeiroAutorEmail() + " -" + RandomAlphaNumeric.randomString(3, 5) + "- " + uploadFileName;
        return nomeArquivo;
    }

    private String definirNomeArquivoAtividade() {
        String nomeArquivo = "/home/sepe/uploads/atividades/" + atividade.getProponenteEmail() + " -" + RandomAlphaNumeric.randomString(3, 5) + "- " + uploadFileName;
        return nomeArquivo;
    }

    private void preencherDadosTrabalho() throws SepeException {
        setCategoriasTrabalho(getSepe().listarCategoriasTrabalhos());
        setAreas(getSepe().listarAreas());
    }

    private void preencherDadosAtiviadade() throws SepeException {
        setCategoriasAtividade(getSepe().listarCategoriasAtividades());
        setAreas(getSepe().listarAreas());
    }

    public String cadastrarTrabalho() throws Exception {
        logger.info("cadastrarTrabalho");
        preencherDadosTrabalho();
        return SUBMETER_TRABALHO;
    }

    public String submeterTrabalho() throws Exception {
        logger.info("submeterTrabalho");
        preencherDadosTrabalho();
        try {
            if (getArea() != null) {
                trabalho.setArea(getArea().getNome());
            }
            if (getCategoria() != null) {
                trabalho.setCategoria(getCategoria().getNome());
            }
            getTrabalho().getParticipante().setEmail(getTrabalho().getPrimeiroAutorEmail());
            getTrabalho().getParticipante().setNome(getTrabalho().getPrimeiroAutorNome());
            verificarDados();
            String nomeArquivo = definirNomeArquivoTrabalho();
            byte[] arquivo = copiarArquivo(nomeArquivo);
            trabalho.setTrabalho(arquivo);
            if (trabalho.getTrabalho() == null) {
                throw new SepeException("erro.bo.trabalho.autor.trabalho");
            }
            trabalho.setFileTrabalho(nomeArquivo);
            getSepe().submeterTrabalho(getTrabalho());
            addActionMessage(getText("view.submissao.action.trabalho.cadastrado"));
        } catch (SepeException ex) {
            logger.error(getText(ex.getMessage()), ex);
            addActionError(getText(ex.getMessage()));
        }
        return SUBMETER_TRABALHO;
    }

    private void verificarDados() throws SepeException {
        if (upload == null) {
            throw new SepeException("erro.submissao.action.upload");
        }
        if (trabalho == null || trabalho.getPrimeiroAutorEmail() == null || "".equals(trabalho.getPrimeiroAutorEmail().trim())) {
            throw new SepeException("erro.submissao.action.autor.email");
        }
        if (trabalho.getPrimeiroAutorNome() == null || "".equals(trabalho.getPrimeiroAutorNome().trim())) {
            throw new SepeException("erro.submissao.action.trabalho.autor.nome");
        }
        if (trabalho.getTitulo() == null || "".equals(trabalho.getTitulo().trim())) {
            throw new SepeException("erro.bo.trabalho.titulo");
        }
        if (trabalho.getCategoria() == null || "".equals(trabalho.getCategoria().trim()) || "0".equals(trabalho.getCategoria().trim())) {
            throw new SepeException("erro.bo.trabalho.categoria");
        }
        if (trabalho.getArea() == null || "".equals(trabalho.getArea().trim()) || "0".equals(trabalho.getArea().trim())) {
            throw new SepeException("erro.bo.trabalho.area");
        }
        if (trabalho.getParticipante() == null) {
            throw new SepeException("erro.bo.trabalho.participante.null");
        }
        if (trabalho.getParticipante().getEmail() == null || "".equals(trabalho.getParticipante().getEmail().trim())) {
            throw new SepeException("erro.bo.trabalho.participante.email");
        }
        if (trabalho.getParticipante().getSenha() == null || "".equals(trabalho.getParticipante().getSenha().trim())) {
            throw new SepeException("erro.bo.trabalho.participante.senha");
        }
        if (trabalho.getPalavrasChave() == null || "".equals(trabalho.getPalavrasChave().trim())) {
            throw new SepeException("erro.bo.trabalho.palavraschave");
        }
    }

    public String cadastrarAtividade() throws Exception {
        logger.info("cadastrarAtividade");
        preencherDadosAtiviadade();
        return SUBMETER_ATIVIDADE;
    }

    public String submeterAtividade() throws Exception {
        logger.info("submeterAtividade");
        preencherDadosAtiviadade();
        if (getArea() != null) {
            atividade.setArea(getArea().getNome());
        }
        if (getCategoria() != null) {
            atividade.setCategoria(getCategoria().getNome());
        }
        try {
            getAtividade().getParticipante().setNome(getAtividade().getProponente());
            getAtividade().getParticipante().setEmail(getAtividade().getProponenteEmail());
            verificarDadosAtividade();
            String nomeArquivo = definirNomeArquivoAtividade();
            byte[] arquivo = copiarArquivo(nomeArquivo);
            getAtividade().setArquivo(arquivo);
            getAtividade().setNomeArquivo(nomeArquivo);
            if (atividade.getArquivo() == null) {
                throw new SepeException("erro.bo.atividade.autor.trabalho");
            }
            getSepe().submeterAtividade(getAtividade());
            addActionMessage(getText("view.submissao.action.atividade.cadastrada"));
        } catch (SepeException ex) {
            logger.error(getText(ex.getMessage()), ex);
            addActionError(getText(ex.getMessage()));
        }
        return SUBMETER_ATIVIDADE;
    }

    private void verificarDadosAtividade() throws SepeException {
        if (upload == null) {
            throw new SepeException("erro.submissao.action.upload");
        }
        if (atividade == null || atividade.getProponenteEmail() == null || "".equals(atividade.getProponenteEmail().trim())) {
            throw new SepeException("erro.submissao.action.proponente.email");
        }
        if (atividade.getProponente() == null || "".equals(atividade.getProponente().trim())) {
            throw new SepeException("erro.bo.atividade.autor.nome");
        }
        if (atividade.getTitulo() == null || "".equals(atividade.getTitulo().trim())) {
            throw new SepeException("erro.bo.atividade.titulo");
        }
        if (atividade.getCategoria() == null || "".equals(atividade.getCategoria().trim()) || "0".equals(atividade.getCategoria().trim())) {
            throw new SepeException("erro.bo.atividade.categoria");
        }
        if (atividade.getArea() == null || "".equals(atividade.getArea().trim()) || "0".equals(atividade.getArea().trim())) {
            throw new SepeException("erro.bo.atividade.area");
        }
        if (atividade.getParticipante() == null) {
            throw new SepeException("erro.bo.atividade.participante.null");
        }
        if (atividade.getParticipante().getEmail() == null || "".equals(atividade.getParticipante().getEmail().trim())) {
            throw new SepeException("erro.bo.atividade.participante.email");
        }
        if (atividade.getParticipante().getSenha() == null || "".equals(atividade.getParticipante().getSenha().trim())) {
            throw new SepeException("erro.bo.atividade.participante.senha");
        }
    }

    public String enviarEmail() throws Exception {
        MailMessage email = new MailMessage();
        email.setTo(trabalho.getPrimeiroAutorEmail());
        email.setSubject("II Semana de Ensino, Pesquisa e Extensão - CERES/UFRN");
        email.setText(trabalho.getTitulo());
        MailManager m = new MailManager();
        try {
            m.send(email);
            addActionMessage(getText("view.submissao.email.enviado"));
        } catch (Exception ex) {
            logger.error(getText("view.submissao.email.naoenviado"), ex);
            addActionError(getText("view.submissao.email.naoenviado"));
        }
        return execute();
    }

    public Trabalho getTrabalho() {
        return trabalho;
    }

    public void setTrabalho(Trabalho trabalho) {
        this.trabalho = trabalho;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public void setAtividade(Atividade atividade) {
        this.atividade = atividade;
    }

    public List<Categoria> getCategoriasTrabalho() {
        return categoriasTrabalho;
    }

    public final void setCategoriasTrabalho(List<Categoria> categorias) {
        this.categoriasTrabalho = categorias;
    }

    public List<Categoria> getCategoriasAtividade() {
        return categoriasAtividade;
    }

    public final void setCategoriasAtividade(List<Categoria> categorias) {
        this.categoriasAtividade = categorias;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public final void setAreas(List<Area> areas) {
        this.areas = areas;
    }

    public String getFileCaption() {
        return fileCaption;
    }

    public void setFileCaption(String fileCaption) {
        this.fileCaption = fileCaption;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    private static void copyFile(File in, File out) {
        try {
            FileChannel sourceChannel = new FileInputStream(in).getChannel();
            FileChannel destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            sourceChannel.close();
            destinationChannel.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
