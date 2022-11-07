ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.2

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/et-wa-post-deployment-ft-tests.jar /opt/app/

EXPOSE 8888
CMD [ "et-wa-post-deployment-ft-tests.jar" ]

