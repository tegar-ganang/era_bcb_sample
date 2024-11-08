package org.xaware.ide.xadev.gui.actions;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Vector;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.jdom.Attribute;
import org.jdom.Element;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.datamodel.JDOMContent;
import org.xaware.ide.xadev.datamodel.JavaClassTreeNode;
import org.xaware.ide.xadev.datamodel.MapTreeNode;
import org.xaware.ide.xadev.datamodel.StoredProcedureParameter;
import org.xaware.ide.xadev.datamodel.XATreeNode;
import org.xaware.ide.xadev.datamodel.XMLTreeNode;
import org.xaware.ide.xadev.gui.DNDListHandler;
import org.xaware.ide.xadev.gui.DNDTableHandler;
import org.xaware.ide.xadev.gui.DNDTreeHandler;
import org.xaware.ide.xadev.gui.FunctoidPanel;
import org.xaware.ide.xadev.gui.MapDNDTreeHandler;
import org.xaware.ide.xadev.gui.MapJavaClassDNDTreeHandler;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;

/**
 * This Action class is to apply the selected functoid to the selected item
 * from the popupmenu on the Tree/List.
 *
 * @author GSVSN Murthy
 * @version 1.0
 */
public class FunctoidAction extends GlobalTreeEditAction {

    /** XA_Designer Translator */
    private final Translator translator = XA_Designer_Plugin.getTranslator();

    /** DNDTreeHandler */
    private DNDTreeHandler theTreeHandlar;

    /** DNDTreeHandler */
    private DNDListHandler theListHandlar;

    /** DNDTableHandler */
    private DNDTableHandler theTableHandlar;

    /** List */
    private Table theList;

    /** Table */
    private Table theTable;

    /** Holds mapped data */
    private String mappedData;

    /** Selected Element */
    private Element selectedElement;

    /** Represents no. of child elements of a node */
    private int childElementCount;

    /** Holds current seletcted object index */
    private int selectedIndex;

    /** Holds currently selected list item */
    private Object selListItem;

    /** Selected Node */
    private XATreeNode selNode;

    /** Edit functoid flag */
    private boolean isEditFunctoId = false;

    /**
     * Creates a new FunctoidAction object.
     */
    public FunctoidAction() {
        setText(translator.getString("App&ly &Functoid..."));
        try {
            setImageDescriptor(UserPrefs.getImageDescriptorIconFor(UserPrefs.FUNCTOID));
        } catch (final Exception e) {
            setImageDescriptor(UserPrefs.getImageDescriptorIconFor(UserPrefs.XA_DESIGNER));
        }
        setToolTipText(translator.getString("Get value from a function"));
    }

    /**
     * Creates a new FunctoidAction object.
     * 
     * @param isEdit edit functoid flag.
     */
    public FunctoidAction(boolean isEdit) {
        super();
        isEditFunctoId = isEdit;
        setText(translator.getString("Edit Functoid..."));
        try {
            setImageDescriptor(UserPrefs.getImageDescriptorIconFor(UserPrefs.FUNCTOID));
        } catch (final Exception e) {
            setImageDescriptor(UserPrefs.getImageDescriptorIconFor(UserPrefs.XA_DESIGNER));
        }
        setToolTipText(translator.getString("Edit the existing functoid"));
    }

    /**
     * Triggers the when the action is invoked.
     */
    @Override
    public void run() {
        try {
            if (evaluateMappedDataAndChildElemCount(null) && isActionValid()) {
                String functoIdValue = getFunctoIdValue();
                if (functoIdValue != null) applyFunctoId(functoIdValue);
            }
        } catch (final Throwable ex) {
            ex.printStackTrace();
            ControlFactory.showInfoDialog(translator.getString("Error executing functoid action. " + ex.getMessage()), ex.toString());
        } finally {
            childElementCount = 0;
            selectedIndex = -1;
            selListItem = null;
            selNode = null;
        }
    }

    /**
     * Verifies if the applicability of the action.
     * @return boolean indicating if the action can be applied or not.
     */
    public boolean isActionValid() {
        if (isEditFunctoId) return true;
        if (mappedData.startsWith("$java:") || mappedData.startsWith("$xaware:")) {
            int confirm = ControlFactory.showConfirmDialog(translator.getString("Already functoid existing on selected node, applying new functoid will overwrite existing functoid. Do you want to continue?"));
            if (confirm == Window.OK) return true;
            return false;
        }
        if (childElementCount > 0) {
            ControlFactory.showMessageDialog(translator.getString("Functoids are not allowed on elements that contain child elements."), translator.getString("Information"));
            return false;
        }
        return true;
    }

    /**
     * Gets the input parameters and sets to the inputParamVector
     *
     * @return inputParamVector
     */
    private Vector getInputParams() {
        final Vector<String> inputParamVector = new Vector<String>();
        final XMLTreeNode rootNode = (XMLTreeNode) theTreeHandlar.getRoot();
        final Element rootElem = (Element) ((Element) rootNode.getJDOMContent().getContent()).clone();
        final Element inputElem = rootElem.getChild("input", XAwareConstants.xaNamespace);
        if (inputElem != null) {
            final java.util.List paramElements = inputElem.getChildren("param", XAwareConstants.xaNamespace);
            if (paramElements != null) {
                final Iterator iter = paramElements.iterator();
                while (iter.hasNext()) {
                    final Element paramElem = (Element) iter.next();
                    final Attribute paramAttr = paramElem.getAttribute("name", XAwareConstants.xaNamespace);
                    if (paramAttr != null) {
                        final String paramName = paramAttr.getValue().trim();
                        if (!paramName.equals("")) {
                            inputParamVector.add(paramName);
                        }
                    }
                }
            }
        }
        return inputParamVector;
    }

    /**
     * This method evaluates mapped data for the selected element and 
     * no. of child elements for that element. 
     * @param selectedText String value to use in place of the mappedData
     * 					if the call is from a textField's popup Menu. 
     *
     */
    public boolean evaluateMappedDataAndChildElemCount(String selectedText) {
        if (getTable() != null) {
            theTableHandlar = getTable();
            theTable = getTable().getTableViewer().getTable();
        } else if (getList() != null) {
            theListHandlar = getList();
            theList = getList().getListViewer().getTable();
        } else if (getTree() != null) {
            theTreeHandlar = getTree();
        }
        if ((theTable != null) && theTableHandlar instanceof DNDTableHandler) {
            selectedIndex = theTable.getSelectionIndex();
            selListItem = theTableHandlar.getModel().elementAt(selectedIndex);
            if (selListItem instanceof Element) {
                selectedElement = (Element) selListItem;
                mappedData = (selectedText != null) ? selectedText : ((Element) selListItem).getText().trim();
            } else if (selListItem instanceof StoredProcedureParameter) {
                mappedData = (selectedText != null) ? selectedText : ((StoredProcedureParameter) selListItem).getMappedValue().trim();
            }
            theTableHandlar.setOldMappedValue(selListItem, mappedData);
        } else if ((theList != null) && theListHandlar instanceof DNDListHandler) {
            selectedIndex = theList.getSelectionIndex();
            selListItem = theListHandlar.getModel().elementAt(selectedIndex);
            if (selListItem instanceof Element) {
                selectedElement = (Element) selListItem;
                mappedData = (selectedText != null) ? selectedText : ((Element) selListItem).getText().trim();
            } else if (selListItem instanceof StoredProcedureParameter) {
                mappedData = (selectedText != null) ? selectedText : ((StoredProcedureParameter) selListItem).getMappedValue().trim();
            }
            theListHandlar.setOldMappedValue(selListItem, mappedData);
        } else if (theTreeHandlar instanceof MapJavaClassDNDTreeHandler) {
            selNode = theTreeHandlar.getSelectedNode();
            selectedElement = ((JavaClassTreeNode) selNode).myElement;
            mappedData = (selectedText != null) ? selectedText : ((((JavaClassTreeNode) selNode).myElement)).getText().trim();
            childElementCount = ((((JavaClassTreeNode) selNode).myElement)).getChildren().size();
        } else if (theTreeHandlar instanceof MapDNDTreeHandler) {
            selNode = theTreeHandlar.getSelectedNode();
            if (((MapTreeNode) selNode).myContent instanceof Element) {
                selectedElement = (Element) ((MapTreeNode) selNode).myContent;
                mappedData = (selectedText != null) ? selectedText : ((Element) (((MapTreeNode) selNode).myContent)).getText().trim();
                childElementCount = ((Element) (((MapTreeNode) selNode).myContent)).getChildren().size();
            } else if (((MapTreeNode) selNode).myContent instanceof Attribute) {
                selectedElement = (Element) ((MapTreeNode) ((MapTreeNode) selNode).getParent()).myContent;
                mappedData = (selectedText != null) ? selectedText : ((Attribute) ((MapTreeNode) selNode).myContent).getValue();
            }
        } else if (theTreeHandlar instanceof DNDTreeHandler) {
            if (theTreeHandlar.getSelectedNode() == null) {
                ControlFactory.showMessageDialog(translator.getString("Please select a tree node on which you want to apply functoid."), translator.getString("Information"));
                return false;
            }
            selNode = theTreeHandlar.getSelectedNode();
            if (((XMLTreeNode) selNode).getJDOMContent().getType() == JDOMContent.ELEMENT) {
                selectedElement = (Element) ((XMLTreeNode) selNode).getJDOMContent().getContent();
                mappedData = (selectedText != null) ? selectedText : ((Element) ((XMLTreeNode) selNode).getJDOMContent().getContent()).getText().trim();
                childElementCount = ((Element) ((XMLTreeNode) selNode).getJDOMContent().getContent()).getChildren().size();
            } else if (((XMLTreeNode) selNode).getJDOMContent().getType() == JDOMContent.ATTRIBUTE) {
                selectedElement = (Element) ((XMLTreeNode) ((XMLTreeNode) selNode).getParent()).getJDOMContent().getContent();
                mappedData = (selectedText != null) ? selectedText : ((Attribute) ((XMLTreeNode) selNode).getJDOMContent().getContent()).getValue().trim();
            }
        }
        return true;
    }

    /**
     * This method constructs the functoid dialog and opens it. If the user constructs the 
     * functoid (by clicking the OK button in the functoid dialog) then it returns  functoid
     * as a string otherwise returns null.
     * 
     * @return FunctoId value 
     * @throws XAwareException if functoid string is invalid.
     * @throws UnsupportedEncodingException if error occurs while decoding functoid params.
     */
    public String getFunctoIdValue() throws XAwareException, UnsupportedEncodingException {
        String functoIdValue = null;
        if (theTableHandlar instanceof DNDTableHandler) {
            final FunctoidPanel pan = new FunctoidPanel(Display.getCurrent().getActiveShell(), mappedData, (theTableHandlar).getInputParams(), new Element("SelectInputXML"), selectedElement, isEditFunctoId);
            if (pan.showDialog()) functoIdValue = pan.getFunctoidString();
        } else if (theListHandlar instanceof DNDListHandler) {
            final FunctoidPanel pan = new FunctoidPanel(Display.getCurrent().getActiveShell(), mappedData, (theListHandlar).getInputParams(), new Element("SelectInputXML"), selectedElement, isEditFunctoId);
            if (pan.showDialog()) functoIdValue = pan.getFunctoidString();
        } else if (theTreeHandlar instanceof MapJavaClassDNDTreeHandler) {
            final FunctoidPanel pan = new FunctoidPanel(Display.getCurrent().getActiveShell(), mappedData, ((MapJavaClassDNDTreeHandler) theTreeHandlar).getInputParams(), ((MapJavaClassDNDTreeHandler) theTreeHandlar).getInputXML(), selectedElement, isEditFunctoId);
            if (pan.showDialog()) functoIdValue = pan.getFunctoidString();
        } else if (theTreeHandlar instanceof MapDNDTreeHandler) {
            final FunctoidPanel pan = new FunctoidPanel(Display.getCurrent().getActiveShell(), mappedData, ((MapDNDTreeHandler) theTreeHandlar).getInputParams(), ((MapDNDTreeHandler) theTreeHandlar).getInputXML(), selectedElement, isEditFunctoId);
            final Vector tableCols = ((MapDNDTreeHandler) theTreeHandlar).getTableColumns();
            if (tableCols != null) {
                pan.setTableColumns(tableCols);
            }
            if (pan.showDialog()) functoIdValue = pan.getFunctoidString();
        } else if (theTreeHandlar instanceof DNDTreeHandler) {
            final Element elem = (Element) ((XMLTreeNode) theTreeHandlar.getRoot()).getJDOMContent().getContent();
            final Vector inputParamVector = getInputParams();
            final FunctoidPanel pan = new FunctoidPanel(Display.getCurrent().getActiveShell(), mappedData, inputParamVector, elem, selectedElement, isEditFunctoId);
            if (pan.showDialog()) functoIdValue = pan.getFunctoidString();
        }
        return functoIdValue;
    }

    /**
     * This method applies the functoid value to the selected node.
     * 
     * @param functoidValue
     */
    @SuppressWarnings("unchecked")
    public void applyFunctoId(String functoidValue) {
        if (theTableHandlar instanceof DNDTableHandler) {
            theTableHandlar.getModel().removeElementAt(selectedIndex);
            if (selListItem instanceof Element) {
                ((Element) selListItem).setText(functoidValue);
            } else if (selListItem instanceof StoredProcedureParameter) {
                ((StoredProcedureParameter) selListItem).setMappedValue(functoidValue);
            }
            theTableHandlar.getModel().insertElementAt(selListItem, selectedIndex);
            theTable.setSelection(selectedIndex);
            theTableHandlar.setLastDroppedOn(selListItem);
            theTableHandlar.getTableViewer().refresh();
            theTable.deselectAll();
        } else if (theListHandlar instanceof DNDListHandler) {
            theListHandlar.getModel().removeElementAt(selectedIndex);
            if (selListItem instanceof Element) {
                ((Element) selListItem).setText(functoidValue);
            } else if (selListItem instanceof StoredProcedureParameter) {
                ((StoredProcedureParameter) selListItem).setMappedValue(functoidValue);
            }
            theListHandlar.getModel().insertElementAt(selListItem, selectedIndex);
            theList.setSelection(selectedIndex);
            theListHandlar.setLastDroppedOn(selListItem);
            theListHandlar.getListViewer().refresh();
            theList.deselectAll();
        } else if (theTreeHandlar instanceof MapJavaClassDNDTreeHandler) {
            selNode.setPathValue(functoidValue);
            theTreeHandlar.refreshTree(selNode);
        } else if (theTreeHandlar instanceof MapDNDTreeHandler) {
            if (((MapTreeNode) selNode).myContent instanceof Element || ((MapTreeNode) selNode).myContent instanceof Attribute) {
                ((MapDNDTreeHandler) theTreeHandlar).setPathValue((MapTreeNode) selNode, functoidValue);
                theTreeHandlar.refreshTree(selNode);
                ((MapDNDTreeHandler) theTreeHandlar).lastNodeModified = (MapTreeNode) selNode;
                ((MapDNDTreeHandler) theTreeHandlar).lastTreeAction = MapDNDTreeHandler.TREE_MAPPING;
                if (((MapDNDTreeHandler) theTreeHandlar).lastNodeModified.myContent instanceof Element) {
                    final Attribute attr = ((Element) ((MapDNDTreeHandler) theTreeHandlar).lastNodeModified.myContent).getAttribute(XAwareConstants.BIZCOMPONENT_ATTR_COPY, XAwareConstants.xaNamespace);
                    if (attr != null) {
                        ((MapDNDTreeHandler) theTreeHandlar).lastNodeModified.setOldDataPathValue(attr.getValue());
                    } else {
                        ((MapDNDTreeHandler) theTreeHandlar).lastNodeModified.setOldDataPathValue("");
                    }
                }
            }
        } else if (theTreeHandlar instanceof DNDTreeHandler) {
            final XMLTreeNode oldNode = new XMLTreeNode(((XMLTreeNode) selNode).getJDOMContent().copyOf());
            final XMLTreeNode parent = (XMLTreeNode) selNode.getParent();
            int index = -1;
            if (parent != null) {
                index = parent.getIndex(selNode);
            }
            if (((XMLTreeNode) selNode).getJDOMContent().getType() == JDOMContent.ELEMENT) {
                selNode.setPathValue(functoidValue);
                theTreeHandlar.refreshTree(selNode);
            } else if (((XMLTreeNode) selNode).getJDOMContent().getType() == JDOMContent.ATTRIBUTE) {
                selNode.setPathValue(functoidValue);
                theTreeHandlar.refreshTree(selNode);
            }
            final UndoableInfoEdit uie = new UndoableInfoEdit(theTreeHandlar, UndoableInfoEdit.TYPE_TEXT_EDIT, parent, index, (XMLTreeNode) selNode, index, oldNode, false);
            uie.addContext(XA_Designer_Plugin.getActiveEditedInternalFrame().getUndoContext());
            XA_Designer_Plugin.getDefault().getWorkbench().getOperationSupport().getOperationHistory().add(uie);
            theTreeHandlar.refreshTree();
        }
    }
}
