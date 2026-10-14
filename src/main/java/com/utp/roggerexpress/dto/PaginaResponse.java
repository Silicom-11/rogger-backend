package com.utp.roggerexpress.dto;

import java.util.List;

/**
 * Envoltura de una consulta paginada, con la informacion que necesita la
 * tabla del front-end.
 */
public record PaginaResponse<T>(List<T> contenido,
                                int pagina,
                                int tamanio,
                                long totalElementos,
                                int totalPaginas) {

    public static <T> PaginaResponse<T> de(List<T> contenido, int pagina, int tamanio, long total) {
        int totalPaginas = tamanio == 0 ? 0 : (int) Math.ceil((double) total / tamanio);
        return new PaginaResponse<>(contenido, pagina, tamanio, total, totalPaginas);
    }
}
