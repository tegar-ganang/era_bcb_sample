package org.jcompany.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jcompany.commons.io.PlcFileUtils;
import org.jcompany.commons.io.PlcJar;
import org.jcompany.commons.io.filters.PlcFilterAgeFile;
import org.jcompany.maven.appserver.PlcGlassfishServer;

/**
 * @goal deploy-restart
 * @phase process-resources
 * @requiresDependencyResolution compile
 * @author Lucas Gon�alves
 */
public class PlcDeployRestartMojo extends PlcDeployFastMojo {

    protected IBehaviourDeploy cdd = null;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isModuloPrincipal()) return;
        super.execute();
        cdd = PlcBehaviourDeployFactory.getIComportamentoDeDeployInstance(getServer());
        try {
            cdd.pararAplicacao(getServer());
        } catch (MojoExecutionException e) {
        }
        copiaContexto();
        try {
            copiarFontes();
            copiarRecursos();
        } catch (IOException e) {
            throw new MojoFailureException("N�o foi poss�vel copiar todos os arquivos fontes");
        }
        try {
            cdd.iniciarAplicacao(getServer());
        } catch (MojoExecutionException e) {
            reiniciarAplicacao();
        }
    }

    private void copiaContexto() throws MojoFailureException, MojoExecutionException {
        getLog().info("Copiando arquivo de Contexto: " + getContextoFile().getAbsolutePath());
        if (!getContextoFile().exists() || getServer() instanceof PlcGlassfishServer) {
            return;
        }
        File destinoContexto = new File(getServer().getContextDir(), this.project.getArtifactId() + ".xml");
        try {
            FileUtils.copyFile(getContextoFile(), destinoContexto);
        } catch (IOException e) {
            throw new MojoFailureException("N�o foi poss�vel copiar o arquivo de contexto para o servidor");
        }
    }

    /**
	 * D� um touch no web.xml da aplica��o.
	 * 
	 * @throws MojoFailureException
	 * @throws MojoExecutionException
	 */
    protected void reiniciarAplicacao() throws MojoFailureException, MojoExecutionException {
        cdd.reiniciarAplicacao(getServer());
        File webXML = new File(getServer().getAppDir(), "WEB-INF/web.xml");
        try {
            FileUtils.touch(webXML);
        } catch (IOException e) {
            throw new MojoFailureException("N�o foi poss�vel reiniciar o Tomcat porque ocorreu alguma falha ao dar um TOUCH no arquivo web.xml");
        }
    }

    private void copiarFontes() throws IOException, MojoExecutionException {
        File dirFontes = null;
        getLog().info("Copiando FONTES: ");
        List<File> projetos = getSubProjetosDirBase();
        int cont = 0;
        for (File projeto : projetos) {
            getLog().info("    -> " + projeto.getName());
            MavenProject project = recuperaProject(projeto.getName());
            dirFontes = new File(projeto, getSourceDirEclipse(projeto));
            if (cont < (projetos.size() - 1) && project != null && !project.getPackaging().equals("war")) {
                PlcJar.jar(project.getArtifactId() + "-" + project.getVersion() + ".jar", new File(getServer().getAppDir(), "/WEB-INF/lib"), new File[] { dirFontes });
            } else {
                PlcFileUtils.copiarDiretorio(dirFontes, new File(getServer().getAppDir(), "/WEB-INF/classes"), false, debug, new PlcFilterAgeFile(), new PlcFilterPreferenceFile(getSubProjetosDirBase(), projeto, getSourceDirEclipse(projeto)));
            }
            cont++;
        }
    }

    private void copiarRecursos() throws IOException, MojoExecutionException {
        File dirRecursos = null;
        getLog().info("Copiando RECURSOS: ");
        for (File projeto : getSubProjetosDirBase()) {
            if (eclipseIsCopyingResources(projeto)) getLog().info("    -> " + projeto.getName() + " j� copiado com os fontes"); else {
                getLog().info("    -> " + projeto.getName());
                dirRecursos = new File(projeto, PlcAbstractMojo.recursos);
                PlcFileUtils.copiarDiretorio(dirRecursos, new File(getServer().getAppDir(), "/WEB-INF/classes"), false, debug, new PlcFilterAgeFile(), new PlcFilterPreferenceFile(getSubProjetosDirBase(), projeto, PlcAbstractMojo.recursos));
            }
        }
    }
}
