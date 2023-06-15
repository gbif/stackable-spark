# stackable-spark
This library contains utility classes to submit and interact with the [Stackable Operator for Spark](https://docs.stackable.tech/home/stable/spark-k8s/index.html).
It contains three basic elements:
 - [SparkCrd](src/main/java/org/gbif/stackable/SparkCrd.java): a Java POJO that abstracts the structure of the CRD (Custom Resource Definition) of [Stackable Spark Application](https://doc.crds.dev/github.com/stackabletech/spark-k8s-operator/spark.stackable.tech/SparkApplication/v1alpha1@23.4.0)
 - [K8StackableSparkController](src/main/java/org/gbif/stackable/K8StackableSparkController.java): allows submit, stop and get the status of Spark Applications.
 - [StackableSparkWatcher](src/main/java/org/gbif/stackable/StackableSparkWatcher.java): allows to subscribe a watcher to a K8 cluster to get the status of one or multiple Spark applications.

This library on the (Kubernetes Java client)[https://github.com/kubernetes-client/java] to perform operations against a running cluster.


## Build
To build and run tests use:
```
mvn clean package install verify
```