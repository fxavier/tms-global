# Documento de Requisitos — TMS (Transport Management System)

## Introdução

O TMS (Transport Management System) é um sistema interno de gestão de transportes e logística, desenvolvido de raiz para suportar operações internas de transporte, controlo operacional, conformidade documental, manutenção de viaturas, rastreabilidade e gestão de atividades logísticas.

O sistema é construído com uma arquitetura monolítica modular (Spring Modulith), priorizando simplicidade, clareza, auditabilidade e manutenção. A autenticação e autorização são delegadas ao Keycloak. O frontend web é desenvolvido em Next.js com shadcn/ui e TailwindCSS. A aplicação móvel é desenvolvida em Flutter para consumo da API.

O TMS não duplica responsabilidades de sistemas de RH existentes — consome dados de RH por integração para determinar disponibilidade operacional de motoristas.

---

## Glossário

- **TMS**: Transport Management System — o sistema descrito neste documento.
- **Viatura**: Veículo registado no sistema, identificado pela matrícula, com dados cadastrais, documentação, manutenção e histórico operacional.
- **Motorista**: Colaborador com carta de condução válida, registado no TMS para fins operacionais logísticos.
- **Atividade**: Entidade operacional central que representa uma missão ou tarefa logística atribuída a uma viatura e a um motorista.
- **Checklist**: Lista de verificação configurável por tipo de viatura, preenchida antes ou durante uma atividade.
- **Documento**: Ficheiro ou registo associado a uma viatura ou motorista com data de validade e entidade emissora (ex.: seguro, carta de condução, inspeção).
- **Acessório**: Item físico presente na viatura (ex.: macaco, extintor, triângulo).
- **Manutenção**: Intervenção preventiva ou corretiva registada para uma viatura.
- **Estado_Operacional**: Estado atual de uma viatura — DISPONÍVEL, INDISPONÍVEL, EM_MANUTENÇÃO, ABATIDA.
- **Estado_Atividade**: Estado de ciclo de vida de uma atividade — PLANEADA, EM_CURSO, SUSPENSA, CONCLUÍDA, CANCELADA.
- **Alerta**: Notificação gerada pelo sistema para documentos próximos da expiração, manutenções previstas ou conflitos de alocação.
- **Auditoria**: Registo imutável de todas as alterações a entidades críticas do sistema.
- **RH_Sistema**: Sistema ou módulo de Recursos Humanos que fornece dados de férias, ausências e estado de contrato dos colaboradores.
- **Keycloak**: Servidor de identidade e autorização utilizado pelo TMS para autenticação e controlo de acesso baseado em papéis (RBAC).
- **Perfil_Utilizador**: Conjunto de permissões atribuído a um utilizador no Keycloak que determina o acesso às funcionalidades do TMS.
- **Matrícula**: Identificador único de uma viatura no sistema.
- **Carta_Conducao**: Documento legal que habilita um motorista a conduzir, com categoria e data de validade.
- **Conflito_Alocacao**: Situação em que uma viatura ou motorista é atribuído a mais do que uma atividade ativa em simultâneo.

---

## Perfis de Utilizador e Modelo de Permissões

Os perfis são geridos no Keycloak e mapeados para roles no TMS. O controlo de acesso é baseado em RBAC (Role-Based Access Control).

| Perfil               | Descrição                      | Acesso Principal                                                                 |
| -------------------- | ------------------------------ | -------------------------------------------------------------------------------- |
| `ADMIN`              | Administrador do sistema       | Acesso total a todas as funcionalidades, configurações e auditoria               |
| `GESTOR_FROTA`       | Gestor de frota e operações    | Gestão de viaturas, motoristas, atividades, manutenções, documentação            |
| `OPERADOR`           | Operador logístico             | Criação e acompanhamento de atividades, consulta de viaturas e motoristas        |
| `MOTORISTA`          | Motorista registado            | Consulta das suas atividades, preenchimento de checklist, registo de ocorrências |
| `TECNICO_MANUTENCAO` | Técnico de manutenção          | Registo e consulta de manutenções, checklists técnicas                           |
| `AUDITOR`            | Auditor interno                | Acesso de leitura a todos os registos, histórico e auditoria                     |
| `RH_INTEGRADOR`      | Conta de serviço do sistema RH | Acesso à API de integração de disponibilidade de motoristas                      |

### Regras de Permissão

- THE TMS SHALL aplicar controlo de acesso baseado no perfil Keycloak em todos os endpoints da API.
- THE TMS SHALL rejeitar pedidos sem token JWT válido emitido pelo Keycloak com resposta HTTP 401.
- THE TMS SHALL rejeitar pedidos com token válido mas sem permissão suficiente com resposta HTTP 403.
- WHEN um utilizador com perfil `MOTORISTA` acede ao sistema, THE TMS SHALL restringir a visualização às suas próprias atividades e checklists.
- THE `ADMIN` SHALL ter acesso irrestrito a todas as funcionalidades, incluindo configurações globais e registos de auditoria.

---

## Requisitos

### Requisito 1: Gestão de Viaturas — Cadastro e Dados Cadastrais

**User Story:** Como Gestor de Frota, quero cadastrar e gerir viaturas com todos os dados operacionais relevantes, para que a frota esteja sempre atualizada e rastreável.

#### Critérios de Aceitação

1. THE TMS SHALL permitir o registo de uma viatura com os seguintes campos obrigatórios: matrícula (único), marca, modelo, tipo de viatura, capacidade, local de atividade, data de início de atividade e estado operacional.
2. THE TMS SHALL permitir o registo de campos opcionais por viatura: motorista responsável atual, observações.
3. WHEN uma matrícula duplicada é submetida no registo de viatura, THE TMS SHALL rejeitar o pedido e retornar uma mensagem de erro indicando que a matrícula já existe.
4. WHEN os dados de uma viatura são atualizados, THE TMS SHALL registar na auditoria o utilizador, a data/hora, os campos alterados e os valores anteriores e novos.
5. THE TMS SHALL permitir a consulta de viaturas por matrícula, marca, modelo, tipo, estado operacional e local de atividade.
6. THE TMS SHALL suportar os seguintes estados operacionais de viatura: DISPONÍVEL, INDISPONÍVEL, EM_MANUTENÇÃO, ABATIDA.
7. WHEN o estado operacional de uma viatura é alterado para ABATIDA, THE TMS SHALL impedir a alocação dessa viatura a novas atividades.
8. THE TMS SHALL manter um histórico completo de todas as alterações aos dados cadastrais de cada viatura.

---

### Requisito 2: Gestão de Viaturas — Acessórios e Itens Físicos

**User Story:** Como Gestor de Frota, quero registar os acessórios físicos presentes em cada viatura, para que a conformidade de equipamento seja verificável a qualquer momento.

#### Critérios de Aceitação

1. THE TMS SHALL suportar os seguintes acessórios padrão por viatura: macaco, roda sobressalente, triângulo de sinalização, extintor, kit de primeiros socorros, colete refletor.
2. THE TMS SHALL permitir a configuração de acessórios adicionais por tipo de viatura, além dos acessórios padrão.
3. WHEN o estado de um acessório é registado, THE TMS SHALL aceitar um dos seguintes valores: PRESENTE, AUSENTE, DANIFICADO.
4. THE TMS SHALL registar a data da última verificação e o utilizador responsável por cada atualização de estado de acessório.
5. THE TMS SHALL manter histórico de todas as alterações ao estado dos acessórios de cada viatura.

---

### Requisito 3: Gestão de Viaturas — Documentação

**User Story:** Como Gestor de Frota, quero gerir a documentação associada a cada viatura com controlo de validade, para que a frota opere sempre em conformidade legal e regulatória.

#### Critérios de Aceitação

1. THE TMS SHALL suportar os seguintes tipos de documento de viatura: livrete, inspeção periódica, seguro, licença de circulação, manifesto de carga, taxa de rádio.
2. THE TMS SHALL permitir a configuração de tipos de documento adicionais por administrador.
3. WHEN um documento de viatura é registado, THE TMS SHALL armazenar: tipo, número do documento, data de emissão, data de validade, entidade emissora, estado, observações e referência ao ficheiro anexo.
4. THE TMS SHALL suportar os seguintes estados de documento: VÁLIDO, EXPIRADO, PENDENTE_RENOVAÇÃO, CANCELADO.
5. WHEN a data atual ultrapassa a data de validade de um documento de viatura, THE TMS SHALL atualizar automaticamente o estado do documento para EXPIRADO.
6. WHEN a data de validade de um documento de viatura está dentro do período de alerta configurado, THE TMS SHALL gerar um alerta de expiração iminente associado à viatura e ao documento.
7. THE TMS SHALL permitir a configuração do período de alerta de expiração em dias, com valor padrão de 30 dias, configurável por tipo de documento.
8. IF um documento de viatura com estado EXPIRADO é detetado no momento de alocação a uma atividade, THEN THE TMS SHALL bloquear a alocação e retornar uma mensagem indicando o documento expirado.
9. THE TMS SHALL permitir o upload de ficheiros anexos a documentos nos formatos PDF, JPG e PNG, com tamanho máximo de 10 MB por ficheiro.
10. THE TMS SHALL manter histórico de todas as versões de documentos registados por viatura.

---

### Requisito 4: Gestão de Viaturas — Manutenção

**User Story:** Como Gestor de Frota e Técnico de Manutenção, quero registar e acompanhar manutenções preventivas e corretivas de cada viatura, para que o estado técnico da frota seja controlado e as intervenções sejam rastreáveis.

#### Critérios de Aceitação

1. THE TMS SHALL suportar dois tipos de manutenção: PREVENTIVA e CORRETIVA.
2. WHEN uma manutenção é registada, THE TMS SHALL armazenar: tipo, data de realização, quilometragem no momento da intervenção, descrição, fornecedor ou oficina, custo total, peças substituídas, utilizador responsável e data prevista da próxima manutenção.
3. WHEN uma manutenção é registada com data prevista de próxima intervenção, THE TMS SHALL criar automaticamente um alerta para a data prevista, com antecedência configurável em dias.
4. WHEN a quilometragem atual de uma viatura ultrapassa o limiar de quilometragem configurado para manutenção preventiva, THE TMS SHALL gerar um alerta de manutenção por quilometragem.
5. WHILE uma viatura tem estado EM_MANUTENÇÃO, THE TMS SHALL impedir a sua alocação a novas atividades.
6. THE TMS SHALL manter o histórico completo de todas as manutenções realizadas por viatura, ordenado cronologicamente.
7. THE TMS SHALL permitir a consulta de manutenções por viatura, tipo, período de datas e fornecedor.
8. WHEN uma manutenção é concluída e o estado da viatura é atualizado de EM_MANUTENÇÃO para DISPONÍVEL, THE TMS SHALL registar o evento na auditoria da viatura.

---

### Requisito 5: Gestão de Viaturas — Checklist de Inspeção

**User Story:** Como Motorista ou Técnico de Manutenção, quero preencher checklists de inspeção configuráveis por tipo de viatura, para que o estado operacional seja verificado antes de cada atividade.

#### Critérios de Aceitação

1. THE TMS SHALL suportar checklists configuráveis por tipo de viatura, com os seguintes itens padrão: pneus, travões, luzes, extintor, triângulo de sinalização, roda sobressalente, macaco, kit de primeiros socorros, colete refletor.
2. THE TMS SHALL permitir a adição de itens personalizados à checklist por tipo de viatura, por utilizador com perfil ADMIN ou GESTOR_FROTA.
3. WHEN um item de checklist é preenchido, THE TMS SHALL aceitar um dos seguintes estados: OK, AVARIA, FALTA.
4. WHEN uma checklist é submetida, THE TMS SHALL registar: viatura, utilizador responsável, data e hora de preenchimento, e o estado de cada item.
5. IF um item de checklist marcado como crítico tem estado AVARIA ou FALTA no momento de início de uma atividade, THEN THE TMS SHALL bloquear o início da atividade e notificar o gestor responsável.
6. THE TMS SHALL permitir a configuração de quais itens da checklist são considerados críticos, por tipo de viatura.
7. THE TMS SHALL manter o histórico completo de todas as checklists preenchidas por viatura.
8. THE TMS SHALL permitir a consulta do histórico de checklists por viatura, período de datas e utilizador responsável.

---

### Requisito 6: Gestão de Motoristas — Dados Cadastrais e Documentação

**User Story:** Como Gestor de Frota, quero gerir os dados operacionais dos motoristas e a sua documentação, para que apenas motoristas com habilitação válida sejam alocados a atividades.

#### Critérios de Aceitação

1. THE TMS SHALL permitir o registo de um motorista com os seguintes campos obrigatórios: nome completo, número de contacto, morada, número do Bilhete de Identidade, número da carta de condução, categoria da carta, data de emissão da carta, data de validade da carta, local de atividade e estado.
2. THE TMS SHALL suportar os seguintes estados de motorista: ATIVO, INATIVO, SUSPENSO.
3. WHEN os dados de um motorista são atualizados, THE TMS SHALL registar na auditoria o utilizador, a data/hora, os campos alterados e os valores anteriores e novos.
4. THE TMS SHALL suportar os seguintes tipos de documento de motorista: carta de condução, bilhete de identidade, outros documentos operacionais.
5. WHEN um documento de motorista é registado, THE TMS SHALL armazenar: tipo, número, data de emissão, data de validade, entidade emissora, estado, observações e referência ao ficheiro anexo.
6. WHEN a data atual ultrapassa a data de validade da carta de condução de um motorista, THE TMS SHALL atualizar automaticamente o estado do documento para EXPIRADO.
7. WHEN a data de validade da carta de condução de um motorista está dentro do período de alerta configurado, THE TMS SHALL gerar um alerta de expiração iminente associado ao motorista.
8. IF a carta de condução de um motorista tem estado EXPIRADO no momento de alocação a uma atividade, THEN THE TMS SHALL bloquear a alocação e retornar uma mensagem indicando a carta expirada.
9. THE TMS SHALL manter histórico completo de todas as alterações aos dados cadastrais e documentação de cada motorista.

---

### Requisito 7: Gestão de Motoristas — Disponibilidade Operacional

**User Story:** Como Gestor de Frota, quero consultar a disponibilidade operacional de cada motorista integrando dados do sistema de RH, para que a alocação de motoristas a atividades seja sempre válida e sem conflitos.

#### Critérios de Aceitação

1. THE TMS SHALL consultar o RH_Sistema para obter o estado de disponibilidade de um motorista antes de permitir a sua alocação a uma atividade.
2. WHEN o RH_Sistema indica que um motorista está em férias ou ausência no período da atividade, THE TMS SHALL bloquear a alocação e apresentar a razão da indisponibilidade.
3. WHEN o RH_Sistema indica que o contrato de um motorista está inativo ou suspenso, THE TMS SHALL bloquear a alocação desse motorista a qualquer atividade.
4. IF o RH_Sistema não estiver disponível no momento da consulta, THEN THE TMS SHALL registar o erro, notificar o gestor e permitir a alocação manual com registo de justificação obrigatória.
5. THE TMS SHALL apresentar na ficha do motorista o estado de disponibilidade atual proveniente do RH_Sistema, com indicação da fonte e data/hora da última sincronização.
6. THE TMS SHALL verificar conflitos de alocação simultânea: WHEN um motorista já está alocado a uma atividade com estado EM_CURSO, THE TMS SHALL impedir a sua alocação a outra atividade no mesmo período.

---

### Requisito 8: Gestão de Atividades — Ciclo de Vida

**User Story:** Como Operador ou Gestor de Frota, quero criar e gerir atividades logísticas com controlo de estado e alocação de recursos, para que as operações de transporte sejam planeadas, executadas e encerradas de forma controlada.

#### Critérios de Aceitação

1. THE TMS SHALL permitir a criação de uma atividade com os seguintes campos obrigatórios: título, tipo de atividade, local, data de início prevista, data de conclusão prevista, prioridade e estado inicial PLANEADA.
2. THE TMS SHALL permitir o registo de campos opcionais por atividade: código interno, descrição detalhada, motorista responsável, viatura associada, observações, anexos.
3. THE TMS SHALL suportar os seguintes estados de atividade e as transições permitidas:
   - PLANEADA → EM_CURSO, CANCELADA
   - EM_CURSO → SUSPENSA, CONCLUÍDA, CANCELADA
   - SUSPENSA → EM_CURSO, CANCELADA
   - CONCLUÍDA → (estado terminal, sem transições)
   - CANCELADA → (estado terminal, sem transições)
4. WHEN uma transição de estado não permitida é solicitada, THE TMS SHALL rejeitar o pedido e retornar uma mensagem indicando a transição inválida.
5. WHEN o estado de uma atividade é alterado, THE TMS SHALL registar na auditoria: utilizador, data/hora, estado anterior, estado novo e observação opcional.
6. THE TMS SHALL gerar um código único sequencial para cada atividade criada, no formato `ACT-{ANO}-{SEQUENCIAL}`.
7. THE TMS SHALL suportar os seguintes níveis de prioridade de atividade: BAIXA, NORMAL, ALTA, URGENTE.
8. THE TMS SHALL manter um histórico completo de todos os eventos e alterações de cada atividade.

---

### Requisito 9: Gestão de Atividades — Regras de Alocação

**User Story:** Como Operador, quero que o sistema valide automaticamente a alocação de viaturas e motoristas a atividades, para que apenas recursos válidos e disponíveis sejam utilizados em operações.

#### Critérios de Aceitação

1. IF uma viatura com documentação expirada é selecionada para alocação a uma atividade, THEN THE TMS SHALL bloquear a alocação e listar os documentos expirados.
2. IF uma viatura com estado EM_MANUTENÇÃO ou INDISPONÍVEL ou ABATIDA é selecionada para alocação, THEN THE TMS SHALL bloquear a alocação e indicar o motivo.
3. IF uma viatura com checklist crítica com falhas não resolvidas é selecionada para alocação, THEN THE TMS SHALL bloquear a alocação e indicar os itens críticos em falha.
4. IF um motorista com carta de condução expirada é selecionado para alocação, THEN THE TMS SHALL bloquear a alocação e indicar o documento expirado.
5. IF um motorista com estado INATIVO ou SUSPENSO é selecionado para alocação, THEN THE TMS SHALL bloquear a alocação e indicar o estado do motorista.
6. WHEN uma viatura já está alocada a uma atividade com estado EM_CURSO ou PLANEADA no mesmo período, THE TMS SHALL detetar o conflito de alocação, bloquear a nova alocação e apresentar a atividade em conflito.
7. WHEN um motorista já está alocado a uma atividade com estado EM_CURSO ou PLANEADA no mesmo período, THE TMS SHALL detetar o conflito de alocação, bloquear a nova alocação e apresentar a atividade em conflito.
8. THE TMS SHALL apresentar ao utilizador um resumo claro de todos os bloqueios de alocação antes de rejeitar definitivamente a operação.

---

### Requisito 10: Consulta Consolidada por Matrícula

**User Story:** Como Gestor de Frota ou Auditor, quero pesquisar uma viatura pela matrícula e obter uma visão consolidada de toda a informação associada, para que a análise operacional e de conformidade seja rápida e completa.

#### Critérios de Aceitação

1. THE TMS SHALL disponibilizar uma funcionalidade de pesquisa por matrícula acessível a partir da página principal da aplicação web e da aplicação móvel.
2. WHEN uma matrícula válida é pesquisada, THE TMS SHALL retornar uma visão consolidada contendo: dados cadastrais, estado operacional atual, motorista responsável atual, histórico de motoristas responsáveis, documentação com estados e validades, histórico de manutenções, checklists e inspeções recentes, acessórios e respetivos estados, atividades passadas e ativas, alertas ativos, ocorrências registadas e histórico de eventos relevantes.
3. WHEN uma matrícula inexistente é pesquisada, THE TMS SHALL retornar uma mensagem indicando que a viatura não foi encontrada.
4. THE TMS SHALL apresentar os alertas ativos de forma destacada no topo da visão consolidada.
5. THE TMS SHALL permitir a navegação direta da visão consolidada para cada secção de detalhe (documentação, manutenção, atividades, etc.).
6. THE TMS SHALL suportar pesquisa parcial de matrícula, retornando sugestões a partir de 3 caracteres introduzidos.

---

### Requisito 11: Sistema de Alertas

**User Story:** Como Gestor de Frota, quero receber alertas automáticos sobre documentos a expirar, manutenções previstas e conflitos operacionais, para que a conformidade e a disponibilidade da frota sejam mantidas proativamente.

#### Critérios de Aceitação

1. THE TMS SHALL gerar alertas automáticos para documentos de viatura cuja data de validade esteja dentro do período de alerta configurado.
2. THE TMS SHALL gerar alertas automáticos para documentos de motorista cuja data de validade esteja dentro do período de alerta configurado.
3. THE TMS SHALL gerar alertas automáticos para manutenções preventivas cuja data prevista esteja dentro do período de alerta configurado.
4. THE TMS SHALL gerar alertas automáticos quando a quilometragem de uma viatura ultrapassa o limiar configurado para manutenção preventiva.
5. THE TMS SHALL suportar os seguintes níveis de severidade de alerta: INFO, AVISO, CRÍTICO.
6. THE TMS SHALL apresentar alertas ativos no painel principal da aplicação web, agrupados por severidade.
7. THE TMS SHALL permitir a configuração dos períodos de alerta por tipo de documento e por tipo de manutenção, em dias, por utilizador com perfil ADMIN ou GESTOR_FROTA.
8. WHEN um alerta é resolvido (documento renovado, manutenção realizada), THE TMS SHALL marcar automaticamente o alerta como resolvido e registar a data de resolução.
9. THE TMS SHALL manter histórico de todos os alertas gerados, incluindo alertas resolvidos.
10. WHERE a configuração de notificações por email estiver ativa, THE TMS SHALL enviar notificações por email aos gestores responsáveis para alertas com severidade CRÍTICO.

---

### Requisito 12: Auditoria e Rastreabilidade

**User Story:** Como Auditor ou Administrador, quero aceder ao histórico completo de todas as alterações a entidades críticas do sistema, para que a rastreabilidade e a conformidade sejam garantidas.

#### Critérios de Aceitação

1. THE TMS SHALL registar automaticamente na auditoria todas as operações de criação, atualização e eliminação lógica sobre as entidades: Viatura, Motorista, Atividade, Documento, Manutenção, Checklist.
2. WHEN um registo de auditoria é criado, THE TMS SHALL armazenar: entidade afetada, identificador da entidade, tipo de operação (CRIAÇÃO, ATUALIZAÇÃO, ELIMINAÇÃO), utilizador responsável, data e hora da operação, endereço IP de origem, valores anteriores e valores novos em formato JSON.
3. THE TMS SHALL garantir que os registos de auditoria são imutáveis — nenhum utilizador, incluindo ADMIN, pode alterar ou eliminar registos de auditoria.
4. THE TMS SHALL disponibilizar uma interface de consulta de auditoria filtrável por entidade, utilizador, tipo de operação e período de datas, acessível a utilizadores com perfil AUDITOR ou ADMIN.
5. THE TMS SHALL manter os registos de auditoria por um período mínimo de 5 anos.
6. THE TMS SHALL registar na auditoria todas as tentativas de acesso não autorizado, incluindo tokens inválidos e permissões insuficientes.

---

### Requisito 13: Integração com Sistema de RH

**User Story:** Como sistema TMS, quero consumir dados de disponibilidade do sistema de RH sem replicar a sua lógica, para que a alocação de motoristas reflita sempre o estado real de disponibilidade dos colaboradores.

#### Critérios de Aceitação

1. THE TMS SHALL integrar com o RH_Sistema através de chamada interna entre módulos Spring Modulith se o RH fizer parte do mesmo monólito, ou através de API REST interna privada se o RH for um sistema separado.
2. THE TMS SHALL consultar o RH_Sistema para verificar: estado do contrato do colaborador, períodos de férias aprovadas e ausências registadas.
3. THE TMS SHALL utilizar os dados do RH_Sistema exclusivamente para determinar disponibilidade operacional — sem replicar dados de RH na base de dados do TMS.
4. IF a integração com o RH_Sistema falhar, THEN THE TMS SHALL registar o erro com detalhes técnicos, apresentar ao utilizador uma mensagem de indisponibilidade temporária e permitir alocação manual com justificação obrigatória registada na auditoria.
5. THE TMS SHALL implementar um mecanismo de cache com TTL configurável para os dados de disponibilidade do RH_Sistema, com valor padrão de 15 minutos, para reduzir a latência e o impacto de indisponibilidade temporária do RH_Sistema.
6. THE TMS SHALL expor um endpoint de integração para o RH_Sistema notificar alterações de disponibilidade em tempo real, protegido pelo perfil `RH_INTEGRADOR` no Keycloak.

---

### Requisito 14: Requisitos Não Funcionais — Desempenho e Escalabilidade

**User Story:** Como equipa de desenvolvimento, quero que o sistema cumpra requisitos de desempenho adequados ao volume operacional esperado, para que a experiência dos utilizadores seja fluida e o sistema seja sustentável.

#### Critérios de Aceitação

1. WHEN um pedido de consulta de dados cadastrais (viatura, motorista, atividade) é recebido, THE TMS SHALL retornar a resposta em menos de 500ms para o percentil 95 das requisições, em condições normais de carga.
2. WHEN uma pesquisa por matrícula é executada, THE TMS SHALL retornar os resultados em menos de 1000ms para o percentil 95 das requisições.
3. THE TMS SHALL suportar pelo menos 50 utilizadores concorrentes sem degradação de desempenho acima dos limites definidos.
4. THE TMS SHALL implementar paginação em todas as listagens com mais de 20 registos, com tamanho de página configurável entre 10 e 100 registos.
5. THE TMS SHALL implementar índices de base de dados nas colunas de pesquisa frequente: matrícula, estado operacional, estado de atividade, data de validade de documentos.

---

### Requisito 15: Requisitos Não Funcionais — Segurança

**User Story:** Como equipa de desenvolvimento e administração, quero que o sistema implemente práticas de segurança adequadas, para que os dados operacionais e pessoais sejam protegidos.

#### Critérios de Aceitação

1. THE TMS SHALL autenticar todos os pedidos à API através de tokens JWT emitidos pelo Keycloak, rejeitando pedidos sem token válido com HTTP 401.
2. THE TMS SHALL validar o token JWT em cada pedido, verificando assinatura, expiração e emissor.
3. THE TMS SHALL implementar HTTPS obrigatório em todos os endpoints expostos em ambiente de produção.
4. THE TMS SHALL sanitizar todos os inputs recebidos da API para prevenir injeção SQL e XSS.
5. THE TMS SHALL implementar rate limiting nos endpoints públicos de autenticação para prevenir ataques de força bruta, com limite configurável de pedidos por minuto por IP.
6. THE TMS SHALL armazenar ficheiros anexos fora da base de dados relacional, em sistema de ficheiros ou object storage, com referência por identificador interno.
7. THE TMS SHALL registar na auditoria todos os acessos a dados sensíveis de motoristas (BI, carta de condução).

---

### Requisito 16: Requisitos Não Funcionais — Disponibilidade e Operação

**User Story:** Como equipa de operações, quero que o sistema seja resiliente e operacionalmente monitorizado, para que a disponibilidade do serviço seja garantida.

#### Critérios de Aceitação

1. THE TMS SHALL disponibilizar um endpoint de health check (`/actuator/health`) que retorne o estado do sistema, da base de dados e das integrações externas.
2. THE TMS SHALL implementar eliminação lógica (soft delete) em todas as entidades principais, preservando o histórico e a integridade referencial.
3. THE TMS SHALL expor métricas operacionais através do Spring Boot Actuator compatíveis com Prometheus/Grafana.
4. THE TMS SHALL implementar logging estruturado em formato JSON, com nível de log configurável por ambiente.
5. IF uma operação de escrita na base de dados falhar, THEN THE TMS SHALL fazer rollback da transação completa e retornar um erro HTTP 500 com identificador de correlação para rastreamento.

---

### Requisito 17: Aplicação Web — Interface e Usabilidade

**User Story:** Como utilizador da aplicação web, quero uma interface clara e responsiva construída com Next.js e shadcn/ui, para que as operações diárias sejam realizadas de forma eficiente.

#### Critérios de Aceitação

1. THE TMS SHALL disponibilizar uma aplicação web desenvolvida em Next.js com shadcn/ui e TailwindCSS, acessível em browsers modernos (Chrome, Firefox, Edge, Safari — versões dos últimos 2 anos).
2. THE TMS SHALL apresentar um painel principal (dashboard) com resumo de alertas ativos, atividades em curso, viaturas indisponíveis e indicadores operacionais chave.
3. THE TMS SHALL implementar navegação por módulo: Viaturas, Motoristas, Atividades, Manutenções, Alertas, Auditoria, Configurações.
4. THE TMS SHALL garantir que a interface web é responsiva e utilizável em ecrãs com largura mínima de 1024px.
5. THE TMS SHALL implementar feedback visual imediato para operações de criação, atualização e erro, através de notificações toast.
6. WHERE o utilizador tem perfil MOTORISTA, THE TMS SHALL apresentar uma interface simplificada focada nas suas atividades e checklists.

---

### Requisito 18: Aplicação Móvel — Flutter

**User Story:** Como Motorista ou Operador em campo, quero aceder às funcionalidades essenciais do TMS através de uma aplicação móvel Flutter, para que as operações em campo sejam suportadas sem necessidade de acesso ao browser.

#### Critérios de Aceitação

1. THE TMS SHALL disponibilizar uma API REST JSON consumível pela aplicação móvel Flutter, com autenticação via Keycloak.
2. THE TMS SHALL suportar na aplicação móvel as seguintes funcionalidades: autenticação, consulta de atividades atribuídas, atualização de estado de atividade, preenchimento de checklist, pesquisa de viatura por matrícula, consulta de alertas.
3. WHEN a aplicação móvel submete uma checklist, THE TMS SHALL processar e persistir os dados com os mesmos critérios de validação aplicados à aplicação web.
4. THE TMS SHALL retornar respostas da API em formato JSON com estrutura consistente: `{ "data": ..., "error": null }` em caso de sucesso e `{ "data": null, "error": { "code": ..., "message": ... } }` em caso de erro.
5. THE TMS SHALL versionar a API REST com prefixo de versão no path (`/api/v1/...`) para garantir compatibilidade com versões anteriores da aplicação móvel.

---

## Fluxos Operacionais e Transições de Estado

### Fluxo de Criação e Execução de Atividade

```
[Operador cria Atividade] → Estado: PLANEADA
        ↓
[Atribui Viatura + Motorista]
        ↓
[Sistema valida: documentos, estado viatura, disponibilidade motorista, conflitos]
        ↓ (validação OK)
[Atividade pronta para início] → Estado: PLANEADA
        ↓
[Motorista/Operador inicia atividade]
        ↓
[Sistema valida checklist crítica]
        ↓ (checklist OK)
[Estado: EM_CURSO]
        ↓
[Motorista/Operador conclui ou suspende]
        ↓
[Estado: CONCLUÍDA ou SUSPENSA]
        ↓ (se SUSPENSA)
[Retoma → EM_CURSO] ou [Cancela → CANCELADA]
```

### Transições de Estado de Viatura

```
DISPONÍVEL ←→ EM_MANUTENÇÃO
DISPONÍVEL → INDISPONÍVEL
INDISPONÍVEL → DISPONÍVEL
DISPONÍVEL → ABATIDA (terminal)
EM_MANUTENÇÃO → DISPONÍVEL
```

### Fluxo de Alerta de Documento

```
[Job diário verifica validades]
        ↓
[Documento dentro do período de alerta?]
        ↓ Sim
[Gera Alerta com severidade AVISO]
        ↓
[Data de validade ultrapassada?]
        ↓ Sim
[Alerta promovido para CRÍTICO + documento → EXPIRADO]
        ↓
[Bloqueio automático de alocação]
        ↓
[Documento renovado → Alerta resolvido automaticamente]
```

---

## Backlog Estruturado

### Épico 1: Gestão de Viaturas

| ID    | Tipo    | Descrição                                           | Prioridade |
| ----- | ------- | --------------------------------------------------- | ---------- |
| E1-F1 | Feature | Cadastro e edição de viaturas                       | Alta       |
| E1-F2 | Feature | Gestão de acessórios por viatura                    | Alta       |
| E1-F3 | Feature | Gestão de documentação de viaturas                  | Alta       |
| E1-F4 | Feature | Gestão de manutenções (preventiva/corretiva)        | Alta       |
| E1-F5 | Feature | Checklists de inspeção configuráveis                | Alta       |
| E1-F6 | Feature | Histórico completo por viatura                      | Média      |
| E1-T1 | Técnica | Modelo de dados: entidade Viatura e relações        | Alta       |
| E1-T2 | Técnica | Módulo Spring Modulith: `vehicle`                   | Alta       |
| E1-T3 | Técnica | API REST: endpoints CRUD de viaturas                | Alta       |
| E1-T4 | Técnica | Upload e armazenamento de ficheiros anexos          | Média      |
| E1-T5 | Técnica | Job de verificação diária de validade de documentos | Alta       |

### Épico 2: Gestão de Motoristas

| ID    | Tipo    | Descrição                                      | Prioridade |
| ----- | ------- | ---------------------------------------------- | ---------- |
| E2-F1 | Feature | Cadastro e edição de motoristas                | Alta       |
| E2-F2 | Feature | Gestão de documentação de motoristas           | Alta       |
| E2-F3 | Feature | Consulta de disponibilidade operacional        | Alta       |
| E2-T1 | Técnica | Modelo de dados: entidade Motorista e relações | Alta       |
| E2-T2 | Técnica | Módulo Spring Modulith: `driver`               | Alta       |
| E2-T3 | Técnica | API REST: endpoints CRUD de motoristas         | Alta       |
| E2-T4 | Técnica | Integração com RH_Sistema (disponibilidade)    | Alta       |
| E2-T5 | Técnica | Cache de disponibilidade com TTL configurável  | Média      |

### Épico 3: Gestão de Atividades

| ID    | Tipo    | Descrição                                          | Prioridade |
| ----- | ------- | -------------------------------------------------- | ---------- |
| E3-F1 | Feature | Criação e edição de atividades                     | Alta       |
| E3-F2 | Feature | Gestão de ciclo de vida e transições de estado     | Alta       |
| E3-F3 | Feature | Alocação de viatura e motorista com validação      | Alta       |
| E3-F4 | Feature | Histórico de eventos por atividade                 | Média      |
| E3-T1 | Técnica | Modelo de dados: entidade Atividade e relações     | Alta       |
| E3-T2 | Técnica | Módulo Spring Modulith: `activity`                 | Alta       |
| E3-T3 | Técnica | Motor de validação de alocação (regras de negócio) | Alta       |
| E3-T4 | Técnica | Geração de código sequencial ACT-{ANO}-{SEQ}       | Média      |

### Épico 4: Consulta Consolidada por Matrícula

| ID    | Tipo    | Descrição                                            | Prioridade |
| ----- | ------- | ---------------------------------------------------- | ---------- |
| E4-F1 | Feature | Pesquisa por matrícula com visão consolidada         | Alta       |
| E4-F2 | Feature | Pesquisa parcial com sugestões                       | Média      |
| E4-T1 | Técnica | Endpoint de consulta consolidada otimizado           | Alta       |
| E4-T2 | Técnica | Índices de base de dados para pesquisa por matrícula | Alta       |

### Épico 5: Sistema de Alertas

| ID    | Tipo    | Descrição                                             | Prioridade |
| ----- | ------- | ----------------------------------------------------- | ---------- |
| E5-F1 | Feature | Painel de alertas ativos no dashboard                 | Alta       |
| E5-F2 | Feature | Configuração de períodos de alerta por tipo           | Média      |
| E5-F3 | Feature | Notificações por email para alertas críticos          | Baixa      |
| E5-T1 | Técnica | Modelo de dados: entidade Alerta                      | Alta       |
| E5-T2 | Técnica | Job agendado de geração de alertas (Spring Scheduler) | Alta       |
| E5-T3 | Técnica | Módulo Spring Modulith: `alert`                       | Alta       |

### Épico 6: Auditoria e Rastreabilidade

| ID    | Tipo    | Descrição                                            | Prioridade |
| ----- | ------- | ---------------------------------------------------- | ---------- |
| E6-F1 | Feature | Interface de consulta de auditoria com filtros       | Alta       |
| E6-T1 | Técnica | Modelo de dados: entidade AuditLog (imutável)        | Alta       |
| E6-T2 | Técnica | Interceptor/AOP para registo automático de auditoria | Alta       |
| E6-T3 | Técnica | Módulo Spring Modulith: `audit`                      | Alta       |

### Épico 7: Infraestrutura e Segurança

| ID    | Tipo    | Descrição                                     | Prioridade |
| ----- | ------- | --------------------------------------------- | ---------- |
| E7-T1 | Técnica | Configuração Keycloak: realms, clients, roles | Alta       |
| E7-T2 | Técnica | Integração Spring Security + Keycloak JWT     | Alta       |
| E7-T3 | Técnica | Configuração PostgreSQL + Flyway migrations   | Alta       |
| E7-T4 | Técnica | Estrutura base Spring Modulith + Maven        | Alta       |
| E7-T5 | Técnica | Health check e métricas Actuator              | Média      |
| E7-T6 | Técnica | Logging estruturado JSON                      | Média      |
| E7-T7 | Técnica | Rate limiting nos endpoints de autenticação   | Média      |

### Épico 8: Frontend Web (Next.js)

| ID    | Tipo    | Descrição                                             | Prioridade |
| ----- | ------- | ----------------------------------------------------- | ---------- |
| E8-F1 | Feature | Dashboard com alertas e indicadores                   | Alta       |
| E8-F2 | Feature | Módulo de Viaturas (listagem, detalhe, formulários)   | Alta       |
| E8-F3 | Feature | Módulo de Motoristas                                  | Alta       |
| E8-F4 | Feature | Módulo de Atividades                                  | Alta       |
| E8-F5 | Feature | Módulo de Manutenções                                 | Alta       |
| E8-F6 | Feature | Módulo de Alertas                                     | Alta       |
| E8-F7 | Feature | Módulo de Auditoria                                   | Média      |
| E8-F8 | Feature | Pesquisa consolidada por matrícula                    | Alta       |
| E8-T1 | Técnica | Setup Next.js + shadcn/ui + TailwindCSS               | Alta       |
| E8-T2 | Técnica | Integração Keycloak no frontend (NextAuth ou similar) | Alta       |
| E8-T3 | Técnica | Cliente API tipado (fetch/axios + tipos gerados)      | Alta       |

### Épico 9: Aplicação Móvel (Flutter)

| ID    | Tipo    | Descrição                            | Prioridade |
| ----- | ------- | ------------------------------------ | ---------- |
| E9-F1 | Feature | Autenticação Keycloak no Flutter     | Alta       |
| E9-F2 | Feature | Ecrã de atividades atribuídas        | Alta       |
| E9-F3 | Feature | Atualização de estado de atividade   | Alta       |
| E9-F4 | Feature | Preenchimento de checklist           | Alta       |
| E9-F5 | Feature | Pesquisa de viatura por matrícula    | Média      |
| E9-F6 | Feature | Consulta de alertas                  | Média      |
| E9-T1 | Técnica | Setup Flutter + integração API REST  | Alta       |
| E9-T2 | Técnica | Gestão de estado (Provider/Riverpod) | Alta       |

---

## Riscos, Lacunas, Trade-offs e Decisões Técnicas

### Riscos Identificados

| ID  | Risco                                                            | Probabilidade | Impacto | Mitigação                                                                                  |
| --- | ---------------------------------------------------------------- | ------------- | ------- | ------------------------------------------------------------------------------------------ |
| R1  | RH_Sistema indisponível durante alocação                         | Média         | Alto    | Cache com TTL + alocação manual com justificação obrigatória                               |
| R2  | Volume de alertas excessivo sem triagem                          | Média         | Médio   | Configuração de períodos por tipo + agrupamento por severidade                             |
| R3  | Conflitos de alocação não detetados em condições de concorrência | Baixa         | Alto    | Transações com isolamento SERIALIZABLE ou pessimistic locking nas verificações de conflito |
| R4  | Crescimento do volume de registos de auditoria                   | Alta          | Baixo   | Política de retenção de 5 anos + particionamento da tabela de auditoria por data           |
| R5  | Integração RH com contrato de API instável                       | Média         | Alto    | Definir contrato de API estável com o módulo RH antes do desenvolvimento                   |

### Lacunas a Clarificar

| ID  | Lacuna                                                                                        | Impacto | Ação Recomendada                                  |
| --- | --------------------------------------------------------------------------------------------- | ------- | ------------------------------------------------- |
| L1  | Contrato exato da API do RH_Sistema (campos, endpoints, autenticação)                         | Alto    | Reunião com equipa de RH antes do Épico 2         |
| L2  | Tipos de viatura suportados e checklists específicas por tipo                                 | Médio   | Workshop com gestores de frota                    |
| L3  | Categorias de carta de condução relevantes para validação                                     | Médio   | Confirmar com equipa operacional                  |
| L4  | Requisitos de notificação por email (servidor SMTP, templates)                                | Baixo   | Definir na fase de configuração de infraestrutura |
| L5  | Política de retenção e backup de ficheiros anexos                                             | Médio   | Definir com equipa de infraestrutura              |
| L6  | Modelo de permissões granular dentro de cada perfil (ex.: OPERADOR pode cancelar atividades?) | Alto    | Validar com stakeholders antes do Épico 7         |

### Trade-offs e Decisões Técnicas

| Decisão                                                           | Alternativa Considerada | Justificação                                                                                               |
| ----------------------------------------------------------------- | ----------------------- | ---------------------------------------------------------------------------------------------------------- |
| Monólito modular (Spring Modulith) em vez de microserviços        | Microserviços           | Reduz complexidade operacional, adequado ao volume e equipa; modularidade mantida por fronteiras de módulo |
| RBAC via Keycloak em vez de ABAC customizado                      | ABAC no código          | Keycloak já é infraestrutura existente; RBAC cobre os casos de uso identificados sem overengineering       |
| Soft delete em vez de hard delete                                 | Hard delete             | Preserva histórico e integridade referencial; auditabilidade garantida                                     |
| Cache TTL para dados de RH em vez de sincronização em tempo real  | Webhook em tempo real   | Pragmático; reduz acoplamento; TTL de 15 min aceitável para disponibilidade operacional                    |
| Auditoria por AOP/interceptor em vez de triggers de base de dados | Triggers PostgreSQL     | Mantém lógica de auditoria na camada de aplicação, mais testável e portável                                |
| API versionada `/api/v1/` desde o início                          | Sem versionamento       | Garante compatibilidade com app móvel Flutter em produção sem forçar atualizações imediatas                |
| Armazenamento de ficheiros fora da BD                             | BYTEA no PostgreSQL     | Evita crescimento descontrolado da BD; melhor desempenho em consultas; facilita backup diferenciado        |
