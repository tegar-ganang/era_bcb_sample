package com.app.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.dao.ProductDao;
import com.app.entity.Images;
import com.app.entity.Product;
import com.app.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProductDao productDao;

    @Transactional(readOnly = false)
    public void deleteProduct(String idStr) {
        productDao.deleteProduct(idStr);
    }

    public Collection<Product> findProductByParameter(Product product) {
        List<Product> productList = (List<Product>) productDao.findProductByParameter(product);
        Collections.sort(productList, new Comparator<Product>() {

            public int compare(Product p1, Product p2) {
                if (!StringUtils.isBlank(p1.getNo()) && !StringUtils.isBlank(p2.getNo())) {
                    if (p1.getNo().length() == p2.getNo().length()) return p1.getNo().compareTo(p2.getNo());
                    if (p1.getNo().length() < p2.getNo().length()) return -1;
                    if (p1.getNo().length() > p2.getNo().length()) return 1;
                }
                return 1;
            }
        });
        return productList;
    }

    public Product load(int id) {
        return productDao.load(id);
    }

    @Transactional(readOnly = false)
    public void saveOrUpdateProduct(Product product, File[] doc, String[] docFileName, String[] docContentType) throws IOException {
        logger.info("addOrUpdateProduct()");
        List<Images> imgList = new ArrayList<Images>();
        InputStream in = null;
        OutputStream out = null;
        String saveDirectory = ServletActionContext.getServletContext().getRealPath("common/userfiles/image/");
        if (doc != null && doc.length > 0) {
            File uploadPath = new File(saveDirectory);
            if (!uploadPath.exists()) uploadPath.mkdirs();
            for (int i = 0; i < doc.length; i++) {
                Images img = new Images();
                in = new FileInputStream(doc[i]);
                img.setName(docFileName[i].substring(0, docFileName[i].lastIndexOf(".")));
                img.setRenameAs(docFileName[i]);
                imgList.add(img);
                out = new FileOutputStream(saveDirectory + "/" + img.getRenameAs());
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                out.flush();
            }
        }
        product.setImagesCollection(imgList);
        productDao.saveOrUpdateProduct(product);
        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                logger.info("addOrUpdateProduct() **********" + e.getStackTrace());
                e.printStackTrace();
            }
        }
    }

    @Transactional(readOnly = false)
    public int updateOnline(String idStr, int value) {
        return productDao.updateOnline(idStr, value);
    }
}
