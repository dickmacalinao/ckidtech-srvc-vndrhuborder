package com.ckidtech.quotation.service.order.service;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ckidtech.quotation.service.core.controller.QuotationResponse;
import com.ckidtech.quotation.service.core.dao.AppUserRepository;
import com.ckidtech.quotation.service.core.dao.ProductRepository;
import com.ckidtech.quotation.service.core.dao.VendorRepository;
import com.ckidtech.quotation.service.core.model.AppUser;
import com.ckidtech.quotation.service.core.model.Component;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.OrderItem;
import com.ckidtech.quotation.service.core.model.Product;
import com.ckidtech.quotation.service.core.model.ReturnMessage;
import com.ckidtech.quotation.service.core.model.Vendor;
import com.ckidtech.quotation.service.core.security.UserRole;
import com.ckidtech.quotation.service.vendorhuborder.service.OrderService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringMongoConfiguration.class})
@ComponentScan({"com.ckidtech.quotation.service.vendorhuborder.service"})
@AutoConfigureDataMongo  
public class OrderServiceTest {
	

	@Autowired
	private OrderService orderService;
	
	@Autowired
	private VendorRepository vendorRepository;
	
	@Autowired
	private AppUserRepository appUserRepository;
	
	@Autowired
	private ProductRepository productRepository; 
	
	private String TEST_USER_ID;
	
	@Before
	public  void initTest() {
		
		orderService.deleteAllOrders();
		
		vendorRepository.deleteAll();
		Vendor vendor = new Vendor("TEST_VENDOR", "Test Vendor", "Address", "9999999", "imgLink");
		vendor.setActiveIndicator(true);
		vendorRepository.save(vendor);
		
		appUserRepository.deleteAll();
		AppUser user = new AppUser("TEST_USER","Test User", "password", UserRole.VENDOR_USER, "TEST_VENDOR", vendor.getId());
		user.setActiveIndicator(true);
		appUserRepository.save(user);
		TEST_USER_ID = user.getId();
	
		productRepository.deleteAll();
		Product product = new Product("TEST_PRODUCT", "TEST_VENDOR", "Food", "Ice Tea", "imglink");
		product.setActiveIndicator(true);
		product.setProdComp(new Component("Ice Tea", 100, 1));
		productRepository.save(product);
	}
	
	@Test
	public void createNewOrderTest() throws Exception {
		
		AppUser loginUser = appUserRepository.findById(TEST_USER_ID).orElse(null);
				
		// Successful creation without parameter		
		QuotationResponse response = orderService.createNewOrder(loginUser, new Order());
		assertTrue("Order created.", response.getMessages().contains(new ReturnMessage("Order created.", ReturnMessage.MessageTypeEnum.INFO)));
		
		assertEquals(true, response.isProcessSuccessful());
		assertNotEquals(null, response.getOrder().getId());
		assertNotEquals(null, response.getOrder().getReferenceOrder());
		assertNotEquals(null, response.getOrder().getOrderDate());		
		assertEquals(Order.Status.New, response.getOrder().getStatus());
		assertEquals(true, response.getOrder().isActiveIndicator());
		assertEquals(loginUser.getObjectRef(), response.getOrder().getVendorId());
		
		// Successful creation with parameter
		LocalDateTime thisTime = LocalDateTime.now();
		response = orderService.createNewOrder(loginUser, new Order(thisTime, "Table1"));
		assertTrue("Order created.", response.getMessages().contains(new ReturnMessage("Order created.", ReturnMessage.MessageTypeEnum.INFO)));
		
		assertEquals(true, response.isProcessSuccessful());
		assertNotEquals(null, response.getOrder().getId());
		assertEquals("Table1", response.getOrder().getReferenceOrder());
		assertEquals(thisTime, response.getOrder().getOrderDate());
		assertEquals(Order.Status.New, response.getOrder().getStatus());
		assertEquals(true, response.getOrder().isActiveIndicator());
		assertEquals(loginUser.getObjectRef(), response.getOrder().getVendorId());
		
	}
	
	@Test
	public void updateOrderTest() throws Exception {
		
		createNewOrderTest();
		
		AppUser loginUser = appUserRepository.findById(TEST_USER_ID).orElse(null);
		LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		
		LocalDateTime endTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusDays(1);
		
		QuotationResponse response;
		List<Order> orders = orderService.getUserOrderForTheDay(loginUser, startTime, endTime);
		for (Order order : orders ) {
			
			// Set to Ordered
			order.setStatus(Order.Status.Ordering);
			response = orderService.updateOrder(loginUser, order);
			assertTrue("Order updated.", response.getMessages().contains(new ReturnMessage("Order updated.", ReturnMessage.MessageTypeEnum.INFO)));
			assertEquals(Order.Status.Ordering, response.getOrder().getStatus());
			
			// Set to Cancelled
			order.setStatus(Order.Status.Cancelled);
			response = orderService.updateOrder(loginUser, order);
			assertTrue("Order updated.", response.getMessages().contains(new ReturnMessage("Order updated.", ReturnMessage.MessageTypeEnum.INFO)));
			assertEquals(Order.Status.Cancelled, response.getOrder().getStatus());
			
			// Set to Paid
			order.setStatus(Order.Status.Paid);
			response = orderService.updateOrder(loginUser, order);
			assertTrue("Order updated.", response.getMessages().contains(new ReturnMessage("Order updated.", ReturnMessage.MessageTypeEnum.INFO)));
			assertEquals(Order.Status.Paid, response.getOrder().getStatus());
			
			// Set to Refund
			order.setStatus(Order.Status.Refund);
			response = orderService.updateOrder(loginUser, order);
			assertTrue("Order updated.", response.getMessages().contains(new ReturnMessage("Order updated.", ReturnMessage.MessageTypeEnum.INFO)));		
			assertEquals(Order.Status.Refund, response.getOrder().getStatus());
			
		}
		
	}
	

	@Test
	public void addOrderItemTest() throws Exception {
		
		createNewOrderTest();
		
		AppUser loginUser = appUserRepository.findById(TEST_USER_ID).orElse(null);
		LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		
		LocalDateTime endTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusDays(1);
		
		QuotationResponse response;
		List<Order> orders = orderService.getUserOrderForTheDay(loginUser, startTime, endTime);
		for (Order order : orders ) {
			
			//Product not found.
			response = orderService.addOrderItem(loginUser, order.getId(), new OrderItem("TEST_PRODUCTXX", 1));
			assertTrue("Product not found.", response.getMessages().contains(new ReturnMessage("Product not found.", ReturnMessage.MessageTypeEnum.ERROR)));
			
			// Successful adding product in the order
			response = orderService.addOrderItem(loginUser, order.getId(), new OrderItem("TEST_PRODUCT", 1));
			assertTrue("Product (Ice Tea) added in the order.", response.getMessages().contains(new ReturnMessage("Product (Ice Tea) added in the order.", ReturnMessage.MessageTypeEnum.INFO)));
			assertEquals(100, response.getOrder().getTotalAmountDue(), 0.01);
			
			// Update the quantity
			response = orderService.addOrderItem(loginUser, order.getId(), new OrderItem("TEST_PRODUCT", 2));
			assertTrue("Product (Ice Tea) was updated from the order.", response.getMessages().contains(new ReturnMessage("Product (Ice Tea) was updated from the order.", ReturnMessage.MessageTypeEnum.INFO)));
			assertEquals(200, response.getOrder().getTotalAmountDue(), 0.01);
			
			// Remove the product from order
			response = orderService.removeFromOrderList(loginUser, order.getId(), "TEST_PRODUCT");
			assertTrue("Product (Ice Tea) was removed from the order.", response.getMessages().contains(new ReturnMessage("Product (Ice Tea) was removed from the order.", ReturnMessage.MessageTypeEnum.INFO)));
			
		}
		
	}
	
	@Test
	public void getOrderByIdTest() throws Exception {
		
		AppUser loginUser = appUserRepository.findById(TEST_USER_ID).orElse(null);
		
		// Successful creation without parameter		
		QuotationResponse response = orderService.createNewOrder(loginUser, new Order());
		assertTrue("Order created.", response.getMessages().contains(new ReturnMessage("Order created.", ReturnMessage.MessageTypeEnum.INFO)));
		
		String orderID = response.getOrder().getId();
		
		
		// Get Order object including history
		response = orderService.getOrderById(loginUser, orderID);
		
		assertEquals(true, response.isProcessSuccessful());
		assertEquals(orderID, response.getOrder().getId());
		assertNotEquals(null, response.getOrder().getHistories());
				
		// Get Order object excluding history
		response = orderService.getOrderByIdWithOutHistory(loginUser, orderID);
		
		assertEquals(true, response.isProcessSuccessful());
		assertEquals(orderID, response.getOrder().getId());
		assertEquals(null, response.getOrder().getHistories());
		
	}
	
}
