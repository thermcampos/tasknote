# Tools

All kind of tools and useful links and commands can be found here!

## Links

- **Docker Hub Container Registry:** https://hub.docker.io/
- **Time tracking:** https://track.toggl.com/timer

## Building locally

**Tasknote-web - Frontend Web App:**

Before building, you need define some env vars:

```bash
export VITE_BUILD=<branch-name-and-PR-number>
```

Then you can call the install and build scripts (from the `client` folder):

```bash
npm ci --ignore-scripts --no-update-notifier --omit=dev \
 && npm run build \
 && rm -rf node_modules
```

If you want to build with Docker:
```bash
docker build --no-cache \
 --build-arg VITE_BUILD="v999-$(date '+%Y-%m-%d-%H%M%S')" \
 --build-arg SOURCE_PR="v999-123456789-$(date '+%Y-%m-%d-%H%M%S')" \
 -t tasknote-web:candidate \
 ./client
```

That's it!

**Tasknote-api - Backend REST API:**

For the backend there's a Dockerfile ready, just run (from the project root):

```bash
docker build --no-cache \
 --build-arg BUILD="v999-$(date '+%Y-%m-%d-%H%M%S')" \
 --build-arg SOURCE_PR="v999-123456789-$(date '+%Y-%m-%d-%H%M%S')" \
 -t tasknote-api:candidate \
 ./server
```

That's it!

## Running with Docker

**DB:**

```bash
docker run -d -p 5432:5432 --rm \
  --name db \
  -e POSTGRES_DB=$POSTGRES_DB \
  -e POSTGRES_USER=$POSTGRES_USER \
  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
  postgres:15.8-bookworm
```

**Tasknote-api:**

```bash
docker run -d -p 8585:8585 --rm \
  --name tasknote-api \
  -e POSTGRES_DB=$POSTGRES_DB \
  -e POSTGRES_USER=$POSTGRES_USER \
  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
  -e POSTGRES_PORT=$POSTGRES_PORT \
  -e POSTGRES_HOST=$POSTGRES_HOST \
  -e CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS \
  rmcampos/tasknote-api:<tag>
```

Build Cloud Native: `./mvnw -B package -Pnative -DskipTests`

**Tasknote-web:**

The frontend app will run on Nginx.

## Interacting with GitHub Package and Container Registry

**Login in:**

```
export CR_PAT=YOUR_TOKEN
echo $CR_PAT | docker login -u RMCampos --password-stdin
```

**Pulling images:**

```sh
docker pull rmcampos/tasknote-app:latest
docker pull rmcampos/tasknote-api:latest
```

**Pushing images:**
```sh
docker push rmcampos/tasknote-api:latest
```

- Get container IP

```sh
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' db
```

- Get reason of health check failing on a container
```sh
docker inspect --format "{{json .State.Health}}" container_name_or_id | jq

# Or to see it live
while true; do clear; docker inspect --format "{{json .State.Health}}" tasknote-api | jq; sleep 1; done
```

- Extract Env value from docker image (without running)
```bash
docker inspect tasknote-web:candidate | jq -r '.[0].Config.Env[] | select(startswith("SOURCE_PR="))' | sed -n 's/SOURCE_PR=\(v[0-9]*\).*/\1/p'
```

## Restoring DB for testing

```bash
docker run \
 --name my-postgres-db \
 -e POSTGRES_USER="<user>" \
 -e POSTGRES_PASSWORD='<password>' \
 -e POSTGRES_DB="<db-name>" \
 -p 5432:5432 \
 -d postgres
```

```bash
docker exec -i my-postgres-db pg_restore -U <user> -d <db-name> < 2025-04-26T20_00_00.114Z.sql

```

## Updating packages

Node packages:

- npm install -g npm-check-updates
- Then run `ncu` and `ncu -u`

Angular dependencies:

- First run: `npx @angular/cli update @angular/cli @angular/core`
- Then run `npx npm-check-updates -u`

---

Build with:
mvn -Pnative -DskipTests spring-boot:build-image \
  -Dspring-boot.build-image.imageName=rmcampos/tasknote:api-latest 

Run with:
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=native \
  -e POSTGRES_HOST=localhost \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=tasknote \
  -e POSTGRES_USER=tasknoteuser \
  -e POSTGRES_PASSWORD=default \
  -e SECURITY_KEY=9052e499446dac5fa2d69dd07f1f6381a360c646c63d555244c3a2911494f63a \
  -e CORS_ALLOWED_ORIGINS=http://localhost:5000 \
  -e SERVER_SERVLET_CONTEXT_PATH=/ \
  --network host \
  docker.io/rmcampos/tasknote:api-latest

## Running a single class test file

```
# For a single class
mvn test -P test -Dtest=YourTestClassName

# For a single class method
mvn test -P test -Dtest=YourTestClassName#method
```

## Running SQL query from the CLI using docker

```bash
docker exec -it tasknote-db psql -U tasknoteuser -d tasknote -c "SET search_path TO tasknote; SELECT * FROM users;
```
