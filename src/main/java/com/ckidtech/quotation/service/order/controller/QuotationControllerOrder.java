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
import com.ckidtech.quotation.service.core.model.PurchaseOrder;
import com.ckidtech.quotation.service.order.service.OrderService;

@ComponentScan({"com.ckidtech.quotation.service.core.service"})
@RestController
public class QuotationControllerOrder {
	
	private static final Logger LOG = Logger.getLogger(QuotationControllerOrder.class.getName());
	
	@Autowired
	private OrderService OrderService;
		
	@RequestMapping(value = "/vendor/getorder/{vendorId}/{dateFrom}/{dateTo}")
	public ResponseEntity<Object> getVendorPurchaseOrder(
			@PathVariable("vendorId") String vendorId,
			@PathVariable("dateFrom") String dateFrom, 
			@PathVariable("dateTo") String dateTo) {		
		LOG.log(Level.INFO, "Calling API /vendor/getorder/" + vendorId + "/" + dateFrom + "/" + dateTo);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime dFrom = LocalDateTime.parse(dateFrom + " 00:00:00", formatter);
		LocalDateTime dTo = LocalDateTime.parse(dateTo + " 23:59:59", formatter);
		
		return new ResponseEntity<Object>(OrderService.getVendorOrder(vendorId, dFrom, dTo), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/vendor/getpochart")
	public ResponseEntity<Object> getVendorPurchaseOrder(
			@RequestBody ChartRequest chartReq) {		
		LOG.log(Level.INFO, "Calling API /vendor/getpochart/");		
		return new ResponseEntity<Object>(OrderService.getPOChart(chartReq), HttpStatus.OK);		
	}
	
	// For testing purposes
	@RequestMapping(value = "/vendor/createmultiplewpurchaseorders", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewPurchaseOrder(@RequestBody PurchaseOrder[] pos) {
		LOG.log(Level.INFO, "Calling API /vendor/createmultiplewpurchaseorders");
		ArrayList<QuotationResponse> quotations = new ArrayList<QuotationResponse>();  
		for( PurchaseOrder po : pos ) {
			quotations.add(OrderService.createNewOrder(po));
			
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

	@RequestMapping(value = "/user/createneorder", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewPurchaseOrder(@RequestBody PurchaseOrder po) {
		LOG.log(Level.INFO, "Calling API /user/createneorder");
		return new ResponseEntity<Object>(OrderService.createNewOrder(po), HttpStatus.CREATED);		
	}	


	@RequestMapping(value = "/user/updateorder", method = RequestMethod.POST)
	public ResponseEntity<Object> updatePurchaseOrder(@RequestBody PurchaseOrder po) {		
		LOG.log(Level.INFO, "Calling API /user/updateorder");
		return new ResponseEntity<Object>(OrderService.updateOrder(po), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/addtoorderlist/{poId}", method = RequestMethod.POST)
	public ResponseEntity<Object> addToOrderList(
			@PathVariable("poId") String poId, 
			@RequestBody Order order) {		
		LOG.log(Level.INFO, "Calling API /user/addtoorderlist/" + poId);
		return new ResponseEntity<Object>(OrderService.addToOrderList(poId, order), HttpStatus.OK);		
	}
	
	@RequestMapping(value = "/user/removefromorderlist/{poID}/{productId}")
	public ResponseEntity<Object> removeFromOrderList(
			@PathVariable("poID") String poID, 
			@PathVariable("productId") String productId) {		
		LOG.log(Level.INFO, "Calling API /user/removefromorderlist/" + poID + "/" + productId);
		return new ResponseEntity<Object>(OrderService.removeFromOrderList(poID, productId), HttpStatus.OK);		
	}
			
}
