package com.zhiyun.estore.website.action;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import com.zhiyun.estore.common.utils.ExFileUtils;
import com.zhiyun.estore.common.utils.NamingConstants;
import com.zhiyun.estore.common.utils.URLConstants;
import com.zhiyun.estore.common.vo.EbItem;
import com.zhiyun.estore.common.vo.EbOrder;
import com.zhiyun.estore.common.vo.EbUser;
import com.zhiyun.estore.common.action.BaseActionSupport;
import com.zhiyun.estore.website.service.ItemService;
import com.zhiyun.estore.website.service.OrderService;

public class RoomAction extends BaseActionSupport {

    private static final long serialVersionUID = -3003610123382536277L;

    private OrderService orderService;

    private ItemService itemService;

    private List<File> docs;

    private List<String> docsFileName;

    private List<String> docsContentType;

    private EbItem item;

    private EbOrder order;

    private String msg;

    private Random generator = new Random();

    public List<File> getDocs() {
        return docs;
    }

    public void setDocs(List<File> docs) {
        this.docs = docs;
    }

    public List<String> getDocsFileName() {
        return docsFileName;
    }

    public void setDocsFileName(List<String> docsFileName) {
        this.docsFileName = docsFileName;
    }

    public List<String> getDocsContentType() {
        return docsContentType;
    }

    public void setDocsContentType(List<String> docsContentType) {
        this.docsContentType = docsContentType;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public EbItem getItem() {
        return item;
    }

    public void setItem(EbItem item) {
        this.item = item;
    }

    public EbOrder getOrder() {
        return order;
    }

    public void setOrder(EbOrder order) {
        this.order = order;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String upload() throws IOException {
        if (!userLogined()) {
            return LOGIN;
        }
        try {
            item = itemService.getBy("id", "CLW0001", null).get(0);
            if (item == null || item.getPrice() == null) {
                msg = "目前无可订商品";
                docs = null;
                order = null;
                return INPUT;
            }
            if (docs == null || docs.size() != 12 || order == null) {
                msg = "订单信息不全, 请重新填写表单";
                docs = null;
                order = null;
                return INPUT;
            }
            String saving_path = URLConstants.get(NamingConstants.FILES_SAVE_PATH) + (new Date().getTime()) + "/";
            ExFileUtils.createFolder(saving_path);
            int i = 0;
            for (File doc : docs) {
                if (doc != null && docsFileName.get(i) != null && !"".equals(docsFileName.get(i))) {
                    String name = saving_path + "f" + generator.nextInt(100) + docsFileName.get(i++);
                    FileUtils.copyFile(doc, new File(name));
                } else {
                    msg = "上传文件少于指定数量, 请重新上传文件";
                    docs = null;
                    order = null;
                    return INPUT;
                }
            }
            String zipName = String.valueOf(new Date().getTime()) + ".zip";
            String zipLocalPath = URLConstants.get(NamingConstants.FILES_SAVE_PATH) + zipName;
            String zipUrlPath = URLConstants.get(NamingConstants.FILES_DOWNLOAD_URL) + zipName;
            ExFileUtils.createZipAnt(saving_path, zipLocalPath);
            order.setId("CLW" + String.valueOf(new Date().getTime()));
            order.setEbUser((EbUser) getSession().getAttribute("user"));
            order.setItem(item);
            order.setPicsPath(zipUrlPath);
            order.setStatus(new Integer(0));
            order.setCreateTime(new Date());
            order.setUpdateTime(new Date());
            orderService.save(order);
            docs = null;
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            msg = "保存上传文件失败, 请重新提交";
            docs = null;
            order = null;
            return INPUT;
        }
    }
}
