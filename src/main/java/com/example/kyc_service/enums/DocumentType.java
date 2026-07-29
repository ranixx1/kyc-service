package com.example.kyc_service.enums;

public enum DocumentType {

        ID_CARD,
        DRIVER_LICENSE,
        PASSPORT,
        BANK_STATEMENT,
        PAY_SLIP,
        UTILITY_BILL,
        PHONE_BILL;

        public String[] expectedPatterns() {
                return switch (this) {
                        case ID_CARD -> new String[] {
                                        "identity",
                                        "identity card",
                                        "id card",
                                        "identification",
                                        "identidade",
                                        "registro geral",
                                        "rg",
                                        "república federativa"
                        };

                        case DRIVER_LICENSE -> new String[] {
                                        "driver license",
                                        "driver's license",
                                        "driving licence",
                                        "driving license",
                                        "carteira nacional",
                                        "habilitação",
                                        "cnh",
                                        "detran"
                        };

                        case PASSPORT -> new String[] {
                                        "passport",
                                        "passaporte",
                                        "pasaporte",
                                        "republic",
                                        "nationality",
                                        "date of birth",
                                        "place of birth"
                        };

                        case BANK_STATEMENT -> new String[] {
                                        "bank statement",
                                        "extrato bancário",
                                        "extrato",
                                        "saldo",
                                        "account statement"
                        };

                        case PAY_SLIP -> new String[] {
                                        "pay slip",
                                        "payslip",
                                        "salary slip",
                                        "paystub",
                                        "holerite",
                                        "contracheque",
                                        "comprovante de pagamento"
                        };

                        case UTILITY_BILL -> new String[] {
                                        "utility bill",
                                        "electricity bill",
                                        "water bill",
                                        "gas bill",
                                        "conta de luz",
                                        "conta de água",
                                        "conta de gás"
                        };

                        case PHONE_BILL -> new String[] {
                                        "phone bill",
                                        "mobile bill",
                                        "telephone bill",
                                        "cell phone bill",
                                        "conta de telefone",
                                        "conta de celular",
                                        "fatura telefone"
                        };
                };
        }
}