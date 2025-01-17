package guiparser;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Component decorator that implements the view interface 
 * for form elements, &lt;input&gt;, &lt;textarea&gt;,
 * and &lt;select&gt;.  The model for the component is stored 
 * as an attribute of the the element (using StyleConstants.ModelAttribute), 
 * and is used to build the component of the view.  The type
 * of the model is assumed to of the type that would be set by
 * <code>HTMLDocument.HTMLReader.FormAction</code>.  If there are
 * multiple views mapped over the document, they will share the 
 * embedded component models.
 * <p>
 * The following table shows what components get built
 * by this view.
 * <table summary="shows what components get built by this view">
 * <tr>
 *   <th>Element Type</th>
 *   <th>Component built</th>
 * </tr>
 * <tr>
 *   <td>input, type button</td>
 *   <td>JButton</td>
 * </tr>
 * <tr>
 *   <td>input, type checkbox</td>
 *   <td>JCheckBox</td>
 * </tr>
 * <tr>
 *   <td>input, type image</td>
 *   <td>JButton</td>
 * </tr>
 * <tr>
 *   <td>input, type password</td>
 *   <td>JPasswordField</td>
 * </tr>
 * <tr>
 *   <td>input, type radio</td>
 *   <td>JRadioButton</td>
 * </tr>
 * <tr>
 *   <td>input, type reset</td>
 *   <td>JButton</td>
 * </tr>
 * <tr>
 *   <td>input, type submit</td>
 *   <td>JButton</td>
 * </tr>
 * <tr>
 *   <td>input, type text</td>
 *   <td>JTextField</td>
 * </tr>
 * <tr>
 *   <td>select, size &gt; 1 or multiple attribute defined</td>
 *   <td>JList in a JScrollPane</td>
 * </tr>
 * <tr>
 *   <td>select, size unspecified or 1</td>
 *   <td>JComboBox</td>
 * </tr>
 * <tr>
 *   <td>textarea</td>
 *   <td>JTextArea in a JScrollPane</td>
 * </tr>
 * <tr>
 *   <td>input, type file</td>
 *   <td>JTextField</td>
 * </tr>
 * </table>
 *
 * @author Timothy Prinzing
 * @author Sunita Mani
 * @version 1.28 05/27/05
 */
public class FormView extends ComponentView implements ActionListener {

    /**
     * If a value attribute is not specified for a FORM input element
     * of type "submit", then this default string is used.
     *
     * @deprecated As of 1.3, value now comes from UIManager property
     *             FormView.submitButtonText
     */
    @Deprecated
    public static final String SUBMIT = new String("Submit Query");

    /**
     * If a value attribute is not specified for a FORM input element
     * of type "reset", then this default string is used.
     *
     * @deprecated As of 1.3, value comes from UIManager UIManager property
     *             FormView.resetButtonText
     */
    @Deprecated
    public static final String RESET = new String("Reset");

    /**
     * Used to indicate if the maximum span should be the same as the
     * preferred span. This is used so that the Component's size doesn't
     * change if there is extra room on a line. The first bit is used for
     * the X direction, and the second for the y direction.
     */
    private short maxIsPreferred;

    /**
     * Creates a new FormView object.
     *
     * @param elem the element to decorate
     */
    public FormView(Element elem) {
        super(elem);
    }

    /**
     * Create the component.  This is basically a
     * big switch statement based upon the tag type
     * and html attributes of the associated element.
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        HTML.Tag t = (HTML.Tag) attr.getAttribute(StyleConstants.NameAttribute);
        JComponent c = null;
        Object model = attr.getAttribute(StyleConstants.ModelAttribute);
        if (t == HTML.Tag.INPUT) {
            c = createInputComponent(attr, model);
        } else if (t == HTML.Tag.SELECT) {
            if (model instanceof OptionListModel) {
                JList list = new JList((ListModel) model);
                int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, 1);
                list.setVisibleRowCount(size);
                list.setSelectionModel((ListSelectionModel) model);
                c = new JScrollPane(list);
            } else {
                c = new JComboBox((ComboBoxModel) model);
                maxIsPreferred = 3;
            }
        } else if (t == HTML.Tag.TEXTAREA) {
            JTextArea area = new JTextArea((Document) model);
            int rows = HTML.getIntegerAttributeValue(attr, HTML.Attribute.ROWS, 1);
            area.setRows(rows);
            int cols = HTML.getIntegerAttributeValue(attr, HTML.Attribute.COLS, 20);
            maxIsPreferred = 3;
            area.setColumns(cols);
            c = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }
        if (c != null) {
            c.setAlignmentY(1.0f);
        }
        return c;
    }

    /**
     * Creates a component for an &lt;INPUT&gt; element based on the
     * value of the "type" attribute.
     *
     * @param set of attributes associated with the &lt;INPUT&gt; element.
     * @param model the value of the StyleConstants.ModelAttribute
     * @return the component.
     */
    private JComponent createInputComponent(AttributeSet attr, Object model) {
        JComponent c = null;
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if (type.equals("submit") || type.equals("reset")) {
            String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
            if (value == null) {
                if (type.equals("submit")) {
                    value = UIManager.getString("FormView.submitButtonText");
                } else {
                    value = UIManager.getString("FormView.resetButtonText");
                }
            }
            JButton button = new JButton(value);
            if (model != null) {
                button.setModel((ButtonModel) model);
                button.addActionListener(this);
            }
            c = button;
            maxIsPreferred = 3;
        } else if (type.equals("image")) {
            String srcAtt = (String) attr.getAttribute(HTML.Attribute.SRC);
            JButton button;
            try {
                URL base = ((HTMLDocument) getElement().getDocument()).getBase();
                URL srcURL = new URL(base, srcAtt);
                Icon icon = new ImageIcon(srcURL);
                button = new JButton(icon);
            } catch (MalformedURLException e) {
                button = new JButton(srcAtt);
            }
            if (model != null) {
                button.setModel((ButtonModel) model);
                button.addMouseListener(new MouseEventListener());
            }
            c = button;
            maxIsPreferred = 3;
        } else if (type.equals("checkbox")) {
            c = new JCheckBox();
            if (model != null) {
                ((JCheckBox) c).setModel((JToggleButton.ToggleButtonModel) model);
            }
            maxIsPreferred = 3;
        } else if (type.equals("radio")) {
            c = new JRadioButton();
            if (model != null) {
                ((JRadioButton) c).setModel((JToggleButton.ToggleButtonModel) model);
            }
            maxIsPreferred = 3;
        } else if (type.equals("text")) {
            int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, -1);
            JTextField field;
            if (size > 0) {
                field = new JTextField();
                field.setColumns(size);
            } else {
                field = new JTextField();
                field.setColumns(20);
            }
            c = field;
            if (model != null) {
                field.setDocument((Document) model);
            }
            field.addActionListener(this);
            maxIsPreferred = 3;
        } else if (type.equals("password")) {
            JPasswordField field = new JPasswordField();
            c = field;
            if (model != null) {
                field.setDocument((Document) model);
            }
            int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, -1);
            field.setColumns((size > 0) ? size : 20);
            field.addActionListener(this);
            maxIsPreferred = 3;
        } else if (type.equals("file")) {
            JTextField field = new JTextField();
            if (model != null) {
                field.setDocument((Document) model);
            }
            int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, -1);
            field.setColumns((size > 0) ? size : 20);
            JButton browseButton = new JButton(UIManager.getString("FormView.browseFileButtonText"));
            Box box = Box.createHorizontalBox();
            box.add(field);
            box.add(Box.createHorizontalStrut(5));
            box.add(browseButton);
            browseButton.addActionListener(new BrowseFileAction(attr, (Document) model));
            c = box;
            maxIsPreferred = 3;
        }
        return c;
    }

    /**
     * Determines the maximum span for this view along an
     * axis. For certain components, the maximum and preferred span are the
     * same. For others this will return the value
     * returned by Component.getMaximumSize along the
     * axis of interest.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into >= 0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.  
     *           The parent may choose to resize or break the view.
     * @exception IllegalArgumentException for an invalid axis
     */
    public float getMaximumSpan(int axis) {
        switch(axis) {
            case View.X_AXIS:
                if ((maxIsPreferred & 1) == 1) {
                    super.getMaximumSpan(axis);
                    return getPreferredSpan(axis);
                }
                return super.getMaximumSpan(axis);
            case View.Y_AXIS:
                if ((maxIsPreferred & 2) == 2) {
                    super.getMaximumSpan(axis);
                    return getPreferredSpan(axis);
                }
                return super.getMaximumSpan(axis);
            default:
                break;
        }
        return super.getMaximumSpan(axis);
    }

    /**
     * Responsible for processeing the ActionEvent.
     * If the element associated with the FormView,
     * has a type of "submit", "reset", "text" or "password" 
     * then the action is processed.  In the case of a "submit" 
     * the form is submitted.  In the case of a "reset"
     * the form is reset to its original state.
     * In the case of "text" or "password", if the 
     * element is the last one of type "text" or "password",
     * the form is submitted.  Otherwise, focus is transferred
     * to the next component in the form.
     *
     * @param evt the ActionEvent.
     */
    public void actionPerformed(ActionEvent evt) {
        Element element = getElement();
        StringBuffer dataBuffer = new StringBuffer();
        HTMLDocument doc = (HTMLDocument) getDocument();
        AttributeSet attr = element.getAttributes();
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if (type.equals("submit")) {
            getFormData(dataBuffer);
            submitData(dataBuffer.toString());
        } else if (type.equals("reset")) {
            resetForm();
        } else if (type.equals("text") || type.equals("password")) {
            if (isLastTextOrPasswordField()) {
                getFormData(dataBuffer);
                submitData(dataBuffer.toString());
            } else {
                getComponent().transferFocus();
            }
        }
    }

    /**
     * This method is responsible for submitting the form data.
     * A thread is forked to undertake the submission.
     */
    protected void submitData(String data) {
        SubmitThread dataThread = new SubmitThread(getElement(), data);
        dataThread.start();
    }

    /**
     * The SubmitThread is responsible for submitting the form.
     * It performs a POST or GET based on the value of method
     * attribute associated with  HTML.Tag.FORM.  In addition to
     * submitting, it is also responsible for display the 
     * results of the form submission.
     */
    class SubmitThread extends Thread {

        String data;

        HTMLDocument hdoc;

        AttributeSet formAttr;

        URL url;

        String method;

        String target;

        URL actionURL;

        public SubmitThread(Element elem, String data) {
            this.data = data;
            hdoc = (HTMLDocument) elem.getDocument();
            Element formE = getFormElement();
            if (formE != null) {
                formAttr = formE.getAttributes();
            }
            method = getMethod();
            try {
                String action = getAction();
                method = getMethod();
                target = getTarget();
                URL baseURL = hdoc.getBase();
                if (action == null) {
                    String file = baseURL.getFile();
                    actionURL = new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort(), file);
                } else {
                    actionURL = new URL(baseURL, action);
                }
            } catch (MalformedURLException m) {
                actionURL = null;
            }
        }

        /**
	 * This method is responsible for extracting the
	 * method and action attributes associated with the
	 * &lt;FORM&gt; and using those to determine how (POST or GET)
	 * and where (URL) to submit the form.  If action is
	 * not specified, the base url of the existing document is
	 * used.  Also, if method is not specified, the default is
	 * GET.  Once form submission is done, run uses the
	 * SwingUtilities.invokeLater() method, to load the results
	 * of the form submission into the current JEditorPane.
	 */
        public void run() {
            if (data.length() > 0) {
                try {
                    URLConnection connection;
                    JEditorPane c = (JEditorPane) getContainer();
                    HTMLEditorKit kit = (HTMLEditorKit) c.getEditorKit();
                    if (kit.isAutoFormSubmission()) {
                        if ("post".equals(method)) {
                            url = actionURL;
                            connection = url.openConnection();
                            postData(connection, data);
                        } else {
                            url = new URL(actionURL + "?" + data);
                        }
                        Runnable callLoadDocument = new Runnable() {

                            public void run() {
                                JEditorPane c = (JEditorPane) getContainer();
                                if (hdoc.isFrameDocument()) {
                                    c.fireHyperlinkUpdate(createFormSubmitEvent());
                                } else {
                                    try {
                                        c.setPage(url);
                                    } catch (IOException e) {
                                    }
                                }
                            }
                        };
                        SwingUtilities.invokeLater(callLoadDocument);
                    } else {
                        c.fireHyperlinkUpdate(createFormSubmitEvent());
                    }
                } catch (MalformedURLException m) {
                } catch (IOException e) {
                }
            }
        }

        /**
	 * Create an event that notifies about form submission
	 */
        private FormSubmitEvent createFormSubmitEvent() {
            FormSubmitEvent.MethodType formMethod = "post".equals(method) ? FormSubmitEvent.MethodType.POST : FormSubmitEvent.MethodType.GET;
            return new FormSubmitEvent(FormView.this, HyperlinkEvent.EventType.ACTIVATED, actionURL, getElement(), target, formMethod, data);
        }

        /**
	 * Get the value of the target attribute.
	 */
        private String getTarget() {
            if (formAttr != null) {
                String target = (String) formAttr.getAttribute(HTML.Attribute.TARGET);
                if (target != null) {
                    return target.toLowerCase();
                }
            }
            return "_self";
        }

        /**
	 * Get the value of the action attribute.
	 */
        public String getAction() {
            if (formAttr == null) {
                return null;
            }
            return (String) formAttr.getAttribute(HTML.Attribute.ACTION);
        }

        /**
	 * Get the form's method parameter.
	 */
        String getMethod() {
            if (formAttr != null) {
                String method = (String) formAttr.getAttribute(HTML.Attribute.METHOD);
                if (method != null) {
                    return method.toLowerCase();
                }
            }
            return null;
        }

        /**
	 * This method is responsible for writing out the form submission
	 * data when the method is POST.
	 * 
	 * @param connection to use.
	 * @param data to write.
	 */
        public void postData(URLConnection connection, String data) {
            connection.setDoOutput(true);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
                out.print(data);
                out.flush();
            } catch (IOException e) {
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * MouseEventListener class to handle form submissions when
     * an input with type equal to image is clicked on.
     * A MouseListener is necessary since along with the image
     * data the coordinates associated with the mouse click
     * need to be submitted.
     */
    protected class MouseEventListener extends MouseAdapter {

        public void mouseReleased(MouseEvent evt) {
            String imageData = getImageData(evt.getPoint());
            imageSubmit(imageData);
        }
    }

    /**
     * This method is called to submit a form in response
     * to a click on an image -- an &lt;INPUT&gt; form
     * element of type "image".
     *
     * @param imageData the mouse click coordinates.
     */
    protected void imageSubmit(String imageData) {
        StringBuffer dataBuffer = new StringBuffer();
        Element elem = getElement();
        HTMLDocument hdoc = (HTMLDocument) elem.getDocument();
        getFormData(dataBuffer);
        if (dataBuffer.length() > 0) {
            dataBuffer.append('&');
        }
        dataBuffer.append(imageData);
        submitData(dataBuffer.toString());
        return;
    }

    /**
     * Extracts the value of the name attribute
     * associated with the input element of type
     * image.  If name is defined it is encoded using
     * the URLEncoder.encode() method and the 
     * image data is returned in the following format:
     *	    name + ".x" +"="+ x +"&"+ name +".y"+"="+ y
     * otherwise,
     * 	    "x="+ x +"&y="+ y
     * 
     * @param point associated with the mouse click.
     * @return the image data.
     */
    private String getImageData(Point point) {
        String mouseCoords = point.x + ":" + point.y;
        int sep = mouseCoords.indexOf(':');
        String x = mouseCoords.substring(0, sep);
        String y = mouseCoords.substring(++sep);
        String name = (String) getElement().getAttributes().getAttribute(HTML.Attribute.NAME);
        String data;
        if (name == null || name.equals("")) {
            data = "x=" + x + "&y=" + y;
        } else {
            name = URLEncoder.encode(name);
            data = name + ".x" + "=" + x + "&" + name + ".y" + "=" + y;
        }
        return data;
    }

    /**
     * Returns the Element representing the <code>FORM</code>.
     */
    private Element getFormElement() {
        Element elem = getElement();
        while (elem != null) {
            if (elem.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.FORM) {
                return elem;
            }
            elem = elem.getParentElement();
        }
        return null;
    }

    /**
     * Iterates over the 
     * element hierarchy, extracting data from the 
     * models associated with the relevant form elements.
     * "Relevant" means the form elements that are part
     * of the same form whose element triggered the submit
     * action.
     *
     * @param buffer        the buffer that contains that data to submit
     * @param targetElement the element that triggered the 
     *                      form submission
     */
    void getFormData(StringBuffer buffer) {
        Element formE = getFormElement();
        if (formE != null) {
            ElementIterator it = new ElementIterator(formE);
            Element next;
            while ((next = it.next()) != null) {
                if (isControl(next)) {
                    String type = (String) next.getAttributes().getAttribute(HTML.Attribute.TYPE);
                    if (type != null && type.equals("submit") && next != getElement()) {
                    } else if (type == null || !type.equals("image")) {
                        loadElementDataIntoBuffer(next, buffer);
                    }
                }
            }
        }
    }

    /**
     * Loads the data
     * associated with the element into the buffer.
     * The format in which data is appended depends
     * on the type of the form element.  Essentially
     * data is loaded in name/value pairs.
     * 
     */
    private void loadElementDataIntoBuffer(Element elem, StringBuffer buffer) {
        AttributeSet attr = elem.getAttributes();
        String name = (String) attr.getAttribute(HTML.Attribute.NAME);
        if (name == null) {
            return;
        }
        String value = null;
        HTML.Tag tag = (HTML.Tag) elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (tag == HTML.Tag.INPUT) {
            value = getInputElementData(attr);
        } else if (tag == HTML.Tag.TEXTAREA) {
            value = getTextAreaData(attr);
        } else if (tag == HTML.Tag.SELECT) {
            loadSelectData(attr, buffer);
        }
        if (name != null && value != null) {
            appendBuffer(buffer, name, value);
        }
    }

    /**
     * Returns the data associated with an &lt;INPUT&gt; form
     * element.  The value of "type" attributes is
     * used to determine the type of the model associated
     * with the element and then the relevant data is
     * extracted.
     */
    private String getInputElementData(AttributeSet attr) {
        Object model = attr.getAttribute(StyleConstants.ModelAttribute);
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        String value = null;
        if (type.equals("text") || type.equals("password")) {
            Document doc = (Document) model;
            try {
                value = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                value = null;
            }
        } else if (type.equals("submit") || type.equals("hidden")) {
            value = (String) attr.getAttribute(HTML.Attribute.VALUE);
            if (value == null) {
                value = "";
            }
        } else if (type.equals("radio") || type.equals("checkbox")) {
            ButtonModel m = (ButtonModel) model;
            if (m.isSelected()) {
                value = (String) attr.getAttribute(HTML.Attribute.VALUE);
                if (value == null) {
                    value = "on";
                }
            }
        } else if (type.equals("file")) {
            Document doc = (Document) model;
            String path;
            try {
                path = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
                path = null;
            }
            if (path != null && path.length() > 0) {
                value = path;
            }
        }
        return value;
    }

    /**
     * Returns the data associated with the &lt;TEXTAREA&gt; form
     * element.  This is done by getting the text stored in the
     * Document model.
     */
    private String getTextAreaData(AttributeSet attr) {
        Document doc = (Document) attr.getAttribute(StyleConstants.ModelAttribute);
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return null;
        }
    }

    /**
     * Loads the buffer with the data associated with the Select
     * form element.  Basically, only items that are selected
     * and have their name attribute set are added to the buffer.
     */
    private void loadSelectData(AttributeSet attr, StringBuffer buffer) {
        String name = (String) attr.getAttribute(HTML.Attribute.NAME);
        if (name == null) {
            return;
        }
        Object m = attr.getAttribute(StyleConstants.ModelAttribute);
        if (m instanceof OptionListModel) {
            OptionListModel model = (OptionListModel) m;
            for (int i = 0; i < model.getSize(); i++) {
                if (model.isSelectedIndex(i)) {
                    Option option = (Option) model.getElementAt(i);
                    appendBuffer(buffer, name, option.getValue());
                }
            }
        } else if (m instanceof ComboBoxModel) {
            ComboBoxModel model = (ComboBoxModel) m;
            Option option = (Option) model.getSelectedItem();
            if (option != null) {
                appendBuffer(buffer, name, option.getValue());
            }
        }
    }

    /**
     * Appends name / value pairs into the 
     * buffer.  Both names and values are encoded using the 
     * URLEncoder.encode() method before being added to the
     * buffer.
     */
    private void appendBuffer(StringBuffer buffer, String name, String value) {
        if (buffer.length() > 0) {
            buffer.append('&');
        }
        String encodedName = URLEncoder.encode(name);
        buffer.append(encodedName);
        buffer.append('=');
        String encodedValue = URLEncoder.encode(value);
        buffer.append(encodedValue);
    }

    /**
     * Returns true if the Element <code>elem</code> represents a control.
     */
    private boolean isControl(Element elem) {
        return elem.isLeaf();
    }

    /**
     * Iterates over the element hierarchy to determine if
     * the element parameter, which is assumed to be an
     * &lt;INPUT&gt; element of type password or text, is the last
     * one of either kind, in the form to which it belongs.
     */
    boolean isLastTextOrPasswordField() {
        Element parent = getFormElement();
        Element elem = getElement();
        if (parent != null) {
            ElementIterator it = new ElementIterator(parent);
            Element next;
            boolean found = false;
            while ((next = it.next()) != null) {
                if (next == elem) {
                    found = true;
                } else if (found && isControl(next)) {
                    AttributeSet elemAttr = next.getAttributes();
                    if (HTMLDocument.matchNameAttribute(elemAttr, HTML.Tag.INPUT)) {
                        String type = (String) elemAttr.getAttribute(HTML.Attribute.TYPE);
                        if ("text".equals(type) || "password".equals(type)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Resets the form
     * to its initial state by reinitializing the models
     * associated with each form element to their initial
     * values.
     *
     * param elem the element that triggered the reset
     */
    void resetForm() {
        Element parent = getFormElement();
        if (parent != null) {
            ElementIterator it = new ElementIterator(parent);
            Element next;
            while ((next = it.next()) != null) {
                if (isControl(next)) {
                    AttributeSet elemAttr = next.getAttributes();
                    Object m = elemAttr.getAttribute(StyleConstants.ModelAttribute);
                    if (m instanceof TextAreaDocument) {
                        TextAreaDocument doc = (TextAreaDocument) m;
                        doc.reset();
                    } else if (m instanceof PlainDocument) {
                        try {
                            PlainDocument doc = (PlainDocument) m;
                            doc.remove(0, doc.getLength());
                            if (HTMLDocument.matchNameAttribute(elemAttr, HTML.Tag.INPUT)) {
                                String value = (String) elemAttr.getAttribute(HTML.Attribute.VALUE);
                                if (value != null) {
                                    doc.insertString(0, value, null);
                                }
                            }
                        } catch (BadLocationException e) {
                        }
                    } else if (m instanceof OptionListModel) {
                        OptionListModel model = (OptionListModel) m;
                        int size = model.getSize();
                        for (int i = 0; i < size; i++) {
                            model.removeIndexInterval(i, i);
                        }
                        BitSet selectionRange = model.getInitialSelection();
                        for (int i = 0; i < selectionRange.size(); i++) {
                            if (selectionRange.get(i)) {
                                model.addSelectionInterval(i, i);
                            }
                        }
                    } else if (m instanceof OptionComboBoxModel) {
                        OptionComboBoxModel model = (OptionComboBoxModel) m;
                        Option option = model.getInitialSelection();
                        if (option != null) {
                            model.setSelectedItem(option);
                        }
                    } else if (m instanceof JToggleButton.ToggleButtonModel) {
                        boolean checked = ((String) elemAttr.getAttribute(HTML.Attribute.CHECKED) != null);
                        JToggleButton.ToggleButtonModel model = (JToggleButton.ToggleButtonModel) m;
                        model.setSelected(checked);
                    }
                }
            }
        }
    }

    /**
     * BrowseFileAction is used for input type == file. When the user
     * clicks the button a JFileChooser is brought up allowing the user
     * to select a file in the file system. The resulting path to the selected
     * file is set in the text field (actually an instance of Document).
     */
    private class BrowseFileAction implements ActionListener {

        private AttributeSet attrs;

        private Document model;

        BrowseFileAction(AttributeSet attrs, Document model) {
            this.attrs = attrs;
            this.model = model;
        }

        public void actionPerformed(ActionEvent ae) {
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            if (fc.showOpenDialog(getContainer()) == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                if (selected != null) {
                    try {
                        if (model.getLength() > 0) {
                            model.remove(0, model.getLength());
                        }
                        model.insertString(0, selected.getPath(), null);
                    } catch (BadLocationException ble) {
                    }
                }
            }
        }
    }
}
