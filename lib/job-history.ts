import AsyncStorage from "@react-native-async-storage/async-storage";
import type { ProcessingResult } from "@/lib/apk-cleaner";

const STORAGE_KEY = "apk-cleaner-history-v1";

export type HistoryItem = Pick<ProcessingResult, "outputName" | "outputUri" | "signed" | "dexPatched" | "manifestPatched" | "removedFiles"> & {
  id: string;
  sourceName: string;
  createdAt: string;
};

export async function loadHistory(): Promise<HistoryItem[]> {
  const raw = await AsyncStorage.getItem(STORAGE_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as HistoryItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export async function addHistory(sourceName: string, result: ProcessingResult): Promise<HistoryItem> {
  const item: HistoryItem = { id: `${Date.now()}-${result.outputName}`, sourceName, createdAt: new Date().toISOString(), ...result };
  const next = [item, ...(await loadHistory())].slice(0, 20);
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  return item;
}

export async function clearHistory() {
  await AsyncStorage.removeItem(STORAGE_KEY);
}
