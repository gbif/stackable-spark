/*
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

import java.io.Closeable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Watch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.stackable.SparkAppUtils.*;

/** Allows to subscribe to events on Stackable Spark Application. */
@Slf4j
public class StackableSparkWatcher implements Runnable, Closeable {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Event listener interface called on every update it gets from K8. */
  public interface EventsListener {
    void onEvent(
        EventType eventType,
        String appName,
        K8StackableSparkController.Phase phase,
        Object payload);
  }

  /** Default listener: logs each captured event. */
  public static class LogEventsListener implements EventsListener {
    @Override
    @SneakyThrows
    public void onEvent(
        EventType eventType,
        String appName,
        K8StackableSparkController.Phase phase,
        Object payload) {
      log.info(
          "Event '{}' received for application '{}' in phase '{}' with payload '{}'",
          eventType,
          appName,
          phase,
          MAPPER.writeValueAsString(payload));
    }
  }

  private final KubeConfig kubeConfig;

  private final EventsListener eventsListener;

  private final Map<String, String> labelSelector;

  private final Map<String, String> fieldSelector;

  private final Pattern nameSelector;

  private boolean stop = false;

  @SneakyThrows
  public static StackableSparkWatcher fromConfigFile(String kubeConfigFile) {
    return new StackableSparkWatcher(ConfigUtils.loadKubeConfig(kubeConfigFile));
  }

  public StackableSparkWatcher(
      KubeConfig kubeConfig,
      EventsListener eventsListener,
      Map<String, String> labelSelector,
      Map<String, String> fieldSelector,
      String nameSelector) {
    this.kubeConfig = kubeConfig;
    this.eventsListener = eventsListener;
    this.fieldSelector = fieldSelector;
    this.labelSelector = labelSelector;
    this.nameSelector = nameSelector != null ? Pattern.compile(nameSelector) : null;
  }

  public StackableSparkWatcher(
      KubeConfig kubeConfig, EventsListener eventsListener, String nameSelector) {
    this.kubeConfig = kubeConfig;
    this.eventsListener = eventsListener;
    this.fieldSelector = Collections.emptyMap();
    this.labelSelector = Collections.emptyMap();
    this.nameSelector = nameSelector != null ? Pattern.compile(nameSelector) : null;
  }

  public StackableSparkWatcher(KubeConfig kubeConfig) {
    this.kubeConfig = kubeConfig;
    this.eventsListener = new LogEventsListener();
    this.labelSelector = Collections.emptyMap();
    this.fieldSelector = Collections.emptyMap();
    this.nameSelector = null;
  }

  /** Creates a started Thread with the current instance as Runnable. */
  public Thread start() {
    Thread watcherThread = new Thread(this);
    watcherThread.start();
    return watcherThread;
  }

  /** Recognised event types from K8. */
  public enum EventType {
    BOOKMARK,
    ADDED,
    MODIFIED,
    DELETED;
  }

  /** Starts the watcher execution. */
  @Override
  @SneakyThrows
  public void run() {
    log.info("Starting K8StackableSpark Watcher");
    Configuration.setDefaultApiClient(ClientBuilder.kubeconfig(kubeConfig).build());
    ApiClient client = Configuration.getDefaultApiClient();
    CustomObjectsApi customObjectsApi = new CustomObjectsApi();
    // Creates a watch for the Stackable Spark application
    // Recreate watcher when it ends cycle
    while (!stop) {
      log.debug("Starting new K8StackableSpark watch cycle");
      try (Watch<Object> watch =
          Watch.createWatch(
              client,
              customObjectsApi
                  .listNamespacedCustomObject(
                      STACKABLE_SPARK_GROUP,
                      STACKABLE_SPARK_VERSION,
                      kubeConfig.getNamespace(),
                      STACKABLE_SPARK_PLURAL)
                  .fieldSelector(toSelectorQuery(fieldSelector))
                  .labelSelector(toSelectorQuery(labelSelector))
                  .watch(true)
                  .buildCall(null),
              new TypeToken<Watch.Response<Object>>() {}.getType())) {
        // Gets the watch response and calls the listener
        for (Watch.Response<Object> item : watch) {
          AbstractMap<String, Object> object = (AbstractMap<String, Object>) item.object;
          EventType eventType = EventType.valueOf(item.type);
          K8StackableSparkController.Phase phase = getPhase(object);
          String appName = getAppName(object);
          if (matchesNameSelector(appName)) {
            eventsListener.onEvent(eventType, appName, phase, object);
          }
          if (stop) {
            break;
          }
        }
      }
      log.debug("End of K8StackableSpark watch cycle");
    }
  }

  /** Matches a string to the name pattern. Null name selector matches all applications. */
  private boolean matchesNameSelector(String appName) {
    return nameSelector == null || nameSelector.matcher(appName).matches();
  }

  /**
   * Takes a K8 selector Map<String,String> and returns a string in the format:
   * key1=value1,..,keyN=valueN.
   */
  private String toSelectorQuery(Map<String, String> selector) {
    if (selector != null && !selector.isEmpty()) {
      return selector.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining(","));
    }
    return null;
  }

  /** Stops the watcher. */
  public void stop() {
    stop = true;
    log.info("Stopping K8StackableSpark Watcher");
  }

  @Override
  public void close() {
    stop();
  }
}
