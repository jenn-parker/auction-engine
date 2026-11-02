package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CreditLedgerRepository extends JpaRepository<CreditLedgerEntity, UUID> {
    List<CreditLedgerEntity> findAllByCustomerId(String customerId);
}
