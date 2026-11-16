package com.example.cobro.tiemporeal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Estado del canal en tiempo real y últimos eventos (para pantallas que cargan sin WebSocket). */
@RestController
@RequestMapping("/api/tiempo-real")
public class TiempoRealController {

    private final RegistroConexiones conexiones;
    private final HistorialEventos historial;
    private final DifusorEventos difusor;

    public TiempoRealController(RegistroConexiones conexiones, HistorialEventos historial, DifusorEventos difusor) {
        this.conexiones = conexiones;
        this.historial = historial;
        this.difusor = difusor;
    }

    public record EstadoDTO(int conexiones, long usuarios, long mensajesEnviados, long ultimoId,
                            List<EventoTiempoReal> recientes) {}

    @GetMapping("/estado")
    public EstadoDTO estado() {
        return new EstadoDTO(conexiones.cantidad(), conexiones.usuariosDistintos(), difusor.getEnviados(),
                difusor.getUltimoId(), historial.recientes(20));
    }
}
