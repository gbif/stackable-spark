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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder(toBuilder = true)
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class SparkCrd implements ToBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  @Builder.Default private String apiVersion = "spark.stackable.tech/v1alpha1";

  @Builder.Default private String kind = "SparkApplication";

  private Metadata metadata;

  private Spec spec;

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
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Metadata implements ToBuilder {

    /** Application name */
    private String name;

    /** K8 namespace. */
    private String namespace;
  }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Spec implements ToBuilder {

    /** Pod overrides */
    private RoleGroups roleGroups;

    /** Application version */
    private String version;

    /** cluster or client. Currently only cluster is supported. */
    @Builder.Default private String mode = "cluster";

    /**
     * User-supplied image containing spark-job dependencies that will be copied to the specified
     * volume mount.
     */
    private String image;

    /**
     * Spark image which will be deployed to driver and executor pods, which must contain spark
     * environment needed by the job e.g.
     * docker.stackable.tech/stackable/spark-k8s:3.3.0-stackable0.3.0.
     */
    @Builder.Default
    private String sparkImage =
        "docker.stackable.tech/stackable/spark-k8s:3.3.0-stackable0.1.0";

    /**
     * Optional Enum (one of Always, IfNotPresent or Never) that determines the pull policy of the
     * spark job image.
     */
    private String sparkImagePullPolicy;

    /**
     * An optional list of references to secrets in the same namespace to use for pulling any of the
     * images used by a SparkApplication resource. Each reference has a single property (name) that
     * must contain a reference to a valid secret.
     */
    private List<String> sparkImagePullSecrets;

    /** The actual application file that will be called by spark-submit. */
    private String mainApplicationFile;

    /** The main class i.e. entry point for JVM artifacts */
    private String mainClass;

    /** Arguments passed directly to the job artifact. */
    private List<String> args;

    /** A map of key/value strings that will be passed directly to spark-submit. */
    private Map<String, String> sparkConf;

    /** Python resources. */
    private Deps deps;

    /** A list of volumes */
    private List<Volume> volumes;

    /** Job resources. */
    private Job job;

    /** Spark driver. */
    private Driver driver;

    /** Spark executors definition. */
    private Executor executor;

    /** Vector configmap containing discovery information for the aggregator **/
    private String vectorAggregatorConfigMapName;

    /** S3 connection specification. See the S3 resources for more details. */
    private S3Connection s3connection;

    /** Log file directory settings. */
    private LogFileDirectory logFileDirectory;

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleGroups implements ToBuilder {

      @JsonProperty("default")
      private Default _default;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Default implements ToBuilder {

        private PodOverrides podOverrides;

        }
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PodOverrides implements ToBuilder {

      private PodOverrides.Metadata metadata;
      private PodOverrides.Spec spec;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Metadata implements ToBuilder {

        /** Labels to be added to the K8 Pod or custom resource. */
        @Builder.Default private Map<String, String> labels = Collections.emptyMap();
        @Builder.Default private Map<String, String> annotations = Collections.emptyMap();
      }

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Spec implements ToBuilder {
        private List<InitContainers> initContainers;
        @Data
        @Builder(toBuilder = true)
        @Jacksonized
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InitContainers implements ToBuilder {
          private String name;
          private String imagePullPolicy;
        }
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deps implements ToBuilder {

      /** A list of python packages that will be installed via pip. */
      private List<String> requirements;

      /** A list of packages that is passed directly to spark-submit. */
      private List<String> packages;

      /** A list of excluded packages that is passed directly to spark-submit */
      private List<String> excludePackages;

      /** A list of repositories that is passed directly to spark-submit */
      private List<String> repositories;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigMap implements ToBuilder {

      private String name;

      private List<Item> items;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      public static class Item implements ToBuilder {

        private String key;

        private String path;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Volume implements ToBuilder {

      /** The volume name. */
      private String name;

      /** The persistent volume claim backing the volume. */
      private PersistentVolumeClaim persistentVolumeClaim;

      private ConfigMap configMap;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class PersistentVolumeClaim implements ToBuilder {

        /** The persistent volume claim name backing the volume. */
        private String claimName;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job implements ToBuilder {

      /** config specification for the initiating Job. */
      private Config config;
    }

    /** Mounted Volume. */
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumeMount implements ToBuilder {

      /** Name of mount. */
      private String name;

      /** Volume mount path. */
      private String mountPath;

      /** Volume mount sub-path. */
      private String subPath;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resources implements ToBuilder {

      private Cpu cpu;

      private Memory memory;

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Cpu implements ToBuilder {

        private String min;

        private String max;
      }

      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Memory implements ToBuilder {
        private String limit;
      }
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Driver implements ToBuilder {

      /** Config specification for the component Pod. */
      private Config config;

      /** A list of mounted volumes for the component Pod. */
      private List<VolumeMount> volumeMounts;

      /**
       * Driver Pod placement affinity. See <a
       * href="https://docs.stackable.tech/home/nightly/spark-k8s/usage-guide/pod-placement.html">Pod
       * placement</a>.
       */
      private String affinity;

      private PodOverrides podOverrides;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Executor implements ToBuilder {

      /** Number of executor replicas launched for this job. */
      private int replicas;

      /** Config for the Pod */
      private Config config;

      /** A list of mounted volumes for the component Pod. */
      private List<VolumeMount> volumeMounts;

      /**
       * Driver Pod placement affinity. See <a
       * href="https://docs.stackable.tech/home/nightly/spark-k8s/usage-guide/pod-placement.html">Pod
       * placement</a>.
       */
      private String affinity;

      private PodOverrides podOverrides;
    }

    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogFileDirectory implements ToBuilder {
      private S3 s3;
      /**
       * S3 bucket definition where applications should publish events for the Spark History server.
       */
      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class S3 implements ToBuilder {
        /** Prefix to use when storing events for the Spark History server. */
        private String prefix;
        /** Definition for how to access the bucket **/
        private Bucket bucket;
        @Data
        @Builder(toBuilder = true)
        @Jacksonized
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Bucket implements ToBuilder {
          /** Inline configuration for connection to the bucket **/
          private Inline inline;
          @Data
          @Builder(toBuilder = true)
          @Jacksonized
          @NoArgsConstructor
          @AllArgsConstructor
          public static class Inline implements ToBuilder {
            /** The name of the bucket to put the logs **/
            private String bucketName;
          }

        }
      }
    }
    @Data
    @Builder(toBuilder = true)
    @Jacksonized
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3Connection implements ToBuilder {
      private Inline inline;
      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Inline implements ToBuilder {
        private String host;
        private int port;
        private String accessStyle;
        private Credentials credentials;
        @Data
        @Builder(toBuilder = true)
        @Jacksonized
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Credentials implements ToBuilder {
          private String secretClass;
        }
      }
    }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Logging implements ToBuilder {
      private boolean enableVectorAgent;
      private Containers containers;
      @Data
      @Builder(toBuilder = true)
      @Jacksonized
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Containers implements ToBuilder {
        private Vector vector;
        private Spark spark;

        @Data
        @Builder(toBuilder = true)
        @Jacksonized
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Vector implements ToBuilder {
          private File file;
        }

        @Data
        @Builder(toBuilder = true)
        @Jacksonized
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Spark implements ToBuilder {
          private Console console;
          private File file;
          private Loggers loggers;

          @Data
          @Builder(toBuilder = true)
          @Jacksonized
          @NoArgsConstructor
          @AllArgsConstructor
          public static class Console implements ToBuilder {
            private String level;
          }

          @Data
          @NoArgsConstructor
          @AllArgsConstructor
          public static class Loggers {
            @Setter(onMethod_ = {@JsonSetter("ROOT")})
            @Getter(onMethod_ = {@JsonGetter("ROOT")})
            private Root ROOT;

            @Data
            @Builder(toBuilder = true)
            @Jacksonized
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Root implements ToBuilder {
              private String level;
            }
          }
        }
      }
  }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  @NoArgsConstructor
  @AllArgsConstructor
  public static class File implements ToBuilder {
      private String level;
  }

  @Data
  @Builder(toBuilder = true)
  @Jacksonized
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Config implements ToBuilder {
      private Resources resources;
      private Logging logging;
  }
}
