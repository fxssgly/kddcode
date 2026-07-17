package com.kdd.demo.repository;

import com.kdd.demo.entity.RegressionSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegressionRepository extends JpaRepository<RegressionSample, Integer> {
    List<RegressionSample> findAllByOrderByIdAsc();
}
