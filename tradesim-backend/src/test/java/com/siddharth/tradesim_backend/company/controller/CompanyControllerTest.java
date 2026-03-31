package com.siddharth.tradesim_backend.company.controller;

import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.service.CompanyManagerAssignmentService;
import com.siddharth.tradesim_backend.company.service.CompanyOnboardingService;
import com.siddharth.tradesim_backend.company.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyManagerAssignmentService companyManagerAssignmentService;

    @MockitoBean
    private CompanyOnboardingService companyOnboardingService;

    @Test
    void shouldReturnCompanies() throws Exception {
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(),
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE
        );

        when(companyService.fetchCompanies()).thenReturn(List.of(company));

        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].code").value("APPLE"))
                .andExpect(jsonPath("$[0].country").value("United States"))
                .andExpect(jsonPath("$[0].status").value(CompanyStatus.ACTIVE.name()));
    }

    @Test
    void shouldReturnCompanyById() throws Exception {
        UUID companyId = UUID.randomUUID();
        CompanyResponse company = new CompanyResponse(
                companyId,
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE
        );

        when(companyService.fetchCompany(companyId)).thenReturn(company);

        mockMvc.perform(get("/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.code").value("APPLE"))
                .andExpect(jsonPath("$.country").value("United States"));
    }
}