package com.shiksha.erp.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public routes: /login and /forgot-password should be accessible without authentication")
    void testPublicRoutesAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/dashboard should redirect to /login")
    void testUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    @DisplayName("/admin/** routes should return 403 Forbidden for ROLE_TEACHER")
    void testAdminRouteForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "parent1", roles = {"PARENT"})
    @DisplayName("/admin/** and /teacher/** routes should return 403 Forbidden for ROLE_PARENT")
    void testStaffRoutesForbiddenForParent() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "teacher1", roles = {"TEACHER"})
    @DisplayName("/student/** routes should return 403 Forbidden for ROLE_TEACHER")
    void testStudentRouteForbiddenForTeacher() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().isForbidden());
    }
}
