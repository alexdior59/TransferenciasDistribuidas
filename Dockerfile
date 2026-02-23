# Usamos una imagen ligera de Java 17
FROM eclipse-temurin:17-jdk-alpine

# Definimos un volumen para los archivos temporales de Tomcat
VOLUME /tmp

# Copiamos el JAR generado por Maven a la imagen de Docker
COPY target/transferencias-distribuidas-1.0.0.jar app.jar

# Exponemos el puerto 8080
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java","-jar","/app.jar"]