package com.playflow.repository;

import com.playflow.entity.Entities.SpendingLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpendingLimitRepository extends JpaRepository<SpendingLimit, Long> {}
