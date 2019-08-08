package com.ckidtech.quotation.service.vendorhuborder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class QuotationServiceOrderApp {
	
	@Autowired
	private Environment env;
	
	public static void main(String[] args) {
		SpringApplication.run(QuotationServiceOrderApp.class, args);
	}

	@RequestMapping("/")
	public String index() {
		return "Welcome to Vendor Hub Order App Service at port " + env.getProperty("local.server.port") + ".";
	}

}
