package fi.arcusys.cygnus.view;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import fi.arcusys.commons.hibernate.dao.DAO;
import fi.arcusys.commons.j2ee.faces.FacesUtil;
import fi.arcusys.commons.j2ee.faces.Outcome;
import fi.arcusys.commons.j2ee.faces.view.AbstractFormBean;
import fi.arcusys.cygnus.Roles;
import fi.arcusys.cygnus.dao.ProductDAO;
import fi.arcusys.cygnus.dao.SupplierDAO;
import fi.arcusys.cygnus.model.Product;
import fi.arcusys.cygnus.model.ProductComponent;
import fi.arcusys.cygnus.model.Supplier;
import fi.arcusys.cygnus.model.delegate.ProductBean;
import fi.arcusys.qnet.common.model.ResourceFileWithData;
import fi.arcusys.qnet.common.model.UserAccount;

public class ProductForm extends AbstractFormBean<Product, Long, ProductBean> {

    private static final long serialVersionUID = 7L;

    private static final Log LOG = LogFactory.getLog(ProductForm.class);

    private Long componentProductId;

    private List<Product> possibleChildProducts;

    private List<SelectItem> possibleChildProductSelectItems;

    private List<File> tempFiles;

    private transient List<Closeable> temporaryStreams;

    public ProductForm() {
        super(Product.class);
    }

    @Override
    protected void onNewEntity() {
        getBean().setRootLevel(true);
    }

    public void setEntity(Product entity) {
        setBean(new ProductBean(entity));
    }

    public Product getEntity() {
        ProductBean bean = getBean();
        return (null != bean) ? bean.getEntity() : null;
    }

    @Override
    protected ProductBean newEntityBeanInstance() {
        return new ProductBean(new Product());
    }

    public UploadedFile getUploadedFile() {
        return null;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        if (null != uploadedFile) {
            ProductBean bean = getBean();
            ResourceFileWithData rf = bean.getDrawingFile();
            if (null == rf) {
                rf = new ResourceFileWithData();
                bean.setDrawingFile(rf);
            }
            Date now = new Date();
            if (null == rf.getCtime()) {
                rf.setCtime(now);
            }
            if (null == rf.getOwner()) {
                rf.setOwner(FacesUtil.getCurrentContextUser(UserAccount.class, false));
            }
            rf.setMtime(now);
            rf.setName(UploadFile.cleanupName(uploadedFile.getName()));
            rf.setContentType(uploadedFile.getContentType());
            rf.setSize(uploadedFile.getSize());
            try {
                BufferedInputStream in = new BufferedInputStream(uploadedFile.getInputStream());
                File f = File.createTempFile("QNRF", ".tmp");
                getTempFiles().add(f);
                f.deleteOnExit();
                rf.setTempFile(f);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
                final byte[] READ_BUF = new byte[1024];
                int read;
                do {
                    read = in.read(READ_BUF);
                    if (read > 0) {
                        out.write(READ_BUF, 0, read);
                    }
                } while (read > 0);
                out.flush();
                out.close();
                in.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            String name = UploadFile.cleanupName(uploadedFile.getName());
            rf.setName(name);
        }
    }

    @Override
    public void afterSaveCommit(DAO<Product> dao) {
        dao.beginTransaction();
    }

    public List<File> getTempFiles() {
        if (null == tempFiles) {
            tempFiles = new ArrayList<File>();
        }
        return tempFiles;
    }

    public void setTempFiles(List<File> tempFiles) {
        this.tempFiles = tempFiles;
    }

    private void deleteTempFiles() {
        for (File f : getTempFiles()) {
            if (f.exists()) {
                f.delete();
            }
        }
    }

    private boolean saveUploadedFile(DAO<?> dao) {
        boolean ok = true;
        ResourceFileWithData rf = getBean().getDrawingFile();
        if (null != rf) {
            File f = rf.getTempFile();
            if (null != f) {
                Long id = rf.getId();
                if (null != id) {
                    ResourceFileWithData existing = dao.get(ResourceFileWithData.class, id);
                    existing.setContentType(rf.getContentType());
                    existing.setCtime(rf.getCtime());
                    existing.setDescription(rf.getDescription());
                    existing.setDirectory(rf.isDirectory());
                    existing.setGroup(rf.getGroup());
                    existing.setMtime(rf.getMtime());
                    existing.setName(rf.getName());
                    existing.setNotes(rf.getNotes());
                    existing.setOwner(rf.getOwner());
                    existing.setParentFile(rf.getParentFile());
                    existing.setSize(rf.getSize());
                    existing.setTag(rf.getTag());
                    existing.setTempFile(rf.getTempFile());
                    rf = existing;
                }
                if (!saveUploadedFile(rf, f)) {
                    ok = false;
                } else {
                    dao.saveOrUpdate(rf);
                    getBean().setDrawingFile(rf);
                }
            }
        }
        return ok;
    }

    private boolean saveUploadedFile(ResourceFileWithData rf, File f) {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            temporaryStreams.add(in);
            rf.setDataBlob(Hibernate.createBlob(in));
        } catch (IOException ex) {
            LOG.error("Failed to save file", ex);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Failed to save file data", ex.getLocalizedMessage()));
            return false;
        } finally {
        }
        return true;
    }

    @Override
    protected void reset() {
        super.reset();
        setUploadedFile(null);
        setComponentProductId(null);
        setPossibleChildProducts(null);
        setPossibleChildProductSelectItems(null);
        setTempFiles(null);
    }

    @Override
    public String newEntity() {
        FacesUtil.checkUserInRole(Roles.ROLE_MANAGER);
        return super.newEntity();
    }

    @Override
    public String editEntity() {
        FacesUtil.checkUserInRole(Roles.ROLE_MANAGER);
        return super.editEntity();
    }

    @Override
    protected void onShowOrEditEntity() {
        ProductBean bean = getBean();
        Hibernate.initialize(bean.getSuppliers());
    }

    @Override
    public String save() {
        try {
            temporaryStreams = new ArrayList<Closeable>();
            return super.save();
        } finally {
            for (Closeable c : temporaryStreams) {
                try {
                    c.close();
                } catch (Exception ex) {
                }
            }
            temporaryStreams.clear();
            temporaryStreams = null;
            deleteTempFiles();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean beforeSaveMerge(DAO<Product> dao) {
        FacesUtil.checkUserInRole(Roles.ROLE_MANAGER);
        ProductDAO pdao = new ProductDAO(dao.getCurrentSession());
        ProductBean b = getBean();
        String code = b.getCode();
        Product existing = pdao.forCode(code);
        if (null != existing) {
            if (null == b.getId() || !existing.getId().equals(b.getId())) {
                FacesMessage msg = new FacesMessage();
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                msg.setSummary("Tuotekoodi on jo käytössä");
                msg.setDetail("Tuotekoodi '" + code + "' on jo käytössä");
                FacesContext.getCurrentInstance().addMessage("productForm:code", msg);
                return false;
            }
        }
        updateSuppliers(b);
        return saveUploadedFile(dao);
    }

    private void updateSuppliers(ProductBean b) {
        HashSet<Long> newSupplierIds = new HashSet<Long>();
        for (Long sid : b.getSupplierIds()) {
            newSupplierIds.add(sid);
        }
        HashSet<Supplier> toRemoveSuppliers = new HashSet<Supplier>();
        Set<Supplier> currentSuppliers = b.getSuppliers();
        if (null != currentSuppliers) {
            for (Supplier s : currentSuppliers) {
                Long sid = s.getId();
                if (!newSupplierIds.contains(sid)) {
                    toRemoveSuppliers.add(s);
                } else {
                    newSupplierIds.remove(sid);
                }
            }
        } else {
            currentSuppliers = new HashSet<Supplier>();
            b.setSuppliers(currentSuppliers);
        }
        SupplierDAO dao = new SupplierDAO();
        currentSuppliers.removeAll(toRemoveSuppliers);
        for (Long sid : newSupplierIds) {
            Supplier s = dao.get(sid);
            currentSuppliers.add(s);
        }
    }

    public Long getComponentProductId() {
        return componentProductId;
    }

    public void setComponentProductId(Long l) {
        this.componentProductId = l;
    }

    static Product findProductReferringProduct(Product rootProduct, Long productId) {
        Product foundProduct;
        if (rootProduct.getId().equals(productId)) {
            foundProduct = rootProduct;
        } else {
            foundProduct = null;
            List<ProductComponent> components = rootProduct.getComponents();
            if (null != components) {
                for (ProductComponent pc : components) {
                    Product p = pc.getProduct();
                    foundProduct = findProductReferringProduct(p, productId);
                    if (null != foundProduct) {
                        break;
                    }
                }
            }
        }
        return foundProduct;
    }

    @SuppressWarnings("unchecked")
    public List<Product> getPossibleChildProducts() {
        if (null == possibleChildProducts) {
            List<ProductComponent> existing = getBean().getComponents();
            Set<Long> existingIds = new HashSet<Long>();
            for (ProductComponent pc : existing) {
                existingIds.add(pc.getProduct().getId());
            }
            ProductDAO pdao = new ProductDAO();
            pdao.beginTransaction();
            existingIds.add(getBean().getId());
            List<Product> available = (List<Product>) pdao.createCriteria().add(Restrictions.eq("rootLevel", false)).add(Restrictions.not(Restrictions.in("id", existingIds))).addOrder(Order.asc("code")).list();
            ListIterator<Product> it = available.listIterator();
            while (it.hasNext()) {
                Product p = it.next();
                if (null != findProductReferringProduct(p, getBean().getId())) {
                    it.remove();
                }
            }
            possibleChildProducts = new ArrayList<Product>();
            possibleChildProducts.addAll(available);
            pdao.commitTransaction();
        }
        return possibleChildProducts;
    }

    public void setPossibleChildProducts(List<Product> l) {
        this.possibleChildProducts = l;
    }

    public List<SelectItem> getPossibleChildProductSelectItems() {
        if (null == possibleChildProductSelectItems) {
            possibleChildProductSelectItems = new ArrayList<SelectItem>();
            for (Product p : getPossibleChildProducts()) {
                SelectItem si = new SelectItem();
                si.setValue(p.getId());
                si.setLabel(p.getCodeAndName());
                possibleChildProductSelectItems.add(si);
            }
        }
        return possibleChildProductSelectItems;
    }

    public void setPossibleChildProductSelectItems(List<SelectItem> possibleNewComponentSelectItems) {
        this.possibleChildProductSelectItems = possibleNewComponentSelectItems;
    }

    public String addComponent() {
        Long pid = getComponentProductId();
        if (null == pid) {
            LOG.error("componentProductId == null");
            return Outcome.ERROR;
        } else {
            ProductDAO pdao = new ProductDAO();
            Product p = pdao.get(pid);
            if (null == p) {
                LOG.error("No such Product exists: id = " + pid);
                return Outcome.ERROR;
            } else {
                ProductComponent pc = new ProductComponent();
                pc.setProduct(p);
                pc.setOwner(getEntity());
                pc.setQuantity(1);
                getBean().getComponents().add(pc);
                setComponentProductId(null);
                setPossibleChildProducts(null);
                setPossibleChildProductSelectItems(null);
                return Outcome.OK;
            }
        }
    }

    public String removeComponent() {
        Long pid = getComponentProductId();
        if (null == pid) {
            LOG.error("componentProductId == null");
            return Outcome.ERROR;
        } else {
            ListIterator<ProductComponent> it = getBean().getComponents().listIterator();
            while (it.hasNext()) {
                ProductComponent pc = it.next();
                if (pc.getProduct().getId().equals(pid)) {
                    it.remove();
                    break;
                }
            }
            return Outcome.OK;
        }
    }

    public String showComponent() {
        ProductDAO dao = new ProductDAO();
        Product p = dao.get(getComponentProductId());
        reset();
        setBean(new ProductBean(p));
        onShowOrEditEntity();
        return "show_Component";
    }
}
