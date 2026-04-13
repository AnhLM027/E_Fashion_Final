package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.AddressRequestDTO;
import org.example.e_fashion.dto.response.AddressResponseDTO;
import org.example.e_fashion.entity.AddressEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.AddressMapper;
import org.example.e_fashion.service.AddressService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
public class AddressCustomerController {
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final ExtractUserUtils extractUserUtils;

    @PostMapping
    public ResponseEntity<AddressResponseDTO> addUserAddress(
            HttpServletRequest request,
            @Valid @RequestBody AddressRequestDTO requestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);

        AddressEntity address =
                addressService.addUserAddress(user.getId(), requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(addressMapper.toResponse(address));
    }

    @GetMapping
    public List<AddressResponseDTO> getUserAddresses(
            HttpServletRequest request
    ) {
        UserEntity user = extractUserUtils.extract(request);

        return addressService.getUserAddresses(user.getId())
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @GetMapping("/{addressId}")
    public AddressResponseDTO getAddressDetail(
            HttpServletRequest request,
            @PathVariable String addressId
    ) {
        UserEntity user = extractUserUtils.extract(request);

        AddressEntity address =
                addressService.getUserAddressById(user.getId(), addressId);

        return addressMapper.toResponse(address);
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<Void> setDefaultAddress(
            HttpServletRequest request,
            @PathVariable String addressId
    ) {
        UserEntity user = extractUserUtils.extract(request);

        addressService.setDefaultAddress(user.getId(), addressId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteUserAddress(
            HttpServletRequest request,
            @PathVariable String addressId
    ) {
        UserEntity user = extractUserUtils.extract(request);

        addressService.deleteUserAddress(user.getId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
