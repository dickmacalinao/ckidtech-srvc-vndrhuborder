package com.ckidtech.quotation.service.purchaseorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class QuotationServicePurchaseOrderApp {
	
	@Autowired
	private Environment env;
	
	public static void main(String[] args) {
		SpringApplication.run(QuotationServicePurchaseOrderApp.class, args);
	}

	@RequestMapping("/")
	public String index() {
		return "Welcome to Quotation Purchase Order App Service at port " + env.getProperty("local.server.port") + ".";
	}

}
