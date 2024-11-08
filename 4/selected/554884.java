package org.wings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wings.event.SMouseEvent;
import javax.swing.*;
import java.io.Serializable;
import java.io.IOException;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.beans.PropertyChangeListener;

/**
 * Slightly simplified Swing-compatible TransferHandler for the wingS-Framework
 * @author Florian Roks
 */
public class STransferHandler implements Serializable {

    private static final transient Log LOG = LogFactory.getLog(STransferHandler.class);

    /**
     * An integer representing no transfer action
     */
    public static final int NONE = DnDConstants.ACTION_NONE;

    /**
     * An integer representing a move transfer action
     */
    public static final int MOVE = DnDConstants.ACTION_MOVE;

    /**
     * An integer representing a copy transfer action
     */
    public static final int COPY = DnDConstants.ACTION_COPY;

    /**
     * An integer representing a copy|move transfer action
     */
    public static final int COPY_OR_MOVE = DnDConstants.ACTION_COPY_OR_MOVE;

    /**
     * An integer representing a link transfer action
     */
    public static final int LINK = DnDConstants.ACTION_LINK;

    private String propertyName = null;

    protected STransferHandler() {
        this(null);
    }

    /**
     * Constructs a new STransferHandler with Property property
     * @param property
     */
    public STransferHandler(String property) {
        this.propertyName = property;
    }

    /**
     * Returns whether if this component can import one of the Transfers DataFlavors  
     * @param component
     * @param transferFlavors
     * @return
     */
    public boolean canImport(SComponent component, DataFlavor[] transferFlavors) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(propertyName, component);
        if (propertyDescriptor == null) return false;
        Method writeMethod = propertyDescriptor.getWriteMethod();
        if (writeMethod == null) return false;
        Class<?>[] arguments = writeMethod.getParameterTypes();
        if (arguments == null || arguments.length != 1) return false;
        DataFlavor flavor = getPropertyDataFlavor(arguments[0], transferFlavors);
        if (flavor == null) return false;
        return true;
    }

    /**
     * Returns if this component can import the given Data within a TransferSupport
     * @param support The TransferSupport to be checked
     * @return Returns true if the Component can import the data, false if not
     */
    public boolean canImport(STransferHandler.TransferSupport support) {
        return canImport(support.getComponent(), support.getDataFlavors());
    }

    /**
     * Creates a Transferable based on the data in the given component
     * @param component
     * @return
     */
    protected Transferable createTransferable(SComponent component) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(propertyName, component);
        if (propertyDescriptor != null) {
            return new PropertyTransferable(propertyDescriptor, component);
        }
        return null;
    }

    /**
     * Creates a Transferable based on the data in the given component and event (calls createTransferable(SComponent))
     * @param component
     * @param event
     * @return
     */
    protected Transferable createTransferable(SComponent component, SMouseEvent event) {
        return createTransferable(component);
    }

    /**
     * Exports component's data for a drag-operation - it is required for this method to call
     * the setTransferSupport Method in the SDragAndDropManager to set a TransferSupport, to be used for this
     * drag-operation 
     * @param component
     * @param event
     * @param action
     */
    public void exportAsDrag(SComponent component, SMouseEvent event, int action) {
        int sourceActions = this.getSourceActions(component);
        if ((action != MOVE && action != COPY && action != LINK) || (sourceActions & action) == 0) {
            action = NONE;
        }
        if (action != NONE) {
            Transferable transferable = createTransferable(component, event);
            component.getSession().getSDragAndDropManager().setTransferSupport(new TransferSupport(component, transferable));
        } else {
            exportDone(component, null, NONE);
        }
    }

    /**
     * Method that is called after a completed Drag-Operation - action will be NONE if
     * unsuccessfull and != NONE if successfull
     * @param source
     * @param data
     * @param action
     */
    public void exportDone(SComponent source, Transferable data, int action) {
    }

    /**
     * Exports the data in componet to the given Clipboard
     * @param component
     * @param clipboard
     * @param action
     * @throws IllegalStateException
     */
    public void exportToClipboard(SComponent component, Clipboard clipboard, int action) throws IllegalStateException {
        Transferable t = createTransferable(component);
        if ((action != COPY && action != MOVE) || (getSourceActions(component) & action) == 0) {
            exportDone(component, null, NONE);
            return;
        }
        if (t != null) {
            try {
                clipboard.setContents(t, null);
                exportDone(component, t, action);
            } catch (IllegalStateException e) {
                exportDone(component, t, NONE);
                throw e;
            }
        }
    }

    /**
     * ClipboardAction - Helper-Class for the Clipboard-Actions, used by getCutAction, getCopyAction, getPasteAction
     */
    public static class ClipboardAction implements Action {

        private String name;

        public ClipboardAction(String name) {
            this.name = name;
        }

        public Object getValue(String key) {
            if (NAME.equals(key)) {
                return name;
            }
            return null;
        }

        public void putValue(String key, Object value) {
        }

        public void setEnabled(boolean b) {
        }

        public boolean isEnabled() {
            return isEnabled(null);
        }

        public boolean isEnabled(Object sender) {
            if (!(sender instanceof SComponent) || ((SComponent) sender).getTransferHandler() != null) return true;
            return false;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        public void actionPerformed(ActionEvent event) {
            SComponent component = (SComponent) event.getSource();
            try {
                STransferHandler th = component.getTransferHandler();
                Clipboard clipboard = component.getSession().getClipboard();
                if (this.name.equals("cut")) {
                    th.exportToClipboard(component, clipboard, MOVE);
                } else if (this.name.equals("copy")) {
                    th.exportToClipboard(component, clipboard, COPY);
                } else if (this.name.equals("paste")) {
                    Transferable transferable = clipboard.getContents(null);
                    TransferSupport support = new TransferSupport(component, transferable);
                    th.importData(support);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static final Action copyAction = new ClipboardAction("copy");

    static final Action cutAction = new ClipboardAction("cut");

    static final Action pasteAction = new ClipboardAction("paste");

    /**
     * Returns an Action representing a copy operation to the Clipboard
     * @return
     */
    public static Action getCopyAction() {
        return copyAction;
    }

    /**
     * Returns an Action representing a cut operation to the Clipboard
     * @return
     */
    public static Action getCutAction() {
        return cutAction;
    }

    /**
     * Returns an Action representing a paste operation from the Clipboard
     * @return
     */
    public static Action getPasteAction() {
        return pasteAction;
    }

    private DataFlavor getPropertyDataFlavor(Class<?> argument, DataFlavor[] flavors) {
        for (DataFlavor flavor : flavors) {
            if ("application".equals(flavor.getPrimaryType()) && "x-java-jvm-local-objectref".equals(flavor.getSubType())) {
                if (argument.isAssignableFrom(flavor.getRepresentationClass())) {
                    return flavor;
                }
            }
        }
        return null;
    }

    private static Object invoke(Method method, SComponent component, Object[] arguments) throws Exception {
        try {
            Object returnValue = method.invoke(component, arguments);
            return returnValue;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static class PropertyDescriptor {

        private String name = "";

        private Method readMethod;

        private Method writeMethod;

        private Class<?> propertyType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Method getReadMethod() {
            return readMethod;
        }

        public void setReadMethod(Method readMethod) {
            this.readMethod = readMethod;
            if (this.readMethod != null) {
                Class<?>[] args = readMethod.getParameterTypes();
                if (args.length != 0) {
                    LOG.error("invalid argument count for readMethod");
                }
                if (this.propertyType == null) this.propertyType = readMethod.getReturnType(); else if (this.propertyType != readMethod.getReturnType()) {
                    LOG.error("type mismatch between writeMethod and readMethod");
                }
            } else {
                this.propertyType = null;
            }
        }

        public Method getWriteMethod() {
            return writeMethod;
        }

        public void setWriteMethod(Method writeMethod) {
            this.writeMethod = writeMethod;
            if (writeMethod != null) {
                Class<?>[] args = writeMethod.getParameterTypes();
                if (args.length != 1) {
                    LOG.error("invalid argument count for writeMethod");
                }
                Class<?>[] params = writeMethod.getParameterTypes();
                if (this.propertyType == null) this.propertyType = params[0]; else if (this.propertyType != params[0]) {
                    LOG.error("type mismatch between writeMethod and readMethod");
                }
            } else {
                this.propertyType = null;
            }
        }

        public Class<?> getPropertyType() {
            return this.propertyType;
        }
    }

    private static PropertyDescriptor getPropertyDescriptor(String propertyName, Class<?> componentClass) {
        PropertyDescriptor descriptor = new PropertyDescriptor();
        Method[] methods = componentClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase("get" + propertyName)) {
                descriptor.setReadMethod(method);
                continue;
            }
            if (method.getName().equalsIgnoreCase("set" + propertyName)) {
                descriptor.setWriteMethod(method);
                continue;
            }
        }
        return descriptor;
    }

    private static PropertyDescriptor getPropertyDescriptor(String propertyName, SComponent component) {
        if (propertyName == null) {
            return null;
        }
        try {
            PropertyDescriptor property = getPropertyDescriptor(propertyName, component.getClass());
            Method readMethod = property.getReadMethod();
            if (readMethod != null) {
                Class<?>[] params = readMethod.getParameterTypes();
                if (params == null || params.length == 0) {
                    return property;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the Actions supported by the given Component - Actions may be any combination of COPY, MOVE, LINK or NONE
     * @param component
     * @return
     */
    public int getSourceActions(SComponent component) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(this.propertyName, component);
        if (propertyDescriptor != null) return COPY;
        return NONE;
    }

    /**
     * Returns an SIcon representing the visual representation of the Transferable transferable
     * @param transferable
     * @return
     */
    public SIcon getVisualRepresentation(Transferable transferable) {
        return null;
    }

    private SLabel label = null;

    /**
     * Returns an SLabel representing the visual representation of the Transferable transferable
     * Note: (calls SIcon getVisualRepresentation(Transferable))
     * @param transferable
     * @return
     */
    public SLabel getVisualRepresentationLabel(Transferable transferable) {
        SIcon icon = getVisualRepresentation(transferable);
        if (icon == null) return null;
        if (label == null) label = new SLabel();
        label.setIcon(icon);
        return label;
    }

    /**
     * Imports the data specified in the Transferable into the given component
     * @param component
     * @param transferable
     * @return
     */
    public boolean importData(SComponent component, Transferable transferable) {
        PropertyDescriptor propertyDecsriptor = getPropertyDescriptor(this.propertyName, component);
        if (propertyDecsriptor == null) return false;
        Method writeMethod = propertyDecsriptor.getWriteMethod();
        if (writeMethod == null) return false;
        Class<?>[] arguments = writeMethod.getParameterTypes();
        if (arguments == null || arguments.length != 1) {
            return false;
        }
        DataFlavor flavor = getPropertyDataFlavor(arguments[0], transferable.getTransferDataFlavors());
        if (flavor != null) {
            try {
                Object value = transferable.getTransferData(flavor);
                STransferHandler.invoke(writeMethod, component, new Object[] { value });
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Imports the data specified in TransferSupport into the component specified in TransferSupport
     * @param support 
     * @return
     */
    public boolean importData(STransferHandler.TransferSupport support) {
        return importData(support.getComponent(), support.getTransferable());
    }

    /**
     * PropertyTransferable - represents Data from a Property
     */
    public static class PropertyTransferable implements Transferable {

        private PropertyDescriptor propertyDescriptor = null;

        private SComponent component;

        public PropertyTransferable(PropertyDescriptor propertyDescriptor, SComponent component) {
            this.propertyDescriptor = propertyDescriptor;
            this.component = component;
        }

        public DataFlavor[] getTransferDataFlavors() {
            Class<?> propertyType = propertyDescriptor.getPropertyType();
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + propertyType.getName();
            try {
                return new DataFlavor[] { new DataFlavor(mimeType) };
            } catch (ClassNotFoundException e) {
                return new DataFlavor[0];
            }
        }

        public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
            Class<?> propertyType = propertyDescriptor.getPropertyType();
            if (dataFlavor.getPrimaryType().equals("application") && dataFlavor.getSubType().equals("x-java-jvm-local-objectref") && dataFlavor.getRepresentationClass().isAssignableFrom(propertyType)) return true;
            return false;
        }

        public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(dataFlavor)) throw new UnsupportedFlavorException(dataFlavor);
            Method reader = this.propertyDescriptor.getReadMethod();
            try {
                return invoke(reader, this.component, null);
            } catch (Exception e) {
                throw new IOException("Invoke: Property read failed: " + this.propertyDescriptor.getName());
            }
        }
    }

    /**
     * TransferSupport encapsulates details of a transfer
     */
    public static final class TransferSupport {

        private SComponent component;

        private Transferable transferable;

        private boolean isDrop = false;

        private int dropAction = -1;

        private int requestedDropAction = -1;

        private DropLocation dropLocation;

        private SComponent source;

        public TransferSupport(SComponent component, Transferable transferable) {
            if (component == null) throw new NullPointerException("component must be not null");
            if (transferable == null) throw new NullPointerException("transferable must be not null");
            this.component = component;
            this.transferable = transferable;
        }

        /**
         * Prepares the TransferSupport to be used in drop cases
         * @param component
         * @param event
         * @param action
         */
        public void setIsDrop(SComponent component, SComponent source, SMouseEvent event, int action) {
            this.isDrop = true;
            this.component = component;
            this.source = source;
            this.requestedDropAction = action;
            setDropLocation(this.component, event.getPoint());
        }

        /**
         * Returns the component on which the cursor is  
         * @return
         */
        public SComponent getComponent() {
            return this.component;
        }

        /**
         * Returns the Data Flavors, that this TransferSupport supports
         * @return
         */
        public DataFlavor[] getDataFlavors() {
            return transferable.getTransferDataFlavors();
        }

        /**
         * Returns the drop action requested by the user
         * @return
         */
        public int getUserDropAction() {
            if (!isDrop()) throw new IllegalStateException("getUserDropAction called when isDrop = false");
            return this.requestedDropAction;
        }

        /**
         * Returns the drop action to be used - either getUserDropAction in case that none was set programatically
         * or the action set with setDropAction(int)
         * @return
         */
        public int getDropAction() {
            if (dropAction == -1) return getUserDropAction();
            return dropAction;
        }

        /**
         * Returns the Drop-Actions supported by the source-component
         * @return
         */
        public int getSourceDropActions() {
            if (!isDrop()) throw new IllegalStateException("getSourceDropActions called when isDrop = false");
            return this.source.getTransferHandler().getSourceActions(this.source);
        }

        /**
         * Returns the Transferable associated with this TransferSupport
         * @return
         */
        public Transferable getTransferable() {
            return this.transferable;
        }

        /**
         * Returns if dataFlavor is supported by this TransferSupport
         * @param dataFlavor
         * @return
         */
        public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
            return transferable.isDataFlavorSupported(dataFlavor);
        }

        /**
         * Returns if this TransferSupport represents a drop
         * @return
         */
        public boolean isDrop() {
            return this.isDrop;
        }

        /**
         * Sets a drop action programmatically - it must be supported by the source component and a valid action
         * @param dropAction
         */
        public void setDropAction(int dropAction) {
            if (!isDrop()) throw new IllegalStateException("isDrop was false when setDropAction was called");
            int action = dropAction & getSourceDropActions();
            if (action != COPY && action != MOVE && action != LINK) {
                throw new IllegalArgumentException("unsupported drop action " + dropAction);
            }
            this.dropAction = dropAction;
        }

        protected void setDropLocation(SComponent component, SPoint point) {
            this.dropLocation = component.dropLocationForPoint(point);
        }

        /**
         * Returns the DropLocation for this  Transfer
         * @return
         */
        public DropLocation getDropLocation() {
            if (!isDrop()) throw new IllegalStateException("isDrop was false when getDropLocation was called");
            return this.dropLocation;
        }
    }

    /**
     * Represents a location where dropped data should be inserted
     */
    public static class DropLocation {

        private final SPoint point;

        protected DropLocation(SPoint point) {
            this.point = point;
        }

        public final SPoint getDropPoint() {
            return this.point;
        }
    }
}
