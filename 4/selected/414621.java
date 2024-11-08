package com.justin.processing.dataexchange.dto;

import java.math.BigDecimal;
import java.util.Date;
import com.justin.foundation.dto.IDto;

public class EquityTradeAddDto implements IDto {

    private static final long serialVersionUID = -2532559390623829087L;

    public static final String MESSAGE_NAME = "EquityTradeAdd";

    private Long key;

    private String cmpnyCode;

    private String acId;

    private String mrktCode;

    private String instrCode;

    private String tradeSide;

    private BigDecimal tradePrice;

    private BigDecimal quantity;

    private String aeAcesGrpCode;

    private BigDecimal accruedInterest;

    private String isLiquidate;

    private String isShortSell;

    private String channelCode;

    private BigDecimal settleFxRate;

    private BigDecimal settlementAmount;

    private BigDecimal releaseHoldAmount;

    private BigDecimal releaseHoldStock;

    private String orderRefNum;

    private String settlementTradeId;

    private String tradingTradeId;

    private Date tradeDate;

    private String tradeCcyCode;

    private String settlementCcyCode;

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public String getCmpnyCode() {
        return cmpnyCode;
    }

    public void setCmpnyCode(String cmpnyCode) {
        this.cmpnyCode = cmpnyCode;
    }

    public String getAcId() {
        return acId;
    }

    public void setAcId(String acId) {
        this.acId = acId;
    }

    public String getMrktCode() {
        return mrktCode;
    }

    public void setMrktCode(String mrktCode) {
        this.mrktCode = mrktCode;
    }

    public String getInstrCode() {
        return instrCode;
    }

    public void setInstrCode(String instrCode) {
        this.instrCode = instrCode;
    }

    public String getTradeSide() {
        return tradeSide;
    }

    public void setTradeSide(String tradeSide) {
        this.tradeSide = tradeSide;
    }

    public BigDecimal getTradePrice() {
        return tradePrice;
    }

    public void setTradePrice(BigDecimal tradePrice) {
        this.tradePrice = tradePrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getAeAcesGrpCode() {
        return aeAcesGrpCode;
    }

    public void setAeAcesGrpCode(String aeAcesGrpCode) {
        this.aeAcesGrpCode = aeAcesGrpCode;
    }

    public BigDecimal getAccruedInterest() {
        return accruedInterest;
    }

    public void setAccruedInterest(BigDecimal accruedInterest) {
        this.accruedInterest = accruedInterest;
    }

    public String getIsLiquidate() {
        return isLiquidate;
    }

    public void setIsLiquidate(String isLiquidate) {
        this.isLiquidate = isLiquidate;
    }

    public String getIsShortSell() {
        return isShortSell;
    }

    public void setIsShortSell(String isShortSell) {
        this.isShortSell = isShortSell;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public BigDecimal getSettleFxRate() {
        return settleFxRate;
    }

    public void setSettleFxRate(BigDecimal settleFxRate) {
        this.settleFxRate = settleFxRate;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public BigDecimal getReleaseHoldAmount() {
        return releaseHoldAmount;
    }

    public void setReleaseHoldAmount(BigDecimal releaseHoldAmount) {
        this.releaseHoldAmount = releaseHoldAmount;
    }

    public BigDecimal getReleaseHoldStock() {
        return releaseHoldStock;
    }

    public void setReleaseHoldStock(BigDecimal releaseHoldStock) {
        this.releaseHoldStock = releaseHoldStock;
    }

    public String getOrderRefNum() {
        return orderRefNum;
    }

    public void setOrderRefNum(String orderRefNum) {
        this.orderRefNum = orderRefNum;
    }

    public String getSettlementTradeId() {
        return settlementTradeId;
    }

    public void setSettlementTradeId(String settlementTradeId) {
        this.settlementTradeId = settlementTradeId;
    }

    public String getTradingTradeId() {
        return tradingTradeId;
    }

    public void setTradingTradeId(String tradingTradeId) {
        this.tradingTradeId = tradingTradeId;
    }

    public Date getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getTradeCcyCode() {
        return tradeCcyCode;
    }

    public void setTradeCcyCode(String tradeCcyCode) {
        this.tradeCcyCode = tradeCcyCode;
    }

    public String getSettlementCcyCode() {
        return settlementCcyCode;
    }

    public void setSettlementCcyCode(String settlementCcyCode) {
        this.settlementCcyCode = settlementCcyCode;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((acId == null) ? 0 : acId.hashCode());
        result = prime * result + ((accruedInterest == null) ? 0 : accruedInterest.hashCode());
        result = prime * result + ((aeAcesGrpCode == null) ? 0 : aeAcesGrpCode.hashCode());
        result = prime * result + ((channelCode == null) ? 0 : channelCode.hashCode());
        result = prime * result + ((cmpnyCode == null) ? 0 : cmpnyCode.hashCode());
        result = prime * result + ((instrCode == null) ? 0 : instrCode.hashCode());
        result = prime * result + ((isLiquidate == null) ? 0 : isLiquidate.hashCode());
        result = prime * result + ((isShortSell == null) ? 0 : isShortSell.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((mrktCode == null) ? 0 : mrktCode.hashCode());
        result = prime * result + ((orderRefNum == null) ? 0 : orderRefNum.hashCode());
        result = prime * result + ((quantity == null) ? 0 : quantity.hashCode());
        result = prime * result + ((releaseHoldAmount == null) ? 0 : releaseHoldAmount.hashCode());
        result = prime * result + ((releaseHoldStock == null) ? 0 : releaseHoldStock.hashCode());
        result = prime * result + ((settleFxRate == null) ? 0 : settleFxRate.hashCode());
        result = prime * result + ((settlementAmount == null) ? 0 : settlementAmount.hashCode());
        result = prime * result + ((settlementCcyCode == null) ? 0 : settlementCcyCode.hashCode());
        result = prime * result + ((settlementTradeId == null) ? 0 : settlementTradeId.hashCode());
        result = prime * result + ((tradeCcyCode == null) ? 0 : tradeCcyCode.hashCode());
        result = prime * result + ((tradeDate == null) ? 0 : tradeDate.hashCode());
        result = prime * result + ((tradePrice == null) ? 0 : tradePrice.hashCode());
        result = prime * result + ((tradeSide == null) ? 0 : tradeSide.hashCode());
        result = prime * result + ((tradingTradeId == null) ? 0 : tradingTradeId.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        EquityTradeAddDto other = (EquityTradeAddDto) obj;
        if (acId == null) {
            if (other.acId != null) return false;
        } else if (!acId.equals(other.acId)) return false;
        if (accruedInterest == null) {
            if (other.accruedInterest != null) return false;
        } else if (!accruedInterest.equals(other.accruedInterest)) return false;
        if (aeAcesGrpCode == null) {
            if (other.aeAcesGrpCode != null) return false;
        } else if (!aeAcesGrpCode.equals(other.aeAcesGrpCode)) return false;
        if (channelCode == null) {
            if (other.channelCode != null) return false;
        } else if (!channelCode.equals(other.channelCode)) return false;
        if (cmpnyCode == null) {
            if (other.cmpnyCode != null) return false;
        } else if (!cmpnyCode.equals(other.cmpnyCode)) return false;
        if (instrCode == null) {
            if (other.instrCode != null) return false;
        } else if (!instrCode.equals(other.instrCode)) return false;
        if (isLiquidate == null) {
            if (other.isLiquidate != null) return false;
        } else if (!isLiquidate.equals(other.isLiquidate)) return false;
        if (isShortSell == null) {
            if (other.isShortSell != null) return false;
        } else if (!isShortSell.equals(other.isShortSell)) return false;
        if (key == null) {
            if (other.key != null) return false;
        } else if (!key.equals(other.key)) return false;
        if (mrktCode == null) {
            if (other.mrktCode != null) return false;
        } else if (!mrktCode.equals(other.mrktCode)) return false;
        if (orderRefNum == null) {
            if (other.orderRefNum != null) return false;
        } else if (!orderRefNum.equals(other.orderRefNum)) return false;
        if (quantity == null) {
            if (other.quantity != null) return false;
        } else if (!quantity.equals(other.quantity)) return false;
        if (releaseHoldAmount == null) {
            if (other.releaseHoldAmount != null) return false;
        } else if (!releaseHoldAmount.equals(other.releaseHoldAmount)) return false;
        if (releaseHoldStock == null) {
            if (other.releaseHoldStock != null) return false;
        } else if (!releaseHoldStock.equals(other.releaseHoldStock)) return false;
        if (settleFxRate == null) {
            if (other.settleFxRate != null) return false;
        } else if (!settleFxRate.equals(other.settleFxRate)) return false;
        if (settlementAmount == null) {
            if (other.settlementAmount != null) return false;
        } else if (!settlementAmount.equals(other.settlementAmount)) return false;
        if (settlementCcyCode == null) {
            if (other.settlementCcyCode != null) return false;
        } else if (!settlementCcyCode.equals(other.settlementCcyCode)) return false;
        if (settlementTradeId == null) {
            if (other.settlementTradeId != null) return false;
        } else if (!settlementTradeId.equals(other.settlementTradeId)) return false;
        if (tradeCcyCode == null) {
            if (other.tradeCcyCode != null) return false;
        } else if (!tradeCcyCode.equals(other.tradeCcyCode)) return false;
        if (tradeDate == null) {
            if (other.tradeDate != null) return false;
        } else if (!tradeDate.equals(other.tradeDate)) return false;
        if (tradePrice == null) {
            if (other.tradePrice != null) return false;
        } else if (!tradePrice.equals(other.tradePrice)) return false;
        if (tradeSide == null) {
            if (other.tradeSide != null) return false;
        } else if (!tradeSide.equals(other.tradeSide)) return false;
        if (tradingTradeId == null) {
            if (other.tradingTradeId != null) return false;
        } else if (!tradingTradeId.equals(other.tradingTradeId)) return false;
        return true;
    }

    public String toString() {
        return "EquityTradeAddDto [acId=" + acId + ", accruedInterest=" + accruedInterest + ", aeAcesGrpCode=" + aeAcesGrpCode + ", channelCode=" + channelCode + ", cmpnyCode=" + cmpnyCode + ", instrCode=" + instrCode + ", isLiquidate=" + isLiquidate + ", isShortSell=" + isShortSell + ", key=" + key + ", mrktCode=" + mrktCode + ", orderRefNum=" + orderRefNum + ", quantity=" + quantity + ", releaseHoldAmount=" + releaseHoldAmount + ", releaseHoldStock=" + releaseHoldStock + ", settleFxRate=" + settleFxRate + ", settlementAmount=" + settlementAmount + ", settlementCcyCode=" + settlementCcyCode + ", settlementTradeId=" + settlementTradeId + ", tradeCcyCode=" + tradeCcyCode + ", tradeDate=" + tradeDate + ", tradePrice=" + tradePrice + ", tradeSide=" + tradeSide + ", tradingTradeId=" + tradingTradeId + "]";
    }
}
