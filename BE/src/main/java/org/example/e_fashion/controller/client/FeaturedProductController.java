package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.service.FeaturedProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class FeaturedProductController {

    private final FeaturedProductService service;

    @GetMapping("/featured")
    public List<?> getFeatured(@RequestParam String type) {
        return service.getFeatured(type);
    }
}