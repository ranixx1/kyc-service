package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.BankStatementDocument;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import org.springframework.stereotype.Component;

@Component
public class BankStatementExtractor extends BaseExtractor {

    // IBAN: up to 34 alphanumeric characters, optionally space-separated
    private static final String IBAN_PATTERN =
            "\\b([A-Z]{2}\\d{2}[A-Z0-9 ]{4,30})\\b";

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.BANK_STATEMENT;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return BankStatementDocument.builder()
                .holderName(extractAfterLabel(text,
                        "account holder", "account name", "name", "holder",
                        "titular", "nome do titular"))
                .bankName(extractAfterLabel(text,
                        "bank", "bank name", "institution",
                        "banco", "nome do banco"))
                .accountNumber(extractAfterLabel(text,
                        "account number", "account no", "acc no",
                        "número da conta", "conta"))
                .iban(extractByPattern(text, IBAN_PATTERN))
                .statementPeriod(extractAfterLabel(text,
                        "statement period", "period", "from", "statement date",
                        "período", "período do extrato"))
                .openingBalance(extractAmount(text,
                        "opening balance", "balance brought forward",
                        "saldo inicial", "saldo anterior"))
                .closingBalance(extractAmount(text,
                        "closing balance", "ending balance", "available balance",
                        "saldo final", "saldo disponível"))
                .build();
    }
}
