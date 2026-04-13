package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.AddressRequestDTO;
import org.example.e_fashion.entity.AddressEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.AddressMapper;
import org.example.e_fashion.repository.AddressRepository;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final UserRepository userRepository;

    @Override
    public AddressEntity addUserAddress(String userId, AddressRequestDTO addressRequestDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        AddressEntity address = addressMapper.toEntity(addressRequestDTO);
        address.setUser(user);

        boolean isFirstAddress = !addressRepository.existsByUserId(userId);

        if (isFirstAddress) {
            address.setIsDefault(true);
        } else if (Boolean.TRUE.equals(addressRequestDTO.getIsDefault())) {
            unsetDefaultAddresses(userId);
            address.setIsDefault(true);
        }

        return addressRepository.save(address);
    }

    @Override
    public List<AddressEntity> getUserAddresses(String userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    public AddressEntity getUserAddressById(String userId, String addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    @Override
    public AddressEntity updateUserAddress(
            String userId,
            String addressId,
            AddressRequestDTO request) {

        AddressEntity address = getUserAddressById(userId, addressId);

        addressMapper.updateEntity(address, request);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetDefaultAddresses(userId);
            address.setIsDefault(true);
        }

        return addressRepository.save(address);
    }

    @Override
    public void deleteUserAddress(String userId, String addressId) {
        AddressEntity address = getUserAddressById(userId, addressId);
        addressRepository.delete(address);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<AddressEntity> remain = addressRepository.findByUserId(userId);
            if (!remain.isEmpty()) {
                remain.get(0).setIsDefault(true);
                addressRepository.save(remain.get(0));
            }
        }
    }

    @Override
    public void setDefaultAddress(String userId, String addressId) {
        AddressEntity address = getUserAddressById(userId, addressId);
        unsetDefaultAddresses(userId);

        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private void unsetDefaultAddresses(String userId) {
        List<AddressEntity> defaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);

        defaults.forEach(addr -> addr.setIsDefault(false));
        addressRepository.saveAll(defaults);
    }
}
