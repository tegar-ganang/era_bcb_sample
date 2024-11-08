package gui;

import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import data.Imagen;
import data.Usuario;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import java.awt.GridBagConstraints;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JSplitPane;
import javax.swing.ImageIcon;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.BoxLayout;
import procesadores.ImageFileView;
import procesadores.ImageFilter;
import procesadores.ImagePreview;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Resorces extends JDialog {

    private static final long serialVersionUID = 1L;

    private GUI gui;

    private Imagen imagen;

    private JPanel jContentPane = null;

    private JPanel panelSuperior = null;

    private JPanel panelInferior = null;

    private JPanel panelCentral = null;

    private JButton buttonCancelar = null;

    private JComboBox comboBoxImagenes = null;

    private JLabel labelNombre = null;

    private JTextField textFieldNombre = null;

    private JLabel labelImagen = null;

    private JLabel labelSonido = null;

    private JPanel panelDetalles = null;

    private JLabel labelImagenPreview = null;

    private JButton buttonImagen = null;

    private JButton buttonSonido = null;

    private JButton buttonSalvar = null;

    private JButton buttonEliminarEntorno = null;

    /**
	 * @param owner
	 */
    public Resorces(Frame owner) {
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
        this.setTitle("Administración de Recursos");
        this.setContentPane(getJContentPane());
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
            labelNombre.setText("1. Nombre:");
            panelInferior = new JPanel();
            panelInferior.setLayout(new GridBagLayout());
            panelInferior.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
            panelInferior.add(getButtonCancelar(), gridBagConstraints1);
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
            labelSonido.setText("3. Sonido");
            labelImagen = new JLabel();
            labelImagen.setText("2. Imagen");
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
    private JButton getButtonCancelar() {
        if (buttonCancelar == null) {
            buttonCancelar = new JButton();
            buttonCancelar.setText("OK");
            buttonCancelar.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    dispose();
                }
            });
        }
        return buttonCancelar;
    }

    /**
	 * This method initializes comboBoxImagenes	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
    private JComboBox getComboBoxImagenes() {
        if (comboBoxImagenes == null) {
            comboBoxImagenes = new JComboBox(gui.procesadorXML.getImagenesArray());
            comboBoxImagenes.setName("comboBoxImagenes");
            comboBoxImagenes.setSelectedIndex(-1);
            comboBoxImagenes.addItemListener(new java.awt.event.ItemListener() {

                public void itemStateChanged(java.awt.event.ItemEvent e) {
                    if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                        actualizaValores((String) comboBoxImagenes.getSelectedItem());
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
            textFieldNombre.addKeyListener(new java.awt.event.KeyAdapter() {

                public void keyTyped(java.awt.event.KeyEvent e) {
                    imagen = new Imagen();
                    imagen.setNombre(textFieldNombre.getText());
                    buttonImagen.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetree.png")));
                    buttonSonido.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetree.png")));
                }
            });
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
            panelDetalles.add(getButtonSalvar(), gridBagConstraints11);
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
            buttonImagen.setText("Cargar Imagen");
            buttonImagen.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetree.png")));
            buttonImagen.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.addChoosableFileFilter(new ImageFilter());
                    fc.setFileView(new ImageFileView());
                    fc.setAccessory(new ImagePreview(fc));
                    int returnVal = fc.showDialog(Resorces.this, "Seleccione una imagen");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        String rutaGlobal = System.getProperty("user.dir") + file.separator + "data" + file.separator + "imagenes" + file.separator + file.getName();
                        String rutaRelativa = "data" + file.separator + "imagenes" + file.separator + file.getName();
                        try {
                            FileInputStream fis = new FileInputStream(file);
                            FileOutputStream fos = new FileOutputStream(rutaGlobal, true);
                            FileChannel canalFuente = fis.getChannel();
                            FileChannel canalDestino = fos.getChannel();
                            canalFuente.transferTo(0, canalFuente.size(), canalDestino);
                            fis.close();
                            fos.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        imagen.setImagenURL(rutaRelativa);
                        System.out.println(rutaGlobal + " " + rutaRelativa);
                        buttonImagen.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetreeOK.png")));
                        labelImagenPreview.setIcon(gui.procesadorDatos.escalaImageIcon(imagen.getImagenURL()));
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
            buttonSonido.setText("Cargar Sonido");
            buttonSonido.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetree.png")));
            buttonSonido.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("actionPerformed()");
                }
            });
        }
        return buttonSonido;
    }

    /**
	 * This method initializes buttonSalvar	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getButtonSalvar() {
        if (buttonSalvar == null) {
            buttonSalvar = new JButton();
            buttonSalvar.setText("4. ¡SALVAR!");
            buttonSalvar.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (imagen.getNombre().equals("") && imagen.getImagenURL().equals("") && imagen.getSonidoURL().equals("")) {
                        gui.procesadorXML.addImagen(imagen);
                    }
                }
            });
        }
        return buttonSalvar;
    }

    /**
	 * This method initializes buttonEliminarEntorno	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getButtonEliminarEntorno() {
        if (buttonEliminarEntorno == null) {
            buttonEliminarEntorno = new JButton();
            buttonEliminarEntorno.setText("Eliminar");
            buttonEliminarEntorno.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (comboBoxImagenes.getSelectedIndex() != -1) {
                        gui.procesadorXML.removeImagen(imagen);
                        comboBoxImagenes.removeItemAt(comboBoxImagenes.getSelectedIndex());
                        comboBoxImagenes.setSelectedIndex(0);
                    }
                }
            });
        }
        return buttonEliminarEntorno;
    }

    private void actualizaValores(String imagenString) {
        imagen = gui.procesadorXML.getImagen(imagenString);
        textFieldNombre.setText(imagen.getNombre());
        buttonImagen.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetreeOK.png")));
        buttonSonido.setIcon(new ImageIcon(getClass().getResource("/data/icons/view_sidetreeOK.png")));
        labelImagenPreview.setIcon(gui.procesadorDatos.escalaImageIcon(imagen.getImagenURL()));
    }
}
