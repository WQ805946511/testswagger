package com.example.testswagger;

import io.swagger.codegen.languages.JavaClientCodegen;

public class TestAutoCodegen extends JavaClientCodegen {

    public TestAutoCodegen() {
        super.apiPackage = "com.example.haha";
        super.modelPackage = "com.example.haha";
    }
}
