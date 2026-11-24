# Promeia Music · Android 1.0

Proyecto de reproductor local para Android 8.0 o superior (Java, Android SDK 35). El código fuente no incluye claves privadas ni archivos musicales. Esta entrega es un prototipo: la sintaxis Java fue revisada, pero la compilación y las pruebas en un teléfono siguen pendientes.

## Primer uso

1. Abre Biblioteca → Elegir carpeta Promeia Music. Selecciona una carpeta local con permiso de escritura, por ejemplo Documentos, o crea y elige directamente `Promeia Music`.
2. Usa Importar audio y selecciona los archivos deseados. La aplicación conserva el original y crea una copia en su biblioteca. No escanea la totalidad del dispositivo. Los duplicados exactos se identifican por SHA-256.
3. Toca una canción para reproducirla; el menú ⋮ permite editar datos, buscar carátula, agregar a playlist, compartir o eliminar la copia.

```
Promeia Music/
  Music/           # copias de audio importadas y descargas directas autorizadas
  Covers/          # imágenes de canciones y playlists
  biblioteca.json  # catálogo, metadatos, favoritos y playlists
```

Los cambios de metadatos son persistentes en el catálogo; no reescriben las etiquetas ID3 del archivo de audio. La exportación JSON es una copia del catálogo, no un respaldo de los audios; copia la carpeta completa para conservarlos.

## Smart Cover

Biblioteca → menú ⋮ de una canción → Carátula → Smart Cover.

- Toma el título y el artista actuales, y genera la consulta `título artista carátula álbum`.
- Si no hay artista, solicita que lo introduzcas antes de buscar.
- Abre Google Imágenes en el navegador. Puedes guardar la imagen y seleccionarla al volver con Importar carátula, compartir un archivo de imagen con Promeia o introducir una URL HTTPS directa de imagen.
- También puedes modificar manualmente la consulta. La imagen se guarda en `Covers` y queda vinculada a la canción concreta, sin cambiar los demás archivos.
- Usa imágenes que tengas derecho a utilizar. Google Custom Search JSON API está cerrada a clientes nuevos y será descontinuada el 1 de enero de 2027: https://developers.google.com/custom-search/v1/overview. Por eso la búsqueda no depende de esa API y los resultados se visualizan en el navegador, no en una cuadrícula nativa.

## YouTube y descargas

1. Crea un proyecto en https://console.cloud.google.com/ y habilita YouTube Data API v3: https://console.cloud.google.com/apis/library/youtube.googleapis.com
2. En Credenciales, genera una clave. Restringe la clave a YouTube Data API v3, supervisa su uso y no la subas al repositorio. Para distribución pública conviene un backend que proteja la clave. La versión de uso personal la guarda en preferencias privadas de Android, no cifrada.
3. En Promeia → Ajustes → Configurar API de YouTube, introduce la clave. En Buscar puedes consultar canciones por nombre, copiar el enlace del resultado o abrir YouTube. La búsqueda usa `search.list` oficial: https://developers.google.com/youtube/v3/docs/search/list
4. El botón Abrir sitio externo abre y2mate.gs en el navegador, sin integración con el descargador ni garantía sobre ese sitio. Usa únicamente fuentes y contenidos cuya descarga esté autorizada. Los archivos guardados por el navegador se incorporan con Importar audio descargado. No se interceptan las descargas del navegador.
5. Descargar audio autorizado admite una URL HTTPS directa de audio (sin redirecciones) y copia el archivo a `Promeia Music/Music`. No extrae audio de YouTube.

## Letras y sonido

- LRCLIB proporciona letras cuando existen; la app muestra la línea correspondiente al tiempo de reproducción si hay marcas LRC.
- ML Kit identifica el idioma y traduce al español, descargando el modelo si es necesario. La disponibilidad y la calidad de la traducción varían. No todas las canciones tienen letras.
- Ecualizador de cinco bandas, refuerzo de graves, control de agudos por la banda alta y ocho perfiles (incluido personalizado). Depende del soporte de efectos del dispositivo.
- Servicio en primer plano, sesión multimedia, controles de notificación/pantalla de bloqueo y rutas de audio del sistema (Bluetooth y auriculares).

## Compilar APK con Android Studio

Abre la carpeta de este proyecto en Android Studio con JDK 17, SDK 35 y Gradle 8.9. Sincroniza Gradle y selecciona Build APK(s). También puedes ejecutar `gradle :app:assembleDebug` con Gradle 8.9 instalado. El archivo resultante será `app/build/outputs/apk/debug/app-debug.apk`.

## Compilar APK con GitHub Actions (megacate30)

1. Crea un repositorio, por ejemplo `PromeiaMusic`, en https://github.com/megacate30 (o tu cuenta activa).
2. Sube el contenido de la carpeta del proyecto a la raíz del repositorio, incluyendo `.github/workflows/android.yml` (archivo oculto).
3. Abre Actions → Build Android APK → Run workflow; también se ejecuta al hacer push a main o master.
4. Si termina correctamente, descarga el artefacto `Promeia-Music-debug`, descomprímelo e instala `app-debug.apk`. Android puede solicitar permiso para instalar desde esa fuente.

El flujo incluye compilación y lint. No se ha ejecutado desde este entorno. No se creó un repositorio ni se accedió a la cuenta GitHub. La APK de depuración no está firmada para publicación; para publicar crea una firma de lanzamiento propia.

## Pruebas recomendadas

- Importar MP3/FLAC y el mismo archivo dos veces; comprobar almacenamiento y duplicados.
- Reproducir, bloquear pantalla, pausar y saltar desde notificaciones y auriculares.
- Crear playlist, reordenar, editar título y artista, reiniciar y comprobar persistencia.
- Smart Cover: canción con artista, sin artista, búsqueda manual, importación de imagen y reapertura de la canción.
- API YouTube: búsqueda con clave válida, copiar enlace, error con clave inválida o sin red.
- Letras con y sin sincronización; traducción y descarga inicial del modelo.
- Ecualizador: activar/desactivar, guardar perfiles y comprobar en un equipo compatible.
