package org.jumpmind.symmetric.extract.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import org.jumpmind.symmetric.common.csv.CsvConstants;
import org.jumpmind.symmetric.extract.DataExtractorContext;
import org.jumpmind.symmetric.model.OutgoingBatch;

public class CsvExtractor extends CsvExtractor16 {

    public void init(BufferedWriter writer, DataExtractorContext context) throws IOException {
        super.init(writer, context);
        Util.write(writer, CsvConstants.BINARY, Util.DELIMITER, dbDialect.getBinaryEncoding().name());
        writer.newLine();
    }

    @Override
    public void begin(OutgoingBatch batch, BufferedWriter writer) throws IOException {
        Util.write(writer, CsvConstants.CHANNEL, Util.DELIMITER, batch.getChannelId());
        writer.newLine();
        Util.write(writer, CsvConstants.BATCH, Util.DELIMITER, Long.toString(batch.getBatchId()));
        writer.newLine();
    }
}
