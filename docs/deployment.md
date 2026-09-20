# Production deployment

This describes the setup the application is run with in production: **MariaDB 11.8 and the application in
Docker, behind nginx that terminates TLS** (optionally behind Cloudflare). It replaces the outdated
deployment notes in the README. Everything below was verified on a small VPS (2 vCPU, 4 GB RAM, 25 GB disk).

## 1. What you need

- A Linux host with Docker and the Docker Compose plugin, 4 GB RAM, about 10 GB free disk.
- A domain name pointing at the host, and ports 80 and 443 open.
- The database dump of the anime data (see step 4).

Disk: the rating table `users_anime_score` has about 24 million rows and is by far the largest part. The data
needs about 7 GB with the original six secondary indexes on that table and about 4 GB with only the two the
application uses (`idx_users_anime_score_anime_rating_range` and `idx_critical_user_exclude_anime`).

## 2. Configuration

Create the files next to `compose.prod.yaml` (all are git-ignored):

```bash
mkdir -p secrets
# .env - no passwords in here
cat > .env <<'EOF'
DB_NAME=animerecoapp
DB_USERNAME=animerecoapp_user
DB_URL=jdbc:mariadb://db:3306/animerecoapp
EOF
# passwords, one per file, no trailing newline needed
openssl rand -hex 24 > secrets/db_pass.txt
openssl rand -hex 24 > secrets/db_root_pass.txt
chmod 700 secrets
```

The application reads the database password from `/run/secrets/DB_PASSWORD`
(`spring.config.import=configtree:/run/secrets/` in `application-prod.yml`).

## 3. Build and start

```bash
docker compose -f compose.prod.yaml up -d --build
docker compose -f compose.prod.yaml ps      # both containers should become "healthy"
```

The database is not published, the application listens on `127.0.0.1:8080` only.

## 4. Load the data

The tables use the collation `utf8mb4_0900_ai_ci`, which needs **MariaDB 11.4.5 or newer** (the compose file
uses 11.8). Restore a dump made with `mariadb-dump --single-transaction`:

```bash
zcat dump.sql.gz | docker exec -i -e MYSQL_PWD="$(cat secrets/db_root_pass.txt)" anime_db mariadb -uroot animerecoapp
```

Loading 24 million rating rows took about three minutes with only the two secondary indexes (more indexes take
longer). To save disk and time, drop the redundant indexes
from the `CREATE TABLE` of `users_anime_score` in the dump before loading it.

## 5. Reverse proxy (nginx)

`/etc/nginx/conf.d/00-limits.conf` (http level):

```nginx
limit_req_zone  $binary_remote_addr zone=anime_req:10m  rate=10r/s;
limit_conn_zone $binary_remote_addr zone=anime_conn:10m;
limit_req_status  429;
limit_conn_status 429;
```

`/etc/nginx/conf.d/anime.conf`:

```nginx
server {
    listen 80;
    server_name anime.example.com;
    location /.well-known/acme-challenge/ { root /var/www/letsencrypt; }   # certbot --webroot
    location / { return 301 https://$host$request_uri; }
}

server {
    listen 443 ssl;
    http2 on;
    server_name anime.example.com;
    ssl_certificate     /etc/letsencrypt/live/anime.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/anime.example.com/privkey.pem;

    # only when the site is behind Cloudflare: real visitor address (list: https://www.cloudflare.com/ips/)
    # include /etc/nginx/snippets/cloudflare-realip.conf;      # set_real_ip_from ...; real_ip_header CF-Connecting-IP;

    limit_req  zone=anime_req burst=40 nodelay;
    limit_conn anime_conn 30;
    client_max_body_size 10m;

    # only the health endpoint of the actuator is public
    location = /actuator/health { proxy_pass http://127.0.0.1:8080; }
    location /actuator           { return 404; }
    location ~ /\.(?!well-known)  { return 404; }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;    # the application relies on this (forward-headers-strategy)
    }
}
```

The type-ahead search sends one request per pause in typing (250 ms debounce); the limits above leave plenty of room.

**Behind Cloudflare** use SSL mode *Full (strict)* with a Let's Encrypt certificate (HTTP-01 works through the
proxy as long as port 80 is open). Because all visitors then arrive from Cloudflare, ports 80/443 can be
restricted to Cloudflare's published address ranges in the host firewall, so the site cannot be reached
around Cloudflare.

## 6. What the production profile does for you

- `server.forward-headers-strategy: framework`: redirects and absolute URLs use `https` (without it the
  application redirects to `http://...` after a search and browsers warn about an insecure connection).
- The session cookie is `Secure` and `SameSite=Lax`. To try the prod profile over plain `http` on localhost
  set `SESSION_COOKIE_SECURE=false` - never on a public server.
- Logging: root `WARN`, application `INFO`. The timings of the recommendation steps are logged at `DEBUG`;
  set `LOGGING_LEVEL_CZ_KOCABEK_ANIMERECOMEDATIONSYSTEM=DEBUG` on the container to see them.

## 7. Operations

**Update to a new version**

```bash
docker tag anime-recommend-app:latest anime-recommend-app:previous     # keep the running version for a rollback
git pull && docker compose -f compose.prod.yaml up -d --build anime-recommend-app
```

**Roll back**

```bash
docker tag anime-recommend-app:previous anime-recommend-app:latest
docker compose -f compose.prod.yaml up -d --no-build anime-recommend-app
```

**Warm cache.** The first search after the *database* was restarted reads its index pages from disk and can
take several seconds. MariaDB saves the cached pages on a normal shutdown and reloads them at start
(`compose.prod.yaml` saves all of them and gives the shutdown up to 120 s to do it), so restarts stay fast.
Stop the stack with `docker compose stop`/`down`, not by killing the containers.

**Backup.** `docker exec -e MYSQL_PWD=... anime_db mariadb-dump --single-transaction animerecoapp | gzip > backup.sql.gz`,
plus snapshots of the host if the provider offers them. The `app_accounts` and `acc_watchlist` tables hold the
only data users create; everything else can be re-imported.

**Security checklist.** Do not publish ports 3306 and 8080; allow SSH by key only (or protect it with fail2ban);
keep `secrets/` and `.env` out of the repository (they are git-ignored).
