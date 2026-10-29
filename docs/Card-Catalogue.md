# Card catalogue

The bank and product picker for credit cards reads `shared/data/core/src/main/resources/fern/card_catalogue.json`.
The file is bundled with the app. It is read-only and is never downloaded at runtime.
[Card-Catalogue-Sources.md](Card-Catalogue-Sources.md) covers the sources, the method used, and the gaps in coverage.

## How cards use it

- A user's card stores catalogue **ids** (`bankId`, `productId`) and keeps its own saved product name.
- Removing or renaming an entry never breaks an existing card. The card falls back to its saved names.
- Users can always type in a bank or card that isn't listed.
- The catalogue never changes a user's settings such as the limit, the dates or the network.

## Bank fields

| Field | Meaning |
|---|---|
| `id` | Stable kebab-case slug. Never reuse or rename it. |
| `name`, `displayName`, `aliases` | Used by the search, which ignores case, accents and punctuation. |
| `rbiListed` | Appears on the RBI "Banks in India" list. |
| `creditCardIssuer` | Reports more than 0 credit cards outstanding in the RBI card statistics. |
| `sourceIds` | References to entries in `sources[]`. |
| `logoAsset` | Optional. See [Logos and artwork](#logos-and-artwork). |

## Product fields

| Field | Meaning |
|---|---|
| `bankId` | Must match a bank with `creditCardIssuer = true`. |
| `name` | The product name as the issuer spells it. |
| `networks` | Filled in only when the official card name states the network. Allowed values: `RUPAY`, `VISA`, `MASTERCARD`, `AMEX`, `DINERS`, `JCB`. |
| `coBrandPartner` | The co-brand partner, if any. |
| `url` | The official product or listing page. |
| `verifiedOn` | The date the entry was checked. |
| `available` | See [Updating](#updating). |
| `artworkAsset` | Optional. See [Logos and artwork](#logos-and-artwork). |

## Updating

1. Use only official sources: RBI pages and the issuer's own website. Do not use aggregator sites.
2. Add or edit entries and update `verifiedOn`. If you add a source, also add it to `sources[]` and to `Card-Catalogue-Sources.md`.
3. When a product is discontinued, set `available` to `false` instead of deleting it. It then disappears from the picker, but existing cards keep their link to it.
4. Run `./gradlew :shared:data:core:testDebugUnitTest --tests '*CardCatalogueTest'`. This test checks:
   - ids are unique
   - every product's `bankId` points to an issuer
   - every network value is known
   - the search works

## Logos and artwork

No bank logos, network logos or card-face images are bundled. These are trademarks, and permission to reuse them has not been confirmed.

The app instead draws a generic Fern card with:
- the issuer's initials
- the network as plain text
- the caption "Illustration made by Fern Wallet. It isn't your bank's card design."

To add an authorised asset:

1. Get written permission from the trademark owner, or use an asset whose licence allows this use.
2. Add the asset as a drawable in `feature/credit-cards`.
3. Set `logoAsset` or `artworkAsset` to the drawable name.
4. Record where the asset came from and its licence in `Card-Catalogue-Sources.md`.

`CardCatalogueTest` fails while any asset field is set. This is deliberate, so that adding the first asset is a reviewed change. Update the test in the same change.
