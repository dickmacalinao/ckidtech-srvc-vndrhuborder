package com.ckidtech.quotation.service.vendorhuborder.controller;

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
import com.ckidtech.quotation.service.core.model.AppUser;
import com.ckidtech.quotation.service.core.model.ChartRequest;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.OrderSearchCriteria;
import com.ckidtech.quotation.service.core.security.UserRole;
import com.ckidtech.quotation.service.core.utils.Util;
import com.ckidtech.quotation.service.vendorhuborder.service.OrderService;

@ComponentScan({"com.ckidtech.quotation.service.core.service"})
@RestController
public class QuotationControllerOrder {
	
	private static final Logger LOG = Logger.getLogger(QuotationControllerOrder.class.getName());
	
	@Autowired
	private OrderService OrderService;
		
	
	// By Vendors
	
	@RequestMapping(value = "/vendoradmin/getvendororder")
	public ResponseEntity<Object> getVendorOrder(
			@RequestHeader("authorization") String authorization, 
			@RequestBody OrderSearchCriteria orderSearchCriteria) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoradmin/getvendororder/" + orderSearchCriteria.toString());
		
		AppUser loginUser = new AppUser(authorization);
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_ADMIN, null);
		return new ResponseEntity<Object>(OrderService.getVendorOrder(loginUser, orderSearchCriteria), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoradmin/getorderfortheday/{currDate}")
	public ResponseEntity<Object> getvendorPurchaseOrderForTheDay(
			@RequestHeader("authorization") String authorization,
			@PathVariable("currDate") String currDate) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoradmin/getorderfortheday/" + currDate);
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_ADMIN, null);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dFrom = LocalDateTime.parse(currDate + " 00:00:00", formatter);
		LocalDateTime dTo = LocalDateTime.parse(currDate + " 23:59:59", formatter);
		
		return new ResponseEntity<Object>(OrderService.getUserOrderForTheDay(loginUser, dFrom, dTo), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoradmin/getorderchart")
	public ResponseEntity<Object> geOrderChart(
			@RequestHeader("authorization") String authorization,
			@RequestBody ChartRequest chartReq) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoradmin/getorderchart/");		
		
		AppUser loginUser = new AppUser(authorization);
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_ADMIN, null);
		return new ResponseEntity<Object>(OrderService.getOrderChart(loginUser, chartReq), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoradmin/updateorder", method = RequestMethod.POST)
	public ResponseEntity<Object> vendorUpdateOrder(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order order) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoradmin/updateorder");
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_ADMIN, order.getVendorId());
		return new ResponseEntity<Object>(OrderService.updateOrder(loginUser, order), HttpStatus.OK);		
	}
	
	// For testing purposes
	@RequestMapping(value = "/vendoradmin/createmultipleorders", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrders(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order[] orders) throws Exception {
		LOG.log(Level.INFO, "Calling API /vendoradmin/createmultipleorders");
		
		AppUser loginUser = new AppUser(authorization);		
		ArrayList<QuotationResponse> quotations = new ArrayList<QuotationResponse>();  
		for( Order order : orders ) {			
			Util.checkAccessGrant(loginUser, UserRole.VENDOR_ADMIN, order.getVendorId());
			quotations.add(OrderService.createNewOrder(loginUser, order));
			
		}
		return new ResponseEntity<Object>(quotations, HttpStatus.CREATED);		
	}
	
	
	// By Users

	@RequestMapping(value = "/vendoruser/getorderfortheday/{currDate}")
	public ResponseEntity<Object> getUserPurchaseOrderForTheDay(
			@RequestHeader("authorization") String authorization,
			@PathVariable("currDate") String currDate) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/getorderfortheday/" + currDate);
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, null);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dFrom = LocalDateTime.parse(currDate + " 00:00:00", formatter);
		LocalDateTime dTo = LocalDateTime.parse(currDate + " 23:59:59", formatter);
		
		return new ResponseEntity<Object>(OrderService.getUserOrderForTheDay(loginUser, dFrom, dTo), HttpStatus.OK);		
	}

	@RequestMapping(value = "/vendoruser/createneworder", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewOrder(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order order) throws Exception {
		LOG.log(Level.INFO, "Calling API /vendoruser/createneworder");
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, order.getVendorId());
		return new ResponseEntity<Object>(OrderService.createNewOrder(loginUser, order), HttpStatus.CREATED);		
	}	


	@RequestMapping(value = "/vendoruser/updateorder", method = RequestMethod.POST)
	public ResponseEntity<Object> updateOrder(
			@RequestHeader("authorization") String authorization,
			@RequestBody Order order) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/updateorder");
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, order.getVendorId());
		return new ResponseEntity<Object>(OrderService.updateOrder(loginUser, order), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoruser/addtoorderitem/{orderID}/{productID}/{quantity}")
	public ResponseEntity<Object> addToOrderList(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID, 
			@PathVariable("productID") String productID,
			@PathVariable("quantity") int quantity) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/addtoorderitem/" + orderID + "/" + productID + "/" + quantity);
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, null);
		return new ResponseEntity<Object>(OrderService.addOrderItem(loginUser, orderID, productID, quantity), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoruser/removefromorderlist/{orderID}/{productId}")
	public ResponseEntity<Object> removeFromOrderList(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID, 
			@PathVariable("productId") String productId) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/removefromorderlist/" + orderID + "/" + productId);
		
		AppUser loginUser = new AppUser(authorization);
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, null);
		return new ResponseEntity<Object>(OrderService.removeFromOrderList(loginUser, orderID, productId), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoruser/getorderbyid/{orderID}")
	public ResponseEntity<Object> getOrderById(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/getorderbyid/" + orderID);
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, null);
		return new ResponseEntity<Object>(OrderService.getOrderById(loginUser, orderID), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendoruser/getorderbyidwithouthistory/{orderID}")
	public ResponseEntity<Object> getOrderByIdWOHistory(
			@RequestHeader("authorization") String authorization,
			@PathVariable("orderID") String orderID) throws Exception {		
		LOG.log(Level.INFO, "Calling API /vendoruser/getorderbyid/" + orderID);
		
		AppUser loginUser = new AppUser(authorization);	
		Util.checkAccessGrant(loginUser, UserRole.VENDOR_USER, null);
		return new ResponseEntity<Object>(OrderService.getOrderByIdWithOutHistory(loginUser, orderID), HttpStatus.OK);		
	}
			
}
