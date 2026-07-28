type NoteResponse = {
  id: number;
  title: string;
  description: string;
  url: string | null;
  tags: string[];
  lastUpdate: string;
  shared: boolean;
  shareToken: string | null;
  archived: boolean;
};

export type { NoteResponse };
