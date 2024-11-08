package org.jcompany.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jcompany.commons.io.PlcZip;
import org.jcompany.maven.appserver.IAppServer;

public class PlcBehaviourDeployTomcat extends PlcAbstractBaseBehaviourDeploy {

    public void apagarAplicacao(IAppServer server) throws MojoExecutionException {
        try {
            getLog().info("Apagando o diret�rio: " + server.getAppDir().getAbsolutePath());
            FileUtils.deleteDirectory(server.getAppDir());
            getLog().info("Apagando o diret�rio: " + server.getAppWorkDir().getAbsolutePath());
            FileUtils.deleteDirectory(server.getAppWorkDir());
        } catch (IOException e) {
            getLog().debug("N�o conseguiu apagar diret�rio do projeto no tomcat: " + e.getMessage());
        }
    }

    public void clearServer(IAppServer server) {
        getLog().info("Limpando Application Server");
        try {
            FileUtils.deleteDirectory(server.getWorkDir());
            FileUtils.deleteDirectory(server.getContextDir());
            FileUtils.deleteDirectory(server.getWebappsDir());
            server.getWorkDir().mkdirs();
            server.getContextDir().mkdirs();
            server.getWebappsDir().mkdirs();
        } catch (Exception e) {
            getLog().error("N�o foi poss�vel limpar o Application Server");
        }
    }

    public void copiarContexto(File contextFile, IAppServer server, String finalName) {
        try {
            getLog().info("Adicionando o contexto do Teste: " + contextFile.getAbsolutePath());
            FileUtils.copyFile(contextFile, new File(server.getContextDir(), finalName + ".xml"));
        } catch (Exception e) {
            getLog().error("N�o foi poss�vel copiar o contexto " + contextFile.getAbsolutePath());
        }
    }

    public void copiarWar(IAppServer server, File targetDir, String finalName) throws MojoExecutionException {
        getLog().info("Copiando o war file para " + server.getAppDir());
        try {
            FileUtils.copyFile(new File(targetDir, finalName + ".war"), new File(server.getWebappsDir(), finalName + ".war"));
        } catch (Exception e) {
            throw new MojoExecutionException("N�o foi poss�vel copiar o arquivo " + new File(targetDir, finalName + ".war") + ".war para o Web Server");
        }
    }

    public void descompactarWar(IAppServer server, File targetDir) throws MojoExecutionException, MojoFailureException {
        PlcZip war = null;
        try {
            war = new PlcZip(targetDir + ".war");
        } catch (ZipException e1) {
            throw new MojoExecutionException("O arquivo war " + targetDir + ".war n�o existe.");
        } catch (IOException e1) {
            throw new MojoExecutionException("Ocorreu algum erro ao abrir o arquivo " + targetDir + ".war");
        }
        if (!war.temArquivo("META-INF/context.xml")) throw new MojoFailureException("N�o � poss�vel fazer deploy Completo porque n�o existe arquivo de contexto no war file");
        try {
            getLog().info("Deploying " + war.getName() + " para " + server.getAppDir());
            war.descompactar(server.getAppDir().getAbsolutePath());
        } catch (IOException e) {
            getLog().error("N�o foi poss�vel descompacar alguns arquivos");
        }
        try {
            war.descompactarArquivoEspecifico("META-INF/context.xml", server.getAppContextFile().getAbsolutePath());
        } catch (FileNotFoundException e) {
            getLog().error("N�o existe o arquivo de contexto no war");
        } catch (IOException e) {
            getLog().error("N�o foi poss�vel descompactar o contexto do war");
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
    }

    public void reiniciarAplicacao(IAppServer server) throws MojoFailureException, MojoExecutionException {
        try {
            FileUtils.touch(server.getAppContextFile());
        } catch (IOException e) {
            getLog().debug("N�o foi poss�vel reiniciar o Tomcat porque ocorreu alguma falha ao dar um TOUCH no arquivo web.xml");
        }
    }

    public void iniciarAplicacao(IAppServer server) throws MojoFailureException, MojoExecutionException {
        throw new MojoExecutionException("Falta Implementar");
    }

    public void pararAplicacao(IAppServer server) throws MojoFailureException, MojoExecutionException {
    }
}
