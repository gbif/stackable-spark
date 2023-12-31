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

/** Common methods and constants used to interact with Stackable Spark Applications. */
public class SparkAppUtils {
  static final String STACKABLE_SPARK_GROUP = "spark.stackable.tech";
  static final String STACKABLE_SPARK_VERSION = "v1alpha1";
  static final String STACKABLE_SPARK_PLURAL = "sparkapplications";

  static K8StackableSparkController.Phase getPhase(AbstractMap<String, Object> object) {
    if (object.containsKey("status")) {
      return K8StackableSparkController.Phase.valueOf(
          ((AbstractMap<String, Object>) object.get("status"))
              .get("phase")
              .toString()
              .toUpperCase());
    }
    return getPhase(object, K8StackableSparkController.Phase.EMPTY);
  }

  static K8StackableSparkController.Phase getPhase(
      AbstractMap<String, Object> object, K8StackableSparkController.Phase defaultPhase) {
    if (object.containsKey("status")) {
      return K8StackableSparkController.Phase.valueOf(
          ((AbstractMap<String, Object>) object.get("status"))
              .get("phase")
              .toString()
              .toUpperCase());
    }
    return defaultPhase;
  }

  static String getAppName(AbstractMap<String, Object> object) {
    return (String) ((AbstractMap<String, Object>) object.get("metadata")).get("name");
  }
}
