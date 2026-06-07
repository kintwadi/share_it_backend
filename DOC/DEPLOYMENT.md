# Docker Hub Deployment Guide

This guide explains how to deploy the Vicinity24 backend application to Docker Hub.

## Prerequisites

1. **Docker Installed**: Make sure Docker Desktop is installed and running on your system
2. **Docker Hub Account**: Create an account at [hub.docker.com](https://hub.docker.com)
3. **Login**: Run `docker login` in your terminal and enter your credentials

## Configuration

### 1. Set your Docker Hub username

Edit the deployment script and set your Docker Hub username:

**For Bash/Linux/Mac:**
```bash
# Edit SCRIPTS/deploy-to-docker.sh
DOCKER_HUB_USERNAME="your_dockerhub_username"
```

**For Windows:**
```batch
:: Edit SCRIPTS/deploy-to-docker.bat
set DOCKER_HUB_USERNAME=your_dockerhub_username
```

### 2. Optional: Create environment file

Copy the configuration template:
```bash
cp docker-deploy.config docker-deploy.env
```

Edit `docker-deploy.env` with your settings:
```bash
DOCKER_HUB_USERNAME=your_dockerhub_username
IMAGE_NAME=Vicinity24-backend
DEFAULT_TAG=latest
```

## Usage

### Basic Deployment

**Linux/Mac/WSL:**
```bash
# Make the script executable
chmod +x SCRIPTS/deploy-to-docker.sh

# Deploy with automatic version detection
./SCRIPTS/deploy-to-docker.sh

# Deploy with specific version tag
./SCRIPTS/deploy-to-docker.sh v1.2.3
```

**Windows:**
```batch
:: Deploy with automatic version detection
SCRIPTS\\deploy-to-docker.bat

:: Deploy with specific version tag
SCRIPTS\\deploy-to-docker.bat v1.2.3
```

### Version Tagging

The script supports multiple tagging strategies:

1. **Automatic**: Extracts version from `pom.xml`
2. **Manual**: Specify version as argument
3. **Additional tags**: Always creates `latest` tag in addition to version

Examples:
- `./SCRIPTS/deploy-to-docker.sh` -> tags: `0.0.1-SNAPSHOT` + `latest`
- `./SCRIPTS/deploy-to-docker.sh v1.0.0` -> tags: `v1.0.0` + `latest`
- `./SCRIPTS/deploy-to-docker.sh production` -> tags: `production` + `latest`

## What the Script Does

1. **Validation**: Checks Docker installation and login status
2. **Version Detection**: Extracts version from pom.xml or uses provided tag
3. **Build**: Creates Docker image with proper tagging
4. **Tagging**: Adds additional tags (latest, version)
5. **Pushing**: Uploads all tagged images to Docker Hub

## Manual Deployment Steps

If you prefer to run commands manually:

```bash
# Build the image
docker build -t yourusername/Vicinity24-backend:version .

# Tag as latest
docker tag yourusername/Vicinity24-backend:version yourusername/Vicinity24-backend:latest

# Push to Docker Hub
docker push yourusername/Vicinity24-backend:version
docker push yourusername/Vicinity24-backend:latest
```

## Environment Variables

The application requires these environment variables when running the container:

```bash
# Database type hint
DB_TYPE=postgres

# Multi-tenant default database
TENANT_DEFAULT_DB_URL=jdbc:postgresql://host:5432/default_database?sslmode=require
TENANT_DEFAULT_DB_USERNAME=username
TENANT_DEFAULT_DB_PASSWORD=password
TENANT_DEFAULT_DB_DRIVER=org.postgresql.Driver

# Optional extra tenants
TENANT_A_DB_URL=jdbc:postgresql://host:5432/vicinity24_tenant_a?sslmode=require
TENANT_A_DB_USERNAME=username
TENANT_A_DB_PASSWORD=password
TENANT_A_DB_DRIVER=org.postgresql.Driver

# TLS / Keystore
SSL_PASSWORD=your_keystore_password
KEYSTORE_ACCESS_TOKEN_ALIAS=accesstoken
KEYSTORE_ACCESS_TOKEN_PW=your_access_token_key_password
KEYSTORE_REFRESH_TOKEN_ALIAS=refreshtoken
KEYSTORE_REFRESH_TOKEN_PW=your_refresh_token_key_password

# Multi-tenant routing
SETTING_USE_DEFAULT_DATABASE=true
TENANT_HEADER_NAME=X-Tenant-ID
TENANT_DEFAULT_ID=default
TENANT_DEFAULT_DB_URL=jdbc:postgresql://host:5432/default_database?sslmode=require
TENANT_DEFAULT_DB_USERNAME=username
TENANT_DEFAULT_DB_PASSWORD=password
TENANT_DEFAULT_DB_DRIVER=org.postgresql.Driver
# Optional extra tenants
TENANT_A_DB_URL=jdbc:postgresql://host:5432/vicinity24_tenant_a?sslmode=require
TENANT_A_DB_USERNAME=username
TENANT_A_DB_PASSWORD=password
TENANT_A_DB_DRIVER=org.postgresql.Driver

# Encryption
ENCRYPTION_KEY=1234567890123456

# AWS/R2 Storage
AWS_ACCESS_KEY_ID=your-key-id
AWS_SECRET_ACCESS_KEY=your-secret-key

# Stripe Payments
STRIPE_PUBLIC_KEY=test_public_key
STRIPE_SECRET_KEY=test_secret_key
```

## Render (Supabase Postgres) notes

If your Render deploy fails with `java.net.SocketException: Network unreachable` while connecting to Postgres, the app is typically trying to use an IPv6 address without IPv6 egress available in the runtime.

Recommended fixes:

1. Use the Supabase connection pooler host (shown in Supabase -> Database -> Connection string / Pooler). It often provides IPv4 connectivity even when the direct `db.<project>.supabase.co` hostname is IPv6-only.
2. Set `JAVA_TOOL_OPTIONS` in Render to force Java to prefer IPv4:

```bash
JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false
```

Also make sure you set your environment variables in Render's Environment tab (Render does not automatically use your local `.env` file).

For multi-tenant deployments on Render:

1. Set `SETTING_USE_DEFAULT_DATABASE=true` if you want existing clients without `X-Tenant-ID` to continue working.
2. Set `TENANT_HEADER_NAME` to the request header your gateway or client will send.
3. Set `TENANT_DEFAULT_ID` and `TENANT_DEFAULT_DB_*` for the fallback database.
4. Add any extra static tenant databases with `TENANT_A_*`, `TENANT_B_*`, and remember that the request header must match the actual `tenants.config.*` keys in `application.properties` such as `vicinity24_tenant_a` and `vicinity24_tenant_b`.

## Troubleshooting

### Common Issues

1. **Docker not running**: Start Docker Desktop
2. **Not logged in**: Run `docker login`
3. **Permission denied**: Run script as administrator (Windows) or with sudo (Linux)
4. **Build fails**: Check Dockerfile syntax and dependencies

### Debug Mode

Add `set -x` to the bash script or `@echo on` to the batch file to see detailed output.

## CI/CD Integration

For automated deployments, set these environment variables in your CI system:

- `DOCKER_HUB_USERNAME`: Your Docker Hub username
- `DOCKER_HUB_PASSWORD`: Your Docker Hub password/access token
- `DOCKER_HUB_REPOSITORY`: Repository name (default: Vicinity24-backend)

## Security Notes

- Never commit actual credentials to version control
- Use Docker secrets or environment files for production
- Regularly update base images for security patches
- Use Docker Hub access tokens instead of passwords

## Support

For issues with deployment, check:

1. Docker logs: `docker logs container-name`
2. Build logs: `docker build .`
3. Network connectivity: `docker pull hello-world`

