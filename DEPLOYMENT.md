# Docker Hub Deployment Guide

This guide explains how to deploy the NearShare backend application to Docker Hub.

## Prerequisites

1. **Docker Installed**: Make sure Docker Desktop is installed and running on your system
2. **Docker Hub Account**: Create an account at [hub.docker.com](https://hub.docker.com)
3. **Login**: Run `docker login` in your terminal and enter your credentials

## Configuration

### 1. Set your Docker Hub username

Edit the deployment script and set your Docker Hub username:

**For Bash/Linux/Mac:**
```bash
# Edit deploy-to-dockerhub.sh
DOCKER_HUB_USERNAME="your_dockerhub_username"
```

**For Windows:**
```batch
:: Edit deploy-to-dockerhub.bat
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
IMAGE_NAME=nearshare-backend
DEFAULT_TAG=latest
```

## Usage

### Basic Deployment

**Linux/Mac/WSL:**
```bash
# Make the script executable
chmod +x deploy-to-dockerhub.sh

# Deploy with automatic version detection
./deploy-to-dockerhub.sh

# Deploy with specific version tag
./deploy-to-dockerhub.sh v1.2.3
```

**Windows:**
```batch
:: Deploy with automatic version detection
deploy-to-dockerhub.bat

:: Deploy with specific version tag
deploy-to-dockerhub.bat v1.2.3
```

### Version Tagging

The script supports multiple tagging strategies:

1. **Automatic**: Extracts version from `pom.xml`
2. **Manual**: Specify version as argument
3. **Additional tags**: Always creates `latest` tag in addition to version

Examples:
- `./deploy-to-dockerhub.sh` → tags: `0.0.1-SNAPSHOT` + `latest`
- `./deploy-to-dockerhub.sh v1.0.0` → tags: `v1.0.0` + `latest`
- `./deploy-to-dockerhub.sh production` → tags: `production` + `latest`

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
docker build -t yourusername/nearshare-backend:version .

# Tag as latest
docker tag yourusername/nearshare-backend:version yourusername/nearshare-backend:latest

# Push to Docker Hub
docker push yourusername/nearshare-backend:version
docker push yourusername/nearshare-backend:latest
```

## Environment Variables

The application requires these environment variables when running the container:

```bash
# Database
DB_URL=jdbc:postgresql://host:5432/database
DB_USERNAME=username
DB_PASSWORD=password

# JWT
JWT_SECRET=your-jwt-secret-key

# AWS/R2 Storage
AWS_ACCESS_KEY_ID=your-key-id
AWS_SECRET_ACCESS_KEY=your-secret-key

# Stripe Payments
STRIPE_PUBLIC_KEY=pk_test_...
STRIPE_SECRET_KEY=sk_test_...
```

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
- `DOCKER_HUB_REPOSITORY`: Repository name (default: nearshare-backend)

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