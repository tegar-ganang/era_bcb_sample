package com.ncs.flex.server.control;

import com.ncs.flex.server.util.UUIDHelper;
import com.ncs.flex.server.service.ImageService;
import com.ncs.flex.server.service.OrderService;
import com.ncs.flex.to.ImageTO;
import com.ncs.flex.to.OrderTO;
import com.ncs.flex.vo.OrderJoinVO;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@Scope("prototype")
public class FileUpload {

    private final Logger log = Logger.getLogger(FileUpload.class);

    @Resource
    private ImageService imageService;

    @Resource
    private OrderService orderService;

    @RequestMapping(value = "/flex/fileUpload.do", method = RequestMethod.POST)
    public void fileUpload3(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("FileUpload : flex/fileUpload.do run...");
        String orderId = request.getParameter("orderId");
        String subjectId = request.getParameter("orderId");
        String seq = request.getParameter("seq");
        log.info("=========subjectId is :" + subjectId + ", orderId is:" + orderId + ",seq is :" + seq);
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");
        if (file == null) {
            throw new Exception("upload failed ：there's no file(s)!");
        }
        String fileName = file.getOriginalFilename();
        String realPath = request.getSession().getServletContext().getRealPath("/");
        String fullPath = realPath + "/uploadFiles/weddingSoftImg/dress/" + subjectId;
        String relatedPath = "uploadFiles/weddingSoftImg/dress/" + subjectId;
        log.info("upload path : fullPath:" + fullPath);
        File uploadFile = new File(fullPath);
        if (!uploadFile.exists()) {
            uploadFile.mkdirs();
        }
        String path = String.valueOf(new Date().getTime());
        String postfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        String pathFileName = path + postfix;
        log.info("SaveFileFromInputStream");
        SaveFileFromInputStream(file.getInputStream(), fullPath, pathFileName);
        log.info("save image to database");
        ImageTO img = new ImageTO();
        img.setImageId(UUIDHelper.getRandomUUID());
        img.setPath("../" + relatedPath + "/" + pathFileName);
        img.setSeq(seq);
        OrderTO order = new OrderTO();
        order.setOrderId(orderId);
        img.setOrder(order);
        imageService.create(img);
        log.info("FileUpload : flex/fileUpload.do end...");
    }

    @RequestMapping(value = "/flex/generateOrderExcel.do", method = RequestMethod.POST)
    public void generateOrderExcel(HttpServletRequest request, HttpServletResponse response) {
        log.info("generateOrderExcel run...");
        try {
            String orderIds = request.getParameter("orderIds");
            List sortedOrderList = getSortedOrders(orderIds);
            Workbook wb = new HSSFWorkbook();
            Sheet sheet = wb.createSheet("sheet1");
            createExcelHeaderRow(sheet);
            OrderJoinVO vo = null;
            for (int i = 0; i < sortedOrderList.size(); i++) {
                vo = (OrderJoinVO) sortedOrderList.get(i);
                createExcelRecord(vo, sheet, i);
            }
            calculateTotalAmount(sheet, sortedOrderList.size());
            response.setContentType("application/vnd.ms-excel; charset=UTF-8");
            String fileName = String.valueOf(new Date().getTime()) + ".xls";
            response.setHeader("Content-Disposition", "filename=" + fileName);
            response.setHeader("Cache-Control", "no-cache");
            wb.write(response.getOutputStream());
        } catch (IOException e) {
            log.error(e.toString());
        }
        log.info("generateOrderExcel end...");
    }

    public void SaveFileFromInputStream(InputStream stream, String path, String filename) throws IOException {
        FileOutputStream fs = new FileOutputStream(path + "/" + filename);
        byte[] buffer = new byte[1024 * 1024];
        int bytesum = 0;
        int byteread = 0;
        while ((byteread = stream.read(buffer)) != -1) {
            bytesum += byteread;
            fs.write(buffer, 0, byteread);
            fs.flush();
        }
        fs.close();
        stream.close();
    }

    private void calculateTotalAmount(Sheet sheet, int dataSize) {
        Row row = sheet.createRow(dataSize + 1);
        Font font = sheet.getWorkbook().createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short) 10);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font);
        Cell text = row.createCell(8);
        text.setCellValue("总金额(元):");
        text.setCellStyle(style);
        Cell totalAmount = row.createCell(9);
        totalAmount.setCellFormula("SUM(J2:J" + String.valueOf(dataSize + 1) + ")");
        totalAmount.setCellStyle(style);
    }

    private void createExcelHeaderRow(Sheet sheet) {
        Row row = sheet.createRow(0);
        Font font = sheet.getWorkbook().createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setFontHeightInPoints((short) 10);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font);
        Cell no = row.createCell(0);
        no.setCellValue("NO");
        no.setCellStyle(style);
        Cell orderNumber = row.createCell(1);
        orderNumber.setCellValue("订单号");
        orderNumber.setCellStyle(style);
        Cell customerName = row.createCell(2);
        customerName.setCellValue("客户");
        customerName.setCellStyle(style);
        Cell machineRecipe = row.createCell(3);
        machineRecipe.setCellValue("车工");
        machineRecipe.setCellStyle(style);
        Cell handRecipe = row.createCell(4);
        handRecipe.setCellValue("手工");
        handRecipe.setCellStyle(style);
        Cell cutRecipe = row.createCell(5);
        cutRecipe.setCellValue("裁剪");
        cutRecipe.setCellStyle(style);
        Cell orderDate = row.createCell(6);
        orderDate.setCellValue("订货日期");
        orderDate.setCellStyle(style);
        Cell dueDate = row.createCell(7);
        dueDate.setCellValue("到货日期");
        dueDate.setCellStyle(style);
        Cell closeDate = row.createCell(8);
        closeDate.setCellValue("完成日期");
        closeDate.setCellStyle(style);
        Cell totalAmount = row.createCell(9);
        totalAmount.setCellValue("订单金额");
        totalAmount.setCellStyle(style);
        Cell orderProgress = row.createCell(10);
        orderProgress.setCellValue("状态");
        orderProgress.setCellStyle(style);
    }

    private void createExcelRecord(OrderJoinVO vo, Sheet sheet, int pos) {
        Row row = sheet.createRow(pos + 1);
        Cell no = row.createCell(0);
        no.setCellValue(String.valueOf(pos + 1));
        Cell orderNumber = row.createCell(1);
        orderNumber.setCellValue(vo.getOrderNumber());
        Cell customerName = row.createCell(2);
        customerName.setCellValue(vo.getCustomerName());
        Cell machineRecipe = row.createCell(3);
        if (vo.getMachineRecipe() != null) {
            machineRecipe.setCellValue(vo.getMachineRecipe());
        }
        Cell handRecipe = row.createCell(4);
        if (vo.getHandRecipe() != null) {
            handRecipe.setCellValue(vo.getHandRecipe());
        }
        Cell cutRecipe = row.createCell(5);
        if (vo.getCutRecipe() != null) {
            cutRecipe.setCellValue(vo.getCutRecipe());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Cell orderDate = row.createCell(6);
        orderDate.setCellValue(sdf.format(vo.getOrderDate()));
        Cell dueDate = row.createCell(7);
        dueDate.setCellValue(sdf.format(vo.getDueDate()));
        Cell closeDate = row.createCell(8);
        if (vo.getCloseDate() != null) {
            closeDate.setCellValue(sdf.format(vo.getCloseDate()));
        }
        Cell totalAmount = row.createCell(9);
        if (vo.getTotalAmount() != null && !"".equals(vo.getTotalAmount())) {
            totalAmount.setCellValue(Integer.valueOf(vo.getTotalAmount()));
        }
        Cell orderProgress = row.createCell(10);
        String sOrderProgress = vo.getOrderProgress();
        if (!"".equals(sOrderProgress) || sOrderProgress != "") {
            if ("Wait".equals(sOrderProgress)) {
                orderProgress.setCellValue("待确定报价");
            } else if ("Pend".equals(sOrderProgress)) {
                orderProgress.setCellValue("报价确定,制作中");
            } else if ("Done".equals(sOrderProgress)) {
                orderProgress.setCellValue("已完成");
            } else if ("Charge".equals(sOrderProgress)) {
                orderProgress.setCellValue("已付款");
            } else if ("Cancel".equals(sOrderProgress)) {
                orderProgress.setCellValue("已取消");
            } else if ("Reject".equals(sOrderProgress)) {
                orderProgress.setCellValue("已退货");
            }
        }
    }

    private List getSortIds(String orderIds) {
        List<String> result = new ArrayList<String>();
        String[] s = orderIds.split(",");
        for (int i = 0; i < s.length; i++) {
            String s3 = s[i].replace("'", "");
            result.add(s3);
        }
        return result;
    }

    private List getSortedOrders(String orderIds) {
        List result = new ArrayList();
        List orderList = orderService.getOrderVODetailByIds(orderIds);
        List sortedIds = getSortIds(orderIds);
        OrderJoinVO vo = null;
        for (int i = 0; i < sortedIds.size(); i++) {
            String orderId = (String) sortedIds.get(i);
            for (int j = 0; j < orderList.size(); j++) {
                vo = (OrderJoinVO) orderList.get(j);
                if (orderId.equals(vo.getOrderId())) {
                    result.add(vo);
                    break;
                }
            }
        }
        return result;
    }
}
