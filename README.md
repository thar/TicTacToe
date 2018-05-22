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

Importante configurar el directorio para introducir siempre en todo commit un changeId :

```bash
gitdir=$(git rev-parse --git-dir); scp -p -P 29418 dev1@localhost:hooks/commit-msg ${gitdir}/hooks/
chmod +x .git/hooks/commit-msg
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

> **Nota: La carpeta **/tmp/video** debe ser creada en el host (el sistema linux que ejecuta la forja)

Para poder lanzar el System Test se ha preparado un fichero para **docker-compose**, el fichero ** 	system-test-docker-compose.yml** que lanza **Selenoid** y un contenedor Maven que ejecutará los tests usando dicho **Selenoid**.

Estos servicios se han de ejecutar usando el modo bridge de **docker-compose**, pues la imagen de **Selenoid** lanzará nuevos contenedores para cada uno de los browsers que se necesiten usar mediante comandos de **docker run** y dichos contenedores deben de estar en la misma red que el contenedor de **Selenoid**.

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
- Problemas con Jenkins y repositorios. En alguna ejecución de los jobs, Jenkins ha dado un error de corrupción de objetos en el .git del job. Ha sido necesario entrar dentro del contenedor de Jenkins para borrar manualmente el job en el workspace correspondiente. /var/jenkins_home/workspace/jobCommit, etc. Una vez borrados estos jobs del Workspace todo ha vuelto a funcionar.
