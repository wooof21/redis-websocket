package com.example.queue;

import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrder {
    private int id;
    private Category category;
}
