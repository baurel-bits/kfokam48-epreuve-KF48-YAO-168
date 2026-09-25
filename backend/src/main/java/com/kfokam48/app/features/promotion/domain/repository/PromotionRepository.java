package com.kfokam48.app.features.promotion.domain.repository;

import com.kfokam48.app.features.promotion.domain.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
