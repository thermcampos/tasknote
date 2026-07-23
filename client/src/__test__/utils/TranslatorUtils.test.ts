import { describe, expect, it } from 'vitest';
import { translateTaskResponse } from '../../utils/TranslatorUtils';
import { TaskResponse } from '../../types/TaskResponse';

const tasks: TaskResponse[] = [
  {
    id: 1,
    description: 'description',
    completed: false,
    highPriority: true,
    dueDate: '',
    dueDateFmt: '',
    lastUpdate: 'Moments ago',
    tag: 'test',
    urls: []
  }
];

describe('Translator utils unit tests', () => {
  it('should translate all task responses to pt_br', () => {
    const newListPtBr = JSON.parse(JSON.stringify(tasks));
    const translatedPtBr = translateTaskResponse(newListPtBr, 'pt_br');
    expect(translatedPtBr.length).toBe(1);

    translatedPtBr.forEach((task: TaskResponse) => {
      expect(task.lastUpdate).toBe('Momentos atrás');
    });
  });

  it('should translate all task responses to es', () => {
    const newListEs = JSON.parse(JSON.stringify(tasks));
    const translatedEs = translateTaskResponse(newListEs, 'es');
    expect(translatedEs.length).toBe(1);

    translatedEs.forEach((task: TaskResponse) => {
      expect(task.lastUpdate).toBe('Hace momentos');
    });
  });

  it('should translate all task responses to ru', () => {
    const newListRu = JSON.parse(JSON.stringify(tasks));
    const translatedRu = translateTaskResponse(newListRu, 'ru');
    expect(translatedRu.length).toBe(1);

    translatedRu.forEach((task: TaskResponse) => {
      expect(task.lastUpdate).toBe('Несколько минут назад');
    });
  });

  it('should not translate a task responses from en', () => {
    const newListEn = JSON.parse(JSON.stringify(tasks));
    const translatedEn = translateTaskResponse(newListEn, 'en');
    expect(translatedEn.length).toBe(1);

    translatedEn.forEach((task: TaskResponse) => {
      expect(task.lastUpdate).toBe('Moments ago');
    });
  });
});
