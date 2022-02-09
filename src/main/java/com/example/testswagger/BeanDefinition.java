package com.example.testswagger;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BeanDefinition {
    public String modelPackage;
    public String className;
    public List<Map<String, String>> importList;
    public List<Map<String, Object>> infoList;
    public List<Map<String, String>> fieldList;
}