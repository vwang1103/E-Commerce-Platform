export interface UserIdentity {
  userId: string;
  username: string;
}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isUserIdentity(value: unknown): value is UserIdentity {
  if (typeof value !== "object" || value === null) return false;
  const candidate = value as Partial<UserIdentity>;
  return (
    typeof candidate.userId === "string" &&
    UUID_PATTERN.test(candidate.userId) &&
    typeof candidate.username === "string" &&
    candidate.username.trim().length > 0
  );
}
