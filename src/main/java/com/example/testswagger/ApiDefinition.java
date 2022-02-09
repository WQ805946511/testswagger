package com.example.testswagger;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ApiDefinition {
    private String apiPackage;
    private String modelPackage;
    private String servicePackage;
    private String basePath;
    private String className;
    private List<Map<String, String>> importList;
    private List<Map<String, Object>> infoList;
}