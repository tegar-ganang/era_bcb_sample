package aplicacion.fisica.documentos;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.JobAttributes.DialogType;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.JobAttributes;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.util.Vector;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.Serializable;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFFile;

public class Documento implements Serializable, Printable {

    private static final long serialVersionUID = 1L;

    private Vector<Pagina> paginas = new Vector<Pagina>();

    private String usuario = "";

    private String rol = "";

    private String path = "";

    public Documento() {
    }

    public void insertarPagina(int index, Pagina o) {
        paginas.insertElementAt(o, index);
    }

    public void insertarPagina(int index, Image o) {
        Pagina nueva = new Pagina();
        nueva.setImagen(o);
        paginas.insertElementAt(nueva, index);
    }

    public Documento(String usu, String ro) {
        usuario = usu;
        rol = ro;
    }

    public void addPagina(Pagina pag) {
        paginas.add(pag);
    }

    public void addPagina(Image img) {
        Pagina nueva = new Pagina();
        nueva.setImagen(new ImageIcon(img));
        paginas.add(nueva);
    }

    public void addPagina(ImageIcon img) {
        Pagina nueva = new Pagina();
        nueva.setImagen(img);
        paginas.add(nueva);
    }

    public void delPagina(int index) {
        paginas.remove(index);
    }

    public void setPagina(int index, Pagina img) {
        paginas.set(index, img);
    }

    public void addAnotacion(Anotacion anot, int numPag) {
        paginas.get(numPag).addAnotacion(anot);
    }

    public void delAnotacion(int numAnotacion, int numPag) {
        paginas.get(numPag).delAnotacion(numAnotacion);
    }

    public void setAnotacion(int numPagina, int index, Anotacion anot) {
        paginas.get(numPagina).setAnotacion(index, anot);
    }

    public void setUsuario(String us) {
        usuario = us;
    }

    public void setRol(String ro) {
        rol = ro;
    }

    public int getNumeroPaginas() {
        return paginas.size();
    }

    public Pagina getPagina(int index) {
        return paginas.get(index);
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    @SuppressWarnings("unchecked")
    public static Documento openDocument(String path, String usuario, String rol) {
        int ind_ext = path.lastIndexOf(".");
        String sub = path.substring(ind_ext + 1);
        sub = sub.toLowerCase();
        Documento doc = null;
        if (sub.compareTo("pdf") == 0) {
            doc = pdf2Documento(path, usuario, rol);
        }
        if (sub.compareTo("txt") == 0) {
            doc = txt2Documento(path, usuario, rol);
        }
        String[] readFormats = ImageIO.getReaderFormatNames();
        for (int i = 0; i < readFormats.length; i++) {
            if (readFormats[i].toLowerCase().compareTo(sub) == 0) {
                doc = img2Documento(path, usuario, rol);
                break;
            }
        }
        if (doc != null) {
            File f = new File(path + ".anot");
            if (f.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(f);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    int npaginas = ois.readInt();
                    if (npaginas != doc.getNumeroPaginas()) {
                        return doc;
                    }
                    for (int i = 0; i < npaginas; i++) {
                        Vector<Anotacion> anot = (Vector<Anotacion>) ois.readObject();
                        doc.getPagina(i).setAnotaciones(anot);
                    }
                    ois.close();
                    fis.close();
                } catch (Exception ex) {
                }
            }
        }
        return doc;
    }

    private static Documento img2Documento(String path, String usuario, String rol) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image imagen = toolkit.getImage(path);
        Documento doc = new Documento(usuario, rol);
        doc.addPagina(imagen);
        doc.setPath(path);
        return doc;
    }

    private static Documento txt2Documento(String path, String usuario, String rol) {
        StringBuffer contents = new StringBuffer();
        try {
            File f = new File(path);
            BufferedReader input = new BufferedReader(new FileReader(f));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String contenido = contents.toString();
        Font font = new Font("Arial", Font.PLAIN, 16);
        BufferedImage bi = new BufferedImage(800, 1280, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.setColor(java.awt.Color.white);
        g.fillRect(0, 0, 800, 1280);
        g.setColor(java.awt.Color.black);
        g.setFont(font);
        int k = 1;
        Documento doc = new Documento(usuario, rol);
        String[] lineas = contenido.split("\n");
        String str;
        int npag = 1;
        for (int i = 0; i < lineas.length; i++) {
            str = lineas[i];
            do {
                if (str.length() < 80) {
                    g.drawString(str, 25, (8 * k + 32) + (8 * (k - 1) + 32));
                    k++;
                    str = "";
                } else {
                    String aux = str.substring(0, 79);
                    g.drawString(aux, 25, (8 * k + 32) + (8 * (k - 1) + 32));
                    k++;
                    str = str.substring(80);
                }
            } while (!str.equals(""));
            if ((8 * k + 32) + (8 * (k - 1) + 32) >= 1240) {
                doc.addPagina(bi);
                bi = new BufferedImage(800, 1280, BufferedImage.TYPE_INT_RGB);
                g = bi.getGraphics();
                g.setColor(java.awt.Color.white);
                g.fillRect(0, 0, 800, 1280);
                g.setColor(java.awt.Color.black);
                g.setFont(font);
                k = 1;
                npag++;
                System.gc();
            }
        }
        if (doc.getNumeroPaginas() != npag) doc.addPagina(bi);
        doc.setPath(path);
        return doc;
    }

    private static Documento pdf2Documento(String path, String usuario, String rol) {
        File file = new File(path);
        RandomAccessFile raf;
        Documento res = new Documento(usuario, rol);
        try {
            raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdffile = new PDFFile(buf);
            int num = pdffile.getNumPages();
            for (int i = 1; i <= num; i++) {
                PDFPage page = pdffile.getPage(i);
                int width = (int) page.getBBox().getWidth();
                int height = (int) page.getBBox().getHeight();
                Rectangle rect = new Rectangle(0, 0, width, height);
                int rotation = page.getRotation();
                Rectangle rect1 = rect;
                if (rotation == 90 || rotation == 270) rect1 = new Rectangle(0, 0, rect.height, rect.width);
                BufferedImage img = (BufferedImage) page.getImage((int) (((float) rect.width * 1.1) + 0.5), (int) (((float) rect.height * 1.1) + 0.5), rect1, null, true, true);
                res.addPagina(new ImageIcon(img));
                System.gc();
            }
            res.setPath(path);
            return res;
        } catch (FileNotFoundException e1) {
            System.err.println(e1.getLocalizedMessage());
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
        return null;
    }

    public static boolean saveDocument(Documento doc, String path_original) {
        File f = new File(path_original + ".anot");
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeInt(doc.getNumeroPaginas());
            for (int i = 0; i < doc.getNumeroPaginas(); i++) oos.writeObject(doc.getPagina(i).getAnotaciones());
            oos.close();
            fos.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
	 * @return the path
	 */
    public String getPath() {
        return path;
    }

    /**
	 * @param path the path to set
	 */
    public void setPath(String path) {
        this.path = path;
    }

    public void imprimir() {
        Impresora p = new Impresora();
        p.inicializar(this);
    }

    private class Impresora {

        int x;

        PrinterJob pjob;

        Graphics pg;

        Font grande, peque;

        /**
		 * Constructor
		 *
		 */
        public Impresora() {
        }

        /**
		 * Inicializa la impresora
		 * @param nombredoc nombre del documento a imprimir
		 * @return true si la inicializaci�n a sido exitosa false en caso contrario
		 */
        public boolean inicializar(Documento d) {
            x = 0;
            grande = new Font("Courier New", Font.PLAIN, 16);
            peque = new Font("Courier New", Font.PLAIN, 12);
            Frame f = new Frame("");
            f.pack();
            JobAttributes atributosImpresion = new JobAttributes();
            atributosImpresion.setCopies(1);
            atributosImpresion.setDialog(DialogType.NATIVE);
            pjob = PrinterJob.getPrinterJob();
            pjob.setPrintable(d);
            if (pjob.printDialog()) {
                try {
                    pjob.print();
                    return true;
                } catch (PrinterException e) {
                    System.out.println(e);
                }
            } else return false;
            return false;
        }
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex < paginas.size()) {
            Image im = paginas.get(pageIndex).getImagen();
            Pagina p = paginas.get(pageIndex);
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(im, 0, 0, im.getWidth(null), im.getHeight(null), null);
            Vector<Anotacion> v = p.getAnotaciones();
            int na = v.size();
            for (int i = 0; i < na; ++i) {
                graphics.setColor(v.get(i).getContenido().getColor());
                v.get(i).getContenido().dibujar(graphics, 1);
            }
            System.out.println("Solicitando p�gina " + pageIndex);
            return PAGE_EXISTS;
        } else return NO_SUCH_PAGE;
    }
}
