package edu.harvard.iq.safe.saasystem.web.auditschema;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 *
 * @author Akio Sone
 */
@ManagedBean
@SessionScoped
public abstract class AbstractAuditSchemaOptionSetManagedBean implements Serializable {

    static Logger logger = Logger.getLogger(AbstractAuditSchemaOptionSetManagedBean.class.getName());

    /** Creates a new instance of AbstractAuditSchemaOptionSetManagedBean */
    public AbstractAuditSchemaOptionSetManagedBean() {
    }

    @PostConstruct
    public abstract void initialize();

    public boolean backupFile(File oldFile, File newFile) {
        boolean isBkupFileOK = false;
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(oldFile).getChannel();
            targetChannel = new FileOutputStream(newFile).getChannel();
            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO exception occurred while copying file", e);
        } finally {
            if ((newFile != null) && (newFile.exists()) && (newFile.length() > 0)) {
                isBkupFileOK = true;
            }
            try {
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (targetChannel != null) {
                    targetChannel.close();
                }
            } catch (IOException e) {
                logger.info("closing channels failed");
            }
        }
        return isBkupFileOK;
    }

    public void saveOptionSetFile(Set<String> newOptionSet, File optionSetFileName) {
        PrintWriter pwout = null;
        try {
            pwout = new PrintWriter(optionSetFileName, "UTF8");
            for (String entry : newOptionSet) {
                pwout.println(entry);
            }
        } catch (IOException e) {
            logger.severe("failed to save the new optionSet file");
        } finally {
            pwout.close();
        }
    }

    public abstract void saveNewEntry(ActionEvent actionEvent);

    public String prepareEditOptionSetPage(String option) {
        return "EditAuditSchemaOptionSetFor" + option + ".xhtml";
    }

    public String gotoSchemaInstanceList() {
        return "ListAuditSchemaInstances.xhtml?faces-redirect=true";
    }
}
