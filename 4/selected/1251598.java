package uk.ac.lkl.migen.system.task.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.lkl.migen.system.task.goal.Goal;
import uk.ac.lkl.migen.system.task.goal.GoalRepository;
import uk.ac.lkl.migen.system.ai.um.AbstractLearnerModelAttribute;
import uk.ac.lkl.migen.system.ai.um.UserModelAttributeRepository;
import uk.ac.lkl.migen.system.ai.reasoning.ReasonerType;
import uk.ac.lkl.migen.system.ai.reasoning.ReasonerTypeRepository;

public class ActivityDesignerTool extends DesignerTool {

    private static final String activitiesDirectoryPath = "src/data/tasks/activities";

    private static final String workingDirectoryPath = "saved/taskdesigner";

    private static final String masterFilePath = "src/data/task-wrappers/linking1.xml";

    private static final String allWithId = "//*[@id]";

    private static final String allWithIdRef = "//*[@idRef]";

    private boolean overwrite;

    private ComponentInfo projectInfo;

    private List<ComponentInfo> activityDocumentTemplates, initialModels, constructions, misconstructions;

    private List<Goal> goals;

    private List<AbstractLearnerModelAttribute> learningObjectives;

    private List<ReasonerType> reasoners;

    public ActivityDesignerTool() {
        parser.getDomConfig().setParameter("cdata-sections", Boolean.TRUE);
        window = new JFrame("Activity & Task Design Tool - MiGen");
        window.setIconImage(getImage("/data/icon16x16.png"));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1024, 768);
        overwrite = false;
        projectInfo = null;
        initialModels = null;
        constructions = null;
        misconstructions = null;
        activityDocumentTemplates = getTemplates();
        goals = GoalRepository.getGoals();
        learningObjectives = UserModelAttributeRepository.getLearningObjectives();
        reasoners = ReasonerTypeRepository.getReasoners();
        loadData();
        buildGUI(window);
        window.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("quit".equals(command)) {
            System.exit(0);
        } else if ("generate".equals(command)) {
            generate();
        } else if ("task-name".equals(command)) {
            JTextField textField = (JTextField) namedComponents.get("task-name");
            String text = filter(textField.getText(), patternXmlName);
            if (text != textField.getText()) textField.setText(text);
        } else {
            debug("action: " + command);
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        JList target = (JList) e.getSource();
        String command = target.getName();
        debug("changed " + command);
        int begin = e.getFirstIndex(), end = e.getLastIndex();
        if ("list-presentations".equals(command)) {
            debug("selected between " + begin + " and " + end);
        }
    }

    public void valueChanged(TreeSelectionEvent e) {
        JTree target = (JTree) e.getSource();
        String command = target.getName();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) target.getLastSelectedPathComponent();
        if (node != null && "list-presentations".equals(command)) {
            if (node.isLeaf()) {
                projectInfo = (ComponentInfo) node.getUserObject();
                initialModels = getInitialModels(projectInfo.projectName);
                constructions = getConstructions(projectInfo.projectName);
                misconstructions = getMisconstructions(projectInfo.projectName);
                ((CheckboxList) namedComponents.get("list-initial-models")).setListData(initialModels);
                ((CheckboxList) namedComponents.get("list-constructions")).setListData(constructions);
                ((CheckboxList) namedComponents.get("list-misconstructions")).setListData(misconstructions);
            } else {
                target.expandRow(target.getMinSelectionRow());
            }
        }
    }

    private void buildGUI(JFrame frame) {
        frame.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new java.awt.Insets(10, 10, 10, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        JPanel panel;
        JButton button;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.weighty = 1.0;
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        frame.add(new TitledWrapper("list-templates", "Activity template"), constraints);
        constraints.gridx = 2;
        frame.add(new TitledWrapper("list-presentations", "Presentation"), constraints);
        constraints.gridx = 3;
        frame.add(new TitledWrapper("list-initial-models", "Initial model"), constraints);
        constraints.gridx = 4;
        frame.add(new TitledWrapper("list-goals", "Goals"), constraints);
        constraints.gridy += 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.weighty = 1.0;
        constraints.gridx = 1;
        frame.add(new TitledWrapper("list-reasoners", "Reasoner"), constraints);
        constraints.gridx = 2;
        frame.add(new TitledWrapper("list-constructions", "Constructions"), constraints);
        constraints.gridx = 3;
        frame.add(new TitledWrapper("list-misconstructions", "Expected difficulties"), constraints);
        constraints.gridx = 4;
        frame.add(new TitledWrapper("list-learning-objectives", "Learning objectives"), constraints);
        constraints.gridy += 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.weighty = 0.0;
        constraints.gridwidth = 3;
        constraints.weightx = 3.0;
        constraints.gridx = 1;
        panel = new JPanel();
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.add(new TitledWrapper("description", "Description for student"));
        panel.add(new TitledWrapper("notes", "Your notes"));
        panel.setMinimumSize(new java.awt.Dimension(240, 240));
        frame.add(panel, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.gridx = 4;
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new TitledWrapper("task-name", "Activity name"), BorderLayout.NORTH);
        button = new JButton("Generate");
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setActionCommand("generate");
        button.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(button);
        panel.add(buttonPanel, BorderLayout.EAST);
        JLabel banner = new JLabel(getIcon("/data/splash200x45.png"));
        banner.setMinimumSize(new java.awt.Dimension(10, 45));
        banner.setPreferredSize(new java.awt.Dimension(10, 45));
        panel.add(banner, BorderLayout.SOUTH);
        frame.add(panel, constraints);
    }

    private void loadData() {
        JTree tree;
        JTextArea textArea;
        JTextField textField;
        CheckboxList checkboxList;
        textArea = new JTextArea();
        textArea.setRows(6);
        addNamedComponent("description", textArea);
        textArea = new JTextArea();
        textArea.setRows(3);
        addNamedComponent("notes", textArea);
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Projects");
        File directory = getDirectory();
        File[] files = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory() && !".svn".equals(files[i].getName())) {
                    String name = files[i].getName();
                    DefaultMutableTreeNode project = new DefaultMutableTreeNode(name);
                    top.add(project);
                    List<ComponentInfo> treeData = getPresentations(name);
                    for (int p = 0; p < treeData.size(); ++p) {
                        DefaultMutableTreeNode presentation = new DefaultMutableTreeNode();
                        presentation.setUserObject(treeData.get(p));
                        project.add(presentation);
                    }
                }
            }
        }
        tree = new JTree(top);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addNamedComponent("list-presentations", tree);
        checkboxList = new CheckboxList();
        addNamedComponent("list-constructions", checkboxList);
        checkboxList = new CheckboxList();
        addNamedComponent("list-misconstructions", checkboxList);
        checkboxList = new CheckboxList();
        checkboxList.setExclusive(true);
        addNamedComponent("list-initial-models", checkboxList);
        checkboxList = new CheckboxList();
        checkboxList.setExclusive(true);
        checkboxList.setListData(activityDocumentTemplates);
        addNamedComponent("list-templates", checkboxList);
        checkboxList = new CheckboxList();
        checkboxList.setListData(goals);
        addNamedComponent("list-goals", checkboxList);
        checkboxList = new CheckboxList();
        checkboxList.setListData(learningObjectives);
        addNamedComponent("list-learning-objectives", checkboxList);
        checkboxList = new CheckboxList();
        checkboxList.setExclusive(true);
        checkboxList.setListData(reasoners);
        addNamedComponent("list-reasoners", checkboxList);
        textField = new JTextField();
        addNamedComponent("task-name", textField);
    }

    private List<ComponentInfo> getPresentations(String name) {
        return getComponents(name, "Presentation[-_.]?(.*).xml");
    }

    private List<ComponentInfo> getConstructions(String name) {
        return getComponents(name, "ExpectedModel[-_.]?(.*).xml");
    }

    private List<ComponentInfo> getMisconstructions(String name) {
        return getComponents(name, "ExpectedDifficulties[-_.]?(.*).xml");
    }

    private List<ComponentInfo> getInitialModels(String name) {
        return getComponents(name, "InitialModel[-_.]?(.*).xml");
    }

    private List<ComponentInfo> getTemplates() {
        return getComponents("-templates", "Template[-_.]?(.*).xml");
    }

    protected File getDirectory(String name) {
        if ("-templates".equals(name)) {
            return new File(activitiesDirectoryPath);
        } else if (name != null) {
            return new File(workingDirectoryPath + File.separator + name);
        } else {
            return new File(workingDirectoryPath);
        }
    }

    protected Element find(String xpath, Node root, String errorIfNotFound) {
        Element element = xpathElement(compileXPath(xpath), root);
        if (null == element) error(errorIfNotFound);
        return element;
    }

    protected Set<String> getIdentifiers(Document document) {
        Set<String> identifiers = new HashSet<String>();
        NodeList nodesWithIdentifier = xpathList(compileXPath(allWithId), document);
        for (int i = 0; i < nodesWithIdentifier.getLength(); ++i) {
            Node node = nodesWithIdentifier.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                String identifier = ((Element) node).getAttribute("id");
                if (identifiers.contains(identifier)) {
                    warn("The identifier '" + identifier + "' is not unique in " + document.getDocumentElement().getNodeName());
                } else {
                    identifiers.add(identifier);
                }
            }
        }
        return identifiers;
    }

    /**
     * Make all identifiers inside tree unique wrt. the given
     * existing identifiers
     *
     * @param tree root of the tree where the identifiers will be made unique
     * @param existingIdentifiers set of existing identifiers. It will be
     *        modified to add the identifiers from the tree
     */
    protected void makeIdentifiersUnique(Node tree, Set<String> existingIdentifiers) {
        Map<String, String> identifierMap = new HashMap<String, String>();
        NodeList nodesWithIdentifier = xpathList(compileXPath(allWithId), tree);
        for (int i = 0; i < nodesWithIdentifier.getLength(); ++i) {
            Node node = nodesWithIdentifier.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                String identifier = ((Element) node).getAttribute("id");
                while (existingIdentifiers.contains(identifier)) {
                    char digit = identifier.charAt(identifier.length() - 1);
                    if (digit >= '0' && digit <= '8') {
                        ++digit;
                        identifier = identifier.substring(0, identifier.length() - 1) + digit;
                    } else {
                        identifier += "0";
                    }
                }
                identifierMap.put(((Element) node).getAttribute("id"), identifier);
                ((Element) node).setAttribute("id", identifier);
                existingIdentifiers.add(identifier);
            } else {
                error("Problem in makeIdentifiersUnique(): the node from the document to be modified is not an element");
            }
        }
        NodeList nodesWithIdRef = xpathList(compileXPath(allWithIdRef), tree);
        for (int i = 0; i < nodesWithIdRef.getLength(); ++i) {
            Node node = nodesWithIdRef.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                String idRef = ((Element) node).getAttribute("idRef");
                ((Element) node).setAttribute("idRef", identifierMap.get(idRef));
            } else {
                error("Problem in makeIdentifiersUnique(): the node from the document to be modified is not an element");
            }
        }
    }

    /**
     * Insert a model from an XML document as the content of the given element.
     * The previous content of the element is removed.
     * 
     * Reusing the existingIdentifiers set is not done for performance
     * as the improvement is negligible, but so that the error messages
     * about duplicated identifiers in the container element document
     * appear only once, not every time a new model is inserted.
     *
     * @param model document with Expresser model
     * @param element container element to hold the model
     * @param existingIdentifiers set of existing identifiers to ensure that
     *        the new inserted ones are unique.
     */
    protected void insertModel(Document model, Element element, Set<String> existingIdentifiers) {
        makeIdentifiersUnique(model, existingIdentifiers);
        Document document = element.getOwnerDocument();
        while (element.hasChildNodes()) element.removeChild(element.getFirstChild());
        element.appendChild(document.createTextNode("\n"));
        element.appendChild(document.importNode(model.getDocumentElement(), true));
        element.appendChild(document.createTextNode("\n"));
    }

    private void generate() {
        Document master = loadXML(masterFilePath);
        if (null == master) {
            error("Can not load master document file");
            return;
        }
        Set<String> existingIdentifiers = getIdentifiers(master);
        CheckboxList list;
        JTextField textField;
        JTextArea textArea;
        ComponentInfo componentInfo;
        int selectedIndex;
        Element element;
        String description, notes, taskName;
        list = (CheckboxList) namedComponents.get("list-templates");
        selectedIndex = list.getSelectedIndex();
        if (selectedIndex < 0) {
            request("Please select an activity template", list);
            return;
        }
        componentInfo = activityDocumentTemplates.get(selectedIndex);
        if (null == projectInfo) {
            request("Please select a presentation", namedComponents.get("list-presentations"));
            return;
        }
        debug("project: " + projectInfo.componentFullPath);
        textArea = (JTextArea) namedComponents.get("description");
        description = textArea.getText();
        textArea = (JTextArea) namedComponents.get("notes");
        notes = textArea.getText();
        textField = (JTextField) namedComponents.get("task-name");
        taskName = filter(textField.getText(), patternXmlName);
        if (taskName.equals(textField.getText())) {
            if (0 == taskName.length()) {
                request("Please write a task name.", textField);
                return;
            }
        } else {
            textField.setText(taskName);
            if (0 == taskName.length()) {
                request("Please write a valid task name.<br/>Invalid characters like spaces,<br/>slashes, etc. will be removed.", textField);
                return;
            }
        }
        Document template, initialModel;
        List<Document> constructionModels, misconstructionModels;
        constructionModels = new ArrayList<Document>();
        misconstructionModels = new ArrayList<Document>();
        Iterator<Document> documentIterator;
        template = loadXML(componentInfo.componentFullPath);
        element = find("/ActivityDocument", template, "Activity document root element not found");
        if (null == element) return;
        element.setAttribute("name", taskName);
        element = find("/ActivityDocument/ActivityDocumentComponentList/ActivityDocumentComponent[@title='TrainTrack Model']", template, "Can't find ActivityDocumentComponent with title 'TrainTrack Model' in template");
        if (null == element) return;
        insertModel(loadXML(new File(projectInfo.componentFullPath)), element, existingIdentifiers);
        list = (CheckboxList) namedComponents.get("list-initial-models");
        initialModel = loadXML(projectInfo.componentFullPath);
        element = find("/activity-description/rulesReasoner", master, "Can't find rulesReasoner in the master file");
        if (null == element) return;
        list = (CheckboxList) namedComponents.get("list-reasoners");
        selectedIndex = list.getSelectedIndex();
        if (selectedIndex >= 0) {
            ReasonerType reasoner = reasoners.get(selectedIndex);
            element.setAttribute("id", Integer.toString(reasoner.getId()));
            Node comment = master.createComment(" " + list.getSelectedButton().getText() + " ");
            Node next = element.getNextSibling();
            if (next != null) {
                element.getParentNode().insertBefore(comment, next);
            } else {
                element.getParentNode().appendChild(comment);
            }
        }
        list = (CheckboxList) namedComponents.get("list-constructions");
        for (int i = 0; i < list.getComponentCount(); ++i) {
            JToggleButton item = (JToggleButton) list.getComponent(i);
            if (item.isSelected()) {
                String modelFilePath = constructions.get(i).componentFullPath;
                constructionModels.add(loadXML(modelFilePath));
            }
        }
        list = (CheckboxList) namedComponents.get("list-misconstructions");
        for (int i = 0; i < list.getComponentCount(); ++i) {
            JToggleButton item = (JToggleButton) list.getComponent(i);
            if (item.isSelected()) {
                String modelFilePath = misconstructions.get(i).componentFullPath;
                misconstructionModels.add(loadXML(modelFilePath));
            }
        }
        element = find("/activity-description/goalsDefinition", master, "Can't find goalsDefinition in the master file");
        if (null == element) return;
        while (element.hasChildNodes()) element.removeChild(element.getFirstChild());
        list = (CheckboxList) namedComponents.get("list-goals");
        for (int i = 0; i < list.getComponentCount(); ++i) {
            JToggleButton item = (JToggleButton) list.getComponent(i);
            Goal goal = goals.get(i);
            if (item.isSelected()) {
                Element goalElement = master.createElement("goal");
                goalElement.setAttribute("id", Integer.toString(goal.getId()));
                element.appendChild(master.createTextNode("\n        "));
                element.appendChild(goalElement);
                element.appendChild(master.createComment(" " + item.getText() + " "));
            }
        }
        element.appendChild(master.createTextNode("\n    "));
        element = find("/activity-description/learningObjectives", master, "Can't find learningObjectives in the master file");
        if (null == element) return;
        while (element.hasChildNodes()) element.removeChild(element.getFirstChild());
        list = (CheckboxList) namedComponents.get("list-learning-objectives");
        for (int i = 0; i < list.getComponentCount(); ++i) {
            JToggleButton item = (JToggleButton) list.getComponent(i);
            AbstractLearnerModelAttribute objective = learningObjectives.get(i);
            if (item.isSelected()) {
                Element objectiveElement = master.createElement("learningObjective");
                objectiveElement.setAttribute("id", Integer.toString(objective.getId()));
                element.appendChild(master.createTextNode("\n        "));
                element.appendChild(objectiveElement);
                element.appendChild(master.createComment(" " + item.getText() + " "));
            }
        }
        element.appendChild(master.createTextNode("\n    "));
        element = find("/activity-description/student-description", master, "Can't find student-description in the master file");
        if (null == element) return;
        while (element.hasChildNodes()) element.removeChild(element.getFirstChild());
        element.appendChild(master.createTextNode("\n" + description + "\n"));
        element = find("/activity-description/notes", master, "Can't find notes in the master file");
        if (null == element) return;
        while (element.hasChildNodes()) element.removeChild(element.getFirstChild());
        element.appendChild(master.createTextNode("\n" + notes + "\n"));
        element = find("/activity-description/activity-document", master, "Can't find activity-document in the master file");
        if (null == element) return;
        insertModel(template, element, existingIdentifiers);
        element = find("/activity-description/initialModel", master, "Can't find initialModel in the master file");
        if (null == element) return;
        insertModel(initialModel, element, existingIdentifiers);
        element = find("/activity-description/expected-constructions", master, "Can't find expected-constructions in the master file");
        if (null == element) return;
        documentIterator = constructionModels.iterator();
        while (documentIterator.hasNext()) {
            insertModel(documentIterator.next(), element, existingIdentifiers);
        }
        element = find("/activity-description/common-misconstructions", master, "Can't find common-misconstructions in the master file");
        if (null == element) return;
        documentIterator = misconstructionModels.iterator();
        while (documentIterator.hasNext()) {
            insertModel(documentIterator.next(), element, existingIdentifiers);
        }
        getIdentifiers(master);
        String fileName = filter(taskName + ".xml", patternFileName);
        File file = new File(getDirectory(), fileName);
        if (!file.exists() || overwrite || (overwrite = confirm("Warning", "There is already a file called\n" + fileName + "\n\nOverwrite it?"))) {
            saveXML(master, file);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new ActivityDesignerTool();
            }
        });
    }
}
