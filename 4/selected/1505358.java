package br.org.ged.direto.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import br.org.direto.util.Config;
import br.org.ged.direto.model.entity.Anexo;
import br.org.ged.direto.model.entity.Usuario;
import br.org.ged.direto.model.service.AnexoService;
import br.org.ged.direto.model.service.SegurancaService;

@Controller
public class FileUploadController {

    @Autowired
    private Config config;

    @Autowired
    private SegurancaService segurancaService;

    @Autowired
    private AnexoService anexoService;

    private String UPLOAD_DIRECTORY;

    private String uploadDiretory() {
        return config.baseDir + "/arquivos_upload_direto/";
    }

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handlerDocumentNotFoundException(RuntimeException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("error", ex.getMessage());
        return mav;
    }

    @SuppressWarnings("static-access")
    @RequestMapping(value = "/upload/upload.html", method = RequestMethod.POST)
    protected void save(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        UPLOAD_DIRECTORY = uploadDiretory();
        File diretorioUsuario = new File(UPLOAD_DIRECTORY);
        boolean diretorioCriado = false;
        if (!diretorioUsuario.exists()) {
            diretorioCriado = diretorioUsuario.mkdir();
            if (!diretorioCriado) throw new RuntimeException("Não foi possível criar o diretório do usuário");
        }
        PrintWriter writer = null;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            writer = response.getWriter();
        } catch (IOException ex) {
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        }
        String filename = request.getHeader("X-File-Name");
        try {
            is = request.getInputStream();
            fos = new FileOutputStream(new File(UPLOAD_DIRECTORY + filename));
            IOUtils.copy(is, fos);
            response.setStatus(response.SC_OK);
            writer.print("{success: true}");
        } catch (FileNotFoundException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        } catch (IOException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        } finally {
            try {
                fos.close();
                is.close();
            } catch (IOException ignored) {
            }
        }
        writer.flush();
        writer.close();
    }

    @SuppressWarnings("static-access")
    @RequestMapping(value = "/upload/check.html", method = RequestMethod.POST)
    protected void checkSign(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        UPLOAD_DIRECTORY = uploadDiretory();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) auth.getPrincipal();
        PrintWriter writer = null;
        InputStream is = null;
        try {
            writer = response.getWriter();
        } catch (IOException ex) {
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        }
        int idAnexo = Integer.parseInt(request.getHeader("X-File-Name"));
        try {
            is = request.getInputStream();
            Anexo anexo = anexoService.selectById(idAnexo);
            boolean match = false;
            if (segurancaService.haveCertificate(usuario.getUsuIdt()) && anexo.getAssinaturaHash().length() > 40) {
                match = segurancaService.checkSignature(is, idAnexo);
                response.setStatus(response.SC_OK);
                writer.print("{success: " + match + "}");
            } else {
                String fileHash = segurancaService.sh1withRSA(is);
                File fileToCheck = new File(UPLOAD_DIRECTORY + anexo.getAnexoCaminho());
                String sha1 = "";
                try {
                    sha1 = segurancaService.sh1withRSA(fileToCheck);
                    anexo.setHash(sha1);
                } catch (Exception e) {
                    anexo.setHash("Não foi possível ler o arquivo.");
                    e.printStackTrace();
                }
                match = fileHash.equals(sha1) ? true : false;
                response.setStatus(response.SC_OK);
                writer.print("{success: " + match + "}");
            }
        } catch (FileNotFoundException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        } catch (IOException ex) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            writer.print("{success: false}");
            System.err.println(FileUploadController.class.getName() + "has thrown an exception: " + ex.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
        writer.flush();
        writer.close();
    }
}
