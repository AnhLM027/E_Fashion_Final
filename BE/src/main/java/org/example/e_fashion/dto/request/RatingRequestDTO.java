package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingRequestDTO {

    private String orderItemId;

    private Integer rating;

    private String reviewText;
}