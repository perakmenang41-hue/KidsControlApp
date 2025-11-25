# KidsControlApp Backend


Minimal backend for danger zone feature.


## Setup


1. Copy `.env.example` to `.env` and set `MONGO_URI`.
2. Run `npm install`.
3. Start with `npm run dev` (requires nodemon) or `npm start`.


## Endpoints


- `POST /api/dangerzone/add` — add a danger zone. Body:
```json
{
"parentId": "parent123",
"name": "Gemilang Building",
"latitude": 3.1390,
"longitude": 101.6869,
"radius": 50
}