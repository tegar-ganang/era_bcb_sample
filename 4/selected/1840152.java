package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.util.Vector;
import javax.swing.JOptionPane;
import control.AcessaDados;
import control.ExtraiCodigo;

/**
 * Esta classe tem por objetivo implementar os atibutos e métodos necessários para realizar a 
 * geração de um arquivo html para um tutorial selecionado.<br />
 * A página de um tutorial é a 'index.html' que será a página raiz para o site gerado a partir 
 * de um tutorial cadastrado.
 * @author Samuel Henrique N. da Silva
 *
 */
public final class TutorialWebPage {

    /**
	 * Atributo string que guarda o diretório selecionado pelo usuário, armazenará todos os arquivos do site.
	 */
    private String diretorioRaiz;

    /**
	 * tutorial passadado por parâmetro de acordo com o selcionado na tela Gerar Tutorial
	 */
    private Tutorial tutorial;

    /**
	 * Código do tutorial a ter o seu web site gerado
	 */
    private int codTutorial;

    /**
	 * Vetor que guarda objetos Capitulo para utilizar os dados na construção das páginas
	 * 
	 */
    private Vector<Capitulo> capitulos = new Vector<Capitulo>();

    /**
	 * Codigos dos capitulos presentes no tutorial selecionado
	 */
    private Vector<Integer> codigosCapitulos = new Vector<Integer>();

    /**
	 * Vetor de tipo inteiro para armazenar os códigos das lições existentes neste tutorial a partir dos capítulos
	 */
    private Vector<Integer> codigosLicoes = new Vector<Integer>();

    /**
	 * Vetor que guarda objetos da classe Lição
	 */
    private Vector<Licao> licoes = new Vector<Licao>();

    /**
	 * Vetor do tipo inteiro que guarda os codigos das midias existentes neste tutorial.
	 */
    private Vector<Integer> codigosMidias = new Vector<Integer>();

    /**
	 * Vetor de objetos da classe Midia
	 */
    private Vector<Midia> midias = new Vector<Midia>();

    /**
	 * Conexão com a base de dados, necessária para buscar os dados do tutorial
	 */
    private Connection outConnection;

    /**
	 * Construtor padrão
	 */
    public TutorialWebPage(final int codigoTutorial, final Connection con, final String dir) {
        setOutConnection(con);
        setCodTutorial(codigoTutorial);
        setTutorial(getCodTutorial());
        setCodigosCapitulos(getCodTutorial());
        setCapitulos(getCodigosCapitulos());
        setCodigosLicoes(getCodigosCapitulos());
        setLicoes(getCodigosLicoes());
        setCodigosMidias(getCodigosLicoes());
        setMidias(getCodigosMidias());
        setDiretorioRaiz(dir);
    }

    /**
	 * Getter method para o atributo diretorioRaiz
	 * @return String diretorio raiz para armazenar arquivos do site do tutorial.
	 */
    public String getDiretorioRaiz() {
        return diretorioRaiz;
    }

    /**
	 * Setter method para o atributo diretorioRaiz
	 * @param diretorioRaiz
	 */
    public void setDiretorioRaiz(String diretorioRaiz) {
        this.diretorioRaiz = diretorioRaiz;
    }

    /**
	 * Getter method para o atributo tutorial
	 * @return Tutorial instância da classe Tutorial
	 */
    public Tutorial getTutorial() {
        return tutorial;
    }

    /**
	 * Setter method para o atributo tutorial
	 * @param tutorial
	 */
    public void setTutorial(final int cod) {
        this.tutorial = AcessaDados.retornaDadosTutorial(getOutConnection(), cod);
    }

    /**
	 * Getter method para o atributo codTutorial
	 * @return int codigo do tutorial a ter o web site gerado
	 */
    public int getCodTutorial() {
        return codTutorial;
    }

    /**
	 * Setter method para o atributo codTutorial
	 * @param codTutorial
	 */
    public void setCodTutorial(int codTutorial) {
        this.codTutorial = codTutorial;
    }

    /**
	 * Setter method para o atributo codigosCapitulos
	 */
    public void setCodigosCapitulos(final int codTutorial) {
        Vector<String> temp = new Vector<String>();
        temp = AcessaDados.retornaCapitulosDeTutCap(codTutorial, getOutConnection());
        for (int i = 0; i < temp.size(); i++) {
            Integer cod = ExtraiCodigo.extraiCodigo(temp.get(i).toString());
            codigosCapitulos.add(cod);
        }
    }

    /**
	 * Getter Method para o atributo codigoCapitulos
	 */
    public Vector<Integer> getCodigosCapitulos() {
        return codigosCapitulos;
    }

    /**
	 * Setter method para o atributo capitulos
	 */
    public void setCapitulos(final Vector<Integer> codigosCapitulos) {
        for (int i = 0; i < codigosCapitulos.size(); i++) {
            capitulos.add(AcessaDados.retornaDadosCapitulo(codigosCapitulos.get(i), getOutConnection()));
        }
    }

    /**
	 * Getter method para o atributo capitulos
	 */
    public Vector<Capitulo> getCapitulos() {
        return capitulos;
    }

    /**
	 * Setter Method para o atributo codigosLicoes
	 */
    public void setCodigosLicoes(final Vector<Integer> codCap) {
        for (int i = 0; i < codCap.size(); i++) {
            Vector<String> s = new Vector<String>();
            s = AcessaDados.retornaLicoesDeCapLic(codCap.get(i), getOutConnection());
            for (int j = 0; j < s.size(); j++) {
                codigosLicoes.add(ExtraiCodigo.extraiCodigo(s.get(j)));
            }
        }
    }

    /**
	 * Getter Method para o atributo codigosLicoes
	 */
    public Vector<Integer> getCodigosLicoes() {
        return codigosLicoes;
    }

    /**
	 * Setter Method para o atributo licoes
	 */
    public void setLicoes(final Vector<Integer> codLic) {
        for (int i = 0; i < codLic.size(); i++) {
            licoes.add(AcessaDados.retornaDadosLicao(codLic.get(i), getOutConnection()));
        }
    }

    /**
	 * Getter Method para o atributo licoes
	 */
    public Vector<Licao> getLicoes() {
        return licoes;
    }

    /**
	 * Setter method para o atributo codigosMidias
	 */
    public void setCodigosMidias(final Vector<Integer> codLic) {
        for (int i = 0; i < codLic.size(); i++) {
            Vector<String> s = new Vector<String>();
            s = AcessaDados.retornaMidiasDeLicMid(codLic.get(i), getOutConnection());
            for (int j = 0; j < s.size(); j++) {
                codigosMidias.add(ExtraiCodigo.extraiCodigo(s.get(j)));
            }
        }
    }

    /**
	 * Getter Method para o atributo codigosMidias
	 */
    public Vector<Integer> getCodigosMidias() {
        return codigosMidias;
    }

    /**
	 * Setter Method para o atributo midias
	 */
    public void setMidias(final Vector<Integer> codMid) {
        for (int i = 0; i < codMid.size(); i++) {
            midias.add(AcessaDados.retornaDadosMidia(codMid.get(i), getOutConnection()));
        }
    }

    /**
	 * Getter Method para o atributo midias
	 */
    public Vector<Midia> getMidias() {
        return midias;
    }

    /**
	 * Getter method para o atributo outConnection
	 * @return Connection conexão com a base de dados vigente
	 */
    public Connection getOutConnection() {
        return outConnection;
    }

    /**
	 * Setter method para o atributo outConnection
	 * @param outConnection
	 */
    public void setOutConnection(Connection outConnection) {
        this.outConnection = outConnection;
    }

    /**
	 * Criar diretório raiz do site
	 */
    public File criarDiretorioSite() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().trim().toString()));
        dir.mkdir();
        return dir;
    }

    /**
	 * Criar diretório dos capitulos do site
	 */
    public File criarDiretorioCapitulos() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().trim().toString()) + "/capitulos");
        dir.mkdir();
        return dir;
    }

    /**
	 * Criar diretório das lições do site
	 */
    public File criarDiretorioLicoes() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().trim().toString()) + "/licoes");
        dir.mkdir();
        return dir;
    }

    /**
	 * Criar diretorio de mídias do site 
	 */
    public File criarDiretorioMidias() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().trim().toString()) + "/midias");
        dir.mkdir();
        return dir;
    }

    /**
	 * Criar diretorio de arquivos css do site 
	 */
    public File criarDiretorioCss() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().trim().toString()) + "/style");
        dir.mkdir();
        return dir;
    }

    /**
	 * Criar diretorio para os arquivos de mídia (imagem e videos) utilizados no site
	 */
    public File criarDiretorioArquivos() {
        File dir = new File(getDiretorioRaiz() + "/" + processaString(getTutorial().getTitulo().toString()) + "/files");
        dir.mkdir();
        return dir;
    }

    /**
	 * Inicio do arquivo html, em especial a seção head, da pagina index do Tutorial
	 * @return String contendo a seção head do arquivo html
	 */
    public String escreverIndexHead() {
        String head = new String();
        head = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"" + "\"http://www.w3c.org/TR/xhtml1-transitional.dtd\"> \n" + "<html>\n" + "<head> \n" + "<title>" + getTutorial().getTitulo() + "</title> \n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/layout.css\" /> \n" + "<!--[if IE]><link rel=\"stylesheet\" type=\"text/css\" href=\"style/layout_ie.css\" /><![endif]--> \n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"style/elementos.css\" /> \n" + "<!--[if IE]><link rel=\"stylesheet\" type=\"text/css\" href=\"style/elementos_ie.css\" /><![endif]--> \n" + "</head> \n";
        return head;
    }

    /**
	 * Parte do arquivo html em que se dá o ínicio da seção body mais a div header da página index
	 * @return String
	 */
    public String escreverBodyHeader() {
        String bodyHeader = new String();
        bodyHeader = "<body> \n" + "<div id=\"conteudo\"> \n" + "<div id=\"header\"> \n " + "<h1>" + getTutorial().getTitulo() + "</h1> \n" + "</div> \n";
        return bodyHeader;
    }

    /**
	 * Escreve a seção de links para os capítulos existentes no tutorial
	 */
    public String escreverIndexLinksCapitulos() {
        String listaLinks = new String();
        listaLinks = "<ul> \n" + "<li class=\"category\">:: Cap&iacute;tulos</li> \n";
        for (int i = 0; i < getCapitulos().size(); i++) {
            String urlCap = processaString(getCapitulos().get(i).getTitulo());
            listaLinks = listaLinks + "<li class=\"doMenu\"><a class=\"doMenu\" href=\"capitulos/" + urlCap + ".html\">" + getCapitulos().get(i).getTitulo() + "\n<span>" + getCapitulos().get(i).getComentario() + "</span>\n" + "</a></li> \n";
        }
        listaLinks = listaLinks + "</ul> \n";
        return listaLinks;
    }

    /**
	 * Parte do arquivo html em que se escreve a seção de conteúdo da página index
	 * @return String
	 */
    public String escreverIndexBodyContent() {
        String bodyContent = new String();
        bodyContent = "<div id=\"menu\"> \n" + escreverIndexLinksCapitulos() + "</div> \n" + "<div id=\"texto\"> \n" + "<h2>" + getTutorial().getComentario() + "</h2> \n" + "<p> \n" + getTutorial().getDescricao() + "\n</p> \n" + "<p class=\"autor\">\n" + "<a class=\"mail\" href=\"mailto:" + getTutorial().getEmailAutor() + "\">" + "Autor: " + getTutorial().getAutor() + "</a>\n</p>\n" + "</div> \n";
        return bodyContent;
    }

    /**
	 * Parte do arquivo html em que se escreve o rodapé e finaliza as seções body e html da página index
	 * @return String
	 */
    public String escreverFooter() {
        String footer = new String();
        footer = "<div id=\"footer\"> \n" + "<p class=\"footer\">\nTuDMaP &copy; 2011\n" + "</p> \n" + "</div> \n " + "</div> \n" + "</body> \n" + "</html>";
        return footer;
    }

    /**
	 * Escreve as paginas para os capítulos
	 */
    public void escreverCapitulosPages(final File diretorio) {
        for (int i = 0; i < getCapitulos().size(); i++) {
            gerarCapituloPage(diretorio, getCapitulos().get(i));
        }
    }

    /**
	 * Gerar páginas de capítulos
	 */
    public void gerarCapituloPage(final File diretorio, final Capitulo cap) {
        Vector<String> idLicoes = new Vector<String>();
        idLicoes = AcessaDados.retornaLicoesDeCapLic(cap.getCodigo(), getOutConnection());
        Vector<Licao> licoes = new Vector<Licao>();
        for (int i = 0; i < idLicoes.size(); i++) {
            licoes.add(AcessaDados.retornaDadosLicao(ExtraiCodigo.extraiCodigo(idLicoes.get(i)), getOutConnection()));
        }
        idLicoes = processaLinkLicoes(idLicoes);
        try {
            FileWriter capHtml = new FileWriter(diretorio + "/" + processaString(cap.getTitulo()) + ".html");
            String capContent = "<div id=\"menu\"> \n" + escreverCapLinksCapitulos() + "</div> \n" + "<div id=\"texto\"> \n" + "<h2>" + cap.getTitulo() + "</h2> \n" + "<p> \n" + cap.getDescricao() + "</p> \n" + "<p class=\"autor\"> \n" + "<a class=\"mail\" href=\"mailto:" + cap.getEmailAutor() + "\">" + "Autor: " + cap.getAutor() + "</a> \n</p>\n" + "<h3>Li&ccedil;&otilde;es do cap&iacute;tulo '" + cap.getTitulo() + "':</h3> \n" + constroiLinkLicoes(licoes) + "<hr /><br /> \n" + "<div id=\"homeLink\"> \n" + "<a class=\"home\" href=\"../index.html\">Home</a><br /><br /> \n" + "</div> \n" + "</div> \n";
            capHtml.write(escreverCapLicMidHead() + escreverBodyHeader() + capContent + escreverFooter());
            capHtml.close();
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }

    /**
	 * Inicio do arquivo html, em especial a seção head, das páginas dos capítulos
	 * @return String contendo a seção head do arquivo html
	 */
    public String escreverCapLicMidHead() {
        String head = new String();
        head = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"" + "\"http://www.w3c.org/TR/xhtml1-transitional.dtd\"> \n" + "<html>\n" + "<head> \n" + "<title>" + getTutorial().getTitulo() + "</title> \n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"../style/layout.css\" /> \n" + "<!--[if IE]><link rel=\"stylesheet\" type=\"text/css\" href=\"../style/layout_ie.css\" /><![endif]--> \n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"../style/elementos.css\" /> \n" + "<!--[if IE]><link rel=\"stylesheet\" type=\"text/css\" href=\"../style/elementos_ie.css\" /><![endif]--> \n" + "<link rel=\"stylesheet\" type=\"text/css\" href=\"../style/estilo-cap-lic-mid.css\" /> \n" + "<!--[if IE]><link rel=\"stylesheet\" type=\"text/css\" href=\"../style/estilo-cap-lic-mid_ie.css\" /><![endif]--> \n" + "</head> \n";
        return head;
    }

    /**
	 * Escreve a seção de links para os capítulos existentes no tutorial nas páginas dos capítulos
	 */
    public String escreverCapLinksCapitulos() {
        String listaLinks = new String();
        listaLinks = "<ul> \n" + "<li class=\"category\">:: Cap&iacute;tulos</li> \n";
        for (int i = 0; i < getCapitulos().size(); i++) {
            String urlCap = processaString(getCapitulos().get(i).getTitulo());
            listaLinks = listaLinks + "<li class=\"doMenu\"><a class=\"doMenu\" href=\"" + urlCap + ".html\">" + getCapitulos().get(i).getTitulo() + "\n<span>" + getCapitulos().get(i).getComentario() + "</span>\n" + "</a></li> \n";
        }
        listaLinks = listaLinks + "</ul> \n";
        return listaLinks;
    }

    /**
	 * Constrói os links paras as paginas das lições na construção de uma página de um capítulo
	 * 
	 */
    public String constroiLinkLicoes(final Vector<Licao> licoes) {
        String linkLicoes = "<ul class=\"conteudo\"> \n ";
        for (int i = 0; i < licoes.size(); i++) {
            String urlLicao = processaString(licoes.get(i).getTitulo());
            linkLicoes = linkLicoes + "<li><a href=\"../licoes/" + urlLicao + ".html\">" + licoes.get(i).getTitulo() + "<span>" + licoes.get(i).getComentario() + "</span>\n" + "</a></li> \n";
        }
        linkLicoes = linkLicoes + "</ul> \n";
        return linkLicoes;
    }

    /**
	 * Processa as string contendo os codigos e titulos das lições para que sejam retornados somente o titulo
	 * para serem usados nas páginas dos capítulos como links para as páginas das respectivas lições
	 */
    public Vector<String> processaLinkLicoes(Vector<String> idLicoes) {
        if (idLicoes != null) {
            Vector<String> ret = new Vector<String>();
            for (int i = 0; i < idLicoes.size(); i++) {
                String aux;
                aux = idLicoes.get(i).substring(idLicoes.get(i).indexOf('-') + 2);
                ret.add(aux);
            }
            return ret;
        } else {
            return null;
        }
    }

    /**
	 * Escreve as páginas para as lições
	 */
    public void escreverLicoesPage(final File dir) {
        for (int i = 0; i < getLicoes().size(); i++) {
            gerarLicaoPage(dir, getLicoes().get(i));
        }
    }

    /**
	*Constrói a página de uma lição
	*/
    public void gerarLicaoPage(final File dir, final Licao lic) {
        Vector<String> idMidias = new Vector<String>();
        idMidias = AcessaDados.retornaMidiasDeLicMid(lic.getCodigo(), getOutConnection());
        Vector<Midia> midias = new Vector<Midia>();
        for (int i = 0; i < idMidias.size(); i++) {
            midias.add(AcessaDados.retornaDadosMidia(ExtraiCodigo.extraiCodigo(idMidias.get(i)), getOutConnection()));
        }
        idMidias = processaMidiaLink(idMidias);
        try {
            FileWriter licHtml = new FileWriter(dir + "/" + processaString(lic.getTitulo()) + ".html");
            String licContent = "<div id=\"menu\"> \n" + escreverLicMidLinksCapitulos() + "</div> \n" + "<div id=\"texto\"> \n" + "<h2>" + lic.getTitulo() + "</h2> \n" + "<p> \n" + lic.getDescricao() + "</p> \n" + "<p class=\"autor\">\n" + "<a class=\"mail\" href=\"mailto:" + lic.getEmailAutor() + "\">" + "Autor: " + lic.getAutor() + "</a>\n</p> \n" + "<h3>M&iacute;dias da li&ccedil;&atilde;o '" + lic.getTitulo() + "'</h3> \n" + constroiLinkMidias(midias) + "<hr /><br /> \n" + "<div id=\"homeLink\"> \n" + "<a class=\"home\" href=\"../index.html\">Home</a><br /><br />\n" + "</div> \n" + "</div> \n";
            licHtml.write(escreverCapLicMidHead() + escreverBodyHeader() + licContent + escreverFooter());
            licHtml.close();
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }

    /**
	 * Escreve a seção de links para os capítulos existentes no tutorial nas páginas dos capítulos
	 */
    public String escreverLicMidLinksCapitulos() {
        String listaLinks = new String();
        listaLinks = "<ul> \n" + "<li class=\"category\">:: Cap&iacute;tulos</li> \n";
        for (int i = 0; i < getCapitulos().size(); i++) {
            String urlCap = processaString(getCapitulos().get(i).getTitulo());
            listaLinks = listaLinks + "<li class=\"doMenu\"><a class=\"doMenu\" href=\"../capitulos/" + urlCap + ".html\">" + getCapitulos().get(i).getTitulo() + "\n<span>" + getCapitulos().get(i).getComentario() + "</span>\n" + "</a></li> \n";
        }
        listaLinks = listaLinks + "</ul> \n";
        return listaLinks;
    }

    /**
	 * Constrói os links paras as paginas das mídias na construção de uma página de uma lição
	 * 
	 */
    public String constroiLinkMidias(final Vector<Midia> midias) {
        String linkMidias = "<ul class=\"conteudo\"> \n ";
        for (int i = 0; i < midias.size(); i++) {
            String urlMidia = processaString(midias.get(i).getTitulo());
            linkMidias = linkMidias + "<li><a href=\"../midias/" + urlMidia + ".html\">" + midias.get(i).getTitulo() + "\n<span>" + midias.get(i).getComentario() + "</span>\n" + "</a></li> \n";
        }
        linkMidias = linkMidias + "</ul> \n";
        return linkMidias;
    }

    /**
	 * Processa as string contendo os codigos e titulos das mídias para que sejam retornados somente o titulo
	 * para serem usados nas páginas das lições como links para as páginas das respectivas mídias
	 */
    public Vector<String> processaMidiaLink(Vector<String> idMidias) {
        if (idMidias != null) {
            Vector<String> ret = new Vector<String>();
            for (int i = 0; i < idMidias.size(); i++) {
                String aux;
                aux = idMidias.get(i).substring(idMidias.get(i).indexOf('-') + 2);
                ret.add(aux);
            }
            return ret;
        } else {
            return null;
        }
    }

    /**
	 * Escreve a páginas das mídias
	 * @param File -  diretorio para gerar as páginas
	 */
    public void escreverMidiasPage(final File dir) {
        for (int i = 0; i < getMidias().size(); i++) {
            gerarMidiaPage(dir, getMidias().get(i));
        }
    }

    /**
	 * Gerar página html para as mídias existentes no tutorial.
	 * @param File - diretorio onde serão armazendos os arquivos html.
	 * @param Midia - objeto da classe Midia cujo os dados serão utilizados para compor a página html
	 */
    public void gerarMidiaPage(final File dir, final Midia mid) {
        try {
            FileWriter midHtml = new FileWriter(dir + "/" + processaString(mid.getTitulo() + ".html"));
            String midContent = "<div id=\"menu\"> \n" + escreverLicMidLinksCapitulos() + "</div> \n" + "<div id=\"texto\"> \n" + "<h2>" + mid.getTitulo() + "</h2> \n" + "<p> \n" + mid.getDescricao() + "</p> \n" + inserirMidia(mid) + "<p class=\"autor\">\n" + "<a class=\"mail\" href=\"mailto:" + mid.getEmailAutor() + "\">" + "Autor: " + mid.getAutor() + "</a>\n</p> \n" + "<hr /><br /> \n" + "<div id=\"homeLink\"> \n" + "<a class=\"home\" href=\"../index.html\">Home</a><br/><br /> \n" + "</div> \n" + "</div> \n";
            midHtml.write(escreverCapLicMidHead() + escreverBodyHeader() + midContent + escreverFooter());
            midHtml.close();
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }

    /**
	 * Insere o vídeo ou imagem no arquivo html utilizando as tags apropriadas de acordo com o tipo de mídia
	 */
    public String inserirMidia(final Midia mid) {
        String inserirMidia;
        if (mid.getTipo().equals("video")) {
            inserirMidia = "<embed autostart=\"false\" width=\"400px\" height=\"300px\" " + "src=\"../files/videos/" + processaString(mid.getTitulo()) + "." + retornaExtensaoMidia(mid) + "\" " + "type=\"video/mpeg\" /> \n";
        } else {
            inserirMidia = "<img width=\"400px\" height=\"300px\" src=\"../files/imagens/" + processaString(mid.getTitulo()) + "." + retornaExtensaoMidia(mid) + "\" /> \n";
        }
        return inserirMidia;
    }

    /**
	 * retorna a extensão (.jpg, .jpeg, .mpg, .mpeg ou .png) da mídia
	 */
    public String retornaExtensaoMidia(final Midia mid) {
        String ret;
        ret = mid.getUrl();
        if (ret.endsWith("mpeg")) {
            return "mpeg";
        } else if (ret.endsWith("mpg")) {
            return "mpg";
        } else if (ret.endsWith("jpeg")) {
            return "jpeg";
        } else if (ret.endsWith("jpg")) {
            return "jpg";
        } else {
            return "png";
        }
    }

    /**
	 * Copiar midias cadastradas para o diretorio criado para o site, passado como parametro
	 * @param midDir - Diretório para armazenar arquivos de mídias
	 */
    public void copiarMidias(final File vidDir, final File imgDir) {
        for (int i = 0; i < getMidias().size(); i++) {
            try {
                FileChannel src = new FileInputStream(getMidias().get(i).getUrl().trim()).getChannel();
                FileChannel dest;
                if (getMidias().get(i).getTipo().equals("video")) {
                    FileChannel vidDest = new FileOutputStream(vidDir + "/" + processaString(getMidias().get(i).getTitulo()) + "." + retornaExtensaoMidia(getMidias().get(i))).getChannel();
                    dest = vidDest;
                } else {
                    FileChannel midDest = new FileOutputStream(imgDir + "/" + processaString(getMidias().get(i).getTitulo()) + "." + retornaExtensaoMidia(getMidias().get(i))).getChannel();
                    dest = midDest;
                }
                dest.transferFrom(src, 0, src.size());
                src.close();
                dest.close();
            } catch (Exception e) {
                System.err.print(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
	 * Metodo para gerar a página html index do tutorial selecionado
	 */
    public boolean gerarTutorialPage() {
        try {
            File indexDir = criarDiretorioSite();
            File cssDir = criarDiretorioCss();
            File capDir = criarDiretorioCapitulos();
            File licDir = criarDiretorioLicoes();
            File midDir = criarDiretorioMidias();
            File filesDir = criarDiretorioArquivos();
            File videosDir = new File(filesDir + "/videos");
            videosDir.mkdir();
            File imagensDir = new File(filesDir + "/imagens");
            imagensDir.mkdir();
            String local = System.getProperty("user.dir");
            FileChannel srcCss1 = new FileInputStream(local + "/bin/style/layout.css").getChannel();
            FileChannel destCss1 = new FileOutputStream(cssDir + "/layout.css").getChannel();
            destCss1.transferFrom(srcCss1, 0, srcCss1.size());
            srcCss1.close();
            destCss1.close();
            FileChannel srcCss2 = new FileInputStream(local + "/bin/style/elementos.css").getChannel();
            FileChannel destCss2 = new FileOutputStream(cssDir + "/elementos.css").getChannel();
            destCss2.transferFrom(srcCss2, 0, srcCss2.size());
            srcCss2.close();
            destCss2.close();
            FileChannel srcCss3 = new FileInputStream(local + "/bin/style/estilo-cap-lic-mid.css").getChannel();
            FileChannel destCss3 = new FileOutputStream(cssDir + "/estilo-cap-lic-mid.css").getChannel();
            destCss3.transferFrom(srcCss3, 0, srcCss3.size());
            srcCss3.close();
            destCss3.close();
            FileChannel srcCss4 = new FileInputStream(local + "/bin/style/layout_ie.css").getChannel();
            FileChannel destCss4 = new FileOutputStream(cssDir + "/layout_ie.css").getChannel();
            destCss4.transferFrom(srcCss4, 0, srcCss4.size());
            srcCss4.close();
            destCss4.close();
            FileChannel srcCss5 = new FileInputStream(local + "/bin/style/elementos_ie.css").getChannel();
            FileChannel destCss5 = new FileOutputStream(cssDir + "/elementos_ie.css").getChannel();
            destCss5.transferFrom(srcCss5, 0, srcCss5.size());
            srcCss5.close();
            destCss5.close();
            FileChannel srcCss6 = new FileInputStream(local + "/bin/style/estilo-cap-lic-mid_ie.css").getChannel();
            FileChannel destCss6 = new FileOutputStream(cssDir + "/estilo-cap-lic-mid_ie.css").getChannel();
            destCss6.transferFrom(srcCss6, 0, srcCss6.size());
            srcCss6.close();
            destCss6.close();
            copiarMidias(videosDir, imagensDir);
            escreverMidiasPage(midDir);
            escreverLicoesPage(licDir);
            escreverCapitulosPages(capDir);
            FileWriter indexHtml = new FileWriter(indexDir + "/index.html");
            indexHtml.write(escreverIndexHead() + escreverBodyHeader() + escreverIndexBodyContent() + escreverFooter());
            indexHtml.close();
            System.out.println("Site gerado com sucesso");
            JOptionPane.showMessageDialog(null, "Web Site gerado com sucesso", "\\o/", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Site não gerado");
            JOptionPane.showMessageDialog(null, "Web Site não gerado corretamente", "Ops...", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
	 * Método que recebe uma string e retorna outra substituindo seus espaços pelo caractere 'underline' ('_')
	 * e com todas as letras minúsculas. Útil para construir urls de links
	 */
    public String processaString(String s) {
        s = s.toLowerCase();
        s = s.replace(' ', '_');
        s = s.replaceAll("[,.;:]", "_");
        s = s.replaceAll("[áàãâä]", "a");
        s = s.replaceAll("[éèêë]", "e");
        s = s.replaceAll("[íìîï]", "i");
        s = s.replaceAll("[óòõôö]", "o");
        s = s.replaceAll("[úùûü]", "u");
        s = s.replaceAll("[ç]", "c");
        return s;
    }
}
