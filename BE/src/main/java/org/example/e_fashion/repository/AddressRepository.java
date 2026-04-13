package org.example.e_fashion.repository;

import org.example.e_fashion.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, String> {
    List<AddressEntity> findByUserId(String userId);

    Optional<AddressEntity> findByIdAndUserId(String id, String userId);

    boolean existsByUserId(String userId);

    List<AddressEntity> findByUserIdAndIsDefaultTrue(String userId);
}
