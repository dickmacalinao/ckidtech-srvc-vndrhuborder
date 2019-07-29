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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ckidtech.quotation.service.core.controller.QuotationResponse;
import com.ckidtech.quotation.service.core.model.ChartRequest;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.OrderItem;
import com.ckidtech.quotation.service.core.model.OrderSearchCriteria;
import com.ckidtech.quotation.service.core.security.UserRole;
import com.ckidtech.quotation.service.core.utils.Util;
import com.ckidtech.quotation.service.order.service.OrderService;

@ComponentScan({"com.ckidtech.quotation.service.core.service"})
@RestController
public class QuotationControllerOrder {
	
	private static final Logger LOG = Logger.getLogger(QuotationControllerOrder.class.getName());
	
	@Autowired
	private OrderService OrderService;
		
	
	// By Vendors
	
	@RequestMapping(value = "/vendor/getvendororder")
	public ResponseEntity<Object> getVendorOrder(
			@RequestHeader("authorization") String authorization, 
			@RequestBody OrderSearchCriteria orderSearchCriteria) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendor/getvendororder/" + orderSearchCriteria.toString());
		
		Util.checkAccessGrant(authorization, UserRole.VENDOR, orderSearchCriteria.getVendorId());
		return new ResponseEntity<Object>(OrderService.getVendorOrder(orderSearchCriteria), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendor/getorderchart")
	public ResponseEntity<Object> geOrderChart(
			@RequestHeader("authorization") String authorization,
			@RequestBody ChartRequest chartReq) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendor/getorderchart/");		
		Util.checkAccessGrant(authorization, UserRole.VENDOR, chartReq.getVendorId());
		return new ResponseEntity<Object>(OrderService.getOrderChart(chartReq), HttpStatus.OK);		
	}
	
	// For testing purposes
	@RequestMapping(value = "/vendor/createmultipleorders", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrders(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order[] orders) throws Exception {
		LOG.log(Level.INFO, "Calling API /vendor/createmultipleorders");
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		ArrayList<QuotationResponse> quotations = new ArrayList<QuotationResponse>();  
		for( Order order : orders ) {
			Util.checkAccessGrant(authorization, UserRole.VENDOR, order.getVendorId());
			quotations.add(OrderService.createNewOrder(userId, order));
			
		}
		return new ResponseEntity<Object>(quotations, HttpStatus.CREATED);		
	}
	
	
	// By Users

	@RequestMapping(value = "/user/getorderfortheday/{currDate}")
	public ResponseEntity<Object> getUserPurchaseOrderForTheDay(
			@RequestHeader("authorization") String authorization,
			@PathVariable("currDate") String currDate) throws Exception {		
		LOG.log(Level.INFO, "Calling API /user/getorderfortheday/" + currDate);
		
		Util.checkAccessGrant(authorization, UserRole.USER, null);
		String vendorId = (String) Util.getClaimsValueFromToken(authorization, "vendor");
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dFrom = LocalDateTime.parse(currDate + " 00:00:00", formatter);
		LocalDateTime dTo = LocalDateTime.parse(currDate + " 23:59:59", formatter);
		
		return new ResponseEntity<Object>(OrderService.getUserOrderForTheDay(vendorId, userId, dFrom, dTo), HttpStatus.OK);		
	}

	@RequestMapping(value = "/user/createneworder", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrder(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order order) throws Exception {
		LOG.log(Level.INFO, "Calling API /user/createneworder");
		Util.checkAccessGrant(authorization, UserRole.USER, order.getVendorId());
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		return new ResponseEntity<Object>(OrderService.createNewOrder(userId, order), HttpStatus.CREATED);		
	}	


	@RequestMapping(value = "/user/updateorder", method = RequestMethod.POST)
	public ResponseEntity<Object> updateOrder(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order order) throws Exception {		
		LOG.log(Level.INFO, "Calling API /user/updateorder");
		Util.checkAccessGrant(authorization, UserRole.USER, order.getVendorId());
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		return new ResponseEntity<Object>(OrderService.updateOrder(userId, order), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/addtoorderitem/{orderID}", method = RequestMethod.POST)
	public ResponseEntity<Object> addToOrderList(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID, 
			@RequestBody OrderItem orderItem) throws Exception {		
		LOG.log(Level.INFO, "Calling API /user/addtoorderitem/" + orderID);
		Util.checkAccessGrant(authorization, UserRole.USER, null);
		String vendorId = (String) Util.getClaimsValueFromToken(authorization, "vendor");
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		return new ResponseEntity<Object>(OrderService.addOrderItem(vendorId, userId, orderID, orderItem), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/removefromorderlist/{orderID}/{productId}")
	public ResponseEntity<Object> removeFromOrderList(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID, 
			@PathVariable("productId") String productId) throws Exception {		
		LOG.log(Level.INFO, "Calling API /user/removefromorderlist/" + orderID + "/" + productId);
		Util.checkAccessGrant(authorization, UserRole.USER, null);
		String vendorId = (String) Util.getClaimsValueFromToken(authorization, "vendor");
		String userId = (String) Util.getClaimsValueFromToken(authorization, "sub");
		return new ResponseEntity<Object>(OrderService.removeFromOrderList(vendorId, userId, orderID, productId), HttpStatus.OK);		
	}
			
}
