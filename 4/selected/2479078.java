package lt.ku.ik.recon.logic.model;

/**
 *
 * @author linas
 */
public class Settings {

    private String device;

    private BaudRate baudRate;

    private DataBits dataBits;

    private StopBits stopBits;

    private Parity parity;

    private int precision;

    private int channels;

    private int startCommand;

    private int stopCommand;

    private CommandFormat commandFormat;

    private int resolutionOfGraph;

    public BaudRate getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(BaudRate baudRate) {
        this.baudRate = baudRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public CommandFormat getCommandFormat() {
        return commandFormat;
    }

    public void setCommandFormat(CommandFormat commandFormat) {
        this.commandFormat = commandFormat;
    }

    public DataBits getDataBits() {
        return dataBits;
    }

    public void setDataBits(DataBits dataBits) {
        this.dataBits = dataBits;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Parity getParity() {
        return parity;
    }

    public void setParity(Parity parity) {
        this.parity = parity;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getResolutionOfGraph() {
        return resolutionOfGraph;
    }

    public void setResolutionOfGraph(int resolutionOfGraph) {
        this.resolutionOfGraph = resolutionOfGraph;
    }

    public int getStartCommand() {
        return startCommand;
    }

    public void setStartCommand(int startCommand) {
        this.startCommand = startCommand;
    }

    public StopBits getStopBits() {
        return stopBits;
    }

    public void setStopBits(StopBits stopBits) {
        this.stopBits = stopBits;
    }

    public int getStopCommand() {
        return stopCommand;
    }

    public void setStopCommand(int stopCommand) {
        this.stopCommand = stopCommand;
    }
}
