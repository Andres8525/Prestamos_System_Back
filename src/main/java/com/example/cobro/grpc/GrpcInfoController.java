package com.example.cobro.grpc;

import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Información del servidor gRPC para el panel web: servicios, métodos con su patrón de comunicación,
 * métricas de uso y los contratos .proto. (Los navegadores no hablan gRPC nativo; por eso esto va por REST.)
 */
@RestController
@RequestMapping("/api/grpc")
public class GrpcInfoController {

    static final List<String> CONTRATOS = List.of("comun.proto", "scoring.proto", "cobranza.proto");

    private final GrpcServidor servidor;
    private final MetricasGrpc metricas;

    public GrpcInfoController(GrpcServidor servidor, MetricasGrpc metricas) {
        this.servidor = servidor;
        this.metricas = metricas;
    }

    public record MetodoDTO(String nombre, String patron, String descripcionPatron, long llamadas, long errores,
                            double promedioMs, String ultimoEstado) {}

    public record ServicioDTO(String nombre, List<MetodoDTO> metodos) {}

    public record InfoDTO(boolean habilitado, boolean activo, int puerto, List<ServicioDTO> servicios) {}

    @GetMapping("/info")
    public InfoDTO info() {
        List<ServicioDTO> servicios = servidor.getServicios().stream()
                .sorted(Comparator.comparing(s -> s.getServiceDescriptor().getName()))
                .map(this::servicio)
                .toList();
        return new InfoDTO(servidor.isHabilitado(), servidor.isRunning(), servidor.getPuerto(), servicios);
    }

    /** Contenido de los archivos .proto (contratos de la API gRPC). */
    @GetMapping("/contratos")
    public Map<String, String> contratos() throws IOException {
        Map<String, String> archivos = new LinkedHashMap<>();
        for (String nombre : CONTRATOS) {
            ClassPathResource recurso = new ClassPathResource("crediruta/v1/" + nombre);
            if (recurso.exists()) {
                archivos.put(nombre, recurso.getContentAsString(StandardCharsets.UTF_8));
            }
        }
        return archivos;
    }

    private ServicioDTO servicio(ServerServiceDefinition definicion) {
        List<MetodoDTO> metodos = definicion.getMethods().stream()
                .map(ServerMethodDefinition::getMethodDescriptor)
                .sorted(Comparator.comparing(MethodDescriptor::getFullMethodName))
                .map(m -> {
                    MetricasGrpc.Metodo uso = metricas.de(m.getFullMethodName());
                    return new MetodoDTO(m.getBareMethodName(), m.getType().name(), patron(m.getType()),
                            uso.llamadas(), uso.errores(), Math.round(uso.promedioMs() * 10) / 10.0, uso.ultimoEstado());
                })
                .toList();
        return new ServicioDTO(definicion.getServiceDescriptor().getName(), metodos);
    }

    static String patron(MethodDescriptor.MethodType tipo) {
        return switch (tipo) {
            case UNARY -> "Unario: una petición, una respuesta";
            case SERVER_STREAMING -> "Streaming de servidor: una petición, muchas respuestas";
            case CLIENT_STREAMING -> "Streaming de cliente: muchas peticiones, una respuesta";
            case BIDI_STREAMING -> "Bidireccional: muchas peticiones y respuestas en paralelo";
            default -> "Desconocido";
        };
    }
}
