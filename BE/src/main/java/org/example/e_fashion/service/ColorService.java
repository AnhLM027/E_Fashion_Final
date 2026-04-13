package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ColorRequestDTO;
import org.example.e_fashion.dto.response.ColorResponseDTO;

import java.util.List;

public interface ColorService {

    ColorResponseDTO create(ColorRequestDTO request);

    ColorResponseDTO update(String id, ColorRequestDTO request);

    void delete(String id); // soft delete

    List<ColorResponseDTO> getAllActive();
}