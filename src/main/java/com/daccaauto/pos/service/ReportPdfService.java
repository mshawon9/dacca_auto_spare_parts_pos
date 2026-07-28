package com.daccaauto.pos.service;

import java.util.List;
import java.util.Map;

public interface ReportPdfService {

    byte[] generate(String reportName, Map<String, Object> parameters, List<? extends Map<String, ?>> rows);
}
