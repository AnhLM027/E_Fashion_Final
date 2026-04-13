package org.example.e_fashion.service;

import org.example.e_fashion.dto.response.FeaturedProductResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface FeaturedProductService {

    public List<FeaturedProductResponseDTO> getFeatured(String type);
}