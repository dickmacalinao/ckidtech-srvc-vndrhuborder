package com.ckidtech.quotation.service.order.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Service;

import com.ckidtech.quotation.service.core.dao.SequenceRepository;
import com.ckidtech.quotation.service.core.model.Sequence;

@ComponentScan({ "com.ckidtech.quotation.service.core.controller" })
@EnableMongoRepositories("com.ckidtech.quotation.service.core.dao")
@Service
public class SequenceService {

	@Autowired
	private SequenceRepository sequenceRepository;
	
	public static int MIN_SEQ_VALUE = 0;
	public static int MAX_SEQ_VALUE = 9999;
	
	
	/**
	 * Generate next sequence no per vendor
	 * @param vendorId
	 * @return
	 */
	public int getNextSequence(String vendorId) {
		
		Sequence sequence = null;
		List<Sequence> listSeq = sequenceRepository.findSequenceByVendor(vendorId);
		
		if ( listSeq.size()==0 ) {
			sequence = new Sequence(vendorId, 0, MIN_SEQ_VALUE, MAX_SEQ_VALUE);
			sequenceRepository.save(sequence);
		} else {
			sequence = listSeq.get(0);
			if ( sequence.getSequnceNo() >= sequence.getMaxValue() ) {
				sequence.setSequnceNo(0);
			}			
		}
		
		sequence.setSequnceNo(sequence.getSequnceNo()+1);		
		sequenceRepository.save(sequence);
		
		return sequence.getSequnceNo();
		
	}
	
	/**
	 * Delete all sequence record. Should not be called in the logic. This is just for unit testing
	 */
	public void deleteAllSequence() {
		sequenceRepository.deleteAll();
	}
	
	
}
