package action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import pojo.Category;
import pojo.Item;
import pojo.Itemstatus;
import pojo.Store;
import pojo.User;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import dao.CategoryDao;
import dao.ItemDao;
import dao.ItemstatusDao;

public class AddItemAction extends ActionSupport implements ServletRequestAware {

    private Item item = new Item();

    private File itemImage;

    private String itemImageContentType;

    private String itemImageFileName;

    private List<Category> categorys;

    private String idCat;

    private String storeId;

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Item getItem() {
        return item;
    }

    public List<Category> getCategorys() {
        categorys = CategoryDao.getAll();
        return categorys;
    }

    public String getIdCat() {
        return idCat;
    }

    public void setIdCat(String idCat) {
        this.idCat = idCat;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public File getItemImage() {
        return itemImage;
    }

    public void setItemImage(File itemImage) {
        this.itemImage = itemImage;
    }

    public String getItemImageContentType() {
        return itemImageContentType;
    }

    public void setItemImageContentType(String itemImageContentType) {
        this.itemImageContentType = itemImageContentType;
    }

    public String getItemImageFileName() {
        return itemImageFileName;
    }

    public void setItemImageFileName(String itemImageFileName) {
        this.itemImageFileName = itemImageFileName;
    }

    private HttpServletRequest servletRequest;

    /**
	 * 
	 */
    private static final long serialVersionUID = -7129448652934921765L;

    @Override
    public void setServletRequest(HttpServletRequest req) {
        this.servletRequest = req;
    }

    @Override
    public String execute() {
        Map session = ActionContext.getContext().getSession();
        User user = (User) session.get("user");
        if (user == null) {
            return ERROR;
        }
        if (item.getName() != null || itemImage != null) {
            if (UploadImage()) {
                setData();
                ItemDao.save(item);
                return SUCCESS;
            }
            return ERROR;
        }
        return ERROR;
    }

    public void setData() {
        Itemstatus itemstatus = ItemstatusDao.get("Open");
        item.setItemstatus(itemstatus);
        item.setImage(itemImageFileName);
        int id = Integer.parseInt(idCat);
        Category cat = new Category();
        cat.setId(id);
        item.setCategory(cat);
        Store store = new Store();
        if (storeId != null) {
            int stid = Integer.parseInt(storeId);
            store.setId(stid);
        }
        item.setStore(store);
    }

    public boolean UploadImage() {
        try {
            String filePath = servletRequest.getRealPath("/") + "images\\";
            File fileToCreate = new File(filePath, this.itemImageFileName);
            FileUtils.copyFile(this.itemImage, fileToCreate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
