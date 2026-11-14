# Digital Wallet frontend

Client-only React app, served by Vite and styled with Tailwind and shadcn.

```sh
npm install
npm run dev
```

```sh
npm run generate:api
npm test
npm run lint
npm run format
```

`generate:api` reads `backend/src/main/resources/openapi/wallet-api.yaml` and writes the RTK Query hooks in `src/store/walletApi.ts`. The dev server proxies `/digital-wallet` to the Spring app on port 8080.
