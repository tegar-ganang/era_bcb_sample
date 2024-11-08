package com.empower.model;

import java.util.Date;

public class TransferMoneyModel {

    Date transferDate;

    Float amount;

    String transferFromEntityCD;

    String transferToEntityCD;

    String transferFrom;

    String transferTo;

    String memo;

    public Date getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(Date transferDate) {
        this.transferDate = transferDate;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public String getTransferFromEntityCD() {
        return transferFromEntityCD;
    }

    public void setTransferFromEntityCD(String transferFromEntityCD) {
        this.transferFromEntityCD = transferFromEntityCD;
    }

    public String getTransferToEntityCD() {
        return transferToEntityCD;
    }

    public void setTransferToEntityCD(String transferToEntityCD) {
        this.transferToEntityCD = transferToEntityCD;
    }

    public String getTransferFrom() {
        return transferFrom;
    }

    public void setTransferFrom(String transferFrom) {
        this.transferFrom = transferFrom;
    }

    public String getTransferTo() {
        return transferTo;
    }

    public void setTransferTo(String transferTo) {
        this.transferTo = transferTo;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
