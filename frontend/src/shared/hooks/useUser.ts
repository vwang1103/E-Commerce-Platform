"use client";

import { createPersistedStore, useStore } from "@/shared/lib/store";
import { isUserIdentity, type UserIdentity } from "@/shared/lib/user";

const userStore = createPersistedStore<UserIdentity | null>("arcade.user", null, (value) => (isUserIdentity(value) ? value : null));

/** Provides the signed-in demo identity shared by every storefront feature. */
export function useUser() {
  const user = useStore(userStore);
  return {
    user,
    userId: user?.userId ?? "",
    username: user?.username ?? "",
    login: (identity: UserIdentity) => userStore.set(identity),
    logout: () => userStore.set(null),
  };
}
