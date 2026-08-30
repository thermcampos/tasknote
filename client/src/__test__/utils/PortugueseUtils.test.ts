import { describe, expect, it } from 'vitest';
import { translateTimeMessage, translateServerResponse } from '../../utils/TranslatorUtils';
import { serverResponses } from '../../constants/serverResponses';

describe('Portuguese Utils unit tests', () => {
  it('should translate all time ago messages to pt_br', () => {
    expect(translateTimeMessage('2 years ago', 'pt_br')).toBe('2 anos atrás');
    expect(translateTimeMessage('1 year ago', 'pt_br')).toBe('1 ano atrás');
    expect(translateTimeMessage('2 months ago', 'pt_br')).toBe('2 meses atrás');
    expect(translateTimeMessage('1 month ago', 'pt_br')).toBe('1 mês atrás');
    expect(translateTimeMessage('2 days ago', 'pt_br')).toBe('2 dias atrás');
    expect(translateTimeMessage('1 day ago', 'pt_br')).toBe('1 dia atrás');
    expect(translateTimeMessage('2 hours ago', 'pt_br')).toBe('2 horas atrás');
    expect(translateTimeMessage('1 hour ago', 'pt_br')).toBe('1 hora atrás');
    expect(translateTimeMessage('2 minutes ago', 'pt_br')).toBe('2 minutos atrás');
    expect(translateTimeMessage('1 minute ago', 'pt_br')).toBe('1 minuto atrás');
    expect(translateTimeMessage('2 seconds ago', 'pt_br')).toBe('2 segundos atrás');
    expect(translateTimeMessage('1 second ago', 'pt_br')).toBe('1 segundo atrás');
    expect(translateTimeMessage('Moments ago', 'pt_br')).toBe('Momentos atrás');
    expect(translateTimeMessage('lala', 'pt_br')).toBe('lala');
    expect(translateTimeMessage('null', 'pt_br')).toBe('null');
  });

  it('should translate all time left messages to pt_br', () => {
    expect(translateTimeMessage('2 years left', 'pt_br')).toBe('2 anos restantes');
    expect(translateTimeMessage('1 year left', 'pt_br')).toBe('1 ano restante');
    expect(translateTimeMessage('2 months left', 'pt_br')).toBe('2 meses restantes');
    expect(translateTimeMessage('1 month left', 'pt_br')).toBe('1 mês restante');
    expect(translateTimeMessage('2 days left', 'pt_br')).toBe('2 dias restantes');
    expect(translateTimeMessage('1 day left', 'pt_br')).toBe('1 dia restante');
    expect(translateTimeMessage('Due tomorrow', 'pt_br')).toBe('Vence amanhã');
    expect(translateTimeMessage('Due today', 'pt_br')).toBe('Vence hoje');
    expect(translateTimeMessage('lala', 'pt_br')).toBe('lala');
    expect(translateTimeMessage('null', 'pt_br')).toBe('null');
  });

  it('should translate all server responses to pt_br', () => {
    const keys: string[] = Object.keys(serverResponses);

    expect(translateServerResponse(keys[0], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 8 letras');
    expect(translateServerResponse(keys[1], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 8 letras, 1 caracter especial');
    expect(translateServerResponse(keys[2], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 8 letras, 1 número, 1 caracter especial');
    expect(translateServerResponse(keys[3], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 8 letras, 1 maiúscula, 1 número, 1 caracter especial');
    expect(translateServerResponse(keys[4], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 8 letras, 1 maiúscula, 1 caracter especial');
    expect(translateServerResponse(keys[5], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 1 maiúscula, 1 caracter especial');
    expect(translateServerResponse(keys[6], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 1 caracter especial');
    expect(translateServerResponse(keys[7], 'pt_br')).toBe('Senha fraca: Senha deve possuir pelo menos 1 número');
    expect(translateServerResponse(keys[8], 'pt_br')).toBe('E-mail já cadastrado!');
    expect(translateServerResponse(keys[9], 'pt_br')).toBe('Proibido! Acesso negado');
    expect(translateServerResponse(keys[10], 'pt_br')).toBe('Se o endereço de e-mail informado estiver associado a uma conta, você receberá um link para resetar a senha em breve.');
    expect(translateServerResponse(keys[11], 'pt_br')).toBe('Erro Interno do Servidor!');
    expect(translateServerResponse(keys[12], 'pt_br')).toBe('Limite máximo de tentativas atingido. Por favor aguarde 3 minutos');
    expect(translateServerResponse(keys[13], 'pt_br')).toBe('Erro de rede ao tentar obter recursos.');
    expect(translateServerResponse(keys[14], 'pt_br')).toBe('Por favor, confirme seu e-mail antes de continuar');
    expect(translateServerResponse(keys[15], 'pt_br')).toBe('Por favor, preencha todos os campos');
    expect(translateServerResponse(keys[16], 'pt_br')).toBe('Por favor, informe seu e-mail');
    expect(translateServerResponse(keys[17], 'pt_br')).toBe('Por favor, informe seu e-mail e senha!');
    expect(translateServerResponse(keys[18], 'pt_br')).toBe('Por favor, informe a nova senha');
    expect(translateServerResponse(keys[19], 'pt_br')).toBe('Por favor, digite pelo menos 3 letras');
    expect(translateServerResponse(keys[20], 'pt_br')).toBe('O tamanho máximo do texto é 2000');
    expect(translateServerResponse(keys[21], 'pt_br')).toBe('Erro desconhecido');
    expect(translateServerResponse(keys[22], 'pt_br')).toBe('Identificação incorreta ou faltando');
    expect(translateServerResponse(keys[23], 'pt_br')).toBe('Informação errada ou incompleta!');
    expect(translateServerResponse(keys[24], 'pt_br')).toBe('E-mail ou senha inválidos!');
    expect(translateServerResponse(keys[25], 'pt_br')).toBe('Nada para atualizar!');
  });
});
