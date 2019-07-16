package com.ckidtech.quotation.service.purchaseorder.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import com.ckidtech.quotation.service.core.controller.MessageController;
import com.ckidtech.quotation.service.core.controller.QuotationResponse;
import com.ckidtech.quotation.service.core.dao.AppUserRepository;
import com.ckidtech.quotation.service.core.dao.ProductRepository;
import com.ckidtech.quotation.service.core.dao.PurchaseOrderRepository;
import com.ckidtech.quotation.service.core.dao.VendorRepository;
import com.ckidtech.quotation.service.core.model.AppUser;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.Product;
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
	private ProductRepository productRepository;
	
	@Autowired
	private MessageController msgController;

	private static final Logger LOG = Logger.getLogger(PurchaseOrderService.class.getName());
	
	public List<PurchaseOrder> getVendorPurchaseOrder(String vendorId, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Purchase Order Service getUserPurchaseOrderForTheDay()");
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return purchaseOrderRepository.findByVendor(vendorId, dateFrom, dateTo, pageable);	
	}
	
	public List<PurchaseOrder> getUserPurchaseOrderForTheDay(String vendorId, String userId, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Purchase Order Service getUserPurchaseOrderForTheDay()");
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return purchaseOrderRepository.findByVendorAndUser(vendorId, userId, dateFrom, dateTo, pageable);	
	}
	
	public QuotationResponse createNewPurchaseOrder(PurchaseOrder po) {
		
		LOG.log(Level.INFO, "Calling Purchase Order Service updatePurchaseOrder()");

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

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");					
				po.setId(po.getVendorId() + "-" + po.getUserId() + "-" + LocalDateTime.now().format(formatter));
				
				po.setStatus(PurchaseOrder.Status.New);		
				po.setOrderDate(LocalDateTime.now());
				po.setActiveIndicator(true);
				Util.initalizeCreatedInfo(po, msgController.getMsg("info.PORC"));	
				purchaseOrderRepository.save(po);
				
				quotation.setPurchaseOrder(po);
			}
		}		
		
		return quotation;
		
	}

	public QuotationResponse updatePurchaseOrder(PurchaseOrder po) {
		
		LOG.log(Level.INFO, "Calling Purchase Order Service updatePurchaseOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (po.getId() == null || "".equals(po.getId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));
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
			
			PurchaseOrder poRep = purchaseOrderRepository.findById(po.getId()).orElse(null);
			// Verify if Purchase order exists and active
			if  ( poRep==null || !poRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			}
			
			if (quotation.getMessages().isEmpty()) { 
					
				Util.initalizeUpdatedInfo(poRep, poRep.getDifferences(po));		
				poRep.setStatus(po.getStatus());	
				poRep.setReferenceOrder(po.getReferenceOrder());	
				poRep.setActiveIndicator(po.isActiveIndicator());
				purchaseOrderRepository.save(poRep);
				
				quotation.setPurchaseOrder(poRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse addToOrderList(String poID, Order order) {
		
		LOG.log(Level.INFO, "Calling Purchase Order Service addOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (poID == null || "".equals(poID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));
		if (order == null) {
			quotation.addMessage(msgController.createMsg("error.MFE", "Order"));
		} else {
			if ( order.getProductId()==null || "".equals(order.getProductId()) ) 
				quotation.addMessage(msgController.createMsg("error.MFE", "Product ID"));			
		}
		
		if (quotation.getMessages().isEmpty()) {
			
			PurchaseOrder poRep = purchaseOrderRepository.findById(poID).orElse(null);
			// Verify if Purchase order exists and active
			if  ( poRep==null || !poRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			} else {
				
				// Only status New or Ordering can add oder to list				
				if ( !poRep.getStatus().equals(PurchaseOrder.Status.New) && !poRep.getStatus().equals(PurchaseOrder.Status.Ordering) ) {
					quotation.addMessage(msgController.createMsg("error.POUNAE"));
				}
			}
			
			Product prod = productRepository.findById(order.getProductId()).orElse(null);
			// Verify if Product exists and active
			if  ( prod==null || !prod.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VPNFE"));
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				Order orderRep = null;
				
				// Retrieve the order from previous and if exists
				if ( poRep.getOrders() == null ) {
					poRep.setOrders(new ArrayList<Order>());
				} else {
					
					for ( Order orderInd : poRep.getOrders() ) {
						if ( orderInd.getProductId().equals(order.getProductId()) ) {
							orderRep = orderInd;
						}
					}
					
				}
								
				if ( orderRep==null ) {
					order.setAmountDue(prod.getProdComp().getComputedAmount() * order.getQuantity());	// Compute the Amount Due
					Util.initalizeUpdatedInfo(poRep, String.format(msgController.getMsg("info.POAO"), order.toString()));
					poRep.getOrders().add(order);
				} else {
					orderRep.setQuantity(order.getQuantity());
					orderRep.setAmountDue(prod.getProdComp().getComputedAmount() * order.getQuantity()); // Compute the Amount Due
					Util.initalizeUpdatedInfo(poRep, String.format(msgController.getMsg("info.POUO"), orderRep.toString()));
				}
					
				purchaseOrderRepository.save(poRep);				
				quotation.setPurchaseOrder(poRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse removeFromOrderList(String poID, String productId) {
		
		LOG.log(Level.INFO, "Calling Purchase Order Service addOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (poID == null || "".equals(poID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));	
		if ( productId==null || "".equals(productId) ) 
			quotation.addMessage(msgController.createMsg("error.MFE", "Product ID"));			
		
		if (quotation.getMessages().isEmpty()) {
			
			PurchaseOrder poRep = purchaseOrderRepository.findById(poID).orElse(null);
			// Verify if Purchase order exists and active
			if  ( poRep==null || !poRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			} else {
				
				// Only status New or Ordering can remove oder from list				
				if ( !poRep.getStatus().equals(PurchaseOrder.Status.New) && !poRep.getStatus().equals(PurchaseOrder.Status.Ordering) ) {
					quotation.addMessage(msgController.createMsg("error.POUNAE"));
				}
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				Order orderRep = null;
				
				// Retrieve the order from previous and if exists
				if ( poRep.getOrders() == null ) {
					poRep.setOrders(new ArrayList<Order>());
				} else {
					
					for ( Order orderInd : poRep.getOrders() ) {
						if ( orderInd.getProductId().equals(productId) ) {
							orderRep = orderInd;
						}
					}
					
				}
								
				if ( orderRep==null ) {
					quotation.addMessage(msgController.createMsg("error.POINFE"));
				} else {
					Util.initalizeUpdatedInfo(poRep, String.format(msgController.getMsg("info.PORO"), orderRep.toString()));					
					poRep.getOrders().remove(orderRep);
					purchaseOrderRepository.save(poRep);
				}	
								
				quotation.setPurchaseOrder(poRep);
				
			}
		}		
		
		return quotation;
		
	}
	
}
