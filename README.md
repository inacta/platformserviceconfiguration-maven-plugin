# platformserviceconfiguration-maven-plugin

This plugin is meant to provide an easy way to configure services like Keycloak, 
RabbitMQ or MinIO via Maven. The supported features by now are:

| Application  | Features |
| ------------ |----------|
| Keycloak   | Creating (or deleting)<br> <ul><li>*REALMS*</li><li>*CLIENTS*</li><li>*USERS*</li><li>*ROLES*</li></ul> from JSON files|
| RabbitMQ   | <ul><li>Creating (or deleting) queues</li></ul> |
| MinIO      | <ul><li>Create a new bucket</li><li>Upload files to bucket root</li><li>Upload files to a target path in the bucket</li><li>Upload files from a folder with their relative folder structure</li></ul>|



## Table of Contents

- [Building from source](#building-from-source)
- [Available goal](#available-goal)
- [Getting started](#getting-started)
- [Configuration](#configuration)
    * [Choose application strategy](#choose-application-strategy)
    * [Set URL Endpoint](#set-url-endpoint)
    * [Add authorization credentials](#add-authorization-credentials)
    * [Add source directories](#add-source-directories)
    * [Specify resource](#specify-resource)
    * [Specify mode](#specify-mode)
    * [Add application specific parameters](#add-application-specific-parameters)
- [Example configurations](#example-configurations)
    * [Keycloak](#keycloak)
    * [RabbitMQ](#rabbitmq)
    * [MinIO](#minio)


## Building from source

Run from the command line: <br>
- `mvn clean install`

## Available goal

* `configure`


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


## Configuration

The configuration for the execution is specified within the *configuration* 
tag:

    <configuration>
      ...
    </configuration>


### Choose application strategy

The *application* tag must specify which application should be configured:

    <configuration>
      <application>KEYCLOAK</application>
    </configuration>

Supported application strategies by now are:
- `KEYCLOAK`
- `RABBITMQ`
- `MINIO`


### Set URL Endpoint

The URL endpoint under which the application can be reached must be specified 
via the parameter *endpoint*:

    <configuration>
      <endpoint>${application.host}/</endpoint>
    </configuration>


### Add authorization credentials

The executions of the configurations need authorization, so the *authorization* 
tag must be added. To work properly, the *username* and *password* have to be 
specified:

    <configuration>
      <authorization>
        <username>${username}</username>
        <password>${password}</password>
      </authorization>
    </configuration>


### Add source directories

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


### Specify resource

The *resource* tag has different meanings, depending on the application
strategy:

| Application  | Meaning | Example |
| ------------ |---------|---------|
| *KEYCLOAK*   | Specifies which resource shall be created. <br> Possible values: <br> <ul><li>*REALMS*</li><li>*CLIENTS*</li><li>*USERS*</li><li>*ROLES*</li></ul> | `<resource>REALMS</resource>` |
| *RABBITMQ*   | Specifies the queue which has to be created | `<resource>${virtual.host}/${queue}</resource>` |
| *MINIO*      | Specifies the target path in the MinIO where the files are uploaded.<br>If not set, the files are uploaded to the bucket root. | `<resource>images/png</resource>` |


### Specify mode

In order to delete a resource instead of creating it, simply specify the *mode* tag with *DELETE*:

    <configuration>
      <mode>DELETE</mode>
    </configuration>

The default value of the *mode* tag is *CREATE*.

### Add application specific parameters

| Parameter  | Application | Explanation | Example |
| -----------|-------------|-------------|---------|
| *realms*   | *KEYCLOAK*  | Comma separated list of realms for which the *resource* shall be created.<br> Only mandatory for creating *clients* and *users*. | `<realms>realm1, realm2</realms>` |
| *bucket*   | *MINIO*     | Specifies the bucket in the MinIO. | `<bucket>${bucket.name}</bucket>` |
| *relative* | *MINIO*     | Uploads files with their relative folder structure to MinIO. <br> If not set to *true*, the files will be uploaded "flatten" to bucket root or *resource* path. | `<relative>true</relative>` |

## Example configurations

### Keycloak

Create realms and clients:

    <profile>
      <id>configureKeycloak</id>
      <build>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>create-realm</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>KEYCLOAK</application>
                  <endpoint>${keycloak.host}/</endpoint>
                  <resource>realms</resource>
                  <authorization>
                    <username>${keycloak.admin.user}</username>
                    <password>${keycloak.admin.user.password}</password>
                  </authorization>
                  <fileSets>
                    <fileSet>
                      <directory>${project.basedir}/configs/keycloak/realms</directory>
                      <includes>
                        <include>**/*.json</include>
                      </includes>
                    </fileSet>
                  </fileSets>
                </configuration>
              </execution>
              <execution>
                <id>create-clients</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>KEYCLOAK</application>
                  <endpoint>${keycloak.host}/</endpoint>
                  <resource>clients</resource>
                  <realms>realm1, realm2, realm3</realms>
                  <authorization>
                    <username>${keycloak.admin.user}</username>
                    <password>${keycloak.admin.user.password}</password>
                  </authorization>
                  <fileSets>
                    <fileSet>
                      <directory>${basedir}/configs/keycloak/clients</directory>
                      <includes>
                        <include>**/*.json</include>
                      </includes>
                    </fileSet>
                  </fileSets>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>

### RabbitMQ

Create a queue:

    <profile>
      <id>configureRabbitmq</id>
      <build>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>create-queue</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>RABBITMQ</application>
                  <endpoint>${rabbitmq.host}:${rabbitmq.port}/</endpoint>
                  <resource>${rabbitmq.virtual.host.name}/${rabbitmq.queue.name}</resource>
                  <authorization>
                    <username>${rabbitmq.user.name}</username>
                    <password>${rabbitmq.user.password}</password>
                  </authorization>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>

### MinIO

Create a bucket, upload XML files to bucket root and finally upload all files 
from a folder (including the subfolder structure) to a target path:

    <profile>
      <id>configureMinio</id>
      <build>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>make-bucket</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>MINIO</application>
                  <endpoint>${minio.host}</endpoint>
                  <bucket>${bucket1.name}</bucket>
                  <authorization>
                    <username>${minio.user.name}</username>
                    <password>${minio.user.password}</password>
                  </authorization>
                </configuration>
              </execution>
              <execution>
                <id>upload-xml-to-root</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>MINIO</application>
                  <endpoint>${minio.host}</endpoint>
                  <bucket>${bucket2.name}</bucket>
                  <authorization>
                    <username>${minio.user.name}</username>
                    <password>${minio.user.password}</password>
                  </authorization>
                  <fileSet>
                    <directory>${project.basedir}/source/configs</directory>
                    <includes>
                      <include>**/*.xml</include>
                    </includes>
                  </fileSet>
                </configuration>
              </execution>
              <execution>
                <id>upload-all-files-from-folder-to-path</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>MINIO</application>
                  <endpoint>${minio.host}</endpoint>
                  <bucket>${bucket3.name}</bucket>
                  <resource>html/images</resource>
                  <relative>true</relative>
                  <authorization>
                    <username>${minio.user.name}</username>
                    <password>${minio.user.password}</password>
                  </authorization>
                  <fileSet>
                    <directory>${project.basedir}/source/images</directory>
                    <includes>
                      <include>**/*</include>
                    </includes>
                  </fileSet>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>