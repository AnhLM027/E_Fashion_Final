package org.example.e_fashion.dto.response;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeDTO {

    private String id;
    private String name;
    private String slug;
    private String parentId;
    private String imageUrl;
    private Boolean isActive;

    private List<CategoryTreeDTO> children = new ArrayList<>();
}