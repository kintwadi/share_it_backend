package com.vicinity24.api.core.service;

import com.vicinity24.api.core.dto.ReturnDTOs;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.AvailabilityStatus;
import com.vicinity24.api.core.model.enums.ListingType;
import com.vicinity24.api.core.model.enums.ReturnStatus;
import com.vicinity24.api.core.model.enums.UserRole;
import com.vicinity24.api.core.model.enums.UserStatus;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import com.vicinity24.api.core.partner.model.Partner;
import com.vicinity24.api.core.partner.model.PartnerAdmin;
import com.vicinity24.api.core.partner.model.PartnerAdminRole;
import com.vicinity24.api.core.partner.model.PartnerStatus;
import com.vicinity24.api.core.partner.repository.PartnerAdminRepository;
import com.vicinity24.api.core.partner.repository.PartnerRepository;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.ReturnSessionRepository;
import com.vicinity24.api.core.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReturnServiceManualFlowTest {
    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private EscrowService escrowService;

    @MockBean
    private ReviewInviteService reviewInviteService;

    @Autowired
    private ReturnService returnService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ReturnSessionRepository returnSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerAdminRepository partnerAdminRepository;

    @BeforeEach
    public void setup() {
        returnSessionRepository.deleteAll();
        listingRepository.deleteAll();
        partnerAdminRepository.deleteAll();
        partnerRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void nonPartner_manualReturn_stillWorks() {
        User owner = createUser("owner@test.com");
        User borrower = createUser("borrower@test.com");

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(owner);
        listing.setPartner(null);
        listing.setBorrower(borrower);
        listing.setTitle("Drill");
        listing.setCategory("Tools");
        listing.setType(ListingType.LEND);
        listing.setStatus(AvailabilityStatus.BORROWED);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setItemReference("12345678");
        listingRepository.save(listing);

        returnService.initiateReturn(listing.getId(), borrower);

        ReturnDTOs.ManualFallbackRequest borrowerReq = new ReturnDTOs.ManualFallbackRequest();
        borrowerReq.setItemNumber("12345678");
        ReturnDTOs.ReturnSessionResponse afterBorrower = returnService.manualFallback(listing.getId(), borrower, borrowerReq);
        Assertions.assertTrue(afterBorrower.isManualBorrowerConfirmed());
        Assertions.assertFalse(afterBorrower.isManualLenderConfirmed());
        Assertions.assertEquals(ReturnStatus.PENDING, afterBorrower.getStatus());

        ReturnDTOs.ManualFallbackRequest ownerReq = new ReturnDTOs.ManualFallbackRequest();
        ownerReq.setItemNumber("12345678");
        ownerReq.setConciergeWitnessId("WITNESS");
        ReturnDTOs.ReturnSessionResponse afterOwner = returnService.manualFallback(listing.getId(), owner, ownerReq);
        Assertions.assertEquals(ReturnStatus.COMPLETED, afterOwner.getStatus());

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        Assertions.assertEquals(AvailabilityStatus.AVAILABLE, updated.getStatus());
        Assertions.assertNull(updated.getBorrower());
        Assertions.assertNull(updated.getItemReference());
    }

    @Test
    public void nonPartner_manualReturn_doesNotAllowRandomUser() {
        User owner = createUser("owner2@test.com");
        User borrower = createUser("borrower2@test.com");
        User stranger = createUser("stranger@test.com");

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(owner);
        listing.setPartner(null);
        listing.setBorrower(borrower);
        listing.setTitle("Bike");
        listing.setCategory("Sports");
        listing.setType(ListingType.LEND);
        listing.setStatus(AvailabilityStatus.BORROWED);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setItemReference("22223333");
        listingRepository.save(listing);

        returnService.initiateReturn(listing.getId(), borrower);

        ReturnDTOs.ManualFallbackRequest req = new ReturnDTOs.ManualFallbackRequest();
        req.setItemNumber("22223333");
        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class, () ->
                returnService.manualFallback(listing.getId(), stranger, req)
        );
        Assertions.assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    public void partner_manualReturn_allowsPartnerAdminToAcceptWithoutBreakingNormalFlow() {
        User borrower = createUser("borrower3@test.com");
        User firstAdmin = createUser("partneradmin1@test.com");
        User secondAdmin = createUser("partneradmin2@test.com");

        Partner partner = Partner.builder()
                .id(UUID.randomUUID())
                .name("Partner A")
                .status(PartnerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        partnerRepository.save(partner);

        partnerAdminRepository.save(PartnerAdmin.builder()
                .id(UUID.randomUUID())
                .partner(partner)
                .user(firstAdmin)
                .role(PartnerAdminRole.ADMIN)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());
        partnerAdminRepository.save(PartnerAdmin.builder()
                .id(UUID.randomUUID())
                .partner(partner)
                .user(secondAdmin)
                .role(PartnerAdminRole.ADMIN)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build());

        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setOwner(null);
        listing.setPartner(partner);
        listing.setBorrower(borrower);
        listing.setTitle("Ladder");
        listing.setCategory("Tools");
        listing.setType(ListingType.LEND);
        listing.setStatus(AvailabilityStatus.BORROWED);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setItemReference("77778888");
        listingRepository.save(listing);

        returnService.initiateReturn(listing.getId(), borrower);

        ReturnDTOs.ManualFallbackRequest borrowerReq = new ReturnDTOs.ManualFallbackRequest();
        borrowerReq.setItemNumber("77778888");
        ReturnDTOs.ReturnSessionResponse afterBorrower = returnService.manualFallback(listing.getId(), borrower, borrowerReq);
        Assertions.assertTrue(afterBorrower.isManualBorrowerConfirmed());
        Assertions.assertFalse(afterBorrower.isManualLenderConfirmed());

        ReturnDTOs.ManualFallbackRequest partnerAdminReq = new ReturnDTOs.ManualFallbackRequest();
        partnerAdminReq.setItemNumber("77778888");
        partnerAdminReq.setConciergeWitnessId("PARTNER_WITNESS");
        ReturnDTOs.ReturnSessionResponse afterPartnerAdmin = returnService.manualFallback(listing.getId(), secondAdmin, partnerAdminReq);
        Assertions.assertEquals(ReturnStatus.COMPLETED, afterPartnerAdmin.getStatus());

        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        Assertions.assertEquals(AvailabilityStatus.PARTNER_ACTIVE, updated.getStatus());
        Assertions.assertNull(updated.getBorrower());
        Assertions.assertNull(updated.getItemReference());
    }

    private User createUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setName("Test User");
        u.setPassword("password");
        u.setRole(UserRole.MEMBER);
        u.setStatus(UserStatus.ACTIVE);
        u.setVerificationStatus(VerificationStatus.VERIFIED);
        u.setJoinedDate(LocalDateTime.now());
        u.setTrustScore(100);
        u.setVouchCount(0);
        return userRepository.save(u);
    }
}
