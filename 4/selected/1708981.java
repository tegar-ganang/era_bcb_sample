package com.CellPlan.utility;

/**
 *This class is responsible for keeping information regarding cell plan in form of channels
 * allocated to which sector and cell, and whats the name of the cell plan.
 * @author channel Allocation and user capacity.
 */
public class CellPlan {

    private int cellId;

    private int sectorId;

    private int channelId;

    private String cellPlanName;

    /**
     * This method returns the cellPlan name
     * @return cellPlanName: name of the cell plan.
     */
    public String getCellPlanName() {
        return cellPlanName;
    }

    /**
 * This mehtod responsible for setting up name of the cell plan.
 * @param cellPlanName: name fo the cell plan.
 */
    public void setCellPlanName(String cellPlanName) {
        this.cellPlanName = cellPlanName;
    }

    /**
  * This method responsible for returning channel id as unique absolute radio frequency channel number to
  * each channel.
  * @return channelId: absolute radio frequency channel number(ARFCN) assigned to each channel.
  */
    public int getChannelId() {
        return channelId;
    }

    /**
  * This method responsible for setting up channel id as unique absolute radio frequency channel number to
  * each channel.
  * @return channelId: absolute radio frequency channel number(ARFCN) assigned to each channel.
  */
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    /**
    * This method is responsible for returning cell id assigned to each cell.
    * @returnc cellId: cell identification number assigned to each cell.
    */
    public int getCellId() {
        return this.cellId;
    }

    /**
    * This method is responsible for setting cell id assigned to each cell.
    * @returnc cellId: cell identification number assigned to each cell.
    */
    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    /**
    * This method is responsible for returning sector id assigned to each sector in cell plan.
    * @returnc sectorId: cell identification number assigned to each cell.
    */
    public int getSectorId() {
        return this.sectorId;
    }

    /**
    * This method is responsible for setting up sector id assigned to each sector in cell plan.
    * @returnc sectorId: cell identification number assigned to each cell.
    */
    public void setSectorId(int SectorId) {
        this.sectorId = SectorId;
    }

    public int getchannelId() {
        return this.channelId;
    }

    public void setchannelId(int channelId) {
        this.channelId = channelId;
    }
}
