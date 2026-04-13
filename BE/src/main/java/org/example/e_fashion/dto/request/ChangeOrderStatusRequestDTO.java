package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeOrderStatusRequestDTO {

    private String newStatus;   // PROCESSING, SHIPPED...
    private String note;
    private String changedBy;   // user/admin id
}