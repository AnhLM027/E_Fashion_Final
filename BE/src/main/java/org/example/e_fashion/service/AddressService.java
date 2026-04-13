package org.example.e_fashion.service;


import org.example.e_fashion.dto.request.AddressRequestDTO;
import org.example.e_fashion.entity.AddressEntity;

import java.util.List;

public interface AddressService {
    AddressEntity addUserAddress(String userId, AddressRequestDTO addressRequestDTO);
    List<AddressEntity> getUserAddresses(String userId);
    AddressEntity getUserAddressById(String userId, String addressId);
    AddressEntity updateUserAddress(String userId, String addressId, AddressRequestDTO request);
    void deleteUserAddress(String userId, String addressId);
    void setDefaultAddress(String userId, String addressId);
}
