package com.wrupple.muba.catalogs.server.integration.handlers.impl;

import java.util.List;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.wrupple.muba.catalogs.server.integration.handlers.CatalogQueryRequestHandler;
import com.wrupple.muba.catalogs.server.integration.locator.CatalogMetadataQueryServiceLocator;
import com.wrupple.muba.catalogs.server.state.catalogMetadata.UserCatalogListBuilder;
import com.wrupple.muba.catalogs.server.state.catalogMetadata.UserCatalogListWriter;
import com.wrupple.vegetate.server.integration.impl.ProcessDelegatingRequestHandler;
import com.wrupple.vegetate.server.process.impl.SimpleRequestHandlingProcess;

public class CatalogQueryRequestHandlerImpl extends ProcessDelegatingRequestHandler implements CatalogQueryRequestHandler {

    @Inject
    public CatalogQueryRequestHandlerImpl(EventBus bus, UserCatalogListBuilder reader, UserCatalogListWriter writer, CatalogMetadataQueryServiceLocator childServiceLocator) {
        super(bus, new SimpleRequestHandlingProcess<List<String>>(reader, writer), childServiceLocator);
    }
}
