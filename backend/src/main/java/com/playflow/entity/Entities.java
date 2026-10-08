package com.playflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class Entities {
    private Entities() {}
    @Entity @Table(name="users") public static class User {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public String name;
        @Column(nullable=false, unique=true) public String email;
        @Column(name="phone_number", unique=true, length=16) public String phoneNumber;
        @Column(nullable=false) public String passwordHash;
        @Column(nullable=false) public String role="USER";
        @Column(nullable=false) public String kycStatus="PENDING";
        @Column(nullable=false) public Instant createdAt=Instant.now();
        protected User() {}
        public User(String name,String email,String passwordHash){this.name=name;this.email=email.toLowerCase();this.passwordHash=passwordHash;}
        public User(String name,String email,String passwordHash,String phoneNumber){this(name,email,passwordHash);this.phoneNumber=phoneNumber;}
        public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getPhoneNumber(){return phoneNumber;} public String getRole(){return role;} public String getKycStatus(){return kycStatus;}
    }
    @Entity(name="Wallet") @Table(name="wallets") public static class Wallet {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true) public Long userId;
        @Column(nullable=false,precision=19,scale=2) public BigDecimal balance=BigDecimal.ZERO;
        @Column(nullable=false,length=3) public String currency="INR";
        @Column(nullable=false) public String status="ACTIVE";
        protected Wallet() {}
        public Wallet(Long userId){this.userId=userId;}
        public Long getUserId(){return userId;} public BigDecimal getBalance(){return balance;} public String getCurrency(){return currency;}
    }
    @Entity @Table(name="spending_limits") public static class SpendingLimit {
        @Id public Long userId;
        @Column(nullable=false,precision=19,scale=2) public BigDecimal dailyLimit=new BigDecimal("5000.00");
        @Column(nullable=false,precision=19,scale=2) public BigDecimal monthlyLimit=new BigDecimal("50000.00");
        @Column(nullable=false) public Instant updatedAt=Instant.now();
        protected SpendingLimit() {}
        public SpendingLimit(Long userId,BigDecimal dailyLimit,BigDecimal monthlyLimit){this.userId=userId;this.dailyLimit=dailyLimit;this.monthlyLimit=monthlyLimit;}
    }
    @Entity @Table(name="payments") public static class Payment {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false,unique=true) public String transactionId;
        @Column(nullable=false) public Long senderId;
        @Column(nullable=false) public Long receiverId;
        @Column(nullable=false,precision=19,scale=2) public BigDecimal amount;
        @Column(nullable=false) public String status;
        @Column(nullable=false,length=1000) public String description;
        @Column(nullable=false) public String category;
        @Column(unique=true) public String idempotencyKey;
        @Column(nullable=false) public Instant createdAt=Instant.now();
        public Payment() {}
        public String getTransactionId(){return transactionId;} public Long getSenderId(){return senderId;} public Long getReceiverId(){return receiverId;} public BigDecimal getAmount(){return amount;} public String getStatus(){return status;} public String getDescription(){return description;} public String getCategory(){return category;} public Instant getCreatedAt(){return createdAt;}
    }
    @Entity @Table(name="journey_events") public static class JourneyEvent {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public String transactionId;
        @Column(nullable=false) public String stage;
        @Column(nullable=false,length=500) public String note;
        @Column(nullable=false) public Instant occurredAt=Instant.now();
        protected JourneyEvent() {}
        public JourneyEvent(String transactionId,String stage,String note){this.transactionId=transactionId;this.stage=stage;this.note=note;}
        public Long getId(){return id;} public String getStage(){return stage;} public String getNote(){return note;} public Instant getOccurredAt(){return occurredAt;}
    }
    @Entity @Table(name="notifications") public static class AppNotification {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
        @Column(nullable=false) public Long userId;
        @Column(nullable=false,length=300) public String message;
        @Column(nullable=false) public String transactionId;
        @Column(nullable=false) public Instant createdAt=Instant.now();
        @Column(name="is_read",nullable=false) public boolean read=false;
        protected AppNotification() {}
        public AppNotification(Long userId,String message,String transactionId){this.userId=userId;this.message=message;this.transactionId=transactionId;}
        public Long getId(){return id;} public Long getUserId(){return userId;} public String getMessage(){return message;} public String getTransactionId(){return transactionId;} public Instant getCreatedAt(){return createdAt;} public boolean isRead(){return read;}
    }
}
