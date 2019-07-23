package com.ckidtech.quotation.service.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.ckidtech.quotation.service.core.model.ChartRequest;
import com.ckidtech.quotation.service.core.model.ChartResponse;
import com.ckidtech.quotation.service.core.model.DatasetItem;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.Product;
import com.ckidtech.quotation.service.core.model.PurchaseOrder;
import com.ckidtech.quotation.service.core.model.Vendor;
import com.ckidtech.quotation.service.core.utils.Util;

@ComponentScan({"com.ckidtech.quotation.service.core.controller"})
@EnableMongoRepositories ("com.ckidtech.quotation.service.core.dao")
@Service
public class OrderService {
	
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

	private static final Logger LOG = Logger.getLogger(OrderService.class.getName());
	
	public List<PurchaseOrder> getVendorOrder(String vendorId, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Order Service getVendorOrder()");
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return purchaseOrderRepository.findByVendorAndRange(vendorId, dateFrom, dateTo, pageable);	
	}
	
	public List<PurchaseOrder> getUserOrderForTheDay(String vendorId, String userId, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Order Service getUserOrderForTheDay()");
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return purchaseOrderRepository.findByVendorAndUser(vendorId, userId, dateFrom, dateTo, pageable);	
	}
	
	public QuotationResponse createNewOrder(PurchaseOrder po) {
		
		LOG.log(Level.INFO, "Calling Order Service createNewOrder()");

		QuotationResponse quotation = new QuotationResponse();		
		
		if (po.getReferenceOrder() == null || "".equals(po.getReferenceOrder()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Reference Order"));
		if (po.getVendorId()== null || "".equals(po.getVendorId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Vendor ID"));
		if (po.getUserId()== null || "".equals(po.getUserId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "User ID"));
		if (po.getOrderDate()== null || "".equals(po.getOrderDate()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Date"));
		
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
				po.setId(po.getVendorId() + "-" + LocalDateTime.now().format(formatter));
				
				po.setStatus(PurchaseOrder.Status.New);		
				po.setActiveIndicator(true);
				Util.initalizeCreatedInfo(po, msgController.getMsg("info.PORC"));	
				purchaseOrderRepository.save(po);
				
				quotation.setPurchaseOrder(po);
			}
		}		
		
		return quotation;
		
	}

	public QuotationResponse updateOrder(PurchaseOrder po) {
		
		LOG.log(Level.INFO, "Calling Order Service updateOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (po.getId() == null || "".equals(po.getId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));
		if (po.getReferenceOrder() == null || "".equals(po.getReferenceOrder()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Reference Order"));
		if (po.getUserId()== null || "".equals(po.getUserId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "User ID"));
		if (po.getStatus()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Status"));
		if (po.getOrderDate()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Date"));
		
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
				poRep.setReferenceOrder(po.getReferenceOrder());
				poRep.setUserId(po.getUserId());
				poRep.setStatus(po.getStatus());
				poRep.setOrderDate(po.getOrderDate());					
				purchaseOrderRepository.save(poRep);
				
				quotation.setPurchaseOrder(poRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse addToOrderList(String poID, Order order) {
		
		LOG.log(Level.INFO, "Calling Order Service addToOrderList()");

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
					poRep.setOrders(new HashMap<String, Order>());
				} else {					
					orderRep = poRep.getOrders().get(order.getProductId());
				}
								
				if ( orderRep==null ) {
					order.setAmountDue(prod.getProdComp().getComputedAmount() * order.getQuantity());	// Compute the Amount Due
					Util.initalizeUpdatedInfo(poRep, String.format(msgController.getMsg("info.POAO"), order.toString()));
					poRep.getOrders().put(order.getProductId(), order);
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
		
		LOG.log(Level.INFO, "Calling Order Service removeFromOrderList()");

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
					poRep.setOrders(new HashMap<String, Order>());
				} else {
					orderRep = poRep.getOrders().get(productId);	
				}
								
				if ( orderRep==null ) {
					quotation.addMessage(msgController.createMsg("error.POINFE"));
				} else {
					Util.initalizeUpdatedInfo(poRep, String.format(msgController.getMsg("info.PORO"), orderRep.toString()));					
					poRep.getOrders().remove(productId);
					purchaseOrderRepository.save(poRep);
				}	
								
				quotation.setPurchaseOrder(poRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public ChartResponse getPOChart(ChartRequest chartRequest) {
		ChartResponse chart = new ChartResponse();
		
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		List<PurchaseOrder> pos = purchaseOrderRepository.findByVendor(chartRequest.getVendorId(), pageable);
		List<String> labels = getChartLabels(chartRequest, pos);
		
		List<DatasetItem> datasets = new ArrayList<DatasetItem>();
		
		datasets.add(getDataSetItem(chartRequest, pos, labels));
		
		chart.setLabels(labels);
		chart.setDatasets(datasets);
		
		return chart;		
	}
	
	private List<String> getChartLabels(ChartRequest chartRequest, List<PurchaseOrder> pos) {
		ArrayList<String> labels = new ArrayList<String>();
		LocalDateTime startRecOrderDate = null;
		LocalDateTime endRecOrderDate = null;
		LocalDateTime runningDate = null;
		
		DateTimeFormatter formatter = null;
		
		if ( pos.size() > 0 ) {
			startRecOrderDate = pos.get(0).getOrderDate();
			endRecOrderDate = pos.get(pos.size()-1).getOrderDate();
		}
		
		if ( startRecOrderDate!=null && endRecOrderDate!=null ) {
			
			int startRange = 0;
			int endRange = 0;
						
			if ( ChartRequest.LabelBy.Yearly.equals(chartRequest.getLabelBy()) ) {
				formatter = DateTimeFormatter.ofPattern("yyyy");
			} else if ( ChartRequest.LabelBy.Monthly.equals(chartRequest.getLabelBy()) ) {	
				formatter = DateTimeFormatter.ofPattern("yyyyMM");
			} else if ( ChartRequest.LabelBy.Daily.equals(chartRequest.getLabelBy()) ) {
				formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			}
			
			if ( formatter!=null ) {
				startRange = Integer.valueOf(startRecOrderDate.format(formatter));
				endRange = Integer.valueOf(endRecOrderDate.format(formatter));
				runningDate = startRecOrderDate;
				
				for (int cnt = startRange; cnt <= endRange; ) {	
					labels.add(String.valueOf(cnt));
					
					if ( ChartRequest.LabelBy.Monthly.equals(chartRequest.getLabelBy()) ) {
						runningDate = runningDate.plusMonths(1);
						cnt = Integer.valueOf(runningDate.format(formatter));
					} else if ( ChartRequest.LabelBy.Daily.equals(chartRequest.getLabelBy()) ) {
						runningDate = runningDate.plusDays(1);
						cnt = Integer.valueOf(runningDate.format(formatter));
					} else {
						cnt++;
					}					
						
				}
				
			}
		}
		
		return labels;
		
	}
	
	private DatasetItem getDataSetItem(ChartRequest chartRequest, List<PurchaseOrder> pos, List<String> labels) {
	
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		
		if ( ChartRequest.LabelBy.Yearly.equals(chartRequest.getLabelBy()) ) {
			formatter = DateTimeFormatter.ofPattern("yyyy");
		} else if ( ChartRequest.LabelBy.Monthly.equals(chartRequest.getLabelBy()) ) {	
			formatter = DateTimeFormatter.ofPattern("yyyyMM");
		} else if ( ChartRequest.LabelBy.Daily.equals(chartRequest.getLabelBy()) ) {
			formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		}
		
		Double summaryData = 0.0; 
		
		DatasetItem data = new DatasetItem();
		
		for ( String label : labels) {
			summaryData = 0.0;
			for ( PurchaseOrder po : pos ) {			
				if ( label.equals(po.getOrderDate().format(formatter)) ) {
					summaryData+= ( ChartRequest.ChartDataContent.ByCount.equals(chartRequest.getChartDataContent()) ? po.getTotalQuantity() : po.getTotalAmountDue() ); 
				}
			}
			data.addData(summaryData);			
		}		
		
		return data;		
	}
}
