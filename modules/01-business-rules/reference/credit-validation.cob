       >>SOURCE FORMAT FREE
identification division.
program-id. CREDIT-VALIDATION.

data division.
working-storage section.
01 ws-invoice-number     pic x(12).
01 ws-requested-cents    pic s9(9).
01 ws-credit-status      pic 99.

procedure division.
    accept ws-invoice-number
    accept ws-requested-cents
    perform validate-credit
    display ws-credit-status
    stop run.

validate-credit.
    evaluate true
        when ws-invoice-number = spaces
            move 04 to ws-credit-status
        when ws-requested-cents <= 0
            move 08 to ws-credit-status
        when other
            move 00 to ws-credit-status
    end-evaluate.
