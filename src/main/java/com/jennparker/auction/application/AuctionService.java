package com.jennparker.auction.application;

import com.jennparker.auction.domain.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import com.jennparker.auction.persistence.LotRepository;
import com.jennparker.auction.persistence.LotEntity;
import com.jennparker.auction.persistence.BidEntity;
import com.jennparker.auction.persistence.BidRepository;
import com.jennparker.auction.persistence.OrderEntity;
import com.jennparker.auction.persistence.OrderRepository;
import com.jennparker.auction.persistence.OutboxEventEntity;
import com.jennparker.auction.persistence.OutboxEventRepository;
import com.jennparker.auction.persistence.PaymentAllocationEntity;
import com.jennparker.auction.persistence.PaymentAllocationRepository;
import com.jennparker.auction.persistence.PaymentEntity;
import com.jennparker.auction.persistence.PaymentRepository;
import org.springframework.transaction.annotation.Transactional;

public class AuctionService {
    private final Map<String, Lot> lots = new ConcurrentHashMap<String, Lot>();
    private final CustomerService customers;
    private final Clock clock;
    private final AuctionStateProvider auctionStates;
    private final LotRepository lotRepository;
    private final BidRepository bidRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuctionMetrics metrics;
    private final CustomerCreditService credits;
    private final PaymentAllocationRepository paymentAllocations;
    private final PaymentOrchestrator paymentOrchestrator;
    private final PaymentRepository paymentRepository;
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository) {
        this(customers, clock, auctionStates, lotRepository, null, null, null, AuctionMetrics.noop(), null, null, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, AuctionMetrics metrics) {
        this(customers, clock, auctionStates, lotRepository, null, null, null, metrics, null, null, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, BidRepository bidRepository, AuctionMetrics metrics) {
        this(customers, clock, auctionStates, lotRepository, bidRepository, null, null, metrics, null, null, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, BidRepository bidRepository,
                          OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, AuctionMetrics metrics) {
        this(customers, clock, auctionStates, lotRepository, bidRepository, orderRepository, outboxEventRepository, metrics, null, null, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, BidRepository bidRepository,
                          OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, AuctionMetrics metrics,
                          CustomerCreditService credits, PaymentAllocationRepository paymentAllocations) {
        this(customers, clock, auctionStates, lotRepository, bidRepository, orderRepository, outboxEventRepository, metrics, credits, paymentAllocations, null, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, BidRepository bidRepository,
                          OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, AuctionMetrics metrics,
                          CustomerCreditService credits, PaymentAllocationRepository paymentAllocations, PaymentOrchestrator paymentOrchestrator) {
        this(customers, clock, auctionStates, lotRepository, bidRepository, orderRepository, outboxEventRepository, metrics, credits, paymentAllocations, paymentOrchestrator, null);
    }
    public AuctionService(CustomerService customers, Clock clock, AuctionStateProvider auctionStates, LotRepository lotRepository, BidRepository bidRepository,
                          OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, AuctionMetrics metrics,
                          CustomerCreditService credits, PaymentAllocationRepository paymentAllocations, PaymentOrchestrator paymentOrchestrator,
                          PaymentRepository paymentRepository) {
        this.customers = customers; this.clock = clock; this.auctionStates = auctionStates; this.lotRepository = lotRepository; this.bidRepository = bidRepository;
        this.orderRepository = orderRepository; this.outboxEventRepository = outboxEventRepository;
        this.metrics = metrics == null ? AuctionMetrics.noop() : metrics;
        this.credits = credits; this.paymentAllocations = paymentAllocations;
        this.paymentOrchestrator = paymentOrchestrator;
        this.paymentRepository = paymentRepository;
    }
    public void register(Lot lot) { lots.put(lot.getId(), lot); }
    public void remove(String lotId) { lots.remove(lotId); }
    public Lot createLot(String lotId, Instant closesAt) {
        Lot lot = new Lot(lotId, closesAt);
        if (lots.putIfAbsent(lotId, lot) != null) throw new ConflictException();
        return lot;
    }
    public Lot lot(String id) { return lots.get(id); }
    @Transactional
    public void bid(String lotId, Bid bid) {
        long started = System.nanoTime();
        try {
            Lot lot = required(lotId);
            if (lot.getAuctionId() == null || auctionStates.stateOf(lot.getAuctionId()) != AuctionState.LIVE) throw new InvalidBidException();
            if (!customers.verify(bid.getCustomerId()).isEligible()) throw new ForbiddenException();
            if (bidRepository == null) {
                if (!lot.accept(bid, clock)) throw new InvalidBidException();
            } else {
                int updated = lotRepository.acceptHigherBid(lotId, bid.getAmount(), bid.getCustomerId(), bid.getTimestamp());
                if (updated != 1) throw new InvalidBidException();
                bidRepository.save(new BidEntity(lotId, bid));
                lotRepository.findById(lotId).map(LotEntity::toDomain).ifPresent(refreshed -> lots.put(lotId, refreshed));
            }
            metrics.recordBid("accepted", "none", System.nanoTime() - started);
        } catch (ForbiddenException ex) {
            metrics.recordBid("rejected", "customer_ineligible", System.nanoTime() - started);
            throw ex;
        } catch (InvalidBidException ex) {
            metrics.recordBid("rejected", "invalid_bid", System.nanoTime() - started);
            throw ex;
        } catch (ConflictException ex) {
            metrics.recordBid("rejected", "lot_not_found", System.nanoTime() - started);
            throw ex;
        }
    }
    @Transactional
    public Order settle(String lotId) {
        long started = System.nanoTime();
        try {
            Lot lot = required(lotId);
            if (lot.getAuctionId() == null || auctionStates.stateOf(lot.getAuctionId()) != AuctionState.CLOSED) throw new ConflictException();
            Bid winner = lot.close(clock);
            if (winner == null) throw new ConflictException();
            Instant settledAt = Instant.now(clock);
            lotRepository.findById(lotId).ifPresent(entity -> { entity.recordWinner(winner, settledAt); lotRepository.save(entity); });
            Order order = new Order(lotId, winner.getCustomerId(), winner.getAmount(), settledAt);
            persistOrderAndPaymentEvent(order);
            metrics.recordSettlement("success", 1, System.nanoTime() - started);
            return order;
        } catch (ConflictException ex) {
            metrics.recordSettlement("conflict", 0, System.nanoTime() - started);
            throw ex;
        }
    }

    @Transactional
    public Order settle(String lotId, SettlementPaymentPlan plan) {
        Lot lot = required(lotId);
        if (lot.getAuctionId() == null || auctionStates.stateOf(lot.getAuctionId()) != AuctionState.CLOSED) throw new ConflictException();
        Bid winner = lot.getHighestBid();
        if (winner == null || !isFullyPaid(winner.getCustomerId(), winner.getAmount(), plan)) throw new PaymentConflictException();
        Order pending = new Order(lotId, winner.getCustomerId(), winner.getAmount(), Instant.now(clock));
        persistPendingOrder(pending);
        authorizePayments(pending, plan, lotId + ":" + winner.getCustomerId());
        if (lot.close(clock) == null) throw new ConflictException();
        Instant settledAt = Instant.now(clock);
        lotRepository.findById(lotId).ifPresent(entity -> { entity.recordWinner(winner, settledAt); lotRepository.save(entity); });
        Order order = paidOrder(pending, lotId, winner, settledAt);
        recordPayments(java.util.Collections.singletonList(order), winner.getCustomerId(), plan);
        return order;
    }

    @Transactional
    public List<Order> settleAuction(String auctionId, List<Lot> auctionLots) {
        long started = System.nanoTime();
        try {
            if (auctionStates.stateOf(auctionId) != AuctionState.CLOSED) throw new ConflictException();
            List<Order> orders = new ArrayList<Order>();
            for (Lot listedLot : auctionLots) {
                Lot lot = lots.get(listedLot.getId());
                if (lot == null) throw new ConflictException();
                Bid winner = lot.closeForAuctionSettlement();
                if (winner != null) {
                    Instant settledAt = Instant.now(clock);
                    lotRepository.findById(lot.getId()).ifPresent(entity -> { entity.recordWinner(winner, settledAt); lotRepository.save(entity); });
                    Order order = new Order(lot.getId(), winner.getCustomerId(), winner.getAmount(), settledAt);
                    persistOrderAndPaymentEvent(order);
                    orders.add(order);
                }
            }
            metrics.recordSettlement("success", orders.size(), System.nanoTime() - started);
            return orders;
        } catch (ConflictException ex) {
            metrics.recordSettlement("conflict", 0, System.nanoTime() - started);
            throw ex;
        }
    }

    @Transactional
    public List<Order> settleAuction(String auctionId, List<Lot> auctionLots, SettlementPaymentPlan plan) {
        if (auctionStates.stateOf(auctionId) != AuctionState.CLOSED) throw new ConflictException();
        List<Lot> openLots = new ArrayList<Lot>();
        BigDecimal total = BigDecimal.ZERO; String customerId = plan == null ? null : plan.getCustomerId();
        if (customerId == null) throw new PaymentConflictException();
        for (Lot listed : auctionLots) {
            Lot lot = lots.get(listed.getId());
            if (lot == null) throw new ConflictException();
            Bid bid = lot.getHighestBid();
            if (bid != null && customerId.equals(bid.getCustomerId())) { openLots.add(lot); total = total.add(bid.getAmount()); }
        }
        if (customerId == null || !isFullyPaid(customerId, total, plan)) throw new PaymentConflictException();
        List<Order> pendingOrders = new ArrayList<Order>();
        for (Lot lot : openLots) {
            Order pending = new Order(lot.getId(), customerId, lot.getHighestBid().getAmount(), Instant.now(clock));
            persistPendingOrder(pending);
            pendingOrders.add(pending);
        }
        authorizePayments(pendingOrders.isEmpty() ? null : pendingOrders.get(0), plan, auctionId + ":" + customerId);
        List<Order> orders = new ArrayList<Order>();
        for (Lot lot : openLots) {
            Bid winner = lot.closeForAuctionSettlement();
            Instant settledAt = Instant.now(clock);
            lotRepository.findById(lot.getId()).ifPresent(entity -> { entity.recordWinner(winner, settledAt); lotRepository.save(entity); });
            orders.add(paidOrder(pendingOrders.get(orders.size()), lot.getId(), winner, settledAt));
        }
        recordPayments(orders, customerId, plan);
        return orders;
    }
    private Lot required(String id) {
        Lot lot = lots.get(id);
        if (lot == null && bidRepository != null) {
            lot = lotRepository.findById(id).map(LotEntity::toDomain).orElse(null);
            if (lot != null) lots.put(id, lot);
        }
        if (lot == null) throw new ConflictException();
        return lot;
    }
    private void persistOrderAndPaymentEvent(Order order) {
        if (orderRepository == null || outboxEventRepository == null) return;
        orderRepository.save(new OrderEntity(order));
        outboxEventRepository.save(new OutboxEventEntity(order));
    }
    private void persistPendingOrder(Order order) {
        if (orderRepository != null) orderRepository.save(new OrderEntity(order));
    }
    private boolean isFullyPaid(String customerId, BigDecimal owed, SettlementPaymentPlan plan) {
        if (plan == null || plan.total().compareTo(owed) < 0 || plan.getCreditAmount().signum() < 0) return false;
        if (customers.find(customerId) == null) return false;
        for (SettlementPaymentPlan.Payment payment : plan.getPayments()) {
            if (payment.getAmount() == null || payment.getAmount().signum() <= 0) return false;
            if (payment.getPaymentId() != null) {
                if (paymentRepository == null) return false;
                try {
                    PaymentEntity authorizedPayment = paymentRepository.findById(java.util.UUID.fromString(payment.getPaymentId())).orElse(null);
                    if (authorizedPayment == null || !authorizedPayment.isAuthorized()
                            || !customerId.equals(authorizedPayment.getCustomerId())
                            || authorizedPayment.getAmount().compareTo(payment.getAmount()) != 0
                            || (payment.getPaymentMethodId() != null && !payment.getPaymentMethodId().equals(authorizedPayment.getPaymentMethodId()))) return false;
                } catch (IllegalArgumentException ex) { return false; }
                continue;
            }
            boolean authorized = customers.find(customerId).getPaymentMethods().stream().anyMatch(m -> m.getId().equals(payment.getPaymentMethodId()) && m.isAuthorized());
            if (!authorized) return false;
        }
        return credits == null || credits.balance(customerId).compareTo(plan.getCreditAmount()) >= 0;
    }
    private Order paidOrder(String lotId, Bid winner, Instant at) {
        Order order = new Order(lotId, winner.getCustomerId(), winner.getAmount(), at);
        return paidOrder(order, lotId, winner, at);
    }
    private Order paidOrder(Order order, String lotId, Bid winner, Instant at) {
        order.transitionTo(Order.Status.PROCESSING); order.transitionTo(Order.Status.SETTLED);
        if (orderRepository != null) orderRepository.save(new OrderEntity(order));
        return order;
    }
    private void authorizePayments(Order order, SettlementPaymentPlan plan, String keyPrefix) {
        if (plan.getPayments().isEmpty()) return;
        if (paymentOrchestrator == null || order == null) throw new PaymentConflictException();
        for (SettlementPaymentPlan.Payment payment : plan.getPayments()) {
            if (payment.getPaymentId() != null) continue;
            paymentOrchestrator.authorizePayment(order, payment.getPaymentMethodId(), payment.getAmount(), keyPrefix + ":" + payment.getPaymentMethodId());
        }
    }
    private void recordPayments(List<Order> orders, String customerId, SettlementPaymentPlan plan) {
        String reference = orders.get(0).getId();
        if (credits != null) credits.consume(customerId, plan.getCreditAmount(), reference);
        int orderIndex = 0;
        BigDecimal remainingOwed = orders.get(0).getAmount();
        for (SettlementPaymentPlan.Payment payment : plan.getPayments()) {
            BigDecimal remaining = payment.getAmount();
            PaymentEntity paymentEntity = null;
            if (paymentRepository != null && payment.getPaymentId() != null) {
                try {
                    paymentEntity = paymentRepository.findById(java.util.UUID.fromString(payment.getPaymentId())).orElseThrow(PaymentConflictException::new);
                } catch (IllegalArgumentException ex) { throw new PaymentConflictException(); }
                if (!paymentEntity.isAuthorized()) throw new PaymentConflictException();
                paymentEntity.updateStatus(PaymentStatus.COMPLETED.name());
                paymentRepository.save(paymentEntity);
            } else if (paymentRepository != null) {
                paymentEntity = paymentRepository.save(new PaymentEntity(customerId, payment.getPaymentMethodId(), payment.getAmount(), PaymentStatus.COMPLETED.name()));
            }
            while (remaining.signum() > 0 && orderIndex < orders.size()) {
                BigDecimal applied = remaining.min(remainingOwed);
                if (paymentAllocations != null) paymentAllocations.save(new PaymentAllocationEntity(paymentEntity == null ? null : paymentEntity.getId(), java.util.UUID.fromString(orders.get(orderIndex).getId()), payment.getPaymentMethodId(), applied, "AUTHORIZED"));
                remaining = remaining.subtract(applied); remainingOwed = remainingOwed.subtract(applied);
                if (remainingOwed.signum() == 0 && ++orderIndex < orders.size()) remainingOwed = orders.get(orderIndex).getAmount();
            }
        }
        BigDecimal overpayment = plan.total().subtract(orders.stream().map(Order::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (credits != null) credits.grant(customerId, overpayment, reference);
    }
    public static class InvalidBidException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class ConflictException extends RuntimeException {}
    public static class PaymentConflictException extends RuntimeException {}
}
