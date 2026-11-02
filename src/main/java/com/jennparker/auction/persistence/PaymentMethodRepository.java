package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, String> {
    List<PaymentMethodEntity> findAllByCustomerId(String customerId);
    Optional<PaymentMethodEntity> findByIdAndCustomerId(String id, String customerId);
}
