import { serverResponsesTranslations, timeAgoTranslations } from '../constants/languageConstants';
import { serverResponses } from '../constants/serverResponses';
import { TaskResponse } from '../types/TaskResponse';
import USER_LANG from '../types/UserLangs';

/**
 * Translates a given message to a given language
 *
 * @param {string} message The message to be translated.
 * @param {string} target The target language. One of USER_LANG
 * @returns {string} The translated message.
 */
function translateTimeMessage(message: string, target: string): string {
  if (target === USER_LANG.ENGLISH) {
    return message;
  }

  if (message === 'Due tomorrow') {
    return timeAgoTranslations[`due tomorrow_${target}`] ?? message;
  }

  if (message === 'Due today') {
    return timeAgoTranslations[`due today_${target}`] ?? message;
  }

  const firstSpace = message.indexOf(' ');
  const numberValue = message.substring(0, firstSpace);
  const textValue = message.substring(firstSpace).trim();

  const keys: string[] = Object.keys(timeAgoTranslations);
  const keyForLang: string = `${textValue}_${target}`;

  if (keys.includes(keyForLang)) {
    const message = timeAgoTranslations[keyForLang].replace('{X}', numberValue);

    // Fixes for Russian language
    const shouldUpdateYearsLeft = target === USER_LANG.RUSSIAN
      && parseInt(numberValue) > 4
      && textValue.includes('left')
      && textValue.includes('years');

    if (shouldUpdateYearsLeft) {
      const valuesToConsider: string[] = ['5', '6', '7', '8', '9'];
      let finalTranslation = '';
      valuesToConsider.forEach((valueStr: string) => {
        if (numberValue.endsWith(valueStr)) {
          finalTranslation = `осталось ${numberValue} лет`;
        }
      });
      return finalTranslation;
    }

    const shouldUpdateMonthsLeft = target === USER_LANG.RUSSIAN
      && parseInt(numberValue) > 4
      && textValue.includes('left')
      && textValue.includes('months');

    if (shouldUpdateMonthsLeft) {
      const valuesToConsider: string[] = ['5', '6', '7', '8', '9', '10'];
      let finalTranslation = '';
      valuesToConsider.forEach((valueStr: string) => {
        if (numberValue.endsWith(valueStr)) {
          finalTranslation = `осталось ${numberValue} месяцев`;
        }
      });
      return finalTranslation;
    }

    const shouldUpdateDaysLeft = target === USER_LANG.RUSSIAN
      && parseInt(numberValue) > 4
      && textValue.includes('left')
      && textValue.includes('days');

    if (shouldUpdateDaysLeft) {
      const valuesToConsider: string[] = ['5', '6', '7', '8', '9', '10'];
      let finalTranslation = '';
      valuesToConsider.forEach((valueStr: string) => {
        if (numberValue.endsWith(valueStr)) {
          finalTranslation = `осталось ${numberValue} дней`;
        }
      });
      return finalTranslation;
    }

    return message;
  }

  return message.includes(' ago')
    ? timeAgoTranslations[`default_${target}`]
    : message;
}

/**
 * Translates a server response message from English to a target language.
 *
 * @param {string} message The text message to be translated.
 * @param {string} target The target language to be translated.
 * @returns The translated message.
 */
function translateServerResponse(message: string, target: string): string {
  if (target === USER_LANG.ENGLISH) {
    return message;
  }
  const serverResponseKeys: string[] = Object.keys(serverResponses);
  if (serverResponseKeys.includes(message)) {
    const value = serverResponses[message];
    const key = `${value}_${target}`;
    return serverResponsesTranslations[key];
  }
  return message;
}

/**
 * Translates all responses from tasks server API.
 *
 * @param {TaskResponse[]} tasks The tasks to be translated.
 * @param {string} target The target language.
 * @returns {TaskResponse[]} Array of tasks translated into the target language.
 */
function translateTaskResponse(tasks: TaskResponse[], target: string): TaskResponse[] {
  tasks.forEach((task: TaskResponse) => {
    task.lastUpdate = translateTimeMessage(task.lastUpdate, target);
    if (task.dueDateFmt) {
      task.dueDateFmt = translateTimeMessage(task.dueDateFmt, target);
    }
  });
  return tasks;
}

export { translateTaskResponse, translateServerResponse, translateTimeMessage };
