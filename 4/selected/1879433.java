package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.Product;
import modelo.ProductType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import web.spring.FileUploadBean;
import web.spring.RegistraProducto;
import services.actualiza.UpdateProductoAction;
import services.busqueda.GetProductAction;
import services.busqueda.GetProductTypeAction;

public class FileUploadController extends SimpleFormController {

    protected final Log logger = LogFactory.getLog(getClass());

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        FileUploadBean bean = (FileUploadBean) command;
        MultipartFile file = multipartRequest.getFile("file");
        if (file == null) {
            logger.debug("Esta jodido");
            return new ModelAndView(new RedirectView("fallo.html"));
        } else try {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            if (file.getSize() > 0) {
                inputStream = file.getInputStream();
                File realUpload = new File("/opt/tomcat-6/tmp/");
                outputStream = new FileOutputStream("/opt/tomcat-6/wtpwebapps/hafnerwebsite/images/" + file.getOriginalFilename());
                logger.debug("====22=========");
                logger.debug(file.getOriginalFilename());
                logger.debug("=============");
                int readBytes = 0;
                byte[] buffer = new byte[8192];
                while ((readBytes = inputStream.read(buffer, 0, 8192)) != -1) {
                    logger.debug("===ddd=======");
                    outputStream.write(buffer, 0, readBytes);
                }
                GetProductAction busca = new GetProductAction();
                Product obtenido = busca.getSelectedProductbyIDProduct(bean.getId());
                obtenido.setProduct_image(file.getOriginalFilename());
                UpdateProductoAction actualiza = new UpdateProductoAction();
                if (actualiza.setSelectedProduct(obtenido.getID())) {
                    actualiza.setUpdateProduct(obtenido);
                    actualiza.updateProduct();
                    outputStream.close();
                    inputStream.close();
                    return new ModelAndView("products4Users");
                } else return new ModelAndView(new RedirectView("fallo.html"));
            } else logger.debug("Esta jodido");
            return new ModelAndView(new RedirectView("fallo.html"));
        } catch (IOException e) {
            logger.debug("Esta jodido");
            return new ModelAndView(new RedirectView("fallo.html"));
        }
    }
}
