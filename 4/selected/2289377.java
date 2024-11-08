package org.gems.designer.metamodel.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.StringTokenizer;
import org.gems.designer.GemsPlugin;

public class GemsPluginGenerator {

    private File target_;

    private ModelProviderTemplate providerGen_ = new ModelProviderTemplate();

    private GraphicsProviderTemplate graphicsProviderGen_ = new GraphicsProviderTemplate();

    private LabelProviderTemplate labelProviderGen_ = new LabelProviderTemplate();

    private AtomTemplate atomGen_ = new AtomTemplate();

    private ModelTemplate modelGen_ = new ModelTemplate();

    private PluginXMLTemplate xmlTemplate_ = new PluginXMLTemplate();

    private PluginBuildTemplate buildTemplate_ = new PluginBuildTemplate();

    private PluginTemplate pluginTemplate_ = new PluginTemplate();

    private PaletteProviderTemplate paletteTemplate_ = new PaletteProviderTemplate();

    private EditorTemplate editorTemplate_ = new EditorTemplate();

    private WizardTemplate wizardTemplate_ = new WizardTemplate();

    private WizardPageTemplate wizardPageTemplate_ = new WizardPageTemplate();

    private BuildPropertiesTemplate buildPropsTemplate_ = new BuildPropertiesTemplate();

    private IconGenerator iconGen_ = new IconGenerator();

    private ConstraintsCheckerTemplate consGen_ = new ConstraintsCheckerTemplate();

    private ConnectionTypeTemplate conTypeGen_ = new ConnectionTypeTemplate();

    private AttributeValidatorsTemplate validatorsGen_ = new AttributeValidatorsTemplate();

    private ClasspathPropertiesTemplate cpathGen_ = new ClasspathPropertiesTemplate();

    private ModelVisitorTemplate visitorTemplate_ = new ModelVisitorTemplate();

    private VisitorImplTemplate visitorImplTemplate_ = new VisitorImplTemplate();

    private VisitorActionSetTemplate visitorActionSet_ = new VisitorActionSetTemplate();

    private ReadmeTemplate readmeTemplate_ = new ReadmeTemplate();

    private File sourceTarget_;

    /**
	 * 
	 */
    public GemsPluginGenerator() {
        super();
    }

    public void generate(ModelProviderInfo info, File projectroot, File target) {
        target_ = projectroot;
        sourceTarget_ = target;
        PluginInfo pluginfo = info.getPluginInfo();
        String targetpkg = pluginfo.getTargetPackage();
        GenerationContext.getInstance().setProperty(GenerationContext.LICENSE_KEY, "");
        GenerationContext.getInstance().setProperty(PluginInfo.TARGET_MODEL_PROVIDER, info.getName() + "Provider");
        GenerationContext.getInstance().setProperty(PluginInfo.TARGET_PLUGIN_PACKAGE, pluginfo.getTargetPackage());
        GenerationContext.getInstance().setProperty(PluginInfo.TARGET_EXTENSIONS, pluginfo.getTargetDSMLFileExtensions());
        GenerationContext.getInstance().setProperty(PluginInfo.TARGET_CATEGORY, pluginfo.getTargetDSMLCategory());
        GenerationContext.getInstance().setProperty(GenerationContext.MODEL_NAME_KEY, info.getName());
        String visitorname = (String) GenerationContext.getInstance().getProperty(GenerationContext.VISITOR_TYPE_KEY);
        String provider = providerGen_.generate(info);
        saveToPackage(targetpkg, info.getName() + "Provider.java", provider);
        String gprovider = graphicsProviderGen_.generate(info);
        saveToPackage(targetpkg, "GraphicsProviderImpl.java", gprovider);
        String lprovider = labelProviderGen_.generate(info);
        saveToPackage(targetpkg, "LabelProviderImpl.java", lprovider);
        List atoms = info.getAtoms();
        for (int i = 0; i < atoms.size(); i++) {
            AtomInfo atom = (AtomInfo) atoms.get(i);
            String aclass = atomGen_.generate(atom);
            saveToPackage(targetpkg, atom.getName() + ".java", aclass);
            iconGen_.generateIcon(packageToDir(targetpkg + ".icons"), atom);
        }
        List models = info.getModels();
        for (int i = 0; i < models.size(); i++) {
            ModelInfo model = (ModelInfo) models.get(i);
            String aclass = modelGen_.generate(model);
            saveToPackage(targetpkg, model.getName() + ".java", aclass);
            iconGen_.generateIcon(packageToDir(targetpkg + ".icons"), model);
        }
        List connections = info.getConnections();
        for (int i = 0; i < connections.size(); i++) {
            ConnectionInfo curr = (ConnectionInfo) connections.get(i);
            String contype = conTypeGen_.generate(curr);
            saveToPackage(targetpkg, curr.getName() + "ConnectionType.java", contype);
        }
        String conschecker = consGen_.generate(info);
        saveToPackage(targetpkg, info.getName() + "ConstraintsChecker.java", conschecker);
        String pluginxml = xmlTemplate_.generate(info);
        save("plugin.xml", pluginxml);
        String plugin = pluginTemplate_.generate(info);
        saveToPackage(targetpkg, info.getName() + "Plugin.java", plugin);
        String palette = paletteTemplate_.generate(info);
        saveToPackage(targetpkg, info.getName() + "PaletteProvider.java", palette);
        String visitor = visitorTemplate_.generate(info);
        saveToPackage(targetpkg, info.getName() + "Visitor.java", visitor);
        if (visitorname != null) {
            String vimpl = visitorImplTemplate_.generate(info);
            saveToPackage(targetpkg, visitorname + ".java", vimpl);
            String vacts = visitorActionSet_.generate(info);
            saveToPackage(targetpkg, "VisitorActionSet.java", vacts);
        }
        String editor = editorTemplate_.generate(info);
        saveToPackage(targetpkg, "DSMLEditor.java", editor);
        String wizard = wizardTemplate_.generate(info);
        saveToPackage(targetpkg, "GemsCreationWizard.java", wizard);
        String wizardpage = wizardPageTemplate_.generate(info);
        saveToPackage(targetpkg, "GemsWizardPage.java", wizardpage);
        String validators = validatorsGen_.generate(info);
        saveToPackage(targetpkg, "AttributeValidators.java", validators);
        String bprops = buildPropsTemplate_.generate(info);
        save("build.properties", bprops);
        String cpprops = cpathGen_.generate(info);
        save(".classpath", cpprops);
        String readme = readmeTemplate_.generate(info);
        save("README.txt", readme);
        try {
            InputStream str = GemsPlugin.class.getResourceAsStream("icons/gems.gif");
            copy(str, new File(target_.getAbsolutePath() + File.separator + info.getName() + ".gif"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File packageToDir(String pkg) {
        StringTokenizer tk = new StringTokenizer(pkg, ".", false);
        File file = new File(sourceTarget_.getAbsolutePath() + File.separator);
        while (tk.hasMoreTokens()) {
            file = new File(file.getAbsolutePath() + File.separator + tk.nextToken());
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        file = new File(file.getAbsolutePath());
        return file;
    }

    public void saveToPackage(String pkg, String name, String src) {
        StringTokenizer tk = new StringTokenizer(pkg, ".", false);
        File file = new File(sourceTarget_.getAbsolutePath());
        while (tk.hasMoreTokens()) {
            file = new File(file.getAbsolutePath() + File.separator + tk.nextToken());
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        file = new File(file.getAbsolutePath() + File.separator + name);
        save(file, src);
    }

    public void save(File file, String src) {
        try {
            FileWriter fout = new FileWriter(file);
            fout.write(src);
            fout.flush();
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(String name, String src) {
        try {
            FileWriter fout = new FileWriter(new File(target_.getCanonicalPath() + File.separator + name));
            fout.write(src);
            fout.flush();
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copy(InputStream str, File dest) throws IOException {
        byte[] buff = new byte[8192];
        FileOutputStream fout = new FileOutputStream(dest);
        int read = 0;
        while ((read = str.read(buff)) != -1) {
            fout.write(buff, 0, read);
        }
        fout.flush();
        fout.close();
    }
}
