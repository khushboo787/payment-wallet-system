package com.paymentwallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentwallet.dto.response.WalletResponse;
import com.paymentwallet.exception.ResourceNotFoundException;
import com.paymentwallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@DisplayName("WalletController Integration Tests")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private com.paymentwallet.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.paymentwallet.security.CustomUserDetailsService userDetailsService;

    private WalletResponse buildWalletResponse() {
        return WalletResponse.builder()
                .id(1L)
                .walletNumber("WLTTEST123456789")
                .balance(new BigDecimal("1000.00"))
                .active(true)
                .userId(1L)
                .userFullName("Test User")
                .userEmail("user@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("GET /wallet/me - should return wallet for authenticated user")
    void getMyWallet_AuthenticatedUser_Returns200() throws Exception {
        when(walletService.getWalletByEmail("user@example.com")).thenReturn(buildWalletResponse());

        mockMvc.perform(get("/api/v1/wallet/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.walletNumber").value("WLTTEST123456789"))
                .andExpect(jsonPath("$.data.balance").value(1000.00));
    }

    @Test
    @DisplayName("GET /wallet/me - should return 403 for unauthenticated request")
    void getMyWallet_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /wallet/{walletNumber} - should return wallet by number")
    void getWalletByNumber_Exists_Returns200() throws Exception {
        when(walletService.getWalletByNumber("WLTTEST123456789")).thenReturn(buildWalletResponse());

        mockMvc.perform(get("/api/v1/wallet/WLTTEST123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletNumber").value("WLTTEST123456789"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /wallet/{walletNumber} - should return 404 for nonexistent wallet")
    void getWalletByNumber_NotFound_Returns404() throws Exception {
        when(walletService.getWalletByNumber("WLTNOTFOUND00000"))
                .thenThrow(new ResourceNotFoundException("Wallet", "walletNumber", "WLTNOTFOUND00000"));

        mockMvc.perform(get("/api/v1/wallet/WLTNOTFOUND00000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("PUT /wallet/deactivate - should deactivate wallet")
    void deactivateWallet_Returns200() throws Exception {
        WalletResponse deactivated = buildWalletResponse();
        deactivated.setActive(false);
        when(walletService.deactivateWallet("user@example.com")).thenReturn(deactivated);

        mockMvc.perform(put("/api/v1/wallet/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
