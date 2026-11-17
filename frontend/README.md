# Digital Wallet frontend

Client-only React app, served by Vite and styled with Tailwind and shadcn.

Start the Spring backend on port 8080 (`backend/./mvnw spring-boot:run`), then:

```sh
npm install
npm run dev
```

Open **http://localhost:5173/digital-wallet/** — `.env.development` sets `VITE_API_BASE_URL=http://localhost:8080/digital-wallet`. The backend allows CORS from `http://localhost:5173` by default.

```sh
npm run generate:api
npm test
npm run lint
npm run format
```

`generate:api` reads `backend/src/main/resources/openapi/wallet-api.yaml` and writes the RTK Query hooks in `src/store/walletApi.ts`.
