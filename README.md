Para lanzar SonarQube

> docker run -d --name codeurjc-forge-sonarqube -p 9000:9000 -p 9092:9092 sonarqube:alpine

OJO! Eso no persiste los datos. Habría que crear un volumen y montarlo en el sitio adecuado, pero en general funciona bien para ir desarrollando la práctica


Configuración de Archiva:
Crear usuario jenkins con clave jenkins1 y añadirlo como manejador de los repos internal y snapshot

La configuración de los jobs está en la carpeta jenkins. No he probado a usarlos como Jenkinsfile, pero si se crea el job y se copia el contenido debería funcinar

Para Selenoid hay que hacer antes de nada:
> docker pull selenoid/vnc:chrome_66.0

> docker pull selenoid/video-recorder:latest

En gerrit hay que importar este proyecto!!!

