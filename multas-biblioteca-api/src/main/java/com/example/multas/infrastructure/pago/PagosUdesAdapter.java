package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.Multa;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "pagosudes", matchIfMissing = true)
public class PagosUdesAdapter implements PasarelaPagoPort {
    private final RestTemplate restTemplate;
    private final String urlPasarela;

    public PagosUdesAdapter(RestTemplate restTemplate,
            @Value("${app.pagos.pagosudes.url:http://localhost:9001/pagosudes/transacciones}") String urlPasarela) {
        this.restTemplate = restTemplate;
        this.urlPasarela = urlPasarela;
    }

    @Override public ResultadoPago procesar(Multa multa) {
        try {
            PagosUdesResponse response = restTemplate.postForObject(urlPasarela,
                    new PagosUdesRequest(multa.getEstudianteId(), multa.getMonto()), PagosUdesResponse.class);
            boolean aprobado = response != null && "APROBADA".equalsIgnoreCase(response.estadoTransaccion());
            return new ResultadoPago("PAGOSUDES", aprobado, response == null ? null : response.idTransaccion(),
                    aprobado ? "Pago aprobado por PagosUDES" : "Transacción rechazada por PagosUDES");
        } catch (RestClientException ex) {
            return new ResultadoPago("PAGOSUDES", false, null, "PagosUDES no disponible: " + ex.getMessage());
        }
    }

    private record PagosUdesRequest(String estudianteId, BigDecimal monto) {}
    private record PagosUdesResponse(String idTransaccion, String estadoTransaccion) {}
}
