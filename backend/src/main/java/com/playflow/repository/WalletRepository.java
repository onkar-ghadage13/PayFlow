package com.playflow.repository;
import com.playflow.entity.Entities.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface WalletRepository extends JpaRepository<Wallet,Long> {
    Optional<Wallet> findByUserId(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.userId=:userId") Optional<Wallet> lockByUserId(@Param("userId") Long userId);
}
