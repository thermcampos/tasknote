type TaskResponse = {
  id: number;
  description: string;
  completed: boolean;
  highPriority: boolean;
  dueDate: string;
  dueDateFmt: string;
  lastUpdate: string;
  tags: string[];
  urls: string[];
};

export type { TaskResponse };
