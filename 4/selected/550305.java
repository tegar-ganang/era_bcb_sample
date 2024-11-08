package org.gruposp2p.controldatosgob.servidor.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.multipart.FormDataParam;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author jj
 */
@Path(value = "/conjuntoDeDatos")
@Component
@Scope("request")
public class RecursoConjuntoDeDatos {

    private static Logger logger = LoggerFactory.getLogger(RecursoConjuntoDeDatos.class);

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String postData(@FormDataParam("archivo") InputStream archivo, @FormDataParam("tipoArchivo") String tipoArchivo, @FormDataParam("descripcion") String descripcion) {
        String result = "ok";
        logger.debug("tipoArchivo: " + tipoArchivo);
        logger.debug("descripcion: " + descripcion);
        try {
            File f = new File("oooutFile.xml");
            OutputStream out = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = archivo.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            archivo.close();
            logger.debug("\nFile is created...................................");
        } catch (IOException e) {
            logger.error(null, e);
        }
        return result;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response get() {
        return Response.ok().entity("Niskawskas").build();
    }

    private String processFileName(String fileNameInput) {
        String fileNameOutput = fileNameInput.substring(fileNameInput.lastIndexOf("\\") + 1, fileNameInput.length());
        return fileNameOutput;
    }
}
