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


## SonarQube

Para lanzar SonarQube

```
[~]$ docker run -d --name codeurjc-forge-sonarqube -p 9000:9000 -p 9092:9092 sonarqube:alpine
```

Al ejecutar de esta forma sonarqube hacemos que los scripts **stop.sh** y **resume.sh** de la forja paren y arranquen el servicio junto con el resto de la forja.

> **Nota:** Eso no persiste los datos. Habría que crear un volumen y montarlo en el sitio adecuado, pero en general funciona bien para ir desarrollando la práctica



## Selenoid

Usamos **Selenoid** para hacer las pruebas de **Selenium** y para grabar los vídeos de dichas pruebas, pero para que funcione bien hay que descargarse las imágenes docker de los navegadores a usar y del grabador de vídeos:

```
[~]$ docker pull selenoid/vnc:chrome_66.0
[~]$ docker pull selenoid/video-recorder:latest
```

Al ejecutar Selenoid dockerizado hay que indicarle un volumen para que guarde los vídeos. Ese volumen debe ser una carpeta del host que ejecuta docker.

En el proyecto usamos la carpeta **/tmp/video**.

> **Nota: La carpeta **/tmp/video** debe ser creada en el host (el sistema linux que ejecuta la forja)


## Archiva


Crear usuario **jenkins** con clave **jenkins1** y añadirlo como manejador de los repos internal y snapshot.

El fichero de settings de maven que configura Archiva en el proyecto está en **.m2/settings.xml**. No hay que hacer nada para que los jobs lo usen.

En el fichero **pom.xml** se configura el plugin de archiva para que maven pueda desplegar en Archiva.


## Jenkins

La configuración de los jobs está en la carpeta **jenkins**.

Se deben crear los jobs en Jenkins como "Pipeline script from SCM"
- **Repository URL**: ssh://codeurjc-forge-gerrit:29418/TicTacToe
- **Credentials**: jenkins (Jenkins Master)
- **Script path**: jenkins/\<type>Jenkinsfile con \<type> un valor de [merge, commit]


Hay que modificar los permisos de docker.sock para que Jenkins pueda lanzar contenedores docker

```
[~]$ sudo chmod a+rw /var/run/docker.sock
```

También hay que instalar docker-compose (en el contenedor de Jenkins), que no viene por defecto.
Con la forja corriendo:

```
[~]$ docker exec -ti -u root codeurjc-forge-jenkins bash
root@codeurjc-forge-jenkins$ curl -L https://github.com/docker/compose/releases/download/1.21.2/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose
```

## Gerrit

En gerrit hay que crear el proyecto como **TicTacToe** e importarlo desde este

Project/New/
Project Name : TicTacToe
Rights Inherit From: awesome-project


