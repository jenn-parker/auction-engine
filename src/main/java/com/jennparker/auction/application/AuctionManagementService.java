package com.jennparker.auction.application;

import com.jennparker.auction.domain.Auction;
import com.jennparker.auction.domain.Lot;
import com.jennparker.auction.domain.AuctionState;
import com.jennparker.auction.persistence.AuctionEntity;
import com.jennparker.auction.persistence.AuctionRepository;
import com.jennparker.auction.persistence.LotEntity;
import com.jennparker.auction.persistence.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuctionManagementService implements AuctionStateProvider {
    private final AuctionRepository auctions;
    private final LotRepository lots;
    private final java.time.Clock clock;
    public AuctionManagementService(AuctionRepository auctions, LotRepository lots, java.time.Clock clock) { this.auctions = auctions; this.lots = lots; this.clock = clock; }

    @Transactional
    public Auction create(Auction auction) {
        if (auctions.existsById(auction.getId())) throw new DuplicateAuctionException();
        return toDomain(auctions.save(new AuctionEntity(auction)));
    }

    @Transactional(readOnly = true)
    public Auction get(String auctionId) {
        return auctions.findById(auctionId).map(this::toDomain).orElseThrow(AuctionNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionState stateOf(String auctionId) { return get(auctionId).getState(); }

    @Transactional
    public Auction transition(String auctionId, AuctionState next) {
        AuctionEntity entity = auctions.findById(auctionId).orElseThrow(AuctionNotFoundException::new);
        Auction transitioned = toDomain(entity).transitionTo(next, clock.instant());
        entity.updateFrom(transitioned);
        return toDomain(auctions.save(entity));
    }

    @Transactional
    public Auction update(Auction auction) {
        AuctionEntity entity = auctions.findById(auction.getId()).orElseThrow(AuctionNotFoundException::new);
        Auction current = toDomain(entity);
        entity.updateFrom(new Auction(auction.getId(), auction.getScheduledAt(), auction.getPhysicalLocation(),
                current.getLots(), current.getState(), current.getPublishedAt(), current.getStartedAt(), current.getClosedAt()));
        return toDomain(auctions.save(entity));
    }

    @Transactional
    public Lot updateLot(String lotId, java.time.Instant closesAt) {
        LotEntity entity = lots.findById(lotId).orElseThrow(LotNotFoundException::new);
        AuctionState state = stateOf(entity.getAuctionId());
        if (!"OPEN".equals(entity.getStatus()) || state == AuctionState.LIVE || state == AuctionState.CLOSED) throw new LotModificationException();
        entity.updateClosingTime(closesAt);
        return lots.save(entity).toDomain();
    }

    @Transactional(readOnly = true)
    public Lot getLot(String lotId) {
        return lots.findById(lotId).map(LotEntity::toDomain).orElseThrow(LotNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<Lot> getLots(String auctionId) {
        if (!auctions.existsById(auctionId)) throw new AuctionNotFoundException();
        return lots.findAllByAuctionId(auctionId).stream().map(LotEntity::toDomain).collect(Collectors.toList());
    }

    @Transactional
    public void deleteLot(String lotId) {
        LotEntity entity = lots.findById(lotId).orElseThrow(LotNotFoundException::new);
        AuctionState state = stateOf(entity.getAuctionId());
        if (state == AuctionState.LIVE || state == AuctionState.CLOSED) throw new LotModificationException();
        try { lots.delete(entity); lots.flush(); }
        catch (DataIntegrityViolationException ex) { throw new LotDeletionConflictException(); }
    }

    @Transactional
    public Lot addLot(String auctionId, String lotId, java.time.Instant closesAt) {
        return addLot(auctionId, new Lot(lotId, auctionId, closesAt));
    }

    @Transactional
    public Lot addLot(String auctionId, Lot lot) {
        if (!auctions.existsById(auctionId)) throw new AuctionNotFoundException();
        AuctionState state = stateOf(auctionId);
        if (state == AuctionState.LIVE || state == AuctionState.CLOSED) throw new LotModificationException();
        if (lots.existsById(lot.getId())) throw new DuplicateLotException();
        return lots.save(new LotEntity(auctionId, lot)).toDomain();
    }

    private Auction toDomain(AuctionEntity entity) {
        List<Lot> lotsForAuction = lots.findAllByAuctionId(entity.getId()).stream()
                .map(LotEntity::toDomain).collect(Collectors.toList());
        return entity.toDomain(lotsForAuction);
    }
    public static class DuplicateAuctionException extends RuntimeException { }
    public static class AuctionNotFoundException extends RuntimeException { }
    public static class DuplicateLotException extends RuntimeException { }
    public static class LotNotFoundException extends RuntimeException { }
    public static class LotModificationException extends RuntimeException { }
    public static class LotDeletionConflictException extends RuntimeException { }
}
