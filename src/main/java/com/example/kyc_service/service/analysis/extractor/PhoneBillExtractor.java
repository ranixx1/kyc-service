package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.PhoneBillDocument;
import org.springframework.stereotype.Component;

@Component
public class PhoneBillExtractor extends BaseExtractor {

    private static final String PHONE_PATTERN =
            "(\\+?\\d[\\d\\s().\\-]{7,20}\\d)";

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.PHONE_BILL;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return PhoneBillDocument.builder()
                .holderName(extractAfterLabel(text,
                        "account holder", "customer name", "name", "billed to",
                        "titular", "nome do cliente", "nome"))
                .phoneNumber(extractByPattern(text, PHONE_PATTERN))
                .carrier(extractAfterLabel(text,
                        "carrier", "operator", "network", "service provider",
                        "operadora", "prestadora"))
                .billingPeriod(extractAfterLabel(text,
                        "billing period", "invoice period", "service period",
                        "período", "competência", "referência"))
                .totalAmount(extractAmount(text,
                        "total amount due", "amount due", "total", "balance due",
                        "valor total", "total a pagar", "valor da fatura"))
                .dueDate(extractDate(text,
                        "due date", "payment due", "pay by",
                        "vencimento", "data de vencimento"))
                .serviceAddress(extractAfterLabel(text,
                        "service address", "address", "billing address",
                        "endereço", "endereço de cobrança"))
                .build();
    }
}
