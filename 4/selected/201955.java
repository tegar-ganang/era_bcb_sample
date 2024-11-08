package com.c2b2.ipoint.processing.jobs;

import com.c2b2.ipoint.model.PersistentModelException;
import com.c2b2.ipoint.model.Property;
import com.c2b2.ipoint.processing.search.LuceneIndex;
import java.io.IOException;

/**
  * This Scheduled Job Optimises the Lucene text index
  * <p>
  * $Date: 2005/12/26 21:13:24 $
  * 
  * $Id: OptimiseLuceneIndex.java,v 1.1 2005/12/26 21:13:24 steve Exp $<br/>
  * 
  * Copyright 2005 C2B2 Consulting Limited. All rights reserved.
  * </p>
  * @author $Author: steve $
  * @version $Revision: 1.1 $
  */
public class OptimiseLuceneIndex extends SchedulableJob {

    public OptimiseLuceneIndex() {
    }

    public void executeJob() throws JobException {
        try {
            Property indexProperty = Property.getProperty("TextIndexPath");
            LuceneIndex index = new LuceneIndex(indexProperty.getValue());
            index.optimise();
        } catch (IOException e) {
            throw new JobException("Unable to read or write the index", e);
        } catch (PersistentModelException e) {
            throw new JobException("Unable to find the directory to put the index into", e);
        }
    }
}
