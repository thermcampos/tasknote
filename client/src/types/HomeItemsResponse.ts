import { TaskResponse } from './TaskResponse';
import { NoteResponse } from './NoteResponse';

type HomeItemsResponse = {
  tasks: TaskResponse[];
  notes: NoteResponse[];
};

export type { HomeItemsResponse };
