package com.college.bookmyslot.auth;

import com.college.bookmyslot.config.WebConfig;
import com.college.bookmyslot.controller.AuthController;
import com.college.bookmyslot.controller.SlotController;
import com.college.bookmyslot.dto.BookSlotRequest;
import com.college.bookmyslot.dto.CreateSlotRequest;
import com.college.bookmyslot.dto.LoginRequest;
import com.college.bookmyslot.interceptor.SessionAuthInterceptor;
import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.SlotBookingRepository;
import com.college.bookmyslot.repository.TeacherSlotRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.EmailService;
import com.college.bookmyslot.service.SlotBookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.catalina.Context;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CorsFilter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionAuthAndCorsTest {

    private static final String PROD_ORIGIN = "https://bookmyslot-frontend-drab.vercel.app";
    private static final String UNAUTHORIZED_ORIGIN = "https://unauthorized-site.com";

    private MockMvc mockMvc;
    private UserRepository userRepository;
    private ClubRepository clubRepository;
    private EmailService emailService;
    private TeacherSlotRepository slotRepository;
    private SlotBookingRepository bookingRepository;
    private SlotBookingService slotBookingService;
    private SessionAuthInterceptor authInterceptor;
    private WebConfig webConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        clubRepository = Mockito.mock(ClubRepository.class);
        emailService = Mockito.mock(EmailService.class);
        slotRepository = Mockito.mock(TeacherSlotRepository.class);
        bookingRepository = Mockito.mock(SlotBookingRepository.class);
        slotBookingService = Mockito.mock(SlotBookingService.class);

        authInterceptor = new SessionAuthInterceptor();
        webConfig = new WebConfig(authInterceptor);
        ReflectionTestUtils.setField(webConfig, "allowedOrigins", PROD_ORIGIN + ",http://localhost:5173,http://localhost:3000");
        ReflectionTestUtils.setField(webConfig, "sameSite", "none");

        AuthController authController = new AuthController(userRepository, clubRepository, emailService);
        SlotController slotController = new SlotController(slotRepository, bookingRepository, userRepository, slotBookingService);

        CorsFilter corsFilter = webConfig.corsFilterRegistrationBean().getFilter();

        mockMvc = MockMvcBuilders.standaloneSetup(authController, slotController)
                .addMappedInterceptors(new String[]{"/api/**"}, authInterceptor)
                .addFilters(corsFilter)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("1. Successful login creates an authenticated session with USER_ID and ROLE")
    void testSuccessfulLoginCreatesSession() throws Exception {
        User user = new User();
        user.setId(60001L);
        user.setEmail("student@example.com");
        user.setPassword("password123");
        user.setName("Test Student");
        user.setRole(User.Role.STUDENT);
        user.setVerified(true);
        user.setUsn("1MS21CS001");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("student@example.com");
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(60001L))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("USER_ID")).isEqualTo(60001L);
        assertThat(session.getAttribute("ROLE")).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("2. Requests without a valid session receive 401 Unauthorized with Login required message")
    void testUnauthenticatedRequestReceives401() throws Exception {
        mockMvc.perform(get("/api/slots/available")
                        .param("date", "2026-10-20"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Login required"));
    }

    @Test
    @DisplayName("3. Authenticated session requests can access protected endpoints")
    void testAuthenticatedSessionCanAccessProtectedEndpoint() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 60001L);
        session.setAttribute("ROLE", "STUDENT");

        when(slotRepository.findByDateAndStatus(LocalDate.parse("2026-10-20"), TeacherSlot.Status.AVAILABLE))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/slots/available")
                        .param("date", "2026-10-20")
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. CORS permits the configured production origin with credentials")
    void testCorsPermitsProductionOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/api/slots/available")
                        .header("Origin", PROD_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PROD_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("5. CORS does not permit unauthorized origins")
    void testCorsRejectsUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/slots/available")
                        .header("Origin", UNAUTHORIZED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("6. Logout invalidates the authenticated session")
    void testLogoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 60001L);
        session.setAttribute("ROLE", "STUDENT");

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("7. Existing role-based access controls remain enforced")
    void testRoleBasedAccessControlEnforced() {
        User nonTeacher = new User();
        nonTeacher.setId(60001L);
        nonTeacher.setRole(User.Role.STUDENT);

        when(userRepository.findById(60001L)).thenReturn(Optional.of(nonTeacher));

        CreateSlotRequest request = new CreateSlotRequest();
        request.setTeacherId(60001L);
        request.setDate("2026-10-20");
        request.setStartTime("10:00");
        request.setEndTime("11:00");

        SlotController controller = new SlotController(slotRepository, bookingRepository, userRepository, slotBookingService);

        assertThatThrownBy(() -> controller.createSlot(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User is not a teacher");
    }

    @Test
    @DisplayName("8. Tomcat cookie processor configures SameSite=None for cross-site cookie support")
    void testTomcatCookieProcessorCustomizer() {
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer = webConfig.cookieProcessorCustomizer();
        assertThat(customizer).isNotNull();

        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        customizer.customize(factory);

        Context tomcatContext = Mockito.mock(Context.class);
        factory.getTomcatContextCustomizers().forEach(c -> c.customize(tomcatContext));

        Mockito.verify(tomcatContext).setCookieProcessor(Mockito.argThat(cp -> {
            if (cp instanceof Rfc6265CookieProcessor rfc) {
                return rfc.getSameSiteCookies() != null &&
                        "none".equalsIgnoreCase(rfc.getSameSiteCookies().getValue());
            }
            return false;
        }));
    }
}
