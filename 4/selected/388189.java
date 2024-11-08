package com.maziade.qml.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.PlainDocument;
import org.w3c.dom.Document;
import com.maziade.qml.contentnode.QMLParseException;
import com.maziade.qml.contentnode.QMLQuest;
import com.maziade.qml.contentnode.QMLStation;
import com.maziade.qml.renderer.PHPRenderer;
import com.maziade.qml.renderer.QMLRenderer;
import com.maziade.qml.tools.XMLUtils;

@SuppressWarnings("serial")
public class QMLEditor extends JDialog {

    /**
	 * Constructor
	 */
    public QMLEditor() throws IOException {
        File file = new File(".").getAbsoluteFile();
        m_fileChooser.setSelectedFile(file);
        m_qmlRenderer = new QMLRenderer();
        m_list = new JList(m_listModel);
        m_list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                onStationListValueChanged(e);
            }
        });
        JPanel editPane = new JPanel();
        editPane.setLayout(new BorderLayout());
        JPanel nameField = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameField.add(new JLabel("Station Name: "));
        m_stationIDField = new JTextField() {

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = 200;
                return d;
            }
        };
        nameField.add(m_stationIDField);
        editPane.add(nameField, BorderLayout.NORTH);
        m_editor = new JTextArea();
        m_editor.setLineWrap(false);
        m_editor.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent e) {
                onEditorTextChanged();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                onEditorTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onEditorTextChanged();
            }
        });
        m_editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, new Integer(2));
        JScrollPane scrollPane = new JScrollPane(m_editor);
        editPane.add(scrollPane, BorderLayout.CENTER);
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        m_infoText = new JLabel() {

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getMinimumSize();
                d.width = getParent().getWidth();
                return d;
            }
        };
        contentPane.add(m_infoText);
        editPane.add(contentPane, BorderLayout.SOUTH);
        JScrollPane listScrollPane = new JScrollPane(m_list);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, editPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(150);
        Dimension minimumSize = new Dimension(100, 50);
        listScrollPane.setMinimumSize(minimumSize);
        m_editor.setMinimumSize(minimumSize);
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        setJMenuBar(buildMenu());
        initializeData();
        m_stationIDField.setEditable(false);
        m_editor.setEditable(false);
        setTitle("QML Maker");
        setSize(800, 500);
        setLocationByPlatform(true);
    }

    /**
	 * Initialize with default data
	 */
    private void initializeData() {
        m_quest = new QMLQuest();
        try {
            QMLStation start = new QMLStation(m_quest, "start");
            m_listModel.clear();
            m_listModel.add(start);
            displayStation(start);
        } catch (QMLParseException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Display the specified station in the editor
	 */
    private void displayStation(QMLStation station) {
        m_currentStation = station;
        m_stationIDField.setText(m_currentStation.getStationName().getName());
        m_list.setSelectedIndex(m_listModel.getIndexOf(m_currentStation));
        m_editor.setText(m_currentStation.getXMLContent());
    }

    /**
	 * Build menu
	 */
    private JMenuBar buildMenu() {
        JMenuBar menu = new JMenuBar();
        JMenu sub = new JMenu("File");
        menu.add(sub);
        JMenuItem item = new JMenuItem("Open...");
        sub.add(item);
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                onMenuOpenFile();
            }
        });
        item = new JMenuItem("Export PHP...");
        sub.add(item);
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                onMenuExportPHP();
            }
        });
        return menu;
    }

    /**
	 * Editor text changed
	 */
    private void onEditorTextChanged() {
        IOException e = m_currentStation.validateXMLContent(m_editor.getText());
        if (e != null) {
            m_infoText.setText("<html>" + e.getCause().getMessage() + "</html>");
        } else m_infoText.setText("");
    }

    /**
	 * 
	 */
    private void onMenuOpenFile() {
        m_fileChooser.setFileFilter(new FileNameExtensionFilter("QML Files (*.xml)", "xml"));
        if (m_fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = m_fileChooser.getSelectedFile();
            Document doc;
            try {
                doc = XMLUtils.parseXMLDocument(file);
                QMLQuest quest = new QMLQuest();
                quest.loadFromFile(doc);
                m_listModel = new StationListModel(quest);
                m_list.setModel(m_listModel);
                m_quest = quest;
                m_inputFile = file;
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	 * 
	 */
    private void onMenuExportPHP() {
        if (m_inputFile == null) {
            JOptionPane.showMessageDialog(this, "You need to open a QML file first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        m_fileChooser.setFileFilter(new FileNameExtensionFilter("PHP Files (*.php)", "php"));
        if (m_fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = m_fileChooser.getSelectedFile();
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(this, "The specified file already exists.\nDo you want to overwrite it?") == JOptionPane.CANCEL_OPTION) return;
                file.delete();
            }
            try {
                Writer out = new FileWriter(file);
                PHPRenderer renderer = new PHPRenderer(out, "");
                m_quest.render(renderer);
                out.flush();
                out.close();
                JOptionPane.showMessageDialog(this, "Output complete.  The quest will need all of the PHP engine files to run.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error rendering file:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
	 * Station has changed
	 * @param event event
	 */
    private void onStationListValueChanged(ListSelectionEvent event) {
        QMLStation station = (QMLStation) m_list.getSelectedValue();
        if (station == null) {
            m_stationIDField.setText("");
            m_editor.setText("");
        } else {
            m_stationIDField.setText(station.getStationName().getName());
            try {
                m_editor.setText(m_qmlRenderer.renderQMLStationToString(station));
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error processing station.\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private QMLRenderer m_qmlRenderer;

    private QMLQuest m_quest = new QMLQuest();

    private StationListModel m_listModel = new StationListModel();

    private QMLStation m_currentStation = null;

    private JList m_list;

    private JTextArea m_editor;

    private JTextField m_stationIDField;

    private JLabel m_infoText;

    private JFileChooser m_fileChooser = new JFileChooser();

    private File m_inputFile = null;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            QMLEditor dlg = new QMLEditor();
            dlg.setModal(true);
            dlg.setVisible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
