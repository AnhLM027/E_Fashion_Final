package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ColorRequestDTO;
import org.example.e_fashion.dto.response.ColorResponseDTO;
import org.example.e_fashion.service.ColorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/colors")
@RequiredArgsConstructor
public class ColorStaffController {

    private final ColorService service;

    @PostMapping
    public ColorResponseDTO create(@RequestBody ColorRequestDTO request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ColorResponseDTO update(@PathVariable String id,
                                   @RequestBody ColorRequestDTO request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @GetMapping
    public List<ColorResponseDTO> getAll() {
        return service.getAllActive();
    }
}
