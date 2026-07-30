package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.PaySlipDocument;
import org.springframework.stereotype.Component;

@Component
public class PaySlipExtractor extends BaseExtractor {

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.PAY_SLIP;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return PaySlipDocument.builder()
                .employeeName(extractAfterLabel(text,
                        "employee name", "employee", "name", "worker",
                        "funcionário", "colaborador", "nome"))
                .employerName(extractAfterLabel(text,
                        "employer", "employer name", "company", "organization",
                        "empresa", "empregador", "razão social"))
                .payPeriod(extractAfterLabel(text,
                        "pay period", "period", "pay date", "payment period",
                        "período", "competência", "mês de referência"))
                .grossSalary(extractAmount(text,
                        "gross salary", "gross pay", "gross earnings", "gross",
                        "salário bruto", "rendimento bruto", "proventos"))
                .netSalary(extractAmount(text,
                        "net salary", "net pay", "net earnings", "take home",
                        "salário líquido", "valor líquido", "líquido a receber"))
                .taxDeductions(extractAmount(text,
                        "tax", "income tax", "deductions", "total deductions",
                        "imposto de renda", "descontos", "total de descontos"))
                .build();
    }
}
