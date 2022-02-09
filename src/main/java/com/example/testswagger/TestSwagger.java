package com.example.testswagger;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.ClientOpts;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.codegen.languages.JavaClientCodegen;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSwagger extends DefaultGenerator {
    public static void main(String[] args) throws IOException {
        String restYamlPath = "src/main/resources/swagger/test.yml";
        String swaggerInfo = FileUtils.readFileToString(new File(restYamlPath), "UTF-8");

        // 将文件解析成swagger
        Swagger swagger = new SwaggerParser().parse(swaggerInfo);

        // 设置自动生成文件的目录
        JavaClientCodegen javaClientCodegen = new JavaClientCodegen();
        javaClientCodegen.setApiPackage("com.example.controller");
        javaClientCodegen.setModelPackage("com.example.controller");

        ClientOptInput clientOptInput = new ClientOptInput().opts(new ClientOpts()).swagger(swagger);
        clientOptInput.setConfig(javaClientCodegen);

        Test02 testSwagger = new Test02();
        testSwagger.opts(clientOptInput).generate();
    }

    @Override
    public List<File> generate() {

        // 再获取restful配置的下的tag
        Map<String, Path> pathsMap = swagger.getPaths();
        Map<String, List<String>> tagPathsMap = new HashMap<>();
        pathsMap.forEach((key, val) -> {
            Operation operation = val.getGet();
            // 这边居然可以配置多个？？？
            List<String> tagList = operation.getTags();
            tagList.forEach(tag -> {
                if (tagPathsMap.containsKey(tag)) {
                    // 如果包含这个tag，则在list里面添加
                    List<String> tags = tagPathsMap.get(tag);
                    tags.add(key);
                } else {
                    // 如果不包含，则加入
                    List<String> tags = new ArrayList<>();
                    tags.add(key);
                    tagPathsMap.put(tag, tags);
                }
            });
        });
        // 按照tag归类后，将请求的url进行公共部分合并处理
        List<ApiDefinition> apiDefinitionList = new ArrayList<>();
        tagPathsMap.forEach((className, tags) -> {
            ApiDefinition apiDefinition = new ApiDefinition();
            apiDefinition.setApiPackage(config.apiPackage());
            apiDefinition.setModelPackage(config.modelPackage());
            apiDefinition.setServicePackage("com.example.controller");
            apiDefinition.setClassName(className);

            List<Map<String, Object>> infoList = new ArrayList<>();
            List<Map<String, String>> importList = new ArrayList<>();

            tags.forEach(urlName -> {
                Map<String, Object> infoMap = new HashMap<>();
                Path path = pathsMap.get(urlName);
                infoMap.put("urlName", urlName);
                infoMap.put("path", path);
                infoList.add(infoMap);

                List<Parameter> parameterList;
                Map<String, String> typeMap = config.typeMapping();
                // 这边区分方法时get，post，put，delete
                if (path.getGet() != null) {
                    infoMap.put("hasGet", true);
                    parameterList = path.getGet().getParameters();
                    // 将设置的参数类型转为大写类型
                    for (Parameter parameter : parameterList) {
                        PathParameter pathParameter = (PathParameter) parameter;
                        pathParameter.setType(typeMap.get(pathParameter.getType()));
                    }
                    Property property = path.getGet().getResponses().get("200").getSchema();
                    if (property != null) {
                        if (property instanceof RefProperty) {
                            RefProperty refProperty = (RefProperty) property;
                            infoMap.put("responseType", refProperty.getSimpleRef());
                            Map<String, String> map = new HashMap<>();
                            map.put("import", config.modelPackage() + "." + refProperty.getSimpleRef());
                            importList.add(map);
                        }
                        if (property instanceof StringProperty) {
                            StringProperty stringProperty = (StringProperty) property;
                            infoMap.put("responseType", typeMap.get(stringProperty.getType()));
                        }
                    }
                }
                if (path.getPost() != null) {
                    infoMap.put("hasPost", true);
                    parameterList = path.getPost().getParameters();
                    // 将设置的参数类型转为大写类型
                    for (Parameter parameter : parameterList) {
                        PathParameter pathParameter = (PathParameter) parameter;
                        pathParameter.setType(typeMap.get(pathParameter.getType()));
                    }
                    Property property = path.getGet().getResponses().get("200").getSchema();
                    if (property != null) {
                        if (property instanceof RefProperty) {
                            RefProperty refProperty = (RefProperty) property;
                            infoMap.put("responseType", refProperty.getSimpleRef());
                            Map<String, String> map = new HashMap<>();
                            map.put("import", config.modelPackage() + "." + refProperty.getSimpleRef());
                            importList.add(map);
                        }
                        if (property instanceof StringProperty) {
                            StringProperty stringProperty = (StringProperty) property;
                            infoMap.put("responseType", typeMap.get(stringProperty.getType()));
                        }
                    }
                }
                if (path.getPut() != null) {
                    infoMap.put("hasPut", true);
                }
                if (path.getDelete() != null) {
                    infoMap.put("hasDelete", true);
                }



            });

            apiDefinition.setInfoList(infoList);
            apiDefinition.setImportList(importList);

            apiDefinitionList.add(apiDefinition);
        });

        for (ApiDefinition info : apiDefinitionList) {
            String apiOutputFilePath = String.join("", "src/main/java/com/example/controller/", info.getClassName(), ".java");
            String apiTemplateFilePath = String.join("", "src/main/resources/controller.mustache");
            String templateFileInfo = "";
            try {
                templateFileInfo = FileUtils.readFileToString(new File(apiTemplateFilePath), "UTF-8");
                Template template = Mustache.compiler().compile(templateFileInfo);

                //解析模板
                Map<String, String> importMap = new HashMap<>();
                importMap.put("import", "com.example.service." + info.getClassName() + "Service");
                info.getImportList().add(importMap);
                String result = template.execute(info);
                //生成service接口文件
                FileUtils.writeStringToFile(new File(apiOutputFilePath), result, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


/*
        // 获取到全部的实体类
        List<BeanDefinition> beanDefinitionList = new ArrayList<>();
        Map<String, Model> definitionsMap = swagger.getDefinitions();
        definitionsMap.forEach((key, val) -> {
            List<Map<String, String>> fieldList = new ArrayList<>();
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setClassName(key);
            beanDefinition.setModelPackage(config.modelPackage());
            Map<String, Property> propertiesMap = val.getProperties();
            propertiesMap.forEach((key1, val1) -> {
                Map<String, String> fieldMap = new HashMap<>();
                Map<String, String> stringStringMap = config.typeMapping();
                String s = stringStringMap.get(val1.getType());
                if (StringUtils.isEmpty(s)) {
                    // 异常
                }
                fieldMap.put("type", s);
                fieldMap.put("field", key1);
                fieldList.add(fieldMap);
            });
            beanDefinition.setFieldList(fieldList);
            beanDefinitionList.add(beanDefinition);
        });
        beanDefinitionList.forEach(beanDefinition -> {
            String beanPath = "src/main/resources/bean.mustache";
            try {
                String templateFileInfo = FileUtils.readFileToString(new File(beanPath), "UTF-8");
                String replace = StringUtils.replace(beanDefinition.getModelPackage(), ".", "/");
                String join = String.join("", "src/main/java/", replace, "/", beanDefinition.className, ".java");
                File file = new File(join);

                Template template = Mustache.compiler().compile(templateFileInfo);
                String result = template.execute(beanDefinition);
                FileUtils.writeStringToFile(file, result, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/

        return new ArrayList<File>();
    }


    private void changeTypeAndGetClassName(Path path, Map<String, Object> infoMap, List<Map<String, String>> importList) {

    }
}
