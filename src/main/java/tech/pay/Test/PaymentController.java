package tech.pay.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Value("${external.api.url}")
    private String apiUrl;

    @Value("${external.api.bearer.token}")
    private String bearerToken;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/submit")
    public String submitAmount(@RequestParam double amount, Model model) {
        RestTemplate restTemplate = new RestTemplate();

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        payload.put("currency", "EUR");
        payload.put("paymentType", "DEPOSIT");

        Map<String, String> customer = new HashMap<>();
        customer.put("referenceId", "This is a test customer");
        payload.put("customer", customer);
        //The above is needed because otherwise the backend API will fail
        /*
            {
      "defaultMessage": "must not be null.",
      "objectName": "PaymentRequest",
      "field": "customer",
      "bindingFailure": false
    }
         */


        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to process payment. Please try again later.");
            return "error";
        }

        // Create entity with headers and payload
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            // Parse the JSON response to extract the redirectUrl field from the result object
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> resultMap = (Map<String, Object>) responseMap.get("result");
            String redirectUrl = (String) resultMap.get("redirectUrl");

            model.addAttribute("redirectUrl", redirectUrl);
            return "redirect";
        } catch (RestClientException e) {
            model.addAttribute("error", "Failed to process payment. Please try again later.");
            return "error";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}