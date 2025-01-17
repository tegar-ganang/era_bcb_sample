package net.sourceforge.poi.hssf.dev;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import net.sourceforge.poi.poifs.filesystem.POIFSFileSystem;
import net.sourceforge.poi.hssf.record.*;
import net.sourceforge.poi.hssf.eventmodel.*;
import net.sourceforge.poi.hssf.usermodel.*;

/**
 * Event Factory version of HSSF test class.
 * @author  andy
 */
public class EFHSSF {

    String infile;

    String outfile;

    HSSFWorkbook workbook = null;

    HSSFSheet cursheet = null;

    /** Creates a new instance of EFHSSF */
    public EFHSSF() {
    }

    public void setInputFile(String infile) {
        this.infile = infile;
    }

    public void setOutputFile(String outfile) {
        this.outfile = outfile;
    }

    public void run() throws IOException {
        FileInputStream fin = new FileInputStream(infile);
        POIFSFileSystem poifs = new POIFSFileSystem(fin);
        InputStream din = poifs.createDocumentInputStream("Workbook");
        HSSFRequest req = new HSSFRequest();
        req.addListenerForAllRecords(new EFHSSFListener(this));
        HSSFEventFactory factory = new HSSFEventFactory();
        factory.processEvents(req, din);
        fin.close();
        din.close();
        FileOutputStream fout = new FileOutputStream(outfile);
        workbook.write(fout);
        fout.close();
        System.out.println("done.");
    }

    public void recordHandler(Record record) {
        HSSFRow row = null;
        HSSFCell cell = null;
        int sheetnum = -1;
        switch(record.getSid()) {
            case BOFRecord.sid:
                BOFRecord bof = (BOFRecord) record;
                if (bof.getType() == bof.TYPE_WORKBOOK) {
                    workbook = new HSSFWorkbook();
                } else if (bof.getType() == bof.TYPE_WORKSHEET) {
                    sheetnum++;
                    cursheet = workbook.getSheetAt(sheetnum);
                }
                break;
            case BoundSheetRecord.sid:
                BoundSheetRecord bsr = (BoundSheetRecord) record;
                workbook.createSheet(bsr.getSheetname());
                break;
            case RowRecord.sid:
                RowRecord rowrec = (RowRecord) record;
                cursheet.createRow(rowrec.getRowNumber());
                break;
            case NumberRecord.sid:
                NumberRecord numrec = (NumberRecord) record;
                row = cursheet.getRow(numrec.getRow());
                cell = row.createCell(numrec.getColumn(), HSSFCell.CELL_TYPE_NUMERIC);
                cell.setCellValue(numrec.getValue());
                break;
            case SSTRecord.sid:
                SSTRecord sstrec = (SSTRecord) record;
                for (int k = 0; k < sstrec.getNumUniqueStrings(); k++) {
                    workbook.addSSTString(sstrec.getString(k));
                }
                break;
            case LabelSSTRecord.sid:
                LabelSSTRecord lrec = (LabelSSTRecord) record;
                row = cursheet.getRow(lrec.getRow());
                cell = row.createCell(lrec.getColumn(), HSSFCell.CELL_TYPE_STRING);
                cell.setCellValue(workbook.getSSTString(lrec.getSSTIndex()));
                break;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("--help")) {
            try {
                EFHSSF viewer = new EFHSSF();
                viewer.setInputFile(args[0]);
                viewer.setOutputFile(args[1]);
                viewer.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("EFHSSF");
            System.out.println("General testbed for HSSFEventFactory based testing and " + "Code examples");
            System.out.println("Usage: java net.sourceforge.poi.hssf.dev.EFHSSF " + "file1 file2");
            System.out.println("   --will rewrite the file reading with the event api");
            System.out.println("and writing with the standard API");
        }
    }
}

class EFHSSFListener implements HSSFListener {

    EFHSSF efhssf;

    public EFHSSFListener(EFHSSF efhssf) {
        this.efhssf = efhssf;
    }

    public void processRecord(Record record) {
        efhssf.recordHandler(record);
    }
}
