package com.c2b2.ipoint.scripts;

import com.c2b2.ipoint.model.HibernateUtil;
import com.c2b2.ipoint.model.Page;
import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.Portlet;
import com.c2b2.ipoint.model.PortletType;
import com.c2b2.ipoint.model.Property;
import com.c2b2.ipoint.model.View;
import com.c2b2.ipoint.presentation.PresentationException;
import com.c2b2.ipoint.presentation.portlets.PortalFileRepository;
import com.c2b2.ipoint.processing.PortalRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;

/**
  * This class implements the Do Post Install Script bean and does all the required
  * post install clean up.
  * <p>
  * $Date: 2006/10/18 10:38:10 $
  * 
  * $Id: DoPostInstall.java,v 1.3 2006/10/18 10:38:10 matt Exp $<br>
  * 
  * Copyright 2005 C2B2 Consulting Limited. All rights reserved.
  * </p>
  * @author $Author: matt $
  * @version $Revision: 1.3 $
  */
public class DoPostInstall implements ScriptBean {

    public static final String POST_INSTALL_PROPERTY = "PostInstallDone";

    public DoPostInstall() {
    }

    public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            PrintWriter writer = response.getWriter();
            PortalRequest pr = PortalRequest.getCurrentRequest();
            String allReadyRun = Property.getPropertyValue(POST_INSTALL_PROPERTY);
            if (allReadyRun != null && allReadyRun.equalsIgnoreCase("True")) {
                writer.println("Post Install Has Already Been Run!!");
            } else {
                Query query = HibernateUtil.currentSession().createQuery("from com.c2b2.ipoint.model.PortletType pt where pt.Name = 'com.c2b2.ipoint.presentation.portlets.PortalFileRepository'");
                PortletType docRepoType = (PortletType) query.uniqueResult();
                Portlet portlet = Portlet.createPortlet("Portal Files", pr.getCurrentUser(), docRepoType);
                PortalFileRepository prenderer = (PortalFileRepository) pr.getRenderFactory().getPortletRenderer(portlet);
                prenderer.initialiseNew();
                Query pageQuery = HibernateUtil.currentSession().createQuery("from com.c2b2.ipoint.model.Page p where p.Name = 'Portal Files'");
                Page page = (Page) pageQuery.uniqueResult();
                Set<View> views = page.getViews();
                View view = views.iterator().next();
                view.addPortlet(2, portlet);
                Property.createProperty(POST_INSTALL_PROPERTY, "True");
                Property.createProperty("PortalDocumentRepository", Long.toString(prenderer.getRepository().getID()));
                writer.println("Post Install Run Successfully <a href='ipoint' title='Return to Home Page'>Return to the Home page </a>");
            }
        } catch (PersistentModelException e) {
            throw new ServletException(e);
        } catch (PresentationException e) {
            throw new ServletException(e);
        } catch (IOException ioe) {
            throw new ServletException(ioe);
        }
    }
}
