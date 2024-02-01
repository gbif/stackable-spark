/*
 * Copyright 2023 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.stackable;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.SneakyThrows;

/**
 * This test class basically compiles the examples provided by Stackable. Most of them found at this
 * <a href="https://docs.stackable.tech/home/stable/spark-k8s/usage-guide/examples.html">page</a>.
 */
class SparkCrdTest {

  private static final String TEST_FILE = "spark-cdrs.yaml";

  @Test
  @SneakyThrows
  void externalResourcesSerDeSerTest() {
    readAllSparkCrds(TEST_FILE)
        .forEach(
            sparkCrd -> {
              String sparkCdrYaml = sparkCrd.toYamlString();

              SparkCrd sparkCrd2 = SparkCrd.fromYaml(sparkCdrYaml);

              Assertions.assertEquals(sparkCrd, sparkCrd2);
            });
  }

  @SneakyThrows
  public List<SparkCrd> readAllSparkCrds(String testFile) {
    try (InputStream testFileInputStream =
        SparkCrdTest.class.getClassLoader().getResourceAsStream(testFile)) {
      YAMLFactory yamlFactory = new YAMLFactory();
      ObjectMapper mapper = new ObjectMapper(yamlFactory);
      return mapper
          .readValues(
              yamlFactory.createParser(testFileInputStream), new TypeReference<SparkCrd>() {})
          .readAll();
    }
  }
}
