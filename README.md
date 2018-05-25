# Memoria para la práctica de CI del curso Experto en Aseguramiento de la calidad del Software

## Prerrequisitos de instalación.
1. Virtual Box con al menos 20G de disco vdi, caso contrario nos quedaremos sin espacio pronto.
2. Necesitaremos configurar el modo host only para acceder a los contenedores con una dirección privada. 
3. Instalar docker CE, seguimos los pasos: https://docs.docker.com/install/linux/docker-ce/ubuntu/#install-using-the-repository
4. Instalar docker-compose https://docs.docker.com/compose/install/ii
5. Tener corriendo el servicio openssh-server

## Configuración Red en ubuntu 14.04
* Una vez configurada la opción en Virtual Box para host only con dhcp, hay que modificar el fichero añadiendo el inteface nuevo que hemos obtenido con ifconfig -a:
Por ejemplo:

```bash
cd /etc/network/interfaces

# This file describes the network interfaces available on your system
# and how to activate them. For more information, see interfaces(5).

# The loopback network interface
auto lo
iface lo inet loopback

# The primary network interface
auto eth0
iface eth0 inet dhcp

# The secondary network interface
auto eth1
iface eth1 inet dhcp
```

## URLs

- Gerrit                -> http://10.0.2.15:8080
- Jenkins               -> http://10.0.2.15:8181/jenkins
- Archiva               -> http://10.0.2.15:8484
- phpLDAPadmin          -> http://10.0.2.15:8383/phpldapadmin
- Self Service Password -> http://10.0.2.15:8282/ssp
- Apache Service        -> http://10.0.2.15:8585


## Instalación y configuración de SonarQube

Lanzaremos **SonarQube** como un contenedor Docker.

```bash
docker run -d --name codeurjc-forge-sonarqube -p 9000:9000 -p 9092:9092 sonarqube:alpine
```

Al nombrar la imagen de esta forma sonarqube hacemos que los scripts **stop.sh** y **resume.sh** de la forja paren y arranquen el servicio junto con el resto de la forja.

> **Nota:** Eso no persiste los datos. Habría que crear un volumen para tal efecto, pero para el desarrollo de la práctica no es absolutamente necesario, siempre y cuando no borremos los contenedores.

## Gerrit

En gerrit hay que crear el proyecto como **TicTacToe**

```
Project/New/
Project Name : TicTacToe
Rights Inherit From: All-Projects
```

Para poder importar el proyecto de GitHub en Gerrit hay que hacer algunos cambios en la configuración del proyecto **TicTacToe** o en la de **All-Projects**

En la sección **Reference: refs/heads** hay que añadir el grupo **Developers** a los permisos que no tenga, pero especialmente a los permisos:

- Forge Author Identity
- Forge Committer Identity

Esto nos permitirá poder crear los parches de los commits que se ha hecho directamente al repositorio de github con usuarios no existentes en la forja.

También será necesario cambiar la opción **Require Change-Id in commit message:** de la pestaña **General** de configuración del proyecto de INHERIT a FALSE. Esto es necesario porque hay commits en el proyecto que no tienen **Change-Id**.

Una vez aplicados estos cambios ya podremos clonar el repositorio recién creado en nuestra máquina. Para ello se seguirán las instrucciones ya vistas en el **Tema 6 - Git avanzado - Code Review con Gerrit**

> **Nota:** Es posible que sea necesario crear algún usuario en el servidor **LDAP**

```bash
git clone ssh://<username>@<gerrit-ip>:29418/TicTacToe && scp -p -P 29418 <username>@<gerrit-ip>:hooks/commit-msg TicTacToe/.git/hooks/
```

Añadimos el remote de github, descargamos el código desde github y publicamos en Gerrit:
```bash
git remote add github   https://github.com/thar/TicTacToe.git
git pull --rebase github master
git push origin HEAD:refs/for/master
```

Una vez hecho esto, si entramos en gerrit con el usuario <username> tendremos la opción de revisar cambios y votarlos(+1). Si entramos con el usuario admin podremos directamente aprobar el merge (+2).

Con el código de github ya importado podemos deshacer los cambios realizados en la configuración del proyecto, más específicamente:

- **Require Change-Id in commit message:**: De FALSE a INHERIT
- **Forge Author Identity**: Eliminar el grupo **Developers**
- **Forge Committer Identity**: Eliminar el grupo **Developers**

## Archiva

El servicio de **Archiva** ya está en ejecución en la forja. Las credenciales de administrador son las mismas que Jenkins:

- **User**: admin
- **Password**: s3cr3t0

Una vez logueados como **admin** podemos proceder a la creación del usuario funcional para **Jenkins**. Este usuario será el encargado de guardar los binarios de las diferentes versiones del proyecto de ejemplo **TicTacToe**.

La configuración de los repositorios se ha dejado la que ya viene por defecto, pues hay un repositorio para **snapshot** (snapshot) y otro para **releases** (internal).

Se crea usuario **jenkins** con clave **jenkins1** y se añade como manejador de los repositorios **internal** y **snapshot**.

Es necesario indicarle a **Jenkins** los credenciales que ha de usar para guardar los artefactos en **Archiva**. Esto lo hacemos a través de la configuración general de Maven, creando el fichero **settings.xml**. Por defecto Maven busca este fichero en la ruta **$HOME/.m2/settings.xml**. Como las compilaciones se harán siempre usando contenedores docker con Maven, hemos creído que una buena opción es guardar el fichero de configuración en el repositorio del proyecto e indicar a Maven que use el directorio del proyecto como su home.

En el fichero **pom.xml** se configura el plugin de archiva para que maven pueda desplegar en Archiva.
Para que el contenedor que realiza la compilación y despliegue pueda acceder a Archiva se lanza en la misma red que la forja. De esta forma puede hacer uso del hostname **codeurjc-forge-archiva** para acceder al servicio. Esto ahorra muchos problemas para configurar los jobs en **Jenkins**.

## Selenoid

Usamos **Selenoid** para hacer las pruebas de **Selenium** y para grabar los vídeos de dichas pruebas, pero para que funcione bien hay que descargarse las imágenes docker de los navegadores a usar y del grabador de vídeos:

```bash
docker pull selenoid/vnc:chrome_66.0
docker pull selenoid/video-recorder:latest
```

Al ejecutar Selenoid dockerizado hay que indicarle un volumen para que guarde los vídeos. Ese volumen debe ser una carpeta del host que ejecuta docker.

En el proyecto usamos la carpeta **/tmp/video**.

> **Nota**: La carpeta **/tmp/video** debe ser creada en el host (el sistema linux que ejecuta la forja)

Para poder lanzar el System Test se ha preparado un fichero para **docker-compose**, el fichero **system-test-docker-compose.yml** que lanza **Selenoid** y un contenedor Maven que ejecutará los tests usando dicho **Selenoid**.

Estos servicios se han de ejecutar usando el modo bridge de **docker-compose**, pues la imagen de **Selenoid** lanzará nuevos contenedores para cada uno de los browsers que se necesiten usar mediante comandos de **docker run** y dichos contenedores deben de estar en la misma red que el contenedor de **Selenoid**.

En el docker file se establecen unas variables de entorno que modifican el comportamiento de los tests.

- ENABLE_VIDEO_RECORDING
- APP_HOST
- APP_PORT

Si la variable APP_HOST no se establece, los test de la aplicación lanzarán antes, en el mismo contendor la aplicación, y cuando terminen, pararán dicha aplicación. Esto se hace así para tratar el caso en el que se quiera testear la imagen docker que contiene la aplicación y se quieran lanzar los tests contra dicho contenedor (Nightly building).

```java
@BeforeClass
    public static void setupClass() {
        if (null == System.getenv("APP_HOST")) {
            WebApp.start();
        }
    }
 
    @AfterClass
    public static void teardownClass() {
        if (null == System.getenv("APP_HOST")) {
            WebApp.stop();
        }
    }
```

De la misma manera se ejecutará un set up general de todos los test de sistema para asegurar que la IP a la que se refieren del servicio es la propia del contenedor.

```java
@Before
    public void setupTest() throws Exception {
        if (null != System.getenv("APP_HOST")) {
            ownIp = getIp(System.getenv("APP_HOST")) + ":" + System.getenv("APP_PORT");
        } else {
            ownIp = new NetworkUtils().getIp4NonLoopbackAddressOfThisMachine().getHostAddress() + ":8080";
        }
 
        DesiredCapabilities capability_browser1 = DesiredCapabilities.chrome();
        if (null != System.getenv("ENABLE_VIDEO_RECORDING")) {
            capability_browser1.setCapability("enableVideo", true);
        }
 
        DesiredCapabilities capability_browser2 = DesiredCapabilities.chrome();
        browser1 = new RemoteWebDriver(new URL("http://selenoid:4444/wd/hub"), capability_browser1);
        browser2 = new RemoteWebDriver(new URL("http://selenoid:4444/wd/hub"), capability_browser2);
    }
```

## Jenkins

Debido a que el contenedor de **Jenkins** hace uso del usuario **jenkis**, que es diferente que el usuario del host, existen problemas a la hora de lanzar imágenes Docker desde el contenedor **Jenkins**. Para evitar estos problemas ha que modificar los permisos de docker.sock para que Jenkins pueda lanzar contenedores docker.

```bash
sudo chmod a+rw /var/run/docker.sock
```

También hay que instalar docker-compose (en el contenedor de Jenkins), que no viene por defecto, pues lo necesitaremos para correr los tests de system test.

Con la forja corriendo:

```bash
docker exec -ti -u root codeurjc-forge-jenkins bash
root@codeurjc-forge-jenkins$ curl -L https://github.com/docker/compose/releases/download/1.21.2/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose
chmod a+x /usr/local/bin/docker-compose
```

Para que **Jenkins** pueda enviar commits y tags a **Gerrit** es necesario configurar el usuario y email de git que se usará en los comandos de **git**.

Con la forja corriendo:

```bash
docker exec -ti codeurjc-forge-jenkins bash
jenkins@codeurjc-forge-jenkins$ git config --global user.email "jenkins@example.com"
jenkins@codeurjc-forge-jenkins$ git config --global user.name "Jenkins"
```

La configuración de los diferentes jobs está en la carpeta **jenkins**.

Se deben crear los jobs en Jenkins como "Pipeline script from SCM"
- **Repository URL**: ssh://codeurjc-forge-gerrit:29418/TicTacToe
- **Credentials**: jenkins (Jenkins Master)
- **Script path**: jenkins/\<type>Jenkinsfile con \<type> un valor entre [merge, commit, nightly y release]

Cada job tendrá sus propios triggers.

### Job de commit

#### Trigger

El trigger es **Patchset created** con los siguientes parámetros:

- **Gerrit Project**: TicTacToe
- **Type**: Path
- **Pattern**: \*\*

#### Pipeline

El contenido de **jenkins/commitJenkinsfile** es el siguiente:

```
pipeline {
    agent none 
    stages {
        stage('Checkout from GerritHub') {
            agent any
            when {
                expression { params.GERRIT_HOST == 'review.gerrithub.io' }
            }
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_REFSPEC"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: 'gerrithub-jenkins', \
              refspec: 'refs/changes/*:refs/changes/*', \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
            }
        }
        stage('Checkout') {
            agent any 
            when {
                expression { params.GERRIT_HOST != 'review.gerrithub.io' }
            }
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_REFSPEC"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: 'jenkins-master', \
              refspec: 'refs/changes/*:refs/changes/*', \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
            }
        }
        stage('Test') {
            agent {
                docker {
                    image 'maven:3-jdk-8-alpine'
                    args  '-v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)"'
                }
            }
            steps {
                sh 'mvn -Dtest=BoardTest,TicTacToeGameTest -Duser.home="$(pwd)" test'
            }
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml'
                }
            }
        }
    }
}
```


### Job de merge

#### Trigger

El trigger es **Ref Updated** con los siguientes parámetros:

- **Gerrit Project**: TicTacToe
- **Type**: Path
- **Pattern**: master

#### Pipeline

El contenido de **jenkins/mergeJenkinsfile** es el siguiente:

```
pipeline {
    agent none 
    environment {
        DOCKER_USERNAME = "thar" //Cambiar a ususario local de docker
    }
    stages {
        stage('Checkout from GerritHub') {
            agent any
            when {
                expression { params.GERRIT_HOST == 'review.gerrithub.io' }
            }
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_NEWREV"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: 'gerrithub-jenkins', \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
            }
        }
        stage('Checkout') {
            agent any 
            when {
                expression { params.GERRIT_HOST != 'review.gerrithub.io' }
            }
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_NEWREV"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: 'jenkins-master', \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
            }
        }
        stage('Test with video recording') {
            agent any
            steps {
                sh 'docker run --rm -v /tmp/video:/tmp/video alpine sh -c "rm /tmp/video/*.mp4 || true"'
                withEnv(['ENABLE_VIDEO_RECORDING=1']) {
                    sh 'docker-compose -f system-test-docker-compose.yml up --abort-on-container-exit --exit-code-from test'
                }
                sh 'export GID=$(id -g); export UID=$(id -u); docker run --rm -v codeurjc-forge-jenkins-volume:/var/jenkins_home -w "$(pwd)" alpine chown -R $UID:$GID .'
                sh 'docker run --rm -v /tmp/video:/tmp/video:z -w /tmp alpine chmod -R a+rw video'
                sh 'docker run --rm -v /tmp/video:/tmp/video:z -v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" alpine sh -c "mkdir -p video && cp /tmp/video/*.mp4 video/"'
            }
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml'
                    archiveArtifacts artifacts: 'video/*.mp4', onlyIfSuccessful: true
                }
                cleanup {
                    sh 'docker run --rm -v /tmp/video:/tmp/video alpine sh -c "rm /tmp/video/*.mp4 || true"'
                    sh 'rm video/*.mp4 || true'
                }
            }
        }
        stage('Analyze code and publish in SonarQube') {
            agent {
                docker {
                    image 'maven:3-jdk-8-alpine'
                    args  '-v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)"'
                }
            }
            steps {
                sh 'mvn sonar:sonar -Dsonar.host.url=http://172.17.0.2:9000 -Duser.home="$(pwd)"'
            }
        }
        stage('Create and deploy package') {
            agent {
                docker {
                    image 'maven:3-jdk-8-alpine'
                    args  '-v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" --network=ci-network'
                }
            }
            steps {
                sh 'mvn package deploy -Dmaven.test.skip=true -Duser.home="$(pwd)"'
            }
        }
        stage('Create and publish docker image') {
            agent any
            steps {
                sh 'docker build --build-arg GIT_COMMIT=\$(git rev-parse HEAD) --build-arg COMMIT_DATE=\$(git log -1 --format=%cd --date=format:%Y-%m-%dT%H:%M:%S) -f docker/Dockerfile -t thar/tic-tac-toe:dev .'
                sh 'docker push ${DOCKER_USERNAME}/tic-tac-toe:dev'
            }
        }
    }
}

```

### Job de nighly

#### Diferencia respecto a los anteriores Jobs

El job de nightly no se dispara por un evento en gerrit, se dispara por medio de un 'scheduler' y por lo tanto le tenemos que proporcionar los parámetros con los que conectarse al repo de gerrit.
#### Trigger

El trigger es **Build periodically** con los siguientes parámetros:

- **Schedule**: "H 4 \* \* \*"

Esto ejecutará el job aproximadamente a las 4 de la mañana.

#### Pipeline

El contenido de **jenkins/nightlyJenkinsfile_v0.1** es el siguiente:

```
pipeline {
    agent none

    environment {
        DOCKER_USERNAME = "thar" // cambiar a ususario local de Docker
        GERRIT_SCHEME = "ssh"
        GERRIT_HOST = "review.gerrithub.io" // cambiar a host de gerrit local
        GERRIT_PORT = 29418
        GERRIT_PROJECT = "thar/TicTacToe"   // si se usa gerrit local cambiar a "TicTacToe"
        GERRIT_BRANCH = "master"
        CREDENTIALS = "gerrithub-jenkins"   // si se usa gerrit local cambiar a "jenkins-master"
    }
    
    stages {
        stage('Checkout') {
            agent any
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_BRANCH"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: "$CREDENTIALS", \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
            }
        }
        stage('Create package') {
            agent {
                docker {
                    image 'maven:3-jdk-8-alpine'
                    args  '-v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" --network=ci-network'
                }   
            }   
            steps {
                sh 'mvn package -Dmaven.test.skip=true -Duser.home="$(pwd)"'
            }   
        }
        stage('Test Docker image and publish') {
            agent any
            steps {
                script {
                    DAY_TAG = sh(returnStdout: true, script: "date +%Y%m%d").trim()
                    APP_VERSION = sh(returnStdout: true, script: 'python -c "import xml.etree.ElementTree as ET; print(ET.parse(open(\'pom.xml\')).getroot().find(\'{http://maven.apache.org/POM/4.0.0}version\').text)"').trim()
                    echo "${DAY_TAG}"
                    echo "${APP_VERSION}"
                    DOCKER_TAG = "${APP_VERSION}" + ".nightly." + "${DAY_TAG}"
                }
                sh '''#!/bin/bash
                  APP_VERSION=$(python -c "import xml.etree.ElementTree as ET; print(ET.parse(open('pom.xml')).getroot().find('{http://maven.apache.org/POM/4.0.0}version').text)")
                  DOCKER_TAG=$APP_VERSION.nightly.$(date +%Y%m%d)
                  docker build --build-arg GIT_COMMIT=\$(git rev-parse HEAD) --build-arg COMMIT_DATE=\$(git log -1 --format=%cd --date=format:%Y-%m-%dT%H:%M:%S) -f docker/Dockerfile -t ${DOCKER_USERNAME}/tic-tac-toe:${DOCKER_TAG} .
                  docker-compose -f system-test-docker-compose.yml -f system-test-docker-compose.override.yml up --abort-on-container-exit --exit-code-from test
                '''
            }  
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml'
                    sh 'docker tag ${DOCKER_USERNAME}/tic-tac-toe:${DOCKER_TAG} ${DOCKER_USERNAME}/tic-tac-toe:nightly'
                    sh 'docker push ${DOCKER_USERNAME}/tic-tac-toe:nightly'
                }   
            }
        }
    }
}

```

### Job de release

#### Parametros

Es necesario indicar que el job es parametrizado. Esto se hace marcando la opción **This project is parameterised** en la sección **General**

Se debe añadir un parámetro de tipo **String Parameter**.

En el campo **Name** pondremos **NEW_VERSION**

#### Trigger

El trigger es manual, por lo que no hay que setear nada

#### Pipeline

El contenido de **jenkins/releaseJenkinsfile_v0.1** es el siguiente:

```
pipeline {
    agent none

    environment {
        DOCKER_USERNAME = "thar"
        GERRIT_SCHEME = "ssh"
        GERRIT_HOST = "review.gerrithub.io"
        GERRIT_PORT = 29418
        GERRIT_PROJECT = "thar/TicTacToe"
        GERRIT_BRANCH = "master"
        CREDENTIALS = "gerrithub-jenkins"
    }
    
    stages {
        stage('Checkout') {
            agent any
            steps {
              checkout poll: false, \
              scm: [$class: 'GitSCM', \
              branches: [[name: "$GERRIT_BRANCH"]], \
              doGenerateSubmoduleConfigurations: false, \
              extensions: [[$class: 'BuildChooserSetting', buildChooser: [$class: 'GerritTriggerBuildChooser']]], \
              submoduleCfg: [], \
              userRemoteConfigs: [[credentialsId: "$CREDENTIALS", \
              url: "$GERRIT_SCHEME://$GERRIT_HOST:$GERRIT_PORT/$GERRIT_PROJECT"]]]
              sh "scp -p -P ${GERRIT_PORT} ${GERRIT_HOST}:hooks/commit-msg .git/hooks/"
            }
        }
        stage('remove SNAPSHOT version and test') {
            agent any 
            steps {
                script {
                    env.APP_VERSION = sh(returnStdout: true, script: 'python -c "import xml.etree.ElementTree as ET; print(ET.parse(open(\'pom.xml\')).getroot().find(\'{http://maven.apache.org/POM/4.0.0}version\').text)"').trim()
                    env.APP_VERSION = env.APP_VERSION.split('-')[0]
                    sh 'docker run --rm -v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" --network=ci-network maven:3-jdk-8-alpine mvn versions:set -DnewVersion=${APP_VERSION} -f pom.xml -Duser.home="$(pwd)"'
                    sh 'docker-compose -f system-test-docker-compose.yml up --abort-on-container-exit --exit-code-from test'
                    junit 'target/surefire-reports/**/*.xml'
                    sh 'docker run --rm -v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" --network=ci-network maven:3-jdk-8-alpine mvn package deploy -Dmaven.test.skip=true -Duser.home="$(pwd)"'
                    sh '''#!/bin/bash
                        docker build --build-arg GIT_COMMIT=\$(git rev-parse HEAD) --build-arg COMMIT_DATE=\$(git log -1 --format=%cd --date=format:%Y-%m-%dT%H:%M:%S) -f docker/Dockerfile -t ${DOCKER_USERNAME}/tic-tac-toe:${APP_VERSION} .
                    '''
                    sh 'docker tag ${DOCKER_USERNAME}/tic-tac-toe:${APP_VERSION} ${DOCKER_USERNAME}/tic-tac-toe:latest'
                    sh 'docker push ${DOCKER_USERNAME}/tic-tac-toe:latest'
                    sh 'docker push ${DOCKER_USERNAME}/tic-tac-toe:${APP_VERSION}'
                    sh '''#!/bin/bash
                      git tag -am "${APP_VERSION} tag" ${APP_VERSION}
                      git push origin ${APP_VERSION} HEAD:refs/heads/master
                    '''
                    env.NEW_VERSION = env.NEW_VERSION.split('-')[0] + '-SNAPSHOT'
                    sh 'docker run --rm -v codeurjc-forge-jenkins-volume:/var/jenkins_home:z -w "$(pwd)" --network=ci-network maven:3-jdk-8-alpine mvn versions:set -DnewVersion=${NEW_VERSION} -f pom.xml -Duser.home="$(pwd)"'
                    sh '''#!/bin/bash
                      git add pom.xml
                      git commit -m "Update version to ${NEW_VERSION}
                      git push origin HEAD:refs/for/master
                    '''
                }
            }
        }
    }
}
```

## Integración con GitHub y GerritHub

Se ha preparado el proyecto para que se pueda evitar el uso del **Gerrit** de la forja y en su lugar se use como repositorio de código **GitHub** y como enlace con **Jenkins** y sistema de revisión de código **GerritHub**.

Para poder hacer esto son necesarios los siguiente pasos:

- Crear el repositorio en GitHub
- Importar el proyecto a GerritHub
- Crear un usuario de github que hará las veces de usuario funcional de Jenkins de cara a GerritHub
- Enlazar el nuevo usuario en la página de GerritHub y añadir la clave SSH del usuario jenkins del contenedor codeurjc-forge-jenkins
- En la configuración del proyecto en GerritHub hay que dar permiso al nuevo usuario creado para que pueda votar
- Crear un nuevo credencial para el usuario funcional de Jenkins que acabamos de crear usando como nombre de usuario el del usuario de GitHub que hemos creado y como fichero SSH el que ya existe por defecto en el home del contenedor Jenkins
- Crear un nuevo servidor de Gerrit en nuestro servidor de Jenkins usando los credenciales recien creados
- En la configuración del proyecto en GerritHub hay que dar permiso al nuevo usuario creado para que pueda votar

Una vez realizados estos pasos es posible mandar commits a GerritHub y nuestro servidor de Jenkins ejecutará los test que configuremos para dicho repositorio y podrá votar en el proceso de code-review

Para poder usar indistintamente el servidor local de Gerrit y GerritHub se han modificado los Jenkinsfile de forma que ahora comprueban el origen del cambio y usan los credenciales creados para gerrithub o el de jenkins-master de forma adecuada para poder hacer un checkout del código


# Problemas encontrados

- Muy complicado correr la forja en portátiles sin la capacidad suficiente de disco duro y memoria RAM. Hemos obtenido mensajes de error de Virtual Box en muchas ocasiones mientras estábamos trabajando con la forja. Teniendo que volver a ejecutar la máquina virtual, ejecutar de nuevo ./resume.sh , etc.

