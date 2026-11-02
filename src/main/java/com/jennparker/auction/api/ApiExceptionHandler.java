package com.jennparker.auction.api;

import com.jennparker.auction.application.AuctionService;
import com.jennparker.auction.application.CustomerManagementService;
import com.jennparker.auction.application.AuctionManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuctionService.InvalidBidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidBid() { }

    @ExceptionHandler(AuctionService.ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void forbidden() { }

    @ExceptionHandler(AuctionService.ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void conflict() { }

    @ExceptionHandler(AuctionService.PaymentConflictException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public void paymentConflict() { }

    @ExceptionHandler(com.jennparker.auction.application.PaymentService.InvalidPaymentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void invalidPayment() { }

    @ExceptionHandler(com.jennparker.auction.application.PaymentService.PaymentConflictException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public void paymentAuthorizationConflict() { }

    @ExceptionHandler(com.jennparker.auction.application.PaymentService.PaymentAuthorizationException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public void paymentAuthorizationFailed() { }

    @ExceptionHandler(com.jennparker.auction.application.PaymentQueryService.PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void paymentNotFound() { }

    @ExceptionHandler(CustomerManagementService.DuplicateCustomerException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void duplicateCustomer() { }

    @ExceptionHandler(CustomerManagementService.CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void customerNotFound() { }

    @ExceptionHandler(CustomerManagementService.DuplicatePaymentMethodException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void duplicatePaymentMethod() { }

    @ExceptionHandler(CustomerManagementService.PaymentMethodNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void paymentMethodNotFound() { }

    @ExceptionHandler(AuctionManagementService.DuplicateAuctionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void duplicateAuction() { }

    @ExceptionHandler(AuctionManagementService.AuctionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void auctionNotFound() { }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void invalidStateTransition() { }

    @ExceptionHandler(AuctionManagementService.DuplicateLotException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void duplicateLot() { }

    @ExceptionHandler(AuctionManagementService.LotNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void lotNotFound() { }

    @ExceptionHandler(AuctionManagementService.LotModificationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void lotModificationConflict() { }

    @ExceptionHandler(AuctionManagementService.LotDeletionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void lotDeletionConflict() { }

    @ExceptionHandler(AuctionManagementController.AuctionRequiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void auctionRequired() { }

    @ExceptionHandler(com.jennparker.auction.application.AuctioneerAuthorizationService.AuctioneerUnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public void auctioneerUnauthorized() { }

    @ExceptionHandler(com.jennparker.auction.application.AuctioneerAuthorizationService.AuctioneerConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void auctioneerConflict() { }
}
