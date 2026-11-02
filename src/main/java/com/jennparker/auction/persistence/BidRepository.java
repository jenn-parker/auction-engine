package com.jennparker.auction.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BidRepository extends JpaRepository<BidEntity, UUID> { }
