-- As cinco tabelas, com as restrições que o banco consegue impor
-- (instrucao.md, seção 5). As que dependem do dia de hoje ficam na aplicação.

-- A restrição de exclusão da fase usa igualdade de inteiro dentro do gist
-- (ADR-0033)
create extension if not exists btree_gist;

create table usuario (
    id int generated always as identity primary key,
    email text not null unique,
    nome text not null,
    foto_url text,
    criado_em timestamptz not null
);

-- A lista de convites da RN-48
create table convidado (
    email text primary key,
    convidado_em timestamptz not null
);

-- O token em si nunca é gravado, só o hash dele (ADR-0041)
create table token_renovacao (
    id int generated always as identity primary key,
    usuario_id int not null references usuario (id) on delete cascade,
    hash text not null unique,
    criado_em timestamptz not null,
    expira_em timestamptz not null
);

-- A base_data guarda o dia da base da RN-68, e não o valor
create table fase (
    id int generated always as identity primary key,
    usuario_id int not null references usuario (id) on delete cascade,
    tipo text not null
        check (tipo in ('cutting', 'bulking', 'manutencao')),
    data_inicio date not null,
    data_fim date,
    nivel text
        check (nivel in ('conservador', 'moderado', 'agressivo', 'personalizado')),
    taxa_alvo_valor numeric(4, 2),
    taxa_alvo_unidade text
        check (taxa_alvo_unidade in ('percentual_semana', 'kg_semana')),
    peso_alvo numeric(4, 1) check (peso_alvo between 20.0 and 400.0),
    base_data date,
    criado_em timestamptz not null,
    constraint fase_datas_check check (data_fim >= data_inicio),
    -- RN-28, RN-29, RN-33, RN-41
    constraint fase_plano_check check (
        (tipo = 'manutencao'
            and nivel is null
            and taxa_alvo_valor is null
            and taxa_alvo_unidade is null
            and peso_alvo is null)
        or (tipo <> 'manutencao'
            and nivel is not null
            and taxa_alvo_valor is not null
            and taxa_alvo_unidade is not null
            and peso_alvo is not null)
    ),
    -- RN-19: o fim nulo deixa o intervalo aberto à direita
    constraint fase_sem_sobreposicao exclude using gist (
        usuario_id with =,
        daterange(data_inicio, data_fim, '[]') with &&
    )
);

create table registro (
    id int generated always as identity primary key,
    usuario_id int not null references usuario (id) on delete cascade,
    data date not null,
    peso numeric(4, 1) not null check (peso between 20.0 and 400.0),
    cintura numeric(4, 1) check (cintura between 40.0 and 200.0),
    criado_em timestamptz not null,
    unique (usuario_id, data)
);
