package net.sf.ncsimulator.models.network.diagram.navigator;

import net.sf.ncsimulator.models.network.Channel;
import net.sf.ncsimulator.models.network.Network;
import net.sf.ncsimulator.models.network.diagram.edit.parts.ChannelEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NetworkEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NodeEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NodeNameEditPart;
import net.sf.ncsimulator.models.network.diagram.part.NetworkDiagramEditorPlugin;
import net.sf.ncsimulator.models.network.diagram.part.NetworkVisualIDRegistry;
import net.sf.ncsimulator.models.network.diagram.providers.NetworkElementTypes;
import net.sf.ncsimulator.models.network.diagram.providers.NetworkParserProvider;
import org.eclipse.gmf.runtime.common.ui.services.parser.IParser;
import org.eclipse.gmf.runtime.common.ui.services.parser.ParserOptions;
import org.eclipse.gmf.runtime.emf.core.util.EObjectAdapter;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * @generated
 */
public class NetworkNavigatorLabelProvider extends LabelProvider implements ICommonLabelProvider, ITreePathLabelProvider {

    /**
	 * @generated
	 */
    static {
        NetworkDiagramEditorPlugin.getInstance().getImageRegistry().put("Navigator?UnknownElement", ImageDescriptor.getMissingImageDescriptor());
        NetworkDiagramEditorPlugin.getInstance().getImageRegistry().put("Navigator?ImageNotFound", ImageDescriptor.getMissingImageDescriptor());
    }

    /**
	 * @generated
	 */
    public void updateLabel(ViewerLabel label, TreePath elementPath) {
        Object element = elementPath.getLastSegment();
        if (element instanceof NetworkNavigatorItem && !isOwnView(((NetworkNavigatorItem) element).getView())) {
            return;
        }
        label.setText(getText(element));
        label.setImage(getImage(element));
    }

    /**
	 * @generated
	 */
    public Image getImage(Object element) {
        if (element instanceof NetworkNavigatorGroup) {
            NetworkNavigatorGroup group = (NetworkNavigatorGroup) element;
            return NetworkDiagramEditorPlugin.getInstance().getBundledImage(group.getIcon());
        }
        if (element instanceof NetworkNavigatorItem) {
            NetworkNavigatorItem navigatorItem = (NetworkNavigatorItem) element;
            if (!isOwnView(navigatorItem.getView())) {
                return super.getImage(element);
            }
            return getImage(navigatorItem.getView());
        }
        return super.getImage(element);
    }

    /**
	 * @generated
	 */
    public Image getImage(View view) {
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case ChannelEditPart.VISUAL_ID:
                return getImage("Navigator?Link?http://ncsimulator.sf.net/models/network/?Channel", NetworkElementTypes.Channel_4003);
            case NetworkEditPart.VISUAL_ID:
                return getImage("Navigator?Diagram?http://ncsimulator.sf.net/models/network/?Network", NetworkElementTypes.Network_1000);
            case NodeEditPart.VISUAL_ID:
                return getImage("Navigator?TopLevelNode?http://ncsimulator.sf.net/models/network/?Node", NetworkElementTypes.Node_2001);
        }
        return getImage("Navigator?UnknownElement", null);
    }

    /**
	 * @generated
	 */
    private Image getImage(String key, IElementType elementType) {
        ImageRegistry imageRegistry = NetworkDiagramEditorPlugin.getInstance().getImageRegistry();
        Image image = imageRegistry.get(key);
        if (image == null && elementType != null && NetworkElementTypes.isKnownElementType(elementType)) {
            image = NetworkElementTypes.getImage(elementType);
            imageRegistry.put(key, image);
        }
        if (image == null) {
            image = imageRegistry.get("Navigator?ImageNotFound");
            imageRegistry.put(key, image);
        }
        return image;
    }

    /**
	 * @generated
	 */
    public String getText(Object element) {
        if (element instanceof NetworkNavigatorGroup) {
            NetworkNavigatorGroup group = (NetworkNavigatorGroup) element;
            return group.getGroupName();
        }
        if (element instanceof NetworkNavigatorItem) {
            NetworkNavigatorItem navigatorItem = (NetworkNavigatorItem) element;
            if (!isOwnView(navigatorItem.getView())) {
                return null;
            }
            return getText(navigatorItem.getView());
        }
        return super.getText(element);
    }

    /**
	 * @generated
	 */
    public String getText(View view) {
        if (view.getElement() != null && view.getElement().eIsProxy()) {
            return getUnresolvedDomainElementProxyText(view);
        }
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case ChannelEditPart.VISUAL_ID:
                return getChannel_4003Text(view);
            case NetworkEditPart.VISUAL_ID:
                return getNetwork_1000Text(view);
            case NodeEditPart.VISUAL_ID:
                return getNode_2001Text(view);
        }
        return getUnknownElementText(view);
    }

    /**
	 * @generated
	 */
    private String getNetwork_1000Text(View view) {
        Network domainModelElement = (Network) view.getElement();
        if (domainModelElement != null) {
            return domainModelElement.getName();
        } else {
            NetworkDiagramEditorPlugin.getInstance().logError("No domain element for view with visualID = " + 1000);
            return "";
        }
    }

    /**
	 * @generated
	 */
    private String getNode_2001Text(View view) {
        IParser parser = NetworkParserProvider.getParser(NetworkElementTypes.Node_2001, view.getElement() != null ? view.getElement() : view, NetworkVisualIDRegistry.getType(NodeNameEditPart.VISUAL_ID));
        if (parser != null) {
            return parser.getPrintString(new EObjectAdapter(view.getElement() != null ? view.getElement() : view), ParserOptions.NONE.intValue());
        } else {
            NetworkDiagramEditorPlugin.getInstance().logError("Parser was not found for label " + 5001);
            return "";
        }
    }

    /**
	 * @generated
	 */
    private String getChannel_4003Text(View view) {
        Channel domainModelElement = (Channel) view.getElement();
        if (domainModelElement != null) {
            return domainModelElement.getTitle();
        } else {
            NetworkDiagramEditorPlugin.getInstance().logError("No domain element for view with visualID = " + 4003);
            return "";
        }
    }

    /**
	 * @generated
	 */
    private String getUnknownElementText(View view) {
        return "<UnknownElement Visual_ID = " + view.getType() + ">";
    }

    /**
	 * @generated
	 */
    private String getUnresolvedDomainElementProxyText(View view) {
        return "<Unresolved domain element Visual_ID = " + view.getType() + ">";
    }

    /**
	 * @generated
	 */
    public void init(ICommonContentExtensionSite aConfig) {
    }

    /**
	 * @generated
	 */
    public void restoreState(IMemento aMemento) {
    }

    /**
	 * @generated
	 */
    public void saveState(IMemento aMemento) {
    }

    /**
	 * @generated
	 */
    public String getDescription(Object anElement) {
        return null;
    }

    /**
	 * @generated
	 */
    private boolean isOwnView(View view) {
        return NetworkEditPart.MODEL_ID.equals(NetworkVisualIDRegistry.getModelID(view));
    }
}
