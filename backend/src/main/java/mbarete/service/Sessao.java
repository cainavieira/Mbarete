package mbarete.service;

/** Os dois tokens que vão nos cookies: o de acesso, um JWT, e o de renovação, aleatório. */
public record Sessao(String acesso, String renovacao) {}
