# platformserviceconfiguration-maven-plugin

This plugin is meant to provide an easy way to configure services like keycloak or 
rabbitmq via REST calls. This means it is only possible to perform PUT and POST 
requests.


## Available goals

* configure


## Getting started

To use this plugin and start working with it, declare the 
platformserviceconfiguration-maven-plugin and add a pluginManagement entry.

    <packaging>pom</packaging>
    ....
    <build>
      <pluginManagement>
        <plugins>
          <plugin>
            <groupId>ch.inacta.isp</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
          </plugin>
        </plugins>
      </pluginManagement>
      ....
      <plugins>
        <plugin>
          <groupId>ch.inacta.isp</groupId>
          <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
          ....
        </plugin>
      </plugins>
    </build>


## REST URL endpoint

The REST URL endpoint is specified via two parameters, the *endpoint* and 
the *resource*. The components are separated so that mapping separate 
execution configurations to different *resource* extensions can still share 
the same base *endpoint* setting.

    <configuration>
      <endpoint>http://localhost:15672/</endpoint>
      <resource>api/queues/%2F/QueueName</resource>
    </configuration>

A REST request can be executed on multiple resources of one endpoint. To 
do so, you can add the placeholder *%4T* to the resource tag and add the 
*realms* tag to the configuration. The *realms* tag expects a comma 
separated list of placeholder values.

    <configuration>
      <endpoint>http://localhost:8099/</endpoint>
      <resource>auth/admin/realms/%4T/clients</resource>
      <realms>realm1, realm2</realms>
    </configuration>

In the example above you can see how a request is executed on the endpoint 
*http://localhost:8099/auth/admin/realms/realm1/clients* and 
*http://localhost:8099/auth/admin/realms/realm2/clients*.


## REST method

The REST request method can be configured by adding the *method* tag. 
Supported methods are PUT and POST. The default value is POST. Because 
it is a configuration plugin it does not support GET requests.

    <configuration>
      <method>POST</method>
    </configuration>


## REST request type

The REST request type can be configured via the *requestType* tag. 
The default for requests is 'application/json'.

The request type parameter uses the *MediaType* datatype and 
consequently can be configured using the tags of the *MediaType* 
object. For example:

    <configuration>
      <requestType>
        <type>application</type>
        <subtype>json</subtype>
      </requestType>
    </configuration>


## REST authorization

If the request needs an additional authorization, the *authorization* 
tag can be added. To work properly at least the *username* and 
*password* have to be specified.

In addition to the *authorization* tag the *app* tag has to be 
defined. With the *app* tag it can be specified what kind of 
application it is. Supported applications are *keycloak* and 
*rabbitmq*.

    <configuration>
      <app>keycloak</app>
      <authorization>
        <username>admin</username>
        <password>Password_1</password>
      </authorization>
    </configuration>

If wanted any number of additional *authorization* parameters can 
be specified. Each parameter will be added to the request header.

    <configuration>
      <app>keycloak</app>
      <authorization>
        <username>admin</username>
        <password>Password_1</password>
         
        <grant_type>password</grant_type>
        <client_id>admin-cli</client_id>   
      </authorization>
    </configuration>


## Adding source directories

To specify the input *fileSet* you can add the following 
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