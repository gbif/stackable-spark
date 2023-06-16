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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class SparkCrd implements ToBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Builder.Default private final String apiVersion = "spark.stackable.tech/v1alpha1";

  @Builder.Default private final String kind = "SparkApplication";

  private final Metadata metadata;

  private final Spec spec;

  @SneakyThrows
  public String toYamlString() {
    return MAPPER.writeValueAsString(this);
  }

  @SneakyThrows
  public static SparkCrd fromYaml(String yaml) {
    return MAPPER.readValue(yaml, SparkCrd.class);
  }

  @SneakyThrows
  public static SparkCrd fromYaml(InputStream yaml) {
    return MAPPER.readValue(yaml, SparkCrd.class);
  }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  public static class Metadata implements ToBuilder {

    /** Application name */
    private final String name;

    /** K8 namespace. */
    private final String namespace;

    /** Labels to be added to the K8 Pod or custom resource.*/
    @Builder.Default
    private final Map<String,String> labels = Collections.emptyMap();
  }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  public static class Spec implements ToBuilder {
    /** Application version */
    private final String version;

    /** cluster or client. Currently only cluster is supported. */
    @Builder.Default private final String mode = "cluster";

    /**
     * User-supplied image containing spark-job dependencies that will be copied to the specified
     * volume mount.
     */
    private final String image;

    /**
     * Spark image which will be deployed to driver and executor pods, which must contain spark
     * environment needed by the job e.g.
     * docker.stackable.tech/stackable/spark-k8s:3.3.0-stackable0.3.0.
     */
    @Builder.Default
    private final String sparkImage =
        "docker.stackable.tech/stackable/spark-k8s:3.3.0-stackable0.1.0";

    /**
     * Optional Enum (one of Always, IfNotPresent or Never) that determines the pull policy of the
     * spark job image.
     */
    private final String sparkImagePullPolicy;

    /**
     * An optional list of references to secrets in the same namespace to use for pulling any of the
     * images used by a SparkApplication resource. Each reference has a single property (name) that
     * must contain a reference to a valid secret.
     */
    private final List<String> sparkImagePullSecrets;

    /** The actual application file that will be called by spark-submit. */
    private final String mainApplicationFile;

    /** The main class i.e. entry point for JVM artifacts */
    private final String mainClass;

    /** Arguments passed directly to the job artifact. */
    private final List<String> args;

    /** S3 connection specification. See the S3 resources for more details. */
    private final String s3connection;

    /** A map of key/value strings that will be passed directly to spark-submit. */
    private final Map<String, String> sparkConf;

    /** Python resources. */
    private final Deps deps;

    /** A list of volumes */
    private final List<Volume> volumes;

    /** Job resources. */
    private final Job job;

    /** Spark driver. */
    private final Driver driver;

    /** Spark executors definition. */
    private final Executor executor;

    /** Log file directory settings. */
    private final LogFileDirectory logFileDirectory;

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class Deps implements ToBuilder {

      /** A list of python packages that will be installed via pip. */
      private final List<String> requirements;

      /** A list of packages that is passed directly to spark-submit. */
      private final List<String> packages;

      /** A list of excluded packages that is passed directly to spark-submit */
      private final List<String> excludePackages;

      /** A list of repositories that is passed directly to spark-submit */
      private final List<String> repositories;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class ConfigMap implements ToBuilder {

      private final String name;

      private final List<Item> items;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      public static class Item implements ToBuilder {

        private final String key;

        private final String path;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class Volume implements ToBuilder {

      /** The volume name. */
      private final String name;

      /** The persistent volume claim backing the volume. */
      private final PersistentVolumeClaim persistentVolumeClaim;

      private final ConfigMap configMap;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      public static class PersistentVolumeClaim implements ToBuilder {

        /** The persistent volume claim name backing the volume. */
        private final String claimName;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class Job implements ToBuilder {

      /** Resources specification for the initiating Job. */
      private final Resources resources;
    }

    /** Mounted Volume. */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static final class VolumeMount implements ToBuilder {

      /** Name of mount. */
      private final String name;

      /** Volume mount path. */
      private final String mountPath;

      /** Volume mount sub-path. */
      private final String subPath;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class Resources implements ToBuilder {

      private final Cpu cpu;

      private final Memory memory;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      public static class Cpu implements ToBuilder {

        private final String min;

        private final String max;
      }

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      public static class Memory implements ToBuilder {
        private final String limit;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static final class Driver implements ToBuilder {

      /** Resources specification for the component Pod. */
      private final Resources resources;

      /** A list of mounted volumes for the component Pod. */
      private final List<VolumeMount> volumeMounts;

      /**
       * Driver Pod placement affinity. See <a
       * href="https://docs.stackable.tech/home/nightly/spark-k8s/usage-guide/pod-placement.html">Pod
       * placement</a>.
       */
      private final String affinity;

      /**
       * Logging aggregation for the driver Pod. See <a
       * href="https://docs.stackable.tech/home/nightly/concepts/logging.html">Logging</a>.
       */
      private final String logging;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static final class Executor implements ToBuilder {

      /** Number of executor instances launched for this job. */
      private final int instances;

      /** Resources specification for the component Pod. */
      private final Resources resources;

      /** A list of mounted volumes for the component Pod. */
      private final List<VolumeMount> volumeMounts;

      /**
       * Driver Pod placement affinity. See <a
       * href="https://docs.stackable.tech/home/nightly/spark-k8s/usage-guide/pod-placement.html">Pod
       * placement</a>.
       */
      private final String affinity;

      /**
       * Logging aggregation for the driver Pod. See <a
       * href="https://docs.stackable.tech/home/nightly/concepts/logging.html">Logging</a>.
       */
      private final String logging;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    public static class LogFileDirectory implements ToBuilder {

      /**
       * S3 bucket definition where applications should publish events for the Spark History server.
       */
      private final String bucket;

      /** Prefix to use when storing events for the Spark History server. */
      private final String prefix;
    }
  }
}
