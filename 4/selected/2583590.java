package mx.com.juca.store.web.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import mx.com.juca.store.business.dto.CategoryDTO;
import mx.com.juca.store.business.dto.ProductDTO;
import mx.com.juca.store.business.exception.ApplicationException;
import mx.com.juca.store.business.service.ICatalogsService;
import mx.com.juca.store.util.GenericConstants;
import mx.com.juca.store.web.BaseCustomController;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 
 * @author Juan Carlos Cruz
 * @since Feb 20, 2011
 */
@Controller
@RequestMapping(value = "/products/*")
public class ProductsController extends BaseCustomController {

    private static final Logger log = Logger.getLogger(ProductsController.class);

    @Autowired
    private ICatalogsService catalogsService;

    @RequestMapping(value = "showProductDetails.do")
    public String showProductDetails(HttpServletRequest request, ModelMap modelMap, @RequestParam("idProduct") final Integer idProduct) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        log.debug("show product details for product " + idProduct);
        try {
            ProductDTO productDTO = catalogsService.getProductById(idProduct);
            modelMap.put(GenericConstants.KEY_SINGLE_ITEM, productDTO);
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        }
        return "products/product_details";
    }

    @RequestMapping(value = "searchProducts.do", method = RequestMethod.GET)
    public String searchProducts(HttpServletRequest request, ModelMap modelMap) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        log.debug("show screen search products");
        List<CategoryDTO> list = new ArrayList<CategoryDTO>();
        try {
            if (this.getSessionObject(request, GenericConstants.KEY_LIST_ITEMS) == null) {
                list = this.catalogsService.getAllCategories();
                this.putObjectInSession(request, GenericConstants.KEY_LIST_ITEMS, list);
            }
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        }
        modelMap.put("CategoryDTO", new CategoryDTO());
        return "products/search_products";
    }

    @RequestMapping(value = "searchProducts.do", method = RequestMethod.POST)
    public String showSearchProductsResults(HttpServletRequest request, ModelMap modelMap, @ModelAttribute("CategoryDTO") final CategoryDTO categoryDTO, @RequestParam(value = "submitted", required = true) final Boolean formSubmitted) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        log.debug("show screen search products with results");
        List<ProductDTO> list = new ArrayList<ProductDTO>();
        try {
            list = this.catalogsService.getProductsByCategory(categoryDTO);
            modelMap.put(GenericConstants.KEY_LIST_ITEMS, list);
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        }
        modelMap.put("CategoryDTO", categoryDTO);
        modelMap.put("formWasSubmitted", formSubmitted);
        return "products/search_products";
    }

    @RequestMapping(value = "listAllProducts.htm", method = RequestMethod.GET)
    public String getAllProducts(HttpServletRequest request, ModelMap modelMap, @RequestParam(value = "maxResults", required = false) final Integer maxResults) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        log.debug("show all products in inventory");
        List<ProductDTO> list = new ArrayList<ProductDTO>();
        try {
            list = this.catalogsService.getAllProducts(maxResults);
            modelMap.put(GenericConstants.KEY_LIST_ITEMS, list);
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        }
        modelMap.put("ProductDTO", new ProductDTO());
        populateCategoriesAndBrands(modelMap);
        return "admin/products/list_products";
    }

    @RequestMapping(value = "searchProductsByCriteria.do", method = RequestMethod.POST)
    public String searchProducts(HttpServletRequest request, ModelMap modelMap, @ModelAttribute("ProductDTO") final ProductDTO productDTO, @RequestParam("idBrand") final Integer brand, @RequestParam("idCategory") final Integer category) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        log.debug("show products in inventory which look like " + productDTO);
        log.debug("brand= " + brand + ", category=" + category);
        List<ProductDTO> list = new ArrayList<ProductDTO>();
        productDTO.getBrandDTO().setIdBrand(brand);
        productDTO.getCategoryDTO().setIdCategory(category);
        try {
            list = this.catalogsService.getProductsByExample(productDTO);
            modelMap.put(GenericConstants.KEY_LIST_ITEMS, list);
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        }
        modelMap.put("ProductDTO", productDTO);
        populateCategoriesAndBrands(modelMap);
        return "admin/products/list_products";
    }

    @RequestMapping(value = "showProductDetailsForUpdate.htm", method = RequestMethod.GET)
    public String showProductDetailsForUpdate(HttpServletRequest request, ModelMap modelMap, @RequestParam(value = "idProduct") final Integer idProduct) {
        this.showProductDetails(request, modelMap, idProduct);
        modelMap.put("ProductDTO", modelMap.get(GenericConstants.KEY_SINGLE_ITEM));
        modelMap.put("Operation", "UPDATE");
        populateCategoriesAndBrands(modelMap);
        return "admin/products/add_update_product";
    }

    @RequestMapping(value = "addProduct.htm", method = RequestMethod.GET)
    public String showAddProductScreen(HttpServletRequest request, ModelMap modelMap) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        modelMap.put("ProductDTO", new ProductDTO());
        modelMap.put("Operation", "ADD");
        populateCategoriesAndBrands(modelMap);
        return "admin/products/add_update_product";
    }

    @RequestMapping(value = "saveOrUpdateProduct.do", method = RequestMethod.POST)
    public String saveUpdateProduct(HttpServletRequest request, ModelMap modelMap, @ModelAttribute("ProductDTO") final ProductDTO productDTO, @RequestParam("productImage") final MultipartFile productImage, @RequestParam("brand") final Integer brand, @RequestParam("category") final Integer category) {
        if (!this.isLoggedIn(request)) {
            return "redirect:/authentication/notLoggedIn.do";
        }
        Boolean result = false;
        File fileProductImage = new File(request.getSession().getServletContext().getRealPath("/img/products/") + File.separatorChar + productDTO.getCode() + ".JPG");
        File dirProductImagesPj = new File("D:/Desarrollos_Experimentos/Experimentos/little-store/src/main/webapp/img/products/");
        log.debug("Save or Update products");
        log.debug("fileProductImage=" + fileProductImage);
        productDTO.getBrandDTO().setIdBrand(brand);
        productDTO.getCategoryDTO().setIdCategory(category);
        log.debug("productDTO= " + productDTO + ", idBrand= " + brand + ", idCategory=" + category);
        try {
            if (productDTO.getIdProduct() == null) {
                result = this.catalogsService.addProduct(productDTO);
            } else {
                result = this.catalogsService.modifyProduct(productDTO);
            }
            if (result) {
                log.debug("Uploading Image");
                log.debug("FileUpload= " + productImage + ", " + productImage.getContentType() + ", " + productImage.getName() + ", " + productImage.getOriginalFilename() + ", " + productImage.getSize());
                FileUtils.writeByteArrayToFile(fileProductImage, productImage.getBytes());
                FileUtils.copyFileToDirectory(fileProductImage, dirProductImagesPj);
            }
        } catch (ApplicationException ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_ERROR, "ErrorCode " + ex.getErrorCode() + ": " + ex.getErrorMessage());
        } catch (Exception ex) {
            log.error(ex);
            modelMap.put(GenericConstants.KEY_FATAL, ex.getMessage());
        }
        if (modelMap.get(GenericConstants.KEY_ERROR) == null) {
            modelMap.put(GenericConstants.KEY_INFO, productDTO.getName());
        }
        return "admin/products/add_update_result";
    }

    private void populateCategoriesAndBrands(ModelMap modelMap) {
        try {
            modelMap.put(GenericConstants.KEY_LIST_CATEGORIES, catalogsService.getAllCategories());
        } catch (ApplicationException ex) {
            log.error(ex);
        }
        try {
            modelMap.put(GenericConstants.KEY_LIST_BRANDS, catalogsService.getAllBrands());
        } catch (ApplicationException ex) {
            log.error(ex);
        }
    }
}
