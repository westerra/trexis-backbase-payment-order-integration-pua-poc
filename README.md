## treXis Backbase Payment Orders Integration
The treXis Backbase Payment Order Integration repository is a single microservice that implements the Backbase SDK and service end points to allow for the exchanges of payments with the Core via Finite.

This repository is utilizing Backbase depedencies, which require configuration of your local maven environment to have access to Backbase depedencies.  Consult Backbase Support for your credentials.

Perform these commands to build this repository :
```
mvn clean install
```


### Finite Dependency
This project utilizes the Finite API, by means of the treXis Finite Java Client dependency.  The Finite Client require the following configurations:
```
finite:
  hosturl: http://localhost:9090
  apikey: demo-api-key-do-not-use-in-production
```

### Versions and Upgrade
The service is developed using the Backbase Service SDK.  The service contains the dependency to the correct service SDK used to package the service, update the dependency to upgrade this to the SDK matching your deployment.


### Timezone configuration for transaction date require the following configurations where zoneId value can be different based on time zone:
```
timeZone:
    zoneId: America/Denver
```

### About treXis Co-Develop
The treXis co-development program allow treXis customers and partners to clone/fork code repositories from the treXis Bitbucket repository.  A list of all accelerators are published on the https://experts.trexis.net.

The <a href="license.txt">license.txt</a> file describe the developer licence agreement for using this repository.