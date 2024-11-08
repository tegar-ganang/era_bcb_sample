package uk.ac.ebi.imex.psivalidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is the managed bean that contains the model of the information show to the user. From this bean,
 * all the information shown is handled. It creates the reports, etc.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: PsiValidatorBean.java 5691 2006-08-04 16:50:40Z baranda $
 * @since <pre>12-Jun-2006</pre>
 */
public class PsiValidatorBean implements Serializable {

    /**
     * Logging is an essential part of an application
     */
    private static final Log log = LogFactory.getLog(PsiValidatorBean.class);

    /**
     * If true, a local file is selected to be uploaded
     */
    private boolean uploadLocalFile;

    /**
     * The file to upload
     */
    private UploadedFile psiFile;

    /**
     * The URL to upload
     */
    private String psiUrl;

    /**
     * If we are viewing a report, this is the report viewed
     */
    private PsiReport currentPsiReport;

    /**
     * Constructor
     */
    public PsiValidatorBean() {
        this.uploadLocalFile = true;
    }

    /**
     * This is a valueChangeEvent. When the selection of File/URL is changed, this event is fired.
     * @param vce needed in valueChangeEvent methods. From it we get the new value
     */
    public void uploadTypeChanged(ValueChangeEvent vce) {
        String type = (String) vce.getNewValue();
        uploadLocalFile = type.equals("local");
        if (log.isDebugEnabled()) log.debug("Upload type changed, is local file? " + uploadLocalFile);
    }

    /**
     * This is an event thrown when the Upload button has been clicked
     * @param evt
     */
    public void uploadFile(ActionEvent evt) {
        try {
            if (uploadLocalFile) {
                uploadFromLocalFile();
            } else {
                uploadFromUrl();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the local file
     * @throws IOException if something has gone wrong with the file
     */
    private void uploadFromLocalFile() throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Uploading local file: " + psiFile.getName());
        }
        byte[] content = psiFile.getBytes();
        String name = psiFile.getName();
        InputStream is = new ByteArrayInputStream(content);
        PsiReportBuilder builder = new PsiReportBuilder(name, is);
        this.currentPsiReport = builder.createPsiReport();
    }

    /**
     * Reads the file from a URL, so it can read locally and remotely
     * @throws IOException if something goes wrong with the file or the connection
     */
    private void uploadFromUrl() throws IOException {
        if (log.isInfoEnabled()) {
            log.info("Uploading Url: " + psiUrl);
        }
        try {
            URL url = new URL(psiUrl);
            String name = psiUrl.substring(psiUrl.lastIndexOf("/") + 1, psiUrl.length());
            PsiReportBuilder builder = new PsiReportBuilder(name, url);
            this.currentPsiReport = builder.createPsiReport();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is a "validator" method. It has the arguments that JSF specifies for this kind of methods.
     * The objective is to validate the URL provided by the user, whether it is in the correct form
     * or the place where it points it does exist
     * @param context The JSF FacesContext
     * @param toValidate The UIComponent to validate (this is a UIInput component), the controller of the text box
     * @param value The value provided in the text box by the user
     */
    public void validateUrlFormat(FacesContext context, UIComponent toValidate, Object value) {
        if (log.isDebugEnabled()) {
            log.debug("Validating URL: " + value);
        }
        currentPsiReport = null;
        URL url = null;
        UIInput inputCompToValidate = (UIInput) toValidate;
        String toValidateClientId = inputCompToValidate.getClientId(context);
        try {
            url = new URL((String) value);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            inputCompToValidate.setValid(false);
            context.addMessage(toValidateClientId, new FacesMessage("Not a valid URL"));
            return;
        }
        try {
            url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
            inputCompToValidate.setValid(false);
            context.addMessage(toValidateClientId, new FacesMessage("Unknown URL"));
        }
    }

    public boolean isUploadLocalFile() {
        return uploadLocalFile;
    }

    public void setUploadLocalFile(boolean uploadLocalFile) {
        this.uploadLocalFile = uploadLocalFile;
    }

    public UploadedFile getPsiFile() {
        return psiFile;
    }

    public void setPsiFile(UploadedFile psiFile) {
        this.psiFile = psiFile;
    }

    public String getPsiUrl() {
        return psiUrl;
    }

    public void setPsiUrl(String psiUrl) {
        this.psiUrl = psiUrl;
    }

    public PsiReport getCurrentPsiReport() {
        return currentPsiReport;
    }

    public void setCurrentPsiReport(PsiReport currentPsiReport) {
        this.currentPsiReport = currentPsiReport;
    }
}