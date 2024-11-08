package com.kongur.network.erp.manager.ic.taobao;

import org.springframework.beans.factory.annotation.Autowired;
import com.eyeieye.melody.util.StringUtil;
import com.kongur.network.erp.cache.CommonCache;
import com.kongur.network.erp.constants.ResultConstants;
import com.kongur.network.erp.manager.ic.AbstractPlatformItemManager;
import com.kongur.network.erp.request.ic.ItemRequest;
import com.kongur.network.erp.request.ic.taobao.TaobaoItemOptRequest;
import com.kongur.network.erp.request.ic.taobao.TaobaoItemRequest;
import com.kongur.network.erp.request.ic.taobao.TaobaoSkuRequest;
import com.kongur.network.erp.result.ic.ItemPromotionsResult;
import com.kongur.network.erp.result.ic.ItemResult;
import com.kongur.network.erp.result.ic.ItemSkuResult;
import com.kongur.network.erp.result.ic.ItemTemplatesResult;
import com.kongur.network.erp.service.AuthorizeManager;
import com.taobao.api.ApiException;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.ItemAddRequest;
import com.taobao.api.request.ItemDeleteRequest;
import com.taobao.api.request.ItemGetRequest;
import com.taobao.api.request.ItemPriceUpdateRequest;
import com.taobao.api.request.ItemQuantityUpdateRequest;
import com.taobao.api.request.ItemSkuAddRequest;
import com.taobao.api.request.ItemSkuDeleteRequest;
import com.taobao.api.request.ItemSkuGetRequest;
import com.taobao.api.request.ItemSkuPriceUpdateRequest;
import com.taobao.api.request.ItemSkuUpdateRequest;
import com.taobao.api.request.ItemSkusGetRequest;
import com.taobao.api.request.ItemTemplatesGetRequest;
import com.taobao.api.request.ItemUpdateDelistingRequest;
import com.taobao.api.request.ItemUpdateListingRequest;
import com.taobao.api.request.ItemUpdateRequest;
import com.taobao.api.request.ItemsCustomGetRequest;
import com.taobao.api.request.ItemsGetRequest;
import com.taobao.api.request.SkusCustomGetRequest;
import com.taobao.api.request.UmpPromotionGetRequest;
import com.taobao.api.response.ItemAddResponse;
import com.taobao.api.response.ItemDeleteResponse;
import com.taobao.api.response.ItemGetResponse;
import com.taobao.api.response.ItemPriceUpdateResponse;
import com.taobao.api.response.ItemQuantityUpdateResponse;
import com.taobao.api.response.ItemSkuAddResponse;
import com.taobao.api.response.ItemSkuDeleteResponse;
import com.taobao.api.response.ItemSkuGetResponse;
import com.taobao.api.response.ItemSkuPriceUpdateResponse;
import com.taobao.api.response.ItemSkuUpdateResponse;
import com.taobao.api.response.ItemSkusGetResponse;
import com.taobao.api.response.ItemTemplatesGetResponse;
import com.taobao.api.response.ItemUpdateDelistingResponse;
import com.taobao.api.response.ItemUpdateListingResponse;
import com.taobao.api.response.ItemUpdateResponse;
import com.taobao.api.response.ItemsCustomGetResponse;
import com.taobao.api.response.ItemsGetResponse;
import com.taobao.api.response.SkusCustomGetResponse;
import com.taobao.api.response.UmpPromotionGetResponse;

/**
 * @author gaojf
 * @version $Id: TaobaoPlatformItemManager.java,v 0.1 2012-3-14 ����11:38:44 gaojf Exp $
 */
public class TaobaoPlatformItemManager extends AbstractPlatformItemManager {

    @Autowired
    private TaobaoClient taobaoClient;

    @Autowired
    private CommonCache commonCache;

    @Autowired
    private AuthorizeManager authorizeManager;

    /**
     * ����һ����Ʒ
     * @param req
     * �������
     * num            ��Ʒ����
     * price          ��Ʒ�۸�
     * type           ��������
     * stuffStatus    �¾ɳ̶�
     * title          ��������
     * desc           ��������
     * state          ���ڵ�ʡ��
     * city           ���ڵس���
     * cid            Ҷ����Ŀid
     * shopId         ����ID
     * ѡ�����
     * ̫�࣬�����뿴req
     */
    public ItemResult add(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemOptRequest itemOptReq = (TaobaoItemOptRequest) itemRequest;
        if (itemOptReq == null || itemOptReq.getItemAddRequest() == null || itemOptReq.getItemAddRequest().getNum() == null || itemOptReq.getShopId() == null || itemOptReq.getItemAddRequest().getCid() == null || StringUtil.isBlank(itemOptReq.getItemAddRequest().getPrice()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getType()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getStuffStatus()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getTitle()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getDesc()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getLocationState()) || StringUtil.isBlank(itemOptReq.getItemAddRequest().getLocationCity())) {
            logger.error("add param error:itemOptReq=" + itemOptReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemAddRequest req = itemOptReq.getItemAddRequest();
        String sessionKey = commonCache.getSessionKey(itemOptReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("add shop[" + itemOptReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemOptReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemAddResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("add ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemOptReq=" + itemOptReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("add ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ɾ������Ʒ
     * @param req
     * �������
     * numIid    ��Ʒ����ID
     * shopId    ����ID
     */
    public ItemResult delete(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || itemReq.getNumIid() == null) {
            logger.error("delete param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemDeleteRequest req = new ItemDeleteRequest();
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("delete shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemDeleteResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("delete ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("delete ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * �õ�������Ʒ��Ϣ 
     * @param req
     * �������
     * numIid    ��Ʒ����ID
     * shopId    ����ID
     * ѡ�����
     * fields    ��Ҫ���ص��ֶ�
     */
    public ItemResult get(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || itemReq.getNumIid() == null) {
            logger.error("get param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemGetRequest req = new ItemGetRequest();
        if (StringUtil.isNotBlank(itemReq.getFields())) {
            req.setFields(itemReq.getFields());
        } else {
            req.setFields("num_iid,title,price,cid,after_sale_id,approve_status,auction_point,auto_fill,cod_postage_id,created," + "delist_time,desc,detail_url,ems_fee,express_fee,freight_payer,has_discount,has_invoice,has_showcase,has_warranty," + "increment,input_pids,input_str,is_3D,is_ex,is_fenxiao,is_lightning_consignment,is_prepay,is_taobao,is_timing," + "is_virtual,is_xinpin,item_img,list_time,location,modified,,nick,num,one_station,outer_id,pic_url,post_fee,postage_id" + "product_id,promoted_service,prop_img,property_alias,props,props_name,score,second_kill,sell_promise,seller_cids,sku" + "stuff_status,sub_stock,template_id,type,valid_thru,video,violation,volume,wap_desc,wap_detail_url,ww_status,");
        }
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("get shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("get ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("get ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ������Ʒ��Ϣ
     * @param req
     * �������
     * numIid    ��Ʒ����ID
     * shopId    ����ID
     * ѡ�����
     * ̫�࣬�����뿴req
     */
    public ItemResult update(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemOptRequest itemOptReq = (TaobaoItemOptRequest) itemRequest;
        if (itemOptReq == null || itemOptReq.getItemUpdateRequest() == null || itemOptReq.getItemUpdateRequest().getNumIid() == null || itemOptReq.getShopId() == null) {
            logger.error("update param error:itemOptReq=" + itemOptReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemUpdateRequest req = itemOptReq.getItemUpdateRequest();
        String sessionKey = commonCache.getSessionKey(itemOptReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("update shop[" + itemOptReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemOptReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemUpdateResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("update ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemOptReq=" + itemOptReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("update ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ������Ʒ�۸� 
     * @param req
     * �������
     * numIid    ��Ʒ����ID
     * shopId    ����ID
     * price     ��Ʒ�۸�   ��λ��
     * ѡ�����
     * ̫�࣬�����뿴req
     */
    public ItemResult priceUpdate(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemOptRequest itemOptReq = (TaobaoItemOptRequest) itemRequest;
        if (itemOptReq == null || itemOptReq.getItemPriceUpdateRequest() == null || itemOptReq.getItemPriceUpdateRequest().getNumIid() == null || itemOptReq.getShopId() == null || StringUtil.isBlank(itemOptReq.getItemPriceUpdateRequest().getPrice())) {
            logger.error("priceUpdate param error:itemOptReq=" + itemOptReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemPriceUpdateRequest req = itemOptReq.getItemPriceUpdateRequest();
        String sessionKey = commonCache.getSessionKey(itemOptReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("priceUpdate shop[" + itemOptReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemOptReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        authorizeManager.shortAuthorize("taobao.item.price.update", sessionKey);
        try {
            ItemPriceUpdateResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("priceUpdate ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemOptReq=" + itemOptReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("priceUpdate ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ����/SKU����޸�
     * @param req
     * �������
     * numIid    ��Ʒ����ID
     * quantity  ����޸�ֵ
     * shopId    ����ID
     * ѡ�����
     * skuId     Ҫ������SKU������ID
     * outerId   SKU���̼ұ���
     * type      �����·�ʽ
     */
    public ItemResult quantityUpdate(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || itemSkuReq.getQuantity() == null) {
            logger.error("quantityUpdate param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemQuantityUpdateRequest req = new ItemQuantityUpdateRequest();
        if (itemSkuReq.getSkuId() != null) {
            req.setSkuId(itemSkuReq.getSkuId());
        }
        if (StringUtil.isNotBlank(itemSkuReq.getOuterId())) {
            req.setOuterId(itemSkuReq.getOuterId());
        }
        if (itemSkuReq.getType() != null) {
            req.setType(itemSkuReq.getType());
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("quantityUpdate shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemQuantityUpdateResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("quantityUpdate ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("quantityUpdate ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ���SKU
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * properties  Sku���Դ�
     * quantity    ����޸�ֵ
     * price       Sku�����ۼ۸�
     * shopId      ����ID
     * ѡ�����
     * outerId     Sku���̼��ⲿid 
     * itemPrice   sku������Ʒ�ļ۸�
     * lang        Sku���ֵİ汾
     */
    public ItemSkuResult skuAdd(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || itemSkuReq.getQuantity() == null || StringUtil.isBlank(itemSkuReq.getProperties()) || itemSkuReq.getPrice() == null) {
            logger.error("skuAdd param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkuAddRequest req = new ItemSkuAddRequest();
        if (itemSkuReq.getItemPrice() != null) {
            req.setItemPrice(String.valueOf(itemSkuReq.getItemPrice()));
        }
        if (StringUtil.isNotBlank(itemSkuReq.getOuterId())) {
            req.setOuterId(itemSkuReq.getOuterId());
        }
        if (StringUtil.isNotBlank(itemSkuReq.getLang())) {
            req.setLang(itemSkuReq.getLang());
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skuAdd shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemSkuAddResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skuAdd ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSku(response.getSku());
        } catch (ApiException e) {
            logger.error("skuAdd ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ɾ��SKU
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * properties  Sku���Դ�
     * shopId      ����ID
     * ѡ�����
     * itemPrice   sku������Ʒ�ļ۸�
     * itemNum     sku������Ʒ������
     * lang        Sku���ֵİ汾
     */
    public ItemSkuResult skuDelete(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || StringUtil.isBlank(itemSkuReq.getProperties())) {
            logger.error("skuDelete param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkuDeleteRequest req = new ItemSkuDeleteRequest();
        if (itemSkuReq.getItemNum() != null) {
            req.setItemNum(itemSkuReq.getItemNum());
        }
        if (itemSkuReq.getItemPrice() != null) {
            req.setItemPrice(String.valueOf(itemSkuReq.getItemPrice()));
        }
        if (StringUtil.isNotBlank(itemSkuReq.getLang())) {
            req.setLang(itemSkuReq.getLang());
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skuDelete shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemSkuDeleteResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skuDelete ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSku(response.getSku());
        } catch (ApiException e) {
            logger.error("skuDelete ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ��ȡSKU
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * skuId       Sku��id
     * shopId      ����ID
     * ѡ�����
     * nick        ����nick
     * fields      �����ֶ�
     */
    public ItemSkuResult skuGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || itemSkuReq.getSkuId() == null) {
            logger.error("skuGet param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkuGetRequest req = new ItemSkuGetRequest();
        if (StringUtil.isNotBlank(itemSkuReq.getNick())) {
            req.setNick(itemSkuReq.getNick());
        }
        if (StringUtil.isNotBlank(itemSkuReq.getFields())) {
            req.setFields(itemSkuReq.getFields());
        } else {
            req.setFields("created,iid,modified,num_iid,outer_id,price,properties,properties_name,quantity,sku_id,status");
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skuGet shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemSkuGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skuGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSku(response.getSku());
        } catch (ApiException e) {
            logger.error("skuGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ����SKU��Ϣ
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * properties  Sku���Դ�
     * shopId      ����ID
     * ѡ�����
     * quantity    ����޸�ֵ
     * price       Sku�����ۼ۸�
     * outerId     Sku���̼��ⲿid 
     * itemPrice   sku������Ʒ�ļ۸�
     * lang        Sku���ֵİ汾
     */
    public ItemSkuResult skuUpdate(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || StringUtil.isBlank(itemSkuReq.getProperties())) {
            logger.error("skuUpdate param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkuUpdateRequest req = new ItemSkuUpdateRequest();
        if (itemSkuReq.getItemPrice() != null) {
            req.setItemPrice(String.valueOf(itemSkuReq.getItemPrice()));
        }
        if (StringUtil.isNotBlank(itemSkuReq.getOuterId())) {
            req.setOuterId(itemSkuReq.getOuterId());
        }
        if (StringUtil.isNotBlank(itemSkuReq.getLang())) {
            req.setLang(itemSkuReq.getLang());
        }
        if (itemSkuReq.getQuantity() != null) {
            req.setQuantity(itemSkuReq.getQuantity());
        }
        if (itemSkuReq.getPrice() != null) {
            req.setPrice(String.valueOf(itemSkuReq.getPrice()));
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skuUpdate shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemSkuUpdateResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skuUpdate ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSku(response.getSku());
        } catch (ApiException e) {
            logger.error("skuUpdate ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ������ƷSKU�ļ۸�
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * properties  Sku���Դ�
     * shopId      ����ID
     * ѡ�����
     * quantity    ����޸�ֵ
     * price       Sku�����ۼ۸�
     * outerId     Sku���̼��ⲿid 
     * itemPrice   sku������Ʒ�ļ۸�
     * lang        Sku���ֵİ汾
     */
    public ItemSkuResult skuPriceUpdate(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || itemSkuReq.getNumIid() == null || StringUtil.isBlank(itemSkuReq.getProperties())) {
            logger.error("skuPriceUpdate param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkuPriceUpdateRequest req = new ItemSkuPriceUpdateRequest();
        if (itemSkuReq.getItemPrice() != null) {
            req.setItemPrice(String.valueOf(itemSkuReq.getItemPrice()));
        }
        if (StringUtil.isNotBlank(itemSkuReq.getOuterId())) {
            req.setOuterId(itemSkuReq.getOuterId());
        }
        if (StringUtil.isNotBlank(itemSkuReq.getLang())) {
            req.setLang(itemSkuReq.getLang());
        }
        if (itemSkuReq.getQuantity() != null) {
            req.setQuantity(itemSkuReq.getQuantity());
        }
        if (itemSkuReq.getPrice() != null) {
            req.setPrice(String.valueOf(itemSkuReq.getPrice()));
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skuPriceUpdate shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        authorizeManager.shortAuthorize("taobao.item.sku.price.update", sessionKey);
        try {
            ItemSkuPriceUpdateResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skuPriceUpdate ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSku(response.getSku());
        } catch (ApiException e) {
            logger.error("skuPriceUpdate ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * �����ƷID�б��ȡSKU��Ϣ 
     * @param req
     * �������
     * numIids     ��Ʒ����ID
     * shopId      ����ID
     * ѡ�����
     * fields      �践�ص��ֶ���
     */
    public ItemSkuResult skusGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || StringUtil.isBlank(itemSkuReq.getNumIids())) {
            logger.error("skusGet param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemSkusGetRequest req = new ItemSkusGetRequest();
        if (StringUtil.isNotBlank(itemSkuReq.getFields())) {
            req.setFields(itemSkuReq.getFields());
        } else {
            req.setFields("created,iid,modified,num_iid,outer_id,price,properties,properties_name,quantity,sku_id,status");
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skusGet shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemSkusGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skusGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSkus(response.getSkus());
        } catch (ApiException e) {
            logger.error("skusGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ����ⲿIDȡ��ƷSKU
     * @param req
     * �������
     * outerId     ��Ʒ���ⲿ��ƷID
     * shopId      ����ID
     * ѡ�����
     * fields      �����ֶ�
     */
    public ItemSkuResult skusCustomGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemSkuResult res = new ItemSkuResult();
        TaobaoSkuRequest itemSkuReq = (TaobaoSkuRequest) itemRequest;
        if (itemSkuReq == null || itemSkuReq.getShopId() == null || StringUtil.isBlank(itemSkuReq.getOuterId())) {
            logger.error("skusCustomGet param error:itemSkuReq=" + itemSkuReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        SkusCustomGetRequest req = new SkusCustomGetRequest();
        if (StringUtil.isNotBlank(itemSkuReq.getFields())) {
            req.setFields(itemSkuReq.getFields());
        } else {
            req.setFields("created,iid,modified,num_iid,outer_id,price,properties,properties_name,quantity,sku_id,status");
        }
        String sessionKey = commonCache.getSessionKey(itemSkuReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("skusCustomGet shop[" + itemSkuReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemSkuReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            SkusCustomGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("skusCustomGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemSkuReq=" + itemSkuReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setSkus(response.getSkus());
        } catch (ApiException e) {
            logger.error("skusCustomGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ��ȡ�û���������ҳģ����� 
     * @param req
     * �������
     * shopId      ����ID
     * @return
     * @throws UnsupportedOperationException
     */
    public ItemTemplatesResult templatesGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemTemplatesResult res = new ItemTemplatesResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null) {
            logger.error("templatesGet param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemTemplatesGetRequest req = new ItemTemplatesGetRequest();
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("templatesGet shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemTemplatesGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("templatesGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItemTemplates(response.getItemTemplateList());
        } catch (ApiException e) {
            logger.error("templatesGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ��Ʒ�¼�
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * shopId      ����ID
     */
    public ItemResult updateDelisting(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || itemReq.getNumIid() == null) {
            logger.error("updateDelisting param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemUpdateDelistingRequest req = new ItemUpdateDelistingRequest();
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("updateDelisting shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemUpdateDelistingResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("updateDelisting ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("updateDelisting ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * һ�ڼ���Ʒ�ϼ�
     * @param req
     * �������
     * numIid      ��Ʒ����ID
     * num         ��Ҫ�ϼܵ���Ʒ������
     * shopId      ����ID
     */
    public ItemResult updateListing(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || itemReq.getNumIid() == null || itemReq.getNum() == null) {
            logger.error("updateListing param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemUpdateListingRequest req = new ItemUpdateListingRequest();
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("updateListing shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemUpdateListingResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("updateListing ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItem(response.getItem());
        } catch (ApiException e) {
            logger.error("updateListing ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ����ⲿIDȡ��Ʒ
     * @param req
     * �������
     * outerId     ��Ʒ���ⲿ��ƷID
     * shopId      ����ID
     * ѡ�����
     * fields      �����ֶ�
     */
    public ItemResult itemsCustomGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || StringUtil.isBlank(itemReq.getOuterId())) {
            logger.error("itemsCustomGet param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemsCustomGetRequest req = new ItemsCustomGetRequest();
        if (StringUtil.isNotBlank(itemReq.getFields())) {
            req.setFields(itemReq.getFields());
        } else {
            req.setFields("num_iid,title,price,cid,after_sale_id,approve_status,auction_point,auto_fill,cod_postage_id,created," + "delist_time,desc,detail_url,ems_fee,express_fee,freight_payer,has_discount,has_invoice,has_showcase,has_warranty," + "increment,input_pids,input_str,is_3D,is_ex,is_fenxiao,is_lightning_consignment,is_prepay,is_taobao,is_timing," + "is_virtual,is_xinpin,item_img,list_time,location,modified,,nick,num,one_station,outer_id,pic_url,post_fee,postage_id" + "product_id,promoted_service,prop_img,property_alias,props,props_name,score,second_kill,sell_promise,seller_cids,sku" + "stuff_status,sub_stock,template_id,type,valid_thru,video,violation,volume,wap_desc,wap_detail_url,ww_status,");
        }
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("itemsCustomGet shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            ItemsCustomGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("itemsCustomGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItems(response.getItems());
        } catch (ApiException e) {
            logger.error("itemsCustomGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ������Ʒ��Ϣ
     * @param req
     * ѡ�����
     * ̫�࣬�����뿴req
     */
    public ItemResult itemsGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemResult res = new ItemResult();
        TaobaoItemOptRequest itemOptReq = (TaobaoItemOptRequest) itemRequest;
        if (itemOptReq == null || itemOptReq.getShopId() == null || itemOptReq.getItemsGetRequest() == null) {
            logger.error("itemsGet param error:itemOptReq=" + itemOptReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        ItemsGetRequest req = new ItemsGetRequest();
        try {
            ItemsGetResponse response = taobaoClient.execute(req);
            if (!response.isSuccess()) {
                logger.error("itemsGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemOptReq=" + itemOptReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setItems(response.getItems());
            res.setTotalResults(response.getTotalResults());
        } catch (ApiException e) {
            logger.error("itemsGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }

    /**
     * ��Ʒ�Ż������ѯ
     * @param req
     * �������
     * itemId      ��ƷID
     * shopId      ����ID
     * ѡ�����
     * channelKey  ������Դ������վ�� 
     */
    public ItemPromotionsResult umpPromotionGet(ItemRequest itemRequest) throws UnsupportedOperationException {
        ItemPromotionsResult res = new ItemPromotionsResult();
        TaobaoItemRequest itemReq = (TaobaoItemRequest) itemRequest;
        if (itemReq == null || itemReq.getShopId() == null || itemReq.getItemId() == null) {
            logger.error("umpPromotionGet param error:itemReq=" + itemReq);
            res.setError(ResultConstants.RESULT_PARAM_NULL, ResultConstants.RESULT_PARAM_NULL_INFO);
            return res;
        }
        UmpPromotionGetRequest req = new UmpPromotionGetRequest();
        if (StringUtil.isNotBlank(itemReq.getChannelKey())) {
            req.setChannelKey(itemReq.getChannelKey());
        }
        String sessionKey = commonCache.getSessionKey(itemReq.getShopId());
        if (StringUtil.isBlank(sessionKey)) {
            logger.error("umpPromotionGet shop[" + itemReq.getShopId() + "] sessionKey is empty.");
            res.setError(ResultConstants.RESULT_SESSIONKEY_NULL, ";shopId[" + itemReq.getShopId() + "] " + ResultConstants.RESULT_SESSIONKEY_NULL_INFO);
            return res;
        }
        try {
            UmpPromotionGetResponse response = taobaoClient.execute(req, sessionKey);
            if (!response.isSuccess()) {
                logger.error("umpPromotionGet ErrorCode=" + response.getErrorCode() + ";ErrorMsg=" + response.getMsg() + ";itemReq=" + itemReq);
                res.setError(response.getErrorCode(), response.getMsg());
                return res;
            }
            res.setPromotions(response.getPromotions());
        } catch (ApiException e) {
            logger.error("umpPromotionGet ApiException", e);
            res.setError(ResultConstants.RESULT_TAOBAO_TOP_API_ERROR, ResultConstants.RESULT_TAOBAO_TOP_API_ERROR_INFO);
            return res;
        }
        return res;
    }
}
