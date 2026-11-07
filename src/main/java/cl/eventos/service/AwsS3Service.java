package cl.eventos.service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class AwsS3Service {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Service.class);

    @Autowired
    private S3Template s3Template;

    @Value("${aws.s3.bucket}")
    private String bucket;


    // 1. Subir archivo al bucket 

    public String subirArchivo(Long numeroResumen, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o es nulo");
        }

        String key = numeroResumen + "/" + file.getOriginalFilename();
        log.info("Subiendo archivo: {} al bucket: {}", key, bucket);

        s3Template.upload(bucket, key, file.getInputStream());
        log.info("Archivo subido exitosamente a S3: {}", key);
        return key;
    }
    
    // Subimos archivo al bucket leyendo desde el disco montado de EFS
    
    public String subirArchivo(Long numeroResumen, File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("El archivo en EFS no existe o es nulo");
        }

        String key = numeroResumen + "/" + file.getName();
        log.info("Subiendo archivo desde EFS: {} al bucket: {}", key, bucket);

        try (InputStream inputStream = new FileInputStream(file)) {
            s3Template.upload(bucket, key, inputStream);
        }

        log.info("Archivo subido exitosamente a S3 desde EFS: {}", key);
        return key;
    }

    // 2. Modificar archivo 

    public String modificarArchivo(Long numeroResumen, MultipartFile file) throws IOException {
        return subirArchivo(numeroResumen, file);
    }

    // AGREGADO PARA EFS

    public String modificarArchivo(Long numeroResumen, File file) throws IOException {
        return subirArchivo(numeroResumen, file);
    }

    // 3. Descargar archivo

    public S3Resource descargarArchivo(Long numeroResumen, String nombreArchivo) {
        String key = numeroResumen + "/" + nombreArchivo;
        log.info("Descargando archivo: {} del bucket: {}", key, bucket);
        return s3Template.download(bucket, key);
    }


    // 4. Eliminar archivo

    public void eliminarArchivo(Long numeroResumen, String nombreArchivo) {
        String key = numeroResumen + "/" + nombreArchivo;
        log.info("Eliminando archivo: {} del bucket: {}", key, bucket);
        s3Template.deleteObject(bucket, key);
        log.info("Archivo eliminado de S3: {}", key);
    }

    // 5. Listar archivos dentro de una carpeta o prefijo
    
    public List<String> listarArchivos(Long numeroResumen) {
        String prefix = numeroResumen + "/";
        log.info("Listando archivos con prefijo: {} en bucket: {}", prefix, bucket);
        
        List<S3Resource> resources = s3Template.listObjects(bucket, prefix);
        
        return resources.stream()
                .map(S3Resource::getFilename)
                .toList();
    }

    // 6. Eliminar carpeta completa

    public void eliminarCarpeta(Long numeroResumen) {
        String prefix = numeroResumen + "/";
        log.info("Eliminando todos los objetos con prefijo: {} en bucket: {}", prefix, bucket);

        List<S3Resource> resources = s3Template.listObjects(bucket, prefix);

        for (S3Resource resource : resources) {
            try {
                // Obtiene la clave exacta tal como existe en S3 (ej: "202/archivo.txt" o "202/")
                String exactKey = resource.getLocation().getObject();
                log.info("Borrando clave exacta de S3: {}", exactKey);
                s3Template.deleteObject(bucket, exactKey);
            } catch (Exception e) {
                log.error("Error al borrar recurso: {}", resource, e);
            }
        }

        // Por seguridad, también eliminamos el prefijo completo

        s3Template.deleteObject(bucket, prefix);

        log.info("Carpeta {} y su contenido eliminados exitosamente de S3", prefix);
    }
}