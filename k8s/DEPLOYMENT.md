# Kubernetes Deployment Guide

## Prerequisites
- Minikube installed and running
- kubectl configured
- Docker installed

## Step 1: Start Minikube

```bash
minikube start --cpus=4 --memory=8192
```

Verify Minikube is running:
```bash
minikube status
```

## Step 2: Build Docker Images in Minikube

Point Docker to Minikube's Docker daemon:
```bash
eval $(minikube docker-env)
```

Build the images:
```bash
# Build User Service
docker build -t user-service:latest ./user-service

# Build Seating Service
docker build -t seating-service:latest ./seating-service
```

Verify images:
```bash
docker images | grep -E "user-service|seating-service"
```

## Step 3: Deploy Databases First

Deploy PostgreSQL databases and Redis:
```bash
kubectl apply -f k8s/userdb.yaml
kubectl apply -f k8s/seatingdb.yaml
kubectl apply -f k8s/redis.yaml
```

Wait for databases to be ready:
```bash
kubectl wait --for=condition=ready pod -l app=userdb --timeout=120s
kubectl wait --for=condition=ready pod -l app=seatingdb --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis --timeout=120s
```

Verify database pods:
```bash
kubectl get pods
```

## Step 4: Deploy Services

Deploy the microservices:
```bash
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/seating-service.yaml
```

Wait for services to be ready:
```bash
kubectl wait --for=condition=ready pod -l app=user-service --timeout=180s
kubectl wait --for=condition=ready pod -l app=seating-service --timeout=180s
```

## Step 5: Verify Deployment

Check all pods are running:
```bash
kubectl get pods
```

Expected output:
```
NAME                              READY   STATUS    RESTARTS   AGE
userdb-xxx                        1/1     Running   0          2m
seatingdb-xxx                     1/1     Running   0          2m
redis-xxx                         1/1     Running   0          2m
user-service-xxx                  1/1     Running   0          1m
user-service-yyy                  1/1     Running   0          1m
seating-service-xxx               1/1     Running   0          1m
seating-service-yyy               1/1     Running   0          1m
```

Check services:
```bash
kubectl get svc
```

Expected output:
```
NAME              TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
userdb            ClusterIP   10.x.x.x        <none>        5432/TCP         2m
seatingdb         ClusterIP   10.x.x.x        <none>        5432/TCP         2m
redis             ClusterIP   10.x.x.x        <none>        6379/TCP         2m
user-service      NodePort    10.x.x.x        <none>        8081:30081/TCP   1m
seating-service   NodePort    10.x.x.x        <none>        8082:30082/TCP   1m
```

## Step 6: Access Services

Get Minikube IP:
```bash
minikube ip
```

Get service URLs:
```bash
minikube service user-service --url
minikube service seating-service --url
```

Or access directly:
```bash
# User Service
curl http://$(minikube ip):30081/actuator/health

# Seating Service
curl http://$(minikube ip):30082/actuator/health
```

## Step 7: Test the Application

### Register a User
```bash
MINIKUBE_IP=$(minikube ip)

curl -X POST http://$MINIKUBE_IP:30081/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User",
    "city": "New York"
  }'
```

### Login
```bash
curl -X POST http://$MINIKUBE_IP:30081/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

### Check Seat Availability (after creating seats)
```bash
curl -X GET "http://$MINIKUBE_IP:30082/v1/seats/availability?eventId=1"
```

## Step 8: View Logs

View logs for debugging:
```bash
# User Service logs
kubectl logs -l app=user-service --tail=100 -f

# Seating Service logs
kubectl logs -l app=seating-service --tail=100 -f

# Database logs
kubectl logs -l app=userdb --tail=50
kubectl logs -l app=seatingdb --tail=50
```

## Step 9: Monitor Resources

Check resource usage:
```bash
kubectl top nodes
kubectl top pods
```

View HPA status:
```bash
kubectl get hpa
```

## Step 10: Scale Services

Manual scaling:
```bash
# Scale User Service
kubectl scale deployment user-service --replicas=3

# Scale Seating Service
kubectl scale deployment seating-service --replicas=3
```

Verify scaling:
```bash
kubectl get pods -l app=user-service
kubectl get pods -l app=seating-service
```

## Troubleshooting

### Pod not starting
```bash
# Describe pod for events
kubectl describe pod <pod-name>

# Check pod logs
kubectl logs <pod-name>
```

### Database connection issues
```bash
# Check database pod
kubectl get pod -l app=userdb
kubectl logs -l app=userdb

# Test database connectivity from service pod
kubectl exec -it <user-service-pod> -- sh
ping userdb
```

### Service not accessible
```bash
# Check service endpoints
kubectl get endpoints

# Verify NodePort
kubectl get svc user-service -o yaml | grep nodePort
```

### Image pull errors
Make sure you're using Minikube's Docker daemon:
```bash
eval $(minikube docker-env)
docker images
```

## Cleanup

Delete all resources:
```bash
kubectl delete -f k8s/
```

Or delete specific resources:
```bash
kubectl delete -f k8s/user-service.yaml
kubectl delete -f k8s/seating-service.yaml
kubectl delete -f k8s/userdb.yaml
kubectl delete -f k8s/seatingdb.yaml
kubectl delete -f k8s/redis.yaml
```

Stop Minikube:
```bash
minikube stop
```

Delete Minikube cluster:
```bash
minikube delete
```

## Additional Commands

### Port Forwarding (Alternative Access)
```bash
# Forward User Service port
kubectl port-forward svc/user-service 8081:8081

# Forward Seating Service port
kubectl port-forward svc/seating-service 8082:8082
```

Then access at:
- User Service: http://localhost:8081
- Seating Service: http://localhost:8082

### Dashboard
```bash
# Open Kubernetes dashboard
minikube dashboard
```

### Check ConfigMaps and Secrets
```bash
kubectl get configmaps
kubectl get secrets

# View specific configmap
kubectl describe configmap user-service-config
```

### Exec into Pod
```bash
# Get shell in user-service pod
kubectl exec -it <user-service-pod-name> -- /bin/sh

# Run commands
curl localhost:8081/actuator/health
```

## Performance Testing

Load test with multiple requests:
```bash
# Install hey (HTTP load generator)
# brew install hey  # MacOS
# or download from https://github.com/rakyll/hey

# Load test user registration
hey -n 100 -c 10 -m POST \
  -H "Content-Type: application/json" \
  -d '{"username":"user'$(date +%s)'","email":"test'$(date +%s)'@example.com","password":"pass","firstName":"Test","lastName":"User"}' \
  http://$(minikube ip):30081/v1/users/register
```

Watch HPA scale:
```bash
kubectl get hpa --watch
```

## Best Practices

1. Always wait for databases to be ready before deploying services
2. Use health probes to ensure pod readiness
3. Set resource requests and limits
4. Use ConfigMaps for configuration
5. Use Secrets for sensitive data
6. Enable HPA for automatic scaling
7. Monitor logs and metrics regularly
8. Use namespaces for better organization (optional)

## Next Steps

1. Set up Ingress controller for external access
2. Configure persistent volumes for production
3. Set up monitoring with Prometheus and Grafana
4. Implement logging with ELK stack
5. Configure Network Policies for security
6. Set up CI/CD pipeline for automated deployments
