package modbuspal.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import modbuspal.automation.Automation;
import modbuspal.automation.NullAutomation;
import modbuspal.binding.Binding;
import modbuspal.generator.Generator;
import modbuspal.instanciator.InstantiableManager;
import modbuspal.link.ModbusSerialLink;
import modbuspal.script.ScriptListener;
import modbuspal.script.ScriptRunner;
import modbuspal.slave.ModbusSlave;
import modbuspal.slave.ModbusPduProcessor;
import modbuspal.toolkit.FileTools;
import modbuspal.toolkit.XMLTools;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * contains all the data related to a modbuspal project
 * @author nnovic
 */
public final class ModbusPalProject extends ModbusPalProject2 implements ModbusPalXML {

    final IdGenerator idGenerator = new IdGenerator();

    File projectFile = null;

    private final ArrayList<ModbusPalListener> listeners = new ArrayList<ModbusPalListener>();

    private final ArrayList<Automation> automations = new ArrayList<Automation>();

    private final ArrayList<ScriptListener> scriptListeners = new ArrayList<ScriptListener>();

    private final ArrayList<ScriptRunner> scripts = new ArrayList<ScriptRunner>();

    private boolean learnModeEnabled = false;

    final InstantiableManager<Generator> generatorFactory = new InstantiableManager<Generator>();

    final InstantiableManager<Binding> bindingFactory = new InstantiableManager<Binding>();

    final InstantiableManager<ModbusPduProcessor> functionFactory = new InstantiableManager<ModbusPduProcessor>();

    String selectedLink = "none";

    String linkTcpipPort = "502";

    String linkSerialComId = "none";

    String linkSerialBaudrate = "9600";

    int linkSerialParity = ModbusSerialLink.PARITY_EVEN;

    int linkSerialStopBits = ModbusSerialLink.STOP_BITS_1;

    boolean linkSerialXonXoff = false;

    boolean linkSerialRtsCts = false;

    File linkReplayFile = null;

    /** Creates a new ModbusPalProject with the configuration contained 
     * in the specified project file.
     * @param source the project file to load
     * @return the ModbusPalProject created from the information contained in
     * the specified file.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    public static ModbusPalProject load(File source) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        Document doc = XMLTools.ParseXML(source);
        doc.getDocumentElement().normalize();
        ModbusPalProject project = new ModbusPalProject(doc, source);
        return project;
    }

    public ModbusPalProject() {
        bindingFactory.add(new modbuspal.binding.Binding_SINT16());
        bindingFactory.add(new modbuspal.binding.Binding_SINT32());
        bindingFactory.add(new modbuspal.binding.Binding_FLOAT32());
        generatorFactory.add(new modbuspal.generator.linear.LinearGenerator());
        generatorFactory.add(new modbuspal.generator.random.RandomGenerator());
        generatorFactory.add(new modbuspal.generator.sine.SineGenerator());
    }

    private ModbusPalProject(Document doc, File source) throws InstantiationException, IllegalAccessException {
        this();
        String name = doc.getDocumentElement().getNodeName();
        System.out.println("load " + name);
        projectFile = source;
        loadScripts(doc, projectFile);
        for (ScriptRunner runner : scripts) {
            if (runner.getType() == ScriptRunner.SCRIPT_TYPE_BEFORE_INIT) {
                runner.execute();
            }
        }
        loadParameters(doc);
        for (ScriptRunner runner : scripts) {
            if (runner.getType() == ScriptRunner.SCRIPT_TYPE_OLD_BINDINGS) {
                importOldBindings(runner);
            }
        }
        for (ScriptRunner runner : scripts) {
            if (runner.getType() == ScriptRunner.SCRIPT_TYPE_OLD_GENERATORS) {
                importOldGenerators(runner);
            }
        }
        loadAutomations(doc);
        loadSlaves(doc);
        loadBindings(doc, null);
        for (ScriptRunner runner : scripts) {
            if (runner.getType() == ScriptRunner.SCRIPT_TYPE_AFTER_INIT) {
                runner.execute();
            }
        }
    }

    /**
     * this method  will parse the content of doc in order to find and parse
     * elements from the doc that are related to the static part of the Automation
     * class.
     * @param doc
     */
    private void loadParameters(Document doc) {
        idGenerator.load(doc);
        loadLinks(doc);
    }

    private void loadLinks(Document doc) {
        NodeList linksNode = doc.getElementsByTagName("links");
        if (linksNode.getLength() == 1) {
            loadLinksParameters(linksNode.item(0));
        } else if (linksNode.getLength() > 1) {
        }
    }

    private void loadLinksParameters(Node linkRoot) {
        selectedLink = XMLTools.getAttribute("selected", linkRoot);
        Node tcpipNode = XMLTools.findChild(linkRoot, "tcpip");
        linkTcpipPort = XMLTools.getAttribute("port", tcpipNode);
        Node serialNode = XMLTools.findChild(linkRoot, "serial");
        loadSerialLinkParameters(serialNode);
        Node replayNode = XMLTools.findChild(linkRoot, "replay");
        if (replayNode != null) {
            loadReplayLinkParameters(replayNode);
        }
    }

    private void loadSerialLinkParameters(Node root) {
        linkSerialComId = XMLTools.getAttribute("com", root);
        linkSerialBaudrate = XMLTools.getAttribute("baudrate", root);
        String parity = XMLTools.getAttribute("parity", root);
        if (parity != null) {
            if (parity.compareTo("none") == 0) {
                linkSerialParity = ModbusSerialLink.PARITY_NONE;
            } else if (parity.compareTo("odd") == 0) {
                linkSerialParity = ModbusSerialLink.PARITY_ODD;
            } else {
                linkSerialParity = ModbusSerialLink.PARITY_EVEN;
            }
        }
        String stops = XMLTools.getAttribute("stops", root);
        if (stops != null) {
            if (stops.compareTo("1.5") == 0) {
                linkSerialStopBits = ModbusSerialLink.STOP_BITS_1_5;
            } else if (parity.compareTo("2") == 0) {
                linkSerialStopBits = ModbusSerialLink.STOP_BITS_2;
            } else {
                linkSerialStopBits = ModbusSerialLink.STOP_BITS_1;
            }
        }
        Node flowControlNode = XMLTools.findChild(root, "flowcontrol");
        if (flowControlNode != null) {
            loadFlowControlParameters(flowControlNode);
        }
    }

    private void loadFlowControlParameters(Node root) {
        String xonxoffValue = XMLTools.getAttribute("xonxoff", root);
        if (xonxoffValue != null) {
            linkSerialXonXoff = Boolean.parseBoolean(xonxoffValue);
        }
        String rtsctsValue = XMLTools.getAttribute("rtscts", root);
        if (rtsctsValue != null) {
            linkSerialRtsCts = Boolean.parseBoolean(rtsctsValue);
        }
    }

    private void loadReplayLinkParameters(Node node) {
        Node rel = XMLTools.findChild(node, "rel");
        if (rel != null) {
            String relativePath = rel.getTextContent();
            String absolutePath = FileTools.makeAbsolute(projectFile, relativePath);
            File file = new File(absolutePath);
            if (file.exists() == true) {
                linkReplayFile = file;
                return;
            }
        }
        Node abs = XMLTools.findChild(node, "abs");
        if (abs != null) {
            String path = abs.getTextContent();
            File file = new File(path);
            if (file.exists() == true) {
                linkReplayFile = file;
                return;
            }
        }
    }

    private void loadAutomations(Document doc) throws InstantiationException, IllegalAccessException {
        NodeList automationsList = doc.getElementsByTagName("automation");
        for (int i = 0; i < automationsList.getLength(); i++) {
            Node automationNode = automationsList.item(i);
            NamedNodeMap attributes = automationNode.getAttributes();
            String name = attributes.getNamedItem("name").getNodeValue();
            Automation automation = getAutomation(name);
            if (automation != null) {
                ErrorMessage dialog = new ErrorMessage(2);
                dialog.append("An automation called \"" + name + "\" already exists. Do you want to overwrite the existing automation or to keep it ?");
                dialog.setButton(0, "Overwrite");
                dialog.setButton(1, "Keep existing");
                dialog.setTitle("Importing automation \"" + name + "\"");
                dialog.setVisible(true);
                if (dialog.getButton() != 0) {
                    continue;
                }
                automation.loadAttributes(attributes);
                automation.removeAllGenerators();
            } else {
                automation = new Automation(attributes);
            }
            automation.loadGenerators(automationNode.getChildNodes(), generatorFactory);
            addAutomation(automation);
        }
    }

    /**
     * looks for all occurences of the "slave" openTag in the provided document
     * and create a modbus slave for each.
     * @param doc
     */
    private void loadSlaves(Document doc) {
        NodeList slavesList = doc.getElementsByTagName("slave");
        for (int i = 0; i < slavesList.getLength(); i++) {
            Node slaveNode = slavesList.item(i);
            ModbusSlave slave = new ModbusSlave(this, slaveNode);
            addModbusSlave(slave);
        }
    }

    /**
     * This method scans the content of the document in order to find all
     * "<binding>" tags, and then call the loadBinding(Node) method for each
     * of them.
     * @param doc
     */
    private void loadBindings(Document doc, ModbusSlave slave) {
        NodeList list = doc.getElementsByTagName("binding");
        for (int i = 0; i < list.getLength(); i++) {
            Node bindingNode = list.item(i);
            try {
                loadBinding(bindingNode, slave);
            } catch (InstantiationException ex) {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(ModbusPalPane.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * This method will examine the content of a "<binding>" tag in order to
     * parse the attributes it contains, and also the child tags that may exist.
     * @param node reference on the Node that represents a "<binding>" tag in the
     * project.
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    private void loadBinding(Node node, ModbusSlave slave) throws InstantiationException, IllegalAccessException {
        NamedNodeMap attributes = node.getAttributes();
        Node automationNode = attributes.getNamedItem("automation");
        String automationName = automationNode.getNodeValue();
        Automation automation = getAutomation(automationName);
        if (automation == null) {
            return;
        }
        Node classNode = attributes.getNamedItem("class");
        String className = classNode.getNodeValue();
        Node orderNode = attributes.getNamedItem("order");
        String orderValue = orderNode.getNodeValue();
        int wordOrder = Integer.parseInt(orderValue);
        Node parentRegister = XMLTools.findParent(node, "register");
        String parentAddress = XMLTools.getAttribute(ModbusPalXML.XML_ADDRESS_ATTRIBUTE, parentRegister);
        int registerAddress = Integer.parseInt(parentAddress);
        Binding binding = bindingFactory.newInstance(className);
        binding.setup(automation, wordOrder);
        if (slave == null) {
            Node parentSlave = XMLTools.findParent(parentRegister, "slave");
            String slaveAddress = XMLTools.getAttribute(ModbusPalXML.XML_SLAVE_ID_ATTRIBUTE, parentSlave);
            int slaveId = Integer.parseInt(slaveAddress);
            slave = getModbusSlave(slaveId);
        }
        slave.getHoldingRegisters().bind(registerAddress, binding);
    }

    /**
     * This method will only load "STARTUP" and "ON DEMAND" scripts.
     * Generator and binding scripts are loaded in a separate procedure.
     * @param doc
     * @param projectFile
     */
    private void loadScripts(Document doc, File projectFile) {
        NodeList startup = doc.getElementsByTagName("startup");
        for (int i = 0; i < startup.getLength(); i++) {
            loadScripts(startup.item(i), projectFile, ScriptRunner.SCRIPT_TYPE_AFTER_INIT);
        }
        NodeList ondemand = doc.getElementsByTagName("ondemand");
        for (int i = 0; i < ondemand.getLength(); i++) {
            loadScripts(ondemand.item(i), projectFile, ScriptRunner.SCRIPT_TYPE_ON_DEMAND);
        }
        NodeList bindings = doc.getElementsByTagName("bindings");
        for (int i = 0; i < bindings.getLength(); i++) {
            loadScripts(bindings.item(i), projectFile, ScriptRunner.SCRIPT_TYPE_OLD_BINDINGS);
        }
        NodeList generators = doc.getElementsByTagName("generators");
        for (int i = 0; i < generators.getLength(); i++) {
            loadScripts(generators.item(i), projectFile, ScriptRunner.SCRIPT_TYPE_OLD_GENERATORS);
        }
        NodeList scriptsList = doc.getElementsByTagName("scripts");
        for (int i = 0; i < scriptsList.getLength(); i++) {
            loadScripts(scriptsList.item(i), projectFile, ScriptRunner.SCRIPT_TYPE_ON_DEMAND);
        }
    }

    private void loadScripts(Node node, File projectFile, int assumedType) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node scriptNode = nodes.item(i);
            if (scriptNode.getNodeName().compareTo("script") == 0) {
                ScriptRunner runner = ScriptRunner.create(scriptNode, this, projectFile, true, assumedType);
                if (runner != null) {
                    addScript(runner);
                }
            }
        }
    }

    public void save(File file) throws FileNotFoundException, IOException {
        projectFile = file;
        save();
    }

    /**
     * Saves the current project into the project file. Implies that this
     * project was loaded from a project file. The same file is overwritten
     * with the current data held by this ModbusPalProject.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void save() throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(projectFile);
        save(out, projectFile);
        out.close();
    }

    private void save(OutputStream out, File projectFile) throws IOException {
        String xmlTag = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\r\n";
        out.write(xmlTag.getBytes());
        String docTag = "<!DOCTYPE modbuspal_project SYSTEM \"modbuspal.dtd\">\r\n";
        out.write(docTag.getBytes());
        String openTag = "<modbuspal_project>\r\n";
        out.write(openTag.getBytes());
        saveParameters(out);
        saveAutomations(out);
        saveSlaves(out);
        saveScripts(out, projectFile);
        String closeTag = "</modbuspal_project>\r\n";
        out.write(closeTag.getBytes());
    }

    /**
     * Saves the project's parameters that do not fall into the "slave" or "automation"
     * categories, like id generator and link settings. The parameters are written
     * into the output stream as XML tags.
     * @param out
     * @throws IOException
     */
    private void saveParameters(OutputStream out) throws IOException {
        idGenerator.save(out);
        saveLinks(out);
    }

    private void saveLinks(OutputStream out) throws IOException {
        StringBuilder openTag = new StringBuilder("<links ");
        openTag.append("selected=\"").append(selectedLink).append("\" ");
        openTag.append(">\r\n");
        out.write(openTag.toString().getBytes());
        saveTcpIpLinkParameters(out);
        saveSerialLinkParameters(out);
        saveReplayLinkParameters(out);
        String closeTag = "</links>\r\n";
        out.write(closeTag.getBytes());
    }

    private void saveTcpIpLinkParameters(OutputStream out) throws IOException {
        StringBuilder tag = new StringBuilder("<tcpip ");
        tag.append("port=\"").append(linkTcpipPort).append("\" ");
        tag.append("/>\r\n");
        out.write(tag.toString().getBytes());
    }

    private void saveSerialLinkParameters(OutputStream out) throws IOException {
        StringBuilder openTag = new StringBuilder("<serial ");
        openTag.append("com=\"").append(linkSerialComId).append("\" ");
        openTag.append("baudrate=\"").append(linkSerialBaudrate).append("\" ");
        switch(linkSerialParity) {
            case ModbusSerialLink.PARITY_NONE:
                openTag.append("parity=\"none\" ");
                break;
            case ModbusSerialLink.PARITY_ODD:
                openTag.append("parity=\"odd\" ");
                break;
            default:
            case ModbusSerialLink.PARITY_EVEN:
                openTag.append("parity=\"even\" ");
                break;
        }
        switch(linkSerialStopBits) {
            default:
            case ModbusSerialLink.STOP_BITS_1:
                openTag.append("stops=\"1\" ");
                break;
            case ModbusSerialLink.STOP_BITS_1_5:
                openTag.append("stops=\"1.5\" ");
                break;
            case ModbusSerialLink.STOP_BITS_2:
                openTag.append("stops=\"2\" ");
                break;
        }
        openTag.append(">\r\n");
        out.write(openTag.toString().getBytes());
        StringBuilder flowControl = new StringBuilder("<flowcontrol ");
        flowControl.append("xonxoff=\"").append(String.valueOf(linkSerialXonXoff)).append("\" ");
        flowControl.append("rtscts=\"").append(String.valueOf(linkSerialRtsCts)).append("\" ");
        flowControl.append("/>\r\n");
        out.write(flowControl.toString().getBytes());
        String closeTag = "</serial>\r\n";
        out.write(closeTag.getBytes());
    }

    private void saveReplayLinkParameters(OutputStream out) throws IOException {
        if (linkReplayFile != null) {
            String openTag = "<replay>\r\n";
            out.write(openTag.getBytes());
            String absTag = "<abs>" + linkReplayFile.getPath() + "</abs>\r\n";
            out.write(absTag.getBytes());
            String rel = FileTools.makeRelative(projectFile, linkReplayFile);
            if (rel != null) {
                String relTag = "<rel>" + rel + "</rel>\r\n";
                out.write(relTag.getBytes());
            }
            String closeTag = "</replay>\r\n";
            out.write(closeTag.getBytes());
        }
    }

    private void saveAutomations(OutputStream out) throws IOException {
        for (int i = 0; i < automations.size(); i++) {
            Automation automation = automations.get(i);
            automation.save(out);
        }
    }

    private void saveSlaves(OutputStream out) throws IOException {
        for (int i = 0; i < ModbusConst.MAX_MODBUS_SLAVE; i++) {
            ModbusSlave slave = getModbusSlave(i);
            if (slave != null) {
                slave.save(out, true);
            }
        }
    }

    private void saveScripts(OutputStream out, File projectFile) throws IOException {
        if (scripts.isEmpty()) {
            return;
        }
        String openTag = "<scripts>\r\n";
        out.write(openTag.getBytes());
        for (ScriptRunner runner : scripts) {
            runner.save(out, projectFile);
        }
        String closeTag = "</scripts>\r\n";
        out.write(closeTag.getBytes());
    }

    /**
     * Find the automation which exactly matches the specified name.
     * @param name is the name of the automation to obtain
     * @return the Automation object associated with the specified name, or null
     * is no automation exists with that name.
     */
    public Automation getAutomation(String name) {
        if (name == null) {
            return null;
        }
        if (NullAutomation.NAME.compareTo(name) == 0) {
            return NullAutomation.getInstance();
        }
        for (int i = 0; i < automations.size(); i++) {
            Automation automation = automations.get(i);
            if (automation.getName().compareTo(name) == 0) {
                return automation;
            }
        }
        return null;
    }

    /**
     * Add the provided automation into the current application. Please note that
     * each automation in the project must have a unique name. The addition will
     * fail if an existing automation already uses the same name as the automation
     * you want to add.
     * @param automation
     * @return true if the automation is added successfully, false otherwise.
     */
    public boolean addAutomation(Automation automation) {
        String name = automation.getName();
        if (getAutomation(name) != null) {
            return false;
        }
        automations.add(automation);
        int index = automations.indexOf(automation);
        notifyAutomationAdded(automation, index);
        return true;
    }

    private void notifyAutomationAdded(Automation automation, int index) {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.automationAdded(automation, index);
            }
        }
    }

    /**
     * Get all the automations that are defined in the current project.
     * @return an array containing all the automations that are defined in
     * the project.
     */
    public Automation[] getAutomations() {
        Automation[] out = new Automation[0];
        return automations.toArray(out);
    }

    /**
     * Starts all the automations of the project.
     */
    public void startAllAutomations() {
        for (int i = 0; i < automations.size(); i++) {
            Automation auto = automations.get(i);
            auto.start();
        }
    }

    /**
     * Stops all the automations defined in the project.
     */
    public void stopAllAutomations() {
        for (int i = 0; i < automations.size(); i++) {
            Automation auto = automations.get(i);
            auto.stop();
        }
    }

    /**
     * Removes the specified automation from the project
     * @param automation the automation to remove
     */
    public void removeAutomation(Automation automation) {
        automation.disconnect();
        automations.remove(automation);
        notifyAutomationRemoved(automation);
    }

    void removeAllAutomations() {
        Automation list[] = new Automation[0];
        list = automations.toArray(list);
        for (int i = 0; i < list.length; i++) {
            removeAutomation(list[i]);
        }
    }

    private void notifyAutomationRemoved(Automation automation) {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.automationRemoved(automation);
            }
        }
    }

    /**
     * Checks if an automation with the specified name exists in the project
     * @param name the name of the automation to check
     * @return true if an automation with the same name exists.
     */
    public boolean automationExists(String name) {
        return (getAutomation(name) != null);
    }

    /**
     * the goal of this method is to verify that each automation in the project
     * has a unique name.
     * @param auto
     * @param name
     * @return a unique automation name. same value as 'name' if it is already
     * unique, or a modified version of 'name'.
     */
    public String checkAutomationNewName(Automation auto, String name) {
        {
            Automation already = getAutomation(name);
            if ((already != null) && (already != auto)) {
                if (name.matches(".*#(\\d+)$") == true) {
                    int pos = name.lastIndexOf('#');
                    name = name.substring(0, pos);
                } else {
                    name = name.trim() + " ";
                }
                name = name + "#" + String.valueOf(idGenerator.createID());
            }
            auto.setName(name);
        }
        return name;
    }

    /**
     * This method adds an automation slave into the application. If an automation
     * with the same name already exists, a dialog will popup and invite the user
     * to choose between keeping the existing automation or replacing it by the new.
     * @param automation the new automation to add
     * @return a reference on the new or existing automation, depending on the
     * user's choice. null if an error occured.
     */
    public Automation submitAutomation(Automation automation) {
        String name = automation.getName();
        Automation existing = getAutomation(name);
        if (existing != null) {
            ErrorMessage conflict = new ErrorMessage(2);
            conflict.setTitle("Address conflict");
            conflict.append("You are trying to add a new automation with name \"" + name + "\".");
            conflict.append("An existing automation already exists with this name. What do you want to do ?");
            conflict.setButton(0, "Keep existing");
            conflict.setButton(1, "Replace with new");
            conflict.setVisible(true);
            if (conflict.getButton() == 0) {
                return existing;
            } else {
                removeAutomation(existing);
            }
        }
        if (addAutomation(automation) == true) {
            return automation;
        }
        return null;
    }

    /**
     * remove all instances of the generator whose name is passed
     * in argument. the method will scan all automations of the current
     * project and remove each instance of the generator identified
     * by the provided name.
     * @param classname
     */
    private void removeAllGenerators(String classname) {
        int max = automations.size();
        for (int i = 0; i < max; i++) {
            Automation auto = automations.get(i);
            auto.removeAllGenerators(classname);
        }
    }

    /**
     * @see #addGeneratorInstantiator(Generator)
     * @param g the instantiable generator to add into the project.
     * @deprecated spelling error in the name of the method
     */
    @Deprecated
    public void addGeneratorInstanciator(Generator g) {
        addGeneratorInstantiator(g);
    }

    /**
     * @see #addGeneratorInstantiator(Generator)
     * @param g the instantiable generator to add into the project.
     * @param name the instantiable generator should have this name. 
     * this parameter is ignored.
     * @deprecated parameter "name" is ignored
     */
    @Deprecated
    public void addGeneratorInstantiator(String name, Generator g) {
        addGeneratorInstantiator(g);
    }

    /**
     * Adds the specified generator to the list of instantiable generators.
     * It means that the Automation Editor will be able to instantiate 
     * generators of this type.
     * @param g the instantiable generator to add into the project.
     */
    public void addGeneratorInstantiator(Generator g) {
        generatorFactory.add(g);
    }

    /**
     * Returns the "library" that holds all the instantiable generators
     * added in the project.
     * @return the "library" of instantiable generators
     */
    public InstantiableManager<Generator> getGeneratorFactory() {
        return generatorFactory;
    }

    private void importOldGenerators(ScriptRunner runner) {
        runner.updateForOldGenerators();
        runner.execute();
    }

    /**
     * remove all instances of the generator whose name is passed
     * in argument. the method will scan all automations of the current
     * project and remove each instance of the generator identified
     * by the provided name.
     * @param classname
     */
    void removeAllBindings(String classname) {
        for (int i = 0; i < ModbusConst.MAX_MODBUS_SLAVE; i++) {
            ModbusSlave slave = getModbusSlave(i);
            if (slave != null) {
                slave.removeAllBindings(classname);
            }
        }
    }

    /**
     * @see #addBindingInstantiator(Binding)
     * @param b the instantiable binding to add into the project.
     * @deprecated spelling error in the name of the method
     */
    @Deprecated
    public void addBindingInstanciator(Binding b) {
        addBindingInstantiator(b);
    }

    /**
     * Adds the specified instantiable binding into the project. It means
     * that the user will be able to use this new type of binding in the
     * MODBUS slave editor.
     * @param b the instantiable binding to add in the project
     */
    public void addBindingInstantiator(Binding b) {
        bindingFactory.add(b);
    }

    /**
     * @see #addBindingInstantiator(Binding)
     * @param b the instantiable binding to add into the project.
     * @param name the instantiable binding should have this name. 
     * this parameter is ignored.
     * @deprecated parameter "name" is ignored
     */
    @Deprecated
    public void addBindingInstantiator(String name, Binding b) {
        addBindingInstantiator(b);
    }

    /**
     * Returns the "library" that holds all the instantiable bindings
     * added in the project.
     * @return the "library" of instantiable bindings
     */
    public InstantiableManager<Binding> getBindingFactory() {
        return bindingFactory;
    }

    private void importOldBindings(ScriptRunner runner) {
        runner.updateForOldBindings();
        runner.execute();
    }

    /**
     * remove all instances of the function whose name is passed
     * in argument. the method will scan all slaves of the current
     * project and remove each instance of the function identified
     * by the provided name.
     * @param classname
     */
    void removeAllFunctions(String classname) {
        for (int i = 0; i < ModbusConst.MAX_MODBUS_SLAVE; i++) {
            ModbusSlave slave = getModbusSlave(i);
            if (slave != null) {
                slave.removeAllFunctions(classname);
            }
        }
    }

    /**
     * @see #addFunctionInstantiator(ModbusPduProcessor)
     * @param mspp the instantiable ModbusPduProcessor to add into the project.
     * @deprecated spelling error in the name of the method
     */
    @Deprecated
    public void addFunctionInstanciator(ModbusPduProcessor mspp) {
        addFunctionInstantiator(mspp);
    }

    /**
     * @see #addFunctionInstantiator(ModbusPduProcessor)
     * @param pi the instantiable ModbusPduProcessor to add into the project.
     * @param name the instantiable ModbusPduProcessor should have this name. 
     * this parameter is ignored.
     * @deprecated parameter "name" is ignored
     */
    @Deprecated
    public void addFunctionInstantiator(String name, ModbusPduProcessor pi) {
        addFunctionInstantiator(pi);
    }

    /**
     * Adds the specified instantiable ModbusPduProcessor into the project. 
     * It means that the user will be able to use this new type of 
     * ModbusPduProcessor in the MODBUS slave editor.
     * @param pi the instantiable ModbusPduProcessor to add in the project
     */
    public void addFunctionInstantiator(ModbusPduProcessor pi) {
        functionFactory.add(pi);
    }

    /**
     * Returns the "library" that holds all the instantiable ModbusPduProcessors
     * added in the project.
     * @return the "library" of instantiable ModbusPduProcessors
     */
    public InstantiableManager<ModbusPduProcessor> getFunctionFactory() {
        return functionFactory;
    }

    /**
     * This method adds a modbus slave into the application. The addition will
     * fail a modbus slave with the same id already exists in thecurrent project.
     * @param slave the new modbus slave to add
     * @return true if added successfully, false otherwise.
     */
    public boolean addModbusSlave(ModbusSlave slave) {
        int slaveID = slave.getSlaveId();
        if (getModbusSlave(slaveID) != null) {
            return false;
        }
        setModbusSlave(slaveID, slave);
        return true;
    }

    @Override
    protected void notifySlaveAdded(ModbusSlave slave) {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.modbusSlaveAdded(slave);
            }
        }
    }

    /**
     * Returns an array of ModbusSlave objects, which hold all the
     * information on the slaves with the specified name. In ModbusPal, multiple
     * slaves can have the same name.
     * @param name the name of the slaves you want to get
     * @return an array of ModbusSlave objects, each slave in the array having the
     * specified name.
     */
    public ModbusSlave[] findModbusSlaves(String name) {
        ArrayList<ModbusSlave> found = new ArrayList<ModbusSlave>();
        for (int i = 0; i < ModbusConst.MAX_MODBUS_SLAVE; i++) {
            ModbusSlave slave = getModbusSlave(i);
            if (slave != null) {
                if (slave.getName().compareTo(name) == 0) {
                    found.add(slave);
                }
            }
        }
        if (found.isEmpty()) {
            return null;
        } else {
            ModbusSlave retval[] = new ModbusSlave[0];
            retval = found.toArray(retval);
            return retval;
        }
    }

    /**
     * This method adds a modbus slave into the application. If a modbus slave
     * with the same id already exists, a dialog will popup and invite the user
     * to choose between keeping the existing slave or replacing it by the new.
     * @param slave the new modbus slave to add
     * @return a reference on the new or existing modbus slave, depending on the
     * user's choice. null if an error occured.
     */
    public ModbusSlave submitModbusSlave(ModbusSlave slave) {
        int slaveID = slave.getSlaveId();
        if (getModbusSlave(slaveID) != null) {
            ErrorMessage conflict = new ErrorMessage(2);
            conflict.setTitle("Address conflict");
            conflict.append("You are trying to add a new slave with address " + slaveID + ".");
            conflict.append("An existing slave already uses this address. What do you want to do ?");
            conflict.setButton(0, "Keep existing");
            conflict.setButton(1, "Replace with new");
            conflict.setVisible(true);
            if (conflict.getButton() == 0) {
                return getModbusSlave(slaveID);
            } else {
                removeModbusSlave(slaveID);
            }
        }
        if (addModbusSlave(slave) == true) {
            return slave;
        }
        return null;
    }

    /**
     * Removes the specified MODBUS  slave from the project.
     * @param slaveID the slave number of the MODBUS slave to remove
     */
    public void removeModbusSlave(int slaveID) {
        ModbusSlave slave = getModbusSlave(slaveID);
        if (slave != null) {
            slave.clear();
        }
        setModbusSlave(slaveID, null);
    }

    /**
     * Removes the specified MODBUS  slave from the project.
     * @param slave the modbus slave to remove from the project.
     */
    public void removeModbusSlave(ModbusSlave slave) {
        int slaveID = slave.getSlaveId();
        removeModbusSlave(slaveID);
    }

    @Override
    protected void notifySlaveRemoved(ModbusSlave slave) {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.modbusSlaveRemoved(slave);
            }
        }
    }

    /**
     * Creates a duplicate of an existing MODBUS slave.
     * @param idSrc the slave number of the model MODBUS slave to duplicate
     * @param idDst the slave number of the copy 
     * @param name the name to give to the copy
     */
    public void duplicateModbusSlave(int idSrc, int idDst, String name) {
        ModbusSlave newSlave = new ModbusSlave(idDst);
        newSlave.setName(name);
        try {
            File tempFile = File.createTempFile("modbuspal", null);
            tempFile.deleteOnExit();
            exportSlave(tempFile, idSrc, true, false);
            importSlave(tempFile, idDst, true, false);
            tempFile.delete();
        } catch (Exception ex) {
            Logger.getLogger(ModbusPalProject.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        addModbusSlave(newSlave);
    }

    /**
     * Enables or disables the specified MODBUS slave
     * @param slaveID slave number of the MODUBS slave to enable or disable
     * @param b if true, enables the MODBUS slave. if false, disables the
     * MODBUS slave.
     */
    public void setSlaveEnabled(int slaveID, boolean b) {
        ModbusSlave ms = getModbusSlave(slaveID, learnModeEnabled);
        if (ms != null) {
            ms.setEnabled(b);
        }
    }

    /**
     * determine if the slave is enabled or not. If "learn mode" is enabled, this
     * method will always return true. Otherwise, it will return true only if the
     * slave exists in the list of known slaves AND if that slave is enabled.
     * In all cases, if the slave with the provided ID doesn't exist in the list
     * of known slaves, it will be added to the list.
     * @param slaveID
     * @return true if the slave is enabled; false otherwise.
     */
    public boolean isSlaveEnabled(int slaveID) {
        if ((slaveID < 0) || (slaveID >= ModbusConst.MAX_MODBUS_SLAVE)) {
            return false;
        }
        ModbusSlave ms = getModbusSlave(slaveID, learnModeEnabled);
        if (ms != null) {
            return ms.isEnabled();
        }
        return false;
    }

    /**
     * Exports the settings of a MODBUS slave into an XML export file.
     * @param exportFile the file where the settings will be written
     * @param modbusID the slave number of the MODBUS slave to export
     * @param withBindings if true, the specs of the bindings are exported.
     * @param withAutomations if true, the automations used by the slave are exported as well
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void exportSlave(File exportFile, int modbusID, boolean withBindings, boolean withAutomations) throws FileNotFoundException, IOException {
        ModbusSlave exportedSlave = getModbusSlave(modbusID);
        OutputStream out = new FileOutputStream(exportFile);
        String xmlTag = "<?xml version=\"1.0\"?>\r\n";
        out.write(xmlTag.getBytes());
        String docTag = "<!DOCTYPE modbuspal_slave SYSTEM \"modbuspal.dtd\">\r\n";
        out.write(docTag.getBytes());
        String openTag = "<modbuspal_slave>\r\n";
        out.write(openTag.getBytes());
        if (withAutomations == true) {
            String names[] = exportedSlave.getRequiredAutomations();
            for (int i = 0; i < names.length; i++) {
                Automation automation = getAutomation(names[i]);
                automation.save(out);
            }
        }
        exportedSlave.save(out, withBindings);
        String closeTag = "</modbuspal_slave>\r\n";
        out.write(closeTag.getBytes());
        out.close();
    }

    /**
     * Loads the settings of a MODBUS slave from an XML file. Configure
     * the specified MODBUS slave with these settings, create the MODBUS slave
     * if it doesn't already exist.
     * @param importFile the XML file containing the settings to import
     * @param idDst the modbus number of the MODBUS slave to configure with the
     * settings from the DOM document.
     * @param withBindings if true, import the bindings that may be defined
     * in the DOM document
     * @param withAutomations if true, import the automations that may be 
     * defined in the DOM document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    public void importSlave(File importFile, int idDst, boolean withBindings, boolean withAutomations) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        Document doc = XMLTools.ParseXML(importFile);
        doc.getDocumentElement().normalize();
        importSlave(doc, idDst, withBindings, withAutomations);
    }

    /**
     * Loads the settings of a MODBUS slave from a XML structure. Configure
     * the specified MODBUS slave with these settings, create the MODBUS slave
     * if it doesn't already exist.
     * @param doc the DOM document where the settings are stored
     * @param idDst the modbus number of the MODBUS slave to configure with the
     * settings from the DOM document.
     * @param withBindings if true, import the bindings that may be defined
     * in the DOM document
     * @param withAutomations if true, import the automations that may be 
     * defined in the DOM document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    public void importSlave(Document doc, int idDst, boolean withBindings, boolean withAutomations) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        ModbusSlave target = getModbusSlave(idDst, true);
        importSlave(doc, target, withBindings, withAutomations);
    }

    /**
     * Loads the settings of a MODBUS slave from a XML structure.
     * @param doc the DOM document where the settings are stored
     * @param target the modbus slave to configure from the settings stored in
     * the DOM document
     * @param withBindings if true, import the bindings that may be defined
     * in the DOM document
     * @param withAutomations if true, import the automations that may be 
     * defined in the DOM document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException 
     */
    public void importSlave(Document doc, ModbusSlave target, boolean withBindings, boolean withAutomations) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        NodeList slaves = doc.getElementsByTagName("slave");
        Node slaveNode = slaves.item(0);
        if (withAutomations == true) {
            loadAutomations(doc);
        }
        target.load(this, slaveNode, true);
        if (withBindings == true) {
            loadBindings(doc, target);
        }
    }

    /**
     * Returns the ModbusPduProcessor that has been assigned to the specified
     * MODBUS slave for the specified MODBUS function.
     * @param slaveID the slave number
     * @param functionCode the function code
     * @return the ModbusPduProcessor, or null if none is assigned for the
     * specified MODBUS slave and function code.
     */
    public ModbusPduProcessor getSlavePduProcessor(int slaveID, byte functionCode) {
        ModbusSlave ms = getModbusSlave(slaveID, learnModeEnabled);
        if (ms != null) {
            return ms.getPduProcessor(functionCode);
        }
        return null;
    }

    /**
     * Checks if the modbus registers exist. If they don't and if the "lear mode"
     * is enabled, then they are created.
     * @param slaveID the slave number for which the existance of registers
     * must be checked
     * @param startingAddress the address of the first register to check
     * @param quantity the quantity of registers to check
     * @return true if all the registers exist, false if one or more of the 
     * registers do not exist.
     * @deprecated the preferred way to do that is to get the ModbusSlave 
     * reference with ModbusPalProject#getModbusSlave(), then call 
     * ModbusSlave#getHoldingRegisters(), and then ModbusRegisters#exist()
     */
    @Deprecated
    public boolean holdingRegistersExist(int slaveID, int startingAddress, int quantity) {
        ModbusSlave ms = getModbusSlave(quantity, learnModeEnabled);
        if (ms == null) {
            return false;
        }
        return ms.getHoldingRegisters().exist(startingAddress, quantity, learnModeEnabled);
    }

    /**
     * Checks if the modbus coils exist. If they don't and if the "lear mode"
     * is enabled, then they are created.
     * @param slaveID the slave number for which the existance of coils
     * must be checked
     * @param startingAddress the address of the first coil to check
     * @param quantity the quantity of coils to check
     * @return true if all the coils exist, false if one or more of the 
     * coils do not exist.
     * @deprecated the preferred way to do that is to get the ModbusSlave 
     * reference with ModbusPalProject#getModbusSlave(), then call 
     * ModbusSlave#getCoils(), and then ModbusCoils#exist()
     */
    @Deprecated
    public boolean coilsExist(int slaveID, int startingAddress, int quantity) {
        ModbusSlave ms = getModbusSlave(quantity, learnModeEnabled);
        if (ms == null) {
            return false;
        }
        return ms.getCoils().exist(startingAddress, quantity, learnModeEnabled);
    }

    /**
     * Adds the specified script file into the project.
     * @param scriptFile the script file to add
     */
    public void addScript(File scriptFile) {
        ScriptRunner runner = ScriptRunner.create(this, scriptFile, ScriptRunner.SCRIPT_TYPE_ON_DEMAND);
        addScript(runner);
    }

    private void addScript(ScriptRunner runner) {
        scripts.add(runner);
        notifyScriptAdded(runner);
    }

    /**
     * Removes the specified script from the project
     * @param runner the script to remove
     */
    public void removeScript(ScriptRunner runner) {
        if (scripts.remove(runner) == true) {
            notifyScriptRemoved(runner);
        }
    }

    private void notifyScriptAdded(ScriptRunner runner) {
        synchronized (scriptListeners) {
            for (ScriptListener l : scriptListeners) {
                l.scriptAdded(runner);
            }
        }
    }

    private void notifyScriptRemoved(ScriptRunner runner) {
        synchronized (scriptListeners) {
            for (ScriptListener l : scriptListeners) {
                l.scriptRemoved(runner);
            }
        }
    }

    /**
     * Returns a list of the scripts currently defined in the project,
     * filtered by type.
     * @param type one of the ScriptRunner.SCRIPT_TYPE_xxx constants
     * @return an Iterable encapsulating the list of scripts contained
     * in the project, using the specified filter.
     */
    public Iterable<ScriptRunner> getScripts(int type) {
        ArrayList<ScriptRunner> output = new ArrayList<ScriptRunner>();
        for (ScriptRunner sr : scripts) {
            if ((type == ScriptRunner.SCRIPT_TYPE_ANY) || (sr.getType() == type)) {
                output.add(sr);
            }
        }
        return output;
    }

    /**
     * Adds a ModbusPalListener to the list of listeners
     * @param l the listener to add
     */
    public void addModbusPalListener(ModbusPalListener l) {
        synchronized (listeners) {
            if (listeners.contains(l) == false) {
                listeners.add(l);
            }
        }
    }

    /**
     * Removes a ModbusPalListener from the list of listeners
     * @param l the listener to remove
     */
    public void removeModbusPalListener(ModbusPalListener l) {
        synchronized (listeners) {
            if (listeners.contains(l) == true) {
                listeners.remove(l);
            }
        }
    }

    /**
     * Adds a ScriptListener to the list of listeners
     * @param l the listener to add
     */
    public void addScriptListener(ScriptListener l) {
        synchronized (scriptListeners) {
            if (scriptListeners.contains(l) == false) {
                scriptListeners.add(l);
            }
        }
    }

    /**
     * Removes a ScriptListener from the list of listeners
     * @param l the listener to remove
     */
    public void removeScriptListener(ScriptListener l) {
        synchronized (scriptListeners) {
            if (scriptListeners.contains(l) == true) {
                scriptListeners.remove(l);
            }
        }
    }

    /**
     * Enables or disables the "learn mode"
     * @param en true to enable the "learn mode", false to disable it.
     */
    public void setLearnModeEnabled(boolean en) {
        learnModeEnabled = en;
    }

    /**
     * Checks if the "learn mode" is enabled or disabled
     * @return true if it is currently enabled, false otherwise.
     */
    public boolean isLeanModeEnabled() {
        return learnModeEnabled;
    }

    /**
     * Triggers the ModbusPalListeners to notify them
     * that a PDU was not serviced.
     */
    public void notifyPDUnotServiced() {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.pduNotServiced();
            }
        }
    }

    /**
     * Triggers the ModbusPalListeners to notify them
     * that a PDU was processed.
     */
    public void notifyPDUprocessed() {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.pduProcessed();
            }
        }
    }

    /**
     * Triggers the ModbusPalListeners to notify them
     * that an exception reply has been sent instead
     * of a normal reply.
     */
    public void notifyExceptionResponse() {
        synchronized (listeners) {
            for (ModbusPalListener l : listeners) {
                l.pduException();
            }
        }
    }

    /**
     * 
     * @param slaveID
     * @param functionCode
     * @return always true
     * @deprecated this method was never implemented
     */
    @Deprecated
    public boolean isFunctionEnabled(int slaveID, byte functionCode) {
        return true;
    }

    /**
     * Optimize the DOM document (expected to contain modbuspal project
     * information). 
     * @param doc the DOM document to optimize
     * @param fix if true, removes from the DOM document any faulty elements.
     */
    public static void optimize(Document doc, boolean fix) {
        optimizeBindingsVsAutomations(doc, fix);
    }

    /**
     * this method will scan the content of the DOM document, which
     * is expected to contain modbuspal project information. It will
     * remove from the DOM document any automation that is not referenced
     * by any binding. Then, if "fix" is true, it will removed any bindings
     * that refer to an automation that is not defined in the DOM document.
     * @param doc the DOM document to optimize
     * @param fix if true, fix the DOM document by removing any binding
     * refering to a non-existing automation.
     */
    private static void optimizeBindingsVsAutomations(Document doc, boolean fix) {
        NodeList bindingList = doc.getElementsByTagName("binding");
        NodeList automationList = doc.getElementsByTagName("automation");
        for (int i = 0; i < automationList.getLength(); i++) {
            String name = XMLTools.getAttribute("name", automationList.item(i));
            boolean matched = false;
            for (int j = 0; j < bindingList.getLength(); j++) {
                String automation = XMLTools.getAttribute("automation", bindingList.item(j));
                if (automation.compareTo(name) == 0) {
                    matched = true;
                    break;
                }
            }
            if (matched == false) {
                doc.removeChild(automationList.item(i));
            }
        }
        if (fix == true) {
            for (int i = 0; i < bindingList.getLength(); i++) {
                String automation = XMLTools.getAttribute("automation", bindingList.item(i));
                boolean matched = false;
                for (int j = 0; j < automationList.getLength(); j++) {
                    String name = XMLTools.getAttribute("name", automationList.item(j));
                    if (automation.compareTo(name) == 0) {
                        matched = true;
                        break;
                    }
                }
                if (matched == false) {
                    doc.removeChild(doc);
                }
            }
        }
    }

    /**
     * Returns the name of the project. Actually, the project
     * is named after the file from which it has been loaded, or into
     * which it has been saved.
     * @return name of the project.
     */
    public String getName() {
        if (projectFile == null) {
            return "no name";
        }
        return projectFile.getName();
    }
}
