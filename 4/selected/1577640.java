package logica;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PagePanel;
import java.awt.Color;

public class visualizadorPdf extends JFrame implements ActionListener {

    public JButton nextPage = new JButton("Abrir");

    public JButton backPage = new JButton("Atrás");

    public JButton search = new JButton("Aceptar");

    public JTextField area1 = new JTextField();

    public int pagina = 0;

    public int paginas = 90;

    public int number = 0;

    public PagePanel panel = new PagePanel();

    public JPanel prueba = new JPanel();

    String ruta;

    public visualizadorPdf(String ruta) {
        setTitle("Visualizacion");
        setResizable(true);
        setLayout(null);
        setSize(750, 780);
        setVisible(true);
        this.ruta = ruta;
        nextPage.setBounds(650, 10, 80, 40);
        nextPage.addActionListener(this);
        backPage.setBounds(650, 61, 80, 40);
        backPage.addActionListener(this);
        search.setBounds(650, 161, 80, 40);
        search.addActionListener(this);
        area1.setBounds(650, 111, 80, 40);
        Dimension pantalla, cuadro;
        pantalla = Toolkit.getDefaultToolkit().getScreenSize();
        cuadro = this.getSize();
        this.setLocation(((pantalla.width - cuadro.width) / 2), (pantalla.height - cuadro.height) / 2);
        panel.setBounds(100, 0, 550, 780);
        panel.setBackground(Color.white);
        add(nextPage);
        add(backPage);
        add(panel);
        add(search);
        add(area1);
        repaint();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == nextPage) {
            pagina += 1;
            if (pagina > paginas || pagina < 1) {
                pagina = 1;
            }
            try {
                File file = new File(ruta);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel channel = raf.getChannel();
                ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                PDFFile pdffile = new PDFFile(buf);
                PDFPage page = pdffile.getPage(pagina);
                paginas = pdffile.getNumPages();
                panel.useZoomTool(false);
                panel.showPage(page);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            repaint();
            panel.repaint();
        }
        if (e.getSource() == backPage) {
            pagina -= 1;
            if (pagina > paginas || pagina < 1) {
                pagina = 1;
            }
            try {
                File file = new File(ruta);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel channel = raf.getChannel();
                ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                PDFFile pdffile = new PDFFile(buf);
                PDFPage page = pdffile.getPage(pagina);
                paginas = pdffile.getNumPages();
                panel.useZoomTool(false);
                panel.showPage(page);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            repaint();
            panel.repaint();
        }
        if (e.getSource() == search) {
            number = Integer.parseInt(area1.getText());
            pagina = number;
            if (pagina > paginas || pagina < 0) {
                pagina = 1;
            }
            try {
                File file = new File(ruta);
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                FileChannel channel = raf.getChannel();
                ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
                PDFFile pdffile = new PDFFile(buf);
                PDFPage page = pdffile.getPage(pagina);
                paginas = pdffile.getNumPages();
                panel.useZoomTool(false);
                panel.showPage(page);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            repaint();
            panel.repaint();
        }
    }

    public void keyPressed(KeyEvent el) {
        if (el.getKeyCode() == KeyEvent.VK_A) panel.removeAll();
        panel.repaint();
        repaint();
    }
}
