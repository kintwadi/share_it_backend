#!/bin/bash

# Docker Hub Deployment Script for NearShare Backend
# Usage: ./SCRIPTS/deploy-to-docker.sh [version-tag]

set -e  # Exit on any error

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# Configuration
DOCKER_HUB_USERNAME=""  # Set your Docker Hub username here
IMAGE_NAME="nearshare-backend"
DEFAULT_TAG="latest"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is installed and running
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Please start Docker."
        exit 1
    fi
}

# Check if user is logged in to Docker Hub
check_docker_login() {
    if ! docker info 2>/dev/null | grep -q "Username:"; then
        print_warning "Not logged in to Docker Hub. Please run 'docker login' first."
        exit 1
    fi
}

# Get version from pom.xml or use provided tag
get_version() {
    local version_tag=$1
    
    if [ -n "$version_tag" ]; then
        echo "$version_tag"
    else
        # Extract version from pom.xml
        local pom_version=$(grep -oP '<version>\K[^<]+' pom.xml | head -1)
        if [ -n "$pom_version" ]; then
            echo "$pom_version"
        else
            echo "$DEFAULT_TAG"
        fi
    fi
}

# Build Docker image
build_image() {
    local tag=$1
    local full_image_name="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${tag}"
    
    print_status "Building Docker image: ${full_image_name}"
    
    if docker build -t "${full_image_name}" . ; then
        print_success "Image built successfully: ${full_image_name}"
        echo "${full_image_name}"  # Return the image name
    else
        print_error "Failed to build Docker image"
        exit 1
    fi
}

# Push image to Docker Hub
push_image() {
    local image_name=$1
    
    print_status "Pushing image to Docker Hub: ${image_name}"
    
    if docker push "${image_name}"; then
        print_success "Image pushed successfully to Docker Hub: ${image_name}"
    else
        print_error "Failed to push image to Docker Hub"
        exit 1
    fi
}

# Tag image with additional tags (latest, version)
tag_image() {
    local source_image=$1
    local target_tag=$2
    local target_image="${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${target_tag}"
    
    print_status "Tagging image: ${source_image} -> ${target_image}"
    
    if docker tag "${source_image}" "${target_image}"; then
        print_success "Image tagged successfully: ${target_image}"
        echo "${target_image}"  # Return the new image name
    else
        print_error "Failed to tag image"
        exit 1
    fi
}

# Main deployment function
deploy() {
    local version_tag=$1
    
    print_status "Starting Docker Hub deployment..."
    
    # Check prerequisites
    check_docker
    check_docker_login
    
    # Get version
    local version=$(get_version "$version_tag")
    print_status "Using version: ${version}"
    
    # Build image
    local image_name=$(build_image "$version")
    
    # Tag as latest if not already
    if [ "$version" != "latest" ]; then
        local latest_image=$(tag_image "$image_name" "latest")
        push_image "$latest_image"
    fi
    
    # Push the versioned image
    push_image "$image_name"
    
    print_success "Deployment completed successfully!"
    echo ""
    echo "Available images on Docker Hub:"
    echo "  - ${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:${version}"
    if [ "$version" != "latest" ]; then
        echo "  - ${DOCKER_HUB_USERNAME}/${IMAGE_NAME}:latest"
    fi
}

# Handle command line arguments
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Usage: $0 [version-tag]"
    echo ""
    echo "Deploy NearShare backend to Docker Hub"
    echo ""
    echo "Arguments:"
    echo "  version-tag  Optional version tag (default: extract from pom.xml or 'latest')"
    echo ""
    echo "Prerequisites:"
    echo "  - Docker installed and running"
    echo "  - Logged in to Docker Hub (docker login)"
    echo "  - Set DOCKER_HUB_USERNAME in the script"
    exit 0
fi

# Check if Docker Hub username is set
if [ -z "$DOCKER_HUB_USERNAME" ]; then
    print_error "DOCKER_HUB_USERNAME is not set. Please edit the script and set your Docker Hub username."
    exit 1
fi

# Run deployment
deploy "$1"
