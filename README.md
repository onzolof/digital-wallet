# digital-wallet

Proof-of-concept digital wallet: React dashboard on [GitHub Pages](https://onzolof.github.io/digital-wallet/) and a Spring Boot API on [Render](https://render.com), backed by the **NatWest Open Banking sandbox** plus synthetic Swiss account data for demos.

## Live demo

**https://onzolof.github.io/digital-wallet/**

![QR code linking to the live demo](./docs/demo-qr-code.png)

## Architecture (POC)

| Layer | Hosting | Notes |
|-------|---------|--------|
| Frontend | GitHub Pages | Static Vite build; API URL baked in at deploy time |
| Backend | Render (Docker) | Spring Boot, context path `/digital-wallet` |
| Bank data | NatWest sandbox | OAuth client credentials via env vars |
| FX rates | [Frankfurter](https://frankfurter.dev) | Public API, in-memory cache |

CI deploys backend and frontend to production only after tests pass on `main`.

## Limitations of this POC

These are intentional or accepted constraints of the current demo—not a production design.

### Major Improvements & Limitations

- Avoid hardcoded mapping when retrieving accounts and transactions from NatWest
- Map status, account type, and account subtype in DTOs to proper enums
- Make NatWest auth flow use token refreshing if possible
- Use the balance state from transactions instead of calculating it from scratch
- Increase testing (especially frontend code; backend is missing several tests too)