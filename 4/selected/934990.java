package br.org.acessobrasil.portal.filtro;

import java.io.File;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import br.org.acessobrasil.portal.Gerenciador;
import br.org.acessobrasil.portal.action.ArquivoAction;
import br.org.acessobrasil.portal.modelo.Sitio;

public class FiltroDeCss implements Filter {

    private static Logger logger = Logger.getLogger(FiltroDeCss.class);

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String url = httpRequest.getRequestURL().toString();
        ArquivoAction arquivoAction = new ArquivoAction();
        String barra = String.valueOf(File.separatorChar);
        try {
            String pathLocal = arquivoAction.getArquivosPath();
            if (!pathLocal.endsWith(barra)) {
                pathLocal = pathLocal + barra;
            }
            String[] urlSplitted = url.split("/css/");
            Sitio sitio = (Sitio) httpRequest.getSession().getAttribute(Gerenciador.KEY_ULTIMO_SITIO);
            if (sitio == null) {
                sitio = Gerenciador.encontrarSitioUrl(url);
            }
            String pastaSitio = sitio.getNoPastaArquivos();
            StringBuilder pathArquivoLocal = new StringBuilder(pathLocal);
            pathArquivoLocal.append(pastaSitio);
            pathArquivoLocal.append(barra);
            pathArquivoLocal.append("css");
            pathArquivoLocal.append(barra);
            pathArquivoLocal.append(urlSplitted[1]);
            File arquivo = new File(pathArquivoLocal.toString());
            if (arquivo.exists()) {
                String token = "\"M" + arquivo.lastModified() + '"';
                String previousToken = httpRequest.getHeader("If-None-Match");
                if (previousToken != null && previousToken.equals(token)) {
                    httpResponse.reset();
                    logger.debug("ETag match: returning 304 Not Modified");
                    httpResponse.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    httpResponse.setHeader("Last-Modified", httpRequest.getHeader("If-Modified-Since"));
                } else {
                    httpResponse.setHeader("ETag", token);
                    httpResponse.setDateHeader("Last-Modified", arquivo.lastModified());
                    httpResponse.getOutputStream().write(FileUtils.readFileToByteArray(arquivo));
                }
            } else {
                chain.doFilter(request, response);
                logger.warn("404 pathArquivoLocal = " + pathArquivoLocal);
            }
        } catch (Exception e) {
            logger.error("Erro ao filtrar arquivo", e);
        }
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }
}
