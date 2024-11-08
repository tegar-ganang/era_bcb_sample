package br.edu.ufcg.ourgridportal.server.servlets;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import br.edu.ufcg.ourgridportal.server.filesoperations.RarExtractor;
import br.edu.ufcg.ourgridportal.server.filesoperations.TarExtractor;
import br.edu.ufcg.ourgridportal.server.filesoperations.ZipExtractor;
import br.edu.ufcg.ourgridportal.server.util.OurgridPortalLESProperties;

/**
 * Classe que implementa o servlet de upload (em Desenvolvimento)
 * 
 * @author Arinaldo Segundo
 * @author Cayo Mesquita
 * @author Romeryto Lira
 * 
 */
public class UploadServlet extends HttpServlet {

    private final String FILE_SEPARATOR = System.getProperty("file.separator");

    OurgridPortalLESProperties properties = OurgridPortalLESProperties.getInstance();

    private final String DEFAULT_PATH = properties.getProperty(OurgridPortalLESProperties.UPLOAD_DIRECTORY);

    private static final long serialVersionUID = -4578382495069228990L;

    private static final String UPLOAD_DIR_ATRIBUTE = "uploaddir";

    public UploadServlet() {
        super();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
	 * Método que submete ao servlet o requisição de upload de arquivos
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contentType = request.getContentType();
        String uploadDirectoryName = (String) request.getSession().getAttribute(UPLOAD_DIR_ATRIBUTE);
        ;
        System.out.println("contente type = " + contentType);
        try {
            if ((contentType != null) && (contentType.indexOf("multipart/form-data") >= 0)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                FileItemIterator it = upload.getItemIterator(request);
                while (it.hasNext()) {
                    FileItemStream file = it.next();
                    if (file.getName() != null) {
                        System.out.println("AQUI");
                        processFileItem(file, uploadDirectoryName);
                    } else {
                        System.out.println("FIM DA EXTRAÇÃO");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
	 * Processa file items que correspondam a um arquivo e os salva
	 * adequadamente no servidor
	 * 
	 * @param file
	 * @param uploadDirectoryName 
	 * @throws Exception 
	 */
    private void processFileItem(FileItemStream file, String uploadDirectoryName) throws Exception {
        System.out.println("Entrou no process File");
        System.out.println("Caminho de Upload: " + DEFAULT_PATH);
        String cannonicalPath = (new File(DEFAULT_PATH + FILE_SEPARATOR + uploadDirectoryName)).getCanonicalPath();
        File destinationDir = new File(cannonicalPath);
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        String fileName = file.getName().substring(file.getName().lastIndexOf("\\") + 1, file.getName().length());
        System.out.println("NOME ARQUIVO: " + fileName);
        File localFile = new File(DEFAULT_PATH + FILE_SEPARATOR + uploadDirectoryName + FILE_SEPARATOR + fileName);
        System.out.println("CAMINHO LOCAL FILE: " + localFile);
        try {
            FileOutputStream out = new FileOutputStream(localFile);
            BufferedInputStream in = new BufferedInputStream(file.openStream());
            copyInputStream(in, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("FILE EXTENSION " + readFileExtension(localFile));
        if (readFileExtension(localFile).equals(".zip")) {
            System.out.println("CANNONICAL PATH: " + cannonicalPath + " " + "FILENAME: " + fileName);
            ZipExtractor.unzip(cannonicalPath, fileName, cannonicalPath);
        } else if (readFileExtension(localFile).equals(".gz")) {
            TarExtractor.untargz(cannonicalPath, fileName, cannonicalPath);
        } else if (readFileExtension(localFile).equals(".tar")) {
            TarExtractor.untar(cannonicalPath, fileName, cannonicalPath);
        } else if (readFileExtension(localFile).equals(".bz2")) {
            TarExtractor.untarbz2(cannonicalPath, fileName, cannonicalPath);
        } else if (readFileExtension(localFile).equals(".bz2")) {
            TarExtractor.untgz(cannonicalPath, fileName, cannonicalPath);
        } else if (readFileExtension(localFile).equals(".rar")) {
            RarExtractor.unrar(cannonicalPath, fileName, cannonicalPath);
        }
    }

    private String readFileExtension(File file) {
        String fileAbsolutePath = file.getAbsolutePath();
        return fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf("."));
    }

    /**
	 * Copia os InputStreams
	 * 
	 * @param in
	 *            Stream do arquivo de entrada
	 * @param out
	 *            Stream de saída
	 * @throws IOException
	 *             exceção de entrada e saída
	 */
    public final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
