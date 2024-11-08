package com.c2b2.ipoint.scripts;

import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.Property;
import com.c2b2.ipoint.processing.search.LuceneIndex;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
  * $Id: OptimiseIndex.java,v 1.1 2005/12/26 23:00:31 steve Exp $
  * 
  * Copyright 2004 C2B2 Consulting Limited. All rights reserved.
  * 
  * This script bean rebuilds the Lucene Text index
  * 
  * @author $Author: steve $
  * @version $Revision: 1.1 $
  * $Date: 2005/12/26 23:00:31 $
  * 
  */
public class OptimiseIndex implements ScriptBean {

    public OptimiseIndex() {
    }

    public void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            Property indexProperty = Property.getProperty("TextIndexPath");
            LuceneIndex index = new LuceneIndex(indexProperty.getValue());
            index.optimise();
            response.getWriter().write("Index Optimised Successfully");
        } catch (IOException e) {
            throw new ServletException("Unable to read or write the index", e);
        } catch (PersistentModelException e) {
            throw new ServletException("Unable to find the directory to put the index into", e);
        }
    }
}
