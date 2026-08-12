package com.sparta.logistics.domain.repository;

import com.sparta.logistics.domain.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {
}
