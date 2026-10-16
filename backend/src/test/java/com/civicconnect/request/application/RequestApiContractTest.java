package com.civicconnect.request.application;

import com.civicconnect.request.domain.RequestCategory;
import com.civicconnect.request.web.RequestController;
import com.civicconnect.shared.ApiExceptionHandler;
import com.civicconnect.shared.NotFoundException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controller/API contract tests; full role/DB integration is tested by scripts/smoke.py. */
class RequestApiContractTest {
  private final RequestService service = mock(RequestService.class);
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RequestController(service))
      .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
      .setControllerAdvice(new ApiExceptionHandler()).build();
  @Test void invalidPayloadReturns400() throws Exception {
    mvc.perform(post("/api/requests").principal(() -> "requester")
        .contentType("application/json").content("{\"title\":\"\",\"description\":\"Valid\",\"category\":\"OTHER\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
  @Test void missingCategoryReturns400() throws Exception {
    mvc.perform(post("/api/requests").principal(() -> "requester")
        .contentType("application/json").content("{\"title\":\"Valid\",\"description\":\"Valid\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
  @Test void historyForNonExistentIdReturns404() throws Exception {
    java.util.UUID id=java.util.UUID.randomUUID();
    when(service.requesterHistory(id,"requester")).thenThrow(new NotFoundException("not found"));
    mvc.perform(get("/api/requests/"+id+"/history").principal(() -> "requester"))
      .andExpect(status().isNotFound());
  }
}
