package org.example.e_fashion.repository;

import org.example.e_fashion.entity.UserCouponEntity;
import org.example.e_fashion.entity.UserCouponId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository
        extends JpaRepository<UserCouponEntity, UserCouponId> {

    Optional<UserCouponEntity>
    findByUser_IdAndCoupon_Code(String userId, String code);

    List<UserCouponEntity>
    findByUser_Id(String userId);

    long countByCoupon_CodeAndIsUsedTrue(String code);
}