package org.icefaces.application.showcase.view.bean.examples.component.outputResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import com.icesoft.faces.context.ByteArrayResource;
import com.icesoft.faces.context.Resource;

public class OutputResourceBean {

    private Resource imgResource;

    private Resource pdfResource;

    private Resource pdfResourceDynFileName;

    private String fileName = "Choose-a-new-file-name";

    private static final String RESOURCE_PATH = "/WEB-INF/classes/org/icefaces/application/showcase/view/resources/";

    public OutputResourceBean() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        try {
            imgResource = new ByteArrayResource(toByteArray(context.getResourceAsStream(RESOURCE_PATH + "logo.jpg")));
            pdfResource = new ByteArrayResource(toByteArray(context.getResourceAsStream(RESOURCE_PATH + "WP_Security_Whitepaper.pdf")));
            pdfResourceDynFileName = new ByteArrayResource(toByteArray(context.getResourceAsStream(RESOURCE_PATH + "WP_Security_Whitepaper.pdf")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Resource getImgResource() {
        return imgResource;
    }

    public Resource getPdfResource() {
        return this.pdfResource;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len = 0;
        while ((len = input.read(buf)) > -1) output.write(buf, 0, len);
        return output.toByteArray();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Resource getPdfResourceDynFileName() {
        return pdfResourceDynFileName;
    }

    public void setPdfResourceDynFileName(Resource pdfResourceDynFileName) {
        this.pdfResourceDynFileName = pdfResourceDynFileName;
    }
}
