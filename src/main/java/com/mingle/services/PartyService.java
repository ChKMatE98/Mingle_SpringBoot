package com.mingle.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mingle.dao.PartyDAO;
import com.mingle.domain.entites.Member;
import com.mingle.domain.entites.PartyMember;
import com.mingle.domain.entites.PartyRegistration;
import com.mingle.domain.repositories.MemberRepository;
import com.mingle.domain.repositories.PartyInformationRepository;
import com.mingle.domain.repositories.PartyMemberRepository;
import com.mingle.domain.repositories.PartyRegistrationRepository;
import com.mingle.domain.repositories.PaymentRepository;
import com.mingle.domain.repositories.ServiceCategoryRepository;
import com.mingle.domain.repositories.ServiceRepository;
import com.mingle.dto.PartyInformationDTO;
import com.mingle.dto.PaymentDTO;
import com.mingle.dto.ServiceCategoryDTO;
import com.mingle.dto.ServiceDTO;
import com.mingle.mappers.PartyInformationMapper;
import com.mingle.mappers.PaymentMapper;
import com.mingle.mappers.ServiceCategoryMapper;
import com.mingle.mappers.ServiceMapper;

@Service
public class PartyService {
	
	// 서비스 카테고리
	@Autowired
	private ServiceCategoryRepository scRepo;
	@Autowired
	private ServiceCategoryMapper scMap;
	
	// 서비스
	@Autowired
	private ServiceRepository sRepo;
	@Autowired
	private ServiceMapper sMap;

	// 파티 정보
	@Autowired
	private PartyInformationRepository piRepo;
	@Autowired
	private PartyInformationMapper piMap;
	@Autowired
	private PartyDAO pdao;
	
	// 파티 등록
	@Autowired
	private PartyRegistrationRepository prRepo;
	
	// 파티장 등록
	@Autowired
	private PartyMemberRepository pmRepo;
	
	// 첫 달 결제 정보 저장을 위한 paymentRepo, mapper
	@Autowired
	private PaymentRepository payRepo;
	@Autowired
	private PaymentMapper payMap;
	
	// 밍글머니 계산을 위한 MemberRepo
	@Autowired
	private MemberRepository mRepo;
	
	// 제공하는 서비스 카테고리명 불러오기
	public List<ServiceCategoryDTO> selectCategoryAll() {
		return scMap.toDtoList(scRepo.findAll());
	}
	
	
	// 카테고리별 서비스 정보 불러오기
	public List<ServiceDTO> selectServiceByCategoryId(String id) {
		if(id.equals("전체")) {
			return sMap.toDtoList(sRepo.findAll());
		}else {
			return sMap.toDtoList(sRepo.findByServiceCategoryId(id));
		}
	}
	
	
	// 가입된 파티 서비스 목록 불러오기
	public List<Integer> selectServiceByIsJoin(String service_category_id, String member_id){
		if(service_category_id.equals("전체")) {
			return sRepo.selectByAllAndIsJoin(member_id);
		}else {
			return sRepo.selectByServiceCategoryIdAndIsJoin(service_category_id, member_id);
		}
	}
	
	// 특정 서비스 정보 불러오기
	public ServiceDTO selectServiceByServiceId(Long id) {
		ServiceDTO dto = sMap.toDto(sRepo.findById(id).get());
		return dto;
	}
	
	// 서비스별 파티 이용자수
	public List<Map<String, Object>> selectCountUserByService() {
		return pdao.selectCountUserByService();
	}

	// 파티 정보 저장
	@Transactional
	public void inertParty(PartyInformationDTO partyData, String member_id){
		// 파티 정보 저장
		long id = piRepo.save(piMap.toEntity(partyData)).getId();
		
		// 파티 등록
		PartyRegistration pre = new PartyRegistration(id);
		prRepo.save(pre);
		
		// 파티장 등록
		PartyMember pme = new PartyMember(0L, id, member_id, true);
		pmRepo.save(pme);
	}
	
	// 파티 가입 & 첫 달 결제 내역 저장 & 밍글 머니 사용
	@Transactional
	public void insertJoinParty(Long party_registration_id, String member_id, PaymentDTO paymentData){
		// 파티 가입
		PartyMember pme = new PartyMember(0L, party_registration_id, member_id, false);
		pmRepo.save(pme);
		
		// 첫 달 결제 내역 저장 -> 파티 시작일 이후에 가입했을 경우에만 첫 결제 바로 진행되도록 변경해야함
		paymentData.setPartyRegistrationId(party_registration_id);
		paymentData.setMemberId(member_id);
		paymentData.setPaymentTypeId("결제");
		payRepo.save(payMap.toEntity(paymentData));
		
		// 밍글 머니 사용했을 경우 업데이트
		if(paymentData.getUsedMingleMoney()!=0) {
			Member m = mRepo.findById(member_id).get();
			m.setMingleMoney(m.getMingleMoney()-paymentData.getUsedMingleMoney());
			mRepo.save(m);
		}
	}
	
	// 등록된 파티 정보 불러오기
	public List<PartyInformationDTO> selectPartyList(Long id){
		return piMap.toDtoList(piRepo.findPartyInformationByServiceIdAndCount(id));
	}
	
	// 등록된 파티 정보 중 선택한 날짜에 해당하는 파티 정보 불러오기
	public List<PartyInformationDTO> selectPartyListByStartDate(Long id, Instant start, Instant end){
		return piMap.toDtoList(piRepo.findPartyInformationByServiceIdAndCountAndStartDate(id, start, end));
	}
	
	// 서비스 명 리스트 불러오기
	public List<ServiceDTO> getServiceNameList(){
	
		return 	sMap.toDtoList(sRepo.findAll());
	}
}
