package org.mftech.dawn.runtime.client.synchronization.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import org.apache.log4j.Logger;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.parts.DiagramDocumentEditor;
import org.eclipse.gmf.runtime.emf.core.resources.GMFResource;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.dialogs.MessageDialog;
import org.mftech.dawn.runtime.client.diagram.part.DawnEditorHelper;
import org.mftech.dawn.runtime.client.exceptions.EClassIncompatibleException;
import org.mftech.dawn.runtime.client.exceptions.NotIdenticalClassException;
import org.mftech.dawn.runtime.client.exceptions.ServerNotAvailableException;
import org.mftech.dawn.runtime.client.exceptions.UnsupportedViewType;
import org.mftech.dawn.runtime.client.modify.DawnChangeHelper;
import org.mftech.dawn.runtime.client.synchronization.ChangeObject;
import org.mftech.dawn.runtime.client.synchronization.DawnElementTypeHelper;
import org.mftech.dawn.runtime.client.synchronization.ResourceDiffHelper;
import org.mftech.dawn.runtime.client.synchronization.ResourceHelper;
import org.mftech.dawn.runtime.client.synchronization.ResourceSynchronizer;
import org.mftech.dawn.runtime.metadata.LocalSession;
import org.mftech.dawn.server.communication.DawnRemoteConnection;
import org.mftech.dawn.server.constants.DawnReturnCodes;
import org.mftech.dawn.server.projects.User;
import org.mftech.dawn.server.projects.impl.Rights;

/**
 * This classes handles the changes on the clientside
 * 
 * @author killa
 * 
 */
public class ResourceSynchronizerImpl extends Observable implements ResourceSynchronizer {

    private String pluginId = "";

    private static Logger logger = Logger.getLogger(ResourceSynchronizerImpl.class);

    private static final String lastResourceSuffix = ".last";

    private DiagramDocumentEditor editor;

    private Set<EObject> selectedElements;

    private Resource lastLocalResource;

    private Resource lastServerResource;

    private Resource localGMFResource;

    private Long lastUpdateTime;

    private Set<String> ignoreList;

    private Map<String, ChangeObject> deletedLocallyConflicts;

    private Map<String, ChangeObject> deletedRemotelyConflicts;

    private Map<String, ChangeObject> changedLocalyAndRemotellyConflicts;

    private String projectName;

    private Set<String> locallyLocked;

    private Set<String> globallyLocked;

    private ResourceDiffHelper resourceDiffHelper;

    private ResourceSet resourceSet;

    private DawnElementTypeHelper elementTypeHelper;

    /****************************************************************************************************
	 * constructor
	 * 
	 * @param edit
	 * @param projectname2
	 * @param localResource
	 ***************************************************************************************************/
    public ResourceSynchronizerImpl(DiagramDocumentEditor edit, String projectname2, Resource localResource, boolean isServerAvailable, ResourceSet res, DawnElementTypeHelper elementTypeHelper, String pluginId) {
        logger.info("starting resource Sycnhronizer");
        editor = edit;
        projectName = projectname2;
        resourceDiffHelper = new ResourceDiffHelper();
        localGMFResource = localResource;
        initLastResource(isServerAvailable);
        resourceSet = res;
        this.elementTypeHelper = elementTypeHelper;
        this.pluginId = pluginId;
    }

    /**********************************************************************************************
	 * 
	 * @param isServerAvailable
	 *********************************************************************************************/
    private void initLastResource(boolean isServerAvailable) {
        if (!isServerAvailable) {
            loadLastResource();
        }
    }

    /****************************************************************************************************
	 * 
	 ***************************************************************************************************/
    @Override
    public Set<String> getLocallyLocked() {
        if (locallyLocked == null) {
            locallyLocked = new HashSet<String>();
        }
        return locallyLocked;
    }

    /****************************************************************************************************
	 * sets the locally locked elements
	 * 
	 * @param locallyLocked
	 ***************************************************************************************************/
    @Override
    public void setLocallyLocked(Set<String> locallyLocked) {
        this.locallyLocked = locallyLocked;
    }

    /****************************************************************************************************
	 * 
	 ***************************************************************************************************/
    @Override
    public Map<String, ChangeObject> getChangedLocalyAndRemotellyConflicts() {
        if (changedLocalyAndRemotellyConflicts == null) {
            changedLocalyAndRemotellyConflicts = new HashMap<String, ChangeObject>();
        }
        return changedLocalyAndRemotellyConflicts;
    }

    /****************************************************************************************************
	 * 
	 * @param changedLocalyAndRemotellyConflicts
	 ***************************************************************************************************/
    @Override
    public synchronized void setChangedLocalyAndRemotellyConflicts(Map<String, ChangeObject> changedLocalyAndRemotellyConflicts) {
        this.changedLocalyAndRemotellyConflicts = changedLocalyAndRemotellyConflicts;
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public Map<String, ChangeObject> getDeletedRemotelyConflicts() {
        if (deletedRemotelyConflicts == null) {
            deletedRemotelyConflicts = new HashMap<String, ChangeObject>();
        }
        return deletedRemotelyConflicts;
    }

    /**********************************************************************************************
 * 
 *********************************************************************************************/
    @Override
    public Set<String> getIgnoreList() {
        if (ignoreList == null) {
            ignoreList = new HashSet<String>();
        }
        return ignoreList;
    }

    @Override
    public void setIgnoreList(Set<String> ignoreList) {
        this.ignoreList = ignoreList;
    }

    /****************************************************************************************************
	 * 
	 * @param deletedRemotelyConflicts
	 ***************************************************************************************************/
    public synchronized void setDeletedRemotelyConflicts(Map<String, ChangeObject> deletedRemotelyConflicts) {
        this.deletedRemotelyConflicts = deletedRemotelyConflicts;
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public synchronized Map<String, ChangeObject> getDeletedLocallyConflicts() {
        if (deletedLocallyConflicts == null) {
            deletedLocallyConflicts = new HashMap<String, ChangeObject>();
        }
        return deletedLocallyConflicts;
    }

    /****************************************************************************************************
	 * 
	 * @param delectConflicts
	 ***************************************************************************************************/
    @Override
    public synchronized void setDeletedLocallyConflicts(Map<String, ChangeObject> delectConflicts) {
        this.deletedLocallyConflicts = delectConflicts;
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public synchronized Set<EObject> getSelectedElements() {
        if (selectedElements == null) {
            selectedElements = new HashSet<EObject>();
        }
        return selectedElements;
    }

    /****************************************************************************************************
	 * 
	 * @param selectedElements
	 ***************************************************************************************************/
    @Override
    public synchronized void setSelectedElements(Set<EObject> selectedElements) {
        this.selectedElements = selectedElements;
    }

    /****************************************************************************************************
	 * sends the local Resources to the server. The Server will merge the
	 * resources with the current resource on the server
	 * 
	 * @param localResource
	 * @return true if conflicted, false otherwise
	 ***************************************************************************************************/
    @Override
    public synchronized boolean publish(final GMFResource localResource, DawnRemoteConnection dawnRemoteConnection) {
        try {
            if (isConflicted(localResource, dawnRemoteConnection)) {
                return true;
            }
            String projectName = localResource.getURI().lastSegment();
            logger.info("Project Name: " + projectName);
            XMLResource changeAndInsertResource = (XMLResource) ResourceHelper.createEmptyResource(resourceSet, pluginId);
            for (EObject view : resourceDiffHelper.getNewObjects()) {
                ResourceHelper.addViewToResource((XMLResource) changeAndInsertResource, (View) view);
                logger.info("new Object :" + view);
            }
            for (EObject view : resourceDiffHelper.getChangedObjects().values()) {
                ResourceHelper.addViewToResource((XMLResource) changeAndInsertResource, (View) view);
                logger.info("chnaged Object :" + view);
            }
            String gmfResouceString = ResourceHelper.saveToXML(changeAndInsertResource);
            logger.info("SENDING RESOURCES");
            logger.info(gmfResouceString);
            Set<String> deleted = new HashSet<String>();
            for (EObject e : resourceDiffHelper.getDeletedObjects()) {
                deleted.add(ResourceHelper.getXmiId(e));
            }
            dawnRemoteConnection.publish(gmfResouceString, deleted, projectName);
            copyLocalResourceToLastResource(localResource);
            cleanConflictLists();
            cleanIgnoreList();
        } catch (RemoteException e) {
            logger.error(e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServerNotAvailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /****************************************************************************************************
	 * cleans the conflicts list an removes all conflicted marker
	 ***************************************************************************************************/
    private void cleanConflictLists() {
        this.getDeletedLocallyConflicts().clear();
        this.getDeletedRemotelyConflicts().clear();
        this.getChangedLocalyAndRemotellyConflicts().clear();
    }

    /****************************************************************************************************
	 * cleans the ignore list
	 ***************************************************************************************************/
    @Override
    public void cleanIgnoreList() {
        getIgnoreList().clear();
    }

    /****************************************************************************************************
	 * This method synchronizes the resources. It also detects conflicts and
	 * makrs the conflictes objekts � Erstelle DIff zum Alten Bestand �
	 * Identifiziere, new, changed und deleted � Hole Daten vom Server � Erkenne
	 * Konflikte � Behandle Konflikte � Merge � Sende Daten an den Server (alle
	 * �ndeurngen) � Speicher Bestand in alten Bestand
	 * 
	 * 
	 * @param resource
	 * @return
	 * @throws Exception
	 * @throws IOException
	 ***************************************************************************************************/
    @Override
    public boolean update(Resource localResouce, Resource serverResource, boolean publish, DawnRemoteConnection dawnRemoteConnection) throws Exception {
        try {
            logger.info(ResourceHelper.saveToXML((XMLResource) serverResource));
            if (lastLocalResource == null) {
                copyLocalResourceToLastResource(localResouce);
            }
            boolean conflicted = identifyConflictsAndChanges(localResouce, (XMIResource) serverResource);
            setSelectedElements();
            synchronizeDiagramXMI(localResouce, serverResource);
            ResourceDiffHelper r = new ResourceDiffHelper();
            r.doDiff(localResouce, serverResource);
            addElements(localResouce, r);
            changeElements(r);
            deleteElements(r);
            lastServerResource = serverResource;
            if (publish) {
                logger.info("PUBLISCHING AFTER UPDATING!!!!");
                this.publish((GMFResource) localResouce, dawnRemoteConnection);
            }
            if (conflicted) {
                return true;
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        return false;
    }

    /****************************************************************************************************
	 * checks for changes from the last update to the current update to identify
	 * the following objects
	 * 
	 * @param localResouce
	 * @param remoteResource
	 * @throws Exception
	 ***************************************************************************************************/
    private synchronized boolean identifyConflictsAndChanges(Resource localResouce, Resource serverResource) throws Exception {
        logger.debug("identifyConflicts started");
        boolean conflicted = false;
        if (lastLocalResource != null) {
            Map<EObject, EObject> changedSincedLastPublish = new HashMap<EObject, EObject>();
            Set<EObject> deletedSincedLastPublish = new HashSet<EObject>();
            Set<EObject> insertedSincedLastPublish = new HashSet<EObject>();
            logger.debug("lastResource is not null, so we will check for conflicts");
            resourceDiffHelper = new ResourceDiffHelper();
            resourceDiffHelper.doDiff(lastLocalResource, localResouce);
            for (EObject lastObject : resourceDiffHelper.getNewObjects()) {
                insertedSincedLastPublish.add(lastObject);
            }
            for (EObject lastObject : resourceDiffHelper.getChangedObjects().keySet()) {
                EObject localObject = localResouce.getEObject(ResourceHelper.getXmiId(lastObject));
                {
                    changedSincedLastPublish.put(localObject, lastObject);
                }
            }
            for (EObject lastObject : resourceDiffHelper.getDeletedObjects()) {
                deletedSincedLastPublish.add(lastObject);
            }
            for (EObject lastObject : insertedSincedLastPublish) {
                logger.info("locally INSERTED since last update: " + lastObject);
                setIgnored(lastObject);
            }
            for (EObject localObject : changedSincedLastPublish.keySet()) {
                EObject lastObject = changedSincedLastPublish.get(localObject);
                setIgnored(lastObject);
                logger.info("locally CHANGED  since last update " + lastObject + " to " + localObject);
                EObject serverObject = serverResource.getEObject(ResourceHelper.getXmiId(lastObject));
                if (!isConflicted(localObject) && !isLocked(localObject)) {
                    logger.info("Locally changed object in server Resource is: " + serverObject);
                    EditPart editPart = findEditPart((View) localObject);
                    if (serverObject == null) {
                        logger.info("Object which was locally changed had been deleted by another Client");
                        DawnChangeHelper.markObjectRemoteDeleted(editPart);
                        ChangeObject changeObject = new ChangeObject(localObject, serverObject, lastObject, "Object remotelly deleted but locally changed");
                        setConflicted(ResourceHelper.getXmiId(lastObject), changeObject, this.REMOTELY_DELTETION_CONFLICT);
                        setIgnored(lastObject);
                        conflicted = true;
                    } else {
                        if (ResourceHelper.objectsHaveChanged(lastObject, serverObject) && ResourceHelper.objectsHaveChanged(localObject, serverObject)) {
                            logger.info("Object which was locally changed has also been changed by another Client");
                            DawnChangeHelper.markObjectRemotelyChanged(editPart);
                            ChangeObject changeObject = new ChangeObject(localObject, serverObject, lastObject, "Object remotelly and locally changed");
                            setConflicted(ResourceHelper.getXmiId(lastObject), changeObject, this.REMOTELY_AND_LOCALLY_CHANGED_CONFLICT);
                            setIgnored(lastObject);
                            conflicted = true;
                        } else {
                            this.getChangedLocalyAndRemotellyConflicts().remove(ResourceHelper.getXmiId(lastObject));
                        }
                    }
                } else {
                    logger.info("Object is still conflicted: " + ResourceHelper.getXmiId(localObject));
                }
            }
            for (EObject lastObject : deletedSincedLastPublish) {
                logger.info("locally DELETED since last update: " + lastObject);
                EObject serverObject = serverResource.getEObject(ResourceHelper.getXmiId(lastObject));
                if (!isConflicted(lastObject)) {
                    if (ResourceHelper.objectsHaveChanged(lastObject, serverObject)) {
                        ChangeObject changeObject = new ChangeObject(null, serverObject, lastObject, "Object remotelly changed but locally deleted");
                        setConflicted(ResourceHelper.getXmiId(lastObject), changeObject, LOCALLY_DELTETION_CONFLICT);
                        logger.info("Size of locally deleted Elements: " + this.getDeletedLocallyConflicts().size());
                        conflicted = true;
                    }
                    setIgnored(lastObject);
                }
            }
        }
        if (conflicted) {
            setChanged();
            notifyObservers();
        }
        logger.debug("identifyConflicts finished with " + conflicted);
        return conflicted;
    }

    /**********************************************************************************************
	 * 
	 * @param localObject
	 * @return
	 *********************************************************************************************/
    private boolean isLocked(EObject localObject) {
        return getGloballyLocked().contains(ResourceHelper.getXmiId(localObject));
    }

    /**********************************************************************************************
	 * sets the xmiid conflicted specified by the conflictType
	 * 
	 * @param xmiId
	 * @param changeObject
	 * @param conflictType
	 *********************************************************************************************/
    private void setConflicted(String xmiId, ChangeObject changeObject, int conflictType) {
        switch(conflictType) {
            case REMOTELY_DELTETION_CONFLICT:
                this.getDeletedRemotelyConflicts().put(xmiId, changeObject);
                break;
            case REMOTELY_AND_LOCALLY_CHANGED_CONFLICT:
                this.getChangedLocalyAndRemotellyConflicts().put(xmiId, changeObject);
                break;
            case LOCALLY_DELTETION_CONFLICT:
                this.getDeletedLocallyConflicts().put(xmiId, changeObject);
                break;
            default:
                ;
        }
    }

    /****************************************************************************************************
	 * copies the localReosurce to the last Resource
	 * 
	 * @param localResouce
	 ***************************************************************************************************/
    private void copyLocalResourceToLastResource(Resource localResouce) {
        try {
            String localResource = ResourceHelper.saveToXML((XMLResource) localResouce);
            this.lastLocalResource = ResourceHelper.loadFromXML(localResource, resourceSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /****************************************************************************************************
	 * handles deletion of Objects
	 * 
	 * @param r
	 ***************************************************************************************************/
    private void deleteElements(ResourceDiffHelper r) {
        for (EObject obj : r.getDeletedObjects()) {
            if (!isConflicted(obj) && !isSelected(obj) && !isIgnored(obj)) {
                logger.info("Deleted Object " + obj);
                DawnChangeHelper.deleteView((View) obj, this.editor);
            }
        }
    }

    /****************************************************************************************************
	 * 
	 * @param o
	 * @return
	 ***************************************************************************************************/
    private boolean isIgnored(EObject o) {
        return isIgnored((ResourceHelper.getXmiId(o)));
    }

    /****************************************************************************************************
	 * 
	 * @param id
	 * @return
	 ***************************************************************************************************/
    private boolean isIgnored(String id) {
        return this.getIgnoreList().contains(id);
    }

    /****************************************************************************************************
	 * checks whether the object is conflited currently only checks for
	 * deletedConflicts TODO add conflict detection for changedObjects, too.
	 ***************************************************************************************************/
    private boolean isConflicted(EObject obj) {
        String xmiId = ResourceHelper.getXmiId(obj);
        if (getDeletedLocallyConflicts().containsKey(xmiId)) return true;
        if (getDeletedRemotelyConflicts().containsKey(xmiId)) return true;
        return false;
    }

    /****************************************************************************************************
	 * chekcs if the resource GMFResource is in conflict with local and last
	 * resource
	 * 
	 * @param localResource
	 * @return
	 * @throws IOException
	 * @throws Exception
	 ***************************************************************************************************/
    @Override
    public synchronized boolean isConflicted(final GMFResource localResource, DawnRemoteConnection dawnRemoteConnection) throws IOException, Exception {
        String xml = dawnRemoteConnection.update(projectName);
        logger.info("Server send: " + xml);
        if (xml != null) {
            Resource remoteResource = ResourceHelper.loadFromXML(xml, resourceSet);
            boolean conflicted = identifyConflictsAndChanges(localResource, (XMIResource) remoteResource);
            logger.info("Conflict statuc: " + conflicted);
            return conflicted;
        } else {
            return true;
        }
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public boolean isConflicted() {
        if (this.getDeletedLocallyConflicts().size() > 0) return true;
        if (this.getDeletedRemotelyConflicts().size() > 0) return true;
        if (this.getChangedLocalyAndRemotellyConflicts().size() > 0) return true;
        return false;
    }

    /****************************************************************************************************
	 * handles changes of Objects does not change the object if it is: selected,
	 * ignored oder locally changed. Otherwise the server object will overwrite
	 * the local object
	 * 
	 * @param root
	 * @param r
	 ***************************************************************************************************/
    private void changeElements(ResourceDiffHelper r) {
        for (EObject oldObj : r.getChangedObjects().keySet()) {
            logger.info("Change Object " + oldObj + " to " + r.getChangedObjects().get(oldObj));
            logger.info("isSelected:" + isSelected(oldObj) + " / isIgnored :" + isIgnored(oldObj));
            if (!isSelected(oldObj) && !isIgnored(oldObj)) {
                View serverView = (View) r.getChangedObjects().get(oldObj);
                unlockEditPartIfNeeded((View) oldObj);
                if (oldObj instanceof Node) {
                    Node serverNode = (Node) r.getChangedObjects().get(oldObj);
                    updateNode((Node) oldObj, serverNode);
                } else if (oldObj instanceof Edge) {
                    Edge oldEdge = (Edge) r.getChangedObjects().get(oldObj);
                    updateEdge(oldObj, oldEdge);
                } else {
                    logger.warn("Unsupported Type: " + oldObj.getClass());
                }
                lastResourceChangeView(serverView);
                lockEditPartIfNeeded((View) oldObj);
            }
        }
    }

    /****************************************************************************************************
	 * 
	 * @param newView
	 ***************************************************************************************************/
    @Override
    public void lastResourceChangeView(View newView) {
        EObject lastObject = ResourceHelper.getSameEObjectFromOtherResource(newView, (XMLResource) lastLocalResource);
        ResourceHelper.changeViewInResource((View) lastObject, newView);
    }

    /****************************************************************************************************
	 * 
	 * @param newView
	 ***************************************************************************************************/
    @Override
    public void lastResourceDeleteView(View newView) {
        try {
            logger.info(ResourceHelper.saveToXML((XMLResource) newView.eResource()));
            logger.info(ResourceHelper.saveToXML((XMLResource) lastLocalResource));
        } catch (IOException e) {
            e.printStackTrace();
        }
        EObject lastObject = ResourceHelper.getSameEObjectFromOtherResource(newView, (XMLResource) lastLocalResource);
        ResourceHelper.deleteViewInResource((XMLResource) lastLocalResource, lastObject);
    }

    /****************************************************************************************************
	 * 
	 * @param obj
	 ***************************************************************************************************/
    private void lockEditPartIfNeeded(View obj) {
        if (getGloballyLocked().contains(ResourceHelper.getXmiId(obj))) {
            EditPart e = findEditPart(obj);
            DawnChangeHelper.deactivateEditPart(e);
        }
    }

    /****************************************************************************************************
	 * 
	 * @param obj
	 ***************************************************************************************************/
    private void unlockEditPartIfNeeded(View obj) {
        if (getGloballyLocked().contains(ResourceHelper.getXmiId(obj))) {
            EditPart e = findEditPart(obj);
            DawnChangeHelper.activateEditPart(e);
        }
    }

    /****************************************************************************************************
	 * 
	 * @param root
	 * @param obj
	 * @param oldEdge
	 ***************************************************************************************************/
    private void updateEdge(EObject obj, Edge oldEdge) {
        EditPart root = this.editor.getDiagramEditPart();
        EditPart e = findEditPart((View) obj);
        logger.info("Edge to be chnaged: " + obj);
        DawnChangeHelper.setAnchorsAndBendPoints(e, oldEdge, (DiagramEditPart) root);
        ResourceHelper.copyXmiIds(oldEdge, obj);
    }

    /****************************************************************************************************
	 * this updates the View of obj with the values from serverNode
	 * 
	 * @param view
	 * @param serverNode
	 ***************************************************************************************************/
    private void updateNode(Node view, Node serverNode) {
        int x = ((Bounds) serverNode.getLayoutConstraint()).getX();
        int y = ((Bounds) serverNode.getLayoutConstraint()).getY();
        int w = ((Bounds) serverNode.getLayoutConstraint()).getWidth();
        int h = ((Bounds) serverNode.getLayoutConstraint()).getHeight();
        EditPart editPart = findEditPart((View) view);
        if (editPart != null) {
            DawnChangeHelper.resizeEditPart(editPart, new Dimension(w, h));
            DawnChangeHelper.moveEditPart(editPart, new Point(x, y));
            updateChildren(editPart, serverNode);
            DawnChangeHelper.updateModel(editPart, serverNode.getElement(), editor);
            removeUnusedChildren(editPart, (View) serverNode);
        }
    }

    private void removeUnusedChildren(EditPart editPart, View rightView) {
        if (rightView == null) {
            View rigthChildView = (View) editPart.getModel();
            DawnChangeHelper.deleteView(rigthChildView, editor);
        } else {
            ArrayList toBeRemoved = new ArrayList();
            for (final Object leftObject : ((EObject) editPart.getModel()).eContents()) {
                toBeRemoved.add(leftObject);
            }
            for (Object leftObject : toBeRemoved) {
                if (leftObject instanceof View) {
                    EObject rightChild = ResourceHelper.getSameEObjectFromOtherResource((EObject) leftObject, (XMLResource) rightView.eResource());
                    EditPart leftChildEditPart = ResourceHelper.findEditPart((View) leftObject, editor.getDiagramEditPart().getViewer());
                    removeUnusedChildren(leftChildEditPart, (View) rightChild);
                }
            }
        }
    }

    /**********************************************************************************************
	 * 
	 * @param editPart
	 * @param serverNode
	 *********************************************************************************************/
    private void updateChildren(EditPart editPart, Node v) {
        for (final Object o : v.getChildren()) {
            updateViewInParent(editPart, (View) o);
        }
    }

    private void updateViewInParent(EditPart editPart, EObject rightObject) {
        if (rightObject != null) {
            if (rightObject instanceof View) {
                View rightView = (View) rightObject;
                View leftView = (View) ResourceHelper.getSameEObjectFromOtherResource((EObject) rightView, (XMLResource) ((EObject) editPart.getModel()).eResource());
                if (leftView == null) {
                    addViewToParent(editPart, rightView);
                } else {
                    EditPart childeEditPart = ResourceHelper.findEditPart(leftView, editor.getDiagramEditPart().getViewer());
                    EObject rightElement = rightView.getElement();
                    DawnChangeHelper.updateModel(childeEditPart, rightElement, editor);
                }
            }
            for (Object o : rightObject.eContents()) {
                updateViewInParent(editPart, (EObject) o);
            }
        }
    }

    /**********************************************************************************************
	 * 
	 * @param editPart
	 * @param v
	 *********************************************************************************************/
    private void addChildren(EditPart editPart, Node v) {
        for (Object o : v.getChildren()) {
            addViewToParent(editPart, o);
        }
    }

    @Deprecated
    private void addChild(EditPart editPart, Object o) {
        logger.info("OBJECT" + o + " " + ((View) o).eContents());
        for (Object k : ((EObject) o).eContents()) {
            logger.info("OBJECT   --" + k);
            addViewToParent(editPart, k);
        }
    }

    /**********************************************************************************************
	 * 
	 * @param editPart
	 * @param rightObject
	 *********************************************************************************************/
    private void addViewToParent(EditPart editPart, Object rightObject) {
        if (rightObject instanceof View) {
            logger.info("Creating Child " + ((View) rightObject).getElement() + " " + ((View) rightObject).getType());
            IElementType elementTypeAttribnute = elementTypeHelper.getElementType((View) rightObject);
            if (elementTypeAttribnute != null) {
                EObject obj = ResourceHelper.getSameEObjectFromOtherResource(((EObject) rightObject), (XMLResource) ((EObject) editPart.getModel()).eResource());
                if (obj == null) {
                    EditPart childEditPart = DawnChangeHelper.createAttribute((Node) rightObject, elementTypeAttribnute, (IGraphicalEditPart) editPart);
                    logger.info("Created new editart" + childEditPart);
                    if (childEditPart != null) {
                        ResourceHelper.copyXmiIds((View) rightObject, (EObject) childEditPart.getModel());
                        try {
                            DawnChangeHelper.updateModel(childEditPart, ((View) rightObject).getElement(), editor);
                            ResourceHelper.copyXmiIds(((View) rightObject).getElement(), ((View) childEditPart.getModel()).getElement());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        logger.info("Could not create requested Editpart : " + elementTypeAttribnute);
                    }
                } else {
                    logger.warn("Object with id '" + ResourceHelper.getXmiId((EObject) rightObject) + "' was still created");
                }
            }
        }
        for (Object child : ((EObject) rightObject).eContents()) {
            logger.info("OBJECT   --" + rightObject);
            addViewToParent(editPart, child);
        }
    }

    /**********************************************************************************************
	 * update the localiew with the information of the globalView
	 * 
	 * @param localView
	 * @param globalView
	 * @throws NotIdenticalClassException
	 * @throws UnsupportedViewType
	 *********************************************************************************************/
    private void updateView(View localView, View globalView) throws NotIdenticalClassException, UnsupportedViewType {
        if (!localView.eClass().equals(globalView.eClass())) {
            throw new NotIdenticalClassException();
        }
        if (localView instanceof Node) {
            updateNode((Node) localView, (Node) globalView);
        } else if (localView instanceof Edge) {
            updateEdge(localView, (Edge) globalView);
        } else {
            throw new UnsupportedViewType("The view-type '" + localView.getClass() + "' is not supported");
        }
    }

    /****************************************************************************************************
	 * updates obj with the information from the serverNode
	 * 
	 * @param obj
	 ***************************************************************************************************/
    @Override
    public void updateViewWithRemoteView(View obj) {
        EObject serverObject = null;
        for (String key : getChangedLocalyAndRemotellyConflicts().keySet()) {
            ChangeObject changeObject = getChangedLocalyAndRemotellyConflicts().get(key);
            if (ResourceHelper.areSameObjects(changeObject.getLastObject(), obj)) {
                serverObject = changeObject.getServerObject();
                if (serverObject instanceof Node) {
                    this.updateNode((Node) obj, (Node) serverObject);
                } else if (serverObject instanceof Edge) {
                    this.updateEdge(obj, (Edge) serverObject);
                }
                break;
            }
        }
        EObject lastObject = ResourceHelper.getSameEObjectFromOtherResource((EObject) obj, (XMLResource) lastLocalResource);
        ResourceHelper.changeViewInResource((View) lastObject, (View) serverObject);
    }

    /****************************************************************************************************
	 * handles adding new objects
	 * 
	 * @param localResouce
	 * @param root
	 * @param r
	 ***************************************************************************************************/
    private void addElements(Resource localResouce, ResourceDiffHelper r) {
        EditPart root = this.editor.getDiagramEditPart();
        for (EObject obj : r.getNewObjects()) {
            if (!isIgnored(obj)) {
                logger.info("New Object " + obj);
                EObject newObject = null;
                if (obj instanceof Node) {
                    Node v = (Node) obj;
                    logger.info("Muss neues Object erstellen: " + ResourceHelper.getXmiId(v) + " / " + v + " " + (v.getElement()));
                    String oldXMI = ResourceHelper.getXmiId(v);
                    String oldXMIElement = ResourceHelper.getXmiId(v.getElement());
                    IElementType elementType = elementTypeHelper.getElementType((View) obj);
                    EditPart editPart = DawnChangeHelper.createNode(v, elementType, (DiagramEditPart) root);
                    addChildren(editPart, v);
                    ResourceHelper.setXmiId((EObject) editPart.getModel(), oldXMI);
                    ResourceHelper.setXmiId((EObject) ((View) editPart.getModel()).getElement(), oldXMIElement);
                    DawnChangeHelper.updateModel(editPart, v.getElement(), editor);
                    newObject = (EObject) editPart.getModel();
                } else if (obj instanceof Edge) {
                    Edge v = (Edge) obj;
                    logger.info("Muss neues Object erstellen (Edge): " + v);
                    EObject source = ResourceHelper.getSameEObjectFromOtherResource(v.getSource(), (XMLResource) localResouce);
                    EditPart sourceEditPart = findEditPart((View) source);
                    EObject target = ResourceHelper.getSameEObjectFromOtherResource(v.getTarget(), (XMLResource) localResouce);
                    EditPart targetEditPart = findEditPart((View) target);
                    logger.info("Muss neues Object erstellen: " + ResourceHelper.getXmiId(v) + " / " + v);
                    String oldXMI = ResourceHelper.getXmiId(v);
                    IElementType elementType = elementTypeHelper.getElementType((View) obj);
                    logger.info("Found ElementType: " + elementType);
                    EditPart e = DawnChangeHelper.createEdge(v, sourceEditPart, targetEditPart, elementType, (DiagramEditPart) root);
                    ResourceHelper.setXmiId((EObject) e.getModel(), oldXMI);
                    newObject = (EObject) e.getModel();
                } else {
                    logger.warn("Unsupported Type: " + obj.getClass());
                }
                if (newObject != null) {
                    ResourceHelper.copyXmiIds(obj, newObject);
                }
            }
            ResourceHelper.addViewToResource((XMLResource) lastLocalResource, (View) obj);
        }
    }

    /****************************************************************************************************
	 * This messages synchronizes the XMI id of the client diagram with the
	 * server diagram. This means only that the client-diagram's xmi id is
	 * overwritten by the server's one. It also sets the xmi Id for the emf root
	 * --oldXMId=newXMIid--
	 * 
	 * NOTE: This method should become @deprecated if the recursively emf
	 * compare based comparison is implemented.
	 * 
	 * @param oldResouce
	 * @param newResource
	 ***************************************************************************************************/
    public static void synchronizeDiagramXMI(Resource oldResouce, Resource newResource) {
        Diagram oldDiagram = ResourceHelper.getDiagramFromResource(oldResouce);
        Diagram newDiagram = ResourceHelper.getDiagramFromResource(newResource);
        EObject oldEmfRoot = oldDiagram.getElement();
        EObject newEmfRoot = newDiagram.getElement();
        ResourceHelper.setXmiId(oldDiagram, ResourceHelper.getXmiId(newDiagram));
        ResourceHelper.setXmiId(oldEmfRoot, ResourceHelper.getXmiId(newEmfRoot));
    }

    /****************************************************************************************************
	 * 
	 * @param obj
	 * @return
	 ***************************************************************************************************/
    private boolean isSelected(EObject obj) {
        return getSelectedElements().contains(obj);
    }

    /****************************************************************************************************
	 * evaluates all currently selected Objects so they can be handled
	 * separately
	 ***************************************************************************************************/
    private void setSelectedElements() {
        List selectedEditParts = this.editor.getDiagramGraphicalViewer().getSelectedEditParts();
        getSelectedElements().clear();
        for (Object selectedEditPart : selectedEditParts) {
            logger.info("currently selected: " + selectedEditPart);
            getSelectedElements().add((EObject) ((EditPart) selectedEditPart).getModel());
            logger.info("ROOT: " + ((EditPart) selectedEditPart).getRoot());
        }
    }

    /**********************************************************************************************
	 * 
	 * @param view
	 * @return
	 *********************************************************************************************/
    private EditPart findEditPart(View view) {
        return ResourceHelper.findEditPart(view, editor);
    }

    /****************************************************************************************************
	 * 
	 * @param xmiId
	 ***************************************************************************************************/
    @Override
    public void resolveDeletedRemotellyConflict(String xmiId) {
        logger.info("Size of deletedRemotelyConflicts A: " + deletedRemotelyConflicts.size());
        getDeletedRemotelyConflicts().remove(xmiId);
        logger.info("Size of deletedRemotelyConflicts B: " + deletedRemotelyConflicts.size());
    }

    /****************************************************************************************************
	 * resolve the locally and globally change conflict for the view behind the
	 * xmiid
	 * 
	 * @param xmiId the id which identifies the view
	 ***************************************************************************************************/
    @Override
    public void resolveChangedLocalyAndRemotellyConflict(String xmiId) {
        getChangedLocalyAndRemotellyConflicts().remove(xmiId);
    }

    /****************************************************************************************************
	 * 
	 * @param xmiId
	 ***************************************************************************************************/
    @Override
    public void resolveDeletedLocallyConflict(String xmiId) {
        getDeletedLocallyConflicts().remove(xmiId);
    }

    /****************************************************************************************************
	 * returns the conflictlist for the object
	 * 
	 * @param model
	 * @return
	 ***************************************************************************************************/
    @Override
    public int getConflictType(String key) {
        if (this.getDeletedLocallyConflicts().containsKey(key)) {
            return LOCALLY_DELTETION_CONFLICT;
        }
        if (this.getDeletedRemotelyConflicts().containsKey(key)) {
            return REMOTELY_DELTETION_CONFLICT;
        }
        if (this.getChangedLocalyAndRemotellyConflicts().containsKey(key)) {
            return REMOTELY_AND_LOCALLY_CHANGED_CONFLICT;
        }
        return NO_CONFLICT;
    }

    /****************************************************************************************************
 * 
 ***************************************************************************************************/
    @Override
    public void unIgnored(String id) {
        getIgnoreList().remove(id);
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public Resource getLastResource() {
        return lastLocalResource;
    }

    /****************************************************************************************************
	 * 
	 * @param lastResource
	 ***************************************************************************************************/
    @Override
    public void setLastResource(Resource lastResource) {
        this.lastLocalResource = lastResource;
    }

    /****************************************************************************************************
	 * 
	 * @param id
	 ***************************************************************************************************/
    @Override
    public void setIgnored(String id) {
        getIgnoreList().add(id);
    }

    /****************************************************************************************************
	 * 
	 * @param obj
	 ***************************************************************************************************/
    @Override
    public void setIgnored(EObject obj) {
        setIgnored(ResourceHelper.getXmiId(obj));
    }

    /****************************************************************************************************
	 * sets all locks from remote
	 * 
	 * @param lockedObjects
	 ***************************************************************************************************/
    @Override
    public void setRemoteLocks(Map<String, Integer> lockedObjects) {
        Diagram diagramFromResource = editor.getDiagram();
        Set globallyLocked = new HashSet<String>();
        for (Object o : diagramFromResource.getChildren()) {
            View v = (View) o;
            String xmiID = ResourceHelper.getXmiId(v);
            if (lockedObjects.keySet().contains(xmiID)) {
                int userId = lockedObjects.get(xmiID);
                if (userId != LocalSession.mfInstance.getUser().getId()) {
                    lock(v);
                    globallyLocked.add(xmiID);
                } else {
                    lockMyLocks(v);
                    getLocallyLocked().add(xmiID);
                }
            } else {
                unlock(v);
            }
        }
        this.setGloballyLocked(globallyLocked);
    }

    /**********************************************************************************************
	 * 
	 * @param v
	 *********************************************************************************************/
    private void unlock(View v) {
        EditPart e = findEditPart(v);
        DawnChangeHelper.markUnLocked(e);
        DawnChangeHelper.activateEditPart(e);
    }

    /**********************************************************************************************
	 * lockes the objekt marked with a global locking
	 * @param v
	 *********************************************************************************************/
    private void lock(View v) {
        EditPart e = findEditPart(v);
        deselect(e);
        DawnChangeHelper.markLocked(e);
        DawnChangeHelper.deactivateEditPart(e);
        try {
            View globalView = (View) ResourceHelper.getSameEObjectFromOtherResource((EObject) e.getModel(), (XMLResource) lastServerResource);
            updateView((View) e.getModel(), globalView);
        } catch (NotIdenticalClassException e1) {
            e1.printStackTrace();
        } catch (UnsupportedViewType e1) {
            e1.printStackTrace();
        } catch (NullPointerException e2) {
            e2.printStackTrace();
        }
    }

    private void lockMyLocks(View v) {
        EditPart e = findEditPart(v);
        DawnChangeHelper.markLockedLocally(e);
    }

    private void deselect(EditPart e) {
        this.getSelectedElements().remove(e.getModel());
        this.unIgnored(ResourceHelper.getXmiId((EObject) e.getModel()));
        DawnChangeHelper.deselect(e);
    }

    /****************************************************************************************************
	 * 
	 * @return
	 ***************************************************************************************************/
    @Override
    public Set<String> getGloballyLocked() {
        if (globallyLocked == null) {
            return new HashSet<String>();
        }
        return globallyLocked;
    }

    /****************************************************************************************************
	 * 
	 * @param lockedObjects
	 ***************************************************************************************************/
    @Override
    public void setGloballyLocked(Set<String> lockedObjects) {
        this.globallyLocked = lockedObjects;
    }

    /****************************************************************************************************
	 * locks the objects on the server in the sepcific project
	 * 
	 * @param selectedElements
	 * @return
	 ***************************************************************************************************/
    @Override
    public int lockObjectsLocally(List<EditPart> selectedElements, DawnRemoteConnection dawnRemoteConnection) {
        Set<String> toBeLockedObjects = new HashSet<String>();
        int ret = 0;
        for (EditPart e : selectedElements) {
            View v = (View) e.getModel();
            toBeLockedObjects.add(ResourceHelper.getXmiId(v));
        }
        try {
            User user = LocalSession.mfInstance.getUser();
            int userId = -1;
            if (user != null) {
                userId = user.getId();
            }
            ret = dawnRemoteConnection.lockObjects(userId, projectName, toBeLockedObjects);
            if (ret == DawnReturnCodes.LOCK_SUCCESSFULL) {
                lockLocally(toBeLockedObjects);
                for (EditPart e : selectedElements) {
                    DawnChangeHelper.markLockedLocally(ResourceHelper.findEditPart((View) e.getModel(), editor));
                }
            } else {
                MessageDialog.openError(DawnEditorHelper.getActiveShell(), "Locking Error", "Not all objects could be locked");
                ;
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    private void lockLocally(Set<String> toBeLockedObjects) {
        getLocallyLocked().addAll(toBeLockedObjects);
    }

    /****************************************************************************************************
	 * unlocks all Objects from the list
	 * 
	 * @param selectedElements
	 * @return
	 ***************************************************************************************************/
    @Override
    public int unlockObjects(List<EditPart> selectedElements, DawnRemoteConnection dawnRemoteConnection) {
        Set<String> toBeUnLockedObjects = new HashSet<String>();
        int ret = 0;
        for (EditPart e : selectedElements) {
            View v = (View) e.getModel();
            toBeUnLockedObjects.add(ResourceHelper.getXmiId(v));
        }
        try {
            User user = LocalSession.mfInstance.getUser();
            int userId = -1;
            if (user != null) {
                userId = user.getId();
            }
            ret = dawnRemoteConnection.unlockObjects(userId, projectName, toBeUnLockedObjects);
            if (ret == DawnReturnCodes.UNLOCK_SUCCESSFULL) {
                locallyLocked.removeAll(toBeUnLockedObjects);
                for (EditPart e : selectedElements) {
                    DawnChangeHelper.markUnLocked(ResourceHelper.findEditPart((View) e.getModel(), editor));
                }
            }
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }
        return ret;
    }

    /****************************************************************************************************
	 * 
	 * @param gmfResource
	 ***************************************************************************************************/
    @Override
    public void saveLastResource(Resource localResource) {
        Resource lastResource = getLastResource();
        try {
            if (lastResource != null) {
                ResourceHelper.writeToFile(ResourceHelper.getLocationFromResource(localResource) + ".last", ResourceHelper.saveToXML((XMLResource) lastResource));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /****************************************************************************************************
	 * 
	 ***************************************************************************************************/
    @Override
    public void loadLastResource() {
        try {
            String locationFromResource = ResourceHelper.getLocationFromResource(localGMFResource);
            String lastResourcePath = locationFromResource + lastResourceSuffix;
            this.setLastResource(ResourceHelper.loadResourceFromFileString(lastResourcePath, resourceSet));
            logger.info("loaded lastresource from filesystem");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
