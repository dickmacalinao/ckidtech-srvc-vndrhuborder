package com.ckidtech.quotation.service.purchaseorder.service;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import com.ckidtech.quotation.service.core.controller.MessageController;
import com.ckidtech.quotation.service.core.controller.QuotationResponse;
import com.ckidtech.quotation.service.core.dao.AppUserRepository;
import com.ckidtech.quotation.service.core.dao.PurchaseOrderRepository;
import com.ckidtech.quotation.service.core.dao.VendorRepository;
import com.ckidtech.quotation.service.core.model.AppUser;
import com.ckidtech.quotation.service.core.model.PurchaseOrder;
import com.ckidtech.quotation.service.core.model.Vendor;
import com.ckidtech.quotation.service.core.utils.Util;

@ComponentScan({"com.ckidtech.quotation.service.core.controller"})
@EnableMongoRepositories ("com.ckidtech.quotation.service.core.dao")
@Service
public class PurchaseOrderService {
	
	@Autowired
	private PurchaseOrderRepository purchaseOrderRepository;
	
	@Autowired
	private AppUserRepository appUserRepository;
	
	@Autowired
	private VendorRepository vendorRepository;
	
	@Autowired
	private MessageController msgController;

	private static final Logger LOG = Logger.getLogger(PurchaseOrderService.class.getName());

	public QuotationResponse createNewPurchaseOrder(PurchaseOrder po) {
		
		LOG.log(Level.INFO, "Calling Purchase Order Service createNewPurchaseOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (po.getReferenceOrder() == null || "".equals(po.getReferenceOrder()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Reference Order"));
		if (po.getVendorId()== null || "".equals(po.getVendorId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Vendor ID"));
		if (po.getUserId()== null || "".equals(po.getUserId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "User ID"));
		
		if (quotation.getMessages().isEmpty()) {
			
			Vendor vendorRep = vendorRepository.findById(po.getVendorId().toUpperCase()).orElse(null);			
			// Verify if Vendor exists and active
			if (vendorRep==null || !vendorRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VNFE"));
			}
				
			AppUser appUserRep = appUserRepository.findById(po.getUserId()).orElse(null);			
			// Verify if App User exists and active
			if  ( appUserRep==null || !appUserRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.AUNFE"));
			} 
								
			if (quotation.getMessages().isEmpty()) { 
					
				po.setId(po.getVendorId() + "-" + po.getUserId() + "-" + LocalDateTime.now());
				po.setStatus(PurchaseOrder.Status.NEW);		
				po.setOrderDate(LocalDateTime.now());
				Util.initalizeCreatedInfo(po, msgController.getMsg("info.PORC"));	
				purchaseOrderRepository.save(po);
				
				quotation.setPurchaseOrder(po);
				
			}
		}		
		
		return quotation;
		
	}
	
}
