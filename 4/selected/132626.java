package br.org.acessobrasil.portal.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import br.org.acessobrasil.portal.modelo.Usuario;
import br.org.acessobrasil.portal.persistencia.LogDao;

public class InstalarTemplateAction extends Super {

    public static String PASTA_TEMPLATE = "templates";

    private static final long serialVersionUID = 289368817449161716L;

    private static Logger logger = Logger.getLogger(InstalarTemplateAction.class);

    private File fileUpload;

    private String fileUploadContentType;

    private String fileUploadFileName;

    protected static final int BUFFER_TAMANHO = 2048;

    private int progresso = 0;

    public int getProgresso() {
        return progresso;
    }

    @Override
    public String execute() throws Exception {
        Usuario usuarioLogado = getUsuarioLogado();
        if (fileUpload != null && !fileUploadFileName.equals("")) {
            try {
                File zipFile = null;
                {
                    String nomeRelativo = PASTA_TEMPLATE + '/' + fileUploadFileName;
                    String fullFileName = getServletContext().getRealPath(nomeRelativo);
                    zipFile = new File(fullFileName);
                    FileUtils.copyFile(fileUpload, zipFile);
                }
                File folderDestino = new File(getServletContext().getRealPath(PASTA_TEMPLATE + '/' + fileUploadFileName.substring(0, fileUploadFileName.lastIndexOf("."))));
                if (folderDestino.exists()) {
                    FileUtils.cleanDirectory(folderDestino);
                    FileUtils.deleteDirectory(folderDestino);
                }
                folderDestino = new File(getServletContext().getRealPath(PASTA_TEMPLATE));
                descompactar(zipFile, folderDestino);
                usuarioLogado.addActionMessage("Template instalado com sucesso.");
                usuarioLogado.addActionMessage("Pasta de destino:" + folderDestino.getAbsolutePath());
                LogDao.getInstance().addLog(usuarioLogado, "Instalou o template " + fileUploadFileName);
            } catch (Exception e) {
                usuarioLogado.addActionError("Erro ao instalar o template. " + e.getMessage());
                logger.error("Erro ao instalar template", e);
            }
        }
        return SUCCESS;
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

    /**
	 * 
	 * @param file
	 * @param basePath
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    public void descompactar(File file, File basePath) throws IOException {
        ZipFile zipFile = null;
        File fileAtual = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        byte[] buffer = new byte[BUFFER_TAMANHO];
        try {
            if (!basePath.exists() || !basePath.isDirectory()) {
                throw new IOException("Informe um diret&oacute;rio v&aacute;lido");
            }
            zipFile = new ZipFile(file);
            int i = 0, total = zipFile.size();
            Enumeration e = zipFile.entries();
            while (e.hasMoreElements()) {
                progresso = (++i * 100) / total;
                ZipEntry entrada = (ZipEntry) e.nextElement();
                fileAtual = new File(basePath, entrada.getName());
                if (entrada.isDirectory() && !fileAtual.exists()) {
                    fileAtual.mkdirs();
                    continue;
                }
                if (!fileAtual.getParentFile().exists()) {
                    fileAtual.getParentFile().mkdirs();
                }
                try {
                    inputStream = zipFile.getInputStream(entrada);
                    outputStream = new FileOutputStream(fileAtual);
                    int bytesLidos = 0;
                    if (inputStream == null) {
                        throw new ZipException("Erro ao ler a entrada do zip: " + entrada.getName());
                    }
                    while ((bytesLidos = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesLidos);
                    }
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception ex) {
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
