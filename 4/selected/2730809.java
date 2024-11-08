package com.tenline.pinecone.platform.web.service.integration;

import static org.junit.Assert.assertEquals;
import java.util.logging.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.tenline.pinecone.platform.model.Category;
import com.tenline.pinecone.platform.model.Device;
import com.tenline.pinecone.platform.model.Driver;
import com.tenline.pinecone.platform.model.Item;
import com.tenline.pinecone.platform.model.User;
import com.tenline.pinecone.platform.model.Variable;
import com.tenline.pinecone.platform.sdk.CategoryAPI;
import com.tenline.pinecone.platform.sdk.DeviceAPI;
import com.tenline.pinecone.platform.sdk.DriverAPI;
import com.tenline.pinecone.platform.sdk.ItemAPI;
import com.tenline.pinecone.platform.sdk.UserAPI;
import com.tenline.pinecone.platform.sdk.VariableAPI;
import com.tenline.pinecone.platform.sdk.development.APIResponse;

/**
 * @author Bill
 *
 */
public class FishShowApplicationIntegrationTest extends AbstractServiceIntegrationTest {

    private User user;

    private Category category;

    private Driver driver;

    private Device device;

    private Variable variable;

    private Item item;

    private UserAPI userAPI;

    private CategoryAPI categoryAPI;

    private DriverAPI driverAPI;

    private DeviceAPI deviceAPI;

    private VariableAPI variableAPI;

    private ItemAPI itemAPI;

    @Before
    public void testSetup() throws Exception {
        category = new Category();
        category.setType(Category.COM);
        driver = new Driver();
        driver.setName("智能鱼缸");
        user = new User();
        user.setName("bill");
        device = new Device();
        device.setDefault(false);
        device.setStatus(Device.OPENED);
        variable = new Variable();
        variable.setName("鱼缸水温");
        variable.setType("read_write_discrete");
        item = new Item();
        item.setText("12°C");
        item.setValue("23810".getBytes());
        deviceAPI = new DeviceAPI("localhost", "8888", "service");
        variableAPI = new VariableAPI("localhost", "8888", "service");
        itemAPI = new ItemAPI("localhost", "8888", "service");
        categoryAPI = new CategoryAPI("localhost", "8888", "service");
        driverAPI = new DriverAPI("localhost", "8888", "service");
        userAPI = new UserAPI("localhost", "8888", "service");
        APIResponse response = categoryAPI.create(category);
        if (response.isDone()) {
            category = (Category) response.getMessage();
            assertEquals(Category.COM, category.getType());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        driver.setCategory(category);
        response = driverAPI.create(driver);
        if (response.isDone()) {
            driver = (Driver) response.getMessage();
            assertEquals("智能鱼缸", driver.getName());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        response = userAPI.create(user);
        if (response.isDone()) {
            user = (User) response.getMessage();
            assertEquals("bill", user.getName());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        device.setDriver(driver);
        device.setUser(user);
    }

    @After
    public void testShutdown() throws Exception {
        APIResponse response = categoryAPI.delete(category.getId());
        if (response.isDone()) {
            assertEquals("Category Deleted!", response.getMessage().toString());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        response = userAPI.delete(user.getId());
        if (response.isDone()) {
            assertEquals("User Deleted!", response.getMessage().toString());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        user = null;
        category = null;
        driver = null;
        device = null;
        variable = null;
        item = null;
        userAPI = null;
        categoryAPI = null;
        driverAPI = null;
        deviceAPI = null;
        variableAPI = null;
        itemAPI = null;
    }

    @Test
    public void test() throws Exception {
        APIResponse response = deviceAPI.create(device);
        if (response.isDone()) {
            device = (Device) response.getMessage();
            assertEquals(false, device.isDefault());
            assertEquals(Device.OPENED, device.getStatus());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        variable.setDevice(device);
        response = variableAPI.create(variable);
        if (response.isDone()) {
            variable = (Variable) response.getMessage();
            assertEquals("鱼缸水温", variable.getName());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("12°C", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item = new Item();
        item.setText("13°C");
        item.setValue("21250".getBytes());
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("13°C", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        variable = new Variable();
        variable.setName("制氧频率");
        variable.setType("write_discrete");
        variable.setDevice(device);
        response = variableAPI.create(variable);
        if (response.isDone()) {
            variable = (Variable) response.getMessage();
            assertEquals("制氧频率", variable.getName());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item = new Item();
        item.setText("1-5");
        item.setValue("281".getBytes());
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("1-5", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item = new Item();
        item.setText("1-6");
        item.setValue("286".getBytes());
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("1-6", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        variable = new Variable();
        variable.setName("鱼缸状态");
        variable.setType("read_discrete");
        variable.setDevice(device);
        response = variableAPI.create(variable);
        if (response.isDone()) {
            variable = (Variable) response.getMessage();
            assertEquals("鱼缸状态", variable.getName());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item = new Item();
        item.setText("离线");
        item.setValue("0".getBytes());
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("离线", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
        item = new Item();
        item.setText("在线");
        item.setValue("1".getBytes());
        item.setVariable(variable);
        response = itemAPI.create(item);
        if (response.isDone()) {
            item = (Item) response.getMessage();
            assertEquals("在线", item.getText());
        } else {
            logger.log(Level.SEVERE, response.getMessage().toString());
        }
    }
}
