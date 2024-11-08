package br.org.acessobrasil.portal.servlet;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import br.org.acessobrasil.portal.action.ArquivoAction;

/**
 * Servlet implementation class ArquivoServlet
 */
public class ArquivoServlet extends HttpServlet {

    private static Logger logger = Logger.getLogger(ArquivoServlet.class);

    private static final long serialVersionUID = 1L;

    private ArquivoAction conf = new ArquivoAction();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ArquivoServlet() {
        super();
    }

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURL = request.getRequestURI().replace(request.getContextPath() + request.getServletPath(), "");
        if (requestURL.startsWith("/")) {
            requestURL = requestURL.substring(1);
        }
        requestURL = "arquivos/" + requestURL;
        String arqPath = conf.getArquivosPath() + requestURL;
        File arq = new File(arqPath);
        if (arq.exists()) {
            response.getOutputStream().write(FileUtils.readFileToByteArray(arq));
        } else {
            logger.warn("Arquivo nao encontrado " + arqPath + " requestURL = '" + requestURL + "'");
            response.setStatus(404);
        }
    }
}
