package gui;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import java.awt.Desktop;
import java.awt.BorderLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PagePanel;
import clases.Cliente;
import clases.ListadoPdf;
import clases.Pdf;
import filtos.FiltroPdf;
import modelo.ModeloImagenes;

/**
 * @author ancabi
 *
 */
public class PanelPdf extends JPanel {

    private static final long serialVersionUID = 1L;

    private JPanel panelSup = null;

    private JPanel panelTitulo = null;

    private JLabel lblTitulo = null;

    private JPanel panelBtn = null;

    private JButton btnAdd = null;

    private JButton btnDel = null;

    private JPanel panelPrev = null;

    private JScrollPane scrollTabla = null;

    private JTable tabla = null;

    private ModeloImagenes modelo;

    private Cliente clienteActual;

    private ListadoPdf listado;

    private JFileChooser fc;

    private SimpleDateFormat formateador = new SimpleDateFormat("dd/MM/yyyy H:mm");

    private RandomAccessFile raf;

    /**
	 * This is the default constructor
	 */
    public PanelPdf() {
        super();
        initialize();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
    private void initialize() {
        this.setSize(723, 444);
        this.setLayout(new BorderLayout());
        this.add(getPanelSup(), BorderLayout.NORTH);
        this.add(getPanelPrev(), BorderLayout.CENTER);
        this.add(getScrollTabla(), BorderLayout.WEST);
    }

    /**
	 * This method initializes panelSup	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanelSup() {
        if (panelSup == null) {
            panelSup = new JPanel();
            panelSup.setLayout(new BorderLayout());
            panelSup.add(getPanelTitulo(), BorderLayout.WEST);
            panelSup.add(getPanelBtn(), BorderLayout.EAST);
        }
        return panelSup;
    }

    /**
	 * This method initializes panelTitulo	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanelTitulo() {
        if (panelTitulo == null) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            lblTitulo = new JLabel();
            lblTitulo.setText("PDF's del cliente");
            lblTitulo.setFont(new Font("Verdana", Font.BOLD, 18));
            panelTitulo = new JPanel();
            panelTitulo.setLayout(new GridBagLayout());
            panelTitulo.add(lblTitulo, gridBagConstraints);
        }
        return panelTitulo;
    }

    /**
	 * This method initializes panelBtn	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanelBtn() {
        if (panelBtn == null) {
            panelBtn = new JPanel();
            panelBtn.setLayout(new FlowLayout());
            panelBtn.add(getBtnAdd(), null);
            panelBtn.add(getBtnDel(), null);
        }
        return panelBtn;
    }

    /**
	 * This method initializes btnAdd	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnAdd() {
        if (btnAdd == null) {
            btnAdd = new JButton();
            btnAdd.setIcon(new ImageIcon(getClass().getResource("/img/add.png")));
            btnAdd.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    File[] pdf = getFiles();
                    File carpeta = new File(clienteActual.getIdCliente() + clienteActual.getNomSinEspacios() + clienteActual.getApelSinEspacios());
                    if (!carpeta.exists()) {
                        carpeta.mkdir();
                        carpeta.setExecutable(true);
                    }
                    if (pdf != null) {
                        for (int x = 0; x < pdf.length; x++) {
                            try {
                                listado.addPdf(new File(pdf[x].getName()), pdf[x].lastModified());
                                InputStream in = new FileInputStream(pdf[x].getAbsoluteFile());
                                OutputStream out = new FileOutputStream(new File(carpeta.toString() + "/" + pdf[x].getName()));
                                byte[] buf = new byte[5120];
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                                in.close();
                                out.close();
                                cargarPdf();
                            } catch (FileNotFoundException e1) {
                                JOptionPane.showMessageDialog(null, e1.getMessage());
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(null, e1.getMessage());
                            }
                        }
                    }
                }
            });
        }
        return btnAdd;
    }

    /**
	 * This method initializes btnDel	
	 * 	
	 * @return javax.swing.JButton	
	 */
    private JButton getBtnDel() {
        if (btnDel == null) {
            btnDel = new JButton();
            btnDel.setIcon(new ImageIcon(getClass().getResource("/img/delete.png")));
            btnDel.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int index = tabla.getSelectedRow();
                    index = tabla.convertRowIndexToModel(index);
                    if (index >= 0) {
                        if (index > 0) {
                            previsualizarPdf(index - 1);
                        } else {
                            previsualizarPdf(1);
                        }
                        listado.removeElementAt(index);
                        Pdf p = listado.getPdf(index);
                        File f = new File(clienteActual.getIdCliente() + clienteActual.getNomSinEspacios() + clienteActual.getApelSinEspacios() + "/" + p.getName());
                        f.delete();
                        cargarPdf();
                    }
                }
            });
        }
        return btnDel;
    }

    /**
	 * This method initializes panelPrev	
	 * 	
	 * @return javax.swing.JPanel	
	 */
    private JPanel getPanelPrev() {
        if (panelPrev == null) {
            panelPrev = new PagePanel();
            panelPrev.setLayout(new GridBagLayout());
        }
        return panelPrev;
    }

    /**
	 * This method initializes scrollTabla	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
    private JScrollPane getScrollTabla() {
        if (scrollTabla == null) {
            scrollTabla = new JScrollPane();
            scrollTabla.setViewportView(getTabla());
        }
        return scrollTabla;
    }

    /**
	 * This method initializes tabla	
	 * 	
	 * @return javax.swing.JTable	
	 */
    private JTable getTabla() {
        if (tabla == null) {
            modelo = new ModeloImagenes();
            Vector<String> head = new Vector<String>();
            head.add("Nombre");
            head.add("Ultima modificacion");
            modelo.setHeader(head);
            tabla = new JTable(modelo);
            tabla.setAutoCreateRowSorter(true);
            tabla.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        int index = tabla.getSelectedRow();
                        index = tabla.convertRowIndexToModel(index);
                        Pdf p = listado.getPdf(index);
                        File f = new File(clienteActual.getIdCliente() + clienteActual.getNomSinEspacios() + clienteActual.getApelSinEspacios() + "/" + p.getName());
                        if (System.getProperty("os.name").equals("Linux") && !Desktop.isDesktopSupported()) {
                            Runtime obj = Runtime.getRuntime();
                            try {
                                obj.exec("okular " + f.toString());
                                cargarPdf();
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(null, e1.getMessage());
                            }
                        } else {
                            try {
                                Desktop.getDesktop().open(f);
                                cargarPdf();
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(null, e1.getMessage());
                            }
                        }
                    } else {
                        int index = tabla.getSelectedRow();
                        index = tabla.convertRowIndexToModel(index);
                        previsualizarPdf(index);
                    }
                }
            });
        }
        return tabla;
    }

    public void setCliente(Cliente c) {
        clienteActual = c;
        listado = new ListadoPdf(c.getIdCliente());
        cargarPdf();
    }

    public void cargarPdf() {
        listado.cargarPdf();
        Vector info;
        Vector data = new Vector();
        Date d;
        String fecha;
        for (int x = 0; x < listado.getSize(); x++) {
            Pdf p = listado.getPdf(x);
            info = new Vector();
            info.add(p.getName());
            d = new Date(p.getLastModified());
            fecha = formateador.format(d);
            info.add(fecha);
            data.add(info);
        }
        modelo.setData(data);
    }

    private File[] getFiles() {
        fc = new JFileChooser();
        fc.addChoosableFileFilter(new FiltroPdf());
        fc.setMultiSelectionEnabled(true);
        int result = fc.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFiles();
        }
        return null;
    }

    private void previsualizarPdf(int index) {
        Pdf p = listado.getPdf(index);
        File f = new File(clienteActual.getIdCliente() + clienteActual.getNomSinEspacios() + clienteActual.getApelSinEspacios() + "/" + p.getName());
        try {
            if (raf != null) {
                raf.close();
            }
            raf = new RandomAccessFile(f, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = new PDFFile(buf);
            PDFPage page = pdffile.getPage(0);
            ((PagePanel) panelPrev).showPage(page);
        } catch (FileNotFoundException e1) {
            JOptionPane.showMessageDialog(null, e1.getMessage());
        } catch (IOException e2) {
            JOptionPane.showMessageDialog(null, e2.getMessage());
        }
    }
}
