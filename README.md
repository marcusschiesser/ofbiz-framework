# OFBiz Docker Demo

Start the PostgreSQL-backed OFBiz demo from the repository root:

```bash
docker compose -f docker/examples/postgres-demo/docker-compose.yml up --build
```

Wait until the OFBiz logs show `is started and ready.`, then open:

- [https://localhost:8443/partymgr](https://localhost:8443/partymgr)
- Login: `admin` / `ofbiz`

Useful docs:

- Docker docs: [DOCKER.adoc](/Users/marcus/code/schiesser/ofbiz-framework/DOCKER.adoc)
- Online docs pointer: [OFBiz-Online-Documentation.adoc](/Users/marcus/code/schiesser/ofbiz-framework/OFBiz-Online-Documentation.adoc)
- Original root README backup: [README.adoc.bak](/Users/marcus/code/schiesser/ofbiz-framework/README.adoc.bak)
