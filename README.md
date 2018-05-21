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

Gerrit                -> http://10.0.2.15:8080
Jenkins               -> http://10.0.2.15:8181/jenkins
Archiva               -> http://10.0.2.15:8484
phpLDAPadmin          -> http://10.0.2.15:8383/phpldapadmin
Self Service Password -> http://10.0.2.15:8282/ssp
Apache Service        -> http://10.0.2.15:8585


## Instalación y configuración de SonarQube

Lanzaremos **SonarQube** como un contenedor Docker.

```bash
docker run -d --name codeurjc-forge-sonarqube -p 9000:9000 -p 9092:9092 sonarqube:alpine
```

Al nombrar la imagen de esta forma sonarqube hacemos que los scripts **stop.sh** y **resume.sh** de la forja paren y arranquen el servicio junto con el resto de la forja.

> **Nota:** Eso no persiste los datos. Habría que crear un volumen para tal efecto, pero para el desarrollo de la práctica no es absolutamente necesario, siempre y cuando no borremos los contenedores.

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

> **Nota: La carpeta **/tmp/video** debe ser creada en el host (el sistema linux que ejecuta la forja)

Para poder lanzar el System Test se ha preparado un fichero para **docker-compose**, el fichero ** 	system-test-docker-compose.yml** que lanza **Selenoid** y un contenedor Maven que ejecutará los tests usando dicho **Selenoid**.

Estos servicios se han de ejecutar usando el modo bridge de **docker-compose**, pues la imagen de **Selenoid** lanzará nuevos contenedores para cada uno de los browsers que se necesiten usar mediante comandos de **docker run** y dichos contenedores deben de estar en la misma red que el contenedor de **Selenoid**.

## Gerrit

En gerrit hay que crear el proyecto como **TicTacToe** e importarlo desde este

Project/New/
Project Name : TicTacToe
Rights Inherit From: awesome-project

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
```

La configuración de los diferentes jobs está en la carpeta **jenkins**.

Se deben crear los jobs en Jenkins como "Pipeline script from SCM"
- **Repository URL**: ssh://codeurjc-forge-gerrit:29418/TicTacToe
- **Credentials**: jenkins (Jenkins Master)
- **Script path**: jenkins/\<type>Jenkinsfile con \<type> un valor entre [merge, commit, nightly y release]

Cada job tendrá sus propios triggers.

### Job de commit

### Job de merge

### Jon de nighly

### Job de release



