package br.com.caelum.jambo.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import org.vraptor.annotations.Component;
import org.vraptor.annotations.InterceptedBy;
import org.vraptor.annotations.Logic;
import br.com.caelum.jambo.dao.DaoFactory;
import br.com.caelum.jambo.interceptors.DaoInterceptor;
import br.com.caelum.jambo.model.Curriculum;

@Component
@InterceptedBy(DaoInterceptor.class)
public class DownloadLogic {

    private final DaoFactory daoFactory;

    private final ServletContext context;

    private final HttpServletResponse response;

    public DownloadLogic(DaoFactory daoFactory, HttpServletResponse response, ServletContext context) {
        this.daoFactory = daoFactory;
        this.response = response;
        this.context = context;
    }

    @Logic(parameters = "id")
    public void curriculum(long id) throws IOException {
        Curriculum curriculum = daoFactory.getCurriculumDao().load(id);
        String fileName = curriculum.getInternalName();
        File file = new File(context.getRealPath(Curriculum.UPLOADED_FILES_PATH + "/" + fileName));
        if (!file.exists()) {
            System.out.println("Erro!!!");
        }
        response.setContentType(curriculum.getRealFileContentType());
        response.setHeader("Content-disposition", "attachment; filename=" + curriculum.getRealFileName());
        response.setContentLength((int) file.length());
        OutputStream stream = response.getOutputStream();
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[16384];
        while (fis.available() != 0) {
            int read = fis.read(buffer);
            stream.write(buffer, 0, read);
        }
        stream.flush();
        response.flushBuffer();
    }
}
