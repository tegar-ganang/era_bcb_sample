package saci.reptil.writer;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.lowagie.text.pdf.PdfContentByte;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PrinterException;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import saci.reptil.Label;
import saci.reptil.ReportManager;
import saci.reptil.ReportPanel;

/**
 * Classe que representa a exporta��o do relat�rio para PDF.
 * <p>
 * <b>Utiliza a biblioteca iText</b>
 *
 * @author  saci
 */
public class PdfExporter {

    private OutputStream out;

    private ReportManager reportManager;

    private PdfContentByte pdfContentByte;

    private boolean rotate = false;

    /**
     * Cria uma nova inst�ncia da classe.
     * <p>
     * Quando chamado este construtor mostra um di�logo de "salvar" onde o
     * usu�rio dever� escolher um diret�rio e o nome do arquivo a ser salvo
     *
     * @param reportManager o gerenciador do relat�rio a ser exportado
     */
    public PdfExporter(ReportManager reportManager) throws UserCancelException, IOException {
        if (reportManager == null) {
            throw new NullPointerException();
        }
        this.reportManager = reportManager;
        while (!showDialog()) {
            showDialog();
        }
    }

    /**
     * Mostra um di�logo de salvar arquivo
     */
    private boolean showDialog() throws UserCancelException, IOException {
        String file = null;
        JFileChooser f = new JFileChooser();
        f.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                if (f.getPath().toUpperCase().endsWith(".PDF") || f.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            public String getDescription() {
                return "PDF files (*.pdf)";
            }
        });
        f.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (f.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            if (!f.getSelectedFile().toString().toUpperCase().endsWith(".PDF")) {
                f.setSelectedFile(new File(f.getSelectedFile().toString() + ".pdf"));
            }
            if (!f.getSelectedFile().exists() || (f.getSelectedFile().exists() && JOptionPane.showConfirmDialog(null, "The file already exists, overwrite?", "Already exists", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                file = f.getSelectedFile().toString();
            }
        } else {
            throw new UserCancelException();
        }
        this.out = new FileOutputStream(file);
        return file != null;
    }

    /**
     * Cria uma nova inst�ncia da classe
     *
     * @param reportManager o gerenciador do relat�rio a ser exportado
     * @param file o nome do arquivo
     */
    public PdfExporter(ReportManager reportManager, String file) throws IOException {
        if (reportManager == null || file == null) {
            throw new NullPointerException();
        }
        this.reportManager = reportManager;
        this.out = new FileOutputStream(file);
    }

    /**
     * Cria uma nova inst�ncia da classe
     *
     * @param reportManager o gerenciador do relat�rio a ser exportado
     * @param out o lugar onde deve ser feita a saida dos dados
     */
    public PdfExporter(ReportManager reportManager, OutputStream out) throws IOException {
        if (reportManager == null || out == null) {
            throw new NullPointerException();
        }
        this.reportManager = reportManager;
        this.out = out;
    }

    public void setRotateable(boolean rotate) {
        this.rotate = rotate;
    }

    public boolean getRotateable(boolean rotate) {
        return this.rotate;
    }

    /**
     * Exporta o relat�rio para o arquivo previamente escolhido
     */
    public void save() throws IOException, PrinterException {
        try {
            float width = (float) reportManager.getPaper().getWidth();
            float height = (float) reportManager.getPaper().getHeight();
            float x = (float) reportManager.getPaper().getImageableX();
            float y = (float) reportManager.getPaper().getImageableY();
            if (!rotate && reportManager.getOrientation() == ReportManager.LANDSCAPE) {
                width = (float) reportManager.getPaper().getHeight();
                height = (float) reportManager.getPaper().getWidth();
                x = (float) reportManager.getPaper().getImageableY();
                y = (float) reportManager.getPaper().getImageableX();
            }
            Document doc = new Document(new Rectangle(width, height), x, y, width - x, height - y);
            PdfWriter writer = PdfWriter.getInstance(doc, this.out);
            doc.open();
            pdfContentByte = writer.getDirectContent();
            Book book = reportManager.getBook();
            Graphics2D g2;
            setPDFFont(reportManager.getTitleBand());
            setPDFFont(reportManager.getHeaderBand());
            if (reportManager.getGroup() != null) {
                setPDFFont(reportManager.getGroup().getHeaderBand());
                setPDFFont(reportManager.getGroup().getFooterBand());
            }
            setPDFFont(reportManager.getDetailBand());
            setPDFFont(reportManager.getSummaryBand());
            setPDFFont(reportManager.getFooterBand());
            DefaultFontMapper mapper = new DefaultFontMapper();
            if (reportManager.getFontDirectory() != null) {
                mapper.insertDirectory(reportManager.getFontDirectory());
            }
            for (int i = 0; i < book.getNumberOfPages(); i++) {
                doc.newPage();
                g2 = pdfContentByte.createGraphics(width, height, mapper);
                if (rotate && reportManager.getOrientation() == ReportManager.LANDSCAPE) {
                    g2.rotate(Math.toRadians(270), height > width ? height / 2 : width / 2, height > width ? height / 2 : width / 2);
                }
                reportManager.paint(g2, i);
                g2.dispose();
            }
            doc.close();
            setNormalFont(reportManager.getTitleBand());
            setNormalFont(reportManager.getHeaderBand());
            if (reportManager.getGroup() != null) {
                setNormalFont(reportManager.getGroup().getHeaderBand());
                setNormalFont(reportManager.getGroup().getFooterBand());
            }
            setNormalFont(reportManager.getDetailBand());
            setNormalFont(reportManager.getSummaryBand());
            setNormalFont(reportManager.getFooterBand());
        } catch (DocumentException de) {
            throw new IOException(de.toString());
        }
    }

    private void setPDFFont(ReportPanel p) throws DocumentException, IOException {
        if (p != null) {
            for (int i = 0; i < p.getComponentCount(); i++) {
                if (p.getComponent(i) instanceof ReportPanel) {
                    setPDFFont((ReportPanel) p.getComponent(i));
                } else if (p.getComponent(i) instanceof Label) {
                    Label lbl = (Label) p.getComponent(i);
                    java.awt.Font f = lbl.getFont();
                    lbl.setFont(lbl.getPDFFont());
                    lbl.setPDFFont(f);
                }
            }
        }
    }

    private void setNormalFont(ReportPanel p) throws DocumentException, IOException {
        if (p != null) {
            for (int i = 0; i < p.getComponentCount(); i++) {
                if (p.getComponent(i) instanceof ReportPanel) {
                    setPDFFont((ReportPanel) p.getComponent(i));
                } else if (p.getComponent(i) instanceof Label) {
                    Label lbl = (Label) p.getComponent(i);
                    java.awt.Font f = lbl.getPDFFont();
                    lbl.setPDFFont(lbl.getFont());
                    lbl.setFont(f);
                }
            }
        }
    }
}
