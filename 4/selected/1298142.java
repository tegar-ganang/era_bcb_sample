package net.sf.woko.facets.command;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Blob;
import net.sf.woko.facets.FacetConstants;
import net.sf.woko.usermgt.IWokoUserManager;
import net.sf.woko.util.Util;
import net.sourceforge.jfacets.annotations.FacetKey;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.validation.SimpleError;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

@FacetKey(name = FacetConstants.uploadBlobCommand, profileId = IWokoUserManager.ROLE_WOKO_USER)
public class UploadBlobCommand extends BaseBlobCommand {

    private static final Logger logger = Logger.getLogger(UploadBlobCommand.class);

    /** FileBean used for upload */
    private FileBean data;

    public FileBean getData() {
        return data;
    }

    public void setData(FileBean data) {
        this.data = data;
    }

    protected Resolution handleExecute(ActionBeanContext actionBeanContext, PropertyDescriptor pd) {
        if (data == null) {
            actionBeanContext.getValidationErrors().add("command.data", new SimpleError("Submitted file contains no data !"));
            return actionBeanContext.getSourcePageResolution();
        } else {
            Blob blob;
            try {
                blob = Hibernate.createBlob(data.getInputStream());
            } catch (IOException e) {
                String msg = "IOException caught while trying to create Blob from FileBean";
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
            if (blob == null) {
                String msg = "Hibernate returned null when trying to create Blob from FileBean !";
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            Method writeMethod = pd.getWriteMethod();
            if (writeMethod == null) {
                String msg = "Property " + getTargetObject().getClass().getSimpleName() + "." + getBlobPropertyName() + " is read only (no write method found)";
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            try {
                writeMethod.invoke(getTargetObject(), new Object[] { blob });
            } catch (Exception e) {
                String msg = "Error while setting Blob property " + getTargetObject().getClass().getSimpleName() + "." + getBlobPropertyName() + " using write method";
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            try {
                getSession().update(getTargetObject());
                getPersistenceUtil().commit();
                actionBeanContext.getMessages().add(new SimpleMessage("Data transmitted"));
                return Util.redirectToView(getTargetObject(), getPersistenceUtil());
            } catch (Throwable t) {
                logger.error("Error while saving data", t);
                actionBeanContext.getValidationErrors().addGlobalError(new SimpleError("Error while transmitting the file. Maybe it's too long ?"));
                return actionBeanContext.getSourcePageResolution();
            }
        }
    }
}
