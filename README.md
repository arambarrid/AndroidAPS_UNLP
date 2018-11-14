# AndroidAPS

* Check the wiki: http://wiki.androidaps.org
*  Everyone who’s been looping with AndroidAPS needs to fill out the form after 3 days of looping  https://docs.google.com/forms/d/14KcMjlINPMJHVt28MDRupa4sz4DDIooI4SrW0P3HSN8/viewform?c=0&w=1

[![Gitter](https://badges.gitter.im/MilosKozak/AndroidAPS.svg)](https://gitter.im/MilosKozak/AndroidAPS?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build status](https://travis-ci.org/MilosKozak/AndroidAPS.svg?branch=master)](https://travis-ci.org/MilosKozak/AndroidAPS)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/androidaps/localized.svg)](https://translations.androidaps.org/project/androidaps)
[![Documentation Status](https://readthedocs.org/projects/androidaps/badge/?version=latest)](https://androidaps.readthedocs.io/en/latest/?badge=latest)
[![codecov](https://codecov.io/gh/MilosKozak/AndroidAPS/branch/master/graph/badge.svg)](https://codecov.io/gh/MilosKozak/AndroidAPS)
dev: [![codecov](https://codecov.io/gh/MilosKozak/AndroidAPS/branch/dev/graph/badge.svg)](https://codecov.io/gh/MilosKozak/AndroidAPS)


[![Donate via PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=Y4LHGJJESAVB8)


# Git
Pequeño instructivo *informal* que suelo usar, hecho por el compañero Augusto Bonifacio allá por el 2016.

### Primero
Hay que clonar el repositorio.
``` sh
$ git clone https://github.com/arambarrid/AndroidAPS_UNLP
$ git remote -v # para ver si lo agrego bien
origin	https://github.com/arambarrid/AndroidAPS_UNLP (fetch)
origin	https://github.com/arambarrid/AndroidAPS_UNLP (push) #si falta esta es porque no tenés permiso para pushear
``` 
Va a crear una carpeta AndroidAPS_UNLP en la carpeta dónde esté parada la consola.
### Antes de trabajar
Hay que actualizar el código local con el del repo. La primera vez no tiene sentido porque el clone ya está actualizado
``` sh
$ git pull origin master
``` 

Después hay que crear la rama donde va a trabajar cada uno.
``` sh
$ git branch #para ver las ramas actuales
$ git checkout develop #para ir a la rama develop
$ git branch <nombre> #para crear la rama <nombre>
$ git checkout <nombre> #para pararse en la rama <nombre>
``` 
Los últimos dos comandos se pueden cambiar por uno solo  que es
``` sh
$ git checkout -b <nombre> #crea y se para
``` 
###### Igual lo de crear la rama se hace una vez, fue un dato nomas.

Cuando visualizas las ramas con `git branch` el asterisco indica en cuál estás parado.
### Manqueando
Ahora ya se puede empezar a editar, **es importante no tocar archivos que no sean nuestros**, si hay que cambiar algo ajeno se guarda lo que hicimos y se espera a que el encargado de esa parte lo cambie.

Si ya terminamos con la partecita de código cambiada hay que guardar los cambios. Para tener un mapeo de qué cambiamos hacemos
``` sh
$ git status
``` 
Esto sirve para verificar que no pisamos nada que no sea nuestro y también para escribir el mensaje del commit sin olvidarnos de contar lo que hicimos.

```
En la rama <nombre>
Cambios no preparados para el commit:
  (use «git add <archivo>...» para actualizar lo que se confirmará)
  (use «git checkout -- <archivo>...» para descartar cambios en el directorio de trabajo)

	modificado:    README.md

no hay cambios agregados al commit (use «git add» o «git commit -a»)
```
El `git status` imprime algo así (si hacen `git diff` se ven las lineas de código también). Detecta los cambios pero dice que no esta preparado para el commit porque lo edité pero todavía no lo agregué. Para agregarlo se puede hacer:
```sh
$ git add README.md # agrega el archivo sólo
```
O sino
```sh
$ git add . # agrega todos los archivos de la carpeta (parado en el AndroidAPS_UNLP) 
```

Esto del add se hace siempre que se creen archivos nuevos o se renombren. Lo más fácil es no tener archivos que no sirvan dentro del proyecto para poder hacer `git add .` en vez de especificar uno por uno.

Ahora viene la parte del commit para informar los cambios hechos. El comando es:
```sh
$ git commit -m 'Actualizado el README' # que sea descriptivo pero corto como un título
```
`-m` es de mensaje, es el que se ve en GitHub al lado de los archivos  y es para llevar un historial de los cambios.

Entonces cada vez que creamos algo tenemos que hacer `git add .`y `git commit -m '...'`. O se puede hacer de una así:
```sh
$ git commit -a -m 'Actualizado el README'
```
### Para subirlo
Ahora queda la parte que puede fallar, la de meterlo en el repo. Para esto hay que hacer un `merge` con la rama develop. Entonces nos paramos en la rama develop, solicitamos un `merge` con nuestra rama y si todo da ok la subimos. Si nada falla
sería:
``` sh
$ git checkout develop # entramos a develop
$ git merge <nombre> # develop + <nombre>
$ git push origin develop #lo sube
```
Lo que puede pasar es que alguien haya mergeado mientras nosotros editabamos entonces nuestra master estaría desactualizada. Ni bien hacemos el `checkout master` la consola te dice si está actualizada o no. Esto se soluciona muy facil con `git pull origin master`. El tema es que si lo editado por otro pisa con lo que editamos nosotros, por lo que vamos a tratar de solucionarlo en grupo. 

**Ahora están parados en la rama develop, si van a seguir editando no hay que olvidarse de pasar a la de cada uno porque sino es un lío**.

### Resumen

Una vez que ya está todo configurado los pasos para editar son
```sh
$ git pull origin develop
$ git checkout <nombre>
####### TRABAJAR #######
$ git status #opcional
$ git commit -a -m 'Mensaje de commit'
$ git checkout develop # si dice que esta desactualizada-> git pull origin develop
$ git merge <nombre>
$ git push origin develop
$ git checkout <nombre> # para que quede ahí y no olvidarse
```

