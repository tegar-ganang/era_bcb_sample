package cwterm.service.rigctl;

public class RigState {

    private int ituRegion;

    private long maxRIT, maxXIT, maxIfShift, announces;

    private boolean hasGetFunc, hasSetFunc, hasGetLevel, hasSetLevel, hasGetParm, hasSetParm;

    private int[] preamp, attenuator;

    private FreqRange[] rxRangeList, txRangeList;

    private TuningStep[] tuningSteps;

    private Filter[] filters;

    private Channel[] channelList;

    /**
	 * @return the announces
	 */
    public long getAnnounces() {
        return announces;
    }

    /**
	 * @param announces the announces to set
	 */
    public void setAnnounces(long announces) {
        this.announces = announces;
    }

    /**
	 * @return the attenuator
	 */
    public int[] getAttenuator() {
        return attenuator;
    }

    /**
	 * @param attenuator the attenuator to set
	 */
    public void setAttenuator(int[] attenuator) {
        this.attenuator = attenuator;
    }

    /**
	 * @return the channelList
	 */
    public Channel[] getChannelList() {
        return channelList;
    }

    /**
	 * @param channelList the channelList to set
	 */
    public void setChannelList(Channel[] channelList) {
        this.channelList = channelList;
    }

    /**
	 * @return the filters
	 */
    public Filter[] getFilters() {
        return filters;
    }

    /**
	 * @param filters the filters to set
	 */
    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }

    /**
	 * @return the hasGetFunc
	 */
    public boolean isHasGetFunc() {
        return hasGetFunc;
    }

    /**
	 * @param hasGetFunc the hasGetFunc to set
	 */
    public void setHasGetFunc(boolean hasGetFunc) {
        this.hasGetFunc = hasGetFunc;
    }

    /**
	 * @return the hasGetLevel
	 */
    public boolean isHasGetLevel() {
        return hasGetLevel;
    }

    /**
	 * @param hasGetLevel the hasGetLevel to set
	 */
    public void setHasGetLevel(boolean hasGetLevel) {
        this.hasGetLevel = hasGetLevel;
    }

    /**
	 * @return the hasGetParm
	 */
    public boolean isHasGetParm() {
        return hasGetParm;
    }

    /**
	 * @param hasGetParm the hasGetParm to set
	 */
    public void setHasGetParm(boolean hasGetParm) {
        this.hasGetParm = hasGetParm;
    }

    /**
	 * @return the hasSetFunc
	 */
    public boolean isHasSetFunc() {
        return hasSetFunc;
    }

    /**
	 * @param hasSetFunc the hasSetFunc to set
	 */
    public void setHasSetFunc(boolean hasSetFunc) {
        this.hasSetFunc = hasSetFunc;
    }

    /**
	 * @return the hasSetLevel
	 */
    public boolean isHasSetLevel() {
        return hasSetLevel;
    }

    /**
	 * @param hasSetLevel the hasSetLevel to set
	 */
    public void setHasSetLevel(boolean hasSetLevel) {
        this.hasSetLevel = hasSetLevel;
    }

    /**
	 * @return the hasSetParm
	 */
    public boolean isHasSetParm() {
        return hasSetParm;
    }

    /**
	 * @param hasSetParm the hasSetParm to set
	 */
    public void setHasSetParm(boolean hasSetParm) {
        this.hasSetParm = hasSetParm;
    }

    /**
	 * @return the ituRegion
	 */
    public int getItuRegion() {
        return ituRegion;
    }

    /**
	 * @param ituRegion the ituRegion to set
	 */
    public void setItuRegion(int ituRegion) {
        this.ituRegion = ituRegion;
    }

    /**
	 * @return the maxIfShift
	 */
    public long getMaxIfShift() {
        return maxIfShift;
    }

    /**
	 * @param maxIfShift the maxIfShift to set
	 */
    public void setMaxIfShift(long maxIfShift) {
        this.maxIfShift = maxIfShift;
    }

    /**
	 * @return the maxRIT
	 */
    public long getMaxRIT() {
        return maxRIT;
    }

    /**
	 * @param maxRIT the maxRIT to set
	 */
    public void setMaxRIT(long maxRIT) {
        this.maxRIT = maxRIT;
    }

    /**
	 * @return the maxXIT
	 */
    public long getMaxXIT() {
        return maxXIT;
    }

    /**
	 * @param maxXIT the maxXIT to set
	 */
    public void setMaxXIT(long maxXIT) {
        this.maxXIT = maxXIT;
    }

    /**
	 * @return the preamp
	 */
    public int[] getPreamp() {
        return preamp;
    }

    /**
	 * @param preamp the preamp to set
	 */
    public void setPreamp(int[] preamp) {
        this.preamp = preamp;
    }

    /**
	 * @return the rxRangeList
	 */
    public FreqRange[] getRxRangeList() {
        return rxRangeList;
    }

    /**
	 * @param rxRangeList the rxRangeList to set
	 */
    public void setRxRangeList(FreqRange[] rxRangeList) {
        this.rxRangeList = rxRangeList;
    }

    /**
	 * @return the tuningSteps
	 */
    public TuningStep[] getTuningSteps() {
        return tuningSteps;
    }

    /**
	 * @param tuningSteps the tuningSteps to set
	 */
    public void setTuningSteps(TuningStep[] tuningSteps) {
        this.tuningSteps = tuningSteps;
    }

    /**
	 * @return the txRangeList
	 */
    public FreqRange[] getTxRangeList() {
        return txRangeList;
    }

    /**
	 * @param txRangeList the txRangeList to set
	 */
    public void setTxRangeList(FreqRange[] txRangeList) {
        this.txRangeList = txRangeList;
    }
}
