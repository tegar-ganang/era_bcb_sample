package edu.infrabig.odf.document;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import anima.annotation.Component;
import anima.component.base.VisualComponentBase;
import edu.infrabig.odf.read.IReadOdfFile;
import edu.infrabig.odf.write.IWriterOdfFile;

/**
 * Componente visual para delegar as chamadas aos componentes de manipula��o de
 * arquivos odf.
 * 
 * @author Raimundo/Elmo
 * 
 */
@Component(id = "http://purl.org/NET/dcc/edu.infrabig.odf.sheet.VisualDocument", requires = "http://purl.org/NET/dcc/edu.infrabig.odf.sheet.IDocument")
public class VisualDocument extends VisualComponentBase implements IDocument {

    private static final long serialVersionUID = 2949647755538324106L;

    private IWriterOdfFile writeOdf;

    private IReadOdfFile readOdf;

    public VisualDocument() {
    }

    public void openDocumentNotExists(String pFile, IWriterOdfFile write) {
        File file = new File(pFile);
        try {
            readOdf.openDocument(file, write);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Connecta ao receptacle para leitura de arquivos.
	 */
    public void connect(IReadOdfFile pReadOdfFile) throws IOException {
        readOdf = pReadOdfFile;
    }

    @Override
    public void connect(IWriterOdfFile write) throws IOException {
        writeOdf = write;
    }

    /**
	 * Connecta ao receptacle para leitura de arquivos.
	 */
    @Override
    public void openDocument(String pFile) throws IOException {
        File file = new File(pFile);
        try {
            readOdf.openDocument(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * Connect para processar escritas no arquivo. Recebe o componenete de
	 * Writer e o caminho do arquivo.
	 * 
	 */
    @Override
    public void writeDocument(String pFile) throws IOException {
        File file = new File(pFile);
        Sheet sheet = null;
        try {
            if (!file.exists()) {
                sheet = writeOdf.createEmptyDocument(file);
            } else {
                sheet = SpreadSheet.createFromFile(file).getSheet(0);
            }
            sheet.ensureColumnCount(1024);
            sheet.ensureRowCount(1024);
            sheet.getCellAt("A1").setValue("Ano mes ano ");
            sheet.getCellAt("B7").setValue("Novo teste ");
            sheet.getSpreadSheet().getSheet(0);
            sheet.getCellAt("I10").setValue(new Date());
            sheet.getCellAt("F24").setValue(3);
            writeOdf.writeDocument(file, sheet);
            writeOdf.openDocument(file);
        } catch (IOException e) {
            throw e;
        }
    }
}
