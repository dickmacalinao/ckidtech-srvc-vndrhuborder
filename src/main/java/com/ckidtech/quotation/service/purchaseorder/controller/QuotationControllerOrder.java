package com.ckidtech.quotation.service.purchaseorder.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ckidtech.quotation.service.core.model.PurchaseOrder;
import com.ckidtech.quotation.service.purchaseorder.service.PurchaseOrderService;

@ComponentScan({"com.ckidtech.quotation.service.core.service"})
@RestController
public class QuotationControllerOrder {
	
	private static final Logger LOG = Logger.getLogger(QuotationControllerOrder.class.getName());
	
	@Autowired
	private PurchaseOrderService purchaseOrderService;
		

	@RequestMapping(value = "/purchaseorder/user/getpurchaseorderfortheday")
	public ResponseEntity<Object> getPurchaseOrderForTheDay() {		
		LOG.log(Level.INFO, "Calling API /purchaseorder/user/getpurchaseorderfortheday");
		return new ResponseEntity<Object>(null, HttpStatus.OK);		
	}

	@RequestMapping(value = "/purchaseorder/user/createnewpurchaseorder", method = RequestMethod.POST)
	public ResponseEntity<Object> createNewPurchaseOrder(@RequestBody PurchaseOrder po) {		
		LOG.log(Level.INFO, "Calling API /purchaseorder/user/createnewpurchaseorder");
		return new ResponseEntity<Object>(purchaseOrderService.createNewPurchaseOrder(po), HttpStatus.OK);		
	}
	
			
}
