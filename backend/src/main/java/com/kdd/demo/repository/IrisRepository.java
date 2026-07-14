package com.kdd.demo.repository;

import com.kdd.demo.entity.IrisSample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IrisRepository extends JpaRepository<IrisSample, Integer> {
}
