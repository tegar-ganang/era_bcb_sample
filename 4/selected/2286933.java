package gov.sns.apps.diagnostics.corede.corede;

import gov.sns.ca.*;

public class PVOutput extends AbstractFixedDataProcessor {

    @Override
    public void processData() throws CoredeException {
        try {
            ch.putValCallback(inputs[0].getDouble(), null);
        } catch (Exception e) {
            throw new CoredeException(e.getMessage());
        }
    }

    private Channel ch;

    public PVOutput(String n) {
        super(n, 1, 0);
        ch = Utilities.getChannelFactory().getChannel(n);
        ch.connect();
    }
}
