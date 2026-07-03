FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:20-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=10000

RUN npm install -g serve

COPY --from=build /app/dist/vicinity24-company/browser ./browser

EXPOSE 10000

CMD sh -c "serve -s /app/browser -l tcp://0.0.0.0:${PORT}"
