package br.org.ged.direto.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import br.org.direto.util.Config;
import br.org.ged.direto.model.entity.Anexo;
import br.org.ged.direto.model.service.AnexoService;

@Controller
public class FileViewController {

    @Autowired
    private AnexoService anexoService;

    @Autowired
    private Config config;

    byte[] arquivo = null;

    @RequestMapping(value = "/fileview.html", method = RequestMethod.GET)
    public void openFile(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") int idAnexo) throws IOException {
        Anexo anexo = anexoService.selectById(idAnexo);
        File file = new File(config.baseDir + "/arquivos_upload_direto/" + anexo.getAnexoCaminho());
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + anexo.getAnexoNome().replace("/[^a-zA-Z0-9_\\(\\*\\)\\.\\s:;,%$!@#&ãàáâäèéêëìíîïõòóôöùúûüçÃÀÁÂÄÈÉÊËÌÍÎÏÖÒÓÔÙÚÛÜÇ]+/g", "-") + "\"");
        ServletOutputStream ouputStream = response.getOutputStream();
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read = 0;
        while ((read = is.read(buffer)) > 0) {
            ouputStream.write(buffer, 0, read);
        }
        ouputStream.flush();
        ouputStream.close();
    }

    @RequestMapping(value = "/verdocumentoFisico.html", method = RequestMethod.GET)
    public String editFile(ModelMap model, @RequestParam("id") int idAnexo) {
        Anexo anexo = anexoService.selectById(idAnexo);
        model.addAttribute("path", anexo.getAnexoCaminho());
        try {
            InputStream is = new FileInputStream(new File(config.baseDir + "/arquivos_upload_direto/" + anexo.getAnexoCaminho()));
            FileOutputStream fos = new FileOutputStream(new File(config.baseDir + "/temp/" + anexo.getAnexoCaminho()));
            IOUtils.copy(is, fos);
            Runtime.getRuntime().exec("chmod 777 " + config.tempDir + anexo.getAnexoCaminho());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "verdocumentoFisico";
    }

    public static InputStream byteToInputStream(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public static String getFileType(String fileName) {
        String type = "";
        String[] arExtensao = fileName.split("\\.");
        System.out.println(arExtensao.length);
        String extensao = arExtensao[arExtensao.length - 1];
        if (arExtensao.length == 1) extensao = "";
        if (extensao.equals("bmp")) {
            type = "image/bmp";
        } else if (extensao.equals("gif")) {
            type = "image/gif";
        } else if (extensao.equals("jpg")) {
            type = "image/jpeg";
        } else if (extensao.equals("odb")) {
            type = "application/vnd.oasis.opendocument.database";
        } else if (extensao.equals("odp")) {
            type = "application/vnd.oasis.opendocument.presentation";
        } else if (extensao.equals("odt")) {
            type = "application/vnd.oasis.opendocument.text";
        } else if (extensao.equals("pdf")) {
            type = "application/pdf";
        } else if (extensao.equals("png")) {
            type = "image/png";
        } else if (extensao.equals("ppt")) {
            type = "application/powerpoint";
        } else if (extensao.equals("tiff")) {
            type = "image/tiff";
        } else if (extensao.equals("xls")) {
            type = "application/vnd.ms-excel";
        } else if (extensao.equals("txt")) {
            type = "text/plain";
        } else if (extensao.equals("zip")) {
            type = "application/zip";
        } else if (extensao.equals("doc")) {
            type = "application/vnd.ms-word";
        } else if (extensao.equals("ppt")) {
            type = "application/vnd.ms-powerpoint";
        } else {
            type = "application/octet-stream";
        }
        return type;
    }
}
