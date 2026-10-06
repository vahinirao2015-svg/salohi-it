package com.salohi.hrms.web;

import com.salohi.hrms.repo.EmployeeRepository;
import com.salohi.hrms.repo.LeaveRequestRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class PortalFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employees;

    @Autowired
    private LeaveRequestRepository leaves;

    @Test
    void signInLeaveApprovalAndPayslip() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound());

        MockHttpSession arjun = session("arjun@salohi.it");
        mockMvc.perform(get("/").session(arjun))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("People desk")));
        mockMvc.perform(get("/admin/employees").session(arjun))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/leaves")
                        .session(arjun)
                        .with(csrf())
                        .param("leaveType", "UNPAID")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-02")
                        .param("reason", "Family travel"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/leaves"));

        MockHttpSession admin = session("admin@salohi.it");
        Long leaveId = leaves.findAll().get(0).getId();
        mockMvc.perform(post("/admin/leaves/" + leaveId + "/approve")
                        .session(admin)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/leaves"));

        Long arjunId = employees.findByEmailIgnoreCase("arjun@salohi.it").orElseThrow().getId();
        MvcResult generated = mockMvc.perform(post("/admin/payslips")
                        .session(admin)
                        .with(csrf())
                        .param("employeeId", arjunId.toString())
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isFound())
                .andReturn();

        String location = generated.getResponse().getRedirectedUrl();
        mockMvc.perform(get(location).session(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Loss of pay")))
                .andExpect(content().string(containsString("2 unpaid days")));
    }

    private MockHttpSession session(String email) throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user(email).password("Salohi@123"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
