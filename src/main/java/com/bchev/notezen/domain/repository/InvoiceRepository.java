package com.bchev.notezen.domain.repository;

import com.bchev.notezen.domain.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    Optional<InvoiceEntity> findByStripeInvoiceId(String stripeInvoiceId);
    List<InvoiceEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    List<InvoiceEntity> findByStatusOrderByCreatedAtDesc(String status);
}
