package com.sky.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MultiDelayMessageDTO {

    private Long orderId;
    private List<Integer> delay;

}
