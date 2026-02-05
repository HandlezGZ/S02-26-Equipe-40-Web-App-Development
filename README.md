# ConversionFlow – Backend

## 📌 Visão Geral

O **ConversionFlow** é uma aplicação backend desenvolvida em **Java com Spring Boot**, responsável por processar **pagamentos reais via Stripe** e registrar **conversões de forma confiável** para plataformas de marketing e CRM.

O sistema utiliza **rastreamento server-side**, garantindo dados precisos mesmo em cenários de bloqueio de cookies, restrições de privacidade e limitações de tracking client-side.

Este backend atua como a **fonte única de verdade** para conversões e decisões de negócio.

---

## 🎯 Objetivo do Projeto

- Registrar apenas **pagamentos aprovados** como conversões
- Garantir mensuração correta para **Google Ads** e **Meta Ads**
- Integrar dados de vendas com **Pipedrive (CRM)**
- Suportar campanhas de tráfego pago com dados confiáveis
- Manter uma arquitetura clara e escalável

---

## 🧱 Arquitetura (Resumo)

Webflow (Frontend)
↓
Stripe Checkout
↓
Webhook Stripe
↓
ConversionFlow (Spring Boot)
↓
Conversões Server-side
↙ ↘
Meta Ads Google Ads
↓
Pipedrive (CRM)


---

## 🛠️ Stack Tecnológica

- **Java 17**
- **Spring Boot**
- **Spring Web**
- **Spring Data JPA**
- **PostgreSQL**
- **Stripe API (Webhooks)**
- **Meta Conversions API**
- **Google Ads Conversion API**
- **Pipedrive API**

---

## 📂 Estrutura do Projeto

backend/conversionflow
├── src/main/java/com/nocountry/conversionflow
│ ├── controller # Endpoints REST e webhooks
│ ├── service # Regras de negócio
│ ├── client # Integrações externas
│ ├── domain # Entidades de domínio
│ ├── repository # Persistência (JPA)
│ ├── config # Configurações
│ └── exception # Exceções customizadas
│
├── src/main/resources
│ ├── application.yml
│
├── pom.xml
└── README.md


---

## 🔄 Fluxo de Conversão

1. Usuário clica em um anúncio
2. Usuário realiza o pagamento via Stripe
3. Stripe envia um webhook (`payment_intent.succeeded`)
4. Backend valida o pagamento
5. Conversão é registrada server-side
6. Evento é enviado para:
   - Google Ads
   - Meta Ads
7. Lead ou venda é sincronizado com o CRM

---

## 🧠 Princípio Central

> **Pagamentos confirmados definem conversões.  
Eventos de navegador são apenas complementares.**

---

## 🗄️ Banco de Dados

**PostgreSQL** é utilizado por oferecer:
- Alta confiabilidade transacional
- Suporte nativo a JSON/JSONB (payloads de webhook)
- Boa performance para aplicações orientadas a eventos

---

## 🚀 Como Executar (local)

```bash
./mvnw spring-boot:run
