package tn.esprit.backend_esprittech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend_esprittech.config.CustomUserDetailsService;
import tn.esprit.backend_esprittech.config.JwtTokenProvider;
import tn.esprit.backend_esprittech.dto.request.CreateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamResponse;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamUserResponse;
import tn.esprit.backend_esprittech.service.ResearchTeamService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ResearchTeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResearchTeamControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ResearchTeamService researchTeamService;
        @MockBean
        private JwtTokenProvider jwtTokenProvider;
        @MockBean
        private CustomUserDetailsService customUserDetailsService;

        @Test
        void createTeam_returns201() throws Exception {
                CreateResearchTeamRequest request = new CreateResearchTeamRequest("AI", "Intelligence artificielle",
                                "Research team", null, List.of());
                ResearchTeamResponse response = new ResearchTeamResponse(
                                10L,
                                "AI",
                                "Intelligence artificielle",
                                "Research team",
                                new ResearchTeamUserResponse(1L, "Leader One"),
                                List.of());

                when(researchTeamService.create(request)).thenReturn(response);

                mockMvc.perform(post("/api/equipes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(10L))
                                .andExpect(jsonPath("$.chef.id").value(1L));
        }

        @Test
        void getTeamById_returns200() throws Exception {
                ResearchTeamResponse response = new ResearchTeamResponse(
                                10L,
                                "AI",
                                "Intelligence artificielle",
                                "Research team",
                                new ResearchTeamUserResponse(1L, "Leader One"),
                                List.of());

                when(researchTeamService.findById(10L)).thenReturn(response);

                mockMvc.perform(get("/api/equipes/10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(10L));
        }

        @Test
        void getAllTeams_returns200() throws Exception {
                ResearchTeamResponse response = new ResearchTeamResponse(
                                10L,
                                "AI",
                                "Intelligence artificielle",
                                "Research team",
                                new ResearchTeamUserResponse(1L, "Leader One"),
                                List.of());

                when(researchTeamService.findAll()).thenReturn(List.of(response));

                mockMvc.perform(get("/api/equipes"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(10L));
        }

        @Test
        void update_shouldReturnOk() throws Exception {

                Long id = 1L;

                UpdateResearchTeamRequest request = new UpdateResearchTeamRequest("AI", "DS", "desc", null, null);

                ResearchTeamResponse response = mock(ResearchTeamResponse.class);

                when(researchTeamService.update(eq(id), any(UpdateResearchTeamRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(put("/api/equipes/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        void delete_shouldReturnNoContent() throws Exception {

                Long id = 1L;

                doNothing().when(researchTeamService).delete(id);

                mockMvc.perform(delete("/api/equipes/{id}", id))
                                .andExpect(status().isNoContent());
        }
}
