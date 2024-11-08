package com.safi.workshop.edit.commands;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gmf.runtime.diagram.ui.commands.PromptForConnectionAndEndCommand;
import org.eclipse.gmf.runtime.diagram.ui.internal.commands.ElementTypeLabelProvider;
import org.eclipse.gmf.runtime.diagram.ui.menus.PopupMenu;
import org.eclipse.gmf.runtime.emf.type.core.MetamodelType;
import org.eclipse.jface.viewers.ILabelProvider;
import com.safi.workshop.edit.parts.HandlerEditPart;
import com.safi.workshop.part.AsteriskPaletteFactory;

public class SafiPromptForConnectionAndEndCommand extends PromptForConnectionAndEndCommand {

    private WeakReference<HandlerEditPart> handlerEP;

    private static WeakReference<HandlerEditPart> lastHandlerEP;

    private static WeakReference<PopupMenu> cachedMenu;

    private static Map<MetamodelType, String> paletteMap;

    public SafiPromptForConnectionAndEndCommand(CreateConnectionRequest request, HandlerEditPart containerEP) {
        super(request, containerEP);
        this.handlerEP = new WeakReference<HandlerEditPart>(containerEP);
        if (lastHandlerEP != null && lastHandlerEP.get() != containerEP) {
            cachedMenu = null;
            lastHandlerEP = handlerEP;
        }
    }

    @Override
    public boolean canExecute() {
        return handlerEP != null && handlerEP.get() != null;
    }

    @Override
    protected PopupMenu createPopupMenu() {
        final List connectionMenuContent = getConnectionMenuContent();
        if (connectionMenuContent == null || connectionMenuContent.isEmpty()) {
            return null;
        } else if (connectionMenuContent.size() == 1) {
            List menuContent = getEndMenuContent(connectionMenuContent.get(0));
            if (menuContent == null || menuContent.isEmpty()) {
                return null;
            }
            ILabelProvider labelProvider = getConnectionAndEndLabelProvider(connectionMenuContent.get(0));
            if (handlerEP != null && handlerEP.get() != null) {
                GraphicalViewer graphicalViewer = ((GraphicalViewer) handlerEP.get().getViewer());
                PaletteRoot root = graphicalViewer.getEditDomain().getPaletteViewer().getPaletteRoot();
                menuContent = groupMenuContent(root, menuContent, labelProvider);
            }
            cachedMenu = new WeakReference<PopupMenu>(new PopupMenu(menuContent, labelProvider) {

                /**
         * @see org.eclipse.gmf.runtime.diagram.ui.menus.PopupMenu#getResult()
         */
                @Override
                public Object getResult() {
                    Object endResult = super.getResult();
                    if (endResult == null) {
                        return null;
                    } else {
                        List resultList = new ArrayList(2);
                        resultList.add(connectionMenuContent.get(0));
                        resultList.add((endResult instanceof List) ? ((List) endResult).get(((List) endResult).size() - 1) : endResult);
                        return resultList;
                    }
                }
            });
        } else {
            List menuContent = new ArrayList();
            for (Iterator iter = connectionMenuContent.iterator(); iter.hasNext(); ) {
                Object connectionItem = iter.next();
                List subMenuContent = getEndMenuContent(connectionItem);
                if (subMenuContent.isEmpty()) {
                    continue;
                }
                PopupMenu subMenu = new PopupMenu(subMenuContent, getEndLabelProvider());
                menuContent.add(new PopupMenu.CascadingMenu(connectionItem, subMenu));
            }
            if (!menuContent.isEmpty()) {
                cachedMenu = new WeakReference<PopupMenu>(new PopupMenu(menuContent, getConnectionLabelProvider()));
            }
        }
        return cachedMenu.get();
    }

    private List groupMenuContent(PaletteRoot root, List menuContent, ILabelProvider labelProvider) {
        List newContent = new ArrayList();
        if (paletteMap == null) {
            List children = root.getChildren();
            paletteMap = new LinkedHashMap<MetamodelType, String>();
            for (Object o : children) {
                if (o instanceof PaletteContainer) {
                    buildPaletteItemMap((PaletteContainer) o, paletteMap);
                }
            }
        }
        Map<String, List> contentMap = new LinkedHashMap<String, List>();
        for (Object o : menuContent) {
            if (o instanceof MetamodelType) {
                MetamodelType mm = (MetamodelType) o;
                String parentMenu = paletteMap.get(mm);
                if (parentMenu == null) {
                    continue;
                }
                List subItems = contentMap.get(parentMenu);
                if (subItems != null) {
                    subItems.add(mm);
                } else {
                    List nl = new ArrayList();
                    nl.add(mm);
                    contentMap.put(parentMenu, nl);
                }
            } else newContent.add(o);
        }
        for (Map.Entry<String, List> entry : contentMap.entrySet()) {
            PopupMenu subMenu = new PopupMenu(entry.getValue(), labelProvider);
            newContent.add(new PopupMenu.CascadingMenu(entry.getKey(), subMenu));
        }
        return newContent;
    }

    private void buildPaletteItemMap(PaletteContainer parent, Map<MetamodelType, String> paletteMap) {
        String label = parent.getLabel();
        for (Object child : parent.getChildren()) {
            if (child instanceof AsteriskPaletteFactory.NodeToolEntry) {
                AsteriskPaletteFactory.NodeToolEntry entry = (AsteriskPaletteFactory.NodeToolEntry) child;
                List types = entry.getElementTypes();
                for (Object t : types) {
                    paletteMap.put((MetamodelType) t, label);
                }
            } else if (child instanceof PaletteContainer) buildPaletteItemMap((PaletteContainer) child, paletteMap);
        }
    }

    @Override
    public IStatus execute(IProgressMonitor progressMonitor, IAdaptable info) throws ExecutionException {
        return super.execute(progressMonitor, info);
    }

    protected ILabelProvider connectionAndEndLabelProvider;

    protected ILabelProvider endLabelProvider;

    protected ILabelProvider connectionLabelProvider;

    @Override
    protected ILabelProvider getConnectionAndEndLabelProvider(Object connectionItem) {
        if (connectionAndEndLabelProvider == null) connectionAndEndLabelProvider = new SafiConnectionAndEndLabelProvider(connectionItem);
        return connectionAndEndLabelProvider;
    }

    @Override
    protected ILabelProvider getEndLabelProvider() {
        if (endLabelProvider == null) endLabelProvider = new SafiEndLabelProvider();
        return endLabelProvider;
    }

    @Override
    protected ILabelProvider getConnectionLabelProvider() {
        if (connectionLabelProvider == null) connectionLabelProvider = new SafiConnectionLabelProvider();
        return connectionLabelProvider;
    }

    protected class SafiConnectionLabelProvider extends ConnectionLabelProvider {

        @Override
        public String getText(Object element) {
            String text = super.getText(element);
            return text;
        }
    }

    protected class SafiConnectionAndEndLabelProvider extends ElementTypeLabelProvider {

        private Object connectionItem;

        public SafiConnectionAndEndLabelProvider(Object connectionItem) {
            this.connectionItem = connectionItem;
        }

        @Override
        public String getText(Object element) {
            String elementText = super.getText(element);
            return elementText;
        }
    }

    protected class SafiEndLabelProvider extends EndLabelProvider {

        @Override
        public String getText(Object element) {
            String text = super.getText(element);
            return text;
        }
    }
}
