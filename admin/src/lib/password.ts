import bcrypt from "bcryptjs";

export const hashPassword = (pw: string) => bcrypt.hash(pw, 10);
export const verifyPassword = (pw: string, hash: string) => bcrypt.compare(pw, hash);

export function passwordError(pw: string): string | null {
  if (pw.length < 8) return "short";
  if (!/[A-Za-z\u0600-\u06FF]/.test(pw)) return "letter";
  if (!/[0-9\u0660-\u0669]/.test(pw)) return "digit";
  return null;
}
