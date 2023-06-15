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

import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.KubeConfig;
import lombok.SneakyThrows;

public class ConfigUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @SneakyThrows
  public static V1ConfigMap loadConfigMap(String configMapFile) {
    try (InputStream configMapInputFile = Files.newInputStream(Paths.get(configMapFile))) {
      return MAPPER.readValue(configMapInputFile, V1ConfigMap.class);
    }
  }

  @SneakyThrows
  public static SparkCrd loadSparkCdr(String sparkApplicationConfigFile) {
    try (InputStream sparkApplicationConfigInputFile =
        Files.newInputStream(Paths.get(sparkApplicationConfigFile))) {
      return SparkCrd.fromYaml(sparkApplicationConfigInputFile);
    }
  }

  @SneakyThrows
  public static KubeConfig loadKubeConfig(String kubeConfigFile) {
    try (FileReader kubeConfigReader = new FileReader(Paths.get(kubeConfigFile).toFile())) {
      return KubeConfig.loadKubeConfig(kubeConfigReader);
    }
  }
}
