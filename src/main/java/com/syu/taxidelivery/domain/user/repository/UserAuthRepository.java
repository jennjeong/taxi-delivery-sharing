package com.syu.taxidelivery.domain.user.repository;

import com.syu.taxidelivery.domain.user.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
}