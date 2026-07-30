package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.UtilityBillDocument;
import org.springframework.stereotype.Component;

@Component
public class UtilityBillExtractor extends BaseExtractor {

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.UTILITY_BILL;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return UtilityBillDocument.builder()
                .holderName(extractAfterLabel(text,
                        "customer name", "account holder", "name", "billed to",
                        "nome do cliente", "titular", "nome"))
                .serviceAddress(extractAfterLabel(text,
                        "service address", "property address", "address",
                        "endereço de fornecimento", "local de entrega", "endereço"))
                .serviceProvider(extractAfterLabel(text,
                        "provider", "company", "utility company", "supplier",
                        "fornecedor", "concessionária", "empresa"))
                .billingPeriod(extractAfterLabel(text,
                        "billing period", "bill period", "service period",
                        "período de faturamento", "competência", "referência"))
                .totalAmount(extractAmount(text,
                        "total amount due", "amount due", "total", "amount payable",
                        "valor total", "total a pagar", "valor a pagar"))
                .dueDate(extractDate(text,
                        "due date", "payment due", "pay by",
                        "data de vencimento", "vencimento"))
                .build();
    }
}
