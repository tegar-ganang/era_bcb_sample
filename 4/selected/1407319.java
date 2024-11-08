package org.jcompany.maven.qa;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Copia war de outro projeto para o App Server. Isso � necess�rio quando um projeto depende de outro projeto deployado
 * no App Server. Os projetos que necessitam ser deployados deve ter a configura��o no pom.xml do modulo principal.
 * Adicione o nome do war entre tags cidependency. O �ltimo war gerado pelo Continuum ser� utilizado.
 * 
 * @goal deploy-dependencias
 * @author Lucas Gon�alves
 * 
 */
public class PlcDependencyCiMojo extends PlcAbstractCiMojo {

    /**
	 * * Ex: <code>
	 * 	<ci>	
	 * 		<cidependencies>
	 * 			<cidependency>meuwar1</cidependency>
	 * 			<cidependency>meuwar2</cidependency>
	 * 		</cidependenies>
	 * 	<ci>
	 * </code>
	 */
    public void execute() throws MojoExecutionException {
        if (!isModuloPrincipal()) return;
        if (ci == null || ci.getCidependencies() == null) return;
        File baseArchives = getArtifactDir();
        for (String warProject : ci.getCidependencies()) {
            File baseArchivesProject = new File(baseArchives, warProject);
            File lastBuildDir = getUltimoBuild(baseArchivesProject);
            File warProjectFile = new File(lastBuildDir, warProject + ".war");
            File contextTest = new File(lastBuildDir, "context.xml");
            try {
                getLog().info("Fazendo deploy do projeto depend�ncia: " + warProject);
                warProjectFile = warProjectFile.getCanonicalFile();
                FileUtils.copyFile(warProjectFile, new File(getServer().getWebappsDir(), warProjectFile.getName()));
                getLog().info("Copiado arquivo: " + warProjectFile.getAbsolutePath());
                contextTest = contextTest.getAbsoluteFile();
                FileUtils.copyFile(contextTest, new File(getServer().getContextDir(), warProject + ".xml"));
                getLog().info("Copiado arquivo: " + contextTest.getAbsolutePath());
            } catch (Exception e) {
                throw new MojoExecutionException("N�o foi poss�vel fazer o deploy do projeto dependencia: " + warProjectFile.getAbsolutePath());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public File getUltimoBuild(File archiveProject) throws MojoExecutionException {
        if (!archiveProject.exists()) throw new MojoExecutionException("N�o existe nenhum war da depend�ncia :" + getCanonicalName(archiveProject) + " para fazer deploy para o teste funcioanal");
        List<File> arquivos = (List<File>) FileUtils.listFiles(archiveProject, new String[] { "war" }, true);
        File last = null;
        for (File file : arquivos) {
            if (last == null) {
                last = file;
                continue;
            }
            if (FileUtils.isFileNewer(file, last) && new File(file.getParent(), "context.xml").exists()) last = file;
        }
        last = last.getParentFile();
        if (last == null) throw new MojoExecutionException("N�o existe nenhum war da depend�ncia :" + getCanonicalName(archiveProject) + " para fazer deploy para o teste funcioanal");
        return last;
    }

    public String getCanonicalName(File name) {
        try {
            return name.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
