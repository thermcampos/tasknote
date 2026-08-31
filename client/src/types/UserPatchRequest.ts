export type UserPatchRequest = {
  name: string | null;
  email: string | null;
  password: string | null;
  passwordAgain: string | null;
  lang: string | null;
  theme?: string | null;
};
