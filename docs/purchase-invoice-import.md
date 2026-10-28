# Purchase order invoice images

## Workflow

On Purchase Order Entry, use Upload Image, Take Photo, or Use Phone.
Phone pairing transfers a photo to the laptop with a private, one-use link
that expires in ten minutes. Both devices must reach the app at the same
address. Open the laptop page at its reachable HTTPS address before generating
the connection QR; localhost links cannot reach the laptop from a phone.
Phone photo capture uses the operating system file picker instead of requesting
a browser video stream. Gallery upload is also available.

Preview the photo, then explicitly select Extract with AI. That sends the image
to the configured provider. Images are resized locally; the server accepts real
JPEG/PNG images up to 3 MB and 25 million pixels. One image represents one invoice
page; extraction is capped at 100 product lines. Multi-page/PDF import is not
included. Images are not saved to disk. Phone uploads are temporarily held in
memory until received/disconnected/expired or the server restarts.

Extraction fills the existing PO supplier, invoice ID and date fields directly,
and puts every extracted item in the normal Purchase Items table, including
quantities, unit prices and VAT. Select the warehouse manually. Unknown suppliers
show an Add Supplier suggestion with the extracted name prefilled. Review the
currency/printed-total warnings and correct values in the normal form.
A unique part-number or SKU
match may be selected automatically; brand disambiguates shared part numbers.
Name-only, conflicting identifiers, and multiple-brand matches require a choice.
Name matching checks the stored name and the name with its category prefix
removed. If those do not match, ordered description words provide suggestions.
Supplier invoices do not need a SKU, barcode or part number for these name
suggestions. They still require confirmation to avoid choosing a wrong brand.
Unmatched items remain visible and must be resolved with product search or
New Product before saving. Remove a row with its normal delete button to exclude
it. Matched rows show a compact status; Change product reveals the selector.

New Product opens a prefilled modal. Choose or create category and brand inside
the same modal. Creating a product links its brand/category and creates its
catalog record immediately, but does not set cost or stock. New categories and
brands are also created immediately and remain if you later cancel the PO.
Duplicate names/brand+part numbers use the application's existing validation.

The PO uses tax-exclusive unit prices plus a tax percentage per line. If only
tax-inclusive prices are readable, extraction leaves the unit price blank for
manual correction. Discounts/freight/unsupported adjustments produce warnings;
the existing PO has no separate discount/freight fields. Reconcile those amounts
before saving. Differences between selected-line and printed totals are shown.
Prices and tax must have at most two decimal places; quantities must be positive
whole numbers. Unknown tax is left blank, never silently assumed to be 5%.

Tick the invoice review confirmation, then use the normal Save & Update Stock.
There is no separate copy/apply step. Re-extraction replaces only the earlier
extracted rows, preserving manual rows after confirmation. Conflicting invoice
or supplier headers are blocked when manual/saved lines already exist. Missing
products and invalid amounts block submission. The normal save remains responsible
for PO persistence, supplier-product mapping, stock adjustments, cost and history.
Re-extraction changes only the form; database stock/cost changes occur on PO save.

## Configuration and demo

The default provider is `demo`. Try Sample Demo loads a fixed, clearly labelled
example without reading the image, making an external API call, or requiring a
key. The real Extract action never falls back to fabricated demo results.

For free-tier Gemini testing, create a key in https://aistudio.google.com/apikey
and set environment variables in the process that starts Spring Boot:

```powershell
$env:INVOICE_AI_PROVIDER = 'gemini'
$env:INVOICE_AI_API_KEY = 'your-key'
$env:INVOICE_AI_MODEL = 'gemini-3.1-flash-lite'
```

For Anthropic:

```powershell
$env:INVOICE_AI_PROVIDER = 'anthropic'
$env:INVOICE_AI_API_KEY = 'your-key'
$env:INVOICE_AI_MODEL = 'claude-haiku-5-5'
```

Restart the app after changing environment variables. Keys stay on the server;
never place them in JavaScript or commit them. `GEMINI_API_KEY` remains an
alternative when `INVOICE_AI_API_KEY` is unset. The model must support image
input and structured JSON. Both providers use bounded requests, timeouts and
strict response validation. No paid request is made automatically or retried
automatically. Phone pairing requires a single server instance or session
affinity; a multi-server deployment needs shared temporary photo storage.
The extraction deadline defaults to 180 seconds and can be changed with
`app.purchase.invoice-ai.timeout-seconds` or `INVOICE_AI_TIMEOUT_SECONDS`
(positive, at most 600). The UI displays elapsed processing time. Connection
timeouts report network/firewall/proxy issues separately. Logs include provider,
model, status and duration, never API keys or invoice contents. Gemini 3.8/3.7
Flash requests use low thinking to reduce extraction latency. A reverse proxy
or tunnel must allow requests at least as long as the configured deadline.
When no model is set, the service chooses the default model for the selected
provider. Importing into an existing PO requires an explicit duplicate-item
confirmation; a different supplier/invoice header is blocked.

## Cost comparison

Checked against official pages on 2026-10-24. USD standard rates per million
tokens; model tokenization, image resolution and output/reasoning affect the
actual bill, so these are not fixed prices per invoice.

| Model | Input | Output | Use |
| --- | ---: | ---: | --- |
| Claude Haiku 5.5, prompts up to 100k tokens | $0.10 | $0.50 | Lower paid token rates among these current options |
| Gemini 3.1 Flash-Lite | $0.25 | $1.50 | Free tier for eligible accounts; useful for demos |
| Gemini 3.5 Flash-Lite | $0.30 | $2.50 | Newer option; higher standard rates |

For illustration only, 3,000 input tokens plus 1,500 output tokens would cost
about $0.00105 on Haiku 5.5 or $0.003 on Gemini 3.1 Flash-Lite, excluding any
additional reasoning/other billing. Test real supplier invoices to compare
accuracy and total cost rather than token rates alone. Gemini's free tier uses
content to improve its products; paid-tier content is not used for that purpose.
Use synthetic/redacted invoices when evaluating the free tier.

Sources:
- https://ai.google.dev/gemini-api/docs/pricing
- https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite
- https://platform.claude.com/docs/en/models/haiku-5-5/overview
- https://platform.claude.com/docs/en/about-claude/pricing

## Verification

Run the focused image validation/provider parsing, phone transfer and matching
tests with:

```powershell
mvn '-Dtest=PurchaseInvoiceAiServiceTest,PurchaseInvoicePhoneServiceTest,PurchaseInvoiceMatchServiceTest' test
```

These tests use local HTTP fixtures and do not contact a paid provider. Real
provider extraction requires a configured API key and a readable test invoice.
