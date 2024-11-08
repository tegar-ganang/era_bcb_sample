package com.jhyle.sce;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

public class Static extends HttpServlet {

    private static final long serialVersionUID = 4149444608193596412L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathTranslated().substring(0, request.getPathTranslated().length() - request.getPathInfo().length()) + request.getServletPath() + request.getPathInfo();
        File file = new File(path);
        if (file.exists()) {
            FileInputStream in = new FileInputStream(file);
            IOUtils.copyLarge(in, response.getOutputStream());
            in.close();
        }
    }
}
