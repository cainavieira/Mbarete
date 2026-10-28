package mbarete.dto;

import java.util.List;
import mbarete.regras.LeituraDoCsv.LinhaRecusada;
import mbarete.regras.LeituraDoCsv.LinhaValida;

/**
 * O relatório da pré-visualização: as linhas válidas, cada uma dizendo se substitui um registro, e
 * as recusadas com número e motivo (RN-58). As contagens saem do tamanho das listas.
 */
public record PrevisualizacaoDto(List<LinhaValida> validas, List<LinhaRecusada> recusadas) {}
