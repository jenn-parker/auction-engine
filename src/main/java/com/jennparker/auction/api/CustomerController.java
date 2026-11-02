package com.jennparker.auction.api;

import com.jennparker.auction.application.CustomerManagementService;
import com.jennparker.auction.application.CustomerBalanceService;
import com.jennparker.auction.application.CustomerInvoiceService;
import com.jennparker.auction.domain.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer profiles and payment-method management")
public class CustomerController {
    private final CustomerManagementService customers;
    private final CustomerBalanceService balances;
    private final CustomerInvoiceService invoices;
    public CustomerController(CustomerManagementService customers, CustomerBalanceService balances, CustomerInvoiceService invoices) { this.customers = customers; this.balances = balances; this.invoices = invoices; }

    @Operation(summary = "Get customer balance", description = "Returns unpaid orders and the total amount currently owed by the customer. Settled orders are excluded.")
    @GetMapping("/{customerId}/balance")
    public CustomerBalanceService.Balance balance(@PathVariable String customerId) { return balances.getBalance(customerId); }

    @Operation(summary = "Get customer invoice", description = "Returns itemized won lots, their costs, payment status, amount paid, and amount still owed.")
    @GetMapping("/{customerId}/invoice")
    public CustomerInvoiceService.Invoice invoice(@PathVariable String customerId) { return invoices.getInvoice(customerId); }

    @Operation(summary = "Create a customer", description = "Creates a customer profile and optional payment-method reference.")
    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerRequest request) {
        Customer customer = customers.create(request.toDomain());
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/customers/" + customer.getId()))).body(customer);
    }
    @Operation(summary = "Replace a customer", description = "Replaces the customer profile while retaining payment methods.")
    @PutMapping("/{customerId}")
    public Customer update(@PathVariable String customerId, @Valid @RequestBody CustomerRequest request) { return customers.update(request.toDomain(customerId)); }
    @Operation(summary = "Partially update a customer", description = "Updates only supplied profile fields.")
    @PatchMapping("/{customerId}")
    public Customer patch(@PathVariable String customerId, @RequestBody CustomerPatchRequest request) { return customers.update(request.apply(customers.get(customerId))); }

    @Operation(summary = "List payment methods", description = "Returns all payment methods on file, including the current default.")
    @GetMapping("/{customerId}/payment-methods")
    public List<PaymentMethod> listPaymentMethods(@PathVariable String customerId) { return customers.get(customerId).getPaymentMethods(); }
    @Operation(summary = "Add a payment method", description = "Stores provider-safe payment metadata; raw card data is not accepted.")
    @PostMapping("/{customerId}/payment-methods")
    public ResponseEntity<PaymentMethod> addPaymentMethod(@PathVariable String customerId, @Valid @RequestBody PaymentMethodRequest request) {
        PaymentMethod method = customers.addPaymentMethod(customerId, request.toDomain());
        return ResponseEntity.created(java.util.Objects.requireNonNull(URI.create("/api/v1/customers/" + customerId + "/payment-methods/" + method.getId()))).body(method);
    }
    @Operation(summary = "Partially update a payment method", description = "Updates authorization, type, or default designation.")
    @PatchMapping("/{customerId}/payment-methods/{paymentMethodId}")
    public PaymentMethod patchPaymentMethod(@PathVariable String customerId, @PathVariable String paymentMethodId, @RequestBody PaymentMethodPatchRequest request) {
        PaymentMethod current = customers.get(customerId).getPaymentMethods().stream().filter(m -> m.getId().equals(paymentMethodId)).findFirst().orElseThrow(CustomerManagementService.PaymentMethodNotFoundException::new);
        return customers.updatePaymentMethod(customerId, paymentMethodId, new PaymentMethod(paymentMethodId,
                request.provider == null ? current.getProvider() : request.provider,
                request.type == null ? current.getType() : request.type, request.authorized == null ? current.isAuthorized() : request.authorized,
                request.isDefault == null ? current.isDefault() : request.isDefault,
                request.expirationMonth == null ? current.getExpirationMonth() : request.expirationMonth,
                request.expirationYear == null ? current.getExpirationYear() : request.expirationYear));
    }
    @Operation(summary = "Delete a payment method", description = "Removes a payment-method reference from the customer profile.")
    @DeleteMapping("/{customerId}/payment-methods/{paymentMethodId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deletePaymentMethod(@PathVariable String customerId, @PathVariable String paymentMethodId) { customers.removePaymentMethod(customerId, paymentMethodId); }

    public static class CustomerRequest {
        @NotBlank public String customerId; @NotBlank public String name; @NotBlank @Email public String email; public String phone;
        @Valid public AddressRequest address; public Customer.AccountStatus accountStatus = Customer.AccountStatus.ACTIVE; public boolean biddingEnabled = true;
        @Valid public PaymentMethodRequest paymentMethod;
        Customer toDomain() { return toDomain(null); }
        Customer toDomain(String id) {
            Address a = address == null ? null : new Address(address.line1, address.line2, address.city, address.stateOrRegion, address.postalCode, address.countryCode);
            List<PaymentMethod> methods = new ArrayList<PaymentMethod>(); if (paymentMethod != null) methods.add(paymentMethod.toDomain());
            return new Customer(id == null ? customerId : id, name, email, phone, a, accountStatus, biddingEnabled, methods);
        }
    }
    public static class CustomerPatchRequest {
        public String name; @Email public String email; public String phone; public Customer.AccountStatus accountStatus; public Boolean biddingEnabled;
        Customer apply(Customer c) { return new Customer(c.getId(), name == null ? c.getName() : name, email == null ? c.getEmail() : email,
                phone == null ? c.getPhone() : phone, c.getAddress(), accountStatus == null ? c.getAccountStatus() : accountStatus,
                biddingEnabled == null ? c.isBiddingEnabled() : biddingEnabled, c.getPaymentMethods()); }
    }
    public static class PaymentMethodRequest {
        @NotBlank public String paymentMethodId; public String provider = "external"; @NotNull public PaymentMethod.Type type; public boolean authorized = true; public boolean isDefault;
        @Min(1) @Max(12) public Integer expirationMonth; @Min(2000) public Integer expirationYear;
        PaymentMethod toDomain() { return new PaymentMethod(paymentMethodId, provider, type, authorized, isDefault, expirationMonth, expirationYear); }
    }
    public static class PaymentMethodPatchRequest { public String provider; public PaymentMethod.Type type; public Boolean authorized; public Boolean isDefault; @Min(1) @Max(12) public Integer expirationMonth; @Min(2000) public Integer expirationYear; }
    public static class AddressRequest {
        @NotBlank public String line1; public String line2; @NotBlank public String city; @NotBlank public String stateOrRegion; @NotBlank public String postalCode; @NotBlank @Size(min = 2, max = 2) public String countryCode;
    }
}
