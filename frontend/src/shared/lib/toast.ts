import { createMemoryStore } from "./store";

export type ToastTone = "success" | "error" | "info";

export interface Toast {
  id: number;
  message: string;
  tone: ToastTone;
}

export const toastStore = createMemoryStore<readonly Toast[]>([]);

let nextId = 1;

export function dismissToast(id: number): void {
  toastStore.set(toastStore.getSnapshot().filter((toast) => toast.id !== id));
}

export function toast(message: string, tone: ToastTone = "success"): void {
  const id = nextId++;
  toastStore.set([...toastStore.getSnapshot().slice(-2), { id, message, tone }]);
  setTimeout(() => dismissToast(id), tone === "error" ? 6000 : 3500);
}
