package org.example.e_fashion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        productService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        productService.restore(id);
        return ResponseEntity.ok().build();
    }
}
