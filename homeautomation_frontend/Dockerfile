FROM node:18 AS build

WORKDIR /app
COPY . .

RUN npm ci
RUN npm run build

FROM node:18

WORKDIR /app
COPY --from=build /app/.next .next
COPY --from=build /app/public public
COPY --from=build /app/package*.json ./
COPY --from=build /app/node_modules node_modules

EXPOSE 3000

CMD ["npm", "start"]
