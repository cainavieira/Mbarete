import type { components } from './contrato'

/** Um esquema do contrato pelo nome, como o backend o declara (ADR-0027). */
export type Esquema<Nome extends keyof components['schemas']> = components['schemas'][Nome]
