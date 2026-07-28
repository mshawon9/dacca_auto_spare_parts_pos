package com.daccaauto.pos.service.serviceImpl;

import com.daccaauto.pos.service.ReportPdfService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportPdfServiceImpl implements ReportPdfService {

    @Override
    public byte[] generate(String reportName, Map<String, Object> parameters, List<? extends Map<String, ?>> rows) {
        try {
            JasperReport report = loadReport(reportName);
            JasperPrint print = JasperFillManager.fillReport(
                report,
                parameters,
                new JRMapCollectionDataSource(new ArrayList<>(rows))
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException | IOException ex) {
            throw new IllegalStateException("Could not generate PDF report: " + reportName, ex);
        }
    }

    private JasperReport loadReport(String reportName) throws IOException, JRException {
        ClassPathResource compiled = new ClassPathResource("reports/" + reportName + ".jasper");
        if (compiled.exists()) {
            try (InputStream input = compiled.getInputStream()) {
                return (JasperReport) JRLoader.loadObject(input);
            }
        }
        try (InputStream input = new ClassPathResource("reports/" + reportName + ".jrxml").getInputStream()) {
            return JasperCompileManager.compileReport(input);
        }
    }
}
