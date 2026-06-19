# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: happy-path.spec.ts >> registers, logs in, creates a transaction, and shows dashboard data
- Location: e2e/happy-path.spec.ts:3:5

# Error details

```
Error: expect(locator).toHaveValue(expected) failed

Locator: getByLabel('Symbol')
Expected: "BTC"
Error: strict mode violation: getByLabel('Symbol') resolved to 3 elements:
    1) <input value="" type="text" placeholder="e.g. BTC" class="mt-2 w-full rounded-lg border border-gray-800 bg-gray-950/70 px-4 py-2.5 text-sm text-white placeholder:text-gray-500 focus:border-purple-500 focus:outline-hidden"/> aka getByRole('textbox', { name: 'Filter by symbol' })
    2) <select class="mt-2 w-full rounded-lg border border-gray-800 bg-gray-950/70 px-4 py-2.5 text-sm text-white focus:border-purple-500 focus:outline-hidden">…</select> aka getByLabel('Sort holdingsSymbol (A → Z)')
    3) <input value="BTC" placeholder="BTC" autocomplete="off" class="rounded-lg border border-gray-800 bg-gray-950/60 px-4 py-3 text-white placeholder:text-gray-600 focus:border-purple-500 focus:outline-hidden"/> aka getByRole('textbox', { name: 'Symbol', exact: true })

Call log:
  - Expect "toHaveValue" with timeout 10000ms
  - waiting for getByLabel('Symbol')

```

# Page snapshot

```yaml
- generic [active] [ref=e1]:
  - navigation [ref=e2]:
    - generic [ref=e3]:
      - link "CryptoTracker" [ref=e4] [cursor=pointer]:
        - /url: /dashboard
      - generic [ref=e5]:
        - link "Dashboard" [ref=e6] [cursor=pointer]:
          - /url: /dashboard
        - generic [ref=e7]: Hi, Satoshi
        - button "Logout" [ref=e8]
  - main [ref=e9]:
    - generic [ref=e10]:
      - generic [ref=e11]:
        - generic [ref=e12]:
          - generic [ref=e13]:
            - heading "Dashboard" [level=1] [ref=e14]
            - paragraph [ref=e15]: Portfolio data is synced from the backend API.
          - generic [ref=e16]:
            - generic [ref=e17]:
              - generic [ref=e18]: Prices just now
              - generic [ref=e19]: Jun 19, 09:55:22 PM
            - button "Refresh prices" [ref=e20]
        - generic [ref=e21]:
          - generic [ref=e22]:
            - generic [ref=e23]:
              - text: Quick add
              - generic [ref=e24]:
                - textbox "Quick add" [ref=e25]:
                  - /placeholder: BTC
                - button "Add" [ref=e26]
            - generic [ref=e27]:
              - text: Popular markets
              - combobox "Popular markets" [ref=e28]:
                - option "Select a coin" [selected]
                - option "XRP"
                - option "ADA"
                - option "AVAX"
                - option "BNB"
                - option "MATIC"
                - option "DOT"
                - option "TRX"
                - option "ATOM"
                - option "LTC"
                - option "NEAR"
                - option "APT"
                - option "ARB"
                - option "FIL"
                - option "AAVE"
                - option "GRT"
                - option "FTM"
                - option "SUI"
                - option "OP"
          - generic [ref=e29]:
            - button "Remove BTC from watchlist" [ref=e30]: BTC
            - button "Remove ETH from watchlist" [ref=e31]: ETH
            - button "Remove SOL from watchlist" [ref=e32]: SOL
            - button "Remove LINK from watchlist" [ref=e33]: LINK
            - button "Remove DOGE from watchlist" [ref=e34]: DOGE
        - generic [ref=e35]:
          - generic [ref=e36]:
            - generic [ref=e37]:
              - heading "BTC" [level=2] [ref=e38]
              - button "Remove BTC" [ref=e39]: Remove
            - paragraph [ref=e40]: $60,000.00
          - generic [ref=e41]:
            - generic [ref=e42]:
              - heading "ETH" [level=2] [ref=e43]
              - button "Remove ETH" [ref=e44]: Remove
            - paragraph [ref=e45]: $3,000.00
          - generic [ref=e46]:
            - generic [ref=e47]:
              - heading "SOL" [level=2] [ref=e48]
              - button "Remove SOL" [ref=e49]: Remove
            - paragraph [ref=e50]: $150.00
          - generic [ref=e51]:
            - generic [ref=e52]:
              - heading "LINK" [level=2] [ref=e53]
              - button "Remove LINK" [ref=e54]: Remove
            - paragraph [ref=e55]: $18.00
          - generic [ref=e56]:
            - generic [ref=e57]:
              - heading "DOGE" [level=2] [ref=e58]
              - button "Remove DOGE" [ref=e59]: Remove
            - paragraph [ref=e60]: $0.20
      - generic [ref=e61]:
        - generic [ref=e62]:
          - generic [ref=e63]:
            - heading "Portfolio summary" [level=2] [ref=e64]
            - paragraph [ref=e65]: Totals, holdings, realised P/L, and unrealised P/L from the backend.
          - generic [ref=e66]:
            - generic [ref=e67]:
              - generic [ref=e68]: Portfolio just now
              - generic [ref=e69]: Jun 19, 09:55:22 PM
            - button "Refresh portfolio" [ref=e70]
        - generic [ref=e72]:
          - generic [ref=e73]:
            - paragraph [ref=e74]: Current value
            - paragraph [ref=e75]: $0.00
            - paragraph [ref=e76]: Live market valuation
          - generic [ref=e77]:
            - paragraph [ref=e78]: Invested
            - paragraph [ref=e79]: $0.00
            - paragraph [ref=e80]: Remaining cost basis
          - generic [ref=e81]:
            - paragraph [ref=e82]: Unrealised P/L
            - paragraph [ref=e83]: $0.00
            - paragraph [ref=e84]: Open position result
          - generic [ref=e85]:
            - paragraph [ref=e86]: Realised P/L
            - paragraph [ref=e87]: $0.00
            - paragraph [ref=e88]: Closed trade result
          - generic [ref=e89]:
            - paragraph [ref=e90]: Total P/L
            - paragraph [ref=e91]: $0.00
            - paragraph [ref=e92]: Realised plus unrealised
          - generic [ref=e93]:
            - paragraph [ref=e94]: Realised vs Unrealised
            - generic [ref=e96]:
              - generic [ref=e99]:
                - generic [ref=e100]:
                  - generic [ref=e102]: Realised
                  - generic [ref=e103]: +$0.00
                - generic [ref=e104]:
                  - generic [ref=e106]: Unrealised
                  - generic [ref=e107]: +$0.00
              - generic [ref=e108]: Shows relative contribution to total P/L
          - generic [ref=e109]:
            - paragraph [ref=e110]: Assets
            - paragraph [ref=e111]: "0"
            - paragraph [ref=e112]: Current open positions
      - generic [ref=e113]:
        - generic [ref=e114]:
          - heading "Asset allocation" [level=2] [ref=e115]
          - paragraph [ref=e116]: Share of known portfolio value by open holding.
        - generic [ref=e117]: No open holdings yet. Allocation will appear after your first buy transaction.
      - generic [ref=e118]:
        - generic [ref=e119]:
          - generic [ref=e120]:
            - heading "Holdings" [level=2] [ref=e121]
            - paragraph [ref=e122]: Open positions calculated by the backend portfolio service.
          - generic [ref=e123]:
            - generic [ref=e124]:
              - text: Filter by symbol
              - textbox "Filter by symbol" [ref=e125]:
                - /placeholder: e.g. BTC
            - generic [ref=e126]:
              - text: Sort holdings
              - combobox "Sort holdings" [ref=e127]:
                - option "Symbol (A → Z)"
                - option "Quantity (high → low)"
                - option "Value (high → low)" [selected]
          - generic [ref=e128]: No open holdings yet. Record a buy transaction to create your first position.
        - generic [ref=e129]:
          - generic [ref=e130]:
            - paragraph [ref=e131]: New transaction
            - heading "Record buy or sell" [level=3] [ref=e132]
          - generic [ref=e133]:
            - generic [ref=e134]:
              - generic [ref=e135]: Type
              - combobox "Type" [ref=e136]:
                - option "Buy" [selected]
                - option "Sell"
            - generic [ref=e137]:
              - generic [ref=e138]: Coin
              - combobox "Coin" [ref=e139]:
                - option "Custom / manual entry"
                - option "BTC - Bitcoin" [selected]
                - option "ETH - Ethereum"
                - option "SOL - Solana"
                - option "LINK - Chainlink"
                - option "DOGE - Dogecoin"
                - option "ADA - Cardano"
                - option "XRP - XRP"
                - option "DOT - Polkadot"
                - option "MATIC - Polygon"
                - option "AVAX - Avalanche"
                - option "UNI - Uniswap"
                - option "ATOM - Cosmos"
                - option "XLM - Stellar"
                - option "LTC - Litecoin"
                - option "TRX - TRON"
                - option "BCH - Bitcoin Cash"
                - option "ALGO - Algorand"
                - option "ICP - Internet Computer"
                - option "NEAR - NEAR Protocol"
                - option "VET - VeChain"
                - option "BNB - BNB"
            - generic [ref=e140]:
              - generic [ref=e141]: Symbol
              - textbox "Symbol" [ref=e142]:
                - /placeholder: BTC
                - text: BTC
            - generic [ref=e143]:
              - generic [ref=e144]: Name
              - textbox "Name" [ref=e145]:
                - /placeholder: Bitcoin
                - text: Bitcoin
            - generic [ref=e146]:
              - generic [ref=e147]: Quantity
              - textbox "Quantity" [ref=e148]:
                - /placeholder: "0.5"
            - generic [ref=e149]:
              - generic [ref=e150]: Price USD
              - textbox "Price USD" [ref=e151]:
                - /placeholder: "65000"
          - button "Save transaction" [ref=e152]
      - generic [ref=e153]:
        - generic [ref=e154]:
          - heading "Transaction summary" [level=2] [ref=e155]
          - paragraph [ref=e156]: Buy, sell, and realised profit totals.
        - generic [ref=e157]:
          - generic [ref=e158]:
            - paragraph [ref=e159]: Buy volume
            - paragraph [ref=e160]: $0.00
            - paragraph [ref=e161]: Total purchase value
          - generic [ref=e162]:
            - paragraph [ref=e163]: Sell volume
            - paragraph [ref=e164]: $0.00
            - paragraph [ref=e165]: Total sale value
          - generic [ref=e166]:
            - paragraph [ref=e167]: Realised profit
            - paragraph [ref=e168]: $0.00
            - paragraph [ref=e169]: Profit from sells
      - generic [ref=e170]:
        - generic [ref=e171]:
          - generic [ref=e172]:
            - heading "Performance history" [level=2] [ref=e173]
            - paragraph [ref=e174]: Snapshots are created after transactions.
          - generic [ref=e175]:
            - button "7d" [ref=e176]
            - button "30d" [ref=e177]
            - button "90d" [ref=e178]
        - generic [ref=e179]: No performance snapshots in this range yet. Snapshots are created after transactions.
      - generic [ref=e180]:
        - generic [ref=e181]:
          - heading "Transactions" [level=2] [ref=e182]
          - paragraph [ref=e183]: Most recent records from the backend transaction service.
        - generic [ref=e184]: No transactions yet. Record a buy or sell transaction to get started.
  - generic [ref=e189] [cursor=pointer]:
    - button "Open Next.js Dev Tools" [ref=e190]:
      - img [ref=e191]
    - generic [ref=e194]:
      - button "Open issues overlay" [ref=e195]:
        - generic [ref=e196]:
          - generic [ref=e197]: "0"
          - generic [ref=e198]: "1"
        - generic [ref=e199]: Issue
      - button "Collapse issues badge" [ref=e200]:
        - img [ref=e201]
  - alert [ref=e203]
```

# Test source

```ts
  1  | import { expect, test } from "@playwright/test";
  2  | 
  3  | test("registers, logs in, creates a transaction, and shows dashboard data", async ({ page }) => {
  4  |   const uniqueSuffix = Date.now();
  5  |   const email = `e2e-${uniqueSuffix}@example.com`;
  6  |   const password = "Password123!";
  7  | 
  8  |   await page.goto("/register");
  9  | 
  10 |   await page.getByLabel("First name").fill("Satoshi");
  11 |   await page.getByLabel("Email").fill(email);
  12 |   await page.getByLabel("Password", { exact: true }).fill(password);
  13 |   await page.getByLabel("Confirm password", { exact: true }).fill(password);
  14 |   await page.getByRole("button", { name: "Register" }).click();
  15 | 
  16 |   await expect(page).toHaveURL(/\/dashboard$/);
  17 |   await expect(page.getByRole("button", { name: "Logout" })).toBeVisible();
  18 | 
  19 |   await page.getByRole("button", { name: "Logout" }).click();
  20 |   await expect(page).toHaveURL(/\/login$/);
  21 | 
  22 |   await page.getByLabel("Email").fill(email);
  23 |   await page.getByLabel("Password", { exact: true }).fill(password);
  24 |   await page.getByRole("button", { name: "Login" }).click();
  25 | 
  26 |   await expect(page).toHaveURL(/\/dashboard$/);
  27 |   await expect(page.getByText("Portfolio summary")).toBeVisible();
  28 | 
  29 |   await page.getByLabel("Coin").selectOption("BTC");
> 30 |   await expect(page.getByLabel("Symbol")).toHaveValue("BTC");
     |                                           ^ Error: expect(locator).toHaveValue(expected) failed
  31 |   await expect(page.getByLabel("Name")).toHaveValue("Bitcoin");
  32 | 
  33 |   await page.getByLabel("Quantity").fill("1");
  34 |   await page.getByLabel("Price USD").fill("50000");
  35 |   await page.getByRole("button", { name: "Save transaction" }).click();
  36 | 
  37 |   await expect(page.getByTestId("holding-row-BTC")).toContainText("Bitcoin");
  38 |   await expect(page.getByTestId("holding-row-BTC")).toContainText("1");
  39 |   await expect(page.getByTestId("holding-row-BTC")).toContainText("$50,000.00");
  40 |   await expect(page.getByTestId("holding-row-BTC")).toContainText("$60,000.00");
  41 |   await expect(page.getByTestId("transaction-row-BTC-buy")).toContainText("BUY");
  42 |   await expect(page.getByTestId("transaction-row-BTC-buy")).toContainText("$50,000.00");
  43 | 
  44 |   await expect(page.getByTestId("portfolio-card-current-value")).toContainText("$60,000.00");
  45 |   await expect(page.getByTestId("portfolio-card-invested")).toContainText("$50,000.00");
  46 |   await expect(page.getByTestId("portfolio-card-unrealised-p-l")).toContainText("$10,000.00");
  47 |   await expect(page.getByTestId("portfolio-card-total-p-l")).toContainText("$10,000.00");
  48 |   await expect(page.getByTestId("portfolio-card-assets")).toContainText("1");
  49 | });
  50 | 
```