package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_attributes")
@Getter
@Setter
public class ProductAttributeEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "attribute_name", length = 100)
    private String attributeName;

    @Column(name = "attribute_value", length = 255)
    private String attributeValue;
}