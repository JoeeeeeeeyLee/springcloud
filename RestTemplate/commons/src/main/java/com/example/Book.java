package com.example;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Book implements Serializable {
    private String name;
    private int price;
    private String author;
    private String publisher;
}
