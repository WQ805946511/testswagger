package com.example.testswagger;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Test02 extends DefaultGenerator {
    @Override
    public List<File> generate() {
        Map<String, Path> pathsMap = swagger.getPaths();
        Map<String, String> typeMap = config.typeMapping();

        // 这边得到全部需要生成的controller类
        Map<String, List<String>> tagPathsMap = getTagPathsMap(pathsMap);

        // 获取需要生成的controller
        List<ApiDefinition> apiDefinitionList = getApiDefinitionList(pathsMap, typeMap, tagPathsMap);

        for (ApiDefinition info : apiDefinitionList) {
            String apiOutputFilePath = String.join("", "src/main/java/com/example/controller/", info.getClassName(),
                    ".java");
            File apiOutputFile = new File(apiOutputFilePath);
            String apiTemplateFilePath = String.join("", "src/main/resources/controller.mustache");
            File apiTemplateFile = new File(apiTemplateFilePath);
            try {
                String templateFileInfo = FileUtils.readFileToString(apiTemplateFile, "UTF-8");
                Template template = Mustache.compiler().compile(templateFileInfo);

                Map<String, String> importMap = new HashMap<>();
                importMap.put("import", "com.example.service." + info.getClassName() + "Service");
                info.getImportList().add(importMap);
                String result = template.execute(info);

                //FileUtils.forceDeleteOnExit(apiOutputFile);

                FileUtils.writeStringToFile(apiOutputFile, result, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<File>();
    }

    private List<ApiDefinition> getApiDefinitionList(Map<String, Path> pathsMap, Map<String, String> typeMap,
                                                     Map<String, List<String>> tagPathsMap) {
        List<ApiDefinition> apiDefinitionList = new ArrayList<>();
        tagPathsMap.forEach((className, tags) -> {
            ApiDefinition apiDefinition = new ApiDefinition();
            apiDefinition.setApiPackage(config.apiPackage());
            apiDefinition.setModelPackage(config.modelPackage());
            apiDefinition.setServicePackage("com.example.controller");
            apiDefinition.setClassName(className);

            List<Map<String, Object>> infoList = new ArrayList<>();
            Set<Map<String, String>> importList = new HashSet<>();

            tags.forEach(urlName -> {
                Map<String, Object> infoMap = new HashMap<>();
                Path path = pathsMap.get(urlName);
                // 设置参数类型和返回类型
                setHasShow(typeMap, importList, infoMap, path);

                infoMap.put("urlName", urlName);
                infoMap.put("path", path);
                infoList.add(infoMap);
            });

            apiDefinition.setInfoList(infoList);
            apiDefinition.setImportList(new ArrayList<>(importList));
            apiDefinitionList.add(apiDefinition);
        });
        return apiDefinitionList;
    }

    private void setHasShow(Map<String, String> typeMap,Set<Map<String, String>> importList,
                            Map<String, Object> infoMap, Path path) {
        if (path.getGet() != null) {
            infoMap.put("hasGet", true);
            infoMap.put("operationId", path.getGet().getOperationId());
            List<Parameter> parameterList = path.getGet().getParameters();
            Property property = path.getGet().getResponses().get("200").getSchema();
            handTypesAndReturn(typeMap, importList, infoMap, parameterList, property);
        }
        if (path.getPost() != null) {
            infoMap.put("hasPost", true);
            List<Parameter> parameterList = path.getPost().getParameters();
            Property property = path.getPost().getResponses().get("200").getSchema();
            handTypesAndReturn(typeMap, importList, infoMap, parameterList, property);
        }
        if (path.getPut() != null) {
            infoMap.put("hasPut", true);
            List<Parameter> parameterList = path.getPut().getParameters();
            Property property = path.getPut().getResponses().get("200").getSchema();
            handTypesAndReturn(typeMap, importList, infoMap, parameterList, property);
        }
        if (path.getDelete() != null) {
            infoMap.put("hasDelete", true);
            List<Parameter> parameterList = path.getDelete().getParameters();
            Property property = path.getDelete().getResponses().get("200").getSchema();
            handTypesAndReturn(typeMap, importList, infoMap, parameterList, property);
        }
    }

    private void handTypesAndReturn(Map<String, String> typeMap, Set<Map<String, String>> importList, Map<String,
            Object> infoMap, List<Parameter> parameterList, Property property) {
        // 将设置的参数类型转为大写类型
       /* for (Parameter parameter : parameterList) {
            PathParameter pathParameter = (PathParameter) parameter;
            pathParameter.setType(typeMap.get(pathParameter.getType()));
        }*/

        List<PathParameter> pathList =
                parameterList.stream().filter(parameter -> StringUtils.equalsIgnoreCase(parameter.getIn(), "path"))
                        .map(parameter -> {
                            PathParameter pathParameter = (PathParameter) parameter;
                            pathParameter.setType(typeMap.get(pathParameter.getType()));
                            return pathParameter;
                        }).collect(Collectors.toList());
        List<Parameter> bodyList =
                parameterList.stream().filter(parameter -> StringUtils.equalsIgnoreCase(parameter.getIn(), "body")).
                        map(parameter -> {
                            BodyParameter bodyParameter = (BodyParameter) parameter;
                            Model model = bodyParameter.getSchema();
                            if (model != null) {
                                if (model instanceof RefModel) {
                                    RefModel refModel = (RefModel) model;
                                    infoMap.put("requestType", refModel.getSimpleRef());
                                    Map<String, String> map = new HashMap<>();
                                    map.put("import", config.modelPackage() + "." + refModel.getSimpleRef());
                                    importList.add(map);
                                }
                                if (model instanceof ModelImpl) {
                                    ModelImpl modelImpl = (ModelImpl) model;
                                    infoMap.put("requestType", typeMap.get(modelImpl.getType()));
                                }
                            }
                            return bodyParameter;
                        }).collect(Collectors.toList());
        List<Parameter> queryList =
                parameterList.stream().filter(parameter -> StringUtils.equalsIgnoreCase(parameter.getIn(), "query")).map(parameter -> {
                    QueryParameter pathParameter = (QueryParameter) parameter;
                    pathParameter.setType(typeMap.get(pathParameter.getType()));
                    return pathParameter;
                }).collect(Collectors.toList());

        infoMap.put("pathList", pathList);
        infoMap.put("bodyList", bodyList);
        infoMap.put("queryList", queryList);
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

    private Map<String, List<String>> getTagPathsMap(Map<String, Path> pathsMap) {
        Map<String, List<String>> tagPathsMap = new HashMap<>();
        pathsMap.forEach((urlName, path) -> {
            if (path.getGet() != null) {
                List<String> tagList = path.getGet().getTags();
                processTagPathsMap(tagPathsMap, urlName, tagList);
            }
            if (path.getPost() != null) {
                List<String> tagList = path.getPost().getTags();
                processTagPathsMap(tagPathsMap, urlName, tagList);
            }
            if (path.getPut() != null) {
                List<String> tagList = path.getPut().getTags();
                processTagPathsMap(tagPathsMap, urlName, tagList);
            }
            if (path.getDelete() != null) {
                List<String> tagList = path.getDelete().getTags();
                processTagPathsMap(tagPathsMap, urlName, tagList);
            }
        });
        return tagPathsMap;
    }

    private void processTagPathsMap(Map<String, List<String>> tagPathsMap, String urlName, List<String> tagList) {
        tagList.forEach(tag -> {
            if (tagPathsMap.containsKey(tag)) {
                // 如果包含这个tag，则在list里面添加
                List<String> tags = tagPathsMap.get(tag);
                tags.add(urlName);
            } else {
                // 如果不包含，则加入
                List<String> tags = new ArrayList<>();
                tags.add(urlName);
                tagPathsMap.put(tag, tags);
            }
        });
    }
}
