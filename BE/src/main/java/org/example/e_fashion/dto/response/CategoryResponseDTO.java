package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CategoryResponseDTO {
    private String id;
    private String name;
    private String slug;
    private String parentId;
    private String imageUrl;
    private Boolean isActive;

    private List<CategoryResponseDTO> children = new ArrayList<>();
}
