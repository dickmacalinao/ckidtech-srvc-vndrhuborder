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
import com.ckidtech.quotation.service.core.dao.OrderRepository;
import com.ckidtech.quotation.service.core.dao.ProductRepository;
import com.ckidtech.quotation.service.core.dao.VendorRepository;
import com.ckidtech.quotation.service.core.exception.ServiceAccessResourceFailureException;
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
	private VendorRepository vendorRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private SequenceService sequenceService;
	
	@Autowired
	private MessageController msgController;

	private static final Logger LOG = Logger.getLogger(OrderService.class.getName());
	
	public QuotationResponse getVendorOrder(AppUser loginUser, OrderSearchCriteria orderSearchCriteria) throws Exception {
		LOG.log(Level.INFO, "Calling Order Service getVendorOrder()");
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);		
		
		QuotationResponse quotation = new QuotationResponse();
		
		if (orderSearchCriteria.getDateFrom() == null)
			quotation.addMessage(msgController.createMsg("error.MFE", "Date From"));
		if (orderSearchCriteria.getDateTo() == null)
			quotation.addMessage(msgController.createMsg("error.MFE", "Date To"));
		
		if ( quotation.getMessages().isEmpty() ) {
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			
			LocalDateTime dateFrom = LocalDateTime.parse(orderSearchCriteria.getDateFrom() + " 00:00:00", formatter);
			LocalDateTime dateTo = LocalDateTime.parse(orderSearchCriteria.getDateTo() + " 23:59:59", formatter);
			
			Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
			
			quotation.setOrders(orderRepository.findVendorOrder(loginUser.getObjectRef(), dateFrom, dateTo, pageable));
			
			
			if ( orderSearchCriteria.getStatus()!=null ) {
				quotation.setOrders(orderRepository.findVendorOrderByStatus(loginUser.getObjectRef(), 
						dateFrom, dateTo, orderSearchCriteria.getStatus(), pageable));
			} else {	
				quotation.setOrders(orderRepository.findVendorOrder(loginUser.getObjectRef(), dateFrom, dateTo, pageable));
			}
				
			
		}
				
		return quotation;
	}
	
	public List<Order> getUserOrderForTheDay(AppUser loginUser, LocalDateTime dateFrom, LocalDateTime dateTo) {
		LOG.log(Level.INFO, "Calling Order Service getUserOrderForTheDay()");
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);
		
		Pageable pageable = new PageRequest(0, 100, Sort.Direction.ASC, "orderDate");
		return orderRepository.findUserOrder(loginUser.getObjectRef(), loginUser.getId(), dateFrom, dateTo, pageable);	
	}
	
	public QuotationResponse createNewOrder(AppUser loginUser, Order order) throws Exception {
		
		LOG.log(Level.INFO, "Calling Order Service createNewOrder()");
		
		Util.checkIfAlreadyActivated(loginUser);
			
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);	
		
		QuotationResponse quotation = new QuotationResponse();
		
		// Set Order Date if not specified
		if ( order.getOrderDate()== null ) {
			order.setOrderDate(LocalDateTime.now());
		}	
		
		// Set Reference Order if not specified
		if ( order.getReferenceOrder()==null || "".equals(order.getReferenceOrder())) {
			order.setReferenceOrder("#" + sequenceService.getNextSequence(loginUser.getObjectRef()));			
		}
		
		order.setVendorId(loginUser.getObjectRef());
		order.setUserId(loginUser.getId());
		order.setStatus(Order.Status.New);		
		order.setActiveIndicator(true);
		Util.initalizeCreatedInfo(order, loginUser.getId(), msgController.getMsg("info.PORC"));	
		orderRepository.save(order);
		
		quotation.setOrder(order);
		quotation.addMessage(msgController.createMsg("info.PORC"));
			
		
		return quotation;
		
	}

	public QuotationResponse updateOrder(AppUser loginUser, Order order) throws Exception {
		
		LOG.log(Level.INFO, "Calling Order Service updateOrder()");
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);

		QuotationResponse quotation = new QuotationResponse();
		
		if (order.getId() == null || "".equals(order.getId()))
			quotation.addMessage(msgController.createMsg("error.MFE", "Order ID"));
		if (order.getStatus()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Status"));
		if (order.getOrderDate()== null )
			quotation.addMessage(msgController.createMsg("error.MFE", "Order Date"));
				
		if (quotation.getMessages().isEmpty()) {
			
			Order orderRep = orderRepository.findById(order.getId()).orElse(null);
			if  ( orderRep==null || !orderRep.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.PONFE"));
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				order.setUserId(loginUser.getId());
					
				Util.initalizeUpdatedInfo(orderRep, loginUser.getId(), orderRep.getDifferences(order));		
				orderRep.setUserId(loginUser.getId());
				orderRep.setStatus(order.getStatus());
				orderRep.setOrderDate(order.getOrderDate());					
				orderRepository.save(orderRep);
				
				quotation.setOrder(orderRep);
				quotation.addMessage(msgController.createMsg("info.PORU"));
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse addOrderItem(AppUser loginUser, String orderID, OrderItem orderItem) throws Exception {
		
		LOG.log(Level.INFO, "Calling Order Service addToOrderList()");
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);

		QuotationResponse quotation = new QuotationResponse();
		
		if (orderID == null || "".equals(orderID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Order ID"));
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
			} else if ( prod.getProdComp()==null )  {
				quotation.addMessage(msgController.createMsg("error.VPCME"));
			}
			
			OrderItem orderItemRep = null;
			
			// Retrieve the order item from previous and if exists, otherwise initialize
			if ( orderRep.getOrders() == null ) {
				orderRep.setOrders(new HashMap<String, OrderItem>());
			} else {					
				orderItemRep = orderRep.getOrders().get(orderItem.getProductId());
				
			}
			
			if (quotation.getMessages().isEmpty()) { 
				
				// If order item is not exists, add otherwise update existing
				if ( orderItemRep==null ) {
					orderItem.setAmountDue(prod.getProdComp().getComputedAmount() * orderItem.getQuantity());	// Compute the Amount Due
					Util.initalizeUpdatedInfo(orderRep, loginUser.getId(), String.format(msgController.getMsg("info.POAO"), orderItem.toString()));
					orderRep.getOrders().put(orderItem.getProductId(), orderItem);
					quotation.addMessage(msgController.createMsg("info.POAO", prod.getName()));
				} else {
					orderItemRep.setQuantity(orderItem.getQuantity());
					orderItemRep.setAmountDue(prod.getProdComp().getComputedAmount() * orderItem.getQuantity()); // Compute the Amount Due
					Util.initalizeUpdatedInfo(orderRep, loginUser.getId(), String.format(msgController.getMsg("info.POUO"), orderRep.toString()));
					quotation.addMessage(msgController.createMsg("info.POUO", prod.getName()));
				}
					
				orderRepository.save(orderRep);				
				quotation.setOrder(orderRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public QuotationResponse removeFromOrderList(AppUser loginUser, String orderID, String productId) throws Exception {
		
		LOG.log(Level.INFO, "Calling Order Service removeFromOrderList()");
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);

		QuotationResponse quotation = new QuotationResponse();
		
		if (orderID == null || "".equals(orderID))
			quotation.addMessage(msgController.createMsg("error.MFE", "Order ID"));	
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
			
			Product prod = productRepository.findById(productId).orElse(null);
			// Verify if Product exists and active
			if  ( prod==null || !prod.isActiveIndicator() ) {
				quotation.addMessage(msgController.createMsg("error.VPNFE"));
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
					Util.initalizeUpdatedInfo(orderRep, loginUser.getId(), String.format(msgController.getMsg("info.PORO"), prod.getName()));					
					orderRep.getOrders().remove(productId);
					orderRepository.save(orderRep);
					quotation.addMessage(msgController.createMsg("info.PORO", prod.getName()));
				}	
								
				quotation.setOrder(orderRep);
				
			}
		}		
		
		return quotation;
		
	}
	
	public ChartResponse getOrderChart(AppUser loginUser, ChartRequest chartRequest) throws Exception {		
		
		Util.checkIfAlreadyActivated(loginUser);
		
		Vendor vendorRep = vendorRepository.findById(loginUser.getObjectRef()).orElse(null);	
		Util.checkIfAlreadyActivated(vendorRep);
		
		ChartResponse chartResponse = new ChartResponse();
		
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
			
			datasets.add(getDataSetItem(loginUser.getObjectRef(), chartRequest, labels));
			
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
	
	private DatasetItem getDataSetItem(String vendorId, ChartRequest chartRequest, List<String> labels) {
	
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
			List<Order> orders = orderRepository.findVendorOrderByStatus(vendorId, 
					dateFrom, dateTo, chartRequest.getStatus().toString(), pageable);
			
			for ( Order order : orders ) {
				summaryData+= ( ChartRequest.ChartDataContent.ByCount.equals(chartRequest.getChartDataContent()) ? order.getTotalQuantity() : order.getTotalAmountDue() );
			}
			data.addData(summaryData);			
		}		
		
		return data;		
	}
	
	
	/**
	 * This should not be called in the logic. This is used for unit testing purposes only
	 */
	public void deleteAllOrders() {
		orderRepository.deleteAll();		
	}
	
}
