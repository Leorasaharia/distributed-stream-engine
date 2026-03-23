<#
.SYNOPSIS
Builds the required Docker images and deploys the Distributed Stream Engine to Kubernetes.

.DESCRIPTION
This script builds the stream-engine, stream-engine-worker, and stream-engine-producer Docker images locally,
and then uses kubectl to apply the manifests in the /k8s directory. 
Note: If using Minikube, you might also need to use `minikube image load <image_name>` after building, 
or run `eval $(minikube docker-env)` before running this script.
#>

Write-Host "Starting build and deployment process for Kubernetes..." -ForegroundColor Cyan

# 1. Build Stream Engine
Write-Host "Building image: stream-engine:latest..." -ForegroundColor Yellow
docker build -t stream-engine:latest ./stream-engine
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed for stream-engine."; exit 1 }

# 2. Build Celery Worker
Write-Host "Building image: stream-engine-worker:latest..." -ForegroundColor Yellow
docker build -t stream-engine-worker:latest ./worker
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed for stream-engine-worker."; exit 1 }

# 3. Build Producer
Write-Host "Building image: stream-engine-producer:latest..." -ForegroundColor Yellow
docker build -t stream-engine-producer:latest ./producer
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed for stream-engine-producer."; exit 1 }

Write-Host "Successfully built all Docker images!" -ForegroundColor Green

# 4. Apply Kubernetes Manifests
Write-Host "Applying Kubernetes manifests from ./k8s directory..." -ForegroundColor Yellow
kubectl apply -f ./k8s
if ($LASTEXITCODE -ne 0) { Write-Error "Failed to apply Kubernetes manifests."; exit 1 }

Write-Host "Deployment applied successfully!" -ForegroundColor Green
Write-Host "Run 'kubectl get pods' to check the status of your deployments." -ForegroundColor Cyan
