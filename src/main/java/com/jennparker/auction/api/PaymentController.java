package com.jennparker.auction.api;

import com.jennparker.auction.application.PaymentQueryService;
import com.jennparker.auction.application.PaymentService;
import com.jennparker.auction.domain.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.util.List;
import java.net.URI;
import java.math.BigDecimal;

/** Payment transaction creation and read endpoints. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments", description = "Authorized payment transactions; distinct from reusable payment methods")
public class PaymentController {
    private final PaymentQueryService payments;
    private final PaymentService paymentService;

    public PaymentController(PaymentQueryService payments, PaymentService paymentService) { this.payments = payments; this.paymentService = paymentService; }

    public PaymentController(PaymentQueryService payments) { this(payments, null); }

    @Operation(summary = "Create a payment", description = "Authorizes a saved payment instrument and records a payment transaction for later lot or auction settlement application.")
    @PostMapping("/payments")
    public ResponseEntity<Payment> create(@Valid @RequestBody PaymentRequest request,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey) {
        Payment payment = paymentService.create(request.customerId, request.paymentMethodId, request.amount, idempotencyKey);
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/payments/" + payment.getId()))).body(payment);
    }

    @Operation(summary = "List customer payments", description = "Returns the customer's authorized payment transactions, newest first.")
    @GetMapping("/customers/{customerId}/payments")
    public List<Payment> list(@PathVariable String customerId) { return payments.list(customerId); }

    @Operation(summary = "Get a payment", description = "Returns one authorized payment transaction belonging to the customer.")
    @GetMapping("/customers/{customerId}/payments/{paymentId}")
    public Payment get(@PathVariable String customerId, @PathVariable String paymentId) { return payments.get(customerId, paymentId); }

    @Operation(summary = "Get a payment by ID", description = "Returns one payment transaction by its unique ID.")
    @GetMapping("/payments/{paymentId}")
    public Payment getById(@PathVariable String paymentId) { return payments.getById(paymentId); }

    public static class PaymentRequest {
        @NotBlank public String customerId;
        @NotBlank public String paymentMethodId;
        @NotNull @DecimalMin("0.01") public BigDecimal amount;
    }
}
