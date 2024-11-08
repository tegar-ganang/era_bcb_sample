package org.jcompany.maven;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jcompany.commons.io.xml.PlcXMLHelper;
import org.jcompany.maven.appserver.PlcGlassfishServer;
import org.jcompany.maven.appserver.IAppServer;
import org.jcompany.maven.appserver.PlcTomcatServer;

/**
 * 
 * @author Lucas Gon�alves
 * 
 */
public abstract class PlcAbstractMojo extends AbstractMojo {

    /**
	 * N�o deve ser alterado � n�o ser que saiba realmente o que esta fazendo.
	 * 
	 * @parameter default-value="${project.artifacts}"
	 * @readonly
	 */
    protected Set dependencias;

    /**
	 * Nome do arquivo war sem a extens�o. N�o deve ser alterado � n�o ser que saiba realmente o que esta fazendo.
	 * 
	 * @parameter expression="${project.build.finalName}"
	 * @readonly
	 */
    protected String finalName;

    /**
	 * N�o deve ser alterado � n�o ser que saiba realmente o que esta fazendo.
	 * 
	 * @parameter default-value="${project}"
	 * @readonly
	 */
    protected MavenProject project;

    /**
	 * @component
	 * @readonly
	 */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
	 * The local repository.
	 * 
	 * @parameter expression="${localRepository}"
	 */
    protected ArtifactRepository localRepository;

    /**
	 * The reactor projects.
	 * 
	 * @parameter expression="${reactorProjects}"
	 * @required
	 * @readonly
	 */
    protected List<MavenProject> reactorProjects;

    private String contexto;

    /**
	 * Diretorio temporario para a descompacta��o das dependencias. N�o deve ser alterado � n�o ser que saiba realmente
	 * o que esta fazendo.
	 * 
	 * @parameter expression="${project.build.directory}/${project.build.finalName}"
	 * @readonly
	 */
    private File AppInTargetDir;

    /**
	 * Diretorio temporario de build. Normalmente � o diret�rio /target N�o deve ser alterado � n�o ser que saiba
	 * realmente o que esta fazendo.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 * @readonly
	 */
    private File targetDir;

    /**
	 * N�o deve ser alterado � n�o ser que saiba realmente o que esta fazendo.
	 * 
	 * @parameter expression="${basedir}"
	 * @readonly
	 */
    protected String baseProjeto;

    private String dirAppComplemento;

    protected static final String webapp = "src/main/webapp";

    protected static final String recursos = "src/main/resources";

    protected static final String BASE_CONTEXTOS = "";

    protected static final String BASE_CONTEXTOS_ANTIGO = "src/main/";

    public static final String CONTEXTO_DEV = ".xml";

    public static final String CONTEXTO_PROD = "_prod_context.xml";

    public static final String CONTEXTO_TEST = "_teste_context.xml";

    public static final String AMBIENTE_DESENVOLVIMENTO = "desenv";

    public static final String AMBIENTE_PRODUCAO = "prod";

    public static final String AMBIENTE_TESTE = "test";

    public static final String AMBIENTE_CI = "ci";

    private IAppServer server = null;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isModuloPrincipal()) try {
            if (System.getProperty("copiaContextMetaInf") == null || System.getProperty("copiaContextMetaInf").equals("true")) copiaArquivoContexto();
        } catch (IOException e) {
            throw new MojoFailureException("N�o foi poss�vel copiar o arquivo de contexto");
        }
    }

    /**
	 * Abre o arquivo .project e pega o output do projeto no eclipse.
	 */
    protected String getSourceDirEclipse(File projeto) {
        try {
            File projectEclipse = new File(projeto, ".classpath");
            Document doc = PlcXMLHelper.getDocumentFromFile(projectEclipse);
            Element elemento = (Element) doc.selectSingleNode("/classpath/classpathentry[@kind='output']");
            return elemento.attribute("path").getValue();
        } catch (Exception e) {
            return "target/classes";
        }
    }

    /**
	 * Verifica se o eclipse esta copiando os resources para o diret�rio output.
	 * 
	 * @return
	 */
    protected boolean eclipseIsCopyingResources(File projeto) {
        File projectEclipse = new File(projeto, ".classpath");
        if (!projectEclipse.exists()) return false;
        Document doc = PlcXMLHelper.getDocumentFromFile(projectEclipse);
        Element elemento = (Element) doc.selectSingleNode("/classpath/classpathentry[@kind='src' and @path='src/main/resources']");
        if (elemento != null) {
            Attribute exc = elemento.attribute("excluding");
            if (exc != null) if (elemento.attribute("excluding").getValue().contains("**/**")) return false;
            return true;
        }
        return false;
    }

    protected String getTargetServer() {
        if (System.getProperty("server") == null) return IBehaviourDeploy.Servers.TOMCAT;
        return System.getProperty("server");
    }

    /**
	 * Copia o arquivo de contexto para o diretorio temporario de build.
	 * 
	 * @throws IOException
	 * 
	 */
    private void copiaArquivoContexto() throws IOException {
        if (!getContextoFile().exists()) {
            getLog().info("O arquivo de contexto n�o existe: " + getContextoFile().getAbsolutePath());
            return;
        }
        getLog().info("Copiando arquivo de contexto " + getContextoFile().getName());
        File destinoContexto = new File(getAppInTargetDir(), "/META-INF/context.xml");
        if (destinoContexto.lastModified() < getContextoFile().lastModified() || destinoContexto.length() != getContextoFile().length()) {
            File contexto = null;
            if (isAmbiente(AMBIENTE_CI)) {
                contexto = getContextoFile(PlcAbstractMojo.CONTEXTO_PROD);
            } else contexto = getContextoFile();
            FileUtils.copyFile(contexto, destinoContexto);
        }
    }

    protected boolean isAmbiente(String tipo) {
        String ambiente = "";
        if (System.getProperty("ambiente") == null) ambiente = AMBIENTE_DESENVOLVIMENTO; else ambiente = System.getProperty("ambiente");
        return ambiente.equals(tipo);
    }

    public File getContextoFile(String nomeArquivoContexto) {
        File contextoFile = new File(this.project.getBasedir(), PlcAbstractMojo.BASE_CONTEXTOS_ANTIGO + this.finalName + nomeArquivoContexto);
        if (!contextoFile.exists()) contextoFile = new File(this.project.getBasedir(), PlcAbstractMojo.BASE_CONTEXTOS + this.finalName + nomeArquivoContexto);
        return contextoFile;
    }

    public File getContextoFile() {
        String nomeArquivoContexto = null;
        if (isAmbiente(AMBIENTE_DESENVOLVIMENTO)) {
            nomeArquivoContexto = PlcAbstractMojo.CONTEXTO_DEV;
        } else if (isAmbiente(AMBIENTE_PRODUCAO)) {
            nomeArquivoContexto = PlcAbstractMojo.CONTEXTO_PROD;
        } else if (isAmbiente(AMBIENTE_TESTE)) {
            nomeArquivoContexto = PlcAbstractMojo.CONTEXTO_PROD;
        } else if (isAmbiente(AMBIENTE_CI)) {
            nomeArquivoContexto = PlcAbstractMojo.CONTEXTO_TEST;
        }
        return getContextoFile(nomeArquivoContexto);
    }

    public File getAppInTargetDir() {
        return this.AppInTargetDir;
    }

    public void setAppInTargetDir(File tempBuildAplicacao) {
        this.AppInTargetDir = tempBuildAplicacao;
    }

    public File getTargetDir() {
        return this.targetDir;
    }

    protected boolean isModuloPrincipal() {
        MavenProject ultimo = (MavenProject) reactorProjects.get(reactorProjects.size() - 1);
        if (ultimo.equals(project) && ultimo.getPackaging().equals("war")) return true;
        return false;
    }

    public String getEnderecoCanonico(String end) {
        String nomeResolvido = "";
        try {
            nomeResolvido = new File(project.getBasedir(), end).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nomeResolvido;
    }

    protected IAppServer getServer() {
        if (server == null) {
            if (IBehaviourDeploy.Servers.TOMCAT.equals(getTargetServer())) {
                getLog().info("Vai retornar um instancia de PlcTomcatServer");
                server = new PlcTomcatServer(finalName);
                server.setPathUrl(finalName);
                String local = System.getProperty("tomcat");
                if (local != null) {
                    try {
                        URI url = new URI(local);
                        ((PlcTomcatServer) server).setHostName(url.getHost());
                        ((PlcTomcatServer) server).setPort(url.getPort());
                        String[] aux = url.getUserInfo().split(":");
                        ((PlcTomcatServer) server).setAdmin(aux[0]);
                        ((PlcTomcatServer) server).setPaswd(aux[1]);
                        ((PlcTomcatServer) server).setPathUrl(url.getPath());
                    } catch (URISyntaxException e) {
                        System.err.print("tomcat: " + local);
                    }
                }
            } else if (IBehaviourDeploy.Servers.GLASSFISH.equals(getTargetServer())) {
                String local = System.getProperty("gfdeploy");
                getLog().info("Vai retornar um instancia de PlcGlassfishServer");
                server = new PlcGlassfishServer(this, finalName);
                try {
                    URI url = new URI(local);
                    ((PlcGlassfishServer) server).setHostName(url.getHost());
                    ((PlcGlassfishServer) server).setServerDir(url.getPath());
                    ((PlcGlassfishServer) server).setPort(url.getPort());
                    String[] aux = url.getUserInfo().split(":");
                    ((PlcGlassfishServer) server).setAdmin(aux[0]);
                    ((PlcGlassfishServer) server).setPaswd(aux[1]);
                    getLog().debug("###" + ((PlcGlassfishServer) server));
                } catch (URISyntaxException e) {
                    System.err.print("gfdeploy: " + local);
                }
            }
        }
        return server;
    }

    public MavenProject getProject() {
        return project;
    }

    public MavenProject recuperaProject(String projectName) {
        MavenProject retorno = null;
        for (MavenProject p : reactorProjects) {
            if (p.getBasedir().getName().equals(projectName)) {
                retorno = p;
            }
        }
        return retorno;
    }

    public String getDirAppComplemento() {
        return dirAppComplemento;
    }

    public void setDirAppComplemento(String dirAppComplemento) {
        this.dirAppComplemento = dirAppComplemento;
    }
}
