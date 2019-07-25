package com.ckidtech.quotation.service.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import com.ckidtech.quotation.service.core.dao.OrderRepository;
import com.ckidtech.quotation.service.core.dao.ProductRepository;
import com.ckidtech.quotation.service.core.dao.VendorRepository;
import com.ckidtech.quotation.service.core.model.AppUser;
import com.ckidtech.quotation.service.core.model.ChartRequest;
import com.ckidtech.quotation.service.core.model.ChartResponse;
import com.ckidtech.quotation.service.core.model.DatasetItem;
import com.ckidtech.quotation.service.core.model.Order;
import com.ckidtech.quotation.service.core.model.OrderItem;
import com.ckidtech.quotation.service.core.model.OrderSearchCriteria;
import com.ckidtech.quotation.service.core.model.Product;
import com.ckidtech.quotation.service.core.model.Vendor;
import com.ckidtech.quotation.service.core.utils.Util;

@ComponentScan({"com.ckidtech.quotation.service.core.controller"})
@EnableMongoRepositories ("com.ckidtech.quotation.service.core.dao")
@Service
public class OrderService {
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private AppUserRepository appUserRepository;
	
	@Autowired
	private VendorRepository vendorRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private MessageController msgController;

	private static final Logger LOG = Logger.getLogger(OrderService.class.getName());
	
	public QuotationResponse getVendorOrder(OrderSearchCriteria orderSearchCriteria) {
		LOG.log(Level.INFO, "Calling Order Service getVendorOrder()");
		
		QuotationResponse quotation = new QuotationResponse();
		
		if (orderSearchCriteria.getVendorId() == null || "".equals(orderSearchCriteria.getVendorId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Vendor ID"));
		if (orderSearchCriteria.getDateFrom() == null)
			quotation.addMessage(msgController.createMsg("error.MFE", "Date From"));
		if (orderSearchCriteria.getDateTo() == null)
			quotation.addMessage(msgController.createMsg("error.MFE", "Date To"));
		
		if ( quotation.getMessages().isEmpty() ) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			
			LocalDateTime dateFrom = LocalDateTime.parse(orderSearchCriteria.getDateFrom() + " 00:00:00", formatter);
			LocalDateTime dateTo = LocalDateTime.parse(orderSearchCriteria.getDateTo() + " 23:59:59", formatter);
			
			Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
			
			if ( orderSearchCriteria.getStatus()!=null && !"".equals(orderSearchCriteria.getUserId()==null?"":orderSearchCriteria.getUserId()) ) {
				quotation.setOrders(orderRepository.findVendorOrderByStatusAndUser(orderSearchCriteria.getVendorId(), 
						dateFrom, dateTo, orderSearchCriteria.getStatus(), orderSearchCriteria.getUserId(), pageable));
			} else if ( orderSearchCriteria.getStatus()!=null ) {
				quotation.setOrders(orderRepository.findVendorOrderByStatus(orderSearchCriteria.getVendorId(), 
						dateFrom, dateTo, orderSearchCriteria.getStatus(), pageable));
			} else if ( !"".equals(orderSearchCriteria.getUserId()==null?"":orderSearchCriteria.getUserId()) ) {
				quotation.setOrders(orderRepository.findVendorOrderByUser(orderSearchCriteria.getVendorId(), 
						dateFrom, dateTo, orderSearchCriteria.getUserId(), pageable));
			} else {	
				quotation.setOrders(orderRepository.findVendorOrder(orderSearchCriteria.getVendorId(), dateFrom, dateTo, pageable));
			}		
			
		}
				
		return quotation;
	}
	
	public List<Order> getUserOrderForTheDay(String vendorId, String userId, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Order Service getUserOrderForTheDay()");
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return orderRepository.findUserOrder(vendorId, userId, dateFrom, dateTo, pageable);	
	}
	
	public QuotationResponse createNewOrder(Order order) {
		
		LOG.log(Level.INFO, "Calling Order Service createNewOrder()");

		QuotationResponse quotation = new QuotationResponse();		
		
		if (order.getReferenceOrder() == null || "".equals(order.getReferenceOrder()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Reference Order"));
		if (order.getVendorId()== null || "".equals(order.getVendorId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Vendor ID"));
		if (order.getUserId()== null || "".equals(order.getUserId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "User ID"));
		if (order.getOrderDate()== null)
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Date"));
		
		if (quotation.getMessages().isEmpty()) {
			
			Vendor vendorRep = vendorRepository.findById(order.getVendorId().toUpperCase()).orElse(null);			
			// Verify if Vendor exists and active
			if (vendorRep==null || !vendorRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VNFE"));
			}
				
			AppUser appUserRep = appUserRepository.findById(order.getUserId()).orElse(null);			
			// Verify if App User exists and active
			if  ( appUserRep==null || !appUserRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.AUNFE"));
			} 
								
			if (quotation.getMessages().isEmpty()) { 

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");					
				order.setId(order.getVendorId() + "-" + LocalDateTime.now().format(formatter));
				
				order.setStatus(Order.Status.New);		
				order.setActiveIndicator(true);
				Util.initalizeCreatedInfo(order, msgController.getMsg("info.PORC"));	
				orderRepository.save(order);
				
				quotation.setOrder(order);
			}
		}		
		
		return quotation;
		
	}

	public QuotationResponse updateOrder(Order order) {
		
		LOG.log(Level.INFO, "Calling Order Service updateOrder()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (order.getId() == null || "".equals(order.getId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));
		if (order.getReferenceOrder() == null || "".equals(order.getReferenceOrder()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Reference Order"));
		if (order.getUserId()== null || "".equals(order.getUserId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "User ID"));
		if (order.getStatus()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Status"));
		if (order.getOrderDate()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Date"));
		
		if (quotation.getMessages().isEmpty()) {
			
			Vendor vendorRep = vendorRepository.findById(order.getVendorId().toUpperCase()).orElse(null);			
			// Verify if Vendor exists and active
			if (vendorRep==null || !vendorRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VNFE"));
			}
				
			AppUser appUserRep = appUserRepository.findById(order.getUserId()).orElse(null);			
			// Verify if App User exists and active
			if  ( appUserRep==null || !appUserRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.AUNFE"));
			} 
			
			Order orderRep = orderRepository.findById(order.getId()).orElse(null);
			// Verify if Purchase order exists and active
			if  ( orderRep==null || !orderRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			}
			
			if (quotation.getMessages().isEmpty()) { 
					
				Util.initalizeUpdatedInfo(orderRep, orderRep.getDifferences(order));		
				orderRep.setReferenceOrder(order.getReferenceOrder());
				orderRep.setUserId(order.getUserId());
				orderRep.setStatus(order.getStatus());
				orderRep.setOrderDate(order.getOrderDate());					
				orderRepository.save(orderRep);
				
				quotation.setOrder(orderRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse addOrderItem(String orderID, OrderItem orderItem) {
		
		LOG.log(Level.INFO, "Calling Order Service addToOrderList()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (orderID == null || "".equals(orderID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));
		if (orderItem == null) {
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Item"));
		} else {
			if ( orderItem.getProductId()==null || "".equals(orderItem.getProductId()) ) 
				quotation.addMessage(msgController.createMsg("error.MFE", "Product ID"));			
		}
		
		if (quotation.getMessages().isEmpty()) {
			
			Order orderRep = orderRepository.findById(orderID).orElse(null);
			// Verify if Purchase order exists and active
			if  ( orderRep==null || !orderRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			} else {
				
				// Only status New or Ordered can add oder to list				
				if ( !orderRep.getStatus().equals(Order.Status.New) && !orderRep.getStatus().equals(Order.Status.Ordered) ) {
					quotation.addMessage(msgController.createMsg("error.POUNAE"));
				}
			}
			
			Product prod = productRepository.findById(orderItem.getProductId()).orElse(null);
			// Verify if Product exists and active
			if  ( prod==null || !prod.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VPNFE"));
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				OrderItem orderItemRep = null;
				
				// Retrieve the order item from previous and if exists, otherwise initialize
				if ( orderRep.getOrders() == null ) {
					orderRep.setOrders(new HashMap<String, OrderItem>());
				} else {					
					orderItemRep = orderRep.getOrders().get(orderItem.getProductId());
				}
				
				// If order item is not exists, add otherwise update existing
				if ( orderItemRep==null ) {
					orderItem.setAmountDue(prod.getProdComp().getComputedAmount() * orderItem.getQuantity());	// Compute the Amount Due
					Util.initalizeUpdatedInfo(orderRep, String.format(msgController.getMsg("info.POAO"), orderItem.toString()));
					orderRep.getOrders().put(orderItem.getProductId(), orderItem);
				} else {
					orderItemRep.setQuantity(orderItem.getQuantity());
					orderItemRep.setAmountDue(prod.getProdComp().getComputedAmount() * orderItem.getQuantity()); // Compute the Amount Due
					Util.initalizeUpdatedInfo(orderRep, String.format(msgController.getMsg("info.POUO"), orderRep.toString()));
				}
					
				orderRepository.save(orderRep);				
				quotation.setOrder(orderRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse removeFromOrderList(String orderID, String productId) {
		
		LOG.log(Level.INFO, "Calling Order Service removeFromOrderList()");

		QuotationResponse quotation = new QuotationResponse();
		
		if (orderID == null || "".equals(orderID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Purchase Order ID"));	
		if ( productId==null || "".equals(productId) ) 
			quotation.addMessage(msgController.createMsg("error.MFE", "Product ID"));			
		
		if (quotation.getMessages().isEmpty()) {
			
			Order orderRep = orderRepository.findById(orderID).orElse(null);
			// Verify if Purchase order exists and active
			if  ( orderRep==null || !orderRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			} else {
				
				// Only status New or Ordered can remove oder from list				
				if ( !orderRep.getStatus().equals(Order.Status.New) && !orderRep.getStatus().equals(Order.Status.Ordered) ) {
					quotation.addMessage(msgController.createMsg("error.POUNAE"));
				}
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				OrderItem orderItemRep = null;
				
				// Retrieve the order from previous and if exists
				if ( orderRep.getOrders() == null ) {
					orderRep.setOrders(new HashMap<String, OrderItem>());
				} else {
					orderItemRep = orderRep.getOrders().get(productId);	
				}
								
				if ( orderItemRep==null ) {
					quotation.addMessage(msgController.createMsg("error.POINFE"));
				} else {
					Util.initalizeUpdatedInfo(orderRep, String.format(msgController.getMsg("info.PORO"), orderRep.toString()));					
					orderRep.getOrders().remove(productId);
					orderRepository.save(orderRep);
				}	
								
				quotation.setOrder(orderRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public ChartResponse getOrderChart(ChartRequest chartRequest) {
		ChartResponse chartResponse = new ChartResponse();
		
		if (chartRequest.getVendorId()==null || "".equals(chartRequest.getVendorId())) {
			chartResponse.addMessage(msgController.createMsg("error.MFE", "Vendor ID"));
		} else {
			Vendor vendorRep = vendorRepository.findById(chartRequest.getVendorId().toUpperCase()).orElse(null);			
			// Verify if Vendor exists and active
			if (vendorRep==null || !vendorRep.isActiveIndicator() ) {
				chartResponse.addMessage(msgController.createMsg("error.VNFE"));
			}				
		}
		if ( chartRequest.getLabelBy()==null ) {
			chartResponse.addMessage(msgController.createMsg("error.MFE", "Label By"));
		}
		if ( chartRequest.getDateFrom()==null ) {
			chartResponse.addMessage(msgController.createMsg("error.MFE", "Date From"));
		}
		if ( chartRequest.getDateTo()==null ) {
			chartResponse.addMessage(msgController.createMsg("error.MFE", "Date To"));
		}
		if (chartRequest.getStatus()==null ) {
			chartResponse.addMessage(msgController.createMsg("error.MFE", "Status"));
		}
		
		if ( chartResponse.getMessages().isEmpty() ) {

			List<String> labels = getChartLabels(chartRequest, chartResponse);
			List<DatasetItem> datasets = new ArrayList<DatasetItem>();
			
			datasets.add(getDataSetItem(chartRequest, labels));
			
			chartResponse.setLabels(labels);
			chartResponse.setDatasets(datasets);
			
		}		
		
		return chartResponse;		
	}
	
	private List<String> getChartLabels(ChartRequest chartRequest, ChartResponse chartResponse) {
		ArrayList<String> labels = new ArrayList<String>();
		
		LocalDateTime startRecOrderDate = null;
		LocalDateTime endRecOrderDate = null;
		LocalDateTime runningDate = null;
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		
		try {
			startRecOrderDate = LocalDateTime.parse(chartRequest.getDateFrom() + " 00:00:00",formatter);
		} catch (DateTimeParseException dtpe) {
			chartResponse.addMessage(msgController.createMsg("error.IDFE", "yyyy-MM-dd", "Date From"));
		}
		
		try {
			endRecOrderDate = LocalDateTime.parse(chartRequest.getDateTo() + " 23:59:59",formatter);
		} catch (DateTimeParseException dtpe) {
			chartResponse.addMessage(msgController.createMsg("error.IDFE", "yyyy-MM-dd", "Date To"));
		}
		
		if ( chartResponse.getMessages().isEmpty() ) {
			
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
	
	private DatasetItem getDataSetItem(ChartRequest chartRequest, List<String> labels) {
	
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");		
		DatasetItem data = new DatasetItem();
		
		Double summaryData = 0.0;
		LocalDateTime dateFrom;
		LocalDateTime dateTo;
		
		for ( String label : labels) {
			summaryData = 0.0;
			
			if ( ChartRequest.LabelBy.Yearly.equals(chartRequest.getLabelBy()) ) {
				dateFrom = LocalDateTime.parse(label + "0101 00:00:00", formatter);
				dateTo = LocalDateTime.parse(label + "1231 23:59:59", formatter);
			} else if ( ChartRequest.LabelBy.Monthly.equals(chartRequest.getLabelBy()) ) {
				dateFrom = LocalDateTime.parse(label + "01 00:00:00", formatter);
				dateTo = dateFrom.plusMonths(1).minusSeconds(1);
			} else { // If Label By is Daily just loop for all Paid orders to get the summary
				dateFrom = LocalDateTime.parse(label + " 00:00:00", formatter);
				dateTo = LocalDateTime.parse(label + " 23:59:59", formatter);
			}
			
			Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
			List<Order> orders = orderRepository.findVendorOrderByStatus(chartRequest.getVendorId(), 
					dateFrom, dateTo, chartRequest.getStatus().toString(), pageable);
			
			for ( Order order : orders ) {
				summaryData+= ( ChartRequest.ChartDataContent.ByCount.equals(chartRequest.getChartDataContent()) ? order.getTotalQuantity() : order.getTotalAmountDue() );
			}
			data.addData(summaryData);			
		}		
		
		return data;		
	}
	
}
