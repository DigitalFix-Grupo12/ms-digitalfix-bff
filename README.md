# ms-digitalfix-bff (puerto 8080)

[![CI](https://github.com/DigitalFix-Grupo12/ms-digitalfix-bff/actions/workflows/ci.yml/badge.svg)](https://github.com/DigitalFix-Grupo12/ms-digitalfix-bff/actions/workflows/ci.yml)

Backend for Frontend de DigitalFix. Es el único servicio expuesto hacia el API Gateway:
valida el JWT emitido por Microsoft Entra ID, autoriza por rol y reenvía la petición
al microservicio de dominio correspondiente.

## Validación del token

| Verificación | Implementación |
|---|---|
| Firma | Claves públicas JWKS del tenant (`issuer-uri` → discovery OIDC) |
| Vigencia (`exp`, `nbf`) | Validadores por defecto de Spring Security |
| Emisor (`iss`) | `https://login.microsoftonline.com/<tenant>/v2.0` |
| Audiencia (`aud`) | `JwtAudienceValidator` (GUID de la app `digitalfix-api`) |
| Roles | `EntraIdRolesConverter`: claim `roles` → `ROLE_Admin`, `ROLE_Supervisor`, ... |

Sin token o con token inválido responde **401**; con un rol sin permiso, **403** (JSON).

## Rutas y roles

| Método | Ruta | Roles | Destino |
|---|---|---|---|
| GET | /actuator/health | público | — |
| GET | /api/workorders | Admin, Supervisor, Cliente | workorders :8082 |
| GET | /api/workorders/{id} | Admin, Supervisor, Cliente | workorders :8082 |
| POST | /api/workorders | Admin, Supervisor, Cliente | workorders :8082 |
| PUT | /api/workorders/{id}/status | Admin, Supervisor | workorders :8082 |
| GET | /api/catalog/services, /api/catalog/services/{id} | Admin, Supervisor | catalog :8083 |
| GET | /api/report/kpis | Admin | report :8084 |
| GET | /api/audit?limit&referencia | Admin, Auditor | audit :8085 |

El BFF propaga la identidad en los headers `X-User-Name` (`preferred_username`) y
`X-User-Roles`; con eso `ms-digitalfix-workorders` limita al rol Cliente a sus propias órdenes.
Si un microservicio no responde, el BFF devuelve **503** con cuerpo JSON en vez de un error genérico.

## Configuración

| Variable | Descripción |
|---|---|
| `AZURE_TENANT_ID` | Tenant de Entra ID |
| `AZURE_API_CLIENT_ID` | Application ID de `digitalfix-api` (audiencia esperada) |
| `DIGITALFIX_SECURITY_ALLOWED_ORIGINS` | Orígenes CORS permitidos (en la EC2 lo define el user-data) |

## Ejecutar

```powershell
$env:AZURE_TENANT_ID = "<tenant-id>"
$env:AZURE_API_CLIENT_ID = "<api-client-id>"
mvn spring-boot:run
```

Tests: `mvn test` (5 pruebas de seguridad: 401, 403 por rol y 503 cuando un servicio está caído).
