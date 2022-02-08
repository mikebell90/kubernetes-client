/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.java.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import io.fabric8.java.generator.nodes.*;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import java.util.*;
import org.junit.jupiter.api.Test;

class GeneratorTest {

    static final Config defaultConfig = new Config();

    @Test
    void testCorrectInterpolationOfPackage() {
        // Arrange
        CRGeneratorRunner runner = new CRGeneratorRunner(defaultConfig);

        // Act
        String packageName = runner.getPackage("test.org");

        // Assert
        assertEquals("org.test", packageName);
    }

    @Test
    void testCR() {
        // Arrange
        JCRObject cro =
                new JCRObject(
                        "v1alpha1",
                        "t",
                        "g",
                        "v",
                        "Spec",
                        "Status",
                        true,
                        true,
                        true,
                        true,
                        defaultConfig);

        // Act
        GeneratorResult res = cro.generateJava();

        // Assert
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("t", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testCRWithoutNamespace() {
        // Arrange
        JCRObject cro =
                new JCRObject(
                        null,
                        "t",
                        "g",
                        "v",
                        "Spec",
                        "Status",
                        true,
                        true,
                        true,
                        true,
                        defaultConfig);

        // Act
        GeneratorResult res = cro.generateJava();

        // Assert
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("t", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testPrimitive() {
        // Arrange
        JPrimitive primitive = new JPrimitive("test", defaultConfig, null);

        // Act
        GeneratorResult res = primitive.generateJava();

        // Assert
        assertEquals("test", primitive.getType());
        assertEquals(0, res.getTopLevelClasses().size());
    }

    @Test
    void testArrayOfPrimitives() {
        // Arrange
        JArray array =
                new JArray(new JPrimitive("primitive", defaultConfig, null), defaultConfig, null);

        // Act
        GeneratorResult res = array.generateJava();

        // Assert
        assertEquals("java.util.List<primitive>", array.getType());
        assertEquals(0, res.getTopLevelClasses().size());
    }

    @Test
    void testMapOfPrimitives() {
        // Arrange
        JMap map = new JMap(new JPrimitive("primitive", defaultConfig, null), defaultConfig, null);

        // Act
        GeneratorResult res = map.generateJava();

        // Assert
        assertEquals("java.util.Map<java.lang.String, primitive>", map.getType());
        assertEquals(0, res.getTopLevelClasses().size());
    }

    @Test
    void testEmptyObject() {
        // Arrange
        JObject obj = new JObject("v1alpha1", "t", null, null, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals("v1alpha1.T", obj.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testEmptyObjectWithSuffix() {
        // Arrange
        Config config = new Config(null, null, Config.Suffix.ALWAYS, null, null, null);
        JObject obj = new JObject("v1alpha1", "t", null, null, false, "", "MySuffix", config, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals("v1alpha1.TMySuffix", obj.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("TMySuffix", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testEmptyObjectWithoutNamespace() {
        // Arrange
        JObject obj = new JObject(null, "t", null, null, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals("T", obj.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testObjectOfPrimitives() {
        // Arrange
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newBool = new JSONSchemaProps();
        newBool.setType("boolean");
        props.put("o1", newBool);
        JObject obj = new JObject("v1alpha1", "t", props, null, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals("v1alpha1.T", obj.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());

        Optional<ClassOrInterfaceDeclaration> clz =
                res.getTopLevelClasses().get(0).getCompilationUnit().getClassByName("T");
        assertTrue(clz.isPresent());
        assertEquals(1, clz.get().getFields().size());
        assertTrue(clz.get().getFieldByName("o1").isPresent());
    }

    @Test
    void testObjectWithRequiredField() {
        // Arrange
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newBool = new JSONSchemaProps();
        newBool.setType("boolean");
        props.put("o1", newBool);
        List<String> req = new ArrayList<>(1);
        req.add("o1");
        JObject obj = new JObject("v1alpha1", "t", props, req, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        Optional<ClassOrInterfaceDeclaration> clz =
                res.getTopLevelClasses().get(0).getCompilationUnit().getClassByName("T");
        assertTrue(clz.get().getFieldByName("o1").get().getAnnotationByName("NotNull").isPresent());
    }

    @Test
    void testDefaultEnum() {
        // Arrange
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newEnum = new JSONSchemaProps();
        newEnum.setType("string");
        List<JsonNode> enumValues = new ArrayList<>();
        enumValues.add(new TextNode("foo"));
        enumValues.add(new TextNode("bar"));
        enumValues.add(new TextNode("baz"));
        props.put("e1", newEnum);
        JEnum enu = new JEnum("t", enumValues, defaultConfig, null);

        // Act
        GeneratorResult res = enu.generateJava();

        // Assert
        assertEquals("T", enu.getType());
        assertEquals(1, res.getInnerClasses().size());
        assertEquals("T", res.getInnerClasses().get(0).getName());

        Optional<EnumDeclaration> en =
                res.getInnerClasses().get(0).getCompilationUnit().getEnumByName("T");
        assertTrue(en.isPresent());
        assertEquals(3, en.get().getEntries().size());
        assertEquals("FOO", en.get().getEntries().get(0).getName().asString());
        assertEquals("BAR", en.get().getEntries().get(1).getName().asString());
        assertEquals("BAZ", en.get().getEntries().get(2).getName().asString());
    }

    @Test
    void testNotUppercaseEnum() {
        // Arrange
        CompilationUnit cu = new CompilationUnit();
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newEnum = new JSONSchemaProps();
        newEnum.setType("string");
        List<JsonNode> enumValues = new ArrayList<>();
        enumValues.add(new TextNode("foo"));
        enumValues.add(new TextNode("bar"));
        enumValues.add(new TextNode("baz"));
        props.put("e1", newEnum);
        JEnum enu =
                new JEnum("t", enumValues, new Config(false, null, null, null, null, null), null);

        // Act
        GeneratorResult res = enu.generateJava();

        // Assert
        assertEquals("T", enu.getType());
        assertEquals(1, res.getInnerClasses().size());
        assertEquals("T", res.getInnerClasses().get(0).getName());

        Optional<EnumDeclaration> en =
                res.getInnerClasses().get(0).getCompilationUnit().getEnumByName("T");
        assertTrue(en.isPresent());
        assertEquals(3, en.get().getEntries().size());
        assertEquals("foo", en.get().getEntries().get(0).getName().asString());
        assertEquals("bar", en.get().getEntries().get(1).getName().asString());
        assertEquals("baz", en.get().getEntries().get(2).getName().asString());
    }

    @Test
    void testArrayOfObjects() {
        // Arrange
        JArray array =
                new JArray(
                        new JObject(null, "t", null, null, false, "", "", defaultConfig, null),
                        defaultConfig,
                        null);

        // Act
        GeneratorResult res = array.generateJava();

        // Assert
        assertEquals("java.util.List<T>", array.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testMapOfObjects() {
        // Arrange
        JMap map =
                new JMap(
                        new JObject(null, "t", null, null, false, "", "", defaultConfig, null),
                        defaultConfig,
                        null);

        // Act
        GeneratorResult res = map.generateJava();

        // Assert
        assertEquals("java.util.Map<java.lang.String, T>", map.getType());
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());
    }

    @Test
    void testObjectOfObjects() {
        // Arrange
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newObj = new JSONSchemaProps();
        newObj.setType("object");
        props.put("o1", newObj);
        JObject obj = new JObject(null, "t", props, null, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals(2, res.getTopLevelClasses().size());
        assertEquals("O1", res.getTopLevelClasses().get(0).getName());
        assertEquals("T", res.getTopLevelClasses().get(1).getName());

        Optional<ClassOrInterfaceDeclaration> clzT =
                res.getTopLevelClasses().get(1).getCompilationUnit().getClassByName("T");
        assertTrue(clzT.isPresent());
        assertEquals(1, clzT.get().getFields().size());
        assertTrue(clzT.get().getFieldByName("o1").isPresent());
        Optional<ClassOrInterfaceDeclaration> clzO1 =
                res.getTopLevelClasses().get(0).getCompilationUnit().getClassByName("O1");
        assertTrue(clzO1.isPresent());
    }

    @Test
    void testObjectOfObjectsWithTopLevelPrefix() {
        // Arrange
        Config config = new Config(null, Config.Prefix.TOP_LEVEL, null, null, null, null);
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newObj = new JSONSchemaProps();
        newObj.setType("object");
        props.put("o1", newObj);
        JObject obj = new JObject(null, "t", props, null, false, "My", "", config, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals(2, res.getTopLevelClasses().size());
        assertEquals("O1", res.getTopLevelClasses().get(0).getName());
        assertEquals("MyT", res.getTopLevelClasses().get(1).getName());
    }

    @Test
    void testObjectOfObjectsWithAlwaysPrefix() {
        // Arrange
        Config config = new Config(null, Config.Prefix.ALWAYS, null, null, null, null);
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newObj = new JSONSchemaProps();
        newObj.setType("object");
        props.put("o1", newObj);
        JObject obj = new JObject(null, "t", props, null, false, "My", "", config, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals(2, res.getTopLevelClasses().size());
        assertEquals("MyO1", res.getTopLevelClasses().get(0).getName());
        assertEquals("MyT", res.getTopLevelClasses().get(1).getName());
    }

    @Test
    void testObjectWithPreservedFields() {
        // Arrange
        JObject obj = new JObject(null, "t", null, null, true, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals(1, res.getTopLevelClasses().size());
        assertEquals("T", res.getTopLevelClasses().get(0).getName());

        Optional<ClassOrInterfaceDeclaration> clzT =
                res.getTopLevelClasses().get(0).getCompilationUnit().getClassByName("T");
        assertTrue(clzT.isPresent());
        assertTrue(clzT.get().getFieldByName("additionalProperties").isPresent());
    }

    @Test
    void testClassNamesDisambiguationWithPackageNesting() {
        // Arrange
        Map<String, JSONSchemaProps> props = new HashMap<>();
        JSONSchemaProps newObj1 = new JSONSchemaProps();
        JSONSchemaProps newObj2 = new JSONSchemaProps();
        newObj1.setType("object");
        newObj2.setType("object");
        Map<String, JSONSchemaProps> obj2Props = new HashMap<>();
        obj2Props.put("o1", newObj1);
        obj2Props.put("o2", newObj1);
        obj2Props.put("o3", newObj1);
        newObj2.setProperties(obj2Props);
        props.put("o1", newObj1);
        props.put("o2", newObj2);
        JObject obj = new JObject("v1alpha1", "t", props, null, false, "", "", defaultConfig, null);

        // Act
        GeneratorResult res = obj.generateJava();

        // Assert
        assertEquals(6, res.getTopLevelClasses().size());
        // The order here is not important
        assertEquals("O1", res.getTopLevelClasses().get(0).getName());
        assertEquals("O1", res.getTopLevelClasses().get(1).getName());
        assertEquals("O2", res.getTopLevelClasses().get(2).getName());
        assertEquals("O3", res.getTopLevelClasses().get(3).getName());
        assertEquals("O2", res.getTopLevelClasses().get(4).getName());
        assertEquals("T", res.getTopLevelClasses().get(5).getName());

        CompilationUnit cuT = res.getTopLevelClasses().get(5).getCompilationUnit();
        Optional<ClassOrInterfaceDeclaration> clzT = cuT.getClassByName("T");
        assertTrue(clzT.isPresent());
        assertEquals(2, clzT.get().getFields().size());
        assertTrue(clzT.get().getFieldByName("o1").isPresent());
        assertEquals("v1alpha1", cuT.getPackageDeclaration().get().getNameAsString());
        assertEquals(
                "v1alpha1.t.O1", clzT.get().getFieldByName("o1").get().getElementType().toString());
        CompilationUnit cuO1 = res.getTopLevelClasses().get(0).getCompilationUnit();
        Optional<ClassOrInterfaceDeclaration> clzO1 = cuO1.getClassByName("O1");
        assertTrue(clzO1.isPresent());
        assertEquals("v1alpha1.t", cuO1.getPackageDeclaration().get().getNameAsString());
        CompilationUnit cuO2 = res.getTopLevelClasses().get(4).getCompilationUnit();
        Optional<ClassOrInterfaceDeclaration> clzO2 = cuO2.getClassByName("O2");
        assertTrue(clzO2.isPresent());
        assertTrue(clzO2.get().getFieldByName("o1").isPresent());
        assertEquals("v1alpha1.t", cuO2.getPackageDeclaration().get().getNameAsString());
        assertEquals(
                "v1alpha1.t.o2.O1",
                clzO2.get().getFieldByName("o1").get().getElementType().toString());
        assertTrue(clzO2.get().getFieldByName("o2").isPresent());
        assertTrue(clzO2.get().getFieldByName("o3").isPresent());
    }
}
