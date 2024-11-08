package br.org.acessobrasil.portal.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import br.org.acessobrasil.portal.controle.ControleCss;
import br.org.acessobrasil.portal.modelo.Usuario;
import br.org.acessobrasil.ases.ferramentas_de_reparo.controle.ControleCssListener;
import br.org.acessobrasil.ases.ferramentas_de_reparo.modelo.AvaliacaoCSS;
import br.org.acessobrasil.silvinha2.util.G_File;

/**
 * Responsavel por cadastrar apagar o CSS
 * @author Fabio Issamu Oshiro, Jonatas Pacheco, Daniel Zupo, Clemilson Barcelos
 *
 */
public class CssAction extends Super implements ControleCssListener {

    private static final long serialVersionUID = 4393096367026115619L;

    private File fileCss;

    private String fileCssContentType;

    private String fileCssFileName;

    private AvaliacaoCSS avaliacaoCSS;

    private List<File> arquivosCss;

    public File getFileCss() {
        return fileCss;
    }

    public void setFileCss(File fileCss) {
        this.fileCss = fileCss;
    }

    public String getFileCssContentType() {
        return fileCssContentType;
    }

    public void setFileCssContentType(String fileCssContentType) {
        this.fileCssContentType = fileCssContentType;
    }

    public String getFileCssFileName() {
        return fileCssFileName;
    }

    public void setFileCssFileName(String fileCssFileName) {
        this.fileCssFileName = fileCssFileName;
    }

    public String listarCss() {
        File theFile = ArquivoAction.getFile(getSitioAtual(), "css");
        if (!theFile.exists()) {
            theFile.mkdirs();
        }
        ArrayList<File> t = new ArrayList<File>();
        for (File file : theFile.listFiles()) {
            if (file.isFile()) t.add(file);
        }
        arquivosCss = t;
        return SUCCESS;
    }

    public String incluirCss() {
        Usuario usuario = getUsuarioLogado();
        if (fileCss != null && !fileCssFileName.equals("")) {
            try {
                int i = fileCssFileName.lastIndexOf(".");
                String extensao = fileCssFileName.substring(i).toLowerCase();
                if (!extensao.equals(".css")) {
                    throw new Exception("Somente arquivos do tipo css.");
                }
                File theFile = ArquivoAction.getFile(getSitioAtual(), "css" + File.separatorChar + fileCssFileName);
                ControleCss controleCss = new ControleCss(this);
                String codCss = new G_File(fileCss.getAbsolutePath()).read();
                controleCss.doAval(codCss);
                if (avaliacaoCSS.getErros().getErrorCount() > 0) {
                    throw new Exception("Existem erros no css e ele n&atilde;o pode ser cadastrado.");
                } else {
                    FileUtils.copyFile(fileCss, theFile);
                    usuario.addActionMessage("Arquivo de CSS cadastrado corretamente.");
                }
                if (avaliacaoCSS.getErros().getErrorCount() > 0 || avaliacaoCSS.getAvisos().getWarningCount() > 0) {
                    getRequest().setAttribute("avaliacaoCSS", avaliacaoCSS);
                } else {
                    getRequest().setAttribute("avaliacaoCSS", null);
                }
            } catch (Exception e) {
                usuario.addActionError(e.getMessage());
                return INPUT;
            }
        }
        return SUCCESS;
    }

    public String apagarCss() {
        try {
            fileCss = ArquivoAction.getFile(getSitioAtual(), "css" + File.separatorChar + fileCssFileName);
            fileCss.delete();
        } catch (Exception e) {
        }
        return SUCCESS;
    }

    public String abrirCss() {
        fileCss = new File(fileCssFileName);
        return SUCCESS;
    }

    public void avaliacaoCssRealizada(AvaliacaoCSS avaliacaoCSS) {
        this.avaliacaoCSS = avaliacaoCSS;
    }

    public AvaliacaoCSS getAvaliacaoCSS() {
        return avaliacaoCSS;
    }

    public void setAvaliacaoCSS(AvaliacaoCSS avaliacaoCSS) {
        this.avaliacaoCSS = avaliacaoCSS;
    }

    public List<File> getArquivosCss() {
        return arquivosCss;
    }

    public void setArquivosCss(List<File> arquivosCss) {
        this.arquivosCss = arquivosCss;
    }
}
