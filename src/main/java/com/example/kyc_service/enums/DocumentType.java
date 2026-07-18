package com.example.kyc_service.enums;

public enum DocumentType {
    RG,
    CPF,
    PASSPORT,
    CNH;

    public String[] expectedPatterns() {
        return switch (this) {
            case RG -> new String[] { "registro geral", "rg", "identidade" };
            case CPF -> new String[] { "cpf", "cadastro de pessoas físicas" };
            case PASSPORT -> new String[] { "passport",
                    "passaporte",
                    "pasaporte" };
            case CNH -> new String[] { "carteira nacional", "habilitação", "cnh", "detran" };
        };
    }
}