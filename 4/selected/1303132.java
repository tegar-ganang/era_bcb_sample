package midi;

public class LEDInfo {

    private LEDSensorInfo[] lsi;

    private double sustain, velocity;

    private int power;

    private int channelR, channelG, channelB;

    private double transition;

    private LEDRFIDInfo[] lri;

    public LEDInfo() {
        int i;
        lsi = new LEDSensorInfo[8];
        for (i = 0; i < 8; i++) {
            lsi[i] = new LEDSensorInfo();
        }
        sustain = 1.0;
        velocity = 1.0;
        power = 250;
        channelR = 0;
        channelG = 0;
        channelB = 0;
        transition = 2.5;
        lri = new LEDRFIDInfo[6];
        for (i = 0; i < 6; i++) {
            lri[i] = new LEDRFIDInfo();
        }
    }

    public LEDSensorInfo[] getLsi() {
        return lsi;
    }

    public void setLsi(LEDSensorInfo[] lsi) {
        this.lsi = lsi;
    }

    public double getSustain() {
        return sustain;
    }

    public void setSustain(double sustain) {
        this.sustain = sustain;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getChannelR() {
        return channelR;
    }

    public void setChannelR(int channelR) {
        this.channelR = channelR;
    }

    public int getChannelG() {
        return channelG;
    }

    public void setChannelG(int channelG) {
        this.channelG = channelG;
    }

    public int getChannelB() {
        return channelB;
    }

    public void setChannelB(int channelB) {
        this.channelB = channelB;
    }

    public double getTransition() {
        return transition;
    }

    public void setTransition(double transition) {
        this.transition = transition;
    }

    public LEDRFIDInfo[] getLri() {
        return lri;
    }

    public void setLri(LEDRFIDInfo[] lri) {
        this.lri = lri;
    }
}
