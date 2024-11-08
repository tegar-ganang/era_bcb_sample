package ch.bbv.mda.cartridges.dotNet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import ch.bbv.application.VersionId;
import ch.bbv.mda.MetaClass;
import ch.bbv.mda.MetaIndexedProperty;
import ch.bbv.mda.MetaModel;
import ch.bbv.mda.MetaPackage;
import ch.bbv.mda.MetaProperty;
import ch.bbv.mda.generators.CartridgeImplNet;
import ch.bbv.mda.generators.CartridgeInfo.Platform;
import ch.bbv.mda.operators.FileOperators;
import com.jgoodies.validation.ValidationResult;

/**
 * Cartridge which generates the CSharp source code for the pmMDA.NET framework.<br/>
 * Currently the following artifacts are supported:
 * <ul>
 * <li> The presenter which deals the entire GUI-mechanism.</li>
 * <li> The view(UI) which shows the wanted Information.</li>
 * <li> The view-interface which is used for the passive view pattern</li>
 * </ul>
 * 
 * @author Hubert Willimann
 */
public class WPFCartridge extends CartridgeImplNet implements NetCartridgeConstants {

    /**
	 * Template folder
	 */
    private static final String ABBRV = "DotNetWPF";

    /**
	 * Cartridge name
	 */
    private static final String CARTRIDGE_NAME = ".Net WPF";

    /**
	 * Mock directory
	 */
    private static final String MOCK_DIRECTORY = "/Mock";

    /**
	 * Theme directory
	 */
    private static final String THEME_DIRECTORY = "/Themes";

    /**
	 * Template file name
	 */
    private static String TEMPLATE_PRESENTER = "wpf-presenter";

    private static String TEMPLATE_IVIEW = "wpf-iview";

    private static String TEMPLATE_VIEW_XAML = "wpf-view-xaml";

    private static String TEMPLATE_VIEW_CLASS = "wpf-view-class";

    private static String TEMPLATE_MOCK_CLASS = "wpf-mock-class";

    /**
	 * Meta model which is currently processed.
	 */
    private MetaModel model;

    /**
	 * Root directory of the generated source files.
	 */
    private File rootDirectory;

    /**
	 * Root directory of the generated mock source files.
	 */
    private File mockRootDirectory;

    /**
	 * Meta package which is currently processed.
	 */
    private MetaPackage metaPackage;

    /**
	 * Velocity templates
	 */
    private Template presenterTemplate;

    private Template ivewTemplate;

    private Template viewXamlTemplate;

    private Template viewClassTemplate;

    private Template mockClassTemplate;

    /**
	 * Instances a new instance of the WPFCartridge class.
	 */
    public WPFCartridge() {
        super(CARTRIDGE_NAME, ABBRV, new VersionId(0, 3, 0));
    }

    /**
	 * Instances a new instance of the WPFCartridge class.
	 * 
	 * @param name
	 * @param abbreviation
	 * @param version
	 */
    public WPFCartridge(String name, String abbreviation, VersionId version) {
        super(name, abbreviation, version);
    }

    @Override
    public void initialize(Properties properties) {
        super.initialize(properties);
        presenterTemplate = retrieveTemplate(TEMPLATE_PRESENTER, null);
        ivewTemplate = retrieveTemplate(TEMPLATE_IVIEW, null);
        viewXamlTemplate = retrieveTemplate(TEMPLATE_VIEW_XAML, null);
        viewClassTemplate = retrieveTemplate(TEMPLATE_VIEW_CLASS, null);
        mockClassTemplate = retrieveTemplate(TEMPLATE_MOCK_CLASS, null);
    }

    @Override
    public void preProcessModel(MetaModel model, ValidationResult messages) {
        super.preProcessModel(model, messages);
        this.model = model;
    }

    @Override
    public void preProcessClass(MetaClass clazz, ValidationResult messages) {
        super.preProcessClass(clazz, messages);
    }

    @Override
    public void preProcessPackage(MetaPackage metaPackage, ValidationResult messages) {
        super.preProcessPackage(metaPackage, messages);
    }

    @Override
    public void processModel(MetaModel model, ValidationResult messages) {
        rootDirectory = new File(getContext().getProperty(NetCartridgeConstants.WPF_ROOT_FOLDER_KEY));
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs();
        }
        mockRootDirectory = new File(getContext().getProperty(NetCartridgeConstants.WPF_ROOT_FOLDER_KEY) + MOCK_DIRECTORY);
        if (!mockRootDirectory.exists()) {
            mockRootDirectory.mkdirs();
        }
        File skinDirectory = new File(rootDirectory.getPath() + THEME_DIRECTORY);
        if (!skinDirectory.exists()) {
            skinDirectory.mkdirs();
        }
        File skinFile = new File(skinDirectory.getPath() + "/skin-default.xaml");
        if (!skinFile.exists()) {
            try {
                StringBuilder buffer = new StringBuilder();
                buffer.append(getConfiguration().getProperty(RESSOURCE_FOLDER));
                if (getPlatform() != null) {
                    buffer.append(Platform.getPath(getPlatform()) + "/");
                }
                buffer.append(getPrefix()).append("/").append("skin-default.xaml");
                File skinFileToCoyp = new File(buffer.toString());
                FileUtils.copyFileToDirectory(skinFileToCoyp, skinDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.processModel(model, messages);
    }

    @Override
    public void processPackage(MetaPackage metaPackage, ValidationResult messages) {
        this.metaPackage = metaPackage;
        super.processPackage(metaPackage, messages);
        for (MetaClass clazz : metaPackage.getClasses()) {
            processClass(clazz, messages);
        }
    }

    @Override
    public void processClass(MetaClass clazz, ValidationResult messages) {
        if (!clazz.booleanValueOfTag(ch.bbv.mda.cartridges.BoConstants.GENERATE) || clazz.booleanValueOfTag(DOTNET_IGNORE) || !clazz.booleanValueOfTag(WPF_GENERATE)) {
            return;
        }
        super.processClass(clazz, messages);
        for (MetaProperty property : clazz.getProperties()) {
            if (property.getTypeName().contains(clazz.getContext().getName())) {
                if (property.booleanValueOfTag(WPF_GENERATE) != property.getClassifier().booleanValueOfTag(WPF_GENERATE) || property.booleanValueOfTag(DOTNET_IGNORE) != property.getClassifier().booleanValueOfTag(DOTNET_IGNORE) || clazz.booleanValueOfTag(ch.bbv.mda.cartridges.BoConstants.GENERATE) != property.getClassifier().booleanValueOfTag(ch.bbv.mda.cartridges.BoConstants.GENERATE)) {
                    messages.addError("The property " + property.getName() + " in class " + clazz.getName() + " must have the same setting like the referenced class " + property.getClassifier().getName() + " of the property.");
                }
            }
        }
        for (MetaIndexedProperty property : clazz.getIndexedProperties()) {
            if (property.booleanValueOfTag(WPF_GENERATE) != property.getClassifier().booleanValueOfTag(WPF_GENERATE) || property.booleanValueOfTag(DOTNET_IGNORE) != property.getClassifier().booleanValueOfTag(DOTNET_IGNORE) || clazz.booleanValueOfTag(ch.bbv.mda.cartridges.BoConstants.GENERATE) != property.getClassifier().booleanValueOfTag(ch.bbv.mda.cartridges.BoConstants.GENERATE)) {
                messages.addError("The indexedproperty " + property.getName() + " in class " + clazz.getName() + " must have the same setting like the referenced class " + property.getClassifier().getName() + " of the indexedproperty.");
            }
        }
        try {
            processTemplate(rootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, TEMPLATE_PRESENTER), presenterTemplate, createWPFContext(clazz, TEMPLATE_PRESENTER), messages);
            processTemplate(rootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, TEMPLATE_IVIEW), ivewTemplate, createWPFContext(clazz, TEMPLATE_IVIEW), messages);
            processTemplate(rootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, TEMPLATE_VIEW_XAML), viewXamlTemplate, createWPFContext(clazz, TEMPLATE_VIEW_XAML), messages);
            processTemplate(rootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, TEMPLATE_VIEW_CLASS), viewClassTemplate, createWPFContext(clazz, TEMPLATE_VIEW_CLASS), messages);
            processTemplate(mockRootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, TEMPLATE_MOCK_CLASS), mockClassTemplate, createWPFContext(clazz, TEMPLATE_MOCK_CLASS), messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Gets the file name of the class which has to be generated.
	 * 
	 * @param clazz
	 *            The class to be generated.
	 * @param template
	 *            The template for the generation.
	 * @return Returns the file name.
	 */
    private String getFileName(MetaClass clazz, String template) {
        if (template.equals(TEMPLATE_PRESENTER)) {
            return clazz.getName() + "Presenter.cs";
        } else if (template.equals(TEMPLATE_IVIEW)) {
            return "I" + clazz.getName() + "WPF.cs";
        } else if (template.equals(TEMPLATE_VIEW_XAML)) {
            return clazz.getName() + "WPF.xaml";
        } else if (template.equals(TEMPLATE_VIEW_CLASS)) {
            return clazz.getName() + "WPF.xaml.cs";
        } else if (template.equals(TEMPLATE_MOCK_CLASS)) {
            return "Mock" + clazz.getName() + "WPF.cs";
        } else {
            return "";
        }
    }

    /**
	 * Creates the template context
	 * 
	 * @param clazz
	 *            The class to be generated.
	 * @param template
	 *            The template for the generation.
	 * @return Returns the completely context
	 * @throws IOException
	 *             If error was encountered when reading the file.
	 */
    private VelocityContext createWPFContext(MetaClass clazz, String template) throws IOException {
        VelocityContext context = new VelocityContext();
        Map<String, List<String>> blocks = extractUserCode(clazz, template);
        context.put("blocks", blocks);
        context.put("netHelper", new NetCartridgeHelper(this, this.model));
        context.put("class", clazz);
        return context;
    }

    /**
	 * Gatherings the user defined blocks.
	 * 
	 * @param clazz
	 *            The class to be generated.
	 * @param template
	 *            The template for the generation.
	 * @return The entire block.
	 * @throws IOException
	 *             If error was encountered when reading the file.
	 */
    private Map<String, List<String>> extractUserCode(MetaClass clazz, String template) throws IOException {
        Map<String, List<String>> blocks = new HashMap<String, List<String>>();
        File source = FileOperators.createFile(rootDirectory, metaPackage.getAttribute(WPF_ROOT_FOLDER_KEY), getFileName(clazz, template));
        blocks.put(USING_BLOCK, FileOperators.extractSourceCode(source, USING_BLOCK_BEGIN, USING_BLOCK_END));
        blocks.put(DECLARATIONS_BLOCK, FileOperators.extractSourceCode(source, DECLARATIONS_BLOCK_BEGIN, DECLARATIONS_BLOCK_END));
        blocks.put(INITIALIZATIONS_BLOCK, FileOperators.extractSourceCode(source, INITIALIZATIONS_BLOCK_BEGIN, INITIALIZATIONS_BLOCK_END));
        blocks.put(CONSTRUCTORS_BLOCK, FileOperators.extractSourceCode(source, CONSTRUCTORS_BLOCK_BEGIN, CONSTRUCTORS_BLOCK_END));
        blocks.put(DEFINITIONS_BLOCK, FileOperators.extractSourceCode(source, DEFINITIONS_BLOCK_BEGIN, DEFINITIONS_BLOCK_END));
        return blocks;
    }
}
