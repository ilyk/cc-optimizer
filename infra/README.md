# Infrastructure

Infrastructure as Code (IaC) and deployment configurations.

## Structure

```
infra/
├── docker/           # Docker configurations
├── kubernetes/       # K8s manifests (future)
├── scripts/          # Deployment scripts
└── terraform/        # Terraform configs (future)
```

## Docker

Docker configurations for containerized deployment:

### Backend Container

```dockerfile
# infra/docker/Dockerfile.backend
FROM node:18-alpine

WORKDIR /app

COPY backend/package*.json ./
RUN npm ci --only=production

COPY backend/ ./

EXPOSE 3000

CMD ["node", "server.js"]
```

### Docker Compose

```yaml
# infra/docker/docker-compose.yml
version: '3.8'

services:
  backend:
    build:
      context: ../..
      dockerfile: infra/docker/Dockerfile.backend
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - DATABASE_URL=postgresql://user:pass@db:5432/ccoptimizer
    env_file:
      - ../../backend/.env
    depends_on:
      - db
  
  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=ccoptimizer
      - POSTGRES_USER=ccuser
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

## Deployment Scripts

Quick deployment scripts for common platforms.

## Future Additions

- Kubernetes manifests for orchestration
- Terraform for cloud infrastructure
- CI/CD pipeline configurations
- Monitoring and logging setup

---

**See Also**:
- [Backend README](../backend/README.md)
- [Deployment Guide](../docs/development/deployment.md)

