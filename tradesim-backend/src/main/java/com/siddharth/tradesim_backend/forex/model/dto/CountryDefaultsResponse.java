package com.siddharth.tradesim_backend.forex.model.dto;

import java.util.List;

public record CountryDefaultsResponse(
        String currency,
        List<String> timezones
) {
}