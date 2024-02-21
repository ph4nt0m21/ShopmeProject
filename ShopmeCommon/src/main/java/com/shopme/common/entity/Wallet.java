package com.shopme.common.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet")
public class Wallet extends IdBasedEntity{

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private Double balance;

    private String currency;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void initialise(){
        if(balance<=0){
            balance=0d;
        }
    }

    public Wallet(){

    }

    public Wallet(Customer customer, Double balance) {
        this.customer = customer;
        this.balance = balance;
    }

    public Customer getCustomer(){
        return customer;
    }

    public void setCustomer(Customer customer){
        this.customer=customer;
    }

    public Double getBalance(){
        return balance;
    }

    public void setBalance(Double balance){
        this.balance=balance;
    }

    public String getCurrency(){
        return currency;
    }

    public void setCurrency(String currency){
        this.currency=currency;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt=createdAt;
    }
}
