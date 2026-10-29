# Card Catalogue – Sources & Method

File: `shared/data/core/src/main/resources/fern/card_catalogue.json` (schemaVersion 1, verifiedOn 2026-10-27).
Contents: 48 banks/issuers and 377 credit-card products (365 currently offered, 12 the issuer lists as discontinued).

## Scope
- **Banks:** all 12 public-sector and 21 private-sector banks on the RBI list (each has a `creditCardIssuer` flag). Also included: every small finance bank and foreign bank that reports more than 0 credit cards outstanding in the RBI card statistics.
- **NBFC issuers:** SBI Cards and Payment Services Ltd. and BOBCARD Ltd. are both included. Neither appears on the RBI bank list, so `rbiListed=false`. The RBI statistics have no separate row for these NBFCs. Their cards appear to be counted under State Bank of India (23.17 M) and Bank of Baroda (3.49 M), so products are attached to `sbi-card` / `bobcard`. No products are attached to the parent banks.
- **Left out:**
  - Payments banks: all 5 report 0 credit cards.
  - Foreign banks with 0 credit cards.
  - Small finance banks with 0 credit cards: Capital, Jana, Shivalik, Ujjivan.
  - Regional rural banks (28), local area banks (2) and cooperative banks. None appear in the card statistics as issuers.
- **Products:** personal, NRI and SME/business credit cards. Not included: corporate/commercial programmes (corporate, purchase, travel-account cards), debit, prepaid and forex cards.

## Official sources (all accessed 2026-10-27)
| id | Source |
|---|---|
| rbi-banks | RBI "Banks in India" – https://www.rbi.org.in/commonman/English/scripts/banksinindia.aspx (also used for bank websites) |
| rbi-master-direction | RBI (Commercial Banks – Credit Cards and Debit Cards: Issuance and Conduct) Directions, 2025, RBI/DOR/2025-26/155, DOR.AUT.REC.No.74/24-01-041/2025-26, 28 Nov 2025, updated as on 1 Oct 2026 – https://www.rbi.org.in/Scripts/BS_ViewMasDirections.aspx?id=13155 |
| rbi-card-stats | RBI Bank-wise ATM/POS/Card Statistics, **September 2026** (provisional; latest available) – https://www.rbi.org.in/Scripts/ATMView.aspx?atmid=187 (column 7 is "credit cards outstanding") |
| `<bankId>-cards` | One official credit-card listing page per issuer. All URLs are in `sources[]` in the JSON. Most banks have moved to `*.bank.in` domains, e.g. https://www.hdfc.bank.in/credit-cards, https://www.icici.bank.in/personal-banking/cards/credit-card, https://www.axis.bank.in/cards/credit-card, https://www.rbl.bank.in/personal-banking/cards/credit-cards/category, https://www.bobcard.co.in/credit-card-types, https://creditcard.pnb.bank.in/, https://www.americanexpress.com/in/credit-cards/ |

## Method
1. **Issuer status:** taken from the September 2026 RBI statistics. A bank is an issuer if it has more than 0 credit cards outstanding. Non-issuers: Bank of Maharashtra, Central Bank of India, Punjab & Sind Bank, UCO Bank, Karnataka Bank, Nainital Bank. Their customers get cards through SBI Card or BOBCARD co-brands, which are listed under those issuers.
2. **Product names:** each name is taken from the issuer's official listing page or product page as fetched on the access date, with the page's own spelling and capitalisation. Taglines and "(Discontinued)" suffixes were removed. Each product's `url` is its own product page if one was found, otherwise the listing page.
3. **`networks`:** filled in only when the official card name itself states the network (e.g. "Platinum RuPay", "Visa Infinite", "Diners Club", "JCB", and Amex's own cards). Otherwise `[]`. Mentions elsewhere on a page (footers, offers) were not used.
4. **`coBrandPartner`:** set only when the partner appears in the official card name. Example: SBI Card's PSB/BOM/City Union co-brands record the partner bank.
5. **`variant`:** `null` throughout. Tiers were not inferred.
6. **`available`:** set to `false` only for IndusInd cards that IndusInd's own listing marks "(Discontinued)".

## Partially verified or unverified
- **SBI Card:** `www.sbicard.com` could not be reached (DNS failure from the sandbox; the fetch tool timed out). The 6 products listed come only from search-result page titles on the official `sbicard.com` domain. The catalogue is therefore far from complete for this issuer. Club Vistara SBI Card PRIME was left out because it may be a legacy card.
- **IDBI Bank:** `idbi.bank.in` and `idbibank.in` could not be reached. The 3 products come from search snippets of official `idbibank.in` pages.
- **BOBCARD:** the site serves an incomplete TLS certificate chain. The pages were read with certificate verification turned off (read-only).
- **Bank sites with zero products:**

  | Bank | Reason |
  |---|---|
  | Federal Bank, CSB Bank | Radware bot challenge |
  | South Indian Bank, Bank of India | 403 / Cloudflare challenge |
  | Indian Bank | card names are rendered client-side; only "Select/Platinum/Gold/Classic/Business" tiers are visible, with no product names |
  | J&K Bank, Dhanlaxmi, ESAF, Suryoday | host unreachable |
  | Unity SFB | 403 |
  | Bandhan | the card portal `creditcards.bandhan.bank.in` was unreachable |
  | Equitas | the page shows no product names |
  | Citibank N.A. | its India site redirects to citigroup.com. The bank still reports 283,748 cards outstanding (Citi India's consumer business has moved to Axis Bank) |

  Third-party aggregators (BankBazaar, Paisabazaar, etc.) were not used as a source.
- **Lists that may be incomplete** because parts of these pages load via JavaScript: Axis (e.g. Atlas/Reserve are not in the static HTML), Kotak, YES Bank, ICICI business cards, HDFC Tata Neu Infinity, DBS SuperX, and City Union's CSK/SRH cards. Only names visible in the fetched HTML were recorded.
- **Possible formatting artefacts:** "PNB Wave & Pay Credit Card" (rendered as "Wave & PayCredit Card") and RBL's "LUMIÈRE"/"NOVA" (shown with ™ marks) were normalised.
