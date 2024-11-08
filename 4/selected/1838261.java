package net.sf.doolin.app.svena.ext.impl;

import java.io.File;
import java.io.IOException;
import net.sf.doolin.app.svena.ext.AbstractRepositoryAccessProvider;
import net.sf.doolin.app.svena.service.RepositoryAccessService;
import net.sf.doolin.gui.action.ActionContext;
import net.sf.doolin.util.PropertySet;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@Component
public class LocalRepositoryAccessProvider extends AbstractRepositoryAccessProvider {

    @Autowired
    public LocalRepositoryAccessProvider(RepositoryAccessService repositoryAccessService) {
        super("LocalRepositoryAccessProvider.name", repositoryAccessService);
    }

    @Override
    public String getConfigViewName() {
        return "LocalRepositoryAccessProviderConfigView";
    }

    @Override
    protected File getFile(ActionContext actionContext, PropertySet config, String path) {
        File root = getRoot(config);
        return new File(root, path);
    }

    @Override
    public String getId() {
        return "LocalRepositoryAccessProvider";
    }

    protected File getRoot(PropertySet config) {
        File root = (File) config.get(LocalRepositoryAccessProviderVocabulary.ROOT);
        return root;
    }

    @Override
    public String getSummary(PropertySet config) {
        File root = getRoot(config);
        return root.getPath();
    }

    @Override
    public void initModel(Model model) {
        model.setNsPrefix(LocalRepositoryAccessProviderVocabulary.PREFIX, LocalRepositoryAccessProviderVocabulary.URI);
    }

    @Override
    public void readConfig(Resource resource, PropertySet config) {
        String rootPath = resource.getRequiredProperty(ResourceFactory.createProperty(LocalRepositoryAccessProviderVocabulary.URI, LocalRepositoryAccessProviderVocabulary.ROOT)).getString();
        File root = new File(rootPath);
        config.set(LocalRepositoryAccessProviderVocabulary.ROOT, root);
    }

    @Override
    public void saveConfig(Resource resource, PropertySet config) {
        setProperty(resource, config, LocalRepositoryAccessProviderVocabulary.URI, LocalRepositoryAccessProviderVocabulary.ROOT);
    }

    @Override
    protected void setFile(ActionContext actionContext, PropertySet config, String path, File file) throws IOException {
        File target = getFile(actionContext, config, path);
        FileUtils.copyFile(file, target);
    }
}
