# Kotlin App

A minimal Kotlin application with a **Paystack marketplace split** demo: one product page (Seller A), checkout, and automatic split (platform fee → you, seller share → Seller A’s subaccount).

## Requirements

- **JDK 17** or later
- **Gradle** (optional if you use the wrapper or an IDE)

## Paystack API keys and env vars

To run the payment flow you need:

| Env var | Required | Description |
|--------|----------|-------------|
| **`PAYSTACK_SECRET_KEY`** | Yes | Secret key from [Paystack Dashboard](https://dashboard.paystack.com) → **Settings → API Keys & Webhooks**. Use **Test** key (`sk_test_...`) for testing, **Live** key (`sk_live_...`) for production. **Never** expose this in the frontend. |
| **`PAYSTACK_SUBACCOUNT_SELLER_A`** | Yes | Subaccount code for Seller A (e.g. `ACCT_xxxxx`). Create in [Dashboard → Subaccounts](https://dashboard.paystack.com/#/subaccounts) or via [Create Subaccount API](https://paystack.com/docs/api/subaccount#create). |
| **`PLATFORM_FEE_KOBO`** | No | Your flat fee in **kobo** (e.g. `10000` = 100 Naira). Default: `10000`. |

**Summary of keys:**

- **Secret key** (`sk_test_...` / `sk_live_...`): Used only on the **backend** to call Paystack (initialize transaction with subaccount + `transaction_charge`). Never put it in frontend or in git.
- **Public key** (`pk_test_...` / `pk_live_...`): Not required for this flow because the app redirects to Paystack’s `authorization_url`. You’d only need it if you use Paystack Inline/Popup on the frontend.

## Where to put env vars

**Option A – `.env` file (easiest for local dev)**  
Create a file named **`.env`** in the project root (same folder as `build.gradle.kts`):

```bash
cp .env.example .env
# Edit .env and set PAYSTACK_SECRET_KEY and PAYSTACK_SUBACCOUNT_SELLER_A
```

The app loads `.env` when it starts. Don’t commit `.env` (it’s in `.gitignore`).

**Option B – Shell**  
`export PAYSTACK_SECRET_KEY=...` and `export PAYSTACK_SUBACCOUNT_SELLER_A=...` in your terminal before running.

**Option C – IDE**  
In IntelliJ: Run → Edit Configurations → select the app → Environment variables, add the keys there.

## Run the app

Set the required env vars (e.g. use a `.env` file as above), then:

```bash
export PAYSTACK_SECRET_KEY=sk_test_xxxx
export PAYSTACK_SUBACCOUNT_SELLER_A=ACCT_xxxx
# optional: export PLATFORM_FEE_KOBO=10000

gradle run
```

If you don’t have Gradle: `brew install gradle`, or run `gradle wrapper` once then `./gradlew run`.

Open **http://localhost:8080** to see the product page. Click **Pay with Paystack** to go through checkout; the payment is split (platform fee to you, rest to Seller A’s subaccount).

## Build

```bash
gradle build
```

## Project structure

```
kotlin/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/
│   ├── kotlin/
│   │   └── Application.kt   # Ktor server + Paystack init
│   └── resources/static/
│       ├── index.html       # Product page (Seller A)
│       └── success.html     # Post-payment page
└── README.md
```
