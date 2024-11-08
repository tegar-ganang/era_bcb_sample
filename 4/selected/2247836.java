package es.unizar.cps.tecnodiscap.gui;

import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import es.unizar.cps.tecnodiscap.data.Imagen;
import es.unizar.cps.tecnodiscap.gui.NombreUsuario;
import es.unizar.cps.tecnodiscap.i18n.Messages;
import es.unizar.cps.tecnodiscap.util.ImageFilter;
import es.unizar.cps.tecnodiscap.util.ImagePreview;
import es.unizar.cps.tecnodiscap.util.SoundFilter;
import es.unizar.cps.tecnodiscap.data.Imagen;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class AdministracionResorces extends JDialog {

    private static final long serialVersionUID = 1L;

    private GUI gui;

    private Imagen imagen;

    private final String rutaDatos = "data/";

    private JPanel jContentPane = null;

    private JPanel panelSuperior = null;

    private JPanel panelInferior = null;

    private JPanel panelCentral = null;

    private JButton buttonOK = null;

    private JComboBox comboBoxImagenes = null;

    private JLabel labelNombre = null;

    private JTextField textFieldNombre = null;

    private JLabel labelImagen = null;

    private JLabel labelSonido = null;

    private JPanel panelDetalles = null;

    private JLabel labelImagenPreview = null;

    private JButton buttonImagen = null;

    private JButton buttonSonido = null;

    private JButton buttonEliminarEntorno = null;

    private JButton buttonNuevo = null;

    /**
	 * @param owner
	 */
    public AdministracionResorces(Frame owner) {
        super(owner, true);
        gui = ((GUI) owner);
        this.dialogInit();
        initialize();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(827, 662);
        this.setTitle(Messages.getString("gui.AdministracionResorces.0"));
        this.setContentPane(getJContentPane());
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        actualizaValores();
        this.setVisible(true);
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getPanelSuperior(), BorderLayout.NORTH);
            jContentPane.add(getPanelInferior(), BorderLayout.SOUTH);
            jContentPane.add(getPanelCentral(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    /**
	 * This method initializes panelSuperior
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getPanelSuperior() {
        if (panelSuperior == null) {
            panelSuperior = new JPanel();
            panelSuperior.setLayout(new FlowLayout());
            panelSuperior.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            panelSuperior.add(getComboBoxImagenes(), null);
            panelSuperior.add(getButtonNuevo(), null);
            panelSuperior.add(getButtonEliminarEntorno(), null);
        }
        return panelSuperior;
    }

    /**
	 * This method initializes panelInferior
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getPanelInferior() {
        if (panelInferior == null) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = -1;
            gridBagConstraints1.insets = new Insets(50, 0, 50, 0);
            gridBagConstraints1.gridy = -1;
            labelNombre = new JLabel();
            labelNombre.setText(Messages.getString("gui.AdministracionResorces.1"));
            panelInferior = new JPanel();
            panelInferior.setLayout(new GridBagLayout());
            panelInferior.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            panelInferior.add(getButtonOK(), gridBagConstraints1);
        }
        return panelInferior;
    }

    /**
	 * This method initializes panelCentral
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getPanelCentral() {
        if (panelCentral == null) {
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.insets = new Insets(0, 50, 0, 50);
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.gridx = 1;
            gridBagConstraints9.insets = new Insets(0, 0, 0, 20);
            gridBagConstraints9.gridy = 0;
            labelImagenPreview = new JLabel();
            labelImagenPreview.setText("");
            labelSonido = new JLabel();
            labelSonido.setText(Messages.getString("gui.AdministracionResorces.3"));
            labelImagen = new JLabel();
            labelImagen.setText(Messages.getString("gui.AdministracionResorces.4"));
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = -1;
            gridBagConstraints3.gridy = -1;
            panelCentral = new JPanel();
            panelCentral.setLayout(new GridBagLayout());
            panelCentral.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            panelCentral.add(getPanelDetalles(), gridBagConstraints10);
            panelCentral.add(labelImagenPreview, gridBagConstraints9);
        }
        return panelCentral;
    }

    /**
	 * This method initializes buttonCancelar
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getButtonOK() {
        if (buttonOK == null) {
            buttonOK = new JButton();
            buttonOK.setText(Messages.getString("gui.AdministracionResorces.5"));
            buttonOK.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    gui.getAudio().paraAudio();
                    dispose();
                }
            });
        }
        return buttonOK;
    }

    /**
	 * This method initializes comboBoxImagenes
	 * 
	 * @return javax.swing.JComboBox
	 */
    private JComboBox getComboBoxImagenes() {
        if (comboBoxImagenes == null) {
            comboBoxImagenes = new JComboBox(gui.procesadorXML.getImagenesArray2());
            comboBoxImagenes.setSelectedIndex(0);
            imagen = (Imagen) comboBoxImagenes.getSelectedItem();
            comboBoxImagenes.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                        imagen = (Imagen) comboBoxImagenes.getSelectedItem();
                        actualizaValores();
                    }
                }
            });
        }
        return comboBoxImagenes;
    }

    /**
	 * This method initializes textFieldNombre
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getTextFieldNombre() {
        if (textFieldNombre == null) {
            textFieldNombre = new JTextField();
            textFieldNombre.setEditable(false);
        }
        return textFieldNombre;
    }

    /**
	 * This method initializes panelDetalles
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getPanelDetalles() {
        if (panelDetalles == null) {
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridx = 1;
            gridBagConstraints11.insets = new Insets(50, 0, 0, 0);
            gridBagConstraints11.gridy = 3;
            GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
            gridBagConstraints8.gridx = 1;
            gridBagConstraints8.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints8.gridy = 2;
            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 1;
            gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.gridy = 1;
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.gridx = 0;
            gridBagConstraints7.anchor = GridBagConstraints.WEST;
            gridBagConstraints7.gridy = 2;
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            gridBagConstraints5.gridx = 0;
            gridBagConstraints5.anchor = GridBagConstraints.WEST;
            gridBagConstraints5.gridy = 1;
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.anchor = GridBagConstraints.WEST;
            gridBagConstraints3.insets = new Insets(20, 0, 20, 20);
            gridBagConstraints3.gridy = 0;
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints4.gridy = 0;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.insets = new Insets(20, 0, 20, 0);
            gridBagConstraints4.gridx = 1;
            panelDetalles = new JPanel();
            panelDetalles.setLayout(new GridBagLayout());
            panelDetalles.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            panelDetalles.add(labelNombre, gridBagConstraints3);
            panelDetalles.add(getTextFieldNombre(), gridBagConstraints4);
            panelDetalles.add(labelImagen, gridBagConstraints5);
            panelDetalles.add(labelSonido, gridBagConstraints7);
            panelDetalles.add(getButtonImagen(), gridBagConstraints6);
            panelDetalles.add(getButtonSonido(), gridBagConstraints8);
        }
        return panelDetalles;
    }

    /**
	 * This method initializes buttonImagen
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getButtonImagen() {
        if (buttonImagen == null) {
            buttonImagen = new JButton();
            buttonImagen.setText(Messages.getString("gui.AdministracionResorces.6"));
            buttonImagen.setIcon(new ImageIcon("data/icons/view_sidetree.png"));
            buttonImagen.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.addChoosableFileFilter(new ImageFilter());
                    fc.setAccessory(new ImagePreview(fc));
                    int returnVal = fc.showDialog(AdministracionResorces.this, Messages.getString("gui.AdministracionResorces.8"));
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        String rutaGlobal = System.getProperty("user.dir") + "/" + rutaDatos + "imagenes/" + file.getName();
                        String rutaRelativa = rutaDatos + "imagenes/" + file.getName();
                        try {
                            FileInputStream fis = new FileInputStream(file);
                            FileOutputStream fos = new FileOutputStream(rutaGlobal, true);
                            FileChannel canalFuente = fis.getChannel();
                            FileChannel canalDestino = fos.getChannel();
                            canalFuente.transferTo(0, canalFuente.size(), canalDestino);
                            fis.close();
                            fos.close();
                            imagen.setImagenURL(rutaRelativa);
                            gui.getEntrenamientoIzquierdaLabel().setIcon(gui.getProcesadorDatos().escalaImageIcon(((Imagen) gui.getComboBoxImagenesIzquierda().getSelectedItem()).getImagenURL()));
                            gui.getEntrenamientoDerechaLabel().setIcon(gui.getProcesadorDatos().escalaImageIcon(((Imagen) gui.getComboBoxImagenesDerecha().getSelectedItem()).getImagenURL()));
                            buttonImagen.setIcon(new ImageIcon("data/icons/view_sidetreeOK.png"));
                            labelImagenPreview.setIcon(gui.getProcesadorDatos().escalaImageIcon(imagen.getImagenURL()));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                    }
                }
            });
        }
        return buttonImagen;
    }

    /**
	 * This method initializes buttonSonido
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getButtonSonido() {
        if (buttonSonido == null) {
            buttonSonido = new JButton();
            buttonSonido.setText(Messages.getString("gui.AdministracionResorces.15"));
            buttonSonido.setIcon(new ImageIcon("data/icons/view_sidetree.png"));
            buttonSonido.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.addChoosableFileFilter(new SoundFilter());
                    int returnVal = fc.showDialog(AdministracionResorces.this, Messages.getString("gui.AdministracionResorces.17"));
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        String rutaGlobal = System.getProperty("user.dir") + "/" + rutaDatos + "sonidos/" + file.getName();
                        String rutaRelativa = rutaDatos + "sonidos/" + file.getName();
                        try {
                            FileInputStream fis = new FileInputStream(file);
                            FileOutputStream fos = new FileOutputStream(rutaGlobal, true);
                            FileChannel canalFuente = fis.getChannel();
                            FileChannel canalDestino = fos.getChannel();
                            canalFuente.transferTo(0, canalFuente.size(), canalDestino);
                            fis.close();
                            fos.close();
                            imagen.setSonidoURL(rutaRelativa);
                            System.out.println(rutaGlobal + " " + rutaRelativa);
                            buttonSonido.setIcon(new ImageIcon("data/icons/view_sidetreeOK.png"));
                            gui.getAudio().reproduceAudio(imagen);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                    }
                }
            });
        }
        return buttonSonido;
    }

    /**
	 * This method initializes buttonEliminarEntorno
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getButtonEliminarEntorno() {
        if (buttonEliminarEntorno == null) {
            buttonEliminarEntorno = new JButton();
            buttonEliminarEntorno.setText(Messages.getString("gui.AdministracionResorces.25"));
            buttonEliminarEntorno.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (comboBoxImagenes.getSelectedIndex() != -1) {
                        if (comboBoxImagenes.getItemCount() != 1) {
                            gui.procesadorXML.removeImagen(imagen);
                            gui.getComboBoxImagenesDerecha().removeItem(imagen);
                            gui.getComboBoxImagenesIzquierda().removeItem(imagen);
                            comboBoxImagenes.removeItem(imagen);
                        }
                    }
                }
            });
        }
        return buttonEliminarEntorno;
    }

    private void actualizaValores() {
        textFieldNombre.setText(imagen.getNombre());
        if (!imagen.getImagenURL().equals("")) {
            buttonImagen.setIcon(new ImageIcon("data/icons/view_sidetreeOK.png"));
            buttonSonido.setIcon(new ImageIcon("data/icons/view_sidetreeOK.png"));
            gui.getAudio().reproduceAudio(imagen);
            labelImagenPreview.setIcon(gui.getProcesadorDatos().escalaImageIcon(imagen.getImagenURL()));
        } else {
            buttonImagen.setIcon(new ImageIcon("data/icons/view_sidetree.png"));
            buttonSonido.setIcon(new ImageIcon("data/icons/view_sidetree.png"));
            gui.getAudio().paraAudio();
            labelImagenPreview.setIcon(new ImageIcon("data/imagenes/foto_no_disponible.jpg"));
        }
    }

    /**
	 * This method initializes buttonNuevo
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getButtonNuevo() {
        if (buttonNuevo == null) {
            buttonNuevo = new JButton();
            buttonNuevo.setText(Messages.getString("gui.AdministracionResorces.32"));
            buttonNuevo.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (dialogoAdminUsuarios()) {
                        actualizaValores();
                        gui.procesadorXML.addImagen(imagen);
                        gui.getComboBoxImagenesDerecha().addItem(imagen);
                        gui.getComboBoxImagenesIzquierda().addItem(imagen);
                        comboBoxImagenes.addItem(imagen);
                        comboBoxImagenes.setSelectedItem(imagen);
                    }
                }
            });
        }
        return buttonNuevo;
    }

    private boolean dialogoAdminUsuarios() {
        NombreUsuario dialog = new NombreUsuario(this);
        if (dialog.isAceptar()) {
            if (gui.procesadorXML.getImagen(dialog.getNombre()) == null) {
                imagen = new Imagen();
                imagen.setNombre(dialog.getNombre());
            } else {
                JOptionPane.showMessageDialog(this, "No se puede repetir el nombre: " + dialog.getNombre(), "El nombre ya existe", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return dialog.isAceptar();
    }
}
