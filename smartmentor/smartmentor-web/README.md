# SmartMentor Web

Vue 3 + Vite frontend for SmartMentor.

## Prerequisites

- Node.js 18+
- npm
- SmartMentor backend running on `http://localhost:8080`

## Install

```powershell
cd D:\Idea\中国软件杯\smartmentor-web
npm install
```

## Run

```powershell
cd D:\Idea\中国软件杯\smartmentor-web
npm run dev
```

Vite proxies `/api` requests to `http://localhost:8080`.

## Build

```powershell
cd D:\Idea\中国软件杯\smartmentor-web
npm run build
```

## Smoke Check

```powershell
cd D:\Idea\中国软件杯\smartmentor-web
npm run smoke
```

## Notes

- `src/` is the active Vue application source.
- `node_modules/` and `dist/` are generated output and should not be committed.
- The legacy top-level `js/` and `css/` folders should be removed after confirming no active page depends on them.
- API calls are centralized in `src/api/index.js`, including token handling and non-JSON backend error protection.
