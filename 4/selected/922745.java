package org.micthemodel.asciiIO;

import org.micthemodel.XMLIO.XMLWriter;
import org.micthemodel.elements.Material;
import org.micthemodel.elements.Product;
import org.micthemodel.elements.Reactant;
import org.micthemodel.elements.Reaction;
import org.micthemodel.elements.Reactor;
import org.micthemodel.factory.Parameters;
import org.micthemodel.elements.ModelGrain;
import org.micthemodel.helpers.SphericalLayer;
import java.io.File;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import org.w3c.dom.Element;
import org.micthemodel.plugins.MicPlugin;
import org.micthemodel.plugins.densityVariation.DensityVariation;
import org.micthemodel.plugins.grainProportionInitialiser.GrainProportionInitialiser;
import org.micthemodel.plugins.materialDistributionProfile.MaterialDistributionProfile;
import org.micthemodel.plugins.nucleiGenerator.NucleiGenerator;
import org.micthemodel.plugins.packer.Packer;
import org.micthemodel.plugins.particleDistributionProfile.ParticleDistributionProfile;
import org.micthemodel.plugins.postProcessor.PostProcessor;
import org.micthemodel.plugins.reactionTrigger.ReactionTrigger;
import org.micthemodel.plugins.recalculator.Recalculator;

/**
 *
 * @author bishnoi
 */
public class XMLInputWriter {

    private XMLWriter writer;

    private Element setupNode;

    Reactor reactor;

    /** 
     * Creates a new instance of XMLInputWriter. The XML file produced is stored
     * in the current project folder.
     */
    public XMLInputWriter(Reactor reactor) {
        this.reactor = reactor;
        File folder = new File(reactor.getParameters().getFileFolder());
        if (!folder.canRead()) {
            folder.mkdir();
        }
        this.writer = new XMLWriter(reactor.getParameters().getFileFolder() + "setup" + this.reactor.getParameters().getProjectName() + ".xml");
        this.initialise();
    }

    /**
     * Creates a new instance of XMLInputWriter that saves the XML file at the 
     * path given by the fileName.
     * 
     * @param fileName the path of the XML file.
     */
    public XMLInputWriter(String fileName, Reactor reactor) {
        this.writer = new XMLWriter(fileName);
        this.reactor = reactor;
        this.initialise();
    }

    private void initialise() {
        Element micinputNode = this.writer.createNewRootElement("micinput");
        this.setupNode = this.writer.createNewChildElement(micinputNode, "setup");
        this.writer.setIndent(4);
        this.writer.setDocType("micthemodelSetup");
        this.writer.setXmlVersion("1.0");
    }

    /**
     * Stores the setup file at the given path.
     */
    public boolean writeXML() {
        this.writeMaterials();
        this.writeModelGrains();
        this.writeReactor();
        this.writeReactions();
        this.writePlugins();
        this.writeCommands();
        return this.writer.writeXML();
    }

    private void writeMaterials() {
        for (Material material : this.reactor.getMaterials()) {
            Element materialNode = this.writer.createNewChildElement(this.setupNode, "material");
            materialNode.setAttribute("name", material.getName());
            if (material.getMaterialClass() != null) {
                materialNode.setAttribute("class", material.getMaterialClass());
            }
            materialNode.setAttribute("id", String.valueOf(material.getIndex()));
            this.writer.createNewChildElement(materialNode, "density", String.valueOf(material.getDensity()));
            this.writer.createNewChildElement(materialNode, "userDefinedParameter", String.valueOf(material.getUserDefinedParameter()));
            this.writer.createNewChildElement(materialNode, "inner", String.valueOf(material.isInner()));
            this.writer.createNewChildElement(materialNode, "universal", String.valueOf(material.isUniversal()));
            this.writer.createNewChildElement(materialNode, "discrete", String.valueOf(material.isDiscrete()));
            if (!material.isDiscrete()) {
                this.writer.createNewChildElement(materialNode, "hostName", material.getHostMaterial().getName());
            }
            this.writer.createNewChildElement(materialNode, "hasVariant", String.valueOf(material.hasVariant()));
            if (material.hasVariant()) {
                this.writer.createNewChildElement(materialNode, "variantName", material.getVariant().getName());
            }
            if (material.getInitialFraction() > 0) {
                this.writer.createNewChildElement(materialNode, "initialVolumeFraction", String.valueOf(material.getInitialFraction()));
            }
            this.writer.createNewChildElement(materialNode, "color", String.valueOf(this.reactor.getImages().getColor(material.getIndex()).getRGB()));
            if (material.getReplaceOriginalMaterials() != null) {
                for (Material replace : material.getReplaceOriginalMaterials()) {
                    this.writer.createNewChildElement(materialNode, "replace", replace.getName());
                }
            }
        }
    }

    private void writeModelGrains() {
        for (ModelGrain model : reactor.getModels()) {
            Element modelNode = this.writer.createNewChildElement(this.setupNode, "modelGrain");
            modelNode.setAttribute("name", model.getName());
            for (SphericalLayer layer : model.layers) {
                this.writer.createNewChildElement(modelNode, "layer", layer.getMaterial().getName());
            }
            if (model.getFilePSD() != null) {
                if (reactor.getParameters().useRelativePaths()) {
                    String path = Parameters.getRelativePath(new File(model.getFilePSD()), reactor.getParameters().getRootFolder());
                    this.writer.createNewChildElement(modelNode, "filePSD", path);
                } else {
                    this.writer.createNewChildElement(modelNode, "filePSD", model.getFilePSD());
                }
            }
        }
    }

    private void writeReactor() {
        Element reactorNode = this.writer.createNewChildElement(this.setupNode, "reactor");
        reactorNode.setAttribute("name", reactor.getName());
        this.writer.createNewChildElement(reactorNode, "size", String.valueOf(reactor.getSide()));
        this.writer.createNewChildElement(reactorNode, "pixelSize", String.valueOf(reactor.getParameters().getPixelSize()));
        if (reactor.getParameters().useRelativePaths()) {
            String path = Parameters.getRelativePath(new File(reactor.getParameters().getFileFolder()), reactor.getParameters().getRootFolder());
            this.writer.createNewChildElement(reactorNode, "fileFolder", path);
            if (reactor.getParameters().getCopyFromFolder() != null) {
                path = Parameters.getRelativePath(new File(reactor.getParameters().getCopyFromFolder()), reactor.getParameters().getRootFolder());
                this.writer.createNewChildElement(reactorNode, "copyFromFolder", path);
            }
            if (reactor.getTimeStepFile() != null) {
                path = Parameters.getRelativePath(new File(reactor.getTimeStepFile()), reactor.getParameters().getRootFolder());
                this.writer.createNewChildElement(reactorNode, "timeStepFile", path);
            }
        } else {
            this.writer.createNewChildElement(reactorNode, "fileFolder", reactor.getParameters().getFileFolder());
            if (reactor.getParameters().getCopyFromFolder() != null) {
                this.writer.createNewChildElement(reactorNode, "copyFromFolder", reactor.getParameters().getCopyFromFolder());
            }
            if (reactor.getTimeStepFile() != null) {
                this.writer.createNewChildElement(reactorNode, "timeStepFile", reactor.getTimeStepFile());
            }
        }
        double step = 0;
        double lastStep = 0;
        double[] timePoints = reactor.getTimePoints();
        if (timePoints != null && timePoints.length > 1) {
            for (int i = 0; i < timePoints.length; i++) {
                double time = timePoints[i];
                if (i == 0) {
                    this.writer.createNewChildElement(reactorNode, "minTime", String.valueOf(time));
                }
                if (i == timePoints.length - 1) {
                    this.writer.createNewChildElement(reactorNode, "maxTime", String.valueOf(time));
                    this.writer.createNewChildElement(reactorNode, "step", String.valueOf(step));
                    continue;
                }
                if (i == 0) {
                    continue;
                }
                step = time - timePoints[i - 1];
                if (i == 1) {
                    lastStep = step;
                }
                if (lastStep != step) {
                    this.writer.createNewChildElement(reactorNode, "maxTime", String.valueOf(timePoints[i - 1]));
                    this.writer.createNewChildElement(reactorNode, "step", String.valueOf(lastStep));
                    this.writer.createNewChildElement(reactorNode, "minTime", String.valueOf(time));
                }
                lastStep = step;
            }
        }
        this.writer.createNewChildElement(reactorNode, "backgroundColor", String.valueOf(reactor.getImages().getBgColor().getRGB()));
        this.writer.createNewChildElement(reactorNode, "maxOccupancyPixels", String.valueOf(reactor.getParameters().getMaxNumberOfOccupancyPixels()));
        this.writer.createNewChildElement(reactorNode, "minOccupancyPixels", String.valueOf(reactor.getParameters().getMinNumberOfOccupancyPixels()));
        Element flocculateElement = this.writer.createNewChildElement(reactorNode, "flocculate");
        flocculateElement.setAttribute("value", String.valueOf(reactor.getParameters().flocculate()));
        this.writer.createNewChildElement(flocculateElement, "duringDistribution", String.valueOf(reactor.getParameters().flocculateDuringDistribution()));
        this.writer.createNewChildElement(flocculateElement, "range", String.valueOf(reactor.getParameters().getFlocculationRange()));
        this.writer.createNewChildElement(flocculateElement, "factor", String.valueOf(reactor.getParameters().getFlocculationFactor()));
        this.writer.createNewChildElement(flocculateElement, "cycles", String.valueOf(reactor.getParameters().getFlocculationCycles()));
        this.writer.createNewChildElement(flocculateElement, "maxTrials", String.valueOf(reactor.getParameters().getMaxTrials()));
        this.writer.createNewChildElement(reactorNode, "sphereSamplingPoints", String.valueOf(reactor.getParameters().getPointsOnSphere().number()));
        this.writer.createNewChildElement(reactorNode, "checkIndividualArea", String.valueOf(reactor.checkIndividualArea()));
        this.writer.createNewChildElement(reactorNode, "checkNoArea", String.valueOf(reactor.checkNoArea()));
        this.writer.createNewChildElement(reactorNode, "findCoveringSphere", String.valueOf(reactor.findCoveringSphere()));
        this.writer.createNewChildElement(reactorNode, "saveHydout", String.valueOf(reactor.getParameters().saveHydout()));
        this.writer.createNewChildElement(reactorNode, "savePorein", String.valueOf(reactor.getParameters().savePorein()));
        this.writer.createNewChildElement(reactorNode, "saveGrains", String.valueOf(reactor.getParameters().saveGrains()));
        this.writer.createNewChildElement(reactorNode, "saveDeepImages", String.valueOf(reactor.getParameters().saveDeepImages())).setAttribute("depth", String.valueOf(reactor.getParameters().getImageDepth()));
        this.writer.createNewChildElement(reactorNode, "useRelativePaths", String.valueOf(reactor.getParameters().useRelativePaths()));
    }

    private void writeReactions() {
        for (Reaction reaction : reactor.getReactions()) {
            Element reactionElement = this.writer.createNewChildElement(this.setupNode, "reaction");
            reactionElement.setAttribute("name", reaction.getName());
            this.writer.createNewChildElement(reactionElement, "index", String.valueOf(reaction.getIndex()));
            for (Reactant reactant : reaction.getReactants()) {
                Element reactantElement = this.writer.createNewChildElement(reactionElement, "reactant");
                this.writer.createNewChildElement(reactantElement, "materialName", reactant.getMaterial().getName());
                this.writer.createNewChildElement(reactantElement, "ratio", String.valueOf(reactant.getRatio()));
            }
            for (Product product : reaction.getProducts()) {
                Element productElement = this.writer.createNewChildElement(reactionElement, "product");
                this.writer.createNewChildElement(productElement, "materialName", product.getMaterial().getName());
                this.writer.createNewChildElement(productElement, "ratio", String.valueOf(product.getRatio()));
            }
            this.writer.createNewChildElement(reactionElement, "discrete", String.valueOf(reaction.isDiscrete()));
            for (Reaction successor : reaction.getSuccesors()) {
                this.writer.createNewChildElement(reactionElement, "successor").setAttribute("name", successor.getName());
            }
        }
    }

    private void writeCommands() {
        if (reactor.getParameters().start()) {
            this.writer.createNewChildElement(this.setupNode, "command", "start");
        }
        if (reactor.getParameters().restart()) {
            this.writer.createNewChildElement(this.setupNode, "command", "restart").setAttribute("fromStep", String.valueOf(reactor.getParameters().getRestartFromStep()));
        }
        if (reactor.getParameters().saveElements()) {
            this.writer.createNewChildElement(this.setupNode, "command", "saveElements");
        } else {
            this.writer.createNewChildElement(this.setupNode, "command", "noElements");
        }
        if (reactor.getParameters().doBurning()) {
            String suffix = new Character((char) (88 + reactor.getParameters().getVectorBurningAxis())).toString();
            this.writer.createNewChildElement(this.setupNode, "command", "doBurning" + suffix);
        } else {
            this.writer.createNewChildElement(this.setupNode, "command", "noBurning");
        }
        if (reactor.getParameters().markPores()) {
            this.writer.createNewChildElement(this.setupNode, "command", "markPores");
        } else {
            this.writer.createNewChildElement(this.setupNode, "command", "markNoPores");
        }
        if (reactor.getParameters().calculateSurface()) {
            this.writer.createNewChildElement(this.setupNode, "command", "doContact");
        } else {
            this.writer.createNewChildElement(this.setupNode, "command", "noContact");
        }
        if (reactor.getParameters().savePixels()) {
            this.writer.createNewChildElement(this.setupNode, "command", "savePixels");
        } else {
            this.writer.createNewChildElement(this.setupNode, "command", "noPixels");
        }
        if (reactor.getParameters().redoPixels()) {
            this.writer.createNewChildElement(this.setupNode, "command", "redoPixels");
        }
        if (reactor.getParameters().getReadStep() > 0) {
            this.writer.createNewChildElement(this.setupNode, "command", "readStep").setAttribute("step", String.valueOf(reactor.getParameters().getReadStep()));
        }
        if (reactor.getParameters().doVectorBurning()) {
            this.writer.createNewChildElement(this.setupNode, "command", "doVectorBurning");
        }
    }

    private void writePlugins() {
        MicPlugin packerPlugin = reactor.getPacker();
        if (packerPlugin != null && packerPlugin.getClass() != Packer.getDefaultImplementation().getClass()) {
            this.writePlugin(packerPlugin);
        }
        for (PostProcessor processor : reactor.getPostProcessors()) {
            this.writePlugin(processor);
        }
        for (Reaction reaction : reactor.getReactions()) {
            MicPlugin trigger = reaction.getTrigger();
            if (trigger.getClass() != ReactionTrigger.getDefaultImplementation().getClass()) {
                this.writePlugin(trigger);
            }
            for (MicPlugin plugin : reaction.getKinetics()) {
                this.writePlugin(plugin);
            }
            MicPlugin recalculator = reaction.getRecalculator();
            if (recalculator.getClass() != Recalculator.getDefaultImplementation().getClass()) {
                this.writePlugin(recalculator);
            }
            this.writePlugin(reaction.getParticleChooser().getIterator());
        }
        for (Material material : reactor.getMaterials()) {
            MicPlugin densityVariation = material.getDensityVariation();
            if (densityVariation.getClass() != DensityVariation.getDefaultImplementation().getClass()) {
                this.writePlugin(densityVariation);
            }
            MicPlugin distributionProfile = material.getDistributionProfile();
            if (distributionProfile.getClass() != MaterialDistributionProfile.getDefaultImplementation().getClass()) {
                this.writePlugin(distributionProfile);
            }
        }
        for (ModelGrain model : reactor.getModels()) {
            ParticleDistributionProfile profile = model.getDistributionProfile();
            if (profile.getClass() != ParticleDistributionProfile.getDefaultImplementation().getClass()) {
                this.writePlugin(profile);
            }
            GrainProportionInitialiser initialiser = model.getGrainProportionInitialiser();
            if (initialiser.getClass() != GrainProportionInitialiser.getDefaultImplementation().getClass()) {
                this.writePlugin(initialiser);
            }
            if (model.getNuclei() != null) {
                for (NucleiGenerator generator : model.getNuclei()) {
                    this.writePlugin(generator);
                }
            }
        }
    }

    private void writePlugin(MicPlugin plugin) {
        if (plugin.constructorParameterValues() == null) {
            return;
        }
        if (plugin.getClass().isMemberClass()) {
            return;
        }
        Element pluginElement = this.writer.createNewChildElement(this.setupNode, "plugin");
        pluginElement.setAttribute("type", plugin.pluginType());
        Class pluginClass = plugin.getClass();
        ClassLoader loader = pluginClass.getClassLoader();
        String name = plugin.getClass().getName();
        URL firstURL = pluginClass.getProtectionDomain().getCodeSource().getLocation();
        URL[] firstURLS = { firstURL };
        URLClassLoader firstUrlLoader = new URLClassLoader(firstURLS);
        try {
            firstUrlLoader.loadClass(name);
            if (pluginClass.getPackage().getName().startsWith(MicPlugin.class.getPackage().getName())) {
                this.writer.createNewChildElement(pluginElement, "classPath", "precompiled");
            } else {
                if (this.reactor.getParameters().useRelativePaths()) {
                    String path = Parameters.getRelativePath(new File(firstURL.getPath()), this.reactor.getParameters().getRootFolder());
                    this.writer.createNewChildElement(pluginElement, "classPath", path.replaceAll("%20", " "));
                } else {
                    this.writer.createNewChildElement(pluginElement, "classPath", firstURL.getPath().replaceAll("%20", " "));
                }
            }
        } catch (ClassNotFoundException ex) {
            if (loader instanceof URLClassLoader) {
                URLClassLoader urlLoader = (URLClassLoader) loader;
                URL[] urls = urlLoader.getURLs();
                for (URL url : urls) {
                    URL[] url2 = { url };
                    URLClassLoader newURLLoader = new URLClassLoader(url2);
                    try {
                        newURLLoader.loadClass(name);
                        break;
                    } catch (ClassNotFoundException ex2) {
                        this.writer.createNewChildElement(pluginElement, "classPath", "precompiled");
                    }
                }
            }
        }
        this.writer.createNewChildElement(pluginElement, "className", name);
        Object[] parameterValues = plugin.constructorParameterValues();
        String[] parameterNames = plugin.constructorParameterNames();
        for (int i = 0; i < parameterValues.length; i++) {
            this.addParameter(pluginElement, parameterValues[i], parameterNames[i], "parameter");
        }
    }

    private void addParameter(Element parentElement, Object parameter, String name, String tagName) {
        if (parameter.getClass().isArray()) {
            this.addArrayParameter(parentElement, parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Float.class) {
            this.addParameter(parentElement, (Float) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Integer.class) {
            this.addParameter(parentElement, (Integer) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Boolean.class) {
            this.addParameter(parentElement, (Boolean) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Byte.class) {
            this.addParameter(parentElement, (Byte) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Short.class) {
            this.addParameter(parentElement, (Short) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Long.class) {
            this.addParameter(parentElement, (Long) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Double.class) {
            this.addParameter(parentElement, (Double) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Reaction.class) {
            this.addParameter(parentElement, (Reaction) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Reactant.class) {
            this.addParameter(parentElement, (Reactant) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Product.class) {
            this.addParameter(parentElement, (Product) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Material.class) {
            this.addParameter(parentElement, (Material) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == ModelGrain.class) {
            this.addParameter(parentElement, (ModelGrain) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == Reactor.class) {
            this.addParameter(parentElement, (Reactor) parameter, name, tagName);
            return;
        }
        if (parameter.getClass() == File.class) {
            this.addParameter(parentElement, (File) parameter, name, tagName);
            return;
        }
        Parameters.getOut().println("Could not find category for " + name + " " + parameter.toString());
    }

    private void addArrayParameter(Element parentElement, Object value, String name, String tagName) {
        Element arrayElement = this.writer.createNewChildElement(parentElement, tagName);
        arrayElement.setAttribute("type", "array");
        if (name != null) {
            arrayElement.setAttribute("name", name);
        }
        for (int i = 0; i < Array.getLength(value); i++) {
            Object element = Array.get(value, i);
            this.addParameter(arrayElement, element, "null", "element");
        }
    }

    private void addParameter(Element element, Float value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "float");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Integer value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "int");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Boolean value, String name, String tagName) {
        Element boolElement = this.writer.createNewChildElement(element, tagName);
        boolElement.setAttribute("type", "boolean");
        this.writer.createNewChildElement(boolElement, "value", value.toString());
        if (name != null) {
            boolElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Byte value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "byte");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Short value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "short");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Long value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "long");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Double value, String name, String tagName) {
        Element numberElement = this.writer.createNewChildElement(element, tagName);
        numberElement.setAttribute("type", "number");
        this.writer.createNewChildElement(numberElement, "class", "double");
        this.writer.createNewChildElement(numberElement, "value", value.toString());
        if (name != null) {
            numberElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, File value, String name, String tagName) {
        Element fileElement = this.writer.createNewChildElement(element, tagName);
        fileElement.setAttribute("type", "file");
        if (this.reactor.getParameters().useRelativePaths()) {
            this.writer.createNewChildElement(fileElement, "name", Parameters.getRelativePath(value, this.reactor.getParameters().getRootFolder()));
        } else {
            this.writer.createNewChildElement(fileElement, "name", value.getAbsolutePath());
        }
        if (name != null) {
            fileElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Reactor value, String name, String tagName) {
        Element reactorElement = this.writer.createNewChildElement(element, tagName);
        reactorElement.setAttribute("type", "reactor");
        this.writer.createNewChildElement(reactorElement, "name", value.getName());
        if (name != null) {
            reactorElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Reaction value, String name, String tagName) {
        Element reactionElement = this.writer.createNewChildElement(element, tagName);
        reactionElement.setAttribute("type", "reaction");
        this.writer.createNewChildElement(reactionElement, "name", value.getName());
        if (name != null) {
            reactionElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Reactant value, String name, String tagName) {
        Element reactantElement = this.writer.createNewChildElement(element, tagName);
        reactantElement.setAttribute("type", "reactant");
        this.writer.createNewChildElement(reactantElement, "hostReaction", value.getReaction().getName());
        this.writer.createNewChildElement(reactantElement, "name", value.getMaterial().getName());
        if (name != null) {
            reactantElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Product value, String name, String tagName) {
        Element productElement = this.writer.createNewChildElement(element, tagName);
        productElement.setAttribute("type", "product");
        this.writer.createNewChildElement(productElement, "hostReaction", value.getReaction().getName());
        this.writer.createNewChildElement(productElement, "name", value.getMaterial().getName());
        if (name != null) {
            productElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, Material value, String name, String tagName) {
        Element materialElement = this.writer.createNewChildElement(element, tagName);
        materialElement.setAttribute("type", "material");
        this.writer.createNewChildElement(materialElement, "name", value.getName());
        if (name != null) {
            materialElement.setAttribute("name", name);
        }
    }

    private void addParameter(Element element, ModelGrain value, String name, String tagName) {
        Element modelGrainElement = this.writer.createNewChildElement(element, tagName);
        modelGrainElement.setAttribute("type", "modelGrain");
        this.writer.createNewChildElement(modelGrainElement, "name", value.getName());
        if (name != null) {
            modelGrainElement.setAttribute("name", name);
        }
    }
}
