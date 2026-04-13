package org.example.e_fashion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.AdminUserUpdateRequestDTO;
import org.example.e_fashion.dto.response.AdminUserResponseDTO;
import org.example.e_fashion.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserResponseDTO> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return adminUserService.getUsers(search, role, active, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserResponseDTO getDetail(@PathVariable String id) {
        return adminUserService.getUserDetail(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponseDTO update(
            @PathVariable String id,
            @RequestBody AdminUserUpdateRequestDTO request) {
        return adminUserService.updateUser(id, request);
    }

    @PutMapping("/{id}/deactivate")
    public void deactivate(@PathVariable String id) {
        adminUserService.deactivateUser(id);
    }

    @PutMapping("/{id}/activate")
    public void activate(@PathVariable String id) {
        adminUserService.activateUser(id);
    }
}