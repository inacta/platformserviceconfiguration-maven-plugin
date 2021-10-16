# platformserviceconfiguration-maven-plugin

This plugin is meant to provide an easy way to configure services like Keycloak, 
RabbitMQ, Postgres or MinIO via Maven. The supported features by now are:

| Application  | Features |
| ------------ |----------|
| Keycloak   | Creating (or deleting)<br> <ul><li>*REALMS*</li><li>*CLIENTS*</li><li>*USERS*</li><li>*ROLES*</li></ul> from JSON files|
| RabbitMQ   | <ul><li>Creating (or deleting) queues</li></ul> |
| MinIO      | <ul><li>Create a new bucket</li><li>Upload files to bucket root</li><li>Upload files to a target path in the bucket</li><li>Upload files from a folder with their relative folder structure</li></ul>|
| Postgres   | <ul><li>Creating (or deleting) databases</li><li>Creating (or deleting) users (with password)</li><li>Apply SQL scripts</li></ul> |
| I18N       | Creating (or deleting) i18n entries for<br> <ul><li>*SELECTION_LIST*</li><li>*TEMPLATE*</li><li>*LABEL*</li></ul> |



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
    * [Specify resource name](#specify-resource-name)
    * [Specify resource password](#specify-resource-password)
    * [Specify mode](#specify-mode)
    * [Add application specific parameters](#add-application-specific-parameters)
- [Example configurations](#example-configurations)
    * [Keycloak](#keycloak)
    * [RabbitMQ](#rabbitmq)
    * [MinIO](#minio)
    * [Postgres](#postgres)
    * [I18N](#i18n)


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
- `POSTGRES`
- `I18N`


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

The *resource* tag has different meanings, depending on the application strategy:

| Application  | Meaning | Example |
| ------------ |---------|---------|
| *KEYCLOAK*   | Specifies which resource shall be created. <br> Possible values: <br> <ul><li>*REALMS*</li><li>*CLIENTS*</li><li>*USERS*</li><li>*ROLES*</li></ul> | `<resource>REALMS</resource>` |
| *RABBITMQ*   | Specifies which resource shall be created. <br> Possible values: <br> <ul><li>*QUEUE*</li></ul> | `<resource>QUEUE</resource>` |
| *MINIO*      | Specifies the target path in the MinIO where the files are uploaded.<br>If not set, the files are uploaded to the bucket root. | `<resource>images/png</resource>` |
| *POSTGRES*   | Specifies which resource shall be created. <br> Possible values: <br> <ul><li>*DATABASE*</li><li>*USER*</li><li>*SCRIPTS*</li></ul> | `<resource>DATABASE</resource>` |
| *I18N*       | Specifies which resource shall be created. <br> Possible values: <br> <ul><li>*SELECTION_LIST*</li><li>*TEMPLATE*</li><li>*LABEL*</li></ul> | `<resource>TEMPLATE</resource>` |

### Specify resource name

The *resourceName* tag specifies the name of the *resource* which shall be created:

| Application  | Meaning | Example |
| ------------ |---------|---------|
| *RABBITMQ*   | Specifies the queue name which shall be created. | `<resourceName>${virtual.host}/${queue}</resourceName>` |
| *POSTGRES*   | Specifies the database name or user name which shall be created. | `<resourceName>${databaseName}</resourceName>` |

### Specify resource password

The *resourcePassword* tag specifies the password of the *resource* which shall be created:

| Application  | Meaning | Example |
| ------------ |---------|---------|
| *POSTGRES*   | Specifies the password of the user which shall be created. | `<resourcePassword>${databaseUserPassword}</resourcePassword>` |

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
                  <resource>queue</resource>
                  <resourceName>${rabbitmq.virtual.host.name}/${rabbitmq.queue.name}</resourceName>
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
    
### Postgres

Create a user, a database and apply SQL scripts:

    <profile>
      <id>configurePostgres</id>
      <build>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>create-user</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>POSTGRES</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/${postgres.defaultDatabase}</endpoint>
                  <resource>user</resource>
                  <resourceName>${postgres.database.user.name}</resourceName>
                  <resourcePassword>${postgres.database.user.password}</resourcePassword>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                </configuration>
              </execution>
              <execution>
                <id>create-database</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>POSTGRES</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/${postgres.defaultDatabase}</endpoint>
                  <resource>database</resource>
                  <resourceName>${postgres.database.name}</resourceName>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                </configuration>
              </execution>
              <execution>
                <id>apply-scripts</id>
                <phase>initialize</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>POSTGRES</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/my_database</endpoint>
                  <resource>scripts</resource>
                  <fileSet>
                    <directory>${project.basedir}/scripts/my_database</directory>
                    <includes>
                      <include>**/*.sql</include>
                    </includes>
                  </fileSet>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                </configuration>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>

### I18N

Create an i18n entry for a selection list, a template and labels

    <profile>
      <id>configureI18N</id>
      <build>
        <plugins>
          <plugin>
            <groupId>ch.inacta.maven</groupId>
            <artifactId>platformserviceconfiguration-maven-plugin</artifactId>
            <executions>
              <execution>
                <id>selection_list</id>
                <phase>compile</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>I18N</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/my_database_with_i18n_tables</endpoint>
                  <mode>CREATE</mode>
                  <resource>selection_list</resource>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                  <fileSets>
                    <fileSet>
                      <directory>${project.basedir}/script/database/selectionlists</directory>
                      <includes>
                        <include>**/*.json</include>
                      </includes>
                    </fileSet>
                  </fileSets>
                </configuration>
              </execution>
              <execution>
                <id>template</id>
                <phase>compile</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>I18N</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/my_database_with_i18n_tables</endpoint>
                  <mode>CREATE</mode>
                  <resource>template</resource>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                  <fileSets>
                    <fileSet>
                      <directory>${project.basedir}/script/database/templates</directory>
                      <includes>
                        <include>**/*.html</include>
                      </includes>
                    </fileSet>
                  </fileSets>
                </configuration>
              </execution>
              <execution>
                <id>labels</id>
                <phase>compile</phase>
                <goals>
                  <goal>configure</goal>
                </goals>
                <configuration>
                  <application>I18N</application>
                  <endpoint>${postgres.jdbc.url}:${postgres.port}/my_database_with_i18n_tables</endpoint>
                  <mode>CREATE</mode>
                  <resource>label</resource>
                  <authorization>
                    <username>${postgres.admin.name}</username>
                    <password>${postgres.admin.password}</password>
                  </authorization>
                  <fileSets>
                    <fileSet>
                      <directory>${project.basedir}/script/database/labels</directory>
                      <includes>
                        <include>**/*.properties</include>
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