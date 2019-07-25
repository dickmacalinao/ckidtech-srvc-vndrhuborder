package com.ckidtech.quotation.service.order.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ckidtech.quotation.service.core.controller.QuotationResponse;
import com.ckidtech.quotation.service.core.model.ChartRequest;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.OrderItem;
import com.ckidtech.quotation.service.core.model.OrderSearchCriteria;
import com.ckidtech.quotation.service.order.service.OrderService;

@ComponentScan({"com.ckidtech.quotation.service.core.service"})
@RestController
public class QuotationControllerOrder {
	
	private static final Logger LOG = Logger.getLogger(QuotationControllerOrder.class.getName());
	
	@Autowired
	private OrderService OrderService;
		
	@RequestMapping(value = "/vendor/getorder")
	public ResponseEntity<Object> getVendorPurchaseOrder(
			@RequestBody OrderSearchCriteria orderSearchCriteria) {		
		LOG.log(Level.INFO, "Calling API /vendor/getorder/" + orderSearchCriteria.toString());
				
		return new ResponseEntity<Object>(OrderService.getVendorOrder(orderSearchCriteria), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendor/getorderchart")
	public ResponseEntity<Object> geOrderChart(
			@RequestBody ChartRequest chartReq) {		
		LOG.log(Level.INFO, "Calling API /vendor/getorderchart/");		
		return new ResponseEntity<Object>(OrderService.getOrderChart(chartReq), HttpStatus.OK);		
	}
	
	// For testing purposes
	@RequestMapping(value = "/vendor/createmultipleorders", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrders(@RequestBody Order[] orders) {
		LOG.log(Level.INFO, "Calling API /vendor/createmultipleorders");
		ArrayList<QuotationResponse> quotations = new ArrayList<QuotationResponse>();  
		for( Order order : orders ) {
			quotations.add(OrderService.createNewOrder(order));
			
		}
		return new ResponseEntity<Object>(quotations, HttpStatus.CREATED);		
	}

	@RequestMapping(value = "/user/getorderfortheday/{vendorId}/{userId}/{currDate}")
	public ResponseEntity<Object> getUserPurchaseOrderForTheDay(
			@PathVariable("vendorId") String vendorId, 
			@PathVariable("userId") String userId,
			@PathVariable("currDate") String currDate) {		
		LOG.log(Level.INFO, "Calling API /user/getorderfortheday/" + vendorId + "/" + userId + "/" + currDate);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dFrom = LocalDateTime.parse(currDate + " 00:00:00", formatter);
		LocalDateTime dTo = LocalDateTime.parse(currDate + " 23:59:59", formatter);
		
		return new ResponseEntity<Object>(OrderService.getUserOrderForTheDay(vendorId, userId, dFrom, dTo), HttpStatus.OK);		
	}

	@RequestMapping(value = "/user/createneworder", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrder(@RequestBody Order order) {
		LOG.log(Level.INFO, "Calling API /user/createneworder");
		return new ResponseEntity<Object>(OrderService.createNewOrder(order), HttpStatus.CREATED);		
	}	


	@RequestMapping(value = "/user/updateorder", method = RequestMethod.POST)
	public ResponseEntity<Object> updateOrder(@RequestBody Order order) {		
		LOG.log(Level.INFO, "Calling API /user/updateorder");
		return new ResponseEntity<Object>(OrderService.updateOrder(order), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/addtoorderitem/{orderID}", method = RequestMethod.POST)
	public ResponseEntity<Object> addToOrderList(
			@PathVariable("orderID") String orderID, 
			@RequestBody OrderItem orderItem) {		
		LOG.log(Level.INFO, "Calling API /user/addtoorderitem/" + orderID);
		return new ResponseEntity<Object>(OrderService.addOrderItem(orderID, orderItem), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/removefromorderlist/{poID}/{productId}")
	public ResponseEntity<Object> removeFromOrderList(
			@PathVariable("poID") String poID, 
			@PathVariable("productId") String productId) {		
		LOG.log(Level.INFO, "Calling API /user/removefromorderlist/" + poID + "/" + productId);
		return new ResponseEntity<Object>(OrderService.removeFromOrderList(poID, productId), HttpStatus.OK);		
	}
			
}
