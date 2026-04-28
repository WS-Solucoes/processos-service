package ws.erh.cadastro.processo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ws.erh.core.enums.processo.TipoCampo;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

/**
 * Testes do {@link ProcessoValidacaoService#validarSchemaFormulario(ProcessoModelo, String)} (S6.9).
 */
class ProcessoValidacaoServiceSchemaTest {

    private final ProcessoValidacaoService service = new ProcessoValidacaoService();

    private ProcessoCampoModelo campo(String nome, String label, TipoCampo tipo, String opcoes) {
        ProcessoCampoModelo c = new ProcessoCampoModelo();
        c.setNomeCampo(nome);
        c.setLabel(label);
        c.setTipoCampo(tipo);
        c.setOpcoesSelect(opcoes);
        c.setObrigatorio(false);
        return c;
    }

    private ProcessoModelo modeloCom(ProcessoCampoModelo... campos) {
        ProcessoModelo m = new ProcessoModelo();
        m.setCamposAdicionais(List.of(campos));
        return m;
    }

    @Test
    @DisplayName("modelo nulo ou sem campos: não valida nada")
    void semCampos_naoLanca() {
        service.validarSchemaFormulario(null, "{}");
        service.validarSchemaFormulario(new ProcessoModelo(), "{}");
        service.validarSchemaFormulario(modeloCom(), "{}");
    }

    @Test
    @DisplayName("dados vazios: não valida (obrigatoriedade é separada)")
    void dadosVazios_naoLanca() {
        service.validarSchemaFormulario(modeloCom(campo("idade", "Idade", TipoCampo.NUMBER, null)), null);
        service.validarSchemaFormulario(modeloCom(campo("idade", "Idade", TipoCampo.NUMBER, null)), "");
        service.validarSchemaFormulario(modeloCom(campo("idade", "Idade", TipoCampo.NUMBER, null)), "{}");
    }

    @Test
    @DisplayName("NUMBER aceita número e string numérica; rejeita texto")
    void number_validacao() {
        ProcessoModelo m = modeloCom(campo("idade", "Idade", TipoCampo.NUMBER, null));
        service.validarSchemaFormulario(m, "{\"idade\": 30}");
        service.validarSchemaFormulario(m, "{\"idade\": \"42.5\"}");
        assertThatThrownBy(() -> service.validarSchemaFormulario(m, "{\"idade\": \"abc\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Idade")
                .hasMessageContaining("numérico");
    }

    @Test
    @DisplayName("DATE aceita ISO yyyy-MM-dd; rejeita outros formatos")
    void date_validacao() {
        ProcessoModelo m = modeloCom(campo("inicio", "Data Início", TipoCampo.DATE, null));
        service.validarSchemaFormulario(m, "{\"inicio\": \"2026-04-29\"}");
        assertThatThrownBy(() -> service.validarSchemaFormulario(m, "{\"inicio\": \"29/04/2026\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Data Início");
    }

    @Test
    @DisplayName("BOOLEAN aceita true/false (boolean ou string); rejeita outros")
    void boolean_validacao() {
        ProcessoModelo m = modeloCom(campo("ativo", "Ativo", TipoCampo.BOOLEAN, null));
        service.validarSchemaFormulario(m, "{\"ativo\": true}");
        service.validarSchemaFormulario(m, "{\"ativo\": \"false\"}");
        assertThatThrownBy(() -> service.validarSchemaFormulario(m, "{\"ativo\": \"talvez\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ativo")
                .hasMessageContaining("true ou false");
    }

    @Test
    @DisplayName("SELECT aceita apenas valores presentes em opcoesSelect (split por |)")
    void select_validacao() {
        ProcessoModelo m = modeloCom(
                campo("forma", "Forma", TipoCampo.SELECT, "Aereo|Rodoviario|Ferroviario"));
        service.validarSchemaFormulario(m, "{\"forma\": \"Aereo\"}");
        assertThatThrownBy(() -> service.validarSchemaFormulario(m, "{\"forma\": \"Maritimo\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Forma")
                .hasMessageContaining("Aereo")
                .hasMessageContaining("Rodoviario");
    }

    @Test
    @DisplayName("SELECT sem opcoesSelect declarado: aceita qualquer string")
    void select_semOpcoes_aceitaQualquer() {
        ProcessoModelo m = modeloCom(campo("livre", "Livre", TipoCampo.SELECT, null));
        service.validarSchemaFormulario(m, "{\"livre\": \"qualquer\"}");
    }

    @Test
    @DisplayName("TEXT/TEXTAREA aceitam qualquer valor")
    void text_aceitaQualquer() {
        ProcessoModelo m = modeloCom(
                campo("nome", "Nome", TipoCampo.TEXT, null),
                campo("desc", "Descrição", TipoCampo.TEXTAREA, null));
        service.validarSchemaFormulario(m, "{\"nome\": \"Joao\", \"desc\": \"linha1\\nlinha2\"}");
    }

    @Test
    @DisplayName("Múltiplos erros: aglutina em uma única mensagem separada por ;")
    void multiplosErros_aglutina() {
        ProcessoModelo m = modeloCom(
                campo("idade", "Idade", TipoCampo.NUMBER, null),
                campo("inicio", "Data Início", TipoCampo.DATE, null));
        assertThatThrownBy(() -> service.validarSchemaFormulario(m,
                "{\"idade\": \"abc\", \"inicio\": \"29/04/2026\"}"))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).contains("Idade");
                    assertThat(ex.getMessage()).contains("Data Início");
                    assertThat(ex.getMessage()).contains(";");
                });
    }

    @Test
    @DisplayName("JSON inválido lança IllegalStateException")
    void jsonInvalido_lanca() {
        ProcessoModelo m = modeloCom(campo("idade", "Idade", TipoCampo.NUMBER, null));
        assertThatThrownBy(() -> service.validarSchemaFormulario(m, "{idade: 30"))
                .isInstanceOf(IllegalStateException.class);
    }
}
