package com.kongur.network.erp.request.ic.taobao;

import com.kongur.network.erp.request.ic.ItemRequest;

/**
 * @author gaojf
 * @version $Id: TaobaoItemRequest.java,v 0.1 2012-3-14 ����03:11:31 gaojf Exp $
 */
public class TaobaoItemRequest extends ItemRequest {

    /**
     * ��Ʒ����ID
     */
    private Long numIid;

    /**
     * ��Ҫ�ϼܵ���Ʒ������
     */
    private Integer num;

    /**
     * ��Ʒ�ⲿ��Ʒid
     */
    private String outerId;

    /**
     * ��ƷID
     */
    private Long itemId;

    /**
     * ������Դ������վ��
     */
    private String channelKey;

    public Long getNumIid() {
        return numIid;
    }

    public void setNumIid(Long numIid) {
        this.numIid = numIid;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getOuterId() {
        return outerId;
    }

    public void setOuterId(String outerId) {
        this.outerId = outerId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }
}
