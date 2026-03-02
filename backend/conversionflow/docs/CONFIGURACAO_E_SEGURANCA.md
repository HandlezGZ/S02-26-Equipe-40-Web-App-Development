# Configuracao e seguranca (backend conversionflow)

Este documento define o contrato de ambiente e as expectativas de seguranca do backend `conversionflow`.

## Variaveis de ambiente

Use `.env.example` como base. A lista abaixo resume as variaveis mais importantes.

### Basicas

- `SPRING_PROFILES_ACTIVE` (`dev` por padrao)
- `SERVER_PORT` (`8080` por padrao)
- `DB_URL`, `DB_USER`, `DB_PASS`
- `JPA_SHOW_SQL` (`true` por padrao)

### Stripe

- `STRIPE_SECRET_KEY` (obrigatoria em producao)
- `STRIPE_WEBHOOK_SECRET` (obrigatoria em producao)
- `CHECKOUT_SUCCESS_URL`
- `CHECKOUT_CANCEL_URL`
- `STRIPE_CURRENCY` (`usd` por padrao)
- `STRIPE_PRICE_INCORPORATION_BASIC`
- `STRIPE_PRICE_INCORPORATION_PREMIUM`
- `STRIPE_PRICE_ACCOUNTING_MONTHLY`

Importante sobre `CHECKOUT_SUCCESS_URL`:

- Defina com `session_id={CHECKOUT_SESSION_ID}` para o frontend conseguir enviar o `checkoutSessionId` no endpoint de pixel event.
- Exemplo:
  - `http://localhost:3000/sucesso?session_id={CHECKOUT_SESSION_ID}`

### Integracoes

- `META_ACCESS_TOKEN`
- `META_PIXEL_ID`
- `GOOGLE_ADS_CONVERSION_ID`
- `GOOGLE_ADS_CONVERSION_LABEL`
- `PIPEDRIVE_API_TOKEN`

### Politica de retry de dispatch

- `DISPATCH_RETRY_MAX_ATTEMPTS` (`5` por padrao)
- `DISPATCH_RETRY_INTERVAL_MS` (`60000` por padrao)

### Credenciais de admin (HTTP Basic)

- `SPRING_SECURITY_USER_NAME`
- `SPRING_SECURITY_USER_PASSWORD`

## Endpoints publicos vs protegidos

Conforme `SecurityConfig`, os endpoints abaixo sao publicos:

- `/leads/**`
- `/checkout/**`
- `/pixel-events/**`
- `/webhooks/stripe`
- `/health/**`
- `/error`

Todos os demais endpoints exigem autenticacao (`HTTP Basic`), incluindo:

- `/admin/stats/**`

## Webhook do Stripe

- Endpoint: `POST /webhooks/stripe`
- O payload deve conter a assinatura valida no header `Stripe-Signature`.
- A assinatura e validada com `STRIPE_WEBHOOK_SECRET`.
- O webhook e a fonte primaria de confirmacao de pagamento.

## Pixel event (fallback controlado)

- Endpoint: `POST /pixel-events/purchase`
- Serve para enriquecer atribuicao e, quando `checkoutSessionId` estiver presente, permite validacao server-side com Stripe.
- Mesmo com fallback, o sistema evita promover evento de conversao sem transicao real para `WON`.
