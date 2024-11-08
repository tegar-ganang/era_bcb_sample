package com.bayareasoftware.tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ChartServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ChartController.get().configure(config);
        this.log("initialized chart controller");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        ChartController cc = ChartController.get();
        File cacheDir = cc.getCacheRoot();
        File img = new File(cacheDir, pathInfo);
        if (!img.isFile()) {
            resp.sendError(404);
            return;
        }
        InputStream is = new FileInputStream(img);
        OutputStream os = resp.getOutputStream();
        resp.setContentLength((int) img.length());
        resp.setContentType("image/png");
        try {
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) > 0) os.write(buf, 0, r);
        } finally {
            is.close();
        }
    }

    private static void p(String s) {
        System.out.println("[ChartServlet] " + s);
    }
}
