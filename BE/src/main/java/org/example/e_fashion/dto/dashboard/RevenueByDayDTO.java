package org.example.e_fashion.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Getter
@AllArgsConstructor
public class RevenueByDayDTO {
    private LocalDate date;
    private BigDecimal revenue;
}