package com.isums.aiservice.domains.dtos;

import java.util.Map;
import java.util.UUID;

public record ScoreRequest(
        String thing,
        UUID houseId,
        UUID areaId,
        long ts,
        Map<String, Double> features  // v,i,p,kwh,hz,pf,w_lpm,d_kwh,d_w_tot,dt follow payload from iot
) {}
