package com.Carwash.test.repository;

import com.Carwash.test.model.ServicePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePriceRepository extends JpaRepository<ServicePrice, Long> {
    ServicePrice findByServiceType(String serviceType);
} 