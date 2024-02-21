package com.shopme.wallet;

import com.shopme.common.entity.Customer;
import com.shopme.common.entity.Wallet;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends CrudRepository<Wallet, Long> {

    public Wallet findByCustomer(Customer customer);

    @Modifying
    @Query("UPDATE Wallet c SET c.balance = ?1 WHERE c.customer.id = ?2")
    public void updateBalance(Double balance, Integer customerId);
}
