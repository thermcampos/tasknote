import { describe, expect, it } from 'vitest';
import { translateTimeMessage, translateServerResponse } from '../../utils/TranslatorUtils';
import { serverResponses } from '../../constants/serverResponses';

describe('Russian Utils unit tests', () => {
  it('should translate all time ago messages to ru', () => {
    expect(translateTimeMessage('2 years ago', 'ru')).toBe('2 года назад');
    expect(translateTimeMessage('1 year ago', 'ru')).toBe('1 год назад');
    expect(translateTimeMessage('2 months ago', 'ru')).toBe('2 месяца назад');
    expect(translateTimeMessage('1 month ago', 'ru')).toBe('1 месяц назад');
    expect(translateTimeMessage('2 days ago', 'ru')).toBe('2 дня назад');
    expect(translateTimeMessage('1 day ago', 'ru')).toBe('1 день назад');
    expect(translateTimeMessage('2 hours ago', 'ru')).toBe('2 часа назад');
    expect(translateTimeMessage('1 hour ago', 'ru')).toBe('1 час назад');
    expect(translateTimeMessage('2 minutes ago', 'ru')).toBe('2 минуты назад');
    expect(translateTimeMessage('1 minute ago', 'ru')).toBe('1 минуту назад');
    expect(translateTimeMessage('2 seconds ago', 'ru')).toBe('2 секунды назад');
    expect(translateTimeMessage('1 second ago', 'ru')).toBe('1 секунду назад');
    expect(translateTimeMessage('Moments ago', 'ru')).toBe('Несколько минут назад');
    expect(translateTimeMessage('lala', 'ru')).toBe('lala');
    expect(translateTimeMessage('null', 'ru')).toBe('null');
  });

  it('should translate all time left messages to ru', () => {
    expect(translateTimeMessage('1 year left', 'ru')).toBe('Остался 1 год');
    expect(translateTimeMessage('2 years left', 'ru')).toBe('осталось 2 года');
    expect(translateTimeMessage('3 years left', 'ru')).toBe('осталось 3 года');
    expect(translateTimeMessage('4 years left', 'ru')).toBe('осталось 4 года');
    expect(translateTimeMessage('5 years left', 'ru')).toBe('осталось 5 лет');
    expect(translateTimeMessage('6 years left', 'ru')).toBe('осталось 6 лет');
    expect(translateTimeMessage('7 years left', 'ru')).toBe('осталось 7 лет');
    expect(translateTimeMessage('8 years left', 'ru')).toBe('осталось 8 лет');
    expect(translateTimeMessage('9 years left', 'ru')).toBe('осталось 9 лет');
    expect(translateTimeMessage('1 month left', 'ru')).toBe('Остался 1 месяц');
    expect(translateTimeMessage('2 months left', 'ru')).toBe('осталось 2 месяца');
    expect(translateTimeMessage('3 months left', 'ru')).toBe('осталось 3 месяца');
    expect(translateTimeMessage('4 months left', 'ru')).toBe('осталось 4 месяца');
    expect(translateTimeMessage('5 months left', 'ru')).toBe('осталось 5 месяцев');
    expect(translateTimeMessage('6 months left', 'ru')).toBe('осталось 6 месяцев');
    expect(translateTimeMessage('7 months left', 'ru')).toBe('осталось 7 месяцев');
    expect(translateTimeMessage('8 months left', 'ru')).toBe('осталось 8 месяцев');
    expect(translateTimeMessage('9 months left', 'ru')).toBe('осталось 9 месяцев');
    expect(translateTimeMessage('1 day left', 'ru')).toBe('Остался 1 день');
    expect(translateTimeMessage('2 days left', 'ru')).toBe('осталось 2 дня');
    expect(translateTimeMessage('3 days left', 'ru')).toBe('осталось 3 дня');
    expect(translateTimeMessage('4 days left', 'ru')).toBe('осталось 4 дня');
    expect(translateTimeMessage('5 days left', 'ru')).toBe('осталось 5 дней');
    expect(translateTimeMessage('6 days left', 'ru')).toBe('осталось 6 дней');
    expect(translateTimeMessage('7 days left', 'ru')).toBe('осталось 7 дней');
    expect(translateTimeMessage('8 days left', 'ru')).toBe('осталось 8 дней');
    expect(translateTimeMessage('9 days left', 'ru')).toBe('осталось 9 дней');
    expect(translateTimeMessage('Due tomorrow', 'ru')).toBe('Срок завтра');
    expect(translateTimeMessage('Due today', 'ru')).toBe('Срок сегодня');
    expect(translateTimeMessage('lala', 'ru')).toBe('lala');
    expect(translateTimeMessage('null', 'ru')).toBe('null');
  });

  it('should translate all server responses to ru', () => {
    const keys: string[] = Object.keys(serverResponses);

    expect(translateServerResponse(keys[0], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 8 символов.');
    expect(translateServerResponse(keys[1], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 8 символов, 1 специальный символ.');
    expect(translateServerResponse(keys[2], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 8 символов, 1 цифру, 1 специальный символ.');
    expect(translateServerResponse(keys[3], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 8 символов, 1 заглавную букву, 1 цифру, 1 специальный символ.');
    expect(translateServerResponse(keys[4], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 8 символов, 1 заглавную букву, 1 специальный символ.');
    expect(translateServerResponse(keys[5], 'ru')).toBe('Неправильный пароль: Пароль должен содержать как минимум 1 заглавную букву и 1 специальный символ.');
    expect(translateServerResponse(keys[6], 'ru')).toBe('Неправильный пароль: Пароль должен содержать хотя бы 1 специальный символ.');
    expect(translateServerResponse(keys[7], 'ru')).toBe('Неправильный пароль: Пароль должен содержать не менее 1 цифры.');
    expect(translateServerResponse(keys[8], 'ru')).toBe('Электронная почта уже используется!');
    expect(translateServerResponse(keys[9], 'ru')).toBe('Доступ запрещен!');
    expect(translateServerResponse(keys[10], 'ru')).toBe('Если введенный вами адрес электронной почты связан с учетной записью, вы вскоре получите ссылку для сброса пароля.');
    expect(translateServerResponse(keys[11], 'ru')).toBe('Внутренняя ошибка сервера!');
    expect(translateServerResponse(keys[12], 'ru')).toBe('Достигнут максимальный лимит попыток входа. Пожалуйста, подождите 30 минут');
    expect(translateServerResponse(keys[13], 'ru')).toBe('Ошибка сети при попытке получить ресурс.');
    expect(translateServerResponse(keys[14], 'ru')).toBe('Пожалуйста, подтвердите свой адрес электронной почты, прежде чем продолжить');
    expect(translateServerResponse(keys[15], 'ru')).toBe('Пожалуйста, заполните все поля');
    expect(translateServerResponse(keys[16], 'ru')).toBe('Пожалуйста, введите свой адрес электронной почты');
    expect(translateServerResponse(keys[17], 'ru')).toBe('Пожалуйста, введите свое имя пользователя и пароль!');
    expect(translateServerResponse(keys[18], 'ru')).toBe('Пожалуйста, введите новый пароль');
    expect(translateServerResponse(keys[19], 'ru')).toBe('Пожалуйста, введите не менее 3 символов');
    expect(translateServerResponse(keys[20], 'ru')).toBe('Максимальная длина текста — 2000 символов.');
    expect(translateServerResponse(keys[21], 'ru')).toBe('Неизвестная ошибка');
    expect(translateServerResponse(keys[22], 'ru')).toBe('Неправильная или отсутствующая идентификация');
    expect(translateServerResponse(keys[23], 'ru')).toBe('Неверная или отсутствующая информация!');
    expect(translateServerResponse(keys[24], 'ru')).toBe('Неправильный пользователь или пароль');
    expect(translateServerResponse(keys[25], 'ru')).toBe('Нечего обновлять!');
  });
});
