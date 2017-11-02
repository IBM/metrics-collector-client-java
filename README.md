# Overview

Metrics Collector Service collects statistics for deployment of a github sample code on Cloud Foundry, Kubernetes, Data Science Experience, OpenWhisk etc.

This is Java client for Metrics Collector Service. It is a module JAR that can track and report details of a demo/tutorial.

# Usage

## Reference the library in your project

To take advantage of the tracking functionality in a web application:

### Add these Maven dependencies

  ```xml
    <dependency>
      <groupId>com.ibm.websphere.appserver.api</groupId>
      <artifactId>com.ibm.websphere.appserver.api.json</artifactId>
      <version>1.0.11</version>
    </dependency>
    <dependency>
      <groupId>com.ibm.journeycode.metricstracker</groupId>
      <artifactId>java-metrics-tracker-client</artifactId>
      <version>0.3.0</version>
    </dependency>
    <dependency>
      <groupId>org.yaml</groupId>
      <artifactId>snakeyaml</artifactId>
      <version>1.19</version>
    </dependency>
  ```

### Or Download/Build it yourself

1. Obtain the tracker client library
 * From the [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ibm.journeycode.metricstracker%22) (groupId **com.ibm.journeycode.metricstracker**, artifactId **java-metrics-tracker-client**) *or*
 * Download the source code from https://github.com/IBM/metrics-collector-client-java and build the JAR with `mvn package` using the default target/goal of the provided ant or pom script.
2. Copy client library `java-metrics-tracker-client-{version}-jar-with-dependencies.jar` to your web application's `scr/main/webapp/WEB-INF/lib` directory.

## Provide information about your application

Create a `repository.yaml` file in the web applications' `scr/main/webapp/META-INF` directory.
Replace the application specific information in the example below with your application's information.
The repository.yaml need to be written in Yaml format. Also, please put all your keys in lower case.

```
id: spring-boot-microservices-on-kubernetes
runtimes: 
  - Kubernetes Cluster
  - OpenWhisk
services: 
  - Compose for MySQL
event_id: web
event_organizer: dev-journeys
language: java
```

Required field:
1. id: Put your journey name/Github URL of your journey/pattern.
   - Note: Please put down the Github URL if your journey/pattern is not in IBM organization.
2. runtimes: Put down all your platform runtime environments in a list.
3. services: Put down all the bluemix services that are used in your journey in a list.
4. event_id: Put down where you will distribute your journey. Default is **web**. 
5. event_organizer: Put down your event organizer if you have one.
6. language: If your journey is not in **java**, please put down the journey's main language in lower case.

## Add a Privacy Notice

Add the following to your application README.md:

```
# Privacy Notice
Sample web applications that include this tracking library may be configured to track
deployments to [IBM Cloud](https://www.bluemix.net/) and other Cloud Foundry platforms.
The following information is sent to a [Deployment Tracker](https://github.com/IBM/metrics-collector-service)
service on each deployment by default:
* Application Name (`application_name`)
* Application GUID (`application_id`)
* Application instance index (`instance_index`)
* Space ID (`space_id`) or OS username
* Application Version (`application_version`)
* Application URIs (`application_uris`)
* Cloud Foundry API (`cf_api`)
* Labels and names of bound services
* Number of instances for each bound service and associated plan information
* Metadata in the repository.yaml file

This data is collected from the `repository.yaml` file in the sample application and the `VCAP_APPLICATION` and `VCAP_SERVICES` environment variables in IBM Cloud and other Cloud Foundry platforms. This data is used by IBM to track metrics around 
deployments of sample applications to IBM Cloud to measure the usefulness of our examples,
so that we can continuously improve the content we offer to you. 

# Disabling Deployment Tracking

To disable deployment tracking remove `java-metrics-tracker-client-{version}.jar`
from the web application's `WebContent/WEB-INF/lib` directory or from the `pom.xml`
and redeploy the application.
```

**Note:** All applications that use the deployment tracker must have a Privacy Notice.

## Package and deploy your web application

To view basic information about the tracker client and the most recent
tracking request, direct your browser to `<APPLICATION_URL>/comibmbluemix/CFAppTracker`,
replacing `<APPLICATION_URL>` with the web application URL.

# Example app

[This project](https://github.com/tomcli/local-liberty-tutorial) uses the deployment tracker library.
You can look at [its pom.xml](https://github.com/tomcli/local-liberty-tutorial/blob/master/pom.xml),
[its repository.yaml](https://github.com/tomcli/local-liberty-tutorial/blob/master/src/main/webapp/META-INF/repository.yaml)
and [its privacy notice](https://github.com/tomcli/local-liberty-tutorial#privacy-notice).

# License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
