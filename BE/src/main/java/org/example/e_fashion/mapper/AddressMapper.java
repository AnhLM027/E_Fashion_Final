package org.example.e_fashion.mapper;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.AddressRequestDTO;
import org.example.e_fashion.dto.response.AddressResponseDTO;
import org.example.e_fashion.entity.AddressEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressMapper {
    private final ModelMapper modelMapper;

    public AddressEntity toEntity(AddressRequestDTO addressRequestDTO) {
        return modelMapper.map(addressRequestDTO, AddressEntity.class);
    }

    public AddressResponseDTO toResponse(AddressEntity addressEntity) {
        return modelMapper.map(addressEntity, AddressResponseDTO.class);
    }

    public void updateEntity(AddressEntity entity, AddressRequestDTO dto) {
        modelMapper.map(dto, entity);
    }
}
