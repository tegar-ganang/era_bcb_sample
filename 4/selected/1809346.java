package br.gov.demoiselle.escola.business.implementation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import br.gov.component.demoiselle.authorization.AuthorizationException;
import br.gov.component.demoiselle.authorization.RequiredRole;
import br.gov.demoiselle.escola.bean.Aluno;
import br.gov.demoiselle.escola.bean.Email;
import br.gov.demoiselle.escola.bean.Endereco;
import br.gov.demoiselle.escola.bean.Foto;
import br.gov.demoiselle.escola.bean.Telefone;
import br.gov.demoiselle.escola.bean.Turma;
import br.gov.demoiselle.escola.bean.Usuario;
import br.gov.demoiselle.escola.business.IAlunoBC;
import br.gov.demoiselle.escola.business.IUsuarioBC;
import br.gov.demoiselle.escola.config.EscolaConfig;
import br.gov.demoiselle.escola.constant.AliasRole;
import br.gov.demoiselle.escola.message.ErrorMessage;
import br.gov.demoiselle.escola.message.InfoMessage;
import br.gov.demoiselle.escola.persistence.dao.IAlunoDAO;
import br.gov.framework.demoiselle.core.context.ContextLocator;
import br.gov.framework.demoiselle.core.exception.ApplicationRuntimeException;
import br.gov.framework.demoiselle.core.layer.integration.Injection;
import br.gov.framework.demoiselle.util.page.Page;
import br.gov.framework.demoiselle.util.page.PagedResult;
import br.gov.framework.demoiselle.web.message.WebMessageContext;

public class AlunoBC implements IAlunoBC {

    @Injection
    IAlunoDAO alunoDao;

    @Injection
    IUsuarioBC usuarioBC;

    @RequiredRole(roles = AliasRole.ROLE_ADMINISTRADOR)
    public void inserir(Aluno aluno, Foto foto) {
        try {
            Usuario usuario = aluno.getUsuario();
            usuarioBC.inserir(usuario);
            aluno.setUsuario(usuario);
            alunoDao.insert(aluno);
            salvarFoto(aluno, foto);
            ContextLocator.getInstance().getMessageContext().addMessage(InfoMessage.ALUNO_INSERIDO_OK);
        } catch (Exception e) {
            throw new ApplicationRuntimeException(ErrorMessage.ALUNO_001, e);
        }
    }

    @RequiredRole(roles = AliasRole.ROLE_ADMINISTRADOR)
    public void remover(Aluno arg0) {
        alunoDao.remove(arg0);
    }

    @RequiredRole(roles = AliasRole.ROLE_ADMINISTRADOR)
    public void alterar(Aluno aluno, Foto foto) throws AuthorizationException {
        salvarFoto(aluno, foto);
        alunoDao.update(aluno);
    }

    public List<Aluno> listar() {
        return alunoDao.listarAluno();
    }

    /**
	 * Lista paginada
	 */
    public PagedResult<Aluno> listar(Page page) {
        return alunoDao.listarAluno(page);
    }

    public List<Aluno> filtrar(Aluno arg0) {
        return alunoDao.filtrarAluno(arg0);
    }

    public PagedResult<Aluno> filtrar(String nome, Page page) {
        Aluno alunofiltro = new Aluno();
        alunofiltro.setNome(nome);
        return alunoDao.filtrarAluno(alunofiltro, page);
    }

    public Aluno buscarAlunoUsuario(Usuario usuario) {
        return alunoDao.buscarAluno(usuario);
    }

    public Aluno buscar(Aluno arg0) {
        return alunoDao.buscarAluno(arg0);
    }

    public Aluno inserirEndereco(Aluno aluno, Endereco detalheEndereco) {
        aluno = buscar(aluno);
        Set<Endereco> listaEndereco = aluno.getEnderecos();
        detalheEndereco.setAluno(aluno);
        listaEndereco.add(detalheEndereco);
        return aluno;
    }

    public Aluno alterarEndereco(Aluno aluno, Endereco detalheEndereco) {
        alunoDao.alterarDetalhe(detalheEndereco);
        return buscar(aluno);
    }

    public Aluno removerEndereco(Aluno aluno, Endereco detalheEndereco) {
        aluno = buscar(aluno);
        Set<Endereco> listaEndereco = aluno.getEnderecos();
        listaEndereco.remove(detalheEndereco);
        return aluno;
    }

    public Aluno inserirEmail(Aluno aluno, Email detalheEmail) {
        aluno = buscar(aluno);
        Set<Email> listaEmail = aluno.getEmails();
        detalheEmail.setAluno(aluno);
        listaEmail.add(detalheEmail);
        return aluno;
    }

    public Aluno alterarEmail(Aluno aluno, Email detalheEmail) {
        alunoDao.alterarDetalhe(detalheEmail);
        return buscar(aluno);
    }

    public Aluno removerEmail(Aluno aluno, Email detalheEmail) {
        aluno = buscar(aluno);
        Set<Email> listaEmail = aluno.getEmails();
        listaEmail.remove(detalheEmail);
        return aluno;
    }

    public Aluno inserirTelefone(Aluno aluno, Telefone detalheTelefone) {
        aluno = buscar(aluno);
        Set<Telefone> listaTelefone = aluno.getTelefones();
        detalheTelefone.setAluno(aluno);
        listaTelefone.add(detalheTelefone);
        return aluno;
    }

    public Aluno alterarTelefone(Aluno aluno, Telefone detalheTelefone) {
        alunoDao.alterarDetalhe(detalheTelefone);
        return buscar(aluno);
    }

    public Aluno removerTelefone(Aluno aluno, Telefone detalheTelefone) {
        aluno = buscar(aluno);
        Set<Telefone> listaTelefone = aluno.getTelefones();
        listaTelefone.remove(detalheTelefone);
        return aluno;
    }

    /**
	 * Incluir um aluno a uma turma
	 */
    public Aluno incluirTurma(Aluno aluno, Turma detalheTurma) {
        aluno = buscar(aluno);
        detalheTurma = (new TurmaBC()).buscar(detalheTurma);
        if (detalheTurma.getAlunos().size() == detalheTurma.getLotacao()) {
            throw new ApplicationRuntimeException(ErrorMessage.ALUNO_002_01);
        }
        if (!aluno.getTurmas().contains(detalheTurma)) {
            aluno.getTurmas().add(detalheTurma);
        } else {
            throw new ApplicationRuntimeException(ErrorMessage.ALUNO_002_02);
        }
        detalheTurma.getAlunos().add(aluno);
        WebMessageContext.getInstance().addMessage(InfoMessage.ALUNO_MATRICULADO_OK);
        return aluno;
    }

    public Aluno removerTurma(Aluno aluno, Turma detalheTurma) {
        aluno = buscar(aluno);
        detalheTurma = (new TurmaBC()).buscar(detalheTurma);
        aluno.getTurmas().remove(detalheTurma);
        detalheTurma.getAlunos().remove(aluno);
        return aluno;
    }

    public void salvarFoto(Aluno aluno, Foto foto) {
        if (foto != null) {
            try {
                aluno.setFoto(aluno.getId() + "_" + foto.getNome());
                FacesContext aFacesContext = FacesContext.getCurrentInstance();
                ServletContext context = (ServletContext) aFacesContext.getExternalContext().getContext();
                String path = context.getRealPath(EscolaConfig.getInstance().getUploadPath() + aluno.getFoto());
                File file = new File(path);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(foto.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                try {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = bufferedInputStream.read(buffer)) > 0) fileOutputStream.write(buffer, 0, count);
                } finally {
                    bufferedInputStream.close();
                    fileOutputStream.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public Aluno buscarPorUsuario(Usuario usuario) {
        return alunoDao.buscarPorUsuario(usuario);
    }
}
