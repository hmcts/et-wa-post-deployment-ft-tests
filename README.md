# wa-post-deployment-ft-tests

[![Build Status](https://travis-ci.org/hmcts/et-wa-post-deployment-ft-tests.svg?branch=master)](https://travis-ci.org/hmcts/et-wa-post-deployment-ft-tests)

## Purpose
This repository contains a set of functional tests which are designed to run periodically or after a helm deployment as a post deployment job to ensure regression.

## What does this app do?
It creates a case in CCD and publishes a message for this case on the Demo ASB topic.
Then your local Case Event Handler can consume this message using your development subscription.
This way we can test user paths end to end.

## Requirements
* Minikube and your local env has to be up and running.
* Bring up the following services:
    * case event handler
    * workflow-api
    * task-management-api
    * case-api
    * documentation-api
    * notification-api

* Configure the following env vars in the application-functional profile:
    * AZURE_SERVICE_BUS_CONNECTION_STRING
    * AZURE_SERVICE_BUS_TOPIC_NAME
    * AZURE_SERVICE_BUS_MESSAGE_AUTHOR (The author of the message this can be used if you have filters set up in your subscription)

* Source your bash so that the Case Event Handler can use those env vars too.
* Finally, set the following env vars in the Case Event Handler:
    * AZURE_SERVICE_BUS_SUBSCRIPTION_NAME to your developer subscription in the demo env.
    * AZURE_SERVICE_BUS_FEATURE_TOGGLE=true

## When merging to master:

When performing a merge against master the withNightlyPipeline() will be used to run this tests
and verify the build this is because this app is not a service
that needs to be deployed but rather just a test framework.
The withNightlyPipeline() will perform:

- Dependency check
- Full functional tests run

## Nightly Builds
The pipeline has also been configured to run every hour in the nightly builds.
This is specified on the `Jenkinsfile_nightly` file as a cron job `pipelineTriggers([cron('0 * * * *')])`

## Publishing as ACR task
Any commit or merge into master will automatically trigger an Azure ACR task. This task has been manually
created using `./bin/deploy-acr-task.sh`. The task is defined in `acr-build-task.yaml`.

Note: the deployment script relies on a GitHub token (https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line) defined in `infra-vault-prod`, secret `hmcts-github-apikey`. The token is for setting up a webhook so Azure will be notified when a merge or commit happens. Make sure you are a repo admin and select token scope of: `admin:repo_hook  Full control of repository hooks`

More info on ACR tasks can be read here: https://docs.microsoft.com/en-us/azure/container-registry/container-registry-tasks-overview

## Running functional tests
```bash
./gradlew functional
```
### You can also target a specific scenario:
```bash
./gradlew clean functional --tests ScenarioRunnerTest --info -Dscenario=ET-RET-2344-Et1Vetting
```
### or multiple scenarios:
```bash
./gradlew clean functional --tests ScenarioRunnerTest --info -Dscenario=ET-RET-2501
```

## Tips for onboarding teams
if you are onboarding into Task Management, you may clone this repository and customize the following items to setup
your own post deployment tests:
# src/functionalTest/resources/application-functional.yaml
- document_management.url: set your local document store api
- ccd_gateway.url: set your local ccd gateway
- core_case_data.api.url: set your local ccd case data api
- azure.servicebus.connection-string: set your service bus connection string
- azure.servicebus.topic set your service bus topic name

# src/functionalTest/resources/templates
add your case data template file in json format, this template will be used in your scenarios
# src/functionalTest/resources/scenarios
- add your test scenario files in json format
# src/testUtils/java/uk/gov/hmcts/reform/wapostdeploymentfttests/services/AuthorizationHeadersProvider.java
customize this class to setup your authorization headers and user credentials.
# Caveat
This repository is still under development and so changes may occur in the future.
We will update this Readme file anytime we modify the structure or the underlying framework.
If you are unsure about something, please check with the team for clarification.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
