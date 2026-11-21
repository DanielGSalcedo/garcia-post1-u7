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
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "wompi")
public class WompiAdapter implements PasarelaPagoPort {
    private final RestTemplate restTemplate;
    private final String urlPasarela;

    public WompiAdapter(RestTemplate restTemplate,
            @Value("${app.pagos.wompi.url:http://localhost:9002/wompi/transactions}") String urlPasarela) {
        this.restTemplate = restTemplate;
        this.urlPasarela = urlPasarela;
    }

    @Override public ResultadoPago procesar(Multa multa) {
        try {
            long centavos = multa.getMonto().multiply(BigDecimal.valueOf(100)).longValueExact();
            WompiResponse response = restTemplate.postForObject(urlPasarela,
                    new WompiRequest("multa-" + multa.getId(), centavos), WompiResponse.class);
            boolean aprobado = response != null && "APPROVED".equalsIgnoreCase(response.status());
            return new ResultadoPago("WOMPI", aprobado, response == null ? null : response.reference(),
                    aprobado ? "Pago aprobado por Wompi" : "Pago rechazado por Wompi");
        } catch (RestClientException | ArithmeticException ex) {
            return new ResultadoPago("WOMPI", false, null, "Wompi no disponible: " + ex.getMessage());
        }
    }

    private record WompiRequest(String reference, long amountInCents) {}
    private record WompiResponse(String reference, String status) {}
}
