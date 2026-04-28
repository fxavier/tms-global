# Plano de Implementação — TMS (Transport Management System)

## Visão Geral

Este plano de implementação converte o design técnico do TMS numa sequência de tarefas de código incrementais, organizadas em 8 fases. Cada fase constrói sobre a anterior, garantindo que nenhum código fica órfão ou por integrar.

**Stack:** Java 21 · Spring Boot 3.x · Spring Modulith · PostgreSQL · Flyway · Keycloak · Next.js · Flutter

---

## Tarefas

### Fase 1 — Infraestrutura Base (Semanas 1–2)

- [x] 1. Criar projeto Spring Boot com estrutura de módulos
  - Inicializar projeto Maven com Java 21 e Spring Boot 3.x
  - Adicionar dependências ao `pom.xml`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-validation`, `spring-boot-starter-aop`, `spring-boot-starter-actuator`, `spring-modulith-starter-core`, `spring-modulith-starter-jpa`, `postgresql`, `flyway-core`, `flyway-database-postgresql`, `lombok`, `mapstruct`, `caffeine`, `bucket4j-core`, `aws-sdk-s3`, `micrometer-registry-prometheus`
  - Criar classe principal `TmsApplication.java` em `pt.company.tms`
  - Criar estrutura de pacotes para os módulos: `vehicle`, `driver`, `activity`, `alert`, `audit`, `integration`, `shared`, `security`
  - _Requisitos: 14.1, 15.1, 16.1_

- [x] 2. Configurar PostgreSQL, Flyway e JPA
  - Criar `docker-compose.yml` na raiz do projeto com serviço PostgreSQL 16 (porta 5432, base de dados `tms_dev`, utilizador `tms_dev`, password `tms_dev`) e volume persistente `postgres_data`
  - Criar `application.yml` com configuração de datasource, JPA (`ddl-auto: validate`), Flyway e logging estruturado JSON
  - Criar `application-dev.yml` com configuração local (PostgreSQL local via Docker Compose, Keycloak local)
  - Criar migration `V0__init_extensions.sql` com `CREATE EXTENSION IF NOT EXISTS "uuid-ossp"` e `CREATE EXTENSION IF NOT EXISTS "pg_trgm"`
  - Configurar `@EnableJpaAuditing` e implementar `AuditorAware<String>` que lê o utilizador do `SecurityContext`
  - _Requisitos: 14.5, 16.2_

- [x] 3. Implementar módulo `shared` — DTOs genéricos, exceções e utilitários
  - Criar `ApiResponse<T>` com campos `data` e `error` (formato `{ "data": ..., "error": null }`)
  - Criar `PagedResponse<T>` com campos `content`, `page`, `size`, `totalElements`, `totalPages`
  - Criar `BusinessException`, `AllocationException`, `ResourceNotFoundException` em `shared/exception`
  - Criar `GlobalExceptionHandler` com `@RestControllerAdvice` tratando `ResourceNotFoundException` (404), `BusinessException` (422), `AllocationException` (422), `MethodArgumentNotValidException` (400) e `Exception` genérica (500 com correlationId)
  - Criar todos os enums em `shared/enums`: `VehicleStatus`, `DriverStatus`, `ActivityStatus`, `ActivityPriority`, `DocumentStatus`, `VehicleDocumentType`, `DriverDocumentType`, `MaintenanceType`, `ChecklistItemStatus`, `AccessoryStatus`, `AccessoryType`, `AlertSeverity`, `AlertType`, `AuditOperation`
  - Criar `DateUtils` e `CodeGenerator` em `shared/util`
  - _Requisitos: 18.4, 18.5_

- [ ] 4. Configurar segurança — Keycloak JWT, RBAC e rate limiting
  - Implementar `KeycloakJwtAuthenticationConverter` que extrai roles de `realm_access.roles` e os mapeia para `ROLE_` prefixados
  - Implementar `SecurityUtils` com métodos estáticos `getCurrentUserId()`, `getCurrentIpAddress()` e `hasRole()`
  - Implementar `SecurityConfig` com `@EnableWebSecurity`, `@EnableMethodSecurity`, configuração stateless, `oauth2ResourceServer` com JWT, e CORS para origens `*.company.pt` e `localhost`
  - Implementar `RateLimitFilter` com Bucket4j (60 req/min por IP) aplicado a `/api/v1/integration/` e `/actuator/`
  - Criar `RateLimitConfig` com `@ConfigurationProperties(prefix = "tms.security.rate-limit")`
  - Configurar `spring.security.oauth2.resourceserver.jwt.issuer-uri` e `jwk-set-uri` no `application.yml`
  - _Requisitos: 15.1, 15.2, 15.3, 15.5_

- [ ] 5. Configurar Spring Boot Actuator e logging
  - Expor endpoints `health`, `info`, `metrics`, `prometheus` via `management.endpoints.web.exposure.include`
  - Configurar `management.endpoint.health.show-details: when-authorized`
  - Configurar logging estruturado JSON com padrão `{"timestamp":...,"level":...,"logger":...,"message":...}`
  - Verificar que `GET /actuator/health` retorna `{"status":"UP"}` com base de dados UP
  - _Requisitos: 16.1, 16.3, 16.4_

- [ ] 6. Checkpoint — Infraestrutura base funcional
  - Verificar que o projeto compila sem erros
  - Verificar que `GET /actuator/health` retorna HTTP 200 com status UP
  - Verificar que pedido sem JWT a `/api/v1/` retorna HTTP 401
  - Verificar que Flyway executa `V0__init_extensions.sql` sem erros
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 2 — Módulo Vehicle (Semanas 3–5)

- [ ] 7. Criar migrations Flyway para o módulo Vehicle
  - Criar `V1__create_vehicles.sql` com tabela `vehicles` (UUID PK, `plate` UNIQUE, `brand`, `model`, `vehicle_type`, `capacity`, `activity_location`, `activity_start_date`, `status` com CHECK, `current_driver_id` FK, `notes`, campos de auditoria, `deleted_at`, `deleted_by`)
  - Criar `V4__create_vehicle_documents.sql` com tabela `vehicle_documents` (UUID PK, `vehicle_id` FK, `document_type` com CHECK, `document_number`, `issue_date`, `expiry_date`, `issuing_entity`, `status` com CHECK, `notes`, `file_id` FK, campos de auditoria, soft delete)
  - Criar `V4b__create_vehicle_accessories.sql` com tabela `vehicle_accessories` (UUID PK, `vehicle_id` FK, `accessory_type` com CHECK, `status` com CHECK, `last_checked_at`, `last_checked_by`, UNIQUE em `vehicle_id + accessory_type`)
  - Criar `V5__create_maintenance_records.sql` com tabela `maintenance_records` (UUID PK, `vehicle_id` FK, `maintenance_type` com CHECK, `performed_at`, `mileage_at_service`, `description`, `supplier`, `total_cost`, `parts_replaced`, `next_maintenance_date`, `next_maintenance_mileage`, `responsible_user`, campos de auditoria)
  - Criar `V6__create_checklists.sql` com tabelas `checklist_templates`, `checklist_template_items`, `checklist_inspections`, `checklist_inspection_items` com todas as FKs e constraints
  - Criar `V9__create_files.sql` com tabela `files` (UUID PK, `original_filename`, `storage_key` UNIQUE, `content_type`, `size_bytes`, `uploaded_by`, `uploaded_at`)
  - Criar `V10__create_indexes.sql` com todos os índices: `idx_vehicles_plate`, `idx_vehicles_status`, `idx_vehicles_plate_trgm` (GIN com `gin_trgm_ops`), `idx_vehicle_docs_expiry`, `idx_vehicle_docs_status`, `idx_vehicle_accessories_vehicle`, `idx_maintenance_vehicle`, `idx_maintenance_next_date`, `idx_checklist_inspections_vehicle`
  - _Requisitos: 1.1, 1.5, 2.1, 3.1, 4.1, 5.1, 14.5_

- [ ] 8. Implementar entidades JPA do módulo Vehicle
  - Criar `Vehicle.java` com `@Entity`, `@Table(name="vehicles")`, `@SQLRestriction("deleted_at IS NULL")`, `@EntityListeners(AuditingEntityListener.class)`, todos os campos mapeados, relações `@OneToMany` para `VehicleDocument`, `VehicleAccessory`, `MaintenanceRecord`, e método `softDelete(String deletedBy)`
  - Criar `VehicleDocument.java` com `@Entity`, `@SQLRestriction("deleted_at IS NULL")`, relação `@ManyToOne` para `Vehicle` e `FileRecord`, método `softDelete()`
  - Criar `VehicleAccessory.java` com `@Entity`, todos os campos incluindo `lastCheckedAt` e `lastCheckedBy`
  - Criar `MaintenanceRecord.java` com `@Entity`, todos os campos incluindo `nextMaintenanceDate` e `nextMaintenanceMileage`
  - Criar `ChecklistTemplate.java`, `ChecklistTemplateItem.java` com `@Entity` e relações
  - Criar `ChecklistInspection.java` com `@Entity`, relações para `Vehicle`, `Activity` e `ChecklistTemplate`, e método `hasCriticalFailures()` que verifica itens críticos com status `AVARIA` ou `FALTA`
  - Criar `ChecklistInspectionItem.java` com `@Entity` e campo `isCritical`
  - Criar `FileRecord.java` com `@Entity` para a tabela `files`
  - _Requisitos: 1.1, 1.2, 2.1, 3.3, 4.2, 5.4_

- [ ] 9. Implementar repositórios do módulo Vehicle
  - Criar `VehicleRepository` com `JpaRepository<Vehicle, UUID>` e queries: `findByPlate(String plate)`, `findByPlateContainingIgnoreCase(String q, Pageable p)` (pesquisa parcial com `pg_trgm`), `existsByPlate(String plate)`, `findAllByFilters(status, location, Pageable)` com `@Query` JPQL
  - Criar `VehicleDocumentRepository` com queries: `findByVehicleId(UUID vehicleId)`, `findByVehicleIdAndStatus(UUID vehicleId, DocumentStatus status)`, `findByExpiryDateBetweenAndStatusNot(LocalDate from, LocalDate to, DocumentStatus status)`, `findByExpiryDateBeforeAndStatusNot(LocalDate date, DocumentStatus status)`
  - Criar `VehicleAccessoryRepository` com `findByVehicleId(UUID vehicleId)`
  - Criar `MaintenanceRepository` com queries: `findByVehicleId(UUID vehicleId, Pageable p)`, `findByNextMaintenanceDateBetween(LocalDate from, LocalDate to)`
  - Criar `ChecklistTemplateRepository` com `findByVehicleTypeAndIsActiveTrue(String vehicleType)`
  - Criar `ChecklistInspectionRepository` com `findLatestByVehicleId(UUID vehicleId)` retornando `Optional<ChecklistInspection>`
  - _Requisitos: 1.5, 3.5, 4.7, 5.7, 5.8, 14.5_

- [ ] 10. Implementar serviços do módulo Vehicle
  - Criar `VehicleService` com métodos: `createVehicle(VehicleCreateDto)` (valida matrícula única, lança `BusinessException` se duplicada), `updateVehicle(UUID, VehicleUpdateDto)`, `updateStatus(UUID, VehicleStatus)` (impede alocação se ABATIDA), `deleteVehicle(UUID)` (soft delete), `getVehicle(UUID)`, `listVehicles(filtros, Pageable)`, `searchByPlate(String q, Pageable)`, `getConsolidated(UUID)` — anotar métodos de escrita com `@Auditable`
  - Criar `VehicleDocumentService` com métodos: `addDocument(UUID vehicleId, VehicleDocumentDto)`, `updateDocument(UUID vehicleId, UUID docId, VehicleDocumentDto)`, `deleteDocument(UUID vehicleId, UUID docId)` (soft delete), `listDocuments(UUID vehicleId)` — validar tamanho máximo de ficheiro (10 MB)
  - Criar `MaintenanceService` com métodos: `registerMaintenance(UUID vehicleId, MaintenanceRecordDto)` (cria alerta automático se `nextMaintenanceDate` preenchido), `listMaintenance(UUID vehicleId, filtros, Pageable)`, `getMaintenance(UUID vehicleId, UUID maintenanceId)`
  - Criar `ChecklistService` com métodos: `submitChecklist(UUID vehicleId, ChecklistInspectionDto)` (valida itens críticos), `listChecklists(UUID vehicleId, filtros, Pageable)`, `getTemplate(UUID templateId)`, `createTemplate(ChecklistTemplateDto)`, `updateTemplate(UUID, ChecklistTemplateDto)`
  - _Requisitos: 1.3, 1.4, 1.7, 2.3, 2.4, 3.5, 3.8, 4.2, 4.3, 5.3, 5.4, 5.5_

- [ ] 11. Implementar módulo `integration` — FileStorage e porta RH
  - Criar interface `FileStoragePort` com métodos `upload(MultipartFile file): FileUploadResultDto` e `download(String storageKey): Resource`
  - Criar `S3FileStorageAdapter` com `@ConditionalOnProperty(name="tms.storage.type", havingValue="s3")` usando AWS SDK v2, validando content-type (PDF, JPG, PNG) e tamanho máximo (10 MB)
  - Criar `LocalFileStorageAdapter` com `@ConditionalOnProperty(name="tms.storage.type", havingValue="local")` para ambiente de desenvolvimento
  - Criar `FileStorageConfig` com `@ConfigurationProperties(prefix="tms.storage")`
  - Criar interface `RhIntegrationPort` com método `checkAvailability(UUID driverId, LocalDate startDate, LocalDate endDate): DriverAvailabilityDto`
  - Criar DTOs: `DriverAvailabilityDto`, `RhAbsenceDto`, `FileUploadResultDto`
  - _Requisitos: 3.9, 15.6, 13.1, 13.2_

- [ ] 12. Implementar controllers REST do módulo Vehicle
  - Criar `VehicleController` com endpoints: `POST /api/v1/vehicles` (`@PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")`), `GET /api/v1/vehicles` (ADMIN, GESTOR_FROTA, OPERADOR, AUDITOR), `GET /api/v1/vehicles/search` (todos os roles), `GET /api/v1/vehicles/{id}`, `GET /api/v1/vehicles/{id}/consolidated`, `PUT /api/v1/vehicles/{id}`, `PATCH /api/v1/vehicles/{id}/status`, `DELETE /api/v1/vehicles/{id}` (apenas ADMIN)
  - Criar `VehicleDocumentController` com endpoints: `GET /api/v1/vehicles/{id}/documents`, `POST /api/v1/vehicles/{id}/documents`, `PUT /api/v1/vehicles/{id}/documents/{docId}`, `DELETE /api/v1/vehicles/{id}/documents/{docId}`
  - Criar `MaintenanceController` com endpoints: `GET /api/v1/vehicles/{id}/maintenance`, `POST /api/v1/vehicles/{id}/maintenance`
  - Criar `ChecklistController` com endpoints: `GET /api/v1/vehicles/{id}/checklists`, `POST /api/v1/vehicles/{id}/checklists`, `GET /api/v1/checklist-templates`, `POST /api/v1/checklist-templates`, `PUT /api/v1/checklist-templates/{id}`
  - Criar endpoint `POST /api/v1/files` e `GET /api/v1/files/{id}` para upload/download
  - Todos os controllers devem retornar `ApiResponse<T>` e `PagedResponse<T>` com paginação (tamanho entre 10 e 100)
  - _Requisitos: 1.1, 1.5, 3.9, 4.1, 5.1, 10.1, 10.6, 14.4, 17.3_

- [ ] 13. Implementar mappers MapStruct do módulo Vehicle
  - Criar `VehicleMapper` com métodos `toResponseDto(Vehicle)`, `toEntity(VehicleCreateDto)`, `updateEntity(VehicleUpdateDto, @MappingTarget Vehicle)`
  - Criar `VehicleDocumentMapper`, `MaintenanceMapper`, `ChecklistMapper` com mapeamentos equivalentes
  - Criar `VehicleConsolidatedDto` e lógica de montagem no `VehicleService.getConsolidated()` agregando dados de documentos, acessórios, manutenções, checklists, atividades ativas e alertas ativos
  - _Requisitos: 10.2, 10.4, 10.5_

- [ ] 14. Implementar módulo `audit` — AOP, eventos e persistência
  - Criar anotação `@Auditable(entityType, operation)` em `audit/annotation`
  - Criar record `AuditEvent` com campos `entityType`, `entityId`, `operation`, `performedBy`, `ipAddress`, `previousValues`, `newValues`, `occurredAt` e factory method `AuditEvent.of(...)`
  - Criar `AuditAspect` com `@Around("@annotation(auditable)")` que captura estado anterior (para UPDATE/DELETE), executa o método, captura estado posterior, extrai `entityId` do resultado, e publica `AuditEvent` via `ApplicationEventPublisher`
  - Criar entidade `AuditLog` imutável (sem `@Setter`, sem `@LastModifiedDate`) com factory method `AuditLog.of(...)` e campos `previousValues`/`newValues` como `JSONB` com `@JdbcTypeCode(SqlTypes.JSON)`
  - Criar `AuditLogRepository` com queries: `findByFilters(entityType, operation, performedBy, from, to, Pageable)`
  - Criar `AuditService` com `@ApplicationModuleListener` que consome `AuditEvent` e persiste `AuditLog`
  - Criar `AuditController` com `GET /api/v1/audit` (filtros + paginação, apenas ADMIN e AUDITOR) e `GET /api/v1/audit/{id}`
  - _Requisitos: 12.1, 12.2, 12.3, 12.4, 12.6_

- [ ] 15. Escrever testes unitários do módulo Vehicle
  - [ ]\* 15.1 Escrever testes unitários do `VehicleService`
    - Testar criação com matrícula duplicada lança `BusinessException`
    - Testar soft delete define `deletedAt` e `deletedBy`
    - Testar alteração de estado para ABATIDA
    - _Requisitos: 1.3, 1.7_
  - [ ]\* 15.2 Escrever testes unitários do `ChecklistService`
    - Testar submissão de checklist com item crítico em AVARIA
    - Testar submissão de checklist sem falhas críticas
    - _Requisitos: 5.3, 5.5_
  - [ ]\* 15.3 Escrever testes de integração dos endpoints Vehicle
    - Testar `POST /api/v1/vehicles` retorna 201 com dados corretos
    - Testar `POST /api/v1/vehicles` com matrícula duplicada retorna 422
    - Testar `GET /api/v1/vehicles/search?q=AA` retorna resultados paginados
    - Testar `GET /api/v1/vehicles/{id}/consolidated` retorna estrutura completa
    - Usar Testcontainers com PostgreSQL real
    - _Requisitos: 1.3, 10.2, 10.6_

- [ ] 16. Checkpoint — Módulo Vehicle completo
  - Verificar que CRUD completo de viaturas funciona via API
  - Verificar que upload de ficheiro PDF/JPG/PNG com limite de 10 MB funciona
  - Verificar que pesquisa parcial por matrícula retorna resultados
  - Verificar que vista consolidada retorna todos os dados associados
  - Verificar que operações de escrita geram registos de auditoria
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 3 — Módulo Driver (Semanas 6–7)

- [ ] 17. Criar migrations Flyway para o módulo Driver
  - Criar `V2__create_drivers.sql` com tabela `drivers` (UUID PK, `full_name`, `phone`, `address`, `id_number` UNIQUE, `license_number` UNIQUE, `license_category`, `license_issue_date`, `license_expiry_date`, `activity_location`, `status` com CHECK, `notes`, campos de auditoria, soft delete)
  - Criar `V4c__create_driver_documents.sql` com tabela `driver_documents` (UUID PK, `driver_id` FK, `document_type` com CHECK, `document_number`, `issue_date`, `expiry_date`, `issuing_entity`, `category`, `status` com CHECK, `notes`, `file_id` FK, campos de auditoria, soft delete)
  - Adicionar a `V10__create_indexes.sql` (ou nova migration `V10b`) os índices: `idx_drivers_status`, `idx_drivers_license_expiry`, `idx_drivers_location`, `idx_driver_docs_driver`, `idx_driver_docs_expiry`
  - _Requisitos: 6.1, 6.4, 14.5_

- [ ] 18. Implementar entidades JPA do módulo Driver
  - Criar `Driver.java` com `@Entity`, `@Table(name="drivers")`, `@SQLRestriction("deleted_at IS NULL")`, `@EntityListeners(AuditingEntityListener.class)`, todos os campos mapeados incluindo `licenseExpiryDate`, relação `@OneToMany` para `DriverDocument`, e método `softDelete(String deletedBy)`
  - Criar `DriverDocument.java` com `@Entity`, `@SQLRestriction("deleted_at IS NULL")`, relação `@ManyToOne` para `Driver` e `FileRecord`, campo `category` para categoria da carta de condução
  - _Requisitos: 6.1, 6.4, 6.5_

- [ ] 19. Implementar repositórios do módulo Driver
  - Criar `DriverRepository` com queries: `findByIdNumber(String idNumber)`, `existsByIdNumber(String idNumber)`, `existsByLicenseNumber(String licenseNumber)`, `findAllByFilters(status, location, Pageable)` com `@Query` JPQL
  - Criar `DriverDocumentRepository` com queries: `findByDriverId(UUID driverId)`, `findByDriverIdAndDocumentType(UUID driverId, DriverDocumentType type)`, `findByExpiryDateBetweenAndStatusNot(LocalDate from, LocalDate to, DocumentStatus status)`, `findByExpiryDateBeforeAndStatusNot(LocalDate date, DocumentStatus status)`
  - _Requisitos: 6.6, 6.7, 14.5_

- [ ] 20. Implementar adaptadores de integração RH com cache Caffeine
  - Criar `RhRestAdapter` com `@ConditionalOnProperty(name="tms.integration.rh.mode", havingValue="rest")`, injetar `Cache<String, DriverAvailabilityDto>` Caffeine, implementar `checkAvailability()` com cache key `driverId:startDate:endDate`, chamada REST ao RH_Sistema com `RestTemplate`, tratamento de `RestClientException` lançando `RhIntegrationException`
  - Criar `RhModuleAdapter` com `@ConditionalOnProperty(name="tms.integration.rh.mode", havingValue="module")` como stub para desenvolvimento (retorna sempre disponível)
  - Criar `RhIntegrationConfig` com `@ConfigurationProperties(prefix="tms.integration.rh")`, bean `Cache<String, DriverAvailabilityDto>` Caffeine com `expireAfterWrite(cacheTtlMinutes, MINUTES)` e `maximumSize(1000)`, bean `RestTemplate` com timeouts configuráveis e interceptor de API key
  - Criar `RhIntegrationException` em `integration/exception`
  - _Requisitos: 7.1, 7.4, 13.1, 13.2, 13.3, 13.5_

- [ ] 21. Implementar serviços do módulo Driver
  - Criar `DriverService` com métodos: `createDriver(DriverCreateDto)` (valida unicidade de `idNumber` e `licenseNumber`), `updateDriver(UUID, DriverUpdateDto)`, `updateStatus(UUID, DriverStatus)`, `deleteDriver(UUID)` (soft delete), `getDriver(UUID)`, `listDrivers(filtros, Pageable)`, `getAvailability(UUID driverId)` (chama `RhIntegrationPort`, trata `RhIntegrationException`) — anotar métodos de escrita com `@Auditable`
  - Criar `DriverDocumentService` com métodos: `addDocument(UUID driverId, DriverDocumentDto)`, `updateDocument(UUID driverId, UUID docId, DriverDocumentDto)`, `listDocuments(UUID driverId)` — anotar com `@Auditable`
  - Implementar lógica de verificação de carta de condução expirada: quando `licenseExpiryDate < LocalDate.now()`, o estado do documento deve ser `EXPIRADO`
  - _Requisitos: 6.1, 6.2, 6.3, 6.6, 6.7, 6.8, 7.5_

- [ ] 22. Implementar controllers REST do módulo Driver
  - Criar `DriverController` com endpoints: `POST /api/v1/drivers` (ADMIN, GESTOR_FROTA), `GET /api/v1/drivers` (ADMIN, GESTOR_FROTA, OPERADOR, AUDITOR), `GET /api/v1/drivers/{id}`, `PUT /api/v1/drivers/{id}`, `PATCH /api/v1/drivers/{id}/status`, `DELETE /api/v1/drivers/{id}` (apenas ADMIN), `GET /api/v1/drivers/{id}/availability`
  - Criar `DriverDocumentController` com endpoints: `GET /api/v1/drivers/{id}/documents`, `POST /api/v1/drivers/{id}/documents`, `PUT /api/v1/drivers/{id}/documents/{docId}`
  - Criar endpoint `POST /api/v1/integration/rh/availability` com `@PreAuthorize("hasRole('RH_INTEGRADOR')")` para webhook de notificação de alteração de disponibilidade do RH
  - Todos os endpoints retornam `ApiResponse<T>` com paginação onde aplicável
  - _Requisitos: 6.1, 6.9, 7.1, 7.5, 13.6, 15.7_

- [ ] 23. Escrever testes unitários do módulo Driver
  - [ ]\* 23.1 Escrever testes unitários do `DriverService`
    - Testar criação com `idNumber` duplicado lança `BusinessException`
    - Testar `getAvailability()` quando RH retorna indisponível
    - Testar `getAvailability()` quando `RhIntegrationException` é lançada (fallback)
    - _Requisitos: 7.2, 7.3, 7.4_
  - [ ]\* 23.2 Escrever testes unitários do `RhRestAdapter`
    - Testar que segunda chamada com mesma chave usa cache (sem chamada HTTP)
    - Testar que `RhIntegrationException` é lançada quando REST falha
    - _Requisitos: 13.5_

- [ ] 24. Checkpoint — Módulo Driver completo
  - Verificar que CRUD completo de motoristas funciona via API
  - Verificar que consulta de disponibilidade retorna dados do RH (ou fallback com mensagem)
  - Verificar que cache de disponibilidade funciona (segunda chamada usa cache)
  - Verificar que webhook RH está protegido por role `RH_INTEGRADOR`
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 4 — Módulo Activity (Semanas 8–10)

- [ ] 25. Criar migrations Flyway para o módulo Activity
  - Criar `V3__create_activities.sql` com tabela `activities` (UUID PK, `code` UNIQUE, `title`, `activity_type`, `location`, `planned_start` TIMESTAMPTZ, `planned_end` TIMESTAMPTZ, `actual_start`, `actual_end`, `priority` com CHECK, `status` com CHECK, `vehicle_id` FK, `driver_id` FK, `description`, `notes`, `rh_override_justification`, campos de auditoria, soft delete)
  - Criar `V3b__create_activity_events.sql` com tabela `activity_events` (UUID PK, `activity_id` FK, `event_type`, `previous_status`, `new_status`, `performed_by`, `performed_at`, `notes`, `created_at`, `created_by`)
  - Adicionar índices: `idx_activities_status`, `idx_activities_vehicle`, `idx_activities_driver`, `idx_activities_planned_start`, `idx_activities_code`, `idx_activity_events_activity`
  - _Requisitos: 8.1, 8.6, 14.5_

- [ ] 26. Implementar entidades JPA do módulo Activity
  - Criar `Activity.java` com `@Entity`, `@Table(name="activities")`, `@SQLRestriction("deleted_at IS NULL")`, `@EntityListeners(AuditingEntityListener.class)`, todos os campos mapeados, relações `@ManyToOne` para `Vehicle` e `Driver` (LAZY), relação `@OneToMany` para `ActivityEvent` com `@OrderBy("performedAt ASC")`, e método `softDelete()`
  - Criar `ActivityEvent.java` com `@Entity`, campos `eventType`, `previousStatus`, `newStatus`, `performedBy`, `performedAt`, `notes`
  - Adicionar ao enum `ActivityStatus` o mapa `ALLOWED_TRANSITIONS` e métodos `canTransitionTo(ActivityStatus target)` e `validateTransition(ActivityStatus target)` que lança `BusinessException("INVALID_STATUS_TRANSITION", ...)` se inválida
  - _Requisitos: 8.1, 8.3, 8.4, 8.8_

- [ ] 27. Implementar `ActivityCodeGenerator`
  - Criar `ActivityCodeGenerator` em `activity/service` com método `generateActivityCode()` que obtém o ano corrente, conta atividades com código iniciado em `ACT-{ANO}-` via `activityRepository.countByCodeStartingWith(prefix)`, e retorna `ACT-{ANO}-{SEQUENCIAL:04d}`
  - Garantir que a geração é feita dentro de transação para evitar duplicados em concorrência
  - _Requisitos: 8.6_

- [ ] 28. Implementar repositórios do módulo Activity com pessimistic locking
  - Criar `ActivityRepository` com queries: `findByCode(String code)`, `countByCodeStartingWith(String prefix)`, `findAllByFilters(status, vehicleId, driverId, from, to, Pageable)` com `@Query` JPQL
  - Adicionar queries com `@Lock(LockModeType.PESSIMISTIC_WRITE)`: `findConflictingActivitiesForVehicle(UUID vehicleId, OffsetDateTime start, OffsetDateTime end, UUID excludeActivityId)` e `findConflictingActivitiesForDriver(UUID driverId, OffsetDateTime start, OffsetDateTime end, UUID excludeActivityId)` — ambas filtram por status `PLANEADA` ou `EM_CURSO`, `deletedAt IS NULL`, e sobreposição de período (`plannedStart < end AND plannedEnd > start`)
  - Criar `ActivityEventRepository` com `findByActivityIdOrderByPerformedAtAsc(UUID activityId)`
  - _Requisitos: 8.8, 9.6, 9.7_

- [ ] 29. Implementar `AllocationValidationService` com os 8 passos de validação
  - Criar `AllocationValidationService` em `activity/service` com método `validate(UUID activityId, UUID vehicleId, UUID driverId, OffsetDateTime plannedStart, OffsetDateTime plannedEnd, String rhOverrideJustification): AllocationResultDto`
  - Passo 1: verificar estado da viatura (EM_MANUTENCAO → `VEHICLE_IN_MAINTENANCE`, INDISPONIVEL → `VEHICLE_UNAVAILABLE`, ABATIDA → `VEHICLE_DECOMMISSIONED`)
  - Passo 2: verificar documentos da viatura com status `EXPIRADO` via `vehicleDocumentRepository.findByVehicleIdAndStatus(vehicleId, EXPIRADO)` — acumular um bloqueio por documento
  - Passo 3: verificar última checklist da viatura via `checklistInspectionRepository.findLatestByVehicleId(vehicleId)` — bloquear se `hasCriticalFailures()` retornar true
  - Passo 4: verificar estado do motorista (INATIVO → `DRIVER_INACTIVE`, SUSPENSO → `DRIVER_SUSPENDED`)
  - Passo 5: verificar carta de condução expirada via `driverDocumentRepository.findByDriverIdAndDocumentType(driverId, CARTA_CONDUCAO)` — bloquear se status `EXPIRADO`
  - Passo 6: chamar `rhIntegrationPort.checkAvailability()` — se indisponível e sem `rhOverrideJustification`, bloquear com `DRIVER_RH_UNAVAILABLE`; se `RhIntegrationException` e sem justificação, bloquear com `RH_SYSTEM_UNAVAILABLE`; se justificação fornecida, registar na auditoria e não bloquear
  - Passo 7: chamar `activityRepository.findConflictingActivitiesForVehicle()` com pessimistic lock — bloquear por cada conflito encontrado
  - Passo 8: chamar `activityRepository.findConflictingActivitiesForDriver()` com pessimistic lock — bloquear por cada conflito encontrado
  - Retornar `AllocationResultDto` com `allocated = blockers.isEmpty()` e lista completa de bloqueios
  - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 7.1, 7.2, 7.3, 7.4, 7.6_

- [ ] 30. Implementar `ActivityService` com ciclo de vida e alocação
  - Criar `ActivityService` com métodos:
    - `createActivity(ActivityCreateDto)`: gera código via `ActivityCodeGenerator`, persiste com status `PLANEADA`, publica `AuditEvent`
    - `updateActivity(UUID, ActivityUpdateDto)`: atualiza campos opcionais, publica `AuditEvent`
    - `deleteActivity(UUID)`: soft delete, publica `AuditEvent`
    - `getActivity(UUID)`: retorna `ActivityResponseDto`
    - `listActivities(filtros, Pageable)`: retorna página paginada
    - `allocate(UUID activityId, AllocationRequestDto)`: chama `AllocationValidationService.validate()`, se `allocated=true` persiste `vehicleId` e `driverId` na atividade, regista `ActivityEvent` de alocação, publica `AuditEvent`; se `allocated=false` lança `AllocationException` com lista de bloqueios
    - `transitionStatus(UUID activityId, StatusTransitionDto)`: valida transição via `currentStatus.validateTransition(newStatus)`, ao iniciar (`PLANEADA → EM_CURSO`) verifica checklist crítica, atualiza `actualStart`/`actualEnd` conforme estado, regista `ActivityEvent`, publica `AuditEvent`
    - `getEvents(UUID activityId)`: retorna lista de `ActivityEventDto`
  - Anotar `createActivity`, `updateActivity`, `deleteActivity`, `allocate`, `transitionStatus` com `@Auditable`
  - _Requisitos: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_

- [ ] 31. Implementar `ActivityController`
  - Criar `ActivityController` com endpoints: `POST /api/v1/activities` (ADMIN, GESTOR_FROTA, OPERADOR), `GET /api/v1/activities` (ADMIN, GESTOR_FROTA, OPERADOR, AUDITOR), `GET /api/v1/activities/{id}` (ADMIN, GESTOR_FROTA, OPERADOR, MOTORISTA com verificação de motorista atribuído, AUDITOR), `PUT /api/v1/activities/{id}`, `DELETE /api/v1/activities/{id}` (ADMIN, GESTOR_FROTA), `POST /api/v1/activities/{id}/allocate` (ADMIN, GESTOR_FROTA, OPERADOR), `PATCH /api/v1/activities/{id}/status` (ADMIN, GESTOR_FROTA, OPERADOR, MOTORISTA), `GET /api/v1/activities/{id}/events` (ADMIN, GESTOR_FROTA, AUDITOR)
  - Para o endpoint `GET /api/v1/activities/{id}` com perfil MOTORISTA, usar `@PreAuthorize` com SpEL: `hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR') or (hasRole('MOTORISTA') and @activitySecurityService.isAssignedDriver(#id))`
  - Criar `ActivitySecurityService` com método `isAssignedDriver(UUID activityId)` que verifica se o utilizador autenticado é o motorista atribuído à atividade
  - _Requisitos: 8.1, 8.3, 8.4, 9.8, 17.6_

- [ ] 32. Escrever testes unitários do módulo Activity
  - [ ]\* 32.1 Escrever testes unitários do `AllocationValidationService`
    - Testar Passo 1: viatura em manutenção gera bloqueio `VEHICLE_IN_MAINTENANCE`
    - Testar Passo 1: viatura ABATIDA gera bloqueio `VEHICLE_DECOMMISSIONED`
    - Testar Passo 2: documento expirado gera bloqueio `VEHICLE_DOCUMENT_EXPIRED`
    - Testar Passo 3: checklist com falha crítica gera bloqueio `CHECKLIST_CRITICAL_FAILURE`
    - Testar Passo 4: motorista SUSPENSO gera bloqueio `DRIVER_SUSPENDED`
    - Testar Passo 5: carta de condução expirada gera bloqueio `DRIVER_LICENSE_EXPIRED`
    - Testar Passo 6: RH indisponível sem justificação gera bloqueio `DRIVER_RH_UNAVAILABLE`
    - Testar Passo 6: `RhIntegrationException` sem justificação gera bloqueio `RH_SYSTEM_UNAVAILABLE`
    - Testar Passo 6: `RhIntegrationException` com justificação não bloqueia
    - Testar Passo 7: conflito de viatura gera bloqueio `VEHICLE_ALLOCATION_CONFLICT`
    - Testar Passo 8: conflito de motorista gera bloqueio `DRIVER_ALLOCATION_CONFLICT`
    - Testar alocação válida retorna `AllocationResultDto` com `allocated=true` e lista vazia de bloqueios
    - _Requisitos: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_
  - [ ]\* 32.2 Escrever testes unitários do `ActivityService` — transições de estado
    - Testar transição válida `PLANEADA → EM_CURSO` atualiza `actualStart`
    - Testar transição válida `EM_CURSO → CONCLUIDA` atualiza `actualEnd`
    - Testar transição inválida `CONCLUIDA → EM_CURSO` lança `BusinessException`
    - Testar transição inválida `CANCELADA → PLANEADA` lança `BusinessException`
    - _Requisitos: 8.3, 8.4_
  - [ ]\* 32.3 Escrever testes de integração do módulo Activity
    - Testar `POST /api/v1/activities` gera código `ACT-{ANO}-{SEQ}` único
    - Testar `POST /api/v1/activities/{id}/allocate` com viatura em manutenção retorna 422 com bloqueio
    - Testar `PATCH /api/v1/activities/{id}/status` com transição inválida retorna 422
    - _Requisitos: 8.6, 9.2, 8.4_

- [ ] 33. Checkpoint — Módulo Activity completo
  - Verificar que criação de atividade gera código `ACT-{ANO}-{SEQ}` único
  - Verificar que alocação com viatura em manutenção é bloqueada com mensagem clara
  - Verificar que conflito de alocação simultânea é detetado e bloqueado
  - Verificar que transição inválida de estado retorna HTTP 422
  - Verificar que transição `PLANEADA → EM_CURSO` verifica checklist crítica
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 5 — Módulo Alert (Semana 11)

- [ ] 34. Criar migrations Flyway para o módulo Alert
  - Criar `V7__create_alerts.sql` com tabela `alerts` (UUID PK, `alert_type` com CHECK, `severity` com CHECK, `entity_type`, `entity_id`, `title`, `message`, `is_resolved` DEFAULT FALSE, `resolved_at`, `resolved_by`, campos de auditoria)
  - Adicionar índice único de deduplicação: `CREATE UNIQUE INDEX idx_alerts_dedup ON alerts(alert_type, entity_id) WHERE is_resolved = FALSE`
  - Adicionar índices: `idx_alerts_entity`, `idx_alerts_is_resolved`, `idx_alerts_severity`
  - Criar tabela `alert_configurations` com `alert_type`, `entity_type`, `days_before_warning` DEFAULT 30, `days_before_critical` DEFAULT 7, `is_active`, UNIQUE em `(alert_type, entity_type)`
  - Inserir dados iniciais de configuração padrão em `alert_configurations` para tipos `DOCUMENT_EXPIRING` (VEHICLE_DOCUMENT e DRIVER_DOCUMENT) e `MAINTENANCE_DUE` (MAINTENANCE_RECORD)
  - _Requisitos: 11.1, 11.5, 11.7, 11.9_

- [ ] 35. Implementar entidades JPA do módulo Alert
  - Criar `Alert.java` com `@Entity`, `@EntityListeners(AuditingEntityListener.class)`, todos os campos mapeados, e método `resolve(String resolvedBy)` que define `resolved=true`, `resolvedAt=OffsetDateTime.now()`, `resolvedBy`
  - Criar `AlertConfiguration.java` com `@Entity`, campos `alertType`, `entityType`, `daysBeforeWarning`, `daysBeforeCritical`, `isActive`, e factory method estático `AlertConfiguration.defaults(AlertType, String)` com valores padrão
  - _Requisitos: 11.5, 11.7_

- [ ] 36. Implementar repositórios do módulo Alert
  - Criar `AlertRepository` com queries: `findByIsResolvedFalse(Pageable p)`, `findByIsResolvedFalseAndSeverity(AlertSeverity severity, Pageable p)`, `existsByAlertTypeAndEntityIdAndResolvedFalse(AlertType type, UUID entityId)`, `findByAlertTypeAndEntityIdAndResolvedFalse(AlertType type, UUID entityId): Optional<Alert>`, `findByAlertTypeInAndResolvedFalse(List<AlertType> types): List<Alert>`
  - Criar `AlertConfigurationRepository` com `findByAlertTypeAndEntityType(AlertType type, String entityType): Optional<AlertConfiguration>`
  - _Requisitos: 11.1, 11.8, 11.9_

- [ ] 37. Implementar `AlertService` com lógica de geração e deduplicação
  - Criar `AlertService` com método `checkDocumentExpiry()`:
    - Carregar `AlertConfiguration` para `DOCUMENT_EXPIRING/VEHICLE_DOCUMENT` e `DOCUMENT_EXPIRING/DRIVER_DOCUMENT` (ou usar defaults)
    - Buscar documentos de viatura com `expiryDate` entre hoje e `today + daysBeforeWarning` via `vehicleDocumentRepository.findByExpiryDateBetweenAndStatusNot()`
    - Para cada documento, determinar severidade (CRITICO se dentro de `daysBeforeCritical`, AVISO caso contrário) e chamar `createAlertIfNotExists()`
    - Buscar documentos de viatura com `expiryDate < today` via `vehicleDocumentRepository.findByExpiryDateBeforeAndStatusNot()`, atualizar status para `EXPIRADO` e criar alerta `DOCUMENT_EXPIRED` com severidade CRITICO
    - Repetir lógica análoga para documentos de motorista
  - Criar método `checkMaintenanceDue()`:
    - Carregar `AlertConfiguration` para `MAINTENANCE_DUE/MAINTENANCE_RECORD`
    - Buscar manutenções com `nextMaintenanceDate` entre hoje e `today + daysBeforeWarning`
    - Criar alertas `MAINTENANCE_DUE` (AVISO) ou `MAINTENANCE_OVERDUE` (CRITICO se data já passou)
  - Criar método privado `createAlertIfNotExists(AlertType, entityType, entityId, severity, title, message)`:
    - Verificar via `alertRepository.existsByAlertTypeAndEntityIdAndResolvedFalse()` se já existe alerta não resolvido
    - Se não existe, criar e persistir novo `Alert`
    - Se existe, atualizar severidade se escalou (ordinal maior)
  - Criar método `resolveObsoleteAlerts()` que resolve automaticamente alertas de documentos que já não estão expirados/a expirar
  - _Requisitos: 11.1, 11.2, 11.3, 11.4, 11.8_

- [ ] 38. Implementar `AlertScheduler` com job diário
  - Criar `AlertScheduler` com `@Component`, `@RequiredArgsConstructor`, `@Slf4j`
  - Implementar método `runDailyAlertCheck()` anotado com `@Scheduled(cron = "${tms.scheduling.alert-check-cron:0 0 6 * * *}")` e `@Transactional`
  - O método deve chamar em sequência: `alertService.checkDocumentExpiry()`, `alertService.checkMaintenanceDue()`, `alertService.resolveObsoleteAlerts()`
  - Adicionar logging de início e fim com `log.info()`
  - Garantir que `@EnableScheduling` está ativo na aplicação principal
  - _Requisitos: 11.1, 11.2, 11.3_

- [ ] 39. Implementar `AlertResolutionService` e controllers do módulo Alert
  - Criar `AlertResolutionService` com método `resolveManually(UUID alertId, String resolvedBy)` que carrega o alerta, chama `alert.resolve(resolvedBy)` e persiste
  - Criar `AlertController` com endpoints: `GET /api/v1/alerts` com filtros `resolved`, `severity`, `entityType` e paginação (ADMIN, GESTOR_FROTA, OPERADOR, AUDITOR), `GET /api/v1/alerts/{id}` (ADMIN, GESTOR_FROTA, AUDITOR), `PATCH /api/v1/alerts/{id}/resolve` (ADMIN, GESTOR_FROTA)
  - Criar `AlertConfigurationController` com endpoints: `GET /api/v1/alert-configurations` (ADMIN, GESTOR_FROTA), `PUT /api/v1/alert-configurations/{id}` (ADMIN, GESTOR_FROTA)
  - _Requisitos: 11.6, 11.7, 11.8, 11.9_

- [ ] 40. Escrever testes unitários do módulo Alert
  - [ ]\* 40.1 Escrever testes unitários do `AlertService`
    - Testar `checkDocumentExpiry()` cria alerta AVISO para documento a expirar em 20 dias (dentro do período de 30 dias)
    - Testar `checkDocumentExpiry()` cria alerta CRITICO para documento a expirar em 5 dias (dentro do período de 7 dias)
    - Testar `checkDocumentExpiry()` atualiza status do documento para EXPIRADO quando `expiryDate < today`
    - Testar `createAlertIfNotExists()` não cria alerta duplicado para mesma entidade+tipo não resolvido
    - Testar `createAlertIfNotExists()` atualiza severidade quando alerta existente tem severidade menor
    - Testar `checkMaintenanceDue()` cria alerta MAINTENANCE_OVERDUE quando `nextMaintenanceDate` já passou
    - _Requisitos: 11.1, 11.2, 11.3, 11.8_

- [ ] 41. Checkpoint — Módulo Alert completo
  - Verificar que job corre às 06:00 e gera alertas para documentos a expirar
  - Verificar que alertas duplicados não são criados (deduplicação funciona)
  - Verificar que quando documento é renovado, alerta é resolvido automaticamente
  - Verificar que configuração de dias de antecedência é respeitada
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 6 — Módulo Audit (Semana 12)

- [ ] 42. Criar migration Flyway para o módulo Audit
  - Criar `V8__create_audit_logs.sql` com tabela `audit_logs` (UUID PK, `entity_type`, `entity_id`, `operation` com CHECK, `performed_by`, `performed_at`, `ip_address`, `previous_values` JSONB, `new_values` JSONB, `created_at` — **sem** `updated_at` e **sem** `deleted_at` — registo imutável)
  - Adicionar índices: `idx_audit_entity` em `(entity_type, entity_id)`, `idx_audit_performed_by`, `idx_audit_performed_at`, `idx_audit_operation`
  - _Requisitos: 12.1, 12.2, 12.3, 12.5_

- [ ] 43. Verificar e completar a implementação do módulo Audit
  - Confirmar que `AuditLog.java` não tem `@Setter` nem `@LastModifiedDate` (entidade imutável)
  - Confirmar que `AuditLog` só pode ser criado via factory method `AuditLog.of(...)`
  - Confirmar que `AuditLogRepository` não expõe métodos `save()` com entidade modificada (apenas `save()` para criação via `AuditService`)
  - Confirmar que `AuditAspect` está a capturar corretamente estado anterior e posterior para operações UPDATE e DELETE
  - Confirmar que `AuditService` usa `@ApplicationModuleListener` para consumir `AuditEvent` de forma assíncrona e desacoplada
  - Verificar que `@Auditable` está aplicado em todos os métodos de escrita de `VehicleService`, `DriverService`, `ActivityService`
  - _Requisitos: 12.1, 12.2, 12.3_

- [ ] 44. Implementar `AuditController` com interface de consulta
  - Criar `AuditController` com endpoint `GET /api/v1/audit` com parâmetros de filtro: `entityType` (String), `entityId` (UUID), `operation` (AuditOperation), `performedBy` (String), `from` (LocalDate), `to` (LocalDate), e paginação — apenas ADMIN e AUDITOR
  - Criar endpoint `GET /api/v1/audit/{id}` para detalhe de registo — apenas ADMIN e AUDITOR
  - Garantir que **não existe** nenhum endpoint `POST`, `PUT`, `PATCH` ou `DELETE` em `/api/v1/audit` (imutabilidade garantida ao nível da API)
  - Criar `AuditQueryDto` com os campos de filtro e `AuditLogResponseDto` com todos os campos do `AuditLog`
  - _Requisitos: 12.3, 12.4_

- [ ] 45. Escrever testes de integração do módulo Audit
  - [ ]\* 45.1 Escrever testes de integração de auditoria
    - Testar que `POST /api/v1/vehicles` gera registo de auditoria com `operation = CRIACAO` e `entityType = VEHICLE`
    - Testar que `PUT /api/v1/vehicles/{id}` gera registo com `operation = ATUALIZACAO` e `previousValues`/`newValues` corretos
    - Testar que `PATCH /api/v1/activities/{id}/status` gera registo com estado anterior e novo
    - Testar que `GET /api/v1/audit` com filtro `entityType=VEHICLE` retorna apenas registos de viaturas
    - Testar que não existe endpoint para alterar ou eliminar registos de auditoria (HTTP 405 ou 404)
    - _Requisitos: 12.1, 12.2, 12.3, 12.4_

- [ ] 46. Checkpoint — Módulo Audit completo
  - Verificar que criação de viatura gera registo de auditoria com `operation = CRIACAO`
  - Verificar que atualização de estado de atividade gera registo com valores anteriores e novos
  - Verificar que nenhum endpoint permite alterar ou eliminar registos de auditoria
  - Verificar que consulta de auditoria com filtros retorna resultados paginados
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 7 — Frontend Web Next.js (Semanas 13–16)

- [ ] 47. Configurar projeto Next.js com autenticação Keycloak
  - Inicializar projeto Next.js com TypeScript, TailwindCSS e shadcn/ui
  - Instalar dependências: `next-auth`, `axios`, `@reduxjs/toolkit`, `react-redux`, `go_router` (ou `next/navigation`)
  - Configurar `NextAuth` com provider Keycloak (clientId, clientSecret, issuer) em `src/app/api/auth/[...nextauth]/route.ts`
  - Criar `src/lib/auth/keycloak.ts` com configuração do provider
  - Criar `src/app/layout.tsx` com `SessionProvider` e `ReduxProvider`
  - Criar página de login `src/app/(auth)/login/page.tsx` que redireciona para Keycloak
  - Criar middleware `src/middleware.ts` que protege todas as rotas exceto `/login` e `/api/auth`
  - _Requisitos: 17.1, 15.1_

- [ ] 48. Implementar cliente API tipado e tipos TypeScript
  - Criar `src/lib/api/client.ts` com instância Axios, interceptor de request que injeta JWT do `getSession()`, e interceptor de response que redireciona para `/login` em caso de 401
  - Criar `src/types/vehicle.ts` com interfaces: `Vehicle`, `VehicleDocument`, `VehicleAccessory`, `MaintenanceRecord`, `VehicleConsolidated`, `VehicleCreateInput`, tipos de enum `VehicleStatus`, `DocumentStatus`
  - Criar `src/types/driver.ts` com interfaces: `Driver`, `DriverDocument`, `DriverAvailability`, `DriverCreateInput`
  - Criar `src/types/activity.ts` com interfaces: `Activity`, `ActivityEvent`, `AllocationRequest`, `AllocationResult`, `StatusTransitionRequest`, tipos de enum `ActivityStatus`, `ActivityPriority`
  - Criar `src/types/alert.ts` com interfaces: `Alert`, `AlertConfiguration`, tipos de enum `AlertSeverity`, `AlertType`
  - Criar módulos de API: `src/lib/api/vehicles.ts`, `drivers.ts`, `activities.ts`, `alerts.ts`, `audit.ts` com funções tipadas para cada endpoint
  - _Requisitos: 18.4, 18.5_

- [ ] 49. Implementar componentes partilhados (shadcn/ui)
  - Criar `src/components/shared/PageHeader.tsx` com título, subtítulo e slot para ações
  - Criar `src/components/shared/DataTable.tsx` com suporte a paginação, ordenação e filtros, usando `@tanstack/react-table`
  - Criar `src/components/shared/StatusBadge.tsx` que mapeia `VehicleStatus`, `ActivityStatus`, `DocumentStatus` para cores de badge shadcn/ui
  - Criar `src/components/shared/AlertBanner.tsx` que apresenta alerta com ícone e cor por severidade (INFO=azul, AVISO=amarelo, CRITICO=vermelho)
  - Criar `src/components/shared/ConfirmDialog.tsx` com Dialog shadcn/ui para confirmação de ações destrutivas
  - Criar `src/components/shared/Timeline.tsx` para histórico de eventos de atividade
  - Configurar `src/store/alertsSlice.ts` com Redux Toolkit para contagem global de alertas críticos e de aviso
  - _Requisitos: 17.2, 17.5_

- [ ] 50. Implementar Dashboard
  - Criar `src/app/dashboard/page.tsx` com Server Component que carrega dados iniciais
  - Implementar cards de KPI: total de alertas críticos, atividades em curso, viaturas indisponíveis, viaturas em manutenção
  - Implementar lista de alertas ativos agrupados por severidade (CRITICO primeiro) com link para detalhe
  - Implementar lista de atividades em curso com estado e viatura/motorista atribuídos
  - Implementar navegação lateral com links para todos os módulos: Viaturas, Motoristas, Atividades, Manutenções, Alertas, Auditoria, Configurações
  - _Requisitos: 17.2, 17.3, 11.6_

- [ ] 51. Implementar módulo de Viaturas no frontend
  - Criar `src/app/vehicles/page.tsx` com listagem paginada de viaturas, filtros por estado e local, e botão de criação
  - Criar `src/features/vehicles/VehicleTable.tsx` com `DataTable` e colunas: matrícula, marca/modelo, tipo, estado (StatusBadge), local, ações
  - Criar `src/app/vehicles/new/page.tsx` com formulário de criação usando `react-hook-form` e validação Zod
  - Criar `src/features/vehicles/VehicleForm.tsx` com todos os campos obrigatórios e opcionais
  - Criar `src/app/vehicles/[id]/page.tsx` com vista consolidada usando `VehicleConsolidatedView`
  - Criar `src/features/vehicles/VehicleConsolidatedView.tsx` com alertas ativos no topo, dados cadastrais, e Tabs para Documentos, Acessórios, Manutenção, Checklists, Atividades
  - Criar `src/features/vehicles/DocumentsSection.tsx` com listagem de documentos, estados com badge, e formulário de adição/edição com upload de ficheiro
  - Criar `src/features/vehicles/AccessoriesSection.tsx` com listagem de acessórios e formulário de atualização de estado
  - Criar `src/features/vehicles/MaintenanceSection.tsx` com histórico de manutenções e formulário de registo
  - Criar `src/features/vehicles/ChecklistSection.tsx` com histórico de checklists e formulário de submissão
  - Implementar pesquisa por matrícula com autocomplete a partir de 3 caracteres usando `GET /api/v1/vehicles/search`
  - _Requisitos: 1.1, 1.5, 2.1, 3.1, 4.1, 5.1, 10.1, 10.2, 10.4, 10.5, 10.6, 17.3, 17.5_

- [ ] 52. Implementar módulo de Motoristas no frontend
  - Criar `src/app/drivers/page.tsx` com listagem paginada de motoristas e filtros
  - Criar `src/features/drivers/DriverTable.tsx` com colunas: nome, número de carta, categoria, estado, local, validade da carta
  - Criar `src/app/drivers/new/page.tsx` com formulário de criação
  - Criar `src/features/drivers/DriverForm.tsx` com todos os campos obrigatórios
  - Criar `src/app/drivers/[id]/page.tsx` com detalhe do motorista incluindo documentos e disponibilidade RH
  - Criar `src/features/drivers/AvailabilityBadge.tsx` que apresenta estado de disponibilidade do RH com fonte e data/hora da última sincronização
  - _Requisitos: 6.1, 6.4, 7.5, 17.3_

- [ ] 53. Implementar módulo de Atividades no frontend
  - Criar `src/app/activities/page.tsx` com listagem paginada de atividades, filtros por estado e prioridade
  - Criar `src/features/activities/ActivityTable.tsx` com colunas: código, título, tipo, estado (StatusBadge), prioridade, viatura, motorista, datas
  - Criar `src/app/activities/new/page.tsx` com formulário de criação de atividade
  - Criar `src/features/activities/ActivityForm.tsx` com campos obrigatórios e opcionais
  - Criar `src/app/activities/[id]/page.tsx` com detalhe da atividade, painel de alocação e histórico de eventos
  - Criar `src/features/activities/AllocationPanel.tsx` com seleção de viatura e motorista, chamada a `POST /api/v1/activities/{id}/allocate`, e apresentação de bloqueios de alocação em lista clara
  - Criar `src/features/activities/StatusTransitionButton.tsx` com botões de transição de estado disponíveis para o estado atual, com `ConfirmDialog` para transições irreversíveis (CONCLUIDA, CANCELADA)
  - _Requisitos: 8.1, 8.3, 9.8, 17.3, 17.5_

- [ ] 54. Implementar módulo de Alertas, Auditoria e Configurações no frontend
  - Criar `src/app/alerts/page.tsx` com listagem de alertas ativos agrupados por severidade, filtros e botão de resolução manual
  - Criar `src/features/alerts/AlertList.tsx` com cards de alerta por severidade
  - Criar `src/features/alerts/AlertSeverityBadge.tsx` com cores por severidade
  - Criar `src/app/audit/page.tsx` com tabela de registos de auditoria, filtros por entidade, operação, utilizador e período
  - Criar `src/app/settings/page.tsx` com configuração de períodos de alerta por tipo e gestão de templates de checklist
  - _Requisitos: 11.6, 11.7, 12.4, 17.3_

- [ ] 55. Implementar interface simplificada para perfil MOTORISTA
  - Criar layout condicional em `src/app/layout.tsx` que detecta role `MOTORISTA` e apresenta navegação simplificada (apenas Atividades e Checklists)
  - Garantir que páginas de gestão (Viaturas, Motoristas, Auditoria, Configurações) redirecionam para 403 quando acedidas por MOTORISTA
  - _Requisitos: 17.6_

- [ ] 56. Escrever testes do frontend
  - [ ]\* 56.1 Escrever testes de componentes React
    - Testar `StatusBadge` renderiza cor correta para cada estado
    - Testar `AllocationPanel` apresenta lista de bloqueios quando alocação falha
    - Testar `StatusTransitionButton` apresenta apenas transições válidas para o estado atual
    - _Requisitos: 8.3, 9.8_
  - [ ]\* 56.2 Escrever testes E2E com Playwright
    - Testar fluxo de criação de viatura e verificação na listagem
    - Testar pesquisa por matrícula retorna sugestões a partir de 3 caracteres
    - Testar fluxo de criação de atividade, alocação e transição de estado
    - _Requisitos: 10.6, 8.1, 8.3_

- [ ] 57. Checkpoint — Frontend Web completo
  - Verificar que interface é responsiva em ecrãs >= 1024px
  - Verificar que feedback visual (toast) aparece em todas as operações de criação, atualização e erro
  - Verificar que pesquisa por matrícula retorna sugestões a partir de 3 caracteres
  - Verificar que interface simplificada para perfil MOTORISTA está funcional
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

### Fase 8 — App Flutter (Semanas 17–19)

- [ ] 58. Configurar projeto Flutter com autenticação Keycloak
  - Inicializar projeto Flutter com estrutura de pastas: `lib/core/`, `lib/features/`, `lib/shared/`
  - Configurar `pubspec.yaml` com dependências: `dio`, `flutter_riverpod`, `riverpod_annotation`, `flutter_appauth`, `flutter_secure_storage`, `json_annotation`, `freezed_annotation`, `go_router`
  - Criar `lib/core/config/app_config.dart` com constantes: `apiBaseUrl`, `keycloakIssuer`, `keycloakClientId`, `keycloakRedirectUri`
  - Criar `lib/core/auth/keycloak_service.dart` com métodos `login()` (usando `FlutterAppAuth.authorizeAndExchangeCode()`), `refreshToken()`, `logout()`, e armazenamento seguro de tokens via `FlutterSecureStorage`
  - Criar `lib/core/auth/auth_provider.dart` com Riverpod `StateNotifierProvider` que expõe `AuthState` (accessToken, isAuthenticated) e métodos `login()`, `logout()`, `refreshToken()`
  - Criar `lib/features/auth/login_screen.dart` com botão de login que chama `keycloakService.login()`
  - Configurar `go_router` com redirecionamento para `/login` quando não autenticado
  - _Requisitos: 18.1, 18.2, 15.1_

- [ ] 59. Implementar cliente API Dio com interceptor JWT
  - Criar `lib/core/api/api_client.dart` com instância `Dio`, interceptor de request que injeta `Authorization: Bearer {accessToken}` do `authProvider`, e interceptor de erro que tenta `refreshToken()` em caso de 401 e repete o pedido; se falhar, chama `authProvider.logout()`
  - Criar `lib/core/api/api_response.dart` com classe `ApiResponse<T>` usando `freezed` e `json_serializable`, com campos `data` e `error`
  - Criar `lib/core/error/app_exception.dart` com exceções tipadas: `ApiException`, `NetworkException`, `AuthException`
  - _Requisitos: 18.1, 18.4, 18.5_

- [ ] 60. Implementar modelos de dados Flutter com serialização JSON
  - Criar `lib/shared/models/activity_model.dart` com classe `Activity` usando `@freezed` e `@JsonSerializable`, campos: `id`, `code`, `title`, `activityType`, `location`, `plannedStart`, `plannedEnd`, `priority`, `status`, `vehicle`, `driver`
  - Criar `lib/shared/models/vehicle_model.dart` com classe `Vehicle` usando `@freezed`, campos: `id`, `plate`, `brand`, `model`, `vehicleType`, `status`, `activityLocation`, `currentDriver`
  - Criar `lib/shared/models/alert_model.dart` com classe `Alert` usando `@freezed`, campos: `id`, `alertType`, `severity`, `title`, `message`, `isResolved`, `createdAt`
  - Criar `lib/shared/models/checklist_model.dart` com classes `ChecklistTemplate`, `ChecklistInspection`, `ChecklistInspectionItem` usando `@freezed`
  - Executar `flutter pub run build_runner build` para gerar código de serialização
  - _Requisitos: 18.2, 18.4_

- [ ] 61. Implementar ecrã de Atividades
  - Criar `lib/features/activities/activities_provider.dart` com Riverpod `AsyncNotifierProvider` que carrega atividades do utilizador autenticado via `GET /api/v1/activities` (filtrado pelo `driverId` do token JWT)
  - Criar `lib/features/activities/activities_screen.dart` com `ListView` de atividades, `RefreshIndicator` para pull-to-refresh, e navegação para detalhe
  - Criar `lib/features/activities/activity_detail_screen.dart` com detalhe completo da atividade: código, título, viatura, motorista, datas, estado atual
  - Criar `lib/features/activities/activity_status_update.dart` com botões de transição de estado disponíveis para o estado atual, chamada a `PATCH /api/v1/activities/{id}/status`, e feedback de sucesso/erro
  - Implementar tratamento de erro com mensagens claras para conectividade intermitente
  - _Requisitos: 18.2, 8.3_

- [ ] 62. Implementar ecrã de Checklist
  - Criar `lib/features/checklist/checklist_provider.dart` com Riverpod `AsyncNotifierProvider` que carrega template de checklist para a viatura da atividade via `GET /api/v1/checklist-templates`
  - Criar `lib/features/checklist/checklist_screen.dart` com lista de itens da checklist, seletor de estado por item (OK, AVARIA, FALTA), campo de notas, e botão de submissão
  - Implementar validação local: se item crítico marcado como AVARIA ou FALTA, apresentar aviso antes de submeter
  - Implementar submissão via `POST /api/v1/vehicles/{id}/checklists` com os mesmos critérios de validação da web
  - Apresentar feedback de sucesso com toast e navegar de volta ao detalhe da atividade
  - _Requisitos: 5.3, 5.4, 5.5, 18.2, 18.3_

- [ ] 63. Implementar pesquisa de Viatura por matrícula
  - Criar `lib/features/vehicles/vehicles_provider.dart` com Riverpod `AsyncNotifierProvider` para pesquisa de viaturas
  - Criar `lib/features/vehicles/vehicle_search_screen.dart` com campo de pesquisa, chamada a `GET /api/v1/vehicles/search?q={query}` a partir de 3 caracteres, e lista de resultados
  - Criar `lib/features/vehicles/vehicle_detail_screen.dart` com dados cadastrais da viatura, documentos com estados e validades, e alertas ativos
  - _Requisitos: 10.1, 10.2, 18.2_

- [ ] 64. Implementar ecrã de Alertas
  - Criar `lib/features/alerts/alerts_provider.dart` com Riverpod `AsyncNotifierProvider` que carrega alertas ativos via `GET /api/v1/alerts?resolved=false`
  - Criar `lib/features/alerts/alerts_screen.dart` com lista de alertas agrupados por severidade, usando `lib/shared/widgets/alert_card.dart`
  - Criar `lib/shared/widgets/alert_card.dart` com card de alerta com cor por severidade, título, mensagem e data de criação
  - _Requisitos: 11.6, 18.2_

- [ ] 65. Implementar widgets partilhados e polimento da app
  - Criar `lib/shared/widgets/status_chip.dart` que mapeia `ActivityStatus` e `VehicleStatus` para chips com cores
  - Criar `lib/shared/widgets/loading_overlay.dart` com indicador de carregamento sobreposto
  - Implementar tratamento global de erros de rede: quando `NetworkException` é lançada, apresentar `SnackBar` com mensagem "Sem ligação à internet. Tente novamente."
  - Implementar tratamento de `ApiException` com apresentação de mensagem de erro do campo `error.message`
  - Configurar `main.dart` com `ProviderScope`, `MaterialApp.router` com `go_router`, e tema Material 3
  - _Requisitos: 18.2_

- [ ] 66. Escrever testes da app Flutter
  - [ ]\* 66.1 Escrever testes de widget Flutter
    - Testar `ChecklistScreen` apresenta aviso quando item crítico marcado como AVARIA
    - Testar `ActivityStatusUpdate` apresenta apenas botões de transição válidos para o estado atual
    - _Requisitos: 5.5, 8.3_
  - [ ]\* 66.2 Escrever testes de integração Flutter
    - Testar `ActivitiesProvider` carrega lista de atividades corretamente
    - Testar `ChecklistProvider` carrega template de checklist para viatura
    - _Requisitos: 18.2, 18.3_

- [ ] 67. Checkpoint final — App Flutter completa
  - Verificar que autenticação Keycloak funciona em Android
  - Verificar que checklist submetida via app é processada com as mesmas validações da web
  - Verificar que pesquisa de viatura por matrícula funciona
  - Verificar que mensagens de erro claras são apresentadas em caso de falha de conectividade
  - Garantir que todos os testes passam, perguntar ao utilizador se surgirem dúvidas.

---

## Notas

- As tarefas marcadas com `*` são opcionais e podem ser ignoradas para uma implementação MVP mais rápida
- Cada tarefa referencia os requisitos específicos que implementa para rastreabilidade
- Os checkpoints garantem validação incremental antes de avançar para a fase seguinte
- As fases 1–6 (backend) devem estar completas antes de iniciar as fases 7–8 (frontend/mobile)
- O módulo `shared` (Tarefa 3) e o módulo `security` (Tarefa 4) são pré-requisitos para todos os outros módulos
- O módulo `audit` (Tarefa 14) deve ser implementado durante a Fase 2 para que a auditoria esteja disponível desde o início
- O módulo `integration` (Tarefa 11) deve ser implementado antes do módulo `driver` (Fase 3) e do módulo `activity` (Fase 4)
