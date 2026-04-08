package org.example.company.tcs.techcellshop.dto.response;

import java.math.BigDecimal;

public record DeviceSummaryResponse(Long idDevice, String nameDevice, BigDecimal devicePrice) {
}
