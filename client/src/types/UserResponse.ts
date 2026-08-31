export type UserResponse = {
  userId: number;
  name: string | null;
  email: string;
  admin: boolean;
  createdAt: Date;
  gravatarImageUrl: string;
  lang: string;
  lastLogin: string;
  theme: string;
};
