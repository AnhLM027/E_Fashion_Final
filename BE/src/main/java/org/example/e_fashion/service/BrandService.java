package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.BrandRequestDTO;
import org.example.e_fashion.entity.BrandEntity;

import java.util.List;

public interface BrandService {

    BrandEntity create(BrandRequestDTO request);

    BrandEntity update(String id, BrandRequestDTO request);

    List<BrandEntity> getAll();

    BrandEntity getById(String id);

    void delete(String id);
}