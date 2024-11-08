package com.siasal.view.backing.documentos;

import com.common.files.FileManager;
import com.common.to.ServiceRequestTO;
import com.commons.deploy.delegate.ServiceDelegate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import oracle.adf.view.faces.component.core.input.CoreInputFile;
import oracle.adf.view.faces.context.AdfFacesContext;
import oracle.adf.view.faces.model.UploadedFile;

/**
 * Managed Backing bean for the FileUpload page
 * $Id: ContenidoUpload.java,v 1.1 2007/03/23 06:03:08 FERNANDO Exp $.
 */
public class ContenidoUpload {

    private String fileName;

    private CoreInputFile srInputFile;

    /**
     * Cancels out of the dialog by calling the returnFromDialog() method.
     * @return empty navigation rule
     */
    public String closeFileUpload_action() {
        AdfFacesContext.getCurrentInstance().returnFromDialog(fileName, null);
        return null;
    }

    private String getContentDirectory() {
        try {
            ServiceRequestTO serviceRequestTO = new ServiceRequestTO("documentos.getContentDirectory");
            ServiceDelegate serviceDelegate = new ServiceDelegate();
            String pathContent = (String) serviceDelegate.executeService(serviceRequestTO);
            return pathContent;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * This fileUploaded method handles a value change event
     * for a file upload and writes the file to a locally defined location.
     * @param event value change event
     */
    public void fileUploaded(ValueChangeEvent event) {
        InputStream in;
        FileOutputStream out;
        String fileUploadLoc = getContentDirectory();
        if (fileUploadLoc == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "No hay directorio para guardar los contenidos", null);
            context.addMessage(event.getComponent().getClientId(context), message);
        }
        UploadedFile file = (UploadedFile) event.getNewValue();
        if (file != null && file.getLength() > 0) {
            System.out.println("sizes:" + file.getLength());
            FacesContext context = FacesContext.getCurrentInstance();
            FacesMessage message = new FacesMessage("Exito" + " " + file.getFilename() + " (" + file.getLength() + " bytes)");
            context.addMessage(event.getComponent().getClientId(context), message);
            try {
                out = new FileOutputStream(FileManager.concat(fileUploadLoc, file.getFilename()));
                in = file.getInputStream();
                while (in.available() > 0) {
                    out.write(in.read());
                }
                in.close();
                out.close();
                fileName = file.getFilename();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String filename = file != null ? file.getFilename() : null;
            String byteLength = file != null ? "" + file.getLength() : "0";
            FacesContext context = FacesContext.getCurrentInstance();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "error" + " " + filename + " (" + byteLength + " bytes)", null);
            context.addMessage(event.getComponent().getClientId(context), message);
        }
    }

    /**
     * Launch the upload - see fileUploaded() for actual upload handling.
     * @return null navigation event - we stay on this page
     */
    public String UploadButton_action() {
        if (this.getSrInputFile().getValue() == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "otro error", null);
            context.addMessage(this.getSrInputFile().getId(), message);
        }
        AdfFacesContext.getCurrentInstance().returnFromDialog(fileName, null);
        return null;
    }

    /**
     * Setter for inputFile UI Component.
     * @param inputFile inputFile UI component
     */
    public void setSrInputFile(CoreInputFile inputFile) {
        this.srInputFile = inputFile;
    }

    /**
     * Getter for inputFile UI Component.
     * @return inputFile UI component
     */
    public CoreInputFile getSrInputFile() {
        return srInputFile;
    }
}
