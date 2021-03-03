# platformserviceconfiguration-maven-plugin

This Maven plugin is meant to provide an easy way to configure services like Keycloak or 
RabbitMQ via REST calls.


## Available goal

* *configure*


## Getting started

To use this plugin and start working with it, declare the 
*platformserviceconfiguration-maven-plugin* and add a *pluginManagement* entry.

    <packaging>pom</packaging>
    ....
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <version>1.1.0-SNAPSHOT</version>
          </plugin>
        </plugins>
      </pluginManagement>
      ....
      <plugins>
        <plugin>
          <groupId>ch.inacta.maven</groupId>
          <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
          ....
        </plugin>
      </plugins>
    </build>

## Application strategy

The *strategy* tag specifies which application should be configured. Supported 
applications by now are *KEYCLOAK* and *RABBITMQ*.

    <configuration>
      <strategy>KEYCLOAK</strategy>
    </configuration>

## REST URL endpoint

The REST URL endpoint is specified via two parameters, the *endpoint* and 
the *resource*. These two parameter are separated so that separate execution 
configurations to different *resources* can still share the same base *endpoint*
setting.

    <configuration>
      <endpoint>${endpoint}:${port}/</endpoint>
      <resource>api/queues/%2F/QueueName</resource>
    </configuration>

A REST request for Keycloak can be executed on multiple resources of one 
endpoint. To do so, you can add the placeholder *%4T* to the resource tag 
and add the *realms* tag to the configuration. The *realms* tag expects a 
comma separated list of placeholder values.

    <configuration>
      <endpoint>${endpoint}:${port}/</endpoint>
      <resource>auth/admin/realms/%4T/clients</resource>
      <realms>realm1, realm2</realms>
    </configuration>

In the example above you can see how a request is executed on the endpoint 
*${endpoint}:${port}/auth/admin/realms/realm1/clients* and 
*${endpoint}:${port}/auth/admin/realms/realm2/clients*.

## REST authorization

The requests need authorization, so the *authorization* tag must be added. 
To work properly, at least the *username* and *password* have to be specified:

    <configuration>
      <strategy>KEYCLOAK</strategy>
      <authorization>
        <username>${username}</username>
        <password>${password}</password>
      </authorization>
    </configuration>

If wanted, any number of additional *authorization* parameters can 
be specified for the *KEYCLOAK* strategy. Each parameter will be added 
to the request header:

    <configuration>
      <strategy>KEYCLOAK</strategy>
      <authorization>
        <username>${username}</username>
        <password>${password}</password>
         
        <grant_type>password</grant_type>
        <client_id>admin-cli</client_id>
      </authorization>
    </configuration>


## Adding source directories

To specify the input *fileSet*, you can add the following 
configurations. To use a single *fileSet*:

    <configuration>
      <fileSet>
        <directory>${basedir}/configs/keycloak/realms</directory>
        <includes>
          <include>**/*.json</include>
        </includes>
      </fileSet>
    </configuration>

To use multiple *fileSets*, just wrap the single *fileSet* 
in a *fileSets* list wrapper:

    <configuration>
      <fileSets>
        <fileSet>
          <directory>${basedir}/configs/keycloak/realms</directory>
          <includes>
            <include>**/*.json</include>
          </includes>
        </fileSet>
        <fileSet>
          <....>
        </fileSet>
      </fileSets>
    </configuration>