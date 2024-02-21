package com.shopme.wallet;

import com.shopme.common.entity.Customer;
import com.shopme.common.entity.Wallet;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class WalletService {

    @Autowired
    private WalletRepository walletRepo;

    public Wallet findWalletByCustomer(Customer customer) {
        System.out.println(customer.getId());
        return walletRepo.findByCustomer(customer);

    }

    public void createWallet(Customer customer){
        System.out.println(customer);
        Wallet newWallet = new Wallet(customer, 0d);
        this.walletRepo.save(newWallet);
    }

    public Wallet getWalletById(Long walletId){
        return walletRepo.findById(walletId).get();
    }

    public Wallet reduceBalance(Long walletId,Double amountToReduce){
        Wallet wallet = walletRepo.findById(walletId).get();
        Double newBalance = wallet.getBalance()-amountToReduce;
        wallet.setBalance(newBalance);
        return walletRepo.save(wallet);
    }
    public void updateWalletBalance(Double balance, Integer customerId) {
        walletRepo.updateBalance(balance, customerId);
    }

    public void depositFunds(Customer customer, double amount){
        Wallet wallet = walletRepo.findByCustomer(customer);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepo.save(wallet);
    }

    public void withdrawFunds(Customer customer, double amount){
        Wallet wallet=walletRepo.findByCustomer(customer);
        if(wallet.getBalance() >= amount){
            wallet.setBalance(wallet.getBalance()-amount);
            walletRepo.save(wallet);
        }
//        else {
//            throw new InsufficientFundsException("Insufficient funds in the wallet");
//        }
    }

    public double checkWalletBalance(Customer customer){
        Wallet wallet=walletRepo.findByCustomer(customer);
        return wallet.getBalance();
    }

}
