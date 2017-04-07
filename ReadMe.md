# Learningpath API
 [![Build Status](https://travis-ci.org/NDLANO/learningpath-api.svg?branch=master)](https://travis-ci.org/NDLANO/learningpath-api)

API for Learningpaths from NDLA

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Run integration tests
    sbt it:test

### IntegrationTest Tag and sbt run problems
Tests that need a running elasticsearch outside of component, e.g. in your local docker are marked with selfdefined java
annotation test tag  ```IntegrationTag``` in ```/ndla/article-api/src/test/java/no/ndla/tag/IntegrationTest.java```. 
As of now we have no running elasticserach or tunnel to one on Travis and need to ignore these tests there or the build will fail.  
Therefore we have the
 ```testOptions in Test += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")``` in ```build.sbt```  
This, it seems, will unfortunalty override runs on your local commandline so that ```sbt "test-only -- -n no.ndla.tag.IntegrationTest"```
 will not run unless this line gets commented out or you comment out the ```@IntegrationTest``` annotation in ```SearchServiceTest.scala```
 This should be solved better!

    sbt "test-only -- -n no.ndla.tag.IntegrationTest"


## Create Docker Image
    ./build.sh

