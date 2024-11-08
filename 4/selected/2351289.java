package scaproject.infra;

import br.com.caelum.vraptor.interceptor.multipart.UploadedFile;
import br.com.caelum.vraptor.ioc.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.tomcat.util.http.fileupload.IOUtils;

@Component
public class FileUpload {

    private File fileFolder;

    public FileUpload(ServletContext context) {
        String path = "C:\\Atividades";
        fileFolder = new File(path);
        if (!fileFolder.exists()) fileFolder.mkdir();
    }

    public void save(UploadedFile file, Long student, Long activity) {
        File destiny = new File(fileFolder, student + "_" + activity + "_" + file.getFileName());
        try {
            IOUtils.copy(file.getFile(), new FileOutputStream(destiny));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao copiar o arquivo.", e);
        }
    }

    public File get(Long student, Long activity, String fileName) {
        return new File(fileFolder, student + "_" + activity + "_" + fileName);
    }
}
