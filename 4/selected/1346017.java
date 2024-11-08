package com.jandan.web.admin;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StringMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import com.jandan.logic.JWordzFacade;

public class ImportWordsController extends SimpleFormController {

    private JWordzFacade jwordz;

    public void setJwordz(JWordzFacade jwordz) {
        this.jwordz = jwordz;
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        WordLibFileUploadBean bean = (WordLibFileUploadBean) command;
        MultipartFile file = bean.getFile();
        if (file == null) {
            throw new Exception("file is null");
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        List<String> list = new ArrayList<String>();
        String s = null;
        while ((s = in.readLine()) != null) {
            list.add(s);
        }
        long id = Long.parseLong(bean.getWordLibID());
        jwordz.importWords(list.toArray(new String[] {}), id);
        in.close();
        return new ModelAndView(this.getSuccessView() + "&wordLibID=" + id);
    }

    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
        binder.registerCustomEditor(String.class, new StringMultipartFileEditor());
    }

    /**
	 * 保存文件
	 * 
	 * @param stream
	 * @param path
	 * @param filename
	 * @throws IOException
	 */
    private void SaveFileFromInputStream(InputStream stream, String path, String filename) throws IOException {
        FileOutputStream fs = new FileOutputStream(path + "/" + filename);
        byte[] buffer = new byte[1024 * 1024];
        int bytesum = 0;
        int byteread = 0;
        while ((byteread = stream.read(buffer)) != -1) {
            bytesum += byteread;
            fs.write(buffer, 0, byteread);
            fs.flush();
        }
        fs.close();
        stream.close();
    }
}
