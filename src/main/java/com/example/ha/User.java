package com.example.ha;

import lombok.Data;

import java.io.Serializable;


@Data
public class User implements Serializable {
    private Integer id;
    private Integer petId;
    private Integer quantity;
    private String shipDate;
    private Boolean complete;
}