import { NativeModules, Platform } from "react-native";

export type Detection = { id: string; label: string; references: number };
export type PackageModule = { name: string; kind: string };

export type Analysis = {
  jobId: string;
  filename: string;
  size: number;
  sha256: string;
  dexCount: number;
  networkCount: number;
  splitMerged: boolean;
  detections: Detection[];
  modules: PackageModule[];
  warnings: string[];
};

export type ProcessingResult = {
  outputUri: string;
  outputName: string;
  signed: boolean;
  dexPatched: number;
  manifestPatched: number;
  removedFiles: number;
  splitMerged: boolean;
  xmlLayoutCleanupAvailable: boolean;
};

type NativeCleaner = {
  analyze(uri: string, filename: string): Promise<Analysis>;
  process(options: {
    jobId: string;
    profile: "safe" | "balanced" | "deep";
    patchAds: boolean;
    stripDebug: boolean;
    sourceName: string;
  }): Promise<ProcessingResult>;
};

function nativeCleaner(): NativeCleaner {
  if (Platform.OS !== "android" || !NativeModules.ApkCleanerNative) {
    throw new Error("Yerel APK motoru yalnızca bu uygulamanın Android derlemesinde kullanılabilir.");
  }
  return NativeModules.ApkCleanerNative as NativeCleaner;
}

export function analyzePackage(uri: string, filename: string) {
  return nativeCleaner().analyze(uri, filename);
}

export function processPackage(options: Parameters<NativeCleaner["process"]>[0]) {
  return nativeCleaner().process(options);
}

export function formatSize(bytes: number) {
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`;
}
