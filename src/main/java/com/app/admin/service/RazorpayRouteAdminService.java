package com.app.admin.service;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.dto.RazorpayRouteLinkedAccountTestRequest;
import com.app.admin.security.AdminSecurityContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class RazorpayRouteAdminService {
    private final String keyId;
    private final String keySecret;
    private final String environment;
    private final boolean testAccountCreationEnabled;
    private final boolean routeEnabled;
    private final JdbcTemplate db;
    private final AdminSecurityContext security;
    private final RestClient client;
    private final RestClient paymentClient;
    private final RestClient routeV2Client;
    private final ObjectMapper objectMapper;
    private volatile Map<String, Object> lastConnection;
    private volatile Map<String, Object> lastAccess;

    @Autowired
    public RazorpayRouteAdminService(@Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret,
            @Value("${razorpay.environment:TEST}") String environment,
            @Value("${training.settlement.route-enabled:false}") boolean routeEnabled,
            @Value("${razorpay.route.test-account-creation-enabled:false}") boolean testAccountCreationEnabled,
            JdbcTemplate db, AdminSecurityContext security, ObjectMapper objectMapper) {
        this(keyId,keySecret,environment,routeEnabled,testAccountCreationEnabled,db,security,objectMapper,
                createClient("https://api.razorpay.com/v1"),createClient("https://api.razorpay.com/v2"));
    }

    RazorpayRouteAdminService(String keyId,String keySecret,String environment,boolean routeEnabled,boolean testAccountCreationEnabled,
            JdbcTemplate db,AdminSecurityContext security,ObjectMapper objectMapper,RestClient paymentClient,RestClient routeV2Client) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.environment = environment;
        this.routeEnabled = routeEnabled;
        this.testAccountCreationEnabled = testAccountCreationEnabled;
        this.db = db;
        this.security = security;
        this.paymentClient = paymentClient;
        this.routeV2Client = routeV2Client;
        this.objectMapper = objectMapper;
        this.client = routeV2Client;
    }

    private static RestClient createClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public Map<String, Object> health() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("environment", effectiveEnvironment());
        result.put("routeEnabled", routeEnabled && "TEST".equals(effectiveEnvironment()));
        result.put("credentialsConfigured", !keyId.isBlank() && !keySecret.isBlank());
        result.put("authentication", lastConnection == null ? unknown("Authentication has not been checked.") : Map.of(
                "status", lastConnection.getOrDefault("status", "UNKNOWN"),
                "checkedAt", lastConnection.getOrDefault("checkedAt", ""),
                "message", lastConnection.getOrDefault("message", "Connection has not been checked.")));
        result.put("connection", lastConnection == null ? unknown("Connection has not been checked.") : lastConnection);
        result.put("routeAccess", lastAccess == null ? unknown("Route access has not been checked.") : lastAccess);
        result.put("routeActivation", Map.of("status", "UNKNOWN", "message", "No current linked-account activation evidence was retrieved. Check a mapped account; activation is account-specific."));
        result.put("linkedAccounts", linkedAccountHealth());
        result.put("transferCapability", transferCapabilityHealth());
        result.put("paymentSource", unknown("Select a local payment or run payment verification."));
        result.put("webhook", webhookHealth());
        result.put("reconciliation", unknown("Recipient settlement evidence is not yet persisted."));
        result.put("productionReadiness", Map.of("status", "BLOCKED", "message", "Live transfers are disabled in this readiness center."));
        result.put("checkedAt", Instant.now().toString());
        return result;
    }

    private Map<String,Object> linkedAccountHealth() {
        try {
            Map<String,Object> summary=db.queryForMap("SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE provider_environment='TEST') AS test_count, COUNT(*) FILTER (WHERE account_status='CREATED') AS created_count, COUNT(*) FILTER (WHERE account_status='ACTIVATED') AS activated_count FROM public.razorpay_route_linked_accounts");
            long total=((Number)summary.get("total")).longValue();
            return Map.of("status",total>0?"VERIFIED":"NOT_TESTED","message",total>0?"Separate environment-scoped Route linked-account mappings are stored.":"No administrator-created Route linked accounts have been recorded.","counts",summary,"checkedAt",Instant.now().toString());
        } catch(RuntimeException e) { return unknown("Route linked-account mapping diagnostics are unavailable. Apply razorpay-route-readiness.sql."); }
    }

    private Map<String,Object> transferCapabilityHealth() {
        try {
            Map<String,Object> latest=db.query("SELECT id,provider_transfer_id,provider_http_status,attempt_status,response_timestamp FROM public.razorpay_route_test_transfer_attempts ORDER BY id DESC LIMIT 1",rs->rs.next()?Map.of("id",rs.getLong("id"),"transferId",rs.getString("provider_transfer_id") == null ? "" : rs.getString("provider_transfer_id"),"httpStatus",rs.getObject("provider_http_status") == null ? 0 : rs.getInt("provider_http_status"),"status",rs.getString("attempt_status"),"checkedAt",rs.getTimestamp("response_timestamp") == null ? "" : rs.getTimestamp("response_timestamp").toInstant().toString()):null);
            if(latest==null)return unknown("No confirmed Test Mode Route transfer has been executed.");
            return Map.of("status","SUCCESS".equals(latest.get("status"))?"VERIFIED":String.valueOf(latest.get("status")),"message","Most recent explicitly submitted Test Mode transfer attempt.","attempt",latest);
        } catch(RuntimeException e) { return unknown("No transfer attempt history is available. Apply razorpay-route-readiness.sql."); }
    }

    public Map<String, Object> checkConnection() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        Map<String, Object> result = checkReadOnly(paymentClient, "/payments?count=1", "Razorpay payment read API");
        result.put("method", "GET");
        result.put("endpoint", "/v1/payments?count=1");
        if ("SUCCESS".equals(result.get("status"))) result.put("status", "VERIFIED");
        lastConnection = result;
        return result;
    }

    public Map<String, Object> checkAccess() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        Map<String, Object> result = checkReadOnly(paymentClient, "/payments?count=1", "Razorpay Payment API credential check");
        result.put("method", "GET");
        result.put("endpoint", "/v1/payments?count=1");
        result.put("routeStatus", "NOT TESTED");
        result.put("routeMessage", "The documented Route API does not provide an account-wide linked-account list endpoint for this diagnostic. Use a specific linked-account ID with GET /v2/accounts/{account_id} once a mapping exists.");
        if ("SUCCESS".equals(result.get("status"))) {
            result.put("paymentApiStatus", "ACCESSIBLE");
            result.put("activation", "NOT_CONFIRMED");
            result.put("status", "NOT TESTED");
            result.put("routeDiagnosticStatus", "NOT TESTED");
            result.put("message", "Payment API credentials are valid. Route API access and activation have not been established by this request.");
        }
        lastAccess = result;
        return result;
    }

    public Map<String, Object> checkLinkedAccount(String accountId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        if (accountId == null || !accountId.matches("acc_[A-Za-z0-9]+")) {
            return Map.of("status", "NOT_TESTED", "message", "A valid mapped Route account ID (acc_...) is required.");
        }
        Instant checkedAt=Instant.now();
        if (keyId.isBlank() || keySecret.isBlank()) {
            return diagnostic("GET", "/v2/accounts/{account_id}", "UNKNOWN", null, "CREDENTIALS_NOT_CONFIGURED", "Razorpay API credentials are not configured on the backend.", checkedAt);
        }
        try {
            Map<String,Object> provider=routeV2Client.get().uri("/accounts/{id}",accountId).headers(h->h.setBasicAuth(keyId,keySecret)).retrieve().body(new ParameterizedTypeReference<>(){});
            if(provider==null)return diagnostic("GET","/v2/accounts/{account_id}","UNKNOWN",200,"EMPTY_RESPONSE","Razorpay returned an empty linked-account response.",checkedAt);
            Map<String,Object> result=diagnostic("GET","/v2/accounts/{account_id}","VERIFIED",200,null,"Razorpay returned the linked account. This verifies access to this account only; activation is not confirmed.",checkedAt);
            Map<String,Object> safe=new LinkedHashMap<>();
            for(String key:List.of("id","type","status","reference_id","created_at"))if(provider.containsKey(key))safe.put(key,provider.get(key));
            Map<?,?> productConfig=provider.get("product_config") instanceof Map<?,?> p?p:Map.of();
            Object activationValue=productConfig.get("activation_status");
            String activationStatus=String.valueOf(activationValue==null?"UNKNOWN":activationValue).toUpperCase(java.util.Locale.ROOT);
            Map<?,?> activeConfiguration=productConfig.get("active_configuration") instanceof Map<?,?> c?c:Map.of();
            String settlementStatus="UNKNOWN";
            if(activeConfiguration.get("settlement_accounts") instanceof List<?> accounts&&!accounts.isEmpty()&&accounts.get(0) instanceof Map<?,?> settlementAccount) {
                Object verificationValue=settlementAccount.get("verification_status");
                settlementStatus=String.valueOf(verificationValue==null?"UNKNOWN":verificationValue).toUpperCase(java.util.Locale.ROOT);
            }
            result.put("account",safe);result.put("activationStatus",activationStatus);result.put("settlementVerificationStatus",settlementStatus);
            db.update("UPDATE public.razorpay_route_linked_accounts SET account_status=?,activation_status=?,settlement_verification_status=?,last_synced_at=CURRENT_TIMESTAMP,last_error_code=NULL,updated_at=CURRENT_TIMESTAMP WHERE provider_environment=? AND route_account_id=?",
                    String.valueOf(provider.getOrDefault("status","UNKNOWN")).toUpperCase(java.util.Locale.ROOT),activationStatus,settlementStatus,effectiveEnvironment(),accountId);
            return result;
        } catch(RestClientResponseException e) {
            Map<String,Object> failure=providerFailure("GET","/v2/accounts/{account_id}",e,checkedAt,"Razorpay rejected this linked-account lookup.");
            db.update("UPDATE public.razorpay_route_linked_accounts SET last_error_code=?,last_synced_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE provider_environment=? AND route_account_id=?",
                    failure.get("providerErrorCode"),effectiveEnvironment(),accountId);
            return failure;
        }
        catch(RuntimeException e) { return diagnostic("GET","/v2/accounts/{account_id}","UNKNOWN",null,"PROVIDER_CONNECTION_UNKNOWN","Razorpay linked-account lookup did not complete.",checkedAt); }
    }

    public List<Map<String, Object>> linkedAccounts() {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        return db.queryForList("""
                SELECT id,trainer_user_id,payout_account_id,provider,provider_environment,route_account_id,
                       account_status,activation_status,settlement_verification_status,last_synced_at,last_error_code,created_at,updated_at
                  FROM public.razorpay_route_linked_accounts ORDER BY updated_at DESC,id DESC
                """);
    }

    public Map<String, Object> createTestLinkedAccount(RazorpayRouteLinkedAccountTestRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        Instant checkedAt = Instant.now();
        if (!"I CONFIRM CREATE TEST LINKED ACCOUNT".equals(request.confirmation())) {
            return diagnostic("POST", "/v2/accounts", "FAILED", 400, "ADMIN_CONFIRMATION_REQUIRED", "Type the required confirmation phrase to create a test linked account.", checkedAt);
        }
        if (!"TEST".equals(effectiveEnvironment()) || !keyId.startsWith("rzp_test_")) {
            return diagnostic("POST", "/v2/accounts", "RESTRICTED", null, "TEST_CREDENTIALS_REQUIRED", "Linked account creation is allowed only with Razorpay Test Mode credentials.", checkedAt);
        }
        if (!testAccountCreationEnabled) {
            return diagnostic("POST", "/v2/accounts", "RESTRICTED", null, "TEST_CREATION_DISABLED", "Enable RAZORPAY_ROUTE_TEST_ACCOUNT_CREATION_ENABLED after reviewing the test account data and confirmation flow.", checkedAt);
        }
        Map<String,Object> payout = db.query("""
                SELECT id,trainer_user_id,provider,account_status FROM public.trainer_payout_accounts WHERE id=?
                """, rs -> rs.next() ? Map.of("id",rs.getLong("id"),"trainerUserId",rs.getLong("trainer_user_id"),"provider",rs.getString("provider"),"status",rs.getString("account_status")) : null, request.payoutAccountId());
        if (payout == null || !request.trainerUserId().equals(payout.get("trainerUserId")) || !"RAZORPAY".equalsIgnoreCase(String.valueOf(payout.get("provider")))) {
            return diagnostic("POST", "/v2/accounts", "FAILED", 400, "PAYOUT_ACCOUNT_MISMATCH", "Select an existing Razorpay payout account belonging to the selected trainer.", checkedAt);
        }
        List<Map<String,Object>> existing = db.queryForList("SELECT id,route_account_id,account_status,activation_status FROM public.razorpay_route_linked_accounts WHERE provider_environment='TEST' AND trainer_user_id=? AND payout_account_id=?", request.trainerUserId(), request.payoutAccountId());
        if (!existing.isEmpty()) return diagnostic("POST", "/v2/accounts", "FAILED", 409, "TEST_MAPPING_EXISTS", "A Test Mode linked account is already mapped to this trainer and payout account. Refresh its status instead of creating another account.", checkedAt);

        String referenceId = "zeto9-trainer-" + request.trainerUserId() + "-payout-" + request.payoutAccountId() + "-" + java.util.UUID.randomUUID();
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("email",request.email()); body.put("phone",request.phone()); body.put("type","route"); body.put("reference_id",referenceId); body.put("tnc_accepted",true);
        body.put("legal_business_name",request.legalBusinessName()); body.put("business_type",request.businessType()); body.put("contact_name",request.contactName());
        body.put("settlement_accounts",List.of(Map.of("method","bank_account","bank_account",Map.of("account_number",request.bankAccountNumber(),"beneficiary_name",request.bankAccountName(),"code_type","ifsc","code",request.ifsc().toUpperCase(java.util.Locale.ROOT),"currency","INR","is_default",true))));
        body.put("notes",Map.of("internal_trainer_id",String.valueOf(request.trainerUserId()),"internal_payout_account_id",String.valueOf(request.payoutAccountId()),"environment","TEST"));
        Long onboardingAttempt;
        try { onboardingAttempt=db.queryForObject("INSERT INTO public.razorpay_route_onboarding_attempts(provider_environment,trainer_user_id,payout_account_id,reference_id,attempt_status,initiated_by_admin_id) VALUES ('TEST',?,?,?,'PROCESSING',?) RETURNING id",Long.class,request.trainerUserId(),request.payoutAccountId(),referenceId,security.getCurrentUserId()); }
        catch(RuntimeException duplicate) { return diagnostic("POST","/v2/accounts","RESTRICTED",409,"ACTIVE_ONBOARDING_ATTEMPT","A linked-account onboarding attempt already exists or has an uncertain outcome. Reconcile before creating another account.",checkedAt); }
        try {
            Map<String,Object> provider = routeV2Client.post().uri("/accounts").headers(h->{h.setBasicAuth(keyId,keySecret);h.set("Idempotency-Key",referenceId);}).body(body).retrieve().body(new ParameterizedTypeReference<>(){});
            if (provider == null || !(provider.get("id") instanceof String routeId) || !routeId.matches("acc_[A-Za-z0-9]+")) {
                db.update("UPDATE public.razorpay_route_onboarding_attempts SET attempt_status='UNKNOWN',provider_http_status=200,provider_error_code='INVALID_PROVIDER_RESPONSE',updated_at=CURRENT_TIMESTAMP WHERE id=?",onboardingAttempt);
                return diagnostic("POST", "/v2/accounts", "UNKNOWN", 200, "INVALID_PROVIDER_RESPONSE", "Razorpay responded without a valid Route account ID. Review the provider Dashboard before retrying.", checkedAt);
            }
            String accountStatus = String.valueOf(provider.getOrDefault("status","UNKNOWN")).toUpperCase(java.util.Locale.ROOT);
            Map<?,?> productConfig=provider.get("product_config") instanceof Map<?,?> p?p:Map.of();
            String activationStatus=String.valueOf(productConfig.containsKey("activation_status")?productConfig.get("activation_status"):"UNKNOWN").toUpperCase(java.util.Locale.ROOT);
            String bankVerificationStatus="UNKNOWN";
            Object configuration=productConfig.get("active_configuration");
            if(configuration instanceof Map<?,?> config && config.get("settlement_accounts") instanceof List<?> accounts && !accounts.isEmpty() && accounts.get(0) instanceof Map<?,?> settlementAccount) bankVerificationStatus=String.valueOf(settlementAccount.containsKey("verification_status")?settlementAccount.get("verification_status"):"UNKNOWN").toUpperCase(java.util.Locale.ROOT);
            Long adminId = security.getCurrentUserId();
            db.update("""
                    INSERT INTO public.razorpay_route_linked_accounts(trainer_user_id,payout_account_id,provider,provider_environment,route_account_id,account_status,activation_status,settlement_verification_status,last_synced_at,created_by_admin_id)
                    VALUES (?,?,'RAZORPAY','TEST',?,?,?,?,CURRENT_TIMESTAMP,?)
                    """, request.trainerUserId(),request.payoutAccountId(),routeId,accountStatus,activationStatus,bankVerificationStatus,adminId);
            db.update("UPDATE public.razorpay_route_onboarding_attempts SET attempt_status='SUCCESS',route_account_id=?,provider_http_status=200,updated_at=CURRENT_TIMESTAMP WHERE id=?",routeId,onboardingAttempt);
            Map<String,Object> result = diagnostic("POST", "/v2/accounts", "VERIFIED", 200, null, "Test Route linked account and settlement destination were created. Provider account status is " + accountStatus + ".", checkedAt);
            result.put("linkedAccountId",routeId); result.put("providerAccountStatus",accountStatus); result.put("activationStatus",activationStatus); result.put("settlementVerificationStatus",bankVerificationStatus);
            return result;
        } catch (RestClientResponseException e) {
            String attemptStatus=e.getStatusCode().is4xxClientError()?"FAILED":"UNKNOWN";
            db.update("UPDATE public.razorpay_route_onboarding_attempts SET attempt_status=?,provider_http_status=?,provider_error_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",attemptStatus,e.getStatusCode().value(),sanitizedProviderCode(e.getResponseBodyAsString()),onboardingAttempt);
            return linkedAccountProviderFailure(e,request,checkedAt);
        } catch (ResourceAccessException e) {
            db.update("UPDATE public.razorpay_route_onboarding_attempts SET attempt_status='UNKNOWN',provider_error_code='PROVIDER_TIMEOUT_OR_CONNECTION',updated_at=CURRENT_TIMESTAMP WHERE id=?",onboardingAttempt);
            return diagnostic("POST", "/v2/accounts", "UNKNOWN", null, "PROVIDER_TIMEOUT_OR_CONNECTION", "The create request outcome is unknown. Check the Razorpay Dashboard for reference " + referenceId + " before attempting another create.", checkedAt);
        } catch (RuntimeException e) {
            db.update("UPDATE public.razorpay_route_onboarding_attempts SET attempt_status='UNKNOWN',provider_error_code='LOCAL_PERSISTENCE_FAILURE',updated_at=CURRENT_TIMESTAMP WHERE id=?",onboardingAttempt);
            return diagnostic("POST", "/v2/accounts", "UNKNOWN", null, "LOCAL_PERSISTENCE_FAILURE", "Razorpay may have created account " + referenceId + ", but local mapping persistence failed. Reconcile the provider account before another create.", checkedAt);
        }
    }

    public Map<String,Object> createTestTransfer(Map<String,String> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_SETTLEMENT_PROCESS);
        Instant started=Instant.now();
        if (request==null || !"I CONFIRM TEST TRANSFER".equals(request.get("confirmation"))) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",400,"ADMIN_CONFIRMATION_REQUIRED","Type the required phrase to submit a test transfer.",started);
        if (!"TEST".equals(effectiveEnvironment()) || !keyId.startsWith("rzp_test_") || !routeEnabled) return diagnostic("POST","/v1/payments/{payment_id}/transfers","RESTRICTED",null,"TEST_MODE_REQUIRED","Test transfer requires Test Mode credentials and RAZORPAY_ROUTE_ENABLED=true.",started);
        String paymentId=request.get("paymentId"), accountId=request.get("linkedAccountId");
        if(paymentId==null||!paymentId.matches("pay_[A-Za-z0-9]+")||accountId==null||!accountId.matches("acc_[A-Za-z0-9]+")) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",400,"INVALID_ID","Provide a real pay_ test payment and mapped acc_ linked account.",started);
        Map<String,Object> mapping=db.query("SELECT id,route_account_id FROM public.razorpay_route_linked_accounts WHERE provider_environment='TEST' AND route_account_id=?",rs->rs.next()?Map.of("id",rs.getLong("id"),"accountId",rs.getString("route_account_id")):null,accountId);
        if(mapping==null) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",400,"TEST_LINKED_ACCOUNT_NOT_MAPPED","The linked account must be a locally mapped Test Mode account.",started);
        Map<String,Object> payment;
        try { payment=paymentClient.get().uri("/payments/{id}",paymentId).headers(h->h.setBasicAuth(keyId,keySecret)).retrieve().body(new ParameterizedTypeReference<>(){}); }
        catch(RestClientResponseException e){return providerFailure("GET","/v1/payments/{payment_id}",e,started,"Razorpay could not verify the payment.");}
        catch(RuntimeException e){return diagnostic("GET","/v1/payments/{payment_id}","UNKNOWN",null,"PAYMENT_LOOKUP_UNKNOWN","Payment lookup did not complete; no transfer was submitted.",started);}
        if(payment==null||!Boolean.TRUE.equals(payment.get("captured"))||!"captured".equalsIgnoreCase(String.valueOf(payment.get("status")))) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",409,"PAYMENT_NOT_CAPTURED","Only a captured Test Mode payment can be selected.",started);
        if(!"INR".equalsIgnoreCase(String.valueOf(payment.get("currency")))) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",409,"UNSUPPORTED_CURRENCY","Route transfer test requires a captured INR payment.",started);
        long amount; try{amount=Long.parseLong(request.get("amountPaise"));}catch(Exception e){return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",400,"INVALID_AMOUNT","Enter a positive transfer amount in paise.",started);}
        long captured=((Number)payment.get("amount")).longValue(); long refunded=payment.get("amount_refunded") instanceof Number n?n.longValue():0;
        if(amount<=0||amount>captured-refunded) return diagnostic("POST","/v1/payments/{payment_id}/transfers","FAILED",400,"AMOUNT_EXCEEDS_PAYMENT","Amount must be positive and no more than provider-reported captured amount less refunds.",started);
        Object known=transferList(paymentId);
        if(!(known instanceof List<?> transfers)) return diagnostic("GET","/v1/payments/{payment_id}/transfers","UNKNOWN",null,"TRANSFER_LOOKUP_UNKNOWN","Existing transfer status could not be verified. No transfer was submitted.",started);
        if(!transfers.isEmpty()) return diagnostic("POST","/v1/payments/{payment_id}/transfers","RESTRICTED",409,"EXISTING_TRANSFER","Payment already has transfer records. This test will not submit another transfer.",started);
        Long attemptId;
        try { attemptId=db.queryForObject("INSERT INTO public.razorpay_route_test_transfer_attempts(provider_environment,payment_provider_payment_id,route_linked_account_id,amount_paise,attempt_status,initiated_by_admin_id) VALUES ('TEST',?,?,?,'PROCESSING',?) RETURNING id",Long.class,paymentId,mapping.get("id"),amount,security.getCurrentUserId()); }
        catch(RuntimeException duplicate){return diagnostic("POST","/v1/payments/{payment_id}/transfers","RESTRICTED",409,"DUPLICATE_OR_ACTIVE_ATTEMPT","An active, successful, or uncertain transfer attempt already exists for this payment.",started);}
        Map<String,Object> transfer=Map.of("account",accountId,"amount",amount,"currency","INR","on_hold",false,"notes",Map.of("purpose","route_integration_test","test_attempt_id",String.valueOf(attemptId)));
        try {
            Map<String,Object> response=paymentClient.post().uri("/payments/{id}/transfers",paymentId).headers(h->h.setBasicAuth(keyId,keySecret)).body(Map.of("transfers",List.of(transfer))).retrieve().body(new ParameterizedTypeReference<>(){});
            Object items=response==null?null:response.get("items"); Map<?,?> item=items instanceof List<?> list&&!list.isEmpty()&&list.get(0) instanceof Map<?,?> value?value:response;
            String transferId=item==null?null:String.valueOf(item.get("id"));
            if(transferId==null||!transferId.startsWith("trf_")){updateAttempt(attemptId,"UNKNOWN",null,null,"UNKNOWN");return diagnostic("POST","/v1/payments/{payment_id}/transfers","UNKNOWN",200,"MISSING_TRANSFER_ID","Razorpay response did not provide a transfer ID. Reconcile before retrying.",started);}
            updateAttempt(attemptId,"SUCCESS",transferId,200,"UNKNOWN"); Map<String,Object> result=diagnostic("POST","/v1/payments/{payment_id}/transfers","VERIFIED",200,null,"Test transfer was accepted by Razorpay. Recipient bank settlement is not confirmed by transfer creation.",started);result.put("attemptId",attemptId);result.put("transferId",transferId);result.put("paymentId",paymentId);result.put("linkedAccountId",accountId);result.put("amountPaise",amount);return result;
        } catch(RestClientResponseException e){updateAttempt(attemptId,"FAILED",null,e.getStatusCode().value(),sanitizedProviderCode(e.getResponseBodyAsString()));return providerFailure("POST","/v1/payments/{payment_id}/transfers",e,started,"Razorpay rejected the test transfer.");}
        catch(RuntimeException e){updateAttempt(attemptId,"UNKNOWN",null,null,"UNKNOWN");return diagnostic("POST","/v1/payments/{payment_id}/transfers","UNKNOWN",null,"TRANSFER_OUTCOME_UNKNOWN","The provider outcome is unknown. This attempt is locked against retry until manually reconciled.",started);}
    }

    private void updateAttempt(Long id,String status,String transferId,Integer httpStatus,String errorCode){db.update("UPDATE public.razorpay_route_test_transfer_attempts SET attempt_status=?,provider_transfer_id=?,provider_http_status=?,provider_error_code=?,response_timestamp=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,transferId,httpStatus,errorCode,id);}

    private Map<String,Object> providerFailure(String method,String endpoint,RestClientResponseException e,Instant checkedAt,String explanation){
        int code=e.getStatusCode().value();String providerCode=sanitizedProviderCode(e.getResponseBodyAsString());
        String status=code==401?"FAILED":code==403?"RESTRICTED":code==400?"FAILED":code==404?"FAILED":code>=500?"UNKNOWN":"FAILED";
        return diagnostic(method,endpoint,status,code,providerCode,explanation+" Provider response: "+statusDescription(code,providerCode),checkedAt);
    }

    private Map<String,Object> linkedAccountProviderFailure(RestClientResponseException exception,
            RazorpayRouteLinkedAccountTestRequest request, Instant checkedAt) {
        int httpStatus = exception.getStatusCode().value();
        ProviderError providerError = parseProviderError(exception.getResponseBodyAsString(), request);
        String status = httpStatus == 401 ? "FAILED" : httpStatus == 403 ? "RESTRICTED"
                : httpStatus >= 500 ? "UNKNOWN" : "FAILED";
        String message = providerError.description() == null || providerError.description().isBlank()
                ? "Razorpay rejected linked-account onboarding. The provider did not return a readable validation description."
                : providerError.description();
        if (providerError.field() != null) message += " (field: " + providerError.field() + ")";
        Map<String,Object> result = diagnostic("POST", "/v2/accounts", status, httpStatus,
                providerError.code() == null ? "UNKNOWN" : providerError.code(), message, checkedAt);
        result.put("providerErrorDescription", providerError.description());
        result.put("providerErrorField", providerError.field());
        result.put("providerErrorReason", providerError.reason());
        return result;
    }

    private ProviderError parseProviderError(String responseBody, RazorpayRouteLinkedAccountTestRequest request) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            return new ProviderError(safeProviderCode(text(error, "code")),
                    sanitizeProviderText(text(error, "description"), request),
                    safeProviderField(text(error, "field")), safeProviderCode(text(error, "reason")));
        } catch (RuntimeException ignored) {
            return new ProviderError(null, null, null, null);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String safeProviderCode(String value) {
        return value != null && value.matches("[A-Za-z0-9_.-]{1,100}") ? value : null;
    }

    private static String safeProviderField(String value) {
        return value != null && value.matches("[A-Za-z0-9_.\\[\\]-]{1,160}") ? value : null;
    }

    private static String sanitizeProviderText(String value, RazorpayRouteLinkedAccountTestRequest request) {
        if (value == null) return null;
        String safe = value;
        if (request != null) {
            for (String privateValue : List.of(request.email(), request.phone(), request.bankAccountNumber(),
                    request.contactName(), request.legalBusinessName(), request.bankAccountName(), request.ifsc())) {
                if (privateValue != null && !privateValue.isBlank()) safe = safe.replace(privateValue, "[REDACTED]");
            }
        }
        safe = safe.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[REDACTED_EMAIL]");
        safe = safe.replaceAll("(?<![A-Za-z0-9])\\+?[0-9][0-9 ()-]{7,}[0-9](?![A-Za-z0-9])", "[REDACTED_NUMBER]");
        return safe.length() > 500 ? safe.substring(0, 500) : safe;
    }

    private record ProviderError(String code, String description, String field, String reason) {}

    private String statusDescription(int status,String code){return switch(status){case 400->"invalid request or onboarding data";case 401->"authentication failed";case 403->"feature or account permission restricted";case 404->"requested provider resource not found";default->status>=500?"provider server failure":"HTTP "+status;};}

    private Map<String,Object> diagnostic(String method,String endpoint,String status,Integer httpStatus,String errorCode,String explanation,Instant timestamp){Map<String,Object> result=new LinkedHashMap<>();result.put("method",method);result.put("endpoint",endpoint);result.put("status",status);result.put("httpStatus",httpStatus);result.put("providerErrorCode",errorCode);result.put("explanation",explanation);result.put("checkedAt",timestamp.toString());result.put("environment",effectiveEnvironment());return result;}

    private Map<String, Object> checkReadOnly(RestClient api, String path, String description) {
        return checkReadOnly(api, path, description, null);
    }

    private Map<String, Object> checkReadOnly(RestClient api, String path, String description, String pathVariable) {
        Instant started = Instant.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("environment", effectiveEnvironment());
        result.put("description", description);
        result.put("checkedAt", started.toString());
        if (keyId.isBlank() || keySecret.isBlank()) {
            result.put("status", "UNKNOWN");
            result.put("message", "Razorpay API credentials are not configured on the backend.");
            return result;
        }
        try {
            Map<String, Object> body = api.get().uri(path, pathVariable == null ? new Object[0] : new Object[]{pathVariable}).headers(h -> h.setBasicAuth(keyId, keySecret))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            result.put("status", "SUCCESS");
            result.put("httpStatus", 200);
            result.put("message", "Read-only API request succeeded.");
            if (body != null) {
                Object items = body.get("items");
                result.put("returnedCount", items instanceof List<?> list ? list.size() : body.getOrDefault("count", 0));
            }
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            result.put("status", status.value() == 401 ? "AUTHENTICATION_FAILED" : status.value() == 403 ? "RESTRICTED" : status.value() == 404 ? "NOT_FOUND" : "ERROR");
            result.put("httpStatus", status.value());
            result.put("errorCode", sanitizedProviderCode(e.getResponseBodyAsString()));
            result.put("message", status.value() == 401 ? "Razorpay rejected the API credentials. Check the configured key pair and environment." : status.value() == 403 ? "Credentials authenticated, but this API is not permitted for the account." : status.value() == 404 ? "The requested Razorpay resource does not exist or is not accessible to these credentials." : "Razorpay did not complete the read-only diagnostic request.");
        } catch (RuntimeException e) {
            result.put("status", "UNKNOWN");
            result.put("message", "Razorpay could not be reached or the response could not be interpreted.");
        }
        return result;
    }

    public Map<String, Object> verifyPayment(Map<String, String> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        String paymentId = request == null ? null : request.get("paymentId");
        if (paymentId == null || !paymentId.matches("pay_[A-Za-z0-9]+")) return Map.of("status", "NOT_TESTED", "message", "Enter a valid Razorpay payment ID (pay_...).");
        if (keyId.isBlank() || keySecret.isBlank()) return Map.of("status", "UNKNOWN", "message", "Razorpay API credentials are not configured.");
        try {
            Map<String, Object> payment = paymentClient.get()
                    .uri("/payments/{id}", paymentId).headers(h -> h.setBasicAuth(keyId, keySecret)).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (payment == null) return Map.of("status", "UNKNOWN", "message", "Razorpay returned an empty payment response.");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", payment.get("status")); result.put("paymentId", payment.get("id"));
            result.put("amountPaise", payment.get("amount")); result.put("currency", payment.get("currency"));
            result.put("captured", payment.get("captured")); result.put("environment", effectiveEnvironment());
            result.put("refundStatus", localRefundStatus(paymentId));
            result.put("existingTransfers", transferList(paymentId));
            result.put("transferability", "UNKNOWN");
            result.put("message", "Provider payment details retrieved. Transferable balance is not inferred from the captured amount.");
            result.put("checkedAt", Instant.now().toString());
            return result;
        } catch (RestClientResponseException e) {
            return Map.of("status", "ERROR", "httpStatus", e.getStatusCode().value(), "errorCode", sanitizedProviderCode(e.getResponseBodyAsString()), "message", "Razorpay could not retrieve this payment ID.", "checkedAt", Instant.now().toString());
        } catch (RuntimeException e) {
            return Map.of("status", "UNKNOWN", "message", "Razorpay could not be reached.", "checkedAt", Instant.now().toString());
        }
    }

    private String localRefundStatus(String providerPaymentId) {
        try {
            List<Map<String, Object>> rows = db.queryForList("""
                    SELECT COALESCE(STRING_AGG(DISTINCT r.status, ', '), 'NO_LOCAL_REFUNDS') AS statuses
                      FROM training_refunds r JOIN training_payments p ON p.id=r.payment_id
                     WHERE p.provider_payment_id=?
                    """, providerPaymentId);
            return rows.isEmpty() ? "UNKNOWN" : String.valueOf(rows.get(0).get("statuses"));
        } catch (RuntimeException e) { return "UNKNOWN"; }
    }

    private Object transferList(String paymentId) {
        try {
            Map<String, Object> response = paymentClient.get().uri("/payments/{id}/transfers", paymentId)
                    .headers(h -> h.setBasicAuth(keyId, keySecret)).retrieve().body(new ParameterizedTypeReference<>() {});
            Object items = response == null ? null : response.get("items");
            return items instanceof List<?> ? items : List.of();
        } catch (RuntimeException e) { return "UNKNOWN"; }
    }

    public Map<String, Object> readiness(Long settlementId) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_VIEW);
        List<Map<String, Object>> rows = db.queryForList("""
                SELECT s.id,s.status,s.amount,s.currency,s.trainer_user_id,s.provider_transfer_id,
                       (SELECT COUNT(*) FROM training_settlement_items i WHERE i.settlement_id=s.id) AS item_count,
                       (SELECT COUNT(DISTINCT p.id) FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id WHERE i.settlement_id=s.id) AS payment_count,
                       (SELECT COUNT(*) FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id WHERE i.settlement_id=s.id AND p.status IN ('CAPTURED','PARTIALLY_REFUNDED') AND p.provider_payment_id IS NOT NULL) AS captured_count,
                       (SELECT COUNT(*) FROM training_settlement_items i JOIN training_commissions c ON c.id=i.commission_id JOIN training_payments p ON p.id=c.payment_id JOIN training_refunds r ON r.payment_id=p.id WHERE i.settlement_id=s.id AND r.status IN ('CREATED','PROCESSING')) AS unresolved_refunds,
                       s.payment_account_id,tpa.account_status,tpa.provider_account_id,
                       COALESCE((SELECT eligible FROM (
                           SELECT (s.status='ELIGIBLE' AND s.provider_transfer_id IS NULL AND s.amount>0 AND s.currency='INR'
                             AND tpa.id IS NOT NULL AND tpa.trainer_user_id=s.trainer_user_id AND tpa.provider=pp.code
                             AND tp.payout_status='VERIFIED' AND tp.payout_enabled=TRUE AND tpa.is_primary=TRUE AND tpa.account_status='VERIFIED'
                             AND NULLIF(BTRIM(tpa.provider_account_id),'') IS NOT NULL AND pp.active=TRUE AND pp.code='RAZORPAY'
                             AND EXISTS (SELECT 1 FROM training_settlement_items si WHERE si.settlement_id=s.id)
                             AND s.amount=(SELECT COALESCE(SUM(si.amount),0) FROM training_settlement_items si WHERE si.settlement_id=s.id)
                             AND (SELECT COUNT(DISTINCT sp.id) FROM training_settlement_items si JOIN training_commissions sc ON sc.id=si.commission_id JOIN training_payments sp ON sp.id=sc.payment_id WHERE si.settlement_id=s.id)=1
                             AND NOT EXISTS (SELECT 1 FROM training_settlement_items si JOIN training_commissions sc ON sc.id=si.commission_id JOIN training_payments sp ON sp.id=sc.payment_id JOIN training_enrollments se ON se.id=si.enrollment_id JOIN training_offerings so ON so.id=se.training_offering_id WHERE si.settlement_id=s.id AND (sc.status<>'CALCULATED' OR sc.trainer_amount<>si.amount OR sc.currency<>s.currency OR sp.provider_payment_id IS NULL OR sp.payment_provider_id<>s.payment_provider_id OR sp.currency<>s.currency OR sp.status NOT IN ('CAPTURED','PARTIALLY_REFUNDED') OR so.status<>'COMPLETED' OR sc.gross_amount>sp.amount-COALESCE((SELECT SUM(rf.refund_amount) FROM training_refunds rf WHERE rf.payment_id=sp.id AND rf.status='PROCESSED'),0) OR NOT EXISTS (SELECT 1 FROM training_offering_sessions ss WHERE ss.training_offering_id=so.id) OR EXISTS (SELECT 1 FROM training_offering_sessions ss WHERE ss.training_offering_id=so.id AND ss.status<>'COMPLETED' AND ss.actual_end_at IS NULL) OR EXISTS (SELECT 1 FROM training_cancellations ca WHERE ca.enrollment_id=se.id AND ca.status IN ('REQUESTED','APPROVED')) OR EXISTS (SELECT 1 FROM training_refunds rf WHERE (rf.enrollment_id=se.id OR rf.payment_id=sp.id) AND rf.status IN ('CREATED','PROCESSING'))))) AS eligible
                           ) readiness) FROM training_settlements WHERE id=s.id),FALSE) AS engine_eligible
                  FROM training_settlements s LEFT JOIN trainer_payout_accounts tpa ON tpa.id=s.payment_account_id WHERE s.id=?
                """, settlementId);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Settlement", settlementId);
        Map<String, Object> row = rows.get(0);
        List<Map<String, Object>> checks = new ArrayList<>();
        add(checks, "Settlement exists", "PASSED", "Settlement " + settlementId + " exists.");
        add(checks, "Settlement is eligible", "ELIGIBLE".equals(row.get("status")) ? "PASSED" : "FAILED", "Current status: " + row.get("status"));
        add(checks, "Trainer payout account verified", "VERIFIED".equals(row.get("account_status")) ? "PASSED" : "FAILED", "Existing payout account status: " + row.get("account_status"));
        add(checks, "Route linked account", "UNKNOWN", "No environment-scoped Route linked-account mapping is stored.");
        add(checks, "Trainer exists", row.get("trainer_user_id") == null ? "FAILED" : "PASSED", "Trainer user ID: " + row.get("trainer_user_id"));
        add(checks, "Source payments captured", number(row.get("item_count")) == 0 ? "FAILED" : number(row.get("captured_count")) == number(row.get("item_count")) ? "PASSED" : "FAILED", "Captured payment items: " + row.get("captured_count") + " / " + row.get("item_count"));
        add(checks, "Single source payment allocation", number(row.get("payment_count")) == 1 ? "PASSED" : "UNKNOWN", "Source payment count: " + row.get("payment_count") + "; current transfer implementation supports a single source payment.");
        add(checks, "Unresolved refunds", number(row.get("unresolved_refunds")) == 0 ? "PASSED" : "FAILED", "Unresolved refund count: " + row.get("unresolved_refunds"));
        add(checks, "Settlement engine eligibility", Boolean.TRUE.equals(row.get("engine_eligible")) ? "PASSED" : "FAILED", "Existing settlement eligibility rules including payment, account, commission, completion and refund checks.");
        add(checks, "No existing or uncertain transfer", row.get("provider_transfer_id") == null && "ELIGIBLE".equals(row.get("status")) ? "PASSED" : "FAILED", "Provider transfer reference: " + row.get("provider_transfer_id"));
        add(checks, "Route API access", lastAccess != null && "ACCESSIBLE".equals(lastAccess.get("status")) ? "PASSED" : "UNKNOWN", "Run Check Route Access to refresh this result.");
        add(checks, "Razorpay Route enabled in application", routeEnabled && "TEST".equals(effectiveEnvironment()) ? "PASSED" : "FAILED", "Application transfer flag is " + routeEnabled + " and current environment is " + effectiveEnvironment() + ".");
        add(checks, "Webhook and reconciliation", "UNKNOWN", "Recipient bank settlement evidence is not currently available.");
        boolean failed = checks.stream().anyMatch(c -> "FAILED".equals(c.get("status")));
        boolean unknown = checks.stream().anyMatch(c -> "UNKNOWN".equals(c.get("status")));
        return Map.of("settlementId", settlementId, "checks", checks, "result", failed ? "NOT_READY" : unknown ? "MANUAL_REVIEW_REQUIRED" : "READY_FOR_TEST_TRANSFER", "checkedAt", Instant.now().toString());
    }

    private Map<String, Object> webhookHealth() {
        try {
            Map<String, Object> stats = db.queryForMap("SELECT COUNT(*) AS total, MAX(received_at) AS last_received, COUNT(*) FILTER (WHERE processing_status='FAILED') AS failed, COUNT(*) FILTER (WHERE processing_status IN ('PENDING','RECEIVED')) AS pending FROM payment_webhook_events");
            return Map.of("status", "LOCAL_EVENTS_AVAILABLE", "endpointConfigured", "UNKNOWN", "dashboardVerificationRequired", true, "events", stats);
        } catch (RuntimeException e) { return unknown("Webhook event diagnostics are unavailable."); }
    }

    private String effectiveEnvironment() { return environment.equalsIgnoreCase("LIVE") || keyId.startsWith("rzp_live_") ? "LIVE" : keyId.startsWith("rzp_test_") || environment.equalsIgnoreCase("TEST") ? "TEST" : "UNKNOWN"; }
    private Map<String, Object> unknown(String message) { return Map.of("status", "UNKNOWN", "message", message); }
    private void add(List<Map<String, Object>> checks, String name, String status, String message) { checks.add(Map.of("name", name, "status", status, "message", message)); }
    private int number(Object value) { return value instanceof Number n ? n.intValue() : 0; }
    private String sanitizedProviderCode(String body) { java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([A-Za-z0-9_]{1,80})\\\"").matcher(body == null ? "" : body); return m.find() ? m.group(1) : "UNKNOWN"; }
}
