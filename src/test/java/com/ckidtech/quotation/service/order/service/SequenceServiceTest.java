package com.ckidtech.quotation.service.order.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {SpringMongoConfiguration.class})
@ComponentScan({"com.ckidtech.quotation.service.order.service"})
@AutoConfigureDataMongo
public class SequenceServiceTest {
	
	@Autowired
	private SequenceService sequenceService;
	
	
	@Before
	public  void initTest() {
		sequenceService.deleteAllSequence();	
	}
	
	@Test
	public void getNextSequenceTest() {		
		
		assertEquals(1, sequenceService.getNextSequence("TEST-VENDOR1"));
		assertEquals(2, sequenceService.getNextSequence("TEST-VENDOR1"));
		assertEquals(3, sequenceService.getNextSequence("TEST-VENDOR1"));
		assertEquals(4, sequenceService.getNextSequence("TEST-VENDOR1"));
		assertEquals(5, sequenceService.getNextSequence("TEST-VENDOR1"));
		
		assertEquals(1, sequenceService.getNextSequence("TEST-VENDOR2"));
		assertEquals(2, sequenceService.getNextSequence("TEST-VENDOR2"));
		assertEquals(3, sequenceService.getNextSequence("TEST-VENDOR2"));
		assertEquals(4, sequenceService.getNextSequence("TEST-VENDOR2"));
		assertEquals(5, sequenceService.getNextSequence("TEST-VENDOR2"));
		
	}
	
	//@Test
	public void sequenceMaxedOutAndResetTest() {
		
		for ( int counter=0; counter<=SequenceService.MAX_SEQ_VALUE; counter++ ) {
			System.out.println(counter);
			if ( counter==SequenceService.MAX_SEQ_VALUE )
				assertEquals(1, sequenceService.getNextSequence("TEST-VENDOR1"));
			else
				assertEquals(counter+1, sequenceService.getNextSequence("TEST-VENDOR1"));
		}
	}
	
	
}
