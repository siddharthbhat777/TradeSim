package com.siddharth.tradesim_backend.order.repository;

import com.siddharth.tradesim_backend.order.model.Fill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FillRepository extends JpaRepository<Fill, UUID> {
}