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

import java.util.AbstractMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.stackable.SparkAppUtils.*;

@Slf4j
public class K8StackableSparkController {

  public static final int NOT_FOUND = 404;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private interface AppOperation {
    AbstractMap<String, Object> apply(String applicationId) throws ApiException;
  }

  /**
   * Phase represents the lifecycle of Pods in Kubernetes.
   * https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/ INITIATING: is the only
   * additional status, it was created because theK K8 doesn't assign a status right after the pod
   * is created.
   */
  public enum Phase {
    EMPTY, // No phase detected
    INITIATING,
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    UNKNOWN;
  }

  private final KubeConfig kubeConfig;

  private final SparkCrd sparkCrd;

  @SneakyThrows
  public static K8StackableSparkController fromConfigFiles(
      String kubeConfigFile, String sparkApplicationConfigFile) {
    return K8StackableSparkController.builder()
        .kubeConfig(ConfigUtils.loadKubeConfig(kubeConfigFile))
        .sparkCrd(ConfigUtils.loadSparkCdr(sparkApplicationConfigFile))
        .build();
  }

  @SneakyThrows
  @Builder
  public K8StackableSparkController(SparkCrd sparkCrd, KubeConfig kubeConfig) {
    this.sparkCrd = sparkCrd;
    this.kubeConfig = kubeConfig;
    Configuration.setDefaultApiClient(ClientBuilder.kubeconfig(kubeConfig).build());
  }

  private void deleteIfExists(String applicationId) throws ApiException {
    try {
      getSparkApplication(applicationId);
      stopSparkApplication(applicationId);
    } catch (ApiException apiException) {
      if (apiException.getCode() != NOT_FOUND) {
        log.error("Error deleting Spark application {}", errorToString(apiException), apiException);
        throw apiException;
      }
    }
  }

  @SneakyThrows
  public Phase getApplicationPhase(String applicationId) {
    CustomObjectsApi customObjectsApi = new CustomObjectsApi();
    try {
      AbstractMap<String, Object> status =
          (AbstractMap<String, Object>)
              customObjectsApi.getNamespacedCustomObjectStatus(
                  STACKABLE_SPARK_GROUP,
                  STACKABLE_SPARK_VERSION,
                  kubeConfig.getNamespace(),
                  STACKABLE_SPARK_PLURAL,
                  applicationId);
      return getPhase(status, Phase.INITIATING);
    } catch (ApiException apiException) {
      log.error(
          "Error getting Spark application status {}", errorToString(apiException), apiException);
      throw new RuntimeException(apiException);
    }
  }

  @SneakyThrows
  public AbstractMap<String, Object> submitSparkApplication(String applicationId) {
    CustomObjectsApi customObjectsApi = new CustomObjectsApi();
    SparkCrd sparkPodConfig = cloneAndRename(sparkCrd, applicationId);
    try {
      deleteIfExists(applicationId);
      return (AbstractMap<String, Object>)
          customObjectsApi.createNamespacedCustomObject(
              STACKABLE_SPARK_GROUP,
              STACKABLE_SPARK_VERSION,
              kubeConfig.getNamespace(),
              STACKABLE_SPARK_PLURAL,
              sparkPodConfig,
              "true",
              null,
              null);
    } catch (ApiException apiException) {
      log.error("Error submitting Spark application {}", errorToString(apiException), apiException);
      throw new RuntimeException(apiException);
    }
  }

  @SneakyThrows
  private String errorToString(ApiException apiException) {
    return MAPPER.writeValueAsString(apiException);
  }

  private static AbstractMap<String, Object> tryApplicationMethod(
      AppOperation operation, String applicationId) {
    try {
      return operation.apply(applicationId);
    } catch (ApiException apiException) {
      if (apiException.getCode() == 404) {
        return null;
      }
      throw new RuntimeException(apiException);
    }
  }

  @SneakyThrows
  public AbstractMap<String, Object> getApplication(String applicationId) {
    return tryApplicationMethod(this::getSparkApplication, applicationId);
  }

  private AbstractMap<String, Object> getSparkApplication(String applicationId)
      throws ApiException {
    CustomObjectsApi customObjectsApi = new CustomObjectsApi();
    return (AbstractMap<String, Object>)
        customObjectsApi.getNamespacedCustomObject(
            STACKABLE_SPARK_GROUP,
            STACKABLE_SPARK_VERSION,
            kubeConfig.getNamespace(),
            STACKABLE_SPARK_PLURAL,
            applicationId);
  }

  @SneakyThrows
  public AbstractMap<String, Object> stopApplication(String applicationId) {
    return tryApplicationMethod(this::stopSparkApplication, applicationId);
  }

  public AbstractMap<String, Object> stopSparkApplication(String applicationId)
      throws ApiException {
    CustomObjectsApi customObjectsApi = new CustomObjectsApi();
    return (AbstractMap<String, Object>)
        customObjectsApi.deleteNamespacedCustomObject(
            STACKABLE_SPARK_GROUP,
            STACKABLE_SPARK_VERSION,
            kubeConfig.getNamespace(),
            STACKABLE_SPARK_PLURAL,
            applicationId,
            null,
            null,
            null,
            null,
            null);
  }

  private static SparkCrd cloneAndRename(SparkCrd v1Pod, String name) {
    return v1Pod.toBuilder().metadata(v1Pod.getMetadata().toBuilder().name(name).build()).build();
  }
}
