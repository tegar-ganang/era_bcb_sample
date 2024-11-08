package vmap.modes.mindmapmode;

import vmap.main.VmapMain;
import vmap.controller.MindMapNodesSelection;
import vmap.main.XMLParseException;
import vmap.main.Tools;
import vmap.modes.MapAdapter;
import vmap.modes.MindMapNode;
import vmap.modes.MindIcon;
import vmap.modes.MindMapLink;
import vmap.modes.mindmapmode.MindMapCloudModel;
import vmap.modes.mindmapmode.MindMapArrowLinkModel;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.TimerTask;
import java.util.Timer;
import java.lang.UnsatisfiedLinkError;
import java.lang.NoClassDefFoundError;
import java.net.URL;
import java.net.MalformedURLException;
import javax.swing.JOptionPane;
import java.nio.channels.FileLock;
import vmap.modes.LinkRegistryAdapter;
import vmap.modes.MindMapLinkRegistry;

public class MindMapMapModel extends MapAdapter {

    LockManager lockManager;

    private LinkRegistryAdapter linkRegistry;

    private Timer timerForAutomaticSaving;

    public MindMapMapModel(VmapMain frame) {
        this(new MindMapNodeModel((String) frame.getResources().getString("new_mindmap"), frame), frame);
    }

    public MindMapMapModel(MindMapNodeModel root, VmapMain frame) {
        super(frame);
        lockManager = frame.getProperty("experimental_file_locking_on").equals("true") ? new LockManager() : new DummyLockManager();
        linkRegistry = new LinkRegistryAdapter();
        setRoot(root);
        readOnly = false;
        timerForAutomaticSaving = new Timer();
        int delay = Integer.parseInt(getFrame().getProperty("time_for_automatic_save"));
        int numberOfTempFiles = Integer.parseInt(getFrame().getProperty("number_of_different_files_for_automatic_save"));
        boolean filesShouldBeDeletedAfterShutdown = Tools.safeEquals(getFrame().getProperty("delete_automatic_saves_at_exit"), "true");
        String path = getFrame().getProperty("path_to_automatic_saves");
        if (Tools.safeEquals(path, "default")) {
            path = null;
        }
        if (Tools.safeEquals(path, "vmap_home")) {
            path = getFrame().getVmapDirectory();
        }
        File dirToStore = null;
        if (path != null) {
            dirToStore = new File(path);
            if (!dirToStore.isDirectory()) {
                dirToStore = null;
                System.err.println("Temporary directory " + path + " not found. Disabling automatic store.");
                delay = Integer.MAX_VALUE;
            }
        }
        timerForAutomaticSaving.schedule(new doAutomaticSave(this, numberOfTempFiles, filesShouldBeDeletedAfterShutdown, dirToStore), delay, delay);
    }

    public MindMapLinkRegistry getLinkRegistry() {
        return linkRegistry;
    }

    public String getRestoreable() {
        return getFile() == null ? null : "MindMap:" + getFile().getAbsolutePath();
    }

    public void setNodeColor(MindMapNodeModel node, Color color) {
        node.setColor(color);
        nodeChanged(node);
    }

    public void blendNodeColor(MindMapNodeModel node) {
        Color mapColor = getBackgroundColor();
        Color nodeColor = node.getColor();
        if (nodeColor == null) {
            nodeColor = Tools.xmlToColor(getFrame().getProperty("standardnodecolor"));
        }
        node.setColor(new Color((3 * mapColor.getRed() + nodeColor.getRed()) / 4, (3 * mapColor.getGreen() + nodeColor.getGreen()) / 4, (3 * mapColor.getBlue() + nodeColor.getBlue()) / 4));
        nodeChanged(node);
    }

    public void setNodeFont(MindMapNodeModel node, Font font) {
        node.setFont(font);
        nodeChanged(node);
    }

    public void setEdgeColor(MindMapNodeModel node, Color color) {
        ((MindMapEdgeModel) node.getEdge()).setColor(color);
        nodeChanged(node);
    }

    public void setEdgeWidth(MindMapNodeModel node, int width) {
        ((MindMapEdgeModel) node.getEdge()).setWidth(width);
        nodeChanged(node);
    }

    public void setNodeStyle(MindMapNodeModel node, String style) {
        node.setStyle(style);
        nodeStructureChanged(node);
    }

    public void setEdgeStyle(MindMapNodeModel node, String style) {
        MindMapEdgeModel edge = (MindMapEdgeModel) node.getEdge();
        edge.setStyle(style);
        nodeStructureChanged(node);
    }

    public void setBold(MindMapNodeModel node) {
        node.setBold(!node.isBold());
        nodeChanged(node);
    }

    public void setCloud(MindMapNodeModel node) {
        if (node.getCloud() == null) {
            node.setCloud(new MindMapCloudModel(node, getFrame()));
        } else {
            node.setCloud(null);
        }
        nodeChanged(node);
    }

    public void setCloudColor(MindMapNodeModel node, Color color) {
        ((MindMapCloudModel) node.getCloud()).setColor(color);
        nodeChanged(node);
    }

    public void setCloudWidth(MindMapNodeModel node, int width) {
        if (node.getCloud() == null) {
            setCloud(node);
        }
        ((MindMapCloudModel) node.getCloud()).setWidth(width);
        nodeChanged(node);
    }

    public void setCloudStyle(MindMapNodeModel node, String style) {
        if (node.getCloud() == null) {
            setCloud(node);
        }
        MindMapCloudModel cloud = (MindMapCloudModel) node.getCloud();
        cloud.setStyle(style);
        nodeStructureChanged(node);
    }

    public void addIcon(MindMapNodeModel node, MindIcon icon) {
        node.addIcon(icon);
        nodeChanged(node);
    }

    public int removeLastIcon(MindMapNodeModel node) {
        int retval = node.removeLastIcon();
        nodeChanged(node);
        return retval;
    }

    /** Source holds the MindMapArrowLinkModel and points to the id placed in target.*/
    public void addLink(MindMapNodeModel source, MindMapNodeModel target) {
        if (getLinkRegistry().getLabel(target) == null) {
            getLinkRegistry().registerLinkTarget(target);
        }
        MindMapArrowLinkModel linkModel = new MindMapArrowLinkModel(source, target, getFrame());
        linkModel.setDestinationLabel(getLinkRegistry().getLabel(target));
        getLinkRegistry().registerLink(linkModel);
        nodeChanged(target);
        nodeChanged(source);
    }

    public void removeReference(MindMapNode source, MindMapArrowLinkModel arrowLink) {
        getLinkRegistry().deregisterLink(arrowLink);
        nodeChanged(source);
        nodeChanged(arrowLink.getTarget());
    }

    public void changeArrowsOfArrowLink(MindMapNode source, MindMapArrowLinkModel arrowLink, boolean hasStartArrow, boolean hasEndArrow) {
        arrowLink.setStartArrow((hasStartArrow) ? "Default" : "None");
        arrowLink.setEndArrow((hasEndArrow) ? "Default" : "None");
        nodeChanged(source);
    }

    public void setArrowLinkColor(MindMapNode source, MindMapArrowLinkModel arrowLink, Color color) {
        arrowLink.setColor(color);
        nodeChanged(source);
    }

    public void setItalic(MindMapNodeModel node) {
        node.setItalic(!node.isItalic());
        nodeChanged(node);
    }

    public void setUnderlined(MindMapNodeModel node) {
        node.setUnderlined(!node.isUnderlined());
        nodeChanged(node);
    }

    public void setNormalFont(MindMapNodeModel node) {
        node.setItalic(false);
        node.setBold(false);
        node.setUnderlined(false);
        nodeChanged(node);
    }

    public void setFontFamily(MindMapNodeModel node, String fontFamily) {
        node.estabilishOwnFont();
        node.setFont(getFrame().getController().getFontThroughMap(new Font(fontFamily, node.getFont().getStyle(), node.getFont().getSize())));
        nodeChanged(node);
    }

    public void setFontSize(MindMapNodeModel node, int fontSize) {
        node.estabilishOwnFont();
        node.setFont(node.getFont().deriveFont((float) fontSize));
        nodeChanged(node);
    }

    public void increaseFontSize(MindMapNodeModel node, int increment) {
        node.estabilishOwnFont();
        node.setFontSize(node.getFont().getSize() + increment);
        nodeChanged(node);
    }

    public String toString() {
        if ((getFile() != null) && (getFile().isDirectory())) {
            return (getFile().getName());
        } else if (getFile() != null) {
            return (getFile().getParentFile().getName());
        }
        return (null);
    }

    public boolean saveHTML(MindMapNodeModel rootNodeOfBranch, File file) {
        try {
            BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            String el = System.getProperty("line.separator");
            fileout.write("<html>" + el + "<head>" + el + "<title>" + rootNodeOfBranch.saveHTML_escapeUnicodeAndSpecialCharacters(rootNodeOfBranch.toString()) + "</title>" + el + "<style type=\"text/css\">" + el + "    span.foldopened { color: white; font-size: xx-small;" + el + "    border-width: 1; font-family: monospace; padding: 0em 0.25em 0em 0.25em; background: #e0e0e0;" + el + "    VISIBILITY: visible;" + el + "    cursor:pointer; }" + el + "" + el + "" + el + "    span.foldclosed { color: #666666; font-size: xx-small;" + el + "    border-width: 1; font-family: monospace; padding: 0em 0.25em 0em 0.25em; background: #e0e0e0;" + el + "    VISIBILITY: hidden;" + el + "    cursor:pointer; }" + el + "" + el + "    span.foldspecial { color: #666666; font-size: xx-small; border-style: none solid solid none;" + el + "    border-color: #CCCCCC; border-width: 1; font-family: sans-serif; padding: 0em 0.1em 0em 0.1em; background: #e0e0e0;" + el + "    cursor:pointer; }" + el + "" + el + "    li { list-style: none; }" + el + "" + el + "    span.l { color: red; font-weight: bold; }" + el + "" + el + "    a:link {text-decoration: none; color: black; }" + el + "    a:visited {text-decoration: none; color: black; }" + el + "    a:active {text-decoration: none; color: black; }" + el + "    a:hover {text-decoration: none; color: black; background: #eeeee0; }" + el + "" + el + "</style>" + el + "<!-- ^ Position is not set to relative / absolute here because of Mozilla -->" + el + "</head>" + el + "<body>" + el);
            String htmlExportFoldingOption = getFrame().getProperty("html_export_folding");
            boolean writeFoldingCode = (htmlExportFoldingOption.equals("html_export_fold_currently_folded") && rootNodeOfBranch.hasFoldedStrictDescendant()) || htmlExportFoldingOption.equals("html_export_fold_all");
            if (writeFoldingCode) {
                fileout.write("" + el + "<script language=\"JavaScript\">" + el + "   // Here we implement folding. It works fine with MSIE5.5, MSIE6.0 and" + el + "   // Mozilla 0.9.6." + el + "" + el + "   if (document.layers) {" + el + "      //Netscape 4 specific code" + el + "      pre = 'document.';" + el + "      post = ''; }" + el + "   if (document.getElementById) {" + el + "      //Netscape 6 specific code" + el + "      pre = 'document.getElementById(\"';" + el + "      post = '\").style'; }" + el + "   if (document.all) {" + el + "      //IE4+ specific code" + el + "      pre = 'document.all.';" + el + "      post = '.style'; }" + el + "" + el + "function layer_exists(layer) {" + el + "   try {" + el + "      eval(pre + layer + post);" + el + "      return true; }" + el + "   catch (error) {" + el + "      return false; }}" + el + "" + el + "function show_layer(layer) {" + el + "   eval(pre + layer + post).position = 'relative'; " + el + "   eval(pre + layer + post).visibility = 'visible'; }" + el + "" + el + "function hide_layer(layer) {" + el + "   eval(pre + layer + post).visibility = 'hidden';" + el + "   eval(pre + layer + post).position = 'absolute'; }" + el + "" + el + "function hide_folder(folder) {" + el + "    hide_folding_layer(folder)" + el + "    show_layer('show'+folder);" + el + "" + el + "    scrollBy(0,0); // This is a work around to make it work in Browsers (Explorer, Mozilla)" + el + "}" + el + "" + el + "function show_folder(folder) {" + el + "    // Precondition: all subfolders are folded" + el + "" + el + "    show_layer('hide'+folder);" + el + "    hide_layer('show'+folder);" + el + "    show_layer('fold'+folder);" + el + "" + el + "    scrollBy(0,0); // This is a work around to make it work in Browsers (Explorer, Mozilla)" + el + "" + el + "    var i;" + el + "    for (i=1; layer_exists('fold'+folder+'_'+i); ++i) {" + el + "       show_layer('show'+folder+'_'+i); }" + el + "}" + el + "" + "function show_folder_completely(folder) {" + el + "    // Precondition: all subfolders are folded" + el + "" + el + "    show_layer('hide'+folder);" + el + "    hide_layer('show'+folder);" + el + "    show_layer('fold'+folder);" + el + "" + el + "    scrollBy(0,0); // This is a work around to make it work in Browsers (Explorer, Mozilla)" + el + "" + el + "    var i;" + el + "    for (i=1; layer_exists('fold'+folder+'_'+i); ++i) {" + el + "       show_folder_completely(folder+'_'+i); }" + el + "}" + el + "" + el + "" + el + "" + el + "function hide_folding_layer(folder) {" + el + "   var i;" + el + "   for (i=1; layer_exists('fold'+folder+'_'+i); ++i) {" + el + "       hide_folding_layer(folder+'_'+i); }" + el + "" + el + "   hide_layer('hide'+folder);" + el + "   hide_layer('show'+folder);" + el + "   hide_layer('fold'+folder);" + el + "" + el + "   scrollBy(0,0); // This is a work around to make it work in Browsers (Explorer, Mozilla)" + el + "}" + el + "" + el + "function fold_document() {" + el + "   var i;" + el + "   var folder = '1';" + el + "   for (i=1; layer_exists('fold'+folder+'_'+i); ++i) {" + el + "       hide_folder(folder+'_'+i); }" + el + "}" + el + "" + el + "function unfold_document() {" + el + "   var i;" + el + "   var folder = '1';" + el + "   for (i=1; layer_exists('fold'+folder+'_'+i); ++i) {" + el + "       show_folder_completely(folder+'_'+i); }" + el + "}" + el + "" + el + "</script>" + el);
                fileout.write("<SPAN class=foldspecial onclick=\"fold_document()\">All +</SPAN>" + el);
                fileout.write("<SPAN class=foldspecial onclick=\"unfold_document()\">All -</SPAN>" + el);
            }
            rootNodeOfBranch.saveHTML(fileout, "1", 0, true, true, 1);
            if (writeFoldingCode) {
                fileout.write("<SCRIPT language=JavaScript>" + el);
                fileout.write("fold_document();" + el);
                fileout.write("</SCRIPT>" + el);
            }
            fileout.write("</body>" + el);
            fileout.write("</html>" + el);
            fileout.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error in MindMapMapModel.saveHTML(): ");
            e.printStackTrace();
            return false;
        }
    }

    public String getAsPlainText(List mindMapNodes) {
        try {
            StringWriter stringWriter = new StringWriter();
            BufferedWriter fileout = new BufferedWriter(stringWriter);
            for (ListIterator it = mindMapNodes.listIterator(); it.hasNext(); ) {
                ((MindMapNodeModel) it.next()).saveTXT(fileout, 0);
            }
            fileout.close();
            return stringWriter.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveTXT(MindMapNodeModel rootNodeOfBranch, File file) {
        try {
            BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            rootNodeOfBranch.saveTXT(fileout, 0);
            fileout.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error in MindMapMapModel.saveTXT(): ");
            e.printStackTrace();
            return false;
        }
    }

    public String getAsRTF(List mindMapNodes) {
        try {
            StringWriter stringWriter = new StringWriter();
            BufferedWriter fileout = new BufferedWriter(stringWriter);
            saveRTF(mindMapNodes, fileout);
            fileout.close();
            return stringWriter.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveRTF(List mindMapNodes, BufferedWriter fileout) {
        try {
            HashSet colors = new HashSet();
            for (ListIterator it = mindMapNodes.listIterator(); it.hasNext(); ) {
                ((MindMapNodeModel) it.next()).collectColors(colors);
            }
            String colorTableString = "{\\colortbl;\\red0\\green0\\blue255;";
            HashMap colorTable = new HashMap();
            int colorPosition = 2;
            for (Iterator it = colors.iterator(); it.hasNext(); ++colorPosition) {
                Color color = (Color) it.next();
                colorTableString += "\\red" + color.getRed() + "\\green" + color.getGreen() + "\\blue" + color.getBlue() + ";";
                colorTable.put(color, new Integer(colorPosition));
            }
            colorTableString += "}";
            fileout.write("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fswiss\\fcharset0 Arial;}" + colorTableString + "}" + "\\viewkind4\\uc1\\pard\\f0\\fs20{}");
            for (ListIterator it = mindMapNodes.listIterator(); it.hasNext(); ) {
                ((MindMapNodeModel) it.next()).saveRTF(fileout, 0, colorTable);
            }
            fileout.write("}");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Return the success of saving
     */
    public boolean save(File file) {
        return save(file, false);
    }

    /**
     * Return the success of saving
     */
    public boolean save(File file, boolean present) {
        return saveInternal(file, false, present);
    }

    /** 
      * This method is intended to provide both normal 
      * save routines and saving of temporary (internal) files.
      */
    private boolean saveInternal(File file, boolean isInternal, boolean pres) {
        if (!isInternal && readOnly) {
            System.err.println("Attempt to save read-only map.");
            return false;
        }
        try {
            BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            fileout.write("<?xml version = \"1.0\"?>\n");
            ((MindMapNodeModel) getRoot()).save(fileout, this, pres);
            fileout.close();
            if (!isInternal) {
                setFile(file);
                setSaved(true);
            }
            return true;
        } catch (FileNotFoundException e) {
            String message = Tools.expandPlaceholders(getText("save_failed"), file.getName());
            if (!isInternal) getFrame().getController().errorMessage(message); else getFrame().out(message);
            return false;
        } catch (Exception e) {
            System.err.println("Error in MindMapMapModel.save(): ");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to lock the map using a semaphore file
     * @param file
     * @return If the map is locked, return the name of the locking user, otherwise return null.
     * @throws Exception, when the locking failed for other reasons than that the
     * file is being edited.
     */
    public String tryToLock(File file) throws Exception {
        String lockingUser = lockManager.tryToLock(file);
        String lockingUserOfOldLock = lockManager.popLockingUserOfOldLock();
        if (lockingUserOfOldLock != null) {
            getFrame().getController().informationMessage(Tools.expandPlaceholders(getText("locking_old_lock_removed"), file.getName(), lockingUserOfOldLock));
        }
        if (lockingUser == null) {
            readOnly = false;
        }
        return lockingUser;
    }

    public void load(File file) throws FileNotFoundException, IOException, XMLParseException {
        if (!file.exists()) {
            throw new FileNotFoundException(Tools.expandPlaceholders(getText("file_not_found"), file.getPath()));
        }
        if (!file.canWrite()) {
            readOnly = true;
        } else {
            try {
                String lockingUser = tryToLock(file);
                if (lockingUser != null) {
                    getFrame().getController().informationMessage(Tools.expandPlaceholders(getText("map_locked_by_open"), file.getName(), lockingUser));
                    readOnly = true;
                } else {
                    readOnly = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                getFrame().getController().informationMessage(Tools.expandPlaceholders(getText("locking_failed_by_open"), file.getName()));
                readOnly = true;
            }
        }
        MindMapNodeModel root = loadTree(file);
        if (root != null) {
            setRoot(root);
        }
        setFile(file);
        setSaved(true);
    }

    /** When a map is closed, this method is called. */
    public void destroy() {
        lockManager.releaseLock();
        lockManager.releaseTimer();
        timerForAutomaticSaving.cancel();
    }

    MindMapNodeModel loadTree(File file) throws XMLParseException, IOException {
        MindMapXMLElement mapElement = new MindMapXMLElement(getFrame(), file);
        try {
            mapElement.parseFromReader(new BufferedReader(new FileReader(file)));
        } catch (Exception ex) {
            System.err.println("Error while parsing file:" + ex);
            ex.printStackTrace();
            return null;
        }
        mapElement.processUnfinishedLinks(getLinkRegistry());
        return (MindMapNodeModel) mapElement.getMapChild();
    }

    public Transferable copy(MindMapNode node) {
        StringWriter stringWriter = new StringWriter();
        try {
            ((MindMapNodeModel) node).save(stringWriter, this, false);
        } catch (IOException e) {
        }
        return new StringSelection(stringWriter.toString());
    }

    public void splitNode(MindMapNode node, int caretPosition, String newText) {
        String currentText = newText != null ? newText : node.getTitle();
        String newContent = currentText.substring(caretPosition, currentText.length());
        MindMapNodeModel upperNode = new MindMapNodeModel(currentText.substring(0, caretPosition), getFrame());
        upperNode.setColor(node.getColor());
        upperNode.setFont(node.getFont());
        node.setUserObject(newContent);
        MindMapNode parent = node.getParentNode();
        insertNodeInto(upperNode, parent, parent.getChildPosition(node));
        nodeStructureChanged(parent);
    }

    public void joinNodes() {
        MindMapNode selectedNode = getFrame().getView().getSelected().getModel();
        ArrayList selectedNodes = getFrame().getView().getSelectedNodesSortedByY();
        String newContent = "";
        boolean firstLoop = true;
        for (Iterator it = selectedNodes.iterator(); it.hasNext(); ) {
            MindMapNode node = (MindMapNode) it.next();
            if (node.hasChildren()) {
                JOptionPane.showMessageDialog(node.getViewer(), getText("cannot_join_nodes_with_children"), "Vmap", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        for (Iterator it = selectedNodes.iterator(); it.hasNext(); ) {
            if (firstLoop) {
                firstLoop = false;
            } else {
                newContent += " ";
            }
            MindMapNode node = (MindMapNode) it.next();
            newContent += node.getTitle();
            if (node != selectedNode) {
                removeNodeFromParent(node);
            }
        }
        getFrame().getView().selectAsTheOnlyOneSelected(selectedNode.getViewer());
        changeNode(selectedNode, newContent);
    }

    public boolean importExplorerFavorites(File folder, MindMapNode target, boolean redisplay) {
        boolean favoritesFound = false;
        if (folder.isDirectory()) {
            File[] list = folder.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    MindMapNodeModel node = new MindMapNodeModel(list[i].getName(), getFrame());
                    insertNodeIntoNoEvent(node, target);
                    boolean favoritesFoundInSubfolder = importExplorerFavorites(list[i], node, false);
                    if (favoritesFoundInSubfolder) {
                        favoritesFound = true;
                    } else {
                        removeNodeFromParent(node, false);
                    }
                }
            }
            for (int i = 0; i < list.length; i++) {
                if (!list[i].isDirectory() && Tools.getExtension(list[i]).equals("url")) {
                    favoritesFound = true;
                    try {
                        MindMapNodeModel node = new MindMapNodeModel(Tools.removeExtension(list[i].getName()), getFrame());
                        BufferedReader in = new BufferedReader(new FileReader(list[i]));
                        while (in.ready()) {
                            String line = in.readLine();
                            if (line.startsWith("URL=")) {
                                node.setLink(line.substring(4));
                                break;
                            }
                        }
                        insertNodeIntoNoEvent(node, target);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (redisplay) {
            nodeStructureChanged(target);
        }
        return favoritesFound;
    }

    public void importFolderStructure(File folder, MindMapNode target, boolean redisplay) {
        if (folder.isDirectory()) {
            File[] list = folder.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    MindMapNodeModel node = new MindMapNodeModel(list[i].getName(), getFrame());
                    try {
                        node.setLink(list[i].toURL().toString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    insertNodeIntoNoEvent(node, target);
                    importFolderStructure(list[i], node, false);
                }
            }
            for (int i = 0; i < list.length; i++) {
                if (!list[i].isDirectory()) {
                    MindMapNodeModel node = new MindMapNodeModel(list[i].getName(), getFrame());
                    try {
                        node.setLink(list[i].toURL().toString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    insertNodeIntoNoEvent(node, target);
                }
            }
        }
        if (redisplay) {
            nodeStructureChanged(target);
        }
    }

    private MindMapNodeModel pasteXMLWithoutRedisplay(String pasted, MindMapNode target) throws XMLParseException {
        return pasteXMLWithoutRedisplay(pasted, target, false);
    }

    private MindMapNodeModel pasteXMLWithoutRedisplay(String pasted, MindMapNode target, boolean asSibling) throws XMLParseException {
        try {
            MindMapXMLElement element = new MindMapXMLElement(getFrame());
            element.parseFromReader(new StringReader(pasted));
            MindMapNodeModel node = (MindMapNodeModel) element.getUserObject();
            if (asSibling) {
                MindMapNode parent = target.getParentNode();
                insertNodeInto(node, parent, parent.getChildPosition(target));
            } else {
                insertNodeIntoNoEvent(node, target);
            }
            element.processUnfinishedLinks(getLinkRegistry());
            return node;
        } catch (IOException ee) {
            ee.printStackTrace();
            return null;
        }
    }

    static final Pattern nonLinkCharacter = Pattern.compile("[ \n()'\",;]");

    /**
     * Paste String (as opposed to other flavours)
     *
     * Split the text into lines; determine the new tree structure
     * by the number of leading spaces in lines.  In case that
     * trimmed line starts with protocol (http:, https:, ftp:),
     * create a link with the same content.
     *
     * If there was only one line to be pasted, return the pasted node, null otherwise.
     */
    private MindMapNode pasteStringWithoutRedisplay(String textFromClipboard, MindMapNode parent, boolean asSibling) {
        Pattern mailPattern = Pattern.compile("([^@ <>\\*']+@[^@ <>\\*']+)");
        String[] textLines = textFromClipboard.split("\n");
        if (textLines.length > 1) {
            getFrame().setWaitingCursor(true);
        }
        MindMapNode realParent = null;
        if (asSibling) {
            realParent = parent;
            parent = new MindMapNodeModel(getFrame());
        }
        ArrayList parentNodes = new ArrayList();
        ArrayList parentNodesDepths = new ArrayList();
        parentNodes.add(parent);
        parentNodesDepths.add(new Integer(-1));
        String[] linkPrefixes = { "http://", "ftp://", "https://" };
        MindMapNodeModel pastedNode = null;
        for (int i = 0; i < textLines.length; ++i) {
            String text = textLines[i];
            text = text.replaceAll("\t", "        ");
            if (text.matches(" *")) {
                continue;
            }
            int depth = 0;
            while (depth < text.length() && text.charAt(depth) == ' ') {
                ++depth;
            }
            String visibleText = text.trim();
            if (visibleText.matches("^http://(www\\.)?[^ ]*$")) {
                visibleText = visibleText.replaceAll("^http://(www\\.)?", "").replaceAll("(/|\\.[^\\./\\?]*)$", "").replaceAll("((\\.[^\\./]*\\?)|\\?)[^/]*$", " ? ...").replaceAll("_|%20", " ");
                String[] textParts = visibleText.split("/");
                visibleText = "";
                for (int textPartIdx = 0; textPartIdx < textParts.length; textPartIdx++) {
                    if (textPartIdx > 0) {
                        visibleText += " > ";
                    }
                    visibleText += textPartIdx == 0 ? textParts[textPartIdx] : Tools.firstLetterCapitalized(textParts[textPartIdx].replaceAll("^~*", ""));
                }
            }
            MindMapNodeModel node = new MindMapNodeModel(visibleText, getFrame());
            if (textLines.length == 1) {
                pastedNode = node;
            }
            Matcher mailMatcher = mailPattern.matcher(visibleText);
            if (mailMatcher.find()) {
                node.setLink("mailto:" + mailMatcher.group());
            }
            for (int j = 0; j < linkPrefixes.length; j++) {
                int linkStart = text.indexOf(linkPrefixes[j]);
                if (linkStart != -1) {
                    int linkEnd = linkStart;
                    while (linkEnd < text.length() && !nonLinkCharacter.matcher(text.substring(linkEnd, linkEnd + 1)).matches()) {
                        linkEnd++;
                    }
                    node.setLink(text.substring(linkStart, linkEnd));
                }
            }
            for (int j = parentNodes.size() - 1; j >= 0; --j) {
                if (depth > ((Integer) parentNodesDepths.get(j)).intValue()) {
                    for (int k = j + 1; k < parentNodes.size(); ++k) {
                        parentNodes.remove(k);
                        parentNodesDepths.remove(k);
                    }
                    MindMapNode target = (MindMapNode) parentNodes.get(j);
                    insertNodeIntoNoEvent(node, target);
                    parentNodes.add(node);
                    parentNodesDepths.add(new Integer(depth));
                    break;
                }
            }
        }
        if (asSibling) {
            for (Iterator i = parent.childrenUnfolded(); i.hasNext(); ) {
                insertNodeIntoNoEvent((MindMapNode) i.next(), realParent, asSibling);
            }
            nodeStructureChanged(realParent.getParentNode());
        } else {
            nodeStructureChanged(parent);
        }
        return pastedNode;
    }

    public void paste(Transferable t, MindMapNode target, boolean asSibling, boolean isLeft) {
        if (t == null) {
            return;
        }
        try {
            if (t.isDataFlavorSupported(MindMapNodesSelection.fileListFlavor)) {
                System.err.println("flflpas");
                List fileList = (List) t.getTransferData(MindMapNodesSelection.fileListFlavor);
                for (ListIterator it = fileList.listIterator(); it.hasNext(); ) {
                    File file = (File) it.next();
                    MindMapNodeModel node = new MindMapNodeModel(file.getName(), getFrame());
                    node.setLink(file.getAbsolutePath());
                    insertNodeIntoNoEvent(node, target, asSibling);
                }
                nodeStructureChanged(asSibling ? target.getParent() : target);
            } else if (t.isDataFlavorSupported(MindMapNodesSelection.mindMapNodesFlavor)) {
                String textFromClipboard = (String) t.getTransferData(MindMapNodesSelection.mindMapNodesFlavor);
                String[] textLines = textFromClipboard.split("<nodeseparator>");
                if (textLines.length > 1) {
                    getFrame().setWaitingCursor(true);
                }
                for (int i = 0; i < textLines.length; ++i) {
                    MindMapNodeModel newModel = pasteXMLWithoutRedisplay(textLines[i], target, asSibling);
                    newModel.setLeft(isLeft);
                }
            } else if (t.isDataFlavorSupported(MindMapNodesSelection.htmlFlavor)) {
                String textFromClipboard = (String) t.getTransferData(MindMapNodesSelection.htmlFlavor);
                MindMapNode pastedNode = pasteStringWithoutRedisplay((String) t.getTransferData(DataFlavor.stringFlavor), target, asSibling);
                textFromClipboard = textFromClipboard.replaceAll("<!--.*?-->", "");
                String[] links = textFromClipboard.split("<[aA][^>]*[hH][rR][eE][fF]=\"");
                MindMapNodeModel linkParentNode = null;
                URL referenceURL = null;
                boolean baseUrlCanceled = false;
                for (int i = 1; i < links.length; i++) {
                    String link = links[i].substring(0, links[i].indexOf("\""));
                    String textWithHtml = links[i].replaceAll("^[^>]*>", "").replaceAll("</[aA]>[\\s\\S]*", "");
                    String text = Tools.toXMLUnescapedText(textWithHtml.replaceAll("\\n", "").replaceAll("<[^>]*>", "").trim());
                    if (text.equals("")) {
                        text = link;
                    }
                    URL linkURL = null;
                    try {
                        linkURL = new URL(link);
                    } catch (MalformedURLException ex) {
                        try {
                            if (referenceURL == null && !baseUrlCanceled) {
                                String referenceURLString = JOptionPane.showInputDialog(getText("enter_base_url"));
                                if (referenceURLString == null) {
                                    baseUrlCanceled = true;
                                } else {
                                    referenceURL = new URL(referenceURLString);
                                }
                            }
                            linkURL = new URL(referenceURL, link);
                        } catch (MalformedURLException ex2) {
                        }
                    }
                    if (linkURL != null) {
                        if (links.length == 2 & pastedNode != null) {
                            ((MindMapNodeModel) pastedNode).setLink(linkURL.toString());
                            break;
                        }
                        if (linkParentNode == null) {
                            linkParentNode = new MindMapNodeModel("Links", getFrame());
                            insertNodeInto(linkParentNode, target);
                            linkParentNode.setBold(true);
                        }
                        MindMapNodeModel linkNode = new MindMapNodeModel(text, getFrame());
                        linkNode.setLink(linkURL.toString());
                        insertNodeInto(linkNode, linkParentNode);
                    }
                }
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String textFromClipboard = (String) t.getTransferData(DataFlavor.stringFlavor);
                pasteStringWithoutRedisplay(textFromClipboard, target, asSibling);
            }
            nodeStructureChanged(asSibling ? target.getParent() : target);
        } catch (Exception e) {
            e.printStackTrace();
        }
        getFrame().setWaitingCursor(false);
    }

    private class LockManager extends TimerTask {

        File lockedSemaphoreFile = null;

        Timer lockTimer = null;

        final long lockUpdatePeriod = 4 * 60 * 1000;

        final long lockSafetyPeriod = 5 * 60 * 1000;

        String lockingUserOfOldLock = null;

        private File getSemaphoreFile(File mapFile) {
            return new File(mapFile.getParent() + System.getProperty("file.separator") + "$~" + mapFile.getName() + "~");
        }

        public synchronized String popLockingUserOfOldLock() {
            String toReturn = lockingUserOfOldLock;
            lockingUserOfOldLock = null;
            return toReturn;
        }

        private void writeSemaphoreFile(File inSemaphoreFile) throws Exception {
            FileOutputStream semaphoreOutputStream = new FileOutputStream(inSemaphoreFile);
            FileLock lock = null;
            try {
                lock = semaphoreOutputStream.getChannel().tryLock();
                if (lock == null) {
                    semaphoreOutputStream.close();
                    System.err.println("Locking failed.");
                    throw new Exception();
                }
            } catch (UnsatisfiedLinkError eUle) {
            } catch (NoClassDefFoundError eDcdf) {
            }
            semaphoreOutputStream.write(System.getProperty("user.name").getBytes());
            semaphoreOutputStream.write('\n');
            semaphoreOutputStream.write(String.valueOf(System.currentTimeMillis()).getBytes());
            semaphoreOutputStream.close();
            semaphoreOutputStream = null;
            Tools.setHidden(inSemaphoreFile, true, false);
            if (lock != null) lock.release();
        }

        public synchronized String tryToLock(File file) throws Exception {
            File semaphoreFile = getSemaphoreFile(file);
            if (semaphoreFile == lockedSemaphoreFile) {
                return null;
            }
            try {
                BufferedReader semaphoreReader = new BufferedReader(new FileReader(semaphoreFile));
                String lockingUser = semaphoreReader.readLine();
                long lockTime = new Long(semaphoreReader.readLine()).longValue();
                long timeDifference = System.currentTimeMillis() - lockTime;
                if (timeDifference > lockSafetyPeriod) {
                    semaphoreReader.close();
                    lockingUserOfOldLock = lockingUser;
                    semaphoreFile.delete();
                } else return lockingUser;
            } catch (FileNotFoundException e) {
            }
            writeSemaphoreFile(semaphoreFile);
            if (lockTimer == null) {
                lockTimer = new Timer();
                lockTimer.schedule(this, lockUpdatePeriod, lockUpdatePeriod);
            }
            releaseLock();
            lockedSemaphoreFile = semaphoreFile;
            return null;
        }

        public synchronized void releaseLock() {
            if (lockedSemaphoreFile != null) {
                lockedSemaphoreFile.delete();
                lockedSemaphoreFile = null;
            }
        }

        public synchronized void releaseTimer() {
            if (lockTimer != null) {
                lockTimer.cancel();
                lockTimer = null;
            }
        }

        public synchronized void run() {
            if (lockedSemaphoreFile == null) {
                System.err.println("unexpected: lockedSemaphoreFile is null upon lock update");
                return;
            }
            try {
                Tools.setHidden(lockedSemaphoreFile, false, true);
                writeSemaphoreFile(lockedSemaphoreFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class DummyLockManager extends LockManager {

        public synchronized String popLockingUserOfOldLock() {
            return null;
        }

        public synchronized String tryToLock(File file) throws Exception {
            return null;
        }

        public synchronized void releaseLock() {
        }

        public synchronized void releaseTimer() {
        }

        public synchronized void run() {
        }
    }

    private class doAutomaticSave extends TimerTask {

        private MindMapMapModel model;

        private Vector tempFileStack;

        private int numberOfFiles;

        private boolean filesShouldBeDeletedAfterShutdown;

        private File pathToStore;

        /** This value is compared with the result of getNumberOfChangesSinceLastSave(). If the values coincide, no further automatic
            saving is performed until the value changes again.*/
        private int changeState;

        doAutomaticSave(MindMapMapModel model, int numberOfTempFiles, boolean filesShouldBeDeletedAfterShutdown, File pathToStore) {
            this.model = model;
            tempFileStack = new Vector();
            numberOfFiles = ((numberOfTempFiles > 0) ? numberOfTempFiles : 1);
            this.filesShouldBeDeletedAfterShutdown = filesShouldBeDeletedAfterShutdown;
            this.pathToStore = pathToStore;
            changeState = 0;
        }

        public void run() {
            if (model.getNumberOfChangesSinceLastSave() == changeState) return;
            changeState = model.getNumberOfChangesSinceLastSave();
            if (model.getNumberOfChangesSinceLastSave() == 0) {
                return;
            }
            File tempFile;
            if (tempFileStack.size() >= numberOfFiles) tempFile = (File) tempFileStack.remove(0); else {
                try {
                    tempFile = File.createTempFile("VMAP_" + ((model.toString() == null) ? "unnamed" : model.toString()), ".xml", pathToStore);
                    if (filesShouldBeDeletedAfterShutdown) tempFile.deleteOnExit();
                } catch (Exception e) {
                    System.err.println("Error in automatic MindMapMapModel.save(): " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
            try {
                model.saveInternal(tempFile, true, false);
                model.getFrame().out("Map was automatically saved (using the file name " + tempFile + ") ...");
            } catch (Exception e) {
                System.err.println("Error in automatic MindMapMapModel.save(): " + e.getMessage());
                e.printStackTrace();
            }
            tempFileStack.add(tempFile);
        }
    }
}
